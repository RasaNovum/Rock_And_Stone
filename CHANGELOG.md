# Rock and Stone 0.2.0 Changelog:

### Features:

- Vanilla ore removal through BiomeModifications.
- Addition of "filtered" ores that are remapped using the modified noise filter to place only in regions matching specific climate/environment conditions.
- Single chunk and radius ore scan commands to check distribution of ores.
- Advancements (default silent) on discovering conditions in which certain ores spawn.
- Noise filter values stored entirely in json files (in worldgen/placed_feature), replace values with whatever values suit the worldgen you are using and/or restrictions you want.

### Changes:

- updated noise filter values for coal upper, iron upper, iron small in accordance with testing observations
- Moved debug features behind config boolean

### Notes:

- N/A