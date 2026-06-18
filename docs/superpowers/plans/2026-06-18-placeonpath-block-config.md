# PlaceOnPath Configurable Block List — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an in-game config screen (Mods menu) where players list blocks/tags that should make a path turn to dirt; everything not listed keeps the path (current behavior).

**Architecture:** Cloth Config (AutoConfig) holds a `List<String>` blacklist in the shared `common` module. A `PathRules` helper resolves block-id and `#tag` entries. The three existing mixin hook points consult `PathRules`. Thin per-loader entrypoints expose the Cloth screen (ModMenu on Fabric, the config-screen extension point on Forge/NeoForge).

**Tech Stack:** Java, Architectury, SpongePowered Mixin, Cloth Config (`me.shedaniel.cloth`), Mod Menu (`com.terraformersmc`). Verification per task = `gradlew build` + `:fabric:runClient` smoke test (no unit-test harness in this project).

**Build invocation (this machine):** `JAVA_HOME=/c/Users/Maxi/.jdks/temurin-21.0.7` and run `"$JAVA_HOME/bin/java.exe" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain <task>` (the bracketed path breaks `gradlew`). JDK 17 for 1.18.2–1.20.4, JDK 21 for 1.21.x, gradle 7.6.4/JDK17 for 1.16.5. See memory `build-environment`.

**Commits:** author Maxi, **no Claude co-author** (user requirement). One commit per branch's full feature.

---

## File structure (master / 1.21.1)

- `common/src/main/java/de/maxi/placeonpath/config/ModConfig.java` — Cloth AutoConfig data class (the blacklist).
- `common/src/main/java/de/maxi/placeonpath/config/ModConfigHolder.java` — register + accessor.
- `common/src/main/java/de/maxi/placeonpath/config/PathRules.java` — id/tag match decision.
- `common/src/main/java/de/maxi/placeonpath/mixin/BlockItemMixin.java` — make rule-aware (modify).
- `common/src/main/java/de/maxi/placeonpath/mixin/DirtPathBlockMixin.java` — make rule-aware (modify).
- `common/src/main/java/de/maxi/placeonpath/Placeonpath.java` — call config register (modify).
- `fabric/src/main/java/de/maxi/placeonpath/fabric/PlaceonpathModMenu.java` — ModMenu entrypoint (create).
- `fabric/src/main/resources/fabric.mod.json` — declare `modmenu` entrypoint + suggest modmenu (modify).
- `neoforge/src/main/java/de/maxi/placeonpath/neoforge/PlaceonpathNeoForge.java` — register config screen (modify).
- `build.gradle` — add Cloth + Mod Menu maven repos (modify).
- `common/build.gradle`, `fabric/build.gradle`, `neoforge/build.gradle` — add deps (modify).

---

## Task 1: Add dependency repos + dependencies (master / 1.21.1)

**Files:**
- Modify: `build.gradle` (root `subprojects { repositories { } }`)
- Modify: `common/build.gradle`, `fabric/build.gradle`, `neoforge/build.gradle`

- [ ] **Step 1: Add the Cloth + Mod Menu repos** to the root `build.gradle` inside `subprojects { ... repositories { ... } }` (the block is currently empty):

```gradle
    repositories {
        maven { url "https://maven.shedaniel.me/" }                 // Cloth Config
        maven { url "https://maven.terraformersmc.com/releases/" }  // Mod Menu
    }
```

- [ ] **Step 2: common/build.gradle** — add Cloth API for compile (after the architectury line):

```gradle
    modImplementation "me.shedaniel.cloth:cloth-config-fabric:15.0.140"
```

- [ ] **Step 3: fabric/build.gradle** — add Cloth + Mod Menu (after the architectury-fabric line):

```gradle
    modImplementation "me.shedaniel.cloth:cloth-config-fabric:15.0.140"
    modImplementation "com.terraformersmc:modmenu:11.0.4"
```

- [ ] **Step 4: neoforge/build.gradle** — add Cloth NeoForge (after the architectury-neoforge line):

```gradle
    modImplementation "me.shedaniel.cloth:cloth-config-neoforge:15.0.140"
```

- [ ] **Step 5: Verify dependencies resolve**

Run: `"$JAVA_HOME/bin/java.exe" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :common:dependencies --configuration modImplementation` (JAVA_HOME=JDK21)
Expected: lists cloth-config-fabric 15.0.140 with no "FAILED". If Cloth pulls a conflicting fabric-loader, add `{ exclude group: "net.fabricmc" }` to the common cloth dep and rebuild.

- [ ] **Step 6: Commit**

```bash
git add build.gradle common/build.gradle fabric/build.gradle neoforge/build.gradle
git commit -m "Add Cloth Config + Mod Menu dependencies"
```

