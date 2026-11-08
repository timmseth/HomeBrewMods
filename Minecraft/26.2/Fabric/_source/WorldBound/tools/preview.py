#!/usr/bin/env python3
"""Render a layer from a tile root to a PNG so we can eyeball the data before Minecraft does.

    python preview.py <tile_root> <layer> <zoom> <out.png> [--bbox W S E N] [--max 4096]

Layer colours: climate = BIOME_TABLE.md legend, cover = WorldCover legend,
elev = hypsometric tint above sea level, blue ramp below. Missing tiles render black.
"""
import argparse
import os
import numpy as np
from PIL import Image

import etile
from tiling import TILE, NODATA, deg_per_tile, tiles_covering, tile_bounds
import biome_table


def elev_rgb(v: np.ndarray) -> np.ndarray:
    out = np.zeros(v.shape + (3,), dtype=np.uint8)
    v = v.astype(np.float32)
    nod = v == NODATA["elev"]
    sea = (v <= 0) & ~nod
    land = (v > 0) & ~nod
    d = np.clip(-v[sea] / 6000.0, 0, 1)
    out[sea] = np.stack([(30 + 60 * (1 - d)), (80 + 120 * (1 - d)), (160 + 80 * (1 - d))], axis=-1).astype(np.uint8)
    h = v[land]
    t = np.clip(h / 4500.0, 0, 1)
    r = np.interp(t, [0, 0.15, 0.4, 0.7, 1.0], [70, 150, 200, 160, 255])
    g = np.interp(t, [0, 0.15, 0.4, 0.7, 1.0], [140, 190, 170, 110, 255])
    b = np.interp(t, [0, 0.15, 0.4, 0.7, 1.0], [60, 90, 90, 80, 255])
    out[land] = np.stack([r, g, b], axis=-1).astype(np.uint8)
    return out


def palette_rgb(v: np.ndarray, colors: dict) -> np.ndarray:
    lut = np.zeros((256, 3), dtype=np.uint8)
    for k, c in colors.items():
        if 0 <= k < 256:
            lut[k] = c
    return lut[v.astype(np.uint8)]


def render(root, layer, z, bbox=None, max_px=4096):
    if bbox is None:
        bbox = (-180, -90, 180, 90)
    tiles = tiles_covering(z, *bbox)
    txs = sorted({t[0] for t in tiles}); tys = sorted({t[1] for t in tiles})
    W = len(txs) * TILE; H = len(tys) * TILE
    scale = max(1, int(np.ceil(max(W, H) / max_px)))
    w = W // scale; h = H // scale
    canvas = np.full((len(tys) * (TILE // scale), len(txs) * (TILE // scale), 3), 0, dtype=np.uint8)
    if layer == "climate":
        colors = {k: v["rgb"] for k, v in biome_table.load().items()}
    elif layer == "cover":
        colors = biome_table.COVER_COLORS
    missing = 0
    for tx, ty in tiles:
        p = etile.tile_path(root, layer, z, tx, ty)
        if not os.path.exists(p):
            missing += 1
            continue
        _, arr = etile.read(p)
        if scale > 1:
            arr = arr[::scale, ::scale]
        rgb = elev_rgb(arr) if layer == "elev" else palette_rgb(arr, colors)
        y0 = tys.index(ty) * (TILE // scale); x0 = txs.index(tx) * (TILE // scale)
        canvas[y0:y0 + rgb.shape[0], x0:x0 + rgb.shape[1]] = rgb
    return canvas, missing, len(tiles)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("root"); ap.add_argument("layer", choices=["climate", "elev", "cover"])
    ap.add_argument("zoom", type=int); ap.add_argument("out")
    ap.add_argument("--bbox", nargs=4, type=float, metavar=("W", "S", "E", "N"))
    ap.add_argument("--max", type=int, default=4096)
    a = ap.parse_args()
    img, missing, total = render(a.root, a.layer, a.zoom, a.bbox, a.max)
    Image.fromarray(img).save(a.out)
    print(f"wrote {a.out} ({img.shape[1]}x{img.shape[0]}); {total - missing}/{total} tiles present")


if __name__ == "__main__":
    main()
