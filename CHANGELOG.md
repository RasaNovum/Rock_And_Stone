# Rock and Stone 0.6.1-d-4 Changelog:

## Changes:

- Rebalanced the default coal, iron, copper, gold, and lapis terrain filters.
- Added an additive gold placer for high-erosion inland terrain.

### Features:

- Vanilla + Modded ore removal through BiomeModifications.
- Addition of "filtered" ores that are remapped using the modified noise filter to place only in regions matching specific climate/environment conditions.
- Single chunk and radius ore scan commands to check distribution of ores.
- Advancements on discovering conditions in which certain ores spawn with an optional datapack to change the advancements to have no display properites, such that they are simply a background utility.
- Noise filter values stored entirely in json files (in worldgen/placed_feature), replace values with whatever values suit the worldgen you are using and/or restrictions you want.
- Filters can be made effectively unrestricted by using sufficiently wide min/max bounds such as `-8.0` to `8.0`.
- Multiversion support through Stonecutter

### Notes:

- N/A