---

## Task 2: Config model + holder (common)

**Files:**
- Create: `common/src/main/java/de/maxi/placeonpath/config/ModConfig.java`
- Create: `common/src/main/java/de/maxi/placeonpath/config/ModConfigHolder.java`
- Modify: `common/src/main/java/de/maxi/placeonpath/Placeonpath.java`

- [ ] **Step 1: Create ModConfig.java**

```java
package de.maxi.placeonpath.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.ArrayList;
import java.util.List;

@Config(name = "placeonpath")
public class ModConfig implements ConfigData {
    @Comment("Blocks (or #tags) that make a path turn to dirt when placed on it. " +
             "Empty = every block keeps the path. Examples: minecraft:oak_fence  #minecraft:fences")
    public List<String> turnPathToDirt = new ArrayList<>();
}
```

- [ ] **Step 2: Create ModConfigHolder.java**

```java
package de.maxi.placeonpath.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

public final class ModConfigHolder {
    private ModConfigHolder() {}

    public static void register() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
    }

    public static ModConfig get() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }
}
```

- [ ] **Step 3: Register on init** — in `Placeonpath.init()` add the call:

```java
    public static void init() {
        ModConfigHolder.register();
        ModBlocks.init();
    }
```

(add `import de.maxi.placeonpath.config.ModConfigHolder;`)

- [ ] **Step 4: Build**

