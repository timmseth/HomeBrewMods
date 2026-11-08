# TATV 1.2.0 — the Truly All-Terrain Vehicle

A hover-boat for getting around big worlds: it drives on land, skims water, jumps, hovers, flies and glides,
climbs walls and can anchor anywhere.

## Install

Minecraft 26.2, Fabric Loader 0.19.5+, [Fabric API](https://modrinth.com/mod/fabric-api), Java 25. Put
[`tatv-mc26.2-fabric0.19.5-1.2.0.jar`](tatv-mc26.2-fabric0.19.5-1.2.0.jar) in `mods` (server and clients). Works in
any world; nothing else needed.

- `/tatv` summons one next to you, anywhere.
- In WorldBound worlds ([Earth](../EarthDataWorldGen) or [Earthish](../EarthishWorldGen)) every player gets one
  free on first join.
- Keyboard and controller: with [Controlify](https://modrinth.com/mod/controlify) installed it takes gamepad input.

Settings are in `config/tatv.json`.

## Credits and licence

Made by [Seth Timmons](https://timmseth.github.io/). MIT. Its texture is drawn by its own script
(`tools/tatv_texture.py`). Source: [`_source/WorldBound/mods/tatv`](../_source/WorldBound/mods/tatv). Building it
needs the Controlify jar as a compile-only library in `mods/tatv/lib/` (not included; see BUILDING.md).
