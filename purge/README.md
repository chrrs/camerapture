## Camerapture purge scripts

This folder contains two Python scripts to help clear up some space in your world by finding and deleting all pictures
that have no alive references in the world. It does this by checking all block entities, entities and players,
searching their NBT for picture items, and extracting their UUID's.

1. Before starting, make sure to install all the requirements listed in `requirements.txt`.

2. `find_uuids.py`: Scans the `world` folder adjacent to the script and produces a file called `uuids.txt`, which lists
   all picture UUID's currently in use.

3. `purge_pictures.py`: Deletes all pictures from the `world/camerapture` folder which are *not* listed in `uuids.txt`.

**NOTE 1:** The scripts scan the NBT files in the `entities` and `regions` folders of `world`, `world/DIM1`,
`world/DIM-1`, and the NBT files in the `world/playerdata` folder. This means that *any items stored outside of these
folders will not be scanned!* This shouldn't happen in Vanilla worlds, but some mods will store data in other locations.

**NOTE 2:** Use this at your own risk! Pictures not found by any of the above methods are deleted, so while mod
compatability is generally good, it is not guaranteed. Please make backups!