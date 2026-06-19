package de.maxi.placeonpath.neoforge;

import de.maxi.placeonpath.Placeonpath;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Placeonpath.MOD_ID)
public final class PlaceonpathNeoForge {
    public PlaceonpathNeoForge(ModContainer container) {
        // The config screen is client-only; isolate it so the server never loads GUI classes.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            PlaceonpathNeoForgeClient.registerConfigScreen(container);
        }
        Placeonpath.init();
    }
}
