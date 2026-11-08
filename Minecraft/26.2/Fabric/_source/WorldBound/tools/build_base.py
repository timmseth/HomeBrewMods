#!/usr/bin/env python3
"""Build the coarse global data: the jar base (z3) and the optional global pack (z6/z7).

    python build_base.py --koppen koppen_geiger_0p00833333.tif --gebco "gebco_2024_*.tif" \
        --jar-base ../mod/src/main/resources/worldbound/base \
        --global  "<instance>/worldbound/packs/global"

Köppen:  https://www.gloh2o.org/koppen/  (Beck et al. 2023; use the 1991-2020 0.00833 deg map)
GEBCO:   https://www.gebco.net/  (GEBCO_2024 grid as GeoTIFF tiles or a single netCDF; any CRS=EPSG:4326 raster works)

Either output can be omitted. Expect ~15-20 MB for the jar base and ~2 GB for the global pack.
"""
import argparse
import glob
import json
import os

from rasterize import rasterize

WORLD = (-180.0, -90.0, 180.0, 90.0)


def expand(patterns):
    out = []
    for p in patterns:
        if p.startswith("/vsi"):          # GDAL virtual path (e.g. /vsizip/.../gebco.zip/tile.tif)
            out.append(p)
            continue
        hits = sorted(glob.glob(p))
        if not hits:
            raise SystemExit(f"no files match {p}")
        out.extend(hits)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--koppen", nargs="+", required=True, help="Köppen-Geiger GeoTIFF(s)")
    ap.add_argument("--gebco", nargs="+", required=True, help="GEBCO GeoTIFF(s) or NETCDF:path:elevation")
    ap.add_argument("--jar-base", help="output dir for the z3 jar base")
    ap.add_argument("--global", dest="global_dir", help="output dir for the z6/z7 global pack")
    ap.add_argument("--bbox", nargs=4, type=float, default=WORLD, metavar=("W", "S", "E", "N"),
                    help="limit to a bbox (for quick tests)")
    a = ap.parse_args()
    koppen = expand(a.koppen)
    gebco = expand(a.gebco) if not a.gebco[0].startswith("NETCDF:") else a.gebco
    bbox = tuple(a.bbox)

    if a.jar_base:
        print(f"== jar base -> {a.jar_base}")
        n1 = rasterize("climate", koppen, 3, bbox, a.jar_base)
        n2 = rasterize("elev", gebco, 3, bbox, a.jar_base)
        with open(os.path.join(a.jar_base, "pack.json"), "w") as f:
            json.dump({"name": "base", "bounds": list(bbox), "layers": {"climate": 3, "elev": 3}}, f, indent=2)
        print(f"   climate {n1} tiles, elev {n2} tiles")

    if a.global_dir:
        print(f"== global pack -> {a.global_dir}")
        n1 = rasterize("climate", koppen, 6, bbox, a.global_dir)
        n2 = rasterize("elev", gebco, 7, bbox, a.global_dir)
        with open(os.path.join(a.global_dir, "pack.json"), "w") as f:
            json.dump({"name": "global", "bounds": list(bbox), "layers": {"climate": 6, "elev": 7}}, f, indent=2)
        print(f"   climate {n1} tiles, elev {n2} tiles")


if __name__ == "__main__":
    main()
