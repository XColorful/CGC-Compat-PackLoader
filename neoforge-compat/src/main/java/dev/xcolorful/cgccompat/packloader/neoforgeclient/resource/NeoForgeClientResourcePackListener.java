package dev.xcolorful.cgccompat.packloader.neoforgeclient.resource;

import dev.xcolorful.cgccompat.packloader.CgccPackLoader;
import dev.xcolorful.cgccompat.packloader.client.resource.ClientResourcePackFinder;
import net.minecraft.server.packs.PackType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * 资源包侧的平台监听。只做事件订阅与 pack 类型过滤，实际注册逻辑在 core 的
 * {@link ClientResourcePackFinder}。
 *
 * <p>{@code value = Dist.CLIENT} 让 NeoForge 只在物理客户端注册本类，专用服务端不会 classload 它，
 * 资源包加载因此被彻底隔离。
 *
 * @see net.neoforged.neoforge.client.loading.ClientModLoader
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = CgccPackLoader.MOD_ID)
public final class NeoForgeClientResourcePackListener {

    private NeoForgeClientResourcePackListener() {
    }

    /**
     * 只处理资源包仓库，把注册工作交给 core。
     *
     * @param event NeoForge 在创建包仓库时抛出的事件
     */
    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;

        ClientResourcePackFinder.onAddPackFinders(event::addRepositorySource);
    }
}
