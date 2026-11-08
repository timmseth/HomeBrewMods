"""End-to-end test of the Python tooling on a synthetic planet (no downloads).

    python -m pytest tools/tests        or        python tools/tests/test_pipeline.py

Builds a fake 0.25-degree Köppen raster and a fake GEBCO raster (a continent with a mountain),
tiles them, round-trips .etile, renders previews and checks a few known pixels.
"""
import os
import sys
import tempfile

import numpy as np
import rasterio
from rasterio.transform import from_origin

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))

import etile  # noqa: E402
import tiling  # noqa: E402
from rasterize import rasterize  # noqa: E402
import preview  # noqa: E402
import biome_table  # noqa: E402


def synth_world(tmp, res=0.25):
    w = int(360 / res); h = int(180 / res)
    lon = -180 + (np.arange(w) + 0.5) * res
    lat = 90 - (np.arange(h) + 0.5) * res
    LON, LAT = np.meshgrid(lon, lat)
    # Continent: ellipse centred on Idaho-ish, mountain peak at (44N, -114E).
    land = ((LON + 100) / 40) ** 2 + ((LAT - 40) / 25) ** 2 < 1
    elev = np.where(land, 300 + 3500 * np.exp(-(((LON + 114) / 4) ** 2 + ((LAT - 44) / 3) ** 2)), -3000 + 10 * LON)
    elev = elev.astype(np.float32)
    climate = np.zeros((h, w), dtype=np.uint8)
    climate[land & (LAT > 50)] = 27   # Dfc
    climate[land & (LAT <= 50) & (LAT > 35)] = 7   # BSk
    climate[land & (LAT <= 35)] = 4   # BWh
    tr = from_origin(-180, 90, res, res)
    kp = os.path.join(tmp, "koppen.tif"); gp = os.path.join(tmp, "gebco.tif")
    with rasterio.open(kp, "w", driver="GTiff", width=w, height=h, count=1, dtype="uint8", crs="EPSG:4326", transform=tr, nodata=0) as ds:
        ds.write(climate, 1)
    with rasterio.open(gp, "w", driver="GTiff", width=w, height=h, count=1, dtype="float32", crs="EPSG:4326", transform=tr) as ds:
        ds.write(elev, 1)
    return kp, gp


def sample(root, layer, z, lon, lat):
    gx, gy = tiling.lonlat_to_global_px(z, lon, lat)
    tx, ty = int(gx // tiling.TILE), int(gy // tiling.TILE)
    p = etile.tile_path(root, layer, z, tx, ty)
    if not os.path.exists(p):
        return None
    _, arr = etile.read(p)
    return int(arr[int(gy) % tiling.TILE, int(gx) % tiling.TILE])


def test_tiling_math():
    assert tiling.tiles_across(3) == 8 and tiling.tiles_down(3) == 4
    assert abs(tiling.deg_per_px(10) - 0.000343) < 1e-5
    assert tiling.tile_bounds(3, 0, 0) == (-180.0, 45.0, -135.0, 90.0)
    gx, gy = tiling.lonlat_to_global_px(3, -180, 90)
    assert gx == 0 and gy == 0
    assert tiling.tiles_covering(3, -117.3, 41.9, -110.9, 49.1) == [(1, 0), (1, 1)]


def test_etile_roundtrip():
    arr = (np.random.default_rng(1).integers(-5000, 5000, (1024, 1024))).astype("<i2")
    data = etile.encode("elev", 7, 3, 5, arr)
    hdr, back = etile.decode(data)
    assert hdr == {"layer": "elev", "z": 7, "tx": 3, "ty": 5, "w": 1024, "h": 1024, "nodata": -32768}
    assert np.array_equal(arr, back)
    assert len(data) < arr.nbytes  # compressed


def test_pipeline():
    with tempfile.TemporaryDirectory() as tmp:
        kp, gp = synth_world(tmp)
        root = os.path.join(tmp, "base")
        n1 = rasterize("climate", [kp], 3, (-180, -90, 180, 90), root, log=None)
        n2 = rasterize("elev", [gp], 3, (-180, -90, 180, 90), root, log=None)
        assert n2 == 32, n2                      # every elev tile has data
        assert 0 < n1 <= 32                      # blank climate tiles (ocean) are skipped
        # Ocean west of the continent, mountain near the peak, class bands.
        assert sample(root, "elev", 3, -170, 0) < -2000
        assert sample(root, "elev", 3, -114, 44) > 3000
        assert sample(root, "climate", 3, -100, 45) == 7
        assert sample(root, "climate", 3, -100, 55) == 27
        assert sample(root, "climate", 3, -100, 30) == 4
        assert sample(root, "climate", 3, -170, 0) in (None, 0)
        # Previews render and have the right size.
        img, missing, total = preview.render(root, "elev", 3, max_px=1024)
        assert img.shape == (512, 1024, 3) and missing == 0
        img, _, _ = preview.render(root, "climate", 3, max_px=1024)
        assert img.shape == (512, 1024, 3)
        # Regional re-tile at a finer zoom from the same coarse source still works (upsampling path).
        n3 = rasterize("elev", [gp], 6, (-120, 40, -110, 48), os.path.join(tmp, "region"), log=None)
        assert n3 >= 4


def test_biome_table_parses():
    t = biome_table.load()
    assert t[4]["code"] == "BWh" and t[4]["default"] == "desert"
    assert t[30]["code"] == "EF" and t[30]["rgb"] == (102, 102, 102)


if __name__ == "__main__":
    test_tiling_math(); test_etile_roundtrip(); test_pipeline(); test_biome_table_parses()
    print("all python tests passed")
