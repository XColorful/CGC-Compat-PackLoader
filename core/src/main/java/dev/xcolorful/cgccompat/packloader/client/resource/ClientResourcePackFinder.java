package dev.xcolorful.cgccompat.packloader.client.resource;

import dev.xcolorful.cgccompat.packloader.core.resource.ExtraPackRepositorySource;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

import java.util.function.Consumer;

/**
 * 资源包侧的实现入口。
 *
 * <p>放在 {@code client.resource} 下，使资源包加载远离服务端：唯一调用它的是平台的客户端监听类，
 * 而专用服务端永远不会加载它。数据包由 {@link ExtraPackRepositorySource} 以
 * {@link PackType#SERVER_DATA} 单独贡献，两个物理侧都会注册。
 *
 * <p>在 Forge 上，该 finder 通过 {@code net.minecraftforge.client.loading.ClientModLoader} 抛出的
 * {@code AddPackFindersEvent} 进入游戏。
 *
 * @see net.minecraft.server.packs.repository.FolderRepositorySource
 */
public final class ClientResourcePackFinder {

    /** 资源包来源；{@code required} 为 false，因此包只出现在选择界面，需玩家手动启用。 */
    private static final PackSource PACK_SOURCE = PackSource.DEFAULT;

    private ClientResourcePackFinder() {
    }

    /**
     * 把配置中的额外目录注册为资源包 finder。
     *
     * @param register 平台侧提供的注册回调，对应 {@code AddPackFindersEvent#addRepositorySource}
     */
    public static void onAddPackFinders(Consumer<RepositorySource> register) {
        register.accept(ExtraPackRepositorySource.fromConfig(PackType.CLIENT_RESOURCES, PACK_SOURCE));
    }
}
