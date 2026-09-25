package dev.xcolorful.cgccompat.packloader;

import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * 模组主类，只保存平台侧传入的路径，不承担任何平台相关逻辑。
 */
public class CgccPackLoader {
    public static final String MOD_ID = "cgccpackloader";
    public static final Logger LOGGER = LogUtils.getLogger();

    protected static boolean initialized;

    /**
     * 合成元数据使用的包格式
     * 不固定使用 1.20.1 的 15，不然高版本每装一个包都要额外点一次提醒
     */
    @SuppressWarnings("deprecation")
    public static final int DATA_PACK_FORMAT = SharedConstants.DATA_PACK_FORMAT;

    @SuppressWarnings("deprecation")
    public static final int RESOURCE_PACK_FORMAT = SharedConstants.RESOURCE_PACK_FORMAT;

    /** 配置文件名，位于平台的 config 目录下。 */
    public static final String FILE_NAME = MOD_ID + ".json";

    /** 客户端文件名，与 {@link #FILE_NAME} 同目录，记录已经默认勾选过的资源包。 */
    public static final String CLIENT_FILE_NAME = MOD_ID + "-client.json";

    /** 游戏根目录，用于解析配置中的相对路径。 */
    private static Path gameDirectory;

    /** 模组配置文件的绝对路径。 */
    private static Path configFile;

    /** 客户端记录文件的绝对路径。 */
    private static Path clientFile;

    /**
     * 记录平台侧提供的路径。重复调用会被忽略。
     *
     * @param gameDirectory 游戏根目录，配置里的相对路径以它为基准解析
     * @param configDirectory 模组配置文件目录
     */
    public static void init(Path gameDirectory,
                            Path configDirectory) {
        if (initialized) return;

        CgccPackLoader.gameDirectory = gameDirectory;
        CgccPackLoader.configFile = configDirectory.resolve(FILE_NAME);
        CgccPackLoader.clientFile = configDirectory.resolve(CLIENT_FILE_NAME);

        initialized = true;
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

    /**
     * @return 客户端记录文件的绝对路径
     */
    public static Path clientFile() {
        return clientFile;
    }
}
