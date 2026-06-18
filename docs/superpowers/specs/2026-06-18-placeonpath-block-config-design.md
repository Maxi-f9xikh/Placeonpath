# PlaceOnPath – Configurable block list (in-game config screen)

**Date:** 2026-06-18
**Status:** Approved design — **revised** to a custom GUI (see Approach). The earlier
Cloth-Config text-list version shipped on master first; this revision replaces the
screen + persistence (the gameplay mixins and the `PathRules` contract are unchanged).

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

A **custom GUI screen** (a hand-rolled `Screen` subclass in the shared module) plus a
plain **gson JSON config** (`config/placeonpath.json`). **No Cloth Config.** The screen
shows every *placeable* block grouped into expandable categories (Fences, Walls, Slabs,
Stairs, Lights, Plants, Wood, Stone, Nature, …). Each block is **green** by default
(keeps the path) or **red** (turns the path to dirt) and is toggled by clicking it; a
category header toggles the whole group; a search box filters. The screen is wired into
the Mods menu per loader (ModMenu on Fabric, the config-screen extension point on
Forge/NeoForge). gson ships with Minecraft, so the only remaining optional dependency is
ModMenu (the Fabric button). Chosen because the player asked for a visual, categorized
green/red toggle grid, which Cloth's generated screens cannot express.

## Architecture

All gameplay logic and the config model live in the shared `common` module; only
the thin screen-registration entrypoints are per-loader.

### Components

- **`ConfigStore` (common)** — the persisted state: a `LinkedHashSet<String>` of block
  ids that turn the path to dirt (the "red" blocks). Saved to `config/placeonpath.json`
  with gson via Architectury `Platform.getConfigFolder()`. Loaded on init; saved when
  the screen closes. `#tag` entries are still honored if hand-added to the file (the GUI
  itself only writes ids).
- **`PathRules` (common)** — unchanged contract: `boolean shouldTurnToDirt(BlockState
  above)`, now reading `ConfigStore` each call. id-equality OR `#tag` membership. Live.
- **`BlockCatalog` (common)** — builds (once, lazily) the categorized list of
  *placeable* blocks (`block.asItem() != AIR`, so technical/no-item blocks like our own
  full-path block are excluded). Each block is bucketed into an ordered category by its
  registry-id name (`…_fence`, `…_slab`, `…_stairs`, `…_button`, …) plus two tag checks
  (flowers, saplings); anything unmatched falls into "Other". Entries are sorted by
  display name. Categorising by id-name (not tags) keeps it portable across versions.
- **`PathConfigScreen` (common, client)** — the hand-rolled `Screen`: title, search box,
  a scroll/clip region of expandable category headers (each with a green/red group
  toggle) and block cells (block icon + name, green = keeps path / red = turns to dirt),
  and a Done button. Clicking a cell toggles that block in `ConfigStore`; clicking a
  header pill toggles the whole category; closing the screen saves.
- **Mixin changes (common)** — **unchanged** from the text-list version already on
  master: `BlockItemMixin` swaps to the full-height block only when
  `!shouldTurnToDirt(...)`; `DirtPathBlockMixin#canSurvive`/`#tick` keep the path unless
  `shouldTurnToDirt(above)`. (1.16.5 uses `GrassPathBlock` as today.)
- **Screen registration (per loader, thin):**
  - Fabric: the existing `ModMenuApi` entrypoint, now returning
    `parent -> new PathConfigScreen(parent)`. ModMenu stays optional/suggested.
  - Forge/NeoForge: the config-screen extension point
    (`IConfigScreenFactory` / `ConfigScreenHandler` per version) returns the same
    `PathConfigScreen` so the mods-list "Config" button opens it.

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

## Dependencies (per branch)

- **No Cloth Config** — dropped (and its maven repo + the `cloth-config` mod-deps in
  `fabric.mod.json` / `neoforge.mods.toml`). gson is bundled with Minecraft.
- **Mod Menu** (Fabric only, optional/suggested) — provides the in-game config button;
  version matched per branch (already present on each). Forge/NeoForge use the built-in
  mods-list config button, no extra dependency.
- The custom screen uses only vanilla client GUI APIs, so the per-branch work is
  adapting those APIs (`GuiGraphics` in 1.20+, `PoseStack` + `ItemRenderer` + 3-arg
  `mouseScrolled`/`renderBackground` in 1.16.5–1.19.x).

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

- The custom `Screen` is the bulk of the work and the main porting cost: the client GUI
  API changed across versions (`GuiGraphics` 1.20+ vs `PoseStack` + `ItemRenderer`
  earlier; `mouseScrolled`/`renderBackground` signatures). Build master first, then port.
- 1.16.5 is the hardest GUI port (oldest API, `PoseStack`, manual item rendering).
- Registry/tag API for the catalog + `PathRules` differs across versions
  (`BuiltInRegistries` vs `Registry`, `TagKey` vs `Tag`) — small, isolated per branch.
- Category coverage: id-name bucketing can misfile an oddly-named block into "Other";
  acceptable (still toggleable), and easy to refine by adding a rule.
