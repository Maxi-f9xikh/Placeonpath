package de.maxi.placeonpath.config;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

public final class PathRules {
    private PathRules() {}

    /** True if the block above should make the path turn to dirt (vanilla behavior). */
    public static boolean shouldTurnToDirt(BlockState above) {
        Set<String> entries = ConfigStore.entries();
        if (entries.isEmpty()) return false;
        ResourceLocation id = Registry.BLOCK.getKey(above.getBlock());
        for (String raw : entries) {
            String e = raw.trim();
            if (e.isEmpty()) continue;
            // 1.16.5 has no TagKey API; the config screen only writes block ids, so #tag entries are ignored here.
            if (e.startsWith("#")) continue;
            if (id.toString().equals(e) || id.getPath().equals(e)) {
                return true;
            }
        }
        return false;
    }
}
