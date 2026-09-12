package dev.xcolorful.cgccompat.packloader.forgeclient;

import dev.xcolorful.cgccompat.packloader.client.CgccPackLoaderClient;

public class CgccPackLoaderForgeClient {

    protected static boolean initialized;

    public static void init() {
        if (initialized) return;

        CgccPackLoaderClient.init();

        initialized = true;
    }
}
