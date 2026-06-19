package de.maxi.placeonpath.neoforge;

import de.maxi.placeonpath.client.PathConfigScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only: wires the PlaceOnPath config screen to the NeoForge mods-list config button. */
public final class PlaceonpathNeoForgeClient {
    private PlaceonpathNeoForgeClient() {}

    public static void registerConfigScreen(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (c, parent) -> new PathConfigScreen(parent));
    }
}
