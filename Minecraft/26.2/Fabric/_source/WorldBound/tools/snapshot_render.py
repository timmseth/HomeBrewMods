#!/usr/bin/env python3
"""Render a WorldBound terrain snapshot (.wbsnap from `/earth snapshot`) as a shaded map and 3-D views.

Colours are the average colours of the real block textures (read from the Minecraft client jar), tinted with each
column's biome grass/foliage/water colours, so the pictures look like the world does in game from far away.

    python3 tools/snapshot_render.py run/snapshots/fuji.wbsnap --jar mod/cdeps/mc-26.2.jar --out out/fuji
"""
import argparse
import io
import math
import struct
import zipfile

import numpy as np
from PIL import Image

# Textures that are greyscale in the jar and tinted in game.
GRASS_TINT = {'grass_block_top', 'short_grass', 'tall_grass_top', 'tall_grass_bottom', 'fern', 'large_fern_top',
              'large_fern_bottom', 'sugar_cane', 'grass_block_side_overlay'}
FOLIAGE_TINT = {'oak_leaves', 'jungle_leaves', 'acacia_leaves', 'dark_oak_leaves', 'mangrove_leaves', 'vine'}
FIXED_TINT = {'birch_leaves': 0x80A755, 'spruce_leaves': 0x619961, 'lily_pad': 0x208030}


def load(path):
    data = open(path, 'rb').read()
    assert data[:8] in (b'WBSNAP1\n', b'WBSNAP2\n'), 'not a wbsnap file'
    v2 = data[:8] == b'WBSNAP2\n'
    off = 8
    n, x0, z0 = struct.unpack_from('>iii', data, off); off += 12
    lat, lon, scale = struct.unpack_from('>ddd', data, off); off += 24
    sea, k = struct.unpack_from('>ii', data, off); off += 8
    names = []
    for _ in range(k):
        ln, = struct.unpack_from('>H', data, off); off += 2
        names.append(data[off:off + ln].decode('utf-8')); off += ln
    rec = np.dtype([('top', '>i2'), ('topb', '>i2'), ('floor', '>i2'), ('floorb', '>i2'),
                    ('grass', '>i4'), ('foliage', '>i4'), ('water', '>i4')])
    a = np.frombuffer(data, dtype=rec, count=n * n, offset=off).reshape(n, n)
    off += rec.itemsize * n * n
    biomes, bidx = None, None
    if v2:
        nb, = struct.unpack_from('>i', data, off); off += 4
        biomes = []
        for _ in range(nb):
            ln, = struct.unpack_from('>H', data, off); off += 2
            biomes.append(data[off:off + ln].decode('utf-8')); off += ln
        bidx = np.frombuffer(data, dtype='>i2', count=n * n, offset=off).reshape(n, n).astype(int)
    return dict(n=n, x0=x0, z0=z0, lat=lat, lon=lon, scale=scale, sea=sea, names=names, a=a, biomes=biomes, bidx=bidx)


def biome_tints(s, jar):
    """Grass and foliage colours per column from the biome's climate and the client colormaps (a dedicated server
    reports 0 for both), with the dark-forest and swamp modifiers and explicit colour overrides."""
    import json
    z = zipfile.ZipFile(jar)
    gmap = np.asarray(Image.open(io.BytesIO(z.read('assets/minecraft/textures/colormap/grass.png'))).convert('RGB'))
    fmap = np.asarray(Image.open(io.BytesIO(z.read('assets/minecraft/textures/colormap/foliage.png'))).convert('RGB'))
    g_cols, f_cols = [], []
    for bid in s['biomes']:
        ns, name = bid.split(':', 1)
        try:
            js = json.loads(z.read(f'data/minecraft/worldgen/biome/{name}.json'))
        except KeyError:
            js = {'temperature': 0.7, 'downfall': 0.6, 'effects': {}}  # modded biome: a temperate default
        t = min(1.0, max(0.0, js.get('temperature', 0.7)))
        d = min(1.0, max(0.0, js.get('downfall', 0.5))) * t
        x, y = int((1 - t) * 255), int((1 - d) * 255)
        gc = gmap[y, x].astype(np.float32)
        fc = fmap[y, x].astype(np.float32)
        eff = js.get('effects', {})
        if 'grass_color' in eff:
            v = int(eff['grass_color'].lstrip('#'), 16) if isinstance(eff['grass_color'], str) else eff['grass_color']
            gc = np.array([(v >> 16) & 255, (v >> 8) & 255, v & 255], np.float32)
        if 'foliage_color' in eff:
            v = int(eff['foliage_color'].lstrip('#'), 16) if isinstance(eff['foliage_color'], str) else eff['foliage_color']
            fc = np.array([(v >> 16) & 255, (v >> 8) & 255, v & 255], np.float32)
        mod = eff.get('grass_color_modifier')
        if mod == 'dark_forest':
            gc = (gc + np.array([0x28, 0x34, 0x0A], np.float32)) / 2
        elif mod == 'swamp':
            gc = np.array([0x6A, 0x70, 0x39], np.float32)
        g_cols.append(gc)
        f_cols.append(fc)
    g_cols, f_cols = np.array(g_cols), np.array(f_cols)
    return g_cols[s['bidx']], f_cols[s['bidx']]


