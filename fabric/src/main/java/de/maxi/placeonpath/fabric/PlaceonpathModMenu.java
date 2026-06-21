package de.maxi.placeonpath.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import de.maxi.placeonpath.client.PathConfigScreen;

public class PlaceonpathModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PathConfigScreen::new;
    }
}
