package dev.xcolorful.cgccompat.packloader.core.resource;

import dev.xcolorful.cgccompat.packloader.CgccPackLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.AbstractPackMetadataResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackMetadataResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.repository.Pack;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

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
 * <p>元数据读取走的是
 * {@link net.minecraft.server.packs.repository.Pack.ResourcesSupplier#openMetadata(PackLocationInfo)}
 * 这条独立路径，因此本类只需实现 {@link PackMetadataResources}，内容访问仍由 {@code delegate} 自己承担。
 *
 * @see AbstractPackMetadataResources#getMetadataSection(MetadataSectionType)
 * @see net.minecraft.server.packs.repository.Pack#readPackMetadata(PackLocationInfo, Pack.ResourcesSupplier, PackFormat, PackType)
 */
public class FallbackMetadataPackResources extends AbstractPackMetadataResources {

    private final PackMetadataResources delegate;
    private final MetadataSectionType<PackMetadataSection> packMetadataType;
    private final PackMetadataSection fallbackMetadata;

    /**
     * @param packType 用于决定合成元数据的包格式
     * @param location 由 {@link #location()} 上报的包位置信息
     * @param delegate 实际提供元数据的包
     * @param description {@code delegate} 没有 {@code pack.mcmeta} 时使用的描述
     */
    public FallbackMetadataPackResources(PackType packType, PackLocationInfo location, PackMetadataResources delegate, Component description) {
        super(location);
        this.delegate = delegate;
        this.packMetadataType = PackMetadataSection.forPackType(packType);
        // 26.3 起包格式是 major.minor，PackCompatibility 按「声明上界 < 当前版本」判 TOO_OLD，
        // 而 PackFormat.of(major) 的 minor 是 0，落在当前 minor 之下。合成元数据本来就不描述具体
        // 内容版本，取整个 minor 区间，免得原版每抬一次 minor 就把包标成旧版本。
        this.fallbackMetadata = new PackMetadataSection(description,
                PackFormat.of(packType == PackType.SERVER_DATA
                        ? CgccPackLoader.DATA_PACK_FORMAT
                        : CgccPackLoader.RESOURCE_PACK_FORMAT).minorRange());
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

    @Override
    public void close() {
        this.delegate.close();
    }
}
