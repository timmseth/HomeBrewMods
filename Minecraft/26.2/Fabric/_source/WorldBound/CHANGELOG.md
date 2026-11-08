# WorldBound — changelog

Newest first.

## The split — WorldBound becomes seven mods

WorldBound is now separate mods, so you can install just the parts you want:

| Mod | What it adds |
|---|---|
| **Earth Data World Gen** | The real Earth world type, the spawn map, real places |
| **Earthish World Gen** | Generated Earth-like planets |
| **Earth Fossil Record** | The dinosaurs and other animals, at their real fossil sites and ranges |
| **Earthish Fossil Record** | The same animals, in home ranges on generated planets |
| **Earth Landmarks** | Obelisks at real landmarks, the travel map (K), walled cities |
| **Earthish Landmarks** | New: every generated planet gets its own named landmarks to find and travel between |
| **TATV** | The vehicle, now in any world |

- Earthish planets now have landmarks: their highest peaks, greatest river mouths, deserts, rainforests and ice domes, each with a name. The compass names where you are by the nearest one.
- New command `/earth landmarks` lists the nearest landmarks.
- Dinosaurs, landmark obelisks and city walls are buttons on the Earth spawn screen. Each world remembers its own choice.
- The dinosaurs already in your worlds come along. Spawn eggs and dino eggs kept in chests or inventories from before don't.
- Developer commands are hidden unless `debug_commands` is on.
- The Terralith ground-block pack is no longer built in. `tools/terralith_compat.py` makes it from your own Terralith.

## 0.7.7 — a raptor pack of your own

Raptors can be tamed and bred. That covers Velociraptor, Deinonychus, Utahraptor, Microraptor, Dromaeosaurus and the other raptors, 18 kinds in all.

- **Taming.** Hold out raw meat (beef, pork, mutton, chicken or rabbit) and a wild raptor won't go for you unless you hit it. Feed it: each piece has a one-in-three chance to win it over.
  - Hearts mean it's yours. Smoke means try again.
- **Like a dog.**
  - Use it empty-handed to make it stay or follow you.
  - It keeps up with you, and teleports to you if it falls far behind.
  - It goes for whatever attacks you and whatever you attack.
  - It never turns on you, and wild hunters leave it alone.
  - Feed it meat to heal it.
  - Your raptors stick together as a pack. Wild raptors don't join them.
- **Eggs.**
  - Feed meat to two of your healthy raptors and they breed. The female lays an egg.
  - A tame female you keep fed also lays an egg now and then, like a hen.
- **Hatching.** Set an egg down on any block and it hatches. A raptor hatchling belongs to whoever hatched it, so you can raise a pack from eggs.
- **Every dinosaur now lays eggs when it breeds** instead of giving birth. Eggs of other kinds hatch wild.

## 0.7.6 — dinosaurs accurate or random

- **New option: where dinosaurs live.** On the Create World map there is a new button, "Dinosaurs":
  - **Accurate** (as before): each one lives only where its fossils were found.
  - **Random**: any of the 232 can turn up anywhere it can live, all over the world.
  - Random still keeps them off the open sea, ice caps, snow, high peaks and towns. Only the cold-country kinds go onto the tundra.
- **Change it in a world you already have:** `/earth fauna placement random` or `/earth fauna placement accurate`. Type `/earth fauna placement` on its own to see the current setting.
  - Dinosaurs already out there stay where they are.
  - New land, and the odd new arrival near you, follow the new setting.
- **No more pile-ups.** New dinosaurs stop arriving near you once about 16 are already within 128 blocks. Before, camping at a fossil site could slowly fill up with them.
- **Spawn eggs in A to Z order.** The dinosaur eggs in the WorldBound creative tab are now sorted by name.
- **Nicknames work in commands.** `/earth fauna where` and `/earth fauna spawn` accept nicknames such as trex, T-Rex, trike, raptor, spino, stego, bronto, brachio, anky, pachy, dilo, allo, carno, giga, compy, galli and bary.

## 0.7.5 — thirty-five more dinosaurs (232 in all)

Thirty-five more real species. Each one's length and weight come as a pair from the same published estimate, most from Gregory Paul's field guide.

**Hunters**
- Zhuchengtyrannus (Shandong), a tyrannosaur close to T. rex's size.
- Ekrixinatosaurus (Neuquén), the "explosion-born lizard", blasted out during pipeline work.
- Eustreptospondylus (Oxford).
- Liliensternus (Thuringia), Europe's best-known Triassic hunter.
- Tawa (Ghost Ranch).
- Feathered hunters from Liaoning:
  - Dilong, an early cousin of Tyrannosaurus.
  - Sinocalliopteryx, found with its last meals inside it.
  - Zhenyuanlong, a big-winged raptor that couldn't fly.
- More raptors: Acheroraptor (Hell Creek), Bambiraptor (Montana), Atrociraptor (Alberta), Tsaagan (the Gobi).
- Avimimus (the Gobi), the "bird mimic".
- Elaphrosaurus (Tanzania), long-legged and shallow-chested.

