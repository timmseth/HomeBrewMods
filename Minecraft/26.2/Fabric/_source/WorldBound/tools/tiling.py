"""Geographic tiling shared by every tool (mirrors mod/.../data/Tiling.java — keep in sync).

Zoom z: 2**z tiles across 360 deg of longitude, 2**(z-1) tiles down 180 deg of latitude,
each tile TILE px square. Pixel (0,0) of tile (0,0) is the NW corner of the world (-180, +90).
"""
TILE = 1024
LAYERS = {"climate": 1, "elev": 2, "cover": 3, "lake": 4, "lakedepth": 5, "veg": 6, "treecover": 7}
LAYER_NAMES = {v: k for k, v in LAYERS.items()}
NODATA = {"climate": 0, "elev": -32768, "cover": 0, "lake": -32768, "lakedepth": 0, "veg": 0, "treecover": 255}
DTYPE = {"climate": "u1", "elev": "<i2", "cover": "u1", "lake": "<i2", "lakedepth": "u1", "veg": "u1", "treecover": "u1"}


def deg_per_px(z: int) -> float:
    return 360.0 / (TILE * (1 << z))


def deg_per_tile(z: int) -> float:
    return 360.0 / (1 << z)


def tiles_across(z: int) -> int:
    return 1 << z


def tiles_down(z: int) -> int:
    return 1 << (z - 1)


def tile_bounds(z: int, tx: int, ty: int):
    """(west, south, east, north) in degrees of tile (tx, ty) at zoom z."""
    t = deg_per_tile(z)
    west = -180.0 + tx * t
    north = 90.0 - ty * t
    return west, north - t, west + t, north


def lonlat_to_global_px(z: int, lon: float, lat: float):
    s = TILE * (1 << z) / 360.0
    return (lon + 180.0) * s, (90.0 - lat) * s


def tiles_covering(z: int, west: float, south: float, east: float, north: float):
    """All (tx, ty) at zoom z intersecting the bbox."""
    t = deg_per_tile(z)
    tx0 = max(0, int((west + 180.0) // t))
    tx1 = min(tiles_across(z) - 1, int((east + 180.0 - 1e-9) // t))
    ty0 = max(0, int((90.0 - north) // t))
    ty1 = min(tiles_down(z) - 1, int((90.0 - south - 1e-9) // t))
    return [(tx, ty) for ty in range(ty0, ty1 + 1) for tx in range(tx0, tx1 + 1)]


def zoom_for_resolution(deg: float) -> int:
    """Smallest zoom whose pixel is no coarser than `deg` (so we never lose source detail)."""
    z = 0
    while deg_per_px(z) > deg and z < 20:
        z += 1
    return z
