package dev.xcolorful.cgccompat.packloader.forge;

import dev.xcolorful.cgccompat.packloader.CgccPackLoader;
import dev.xcolorful.cgccompat.packloader.forgeclient.CgccPackLoaderForgeClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod(CgccPackLoader.MOD_ID)
public class CgccPackLoaderForge {

    public CgccPackLoaderForge() {
        CgccPackLoader.init();

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
