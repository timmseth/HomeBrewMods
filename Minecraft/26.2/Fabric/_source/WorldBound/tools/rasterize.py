"""Resample any georeferenced raster(s) into .etile tiles at a given zoom.

Every source is warped into the tile grid with rasterio, so projection, pixel size and
alignment of the source don't matter. Several sources can feed one layer (GEBCO's 8
GeoTIFF tiles, or many Copernicus 1-degree tiles); pixels are merged first-non-nodata.
"""
import os
import sys
import numpy as np
import rasterio
from rasterio.enums import Resampling
from rasterio.transform import from_origin
from rasterio.warp import reproject
from rasterio.crs import CRS

import etile
from tiling import TILE, NODATA, DTYPE, deg_per_px, tile_bounds, tiles_covering

WGS84 = CRS.from_epsg(4326)

# How each layer is resampled when the source is finer than the tile pixel (the usual case).
RESAMPLING = {
    "climate": Resampling.mode,
    "cover": Resampling.mode,
    "elev": Resampling.average,
    "veg": Resampling.nearest,
    "treecover": Resampling.average,
}
# When the source is coarser than the tile pixel we interpolate elevation but not classes.
UPSAMPLING = {
    "climate": Resampling.nearest,
    "cover": Resampling.nearest,
    "elev": Resampling.bilinear,
    "veg": Resampling.nearest,
    "treecover": Resampling.bilinear,
}


def _bbox_intersects(a, b):
    return not (a[2] <= b[0] or b[2] <= a[0] or a[3] <= b[1] or b[3] <= a[1])


def rasterize(layer: str, sources, z: int, bbox, out_root: str, *, value_fn=None,
              src_nodata=None, log=print, skip_blank=True, budget=None, skip=None):
    """Write every tile at zoom z inside bbox=(w,s,e,n) for `layer` from `sources` (paths).

    value_fn: optional callable(np.ndarray float32) -> np.ndarray applied to the warped source
              before casting (e.g. WorldCover 10-m classes pass through; Köppen ids pass through;
              GEBCO float metres -> rounded int16).
    Returns the number of tiles written.
    """
    tiles = tiles_covering(z, *bbox)
    # Resume support: tiles already scanned (written or blank) are listed in .scanned and skipped,
    # so a long build can be run in slices (the device VM kills processes after ~3 minutes).
    scanned_path = os.path.join(out_root, "layers", layer, str(z), ".scanned")
    scanned = set()
    if os.path.exists(scanned_path):
        with open(scanned_path) as f:
            scanned = {line.strip() for line in f if line.strip()}
    if scanned:
        tiles = [t for t in tiles if f"{t[0]}_{t[1]}" not in scanned]
        if log:
            log(f"  {layer} z{z}: resuming, {len(scanned)} tiles already scanned, {len(tiles)} to go")
    os.makedirs(os.path.dirname(scanned_path), exist_ok=True)
    scanned_f = open(scanned_path, "a")
    res = deg_per_px(z)
    dtype = DTYPE[layer]
    nodata = NODATA[layer]
    opened = []
    for p in sources:
        ds = rasterio.open(p)
        b = ds.bounds
        if ds.crs is None:
            raise SystemExit(f"{p}: no CRS; refusing to guess")
        if ds.crs != WGS84:
            from rasterio.warp import transform_bounds
            b = transform_bounds(ds.crs, WGS84, *b)
            b = type(ds.bounds)(*b)
        opened.append((ds, (b.left, b.bottom, b.right, b.top)))
    written = 0
    import time as _t
    _t0 = _t.time()
    for i, (tx, ty) in enumerate(tiles):
        if budget is not None and _t.time() - _t0 > budget:
            break
        if skip is not None and skip(tx, ty):
            scanned_f.write(f"{tx}_{ty}\n"); scanned_f.flush()
            continue
        tb = tile_bounds(z, tx, ty)
        acc = np.full((TILE, TILE), np.nan, dtype=np.float32)
        touched = False
        for ds, sb in opened:
            if not _bbox_intersects(tb, sb):
                continue
            src_res = min(abs(ds.transform.a), abs(ds.transform.e))
            # Source resolution in degrees; non-4326 sources are rare here, treat as fine.
            rs = RESAMPLING[layer] if (ds.crs != WGS84 or src_res <= res) else UPSAMPLING[layer]
            dst = np.full((TILE, TILE), np.nan, dtype=np.float32)
            reproject(
                source=rasterio.band(ds, 1),
                destination=dst,
                dst_transform=from_origin(tb[0], tb[3], res, res),
                dst_crs=WGS84,
                dst_nodata=np.nan,
                src_nodata=src_nodata if src_nodata is not None else ds.nodata,
                resampling=rs,
            )
            mask = np.isnan(acc) & ~np.isnan(dst)
            if mask.any():
                acc[mask] = dst[mask]
                touched = True
        if not touched:
            scanned_f.write(f"{tx}_{ty}\n"); scanned_f.flush()
            continue
        if value_fn is not None:
            acc = value_fn(acc)
        out = np.where(np.isnan(acc), nodata, np.rint(acc)).astype(dtype)
        if skip_blank and etile.is_blank(layer, out):
            scanned_f.write(f"{tx}_{ty}\n"); scanned_f.flush()
            continue
        etile.write(out_root, layer, z, tx, ty, out)
        scanned_f.write(f"{tx}_{ty}\n"); scanned_f.flush()
        written += 1
        if log and (written % 25 == 0 or i == len(tiles) - 1):
            log(f"  {layer} z{z}: {i + 1}/{len(tiles)} tiles scanned, {written} written")
    scanned_f.close()
    for ds, _ in opened:
        ds.close()
    return written
