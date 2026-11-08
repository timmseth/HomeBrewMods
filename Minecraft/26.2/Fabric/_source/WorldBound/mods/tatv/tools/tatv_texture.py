"""TATV model + texture generator. Edits PARTS here, then run: writes the texture, the icons, and TatvModel.java.
Model space: pixels, y down, ground at y=24, nose toward -Z."""
import random, os
from PIL import Image, ImageDraw
random.seed(7)
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main') + '/'
NAVY, NAVY2, TEAL, WHITE, PEARL, ORANGE, GLASS, DARK, CYAN, STEEL, LAMP = (22,34,58), (34,52,86), (18,160,170), (232,238,244), (205,214,224), (250,128,28), (110,200,235), (26,26,30), (70,245,255), (112,120,132), (255,246,200)
# name, (x,y,z), (w,h,d), faces dict(top,bottom,side,front,back), pivot (or None), rot degrees (x,y,z), detail tag
def F(top, side=None, front=None, back=None, bottom=None):
    side = side or top
    return dict(top=top, side=side, front=front or side, back=back or side, bottom=bottom or NAVY)
P = [
 ('keel',      (-10,20,-16), (20,3,34), F(NAVY, NAVY, NAVY, NAVY), None, None, 'keel'),
 ('hull',      (-12,17,-14), (24,3,32), F(WHITE, TEAL, NAVY, STEEL), None, None, 'hull'),
 ('noseA',     (-9,18,-21),  (18,4,7),  F(WHITE, NAVY2, NAVY), None, None, 'nose'),
 ('noseB',     (-6,19,-26),  (12,3,5),  F(WHITE, NAVY2, NAVY), None, None, 'nose'),
 ('noseTip',   (-3,20,-29),  (6,2,3),   F(ORANGE), None, None, ''),
 ('lampL',     (-8,18.5,-21.5), (3,1,1), F(LAMP), None, None, ''),
 ('lampR',     (5,18.5,-21.5),  (3,1,1), F(LAMP), None, None, ''),
 ('dash',      (-7,15,-12),  (14,2,6),  F(NAVY2, NAVY, NAVY), None, None, ''),
 ('windshield',(-6,-5,0),    (12,5,1),  F(GLASS), (0,15,-8), (-35,0,0), 'glass'),
 ('seat',      (-5,15,-2),   (10,2,8),  F(DARK), None, None, 'seat'),
 ('seatBack',  (-5,8,5),     (10,7,2),  F(DARK), None, None, 'seat'),
 ('headrest',  (-3,5,5.5),   (6,3,1.5), F(DARK), None, None, ''),
 ('engineDeck',(-9,15,7),    (18,2,10), F(PEARL, NAVY2), None, None, 'vents'),
 ('podL',      (-17,17,-10), (5,5,26),  F(WHITE, NAVY2, NAVY, STEEL), None, None, 'pod'),
 ('podR',      (12,17,-10),  (5,5,26),  F(WHITE, NAVY2, NAVY, STEEL), None, None, 'pod'),
 ('intakeL',   (-16.5,18,-13), (4,3,3), F(NAVY, NAVY, DARK), None, None, ''),
 ('intakeR',   (12.5,18,-13),  (4,3,3), F(NAVY, NAVY, DARK), None, None, ''),
 ('thrL',      (-11,15,17),  (7,7,6),   F(STEEL, STEEL, STEEL, CYAN), None, None, 'thruster'),
 ('thrR',      (4,15,17),    (7,7,6),   F(STEEL, STEEL, STEEL, CYAN), None, None, 'thruster'),
 ('wing',      (-15,9,20),   (30,1,5),  F(ORANGE, ORANGE, ORANGE, ORANGE, ORANGE), None, None, 'wing'),
 ('strutL',    (-11,10,21),  (1,5,2),   F(NAVY), None, None, ''),
 ('strutR',    (10,10,21),   (1,5,2),   F(NAVY), None, None, ''),
 ('finL',      (-0.5,-6,0),  (1,6,6),   F(ORANGE), (-14.5,17,9), (-20,0,0), ''),
 ('finR',      (-0.5,-6,0),  (1,6,6),   F(ORANGE), (14.5,17,9), (-20,0,0), ''),
 ('padFL',     (-15,22,-8),  (5,1,5),   F(CYAN, CYAN, CYAN, CYAN, CYAN), None, None, ''),
 ('padFR',     (10,22,-8),   (5,1,5),   F(CYAN, CYAN, CYAN, CYAN, CYAN), None, None, ''),
 ('padBL',     (-15,22,8),   (5,1,5),   F(CYAN, CYAN, CYAN, CYAN, CYAN), None, None, ''),
 ('padBR',     (10,22,8),    (5,1,5),   F(CYAN, CYAN, CYAN, CYAN, CYAN), None, None, ''),
 ('glow',      (-8,22.5,-12),(16,1,24), F(CYAN, CYAN, CYAN, CYAN, CYAN), None, None, ''),
]
import math
W, H = 256, 128
def isz(v): return max(1, math.ceil(v))
x = y = shelf = 0; uv = []
for p in P:
    w, h, d = (isz(v) for v in p[2]); bw, bh = 2*(d+w), d+h
    if x + bw > W: x, y, shelf = 0, y + shelf, 0
    uv.append((x, y)); x += bw; shelf = max(shelf, bh)
