package dev.xcolorful.cgccompat.packloader.core.resource;

import java.util.function.Consumer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

/**
 * 数据包侧的实现入口。平台侧只负责事件订阅与 pack 类型过滤，注册逻辑集中在这里。
 *
 * @see net.minecraft.server.packs.repository.ServerPacksSource#createPackRepository(java.nio.file.Path)
 */
public final class ExtraPackFinder {
    /** 数据包类型。 */
    private static final PackType PACK_TYPE = PackType.SERVER_DATA;

    /** 数据包来源；{@code shouldAddAutomatically()} 为 true，新发现的数据包会被自动启用。 */
    private static final PackSource PACK_SOURCE = PackSource.DEFAULT;

    private ExtraPackFinder() {
    }

    /**
     * 把配置中的额外目录注册为数据包 finder。
     *
     * @param register 平台侧提供的注册回调，对应 {@code AddPackFindersEvent#addRepositorySource}
     */
    public static void onAddPackFinders(Consumer<RepositorySource> register) {
        register.accept(ExtraPackRepositorySource.fromConfig(PACK_TYPE, PACK_SOURCE));
    }
}
