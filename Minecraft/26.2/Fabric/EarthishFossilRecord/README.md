# Earthish Fossil Record 1.0.0

Real dinosaurs, at their real sizes, on Earthish planets; plus kangaroos and green anacondas.

## Install

Needs [Earthish World Gen](../EarthishWorldGen) (and Minecraft 26.2, Fabric Loader 0.19.5+, Fabric API, Java 25). Put [`earthish_fossils-mc26.2-fabric0.19.5-1.0.0.jar`](earthish_fossils-mc26.2-fabric0.19.5-1.0.0.jar) in `mods`
(server and clients). The shared animals (`fossilrecord` 1.0.0) are inside the jar; installing both Fossil Records
loads them once.

## Where they live

- **Accurate** (default): every species gets three home ranges on the planet, drawn from its seed, and lives
  there where the climate suits it (tropical, dry, temperate or cold kinds). `/earth fauna where <name>` shows the
  ranges. Kangaroos and anacondas live wherever their ground and climate are.
- **Random**: any of them can turn up anywhere they can live.
- Either way they stay off the open sea, ice caps, snow, peaks over 2,800 m, and only the cold-country kinds go onto
  the tundra. Earthish worlds use the placement last chosen on the Earth spawn screen (Accurate if never), or
  `/earth fauna placement` in the world.

## The animals

- **232 real dinosaurs at their real size** (1 block = 1 m), from the Late Triassic to the end of the Cretaceous:
  Tyrannosaurus, Triceratops, Spinosaurus, Argentinosaurus, Velociraptor, Stegosaurus, Brachiosaurus, Iguanodon and
  220-odd more. Each has one published length and mass for an average adult, its own build and colouring, and a
  voice of its own.
- **They behave like what they were:** lone hunters stalk prey, pack hunters gang up, herds stick together and
  defend each other, browsers reach up into the trees, the small ones flee. Hunters never go for creative players or
  anyone on Peaceful.
- **Raptors can be tamed** (Velociraptor, Deinonychus, Utahraptor, Microraptor and the rest of the 18 raptors).
  Hold out raw meat and a wild one won't go for you; feed it and it may take to you (one in three). A tame raptor
  follows you, stays when told (use it empty-handed), defends you and attacks what you attack, and heals on meat.
- **Every dinosaur lays eggs.** Two fed tame raptors breed and the female lays an egg; a fed tame female lays now
  and then like a hen. Set an egg down on any block and it hatches; a raptor hatchling belongs to whoever hatched it.
- **Kangaroos** (males box, females flee with the mob) and **green anacondas** (they constrict).
- Spawn eggs for all of them in the **Fossil Record** creative tab, A to Z. Nicknames work in commands: trex,
  raptor, trike, spino, stego, bronto, brachio, anky, pachy, dilo, allo, carno, giga, compy, galli, bary.

## Commands

| Command | |
|---|---|
| `/earth fauna` | which animals can live where you stand, and how many dinosaurs are within 128 blocks |
| `/earth fauna list` | every dinosaur, by where it lives |
| `/earth fauna where <name>` | its size, age and where to find it, with block coordinates |
| `/earth fauna spawn <name>` | spawn one (or a group) here |
| `/earth fauna placement [accurate\|random]` | see or change this world's dinosaur placement (operators) |

## Config

`config/fossilrecord.json`: `enabled` (true), `dinosaurs` (true), `spawn_scale` (1.0: how often they're placed),
`debug_commands` (false). Made from the old `fauna` section of `config/worldbound.json` on first run.

Worlds from WorldBound 0.7.x: the dinosaurs already living in them carry over (their old `worldbound:` ids load as
Fossil Record's); spawn eggs and dino eggs left in inventories from before don't.

## Credits and licence

Made by [Seth Timmons](https://timmseth.github.io/). MIT. Textures are painted by the mod's own code from each
animal's body plan; sounds reuse vanilla sound events. Sizes are published estimates (mostly Gregory S. Paul,
*The Princeton Field Guide to Dinosaurs*, and the species' descriptions); fossil sites from the Paleobiology
Database and the literature. Contested findings are named as contested. Source:
[`_source/WorldBound`](../_source/WorldBound) (`mods/fossil-core` and `mods/earthish-fossils`).
