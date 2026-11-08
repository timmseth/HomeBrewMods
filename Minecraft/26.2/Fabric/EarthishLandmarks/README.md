# Earthish Landmarks 1.0.0

Every Earthish planet gets landmarks of its own to discover and travel between.

## Install

Needs [Earthish World Gen](../EarthishWorldGen) (and Minecraft 26.2, Fabric Loader 0.19.5+, Fabric API, Java 25).
Put [`earthish_landmarks-mc26.2-fabric0.19.5-1.0.0.jar`](earthish_landmarks-mc26.2-fabric0.19.5-1.0.0.jar) in
`mods` (server and clients).

## The planet's landmarks

Worked out from the planet itself, so every player sees the same ones, and named from its seed:

- its **highest peaks** (ice-free summits of 2,500 m and up, at least 900 km apart): "Mount Torra", "Jus Peak"...
- the **mouths of its greatest rivers**: "Mouth of the Norluis River"
- the hearts of its biggest **deserts** and **rainforests**: "The Velra Desert"
- the summits of its **ice caps**: "Ka Ice Dome"

Usually 25–35 per planet. The compass names where you are by the nearest one ("120 km NE of Mount Torra").

## Obelisks and travel

- **Discover** a landmark by walking up to its obelisk: a title and a chime, and it's yours.
- **Travel map** (**K**): travel anywhere within 100 km of a place you've discovered (your spawn, obelisks, places
  you've travelled to). Free, no cooldown, and you arrive on finished ground.
- **Landmarks list** in the travel map, sorted by distance, with a guide to each.
- `/earth landmarks`: how many this world has and the nearest five.
- On the Earth spawn screen: **Landmark obelisks** on/off for the new world.

## Credits and licence

Made by [Seth Timmons](https://timmseth.github.io/). MIT. Source: [`_source/WorldBound`](../_source/WorldBound)
(`mods/landmarks-core`, `mods/earthish-landmarks`).
