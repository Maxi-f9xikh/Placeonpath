package de.maxi.placeonpath.neoforge;

import de.maxi.placeonpath.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only: wires the Cloth config screen to the NeoForge mods-list config button. */
public final class PlaceonpathNeoForgeClient {
    private PlaceonpathNeoForgeClient() {}

    public static void registerConfigScreen(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (c, parent) -> AutoConfig.getConfigScreen(ModConfig.class, parent).get());
    }
}
