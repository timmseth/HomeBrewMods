"""Build WorldBound's Terralith compatibility pack: Earth noise settings whose surface rule is Terralith's,
re-anchored for WorldBound's sea level (y -700).

Terralith's licence does not allow its code to be redistributed in another mod, so this pack is NOT
shipped with WorldBound. Run this script against your own Terralith jar and add the output folder as a
data pack when creating a world. Keep it for your own use.

Anchors at or below vanilla sea level (63) shift by -763 (same as our vanilla rules); anchors above it are
stretched 3x (Earth land is ~3 m/block, vanilla terrain ~10 m/block) so Terralith's altitude bands land
at comparable real heights.

  python3 tools/terralith_compat.py <Terralith.jar> <worldbound earth.json> <out dir>
"""
import json, os, sys, zipfile

def remap(y):
    return y - 763 if y <= 63 else min(2000, -700 + (y - 63) * 3)

def walk(n):
    if isinstance(n, dict):
        if set(n.keys()) == {"absolute"}:
            return {"absolute": remap(n["absolute"])}
        return {k: walk(v) for k, v in n.items()}
    if isinstance(n, list):
        return [walk(v) for v in n]
    return n

def main(jar, earth_json, out):
    with zipfile.ZipFile(jar) as z:
        tl = json.loads(z.read("data/minecraft/worldgen/noise_settings/overworld.json"))
        mcmeta = json.loads(z.read("pack.mcmeta"))
    earth = json.load(open(earth_json))
    earth["surface_rule"] = walk(tl["surface_rule"])
    d = os.path.join(out, "data", "worldbound", "worldgen", "noise_settings")
    os.makedirs(d, exist_ok=True)
    json.dump(earth, open(os.path.join(d, "earth.json"), "w"), indent=1)
    pk = mcmeta["pack"]
    pk.pop("id", None)
    pk["description"] = "WorldBound: Terralith ground blocks in Earth worlds"
    json.dump({"pack": pk}, open(os.path.join(out, "pack.mcmeta"), "w"), indent=2)
    print("wrote", out)

if __name__ == "__main__":
    main(*sys.argv[1:4])
