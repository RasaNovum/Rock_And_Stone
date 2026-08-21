# Rock and Stone 0.6.1-d-1 Changelog:

## Changes:
- Added optional Create RnS resource-node terrain filtering, using the active ore noise filters.
- Added the `doCreateRnsTerrainFiltering` config option; it is disabled by default to preserve existing Create RnS generation.

# Rock and Stone 0.6.0 Changelog:


### Features:

- Vanilla + Modded ore removal through BiomeModifications.
- Addition of "filtered" ores that are remapped using the modified noise filter to place only in regions matching specific climate/environment conditions.
- Single chunk and radius ore scan commands to check distribution of ores.
- Advancements on discovering conditions in which certain ores spawn with an optional datapack to change the advancements to have no display properites, such that they are simply a background utility.
- Noise filter values stored entirely in json files (in worldgen/placed_feature), replace values with whatever values suit the worldgen you are using and/or restrictions you want.
- Filters can be removed by setting the min/max values to an unrestricted `-2.0` to `2.0` noise bounds, preserving vanilla placement.
- Multiversion support through Stonecutter

### Changes:

- updated datapack getting to fix incompatibility with modded ore replacement
- added support for disabling ore replacement via config, that way you can compare generation without relaunching the game.
- added the `doSilentAdvancements` config option to automatically enable the built-in silent advancements datapack.
- updated lang file to feature
- Added Stonecutter builds for Fabric and NeoForge 1.21.1

### Notes:

- N/A
