#!/usr/bin/env python3
"""Rasterize HydroLAKES into two packs the mod reads: lake surface elevation (i16 m) and mean depth (u8 m).

    python build_lakes.py --gdb ~/lakes/HydroLAKES_polys_v10.gdb --out "<profile>/worldbound/packs" [--budget 150]

Writes packs/lakes (z10, ~38 m pixels, for world generation) and packs/lakes_coarse (z7, for the map).
Resumable: work goes block by block (z5 tiles, 11.25 deg) and tile by tile; finished tiles are listed in
<pack>/.done, so run it repeatedly until it prints "all done" (the device VM kills calls after ~3 min).

Source: HydroLAKES v1.0 (Messager et al. 2016, hydrosheds.org): 1.43 M lakes >= 10 ha with surface
elevation ("Elevation", m) and average depth ("Depth_avg", m).
"""
import argparse
import json
import os
import time

import numpy as np
import shapely
from pyogrio.raw import read
from rasterio.features import rasterize
from rasterio.transform import from_origin

import etile
from tiling import TILE, deg_per_px, tile_bounds, tiles_covering

PACKS = {"lakes": 10, "lakes_coarse": 7}
BLOCK_Z = 5


def load_block(gdb, b):
    w, s, e, n = b
    meta, _, geom, fields = read(gdb, columns=["Elevation", "Depth_avg"], bbox=(w - 0.01, s - 0.01, e + 0.01, n + 0.01))
    if geom is None or len(geom) == 0:
        return None
    names = list(meta["fields"])
    elev = np.asarray(fields[names.index("Elevation")], dtype=np.float32)
    depth = np.asarray(fields[names.index("Depth_avg")], dtype=np.float32)
    g = shapely.from_wkb(geom)
    return g, elev, depth, shapely.STRtree(g)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--gdb", required=True)
    ap.add_argument("--out", required=True, help="the packs directory")
    ap.add_argument("--budget", type=float, default=150, help="seconds before stopping (resume later)")
    a = ap.parse_args()
    t0 = time.time()
    roots = {name: os.path.join(a.out, name) for name in PACKS}
    done = {}
    for name, root in roots.items():
        os.makedirs(root, exist_ok=True)
        with open(os.path.join(root, "pack.json"), "w") as f:
            json.dump({"name": name, "bounds": [-180, -90, 180, 90], "layers": {"lake": PACKS[name], "lakedepth": PACKS[name]}}, f, indent=2)
        p = os.path.join(root, ".done")
        done[name] = set(open(p).read().split()) if os.path.exists(p) else set()
    logs = {name: open(os.path.join(root, ".done"), "a") for name, root in roots.items()}
    blocks_path = os.path.join(a.out, ".lakes_blocks_done")
    blocks_done = set(open(blocks_path).read().split()) if os.path.exists(blocks_path) else set()
    blog = open(blocks_path, "a")
    written = 0
    for bx, by in tiles_covering(BLOCK_Z, -180, -90, 180, 90):
        key = f"{bx}_{by}"
        if key in blocks_done:
            continue
        if time.time() - t0 > a.budget:
            print(f"budget reached; {written} tiles written this run, {len(blocks_done)} blocks done")
            return
        bb = tile_bounds(BLOCK_Z, bx, by)
        data = load_block(a.gdb, bb)
        if data is not None:
            g, elev, depth, tree = data
            for name, z in PACKS.items():
                res = deg_per_px(z)
                for tx, ty in tiles_covering(z, bb[0] + 1e-9, bb[1] + 1e-9, bb[2] - 1e-9, bb[3] - 1e-9):
                    tk = f"{tx}_{ty}"
                    if tk in done[name]:
                        continue
                    if time.time() - t0 > a.budget:
                        print(f"budget reached mid-block {key}; {written} tiles written this run")
                        return
                    tb = tile_bounds(z, tx, ty)
                    hits = tree.query(shapely.box(*tb))
                    if len(hits):
                        idx = rasterize(((g[i], int(i) + 1) for i in hits), out_shape=(TILE, TILE),
                                        transform=from_origin(tb[0], tb[3], res, res), fill=0, dtype="int32")
                        if idx.any():
                            m = idx > 0
                            lake = np.full((TILE, TILE), -32768, dtype="<i2")
                            dep = np.zeros((TILE, TILE), dtype="u1")
                            lake[m] = np.clip(np.rint(elev[idx[m] - 1]), -1000, 9000)
                            dep[m] = np.clip(np.rint(depth[idx[m] - 1]), 1, 255)
                            etile.write(roots[name], "lake", z, tx, ty, lake)
                            etile.write(roots[name], "lakedepth", z, tx, ty, dep)
                            written += 1
                    logs[name].write(tk + "\n")
                    logs[name].flush()
        blog.write(key + "\n")
        blog.flush()
        blocks_done.add(key)
    print(f"all done; {written} tiles written this run in {time.time() - t0:.0f}s")


if __name__ == "__main__":
    main()
