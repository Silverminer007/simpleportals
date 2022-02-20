# Simple Portals Reloaded
 A Minecraft mod that adds portals for easy intra- and interdimensional travel. This is a fork of https://github.com/Zarathul/simpleportals

# SimplePortals (SP)

SimplePortals (SP) is a minecraft mod that adds constructable portals that allow intra- and interdimensional travel.
# Design

This is supposed to serve as a rough overview of the mods inner workings.

All portal related data is stored in a central registry. This registry is saved and loaded utilizing world save data. This way there is no need for the usual setup of other mods that use some sort of tile entity backed controller block.

# Build from source

 -   Run ```git clone https://github.com/Silverminer007/simpleportals.git```
 -   Change Directory ```cd simpleportals```
 -   Build ```./gradlew build``` or on Windows ```gradlew build```
 -   You may have to grant access to the gradlew script on Unix System ```chmod +x ./gradlew```