**Long-necks**
- Sauroposeidon (Oklahoma, Texas, Wyoming), head held 17 metres up.
- Cetiosaurus (Oxfordshire and Rutland), the first long-neck ever named.
- Atlasaurus (Morocco's High Atlas), very long legs and a short neck.
- Barapasaurus (central India).
- Opisthocoelicaudia (the Gobi).
- Haplocanthosaurus (the Morrison Formation).
- Neuquensaurus (Río Negro), armoured.
- Early cousins: Riojasaurus (La Rioja) and Thecodontosaurus (Bristol), one of the first dinosaurs ever named.

**Plant-eaters**
- Duckbills:
  - Amurosaurus (Blagoveshchensk), Russia's most complete dinosaur.
  - Charonosaurus (Heilongjiang), with a long tube crest.
  - Kritosaurus (New Mexico), with its humped nose.
- Small runners: Gasparinisaura (Río Negro), Orodromeus (Montana).
- Horned:
  - Utahceratops (Utah), with sideways brow horns.
  - Zuniceratops (New Mexico), found by an 8-year-old.
  - Bagaceratops (the Gobi), small.
- Plated and armoured: Hesperosaurus (Wyoming), Wuerhosaurus (Xinjiang), Scolosaurus (Alberta).
- Domed: Sphaerotholus (New Mexico to Alberta), the "ball dome".

## 0.7.4 — forty-one more dinosaurs (197 in all)

Forty-one more real species, each with its length, weight, dates and sites taken from published sources.

**Hunters**
- Big ones:
  - Tyrannotitan (Chubut), as big as Giganotosaurus.
  - Yangchuanosaurus (Sichuan), with a ridge along its nose.
  - Teratophoneus (Utah); a quarry there holds a group that died together.
  - Sinraptor (Xinjiang), whose skull carries healed bites.
  - Megaraptor (Neuquén), with a giant thumb claw.
- Mid-sized ones:
  - Crested Sinosaurus (Yunnan) and Monolophosaurus (Xinjiang).
  - Neovenator (Isle of Wight).
  - Aucasaurus (Neuquén), from a titanosaur nesting ground.
  - Lythronax (Utah), one of the oldest tyrannosaurids.
  - Nanuqsaurus, the "polar bear lizard" of Alaska's North Slope.
- Raptors:
  - Austroraptor (Río Negro), a giant with short arms and a long snout.
  - Buitreraptor (Río Negro).
  - Sinornithosaurus (Liaoning), the first feathered dromaeosaur found.
  - Saurornitholestes (Alberta, Montana and New Mexico).
  - Halszkaraptor (the Gobi), mallard-sized, with a swan-like neck and flipper-like arms.
- Odd ones:
  - Anzu (Hell Creek), the crested "chicken from hell".
  - Limusaurus (Xinjiang), which lost its teeth as it grew up.
  - Mononykus (the Gobi), with a single claw on each stubby arm.
  - Pelecanimimus (Spain), with about 220 tiny teeth and a throat pouch.
  - Beipiaosaurus (Liaoning) and Nothronychus (New Mexico and Utah), clawed plant-eaters.

**Long-necks**
- Supersaurus (the Morrison Formation), one of the longest dinosaurs known.
- Omeisaurus (Sichuan), with a neck nearly half its length.
- Futalognkosaurus (Neuquén), with shark-fin spines on its neck.
- Dicraeosaurus (Tanzania), with forked spines making a double ridge down its back.
- Brachytrachelopan (Chubut), with the shortest neck of any sauropod.
- Lufengosaurus (Yunnan), China's first mounted dinosaur skeleton.

**Plant-eaters**
- Scutellosaurus (Arizona), a small runner covered in little armour plates.
- Camptosaurus and Dryosaurus (the Morrison Formation).
- Yinlong (Xinjiang), one of the earliest horned dinosaurs, still on two legs.
- Prosaurolophus (Alberta and Montana).
- Horned dinosaurs: Achelousaurus (Montana), with bony bosses instead of horns; Diabloceratops (Utah), the "devil-horned face"; Anchiceratops (Alberta); Leptoceratops (Hell Creek), hornless and almost frill-less.
- Armoured: Polacanthus (Isle of Wight), with a bony shield over its hips; Hylaeosaurus (Sussex), one of the three dinosaurs used to define the word "dinosaur" in 1842; Saichania and Tarchia (the Gobi), club-tailed ankylosaurs.

## 0.7.3 — fifty-two more dinosaurs (156 in all), from new countries

Fifty-two more real species. Many come from places that had no dinosaurs until now: Japan, India, Brazil, Romania, Hungary, Italy, Laos, Thailand, Malawi, Uzbekistan, Siberia and the US East Coast.

**New places**
- **Japan**: Kamuysaurus (Mukawa, Hokkaido), one of Japan's most complete dinosaur skeletons; Fukuiraptor and Fukuisaurus (Katsuyama, Fukui).
- **India**: Rajasaurus (Gujarat), with a single low horn on its head; Isisaurus (central India), a titanosaur with a short neck and long front legs.
- **Brazil**: Irritator (Ceará), a fish-eating spinosaur; Staurikosaurus and Saturnalia (Rio Grande do Sul), two of the first dinosaurs.
- **The islands of Cretaceous Europe**: Romania's Hațeg island had dwarf giants: Magyarosaurus, a titanosaur hardly bigger than a horse, plus Telmatosaurus, Zalmoxes and Balaur, a raptor with two sickle claws on each foot. Also Hungarosaurus (Hungary), Tethyshadros (Trieste, Italy), and Ampelosaurus and Rhabdodon (southern France).
- **Southeast Asia and Africa**: Ichthyovenator (Laos), a spinosaur with a two-humped sail; Phuwiangosaurus (Thailand); Malawisaurus (Malawi); Jobaria, Afrovenator and Rugops (Niger); Paralititan (Egypt); Masiakasaurus and Rapetosaurus (Madagascar).
- **Siberia**: Kulindadromeus, a small plant-eater with scales and filaments that may be simple feathers.
- **Uzbekistan**: Timurlengia, a horse-sized cousin of Tyrannosaurus.
- **The US East Coast**: Hadrosaurus (New Jersey), the first dinosaur skeleton identified in North America; Dryptosaurus (New Jersey), a long-armed tyrannosaur; Appalachiosaurus (Alabama); Anchisaurus (Connecticut).

**More from familiar places**
- **The Gobi and China**:
  - Deinocheirus: 11.5 m, with giant arms, a humped back and a duck-like bill.
  - Alioramus and Qianzhousaurus ("Pinocchio rex"), long-snouted tyrannosaurs.
  - Gigantoraptor, an Oviraptor cousin as big as a mid-sized tyrannosaur.
  - Caudipteryx, with its tail fan.
  - Anchiornis, coloured as its fossils show: grey, with black-spangled white wings and a reddish crown.
  - Guanlong, with a crest along its snout.
  - Tsintaosaurus, with a tall crest.
  - Gigantspinosaurus, with shoulder spikes twice as long as its shoulder blades.
- **North America**:
  - Ornithomimus (Alberta), feathered; grown-ups seem to have had wing-like arms.
  - Dromaeosaurus (Alberta).
  - Hypacrosaurus (Alberta and Montana), with a helmet crest and tall back spines.
  - Pentaceratops (New Mexico), the "five-horned face" with a giant frill.
  - Regaliceratops (Alberta), nicknamed Hellboy.
  - Barosaurus (the Morrison Formation).
- **South America**: Eoraptor (Valley of the Moon), Skorpiovenator (Neuquén), Dreadnoughtus (Santa Cruz), one of the most complete giant titanosaurs, and Mussaurus (Santa Cruz), whose nesting ground held 80 skeletons from embryos to adults.
- **Spain and Australia**: Turiasaurus (Teruel), one of Europe's biggest dinosaurs; Diamantinasaurus (Winton, Queensland), found beside Australovenator.

`/earth fauna spawn` also takes the nicknames `pinocchio_rex` (Qianzhousaurus) and `hellboy` (Regaliceratops).

**Also in this version**
- Long shoulder spikes taper to a point instead of looking like planks. This shows on Edmontonia, Kentrosaurus, Sauropelta and the other spiky ones.

## 0.7.2 — forty-one more dinosaurs (104 in all)

Forty-one more real species, each at its own fossil formations. This batch adds the first dinosaurs from Australia, southern Africa, Russia and Spain.

**Theropods**
- Megalosaurus (Stonesfield, Oxfordshire), the first dinosaur ever named, in 1824.
- Big hunters: Torvosaurus (the Morrison Formation and Portugal) and Mapusaurus (Neuquén, Argentina). Mapusaurus sometimes hunts in a group: a bonebed of at least seven may mean they lived together.
- Concavenator (Las Hoyas, Spain), with a hump just in front of its hips.
- Australovenator (Winton, Queensland), with big hand claws.
- Herrerasaurus (Valley of the Moon, Argentina), one of the earliest dinosaurs.
- Stenonychosaurus, long known as Troodon (Alberta and Montana). It has big forward-facing eyes. `/earth fauna spawn troodon` works too.
- Sinosauropteryx (Liaoning), the first dinosaur other than a bird found with feathers. It has the pattern its fossils show: a dark back, pale belly, banded tail and dark mask.

**Long-necks**
- Shunosaurus (Sichuan), with a spiked club on its tail.
- Saltasaurus (El Brete, southern Salta, Argentina), its back studded with bony plates.
- Nigersaurus (Niger), with its wide, straight-edged mouth held low to the ground.
- Alamosaurus (New Mexico, Texas and Utah), the last of North America's giant sauropods.
- Europasaurus (Lower Saxony, Germany), an island dwarf only about six metres long.
- Massospondylus (South Africa and Lesotho), an early two-legged cousin.

**Duckbills and kin**
- Saurolophus (the Gobi and Alberta), with a spike-shaped crest.
- Shantungosaurus (Shandong, China), the biggest duckbill: as long as a bus.
- Brachylophosaurus (Montana and Alberta), with a flat crest over its head; Olorotitan (Russia's Amur region), with a hatchet crest; Gryposaurus (Alberta and Utah), with a humped nose.
- Muttaburrasaurus (Queensland) and Mantellisaurus (the Isle of Wight and the Wealden).
- Small runners:
  - Thescelosaurus (Hell Creek).
  - Oryctodromeus (Montana and eastern Idaho), which dug burrows.
  - Leaellynasaura (Dinosaur Cove, Victoria), which lived inside the Antarctic Circle of its day.
  - Heterodontosaurus (South Africa), with tusks in both jaws.

**Horned dinosaurs**
- Einiosaurus (Montana), with a nose horn that curves down.
- Torosaurus (Hell Creek to Texas), with one of the biggest skulls of any land animal.
- Nasutoceratops (Utah), big-nosed, with long horns over its eyes.
- Sinoceratops (Shandong), the first big horned dinosaur found in China.

**Armoured and spiky**
- Stegosaurs:
  - Miragaia (Portugal), which had a long neck.
  - Tuojiangosaurus and Huayangosaurus (Sichuan).
  - Dacentrurus (Wiltshire and Portugal).
- Nodosaurs:
  - Edmontonia (Alberta), with big shoulder spikes pointing forward.
  - Sauropelta (Montana and Wyoming), with tall shoulder spikes.
  - Gastonia (Utah).
  - Minmi (Queensland).
- Scelidosaurus (Dorset), one of the first armoured dinosaurs.
- Club-tailed ankylosaurs: Pinacosaurus (the Gobi), which lives in groups, and Zuul (Montana).
- Prenocephale (the Gobi), a dome-head.

**Corrections to earlier dinosaurs**

Every animal's numbers, places, dates and notes were checked against published sources. Fixes to the ones from 0.7.0 and 0.7.1:
- `/earth fauna where` now names every species of the animal that lives at its sites. For example, Edmontosaurus regalis is the one at Drumheller and E. annectens is the one in Hell Creek. Animals affected: Allosaurus, Stegosaurus, Edmontosaurus, Parasaurolophus, Daspletosaurus, Mamenchisaurus and Pachyrhinosaurus.
- Parasaurolophus and Lambeosaurus are 7.5 m long, the size of an average adult (they were 9.5 m and 9 m).
- Microraptor's sheen is a faint crow-like blue, not blue-green.
- Dates: Dilophosaurus lived about 186 million years ago, and Patagotitan about 102.
- Borealopelta's reddish-brown colour is now described as what chemical traces suggest, which not everyone accepts.
- Psittacosaurus's note now says its colours come from a Psittacosaurus fossil from the same beds (the species of that fossil isn't settled).
- The Gobi's Djadochta range reaches a little further west, so all of Ukhaa Tolgod, where Citipati was found, is inside it.

## 0.7.1 — forty more dinosaurs (63 in all)

Forty more real species, each at its own fossil formations.

**Theropods**
- Tyrannosaurids: Albertosaurus (Drumheller), Gorgosaurus and Daspletosaurus (southern Alberta; Daspletosaurus also in Montana), Tarbosaurus (the Gobi), and the feathered Yutyrannus (Liaoning, China).
- Big hunters: Carcharodontosaurus (Morocco and Egypt), Acrocanthosaurus with its ridged back (Oklahoma and Texas), Ceratosaurus with its nose horn and a row of bony scutes (Colorado to Portugal), and Majungasaurus (Madagascar).
- Spinosaurids that fish and swim: Baryonyx with its great thumb claws (Surrey and the Isle of Wight) and Suchomimus (Niger).
- Pack hunters: Utahraptor (Utah) and Deinonychus (Montana, Wyoming, Oklahoma). Coelophysis hunts in packs at Ghost Ranch, New Mexico.
- Microraptor (Liaoning): crow-sized with feathered arms and legs, glossy black with a blue sheen, the colour preserved in its fossils.
- Struthiomimus (Alberta), an ostrich-like runner, and the tall-crested Citipati (Gobi).

**Long-necks**
- Apatosaurus and Brontosaurus (the Morrison Formation; Brontosaurus is its own genus again).
- Camarasaurus, the commonest Morrison sauropod.
- Giraffatitan (Tanzania), Mamenchisaurus with the longest neck here (Sichuan and Xinjiang), and Amargasaurus with twin rows of neck spines (Argentina).
- Patagotitan, the second titan of Patagonia.
- Plateosaurus (southern Germany and Switzerland), a two-legged early cousin.

**Duckbills and kin**
- Crested hadrosaurs: Corythosaurus (helmet crest) and Lambeosaurus (hatchet crest), both southern Alberta.
- Maiasaura, the "good mother lizard", in big herds in Montana.
- Ouranosaurus with its sail (Niger).
- Tenontosaurus (Montana to Oklahoma, and Idaho), Deinonychus's favourite prey.
- Hypsilophodon, small and quick (Isle of Wight).

**Horned dinosaurs**
- Centrosaurus (in big herds), Chasmosaurus with its long frill, and Pachyrhinosaurus with a bony boss on its nose. Pachyrhinosaurus lives as far north as Alaska.
- Kosmoceratops (Utah), with fifteen horns.
- Psittacosaurus (Liaoning), two-legged and parrot-beaked, coloured dark above and pale below as its fossils show.

**Armoured and spiky**
- Kentrosaurus (Tanzania): plates in front, long spikes behind.
- Euoplocephalus (club tail).
- Borealopelta: reddish-brown, as its mummified fossil shows.
- Stegoceras, a small dome-head.

**Also in this version**
- **Fixed: the Create World screen showed WorldBound's "Earth: …" spawn button twice.** Each time the window was resized, the screen built the button again without removing the old one. Now there's only ever one, in the right place for the window size. (The duplicate "Spawn Map" button in the screenshot comes from the SoulBound mod, which has the same bug.)
- Styracosaurus has a shorter face and a taller, thicker frill. All the frills stand out better.

## 0.7.0 — dinosaurs, living where their fossils were found

Twenty-three real dinosaurs, at their real size, living where their bones were dug up. On an Earth world you'll find Tyrannosaurus in the Hell Creek badlands of Montana, Velociraptor in the Gobi, Spinosaurus in Morocco's Kem Kem beds and Egypt's Bahariya Oasis, Argentinosaurus in Patagonia and Iguanodon at Bernissart in Belgium. On an Earthish planet, each kind gets three home ranges of its own where the climate suits it.

**Where they are**
- **Hell Creek** (Montana, the Dakotas, Wyoming, Alberta, Saskatchewan): Tyrannosaurus, Triceratops, Ankylosaurus, Pachycephalosaurus, Edmontosaurus.
- **Southern Alberta** and the American southwest (Utah, New Mexico): Parasaurolophus, Styracosaurus.
- **The Morrison Formation** (Colorado, Utah, Wyoming): Allosaurus, Stegosaurus, Brachiosaurus, Diplodocus. Allosaurus and Stegosaurus also live in Portugal.
- **The Gobi**: Velociraptor, Protoceratops and Oviraptor at the Flaming Cliffs; Gallimimus and Therizinosaurus in the Nemegt Basin.
- **Argentina**: Giganotosaurus and Argentinosaurus in Neuquén; Carnotaurus in Chubut.
- **North Africa**: Spinosaurus.
- **Europe**: Iguanodon (southern England, Belgium, Spain, Germany) and Compsognathus (Bavaria, Provence).
- **Arizona**: Dilophosaurus.
- `/earth fauna where <name>` gives the coordinates of each site, nearest first. `/earth fauna list` shows who lives where.

**Real size.** Each dinosaur is built from its own skeleton's proportions and drawn full size, from the metre-long Compsognathus to the 33-metre Argentinosaurus. Tyrannosaurus's hips are 3.4 blocks off the ground, and Brachiosaurus holds its head 12 blocks up. Horns, frills, crests, sails, plates, tail spikes, clubs, armour and feathers are all modelled. The young hatch tiny and grow slowly; the giants take over an hour to grow up.

**Hunters** (Tyrannosaurus, Giganotosaurus, Allosaurus, Carnotaurus, Dilophosaurus, Spinosaurus)
- They get hungry and hunt animals in their size range.
- They go for you if you come too close, or from further away if they're hungry. Crouch, and they notice you at half the distance.
- The big ones roar when they pick a target.
- They never go for creative players, and never for anyone on Peaceful.

**Packs and skulkers**
- Velociraptors hunt in packs: when one goes for something, the rest join in, leaping in for the kill.
- Compsognathus go after small animals and scatter from people.

**Plant-eaters that stand their ground** (Triceratops, Styracosaurus, Stegosaurus, Ankylosaurus, the sauropods, Pachycephalosaurus, Therizinosaurus, Iguanodon)
- Hit one and it fights back, and its herd joins in.
- They fight with horn charges, tail clubs and spikes, head-butts, stamps and claws.

**Plant-eaters that run** (Edmontosaurus, Parasaurolophus, Gallimimus, Protoceratops, Oviraptor)
- They run from hunters, and from you if you walk up to them. Crouch to get close.
- Parasaurolophus calls with a low horn.

**Living together**
- Herds keep together, and plant-eaters stop to graze.
- The big ones shake the ground as they walk and push through leaves (if mob griefing is on).

**Breeding and drops**
- Plant-eaters breed with ferns or leaves, hunters with raw meat. Spinosaurus also takes fish, and Oviraptor takes eggs.
- They drop meat (beef from the big ones, chicken from the feathered ones), bones, and hide or feathers.

**Getting them**
- Spawn eggs are in a new creative tab, WorldBound Animals.
- With cheats on, `/earth fauna spawn <name>` places a group.
- `/earth fauna` tells you which dinosaurs live where you stand.
- Turn them off in the config (`config/worldbound.json`, `fauna.dinosaurs`); `fauna.spawn_scale` thins or thickens them along with the other animals.

Models, textures and animation have only been checked in previews outside the game so far, so this is their first real look.

## 0.6.0 — kangaroos in Australia, giant anacondas in the Amazon

WorldBound's first animals, and they live where they really live: on an Earth world you'll find kangaroos only in Australia and green anacondas only in the Amazon (and on an Earthish planet, in the climates that suit them).

**Kangaroos** (Australia, mainland and Tasmania; grassland, scrub, savanna, open woodland and farmland, not rainforest or mountains)
- Red kangaroos in the dry interior, eastern greys in the wetter east and south: the colour comes from the climate where they live.
- They graze in mobs of two to six, with a joey now and then, and bound along with both feet together, stepping up whole blocks in their stride.
- Frighten one and it bounds off. A grown male (bigger than the rest) stands his ground when hit, leans back on his tail and kicks both feet into you, hard enough to send you flying. Never a creative player, and never on Peaceful.
- Wheat or grass to tempt and breed them. They drop leather and mutton.

**Green anacondas** (the Amazon and Orinoco basins and the Guianas, hot lowland near water)
- Four to eight blocks long, and one in eight is a giant of nine to eleven: more health and a harder bite the bigger it is.
- It lies coiled at the water's edge with its head resting on its coils, trails its body behind it as it moves, and is quick in the water but slow on land (you can walk away from one).
- It hisses at you when you come near. Step right up to it, or hit it, and it strikes; what it bites, it wraps up in its coils and squeezes, slowing you to a crawl and hurting you every second until the hold runs out, you fight your way out by hitting it, or someone dies. When it's hungry it hunts small animals the same way.
- It never goes for creative players, or for anyone on Peaceful. It drops leather.

Where and how many:
- They are placed by WorldBound itself, not by Minecraft's biome spawn lists: a chance in each newly generated chunk where the land suits them (as Minecraft does with animals), and a slow top-up near you if there are only a few around. Ordinary animals spawn as before everywhere.
- `/earth fauna` tells you which of them live where you stand and how many are near. `/earth fauna spawn kangaroo|anaconda` places a group (cheats on).
- Config (`config/worldbound.json`, `fauna`): `enabled` (default true) and `spawn_scale` (1.0; 0.5 for half as many, 2 for twice).
- Spawn eggs for both are in the Spawn Eggs tab.

## 0.5.1 — the horizon no longer covers blocks you've broken

- **Fixed: broken or blasted-out ground could still look solid.** The horizon (WorldBound's own distant terrain) started at 60% of your render distance and was drawn wherever its ground was nearer than the real blocks. Out there, a hole you dug or shot into the ground was farther away than the horizon's untouched surface, so the old ground stayed on screen even though the blocks were gone (and in your inventory). Now the horizon checks chunk by chunk and stays out of every chunk Minecraft is drawing itself, so what you see near you is always the real world, holes and all.
- Toward the edge of your view, chunks Minecraft hasn't drawn yet (while they're still loading) now show the horizon's terrain instead of sky.
- `/earthhorizon status` now also shows how many real chunks the horizon is staying out of.

## 0.5.0 — Earthish: a new world type, a realistic planet that's different every seed

**New world type: Earthish** (Create World → More → World Type → Earthish). It isn't Earth: it's a whole new planet made from your world seed the way Earth's geography came about, then held to Earth's own numbers. Every seed is a different world with its own continents, oceans, ranges, rivers and climates.

How the planet is made (when the world is created; it's kept in the world folder, so reopening is quick):

- **Plate tectonics.** A supercontinent breaks up and its pieces drift apart. New oceans open behind them with a mid-ocean ridge down the middle and coasts that fit together across them. Leading coasts get a deep trench offshore and a volcanic range inland (like the Andes); trailing coasts get broad shallow shelves. Where continents collide, high ranges rise with a plateau behind them (like the Himalaya and Tibet). Worn-down old ranges run along old rift lines and inside continents, and hotspot volcano chains trail across the oceans (like Hawaii).
- **Shaped by rivers.** An erosion simulation lets rain run off the land, so ranges are cut into branching valleys and hills drain into rivers instead of being noise.
- **Earth's proportions.** As much lowland, upland, mountain and sea floor, by area, as the real planet; about 29% land.
- **Climate from the geography.** Seasonal pressure belts and winds, monsoons, rain carried in from warm seas and wrung out on mountain slopes, rain shadows behind them, dry coasts beside cold ocean currents, mild coasts beside warm ones, bitter continental winters, and ice sheets wherever summers never thaw. Run on the real Earth's terrain, this model gets Earth's climate zones right on 69% of the land.
- **Rivers and lakes from that rain.** Rivers grow downstream, meander like real ones and sit in valleys cut to their beds. Lakes fill closed basins; in dry country a lake settles where evaporation balances what flows in (like the Great Salt Lake), and a basin that gets too little water stays a dry pan. Lakes cover about the same share of the land as on Earth.
- **Forests, grassland, desert, tundra and wetlands** follow the climate, with natural clearings and patches, and tree density follows too.

In play:

- You start on temperate low ground near a river or the coast, and the map's scale is true where you start.
- Everything from Earth worlds works the same: biomes (Terralith and Terrestria biomes appear in the climates that suit them), the compass (it says "Earthish" where it would name a town), seasons, the horizon, Distant Horizons.
- Off in Earthish worlds, because they're about real places: landmarks and fast travel, city walls, town names. Nothing is downloaded.
- Creating an Earthish world takes about half a minute longer than usual (building the planet). Reopening it takes a few seconds.
- On a server, players' clients build the same planet in the background for their map and horizon.

## 0.4.32 — new worlds open fast again, and smoother, more natural terrain

- **Fixed: a new Earth world could sit on "Loading terrain…" for minutes.** Minecraft first searched for a spawn near the map's origin, generating ~120 chunks there that you never see (WorldBound then moves you to your picked spot), and with 0.4.31's fresh elevation downloads each of those chunks waited on the network again and again. Earth worlds now skip that search: the data under your chosen spawn downloads once, up front (up to 30 s), and the world opens there. While playing, a chunk never re-waits on a download that already timed out, and a chunk's total wait for data is capped at `near_wait_seconds` (4 s).
- **Smoother real terrain.** Elevation between data points is now a smooth curve instead of flat facets with creases (which showed as planes and grid lines on every slope), and peaks keep their points.
- **Natural ground texture.** Real data is 30–90 m per sample, so the mod adds what's missing at block scale: gentle single-block bumps on all land (flat beaches and shores stay flat), and on steep ground ribs, ledges and gullies that grow with the steepness.
- **No more square patches.** Climate (~1 km), vegetation (~300 m) and land cover (10 m) are each sampled with noise scaled to their own pixel size, so snowfields, forests and biome edges get irregular natural boundaries instead of data-pixel squares.
- **Snow slides off cliffs**: no snow layers on faces steeper than ~45°, at world generation and during snowfall.

## 0.4.31 — bug hunt: no more underground lava sheets, real lake beds, cleaner terrain data

A full review of the code turned up these; all fixed. **Start a new world** to get all of it (old chunks keep what they were generated with).

- **Lava is gone from where it shouldn't be.** Vanilla's cave and canyon carvers filled every tunnel below y −700 with lava: lava tubes just under every lowland near sea level (Netherlands, Amazon, coasts) and lava pockets floating in the deep ocean. Carved space is now air in rock and water in the sea, never lava. Tunnels and ravines are kept (`vanilla_caves` in the config).
- **Lake and river beds get sand, gravel and dirt** instead of bare stone, and so do narrow valley floors (the surface rules were measuring depth from a blend of points 16 blocks apart).
- **Smoother real terrain from streamed 30 m elevation.** It was sampled nearest-pixel, which showed in game as straight ridges and flat bands every ~40 blocks. Now it's interpolated. Your old elevation downloads are dropped once and re-download as you explore. A firewall or proxy error no longer marks an area as "no fine data" forever, and "no data" marks are rechecked after 30 days.
- **Distant Horizons terrain matches the chunks on mountainsides** (steepness was measured in different units, so LODs showed grass where chunks are rock).
- **WorldBound's horizon renderer draws now.** Minecraft 26.2 uses reversed depth; the first version drew nothing without shaders. It is still off in your config and still pauses under Iris shaders. Press O to try it.
- **Safety on servers and LAN:** travel requests are validated (a modified client could freeze the server), one trip at a time, and a trip cancels if you die or go through a portal while it loads. `/earth` commands that teleport or generate lots of chunks now need cheats/operator (`where`, `status`, `nearby`, `city` stay open to everyone).
- **Your world's settings file is written safely with a backup.** An unreadable `worldbound.json` used to be silently replaced, which could switch a 1:3 world to 1:10 for good. The same protection now covers landmark discoveries and the config.
- **Screenshot tour:** time really stops during each shot now (26.2 renamed the game rule), and your game mode is restored afterwards.
- The South Pole landmark moved to −89.4° so it can actually be reached.
- Smaller: newly downloaded data no longer mixes with old data inside one chunk (it could leave a grid of spikes or pits); ores and springs that would land outside the world are skipped cleanly; columns exactly at the South Pole no longer read as deep ocean; distant terrain no longer shows dry pits along some coasts.

## 0.4.30 — faster chunk generation, seasons really off

- **Chunks generate about 30–40% faster** (measured at Mt Fuji and Pocatello on 30 m data). Three fixes:
  - Biomes were being worked out ~14,000 times per chunk instead of 16 (Minecraft asks for every 4×4×4 cell and the cache only remembered one column at a time). Now each chunk's 16 biome columns are computed once.
  - Caves are computed on a 4-block lattice and blended, instead of four noise lookups for every block in a ~260-block band. Same caves (cave volume within 1% in tests), a fraction of the work. Sections that are entirely underground start as solid rock and only the caves are carved in.
  - Villages and other structures read the ground height straight from the Earth data instead of evaluating terrain noise down a 3,500-block column.
- **Seasons are now actually off** for configs written before 0.4.21 (yours still said `seasons: true`, so seasonal snow was still on). Config files now carry a version number so changed defaults reach old files.

## 0.4.29 — horizon toggle in game, two fixes

- **Press O** (or `/earthhorizon`) for the horizon settings screen: on/off, range, detail, where it starts, and what to do when Distant Horizons is installed. Settings save to the config.
- Fixed: the horizon was drawn right next to the player (only the tile you stood in was skipped), so a dug block showed horizon colour instead of a hole. Nothing is drawn inside the start radius now.
- With an Iris shader pack active the horizon pauses (Iris redraws the frame with its own programs and ours never reached the screen); the settings screen says so. Shaders off = horizon on. Proper shader-pack support is a separate job.

## 0.4.28 — WorldBound's own horizon (Distant Horizons replacement, first cut)

- **The world now draws out to the horizon on its own** — no Distant Horizons needed. WorldBound builds the far terrain straight from the Earth data (the same elevation, climate, land-cover and tree-cover layers the chunks come from), as columns 2 blocks wide near you doubling to 512 blocks wide far out, to **32,768 blocks (≈100 km at 1:3) in every direction** by default. Nothing is generated, lit or saved for it: it costs a few milliseconds per tile on two background threads and lives on the GPU.
- Coloured the way the real blocks look from afar: biome-tinted grass and leaves (forests rise to canopy height where the tree-cover map says so), sand, stone, badlands, snow caps and seasonal snow, ice; water in the biome's colour, darker with depth. Mountains are shaded by slope. Day/night, night vision and weather/cave/underwater fog match the real chunks, and the vanilla fog wall moves out to the horizon so chunks blend into the distance instead of fading into a curtain.
- **Distant Horizons:** when DH is installed, WorldBound's horizon stays off and DH keeps doing its job (set `horizon.with_distant_horizons: true` to see both). To try ours, disable the DH mod in the pack. `/earthhorizon` shows what it is doing, `/earthhorizon off|on` toggles it (saved), `/earthhorizon reload` rebuilds the tiles. Config `horizon`: `range_blocks`, `detail` (2.0; higher = sharper and heavier), `overdraw` (0.6 of the render distance), `threads`.
- First cut: singleplayer only (the data has to be on this machine), realistic colours even in Wild biome mode, no shader-pack integration, and far cities/structures are not drawn (walls and obelisks appear with the chunks). Expect the join between chunks and horizon to be visible at the render-distance edge; that is the next thing to tune.

## 0.4.27 — the hadal zone: oceans clamped at 6 km, trenches, and holes in the world

- **The world is now 3,536 blocks tall** (floor at y −1504, was −2032 / 4,064). Land is unchanged; the ocean floor is true to 6,000 m (every abyssal plain, ridge and seamount keeps its real depth) and the deep trenches below that are cut as narrow canyons, 1 block per 150 m. That removes 33 of the 254 chunk sections: about 13% less lighting, saving and network work per chunk.
- **Trench floors** are deepslate and tuff with sculk patches (and the odd catalyst), magma vents that pull you down and soul-sand vents whose bubble columns carry you back up: swim into one and ride it 700+ blocks to the surface.
- **The deepest spots on Earth are open to the void.** Where the real ocean is deeper than 10,000 m (Challenger Deep, Horizon Deep and a few others) there is no floor: the water column ends at the bottom of the world and you can swim straight out of it. A bedrock rim marks the edge. `trench_void: false` in the config gives those spots a flat bedrock floor instead.
- **Start a new world for this.** Existing Earth worlds were generated 4,064 tall; the game will load them at the new height, but ocean floors won't match between old and new chunks and anything saved below y −1504 is gone.

## 0.4.26 — chunk pruning removed

- The "don't save unchanged chunks" feature from 0.4.24 is gone: dropping and regenerating chunks made terrain pop in and out around the player. Chunks are saved normally again. (Worlds played on 0.4.24/0.4.25 just regenerate what was dropped as you revisit it.)

## 0.4.25 — walled cities

- **Every city of 500,000+ people has a ring wall** around its core (1.5–4.5 km real across, sized by population): 7 blocks thick, 2 of foundation, a lit 5-tall hallway inside with arrow slits, a 5-wide walkway on top between crenellated parapets, 9×9 towers every ~70 blocks with rooms, ladders and roof terraces, and four gatehouses. The ring is an irregular loop that follows the ground and stops at the water's edge.
- **A keep at the city centre** — two tiers, 21 blocks tall, with a ladder up the middle — carries a **landmark obelisk on top**.
- **Nine regional styles:** stone (Europe), sandstone (Middle East / North Africa / Central Asia), red fort (South Asia), eastern deepslate with red tile (East Asia), kremlin brick (Russia), mud brick (sub-Saharan Africa), colonial brick (North America), Inca andesite (Latin America), mossy tropical (Southeast Asia).
- `/earth city <name>` tells you where a city's keep and gates are. Config `city_walls` turns it off. Two new landmarks: Ruhr valley at Mülheim, Tafí del Valle.
- Chunk pruning now also keeps chunks whose creatures you've traded with, tamed, leashed, sheared or ridden.

## 0.4.24 — small saves: unchanged chunks aren't kept

- **Chunks nobody changed are no longer written to disk.** Earth terrain regenerates identically from the data, so a chunk you only flew over is dropped when it unloads and built again on your next visit. Kept always: chunks a player broke, placed or used a block in (plus their neighbours), chunks holding named, tamed or leashed animals, dropped items, boats/minecarts, item frames and armor stands, the 8 chunks around world spawn and every player's bed, and wherever a player is standing. Dropped chunks take their wild animals with them (they respawn with the terrain).
- **`/earth prune`** (ops) cleans an existing world the same way in the background; `/earth prune stats` shows this session's kept/dropped counts. Config `prune_unchanged_chunks` (on by default) turns it off.
- Note: Minecraft never shrinks region files in place, so a cleaned world's files keep their size until the game reuses the space; the chunks inside are gone.

## 0.4.23 — screenshot tour

- **`/earthshot tour`** flies to every landmark (106), looks at it from the south-east, 45° down, and takes a screenshot at day, sunset, night and sunrise, waiting at each for the land to load. Runs in spectator mode and gives you your game mode back after. `/earthshot tour landmarks day,sunset` limits it; `/earthshot stop` cancels.
- **Your own views:** stand where you like, `/earthshot add South Rim`, then `/earthshot tour views` (or `all`). `/earthshot list`, `remove`, `go <name>`.
- Screenshots land in `screenshots/worldbound/` named `<view>__<time>__v<version>.png`, with a **gallery.html** that shows each view at the four times and, where an earlier version exists, that one small beneath for comparison.

## 0.4.22 — Wild biome mode

- **New world option "Biomes: Realistic / Wild"** on the Earth spawn screen. Wild keeps the real planet's shape (elevation, coasts, rivers, lakes, glaciers, snow caps, shores) but covers the land in big ragged patches (~3–4 km real) drawn from **every overworld biome installed**: vanilla plus Terralith, Terrestria and any other biome mod. Mountain-type biomes go on high or steep ground, everything else on the lowlands. Each biome keeps its own weather (a snowy biome snows, a desert is dry) with no "colder every block up" effect.
- Realistic mode is unchanged and remains the default. The mode is saved per world (`biome_mode` in `<world>/worldbound.json`); `wild_seed` in the config reshuffles the patch layout for new worlds.

## 0.4.21 — no more endless snow

- **Seasons are off by default** (`seasons` in `config/worldbound.json` turns the real-date swing back on). Weather now follows each place's typical climate all year.
- **Snow only where it belongs:** freezing weather in truly cold climates (polar, tundra, extreme Siberia), and as a **snow cap on the top stretch of high mountains**, which now also gets snowy weather. Mountain areas below the cap are alpine meadow and bare rock with rain, not snowfields. The Tibetan plateau and the Altiplano are bare at 4,000–5,000 m; the Rockies' caps start around 3,800 m, the Alps' around 3,400 m; Fuji keeps its classic cap on the top ~400 m.
- `/earth probe` now reports elevation, climate class, land cover, slope and the local snowline.

## 0.4.20 — map travel

- **Press K for the travel map.** Green rings show everywhere you can go: within 100 km of any place you've discovered. Click a spot inside a ring and "Travel here". Free, no cooldown.
- **What counts as discovered:** your spawn, every landmark obelisk you've found, every city of 100,000+ you've walked into (6,204 worldwide — title and chime when you enter one), and every place you've travelled to. Each trip pushes your reach another 100 km.
- **You arrive on finished ground.** A "Travelling to Kyoto… 70%" screen shows while the land around the arrival point is built; obelisk-to-obelisk travel works the same way.
- `/earthmap` opens the same travel map; the landmark list is a button on the map.

## 0.4.19 — landmark obelisks

- **Obsidian obelisks at 106 real landmarks** (Everest, Fuji, the Grand Canyon, the Pyramids…), built on the flattest dry ground nearby the first time you get close. 13 blocks tall with glowing crying-obsidian seams, a gilded band, soul-lantern posts and an end-rod spire.
- **Discover them by walking up to one** (title + sound, "12 / 106").
- **Fast travel:** right-click an obelisk to open the landmark menu, then click any landmark you've discovered to travel there.
- **Guide trail:** press **K** anywhere (or use the menu) and click any landmark, discovered or not. A trail of breeze puffs runs along the ground for 30 blocks toward it, with an arrow and a blocks-to-go count at the top of the screen.
- **World option:** "Landmark obelisks" on the Earth spawn screen when creating a world (on by default). Existing Earth worlds get them too.

## 0.4.18 — mountains without holes

- **No see-through holes on mountains.** Minecraft doesn't draw terrain far below the camera, and Distant Horizons used to leave that area to Minecraft. Standing on a high peak (like Fuji's rim), the slopes dropping away below you turned into a window onto the sky. Distant Horizons now draws close in whenever the ground anywhere around you falls well below the camera, not only straight down.
- **Distant terrain shows today's seasonal snow,** the same snow new chunks get, so a mountain no longer turns from green to white as the real chunks load in.

## 0.4.17 — faster chunks

- **New chunks generate about 40% faster** (160 → ~90 ms each in a Tokyo test with full data packs). Map-tile lookups were spending most of their time in a badly spread lookup table and waiting on one shared lock; each worker thread now keeps its own small tile cache.
- **Biomes are worked out once per column** instead of about a thousand times up each 4,064-block column.
- **Saving chunks skips the empty layers:** the hundreds of all-stone and all-air layers in each chunk are written directly instead of being unpacked first.
- **Distant Horizons terrain builds 2–8× faster:** far-away pieces read map data at a resolution that matches their distance and skip the block-level coast, river and lake-shore checks.
- Default map tile memory is now 512 MB (`tile_cache_mb`).

## 0.4.16 — no more see-through holes with Distant Horizons

- **Flying high no longer shows a hole into the ground.** Distant Horizons leaves out everything inside your normal render distance, expecting Minecraft to draw it. Earth worlds are so tall that you can be farther above the ground than that distance; Minecraft then draws nothing and you saw straight into the insides of the distant terrain (caves, lava). Now, when you're high above the ground or the ground below hasn't loaded, Distant Horizons draws right up to you.
- **Caves are left out of distant terrain,** so any cut-out shows solid ground rather than cave interiors.
- Both only apply in Earth worlds and don't change your saved Distant Horizons settings. Turn off with `dh_display_tweaks` in `config/worldbound.json`.

## 0.4.15 — faster loading, instant distant terrain

- **Distant Horizons terrain comes straight from Earth data.** With Distant Horizons installed, far-away terrain is built directly from WorldBound's height, ground and forest data instead of generating full 4,064-block-tall chunks: about a thousand times less work, so the distance fills in quickly and the CPU is free for the chunks around you. Forests show as tree canopy in the distance; the real trees, villages and structures appear as you get close. (Config `dh_lod_generator`, on by default.)
- **New areas no longer stall on downloads.** Only chunks right around you wait for streamed data, and only for up to 4 seconds (`streaming.near_wait_seconds`, `streaming.wait_radius_blocks`). Everything else generates immediately from the installed data.
- **Downloads near you go first,** and when you're moving fast (TATV, elytra) the area you're heading into is fetched ahead of time.
- `/earth lod` shows the Distant Horizons generator's stats; streaming progress is logged once a minute.

## 0.4.14 — 2026-10-25

**Fine-detail streaming.** The game now downloads sharp terrain and land cover for wherever you explore:
- **Terrain at ~30 m** (Copernicus GLO-30) everywhere on land, not just the five mountain ranges, and **land cover at ~10 m** (ESA WorldCover 2021): fields and clearings inside forests, woods inside farmland, sharper snowfields.
- Tiles download in the background **ahead of you** (a bit past your view distance) and are saved in `worldbound\cache\` for next time. When generation reaches an area before its tile arrives it waits briefly (up to 20 s), then falls back to your installed packs.
- **Offline is fine:** if the data servers can't be reached, WorldBound uses your packs and retries every 10 minutes.
- The cache is capped at 4 GB (oldest tiles are removed first). Settings live under `"streaming"` in `config/worldbound.json`: `enabled`, `elevation`, `land_cover`, `wait_seconds`, `threads`, `prefetch_blocks`, `max_cache_mb`.
- `/earth stream` shows what's been downloaded.
- **Best in a new world:** in existing worlds, chunks you've already explored keep the older, softer terrain, so you may see small steps where old and new chunks meet.

## 0.4.13 — 2026-10-25

**Real forest density, plus ten quality-of-life improvements.**
- **Forests as dense as the real ones** (Copernicus tree-cover data, new `treecover` pack): thick forests like the Black Forest and the Ozarks get about 25–50% more trees, open woodland is thinner, and farmland loses its stray trees. Toggle: `"tree_density"`.
- **`/earth goto <place>`:** teleport by name to any city, country, US state, ZIP code or landmark, with tab-completion (`/earth goto Paris`, `/earth goto Mount Fuji`).
- **`/earth nearby`:** the nearest town and the three nearest landmarks, with distance and direction.
- **`/earth sun`:** sets the time of day to where the real sun is right now at your longitude.
- **"Open real map" link** in `/earth where`: click it to see the spot on OpenStreetMap.
- **The poles are the edge of the world:** walking past 89.5° turns you back with a message, instead of endless smeared ice.
- **Grand Canyon in colour:** red and orange terracotta bands on the canyon walls instead of pale sand.
- **Accented place names display correctly** ("Chārīkot", not "Chari¯kot"), and search ignores accents ("Sao Paulo" finds São Paulo).
- **Compass remembers on/off** between sessions and shows the world's scale.
- **Picker block coordinates** match the scale you've chosen for the new world.
- **README and Modrinth text** updated for per-world scale and the new features.

## 0.4.12 — 2026-10-25

**Choose your scale, and structures that fit the land.**
- **Per-world scale, chosen when you create the world** (Earth spawn → Scale button in the map picker). The picker lists the trade-offs for each:
  - **1:3, true proportions (new default).** Minecraft's height limit already squeezes the vertical to about 1:3, so at 1:3 horizontal, slopes, valleys and peaks have their real steepness. Huge world: about 9.8M blocks around.
  - **1:5, faster travel.** Mountains about 1.7× steeper than real.
  - **1:10, epic.** About 3.3× steeper; the old default.
  - **1:20, quick trips.** About 6.7× steeper and spiky; small rivers get thin.
  - Each world remembers its own scale, so your existing 1:10 worlds keep working unchanged.
- **No more floating structures.** Trial chambers, mineshafts, strongholds and ancient cities are always buried under the real ground, at depths you can actually dig to (mineshafts were ending up 1,000+ blocks down).
- **Villages and other surface structures need flat ground.** No more villages staggered up a mountainside: they spawn in valleys, plains and plateaus.
- **Foundations are back:** villages, trail ruins and similar get their ground levelled and filled like in vanilla.
- **Ancient cities spawn again**, in deep-dark regions about 100 blocks under the ground.
- Lava pockets under the sea floor are kept on purpose.

## 0.4.11 — 2026-10-25

- High peaks above the snowline are snow (and bare rock on steep faces), not pale-blue packed ice. Blue glacier ice stays only on mapped flat glaciers and ice caps.

## 0.4.10 — 2026-10-25

**Better high mountains** (from the Everest test):
- **No more lava and water sheets down mountainsides.** Springs now only appear deep underground, feeding caves, instead of pouring out of giant cliff faces.
- **Rock and snow, not blue ice.** Steep faces are bare stone with snow on top, slopes are snowfields, and blue-white glacier ice is only on flat glaciers and ice caps.
- **Clouds at a real height:** about 2,100 m (y 0) instead of 3,300 m, so high plateaus and peaks rise above them.
- Areas you've already generated (and Distant Horizons' saved far terrain) keep the old look; check in a new world or a fresh area.

## 0.4.9 — 2026-10-25

**Sail around the world.** Crossing 180° longitude in the middle of the Pacific now puts you on the other side of the world without a loading screen. Keep sailing east from Pocatello and you come back to Pocatello.
- Works on foot, flying, or in a boat (passengers and speed are kept).
- The world now repeats exactly every 360°, so the land you see across the line is the land you arrive in. This needed a 0.0003% scale tweak; near your existing builds nothing moves, but chunks first generated very far east or west in an old test world may not line up with new ones.
- Turn it off with `"wrap_around": false` in `config/worldbound.json`.

## 0.4.8 — 2026-10-25

**Coastlines, compass and where-am-I.**
- **Shaped coastlines:** where real coasts climb steeply (Dover, the Cliffs of Moher, Big Sur), the land stays high right to the water and drops as a stone cliff. Low, flat coasts get beaches, now up to ~700 m wide.
- **Earth compass (press J):** a small corner readout with your real latitude/longitude, heading, altitude in metres and the nearest town ("near Pocatello, ID, US (5 km SE)"). Hidden when F3 is open. Rebind under Controls → WorldBound.
- **Nearest town** also shows in `/earth where`.
- **Where you died:** dying in an Earth world tells you the real place ("You died at 27.988°N 86.925°E, near …").
- **Screenshot locations:** every screenshot in an Earth world adds a line to `screenshots\worldbound-locations.txt` (time, file name, lat/lon, altitude, nearest town).
- **Sharper mountains (data):** new `tools/build_glo90.py` turns the Copernicus 90 m download into a `glo90` pack (~76 m) for the Alps, Rockies, Himalaya, Andes and Caucasus. It's being built on your PC and is picked up automatically, no new jar needed.

## 0.4.7 — 2026-10-25

**Faster worlds, caves, ores and dry depressions.**
- **About 3–5× faster chunk generation.** Terrain is now written a section at a time straight from the map data instead of block by block, and ground-block rules only run near the surface.
- **Caves:** big cheese caverns and winding tunnels from 6 to about 260 blocks under the ground, everywhere. Never under oceans, lakes or rivers, so no flooding.
- **Ores at the right depth:** ores, geodes and dungeons sit at vanilla depths measured from the ground under you, so diamonds are about 120 blocks down wherever you dig, even on a mountain. Amounts match vanilla.
- **Land below sea level stays dry:** Death Valley, the Qattara Depression, the Turpan basin and the Dead Sea shore are dry land at their real depth. The Dead Sea, Caspian Sea and Salton Sea sit at their own real water levels. Low coasts and polders become beach at sea level.
- **Fix:** a crash in badlands that sit far below vanilla heights (e.g. the Turpan basin).
- Two new config switches in `config/worldbound.json`: `fast_fill` and `caves` (both on). Turning `fast_fill` off returns to the old, slower fill without caves.

## 0.4.6 — 2026-10-25

**Real forests, fields and ice.** Biomes now follow real land cover (Copernicus, 2019), not just climate:
- Forests appear where real forests are, with the right kind of tree: spruce in the Black Forest, taiga in Siberia, jungle in the Amazon, oak and dark oak in the Ozarks, birch in cold broadleaf forests.
- Farmland and grassland become plains, shrubland becomes savanna or scrub, bare rock and sand become the local barren biome, wetlands become swamps (mangroves in the tropics).
- **Glaciers and ice caps:** Greenland, Antarctica and mountain glaciers are packed ice and snow instead of snowy grass.
- Needs the `veg` pack in `worldbound\packs\` (already on your PC); without it, the old climate-only biomes are used.

## 0.4.5 — 2026-10-25

**Terralith ground blocks.** With Terralith installed, its biomes in Earth worlds now have their proper ground: calcite and hot-spring colours at Yellowstone, terracotta bands at Bryce Canyon, sand and sandstone in the Grand Canyon, and so on. Heights in Terralith's rules are adjusted to Earth's sea level.
- New test command: `/earth probe <lat> <lon>` generates that spot and lists its top blocks.

## 0.4.4 — 2026-10-25

**Seasons on servers.** On a dedicated server, the server now tells each player's game it's an Earth world, so rain and snow follow the real seasons on everyone's screen too. `/earthmap` also uses the server's map layout.

## 0.4.3 — 2026-10-25

**Real seasons.** Rain, snow and freezing now follow the real climate of where you are and today's real date, instead of getting colder with every block of height.
- Northern hemisphere is in its season, southern in the opposite one; the tropics barely change.
- Siberia snows in late October, Pocatello rains, Sydney is heading into summer.
- `/earth where` shows today's typical temperature and whether it's raining or snowing there.
- Turn it off with `"seasons": false` in `config/worldbound.json`.

**Fix:** worlds no longer fail to load when Terralith or Terrestria isn't installed (0.4.2 bug).

## 0.4.2 — "Places" (2026-10-25)

- **Real-world biome flavour, using your installed biome mods.** Named places and regions get the biome
  that fits them, from Terralith / Terrestria when installed, vanilla otherwise (nothing breaks without them):
  - Yellowstone, Yosemite Valley, Bryce, the Grand Canyon, Monument Valley, the White Cliffs of Dover, Provence lavender.
  - Coastal redwoods (N. California/S. Oregon), hemlock rainforest along the Pacific Northwest and BC coast.
  - Sakura groves, cherry groves and Japanese maple forests in Japan/Korea; bamboo in southern China.
  - Siberian taiga and larch groves; Central Asian and Patagonian steppe; sagebrush shrubland across the
    US interior West (the Snake River Plain included); hot shrubland in the Sahel/Mexico; the Outback.
  - Cypress swamps and forests in the US Southeast, tropical jungle across the Amazon, Canadian Shield,
    flower forests and the occasional pale garden in Europe and the eastern US, old-growth birch in Scandinavia,
    alpine highlands below the snowline in mountain ranges.
  Variety comes in ~20 km swaths, not speckle.
- Mid-latitude mountain snowlines raised (the Rockies were turning to snowy slopes at 2,000 m).
- `/earth where` now reports the actual biome, including modded ones.
- Limit: mod biomes bring their trees, plants and mobs, not their custom ground/rock colours (those live in
  Terralith's own terrain rules, which don't run in the Earth world type).

## 0.4.1 — "Lakes" (2026-10-24)

- **1.4 million lakes** from HydroLAKES (every lake of 10 ha or more), at their real surface elevation and
  with depth from each lake's measured average: the Great Salt Lake sits at y −273 and is 2 blocks deep
  (it really is ~4 m), Tahoe and Crater Lake go down hundreds of blocks, Baikal deeper still. Shores get
  shallower toward the edge and are ragged rather than pixel-stepped. Islands are kept (Antelope Island).
- Lakes show on the picker map once you're zoomed in to region level.
- Needs `worldbound\packs\lakes` and `lakes_coarse` in the instance (built from HydroLAKES).
- Known: lakes below sea level (Dead Sea, Caspian) still follow the ocean; Death Valley is still flooded.

## 0.4.0 — "Rivers" (2026-10-24)

- **Every major river on Earth.** 1.17 million river reaches from HydroRIVERS — anything carrying 5 m³/s
  or more — are cut into the terrain and filled with water. Widths and depths come from each river's
  real discharge: a creek is 2 blocks wide and 1 deep, the Rhine at Duisburg ~35 blocks and 3 deep,
  the Mississippi ~58, the Amazon 250+ blocks and 10 deep. Rivers sit on the valley floor, so they run
  through canyons instead of over their rims. River columns use the river biome.
- Rivers show on the picker map too, filtered by zoom (great rivers on the world view, every stream up close).
- Terrain is now evaluated per block instead of interpolated per 4×8 cell — narrow channels come out the
  right shape — with a per-chunk column cache that made spawn generation faster, not slower.
- Needs `worldbound\rivers.bin` (103 MB) in the instance; without it, worlds generate as before.
- Not yet: lakes (Great Salt Lake, Great Lakes, reservoirs) — next, from HydroLAKES.

## 0.3.3 (2026-10-24)

- **Fix: the picker froze when zoomed out.** It was reading the 300 m pack for every screen pixel —
  thousands of tiles per frame — so the picture never updated: markers drifted over a stale map, dragging
  moved the points but not the map, and places looked like open sea. The renderer now samples the data
  layer that matches the zoom (base when zoomed out, 300 m pack when zoomed in; up to two levels finer).
- **Land is never blue.** The Köppen legend paints tropical land pure blue (that was "Ternate"). The map
  now uses an atlas palette: greens for wet climates, sand for deserts, grey-greens for cold, white for ice.
- **Coastlines** drawn at every zoom; **place labels** — countries zoomed out, states and big cities in the
  middle, every city and landmark name up close — with overlap culling.
- Zoom-out now stops when the whole planet fits once (no repeated worlds).
- Closing the picker stops its render thread.

## 0.3.2 (2026-10-24)

- Picker's flat map is now **Mercator** (the wall-map look; cuts off at ±85°). Globe unchanged.
- **Fix:** on wide screens the map picture was stretched relative to the markers and clicks, so the
  selection cross and the Pocatello square sat on the wrong places. Picks now match the picture.

## 0.3.1 (2026-10-24)

- **Landmarks** button in the picker: 106 notable places (mountains, canyons, deserts, falls, coasts,
  polar, cities). Scroll the list, click to fly there; shown as cyan dots on the map (hover for name,
  click to snap). Landmarks rank first in search.

## 0.3.0 — "Pick a Place" (2026-10-24)

- **Earth location picker.** On the Create World screen, when the Earth world type is selected, an
  **Earth spawn: lat, lon** button opens the picker; the place you choose becomes the spawn of the
  world you are about to create. Other world types never see the button.
  - Flat map **and globe**, drag to pan/rotate, wheel to zoom without limit (world → a few metres per pixel).
    The map is drawn from the loaded data, so with the global pack installed zooming in shows real relief
    (hillshaded), and a regional pack shows 30 m detail wherever it covers.
  - **Search** one box for countries (244), US states (51), cities over 15k people (34,006) and
    US zip codes (41,749) — "Idaho", "Pocatello", "83201", "Tokyo". Enter picks the top hit.
  - **Coordinates:** type lat/lon and go. **Random city.** **Here (Pocatello)**.
  - Readout for the selected point: elevation, climate class, biome, block coordinates.
- `/earthmap` in a running Earth world opens the same picker with **Teleport here**.
- `/earth spawn here` and `/earth spawn <lat> <lon>` move the world spawn.
- The world's spawn and generation settings are recorded once in `<world>/worldbound.json`, so
  restarting a world never resets a spawn you changed.
- Mod Menu icon.

## 0.2.0 — "There Is an Earth" (2026-10-24)

First playable release: the **Earth** world type at 1:10 in a 4,064-tall world (sea level y −700), real
coastlines/mountains/oceans from GEBCO 2026, biomes from Köppen-Geiger climate, spawn on the Pocatello
is picked up automatically. Commands: `/earth where`, `/earth tp <lat> <lon>`, `/earth status`.
Known gaps: no rivers/lakes above sea level yet, vanilla snow lines, no carved caves, world repeats
instead of wrapping, tall-chunk generation is slower than vanilla.

## 0.1.0 (2026-10-24)

Engine only, no data — every world is ocean. Proved the preset loads in the real pack.
