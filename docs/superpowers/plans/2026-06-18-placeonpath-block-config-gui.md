# PlaceOnPath Custom Config GUI — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Supersedes** `2026-06-18-placeonpath-block-config.md` (the Cloth-Config text-list version). The gameplay mixins (`BlockItemMixin`, `DirtPathBlockMixin`) and the `PathRules.shouldTurnToDirt(BlockState)` contract are **unchanged and kept**; only the config persistence and the config screen are replaced.

**Goal:** Replace the Cloth/AutoConfig text-list config with a hand-rolled GUI screen that lists every *placeable* block in expandable categories, each block green (keeps the path, default) or red (turns the path to dirt), toggled by clicking; plus a plain gson JSON config. Then port to all 8 version branches.

**Architecture:** A `ConfigStore` (gson, `config/placeonpath.json`) holds the set of "red" block ids. `BlockCatalog` buckets placeable blocks into ordered categories by id-name. `PathConfigScreen` (a vanilla `Screen`, in `common`) renders the categorized green/red grid with search + per-category toggle and saves on close. `PathRules` reads `ConfigStore`. Thin per-loader entrypoints open the screen (ModMenu on Fabric, the config-screen extension point on Forge/NeoForge). **Cloth Config is removed entirely.**

**Tech Stack:** Java, Architectury (`dev.architectury.platform.Platform` for the config dir), SpongePowered Mixin (unchanged), gson (bundled with Minecraft), Mod Menu (`com.terraformersmc`, Fabric only, optional). Vanilla client GUI (`GuiGraphics` on 1.20+, `PoseStack`+`ItemRenderer` on ≤1.19.4).

**Verification model:** This project has **no unit-test harness** (Minecraft mod; logic needs the game runtime). Per the existing project workflow, each task is verified by `build` + a `:fabric:runClient` smoke test + an in-game check — not by unit tests. Do not add a JUnit harness; follow the project's established pattern.

**Build invocation (this machine):** the bracketed path breaks `gradlew`, so call the wrapper main directly. JDK 21 for 1.21.x:
```bash
JAVA_HOME=/c/Users/Maxi/.jdks/temurin-21.0.7 \
"$JAVA_HOME/bin/java.exe" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain <task>
```
JDK 17 for 1.18.2–1.20.4; gradle 7.6.4 + JDK 17 for 1.16.5. See memory `build-environment`.

**Commits:** author **Maxi**, **NO Claude co-author** (hard user requirement — do not add a `Co-Authored-By` trailer). One commit per task; the feature may also be squashed per branch.

---

## File structure (master / 1.21.1)

Create:
- `common/src/main/java/de/maxi/placeonpath/config/ConfigStore.java` — gson load/save of the blacklist set (thread-safe).
- `common/src/main/java/de/maxi/placeonpath/config/BlockCatalog.java` — placeable blocks bucketed into ordered categories.
- `common/src/main/java/de/maxi/placeonpath/client/PathConfigScreen.java` — the GUI screen.
- `common/src/main/resources/assets/placeonpath/lang/de_de.json` — German strings.

Modify:
- `common/src/main/java/de/maxi/placeonpath/config/PathRules.java` — read `ConfigStore` instead of `ModConfigHolder`.
- `common/src/main/java/de/maxi/placeonpath/Placeonpath.java` — call `ConfigStore.load()` instead of `ModConfigHolder.register()`.
- `common/src/main/resources/assets/placeonpath/lang/en_us.json` — replace autoconfig keys with screen/category keys.
- `fabric/src/main/java/de/maxi/placeonpath/fabric/PlaceonpathModMenu.java` — return `PathConfigScreen`.
- `neoforge/.../PlaceonpathNeoForgeClient.java` — return `PathConfigScreen`.
- `build.gradle` (root) — remove the Cloth maven repo (keep Mod Menu).
- `common/build.gradle`, `fabric/build.gradle`, `neoforge/build.gradle` — remove Cloth deps.
- `fabric/src/main/resources/fabric.mod.json` — remove `cloth-config` depend.
- `neoforge/src/main/resources/META-INF/neoforge.mods.toml` — remove the `cloth_config` dependency block.

Delete:
- `common/src/main/java/de/maxi/placeonpath/config/ModConfig.java`
- `common/src/main/java/de/maxi/placeonpath/config/ModConfigHolder.java`

The mixins (`BlockItemMixin`, `DirtPathBlockMixin`) and `ModBlocks` / `PathFullBlock` are untouched.

---

## Task 1: Swap Cloth → gson config (ConfigStore) and remove Cloth