class Textures:
    def __init__(self, jar):
        self.jar = jar
        self.z = zipfile.ZipFile(jar)
        self.cache = {}

    def avg(self, tex):
        if tex in self.cache:
            return self.cache[tex]
        try:
            im = Image.open(io.BytesIO(self.z.read(f'assets/minecraft/textures/block/{tex}.png'))).convert('RGBA')
            a = np.asarray(im, dtype=np.float32)
            a = a[:a.shape[1]]  # animated strips: first frame
            w = a[..., 3:4] / 255.0
            c = (a[..., :3] * w).sum((0, 1)) / max(1e-6, w.sum())
            v = c
        except KeyError:
            v = None
        self.cache[tex] = v
        return v

    def block(self, bid):
        """(rgb float array, tint kind) for a block id's top face."""
        name = bid.split(':', 1)[1]
        cands = [name + '_top', name, name.replace('_block', ''), name + '_side']
        if name == 'grass_block':
            cands = ['grass_block_top']
        elif name == 'water':
            cands = ['water_still']
        elif name in ('tall_grass', 'large_fern'):
            cands = [name + '_top']
        elif name.endswith('_wood') or name.endswith('_hyphae'):
            cands = [name.replace('_wood', '_log').replace('_hyphae', '_stem')]
        elif name == 'snow':
            cands = ['snow']
        elif name == 'lava':
            cands = ['lava_still']
        elif name.startswith('infested_'):
            cands = [name[len('infested_'):]]
        elif name in ('cave_air', 'void_air', 'air'):
            return np.array([150, 180, 230], np.float32), None
        for t in cands:
            v = self.avg(t)
            if v is not None:
                kind = 'grass' if t in GRASS_TINT else 'foliage' if t in FOLIAGE_TINT else 'water' if t == 'water_still' else None
                if t in FIXED_TINT:
                    f = FIXED_TINT[t]
                    v = v / 255.0 * np.array([(f >> 16) & 255, (f >> 8) & 255, f & 255], np.float32)
                return v, kind
        return np.array([255, 0, 255], np.float32), None  # unknown: magenta


def rgb(arr):
    a = arr.astype(np.int64) & 0xFFFFFF
    return np.stack([(a >> 16) & 255, (a >> 8) & 255, a & 255], -1).astype(np.float32)


def colours(s, tex):
    a = s['a']
    n = s['n']
    base = np.zeros((len(s['names']), 3), np.float32)
    kinds = []
    for i, bid in enumerate(s['names']):
        v, k = tex.block(bid)
        base[i] = v
        kinds.append(k)
    kinds = np.array([{'grass': 1, 'foliage': 2, 'water': 3}.get(k, 0) for k in kinds])
    grass, foliage, water = rgb(a['grass']), rgb(a['foliage']), rgb(a['water'])
    if s.get('bidx') is not None and int(a['grass'].max()) == 0:
        grass, foliage = biome_tints(s, tex.jar)

    def shade(ids):
        c = base[ids].copy()
        k = kinds[ids]
        for code, tint in ((1, grass), (2, foliage), (3, water)):
            m = k == code
            c[m] = c[m] * tint[m] / 255.0
        return c

    top = shade(a['topb'].astype(int))
    flo = shade(a['floorb'].astype(int))
    depth = (a['top'].astype(np.float32) - a['floor'].astype(np.float32))
    is_water = np.array(['water' in nm for nm in s['names']])[a['topb'].astype(int)]
    # Water: the floor shows through in the shallows, then the water colour darkens with depth.
    t = np.clip(depth / 10.0, 0, 1)[..., None]
    wcol = top * (0.55 + 0.45 * np.exp(-np.clip(depth, 0, None) / 60.0))[..., None]
    col = np.where(is_water[..., None], flo * (1 - t) * 0.85 + wcol * t, top)
    return col, is_water


def hillshade(h, az=315, alt=45, z=1.0):
    gy, gx = np.gradient(h.astype(np.float32) * z)
    slope = np.pi / 2 - np.arctan(np.hypot(gx, gy))
    aspect = np.arctan2(-gx, gy)
    azr, altr = math.radians(360 - az + 90), math.radians(alt)
    return np.clip(np.sin(altr) * np.sin(slope) + np.cos(altr) * np.cos(slope) * np.cos(azr - aspect), 0, 1)


