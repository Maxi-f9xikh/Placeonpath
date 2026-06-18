package de.maxi.placeonpath;

import de.maxi.placeonpath.config.ModConfigHolder;
import de.maxi.placeonpath.registry.ModBlocks;

public final class Placeonpath {
    public static final String MOD_ID = "placeonpath";

    public static void init() {
        ModConfigHolder.register();
        ModBlocks.init();
    }
}