**Files:**
- Create: `common/src/main/java/de/maxi/placeonpath/config/ConfigStore.java`
- Modify: `common/src/main/java/de/maxi/placeonpath/config/PathRules.java`
- Modify: `common/src/main/java/de/maxi/placeonpath/Placeonpath.java`
- Delete: `ModConfig.java`, `ModConfigHolder.java`
- Modify: `build.gradle`, `common/build.gradle`, `fabric/build.gradle`, `neoforge/build.gradle`
- Modify: `fabric/src/main/resources/fabric.mod.json`, `neoforge/src/main/resources/META-INF/neoforge.mods.toml`

- [ ] **Step 1: Create `ConfigStore.java`** (thread-safe so the integrated-server placement thread can read while the client edits):

```java
package de.maxi.placeonpath.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/** Plain JSON config: the set of block ids (and optional #tags) that turn a path to dirt. */
public final class ConfigStore {
    private ConfigStore() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // CopyOnWriteArraySet: ordered, and safe to iterate (PathRules, server thread) while edited (screen).
    private static final Set<String> TURN_TO_DIRT = new CopyOnWriteArraySet<>();
    private static Path file;

    /** On-disk JSON shape. */
    private static final class Data {
        List<String> turnPathToDirt = new ArrayList<>();
    }

    public static void load() {
        file = Platform.getConfigFolder().resolve("placeonpath.json");
        TURN_TO_DIRT.clear();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                Data data = GSON.fromJson(r, Data.class);
                if (data != null && data.turnPathToDirt != null) {
                    TURN_TO_DIRT.addAll(data.turnPathToDirt);
                }
            } catch (Exception ignored) {
                // Corrupt/old file: start empty rather than crash.
            }
        }
    }

    public static void save() {
        if (file == null) load();
        Data data = new Data();
        data.turnPathToDirt = new ArrayList<>(TURN_TO_DIRT);
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(data, w);
            }
        } catch (IOException ignored) {
            // Best effort.
        }
    }

    public static Set<String> entries() { return TURN_TO_DIRT; }

    public static boolean isBlacklisted(String id) { return TURN_TO_DIRT.contains(id); }

    public static void set(String id, boolean blacklisted) {
        if (blacklisted) TURN_TO_DIRT.add(id);
        else TURN_TO_DIRT.remove(id);
    }

    public static void setAll(Collection<String> ids, boolean blacklisted) {
        if (blacklisted) TURN_TO_DIRT.addAll(ids);
        else TURN_TO_DIRT.removeAll(ids);
    }
}
```

- [ ] **Step 2: Update `PathRules.java`** — read `ConfigStore.entries()` (everything else identical):

```java
package de.maxi.placeonpath.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;

public final class PathRules {
    private PathRules() {}

    /** True if the block above should make the path turn to dirt (vanilla behavior). */
    public static boolean shouldTurnToDirt(BlockState above) {
        var entries = ConfigStore.entries();
        if (entries.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(above.getBlock());
        for (String raw : entries) {
            String e = raw.trim();
            if (e.isEmpty()) continue;
            if (e.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(e.substring(1));
                if (tag != null && above.is(TagKey.create(Registries.BLOCK, tag))) return true;
            } else if (id.toString().equals(e) || id.getPath().equals(e)) {
                return true;
            }
        }
        return false;
    }
}
```

(Removed the unused `Block` import that the old file had.)

- [ ] **Step 3: Update `Placeonpath.java`** — load config on init:

```java
package de.maxi.placeonpath;

import de.maxi.placeonpath.config.ConfigStore;
import de.maxi.placeonpath.registry.ModBlocks;

public final class Placeonpath {
    public static final String MOD_ID = "placeonpath";

    public static void init() {
        ConfigStore.load();
        ModBlocks.init();
    }
}
```

- [ ] **Step 4: Delete the Cloth config classes**

```bash
git rm common/src/main/java/de/maxi/placeonpath/config/ModConfig.java \
       common/src/main/java/de/maxi/placeonpath/config/ModConfigHolder.java
```

- [ ] **Step 5: Remove Cloth from the build files.**

`build.gradle` (root) — delete the Cloth repo line, keep Mod Menu:
```gradle
    repositories {
        maven { url "https://maven.terraformersmc.com/releases/" }  // Mod Menu
    }
```
`common/build.gradle` — delete the line `modImplementation "me.shedaniel.cloth:cloth-config-fabric:15.0.140"`.
`fabric/build.gradle` — delete the line `modImplementation "me.shedaniel.cloth:cloth-config-fabric:15.0.140"` (keep the `modmenu` line).
`neoforge/build.gradle` — delete the line `modImplementation "me.shedaniel.cloth:cloth-config-neoforge:15.0.140"`.

- [ ] **Step 6: Remove the `cloth-config` mod-dependency declarations.**

