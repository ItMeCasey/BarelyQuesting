package com.itmecasey.barelyquesting.core.script;

import java.nio.file.Path;

/** Installs the optional script bridges. Called once from each loader's mod constructor (the mod loads before ModJS and KubeJS). */
public final class Bridges {
    /** The single API object shared by every bridge. */
    public static final QuestApi API = new QuestApi();

    private Bridges() {
    }

    public static void install(Path gameDir) {
        ModJsBridge.install(API);
        KubeJsBridge.install(gameDir);
    }
}
