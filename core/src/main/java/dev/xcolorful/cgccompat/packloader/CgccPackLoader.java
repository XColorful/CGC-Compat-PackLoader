package dev.xcolorful.cgccompat.packloader;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class CgccPackLoader {
    public static final String MOD_ID = "cgccpackloader";
    public static final Logger LOGGER = LogUtils.getLogger();

    protected static boolean initialized;

    public static void init() {
        if (initialized) return;

        initialized = true;
    }
}
