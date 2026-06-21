package de.maxi.placeonpath.config;

import net.minecraft.core.Registry;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Groups every placeable block into ordered, named categories for the config screen. */
public final class BlockCatalog {
    private BlockCatalog() {}

    /** One block in the catalog: its registry id, the block (for the icon), and the display name. */
    public static final class Entry {
        public final String id;
        public final Block block;
        public final String name;
        Entry(String id, Block block, String name) { this.id = id; this.block = block; this.name = name; }
    }

    /** A named, ordered group of blocks. */
    public static final class Category {
        public final String id;
        public final List<Entry> entries = new ArrayList<>();
        Category(String id) { this.id = id; }
        public String langKey() { return "placeonpath.category." + id; }
    }

    // Display order; "other" is the catch-all and must be last.
    private static final String[] ORDER = {
        "fences", "walls", "plates_buttons", "signs_banners", "slabs", "stairs",
        "doors", "carpets", "lights", "plants", "wood", "stone", "nature", "other"
    };

    private static List<Category> cache;

    public static List<Category> categories() {
        if (cache != null) return cache;
        Map<String, Category> byId = new LinkedHashMap<>();
        for (String id : ORDER) byId.put(id, new Category(id));

        for (Block block : Registry.BLOCK) {
            if (block.asItem() == Items.AIR) continue; // not placeable / technical (e.g. our full-path block)
            String id = Registry.BLOCK.getKey(block).toString();
            String name = block.getName().getString();
            byId.get(categorize(block, id)).entries.add(new Entry(id, block, name));
        }

        List<Category> result = new ArrayList<>();
        for (String id : ORDER) {
            Category c = byId.get(id);
            if (c.entries.isEmpty()) continue;
            c.entries.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
            result.add(c);
        }
        cache = result;
        return result;
    }

    private static String categorize(Block block, String fullId) {
        String p = fullId.contains(":") ? fullId.substring(fullId.indexOf(':') + 1) : fullId;
        if (p.endsWith("_fence") || p.endsWith("_fence_gate")) return "fences";
        if (p.endsWith("_wall")) return "walls";
        if (p.endsWith("_pressure_plate") || p.endsWith("_button")) return "plates_buttons";
        if (p.endsWith("_sign") || p.endsWith("_banner")) return "signs_banners";
        if (p.endsWith("_slab")) return "slabs";
        if (p.endsWith("_stairs")) return "stairs";
        if (p.endsWith("_trapdoor") || p.endsWith("_door")) return "doors";
        if (p.endsWith("_carpet")) return "carpets";
        if (p.contains("torch") || p.endsWith("lantern") || p.endsWith("candle")
                || p.contains("campfire") || p.equals("end_rod")) return "lights";
        if (block.defaultBlockState().is(BlockTags.FLOWERS) || block.defaultBlockState().is(BlockTags.SAPLINGS)
                || p.endsWith("_sapling") || p.contains("mushroom") || p.endsWith("_fern")
                || p.endsWith("sprouts") || p.endsWith("roots") || p.contains("flower")) return "plants";
        if (p.endsWith("_planks") || p.endsWith("_log") || p.endsWith("_wood")
                || p.endsWith("_stem") || p.endsWith("_hyphae")) return "wood";
        if (p.contains("stone") || p.contains("brick") || p.contains("cobble")
                || p.contains("deepslate") || p.contains("granite") || p.contains("diorite")
                || p.contains("andesite") || p.contains("blackstone") || p.contains("tuff")
                || p.contains("calcite") || p.contains("basalt") || p.contains("sandstone")) return "stone";
        if (p.endsWith("_leaves") || p.equals("dirt") || p.equals("coarse_dirt") || p.equals("rooted_dirt")
                || p.equals("sand") || p.equals("red_sand") || p.equals("gravel") || p.equals("grass_block")
                || p.equals("podzol") || p.equals("mycelium") || p.equals("mud") || p.equals("clay")
                || p.equals("snow_block") || p.equals("ice") || p.equals("packed_ice") || p.equals("blue_ice")
                || p.equals("moss_block")) return "nature";
        return "other";
    }
}
