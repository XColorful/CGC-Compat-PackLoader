package dev.xcolorful.cgccompat.packloader.forgeclient;

import dev.xcolorful.cgccompat.packloader.client.CgccPackLoaderClient;

/**
 * 客户端初始化入口。
 *
 * <p>资源包的事件监听由
 * {@code dev.xcolorful.cgccompat.packloader.forgeclient.resource.ForgeClientResourcePackListener}
 * 通过注解自动注册，这里只负责驱动 core 的客户端初始化。
 */
public class CgccPackLoaderForgeClient {

    protected static boolean initialized;

    public static void init() {
        if (initialized) return;

        CgccPackLoaderClient.init();

        initialized = true;
    }
}
