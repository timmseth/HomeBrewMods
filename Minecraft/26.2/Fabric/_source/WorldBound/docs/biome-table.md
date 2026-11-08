# BIOME_TABLE — Köppen-Geiger → Minecraft

Source classes are the Beck et al. 2023 legend (ids 1–30, 0 = ocean/nodata).
`BiomeTable.java` must match this file; when one changes, change the other.

Columns:
- **Default** — biome when no fine `cover` layer is loaded, or cover is crop/grass/shrub.
- **Tree** — when WorldCover says tree cover (10) or mangrove (95).
- **Bare** — when WorldCover says bare/sparse (60) or moss/lichen (100).
- **Snowline (m)** — above this elevation, switch to the peak biomes (§5.3 of PLAN.md).
- **Preview** — RGB for `tools/preview.py` (Beck legend colours, approx).

| id | Class | Meaning | Default | Tree | Bare | Snowline | Preview |
|---|---|---|---|---|---|---|---|
| 1 | Af | Tropical rainforest | `jungle` | `jungle` | `sparse_jungle` | 4800 | 0,0,255 |
| 2 | Am | Tropical monsoon | `sparse_jungle` | `jungle` | `savanna` | 4800 | 0,120,255 |
| 3 | Aw | Tropical savanna | `savanna` | `sparse_jungle` | `savanna_plateau` | 4800 | 70,170,250 |
| 4 | BWh | Hot desert | `desert` | `desert` | `desert` | 4500 | 255,0,0 |
| 5 | BWk | Cold desert | `badlands` | `wooded_badlands` | `eroded_badlands` | 3800 | 255,150,150 |
| 6 | BSh | Hot semi-arid | `savanna_plateau` | `savanna` | `desert` | 4500 | 245,165,0 |
| 7 | BSk | Cold semi-arid | `plains` (dry) | `wooded_badlands` | `badlands` | 3600 | 255,220,100 |
| 8 | Csa | Hot-summer Mediterranean | `plains` | `forest` | `stony_shore` | 3200 | 255,255,0 |
| 9 | Csb | Warm-summer Mediterranean | `forest` | `forest` | `plains` | 3000 | 200,200,0 |
| 10 | Csc | Cold-summer Mediterranean | `taiga` | `taiga` | `meadow` | 2400 | 150,150,0 |
| 11 | Cwa | Humid subtropical, dry winter | `forest` | `bamboo_jungle` | `savanna` | 4200 | 150,255,150 |
| 12 | Cwb | Subtropical highland | `meadow` | `forest` | `plains` | 4000 | 100,200,100 |
| 13 | Cwc | Cold subtropical highland | `grove` | `taiga` | `meadow` | 3400 | 50,150,50 |
| 14 | Cfa | Humid subtropical | `forest` | `dark_forest` | `plains` | 3800 | 200,255,80 |
| 15 | Cfb | Oceanic | `forest` | `dark_forest` | `meadow` | 2800 | 100,255,80 |
| 16 | Cfc | Subpolar oceanic | `taiga` | `old_growth_spruce_taiga` | `meadow` | 1600 | 50,200,0 |
| 17 | Dsa | Humid continental, dry hot summer | `plains` | `forest` | `badlands` | 3400 | 255,0,255 |
| 18 | Dsb | Humid continental, dry warm summer | `forest` | `old_growth_pine_taiga` | `meadow` | 3000 | 200,0,200 |
| 19 | Dsc | Subarctic, dry summer | `taiga` | `old_growth_spruce_taiga` | `snowy_plains` | 2200 | 150,50,150 |
| 20 | Dsd | Subarctic, dry very cold winter | `snowy_taiga` | `snowy_taiga` | `snowy_plains` | 1800 | 150,100,150 |
| 21 | Dwa | Monsoon continental, hot summer | `plains` | `forest` | `plains` | 3400 | 170,175,255 |
| 22 | Dwb | Monsoon continental, warm summer | `forest` | `birch_forest` | `meadow` | 3000 | 90,120,220 |
| 23 | Dwc | Monsoon subarctic | `taiga` | `taiga` | `snowy_plains` | 2200 | 75,80,180 |
| 24 | Dwd | Monsoon subarctic, very cold winter | `snowy_taiga` | `snowy_taiga` | `snowy_plains` | 1800 | 50,0,135 |
| 25 | Dfa | Humid continental, hot summer | `plains` | `forest` | `plains` | 3400 | 0,255,255 |
| 26 | Dfb | Humid continental, warm summer | `forest` | `birch_forest` | `meadow` | 3000 | 55,200,255 |
| 27 | Dfc | Subarctic | `taiga` | `old_growth_spruce_taiga` | `snowy_plains` | 2000 | 0,125,125 |
| 28 | Dfd | Subarctic, very cold winter | `snowy_taiga` | `snowy_taiga` | `snowy_plains` | 1600 | 0,70,95 |
| 29 | ET | Tundra | `snowy_plains` | `snowy_taiga` | `snowy_slopes` | 800 | 178,178,178 |
| 30 | EF | Ice cap | `ice_spikes` | `ice_spikes` | `frozen_peaks` | 0 | 102,102,102 |

## Overrides applied before the table (PLAN.md §5.3)

| Condition | Biome |
|---|---|
| WorldCover water (80), above sea level | `river` (lakes are rivers with still water — vanilla has no lake biome) |
| WorldCover wetland (90) | `swamp`; `mangrove_swamp` if class 1–3 |
| WorldCover snow/ice (70) | `snowy_plains` below snowline, `frozen_peaks` above |
| Above snowline, slope steep | `jagged_peaks` |
| Above snowline, slope gentle | `snowy_slopes` (`frozen_peaks` if > snowline + 800 m) |
| Above snowline − 600 m, class 4–7 (arid) | `stony_peaks` |
| Ocean, \|lat\| < 23 | `warm_ocean` |
| Ocean, 23 ≤ \|lat\| < 40 | `lukewarm_ocean` |
| Ocean, 40 ≤ \|lat\| < 58 | `ocean` |
| Ocean, 58 ≤ \|lat\| < 68 | `cold_ocean` |
| Ocean, \|lat\| ≥ 68 | `frozen_ocean` |
| Any ocean deeper than 1500 m | `deep_` variant (`deep_frozen_ocean` etc.; `warm_ocean` has no deep variant → `deep_lukewarm_ocean`) |
| Land within 2 px of ocean, elev < 20 m | `beach` (`snowy_beach` if class 29/30, `stony_shore` if slope steep) |

Slope = elevation difference across ±1 px at the current zoom, in m per block
after the height curve; "steep" = > 1.0 (45°).

## Known taste calls for Seth to tune in-game

- Cold deserts (Gobi, Great Basin, Patagonia) as `badlands` rather than a sand desert.
- BSk (most of southern Idaho, the Snake River Plain) is `plains` — may want a sagebrush
  feel; vanilla has nothing closer without a custom biome, which is out of scope.
- Mediterranean as `plains`/`forest` vs `wooded_badlands`.
- Snowline numbers are rough averages per class; they'll need lowering in the Rockies
  and raising in the Andes. A per-latitude adjustment is a cheap improvement.
