package dev.xcolorful.cgccompat.packloader.neoforge.resource;

import dev.xcolorful.cgccompat.packloader.CgccPackLoader;
import dev.xcolorful.cgccompat.packloader.core.resource.ExtraPackFinder;
import net.minecraft.server.packs.PackType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * 数据包侧的平台监听。只做事件订阅与 pack 类型过滤，实际注册逻辑在 core 的 {@link ExtraPackFinder}。
 *
 * <p>{@code value} 保持默认（{@link net.neoforged.api.distmarker.Dist#CLIENT} 与
 * {@link net.neoforged.api.distmarker.Dist#DEDICATED_SERVER}），因为数据包在两个物理侧都要加载。
 *
 * @see net.minecraft.server.packs.repository.ServerPacksSource#createPackRepository(java.nio.file.Path, net.minecraft.world.level.validation.DirectoryValidator)
 */
@EventBusSubscriber(modid = CgccPackLoader.MOD_ID)
public final class NeoForgeDataPackListener {

    private NeoForgeDataPackListener() {
    }

    /**
     * 只处理数据包仓库，把注册工作交给 core。
     *
     * @param event NeoForge 在创建包仓库时抛出的事件
     */
    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;

        ExtraPackFinder.onAddPackFinders(event::addRepositorySource);
    }
}
