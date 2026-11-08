"""The .etile container (mirrors mod/.../data/EtileCodec.java — keep in sync).

    magic   "ETIL"       4 bytes
    version u8 = 1
    layer   u8           1 climate, 2 elev, 3 cover
    dtype   u8           1 = u8, 2 = i16 little-endian
    z       u8
    tx      u16 LE
    ty      u16 LE
    w       u16 LE
    h       u16 LE
    nodata  i16 LE
    payload zlib(raw pixels, row-major)

Header is 18 bytes. Path inside a pack: layers/<layer>/<z>/<tx>_<ty>.etile
"""
import os
import struct
import zlib
import numpy as np
from tiling import LAYERS, LAYER_NAMES, NODATA, DTYPE, TILE

MAGIC = b"ETIL"
VERSION = 1
HEADER = struct.Struct("<4sBBBBHHHHh")
assert HEADER.size == 18


def tile_path(root: str, layer: str, z: int, tx: int, ty: int) -> str:
    return os.path.join(root, "layers", layer, str(z), f"{tx}_{ty}.etile")


def encode(layer: str, z: int, tx: int, ty: int, arr: np.ndarray, level: int = 6) -> bytes:
    arr = np.ascontiguousarray(arr.astype(DTYPE[layer], copy=False))
    h, w = arr.shape
    dtype_id = 1 if arr.dtype.itemsize == 1 else 2
    hdr = HEADER.pack(MAGIC, VERSION, LAYERS[layer], dtype_id, z, tx, ty, w, h, NODATA[layer])
    return hdr + zlib.compress(arr.tobytes(), level)


def decode(data: bytes):
    magic, ver, layer_id, dtype_id, z, tx, ty, w, h, nodata = HEADER.unpack_from(data, 0)
    if magic != MAGIC or ver != VERSION:
        raise ValueError("not an .etile v1")
    layer = LAYER_NAMES[layer_id]
    raw = zlib.decompress(data[HEADER.size:])
    arr = np.frombuffer(raw, dtype=DTYPE[layer]).reshape(h, w)
    return {"layer": layer, "z": z, "tx": tx, "ty": ty, "w": w, "h": h, "nodata": nodata}, arr


def write(root: str, layer: str, z: int, tx: int, ty: int, arr: np.ndarray) -> str:
    p = tile_path(root, layer, z, tx, ty)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "wb") as f:
        f.write(encode(layer, z, tx, ty, arr))
    return p


def read(path: str):
    with open(path, "rb") as f:
        return decode(f.read())


def is_blank(layer: str, arr: np.ndarray) -> bool:
    """A tile that is entirely NODATA is not worth writing; the reader treats a missing tile the same way."""
    return bool(np.all(arr == NODATA[layer]))
