# PlaceOnPath

A small Minecraft mod that lets you place **any block on dirt path blocks** without the path turning into plain dirt.

## What it does

In vanilla Minecraft, placing a solid block (fence, pressure plate, etc.) on a dirt path turns the path back into dirt. And even for blocks that *can* be placed (torches, lanterns), the path block is slightly shorter than a full block, leaving an ugly gap underneath.

PlaceOnPath fixes both problems:

- ✅ Fences, walls and gates can be placed on paths
- ✅ Pressure plates can be placed on paths
- ✅ Torches and lanterns can be placed on paths
- ✅ Flower pots, signs and any other block can be placed on paths
- ✅ The path is **never** turned into dirt

When you place a block on a path, the path is swapped for a **full-height path block** that uses the normal path texture but has a full `1×1×1` shape, so there is no gap between the path and the block on top.

The full-height path block is purely technical:

- It has no item and cannot be obtained in the inventory.
- It drops dirt when broken, exactly like a normal dirt path.
- It automatically reverts to a normal dirt path as soon as the block resting on it is removed.

## Supported versions

| Minecraft | Loaders | Branch |
|-----------|---------|--------|
| 1.21.1 | Fabric, NeoForge | `master` |
| 1.21.4 | Fabric, NeoForge | `1.21.4` |
| 1.20.4 | Fabric, Forge, NeoForge | `1.20.4` |
| 1.20.1 | Fabric, Forge | `1.20.1` |
| 1.19.4 | Fabric, Forge | `1.19.4` |
| 1.19.2 | Fabric, Forge | `1.19.2` |
| 1.18.2 | Fabric, Forge | `1.18.2` |
| 1.16.5 | Fabric, Forge | `1.16.5` |

## Building

Requires JDK 21 (JDK 17 for 1.16.5–1.18.2 branches).

```bash
./gradlew build
```

The built jars are placed in `fabric/build/libs/`, `neoforge/build/libs/` and `forge/build/libs/`.

## License

MIT — see [LICENSE.txt](LICENSE.txt).
