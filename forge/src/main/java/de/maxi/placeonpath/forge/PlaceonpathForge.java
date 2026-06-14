package de.maxi.placeonpath.forge;

import de.maxi.placeonpath.Placeonpath;
import me.shedaniel.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Placeonpath.MOD_ID)
public final class PlaceonpathForge {
    public PlaceonpathForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(Placeonpath.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // Run our common setup.
        Placeonpath.init();
    }
}
