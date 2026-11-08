# Earthish World Gen 1.0.0

**A new, realistic planet from every seed.** Not Earth, but built the way Earth's geography came about, then
held to Earth's own numbers.

## Install

1. Minecraft **26.2**, **Fabric Loader 0.19.5+**, **[Fabric API](https://modrinth.com/mod/fabric-api)**, Java 25.
2. Put [`earthish-mc26.2-fabric0.19.5-1.0.0.jar`](earthish-mc26.2-fabric0.19.5-1.0.0.jar) in your `mods` folder (server and clients).
3. Create World → More → World Type: **Earthish**.

Works alone or next to [Earth Data World Gen](../EarthDataWorldGen) (they share the same engine, loaded once).
Add-ons: [Earthish Fossil Record](../EarthishFossilRecord), [Earthish Landmarks](../EarthishLandmarks), [TATV](../TATV).

## What you get
A new, realistic planet that's different every seed (Create World → More → World Type → **Earthish**). It isn't Earth: it's built from your seed the way Earth's geography came about, then held to Earth's own numbers.

- **Plate tectonics.** A supercontinent breaks up and drifts apart: mid-ocean ridges, trenches and volcanic ranges on leading coasts, broad shelves on trailing ones, high ranges and plateaus where continents collide, hotspot island chains.
- **Shaped by rivers.** An erosion simulation cuts ranges into branching valleys.
- **Climate from the geography:** pressure belts and winds, monsoons, rain shadows, cold-current deserts, continental winters and ice sheets. Run on the real Earth, the same model gets Earth's climate zones right on 69% of the land.
- **Rivers and lakes from that rain,** forests, grassland, desert, tundra and wetlands that follow the climate, and Earth's proportions of lowland, mountain and sea.

Creating an Earthish world takes about half a minute longer than usual. Add [Earthish Landmarks](../EarthishLandmarks) for landmarks of its own and [Earthish Fossil Record](../EarthishFossilRecord) for animals.

Everything else comes from the shared engine, same as on Earth: real-feeling terrain heights (a 3,536-block-tall
world), climate biomes, rivers and lakes, caves and ores measured from the ground, the compass (**J**), the
built-in horizon and Distant Horizons support, `/earth where`, `status`, `tp`, `spawn`, `sun`.

## Credits and licence

Made by [Seth Timmons](https://timmseth.github.io/). MIT. Earth's land/ocean height distribution (from the datasets
in [ATTRIBUTION.md](../_source/WorldBound/ATTRIBUTION.md)) is used to shape the planets. Source:
[`_source/WorldBound`](../_source/WorldBound) (`mods/earthish`, engine in `mods/core`).
