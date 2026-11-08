package com.seth.worldbound;

import com.seth.worldbound.data.EtileCodec;
import com.seth.worldbound.data.Tile;
import com.seth.worldbound.data.Tiling;
import com.seth.worldbound.geo.GeoTransform;
import com.seth.worldbound.geo.HeightCurve;
import com.seth.worldbound.worldgen.BiomeTable;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dependency-free tests (no JUnit on the compile path). Run with ./test.sh, which compiles this
 * against the mod classes and executes main(). Any failure throws.
 */
public final class CoreTests {
    static int passed = 0;

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("FAILED: " + what);
        passed++;
    }

    static void near(double a, double b, double tol, String what) {
        check(Math.abs(a - b) <= tol, what + " (" + a + " vs " + b + ")");
    }

    public static void main(String[] args) throws Exception {
        geo();
        curve();
        etile(args.length > 0 ? Path.of(args[0]) : null);
        biomes();
        picker();
        fauna();
        dinos();
        nicknames();
        planet();
        System.out.println("CoreTests: " + passed + " checks passed");
    }

    /** Where WorldBound's animals live: the range boxes, then the full rule on the real (jar base) map. */
    static void fauna() {
        double[][] oz = {{-23.70, 133.88}, {-31.95, 115.86}, {-42.88, 147.33}, {-27.47, 153.03}, {-12.46, 130.84}};
        for (double[] c : oz) check(com.seth.fossilrecord.Habitat.australia(c[0], c[1]), "Australia at " + c[0] + "," + c[1]);
        double[][] notOz = {{-9.44, 147.18}, {-36.85, 174.76}, {-6.2, 106.8}, {-8.65, 115.2}, {51.5, -0.1}};
        for (double[] c : notOz) check(!com.seth.fossilrecord.Habitat.australia(c[0], c[1]), "not Australia at " + c[0] + "," + c[1]);
        double[][] amz = {{-3.10, -60.02}, {-3.75, -73.25}, {-1.45, -48.50}, {5.5, -59.0}, {-10.5, -67.8}};
        for (double[] c : amz) check(com.seth.fossilrecord.Habitat.amazonia(c[0], c[1]), "Amazonia at " + c[0] + "," + c[1]);
        double[][] notAmz = {{1.2, -77.9}, {-0.18, -78.47}, {9.0, -79.5}, {-23.5, -46.6}, {-3.1, 60.0}};
        for (double[] c : notAmz) check(!com.seth.fossilrecord.Habitat.amazonia(c[0], c[1]), "not Amazonia at " + c[0] + "," + c[1]);
        check(com.seth.fossilrecord.Habitat.kangarooVariant(4) == com.seth.fossilrecord.Habitat.RED, "red kangaroos in the desert (BWh)");
        check(com.seth.fossilrecord.Habitat.kangarooVariant(14) == com.seth.fossilrecord.Habitat.GREY, "grey kangaroos in Cfa country");
        check(!com.seth.fossilrecord.Habitat.kangarooGround(1, 10, 100), "no kangaroos in rainforest (Af)");
        check(!com.seth.fossilrecord.Habitat.anacondaGround(1, 10, 2600), "no anacondas at 2,600 m");
        // the whole rule on the real map (Earth was set up by picker(): jar base, default scale and origin)
        Earth e = Earth.get();
        Earth.setEarthWorldRunning(true);   // the animals' homes apply to a running world
        String[][] places = {{"-23.9", "133.5", "kangaroo"}, {"-30.5", "146.0", "kangaroo"}, {"-34.4", "148.5", "kangaroo"},
                {"-3.3", "-60.6", "anaconda"}, {"-4.2", "-69.9", "anaconda"}, {"-1.9", "-55.5", "anaconda"},
                {"51.5", "-0.1", "none"}, {"-3.1", "37.4", "none"}, {"35.7", "139.7", "none"}, {"-8.0", "-40.0", "none"}, {"-17.0", "-65.0", "anaconda"}};
        int kOk = 0, aOk = 0, kWant = 0, aWant = 0;
        for (String[] pl : places) {
            double lat = Double.parseDouble(pl[0]), lon = Double.parseDouble(pl[1]);
            int x = (int) Math.floor(e.transform.blockX(lon)), z = (int) Math.floor(e.transform.blockZ(lat));
            boolean k = com.seth.fossilrecord.Fauna.suits(com.seth.fossilrecord.Fauna.Species.KANGAROO, x, z);
            boolean a = com.seth.fossilrecord.Fauna.suits(com.seth.fossilrecord.Fauna.Species.ANACONDA, x, z);
            Earth.Surface c = e.at(x, z);
            System.out.printf(java.util.Locale.ROOT, "  fauna %s,%s: climate %d cover %d elev %.0f m -> kangaroo %s, anaconda %s%n", pl[0], pl[1], c.climate(), c.cover(), c.elevM(), k, a);
            if (pl[2].equals("kangaroo")) { kWant++; if (k) kOk++; check(!a, "no anacondas in Australia"); }
            else if (pl[2].equals("anaconda")) { aWant++; if (a) aOk++; check(!k, "no kangaroos in the Amazon"); }
            else check(!k && !a, "neither animal at " + pl[0] + "," + pl[1]);
        }
        check(kOk == kWant, "kangaroos suit " + kOk + " of " + kWant + " Australian places");
        check(aOk == aWant, "anacondas suit " + aOk + " of " + aWant + " Amazon places");
    }

    /** The dinosaurs: sane numbers for every species, its resources present, and its fossil sites where they should be. */
    static void dinos() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (var s : com.seth.fossilrecord.dino.Roster.ALL) {
            var r = s.rig();
            check(ids.add(s.id), "one " + s.id);
            check(s.mass > 0 && s.body.L > 0.3 && s.body.hip > 0.1 && s.body.hip < s.body.L, s.id + " has a real size");
            check(r.width() >= 0.3 && r.width() <= 4.0 && r.height() > 0.25 && r.height() <= 8, s.id + " hitbox " + r.width() + " x " + r.height());
            float least = Math.min(0.2f, 0.25f * s.body.L);                      // tiny feathered ones are a third of a metre long
            check(r.eye() > least / 2 && r.frontReach() > least && r.backReach() > least, s.id + " eye and reach");
            check(r.thighL > 0 && r.shinL > 0 && r.metaL > 0 && r.upperL > 0, s.id + " legs reach the ground");
            check(s.health() >= 4 && s.health() <= 500 && s.damage() >= 1 && s.damage() <= 28, s.id + " health " + s.health() + " damage " + s.damage());
            check(s.run >= s.walk && s.run <= 12.5 && s.speedAttribute() > 0.05 && s.speedAttribute() < 0.5, s.id + " speeds");
            check(!s.sites.isEmpty() && s.look != null && s.voice != null && !s.age.isEmpty(), s.id + " has sites, colours, a voice and an age");
            for (var site : s.sites) {
                check(site.lat0() < site.lat1() && site.lon0() < site.lon1(), s.id + " site box " + site.name());
                check(s.ranges(site.lat(), site.lon()), s.id + " lives at the middle of " + site.name());
            }
            java.io.File res = new java.io.File("fossil-core/src/main/resources");
            check(new java.io.File(res, "assets/fossilrecord/textures/entity/dino/" + s.id + ".png").isFile(), s.id + " texture painted");
            check(new java.io.File(res, "assets/fossilrecord/textures/item/" + s.id + "_spawn_egg.png").isFile(), s.id + " egg icon");
            check(new java.io.File(res, "assets/fossilrecord/items/" + s.id + "_spawn_egg.json").isFile(), s.id + " egg item model");
            check(new java.io.File(res, "data/fossilrecord/loot_table/entities/" + s.id + ".json").isFile(), s.id + " loot table");
        }
        // famous localities, each inside its animal's sites; and places with none
        Object[][] at = {{"tyrannosaurus", 45.0, -102.0, "Sue, South Dakota"}, {"tyrannosaurus", 49.4, -107.8, "Scotty, Saskatchewan"},
                {"triceratops", 47.6, -106.9, "Hell Creek, Montana"}, {"velociraptor", 44.14, 103.73, "Flaming Cliffs"},
                {"protoceratops", 43.75, 103.4, "Tugrikin Shireh"}, {"gallimimus", 43.5, 101.0, "Nemegt"},
                {"spinosaurus", 28.35, 28.85, "Bahariya Oasis"}, {"spinosaurus", 30.9, -4.0, "Kem Kem, Erfoud"},
                {"argentinosaurus", -38.93, -69.21, "Plaza Huincul"}, {"giganotosaurus", -39.26, -68.79, "Villa El Chocón"},
                {"iguanodon", 50.47, 3.65, "Bernissart"}, {"compsognathus", 48.9, 11.2, "Solnhofen"}, {"compsognathus", 43.65, 6.45, "Canjuers"},
                {"stegosaurus", 38.55, -105.2, "Garden Park, Colorado"}, {"diplodocus", 40.44, -109.3, "Dinosaur National Monument"},
                {"brachiosaurus", 39.07, -108.55, "Grand Junction, Colorado"}, {"allosaurus", 39.3, -110.7, "Cleveland-Lloyd Quarry"},
                {"dilophosaurus", 36.1, -111.25, "near Tuba City, Arizona"}, {"parasaurolophus", 50.75, -111.5, "Dinosaur Provincial Park"},
                {"edmontosaurus", 51.47, -112.7, "Drumheller"},
                {"megalosaurus", 51.85, -1.43, "Stonesfield"}, {"herrerasaurus", -30.0, -67.8, "Valle de la Luna"},
                {"leaellynasaura", -38.8, 143.45, "Dinosaur Cove"}, {"minmi", -26.6, 148.8, "Roma, Queensland"},
                {"olorotitan", 49.08, 130.85, "Kundur"}, {"europasaurus", 51.9, 10.48, "Langenberg quarry"},
                {"scelidosaurus", 50.73, -2.9, "Charmouth"}, {"concavenator", 40.1, -2.05, "Las Hoyas"},
                {"saltasaurus", -26.1, -65.4, "El Brete"}, {"nigersaurus", 16.6, 9.1, "Gadoufaoua"},
                {"zuul", 48.5, -109.5, "near Havre, Montana"}, {"alamosaurus", 29.3, -103.4, "Big Bend"},
                {"stenonychosaurus", 50.75, -111.5, "Dinosaur Provincial Park"}, {"heterodontosaurus", -30.7, 27.4, "Tyinindini, Eastern Cape"},
                {"kamuysaurus", 42.78, 142.13, "Hobetsu, Mukawa"}, {"fukuiraptor", 36.1, 136.55, "Kitadani quarry"},
                {"rajasaurus", 22.95, 73.3, "Rahioli"}, {"irritator", -7.19, -39.74, "Santana do Cariri"},
                {"staurikosaurus", -29.68, -53.8, "Santa Maria"}, {"balaur", 45.95, 23.57, "Sebes"},
                {"hungarosaurus", 47.22, 17.65, "Iharkut"}, {"ampelosaurus", 42.92, 2.2, "Campagne-sur-Aude"},
                {"tethyshadros", 45.78, 13.6, "Villaggio del Pescatore"}, {"hadrosaurus", 39.9, -75.03, "Haddonfield"},
                {"anchisaurus", 41.78, -72.52, "Manchester, Connecticut"}, {"timurlengia", 42.1, 62.6, "Dzharakuduk"},
                {"anchiornis", 40.8, 119.8, "Jianchang"}, {"tsintaosaurus", 36.97, 120.7, "Laiyang"},
                {"qianzhousaurus", 25.85, 114.93, "Ganzhou"}, {"regaliceratops", 49.6, -113.9, "Oldman River"},
                {"turiasaurus", 40.12, -1.15, "Riodeva"}, {"kulindadromeus", 52.5, 116.6, "Kulinda"},
                {"malawisaurus", -9.93, 33.93, "Karonga"}, {"phuwiangosaurus", 16.68, 102.35, "Phu Wiang"},
                {"anzu", 46.33, -103.98, "Marmarth, North Dakota"}, {"limusaurus", 45.15, 88.90, "Wucaiwan"},
                {"sinosaurus", 25.18, 102.08, "Lufeng"}, {"megaraptor", -38.9, -69.55, "Sierra del Portezuelo"},
                {"aucasaurus", -37.88, -68.52, "Auca Mahuevo"}, {"buitreraptor", -39.63, -68.70, "La Buitrera"},
                {"austroraptor", -40.06, -66.80, "Bajo de Santa Rosa"}, {"nothronychus", 37.08, -111.66, "Big Water, Utah"},
                {"nothronychus", 34.6, -108.8, "Zuni Basin"}, {"lythronax", 37.19, -111.65, "Nipple Butte"},
                {"brachytrachelopan", -43.2, -69.1, "Cerro Condor"}, {"hylaeosaurus", 51.10, -0.16, "Tilgate Forest"},
                {"saichania", 43.51, 101.13, "Khulsan"}, {"futalognkosaurus", -38.45, -68.72, "Lago Barreales"},
                {"scutellosaurus", 35.68, -111.03, "Ward Terrace"}, {"dicraeosaurus", -9.68, 39.23, "Kindope"},
                {"halszkaraptor", 43.66, 100.90, "Ukhaa Tolgod"}, {"tyrannotitan", -43.78, -68.83, "La Juanita"},
                {"achelousaurus", 48.97, -112.64, "Landslide Butte"}, {"diabloceratops", 37.18, -111.35, "Last Chance Creek"},
                {"polacanthus", 50.62, -1.38, "Barnes High"}, {"leptoceratops", 51.82, -112.98, "Red Deer River"},
                {"dilong", 41.6, 120.9, "Lujiatun"}, {"zhuchengtyrannus", 36.0, 119.4, "Zhucheng"},
                {"eustreptospondylus", 51.8, -1.3, "Wolvercote"}, {"elaphrosaurus", -9.7, 39.2, "Tendaguru"},
                {"acheroraptor", 47.1, -107.4, "near Jordan, Montana"}, {"bambiraptor", 48.6, -113.8, "near Glacier National Park"},
                {"atrociraptor", 51.5, -112.8, "Drumheller"}, {"tsaagan", 43.7, 100.9, "Ukhaa Tolgod"},
                {"avimimus", 44.1, 102.9, "Udyn Sayr"}, {"avimimus", 43.9, 100.0, "Bugiin Tsav"},
                {"tawa", 36.3, -106.5, "Hayden Quarry, Ghost Ranch"}, {"liliensternus", 50.4, 10.6, "Grosser Gleichberg"},
                {"ekrixinatosaurus", -38.0, -69.1, "Bajo del Anelo"}, {"sauroposeidon", 34.4, -96.1, "Atoka County, Oklahoma"},
                {"sauroposeidon", 32.3, -97.8, "Glen Rose, Texas"}, {"cetiosaurus", 51.9, -1.3, "Bletchingdon"},
                {"cetiosaurus", 52.6, -0.7, "Great Casterton"}, {"opisthocoelicaudia", 43.6, 100.5, "Altan Uul"},
                {"barapasaurus", 18.9, 80.0, "Pochampalli"}, {"atlasaurus", 32.0, -6.3, "Wawmda"},
                {"riojasaurus", -29.8, -67.9, "Los Colorados"}, {"thecodontosaurus", 51.6, -2.5, "Tytherington"},
                {"neuquensaurus", -38.8, -68.1, "Cinco Saltos"}, {"amurosaurus", 50.3, 127.5, "Blagoveshchensk"},
                {"charonosaurus", 48.9, 130.4, "Jiayin"}, {"kritosaurus", 36.3, -108.2, "Ojo Alamo"},
                {"utahceratops", 37.6, -111.9, "Kaiparowits Plateau"}, {"bagaceratops", 43.5, 99.8, "Hermiin Tsav"},
                {"zuniceratops", 35.1, -108.8, "Haystack Butte"}, {"hesperosaurus", 44.3, -106.7, "Johnson County, Wyoming"},
                {"wuerhosaurus", 46.0, 85.8, "Wuerho"}, {"scolosaurus", 50.7, -111.4, "Deadlodge Canyon"},
                {"sphaerotholus", 36.3, -108.2, "Willow Wash"}, {"sphaerotholus", 45.8, -105.0, "Carter County, Montana"},
                {"gasparinisaura", -38.8, -68.0, "Cinco Saltos"}, {"orodromeus", 47.8, -112.2, "Egg Mountain"}};
        for (Object[] a : at) {
            var s = com.seth.fossilrecord.dino.Roster.get((String) a[0]);
            check(s != null && s.ranges((double) a[1], (double) a[2]), a[0] + " lives at " + a[3]);
        }
        double[][] none = {{-34.6, -58.4}, {35.7, 139.7}, {-33.9, 151.2}, {-3.3, -60.6}, {23.0, 10.0}, {64.1, -21.9}, {-1.3, 36.8}};
        for (double[] p : none) {
            int n = 0;
            for (var s : com.seth.fossilrecord.dino.Roster.ALL) if (s.ranges(p[0], p[1])) n++;
            check(n == 0, "no dinosaurs at " + p[0] + "," + p[1]);
        }
        // every fossil site has ground the animal will actually stand on (not all sea, ice, mountain or city)
        var earth = Earth.get();
        Earth.setEarthWorldRunning(true);   // the animals' homes apply to a running world
        for (var s : com.seth.fossilrecord.dino.Roster.ALL)
            for (var site : s.sites) {
                int ok = 0;
                for (int i = 0; i <= 8; i++) for (int j = 0; j <= 8; j++) {
                    double lat = site.lat0() + (site.lat1() - site.lat0()) * i / 8, lon = site.lon0() + (site.lon1() - site.lon0()) * j / 8;
                    if (com.seth.fossilrecord.dino.Dinos.suits(s, (int) Math.floor(earth.transform.blockX(lon)), (int) Math.floor(earth.transform.blockZ(lat)))) ok++;
                }
                check(ok > 0, s.id + " can stand somewhere in " + site.name() + " (" + ok + " of 81 sampled columns)");
            }
        // random placement (0.7.6): any of them anywhere they can stand, but still never at sea or on ice, and only
        // the cold-country kinds on the tundra
        var dinos = com.seth.fossilrecord.dino.Roster.ALL;
        int sx = (int) Math.floor(earth.transform.blockX(10.0)), sz = (int) Math.floor(earth.transform.blockZ(23.0));
        check(com.seth.fossilrecord.dino.Dinos.here(sx, sz).isEmpty(), "accurate placement: no dinosaurs in the central Sahara");
        com.seth.fossilrecord.dino.Dinos.forcePlacement(true);
        try {
            int all = com.seth.fossilrecord.dino.Dinos.here(sx, sz).size();
            check(all == dinos.size(), "random placement: all " + dinos.size() + " may turn up in the central Sahara (got " + all + ")");
            check(com.seth.fossilrecord.dino.Dinos.here((int) Math.floor(earth.transform.blockX(-40.0)), (int) Math.floor(earth.transform.blockZ(30.0))).isEmpty(),
                    "random placement: none on the open Atlantic");
            check(com.seth.fossilrecord.dino.Dinos.here((int) Math.floor(earth.transform.blockX(-40.0)), (int) Math.floor(earth.transform.blockZ(72.0))).isEmpty(),
                    "random placement: none on the Greenland ice sheet");
            var tundra = com.seth.fossilrecord.dino.Dinos.here((int) Math.floor(earth.transform.blockX(125.0)), (int) Math.floor(earth.transform.blockZ(72.0)));
            boolean cold = !tundra.isEmpty();
            for (var s : tundra) cold &= (s.climates & com.seth.fossilrecord.dino.DinoSpecies.COLD) != 0;
            check(cold, "random placement: only cold-country kinds on the Yakutian tundra (" + tundra.size() + " of them)");
            check(com.seth.fossilrecord.dino.Dinos.suits(rexFor(), sx, sz), "random placement: a T. rex suits the Sahara");
        } finally {
            com.seth.fossilrecord.dino.Dinos.forcePlacement(false);
        }
        check(!com.seth.fossilrecord.dino.Dinos.suits(rexFor(), sx, sz), "accurate placement: a T. rex doesn't suit the Sahara");
        // a T. rex is about four times as tall at the hip as a person, an Argentinosaurus over thirty blocks long
        var rex = com.seth.fossilrecord.dino.Roster.get("tyrannosaurus");
        check(rex.body.hip > 3 && rex.body.hip < 4 && rex.rig().frontReach() + rex.rig().backReach() > 11, "T. rex at full size");
        check(com.seth.fossilrecord.dino.Roster.get("argentinosaurus").body.L >= 30, "Argentinosaurus over 30 m");
    }

    static void nicknames() {
        for (var e : com.seth.fossilrecord.dino.Dinos.NICKNAMES.entrySet())
            check(com.seth.fossilrecord.dino.Roster.get(e.getValue()) != null, "nickname " + e.getKey() + " names a dinosaur we have (" + e.getValue() + ")");
        check(com.seth.fossilrecord.dino.Dinos.byName("T-Rex") == rexFor() && com.seth.fossilrecord.dino.Dinos.byName("trex") == rexFor()
                && com.seth.fossilrecord.dino.Dinos.byName("t.rex") == rexFor(), "trex, T-Rex and t.rex find Tyrannosaurus");
        check(com.seth.fossilrecord.dino.Dinos.byName("Troodon") == com.seth.fossilrecord.dino.Roster.get("stenonychosaurus"), "Troodon still finds Stenonychosaurus");
    }

    static com.seth.fossilrecord.dino.DinoSpecies rexFor() { return com.seth.fossilrecord.dino.Roster.get("tyrannosaurus"); }

    /** Earthish pieces that are cheap to check: Köppen rules, the meander-free river course, hypsometry. */
    static void planet() {
        // annual mean °C, yearly range, annual mm, summer-half share -> class (Beck et al. rules)
        Object[][] cases = {
                {27.0, 2.0, 2500.0, 0.55, 1, "Af"}, {26.0, 4.0, 1500.0, 0.85, 3, "Aw"}, {27.0, 8.0, 100.0, 0.6, 4, "BWh"},
                {12.0, 15.0, 650.0, 0.5, 15, "Cfb"}, {16.0, 14.0, 600.0, 0.15, 8, "Csa"}, {5.0, 30.0, 600.0, 0.62, 26, "Dfb"},
                {-15.0, 20.0, 200.0, 0.7, 30, "EF"}, {-8.0, 24.0, 300.0, 0.6, 29, "ET"}, {-9.0, 60.0, 240.0, 0.7, 28, "Dfd"},
                {10.0, 28.0, 300.0, 0.6, 7, "BSk"}};
        for (Object[] c : cases) {
            int k = com.seth.worldbound.planet.ClimateAccess.koppen((double) c[0], (double) c[1], (double) c[2], (double) c[3]);
            check(k == (int) c[4], "koppen " + c[5] + " got " + com.seth.worldbound.worldgen.BiomeTable.code(k));
        }
        // more summer rain never makes a place drier-classed than the same total spread evenly
        check(com.seth.worldbound.planet.ClimateAccess.koppen(24, 6, 1200, 0.5) <= 3, "even tropical rain is A");
    }

    static void picker() throws Exception {
        // Real data: the jar base tiles are on the test classpath via out/classes.
        Path tmp = Files.createTempDirectory("wb");
        Earth.init(new WorldBoundConfig(), tmp);
        com.seth.earthdata.EarthDataMod.registerData();   // the real Earth (Earth Data World Gen)
        com.seth.fossilrecord.FossilRecord.registerHomes(com.seth.fossilrecord.earth.EarthFossilRecord.HOMES);
        com.seth.worldbound.client.MapView v = new com.seth.worldbound.client.MapView();
        int w = 256, h = 128;
        v.degPerPx = 360.0 / w; v.centerLat = 0; v.centerLon = 0;
        near(com.seth.worldbound.client.MapView.latFromMercY(com.seth.worldbound.client.MapView.mercY(42.87)), 42.87, 1e-9, "mercator inverse");
        check(v.toLonLat(w / 2.0, -1e9, w, h) == null, "beyond mercator cut-off");
        // texture rendered at half size must line up with screen-space markers
        com.seth.worldbound.client.MapView half = v.scaled(2.0);
        double[] a = v.toPixel(-112.44, 42.87, w, h), b = half.toPixel(-112.44, 42.87, w / 2, h / 2);
        near(a[0] / 2, b[0], 1e-9, "scaled x"); near(a[1] / 2, b[1], 1e-9, "scaled y");
        for (double[] ll : new double[][]{{-112.44, 42.87}, {10, 50}, {170, -40}, {-179.9, 10}}) {
            double[] p = v.toPixel(ll[0], ll[1], w, h);
            double[] back = v.toLonLat(p[0], p[1], w, h);
            near(back[0], ll[0], 1e-9, "flat lon round trip"); near(back[1], ll[1], 1e-9, "flat lat round trip");
        }
        int[] px = v.render(w, h);
        int pocatello = px[(int) v.toPixel(-112.44, 42.87, w, h)[1] * w + (int) v.toPixel(-112.44, 42.87, w, h)[0]];
        int pacific = px[(int) v.toPixel(-150, -20, w, h)[1] * w + (int) v.toPixel(-150, -20, w, h)[0]];
        int sahara = px[(int) v.toPixel(10, 25, w, h)[1] * w + (int) v.toPixel(10, 25, w, h)[0]];
        check(((pacific >> 16) & 0xFF) < 60 && (pacific & 0xFF) > 100, "pacific is blue");
        check(((pocatello >> 16) & 0xFF) > 60, "pocatello is land");
        check(((sahara >> 16) & 0xFF) > ((sahara >> 8) & 0xFF), "sahara is reddish");
        // globe
        v.globe = true; v.radius = 60; v.centerLat = 42.87; v.centerLon = -112.44;
        double[] c = v.toLonLat(w / 2.0, h / 2.0, w, h);
        near(c[0], -112.44, 1e-6, "globe centre lon"); near(c[1], 42.87, 1e-6, "globe centre lat");
        for (double[] ll : new double[][]{{-100, 40}, {-120, 30}, {-112, 60}}) {
            double[] p = v.toPixel(ll[0], ll[1], w, h);
            check(p != null, "front side visible");
            double[] back = v.toLonLat(p[0], p[1], w, h);
            near(back[0], ll[0], 1e-6, "globe lon round trip"); near(back[1], ll[1], 1e-6, "globe lat round trip");
        }
        check(v.toPixel(70, 42, w, h) == null, "far side hidden");
        int[] g = v.render(w, h);
        check(g[0] == 0xFF0B0E1A, "space outside disc");
        check(g[(h / 2) * w + w / 2] != 0xFF0B0E1A, "globe centre drawn");
        // zoom keeps the point under the cursor
        v.globe = false; v.degPerPx = 0.1; v.centerLat = 42.87; v.centerLon = -112.44;
        double[] before = v.toLonLat(30, 20, w, h);
        v.zoom(2.0, 30, 20, w, h);
        double[] after = v.toLonLat(30, 20, w, h);
        near(after[0], before[0], 1e-9, "zoom anchor lon"); near(after[1], before[1], 1e-9, "zoom anchor lat");
        // gazetteer
        com.seth.worldbound.client.Gazetteer gz = com.seth.worldbound.client.Gazetteer.get();
        check(gz.size() > 70000, "gazetteer loaded " + gz.size());
        check(gz.search("Pocatello", 5).get(0).name().startsWith("Pocatello"), "city search");
        check(gz.search("Idaho", 5).get(0).type().equals("state"), "state before city: " + gz.search("Idaho", 5).get(0));
        check(gz.search("83201", 5).get(0).type().equals("zip"), "zip search");
        check(gz.search("United States", 5).get(0).type().equals("country"), "country search");
        check(gz.search("tokyo", 5).get(0).lat() > 35, "case-insensitive");
        check(gz.randomCity().type().equals("city"), "random city");
        check(gz.landmarks().size() >= 100, "landmarks " + gz.landmarks().size());
        check(gz.search("grand canyon", 3).get(0).type().equals("landmark"), "landmark search");
        check(gz.search("everest", 3).get(0).span() == 0.6, "landmark span");
    }

    static void geo() {
        GeoTransform t = new GeoTransform(42.87, -112.44, 10);
        near(t.lon(0), -112.44, 1e-9, "origin lon");
        near(t.lat(0), 42.87, 1e-9, "origin lat");
        near(t.blockX(-112.44), 0, 1e-9, "origin x");
        near(t.blockZ(42.87), 0, 1e-9, "origin z");
        // Round trips far from the origin.
        for (double lon : new double[]{-170, -100, 0, 86.93, 179.9}) {
            for (double lat : new double[]{-80, -10, 27.99, 60}) {
                near(t.lon(t.blockX(lon)), lon, 1e-6, "lon round trip " + lon);
                near(t.lat(t.blockZ(lat)), lat, 1e-6, "lat round trip " + lat);
            }
        }
        // One degree of latitude is 11057 blocks at 1:10; one degree of longitude at 42.87N is ~8160.
        near(t.blockZ(41.87), 11057.4, 0.1, "deg lat in blocks");
        near(t.blockX(-111.44), 111320 * Math.cos(Math.toRadians(42.87)) / 10, 0.1, "deg lon in blocks");
        near(t.worldWidthBlocks(), 2.937e6, 2e3, "world width");
        // East is +x, south is +z.
        check(t.blockX(-100) > 0 && t.blockZ(30) > 0, "axis directions");
        // Antimeridian wrap: 100 blocks past 180E comes out near -180.
        double xAt180 = t.blockX(179.999);
        check(t.lon(xAt180 + 100) < -179.9, "wrap past 180");
    }

    static void curve() {
        HeightCurve c = HeightCurve.defaults();
        near(c.y(0), -700, 1e-9, "sea level");
        near(c.y(3000), 300, 1e-9, "3000 m");
        near(c.y(8849), 2020, 0.5, "Everest");
        near(c.y(-200), -800, 1e-9, "shelf edge");
        near(c.y(-6000), -1459, 0.5, "6 km clamp"); near(c.y(-10935), -1492, 1.0, "Challenger Deep"); near(c.y(-11034), -1493, 1.0, "deepest");
        near(c.y(1360), -246.7, 0.5, "Pocatello");
        near(c.y(3859), 552.6, 0.5, "Borah Peak");
        check(c.y(20000) <= 2031 && c.y(-30000) >= -1504, "clamps");
        double prev = Double.NEGATIVE_INFINITY;
        for (int m = -11500; m <= 9500; m += 7) {
            double y = c.y(m);
            check(y >= prev, "monotonic at " + m);
            prev = y;
            if (m > -11034 && m < 8849) near(c.metres(y), m, 0.01, "inverse at " + m);
        }
        near(c.metresPerBlockAt(100), 3.0, 1e-9, "rate low land");
        near(c.metresPerBlockAt(5000), 3.4, 1e-9, "rate high land");
        near(c.metresPerBlockAt(-5000), 8.8, 1e-9, "rate deep ocean");
    }

    static void etile(Path pythonTile) throws Exception {
        short[] px = new short[1024 * 1024];
        for (int i = 0; i < px.length; i++) px[i] = (short) ((i * 7919) % 12000 - 6000);
        Tile t = new Tile(Tiling.LAYER_ELEV, 7, 3, 5, 1024, 1024, Tiling.NODATA_ELEV, px);
        byte[] enc = EtileCodec.encode(t);
        check(enc.length < px.length * 2, "compressed");
        Tile back = EtileCodec.decode(enc);
        check(back.z == 7 && back.tx == 3 && back.ty == 5 && back.w == 1024 && back.nodata == Tiling.NODATA_ELEV, "header round trip");
        for (int i = 0; i < px.length; i += 997) check(back.get(i % 1024, i / 1024) == px[i], "pixel " + i);
        // u8 layer
        short[] cl = new short[64 * 64];
        for (int i = 0; i < cl.length; i++) cl[i] = (short) (i % 31);
        Tile c = EtileCodec.decode(EtileCodec.encode(new Tile(Tiling.LAYER_CLIMATE, 3, 1, 1, 64, 64, 0, cl)));
        check(c.get(30, 0) == 30 && c.get(0, 1) == 64 % 31, "u8 round trip");
        // Cross-language: a tile written by tools/etile.py
        if (pythonTile != null) {
            Tile p = EtileCodec.decode(Files.readAllBytes(pythonTile));
            check(p.layer == Tiling.LAYER_ELEV && p.w == 1024 && p.h == 1024, "python tile header");
            check(p.get(0, 0) == 1234 && p.get(1023, 1023) == -4321 && p.get(512, 256) == 77, "python tile pixels");
        }
        near(Tiling.degPerPx(10), 0.000343, 1e-6, "deg/px z10");
        near(Tiling.globalPx(3, -180), 0, 1e-9, "global px west edge");
        near(Tiling.globalPy(3, 90), 0, 1e-9, "global py north edge");
    }

    static Earth.Surface s(double lat, double elev, int climate, int cover, double slope, boolean nearOcean) {
        return new Earth.Surface(-112, lat, elev, 0, climate, cover, slope, nearOcean, 0, 0, false, false);
    }

    static void biomes() {
        check(BiomeTable.allBiomes().size() > 40, "biome list");
        check(BiomeTable.surface(s(42, 1360, 7, 0, 0.1, false)).equals("plains"), "BSk default");
        check(BiomeTable.surface(s(42, 1360, 7, 10, 0.1, false)).equals("wooded_badlands"), "BSk tree");
        check(BiomeTable.surface(s(42, 3859, 7, 0, 1.5, false)).equals("jagged_peaks"), "Borah");
        check(BiomeTable.surface(s(42, 3100, 7, 0, 0.2, false)).equals("stony_peaks"), "high arid");
        check(BiomeTable.surface(s(0, -3000, 0, 0, 0, false)).equals("deep_lukewarm_ocean"), "deep warm ocean");
        check(BiomeTable.surface(s(50, -100, 0, 0, 0, false)).equals("ocean"), "temperate shelf");
        check(BiomeTable.surface(s(75, -2000, 0, 0, 0, false)).equals("deep_frozen_ocean"), "deep frozen");
        check(BiomeTable.surface(s(10, 100, 1, 0, 0, false)).equals("jungle"), "Af");
        check(BiomeTable.surface(s(42, 1200, 7, 80, 0, false)).equals("river"), "lake");
        check(BiomeTable.surface(s(5, 2, 2, 90, 0, false)).equals("mangrove_swamp"), "tropical wetland");
        check(BiomeTable.surface(s(42, 5, 7, 0, 0.1, true)).equals("beach"), "beach");
        check(BiomeTable.surface(s(70, 5, 29, 0, 0.1, true)).equals("snowy_beach"), "snowy beach");
        check(BiomeTable.surface(s(-80, 2000, 30, 0, 0.1, false)).equals("frozen_peaks"), "ice cap");
        check(BiomeTable.cave("plains", -1400, -200, -1504, 0).equals("deep_dark"), "deep dark");
        check(BiomeTable.cave("plains", -300, -200, -1504, -0.5).equals("deep_dark"), "deep dark under ground");
        check(BiomeTable.cave("plains", -100, -50, -1504, 0.9).equals("lush_caves"), "lush");
        check(BiomeTable.cave("plains", -100, -50, -1504, 0).equals("plains"), "cave fallthrough");
    }
}
