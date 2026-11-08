#!/usr/bin/env python3
"""Tree cover % from Copernicus Global Land Cover 100 m (CGLS-LC100 v3.0.1, 2019, Tree-CoverFraction layer).

    python build_treecover.py --tif raw/PROBAV_LC100_global_v3.0.1_2019-nrt_Tree-CoverFraction-layer_EPSG-4326.tif \
        --out "<profile>/worldbound/packs" [--budget 160]

Writes packs/treecover (z7, ~300 m; only under land z5 tiles of veg_coarse). Resumable (.scanned).
Classes kept as-is (u8): 20 shrub, 30 herbaceous, 40 cropland, 50 urban, 60 bare/sparse, 70 snow/ice,
80 water, 90 herbaceous wetland, 100 moss/lichen, 111-116 closed forest (evergreen needle, evergreen broad,
deciduous needle, deciduous broad, mixed, unknown), 121-126 open forest (same order). 200 (ocean) -> 0.
"""
import argparse
import json
import os
import time

import numpy as np

import rasterize as R
from tiling import tiles_covering

PACKS = {"treecover": 7}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--tif", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--budget", type=float, default=160)
    a = ap.parse_args()
    t0 = time.time()

    def fn(v):
        return np.where(v > 100, np.nan, v)

    for name, z in PACKS.items():
        root = os.path.join(a.out, name)
        os.makedirs(root, exist_ok=True)
        with open(os.path.join(root, "pack.json"), "w") as f:
            json.dump({"name": name, "bounds": [-180, -60, 180, 80], "layers": {"treecover": z}}, f, indent=2)
        left = a.budget - (time.time() - t0)
        if left <= 5:
            print("budget reached"); return
        skip = None
        if z > 5:
            croot = os.path.join(a.out, "veg_coarse", "layers", "veg", "5")
            land = {n[:-6] for n in os.listdir(croot) if n.endswith(".etile")}
            f = 1 << (z - 5)
            skip = lambda tx, ty: f"{tx // f}_{ty // f}" not in land
        n = R.rasterize("treecover", [a.tif], z, (-180, -60, 180, 80), root, value_fn=fn, src_nodata=255, log=None, budget=left, skip=skip)
        sc = os.path.join(root, "layers", "treecover", str(z), ".scanned")
        done = sum(1 for _ in open(sc)) if os.path.exists(sc) else 0
        total = len(tiles_covering(z, -180, -60, 180, 80))
        print(f"{name} z{z}: +{n} tiles this run, {done}/{total} scanned")
        if done < total:
            return
    print("all done")


if __name__ == "__main__":
    main()
