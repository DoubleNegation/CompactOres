# Compact Ores
CompactOres is a Minecraft mod that adds ores which yield extra resources.  
This mod is heavily inspired by [RWTema's Dense Ores](https://www.curseforge.com/minecraft/mc-mods/dense-ores).
## Features
A compact ore is a block which mimics an ore from Minecraft or another mod. It has a different (automatically generated) texture from the ore block which it's based on, and drops several times more loot when broken.  
Compact ores can be heavily configured. For configuration instructions, please see the [configuration readme](src/main/resources/assets/compactores/default_config/config_readme.txt).
## Workspace setup
Setting up the mod workspace involves importing `build.gradle` as a project into your IDE.  
When importing, it will look for a "vendor name" in `~/.config/.VENDOR_NAME`. If this name cannot be found, the import
will fail. You either have to make sure this file exists or hardcode a vendor name in the `build.gradle` file (in `jar`
→ `manifest` → `attributes`) before importing the project.
## Build Instructions
Follow the same instructions concerning the "vendor name" as in _Workspace setup_. Then run the appropriate build
command for your platform:  
Windows: `gradlew.bat build`  
Unix/Linux: `./gradlew build`  
The resulting mod jar will be placed in `build/libs/`
