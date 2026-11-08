#!/usr/bin/env python3
"""Build a region pack (30 m elevation + 10 m land cover) from AWS Open Data.

    python build_pack.py idaho -117.3 41.9 -110.9 49.1 --out "<profile>/worldbound/packs/idaho"

Downloads the Copernicus GLO-30 1x1 degree COGs and ESA WorldCover 3x3 degree tiles that cover
the bbox into tools/raw/ (kept, so re-runs are free), then writes elev at z10 and cover at z11.

Sources (public, no credentials; names drift — the script prints every URL it tries):
  Copernicus DEM GLO-30: https://copernicus-dem-30m.s3.amazonaws.com/  (tileList.txt at the root)
  ESA WorldCover 2021:   https://esa-worldcover.s3.eu-central-1.amazonaws.com/v200/2021/map/
"""
import argparse
import json
import math
import os
import sys
import urllib.error
import urllib.request

from rasterize import rasterize

HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(HERE, "raw")

COP_BASE = "https://copernicus-dem-30m.s3.amazonaws.com"
WC_BASE = "https://esa-worldcover.s3.eu-central-1.amazonaws.com/v200/2021/map"

ELEV_ZOOM = 10
COVER_ZOOM = 11


def _lat(v, digits):
    return ("N" if v >= 0 else "S") + f"{abs(v):0{digits}d}"


def _lon(v, digits):
    return ("E" if v >= 0 else "W") + f"{abs(v):0{digits}d}"


def copernicus_urls(w, s, e, n):
    for lat in range(math.floor(s), math.ceil(n)):
        for lon in range(math.floor(w), math.ceil(e)):
            name = f"Copernicus_DSM_COG_10_{_lat(lat, 2)}_00_{_lon(lon, 3)}_00_DEM"
            yield name + ".tif", f"{COP_BASE}/{name}/{name}.tif"


def worldcover_urls(w, s, e, n):
    for lat in range(math.floor(s / 3) * 3, math.ceil(n / 3) * 3, 3):
        for lon in range(math.floor(w / 3) * 3, math.ceil(e / 3) * 3, 3):
            name = f"ESA_WorldCover_10m_2021_v200_{_lat(lat, 2)}{_lon(lon, 3)}_Map.tif"
            yield name, f"{WC_BASE}/{name}"


def fetch(url, dest, optional=False):
    if os.path.exists(dest) and os.path.getsize(dest) > 0:
        return dest
    print(f"GET {url}")
    tmp = dest + ".part"
    try:
        with urllib.request.urlopen(url, timeout=120) as r, open(tmp, "wb") as f:
            total = int(r.headers.get("Content-Length") or 0)
            got = 0
            while True:
                chunk = r.read(1 << 20)
                if not chunk:
                    break
                f.write(chunk)
                got += len(chunk)
                if total:
                    print(f"\r   {got / 1e6:7.1f} / {total / 1e6:7.1f} MB", end="")
            print()
        os.replace(tmp, dest)
        return dest
    except urllib.error.HTTPError as ex:
        if os.path.exists(tmp):
            os.remove(tmp)
        if ex.code == 404 and optional:
            print(f"   404 (no tile here — ocean?) {url}")
            return None
        raise SystemExit(f"download failed: HTTP {ex.code} for {url}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("name")
    ap.add_argument("west", type=float); ap.add_argument("south", type=float)
    ap.add_argument("east", type=float); ap.add_argument("north", type=float)
    ap.add_argument("--out", required=True, help="pack directory to create")
    ap.add_argument("--no-cover", action="store_true", help="elevation only")
    ap.add_argument("--no-elev", action="store_true", help="land cover only")
    a = ap.parse_args()
    bbox = (a.west, a.south, a.east, a.north)
    os.makedirs(RAW, exist_ok=True)
    layers = {}

    if not a.no_elev:
        srcs = []
        for name, url in copernicus_urls(*bbox):
            p = fetch(url, os.path.join(RAW, name), optional=True)
            if p:
                srcs.append(p)
        if not srcs:
            raise SystemExit("no Copernicus tiles found for this bbox")
        print(f"== elev z{ELEV_ZOOM} from {len(srcs)} Copernicus tiles")
        n = rasterize("elev", srcs, ELEV_ZOOM, bbox, a.out)
        layers["elev"] = ELEV_ZOOM
        print(f"   {n} tiles")

    if not a.no_cover:
        srcs = []
        for name, url in worldcover_urls(*bbox):
            p = fetch(url, os.path.join(RAW, name), optional=True)
            if p:
                srcs.append(p)
        if not srcs:
            raise SystemExit("no WorldCover tiles found for this bbox")
        print(f"== cover z{COVER_ZOOM} from {len(srcs)} WorldCover tiles")
        n = rasterize("cover", srcs, COVER_ZOOM, bbox, a.out, src_nodata=0)
        layers["cover"] = COVER_ZOOM
        print(f"   {n} tiles")

    with open(os.path.join(a.out, "pack.json"), "w") as f:
        json.dump({"name": a.name, "bounds": list(bbox), "layers": layers}, f, indent=2)
    size = sum(os.path.getsize(os.path.join(d, f)) for d, _, fs in os.walk(a.out) for f in fs)
    print(f"pack {a.name}: {size / 1e6:.0f} MB at {a.out}")


if __name__ == "__main__":
    main()
