# Compact Ores
CompactOres is a Minecraft mod that adds ores which yield extra resources.  
This mod is heavily inspired by [RWTema's Dense Ores](https://www.curseforge.com/minecraft/mc-mods/dense-ores).
## Features
A compact ore is a block which mimics an ore from Minecraft or another mod. It has a different (automatically generated) texture from the ore block which it's based on, and drops several times more loot when broken.  
Compact Ores are defined in a configuration file located in `.minecraft/config/compactores.toml`. The configuration file has the following global options in it's `[global]` section:
 * `compactOreProbability`: The likelihood that a newly generated ore block is replaced by a compact ore block
 * `minRolls`/`maxRolls`: range from which the drop multiplier will be chosen when breaking a compact ore


Compact ores can be defined by creating a new section in the configuration file for them. The name of the section needs to match the registry name of the base block, for example `["minecraft:coal_ore"]`. The compact ore definition supports the following parameters:
 * `oreTexture`: resource location of the texture of the base ore block, for example `"minecraft:block/coal_ore"`
 * `rockTexture`: resource location of the texture which is "behind" the texture of the base block. This is needed for generating the compact ore texture. Note that sometimes, a seemingly matching rock texture may not perfectly match the background of the ore texture and may therefore lead to unexpected results.
 * `compactOreProbaility`: _(optional)_ Overrides the equivalent global value for this ore
 * `minRolls`/`maxRolls`: _(optional)_ Overrides the equivalent global values for this ore
 * `useGetDrops`: _(optional)_ use an alternative method to find out the drops of the base ore block (default = `false`)

For more examples, see the [default configuration](src/main/resources/assets/compactores/default_config.toml).