Run: `... GradleWrapperMain :common:build` (JDK21)
Expected: BUILD SUCCESSFUL. If `@Comment` import is wrong for this Cloth version, remove the `@Comment` line and its import (it is cosmetic).

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/de/maxi/placeonpath/config common/src/main/java/de/maxi/placeonpath/Placeonpath.java
git commit -m "Add config model (blacklist of blocks/tags)"
```

---

## Task 3: PathRules decision helper (common)

**Files:**
- Create: `common/src/main/java/de/maxi/placeonpath/config/PathRules.java`

- [ ] **Step 1: Create PathRules.java** (1.21.1 registry/tag API)

```java
package de.maxi.placeonpath.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class PathRules {
    private PathRules() {}

    /** True if the block above should make the path turn to dirt (vanilla). */
    public static boolean shouldTurnToDirt(BlockState above) {
        var entries = ModConfigHolder.get().turnPathToDirt;
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

- [ ] **Step 2: Build**

Run: `... GradleWrapperMain :common:build` (JDK21)
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add common/src/main/java/de/maxi/placeonpath/config/PathRules.java
git commit -m "Add PathRules (block id + #tag matching)"
```

---

## Task 4: Make BlockItemMixin rule-aware (common)

**Files:**
- Modify: `common/src/main/java/de/maxi/placeonpath/mixin/BlockItemMixin.java`

- [ ] **Step 1: Guard both swaps with PathRules.** Replace the two `if` bodies so the swap only happens when the relevant block is NOT blacklisted:

```java
        // Case 1: block placed on top of a path -> full-height, unless it's blacklisted
        BlockPos belowPlaced = placedPos.below();
        if (level.getBlockState(belowPlaced).is(Blocks.DIRT_PATH)
                && !PathRules.shouldTurnToDirt(placedState)) {
            level.setBlock(belowPlaced, ModBlocks.FULL_PATH_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        }

        // Case 2: path placed under an existing block -> full-height, unless that block is blacklisted
        BlockState aboveState = level.getBlockState(placedPos.above());
        if (placedState.is(Blocks.DIRT_PATH) && !aboveState.isAir()
                && !PathRules.shouldTurnToDirt(aboveState)) {
            level.setBlock(placedPos, ModBlocks.FULL_PATH_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        }
```

(add `import de.maxi.placeonpath.config.PathRules;`)

- [ ] **Step 2: Build**

Run: `... GradleWrapperMain :common:build` (JDK21)
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add common/src/main/java/de/maxi/placeonpath/mixin/BlockItemMixin.java
git commit -m "BlockItemMixin respects the blacklist"
```

---

## Task 5: Make DirtPathBlockMixin rule-aware (common)

**Files:**
- Modify: `common/src/main/java/de/maxi/placeonpath/mixin/DirtPathBlockMixin.java`

- [ ] **Step 1: canSurvive** — only force survive=true when the block above is NOT blacklisted:

```java
    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void placeonpath$alwaysSurvive(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!PathRules.shouldTurnToDirt(level.getBlockState(pos.above()))) {
            cir.setReturnValue(true);
        }
        // blacklisted -> do not override; vanilla canSurvive decides (turns to dirt for solid blocks)
    }
```

- [ ] **Step 2: tick** — redirect to full block only when not blacklisted; otherwise let vanilla turnToDirt run:

```java
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void placeonpath$keepPath(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        BlockState above = level.getBlockState(pos.above());
        if (above.isAir()) { ci.cancel(); return; }
        if (PathRules.shouldTurnToDirt(above)) return;  // let vanilla turnToDirt proceed
        level.setBlockAndUpdate(pos, ModBlocks.FULL_PATH_BLOCK.get().defaultBlockState());
        ci.cancel();
    }
```

(add `import de.maxi.placeonpath.config.PathRules;`)

- [ ] **Step 3: Build**

Run: `... GradleWrapperMain :common:build` (JDK21)
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add common/src/main/java/de/maxi/placeonpath/mixin/DirtPathBlockMixin.java
git commit -m "DirtPathBlockMixin respects the blacklist"
```

---

## Task 6: Fabric ModMenu entrypoint

**Files:**
- Create: `fabric/src/main/java/de/maxi/placeonpath/fabric/PlaceonpathModMenu.java`
- Modify: `fabric/src/main/resources/fabric.mod.json`

- [ ] **Step 1: Create PlaceonpathModMenu.java**

```java
package de.maxi.placeonpath.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import de.maxi.placeonpath.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;

public class PlaceonpathModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(ModConfig.class, parent).get();
    }
}
```

- [ ] **Step 2: fabric.mod.json** — add the `modmenu` entrypoint inside `"entrypoints"` and suggest Mod Menu:

```json
  "entrypoints": {
    "main": [ "de.maxi.placeonpath.fabric.PlaceonpathFabric" ],
    "client": [ "de.maxi.placeonpath.fabric.client.PlaceonpathFabricClient" ],
    "modmenu": [ "de.maxi.placeonpath.fabric.PlaceonpathModMenu" ]
  },
  "suggests": { "modmenu": "*" },
```

- [ ] **Step 3: Build fabric**

Run: `... GradleWrapperMain :fabric:build` (JDK21)
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add fabric/src/main/java/de/maxi/placeonpath/fabric/PlaceonpathModMenu.java fabric/src/main/resources/fabric.mod.json
git commit -m "Fabric: Mod Menu config screen entrypoint"
```

---

## Task 7: NeoForge config-screen registration

**Files:**
- Modify: `neoforge/src/main/java/de/maxi/placeonpath/neoforge/PlaceonpathNeoForge.java`

- [ ] **Step 1: Register the Cloth screen as the NeoForge config screen.** In the `@Mod` constructor (which receives the `ModContainer` on NeoForge 1.21.1), register the extension point:

```java
package de.maxi.placeonpath.neoforge;

import de.maxi.placeonpath.Placeonpath;
import de.maxi.placeonpath.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(Placeonpath.MOD_ID)
public final class PlaceonpathNeoForge {
    public PlaceonpathNeoForge(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (c, parent) -> AutoConfig.getConfigScreen(ModConfig.class, parent).get());
        Placeonpath.init();
    }
}
```

- [ ] **Step 2: Build neoforge**

Run: `... GradleWrapperMain :neoforge:build` (JDK21)
Expected: BUILD SUCCESSFUL. If `IConfigScreenFactory`'s package/signature differs for this NeoForge build, check the decompiled `net.neoforged.neoforge.client.gui` package; the lambda takes `(ModContainer, Screen parent)` and returns a `Screen`. If Cloth already auto-registers a NeoForge screen (some versions do), this step may be optional — verify in Step 3 of Task 8.

- [ ] **Step 3: Commit**

```bash
git add neoforge/src/main/java/de/maxi/placeonpath/neoforge/PlaceonpathNeoForge.java
git commit -m "NeoForge: register config screen"
```

---

## Task 8: Verify on master (build + runtime + in-game)

- [ ] **Step 1: Full build**

Run: `... GradleWrapperMain build` (JDK21)
Expected: BUILD SUCCESSFUL; jars in `fabric/build/libs`, `neoforge/build/libs`.

- [ ] **Step 2: Fabric client smoke test (config + screen load)**

Run `:fabric:runClient`. Watch the log for `Sound engine started` and NO mixin/Cloth/ModMenu errors. (ModMenu is on the dev classpath via the fabric dep.)

- [ ] **Step 3: In-game functional check** (ask the user, or via the running client)

  1. Mods menu → PlaceOnPath → config button opens; the list field is editable.
  2. Empty list: place a fence on a path → path preserved (full-height). (unchanged)
  3. Add `minecraft:oak_fence`, save → place an oak fence on a path → path turns to dirt.
  4. Replace with `#minecraft:fences`, save → any fence turns the path to dirt; a torch (not listed) keeps the path.

- [ ] **Step 4: Commit any fixups**, then the feature on master is complete.

```bash
git add -A && git commit -m "Add configurable block list with in-game config screen"
```

(If tasks were committed individually, this may be empty — that is fine.)

---

## Task 9: Port to the other 7 branches

For each branch below, branch is already created. Repeat Tasks 1–8 with the per-branch dependency versions and API notes. **Clean `.gradle/loom-cache` + build dirs when switching branches** (see memory). Commit the whole feature per branch as Maxi.

Dependency versions to look up per branch (Cloth Config + Mod Menu) via the Modrinth API
`https://api.modrinth.com/v2/project/cloth-config/version?game_versions=["<mc>"]&loaders=["fabric"]`
and the same for `modmenu`; Cloth NeoForge/Forge variant shares the version number.

- [ ] **Step 1: `1.21.4`** (JDK21) — Cloth `cloth-config-*:17.x`, Mod Menu `13.x` (verify). Same code as master (1.21.2+ API already in this branch). Forge: n/a (NeoForge only).
- [ ] **Step 2: `1.20.4`** (JDK17) — Cloth `cloth-config-*:13.x`, Mod Menu `9.x` (verify). Has Fabric + Forge + NeoForge: also add the **Forge** screen — in `PlaceonpathForge` register via `ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> AutoConfig.getConfigScreen(ModConfig.class, parent).get()))`. `PathRules` uses `BuiltInRegistries` (present in 1.20.4).
- [ ] **Step 3: `1.20.1`** (JDK17) — Cloth `11.x`, Mod Menu `7.x` (verify). Fabric + Forge. `PathRules`: `Registry.BLOCK` instead of `BuiltInRegistries.BLOCK` for the id lookup; `state.is(TagKey...)` exists. Forge screen via `ConfigScreenHandler` (1.19/1.20 form).
- [ ] **Step 4: `1.19.4`** (JDK17) — Cloth `10.x`, Mod Menu `6.x` (verify). Fabric + Forge. Same `PathRules` registry note as 1.20.1.
- [ ] **Step 5: `1.19.2`** (JDK17) — Cloth `8.x`, Mod Menu `4.x` (verify). `PathRules`: `Registry.BLOCK.getKey(...)` and `Registry.BLOCK_REGISTRY`-style tag key; tags via `state.is(TagKey.create(Registry.BLOCK_REGISTRY, tag))`.
- [ ] **Step 6: `1.18.2`** (JDK17) — Cloth `6.x`, Mod Menu `3.x` (verify). `PathRules`: `Registry.BLOCK`; tag membership via `state.is(TagKey...)` (1.18.2 has TagKey) or `BlockTags`. Forge screen via `ConfigGuiHandler.ConfigGuiFactory` (1.18 name).
- [ ] **Step 7: `1.16.5`** (JDK17, gradle 7.6.4) — **hardest.** Old Cloth `me.shedaniel.cloth:config-2:4.x.x` (on `maven.shedaniel.me`, group `me.shedaniel.cloth`) and the period Mod Menu `io.github.prospector:modmenu:1.16.x`. AutoConfig API exists in old Cloth (`me.shedaniel.autoconfig`). `PathRules`: 1.16.5 has no `TagKey`/`BuiltInRegistries`; use `Registry.BLOCK.getKey(block)` and tag membership via `BlockTags.getAllTags().getTagOrEmpty(tagId).contains(block)` (or `state.is(net.minecraft.tags.Tag)`); `GrassPathBlock`. Mod Menu entrypoint class is `io.github.prospector.modmenu.api.ModMenuApi` with `getModConfigScreenFactory`. If the old Cloth/ModMenu integration proves unworkable on the old toolchain, fall back to a plain JSON config file edited by hand for 1.16.5 (no screen) and flag to the user.

- [ ] **Step 8: After all branches**, rebuild all and refresh `Alle Versionen/` (reuse the existing per-branch build+copy batch), and update memory `version-branches` to note the config feature.

---

## Self-review notes

- **Spec coverage:** blacklist + tags (Task 3); default-empty unchanged behavior (Tasks 4–5 guards); screen via ModMenu (Task 6) + Forge/NeoForge (Tasks 7, 9); live edits (`PathRules` reads holder each call); all 8 branches (Tasks 1–8 master, Task 9 rest); server/client (logic is server-side, mixins already run server-side). Covered.
- **Type consistency:** `ModConfigHolder.get()`, `PathRules.shouldTurnToDirt(BlockState)`, `ModConfig.turnPathToDirt` used consistently across Tasks 2–7.
- **Known verification points (not placeholders):** exact Cloth/ModMenu versions per branch (looked up in Task 9 steps), NeoForge/Forge config-screen extension-point class names per version (decompiled-source check noted), and the common-module Cloth classpath exclude (Task 1 Step 5). Each has a concrete check + fallback.
