package dev.xcolorful.cgccompat.packloader.client;

public class CgccPackLoaderClient {

    protected static boolean initialized;

    public static void init() {
        if (initialized) return;

        initialized = true;
    }
}
