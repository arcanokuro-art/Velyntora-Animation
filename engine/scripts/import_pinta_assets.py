#!/usr/bin/env python3
"""Import original graphic resources from the pinned Pinta source revision.

Copies PNG/SVG/ICO files byte-for-byte. Keeps original directory layout under
reference/pinta-assets to avoid Android resource naming collisions.
"""
import json
from pathlib import Path
from urllib.request import Request, urlopen

OWNER = "arcanokuro-art"
SOURCE = "Pinta-2.0"
COMMIT = "2cfea9cf1eae94e45e9d0ef98d76d9ccf4a98481"
BASE = Path(__file__).resolve().parents[1] / "reference" / "pinta-assets"
EXTENSIONS = {".png", ".svg", ".ico"}
PREFIX = "Pinta.Resources/"

def download(url):
    req = Request(url, headers={"User-Agent": "Velyntora-Core-asset-importer"})
    with urlopen(req, timeout=40) as response:
        return response.read()

def main():
    tree_url = f"https://api.github.com/repos/{OWNER}/{SOURCE}/git/trees/{COMMIT}?recursive=1"
    tree = json.loads(download(tree_url))
    if tree.get("truncated"):
        raise RuntimeError("Incomplete Git tree; refusing partial import")
    entries = [entry for entry in tree["tree"]
               if entry["type"] == "blob"
               and entry["path"].startswith(PREFIX)
               and Path(entry["path"]).suffix.lower() in EXTENSIONS]
    if not entries:
        raise RuntimeError("No graphic assets found")
    for entry in entries:
        path = entry["path"]
        destination = BASE / path[len(PREFIX):]
        destination.parent.mkdir(parents=True, exist_ok=True)
        source = f"https://raw.githubusercontent.com/{OWNER}/{SOURCE}/{COMMIT}/{path}"
        destination.write_bytes(download(source))
    print(f"Imported {len(entries)} original Pinta graphics into {BASE}")

if __name__ == "__main__":
    main()
