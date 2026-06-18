package de.maxi.placeonpath.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class PathRules {
    private PathRules() {}

    /** True if the block above should make the path turn to dirt (vanilla behavior). */
    public static boolean shouldTurnToDirt(BlockState above) {
        var entries = ModConfigHolder.get().turnPathToDirt;
        if (entries.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(above.getBlock());
        for (String raw : entries) {
            String e = raw.trim();
            if (e.isEmpty()) continue;
            if (e.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(e.substring(1));
                if (tag != null && above.is(TagKey.create(Registries.BLOCK, tag))) return true;
            } else if (id.toString().equals(e) || id.getPath().equals(e)) {
                return true;
            }
        }
        return false;
    }
}
