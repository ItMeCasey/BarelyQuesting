package com.itmecasey.barelyquesting.compat;

import net.minecraft.server.MinecraftServer;

/** Starts the headless self test when this build includes it (-PdevTools) and BARELYQUESTING_SELFTEST is set. */
public final class DevHooks {
    private DevHooks() {
    }

    public static void serverStarted(MinecraftServer server) {
        if (System.getenv("BARELYQUESTING_SELFTEST") == null) {
            return;
        }
        try {
            Class.forName("com.itmecasey.barelyquesting.dev.SelfTest").getMethod("run", MinecraftServer.class).invoke(null, server);
        } catch (ReflectiveOperationException e) {
            System.out.println("SELFTEST FAIL no self test in this build: " + e);
            System.out.println("SELFTEST DONE failures=1");
            server.halt(false);
        }
    }
}
