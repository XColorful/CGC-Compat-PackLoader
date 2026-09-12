package dev.xcolorful.cgccompat.packloader.core.resource;

import dev.xcolorful.cgccompat.packloader.CgccPackLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.util.InclusiveRange;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * 代理真实的包，并在其缺少 {@code pack.mcmeta} 时补上一份合成元数据。
 *
 * <p>原版会在两处丢弃读不到 {@link PackMetadataSection} 的包：没有 {@code pack.mcmeta} 的目录
 * 根本不会被
 * {@link net.minecraft.server.packs.repository.PackDetector#detectPackResources(Path, List)}
 * 识别；没有它的压缩包则会被
 * {@link net.minecraft.server.packs.repository.Pack#readPackMetadata(PackLocationInfo, Pack.ResourcesSupplier, PackFormat, PackType)}
 * 丢弃。由代理层直接应答元数据查询，包的内容就能在通过这两步的同时仍可被访问。
 *
 * <p>真实元数据优先，只有确实缺失的 section 才会被替换。Forge 合并出的 {@code mod_resources}
 * 包通过 {@code net.minecraftforge.resource.DelegatingPackResources} 做了同样的事。
 *
 * @see AbstractPackResources#getMetadataSection(MetadataSectionType)
 * @see net.minecraft.server.packs.repository.Pack#readPackMetadata(PackLocationInfo, Pack.ResourcesSupplier, PackFormat, PackType)
 */
public class FallbackMetadataPackResources extends AbstractPackResources {

    private final PackResources delegate;
    private final MetadataSectionType<PackMetadataSection> packMetadataType;
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
        this.packMetadataType = PackMetadataSection.forPackType(packType);
        this.fallbackMetadata = new PackMetadataSection(description,
                new InclusiveRange<>(PackFormat.of(packType == PackType.SERVER_DATA
                        ? CgccPackLoader.DATA_PACK_FORMAT
                        : CgccPackLoader.RESOURCE_PACK_FORMAT)));
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMetadataSection(MetadataSectionType<T> type) throws IOException {
        T section = this.delegate.getMetadataSection(type);
        if (section != null) {
            return section;
        }
        // 按 section 名比较，与 Forge 的 DelegatingPackResources 一致：原版会依次用本包类型的
        // section 和 FALLBACK_TYPE 询问，它们是 codec 不同但同名的 record，名称才是唯一稳定的
        // 比较依据。
        return this.packMetadataType.name().equals(type.name())
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
    public IoSupplier<InputStream> getResource(PackType type, Identifier location) {
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
