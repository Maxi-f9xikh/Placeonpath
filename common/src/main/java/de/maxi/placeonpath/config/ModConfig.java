package de.maxi.placeonpath.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@Config(name = "placeonpath")
public class ModConfig implements ConfigData {
    /**
     * Blocks (or #tags) that make a path turn to dirt when placed on it.
     * Empty = every block keeps the path. Examples: minecraft:oak_fence  #minecraft:fences
     */
    public List<String> turnPathToDirt = new ArrayList<>();
}
