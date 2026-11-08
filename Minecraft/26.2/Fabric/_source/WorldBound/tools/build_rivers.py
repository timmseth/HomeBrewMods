#!/usr/bin/env python3
"""Build rivers.bin — a spatially indexed set of river segments the mod queries per block column.

    python build_rivers.py --gdb raw/HydroRIVERS_v10.gdb.zip --out "<profile>/worldbound/rivers.bin" [--min-q 5]

Source: HydroRIVERS v1.0 (Lehner & Grill 2013, hydrosheds.org), every river reach with long-term mean
discharge DIS_AV_CMS. Kept: reaches with discharge >= --min-q m^3/s (5 -> ~1.2M reaches, narrowest ~16 m).

Width and depth from discharge (Andreadis et al. 2013 global hydraulic geometry):
    width_m = 7.2 * Q^0.50        depth_m = 0.27 * Q^0.39

Two steps so each fits in one ~3-minute device-VM call:
    --stage read    : read the geodatabase -> ~/rivers_raw.npz
    --stage index   : npz -> rivers.bin
(default: both)

rivers.bin (little endian), mirrors mod/.../data/RiverIndex.java:
    magic "WBRV" | u32 version=1 | f32 cell_deg | u32 cols | u32 rows | u32 pad
    i64 offsets[cols*rows + 1]          record index where each cell starts (row-major, row 0 = north)
    records: f32 lon1, lat1, lon2, lat2 | u16 width_m | u16 depth_dm      (20 bytes)
A segment is stored in every cell its bbox (grown by half its width) touches; within a cell records are
sorted widest first, so map rendering can stop early.
"""
import argparse
import os
import struct
import time

import numpy as np

CELL = 0.25
COLS, ROWS = int(360 / CELL), int(180 / CELL)
RAW = os.path.expanduser("~/rivers_raw.npz")


def stage_read(gdb, min_q):
    import shapely
    from pyogrio.raw import read
    path = gdb if gdb.startswith("/vsi") else ("/vsizip/" + os.path.abspath(gdb) + "/HydroRIVERS_v10.gdb" if gdb.endswith(".zip") else gdb)
    t = time.time()
    _, _, geom, fields = read(path, columns=["DIS_AV_CMS"], where=f"DIS_AV_CMS >= {min_q}")
    g = shapely.from_wkb(geom)
    parts, pidx = shapely.get_parts(g, return_index=True)      # keep multi-part reaches from joining up
    coords, vidx = shapely.get_coordinates(parts, return_index=True)
    q = fields[0].astype(np.float32)[pidx]                      # discharge per part
    np.savez(RAW, coords=coords.astype(np.float32), idx=vidx.astype(np.int32), q=q)
    print(f"read {len(g)} reaches, {len(parts)} parts, {len(coords)} vertices in {time.time() - t:.0f}s -> {RAW}")


def stage_index(out):
    t = time.time()
    d = np.load(RAW)
    coords, idx, q = d["coords"], d["idx"], d["q"]
    same = idx[1:] == idx[:-1]                                 # consecutive vertices of the same line
    a, b = coords[:-1][same], coords[1:][same]
    qq = q[idx[:-1][same]]
    keep = np.abs(b[:, 0] - a[:, 0]) < 180                      # drop the odd antimeridian-crossing segment
    a, b, qq = a[keep], b[keep], qq[keep]
    width = np.clip(7.2 * np.sqrt(qq), 1, 65535)
    depth_dm = np.clip(10 * 0.27 * np.power(qq, 0.39), 1, 65535)
    n = len(a)
    # cell span of each segment's bbox, grown by half the width
    lat_mid = (a[:, 1] + b[:, 1]) / 2
    hw_lat = width / 2 / 110574.0
    hw_lon = width / 2 / (111320.0 * np.maximum(np.cos(np.radians(lat_mid)), 0.05))
    lon0 = np.minimum(a[:, 0], b[:, 0]) - hw_lon
    lon1 = np.maximum(a[:, 0], b[:, 0]) + hw_lon
    lat0 = np.minimum(a[:, 1], b[:, 1]) - hw_lat
    lat1 = np.maximum(a[:, 1], b[:, 1]) + hw_lat
    c0 = np.clip(np.floor((lon0 + 180) / CELL), 0, COLS - 1).astype(np.int64)
    c1 = np.clip(np.floor((lon1 + 180) / CELL), 0, COLS - 1).astype(np.int64)
    r0 = np.clip(np.floor((90 - lat1) / CELL), 0, ROWS - 1).astype(np.int64)
    r1 = np.clip(np.floor((90 - lat0) / CELL), 0, ROWS - 1).astype(np.int64)
    nc = c1 - c0 + 1
    nr = r1 - r0 + 1
    cnt = nc * nr
    seg = np.repeat(np.arange(n), cnt)                          # one entry per (segment, cell)
    k = np.arange(len(seg)) - np.repeat(np.cumsum(cnt) - cnt, cnt)
    cell = (r0[seg] + k // nc[seg]) * COLS + (c0[seg] + k % nc[seg])
    order = np.lexsort((-width[seg], cell))                     # by cell, widest first
    seg, cell = seg[order], cell[order]
    offsets = np.zeros(COLS * ROWS + 1, dtype=np.int64)
    np.add.at(offsets, cell + 1, 1)
    offsets = np.cumsum(offsets)
    rec = np.zeros(len(seg), dtype=[("x1", "<f4"), ("y1", "<f4"), ("x2", "<f4"), ("y2", "<f4"), ("w", "<u2"), ("d", "<u2")])
    rec["x1"], rec["y1"] = a[seg, 0], a[seg, 1]
    rec["x2"], rec["y2"] = b[seg, 0], b[seg, 1]
    rec["w"] = width[seg].astype(np.uint16)
    rec["d"] = depth_dm[seg].astype(np.uint16)
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    tmp = out + ".part"
    with open(tmp, "wb") as f:
        f.write(struct.pack("<4sIfIII", b"WBRV", 1, CELL, COLS, ROWS, 0))
        f.write(offsets.astype("<i8").tobytes())
        f.write(rec.tobytes())
    os.replace(tmp, out)
    print(f"{n} segments, {len(seg)} cell entries, {os.path.getsize(out) / 1e6:.0f} MB -> {out} in {time.time() - t:.0f}s")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--gdb", default="raw/HydroRIVERS_v10.gdb.zip")
    ap.add_argument("--out", required=True)
    ap.add_argument("--min-q", type=float, default=5.0)
    ap.add_argument("--stage", choices=["read", "index", "all"], default="all")
    a = ap.parse_args()
    if a.stage in ("read", "all"):
        stage_read(a.gdb, a.min_q)
    if a.stage in ("index", "all"):
        stage_index(a.out)


if __name__ == "__main__":
    main()
