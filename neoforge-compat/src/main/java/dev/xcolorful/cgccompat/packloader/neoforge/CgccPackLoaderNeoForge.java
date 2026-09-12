package dev.xcolorful.cgccompat.packloader.neoforge;

import dev.xcolorful.cgccompat.packloader.CgccPackLoader;
import dev.xcolorful.cgccompat.packloader.neoforge.resource.NeoForgeDataPackListener;
import dev.xcolorful.cgccompat.packloader.neoforgeclient.CgccPackLoaderNeoForgeClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

/**
 * 模组主类。只负责初始化并把平台路径交给 core。
 *
 * <p>事件监听不在这里手动注册：{@link NeoForgeDataPackListener} 与客户端侧的监听类都通过
 * {@code @Mod.EventBusSubscriber} 自动注册。
 */
@Mod(CgccPackLoader.MOD_ID)
public class CgccPackLoaderNeoForge {

    public CgccPackLoaderNeoForge() {
        CgccPackLoader.init(FMLPaths.GAMEDIR.get(),
                FMLPaths.CONFIGDIR.get());

        Dist dist = FMLLoader.getCurrent().getDist();

        if (dist.isClient()) {
            _CgccPackLoaderNeoForgeClient.init();
        }
    }

    private static class _CgccPackLoaderNeoForgeClient {
        public static void init() {
            CgccPackLoaderNeoForgeClient.init();
        }
    }
}
