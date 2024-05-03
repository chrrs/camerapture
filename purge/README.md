This folder contains two Python script to help clear up some space in your world, by finding and deleting all pictures that have no alive references in the world anymore. It does this by checking all block entities, entities and players and searching their NBT for picture items, and extracting their UUID's.

0. Before starting, make sure to install all the requirements listed in `requirements.txt`.

1. `find_uuids.py`: Scans the `world` folder adjacent to the script and produces a file called `uuids.txt`, which lists all picture UUID's currently in use.
2. `purge_pictures.py`: Deletes all pictures from the `world/camerapture` folder which are *not* listed in `uuids.txt`.

**NOTE:** Use this at your own risk! Pictures not found by any of the above methods are deleted, so while mod compatability is generally good, it is not guaranteed. Please make backups!