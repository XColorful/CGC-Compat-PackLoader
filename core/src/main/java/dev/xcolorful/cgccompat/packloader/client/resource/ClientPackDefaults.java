package dev.xcolorful.cgccompat.packloader.client.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import dev.xcolorful.cgccompat.packloader.CgccPackLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.packs.repository.RepositorySource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 把额外目录里的资源包默认勾选一次，之后交给原版。
 *
 * <p>原版客户端没有「默认启用」这个概念：{@code PackSource#shouldAddAutomatically()} 只在
 * {@code MinecraftServer} 里被读取，资源包的启停完全由 options.txt 的 {@code resourcePacks} 决定。
 * 所以这一次勾选得自己补——写进 {@link Options#resourcePacks} 并落盘，之后就归原版管：
 * 玩家可以在资源包界面正常取消，取消结果由原版记在 options.txt 里，重启不会被重新勾上。
 *
 * <p>开关和记录都在 {@link CgccPackLoader#clientFile()}：{@code resourcePackEnabledByDefault}
 * 是给用户改的开关，{@code defaultEnabled} 由本类维护。后者用来区分「还没默认勾过」和
 * 「玩家已经取消」，玩家取消过的包不会被重新勾上。
 */
final class ClientPackDefaults {

    private static final String ENABLED_BY_DEFAULT_KEY = "resourcePackEnabledByDefault";
    private static final String DEFAULT_ENABLED_KEY = "defaultEnabled";

    /** 文件缺失或没写 {@link #ENABLED_BY_DEFAULT_KEY} 时采用的值。 */
    private static final boolean DEFAULT_ENABLED_BY_DEFAULT = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ID_LIST_TYPE = new TypeToken<List<String>>() {}.getType();

    /** 只在本次启动的第一次发现时补默认值。之后的重载（F3+T、关闭资源包界面触发的重载）不该把刚取消的包翻回来。 */
    private static boolean applied;

    private ClientPackDefaults() {
    }

    /** 客户端文件的内容。 */
    private record Settings(boolean enabledByDefault, Set<String> defaultEnabled) {
    }

    /**
     * 让 {@code source} 在枚举完包之后把这些包默认勾选一次；开关关掉时原样返回，什么都不做。
     *
     * <p>{@link RepositorySource#loadPacks} 由 {@code PackRepository#reload()} 调用，而客户端启动时
     * {@code Minecraft} 构造里先 {@code reload()}、再 {@code Options#loadSelectedResourcePacks}，
     * 所以这里补上的选项会被同一次资源加载采纳，不需要额外再触发一次重载。唯一注册
     * {@code CLIENT_RESOURCES} finder 的地方是
     * {@code net.minecraftforge.client.loading.ClientModLoader#begin}，用的是 {@code Minecraft}
     * 自己的包仓库，因此这里改的选项就是客户端实际使用的那份。
     *
     * @param source 真正枚举包的 finder
     * @return 补完默认勾选的 finder
     */
    static RepositorySource decorate(RepositorySource source) {
        Settings settings = readSettings();
        if (!settings.enabledByDefault()) {
            return source;
        }
        return consumer -> {
            List<String> ids = new ArrayList<>();
            source.loadPacks(pack -> {
                ids.add(pack.getId());
                consumer.accept(pack);
            });
            defaultEnable(ids, settings);
        };
    }

    private static void defaultEnable(List<String> ids, Settings settings) {
        if (applied) {
            return;
        }
        applied = true;

        Options options = Minecraft.getInstance().options;
        Set<String> defaultEnabled = settings.defaultEnabled();
        List<String> unfamiliar = ids.stream().distinct().filter(id -> !defaultEnabled.contains(id)).toList();
        if (unfamiliar.isEmpty()) {
            return;
        }

        defaultEnabled.addAll(unfamiliar);
        for (String id : unfamiliar) {
            if (!options.resourcePacks.contains(id)) {
                options.resourcePacks.add(id);
            }
        }
        // 必须先落盘选项再写记录：万一记录写失败，下次启动这些 id 已经在 options.txt 里，
        // 会被当成「已经见过」补记下来，不会再被勾一次。
        options.save();
        writeSettings(new Settings(settings.enabledByDefault(), defaultEnabled));
    }

    private static Settings readSettings() {
        Path file = CgccPackLoader.clientFile();
        if (!Files.isRegularFile(file)) {
            Settings defaults = defaults();
            // 写出来用户才知道有这个开关可以改。
            writeSettings(defaults);
            return defaults;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = GSON.fromJson(reader, JsonElement.class);
            if (root == null || !root.isJsonObject()) {
                CgccPackLoader.LOGGER.error("{} is not a JSON object; using the defaults", file);
                return defaults();
            }
            JsonObject object = root.getAsJsonObject();
            return new Settings(enabledByDefaultOf(object), defaultEnabledOf(object));
        } catch (IOException | JsonParseException e) {
            CgccPackLoader.LOGGER.error("Failed to read {}; using the defaults", file, e);
            return defaults();
        }
    }

    private static boolean enabledByDefaultOf(JsonObject object) {
        JsonElement flag = object.get(ENABLED_BY_DEFAULT_KEY);
        // 缺失说明用户删掉了它，按默认值处理；写错类型则报出来，不做静默转换。
        if (flag == null) {
            return DEFAULT_ENABLED_BY_DEFAULT;
        }
        if (!flag.isJsonPrimitive() || !flag.getAsJsonPrimitive().isBoolean()) {
            CgccPackLoader.LOGGER.error("'{}' in {} is not a boolean; using {}", ENABLED_BY_DEFAULT_KEY,
                    CgccPackLoader.CLIENT_FILE_NAME, DEFAULT_ENABLED_BY_DEFAULT);
            return DEFAULT_ENABLED_BY_DEFAULT;
        }
        return flag.getAsBoolean();
    }

    private static Set<String> defaultEnabledOf(JsonObject object) {
        JsonElement list = object.get(DEFAULT_ENABLED_KEY);
        if (list == null || list.isJsonNull()) {
            return new LinkedHashSet<>();
        }
        try {
            List<String> ids = GSON.fromJson(list, ID_LIST_TYPE);
            return ids == null ? new LinkedHashSet<>() : new LinkedHashSet<>(ids);
        } catch (JsonParseException e) {
            // 记录坏了不该拦住游戏启动：当成没记录过，最多再默认勾选一次。
            CgccPackLoader.LOGGER.error("Failed to read '{}' in {}; packs will be default-enabled again",
                    DEFAULT_ENABLED_KEY, CgccPackLoader.CLIENT_FILE_NAME, e);
            return new LinkedHashSet<>();
        }
    }

    private static Settings defaults() {
        return new Settings(DEFAULT_ENABLED_BY_DEFAULT, new LinkedHashSet<>());
    }

    private static void writeSettings(Settings settings) {
        Path file = CgccPackLoader.clientFile();
        JsonObject root = new JsonObject();
        root.addProperty(ENABLED_BY_DEFAULT_KEY, settings.enabledByDefault());
        root.add(DEFAULT_ENABLED_KEY, GSON.toJsonTree(new ArrayList<>(settings.defaultEnabled())));
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            CgccPackLoader.LOGGER.error("Failed to write {}", file, e);
        }
    }
}
