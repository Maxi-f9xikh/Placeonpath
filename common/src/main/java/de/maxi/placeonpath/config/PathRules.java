package de.maxi.placeonpath.config;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;

public final class PathRules {
    private PathRules() {}

    /** True if the block above should make the path turn to dirt (vanilla behavior). */
    public static boolean shouldTurnToDirt(BlockState above) {
        var entries = ConfigStore.entries();
        if (entries.isEmpty()) return false;
        ResourceLocation id = Registry.BLOCK.getKey(above.getBlock());
        for (String raw : entries) {
            String e = raw.trim();
            if (e.isEmpty()) continue;
            if (e.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(e.substring(1));
                if (tag != null && above.is(TagKey.create(Registry.BLOCK_REGISTRY, tag))) return true;
            } else if (id.toString().equals(e) || id.getPath().equals(e)) {
                return true;
            }
        }
        return false;
    }
}
