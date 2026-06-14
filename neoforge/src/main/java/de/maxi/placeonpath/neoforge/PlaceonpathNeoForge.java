package de.maxi.placeonpath.neoforge;

import de.maxi.placeonpath.Placeonpath;
import net.neoforged.fml.common.Mod;

@Mod(Placeonpath.MOD_ID)
public final class PlaceonpathNeoForge {
    public PlaceonpathNeoForge() {
        // Run our common setup.
        Placeonpath.init();
    }
}
