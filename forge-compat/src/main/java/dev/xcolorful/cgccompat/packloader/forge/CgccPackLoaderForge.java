package dev.xcolorful.cgccompat.packloader.forge;

import dev.xcolorful.cgccompat.packloader.CgccPackLoader;
import dev.xcolorful.cgccompat.packloader.core.resource.ExtraPackConfig;
import dev.xcolorful.cgccompat.packloader.forge.resource.ForgeDataPackListener;
import dev.xcolorful.cgccompat.packloader.forgeclient.CgccPackLoaderForgeClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * 模组主类。只负责初始化并把平台路径交给 core。
 *
 * <p>事件监听不在这里手动注册：{@link ForgeDataPackListener} 与客户端侧的监听类都通过
 * {@code @Mod.EventBusSubscriber} 自动注册。
 */
@Mod(CgccPackLoader.MOD_ID)
public class CgccPackLoaderForge {

    public CgccPackLoaderForge() {
        CgccPackLoader.init(FMLPaths.GAMEDIR.get(), FMLPaths.CONFIGDIR.get().resolve(ExtraPackConfig.FILE_NAME));

        Dist dist = FMLLoader.getDist();

        if (dist.isClient()) {
            _CgccPackLoaderForgeClient.init();
        }
    }

    private static class _CgccPackLoaderForgeClient {
        public static void init() {
            CgccPackLoaderForgeClient.init();
        }
    }
}