`fabric/src/main/resources/fabric.mod.json` — delete `"cloth-config": "*"` from `"depends"` (keep the trailing entries valid JSON; `fabric-api` becomes the last depends key):
```json
  "depends": {
    "fabricloader": ">=0.19.3",
    "minecraft": "~1.21.1",
    "java": ">=21",
    "architectury": ">=13.0.8",
    "fabric-api": "*"
  },
```
`neoforge/src/main/resources/META-INF/neoforge.mods.toml` — delete the whole block:
```toml
[[dependencies.placeonpath]]
modId = "cloth_config"
type = "required"
versionRange = "[15,)"
ordering = "AFTER"
side = "BOTH"
```

- [ ] **Step 7: Build common** (it must compile without Cloth; the screen/catalog come next so PathRules + ConfigStore are what's exercised here):

Run: `... GradleWrapperMain :common:build` (JDK21)
Expected: BUILD SUCCESSFUL. (The fabric/neoforge ModMenu + NeoForge classes still reference `ModConfig`/`AutoConfig` at this point — that's fixed in Task 4. If you build `:fabric`/`:neoforge` now they will fail to compile; build only `:common` in this task.)

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "Replace Cloth config with a plain gson ConfigStore"
```

---

## Task 2: BlockCatalog (categorize placeable blocks)

**Files:**
- Create: `common/src/main/java/de/maxi/placeonpath/config/BlockCatalog.java`

- [ ] **Step 1: Create `BlockCatalog.java`.** Categorization is by registry-id name (stable across versions) plus two tag checks for plants:

```java
package de.maxi.placeonpath.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Groups every placeable block into ordered, named categories for the config screen. */
public final class BlockCatalog {
    private BlockCatalog() {}

    /** One block in the catalog: its registry id, the block (for the icon), and the display name. */
    public static final class Entry {
        public final String id;
        public final Block block;
        public final String name;
        Entry(String id, Block block, String name) { this.id = id; this.block = block; this.name = name; }
    }

    /** A named, ordered group of blocks. */
    public static final class Category {
        public final String id;
        public final List<Entry> entries = new ArrayList<>();
        Category(String id) { this.id = id; }
        public String langKey() { return "placeonpath.category." + id; }
    }

    // Display order; "other" is the catch-all and must be last.
    private static final String[] ORDER = {
        "fences", "walls", "plates_buttons", "signs_banners", "slabs", "stairs",
        "doors", "carpets", "lights", "plants", "wood", "stone", "nature", "other"
    };

    private static List<Category> cache;

    public static List<Category> categories() {
        if (cache != null) return cache;
        Map<String, Category> byId = new LinkedHashMap<>();
        for (String id : ORDER) byId.put(id, new Category(id));

        for (Block block : BuiltInRegistries.BLOCK) {
            if (block.asItem() == Items.AIR) continue; // not placeable / technical (e.g. our full-path block)
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            String name = block.getName().getString();
            byId.get(categorize(block, id)).entries.add(new Entry(id, block, name));
        }

        List<Category> result = new ArrayList<>();
        for (String id : ORDER) {
            Category c = byId.get(id);
            if (c.entries.isEmpty()) continue;
            c.entries.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
            result.add(c);
        }
        cache = result;
        return result;
    }

    private static String categorize(Block block, String fullId) {
        String p = fullId.contains(":") ? fullId.substring(fullId.indexOf(':') + 1) : fullId;
        if (p.endsWith("_fence") || p.endsWith("_fence_gate")) return "fences";
        if (p.endsWith("_wall")) return "walls";
        if (p.endsWith("_pressure_plate") || p.endsWith("_button")) return "plates_buttons";
        if (p.endsWith("_sign") || p.endsWith("_banner")) return "signs_banners";
        if (p.endsWith("_slab")) return "slabs";
        if (p.endsWith("_stairs")) return "stairs";
        if (p.endsWith("_trapdoor") || p.endsWith("_door")) return "doors";
        if (p.endsWith("_carpet")) return "carpets";
        if (p.contains("torch") || p.endsWith("lantern") || p.endsWith("candle")
                || p.contains("campfire") || p.equals("end_rod")) return "lights";
        if (block.defaultBlockState().is(BlockTags.FLOWERS) || block.defaultBlockState().is(BlockTags.SAPLINGS)
                || p.endsWith("_sapling") || p.contains("mushroom") || p.endsWith("_fern")
                || p.endsWith("sprouts") || p.endsWith("roots") || p.contains("flower")) return "plants";
        if (p.endsWith("_planks") || p.endsWith("_log") || p.endsWith("_wood")
                || p.endsWith("_stem") || p.endsWith("_hyphae")) return "wood";
        if (p.contains("stone") || p.contains("brick") || p.contains("cobble")
                || p.contains("deepslate") || p.contains("granite") || p.contains("diorite")
                || p.contains("andesite") || p.contains("blackstone") || p.contains("tuff")
                || p.contains("calcite") || p.contains("basalt") || p.contains("sandstone")) return "stone";
        if (p.endsWith("_leaves") || p.equals("dirt") || p.equals("coarse_dirt") || p.equals("rooted_dirt")
                || p.equals("sand") || p.equals("red_sand") || p.equals("gravel") || p.equals("grass_block")
                || p.equals("podzol") || p.equals("mycelium") || p.equals("mud") || p.equals("clay")
                || p.equals("snow_block") || p.equals("ice") || p.equals("packed_ice") || p.equals("blue_ice")
                || p.equals("moss_block")) return "nature";
        return "other";
    }
}
```

- [ ] **Step 2: Build common**

Run: `... GradleWrapperMain :common:build` (JDK21)
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add common/src/main/java/de/maxi/placeonpath/config/BlockCatalog.java
git commit -m "Add BlockCatalog (placeable blocks by category)"
```

---

## Task 3: PathConfigScreen (the GUI)

**Files:**
- Create: `common/src/main/java/de/maxi/placeonpath/client/PathConfigScreen.java`

- [ ] **Step 1: Create `PathConfigScreen.java`** (1.21.1 `GuiGraphics` API). Layout is computed once per render into content-space items and reused for hit-testing:

```java
package de.maxi.placeonpath.client;

import de.maxi.placeonpath.config.BlockCatalog;
import de.maxi.placeonpath.config.ConfigStore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PathConfigScreen extends Screen {

    private final Screen parent;
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
    }

    @Override
    protected void init() {
        int boxW = Math.min(this.width - 40, 220);
        this.search = new EditBox(this.font, this.width / 2 - boxW / 2, 28, boxW, 18,
                Component.translatable("placeonpath.config.search"));
        this.search.setHint(Component.translatable("placeonpath.config.search"));
        this.search.setResponder(s -> { this.query = s.toLowerCase(); this.scroll = 0; });
        addRenderableWidget(this.search);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 75, this.height - 28, 150, 20).build());

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
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        g.drawCenteredString(this.font, this.title, this.width / 2, 12, TEXT);
        drawLegend(g);

        List<LayoutItem> items = buildLayout();
        int maxScroll = Math.max(0, contentHeight - (listBottom - listTop));
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        g.enableScissor(listLeft - 2, listTop, listRight + 2, listBottom);
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
        g.disableScissor();
    }

    private void drawLegend(GuiGraphics g) {
        Component keep = Component.translatable("placeonpath.config.legend_keep");
        Component dirt = Component.translatable("placeonpath.config.legend_dirt");
        int gap = 18;
        int kw = 12 + this.font.width(keep);
        int dw = 12 + this.font.width(dirt);
        int x = this.width / 2 - (kw + gap + dw) / 2;
        int y = 50;
        g.fill(x, y, x + 8, y + 8, PILL_GREEN);
        g.drawString(this.font, keep, x + 12, y, SUBTLE, false);
        int x2 = x + kw + gap;
        g.fill(x2, y, x2 + 8, y + 8, PILL_RED);
        g.drawString(this.font, dirt, x2 + 12, y, SUBTLE, false);
    }

    private void renderHeader(GuiGraphics g, HeaderItem h, int sy) {
        g.fill(listLeft, sy, listRight, sy + HEADER_H, BG_HEADER);
        g.drawString(this.font, isExpanded(h.cat) ? "v" : ">", listLeft + 6, sy + 7, SUBTLE, false);
        Component name = Component.translatable(h.cat.langKey());
        g.drawString(this.font, name, listLeft + 20, sy + 7, TEXT, false);
        g.drawString(this.font, "(" + h.visibleCount + ")",
                listLeft + 20 + this.font.width(name) + 6, sy + 7, SUBTLE, false);

        Component pill = pillComponent(h.cat);
        int pw = this.font.width(pill) + 12;
        int px = listRight - pw - 6;
        g.fill(px, sy + 4, px + pw, sy + HEADER_H - 4, pillColor(h.cat));
        g.drawString(this.font, pill, px + 6, sy + 7, PILL_TEXT, false);
    }

    private void renderCell(GuiGraphics g, CellItem c, int sy, int mouseX, int mouseY) {
        boolean red = ConfigStore.isBlacklisted(c.entry.id);
        boolean hover = mouseX >= c.x && mouseX <= c.x + c.w && mouseY >= sy && mouseY <= sy + c.h
                && mouseY >= listTop && mouseY <= listBottom;
        int bg = red ? (hover ? RED_HOVER : RED_CELL) : (hover ? GREEN_HOVER : GREEN_CELL);
        g.fill(c.x, sy, c.x + c.w, sy + c.h, bg);
        g.renderItem(new ItemStack(c.entry.block), c.x + 2, sy + (c.h - 16) / 2);
        int textX = c.x + 22;
        String label = this.font.plainSubstrByWidth(c.entry.name, c.x + c.w - 4 - textX);
        g.drawString(this.font, label, textX, sy + (c.h - 8) / 2, TEXT, false);
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
        // all green -> turn whole category red; otherwise -> reset whole category green
        ConfigStore.setAll(ids, allGreen);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseY >= listTop && mouseY <= listBottom) {
            int maxScroll = Math.max(0, contentHeight - (listBottom - listTop));
            scroll = Math.max(0, Math.min(scroll - scrollY * 18, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        ConfigStore.save();
        this.minecraft.setScreen(parent);
    }
}
```

- [ ] **Step 2: Build common**

Run: `... GradleWrapperMain :common:build` (JDK21)
Expected: BUILD SUCCESSFUL. If `renderBackground(GuiGraphics,int,int,float)` is rejected, this branch uses the 1-arg form — see the porting notes (Task 7) for the exact signature per version; on 1.21.1 the 4-arg form is correct.

- [ ] **Step 3: Commit**

```bash
git add common/src/main/java/de/maxi/placeonpath/client/PathConfigScreen.java
git commit -m "Add PathConfigScreen (categorized green/red block toggles)"
```

---

## Task 4: Wire the screen into Fabric + NeoForge (remove AutoConfig)

**Files:**
- Modify: `fabric/src/main/java/de/maxi/placeonpath/fabric/PlaceonpathModMenu.java`
- Modify: `neoforge/src/main/java/de/maxi/placeonpath/neoforge/PlaceonpathNeoForgeClient.java`

- [ ] **Step 1: `PlaceonpathModMenu.java`** — return the custom screen:

```java
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
```

- [ ] **Step 2: `PlaceonpathNeoForgeClient.java`** — return the custom screen (constructor of `PlaceonpathNeoForge` is unchanged; it already calls this only on `Dist.CLIENT`):

```java
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
```

- [ ] **Step 3: Build fabric + neoforge**

Run: `... GradleWrapperMain :fabric:build :neoforge:build` (JDK21)
Expected: BUILD SUCCESSFUL for both. No more references to `ModConfig`/`AutoConfig` anywhere (grep to confirm: `git grep -n "AutoConfig\|ModConfig\|cloth" -- '*.java' '*.gradle' '*.json' '*.toml'` returns nothing).

- [ ] **Step 4: Commit**

```bash
git add fabric/src/main/java/de/maxi/placeonpath/fabric/PlaceonpathModMenu.java \
        neoforge/src/main/java/de/maxi/placeonpath/neoforge/PlaceonpathNeoForgeClient.java
git commit -m "Open the custom config screen from ModMenu + NeoForge"
```

---

## Task 5: Lang files

**Files:**
- Modify: `common/src/main/resources/assets/placeonpath/lang/en_us.json`
- Create: `common/src/main/resources/assets/placeonpath/lang/de_de.json`

- [ ] **Step 1: Replace `en_us.json`** (drop the autoconfig keys, add screen + category keys):

```json
{
  "block.placeonpath.path_full_block": "Dirt Path",
  "placeonpath.config.title": "PlaceOnPath",
  "placeonpath.config.search": "Search blocks...",
  "placeonpath.config.legend_keep": "keeps path",
  "placeonpath.config.legend_dirt": "turns to dirt",
  "placeonpath.config.all_on": "all on",
  "placeonpath.config.all_off": "all off",
  "placeonpath.config.mixed": "mixed",
  "placeonpath.category.fences": "Fences & Gates",
  "placeonpath.category.walls": "Walls",
  "placeonpath.category.plates_buttons": "Pressure Plates & Buttons",
  "placeonpath.category.signs_banners": "Signs & Banners",
  "placeonpath.category.slabs": "Slabs",
  "placeonpath.category.stairs": "Stairs",
  "placeonpath.category.doors": "Doors & Trapdoors",
  "placeonpath.category.carpets": "Carpets",
  "placeonpath.category.lights": "Torches & Lanterns",
  "placeonpath.category.plants": "Plants & Flowers",
  "placeonpath.category.wood": "Wood",
  "placeonpath.category.stone": "Stone",
  "placeonpath.category.nature": "Nature",
  "placeonpath.category.other": "Other"
}
```

- [ ] **Step 2: Create `de_de.json`** (the author is German):

```json
{
  "block.placeonpath.path_full_block": "Trampelpfad",
  "placeonpath.config.title": "PlaceOnPath",
  "placeonpath.config.search": "Bloecke suchen...",
  "placeonpath.config.legend_keep": "haelt Pfad",
  "placeonpath.config.legend_dirt": "wird zu Erde",
  "placeonpath.config.all_on": "alle an",
  "placeonpath.config.all_off": "alle aus",
  "placeonpath.config.mixed": "gemischt",
  "placeonpath.category.fences": "Zaeune & Tore",
  "placeonpath.category.walls": "Mauern",
  "placeonpath.category.plates_buttons": "Druckplatten & Knoepfe",
  "placeonpath.category.signs_banners": "Schilder & Banner",
  "placeonpath.category.slabs": "Stufen",
  "placeonpath.category.stairs": "Treppen",
  "placeonpath.category.doors": "Tueren & Falltueren",
  "placeonpath.category.carpets": "Teppiche",
  "placeonpath.category.lights": "Fackeln & Laternen",
  "placeonpath.category.plants": "Pflanzen & Blumen",
  "placeonpath.category.wood": "Holz",
  "placeonpath.category.stone": "Stein",
  "placeonpath.category.nature": "Natur",
  "placeonpath.category.other": "Sonstiges"
}
```

(ASCII-only here to avoid any encoding surprise across branches; if you prefer real umlauts use the `\uXXXX` escapes — but vanilla resource loading reads these as UTF-8, so umlauts are fine too. Keeping ASCII is the safe default.)

- [ ] **Step 3: Build**

Run: `... GradleWrapperMain :common:build` (JDK21)
Expected: BUILD SUCCESSFUL (lang files are data; this just confirms nothing else broke).

- [ ] **Step 4: Commit**

```bash
git add common/src/main/resources/assets/placeonpath/lang/en_us.json \
        common/src/main/resources/assets/placeonpath/lang/de_de.json
git commit -m "Lang: config screen + category names (en, de)"
```

---

## Task 6: Verify on master (build + smoke + in-game)

- [ ] **Step 1: Full build**

Run: `... GradleWrapperMain build` (JDK21)
Expected: BUILD SUCCESSFUL; jars in `fabric/build/libs`, `neoforge/build/libs`.

- [ ] **Step 2: Fabric client smoke test**

Run `:fabric:runClient`. Watch the log for `Sound engine started` and NO mixin/ModMenu/`PathConfigScreen` classload errors. (Do not kill Lunar Client when stopping — see memory `dont-kill-lunar-client`.)

- [ ] **Step 3: In-game functional check** (have the user drive, or the running client)

  1. Mods → PlaceOnPath → Config: the screen opens, shows categories, every block green by default.
  2. Expand "Fences & Gates", click "Oak Fence" → it turns red. Close (Done).
  3. Place an oak fence on a dirt path → the path turns to dirt (because it's red).
  4. Place a torch (still green) on a path → path is preserved (full-height), as before.
  5. Reopen Config: oak fence is still red (persisted to `config/placeonpath.json`).
  6. Click the "Fences & Gates" pill → whole category toggles red; click again → all green.
  7. Type "oak" in search → only oak-named blocks show, categories auto-expand.

- [ ] **Step 4: Background/scroll polish check.** Confirm the screen has the normal dimmed background (not see-through) and that scrolling clips cleanly at the top/bottom edges. If the background is missing or doubly-darkened, adjust whether `renderBackground(...)` is called explicitly in `render` (on 1.21.1 the explicit 4-arg call is correct). Commit any fixup:

```bash
git add -A && git commit -m "Polish config screen rendering"
```

(May be empty — fine.)

---

## Task 7: Port to the other 7 branches

For each branch (already created): close IntelliJ first (it reverts files / switches branches — see prior session), `git switch <branch>`, clean `.gradle`/loom caches + build dirs when switching, then apply the same change set with the per-version deltas below. Build (`:fabric:build` + the loaders that branch has) + Fabric `runClient` smoke + the Task 6 in-game check. Commit the whole feature per branch as **Maxi** (no co-author). Finally refresh `Alle Versionen/`.

**Common to every branch (same as master):** delete `ModConfig`/`ModConfigHolder` + Cloth deps/repo + `cloth-config` mod-deps; add `ConfigStore`, `BlockCatalog`, `PathConfigScreen`; update `PathRules`, `Placeonpath.init()`, the ModMenu entrypoint, lang files. The differences are (a) registry/tag API in `PathRules`+`BlockCatalog`, (b) the GUI API in `PathConfigScreen`, (c) Forge screen registration on branches that have Forge, (d) Mod Menu version.

### 7a. GUI API mapping (the only lines that differ in `PathConfigScreen`)

The screen's *logic* (layout, hit-testing, toggle rules) is identical everywhere. Only these primitives change:

**1.20.2 – 1.21.4 (GuiGraphics, 4-arg scroll/background)** — identical to master. (1.21.4 also identical; 1.20.2/1.20.4 identical.)

**1.20.1 (GuiGraphics, but 3-arg scroll + 1-arg background):**
- `renderBackground(g, mouseX, mouseY, partialTick)` → `renderBackground(g)`.
- `mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)` → `mouseScrolled(double mouseX, double mouseY, double delta)`, and inside use `delta` in place of `scrollY` (`scroll - delta * 18`), and the `super` call passes `(mouseX, mouseY, delta)`.
- Everything else (`g.fill`, `g.drawString`, `g.renderItem`, `g.drawCenteredString`, `g.enableScissor/disableScissor`, `EditBox.setHint`) is the same.

**1.18.2 – 1.19.4 (no GuiGraphics — use PoseStack + ItemRenderer):** change the render signatures and primitives:
- `import net.minecraft.client.gui.GuiGraphics;` → `import com.mojang.blaze3d.vertex.PoseStack;` and `extends Screen` keeps `GuiComponent` statics available (Screen extends GuiComponent in these versions).
- `render(GuiGraphics g, ...)` → `render(PoseStack g, ...)`; `renderBackground(g)` (1-arg, PoseStack).
- `g.fill(x1,y1,x2,y2,color)` → `fill(g, x1,y1,x2,y2,color)` (inherited `GuiComponent.fill(PoseStack,...)`).
- `g.drawString(font, text, x, y, color, false)` → `font.draw(g, <text>, x, y, color)` (Component and String overloads exist; no shadow arg).
- `g.drawCenteredString(font, title, cx, y, color)` → `drawCenteredString(g, font, title, cx, y, color)` (inherited static).
- `g.renderItem(stack, x, y)` → `this.itemRenderer.renderGuiItem(stack, x, y)` (field `itemRenderer` on Screen in these versions; for 1.18.2 it is `this.minecraft.getItemRenderer()`).
- `g.enableScissor(x1,y1,x2,y2)/disableScissor()` → no GuiGraphics scissor; clip with `com.mojang.blaze3d.platform.Window`-based scissor helper: `net.minecraft.client.gui.GuiComponent`-era code uses `RenderSystem.enableScissor(...)` with framebuffer coords, which is fiddly. Simpler and good enough: **cull by y-range only** (skip items fully outside `[listTop, listBottom]`) and accept that a partially-scrolled row at the very edge can draw a few pixels past the list — or add a solid `fill` strip over the header/footer margins to mask overflow. Use the masking-strip approach: after drawing the list, `fill` from `0..listTop` and `listBottom..height` with the background tint.
- `mouseScrolled` is 3-arg here too (`delta`).
- `EditBox.setHint` does **not** exist before 1.19.4 → on 1.18.2 drop `this.search.setHint(...)` and instead render placeholder text manually when the box is empty (or just omit the hint).

**1.16.5 (oldest; Fabric only) — hardest:** PoseStack exists (mojmap), but:
- `Registry.BLOCK` (not `BuiltInRegistries`) in `PathRules` + `BlockCatalog` (see 7b).
- Tags: `state.is(BlockTags.FLOWERS)` works (1.16.5 `BlockTags.FLOWERS` is a `Tag.Named<Block>` and `BlockState.is(Tag)` exists). `PathRules` `#tag` lookup: replace `above.is(TagKey.create(...))` with `BlockTags.getAllTags().getTagOrEmpty(tagId).contains(above.getBlock())` (no `TagKey` in 1.16.5).
- Item icon: `this.itemRenderer.renderGuiItem(new ItemStack(block), x, y)` wrapped in `RenderSystem`/`Lighting.setupForFlatItems()` as needed; `this.itemRenderer` is available via `this.minecraft.getItemRenderer()`.
- `font.draw(PoseStack, ...)`, `fill(PoseStack, ...)`, `drawCenteredString(PoseStack, font, ...)`, `renderBackground(PoseStack)`, 3-arg `mouseScrolled`, no `setHint`.
- Mod Menu entrypoint class differs: `io.github.prospector.modmenu.api.ModMenuApi` with `getModConfigScreenFactory()` returning the period factory; declare under the `modmenu` entrypoint as today.
- **Fallback:** if the old GUI stack proves too costly on the 1.16.5 toolchain, ship 1.16.5 with the gson config only (hand-edited `placeonpath.json`, no screen) and tell the user — consistent with 1.16.5 already being the reduced (Fabric-only) branch.

### 7b. Registry/tag API in `PathRules` + `BlockCatalog`

- **1.19.3 – 1.21.4:** `BuiltInRegistries.BLOCK` + `TagKey` — as master. (1.19.4, 1.20.1, 1.20.4, 1.21.4.)
- **1.18.2 & 1.19.2:** no `BuiltInRegistries`. Use `net.minecraft.core.Registry.BLOCK` → `Registry.BLOCK.getKey(block)` and iterate `Registry.BLOCK`. `TagKey` exists in 1.18.2/1.19.2, so `state.is(TagKey.create(Registry.BLOCK_REGISTRY, tag))` and `state.is(BlockTags.FLOWERS)` work.
- **1.16.5:** `Registry.BLOCK` + the no-`TagKey` tag handling from 7a.

### 7c. Forge screen registration (branches with Forge: 1.20.4, 1.20.1, 1.19.4, 1.19.2, 1.18.2)

Add a client-side registration of the same `PathConfigScreen` so the Forge mods-list "Config" button opens it. Class/extension-point name by version:

- **1.20.1 / 1.20.4 / 1.19.x:** in the Forge client init (guarded to client):
```java
ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
        () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new PathConfigScreen(parent)));
```
- **1.18.2:** the type is `ConfigGuiHandler.ConfigGuiFactory` (same shape):
```java
ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class,
        () -> new ConfigGuiHandler.ConfigGuiFactory((mc, parent) -> new PathConfigScreen(parent)));
```
Register from the Forge mod's `FMLClientSetupEvent` (or constructor guarded by `FMLEnvironment.dist == Dist.CLIENT`), mirroring how the NeoForge branch isolates client GUI code.

### 7d. Per-branch checklist

- [ ] **`1.21.4`** (JDK21, Fabric+NeoForge) — GUI/registry identical to master. Mod Menu version per branch (verify via Modrinth). NeoForge wiring identical.
- [ ] **`1.20.4`** (JDK17, Fabric+Forge+NeoForge) — GUI identical (1.20.2+). Registry identical. Add **Forge** registration (7c). Mod Menu version per branch.
- [ ] **`1.20.1`** (JDK17, Fabric+Forge) — GUI deltas: 1-arg `renderBackground`, 3-arg `mouseScrolled` (7a). Registry identical. Forge registration (7c).
- [ ] **`1.19.4`** (JDK17, Fabric+Forge) — **PoseStack** GUI rewrite (7a). `BuiltInRegistries` still present. Forge registration (7c).
- [ ] **`1.19.2`** (JDK17, Fabric+Forge) — PoseStack GUI (7a). `Registry.BLOCK` (7b). Forge registration (7c).
- [ ] **`1.18.2`** (JDK17, Fabric+Forge) — PoseStack GUI (7a), no `EditBox.setHint`. `Registry.BLOCK` (7b). Forge registration via `ConfigGuiHandler` (7c).
- [ ] **`1.16.5`** (JDK17 + gradle 7.6.4, Fabric only) — hardest (7a 1.16.5 notes + 7b). Period Mod Menu entrypoint. Use the gson-only fallback if the GUI proves unworkable.
- [ ] **After all branches:** rebuild all, refresh `Alle Versionen/` (reuse the per-branch build+copy batch), and update memory `version-branches` to note the config GUI replaced the Cloth screen.

---

## Self-review notes

- **Spec coverage:** custom categorized green/red grid (Tasks 2–3); default all-green / empty blacklist (ConfigStore empty → `PathRules` returns false → existing keep-path behavior, Task 1); per-block + per-category toggle + search (Task 3); persistence via gson (Task 1); screen reachable from Mods menu on every loader (Task 4 + 7c); all 8 branches (Task 7); live edits (`PathRules` reads `ConfigStore` each call); server/client model unchanged (logic server-side, screen client-side, guarded). Covered.
- **Type consistency:** `ConfigStore.entries()/isBlacklisted(String)/set(String,boolean)/setAll(Collection,boolean)` used identically in `PathRules` and `PathConfigScreen`; `BlockCatalog.categories()` → `Category{id, entries, langKey()}` and `Entry{id, block, name}` used in the screen; `PathConfigScreen(Screen parent)` constructor referenced by both loader entrypoints.
- **Known verify-points (not placeholders):** the `renderBackground` arg-count per version (mapping in 7a, fix noted in Task 6 Step 4); Forge extension-point class name per version (7c); Mod Menu version per branch (Modrinth lookup); 1.16.5 GUI feasibility with a stated gson-only fallback. Each has a concrete check + fallback, not a TODO.
- **Deviation from skill default (TDD):** no unit tests — this project has no test harness and Minecraft GUI/registry/mixin code requires the running game; verification is build + smoke + in-game, matching the existing project workflow. This is the project-context override the skill allows.
```