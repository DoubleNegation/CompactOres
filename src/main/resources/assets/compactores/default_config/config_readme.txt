Welcome to the Compact Ores configuration!

This is the configuration readme for Compact Ores version ${VERSION}.
You will find instructions for performing several tasks in the Compact Ores configuration here.

These instructions refer to the default configuration. If the configuration was changed by someone,
some things might be different!

Also remember that you need to restart the game for any configuration changes to take effect!


Changing Ore Textures:
    Your best bet is to do this with a resource pack or a mod like ResourceLoader.
    You can manually overwrite all Compact Ore textures by adding new textures with these names:
    assets/compactores/textures/<ore_block_name>.png
    You can find the value for <ore_block_name> by looking at an ore block in-game and opening the F3 menu.
    On the right side of the F3 menu, the first line under the heading "Targeted Block" shows the ore name
    (which starts with compactores:compactore__)
    Replace <ore_block_name> with the part of the ore name after compactores: . Example:
    assets/compactores/textures/compactore__minecraft__coal_ore.png
    Note that the textures are not located in textures/block/ like most other textures, but are directly
    in the textures/ directory.

    If you have chosen this option and are comfortable in dealing with complex configurations, you may also want to
    keep reading to find out how to disable the auto-generated texture (speeds up resource loading and therefore
    startup time).


Customizing, Adding and Removing Ores:
    By default, there is a _global.toml file for global settings as well as a further file for each supported mod,
    named after the modid of the mod.
    When editing these files, please note that there is an extended resource location syntax for specifying block and
    texture names:
     -             example_resource   =>     namespace = minecraft                   name = example_resource
     -            :example_resource   =>     namespace = filename (without .toml)    name = example_resource
     - example_mod:example_resource   =>     namespace = example_mod                 name = example_resource

    To customize an ore, open the file corresponding to the ore's mod. The look for the section which has the
    ore's block id as its heading. Change the options in the section to your liking.

    To define an ore, add a new top-level section to any file which is in the config directory (except this one),
    but not a subdirectory of it.
    The name of the section should be a resource location which resolves to the namespaced block id of the ore block.

    A local block can also be added to any file. The options in the local block will apply to all ores that are
    defined in the same file as the local block is located. The local block section's name is "!local".

    The following options exist for defining and customizing an ore:
     - minRolls
           Type: integer
           Required: no
           Default: 3
           Allowed scopes: global, local, ore
           Function: The minimum drop multiplier when a compact ore is broken.
                     A compact ore will drop at least minRolls times as much as the normal ore.
     - maxRolls
           Type: integer
           Required: no
           Default: 5
           Allowed scopes: global, local, ore
           Function: The maximum drop multiplier when a compact ore is broken.
                     A compact ore will drop at most maxRolls times as much as the normal ore.
     - spawnProbability
           Type: decimal number
           Required: no
           Default: 0.1
           Allowed scopes: global, local, ore
           Function: The likelihood that any ore block which is generated will be a compact ore.
                     Range: 0 (=0%) to 1 (=100%)
                     CAUTION: The decimal point is required for this option to work. If you want to set it to 1,
                              use 1.0 instead of just 1
     - generateTexture
           Type: boolean
           Required: no
           Default: true
           Allowed scopes: global, local, ore
           Function: Can be used to disable texture generation for an ore to increase resource loading speed.
                     Use if the generated texture is overridden by a texture from a resource pack anyway.
     - maxOreLayerColorDiff
           Type: integer
           Required: no
           Default: 50
           Allowed scopes: global, local, ore
           Function: Relevant for texture generation. Changes the threshold for differentiating between
                     rock-pixels and ore-pixels on the ore texture.
     - oreTexture
           Type: resource location
           Required: no
           Default: N/A
           Allowed scopes: local, ore
           Function: Relevant for texture generation. If not set, no texture will be generated.
                     Specifies the name of the ore texture. <modid>:<texture> will be loaded as
                     assets/<modid>/textures/<texture>.png. Example for coal ore: minecraft:block/coal_ore
     - rockTexture
           Type: resource location
           Required: no
           Default: N/A
           Allowed scopes: local, ore
           Function: Relevant for texture generation. If not set, no texture will be generated.
                     Specifies the name of the texture of the rock in which the ore is embedded,
                     e.g. minecraft:block/stone for coal ore, minecraft:block/netherrack for nether quartz ore
                     Resolved like oreTexture.
     - lateGeneration
           Type: boolean
           Required: no
           Default: false
           Allowed scopes: local, ore
           Function: When enabled, generates compact ores of this ore block during a later stage of world generation
                     than usual. This can be required for some compact ores to generate at all.
                     However, it should be enabled with caution, as it causes the compact ore to not generate at all
                     in certain circumstances (like a mining dimension)
     - useGetDrops
           Type: boolean
           Required: no
           Default: false
           Allowed scopes: local, ore
           Function: When enabled, changes how compact ore drops are generated. Enable this only if a compact ore
                     does not produce any drops without this option.


Happy configuring!
