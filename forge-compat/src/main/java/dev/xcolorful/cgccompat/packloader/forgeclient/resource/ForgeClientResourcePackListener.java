package dev.xcolorful.cgccompat.packloader.forgeclient.resource;

import dev.xcolorful.cgccompat.packloader.CgccPackLoader;
import dev.xcolorful.cgccompat.packloader.client.resource.ClientResourcePackFinder;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 资源包侧的平台监听。只做事件订阅与 pack 类型过滤，实际注册逻辑在 core 的
 * {@link ClientResourcePackFinder}。
 *
 * <p>{@code value = Dist.CLIENT} 让 Forge 只在物理客户端注册本类，专用服务端不会 classload 它，
 * 资源包加载因此被彻底隔离。
 *
 * @see net.minecraftforge.client.loading.ClientModLoader
 */
@Mod.EventBusSubscriber(modid = CgccPackLoader.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ForgeClientResourcePackListener {

    private ForgeClientResourcePackListener() {
    }

    /**
     * 只处理资源包仓库，把注册工作交给 core。
     *
     * @param event Forge 在创建包仓库时抛出的事件
     */
    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;

        ClientResourcePackFinder.onAddPackFinders(event::addRepositorySource);
    }
}
