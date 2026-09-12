package dev.xcolorful.cgccompat.packloader.core.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

/**
 * 读写列出额外包目录的配置文件。
 *
 * <p>文件内容是一个 JSON 字符串数组，每一项指向一个目录，该目录的直接子项会被当作包加载，
 * 语义与原版的 {@code resourcepacks}、{@code datapacks} 目录一致。文件不存在时会创建它并写入
 * {@link #DEFAULT_ENTRIES}。
 *
 * <p>本类只接触文件系统，因此与加载器无关；游戏目录与配置文件位置由平台侧提供。
 *
 * @see net.minecraft.server.packs.repository.FolderRepositorySource
 */
public final class ExtraPackConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 配置文件名，位于平台的 config 目录下。 */
    public static final String FILE_NAME = "cgccpackloader.json";

    /** 配置文件不存在时写入的内容。 */
    public static final List<String> DEFAULT_ENTRIES = List.of("./tacz/");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ENTRY_LIST_TYPE = new TypeToken<List<String>>() {}.getType();

    private ExtraPackConfig() {
    }

    /**
     * 读取配置中的目录；{@code configFile} 不存在时先按 {@link #DEFAULT_ENTRIES} 创建它。
     *
     * <p>相对路径以 {@code gameDir} 为基准解析，例如 {@code "./tacz/"} 表示 {@code <gameDir>/tacz}；
     * 绝对路径原样使用。
     *
     * @param gameDir 用于解析相对路径的游戏目录
     * @param configFile 要读取的配置文件
     * @return 解析后的目录；文件不可读或未列出任何目录时为空
     */
    public static List<Path> read(Path gameDir, Path configFile) {
        List<Path> directories = new ArrayList<>();
        for (String entry : readEntries(configFile)) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            try {
                directories.add(gameDir.resolve(entry).normalize());
            } catch (InvalidPathException e) {
                LOGGER.warn("忽略 {} 中非法的包目录 '{}'", configFile, entry, e);
            }
        }
        return List.copyOf(directories);
    }

    private static List<String> readEntries(Path configFile) {
        if (!Files.isRegularFile(configFile)) {
            write(configFile, DEFAULT_ENTRIES);
            return DEFAULT_ENTRIES;
        }
        try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            List<String> entries = GSON.fromJson(reader, ENTRY_LIST_TYPE);
            // 文件里完全没有 JSON 值时 Gson 返回 null。
            return entries == null ? List.of() : entries;
        } catch (IOException | JsonParseException e) {
            // 该文件由用户维护，出错时保持原样而不是覆盖它。
            LOGGER.error("读取 {} 失败，不会加载任何额外包目录", configFile, e);
            return List.of();
        }
    }

    private static void write(Path configFile, List<String> entries) {
        try {
            if (configFile.getParent() != null) {
                Files.createDirectories(configFile.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(entries, writer);
            }
            LOGGER.info("已创建 {}，默认包目录为 {}", configFile, entries);
        } catch (IOException e) {
            LOGGER.error("创建 {} 失败", configFile, e);
        }
    }
}