def topdown(s, col, water):
    h = s['a']['top'].astype(np.float32)
    hs = hillshade(h)
    k = np.where(water, 1.0, 0.45 + 0.75 * hs)[..., None]
    return Image.fromarray(np.clip(col * k, 0, 255).astype(np.uint8))


def view3d(s, col, water, yaw_deg=0.0, cam_frac=(0.5, 1.0), height_above=120, pitch=0.30, fov=70, w=1280, hgt=720,
           far=None):
    """Voxel-space ray caster: camera at cam_frac of the snapshot (x, z), looking toward yaw (0 = north, -z)."""
    a = s['a']
    n = s['n']
    H = a['top'].astype(np.float32)
    hs = hillshade(H)
    C = col * np.where(water, 1.0, 0.5 + 0.7 * hs)[..., None]
    cx, cz = cam_frac[0] * (n - 1), cam_frac[1] * (n - 1)
    # Above the highest ground in view, so steep terrain is seen from above rather than as a wall.
    cy = float(np.percentile(H, 99.5)) + height_above
    far = far or n * 1.5
    yaw = math.radians(yaw_deg)
    f = (w / 2) / math.tan(math.radians(fov) / 2)
    horizon = hgt * (0.5 - pitch)
    ang = yaw + np.arctan((np.arange(w) - w / 2) / f)
    sx, sz = np.sin(ang), -np.cos(ang)
    corr = np.cos(ang - yaw)
    ds = [1.0]
    while ds[-1] < far:
        ds.append(ds[-1] + max(0.35, ds[-1] * 0.0035))
    ds = np.array(ds, np.float32)
    px = cx + np.outer(ds, sx)
    pz = cz + np.outer(ds, sz)
    inside = (px >= 0) & (px < n - 1) & (pz >= 0) & (pz < n - 1)
    ix = np.clip(px.astype(int), 0, n - 1)
    iz = np.clip(pz.astype(int), 0, n - 1)
    hh = H[iz, ix]
    proj = horizon + (cy - hh) * f / (ds[:, None] * corr[None, :])
    proj = np.where(inside, proj, hgt + 1)
    cols = C[iz, ix]
    # fog toward the sky colour with distance
    sky_top, sky_bot = np.array([120, 160, 220], np.float32), np.array([200, 220, 240], np.float32)
    fog = np.clip((ds / far) ** 1.4, 0, 1)[:, None, None]
    cols = cols * (1 - fog) + sky_bot * fog
    m = np.minimum.accumulate(proj, axis=0)
    img = np.zeros((hgt, w, 3), np.float32)
    rows = np.arange(hgt, dtype=np.float32)
    tsky = np.clip(rows / max(1, horizon), 0, 1)[:, None]
    sky = sky_top * (1 - tsky) + sky_bot * tsky
    for c in range(w):
        A = -m[:, c]
        k = np.searchsorted(A, -rows, side='left')
        hit = k < len(ds)
        kk = np.clip(k, 0, len(ds) - 1)
        img[:, c] = np.where(hit[:, None], cols[kk, c], sky)
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('snap')
    ap.add_argument('--jar', default='mod/cdeps/mc-26.2.jar')
    ap.add_argument('--out', required=True, help='output prefix')
    ap.add_argument('--views', default='0,90,180,270', help='yaws for 3-D views (degrees, 0 = looking north)')
    ap.add_argument('--above', type=float, default=140)
    args = ap.parse_args()
    s = load(args.snap)
    tex = Textures(args.jar)
    col, water = colours(s, tex)
    topdown(s, col, water).save(args.out + '_map.png')
    n = s['n']
    for y in [float(v) for v in args.views.split(',') if v]:
        # camera on the edge opposite the view direction, looking across the area
        r = math.radians(y)
        fx, fz = 0.5 - 0.48 * math.sin(r), 0.5 + 0.48 * math.cos(r)
        view3d(s, col, water, yaw_deg=y, cam_frac=(fx, fz), height_above=args.above).save(f'{args.out}_view{int(y)}.png')
    unknown = [b for b in s['names'] if tex.block(b)[0][0] == 255 and tex.block(b)[0][2] == 255]
    print(f"{args.snap}: {n}x{n} blocks at {s['lat']:.4f},{s['lon']:.4f} 1:{s['scale']:g}; blocks {len(s['names'])}"
          + (f"; no texture for {unknown}" if unknown else ''))


if __name__ == '__main__':
    main()
