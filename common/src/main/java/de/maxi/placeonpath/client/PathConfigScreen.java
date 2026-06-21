package de.maxi.placeonpath.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.maxi.placeonpath.config.BlockCatalog;
import de.maxi.placeonpath.config.ConfigStore;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PathConfigScreen extends Screen {

    private final Screen parent;
    private final Set<String> original;            // baseline at open, to detect/discard unsaved edits
    private final Set<String> expanded = new HashSet<>();
    private EditBox search;
    private String query = "";

    // layout (set in init)
    private int listLeft, listRight, listTop, listBottom, cellW, cols;
    private double scroll = 0;
    private int contentHeight = 0;

    // colors (ARGB)
    private static final int BG_HEADER   = 0x55000000;
    private static final int GREEN_CELL  = 0xC0245A2C;
    private static final int RED_CELL    = 0xC05A2424;
    private static final int GREEN_HOVER = 0xE02E7A38;
    private static final int RED_HOVER   = 0xE07A2E2E;
    private static final int PILL_GREEN  = 0xFF3BA55A;
    private static final int PILL_RED    = 0xFFC04545;
    private static final int PILL_MIXED  = 0xFFC9A227;
    private static final int TEXT        = 0xFFFFFFFF;
    private static final int SUBTLE      = 0xFFB0B0B0;
    private static final int PILL_TEXT   = 0xFF202020;

    private static final int HEADER_H = 22;
    private static final int CELL_H = 22;

    public PathConfigScreen(Screen parent) {
        super(Component.translatable("placeonpath.config.title"));
        this.parent = parent;
        this.original = new HashSet<>(ConfigStore.entries());
    }

    @Override
    protected void init() {
        int boxW = Math.min(this.width - 40, 220);
        this.search = new EditBox(this.font, this.width / 2 - boxW / 2, 28, boxW, 18,
                Component.translatable("placeonpath.config.search"));
        this.search.setHint(Component.translatable("placeonpath.config.search"));
        this.search.setResponder(s -> { this.query = s.toLowerCase(); this.scroll = 0; });
        addRenderableWidget(this.search);

        int by = this.height - 28;
        addRenderableWidget(Button.builder(Component.translatable("placeonpath.config.save_exit"), b -> saveAndExit())
                .bounds(this.width / 2 - 154, by, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("placeonpath.config.exit"), b -> exitWithoutSave())
                .bounds(this.width / 2 + 4, by, 150, 20).build());

        int listW = Math.min(this.width - 40, 360);
        this.listLeft = this.width / 2 - listW / 2;
        this.listRight = this.listLeft + listW;
        this.listTop = 66;
        this.listBottom = this.height - 36;
        this.cols = Math.max(1, listW / 170);
        this.cellW = listW / this.cols;
    }

    // ---- layout model ----
    private interface LayoutItem {}
    private static final class HeaderItem implements LayoutItem {
        BlockCatalog.Category cat; int y; int visibleCount;
    }
    private static final class CellItem implements LayoutItem {
        BlockCatalog.Entry entry; int x; int y; int w; int h;
    }

    private boolean isExpanded(BlockCatalog.Category cat) {
        return !query.isEmpty() || expanded.contains(cat.id); // searching forces expansion
    }

    private List<BlockCatalog.Entry> filter(BlockCatalog.Category cat) {
        if (query.isEmpty()) return cat.entries;
        List<BlockCatalog.Entry> out = new ArrayList<>();
        for (BlockCatalog.Entry e : cat.entries) {
            if (e.name.toLowerCase().contains(query) || e.id.contains(query)) out.add(e);
        }
        return out;
    }

    private List<LayoutItem> buildLayout() {
        List<LayoutItem> items = new ArrayList<>();
        int y = 0;
        for (BlockCatalog.Category cat : BlockCatalog.categories()) {
            List<BlockCatalog.Entry> visible = filter(cat);
            if (visible.isEmpty()) continue;
            HeaderItem h = new HeaderItem();
            h.cat = cat; h.y = y; h.visibleCount = visible.size();
            items.add(h);
            y += HEADER_H + 2;
            if (isExpanded(cat)) {
                for (int i = 0; i < visible.size(); i++) {
                    CellItem c = new CellItem();
                    c.entry = visible.get(i);
                    c.x = listLeft + (i % cols) * cellW;
                    c.y = y + (i / cols) * CELL_H;
                    c.w = cellW - 3;
                    c.h = CELL_H - 3;
                    items.add(c);
                }
                int rows = (visible.size() + cols - 1) / cols;
                y += rows * CELL_H;
            }
            y += 6;
        }
        contentHeight = y;
        return items;
    }

    private int countBlacklisted(BlockCatalog.Category cat) {
        int n = 0;
        for (BlockCatalog.Entry e : cat.entries) if (ConfigStore.isBlacklisted(e.id)) n++;
        return n;
    }

    private Component pillComponent(BlockCatalog.Category cat) {
        int red = countBlacklisted(cat);
        int total = cat.entries.size();
        String key = red == 0 ? "placeonpath.config.all_on"
                   : red == total ? "placeonpath.config.all_off" : "placeonpath.config.mixed";
        return Component.translatable(key);
    }

    private int pillColor(BlockCatalog.Category cat) {
        int red = countBlacklisted(cat);
        int total = cat.entries.size();
        return red == 0 ? PILL_GREEN : (red == total ? PILL_RED : PILL_MIXED);
    }

    // ---- rendering ----
    @Override
    public void render(PoseStack g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);

        drawCenteredString(g, this.font, this.title, this.width / 2, 12, TEXT);
        drawLegend(g);

        List<LayoutItem> items = buildLayout();
        int maxScroll = Math.max(0, contentHeight - (listBottom - listTop));
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        enableScissor(listLeft - 2, listTop, listRight + 2, listBottom);
        for (LayoutItem item : items) {
            if (item instanceof HeaderItem h) {
                int sy = listTop - (int) scroll + h.y;
                if (sy + HEADER_H < listTop || sy > listBottom) continue;
                renderHeader(g, h, sy);
            } else if (item instanceof CellItem c) {
                int sy = listTop - (int) scroll + c.y;
                if (sy + c.h < listTop || sy > listBottom) continue;
                renderCell(g, c, sy, mouseX, mouseY);
            }
        }
        disableScissor();
    }

    private void drawLegend(PoseStack g) {
        Component keep = Component.translatable("placeonpath.config.legend_keep");
        Component dirt = Component.translatable("placeonpath.config.legend_dirt");
        int gap = 18;
        int kw = 12 + this.font.width(keep);
        int dw = 12 + this.font.width(dirt);
        int x = this.width / 2 - (kw + gap + dw) / 2;
        int y = 50;
        fill(g, x, y, x + 8, y + 8, PILL_GREEN);
        this.font.draw(g, keep, x + 12, y, SUBTLE);
        int x2 = x + kw + gap;
        fill(g, x2, y, x2 + 8, y + 8, PILL_RED);
        this.font.draw(g, dirt, x2 + 12, y, SUBTLE);
    }

    private void renderHeader(PoseStack g, HeaderItem h, int sy) {
        fill(g, listLeft, sy, listRight, sy + HEADER_H, BG_HEADER);
        this.font.draw(g, isExpanded(h.cat) ? "v" : ">", listLeft + 6, sy + 7, SUBTLE);
        Component name = Component.translatable(h.cat.langKey());
        this.font.draw(g, name, listLeft + 20, sy + 7, TEXT);
        this.font.draw(g, "(" + h.visibleCount + ")", listLeft + 20 + this.font.width(name) + 6, sy + 7, SUBTLE);

        Component pill = pillComponent(h.cat);
        int pw = this.font.width(pill) + 12;
        int px = listRight - pw - 6;
        fill(g, px, sy + 4, px + pw, sy + HEADER_H - 4, pillColor(h.cat));
        this.font.draw(g, pill, px + 6, sy + 7, PILL_TEXT);
    }

    private void renderCell(PoseStack g, CellItem c, int sy, int mouseX, int mouseY) {
        boolean red = ConfigStore.isBlacklisted(c.entry.id);
        boolean hover = mouseX >= c.x && mouseX <= c.x + c.w && mouseY >= sy && mouseY <= sy + c.h
                && mouseY >= listTop && mouseY <= listBottom;
        int bg = red ? (hover ? RED_HOVER : RED_CELL) : (hover ? GREEN_HOVER : GREEN_CELL);
        fill(g, c.x, sy, c.x + c.w, sy + c.h, bg);
        this.minecraft.getItemRenderer().renderGuiItem(g, new ItemStack(c.entry.block), c.x + 2, sy + (c.h - 16) / 2);
        int textX = c.x + 22;
        String label = this.font.plainSubstrByWidth(c.entry.name, c.x + c.w - 4 - textX);
        this.font.draw(g, label, textX, sy + (c.h - 8) / 2, TEXT);
    }

    // ---- input ----
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0 || mouseY < listTop || mouseY > listBottom
                || mouseX < listLeft - 2 || mouseX > listRight + 2) return false;

        for (LayoutItem item : buildLayout()) {
            if (item instanceof HeaderItem h) {
                int sy = listTop - (int) scroll + h.y;
                if (mouseY < sy || mouseY > sy + HEADER_H) continue;
                int pw = this.font.width(pillComponent(h.cat)) + 12;
                int px = listRight - pw - 6;
                if (mouseX >= px && mouseX <= px + pw) toggleCategory(h.cat);
                else toggleExpand(h.cat);
                return true;
            } else if (item instanceof CellItem c) {
                int sy = listTop - (int) scroll + c.y;
                if (mouseX >= c.x && mouseX <= c.x + c.w && mouseY >= sy && mouseY <= sy + c.h) {
                    ConfigStore.set(c.entry.id, !ConfigStore.isBlacklisted(c.entry.id));
                    return true;
                }
            }
        }
        return false;
    }

    private void toggleExpand(BlockCatalog.Category cat) {
        if (!query.isEmpty()) return; // expansion forced while searching
        if (!expanded.add(cat.id)) expanded.remove(cat.id);
    }

    private void toggleCategory(BlockCatalog.Category cat) {
        List<String> ids = new ArrayList<>();
        for (BlockCatalog.Entry e : cat.entries) ids.add(e.id);
        boolean allGreen = countBlacklisted(cat) == 0;
        ConfigStore.setAll(ids, allGreen); // all green -> turn category red; otherwise reset to green
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= listTop && mouseY <= listBottom) {
            int maxScroll = Math.max(0, contentHeight - (listBottom - listTop));
            scroll = Math.max(0, Math.min(scroll - delta * 18, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean hasUnsavedChanges() {
        return !original.equals(new HashSet<>(ConfigStore.entries()));
    }

    private void saveAndExit() {
        ConfigStore.save();
        this.minecraft.setScreen(parent);
    }

    private void exitWithoutSave() {
        if (!hasUnsavedChanges()) {
            this.minecraft.setScreen(parent);
            return;
        }
        this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                ConfigStore.replaceAll(original); // discard the unsaved edits
                this.minecraft.setScreen(parent);
            } else {
                this.minecraft.setScreen(this);
            }
        }, Component.translatable("placeonpath.config.discard_title"),
           Component.translatable("placeonpath.config.discard_message")));
    }

    @Override
    public void onClose() {
        exitWithoutSave(); // Esc routes through the same discard guard
    }
}
