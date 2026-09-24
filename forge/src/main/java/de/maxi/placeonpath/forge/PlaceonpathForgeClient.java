package de.maxi.placeonpath.forge;

import de.maxi.placeonpath.client.PathConfigScreen;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;

/** Client-only: wires the PlaceOnPath config screen to the Forge mods-list config button. */
public final class PlaceonpathForgeClient {
    private PlaceonpathForgeClient() {}

    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY,
                () -> (mc, parent) -> new PathConfigScreen(parent));
    }
}
