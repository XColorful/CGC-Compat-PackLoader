package dev.xcolorful.cgccompat.packloader.core.resource;

import dev.xcolorful.cgccompat.packloader.CgccPackLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.repository.Pack;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 代理真实的包，并在其缺少 {@code pack.mcmeta} 时补上一份合成元数据。
 *
 * <p>原版会在两处丢弃读不到 {@link PackMetadataSection} 的包：没有 {@code pack.mcmeta} 的目录
 * 根本不会被
 * {@link net.minecraft.server.packs.repository.PackDetector#detectPackResources(Path, List)}
 * 识别；没有它的压缩包则会被
 * {@link net.minecraft.server.packs.repository.Pack#readPackMetadata(PackLocationInfo, Pack.ResourcesSupplier, int)}
 * 丢弃。由代理层直接应答元数据查询，包的内容就能在通过这两步的同时仍可被访问。
 *
 * <p>真实元数据优先，只有确实缺失的 section 才会被替换。Forge 合并出的 {@code mod_resources}
 * 包通过 {@code net.minecraftforge.resource.DelegatingPackResources} 做了同样的事。
 *
 * @see AbstractPackResources#getMetadataSection(MetadataSectionSerializer)
 * @see net.minecraft.server.packs.repository.Pack#readPackMetadata(PackLocationInfo, Pack.ResourcesSupplier, int)
 */
public class FallbackMetadataPackResources extends AbstractPackResources {

    private final PackResources delegate;
    private final PackMetadataSection fallbackMetadata;

    /**
     * @param packType 用于决定合成元数据的包格式
     * @param location 由 {@link #location()} 上报的包位置信息
     * @param delegate 实际提供内容的包
     * @param description {@code delegate} 没有 {@code pack.mcmeta} 时使用的描述
     */
    public FallbackMetadataPackResources(PackType packType, PackLocationInfo location, PackResources delegate, Component description) {
        super(location);
        this.delegate = delegate;
        this.fallbackMetadata = new PackMetadataSection(description, packType == PackType.SERVER_DATA ? CgccPackLoader.DATA_PACK_FORMAT : CgccPackLoader.RESOURCE_PACK_FORMAT, Optional.empty());
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {
        T section = this.delegate.getMetadataSection(serializer);
        if (section != null) {
            return section;
        }
        // 按 section 名比较，与 Forge 的 DelegatingPackResources 一致：入参本身就是 section 的类型
        // 描述符，名称才是跨版本比较时唯一稳定的东西。
        return PackMetadataSection.TYPE.getMetadataSectionName().equals(serializer.getMetadataSectionName())
                ? (T) this.fallbackMetadata
                : null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... paths) {
        return this.delegate.getRootResource(paths);
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        return this.delegate.getResource(type, location);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, PackResources.ResourceOutput output) {
        this.delegate.listResources(type, namespace, path, output);
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return this.delegate.getNamespaces(type);
    }

    @Override
    public void close() {
        this.delegate.close();
    }
}
