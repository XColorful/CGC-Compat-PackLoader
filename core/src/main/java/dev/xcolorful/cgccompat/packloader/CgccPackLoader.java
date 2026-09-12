package dev.xcolorful.cgccompat.packloader;

import com.mojang.logging.LogUtils;
import java.nio.file.Path;
import org.slf4j.Logger;

/**
 * 模组主类，只保存平台侧传入的路径，不承担任何平台相关逻辑。
 */
public class CgccPackLoader {
    public static final String MOD_ID = "cgccpackloader";
    public static final Logger LOGGER = LogUtils.getLogger();

    protected static boolean initialized;

    /** 游戏根目录，用于解析配置中的相对路径。 */
    private static Path gameDirectory;

    /** 模组配置文件的绝对路径。 */
    private static Path configFile;

    /**
     * 记录平台侧提供的路径。重复调用会被忽略。
     *
     * @param gameDirectory 游戏根目录，配置里的相对路径以它为基准解析
     * @param configFile 模组配置文件的绝对路径
     */
    public static void init(Path gameDirectory, Path configFile) {
        if (initialized) return;

        initialized = true;
        CgccPackLoader.gameDirectory = gameDirectory;
        CgccPackLoader.configFile = configFile;
    }

    /**
     * @return 游戏根目录
     */
    public static Path gameDirectory() {
        return gameDirectory;
    }

    /**
     * @return 模组配置文件的绝对路径
     */
    public static Path configFile() {
        return configFile;
    }
}