assert y + shelf <= H, (y, shelf)
img = Image.new('RGBA', (W, H), (0,0,0,0))
def px(x, y, c): img.putpixel((x, y), c + (255,) if len(c) == 3 else c)
def shade(c, f): return tuple(max(0, min(255, int(v*f))) for v in c)
def fill(x0, y0, w, h, c, f=1.0, noise=0.05, vgrad=0.0):
    for i in range(w):
        for j in range(h):
            k = f * (1 + (random.random()-0.5)*noise) * (1 - vgrad*j/max(1,h-1))
            px(x0+i, y0+j, shade(c, k))
for (u, v), p in zip(uv, P):
    name, (w, h, d), fc, tag = p[0], (isz(t) for t in p[2]), p[3], p[6]
    glass = tag == 'glass'
    top_r, bot_r = (u+d, v, w, d), (u+d+w, v, w, d)
    e_r, f_r, w_r, b_r = (u, v+d, d, h), (u+d, v+d, w, h), (u+d+w, v+d, d, h), (u+2*d+w, v+d, w, h)
    fill(*top_r, fc['top'], 1.0)
    fill(*bot_r, fc['bottom'], 0.7)
    fill(*e_r, fc['side'], 0.85, vgrad=0.25)
    fill(*w_r, fc['side'], 0.85, vgrad=0.25)
    fill(*f_r, fc['front'], 0.95, vgrad=0.2)
    fill(*b_r, fc['back'], 0.9, vgrad=0.2)
    if glass:  # glossy streak
        for r in (f_r, b_r):
            for i in range(r[2]): 
                j = (i // 2) % r[3]
                if i % 5 < 2: px(r[0]+i, r[1]+j, (220,245,255))
    if tag == 'hull':
        for j in range(d):                    # twin racing stripes
            for i in (w//2-4, w//2-3, w//2+2, w//2+3): px(u+d+i, v+j, ORANGE)
        for r in (e_r, w_r):                  # orange waterline + panel seams
            for i in range(r[2]): px(r[0]+i, r[1]+r[3]-1, ORANGE)
            for i in range(0, r[2], 8): px(r[0]+i, r[1], shade(TEAL, 0.6))
    if tag == 'keel':
        for r in (e_r, w_r):
            for i in range(r[2]): px(r[0]+i, r[1]+r[3]-1, shade(CYAN, 0.9))
    if tag == 'nose':
        for i in range(top_r[2]): px(top_r[0]+i, top_r[1]+top_r[3]-1, ORANGE)
    if tag == 'pod':
        for r in (e_r, w_r):
            for i in range(r[2]): px(r[0]+i, r[1]+1, ORANGE)
        for i in range(0, top_r[3], 4): 
            for k in range(top_r[2]): px(top_r[0]+k, top_r[1]+i, shade(PEARL, 0.9))
    if tag == 'vents':
        for j in range(1, top_r[3]-1, 2):
            for i in range(2, top_r[2]-2): px(top_r[0]+i, top_r[1]+j, DARK)
    if tag == 'seat':
        for j in range(0, top_r[3], 2):
            for i in range(top_r[2]): px(top_r[0]+i, top_r[1]+j, shade(DARK, 1.6))
    if tag == 'thruster':  # glowing nozzle with a dark rim
        bx, by, bw_, bh_ = b_r
        for i in range(bw_):
            for j in range(bh_):
                edge = min(i, j, bw_-1-i, bh_-1-j)
                px(bx+i, by+j, STEEL if edge == 0 else DARK if edge == 1 else shade(CYAN, 0.8 + 0.1*edge))
    if tag == 'wing':
        for i in range(0, top_r[2], 1): px(top_r[0]+i, top_r[1], WHITE)
os.makedirs(ROOT + 'resources/assets/tatv/textures/entity', exist_ok=True)
os.makedirs(ROOT + 'resources/assets/tatv/textures/item', exist_ok=True)
img.save(ROOT + 'resources/assets/tatv/textures/entity/tatv.png')

def f(v): return ('%gf' % v)
lines = []
for i, ((u, v), p) in enumerate(zip(uv, P)):
    name, (x0, y0, z0), (w, h, d), _, piv, rot, _ = p
    if piv is None:
        pose = 'PartPose.offset(0, 24, 0)'; bx, by, bz = x0, y0 - 24, z0
    else:
        r = [math.radians(a) for a in rot]
        pose = 'PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)' % tuple(f(t) for t in (*piv, *r)); bx, by, bz = x0, y0, z0
    lines.append('        root.addOrReplaceChild("%s", CubeListBuilder.create().texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s), %s);'
                 % (name, u, v, f(bx), f(by), f(bz), f(w), f(h), f(d), pose))
java = '''package com.seth.tatv.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Sleek hover-boat. GENERATED by tools/tatv_texture.py (geometry and texture together); edit that, not this. Faces -Z. */
public class TatvModel extends EntityModel<TatvRenderState> {
    public TatvModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
%s
        return LayerDefinition.create(mesh, %d, %d);
    }
}
''' % ('\n'.join(lines), W, H)
open(ROOT + 'java/com/seth/tatv/client/TatvModel.java', 'w').write(java)

def icon(n):
    im = Image.new('RGBA', (16, 16), (0,0,0,0)); g = ImageDraw.Draw(im)
    g.polygon([(0,10),(3,8),(13,8),(15,9),(15,11),(2,11)], fill=WHITE)
    g.line([(2,11),(15,11)], fill=NAVY); g.line([(1,10),(14,10)], fill=TEAL)
    g.line([(3,9),(13,9)], fill=ORANGE)
    g.polygon([(5,8),(7,5),(8,5),(7,8)], fill=GLASS)
    g.rectangle([10,5,11,7], fill=DARK)
    g.rectangle([12,4,15,4], fill=ORANGE)
    g.point((15,9), fill=CYAN); g.point((15,10), fill=CYAN)
    g.line([(3,13),(13,13)], fill=CYAN); g.point((0,10), fill=ORANGE)
    return im.resize((n, n), Image.NEAREST)
icon(16).save(ROOT + 'resources/assets/tatv/textures/item/tatv.png')
bg = Image.new('RGBA', (128,128), (18,24,40,255)); bg.alpha_composite(icon(128)); bg.save(ROOT + 'resources/assets/tatv/icon.png')
print('packed', W, 'x', y + shelf, len(P), 'parts')
