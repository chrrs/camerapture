import anvil
from nbt import nbt
import os
import uuid

os.system('color')
RED = "\033[91m"
BLUE = "\033[94m"
RESET = "\033[0m"

base_dir = "world"

uuids: set[str] = set()

def error(s: str):
    print(f"{RED}{s}{RESET}")

def info(s: str):
    print(f"{BLUE}{s}{RESET}")

# Analyze entities and block entities for an entire world.
def analyze_world(dir: str):
    if not os.path.isdir(dir):
        return
    
    world = os.path.basename(dir)

    entities_dir = os.path.join(dir, "entities")
    if os.path.exists(entities_dir):
        entities_mca = os.listdir(entities_dir)

        for i, mca in enumerate(entities_mca):
            print(f":: analyzing {world}/entities: {mca} [{i}/{len(entities_mca)}]")
            analyze_mca(os.path.join(entities_dir, mca))
    
    region_dir = os.path.join(dir, "region")
    if os.path.exists(region_dir):
        region_mca = os.listdir(region_dir)

        for i, mca in enumerate(region_mca):
            print(f":: analyzing {world}/regions: {mca} [{i}/{len(region_mca)}]")
            analyze_mca(os.path.join(region_dir, mca))

# Analyze a single region file, looking through every chunk.
def analyze_mca(path: str):
    region = anvil.Region.from_file(path)

    # Some regions files seem to be empty.
    if len(region.data) == 0:
        return
    
    for cx in range(32):
        for cy in range(32):
            level = region.chunk_data(cx, cy)
            if level is None:
                continue

            analyze_chunk(level)

# Analyze a single chunk's NBT data.
# We look through both the 'block_entities' tag and the 'Entities' tag.
def analyze_chunk(level: nbt.TAG_Compound):
    if "block_entities" in level:
        for block_entity in level["block_entities"]:
            analyze_tag(block_entity)
    if "Entities" in level:
        for entity in level["Entities"]:
            analyze_tag(entity)

# Recursively analyze an NBT tag. We are looking for an ItemStack NBT tag
# with a 'camerapture:picture' item.
def analyze_tag(tag: nbt.TAG):
    if isinstance(tag, nbt.TAG_List):
        for item in tag:
            analyze_tag(item)
        return

    if not isinstance(tag, nbt.TAG_Compound):
        return
    
    check_tag(tag)
    
    for key in tag:
        analyze_tag(tag[key])

# Check if a compound NBT tag is a picture ItemStack, and if so, find it's UUID.
def check_tag(tag: nbt.TAG_Compound):
    if not "id" in tag:
        return
    
    id = tag["id"]
    if not isinstance(id, nbt.TAG_String):
        return
    
    if not id.value == "camerapture:picture":
        return
    
    if "tag" in tag:
        picture_tag = tag["tag"]
        if not "uuid" in picture_tag:
            error(f"found picture, but it doesn't have a UUID: {picture_tag}")
            return
        
        uuid_tag = picture_tag["uuid"]
        if isinstance(uuid_tag, nbt.TAG_Int_Array):
            bytes = b''
            for i in uuid_tag.value:
                bytes += i.to_bytes(4, byteorder='big', signed=True)
            id = str(uuid.UUID(bytes=bytes))

            info(f"found picture with UUID {id} (NBT)")
            uuids.add(id)
        else:
            error(f"found picture with an invalid UUID tag: {uuid_tag}")
    elif "components" in tag:
        if not "camerapture:picture_data" in tag["components"]:
            error(f"found picture, but it doesn't have a picture data: {tag}")
            return
        
        picture_data = tag["components"]["camerapture:picture_data"]
        uuid_tag = picture_data["id"]
        if isinstance(uuid_tag, nbt.TAG_String):
            id = str(uuid.UUID(hex=uuid_tag.value))

            info(f"found picture with UUID {id} (Data Component)")
            uuids.add(id)
        else:
            error(f"found picture with an invalid UUID tag: {uuid_tag}")
    else:
        error(f"found picture, but it doesn't have nbt: {tag}")
        return
    
    

# Analyze all playerdata files by analyzing all their NBT tags.
playerdata_dir = os.path.join(base_dir, "playerdata")
for file in os.listdir(playerdata_dir):
    if file.endswith(".dat_old"):
        continue

    print(f":: analyzing playerdata/{file}")
    tag = nbt.NBTFile(filename=os.path.join(playerdata_dir, file))
    analyze_tag(tag)

analyze_world(base_dir)
analyze_world(os.path.join(base_dir, "DIM-1"))
analyze_world(os.path.join(base_dir, "DIM1"))

with open("uuids.txt", "w") as f:
    f.write("\n".join(uuids))
info("done! result written to uuids.txt")