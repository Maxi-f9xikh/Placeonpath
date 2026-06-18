# PlaceOnPath – Configurable block list (in-game config screen)

**Date:** 2026-06-18
**Status:** Approved design (pending spec review)

## Goal

Let players configure, from an in-game config screen reachable via the Mods menu,
which blocks keep the path versus which let the path turn to dirt.

- **Default:** every block keeps the path (current mod behavior — list is empty).
- The player maintains a **blacklist**: blocks (or tags) that, when placed on a
  path, make the path turn into dirt (vanilla behavior) instead of being preserved.

## Requirements

1. A blacklist of entries, each either a block id (`minecraft:oak_fence`) or a
   block tag (prefixed with `#`, e.g. `#minecraft:fences`).
2. Empty by default → unchanged current behavior (path always preserved + made full-height).
3. Editable from the Mods menu: ModMenu on Fabric, the Forge/NeoForge mods-list config button.
4. Cross-loader (Fabric + Forge + NeoForge), all 8 version branches (1.16.5–1.21.4).
5. Live: editing the list takes effect without restarting the game.

## Approach

**Cloth Config** (`me.shedaniel.cloth`) — the standard cross-loader config library
with a built-in GUI builder that integrates with ModMenu (Fabric) and the
Forge/NeoForge config-screen extension point. One config definition yields the
screen on every loader. (Chosen over hand-rolling a per-loader screen or mixing
ForgeConfigSpec + Cloth.)

## Architecture

All gameplay logic and the config model live in the shared `common` module; only
the thin screen-registration entrypoints are per-loader.

### Components

- **`ModConfig` (common)** — the config object. One field:
  `List<String> turnPathToDirt` (default empty). Persisted by Cloth to
  `config/placeonpath.json`.
- **`PathRules` (common)** — single source of truth for the decision:
  `boolean shouldTurnToDirt(BlockState above)`. Returns true if `above`'s block id
  equals a non-`#` entry, OR `above` is in a tag named by a `#`-entry. Reads the
  current `ModConfig` each call (so edits are live). Tag entries are resolved via
  the block's holder/tag membership for that MC version.
- **Mixin changes (common)** — the three existing hook points become rule-aware:
  - `BlockItemMixin#onBlockPlaced`: swap path → full-height block only when
    `!PathRules.shouldTurnToDirt(placedBlock)`; otherwise do nothing (vanilla turns
    it to dirt). Applies to both existing cases (block-on-path and path-under-block).
  - `DirtPathBlockMixin#canSurvive`: keep forcing the survive=true (path stays)
    **unless** `shouldTurnToDirt(above)` — in that case do not override, letting
    vanilla `canSurvive` run (schedules the turn-to-dirt tick for solid blocks).
  - `DirtPathBlockMixin#tick`: redirect to the full-height block **unless**
    `shouldTurnToDirt(above)` — in that case let vanilla `turnToDirt` proceed.
  - (1.16.5 uses `GrassPathBlock` and its `Block.UPDATE_ALL`=`3` quirk as today.)
- **Screen registration (per loader, thin):**
  - Fabric: a `ModMenuApi` entrypoint returning the Cloth config screen factory
    (declared in `fabric.mod.json` under the `modmenu` entrypoint). ModMenu is an
    optional/suggested dependency — without it the mod still loads, just no button.
  - Forge/NeoForge: register the config screen through the platform extension point
    (`ConfigScreenHandler`/`IConfigScreenFactory` per version) so the mods-list
    "Config" button opens the Cloth screen.

### Data flow

1. Player places a block on (or a path under) a dirt/grass path.
2. The relevant mixin calls `PathRules.shouldTurnToDirt(aboveBlockState)`.
3. Match (id or tag) → vanilla path-to-dirt. No match → full-height path block (preserved).
4. The config screen edits `turnPathToDirt` → Cloth saves the json → `PathRules`
   reads the updated list on the next placement.

## Behavior / semantics

- Blacklist semantics (not whitelist): only listed blocks/tags turn the path to dirt.
- A listed block placed on a path → path becomes dirt exactly like vanilla.
- A non-listed block → current behavior (path swapped to the full-height variant, no gap).
- Tag entry `#ns:name` matches any block in that tag for the running version.
- Unknown/typo entries are ignored (no crash); optionally logged once.

### Server vs client

The decision runs server-side (where placement is processed). In singleplayer the
integrated server reads the local `config/placeonpath.json`, which the Mods-menu
screen edits — so it just works. On a dedicated server the **server's** config file
governs gameplay; the client's Mods-menu screen only edits the client's own file
(relevant for that client's singleplayer). This is the normal model for such configs
and is acceptable; documented, not engineered around.

## Dependencies (per branch, resolved during planning)

- Modern branches (1.18.2–1.21.4): `cloth-config-fabric/forge/neoforge` + ModMenu
  (Fabric), versions matching each MC like the existing deps.
- 1.16.5: the old `me.shedaniel.cloth:config-2` + period ModMenu on the existing
  `me.shedaniel` / loom-0.10 toolchain — the riskiest port; isolated to that branch.

## Rollout

One branch at a time (master/1.21.1 first to validate UX, then the rest), mirroring
the existing per-version workflow. Each branch: add deps, config model + screen
entrypoints, make the mixins rule-aware, build-verify + Fabric client smoke test,
then user spot-check. Commit per branch (author Maxi, no Claude co-author).

## Testing

- Build verification per branch (all loaders).
- Fabric client smoke test: config loads, ModMenu shows the screen, list edit persists.
- In-game functional check: blacklist a fence (id and via `#…:fences` tag) → placing
  it on a path turns the path to dirt; a torch (not listed) → path preserved/full-height;
  empty list → unchanged behavior.

## Risks

- Cloth/ModMenu version matrix across 8 MC versions (same kind of work as the ports).
- 1.16.5 old toolchain (old Cloth API + me.shedaniel artifacts).
- Tag-membership API differs across versions (BlockState/Holder tag checks) — handled
  in `PathRules` per branch.
