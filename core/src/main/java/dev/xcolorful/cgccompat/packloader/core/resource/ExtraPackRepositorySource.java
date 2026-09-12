package dev.xcolorful.cgccompat.packloader.core.resource;

import com.mojang.logging.LogUtils;
import dev.xcolorful.cgccompat.packloader.CgccPackLoader;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import org.slf4j.Logger;

/**
 * 把配置目录的每个直接子项注册成一个包。
 *
 * <p>对应原版用于 {@code resourcepacks}、{@code datapacks} 目录的
 * {@link net.minecraft.server.packs.repository.FolderRepositorySource}，两点不同：子项没有
 * {@code pack.mcmeta} 也接受（见 {@link FallbackMetadataPackResources}）；目录不存在时直接跳过，
 * 不会创建它。
 *
 * <p>平台侧通过 Forge 的 {@code AddPackFindersEvent} 注册进来，该事件最终会调用
 * {@link net.minecraft.server.packs.repository.PackRepository#addPackFinder(RepositorySource)}。
 */
public class ExtraPackRepositorySource implements RepositorySource {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 包 id 前缀取自模组 id，避免与原版的 {@code file/} 和 Forge 的 {@code mod/} 前缀冲突。
     */
    private static final String PACK_ID_PREFIX = CgccPackLoader.MOD_ID + "";

    private static final String ARCHIVE_SUFFIX = ".zip";

    private final List<Path> directories;
    private final PackType packType;
    private final PackSource packSource;

    /**
     * @param directories 子项会被注册为包的目录
     * @param packType 注册成的包类型
     * @param packSource 在包选择界面上展示的来源
     */
    public ExtraPackRepositorySource(List<Path> directories, PackType packType, PackSource packSource) {
        this.directories = List.copyOf(directories);
        this.packType = packType;
        this.packSource = packSource;
    }

    /**
     * 按 {@link CgccPackLoader} 中记录的路径读取配置并构建 finder。
     *
     * @param packType 注册成的包类型
     * @param packSource 在包选择界面上展示的来源
     * @return 由配置目录构建出的 finder
     */
    public static ExtraPackRepositorySource fromConfig(PackType packType, PackSource packSource) {
        List<Path> directories =
                ExtraPackConfig.read(CgccPackLoader.gameDirectory(), CgccPackLoader.configFile());
        return new ExtraPackRepositorySource(directories, packType, packSource);
    }

    @Override
    public void loadPacks(Consumer<Pack> consumer) {
        for (Path directory : this.directories) {
            if (!Files.isDirectory(directory)) {
                LOGGER.warn("额外包目录 {} 不存在，已跳过", directory);
                continue;
            }
            this.loadDirectory(directory, consumer);
        }
    }

    private void loadDirectory(Path directory, Consumer<Pack> consumer) {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                Pack.ResourcesSupplier supplier = resourcesSupplier(entry);
                if (supplier == null) {
                    LOGGER.info("发现非包条目 '{}'，已忽略", entry);
                    continue;
                }
                Pack pack = this.createPack(directory, entry, supplier);
                if (pack != null) {
                    consumer.accept(pack);
                }
            }
        } catch (IOException e) {
            LOGGER.error("列出 {} 中的包失败", directory, e);
        }
    }

    /**
     * 为 {@code entry} 挑选对应的原版 {@link Pack.ResourcesSupplier}；既不是目录也不是压缩包时返回
     * {@code null}。
     *
     * @see net.minecraft.server.packs.repository.FolderRepositorySource#detectPackResources(Path, boolean)
     */
    @Nullable
    private static Pack.ResourcesSupplier resourcesSupplier(Path entry) {
        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(entry, BasicFileAttributes.class);
        } catch (IOException e) {
            LOGGER.warn("读取 '{}' 的属性失败，已忽略", entry, e);
            return null;
        }

        if (attributes.isDirectory()) {
            return packId -> new PathPackResources(packId, entry, false);
        }
        if (attributes.isRegularFile() && entry.getFileName().toString().endsWith(ARCHIVE_SUFFIX)
                && entry.getFileSystem() == FileSystems.getDefault()) {
            // FilePackResources 接收 File，因此无法读取非默认文件系统上的路径。
            File archive = entry.toFile();
            return packId -> new FilePackResources(packId, archive, false);
        }
        return null;
    }

    /**
     * 为 {@code entry} 创建包。文件名既作为包自身没有 {@code pack.mcmeta} 时的描述，也作为 id 后缀，
     * 使 id 在多个配置目录之间保持唯一。
     */
    @Nullable
    private Pack createPack(Path directory, Path entry, Pack.ResourcesSupplier supplier) {
        String name = entry.getFileName().toString();
        String id = PACK_ID_PREFIX + "/" + directory.getFileName() + "/" + name;
        Component description = Component.literal(name);

        Pack.ResourcesSupplier resources =
                packId -> new FallbackMetadataPackResources(packId, supplier.open(packId), description);

        return Pack.readMetaAndCreate(id, description, false, resources, this.packType, Pack.Position.TOP, this.packSource);
    }
}
