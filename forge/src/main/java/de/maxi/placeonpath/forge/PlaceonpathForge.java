package de.maxi.placeonpath.forge;

import de.maxi.placeonpath.Placeonpath;
import me.shedaniel.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Placeonpath.MOD_ID)
public final class PlaceonpathForge {
    public PlaceonpathForge() {
        EventBuses.registerModEventBus(Placeonpath.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        if (FMLEnvironment.dist == Dist.CLIENT) {
            PlaceonpathForgeClient.registerConfigScreen();
        }
        Placeonpath.init();
    }
}
