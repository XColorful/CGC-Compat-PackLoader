package dev.xcolorful.cgccompat.packloader.forge.resource;

import dev.xcolorful.cgccompat.packloader.CgccPackLoader;
import dev.xcolorful.cgccompat.packloader.core.resource.ExtraPackFinder;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 数据包侧的平台监听。只做事件订阅与 pack 类型过滤，实际注册逻辑在 core 的 {@link ExtraPackFinder}。
 *
 * <p>{@code value} 保持默认（{@link net.minecraftforge.api.distmarker.Dist#CLIENT} 与
 * {@link net.minecraftforge.api.distmarker.Dist#DEDICATED_SERVER}），因为数据包在两个物理侧都要加载。
 *
 * @see net.minecraft.server.packs.repository.ServerPacksSource#createPackRepository(java.nio.file.Path)
 */
@Mod.EventBusSubscriber(modid = CgccPackLoader.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ForgeDataPackListener {

    private ForgeDataPackListener() {
    }

    /**
     * 只处理数据包仓库，把注册工作交给 core。
     *
     * @param event Forge 在创建包仓库时抛出的事件
     */
    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;

        ExtraPackFinder.onAddPackFinders(event::addRepositorySource);
    }
}
