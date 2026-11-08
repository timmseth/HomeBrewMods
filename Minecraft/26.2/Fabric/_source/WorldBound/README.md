# WorldBound — source

Source for the seven [WorldBound mods](..) for Minecraft 26.2 / Fabric.

```
mods/
  core/                 the WorldBound engine (library `worldbound`): terrain, biomes, rivers, caves, horizon, API
  earthdata/            Earth Data World Gen: real data, streaming, gazetteer, spawn screen
  earthish/             Earthish World Gen: generated planets
  fossil-core/          the animals (library `fossilrecord`)
  earth-fossils/        Earth Fossil Record: fossil sites, real ranges
  earthish-fossils/     Earthish Fossil Record: planet home ranges
  landmarks-core/       obelisks, discovery, travel (library `worldbound_landmarks`)
  earth-landmarks/      Earth Landmarks: real landmarks, cities, walled cities
  earthish-landmarks/   Earthish Landmarks: the planet's own landmarks
  tatv/                 TATV
  tests/                CoreTests and the art tools
  build.sh  runserver.sh
mod/setup-deps.sh, patchver.py   one-time setup of the game jars
tools/                  Python geodata pipeline
docs/                   previews, biome table
CHANGELOG.md  ATTRIBUTION.md  BUILDING.md  LICENSE
```

## The API (for add-ons and other mods)

`com.seth.worldbound.api` in the engine:

- `WorldBound`: is this an Earth/Earthish world; block position ↔ latitude/longitude; scale; elevation, Köppen
  climate class, land-cover class and snowline at a position; `places()` (real places, nullable),
  `landmarks()` (the running world's), `near(lat, lon)` (words for a spot).
- `WorldOptions`: register a per-world option shown on the Earth spawn screen (button + saved per world), read it
  with `get(id)`, change it with `set(server, id, value)`.
- `EarthCommandsApi.register(earth -> earth.then(...))`: add subcommands under `/earth`.
- `Places`, `GeoText`: place records, distances, directions, coordinate text.

World data comes from a `WorldKind` (`Earth.registerKind`); Earth Data and Earthish each register one.

Licence: MIT ([LICENSE](LICENSE)); datasets: [ATTRIBUTION.md](ATTRIBUTION.md).
