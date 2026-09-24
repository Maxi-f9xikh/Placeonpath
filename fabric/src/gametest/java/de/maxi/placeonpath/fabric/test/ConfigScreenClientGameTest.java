package de.maxi.placeonpath.fabric.test;

import de.maxi.placeonpath.client.PathConfigScreen;
import de.maxi.placeonpath.config.ConfigStore;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigScreenClientGameTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger("placeonpath-test");

    @Override
    public void runTest(ClientGameTestContext context) {
        context.waitForScreen(TitleScreen.class);
        check(context, "title");
        try (TestSingleplayerContext sp = context.worldBuilder().create()) {
            sp.getConnection().waitForChunksRender();
            check(context, "world");
        }
    }

    private void check(ClientGameTestContext context, String where) {
        context.setScreen(() -> new PathConfigScreen(null));
        context.waitTicks(5);
        context.takeScreenshot("pop-" + where + "-1-open");
        LOG.info("POPTEST {}: item components bound = {}", where,
                context.computeOnClient(mc -> net.minecraft.world.item.Items.STONE.builtInRegistryHolder().areComponentsBound()));

        // GUI -> window pixel scale, and the list geometry PathConfigScreen.init() uses
        double scale = context.computeOnClient(mc -> (double) mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth());
        int guiW = context.computeOnClient(mc -> mc.getWindow().getGuiScaledWidth());
        int listW = Math.min(guiW - 40, 360);
        int listLeft = guiW / 2 - listW / 2;

        // click the first category header (not its pill) -> expands it
        context.getInput().setCursorPos((listLeft + 40) * scale, (66 + 11) * scale);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(3);
        context.takeScreenshot("pop-" + where + "-2-expanded");

        // click the first cell -> toggles its block
        int before = context.computeOnClient(mc -> ConfigStore.entries().size());
        context.getInput().setCursorPos((listLeft + 40) * scale, (90 + 9) * scale);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(3);
        int after = context.computeOnClient(mc -> ConfigStore.entries().size());
        context.takeScreenshot("pop-" + where + "-3-toggled");
        LOG.info("POPTEST {}: scale={} guiW={} blacklist {} -> {} => {}", where, scale, guiW, before, after,
                before != after ? "CLICK OK" : "CLICK FAILED");

        context.setScreen(() -> null);
        context.waitTicks(2);
    }
}
