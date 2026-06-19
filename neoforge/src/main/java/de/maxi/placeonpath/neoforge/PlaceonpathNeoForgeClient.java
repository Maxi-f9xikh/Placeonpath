package de.maxi.placeonpath.neoforge;

import de.maxi.placeonpath.client.PathConfigScreen;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.ConfigScreenHandler;

/** Client-only: wires the PlaceOnPath config screen to the NeoForge mods-list config button. */
public final class PlaceonpathNeoForgeClient {
    private PlaceonpathNeoForgeClient() {}

    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new PathConfigScreen(parent)));
    }
}
