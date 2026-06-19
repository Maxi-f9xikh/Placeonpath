package de.maxi.placeonpath.forge;

import de.maxi.placeonpath.Placeonpath;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Placeonpath.MOD_ID)
public final class PlaceonpathForge {
    public PlaceonpathForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(Placeonpath.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // The config screen is client-only; isolate it so the server never loads GUI classes.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            PlaceonpathForgeClient.registerConfigScreen();
        }

        // Run our common setup.
        Placeonpath.init();
    }
}
