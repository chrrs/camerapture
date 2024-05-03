import os

base_dir = "world"

uuids: set[str] = set()
with open("uuids.txt", "r") as f:
    for uuid in f.readlines():
        uuid = uuid.strip()
        if uuid:
            uuids.add(uuid)

camerapture_dir = os.path.join(base_dir, "camerapture")
for file in os.listdir(camerapture_dir):
    if file.endswith(".webp"):
        file_uuid = file.split(".")[0]
        if file_uuid not in uuids:
            os.remove(os.path.join(camerapture_dir, file))
            print(f"deleted {file}")