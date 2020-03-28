Welcome to the Compact Ores configuration!

You will find instructions for performing several tasks in the Compact Ores configuration here.

These instructions refer to the default configuration. If the configuration was changed by someone,
some things might be different!

Also remember that you need to restart the game for any configuration changes to take effect!


Changing Ore Textures:
    Your best bet is to do this with a resource pack or a mod like ResourceLoader.
    You can manually overwrite all Compact Ore textures by adding new textures with these names:
    assets/compactores/textures/<ore_resource_name>.png
    You can find the value for <ore_resource_name> by looking at an ore block in-game and opening the F3 menu.
    On the right side of the F3 menu, under the heading Targeted Block, there is a line starting with ore:
    Replace <ore_resource_name> with the entire string after ore: . Example:
    assets/compactores/textures/minecraft__coal_ore.png
    Note that the textures are not located in textures/block/ like most other textures, but are directly
    in the textures/ directory.

    If you have chosen this option and are comfortable in dealing with complex configurations, you may also want to
    take a look at the section 'Defining Ores / Changing Ore Definitions' to disable the auto-generated texture
    (speeds up resource loading and therefore startup time).

    If you instead want to change the parameters which are used for the auto-generated textures, check out the
    'Defining Ores / Changing Ore Definitions' section of this file.


Customizing Rarity And Drops:
    These parameters are specified in the so-called customization configs.
    You can find them in the customizations directory.

    In the customizations directory, there is a file called _global.toml.
    In that file, there is a ["!global"] section. It contains settings that apply to all ores that no specific
    settings are specified for. The defaults for these values are:
    minRolls = 3                 # Every compact ore will drop at least 3x as much as a normal ore
    maxRolls = 5                 # Every compact ore will drop at most 5x as much as a normal ore
    spawnProbability = 0.1       # A newly generated ore will have a 10% chance of being a compact ore

    You can also change these three values for specific ores. To do that, find the file which correlates with the
    mod which adds the ore that you want to change (vanilla ores are in minecraft.toml).
    In that file, you will be able to find a section corresponding to the ore that you want to change.
    Simply place the options below the section headings (=lines with square brackets around them) to apply them.

    You can also change options for all ores which have a section in a file by adding an additional ["!local"]
    heading to the file and placing your options below that.


Defining Ores / Changing Ore Definitions:
    Ore definitions are stored in the definitions directory.
    By default, there is a _global.toml file for global settings as well as a further file for each supported mod,
    named after the modid of the mod.
    When editing these files, please note that there is an extended resource location syntax for specifying block and
    texture names:
     -             example_resource   =>     namespace = minecraft                   name = example_resource
     -            :example_resource   =>     namespace = filename (without .toml)    name = example_resource
     - example_mod:example_resource   =>     namespace = example_mod                 name = example_resource

    To define an ore, add a new top-level section to any file which is in the definitions directory, but not a
    subdirectory of it.
    The name of the section should be a resource location which resolves to the namespaced block id of the ore block.

    A local block can also be added to any file. The options in the local block will apply to all ores that are
    defined in the same file as the local block is located. The local block section's name is "!local".

    When defining a new ore, please make sure to also add a corresponding section heading to the customization file
    to make sure that local customization settings apply and to help users customize the ore more easily.

    The following options exist for defining an ore:
     - generateTexture
           Type: boolean
           Required: no
           Default: true
           Allowed scopes: global, local, ore
           Function: Can be used to disable texture generation for an ore to increase resource loading speed.
                     Use if the generated texture is overridden by a texture from a resource pack anyway.
     - maxOreLayerColorDiff
           Type: int
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


Happy configuring!
