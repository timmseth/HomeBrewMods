package com.seth.fossilrecord.dino;

import com.seth.fossilrecord.dino.DinoSpecies.Look;
import com.seth.fossilrecord.dino.DinoSpecies.Site;
import com.seth.fossilrecord.dino.DinoSpecies.Voice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.seth.fossilrecord.dino.Body.Plan.*;
import static com.seth.fossilrecord.dino.DinoSpecies.Attack.*;
import static com.seth.fossilrecord.dino.DinoSpecies.Diet.*;
import static com.seth.fossilrecord.dino.DinoSpecies.Pattern.*;
import static com.seth.fossilrecord.dino.DinoSpecies.Temper.*;

/**
 * The dinosaurs. Sizes are published estimates for grown animals (length snout to tail tip, body mass, hip joint
 * height); speeds are biomechanical estimates (top speeds over ~10 m/s are held to 10-12 so the game can path them).
 * Each lives where its fossils were found: the formations below, as latitude/longitude boxes around their outcrops.
 * Colours are guesses in the spirit of modern reconstructions (no skin colour is known for these), feathered where
 * their family is known to have been.
 */
public final class Roster {
    private Roster() {}

    // ---------------------------------------------------------------- fossil sites (latitude, longitude boxes)

    static final Site HELL_CREEK = new Site("the Hell Creek, Lance, Scollard and Frenchman formations (Montana, the Dakotas, Wyoming, Alberta, Saskatchewan)", 43.0, 52.5, -114.0, -100.0);
    static final Site DINOSAUR_PARK = new Site("the Dinosaur Park and Oldman formations (southern Alberta)", 49.3, 52.3, -114.0, -109.5);
    static final Site HORSESHOE_CANYON = new Site("the Horseshoe Canyon Formation (Drumheller, Alberta)", 50.8, 52.8, -114.2, -111.4);
    static final Site MORRISON = new Site("the Morrison Formation (Colorado, Utah, Wyoming, Montana, New Mexico)", 35.5, 46.0, -112.5, -103.0);
    static final Site LOURINHA = new Site("the Lourinhã Formation (central Portugal)", 38.8, 39.8, -9.6, -8.7);
    static final Site DJADOCHTA = new Site("the Djadochta Formation (Flaming Cliffs, Tugrikin Shireh, Ukhaa Tolgod) and Bayan Mandahu (Gobi Desert)", 41.4, 45.2, 100.8, 107.5);
    static final Site NEMEGT = new Site("the Nemegt Formation (Nemegt Basin, Gobi Desert, Mongolia)", 42.8, 44.6, 99.0, 102.5);
    static final Site NEUQUEN = new Site("the Candeleros and Huincul formations (Neuquén, Argentina)", -40.2, -37.8, -70.6, -68.0);
    static final Site LA_COLONIA = new Site("the La Colonia Formation (Chubut, Argentina)", -43.6, -41.8, -69.5, -66.5);
    static final Site KEM_KEM = new Site("the Kem Kem beds (Morocco and Algeria)", 29.5, 32.2, -6.0, -2.0);
    static final Site BAHARIYA = new Site("the Bahariya Formation (Bahariya Oasis, Egypt)", 27.6, 28.8, 28.3, 29.4);
    static final Site KAYENTA = new Site("the Kayenta Formation (northern Arizona)", 35.6, 37.2, -112.2, -110.4);
    static final Site SOLNHOFEN = new Site("the Solnhofen limestone (Bavaria, Germany)", 48.7, 49.2, 10.8, 11.9);
    static final Site CANJUERS = new Site("the Canjuers plateau (Provence, France)", 43.5, 43.8, 6.2, 6.8);
    static final Site WEALDEN = new Site("the Wealden of southern England, Bernissart (Belgium) and northern France", 49.6, 51.8, -2.0, 6.0);
    static final Site IBERIA_BARREMIAN = new Site("the Barremian beds of Teruel and Morella (Spain)", 40.0, 41.0, -1.2, 0.2);
    static final Site NEHDEN = new Site("Nehden (Sauerland, Germany)", 51.2, 51.6, 8.3, 8.9);
    static final Site KAIPAROWITS = new Site("the Kaiparowits Formation (Grand Staircase, Utah)", 37.0, 37.9, -112.2, -111.0);
    static final Site SAN_JUAN = new Site("the Fruitland and Kirtland formations (San Juan Basin, New Mexico)", 35.8, 36.9, -108.7, -107.3);

    static final Site TWO_MEDICINE = new Site("the Two Medicine Formation (northern Montana)", 47.4, 49.0, -114.0, -111.6);
    static final Site JUDITH_RIVER = new Site("the Judith River Formation (Montana)", 47.2, 48.8, -110.6, -107.4);
    static final Site CLOVERLY = new Site("the Cloverly Formation (Montana and Wyoming)", 44.0, 46.6, -110.2, -106.0);
    static final Site ANTLERS = new Site("the Antlers and Twin Mountains formations (Oklahoma and Texas)", 32.0, 34.8, -98.2, -95.0);
    static final Site CEDAR_MOUNTAIN = new Site("the Cedar Mountain Formation (eastern Utah)", 38.5, 39.8, -111.2, -108.8);
    static final Site WAYAN = new Site("the Wayan Formation (eastern Idaho)", 42.6, 43.6, -111.7, -110.9);
    static final Site PRINCE_CREEK = new Site("the Prince Creek Formation (North Slope, Alaska)", 69.3, 70.6, -153.5, -149.5);
    static final Site PIPESTONE = new Site("Pipestone Creek and the Wapiti Formation (Grande Prairie, Alberta)", 54.7, 55.6, -120.0, -118.0);
    static final Site CLEARWATER = new Site("the Clearwater Formation (Fort McMurray, Alberta)", 56.6, 57.6, -112.2, -110.8);
    static final Site GHOST_RANCH = new Site("the Chinle Formation (Ghost Ranch, New Mexico)", 35.9, 36.7, -106.9, -106.0);
    static final Site LIAONING = new Site("the Yixian and Jiufotang formations (Liaoning, China)", 40.5, 42.5, 119.0, 122.0);
    static final Site SHAXIMIAO = new Site("the Shaximiao Formation (Sichuan Basin, China)", 28.8, 31.5, 103.5, 106.8);
    static final Site SHISHUGOU = new Site("the Shishugou Formation (Junggar Basin, Xinjiang, China)", 44.4, 45.6, 88.0, 90.8);
    static final Site TENDAGURU = new Site("the Tendaguru Formation (Lindi, Tanzania)", -10.2, -9.3, 38.8, 39.6);
    static final Site ELRHAZ = new Site("the Elrhaz Formation (Gadoufaoua, Niger)", 16.0, 17.6, 8.0, 10.0);
    static final Site MAEVARANO = new Site("the Maevarano Formation (Mahajanga Basin, Madagascar)", -16.5, -15.7, 46.1, 46.9);
    static final Site LA_AMARGA = new Site("the La Amarga Formation (Neuquén, Argentina)", -39.8, -39.1, -70.5, -69.6);
    static final Site CERRO_BARCINO = new Site("the Cerro Barcino Formation (Chubut, Argentina)", -44.0, -43.2, -69.2, -67.6);
    static final Site TROSSINGEN = new Site("Trossingen, Frick and the Keuper of southern Germany and Switzerland", 47.3, 48.4, 7.7, 9.4);
    static final Site HALBERSTADT = new Site("Halberstadt (Saxony-Anhalt, Germany)", 51.7, 52.1, 10.8, 11.4);
    static final Site ISLE_OF_WIGHT = new Site("the Wessex Formation (Isle of Wight)", 50.55, 50.8, -1.6, -1.05);
    static final Site SURREY = new Site("the Weald Clay (Surrey and Sussex)", 50.9, 51.3, -0.8, 0.3);

    static final Site OXFORDSHIRE = new Site("the Taynton Limestone (Stonesfield, Oxfordshire)", 51.6, 52.1, -1.9, -1.0);
    static final Site LAS_HOYAS = new Site("Las Hoyas (Cuenca, Spain)", 39.9, 40.4, -2.4, -1.8);
    static final Site ISCHIGUALASTO = new Site("the Ischigualasto Formation (Valley of the Moon, San Juan, Argentina)", -30.5, -29.6, -68.2, -67.4);
    static final Site WINTON = new Site("the Winton Formation (central Queensland)", -23.2, -21.4, 142.3, 144.2);
    static final Site MUTTABURRA = new Site("the Mackunda Formation (Muttaburra, Queensland)", -23.0, -22.2, 144.2, 145.0);
    static final Site DINOSAUR_COVE = new Site("Dinosaur Cove and the Otway coast (Victoria, Australia)", -38.95, -38.55, 143.1, 143.8);
    static final Site BUNGIL = new Site("the Bungil Formation (Roma, Queensland)", -26.9, -26.2, 148.4, 149.2);
    static final Site LECHO = new Site("the Lecho Formation (El Brete, southern Salta, Argentina)", -26.5, -25.7, -65.8, -65.0);
    static final Site ELLIOT = new Site("the Elliot Formation (South Africa and Lesotho)", -31.5, -28.3, 26.8, 29.8);
    static final Site JAVELINA = new Site("the Javelina Formation (Big Bend, Texas)", 28.9, 29.8, -104.0, -102.8);
    static final Site NORTH_HORN = new Site("the North Horn Formation (central Utah)", 38.8, 39.8, -111.8, -110.8);
    static final Site LANGENBERG = new Site("the Langenberg Quarry (Oker, Lower Saxony, Germany)", 51.75, 52.05, 10.3, 10.8);
    static final Site WANGSHI = new Site("the Wangshi Group (Zhucheng, Shandong, China)", 35.6, 36.4, 119.0, 119.9);
    static final Site UDURCHUKAN = new Site("the Udurchukan Formation (Kundur, Amur, Russia)", 48.9, 49.4, 130.5, 131.3);
    static final Site KIMMERIDGE = new Site("the Kimmeridge Clay (Swindon, Wiltshire)", 51.4, 51.7, -2.0, -1.6);
    static final Site DORSET = new Site("the Charmouth Mudstone (Dorset coast)", 50.6, 50.8, -3.1, -2.6);
    static final Site BLACKLEAF = new Site("the Blackleaf Formation (southwest Montana)", 44.4, 45.4, -113.0, -112.2);

    static final Site HAKOBUCHI = new Site("the Hakobuchi Formation (Mukawa, Hokkaido, Japan)", 42.5, 43.0, 141.8, 142.4);
    static final Site KITADANI = new Site("the Kitadani Formation (Katsuyama, Fukui, Japan)", 35.95, 36.25, 136.4, 136.75);
    static final Site LAMETA_GUJARAT = new Site("the Lameta Formation at Rahioli (Gujarat, India)", 22.6, 23.3, 73.0, 73.7);
    static final Site LAMETA_CENTRAL = new Site("the Lameta Formation of central India (Jabalpur to Dongargaon)", 20.0, 23.4, 78.8, 80.3);
    static final Site ROMUALDO = new Site("the Romualdo Formation (Araripe Basin, Ceará, Brazil)", -7.6, -7.0, -40.4, -39.0);
    static final Site SANTA_MARIA = new Site("the Santa Maria Formation (Rio Grande do Sul, Brazil)", -30.0, -29.4, -54.2, -53.0);
    static final Site HATEG = new Site("the Hațeg Basin and Sebeș (Transylvania, Romania)", 45.4, 46.1, 22.6, 23.7);
    static final Site IHARKUT = new Site("the Csehbánya Formation (Iharkút, Bakony Hills, Hungary)", 47.1, 47.35, 17.5, 17.8);
    static final Site AUDE = new Site("the Marnes Rouges Inférieures (upper Aude valley, France)", 42.75, 43.1, 2.0, 2.5);
    static final Site FUVEAU = new Site("the Fuveau basin (Bouches-du-Rhône, France)", 43.35, 43.6, 5.2, 5.8);
    static final Site TRIESTE = new Site("Villaggio del Pescatore (Trieste, Italy)", 45.7, 45.85, 13.5, 13.7);
    static final Site SAVANNAKHET = new Site("the Grès supérieurs Formation (Savannakhet, Laos)", 16.2, 17.1, 105.2, 106.2);
    static final Site SAO_KHUA = new Site("the Sao Khua Formation (Phu Wiang, Khon Kaen, Thailand)", 16.4, 16.9, 102.1, 102.6);
    static final Site KARONGA = new Site("the Dinosaur Beds (Karonga, Malawi)", -10.4, -9.5, 33.6, 34.2);
    static final Site KULINDA = new Site("Kulinda (Zabaykalsky Krai, Russia)", 52.3, 52.8, 116.0, 117.2);
    static final Site WOODBURY = new Site("the Woodbury and New Egypt formations (southern New Jersey)", 39.6, 40.4, -75.3, -74.0);
    static final Site DEMOPOLIS = new Site("the Demopolis Chalk (Montgomery County, Alabama)", 32.0, 32.7, -86.7, -85.9);
    static final Site PORTLAND = new Site("the Portland Formation (Connecticut River valley, Connecticut and Massachusetts)", 41.5, 42.4, -72.8, -72.3);
    static final Site BISSEKTY = new Site("the Bissekty Formation (Dzharakuduk, Kyzylkum Desert, Uzbekistan)", 41.9, 42.4, 62.2, 63.0);
    static final Site NIGER_AGADEZ = new Site("the Tiouraren and Echkar formations (Agadez region, Niger)", 16.2, 18.2, 5.3, 8.3);
    static final Site CERRO_FORTALEZA = new Site("the Cerro Fortaleza Formation (Santa Cruz, Argentina)", -50.4, -49.6, -72.6, -71.6);
    static final Site LAGUNA_COLORADA = new Site("the Laguna Colorada Formation (Santa Cruz, Argentina)", -48.6, -47.8, -70.0, -68.0);
    static final Site IREN_DABASU = new Site("the Iren Dabasu Formation (Erenhot, Inner Mongolia, China)", 43.4, 43.9, 111.6, 112.8);
    static final Site TIAOJISHAN = new Site("the Tiaojishan Formation (Jianchang, Liaoning, China)", 40.5, 41.2, 119.4, 120.2);
    static final Site JINGANGKOU = new Site("the Jingangkou Formation (Laiyang, Shandong, China)", 36.8, 37.2, 120.4, 121.0);
    static final Site NANXIONG = new Site("the Nanxiong Formation (Ganzhou, Jiangxi, China)", 25.5, 26.1, 114.6, 115.3);
    static final Site ST_MARY_RIVER = new Site("the St. Mary River Formation (southwestern Alberta)", 49.2, 49.9, -114.3, -113.3);
    static final Site RIODEVA = new Site("the Villar del Arzobispo Formation (Riodeva, Teruel, Spain)", 40.0, 40.25, -1.3, -1.0);

    static final Site LUFENG = new Site("the Lufeng Formation (Lufeng and Wuding, Yunnan, China)", 25.0, 25.7, 101.9, 102.5);
    static final Site PORTEZUELO = new Site("the Portezuelo Formation (Sierra del Portezuelo and Lago Barreales, Neuquén)", -39.1, -38.3, -69.8, -68.5);
    static final Site AUCA_MAHUEVO = new Site("the Anacleto Formation at Auca Mahuevo (Neuquén, Argentina)", -38.1, -37.6, -68.9, -68.2);
    static final Site LA_BUITRERA = new Site("the Candeleros Formation at La Buitrera (Río Negro, Argentina)", -39.8, -39.45, -68.95, -68.45);
    static final Site ALLEN = new Site("the Allen Formation (Bajo de Santa Rosa, Río Negro, Argentina)", -40.3, -39.7, -67.1, -66.3);
    static final Site MORENO_HILL = new Site("the Moreno Hill Formation (Zuni Basin, New Mexico)", 34.3, 35.2, -109.1, -108.5);
    static final Site TROPIC_SHALE = new Site("the Tropic Shale (Big Water, Utah)", 36.9, 37.3, -111.9, -111.4);
    static final Site WAHWEAP = new Site("the Wahweap Formation (Grand Staircase-Escalante, Utah)", 37.0, 37.4, -111.9, -111.1);
    static final Site CANADON_CALCAREO = new Site("the Cañadón Calcáreo Formation (Cerro Cóndor, Chubut, Argentina)", -43.4, -43.0, -69.4, -68.8);
    static final Site TILGATE = new Site("the Grinstead Clay (Tilgate Forest, West Sussex)", 51.0, 51.2, -0.3, 0.0);
    static final Site BARUN_GOYOT = new Site("the Barun Goyot Formation (Khulsan, Gobi Desert, Mongolia)", 43.3, 43.7, 99.6, 101.4);

    static final Site OXFORD_CLAY = new Site("the Oxford Clay (Summertown and Wolvercote, Oxford)", 51.7, 51.9, -1.4, -1.2);
    static final Site GLEICHBERG = new Site("the Trossingen Formation at the Grosser Gleichberg (Römhild, Thuringia)", 50.3, 50.5, 10.45, 10.75);
    static final Site FOREST_MARBLE = new Site("the Forest Marble Formation (Bletchingdon and Kirtlington, Oxfordshire)", 51.8, 52.0, -1.4, -1.2);
    static final Site RUTLAND = new Site("the Rutland Formation (Great Casterton, Rutland)", 52.5, 52.7, -0.8, -0.5);
    static final Site KOTA = new Site("the Kota Formation (Pranhita-Godavari valley, India)", 18.7, 19.4, 79.3, 80.2);
    static final Site GUETTIOUA = new Site("the Guettioua Formation (Azilal, High Atlas, Morocco)", 31.8, 32.2, -6.6, -6.0);
    static final Site LOS_COLORADOS = new Site("the Los Colorados Formation (La Rioja, Argentina)", -30.2, -29.5, -68.3, -67.6);
    static final Site BRISTOL = new Site("the Triassic fissure fills of Bristol (Durdham Down and Tytherington)", 51.4, 51.7, -2.75, -2.4);
    static final Site CINCO_SALTOS = new Site("the Anacleto Formation (Cinco Saltos, Río Negro, Argentina)", -39.1, -38.6, -68.4, -67.8);
    static final Site BLAGOVESHCHENSK = new Site("the Udurchukan Formation (Blagoveshchensk, Amur, Russia)", 50.1, 50.5, 127.3, 127.8);
    static final Site JIAYIN = new Site("the Yuliangze Formation (Jiayin, Heilongjiang, China)", 48.7, 49.1, 130.0, 130.7);
    static final Site LIANMUQIN = new Site("the Lianmuqin Formation (Wuerho, Junggar Basin, Xinjiang)", 45.8, 46.3, 85.4, 86.1);

    // ---------------------------------------------------------------- voices (vanilla recordings, pitched)

    static final Voice BIG_ROAR = new Voice("growls", "entity.ravager.ambient", 0.55f, "entity.ravager.hurt", 0.6f, "entity.ravager.death", 0.5f,
            "entity.ravager.attack", 0.65f, "entity.warden.roar", 0.72f, 1.2f);
    static final Voice ROAR = new Voice("growls", "entity.hoglin.ambient", 0.55f, "entity.hoglin.hurt", 0.6f, "entity.hoglin.death", 0.55f,
            "entity.ravager.attack", 0.9f, "entity.ravager.roar", 0.8f, 1f);
    static final Voice GROWL = new Voice("growls", "entity.hoglin.angry", 0.85f, "entity.hoglin.hurt", 0.9f, "entity.hoglin.death", 0.85f,
            "entity.fox.bite", 0.6f, "entity.polar_bear.warning", 0.9f, 0.9f);
    static final Voice RAPTOR_CALL = new Voice("chirps", "entity.parrot.ambient", 0.55f, "entity.fox.hurt", 0.7f, "entity.fox.death", 0.6f,
            "entity.cat.hiss", 0.8f, "entity.fox.screech", 0.75f, 0.9f);
    static final Voice CHIRP = new Voice("chirps", "entity.parrot.ambient", 1.1f, "entity.parrot.hurt", 1.0f, "entity.parrot.death", 1.0f,
            "entity.fox.bite", 1.2f, "entity.fox.screech", 1.3f, 0.7f);
    static final Voice BIRD = new Voice("calls", "entity.parrot.ambient", 0.45f, "entity.goat.hurt", 0.8f, "entity.goat.death", 0.7f,
            "entity.fox.bite", 0.8f, "entity.goat.screaming.ambient", 0.6f, 0.9f);
    static final Voice HORN = new Voice("honks", "event.raid.horn", 1.35f, "entity.camel.hurt", 0.7f, "entity.camel.death", 0.6f,
            "entity.goat.ram_impact", 0.6f, "block.note_block.didgeridoo", 0.5f, 0.6f);
    static final Voice BELLOW = new Voice("bellows", "entity.camel.ambient", 0.6f, "entity.camel.hurt", 0.6f, "entity.camel.death", 0.55f,
            "entity.goat.ram_impact", 0.7f, "block.note_block.didgeridoo", 0.6f, 1f);
    static final Voice SAUROPOD = new Voice("rumbles", "entity.sniffer.idle", 0.45f, "entity.sniffer.hurt", 0.45f, "entity.sniffer.death", 0.4f,
            "entity.warden.attack_impact", 0.6f, "block.note_block.didgeridoo", 0.5f, 1.3f);
    static final Voice CERATOPS = new Voice("grunts", "entity.hoglin.ambient", 0.6f, "entity.hoglin.hurt", 0.6f, "entity.hoglin.death", 0.55f,
            "entity.goat.ram_impact", 0.5f, "entity.ravager.ambient", 0.7f, 1f);
    static final Voice STEGO = new Voice("grunts", "entity.sniffer.idle", 0.7f, "entity.hoglin.hurt", 0.5f, "entity.sniffer.death", 0.6f,
            "entity.goat.ram_impact", 0.6f, "entity.sniffer.happy", 0.6f, 1f);
    static final Voice ANKY = new Voice("snorts", "entity.armadillo.ambient", 0.45f, "entity.armadillo.hurt", 0.5f, "entity.armadillo.death", 0.45f,
            "entity.iron_golem.attack", 0.7f, "entity.hoglin.ambient", 0.5f, 1f);
    static final Voice GOAT = new Voice("grunts", "entity.goat.ambient", 0.7f, "entity.goat.hurt", 0.7f, "entity.goat.death", 0.65f,
            "entity.goat.ram_impact", 0.8f, "entity.goat.screaming.prepare_ram", 0.7f, 1f);
    static final Voice SMALL_HERB = new Voice("bleats", "entity.goat.ambient", 1.0f, "entity.goat.hurt", 1.0f, "entity.goat.death", 1.0f,
            "entity.fox.bite", 0.8f, "entity.goat.ambient", 0.8f, 0.8f);
    static final Voice HOOT = new Voice("hoots", "entity.polar_bear.ambient", 0.6f, "entity.polar_bear.hurt", 0.6f, "entity.polar_bear.death", 0.55f,
            "entity.player.attack.sweep", 0.6f, "entity.polar_bear.warning", 0.6f, 1f);

    static Look look(int base, int back, int belly, int mark, DinoSpecies.Pattern p, float scale, int accent, int accent2, int eye, int horn, int claw) {
        return new Look(base, back, belly, mark, p, scale, accent, accent2, eye, horn, claw);
    }

    static DinoSpecies sp(String id, String name, Body.Plan plan, float length) { return new DinoSpecies(id, name, plan, length); }

    // ---------------------------------------------------------------- the animals

    public static final List<DinoSpecies> ALL;
    private static final Map<String, DinoSpecies> BY_ID = new LinkedHashMap<>();

    static {
        List<DinoSpecies> l = new ArrayList<>();

        // ---- Hell Creek (latest Cretaceous, western North America)
        l.add(sp("tyrannosaurus", "Tyrannosaurus rex", THEROPOD, 12.3f).shortName("Tyrannosaurus").real(8400, 3.4f).speed(1.3f, 5.3f)
                .temper(APEX, BITE, CARNIVORE).group(1, 1, 1).age("Late Cretaceous, 68-66 million years ago").site(HELL_CREEK)
                .body(b -> { b.skull = 1.45f; b.skullW = 0.55f; b.snout = 0.42f; b.snoutW = 0.62f; b.neck = 1.15f; b.neckD = 1.0f;
                    b.torso = 3.0f; b.depth = 1.85f; b.width = 1.45f; b.legW = 0.85f; b.thighA = 15; b.shinA = -22; b.metaA = 18; b.shinF = 0.85f; b.skullH = 0.56f;
                    b.armL = 0.95f; b.armW = 0.16f; b.footL = 0.75f; b.tailD = 1.05f; })
                .look(look(0x6E5A44, 0x4C3E31, 0xCBB898, 0x3A2F25, STRIPES, 1f, 0x8E4A30, 0xD8C9A8, 0xD7A21E, 0xC9BBA0, 0x2B2420)).voice(BIG_ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("triceratops", "Triceratops horridus", CERATOPSIAN, 8.5f).shortName("Triceratops").real(8000, 2.15f).shoulder(1.75f).speed(1.4f, 6.5f)
                .temper(DEFENDER, GORE, HERBIVORE).group(1, 3, 4).age("Late Cretaceous, 68-66 million years ago").site(HELL_CREEK)
                .body(b -> { b.skull = 1.55f; b.skullH = 0.62f; b.skullW = 0.55f; b.frillH = 1.1f; b.frillW = 1.65f; b.frillUp = 52;
                    b.hornBrow = 1.0f; b.hornBrowUp = 32; b.hornBrowOut = 12; b.hornNasal = 0.32f; b.hornNasalUp = 60; b.hornCheek = 0.12f;
                    b.torso = 2.9f; b.depth = 1.9f; b.width = 1.65f; b.legW = 0.62f; b.frontW = 0.52f; })
                .look(look(0x7A6A55, 0x5E5142, 0xCDBB9C, 0x4A3E31, SADDLES, 1f, 0xA35A30, 0xEAD9AE, 0x3A2A10, 0xE3D6B8, 0x2E2A25)).voice(CERATOPS)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("ankylosaurus", "Ankylosaurus magniventris", ANKYLOSAUR, 7.0f).shortName("Ankylosaurus").real(6000, 1.35f).shoulder(1.15f).speed(1.0f, 2.7f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(1, 1, 2).age("Late Cretaceous, 68-66 million years ago").site(HELL_CREEK)
                .body(b -> { b.skull = 0.66f; b.skullW = 1.1f; b.width = 1.75f; b.depth = 1.15f; b.torso = 2.7f; b.clubW = 0.6f; b.hornBrow = 0.14f;
                    b.hornBrowUp = 0; b.hornBrowOut = 60; b.hornBrowZ = 0; b.legW = 0.42f; b.frontW = 0.36f; })
                .look(look(0x8A6E4E, 0x6A5238, 0xD2BFA0, 0x4E3A28, MOTTLED, 1f, 0x9C7B55, 0x5A4430, 0x2E1E0E, 0xD7C8A8, 0x2E2A25)).voice(ANKY)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("pachycephalosaurus", "Pachycephalosaurus wyomingensis", PACHYCEPHALOSAUR, 4.5f).shortName("Pachycephalosaurus").real(400, 1.15f).speed(1.5f, 7f)
                .temper(DEFENDER, HEADBUTT, HERBIVORE).group(1, 4, 2).age("Late Cretaceous, 70-66 million years ago").site(HELL_CREEK)
                .body(b -> { b.skull = 0.6f; b.dome = 0.25f; })
                .look(look(0xA0845E, 0x7E6646, 0xE2D3B6, 0x5E4630, SPOTS, 1f, 0xDDD0B0, 0x8A3A2A, 0xD0A030, 0xCFC2A6, 0x3A3530)).voice(GOAT)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("edmontosaurus", "Edmontosaurus regalis and E. annectens", HADROSAUR, 12f).shortName("Edmontosaurus").real(6000, 3.0f).shoulder(2.15f).speed(1.5f, 9f)
                .temper(SKITTISH, KICK, HERBIVORE).group(4, 10, 5).age("Late Cretaceous, 73-66 million years ago").site(HELL_CREEK, HORSESHOE_CANYON)
                .body(b -> { b.skull = 1.15f; b.crest = Body.Crest.COMB; b.crestH = 0.18f; b.crestL = 0.45f; })
                .look(look(0x6E7A6E, 0x55605A, 0xD8D6C6, 0x434C46, MOTTLED, 1f, 0xB8402E, 0xD7CDB0, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(BELLOW)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE | DinoSpecies.COLD));

        // ---- Alberta and the American southwest (Campanian)
        l.add(sp("parasaurolophus", "Parasaurolophus walkeri, P. cyrtocristatus and P. tubicen", HADROSAUR, 7.5f).shortName("Parasaurolophus").real(2600, 2.05f).shoulder(1.46f).speed(1.5f, 9f)
                .temper(SKITTISH, KICK, HERBIVORE).group(3, 8, 4).age("Late Cretaceous, 76.5-73 million years ago").site(DINOSAUR_PARK, KAIPAROWITS, SAN_JUAN)
                .body(b -> { b.skull = 0.95f; b.crest = Body.Crest.TUBE; b.crestL = 1.1f; b.crestH = 0.2f; })
                .look(look(0x7F8456, 0x5E6440, 0xDDD3AE, 0x3E4228, STRIPES, 1f, 0xC8572E, 0xF0DFA0, 0x2E2A10, 0xBDB19A, 0x3A3530)).voice(HORN));
        l.add(sp("styracosaurus", "Styracosaurus albertensis", CERATOPSIAN, 5.5f).shortName("Styracosaurus").real(2700, 1.4f).shoulder(1.15f).speed(1.3f, 6f)
                .temper(DEFENDER, GORE, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, 75.5-75 million years ago").site(DINOSAUR_PARK)
                .body(b -> { b.skull = 0.7f; b.hornNasal = 0.57f; b.hornNasalUp = 75; b.frillH = 0.6f; b.frillW = 1.0f; b.frillUp = 60; b.frillSpikes = 6; b.frillSpikeL = 0.55f;
                    b.hornBrow = 0.08f; b.torso = 1.9f; b.depth = 1.25f; b.width = 1.05f; })
                .look(look(0x806650, 0x5E4A3A, 0xD6C6A8, 0x4A3A2C, SADDLES, 1f, 0x7A3A26, 0xE6CFA0, 0x2E1E0E, 0xE3D6B8, 0x2E2A25)).voice(CERATOPS));

        // ---- the Morrison Formation (Late Jurassic)
        l.add(sp("allosaurus", "Allosaurus fragilis and A. europaeus", THEROPOD, 8.5f).shortName("Allosaurus").real(2000, 2.05f).speed(1.5f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 2, 2).age("Late Jurassic, 155-145 million years ago").site(MORRISON, LOURINHA)
                .body(b -> { b.skull = 0.85f; b.skullW = 0.33f; b.skullH = 0.42f; b.hornBrow = 0.1f; b.hornBrowUp = 60; b.hornBrowOut = 5;
                    b.crest = Body.Crest.TWIN; b.crestH = 0.05f; b.crestL = 0.45f; b.armL = 1.25f; b.armW = 0.13f; b.clawL = 0.18f; })
                .look(look(0xA0805A, 0x7A5C3E, 0xE4D6B8, 0x4E3A26, STRIPES, 1.2f, 0xB43A2A, 0xE8D8B8, 0xE0B030, 0xD8CBB0, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("stegosaurus", "Stegosaurus stenops and S. ungulatus", STEGOSAUR, 7f).shortName("Stegosaurus").real(4000, 2.0f).shoulder(1.25f).speed(1.1f, 2.5f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(1, 3, 3).age("Late Jurassic, 155-150 million years ago").site(MORRISON, LOURINHA)
                .body(b -> { b.skull = 0.4f; b.plateH = 0.62f; b.tailSpikeL = 0.75f; b.torso = 2.1f; b.depth = 1.4f; b.width = 0.95f; b.legW = 0.42f; b.frontW = 0.33f; })
                .look(look(0x7C7A52, 0x5A5A3A, 0xD2C9A2, 0x4E4A2E, SPOTS, 1f, 0xB4532A, 0x5A6A38, 0x2A1A0A, 0xD9CDB1, 0x2E2A25)).voice(STEGO)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("brachiosaurus", "Brachiosaurus altithorax", BRACHIOSAUR, 20f).shortName("Brachiosaurus").real(35000, 3.9f).shoulder(4.6f).speed(1.4f, 3.5f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(1, 3, 2).age("Late Jurassic, 154-153 million years ago").site(MORRISON)
                .body(b -> { b.skull = 0.85f; b.crest = Body.Crest.ROUND; b.crestH = 0.18f; b.crestL = 0.35f; })
                .look(look(0x7E8A80, 0x5F6B62, 0xCFCFC0, 0x55615A, MOTTLED, 1.5f, 0x9A6A45, 0x8A9A8E, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID | DinoSpecies.TROPICAL));
        l.add(sp("diplodocus", "Diplodocus carnegii", DIPLODOCID, 25f).shortName("Diplodocus").real(14000, 3.3f).shoulder(2.85f).speed(1.6f, 3.5f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(2, 6, 3).age("Late Jurassic, 154-152 million years ago").site(MORRISON)
                .body(b -> { b.skull = 0.6f; b.neckSpines = 0.18f; b.legW = 1.0f; b.frontW = 0.8f; b.depth = 3.0f; })
                .look(look(0x8A8070, 0x6A6255, 0xD6CDBA, 0x5A5246, BANDS, 1.4f, 0x7A5040, 0xA89A86, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));

        // ---- the Gobi (Campanian Djadochta, Maastrichtian Nemegt)
        l.add(sp("velociraptor", "Velociraptor mongoliensis", RAPTOR, 2.0f).shortName("Velociraptor").real(17, 0.45f).speed(1.5f, 10f)
                .temper(PACK, KICK, CARNIVORE).group(2, 4, 3).age("Late Cretaceous, 75-71 million years ago").site(DJADOCHTA)
                .look(look(0x9C7A4E, 0x6E5434, 0xE2D2B0, 0x3B2C1C, BARS, 1.2f, 0x5A3B23, 0xF0E6D2, 0xE0A520, 0x2B2420, 0x1E1A16)).voice(RAPTOR_CALL)
                .climates(DinoSpecies.ARID));
        l.add(sp("protoceratops", "Protoceratops andrewsi", PROTOCERATOPS, 2.2f).shortName("Protoceratops").real(83, 0.6f).shoulder(0.48f).speed(1.2f, 5f)
                .temper(SKITTISH, BITE, HERBIVORE).group(3, 8, 5).age("Late Cretaceous, 75-71 million years ago").site(DJADOCHTA)
                .body(b -> { b.sailOnTail = 0.1f; })
                .look(look(0xB59A6E, 0x8E7650, 0xE6DABE, 0x6A5236, SPOTS, 1f, 0x9A5A36, 0xEAD2A0, 0x2E2010, 0x6A5E4E, 0x3A3530)).voice(SMALL_HERB)
                .climates(DinoSpecies.ARID));
        l.add(sp("oviraptor", "Oviraptor philoceratops", OVIRAPTOR, 1.8f).shortName("Oviraptor").real(35, 0.55f).speed(1.4f, 8f)
                .temper(SKITTISH, KICK, OMNIVORE).group(1, 3, 2).age("Late Cretaceous, 75-71 million years ago").site(DJADOCHTA)
                .body(b -> { b.crest = Body.Crest.ROUND; b.crestH = 0.07f; b.crestL = 0.09f; })
                .look(look(0x4E4A48, 0x3A3634, 0xD8D0C2, 0xF0EEE6, BARS, 1f, 0x3A6EA8, 0xC84030, 0xE0B030, 0x2B2A28, 0x2B2420)).voice(BIRD)
                .climates(DinoSpecies.ARID));
        l.add(sp("gallimimus", "Gallimimus bullatus", ORNITHOMIMID, 6f).shortName("Gallimimus").real(440, 1.85f).speed(1.8f, 12f)
                .temper(SKITTISH, KICK, OMNIVORE).group(4, 12, 4).age("Late Cretaceous, 70 million years ago").site(NEMEGT)
                .look(look(0xB59E7A, 0x8C7656, 0xF0E8D8, 0x5A4A36, PLAIN, 1f, 0x4A3E32, 0xF2EEE6, 0xD08A20, 0x8A7A60, 0x2B2420)).voice(BIRD)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("therizinosaurus", "Therizinosaurus cheloniformis", THERIZINOSAUR, 9.5f).shortName("Therizinosaurus").real(5000, 2.7f).speed(1.0f, 3.5f)
                .temper(DEFENDER, CLAW, HERBIVORE).group(1, 1, 1).age("Late Cretaceous, 70 million years ago").site(NEMEGT)
                .body(b -> { b.clawL = 0.75f; b.armL = 2.6f; b.armW = 0.24f; })
                .look(look(0x6A4E36, 0x4E3828, 0xC9B08E, 0x3A2A1E, PLAIN, 1f, 0xD8C8A8, 0x2E2218, 0x2A1A0A, 0x9A8A70, 0xE8E0CC)).voice(HOOT)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));

        // ---- South America
        l.add(sp("giganotosaurus", "Giganotosaurus carolinii", THEROPOD, 12.5f).shortName("Giganotosaurus").real(7000, 3.3f).speed(1.5f, 6f)
                .temper(APEX, BITE, CARNIVORE).group(1, 2, 1).age("Late Cretaceous, 99.6-97 million years ago").site(NEUQUEN)
                .body(b -> { b.skull = 1.6f; b.skullW = 0.3f; b.skullH = 0.42f; b.snout = 0.5f; b.torso = 3.2f; b.depth = 1.75f; b.width = 1.25f;
                    b.armL = 1.6f; b.armW = 0.18f; b.clawL = 0.2f; b.legW = 0.78f; b.thighA = 16; b.shinA = -24; b.metaA = 18; b.shinF = 0.88f; b.crest = Body.Crest.RIDGE; b.crestH = 0.08f; b.crestL = 0.7f; })
                .look(look(0x7C7368, 0x5A534A, 0xD6CEBE, 0x3E3830, STRIPES, 1.3f, 0x8A4A3A, 0xCFC5B0, 0xD9A02A, 0xD8CDB2, 0x2B2420)).voice(BIG_ROAR));
        l.add(sp("argentinosaurus", "Argentinosaurus huinculensis", TITANOSAUR, 33f).shortName("Argentinosaurus").real(70000, 5.0f).shoulder(5.1f).speed(1.5f, 2.3f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(2, 5, 2).age("Late Cretaceous, 96-92 million years ago").site(NEUQUEN)
                .body(b -> { b.skull = 1.0f; b.legW = 1.6f; b.frontW = 1.4f; b.width = 3.9f; b.depth = 4.2f; b.torso = 6.8f; b.neckD = 1.6f; })
                .look(look(0x9A8E7A, 0x7A6E5C, 0xDCD4C2, 0x6A604E, MOTTLED, 1.8f, 0x8A7A62, 0xB8AC96, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD));
        l.add(sp("carnotaurus", "Carnotaurus sastrei", THEROPOD, 7.8f).shortName("Carnotaurus").real(1500, 2.1f).speed(1.6f, 10f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Late Cretaceous, 72-70 million years ago").site(LA_COLONIA)
                .body(b -> { b.skull = 0.6f; b.skullH = 0.55f; b.skullW = 0.45f; b.snout = 0.38f; b.hornBrow = 0.16f; b.hornBrowUp = 70; b.hornBrowOut = 55;
                    b.armL = 0.42f; b.armW = 0.1f; b.neck = 0.95f; b.torso = 1.9f; b.depth = 1.15f; b.width = 0.8f; b.legW = 0.52f; b.armour = 0.07f; })
                .look(look(0x9A5A3E, 0x6E3E2A, 0xDFC9A8, 0x4A2A1C, SPOTS, 0.8f, 0x3A2A22, 0xC9A07A, 0xE0A020, 0x4A3A30, 0x2B2420)).voice(ROAR));

        // ---- Africa
        l.add(sp("spinosaurus", "Spinosaurus aegyptiacus", SPINOSAUR, 14f).shortName("Spinosaurus").real(7000, 2.2f).speed(1.2f, 5f)
                .temper(HUNTER, BITE, PISCIVORE).group(1, 1, 1).age("Late Cretaceous, 99-93.5 million years ago").site(KEM_KEM, BAHARIYA)
                .aquatic().body(b -> { b.skull = 1.6f; b.sailH = 1.65f; b.sailFrom = 0.05f; b.sailTo = 1.0f; b.sailOnTail = 0.9f; b.tailFin = 1.0f; b.crest = Body.Crest.RIDGE;
                    b.crestH = 0.08f; b.crestL = 0.35f; b.armL = 1.8f; b.armW = 0.2f; b.clawL = 0.25f; })
                .look(look(0x5C5A4E, 0x403F36, 0xBDB59A, 0x2E2D26, STRIPES, 1.4f, 0xC2502E, 0xE8C47A, 0xE0B030, 0xD8CDB2, 0x2B2420)).voice(BIG_ROAR)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.ARID | DinoSpecies.WARM));

        // ---- Europe
        l.add(sp("iguanodon", "Iguanodon bernissartensis", IGUANODONT, 10f).shortName("Iguanodon").real(3500, 2.7f).shoulder(2.0f).speed(1.4f, 6.7f)
                .temper(DEFENDER, CLAW, HERBIVORE).group(3, 8, 4).age("Early Cretaceous, 126-122 million years ago").site(WEALDEN, IBERIA_BARREMIAN, NEHDEN)
                .body(b -> { b.skull = 0.9f; b.thumbSpike = 0.2f; })
                .look(look(0x6F7A5A, 0x56604A, 0xD3D0B5, 0x48523C, BANDS, 1f, 0x9A8A6A, 0xC8C2A0, 0x2A2A1A, 0xDCD2B5, 0x3A3530)).voice(BELLOW)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("compsognathus", "Compsognathus longipes", SMALL_THEROPOD, 1.1f).shortName("Compsognathus").real(3, 0.24f).speed(1.3f, 8f)
                .temper(SKULKER, BITE, CARNIVORE).group(3, 7, 3).age("Late Jurassic, 150 million years ago").site(SOLNHOFEN, CANJUERS)
                .body(b -> { b.feathers = true; b.skull = 0.085f; })
                .look(look(0x8A8A50, 0x6A6A3A, 0xE8E2C2, 0x3E4024, STRIPES, 0.8f, 0x5A5A2E, 0xEADFB0, 0xD8A020, 0xC9BBA0, 0x2B2420)).voice(CHIRP)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));

        // ---- Arizona (Early Jurassic)
        l.add(sp("dilophosaurus", "Dilophosaurus wetherilli", THEROPOD, 7f).shortName("Dilophosaurus").real(400, 1.6f).speed(1.5f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 2, 2).age("Early Jurassic, about 186 million years ago").site(KAYENTA)
                .body(b -> { b.skull = 0.6f; b.skullW = 0.28f; b.skullH = 0.38f; b.snout = 0.5f; b.crest = Body.Crest.TWIN; b.crestH = 0.17f; b.crestL = 0.4f;
                    b.neck = 0.95f; b.neckD = 0.32f; b.torso = 1.55f; b.depth = 0.85f; b.width = 0.55f; b.legW = 0.32f; b.armL = 0.95f; b.armW = 0.08f; b.clawL = 0.08f; })
                .look(look(0x9C8A5A, 0x6E6440, 0xE2D8B6, 0x4A4028, STRIPES, 1f, 0xC84A2A, 0xF2C66A, 0xE0B020, 0xD8CDB2, 0x2B2420)).voice(GROWL)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));

        // ================================================================ batch 2 (0.7.1)

        // ---- tyrannosaurids
        l.add(sp("albertosaurus", "Albertosaurus sarcophagus", THEROPOD, 9f).shortName("Albertosaurus").real(2000, 2.4f).speed(1.5f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 3, 2).age("Late Cretaceous, 71-68 million years ago").site(HORSESHOE_CANYON)
                .body(b -> { b.skull = 1.0f; b.skullW = 0.42f; b.skullH = 0.5f; b.armL = 0.8f; b.armW = 0.1f; b.shinF = 1.05f; b.metaF = 0.6f;
                    b.hornBrow = 0.08f; b.hornBrowUp = 70; b.hornBrowOut = 5; })
                .look(look(0x8B7458, 0x645139, 0xDCCBA8, 0x4A3C2A, STRIPES, 1.1f, 0xA8442E, 0xD8C9A8, 0xD9A02A, 0xCFC2A6, 0x2B2420)).voice(ROAR));
        l.add(sp("gorgosaurus", "Gorgosaurus libratus", THEROPOD, 8.6f).shortName("Gorgosaurus").real(2500, 2.4f).speed(1.5f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 2, 2).age("Late Cretaceous, 76.6-75 million years ago").site(DINOSAUR_PARK)
                .body(b -> { b.skull = 0.99f; b.skullW = 0.42f; b.skullH = 0.5f; b.armL = 0.75f; b.armW = 0.1f; b.shinF = 1.05f; b.metaF = 0.6f;
                    b.hornBrow = 0.1f; b.hornBrowUp = 75; b.hornBrowOut = 5; })
                .look(look(0x5E5A4C, 0x45423A, 0xD0C8B0, 0x2E2C26, BANDS, 1.2f, 0xB8402E, 0xD8C9A8, 0xD9A02A, 0xCFC2A6, 0x2B2420)).voice(ROAR));
        l.add(sp("daspletosaurus", "Daspletosaurus torosus and D. horneri", THEROPOD, 9f).shortName("Daspletosaurus").real(3000, 2.5f).speed(1.4f, 6.5f)
                .temper(APEX, BITE, CARNIVORE).group(1, 2, 1).age("Late Cretaceous, 77-75 million years ago").site(DINOSAUR_PARK, TWO_MEDICINE)
                .body(b -> { b.skull = 1.1f; b.skullW = 0.48f; b.skullH = 0.55f; b.armL = 0.75f; b.armW = 0.12f; b.legW = 0.6f; b.hornBrow = 0.1f; b.hornBrowUp = 70; b.hornBrowOut = 5; })
                .look(look(0x7A5A44, 0x553E2E, 0xD6C2A2, 0x3A2A20, SADDLES, 1f, 0x9A3E2A, 0xD8C9A8, 0xE0A020, 0xCFC2A6, 0x2B2420)).voice(BIG_ROAR));
        l.add(sp("tarbosaurus", "Tarbosaurus bataar", THEROPOD, 10.5f).shortName("Tarbosaurus").real(5000, 2.9f).speed(1.4f, 6f)
                .temper(APEX, BITE, CARNIVORE).group(1, 1, 1).age("Late Cretaceous, 70 million years ago").site(NEMEGT)
                .body(b -> { b.skull = 1.3f; b.skullW = 0.38f; b.skullH = 0.5f; b.armL = 0.75f; b.armW = 0.12f; b.legW = 0.7f; b.thighA = 16; b.shinA = -24; b.metaA = 18; })
                .look(look(0x9C8462, 0x7A6446, 0xE6D8BA, 0x5A4630, STRIPES, 1.3f, 0x8E4A30, 0xD8C9A8, 0xD7A21E, 0xC9BBA0, 0x2B2420)).voice(BIG_ROAR)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("yutyrannus", "Yutyrannus huali", THEROPOD, 9f).shortName("Yutyrannus").real(1400, 2.2f).speed(1.4f, 7f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 3, 2).age("Early Cretaceous, 125 million years ago").site(LIAONING)
                .body(b -> { b.feathers = true; b.skull = 0.9f; b.crest = Body.Crest.RIDGE; b.crestH = 0.07f; b.crestL = 0.5f; b.armL = 1.1f; b.armW = 0.1f; b.clawL = 0.12f; })
                .look(look(0x8A7A6A, 0x5E5246, 0xE8E0D2, 0x4A3E34, BANDS, 1.2f, 0xC0603A, 0xE8E0D2, 0xD9A02A, 0xCFC2A6, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.COLD));

        // ---- carcharodontosaurs and other big theropods
        l.add(sp("carcharodontosaurus", "Carcharodontosaurus saharicus", THEROPOD, 12f).shortName("Carcharodontosaurus").real(6500, 3.2f).speed(1.5f, 6f)
                .temper(APEX, BITE, CARNIVORE).group(1, 1, 1).age("Late Cretaceous, 100-94 million years ago").site(KEM_KEM, BAHARIYA)
                .body(b -> { b.skull = 1.6f; b.skullW = 0.3f; b.skullH = 0.42f; b.snout = 0.5f; b.torso = 3.0f; b.depth = 1.7f; b.width = 1.2f; b.armL = 1.4f; b.armW = 0.16f;
                    b.clawL = 0.18f; b.legW = 0.75f; b.thighA = 16; b.shinA = -24; b.metaA = 18; b.shinF = 0.88f; })
                .look(look(0xA88A62, 0x7E6646, 0xE8DCC0, 0x5A4630, STRIPES, 1.4f, 0x9A4A30, 0xE0D0B0, 0xE0B030, 0xD8CDB2, 0x2B2420)).voice(BIG_ROAR)
                .climates(DinoSpecies.ARID | DinoSpecies.TROPICAL | DinoSpecies.WARM));
        l.add(sp("acrocanthosaurus", "Acrocanthosaurus atokensis", THEROPOD, 11.5f).shortName("Acrocanthosaurus").real(5500, 3.0f).speed(1.4f, 6f)
                .temper(APEX, BITE, CARNIVORE).group(1, 1, 1).age("Early Cretaceous, 113-110 million years ago").site(ANTLERS)
                .body(b -> { b.skull = 1.29f; b.skullW = 0.3f; b.skullH = 0.42f; b.snout = 0.5f; b.sailH = 0.55f; b.sailFrom = 0f; b.sailTo = 1f; b.sailOnTail = 0.7f;
                    b.armL = 1.4f; b.armW = 0.15f; b.clawL = 0.2f; b.legW = 0.72f; b.crest = Body.Crest.RIDGE; b.crestH = 0.06f; b.crestL = 0.55f; })
                .look(look(0x7E6E5C, 0x5A4E42, 0xDCD2C0, 0x3E362E, BANDS, 1.4f, 0x8A3E2A, 0xC8B080, 0xE0B030, 0xD8CDB2, 0x2B2420)).voice(BIG_ROAR));
        l.add(sp("ceratosaurus", "Ceratosaurus nasicornis", THEROPOD, 6f).shortName("Ceratosaurus").real(700, 1.7f).speed(1.5f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Late Jurassic, 153-148 million years ago").site(MORRISON, LOURINHA)
                .body(b -> { b.skull = 0.62f; b.skullW = 0.3f; b.skullH = 0.5f; b.hornNasal = 0.17f; b.hornNasalUp = 80; b.hornBrow = 0.07f; b.hornBrowUp = 70; b.hornBrowOut = 5;
                    b.armour = 0.06f; b.armourCols = 1; b.tailD = 0.6f; b.tailW = 0.5f; b.armL = 0.6f; b.armW = 0.08f; })
                .look(look(0x4A4636, 0x34322A, 0xC8BC98, 0xD8B040, STRIPES, 1f, 0xC84A2A, 0xE8C86A, 0xE0A020, 0xD8CDB2, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("majungasaurus", "Majungasaurus crenatissimus", THEROPOD, 6.5f).shortName("Majungasaurus").real(1100, 1.55f).speed(1.3f, 6f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Late Cretaceous, 70-66 million years ago").site(MAEVARANO)
                .body(b -> { b.skull = 0.6f; b.skullH = 0.58f; b.skullW = 0.45f; b.snout = 0.36f; b.hornNasal = 0.1f; b.hornNasalUp = 85; b.armL = 0.35f; b.armW = 0.08f;
                    b.neckD = 0.55f; b.legW = 0.42f; b.thighA = 14; b.shinA = -20; b.metaA = 16; b.shinF = 0.8f; b.metaF = 0.4f; })
                .look(look(0x8A6A52, 0x6A4E3A, 0xDCC8A8, 0x4A3426, SPOTS, 0.9f, 0xB04030, 0xD8C0A0, 0xE0A020, 0xC9B79C, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.ARID));

        // ---- spinosaurids
        l.add(sp("baryonyx", "Baryonyx walkeri", SPINOSAUR, 8.5f).shortName("Baryonyx").real(1500, 1.9f).speed(1.3f, 7f)
                .temper(HUNTER, CLAW, PISCIVORE).group(1, 1, 2).age("Early Cretaceous, 130-125 million years ago").site(SURREY, ISLE_OF_WIGHT)
                .aquatic().body(b -> { b.skull = 0.95f; b.tailW = 0.6f; b.crest = Body.Crest.RIDGE; b.crestH = 0.05f; b.crestL = 0.2f; b.clawL = 0.31f; b.hip = 1.9f;
                    b.sailH = 0.12f; b.sailFrom = 0.2f; b.sailTo = 0.9f; b.armL = 1.2f; b.armW = 0.14f; })
                .look(look(0x6A7058, 0x4E5442, 0xD0CCB0, 0x3A3E30, BANDS, 1.2f, 0x9A5A36, 0xCFC2A0, 0xE0B030, 0xD8CDB2, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("suchomimus", "Suchomimus tenerensis", SPINOSAUR, 10f).shortName("Suchomimus").real(3000, 2.4f).speed(1.3f, 6f)
                .temper(HUNTER, CLAW, PISCIVORE).group(1, 1, 2).age("Early Cretaceous, 125-112 million years ago").site(ELRHAZ)
                .aquatic().body(b -> { b.skull = 1.2f; b.tailW = 0.55f; b.sailH = 0.4f; b.sailFrom = 0.15f; b.sailTo = 1f; b.sailOnTail = 0.6f; b.clawL = 0.3f;
                    b.armL = 1.4f; b.armW = 0.16f; b.crest = Body.Crest.RIDGE; b.crestH = 0.05f; b.crestL = 0.3f; })
                .look(look(0x9A8060, 0x725C42, 0xE2D4B8, 0x4E3E2C, STRIPES, 1.3f, 0xB0603A, 0xE8C890, 0xE0B030, 0xD8CDB2, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.ARID));

        // ---- dromaeosaurs and other small theropods
        l.add(sp("utahraptor", "Utahraptor ostrommaysi", RAPTOR, 6.5f).shortName("Utahraptor").real(400, 1.45f).speed(1.5f, 9f)
                .temper(PACK, KICK, CARNIVORE).group(1, 3, 2).age("Early Cretaceous, 139-134 million years ago").site(CEDAR_MOUNTAIN)
                .body(b -> { b.sickle = 0.22f; b.skullH = 0.4f; b.depth = 0.85f; b.width = 0.6f; b.legW = 0.4f; })
                .look(look(0x7A6450, 0x54443A, 0xE0D4C0, 0x2E261E, BARS, 1.4f, 0xB04A2E, 0xF0E6D2, 0xE0A520, 0x2B2420, 0x1E1A16)).voice(RAPTOR_CALL)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("deinonychus", "Deinonychus antirrhopus", RAPTOR, 3.4f).shortName("Deinonychus").real(73, 0.85f).speed(1.5f, 10f)
                .temper(PACK, KICK, CARNIVORE).group(2, 5, 3).age("Early Cretaceous, 115-108 million years ago").site(CLOVERLY, ANTLERS)
                .body(b -> { b.sickle = 0.12f; })
                .look(look(0x5A5048, 0x3E3632, 0xD8CEC0, 0x1E1A16, BARS, 1.1f, 0x3E5A7A, 0xE8E2D8, 0xE0A520, 0x2B2420, 0x1E1A16)).voice(RAPTOR_CALL));
        l.add(sp("microraptor", "Microraptor zhaoianus", RAPTOR, 0.8f).shortName("Microraptor").real(1, 0.16f).speed(1.2f, 8f)
                .temper(SKULKER, BITE, CARNIVORE).group(1, 3, 2).age("Early Cretaceous, 120 million years ago").site(LIAONING)
                .note("Fossils keep the colour of its feathers: glossy black with a faint blue sheen, like a crow.")
                .body(b -> { b.legWings = true; b.armL = 0.3f * 0.8f; })
                .look(look(0x1E2228, 0x14181E, 0x2A3038, 0x2A3A58, BARS, 0.6f, 0x1E2A44, 0x34507A, 0xE0C040, 0x2B2420, 0x14120E)).voice(CHIRP)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("coelophysis", "Coelophysis bauri", SMALL_THEROPOD, 3f).shortName("Coelophysis").real(20, 0.7f).speed(1.5f, 9f)
                .temper(PACK, BITE, CARNIVORE).group(3, 8, 3).age("Late Triassic, 216-203 million years ago").site(GHOST_RANCH)
                .body(b -> { b.skull = 0.27f; b.skullW = 0.28f; b.skullH = 0.36f; b.snout = 0.55f; b.neck = 0.45f; b.neckD = 0.09f; })
                .look(look(0xA8885A, 0x7A603E, 0xE8DCC0, 0x4E3A26, STRIPES, 0.8f, 0xC0602E, 0xE8D8B8, 0xE0B030, 0xD8CDB2, 0x2B2420)).voice(GROWL)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));

        // ---- ornithomimids and oviraptorids
        l.add(sp("struthiomimus", "Struthiomimus altus", ORNITHOMIMID, 4.3f).shortName("Struthiomimus").real(150, 1.4f).speed(1.8f, 12f)
                .temper(SKITTISH, KICK, OMNIVORE).group(3, 10, 3).age("Late Cretaceous, 76-75 million years ago").site(DINOSAUR_PARK)
                .look(look(0x8C7E6A, 0x6A5E4E, 0xF0EAE0, 0x3E362E, PLAIN, 1f, 0x2E2A26, 0xF4F0EA, 0xD08A20, 0x6A5E4E, 0x2B2420)).voice(BIRD));
        l.add(sp("citipati", "Citipati osmolskae", OVIRAPTOR, 2.9f).shortName("Citipati").real(80, 0.9f).speed(1.4f, 8f)
                .temper(SKITTISH, KICK, OMNIVORE).group(1, 3, 2).age("Late Cretaceous, 75-71 million years ago").site(DJADOCHTA)
                .body(b -> { b.crest = Body.Crest.ROUND; b.crestH = 0.17f; b.crestL = 0.16f; })
                .look(look(0x6A5244, 0x4A3A30, 0xE2D6C6, 0xF0E6DA, BARS, 1f, 0xC8402E, 0x3A6A9A, 0xE0B030, 0x2E2A26, 0x2B2420)).voice(BIRD)
                .climates(DinoSpecies.ARID));

        // ---- sauropods and their relatives
        l.add(sp("apatosaurus", "Apatosaurus ajax", DIPLODOCID, 22f).shortName("Apatosaurus").real(20000, 3.6f).shoulder(3.05f).speed(1.5f, 3.5f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(2, 5, 3).age("Late Jurassic, 152-151 million years ago").site(MORRISON)
                .body(b -> { b.skull = 0.6f; b.neckD = 1.25f; b.neckUp = 14; b.legW = 1.2f; b.frontW = 1.0f; b.depth = 3.2f; b.width = 2.5f; b.torso = 4.2f; })
                .look(look(0x7E7468, 0x5E564C, 0xD2CAB8, 0x4A443C, SADDLES, 1.6f, 0x7A5040, 0xA89A86, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("brontosaurus", "Brontosaurus excelsus", DIPLODOCID, 21f).shortName("Brontosaurus").real(15000, 3.4f).shoulder(2.9f).speed(1.5f, 3.5f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(2, 5, 3).age("Late Jurassic, 155-152 million years ago").site(MORRISON)
                .body(b -> { b.skull = 0.6f; b.neckD = 1.1f; b.neckUp = 16; b.legW = 1.05f; b.frontW = 0.9f; b.depth = 2.9f; b.width = 2.2f; })
                .look(look(0x8A7E66, 0x665C4A, 0xDCD2BA, 0x544A3A, BANDS, 1.4f, 0x8A5A40, 0xB0A28A, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("camarasaurus", "Camarasaurus lentus", BRACHIOSAUR, 15f).shortName("Camarasaurus").real(15000, 3.0f).shoulder(3.25f).speed(1.4f, 3.5f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(3, 8, 4).age("Late Jurassic, 155-145 million years ago").site(MORRISON)
                .body(b -> { b.skull = 0.75f; b.skullH = 0.65f; b.snout = 0.35f; b.snoutH = 0.85f; b.neck = 0.33f * 15f; b.neckUp = 38; b.tail = 0.33f * 15f; })
                .look(look(0x8C8270, 0x6A6252, 0xD8D0BE, 0x5A5242, MOTTLED, 1.4f, 0x9A6A45, 0xB0A48E, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("giraffatitan", "Giraffatitan brancai", BRACHIOSAUR, 22f).shortName("Giraffatitan").real(30000, 4.0f).shoulder(4.8f).speed(1.4f, 3.5f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(1, 3, 2).age("Late Jurassic, 150-145 million years ago").site(TENDAGURU)
                .body(b -> { b.skull = 0.9f; b.crest = Body.Crest.ROUND; b.crestH = 0.32f; b.crestL = 0.32f; })
                .look(look(0x8E8A78, 0x6E6A5A, 0xDAD6C6, 0x5E5A4A, MOTTLED, 1.5f, 0xA0703E, 0xB8B4A0, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("mamenchisaurus", "Mamenchisaurus hochuanensis and M. sinocanadorum", DIPLODOCID, 22f).shortName("Mamenchisaurus").real(15000, 3.3f).shoulder(3.0f).speed(1.4f, 3.5f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(2, 4, 2).age("Late Jurassic, 160-145 million years ago").site(SHAXIMIAO, SHISHUGOU)
                .body(b -> { b.skull = 0.6f; b.neck = 0.44f * 22f; b.neckN = 6; b.neckUp = 28; b.neckS = 0; b.tail = 0.3f * 22f; b.tailTaper = 0.12f; b.tailPow = 1.4f; })
                .look(look(0x6E7A68, 0x52604E, 0xD0D4C0, 0x46523E, BANDS, 1.4f, 0x8A6A40, 0x9EAA92, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.TROPICAL | DinoSpecies.ARID));
        l.add(sp("amargasaurus", "Amargasaurus cazaui", DIPLODOCID, 9.5f).shortName("Amargasaurus").real(2600, 2.0f).shoulder(1.75f).speed(1.4f, 3.5f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(2, 4, 2).age("Early Cretaceous, 129-122 million years ago").site(LA_AMARGA)
                .body(b -> { b.skull = 0.42f; b.neck = 0.24f * 9.5f; b.neckN = 4; b.neckUp = 22; b.neckSpines = 0.6f; b.pairedSpines = true; b.depth = 1.3f; b.width = 1.0f; })
                .look(look(0xA08A6C, 0x7A684E, 0xE4D8C2, 0x5E4E3A, BANDS, 1.2f, 0x8A4E36, 0xD8C0A0, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("patagotitan", "Patagotitan mayorum", TITANOSAUR, 33f).shortName("Patagotitan").real(57000, 4.8f).shoulder(5.0f).speed(1.5f, 2.3f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(1, 3, 2).age("Early Cretaceous, about 102 million years ago").site(CERRO_BARCINO)
                .body(b -> { b.skull = 1.0f; b.legW = 1.5f; b.frontW = 1.3f; b.width = 3.6f; b.depth = 4.0f; b.torso = 6.6f; b.neckD = 1.5f; })
                .look(look(0x8E8472, 0x6C6454, 0xD8D0BE, 0x5C5444, MOTTLED, 1.8f, 0x8A7A62, 0xB0A690, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD));
        l.add(sp("plateosaurus", "Plateosaurus trossingensis", PROSAUROPOD, 7.5f).shortName("Plateosaurus").real(700, 1.8f).speed(1.4f, 5f)
                .temper(SKITTISH, CLAW, HERBIVORE).group(3, 8, 3).age("Late Triassic, 214-204 million years ago").site(TROSSINGEN, HALBERSTADT)
                .body(b -> { b.clawL = 0.12f; })
                .look(look(0x8E7A52, 0x6A5A3C, 0xE0D4B4, 0x4A3E2A, BANDS, 1.1f, 0xA0603A, 0xD8C8A0, 0xD0A030, 0xD8CDB2, 0x2B2420)).voice(BELLOW)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM | DinoSpecies.TEMPERATE));

        // ---- ornithopods
        l.add(sp("corythosaurus", "Corythosaurus casuarius", HADROSAUR, 9f).shortName("Corythosaurus").real(3000, 2.45f).shoulder(1.75f).speed(1.5f, 9f)
                .temper(SKITTISH, KICK, HERBIVORE).group(4, 10, 4).age("Late Cretaceous, 77-75.7 million years ago").site(DINOSAUR_PARK)
                .body(b -> { b.skull = 0.75f; b.crest = Body.Crest.HELMET; b.crestH = 0.32f; b.crestL = 0.5f; })
                .look(look(0x6E7A5A, 0x525E42, 0xDCD8BC, 0x3A422C, STRIPES, 1.2f, 0x3E6A9A, 0xE8D8A0, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(HORN));
        l.add(sp("lambeosaurus", "Lambeosaurus lambei", HADROSAUR, 7.5f).shortName("Lambeosaurus").real(3000, 2.05f).shoulder(1.46f).speed(1.5f, 9f)
                .temper(SKITTISH, KICK, HERBIVORE).group(4, 10, 4).age("Late Cretaceous, 76-75 million years ago").site(DINOSAUR_PARK)
                .body(b -> { b.skull = 0.75f; b.crest = Body.Crest.HATCHET; b.crestH = 0.32f; b.crestL = 0.42f; })
                .look(look(0x8A7A5A, 0x6A5C42, 0xE2D8BC, 0x4A3E2A, SADDLES, 1.2f, 0xC8762E, 0xF0DFA0, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(HORN));
        l.add(sp("maiasaura", "Maiasaura peeblesorum", HADROSAUR, 9f).shortName("Maiasaura").real(3000, 2.4f).shoulder(1.75f).speed(1.5f, 8f)
                .temper(SKITTISH, KICK, HERBIVORE).group(6, 14, 5).age("Late Cretaceous, 76.7 million years ago").site(TWO_MEDICINE)
                .note("\"Good mother lizard\": found in nesting colonies with eggs, hatchlings and young.")
                .body(b -> { b.skull = 0.85f; b.crest = Body.Crest.COMB; b.crestH = 0.1f; b.crestL = 0.3f; })
                .look(look(0x7A6E58, 0x5A5042, 0xDCD2BE, 0x46403A, MOTTLED, 1f, 0x8A5A3A, 0xD8C8A8, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(BELLOW));
        l.add(sp("ouranosaurus", "Ouranosaurus nigeriensis", IGUANODONT, 8f).shortName("Ouranosaurus").real(2200, 2.2f).shoulder(1.65f).speed(1.4f, 6.5f)
                .temper(SKITTISH, CLAW, HERBIVORE).group(3, 8, 3).age("Early Cretaceous, 125-112 million years ago").site(ELRHAZ)
                .body(b -> { b.skull = 0.67f; b.snoutW = 0.85f; b.snoutH = 0.55f; b.sailH = 0.63f; b.sailFrom = 0.05f; b.sailTo = 1f; b.sailOnTail = 0.6f; b.thumbSpike = 0.1f; })
                .look(look(0xB09670, 0x8A7250, 0xEADEC6, 0x5E4A32, BANDS, 1.1f, 0xC86A3A, 0xF0D8A0, 0x2A2A1A, 0xDCD2B5, 0x3A3530)).voice(BELLOW)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.ARID));
        l.add(sp("hypsilophodon", "Hypsilophodon foxii", SMALL_ORNITHOPOD, 1.8f).shortName("Hypsilophodon").real(20, 0.45f).speed(1.5f, 9f)
                .temper(SKITTISH, KICK, HERBIVORE).group(4, 10, 4).age("Early Cretaceous, 126 million years ago").site(ISLE_OF_WIGHT)
                .look(look(0x7E8A5A, 0x5E6A40, 0xE2E0C2, 0x3E4428, STRIPES, 0.8f, 0x9A8A5A, 0xE2DAB0, 0xD0A030, 0xBDB19A, 0x3A3530)).voice(SMALL_HERB)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("tenontosaurus", "Tenontosaurus tilletti", IGUANODONT, 7f).shortName("Tenontosaurus").real(1000, 1.75f).shoulder(1.3f).speed(1.4f, 6f)
                .temper(SKITTISH, KICK, HERBIVORE).group(2, 5, 3).age("Early Cretaceous, 115-108 million years ago").site(CLOVERLY, ANTLERS, WAYAN)
                .body(b -> { b.skull = 0.55f; b.thumbSpike = 0; b.tail = 0.52f * 7f; b.tailD = 0.55f; })
                .look(look(0x8A7E5E, 0x6A5E44, 0xE0D8BE, 0x4E4430, BANDS, 1f, 0x9A7A4E, 0xD8CCA8, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(BELLOW));

        // ---- ceratopsians
        l.add(sp("centrosaurus", "Centrosaurus apertus", CERATOPSIAN, 5.5f).shortName("Centrosaurus").real(2300, 1.45f).shoulder(1.15f).speed(1.3f, 6f)
                .temper(DEFENDER, GORE, HERBIVORE).group(4, 12, 4).age("Late Cretaceous, 76.5-75.5 million years ago").site(DINOSAUR_PARK)
                .body(b -> { b.skull = 0.62f; b.hornNasal = 0.42f; b.hornNasalUp = 52; b.frillH = 0.6f; b.frillW = 0.9f; b.frillUp = 62; b.frillSpikes = 2; b.frillSpikeL = 0.22f;
                    b.hornBrow = 0.05f; b.torso = 1.9f; b.depth = 1.25f; b.width = 1.05f; })
                .look(look(0x7A6650, 0x5A4A3A, 0xD8C8AA, 0x3E3226, SADDLES, 1f, 0xB04A2E, 0xEAD2A0, 0x2E1E0E, 0xE3D6B8, 0x2E2A25)).voice(CERATOPS));
        l.add(sp("chasmosaurus", "Chasmosaurus belli", CERATOPSIAN, 4.8f).shortName("Chasmosaurus").real(1800, 1.3f).shoulder(1.05f).speed(1.3f, 6f)
                .temper(DEFENDER, GORE, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, 76.5-75.5 million years ago").site(DINOSAUR_PARK)
                .body(b -> { b.skull = 0.55f; b.frillH = 1.0f; b.frillW = 0.9f; b.frillUp = 40; b.hornBrow = 0.12f; b.hornBrowUp = 50; b.hornNasal = 0.08f;
                    b.torso = 1.7f; b.depth = 1.1f; b.width = 0.95f; })
                .look(look(0x8A7454, 0x68563E, 0xDCCCAA, 0x4A3C2A, SPOTS, 1f, 0xA8522E, 0xE6CE90, 0x2E1E0E, 0xE3D6B8, 0x2E2A25)).voice(CERATOPS));
        l.add(sp("pachyrhinosaurus", "Pachyrhinosaurus canadensis, P. lakustai and P. perotorum", CERATOPSIAN, 7f).shortName("Pachyrhinosaurus").real(3500, 1.8f).shoulder(1.45f).speed(1.3f, 6f)
                .temper(DEFENDER, GORE, HERBIVORE).group(4, 12, 4).age("Late Cretaceous, 73.5-69 million years ago").site(HORSESHOE_CANYON, PIPESTONE, PRINCE_CREEK)
                .body(b -> { b.skull = 0.8f; b.nasalBoss = 0.2f; b.frillH = 0.7f; b.frillW = 1.05f; b.frillUp = 60; b.frillSpikes = 2; b.frillSpikeL = 0.38f; })
                .look(look(0x6E655A, 0x524A42, 0xD2CABA, 0x3E3832, MOTTLED, 1f, 0x8A4E36, 0xD8C8A8, 0x2E1E0E, 0xD8CCB0, 0x2E2A25)).voice(CERATOPS)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.COLD));
        l.add(sp("kosmoceratops", "Kosmoceratops richardsoni", CERATOPSIAN, 4.5f).shortName("Kosmoceratops").real(1200, 1.25f).shoulder(1.0f).speed(1.3f, 6f)
                .temper(DEFENDER, GORE, HERBIVORE).group(2, 6, 3).age("Late Cretaceous, 76 million years ago").site(KAIPAROWITS)
                .note("Fifteen horns on its head: the most of any known dinosaur.")
                .body(b -> { b.skull = 0.55f; b.frillH = 0.5f; b.frillW = 0.8f; b.frillUp = 68; b.frillSpikes = 10; b.frillSpikeL = 0.15f; b.hornBrow = 0.25f; b.hornBrowUp = 15; b.hornBrowOut = 65;
                    b.hornNasal = 0.1f; b.hornNasalUp = 70; })
                .look(look(0x7E6A52, 0x5E4E3C, 0xDCCCAE, 0x3E3226, BANDS, 1f, 0xC0502E, 0xF0D898, 0x2E1E0E, 0xE3D6B8, 0x2E2A25)).voice(CERATOPS)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("psittacosaurus", "Psittacosaurus lujiatunensis", SMALL_ORNITHOPOD, 1.8f).shortName("Psittacosaurus").real(20, 0.45f).speed(1.3f, 7f)
                .temper(SKITTISH, BITE, HERBIVORE).group(3, 10, 4).age("Early Cretaceous, 125-120 million years ago").site(LIAONING)
                .note("A Psittacosaurus fossil from these beds keeps its colours: dark above and pale below, camouflage for a shady forest floor.")
                .body(b -> { b.skull = 0.2f; b.skullH = 0.8f; b.snout = 0.3f; b.snoutW = 0.7f; b.snoutH = 0.85f; b.hornCheek = 0.04f; b.sailOnTail = 0.12f; b.armL = 0.22f; })
                .look(look(0x6E5A44, 0x4A3C2E, 0xE2D6C0, 0x3A2E22, MOTTLED, 0.9f, 0x5A4636, 0x8A7458, 0x2A1A0A, 0x4A4038, 0x3A3530)).voice(SMALL_HERB)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM | DinoSpecies.COLD));

        // ---- thyreophorans and a dome-head
        l.add(sp("kentrosaurus", "Kentrosaurus aethiopicus", STEGOSAUR, 4.5f).shortName("Kentrosaurus").real(1000, 1.3f).shoulder(0.85f).speed(1.1f, 2.5f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(2, 6, 3).age("Late Jurassic, 152 million years ago").site(TENDAGURU)
                .body(b -> { b.plates = 9; b.plateH = 0.28f; b.tailSpikes = 4; b.tailSpikeL = 0.6f; b.shoulderSpikeL = 0.65f; b.skull = 0.3f; })
                .look(look(0x8C7A52, 0x6A5A3A, 0xDCD0AE, 0x4E4228, BANDS, 1f, 0x9A4A2A, 0x6A5A3A, 0x2A1A0A, 0xD9CDB1, 0x2E2A25)).voice(STEGO)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.WARM));
        l.add(sp("euoplocephalus", "Euoplocephalus tutus", ANKYLOSAUR, 5.5f).shortName("Euoplocephalus").real(2300, 1.15f).shoulder(0.98f).speed(1.0f, 2.7f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(1, 2, 2).age("Late Cretaceous, 76.5-75.5 million years ago").site(DINOSAUR_PARK)
                .body(b -> { b.skull = 0.5f; b.clubW = 0.45f; b.hornBrow = 0.09f; b.hornBrowUp = 0; b.hornBrowOut = 60; b.hornBrowZ = 0; })
                .look(look(0x6E6450, 0x544A3A, 0xCEC4A8, 0x3A3226, SPOTS, 0.9f, 0x7A6A50, 0x4A4030, 0x2E1E0E, 0xD7C8A8, 0x2E2A25)).voice(ANKY));
        l.add(sp("borealopelta", "Borealopelta markmitchelli", NODOSAUR, 5.5f).shortName("Borealopelta").real(1300, 1.15f).shoulder(1.0f).speed(1.0f, 2.7f)
                .temper(DEFENDER, GORE, HERBIVORE).group(1, 1, 1).age("Early Cretaceous, 112-110 million years ago").site(CLEARWATER)
                .note("Its fossil is a mummy, skin and all. Chemical traces in it suggest it was reddish brown, paler underneath, though not everyone agrees.")
                .body(b -> { b.shoulderSpikeL = 0.5f; })
                .look(look(0x8A4E36, 0x6E3A28, 0xC8A08A, 0x5A3022, PLAIN, 1f, 0xB07A5A, 0x6E3A28, 0x2E1E0E, 0xD0B898, 0x2E2A25)).voice(ANKY)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.COLD));
        l.add(sp("stegoceras", "Stegoceras validum", PACHYCEPHALOSAUR, 2.2f).shortName("Stegoceras").real(40, 0.55f).speed(1.4f, 7f)
                .temper(DEFENDER, HEADBUTT, HERBIVORE).group(2, 6, 3).age("Late Cretaceous, 77-74 million years ago").site(DINOSAUR_PARK)
                .body(b -> { b.skull = 0.22f; b.dome = 0.08f; })
                .look(look(0x8A7A5E, 0x6A5C44, 0xE0D6BE, 0x4A4030, STRIPES, 0.9f, 0xD8CCB0, 0x7A3A2A, 0xD0A030, 0xCFC2A6, 0x3A3530)).voice(GOAT));

        // ================================================================ batch 3 (0.7.2)

        // ---- more theropods
        l.add(sp("megalosaurus", "Megalosaurus bucklandii", THEROPOD, 6.5f).shortName("Megalosaurus").real(900, 1.7f).speed(1.5f, 7f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Middle Jurassic, 166 million years ago").site(OXFORDSHIRE)
                .note("The first dinosaur ever named, in 1824.")
                .body(b -> { b.skull = 0.75f; b.skullW = 0.3f; b.armL = 0.9f; b.armW = 0.1f; b.clawL = 0.1f; })
                .look(look(0x8A5E44, 0x664432, 0xDCC6A8, 0x46301F, BANDS, 1.1f, 0xA8442E, 0xD8C0A0, 0xE0A020, 0xD8CDB2, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("torvosaurus", "Torvosaurus tanneri and T. gurneyi", THEROPOD, 9.5f).shortName("Torvosaurus").real(2500, 2.4f).speed(1.4f, 7f)
                .temper(APEX, BITE, CARNIVORE).group(1, 1, 1).age("Late Jurassic, 153-148 million years ago").site(MORRISON, LOURINHA)
                .body(b -> { b.skull = 1.15f; b.skullW = 0.3f; b.skullH = 0.45f; b.armL = 0.9f; b.armW = 0.14f; b.clawL = 0.15f; b.legW = 0.6f; })
                .look(look(0x6E5844, 0x4E3E30, 0xD6C6AA, 0x2E241A, STRIPES, 1.3f, 0x9A3A2A, 0xD8C8A8, 0xE0A020, 0xD8CDB2, 0x2B2420)).voice(BIG_ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("mapusaurus", "Mapusaurus roseae", THEROPOD, 11.5f).shortName("Mapusaurus").real(4000, 2.9f).speed(1.5f, 6f)
                .temper(APEX, BITE, CARNIVORE).group(1, 3, 1).age("Late Cretaceous, 97-93 million years ago").site(NEUQUEN)
                .note("A bonebed of at least seven, young and old, may mean they lived in groups.")
                .body(b -> { b.skull = 1.3f; b.skullW = 0.3f; b.skullH = 0.42f; b.snout = 0.5f; b.armL = 1.3f; b.armW = 0.14f; b.clawL = 0.16f; b.legW = 0.7f; })
                .look(look(0x8C7A66, 0x665848, 0xDCD2C0, 0x463A30, SADDLES, 1.2f, 0x9A4A3A, 0xCFC5B0, 0xD9A02A, 0xD8CDB2, 0x2B2420)).voice(BIG_ROAR));
        l.add(sp("concavenator", "Concavenator corcovatus", THEROPOD, 6f).shortName("Concavenator").real(400, 1.6f).speed(1.5f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 1).age("Early Cretaceous, 125 million years ago").site(LAS_HOYAS)
                .note("Two tall vertebrae made a hump just in front of its hips.")
                .body(b -> { b.skull = 0.6f; b.skullW = 0.3f; b.hump = 0.4f; b.armL = 0.85f; b.armW = 0.08f; b.clawL = 0.08f; })
                .look(look(0x7A6A4E, 0x5A4C38, 0xDED2B6, 0x3E3424, BANDS, 1f, 0xB8582E, 0xE8C890, 0xE0B030, 0xD8CDB2, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.TROPICAL));
        l.add(sp("australovenator", "Australovenator wintonensis", THEROPOD, 6f).shortName("Australovenator").real(500, 1.6f).speed(1.6f, 9f)
                .temper(HUNTER, CLAW, CARNIVORE).group(1, 1, 1).age("Late Cretaceous, 95 million years ago").site(WINTON)
                .body(b -> { b.skull = 0.55f; b.skullW = 0.28f; b.armL = 1.2f; b.armW = 0.09f; b.clawL = 0.2f; b.shinF = 1.1f; b.metaF = 0.6f; })
                .look(look(0xA8744C, 0x7E5436, 0xE6D2B4, 0x5A3A24, SPOTS, 0.9f, 0xC0603A, 0xE8C8A0, 0xE0B030, 0xD8CDB2, 0x2B2420)).voice(GROWL)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE | DinoSpecies.TROPICAL));
        l.add(sp("herrerasaurus", "Herrerasaurus ischigualastensis", SMALL_THEROPOD, 4.5f).shortName("Herrerasaurus").real(260, 1.15f).speed(1.5f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Late Triassic, 231 million years ago").site(ISCHIGUALASTO)
                .note("One of the earliest dinosaurs.")
                .body(b -> { b.skull = 0.56f; b.skullH = 0.42f; b.armL = 0.75f; b.armW = 0.08f; b.clawL = 0.07f; })
                .look(look(0x9A5E3E, 0x704430, 0xE0C8A8, 0x4A2C1E, SPOTS, 0.9f, 0xB04A2E, 0xE0C8A8, 0xE0A020, 0xD8CDB2, 0x2B2420)).voice(GROWL)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("stenonychosaurus", "Stenonychosaurus inequalis", RAPTOR, 2.4f).shortName("Stenonychosaurus").aka("troodon").real(50, 0.7f).speed(1.4f, 9f)
                .temper(SKULKER, BITE, CARNIVORE).group(1, 3, 2).age("Late Cretaceous, 77-75 million years ago").site(DINOSAUR_PARK, TWO_MEDICINE)
                .note("Long known as Troodon. Big forward-facing eyes; it may have hunted at dusk.")
                .body(b -> { b.eyeK = 1.7f; b.sickle = 0.05f; b.armL = 0.2f * 2.4f; b.skullH = 0.42f; })
                .look(look(0x8A7E62, 0x645A44, 0xEAE2D0, 0x3E3628, SPOTS, 0.8f, 0x5A4E3A, 0xEAE2D0, 0xF0C030, 0x2B2420, 0x1E1A16)).voice(RAPTOR_CALL)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.COLD));
        l.add(sp("sinosauropteryx", "Sinosauropteryx prima", SMALL_THEROPOD, 1.07f).shortName("Sinosauropteryx").real(0.55f, 0.15f).speed(1.0f, 6f)
                .temper(SKULKER, BITE, CARNIVORE).group(1, 3, 2).age("Early Cretaceous, 125 million years ago").site(LIAONING)
                .note("The first dinosaur other than a bird found with feathers. Its fossils show the pattern: dark back, pale belly, a banded tail and a dark mask.")
                .body(b -> { b.feathers = true; b.tail = 0.6f * 1.07f; b.armL = 0.1f * 1.07f; })
                .look(look(0x8A6040, 0x5E3E26, 0xF0E6D8, 0xF4EEE6, TAIL_BANDS, 0.7f, 0x5E3E26, 0xF4EEE6, 0xE0B030, 0xC9BBA0, 0x2B2420)).voice(CHIRP)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));

        // ---- more sauropods
        l.add(sp("shunosaurus", "Shunosaurus lii", DIPLODOCID, 9.5f).shortName("Shunosaurus").real(3000, 1.9f).shoulder(1.6f).speed(1.3f, 3f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(2, 6, 3).age("Middle Jurassic, about 165 million years ago").site(SHAXIMIAO)
                .note("A sauropod with a spiked club on the end of its tail.")
                .body(b -> { b.skull = 0.45f; b.neck = 0.27f * 9.5f; b.neckN = 4; b.neckUp = 12; b.tail = 0.42f * 9.5f; b.tailTaper = 0.25f; b.tailPow = 1f;
                    b.clubW = 0.35f; b.tailSpikes = 2; b.tailSpikeL = 0.15f; b.depth = 1.3f; b.width = 1.05f; })
                .look(look(0x7E7A62, 0x5E5A48, 0xD6D2BC, 0x4A4636, BANDS, 1.1f, 0x7A6040, 0xA89E86, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.TROPICAL));
        l.add(sp("saltasaurus", "Saltasaurus loricatus", TITANOSAUR, 8.5f).shortName("Saltasaurus").real(2500, 1.8f).shoulder(1.7f).speed(1.4f, 3f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(3, 6, 3).age("Late Cretaceous, 70-66 million years ago").site(LECHO)
                .note("An armoured sauropod: its back was studded with bony plates.")
                .body(b -> { b.skull = 0.4f; b.armour = 0.12f; b.armourCols = 3; b.neck = 0.3f * 8.5f; b.legW = 0.47f; b.frontW = 0.41f; })
                .look(look(0x8E7E64, 0x6A5C48, 0xDACEB8, 0x544636, MOTTLED, 1.2f, 0x8A7A62, 0x6A5A46, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("nigersaurus", "Nigersaurus taqueti", DIPLODOCID, 9f).shortName("Nigersaurus").real(3000, 1.8f).shoulder(1.5f).speed(1.3f, 3f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(3, 8, 3).age("Early Cretaceous, 115-105 million years ago").site(ELRHAZ)
                .note("Its wide, straight-edged mouth held hundreds of teeth for cropping low plants.")
                .body(b -> { b.skull = 0.42f; b.snout = 0.5f; b.snoutW = 1.25f; b.snoutH = 0.6f; b.headDown = 50; b.neck = 0.22f * 9f; b.neckN = 3; b.neckUp = 4; b.neckS = -12; })
                .look(look(0x9A866A, 0x76664E, 0xE2D6C0, 0x5E4E3A, BANDS, 1.1f, 0x8A6A4A, 0xC8B89A, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.ARID));
        l.add(sp("alamosaurus", "Alamosaurus sanjuanensis", TITANOSAUR, 28f).shortName("Alamosaurus").real(32000, 4.2f).shoulder(4.3f).speed(1.5f, 2.5f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(2, 5, 2).age("Late Cretaceous, 70-66 million years ago").site(SAN_JUAN, JAVELINA, NORTH_HORN)
                .note("The last of the giant sauropods in North America, a neighbour of Tyrannosaurus.")
                .body(b -> { b.skull = 0.85f; b.legW = 1.4f; b.frontW = 1.2f; b.width = 3.2f; b.depth = 3.6f; b.torso = 5.6f; b.neckD = 1.3f; })
                .look(look(0x9A8C76, 0x786A58, 0xDED4C2, 0x645848, MOTTLED, 1.6f, 0x8A7A62, 0xB0A690, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("europasaurus", "Europasaurus holgeri", BRACHIOSAUR, 6.2f).shortName("Europasaurus").real(800, 1.35f).shoulder(1.6f).speed(1.2f, 3f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(3, 8, 3).age("Late Jurassic, 154 million years ago").site(LANGENBERG)
                .note("An island dwarf: a relative of Brachiosaurus only about six metres long.")
                .body(b -> { b.skull = 0.32f; b.crest = Body.Crest.ROUND; b.crestH = 0.06f; b.crestL = 0.12f; })
                .look(look(0x8A8270, 0x6A6252, 0xD8D2C0, 0x5A5244, MOTTLED, 1.2f, 0x9A6A45, 0xB0A690, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("massospondylus", "Massospondylus carinatus", PROSAUROPOD, 5f).shortName("Massospondylus").real(300, 1.25f).speed(1.3f, 4f)
                .temper(SKITTISH, CLAW, HERBIVORE).group(2, 6, 3).age("Early Jurassic, 200-183 million years ago").site(ELLIOT)
                .note("Its nests, with eggs and hatchlings, are among the oldest known.")
                .body(b -> { b.clawL = 0.08f; })
                .look(look(0x8A7448, 0x6A5636, 0xDED0AE, 0x4A3A24, STRIPES, 0.9f, 0xA0703A, 0xD8C8A0, 0xD0A030, 0xD8CDB2, 0x2B2420)).voice(BELLOW)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));

        // ---- more ornithopods
        l.add(sp("saurolophus", "Saurolophus angustirostris and S. osborni", HADROSAUR, 12f).shortName("Saurolophus").real(8000, 3.0f).shoulder(2.2f).speed(1.5f, 8f)
                .temper(SKITTISH, KICK, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, 70 million years ago").site(NEMEGT, HORSESHOE_CANYON)
                .body(b -> { b.skull = 1.2f; b.crest = Body.Crest.SPIKE; b.crestH = 0.12f; b.crestL = 0.45f; })
                .look(look(0x8A7E62, 0x6A604A, 0xE0D8C2, 0x4A4234, STRIPES, 1.2f, 0xB0503A, 0xE0D0A8, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(BELLOW)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("shantungosaurus", "Shantungosaurus giganteus", HADROSAUR, 15f).shortName("Shantungosaurus").real(13000, 3.7f).shoulder(2.7f).speed(1.6f, 7f)
                .temper(SKITTISH, KICK, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, 77-73.5 million years ago").site(WANGSHI)
                .note("The biggest duckbill: as long as a bus and as heavy as two elephants.")
                .body(b -> { b.skull = 1.6f; })
                .look(look(0x7A7E68, 0x5A5E4C, 0xDADCC8, 0x464A3A, MOTTLED, 1.4f, 0x8A6A4A, 0xC8C0A8, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(BELLOW));
        l.add(sp("brachylophosaurus", "Brachylophosaurus canadensis", HADROSAUR, 9.5f).shortName("Brachylophosaurus").real(5000, 2.6f).shoulder(1.9f).speed(1.5f, 8f)
                .temper(SKITTISH, KICK, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, 78-76 million years ago").site(JUDITH_RIVER, DINOSAUR_PARK)
                .note("\"Leonardo\", a mummified Brachylophosaurus, kept its skin, some muscle and probably its last meal.")
                .body(b -> { b.skull = 0.9f; b.crest = Body.Crest.SPIKE; b.crestH = 0.1f; b.crestL = 0.3f; })
                .look(look(0x6E7458, 0x525840, 0xD6D8BE, 0x3E4430, BANDS, 1.1f, 0x9A5A3A, 0xD0C8A8, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(BELLOW));
        l.add(sp("olorotitan", "Olorotitan arharensis", HADROSAUR, 8f).shortName("Olorotitan").real(3000, 2.3f).shoulder(1.7f).speed(1.5f, 8f)
                .temper(SKITTISH, KICK, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, 67 million years ago").site(UDURCHUKAN)
                .body(b -> { b.skull = 0.75f; b.crest = Body.Crest.HATCHET; b.crestH = 0.45f; b.crestL = 0.5f; })
                .look(look(0x7E6E58, 0x5E5242, 0xDCD0BC, 0x463C30, SADDLES, 1.1f, 0xC8402E, 0xF0C890, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(HORN)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.COLD));
        l.add(sp("gryposaurus", "Gryposaurus notabilis and G. monumentensis", HADROSAUR, 8f).shortName("Gryposaurus").real(2600, 2.3f).shoulder(1.7f).speed(1.5f, 8f)
                .temper(SKITTISH, KICK, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, 77-75 million years ago").site(DINOSAUR_PARK, KAIPAROWITS)
                .body(b -> { b.skull = 0.85f; b.nasalBoss = 0.15f; })
                .look(look(0x7A6E5A, 0x5A5042, 0xDCD2BE, 0x423A30, STRIPES, 1f, 0xA8503A, 0xD8C8A8, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(BELLOW));
        l.add(sp("muttaburrasaurus", "Muttaburrasaurus langdoni", IGUANODONT, 8f).shortName("Muttaburrasaurus").real(5000, 2.2f).shoulder(1.65f).speed(1.4f, 6f)
                .temper(SKITTISH, CLAW, HERBIVORE).group(2, 5, 2).age("Cretaceous, about 103-96 million years ago").site(MUTTABURRA)
                .body(b -> { b.skull = 0.8f; b.nasalBoss = 0.15f; b.thumbSpike = 0.12f; })
                .look(look(0x9A8668, 0x76664E, 0xE2D8C2, 0x5A4C3A, BANDS, 1f, 0xA86A3A, 0xE0CCA8, 0x2A2A1A, 0xDCD2B5, 0x3A3530)).voice(BELLOW)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID | DinoSpecies.TEMPERATE));
        l.add(sp("mantellisaurus", "Mantellisaurus atherfieldensis", IGUANODONT, 7f).shortName("Mantellisaurus").real(750, 1.9f).shoulder(1.4f).speed(1.4f, 7f)
                .temper(SKITTISH, CLAW, HERBIVORE).group(3, 8, 3).age("Early Cretaceous, 125 million years ago").site(ISLE_OF_WIGHT, WEALDEN)
                .body(b -> { b.skull = 0.6f; b.thumbSpike = 0.08f; })
                .look(look(0x7A8462, 0x5C6648, 0xDDDCC2, 0x444C34, STRIPES, 1f, 0x9A8A5A, 0xD8D0A8, 0x2A2A1A, 0xDCD2B5, 0x3A3530)).voice(BELLOW)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("thescelosaurus", "Thescelosaurus neglectus", SMALL_ORNITHOPOD, 3.5f).shortName("Thescelosaurus").real(270, 0.85f).speed(1.2f, 6f)
                .temper(SKITTISH, KICK, HERBIVORE).group(1, 3, 2).age("Late Cretaceous, 66 million years ago").site(HELL_CREEK)
                .body(b -> { b.depth = 0.18f * 3.5f; b.width = 0.12f * 3.5f; b.legW = 0.07f * 3.5f; })
                .look(look(0x7A7058, 0x5A5242, 0xDCD6C0, 0x3E3A2E, MOTTLED, 0.9f, 0x8A7A5A, 0xD0C8A8, 0xD0A030, 0xBDB19A, 0x3A3530)).voice(SMALL_HERB)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("oryctodromeus", "Oryctodromeus cubicularis", SMALL_ORNITHOPOD, 2.1f).shortName("Oryctodromeus").real(25, 0.5f).speed(1.3f, 8f)
                .temper(SKITTISH, KICK, HERBIVORE).group(2, 4, 3).age("Late Cretaceous, 95 million years ago").site(WAYAN, BLACKLEAF)
                .note("It dug burrows: the first one found, in Montana, held an adult and two young. More of its bones come from eastern Idaho, near where WorldBound's Earth begins.")
                .look(look(0x9A845E, 0x766444, 0xE6DAC0, 0x54442E, STRIPES, 0.8f, 0x8A6A44, 0xDCCCA8, 0xD0A030, 0xBDB19A, 0x3A3530)).voice(SMALL_HERB));
        l.add(sp("leaellynasaura", "Leaellynasaura amicagraphica", SMALL_ORNITHOPOD, 1.8f).shortName("Leaellynasaura").real(10, 0.33f).speed(1.2f, 7f)
                .temper(SKITTISH, KICK, HERBIVORE).group(3, 8, 3).age("Early Cretaceous, 113-110 million years ago").site(DINOSAUR_COVE)
                .note("It lived inside the Antarctic Circle of its day, through months of winter darkness. Its tail may have been three times as long as its body.")
                .body(b -> { b.tail = 0.7f * 1.8f; b.tailN = 6; b.tailTaper = 0.12f; })
                .look(look(0x6E6656, 0x504A3E, 0xDCD6C6, 0x34302A, BANDS, 0.7f, 0x7A6A54, 0xD0C8B8, 0xD0A030, 0xBDB19A, 0x3A3530)).voice(SMALL_HERB)
                .climates(DinoSpecies.COLD | DinoSpecies.TEMPERATE));
        l.add(sp("heterodontosaurus", "Heterodontosaurus tucki", SMALL_ORNITHOPOD, 1.2f).shortName("Heterodontosaurus").real(3.5f, 0.28f).speed(1.2f, 8f)
                .temper(SKITTISH, BITE, OMNIVORE).group(2, 6, 3).age("Early Jurassic, 200-190 million years ago").site(ELLIOT)
                .note("Named for its different teeth: cutting teeth, grinding teeth and tusks in both jaws.")
                .body(b -> { b.sailOnTail = 0.04f; b.armL = 0.15f * 1.2f; b.clawL = 0.02f; })
                .look(look(0x9A6A44, 0x704C30, 0xE6D2B6, 0x4A3020, STRIPES, 0.8f, 0x8A5A3A, 0xE0C8A8, 0xD0A030, 0xE8E0CC, 0x3A3530)).voice(SMALL_HERB)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));

        // ---- more horned dinosaurs
        l.add(sp("einiosaurus", "Einiosaurus procurvicornis", CERATOPSIAN, 4.5f).shortName("Einiosaurus").real(1300, 1.25f).shoulder(1.0f).speed(1.3f, 6f)
                .temper(DEFENDER, GORE, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, 74.5 million years ago").site(TWO_MEDICINE)
                .body(b -> { b.skull = 0.5f; b.hornNasal = 0.32f; b.hornNasalUp = -15; b.frillH = 0.5f; b.frillW = 0.8f; b.frillUp = 62; b.frillSpikes = 2; b.frillSpikeL = 0.45f; })
                .look(look(0x86705A, 0x645240, 0xDCCCB0, 0x483A2C, SADDLES, 1f, 0xB0583A, 0xEAD2A0, 0x2E1E0E, 0xE3D6B8, 0x2E2A25)).voice(CERATOPS));
        l.add(sp("torosaurus", "Torosaurus latus and T. utahensis", CERATOPSIAN, 8f).shortName("Torosaurus").real(6000, 2.1f).shoulder(1.7f).speed(1.4f, 6f)
                .temper(DEFENDER, GORE, HERBIVORE).group(1, 3, 2).age("Late Cretaceous, 68-66 million years ago").site(HELL_CREEK, JAVELINA, SAN_JUAN)
                .note("One of the biggest skulls of any land animal, most of it frill.")
                .body(b -> { b.skull = 1.3f; b.frillH = 1.45f; b.frillW = 1.8f; b.frillUp = 48; b.hornBrow = 0.85f; b.hornBrowUp = 30; b.hornNasal = 0.15f; b.torso = 2.7f; b.depth = 1.8f; b.width = 1.55f; })
                .look(look(0x7E7058, 0x5E5444, 0xD8CCB0, 0x463C30, MOTTLED, 1.1f, 0x9A4A3A, 0xE6CEA0, 0x2E1E0E, 0xE3D6B8, 0x2E2A25)).voice(CERATOPS));
        l.add(sp("nasutoceratops", "Nasutoceratops titusi", CERATOPSIAN, 4.5f).shortName("Nasutoceratops").real(1500, 1.3f).shoulder(1.05f).speed(1.3f, 6f)
                .temper(DEFENDER, GORE, HERBIVORE).group(2, 6, 2).age("Late Cretaceous, 76 million years ago").site(KAIPAROWITS)
                .body(b -> { b.skull = 0.5f; b.hornBrow = 0.45f; b.hornBrowUp = 8; b.hornBrowOut = 35; b.nasalBoss = 0.08f; b.frillH = 0.45f; b.frillW = 0.75f; b.frillUp = 55; })
                .look(look(0x8A6A50, 0x684E3A, 0xDCC8AA, 0x4A3628, BANDS, 1f, 0xC05A3A, 0xF0D8A0, 0x2E1E0E, 0xE3D6B8, 0x2E2A25)).voice(CERATOPS)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("sinoceratops", "Sinoceratops zhuchengensis", CERATOPSIAN, 5f).shortName("Sinoceratops").real(2000, 1.35f).shoulder(1.1f).speed(1.3f, 6f)
                .temper(DEFENDER, GORE, HERBIVORE).group(2, 6, 2).age("Late Cretaceous, 77-73.5 million years ago").site(WANGSHI)
                .body(b -> { b.skull = 0.7f; b.hornNasal = 0.3f; b.hornNasalUp = 65; b.frillH = 0.55f; b.frillW = 0.9f; b.frillUp = 62; b.frillSpikes = 10; b.frillSpikeL = 0.15f; })
                .look(look(0x7A6650, 0x5A4A3C, 0xD8C8AE, 0x3E3226, SPOTS, 1f, 0xA8402E, 0xE8D0A0, 0x2E1E0E, 0xE3D6B8, 0x2E2A25)).voice(CERATOPS));

        // ---- more plated and armoured dinosaurs, and a dome-head
        l.add(sp("miragaia", "Miragaia longicollum", STEGOSAUR, 6.5f).shortName("Miragaia").real(2000, 1.8f).shoulder(1.15f).speed(1.2f, 2.5f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(1, 3, 2).age("Late Jurassic, 150 million years ago").site(LOURINHA)
                .note("A stegosaur with a long neck: seventeen neck vertebrae.")
                .body(b -> { b.neck = 0.22f * 6.5f; b.neckN = 3; b.neckUp = -2; b.plateH = 0.4f; b.skull = 0.35f; })
                .look(look(0x86784E, 0x665A3A, 0xDCD0AA, 0x4A4028, BANDS, 1f, 0xA8502A, 0x6A5E3E, 0x2A1A0A, 0xD9CDB1, 0x2E2A25)).voice(STEGO)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("tuojiangosaurus", "Tuojiangosaurus multispinus", STEGOSAUR, 7f).shortName("Tuojiangosaurus").real(2800, 1.9f).shoulder(1.2f).speed(1.1f, 2.5f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(1, 3, 2).age("Late Jurassic, 160 million years ago").site(SHAXIMIAO)
                .body(b -> { b.plates = 15; b.plateH = 0.48f; b.shoulderSpikeL = 0.4f; b.skull = 0.38f; })
                .look(look(0x6E6A4A, 0x524E36, 0xD2CCAA, 0x3E3A26, SPOTS, 1f, 0xC0402A, 0x524E36, 0x2A1A0A, 0xD9CDB1, 0x2E2A25)).voice(STEGO)
                .climates(DinoSpecies.WARM | DinoSpecies.TROPICAL));
        l.add(sp("huayangosaurus", "Huayangosaurus taibaii", STEGOSAUR, 4.0f).shortName("Huayangosaurus").real(500, 1.05f).shoulder(0.85f).speed(1.1f, 3f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(2, 5, 2).age("Middle Jurassic, about 165 million years ago").site(SHAXIMIAO)
                .body(b -> { b.plates = 12; b.plateH = 0.25f; b.shoulderSpikeL = 0.35f; b.skull = 0.3f; b.clubW = 0.12f; })
                .look(look(0x8A7A54, 0x6A5C3E, 0xDCD0AE, 0x4E4228, MOTTLED, 1f, 0x9A5A30, 0x6A5C3E, 0x2A1A0A, 0xD9CDB1, 0x2E2A25)).voice(STEGO)
                .climates(DinoSpecies.WARM | DinoSpecies.TROPICAL));
        l.add(sp("dacentrurus", "Dacentrurus armatus", STEGOSAUR, 8f).shortName("Dacentrurus").real(5000, 2.2f).shoulder(1.5f).speed(1.1f, 2.5f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(1, 2, 1).age("Late Jurassic, 152-145 million years ago").site(KIMMERIDGE, LOURINHA)
                .body(b -> { b.plates = 10; b.plateH = 0.3f; b.tailSpikeL = 0.5f; b.skull = 0.42f; })
                .look(look(0x7A7454, 0x5A5640, 0xD4CEAE, 0x403C2A, BANDS, 1.1f, 0x8A4A2A, 0x5A5640, 0x2A1A0A, 0xD9CDB1, 0x2E2A25)).voice(STEGO)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("edmontonia", "Edmontonia rugosidens and E. longiceps", NODOSAUR, 6.6f).shortName("Edmontonia").real(3000, 1.4f).shoulder(1.2f).speed(1.0f, 2.7f)
                .temper(DEFENDER, GORE, HERBIVORE).group(1, 2, 2).age("Late Cretaceous, 76-71 million years ago").site(DINOSAUR_PARK, HORSESHOE_CANYON)
                .body(b -> { b.shoulderSpikeL = 0.55f; b.shoulderSpikeA = 55; })
                .look(look(0x7A6A52, 0x5A4C3A, 0xD2C4A8, 0x3E3226, MOTTLED, 1f, 0x8A7458, 0x5A4C3A, 0x2E1E0E, 0xD7C8A8, 0x2E2A25)).voice(ANKY));
        l.add(sp("sauropelta", "Sauropelta edwardsorum", NODOSAUR, 5.2f).shortName("Sauropelta").real(1500, 1.15f).shoulder(1.0f).speed(1.0f, 2.7f)
                .temper(DEFENDER, GORE, HERBIVORE).group(1, 2, 2).age("Early Cretaceous, 115-110 million years ago").site(CLOVERLY)
                .body(b -> { b.shoulderSpikeL = 0.45f; b.shoulderSpikeA = 5; b.tail = 0.45f * 5.2f; })
                .look(look(0x8A7A5C, 0x6A5C44, 0xD8CCAE, 0x4A3E2C, SPOTS, 1f, 0x9A8462, 0x6A5C44, 0x2E1E0E, 0xD7C8A8, 0x2E2A25)).voice(ANKY));
        l.add(sp("minmi", "Minmi paravertebra", NODOSAUR, 3f).shortName("Minmi").real(300, 0.75f).shoulder(0.65f).speed(1.0f, 3f)
                .temper(DEFENDER, GORE, HERBIVORE).group(1, 2, 2).age("Early Cretaceous, 120-113 million years ago").site(BUNGIL)
                .body(b -> { b.shoulderSpikeL = 0f; })
                .look(look(0x8C7A5A, 0x6C5C42, 0xDCD0B0, 0x4C402C, MOTTLED, 0.9f, 0x9A8462, 0x6C5C42, 0x2E1E0E, 0xD7C8A8, 0x2E2A25)).voice(ANKY)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("gastonia", "Gastonia burgei", NODOSAUR, 5f).shortName("Gastonia").real(1900, 1.15f).shoulder(1.0f).speed(1.0f, 2.7f)
                .temper(DEFENDER, GORE, HERBIVORE).group(1, 3, 2).age("Early Cretaceous, 139-134 million years ago").site(CEDAR_MOUNTAIN)
                .body(b -> { b.shoulderSpikeL = 0.45f; b.sideSpikes = 0.12f; })
                .look(look(0x8A7454, 0x68563E, 0xD8C8A8, 0x4A3A28, BANDS, 1f, 0x9A8462, 0x68563E, 0x2E1E0E, 0xD7C8A8, 0x2E2A25)).voice(ANKY)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("scelidosaurus", "Scelidosaurus harrisonii", NODOSAUR, 4f).shortName("Scelidosaurus").real(270, 1.0f).shoulder(0.8f).speed(1.1f, 3f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(1, 2, 2).age("Early Jurassic, 191 million years ago").site(DORSET)
                .note("One of the first armoured dinosaurs, from the Jurassic Coast.")
                .body(b -> { b.shoulderSpikeL = 0f; b.tail = 0.5f * 4f; b.width = 0.14f * 4f; b.armour = 0.06f; })
                .look(look(0x7E7A5A, 0x5E5A42, 0xD8D4B8, 0x44402E, SPOTS, 0.9f, 0x8A7A5A, 0x5E5A42, 0x2E1E0E, 0xD7C8A8, 0x2E2A25)).voice(ANKY)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("pinacosaurus", "Pinacosaurus grangeri", ANKYLOSAUR, 5f).shortName("Pinacosaurus").real(1900, 1.05f).shoulder(0.9f).speed(1.0f, 3f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, 80-75 million years ago").site(DJADOCHTA)
                .note("Young ones have been found together, buried by a sandstorm.")
                .body(b -> { b.skull = 0.4f; b.clubW = 0.35f; b.hornBrow = 0.08f; b.hornBrowUp = 0; b.hornBrowOut = 60; b.hornBrowZ = 0; })
                .look(look(0xA8906A, 0x84704E, 0xE6D8BC, 0x5E4C34, MOTTLED, 1f, 0x9A8462, 0x6A5A40, 0x2E1E0E, 0xD7C8A8, 0x2E2A25)).voice(ANKY)
                .climates(DinoSpecies.ARID));
        l.add(sp("zuul", "Zuul crurivastator", ANKYLOSAUR, 6f).shortName("Zuul").real(2500, 1.25f).shoulder(1.05f).speed(1.0f, 2.7f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(1, 1, 1).age("Late Cretaceous, 75 million years ago").site(JUDITH_RIVER)
                .note("Named after the Ghostbusters monster; crurivastator means \"destroyer of shins\".")
                .body(b -> { b.skull = 0.5f; b.clubW = 0.5f; b.hornBrow = 0.12f; b.hornBrowUp = 0; b.hornBrowOut = 60; b.hornBrowZ = 0; b.sideSpikes = 0.15f; })
                .look(look(0x7A6650, 0x5A4A3A, 0xD6C6A8, 0x3E3226, SPOTS, 1f, 0x8A7458, 0x5A4A3A, 0x2E1E0E, 0xD7C8A8, 0x2E2A25)).voice(ANKY));
        l.add(sp("prenocephale", "Prenocephale prenes", PACHYCEPHALOSAUR, 2.4f).shortName("Prenocephale").real(130, 0.65f).speed(1.4f, 7f)
                .temper(DEFENDER, HEADBUTT, HERBIVORE).group(1, 4, 2).age("Late Cretaceous, 70 million years ago").site(NEMEGT)
                .body(b -> { b.skull = 0.32f; b.dome = 0.1f; })
                .look(look(0xA08A66, 0x7E6A4C, 0xE6DCC4, 0x5A4A34, SPOTS, 0.9f, 0xE0D4B8, 0x8A4A30, 0xD0A030, 0xCFC2A6, 0x3A3530)).voice(GOAT)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));

        // ================================================================ batch 4 (0.7.3): new countries and odd ones

        // ---- theropods
        l.add(sp("rajasaurus", "Rajasaurus narmadensis", THEROPOD, 11f).shortName("Rajasaurus").real(4000, 2.9f).speed(1.4f, 7f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Late Cretaceous, 70-66 million years ago").site(LAMETA_GUJARAT)
                .note("'Regal lizard': it wore a single low horn on top of its head.")
                .body(b -> { b.skull = 1.0f; b.skullH = 0.55f; b.skullW = 0.45f; b.snout = 0.38f; b.hornNasal = 0.13f; b.hornNasalUp = 85; b.armL = 0.67f; b.armW = 0.15f; b.legW = 0.75f; })
                .look(look(0xA0704A, 0x744E32, 0xE2CCA8, 0x4E3220, SPOTS, 0.9f, 0xB8402E, 0xE0C8A0, 0xE0A020, 0xC9B79C, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("rugops", "Rugops primus", THEROPOD, 6f).shortName("Rugops").real(750, 1.6f).speed(1.4f, 7f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Late Cretaceous, 95 million years ago").site(NIGER_AGADEZ)
                .note("'Wrinkle face': its skull was covered in grooves and pits.")
                .body(b -> { b.skull = 0.75f; b.skullH = 0.55f; b.skullW = 0.42f; b.snout = 0.36f; b.crest = Body.Crest.RIDGE; b.crestH = 0.04f; b.crestL = 0.3f; b.armL = 0.35f; b.armW = 0.08f; })
                .look(look(0xA88458, 0x7E603E, 0xE4D2B0, 0x56402A, MOTTLED, 1f, 0xC0502E, 0xE4D2B0, 0xE0A020, 0xC9B79C, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.ARID));
        l.add(sp("skorpiovenator", "Skorpiovenator bustingorryi", THEROPOD, 7.5f).shortName("Skorpiovenator").real(1670, 2.0f).speed(1.5f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Late Cretaceous, 95 million years ago").site(NEUQUEN)
                .note("'Scorpion hunter', for the scorpions living around the dig. Its face was covered in bumps and ridges.")
                .body(b -> { b.skull = 0.7f; b.skullH = 0.55f; b.skullW = 0.45f; b.snout = 0.36f; b.crest = Body.Crest.RIDGE; b.crestH = 0.05f; b.crestL = 0.35f; b.armL = 0.43f; b.armW = 0.1f; b.legW = 0.51f; })
                .look(look(0x8E5E44, 0x663F2C, 0xDCC4A4, 0x40261A, BANDS, 1f, 0xA8382A, 0xE0C8A0, 0xE0A020, 0xC9B79C, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("masiakasaurus", "Masiakasaurus knopfleri", SMALL_THEROPOD, 2f).shortName("Masiakasaurus").real(20, 0.55f).speed(1.4f, 8f)
                .temper(SKULKER, BITE, CARNIVORE).group(1, 3, 2).age("Late Cretaceous, 70-66 million years ago").site(MAEVARANO)
                .note("Its front teeth stuck out forward, perhaps for snatching fish or small animals. Named after Mark Knopfler of Dire Straits.")
                .body(b -> { b.skull = 0.2f; b.snout = 0.55f; b.armL = 0.3f; })
                .look(look(0x9A7A58, 0x6E5640, 0xE6D8BE, 0x4A3826, STRIPES, 0.8f, 0xB0603A, 0xE6D8BE, 0xE0B030, 0xD8CDB2, 0x2B2420)).voice(GROWL)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.ARID));
        l.add(sp("afrovenator", "Afrovenator abakensis", THEROPOD, 8f).shortName("Afrovenator").real(1000, 2.1f).speed(1.5f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Middle-Late Jurassic, about 167-161 million years ago").site(NIGER_AGADEZ)
                .body(b -> { b.skull = 0.84f; b.skullW = 0.3f; b.armL = 1.0f; b.armW = 0.11f; b.clawL = 0.12f; })
                .look(look(0xB08A5E, 0x84663E, 0xE8D8B8, 0x5A4228, SADDLES, 1.1f, 0xB8582E, 0xE8D0A0, 0xE0A020, 0xD8CDB2, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.ARID | DinoSpecies.TROPICAL));
        l.add(sp("irritator", "Irritator challengeri", SPINOSAUR, 7.5f).shortName("Irritator").real(1000, 1.6f).speed(1.2f, 6f)
                .temper(HUNTER, BITE, PISCIVORE).group(1, 1, 2).age("Early Cretaceous, 113-110 million years ago").site(ROMUALDO)
                .note("Its skull, bought from fossil dealers, had been made longer with grafted-on pieces of jaw and buried in plaster, which irritated the scientists who described it.")
                .aquatic().body(b -> { b.skull = 0.85f; b.sailH = 0.25f; b.sailFrom = 0.1f; b.sailTo = 1f; b.sailOnTail = 0.4f; b.crest = Body.Crest.RIDGE; b.crestH = 0.05f; b.crestL = 0.3f; b.armL = 1.0f; b.armW = 0.13f; b.clawL = 0.18f; })
                .look(look(0x7E6A52, 0x5A4A38, 0xDCD0B8, 0x3A2E22, STRIPES, 1.2f, 0xC05A30, 0xE8C88A, 0xE0B030, 0xD8CDB2, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.WARM));
        l.add(sp("ichthyovenator", "Ichthyovenator laosensis", SPINOSAUR, 8.5f).shortName("Ichthyovenator").real(2000, 1.9f).speed(1.2f, 6f)
                .temper(HUNTER, BITE, PISCIVORE).group(1, 1, 1).age("Early Cretaceous, 120-113 million years ago").site(SAVANNAKHET)
                .note("'Fish hunter' from Laos. Its sail dipped over the hips, making two humps.")
                .aquatic().body(b -> { b.skull = 1.0f; b.sailH = 0.55f; b.sailFrom = 0.2f; b.sailTo = 1f; b.sailOnTail = 0.5f; b.crest = Body.Crest.RIDGE; b.crestH = 0.05f; b.crestL = 0.35f; b.armL = 1.1f; b.armW = 0.14f; b.clawL = 0.22f; })
                .look(look(0x6A6A58, 0x4A4A3E, 0xD0CCB4, 0x2E2E26, STRIPES, 1.3f, 0xB8462A, 0xE8B86A, 0xE0B030, 0xD8CDB2, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.WARM));
        l.add(sp("timurlengia", "Timurlengia euotica", THEROPOD, 4f).shortName("Timurlengia").real(270, 1.1f).speed(1.5f, 9f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Late Cretaceous, 92-90 million years ago").site(BISSEKTY)
                .note("A horse-sized cousin of Tyrannosaurus, with a big brain and an inner ear tuned to deep sounds.")
                .body(b -> { b.skull = 0.48f; b.armL = 0.57f; b.armW = 0.06f; b.shinF = 1.1f; b.metaF = 0.6f; })
                .look(look(0xA08460, 0x766040, 0xE6D8BC, 0x4E3C28, BANDS, 1f, 0xB8582E, 0xE6D8BC, 0xE0A020, 0xD8CDB2, 0x2B2420)).voice(GROWL)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("dryptosaurus", "Dryptosaurus aquilunguis", THEROPOD, 7.5f).shortName("Dryptosaurus").real(1500, 2.0f).speed(1.5f, 8f)
                .temper(HUNTER, CLAW, CARNIVORE).group(1, 1, 1).age("Late Cretaceous, 67 million years ago").site(WOODBURY)
                .note("An East Coast tyrannosaur with long arms and eagle-like claws. Charles Knight painted two of them leaping in battle in 1897.")
                .body(b -> { b.skull = 0.9f; b.armL = 1.2f; b.armW = 0.12f; b.clawL = 0.2f; b.legW = 0.5f; })
                .look(look(0x6E5A48, 0x4E3E32, 0xD8C8AE, 0x2E241C, STRIPES, 1.2f, 0x9A3A2A, 0xD8C8AE, 0xE0A020, 0xCFC2A6, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("appalachiosaurus", "Appalachiosaurus montgomeriensis", THEROPOD, 7f).shortName("Appalachiosaurus").real(600, 1.9f).speed(1.5f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 1).age("Late Cretaceous, 77 million years ago").site(DEMOPOLIS)
                .note("From Appalachia, the eastern half of North America, cut off from the west by an inland sea.")
                .body(b -> { b.skull = 0.8f; b.armL = 0.9f; b.armW = 0.09f; b.clawL = 0.12f; b.crest = Body.Crest.RIDGE; b.crestH = 0.03f; b.crestL = 0.4f; })
                .look(look(0x7A6650, 0x56483A, 0xDCCCB0, 0x3A2E22, SADDLES, 1.1f, 0x9A4A32, 0xDCCCB0, 0xE0A020, 0xCFC2A6, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE | DinoSpecies.TROPICAL));
        l.add(sp("fukuiraptor", "Fukuiraptor kitadaniensis", THEROPOD, 5f).shortName("Fukuiraptor").real(300, 1.35f).speed(1.6f, 9f)
                .temper(HUNTER, CLAW, CARNIVORE).group(1, 1, 2).age("Early Cretaceous, about 121-113 million years ago").site(KITADANI)
                .note("Its big hand claws were at first mistaken for the sickle foot claws of a raptor (a dromaeosaur).")
                .body(b -> { b.skull = 0.5f; b.skullW = 0.3f; b.armL = 1.0f; b.armW = 0.07f; b.clawL = 0.14f; b.shinF = 1.1f; b.metaF = 0.6f; })
                .look(look(0x8A7458, 0x64523E, 0xE2D6BC, 0x403226, STRIPES, 1f, 0xA8502E, 0xE2D6BC, 0xE0A020, 0xD8CDB2, 0x2B2420)).voice(GROWL)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("guanlong", "Guanlong wucaii", SMALL_THEROPOD, 3f).shortName("Guanlong").real(125, 0.85f).speed(1.5f, 9f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 2, 2).age("Late Jurassic, 160 million years ago").site(SHISHUGOU)
                .note("'Crowned dragon': an early cousin of Tyrannosaurus with a thin, hollow crest along its snout.")
                .body(b -> { b.feathers = true; b.skull = 0.35f; b.crest = Body.Crest.BLADE; b.crestH = 0.08f; b.crestL = 0.3f; b.armL = 0.6f; b.armW = 0.05f; b.clawL = 0.05f; })
                .look(look(0x8A6A4E, 0x644C36, 0xE2D2B8, 0x3E2E20, BANDS, 0.9f, 0xD0602E, 0xF0C060, 0xE0A020, 0xD8CDB2, 0x2B2420)).voice(GROWL)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE | DinoSpecies.ARID));
        l.add(sp("qianzhousaurus", "Qianzhousaurus sinensis", THEROPOD, 6.3f).shortName("Qianzhousaurus").aka("pinocchio_rex").real(757, 2.0f).speed(1.5f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 1).age("Late Cretaceous, about 67 million years ago").site(NANXIONG)
                .note("Nicknamed Pinocchio rex: a slim tyrannosaur with a long, narrow snout.")
                .body(b -> { b.skull = 0.8f; b.skullH = 0.3f; b.skullW = 0.26f; b.snout = 0.62f; b.snoutW = 0.55f; b.armL = 0.36f; b.armW = 0.045f; b.legW = 0.3f; b.shinF = 1.1f; b.metaF = 0.6f; })
                .look(look(0x8A6E56, 0x645040, 0xE0D0B8, 0x3E3024, STRIPES, 1.2f, 0xA84A30, 0xE0D0B8, 0xE0A020, 0xCFC2A6, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.TROPICAL | DinoSpecies.TEMPERATE));
        l.add(sp("alioramus", "Alioramus remotus and A. altai", THEROPOD, 5.5f).shortName("Alioramus").real(500, 1.5f).speed(1.6f, 9f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Late Cretaceous, 70 million years ago").site(NEMEGT)
                .note("A long-snouted tyrannosaur with a row of small bony bumps along its nose.")
                .body(b -> { b.skull = 0.64f; b.skullH = 0.32f; b.skullW = 0.28f; b.snout = 0.6f; b.snoutW = 0.55f; b.crest = Body.Crest.RIDGE; b.crestH = 0.04f; b.crestL = 0.35f; b.armL = 0.37f; b.armW = 0.05f; b.shinF = 1.1f; b.metaF = 0.6f; })
                .look(look(0xA68A66, 0x7C6648, 0xE8DCC4, 0x54442E, SPOTS, 0.9f, 0xB0563A, 0xE8DCC4, 0xE0A020, 0xCFC2A6, 0x2B2420)).voice(GROWL)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("deinocheirus", "Deinocheirus mirificus", ORNITHOMIMID, 11.5f).shortName("Deinocheirus").real(5000, 3.0f).speed(1.2f, 5f)
                .temper(DEFENDER, CLAW, OMNIVORE).group(1, 1, 1).age("Late Cretaceous, 70 million years ago").site(NEMEGT)
                .note("For nearly fifty years it was known only from a pair of giant arms and shoulders. Two new partial skeletons, described in 2014, showed a humped back, a duck-like bill and fish in its gut.")
                .body(b -> { b.tailFan = true; b.wings = false; b.skull = 1.0f; b.skullH = 0.38f; b.snout = 0.6f; b.snoutW = 1.0f; b.snoutH = 0.5f; b.neck = 1.9f; b.neckD = 0.5f; b.torso = 3.0f; b.depth = 2.0f; b.width = 1.3f; b.pitch = 10;
                    b.sailH = 0.8f; b.sailFrom = 0.35f; b.sailTo = 1f; b.sailOnTail = 0.3f; b.armL = 2.4f; b.armW = 0.24f; b.clawL = 0.2f; b.legW = 0.75f; b.shinF = 0.9f; b.metaF = 0.45f; b.thighA = 18; b.shinA = -28; b.metaA = 20; b.footW = 1.1f; b.tailD = 0.75f; })
                .look(look(0x8A7A62, 0x625644, 0xE0D6C0, 0x463C30, PLAIN, 1f, 0xC86A3A, 0xF0E0C0, 0xD08A20, 0x6A5A48, 0x2B2420)).voice(HOOT)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("gigantoraptor", "Gigantoraptor erlianensis", OVIRAPTOR, 8f).shortName("Gigantoraptor").real(1400, 3.0f).speed(1.6f, 8f)
                .temper(DEFENDER, KICK, OMNIVORE).group(1, 3, 2).age("Late Cretaceous, 96-70 million years ago (dating disputed)").site(IREN_DABASU)
                .note("A beaked cousin of Oviraptor, as big as a mid-sized tyrannosaur.")
                .body(b -> { b.skull = 0.6f; b.armL = 1.6f; b.armW = 0.12f; b.clawL = 0.12f; b.legW = 0.5f; })
                .look(look(0x6E5E50, 0x4A3E36, 0xDCD2C4, 0xEDE4D8, BARS, 1.2f, 0xC0402E, 0x3A6A9A, 0xE0B030, 0x2E2A26, 0x2B2420)).voice(BIRD)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("caudipteryx", "Caudipteryx zoui", OVIRAPTOR, 1f).shortName("Caudipteryx").real(5, 0.4f).speed(1.4f, 8f)
                .temper(SKITTISH, KICK, OMNIVORE).group(2, 6, 3).age("Early Cretaceous, 125 million years ago").site(LIAONING)
                .note("A turkey-sized feathered dinosaur with a fan of long feathers at the end of its short tail.")
                .body(b -> { b.skull = 0.1f; b.skullH = 0.6f; b.armL = 0.25f; })
                .look(look(0x6A5E52, 0x4A4038, 0xE2DAD0, 0x2E2824, BARS, 0.6f, 0x3A3430, 0xF2EEE6, 0xE0B030, 0xC9BBA0, 0x2B2420)).voice(BIRD)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("anchiornis", "Anchiornis huxleyi", RAPTOR, 0.34f).shortName("Anchiornis").real(0.11f, 0.12f).speed(0.8f, 5f)
                .temper(SKULKER, BITE, CARNIVORE).group(1, 3, 2).age("Late Jurassic, 160 million years ago").site(TIAOJISHAN)
                .note("The first dinosaur whose almost whole colour pattern was read from its fossils: grey, with white wings and legs spangled with black, and a reddish crown.")
                .body(b -> { b.legWings = true; b.sickle = 0; b.crest = Body.Crest.COMB; b.crestH = 0.015f; b.crestL = 0.03f; })
                .look(look(0x4A4A4C, 0x303032, 0x8A8A8C, 0xF2F2F0, BARS, 0.5f, 0xA8442A, 0xF2F2F0, 0xE0B030, 0x2B2420, 0x1E1A16)).voice(CHIRP)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("ornithomimus", "Ornithomimus edmontonicus", ORNITHOMIMID, 3.8f).shortName("Ornithomimus").real(170, 1.3f).speed(1.8f, 12f)
                .temper(SKITTISH, KICK, OMNIVORE).group(4, 12, 4).age("Late Cretaceous, 76-70 million years ago").site(HORSESHOE_CANYON, DINOSAUR_PARK)
                .note("Fossils show it was feathered, and grown-ups seem to have had wing-like feathered arms (a disputed reading), perhaps for display or for sitting on eggs.")
                .look(look(0x9A8670, 0x6E5E4C, 0xEAE2D4, 0x3E342A, PLAIN, 1f, 0x2E2620, 0xF2EEE6, 0xD08A20, 0x8A7A60, 0x2B2420)).voice(BIRD)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM | DinoSpecies.COLD));
        l.add(sp("dromaeosaurus", "Dromaeosaurus albertensis", RAPTOR, 2f).shortName("Dromaeosaurus").real(15, 0.55f).speed(1.4f, 9f)
                .temper(PACK, BITE, CARNIVORE).group(1, 3, 2).age("Late Cretaceous, 76.5-75 million years ago").site(DINOSAUR_PARK)
                .note("The raptor its whole family is named after: short-snouted, with a strong bite.")
                .body(b -> { b.skull = 0.23f; b.skullH = 0.48f; b.skullW = 0.4f; b.snout = 0.42f; })
                .look(look(0x6E5E4E, 0x4A3E34, 0xE2D8C8, 0x2A221C, BARS, 1f, 0x8A3A2A, 0xEDE6DA, 0xE0A520, 0x2B2420, 0x1E1A16)).voice(RAPTOR_CALL)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("balaur", "Balaur bondoc", RAPTOR, 2f).shortName("Balaur").real(15, 0.5f).speed(1.2f, 7f)
                .temper(SKULKER, KICK, CARNIVORE).group(1, 2, 2).age("Late Cretaceous, 70 million years ago").site(HATEG)
                .note("'Stocky dragon': two sickle claws on each foot where its cousins had one, and only two working fingers.")
                .body(b -> { b.sickles = 2; b.sickle = 0.07f; b.legW = 0.13f; b.shinF = 0.95f; b.metaF = 0.45f; b.armL = 0.4f; })
                .look(look(0x5E5244, 0x40382E, 0xD8CEBE, 0x262019, BARS, 0.9f, 0x6A3A2A, 0xE6DED0, 0xE0A520, 0x2B2420, 0x1E1A16)).voice(RAPTOR_CALL)
                .climates(DinoSpecies.WARM | DinoSpecies.TROPICAL));
        l.add(sp("eoraptor", "Eoraptor lunensis", SMALL_THEROPOD, 1f).shortName("Eoraptor").real(10, 0.3f).speed(1.3f, 7f)
                .temper(SKITTISH, BITE, OMNIVORE).group(1, 3, 2).age("Late Triassic, 231 million years ago").site(ISCHIGUALASTO)
                .note("'Dawn plunderer': one of the oldest known dinosaurs, about the size of a fox.")
                .body(b -> { b.skull = 0.12f; b.armL = 0.2f; })
                .look(look(0xA07A50, 0x765A3A, 0xE8D8BC, 0x4E3A26, STRIPES, 0.7f, 0xB05E32, 0xE8D8BC, 0xE0B030, 0xD8CDB2, 0x2B2420)).voice(GROWL)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("staurikosaurus", "Staurikosaurus pricei", SMALL_THEROPOD, 2.1f).shortName("Staurikosaurus").real(12, 0.5f).speed(1.4f, 8f)
                .temper(SKULKER, BITE, CARNIVORE).group(1, 1, 2).age("Late Triassic, 233 million years ago").site(SANTA_MARIA)
                .note("Named for the Southern Cross, the constellation on Brazil's flag. One of the first dinosaurs.")
                .body(b -> { b.skull = 0.26f; b.armL = 0.3f; })
                .look(look(0x9A6A48, 0x704C34, 0xE4D0B4, 0x4A3020, BANDS, 0.8f, 0xB04A2E, 0xE4D0B4, 0xE0A020, 0xD8CDB2, 0x2B2420)).voice(GROWL)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));

        // ---- long-necks and their early cousins
        l.add(sp("saturnalia", "Saturnalia tupiniquim", PROSAUROPOD, 1.5f).shortName("Saturnalia").real(10, 0.4f).speed(1.3f, 7f)
                .temper(SKITTISH, BITE, OMNIVORE).group(2, 5, 3).age("Late Triassic, 233 million years ago").site(SANTA_MARIA)
                .note("One of the earliest relatives of the long-necked giants, no bigger than a dog. Found during Carnival, hence its name.")
                .body(b -> { b.skull = 0.1f; b.armL = 0.3f; })
                .look(look(0x8E7A56, 0x6A5A3E, 0xE2D8BC, 0x4A3E2A, STRIPES, 0.8f, 0xA0603A, 0xE2D8BC, 0xD0A030, 0xD8CDB2, 0x2B2420)).voice(SMALL_HERB)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("anchisaurus", "Anchisaurus polyzelus", PROSAUROPOD, 2.2f).shortName("Anchisaurus").real(20, 0.55f).speed(1.3f, 6f)
                .temper(SKITTISH, CLAW, HERBIVORE).group(2, 5, 3).age("Early Jurassic, 200-190 million years ago").site(PORTLAND)
                .note("Bones blasted out of a well in Connecticut in 1818, among the first dinosaur bones found in North America, were once credited to it; today they can't be pinned to any one genus.")
                .body(b -> { b.clawL = 0.04f; })
                .look(look(0x86744E, 0x645638, 0xE0D6B8, 0x463A26, BANDS, 0.8f, 0x9A6A3A, 0xE0D6B8, 0xD0A030, 0xD8CDB2, 0x2B2420)).voice(SMALL_HERB)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("mussaurus", "Mussaurus patagonicus", PROSAUROPOD, 6f).shortName("Mussaurus").real(1000, 1.5f).speed(1.3f, 4f)
                .temper(SKITTISH, CLAW, HERBIVORE).group(3, 10, 3).age("Early Jurassic, 192 million years ago").site(LAGUNA_COLORADA)
                .note("'Mouse lizard', named from its tiny hatchlings; grown-ups weighed a tonne or more. One nesting ground held over 100 eggs and 80 skeletons, from embryos to adults, with youngsters of an age found together: early evidence of herds.")
                .body(b -> { b.clawL = 0.08f; })
                .look(look(0x8E7E5E, 0x6A5E44, 0xE0D8C0, 0x4A402E, MOTTLED, 1f, 0xA0703E, 0xE0D8C0, 0xD0A030, 0xD8CDB2, 0x2B2420)).voice(BELLOW)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE | DinoSpecies.ARID));
        l.add(sp("jobaria", "Jobaria tiguidensis", TITANOSAUR, 16f).shortName("Jobaria").real(16000, 2.85f).shoulder(2.95f).speed(1.4f, 3f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(2, 6, 3).age("Middle-Late Jurassic, about 167-161 million years ago").site(NIGER_AGADEZ)
                .note("Named after Jobar, a creature of Tuareg legend said to own the bones.")
                .body(b -> { b.neck = 0.3f * 16f; b.neckUp = 35; b.skull = 0.5f; })
                .look(look(0x9A846A, 0x76644E, 0xE2D8C4, 0x5A4C3A, BANDS, 1.4f, 0x8A6A4A, 0xC8B89C, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.ARID | DinoSpecies.TROPICAL | DinoSpecies.WARM));
        l.add(sp("turiasaurus", "Turiasaurus riodevensis", BRACHIOSAUR, 30f).shortName("Turiasaurus").real(50000, 4.5f).shoulder(5.0f).speed(1.5f, 2.5f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(1, 3, 2).age("Late Jurassic, 155-146 million years ago").site(RIODEVA)
                .note("One of the biggest dinosaurs found in Europe.")
                .body(b -> { b.neck = 0.33f * 30f; b.neckUp = 35; b.legW = 1.4f; b.frontW = 1.3f; b.torso = 6.5f; b.depth = 4.0f; b.width = 3.4f; })
                .look(look(0x8C8270, 0x6C6454, 0xD8D0BE, 0x5C5444, MOTTLED, 1.8f, 0x8A7A62, 0xB0A690, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("barosaurus", "Barosaurus lentus", DIPLODOCID, 26f).shortName("Barosaurus").real(12000, 3.6f).shoulder(3.2f).speed(1.5f, 3.5f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(2, 5, 2).age("Late Jurassic, 152-150 million years ago").site(MORRISON)
                .note("A cousin of Diplodocus with a longer neck and a shorter tail.")
                .body(b -> { b.neck = 0.36f * 26f; b.neckUp = 22; })
                .look(look(0x7E8070, 0x5E6254, 0xD4D6C6, 0x464A3E, BANDS, 1.5f, 0x8A7A5A, 0xA8AC98, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD));
        l.add(sp("dreadnoughtus", "Dreadnoughtus schrani", TITANOSAUR, 26f).shortName("Dreadnoughtus").real(31000, 4.4f).shoulder(4.6f).speed(1.5f, 2.5f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(1, 2, 1).age("Late Cretaceous, 76-72 million years ago").site(CERRO_FORTALEZA)
                .note("Named for the dreadnought battleships: so big it feared nothing. One of the most complete giant titanosaurs found.")
                .body(b -> { b.skull = 0.8f; b.legW = 1.3f; b.frontW = 1.15f; b.width = 3.2f; b.depth = 3.6f; b.torso = 5.6f; b.neckD = 1.3f; })
                .look(look(0x86806E, 0x666052, 0xD6D0BE, 0x565042, MOTTLED, 1.8f, 0x8A7A62, 0xB0A690, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("isisaurus", "Isisaurus colberti", TITANOSAUR, 18f).shortName("Isisaurus").real(15000, 3.0f).shoulder(3.6f).speed(1.4f, 3f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(2, 6, 2).age("Late Cretaceous, 70-66 million years ago").site(LAMETA_CENTRAL)
                .note("A titanosaur with a short, upright neck and long front legs. Named after the Indian Statistical Institute.")
                .body(b -> { b.neck = 0.24f * 18f; b.neckUp = 42; })
                .look(look(0x948066, 0x72604C, 0xDED2BC, 0x544638, MOTTLED, 1.6f, 0x8A7A62, 0xB0A690, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("rapetosaurus", "Rapetosaurus krausei", TITANOSAUR, 16.5f).shortName("Rapetosaurus").real(10300, 2.9f).shoulder(3.0f).speed(1.4f, 3f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(2, 5, 2).age("Late Cretaceous, 70-66 million years ago").site(MAEVARANO)
                .note("Named after Rapeto, a mischievous giant of Malagasy folklore. Its skeletons include a near-complete youngster.")
                .body(b -> { b.skull = 0.77f; b.snout = 0.55f; })
                .look(look(0x8C7C66, 0x6A5C4C, 0xDCD2BE, 0x524638, BANDS, 1.4f, 0x8A7A62, 0xB0A690, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.ARID));
        l.add(sp("paralititan", "Paralititan stromeri", TITANOSAUR, 20f).shortName("Paralititan").real(20000, 3.4f).shoulder(3.5f).speed(1.5f, 2.5f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(1, 3, 1).age("Late Cretaceous, 99-93.5 million years ago").site(BAHARIYA)
                .note("'Tidal giant': it lived among mangroves on a coast, alongside Spinosaurus.")
                .body(b -> { b.skull = 0.62f; b.legW = 1.0f; b.frontW = 0.9f; b.width = 2.45f; b.depth = 2.8f; b.torso = 4.3f; b.neckD = 1.0f; })
                .look(look(0x8E846E, 0x6C6452, 0xDAD2BE, 0x5A5242, MOTTLED, 1.8f, 0x8A7A62, 0xB0A690, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("magyarosaurus", "Magyarosaurus dacus", TITANOSAUR, 6f).shortName("Magyarosaurus").real(1000, 1.2f).shoulder(1.25f).speed(1.2f, 3f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, 70-66 million years ago").site(HATEG)
                .note("An island dwarf: a titanosaur hardly bigger than a horse, from the Hateg island of Cretaceous Europe.")
                .look(look(0x8A7E6A, 0x686050, 0xDCD4C2, 0x524A3C, MOTTLED, 1.2f, 0x8A7A62, 0xB0A690, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.TROPICAL));
        l.add(sp("ampelosaurus", "Ampelosaurus atacis", TITANOSAUR, 16f).shortName("Ampelosaurus").real(8000, 2.75f).shoulder(2.85f).speed(1.4f, 3f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(2, 5, 2).age("Late Cretaceous, 71-70 million years ago").site(AUDE)
                .note("'Vineyard lizard', for the vines around the dig. An armoured titanosaur: its back carried bony plates and spines.")
                .body(b -> { b.armour = 0.12f; b.armourCols = 3; })
                .look(look(0x8E7A64, 0x6C5C4A, 0xDCD0BC, 0x544636, BANDS, 1.4f, 0x8A7A62, 0xB0A690, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("phuwiangosaurus", "Phuwiangosaurus sirindhornae", TITANOSAUR, 19f).shortName("Phuwiangosaurus").real(17000, 3.2f).shoulder(3.4f).speed(1.4f, 3f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(2, 6, 2).age("Early Cretaceous, about 134-132 million years ago").site(SAO_KHUA)
                .note("Thailand's best-known sauropod, named in honour of Princess Maha Chakri Sirindhorn.")
                .look(look(0x8A806C, 0x686052, 0xDAD4C2, 0x524C3E, BANDS, 1.5f, 0x8A7A62, 0xB0A690, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.WARM));
        l.add(sp("malawisaurus", "Malawisaurus dixeyi", TITANOSAUR, 16f).shortName("Malawisaurus").real(10000, 2.8f).shoulder(2.9f).speed(1.4f, 3f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(2, 5, 2).age("Early Cretaceous, 121-113 million years ago").site(KARONGA)
                .note("One of the few titanosaurs known from its skull, and probably armoured with bony plates.")
                .body(b -> { b.armour = 0.08f; b.armourCols = 3; })
                .look(look(0x92806A, 0x706050, 0xDED4C2, 0x56483A, MOTTLED, 1.4f, 0x8A7A62, 0xB0A690, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.WARM));
        l.add(sp("diamantinasaurus", "Diamantinasaurus matildae", TITANOSAUR, 16f).shortName("Diamantinasaurus").real(15000, 2.9f).shoulder(3.0f).speed(1.4f, 3f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(2, 5, 2).age("Late Cretaceous, 95 million years ago").site(WINTON)
                .note("Found beside the hunter Australovenator. Its species name honours Waltzing Matilda, the song Banjo Paterson began writing in Winton in 1895.")
                .look(look(0x96826A, 0x726250, 0xE0D6C4, 0x584A3A, BANDS, 1.4f, 0x8A7A62, 0xB0A690, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE | DinoSpecies.TROPICAL));

        // ---- duckbills, iguanodonts and small plant-eaters
        l.add(sp("kamuysaurus", "Kamuysaurus japonicus", HADROSAUR, 8f).shortName("Kamuysaurus").real(5300, 2.3f).shoulder(1.7f).speed(1.5f, 8f)
                .temper(SKITTISH, KICK, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, 72 million years ago").site(HAKOBUCHI)
                .note("One of Japan's most complete dinosaur skeletons, found in rocks laid down on the sea floor: its body likely drifted out to sea. Its name joins kamuy, Ainu for 'deity', with Greek sauros.")
                .body(b -> { b.skull = 0.95f; b.crest = Body.Crest.COMB; b.crestH = 0.06f; b.crestL = 0.25f; })
                .look(look(0x6E7462, 0x52584A, 0xDAD8C8, 0x3E4436, MOTTLED, 1f, 0x9A5A3A, 0xD0C8A8, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(BELLOW)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM | DinoSpecies.COLD));
        l.add(sp("fukuisaurus", "Fukuisaurus tetoriensis", IGUANODONT, 4.5f).shortName("Fukuisaurus").real(400, 1.25f).shoulder(0.95f).speed(1.4f, 6f)
                .temper(SKITTISH, KICK, HERBIVORE).group(2, 6, 3).age("Early Cretaceous, about 121-113 million years ago").site(KITADANI)
                .note("Its describers found its upper jaw was fixed to its skull: unlike its cousins', it couldn't flex outward as it chewed.")
                .look(look(0x8A7E5E, 0x6A5E44, 0xE0D8BE, 0x4E4430, BANDS, 1f, 0x9A7A4E, 0xD8CCA8, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(BELLOW)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("hadrosaurus", "Hadrosaurus foulkii", HADROSAUR, 7.5f).shortName("Hadrosaurus").real(2500, 2.1f).shoulder(1.5f).speed(1.5f, 8f)
                .temper(SKITTISH, KICK, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, 80-78 million years ago").site(WOODBURY)
                .note("The first dinosaur skeleton identified in North America, dug from a marl pit at Haddonfield, New Jersey, in 1858. In 1868 it became the first dinosaur skeleton ever mounted for display.")
                .body(b -> { b.skull = 0.85f; })
                .look(look(0x7A7058, 0x5A5242, 0xDCD6C0, 0x423C2E, STRIPES, 1.1f, 0x9A5A3A, 0xD8C8A8, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(BELLOW)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("telmatosaurus", "Telmatosaurus transsylvanicus", HADROSAUR, 5f).shortName("Telmatosaurus").real(600, 1.4f).shoulder(1.0f).speed(1.4f, 7f)
                .temper(SKITTISH, KICK, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, 70-66 million years ago").site(HATEG)
                .note("'Marsh lizard': a small, primitive duckbill from the islands of Cretaceous Europe.")
                .body(b -> { b.skull = 0.55f; })
                .look(look(0x727A5E, 0x565E46, 0xDCDCC6, 0x3C4232, BANDS, 1f, 0x8A6A44, 0xD0C8A8, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(BELLOW)
                .climates(DinoSpecies.WARM | DinoSpecies.TROPICAL));
        l.add(sp("tethyshadros", "Tethyshadros insularis", HADROSAUR, 4f).shortName("Tethyshadros").real(350, 1.3f).shoulder(0.95f).speed(1.5f, 8f)
                .temper(SKITTISH, KICK, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, 81.5-80.5 million years ago").site(TRIESTE)
                .note("Italy's most complete dinosaur skeleton, nicknamed Antonio: a small duckbill cousin from an island in the Tethys Sea.")
                .body(b -> { b.skull = 0.4f; })
                .look(look(0x7E7460, 0x5E5648, 0xDED8C6, 0x443E32, SADDLES, 1f, 0x9A6A44, 0xD8CCA8, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(BELLOW)
                .climates(DinoSpecies.WARM | DinoSpecies.TROPICAL));
        l.add(sp("tsintaosaurus", "Tsintaosaurus spinorhinus", HADROSAUR, 8.3f).shortName("Tsintaosaurus").real(2500, 2.3f).shoulder(1.65f).speed(1.5f, 8f)
                .temper(SKITTISH, KICK, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, 77-73.5 million years ago").site(JINGANGKOU)
                .note("Long pictured with a single unicorn-like spike; a 2013 restudy showed the spike was the back of a tall, hollow crest that began at the tip of the snout.")
                .body(b -> { b.skull = 0.8f; b.crest = Body.Crest.HATCHET; b.crestH = 0.42f; b.crestL = 0.18f; })
                .look(look(0x7A7260, 0x5A5446, 0xDCD6C4, 0x423E32, STRIPES, 1.1f, 0xC8582E, 0xF0D8A0, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(HORN)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("hypacrosaurus", "Hypacrosaurus altispinus and H. stebingeri", HADROSAUR, 9f).shortName("Hypacrosaurus").real(4000, 2.5f).shoulder(1.8f).speed(1.5f, 8f)
                .temper(SKITTISH, KICK, HERBIVORE).group(4, 10, 4).age("Late Cretaceous, 75-69.5 million years ago").site(HORSESHOE_CANYON, TWO_MEDICINE)
                .note("A helmet-crested duckbill with tall spines along its back. Nests of H. stebingeri, with eggs and babies, are known.")
                .body(b -> { b.skull = 0.85f; b.crest = Body.Crest.HELMET; b.crestH = 0.25f; b.crestL = 0.45f; b.sailH = 0.45f; b.sailFrom = 0.1f; b.sailTo = 1f; b.sailOnTail = 0.4f; })
                .look(look(0x6E6A52, 0x525040, 0xDAD8C2, 0x3A3A2C, STRIPES, 1.2f, 0x3E6A9A, 0xE8D8A0, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(HORN)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM | DinoSpecies.COLD));
        l.add(sp("zalmoxes", "Zalmoxes robustus", SMALL_ORNITHOPOD, 2.5f).shortName("Zalmoxes").real(45, 0.6f).speed(1.3f, 6f)
                .temper(SKITTISH, KICK, HERBIVORE).group(2, 6, 3).age("Late Cretaceous, 70-66 million years ago").site(HATEG)
                .note("A stocky plant-eater of the Hateg island, named after Zalmoxis, a god of the ancient Dacians.")
                .body(b -> { b.skull = 0.3f; b.skullH = 0.55f; b.legW = 0.16f; b.depth = 0.38f; b.width = 0.3f; })
                .look(look(0x7E7A5A, 0x5E5A42, 0xE0DCC2, 0x444030, MOTTLED, 0.9f, 0x8A7A54, 0xD8D0B0, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(SMALL_HERB)
                .climates(DinoSpecies.WARM | DinoSpecies.TROPICAL));
        l.add(sp("rhabdodon", "Rhabdodon priscus", SMALL_ORNITHOPOD, 4f).shortName("Rhabdodon").real(250, 1.0f).speed(1.3f, 6f)
                .temper(SKITTISH, KICK, HERBIVORE).group(2, 6, 3).age("Late Cretaceous, 74-66 million years ago").site(AUDE, FUVEAU)
                .note("Named for its fluted, rod-like ridges on its teeth.")
                .body(b -> { b.skull = 0.4f; b.legW = 0.22f; b.depth = 0.6f; b.width = 0.45f; })
                .look(look(0x86805E, 0x666044, 0xE0DCC0, 0x4A4430, BANDS, 1f, 0x9A7A4E, 0xD8D0AC, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(SMALL_HERB)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("kulindadromeus", "Kulindadromeus zabaikalicus", SMALL_ORNITHOPOD, 1.5f).shortName("Kulindadromeus").real(2, 0.35f).speed(1.3f, 7f)
                .temper(SKITTISH, KICK, HERBIVORE).group(3, 10, 4).age("Middle Jurassic, 168-166 million years ago").site(KULINDA)
                .note("A plant-eater with scales plus filaments its describers read as simple feathers (others suggest skin fibres). If they are feathers, perhaps most dinosaurs were a little fuzzy.")
                .body(b -> { b.feathers = true; b.tail = 0.6f * 1.5f; })
                .look(look(0x6E6250, 0x4E4438, 0xDCD4C2, 0x2E2820, STRIPES, 0.7f, 0x8A6A44, 0xDCD4C2, 0xD0A030, 0xBDB19A, 0x3A3530)).voice(SMALL_HERB)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.COLD));

        // ---- armoured, spiked and horned
        l.add(sp("hungarosaurus", "Hungarosaurus tormai", NODOSAUR, 4.5f).shortName("Hungarosaurus").real(650, 1.0f).shoulder(0.85f).speed(1.0f, 3f)
                .temper(DEFENDER, GORE, HERBIVORE).group(1, 2, 2).age("Late Cretaceous, 85 million years ago").site(IHARKUT)
                .note("Hungary's best-known dinosaur: a lightly built armoured dinosaur with long legs for its kind.")
                .body(b -> { b.shoulderSpikeL = 0.3f; })
                .look(look(0x7E6E56, 0x5E5040, 0xD8CCB2, 0x3E3428, MOTTLED, 1f, 0x8A7458, 0x5E5040, 0x2E1E0E, 0xD7C8A8, 0x2E2A25)).voice(ANKY)
                .climates(DinoSpecies.WARM | DinoSpecies.TROPICAL));
        l.add(sp("gigantspinosaurus", "Gigantspinosaurus sichuanensis", STEGOSAUR, 4.2f).shortName("Gigantspinosaurus").real(700, 1.15f).shoulder(0.8f).speed(1.1f, 2.5f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(1, 3, 2).age("Late Jurassic, 160 million years ago").site(SHAXIMIAO)
                .note("Its shoulder spikes were twice as long as its shoulder blades.")
                .body(b -> { b.plates = 12; b.plateH = 0.12f; b.shoulderSpikeL = 0.9f; b.shoulderSpikeA = -55; b.skull = 0.32f; b.tailSpikes = 4; b.tailSpikeL = 0.35f; })
                .look(look(0x86744E, 0x665638, 0xDCD0AE, 0x4A3E28, MOTTLED, 1f, 0x9A5A30, 0x665638, 0x2A1A0A, 0xD9CDB1, 0x2E2A25)).voice(STEGO)
                .climates(DinoSpecies.WARM | DinoSpecies.TROPICAL));
        l.add(sp("pentaceratops", "Pentaceratops sternbergii", CERATOPSIAN, 5.5f).shortName("Pentaceratops").real(2500, 1.5f).shoulder(1.25f).speed(1.3f, 6f)
                .temper(DEFENDER, GORE, HERBIVORE).group(2, 6, 2).age("Late Cretaceous, 76-73 million years ago").site(SAN_JUAN)
                .note("'Five-horned face': two over the eyes, one on the nose and a spike on each cheek. Its skull and frill were among the longest of any land animal's.")
                .body(b -> { b.skull = 0.8f; b.frillH = 1.15f; b.frillW = 1.2f; b.frillUp = 52; b.frillSpikes = 8; b.frillSpikeL = 0.1f; b.hornBrow = 0.63f; b.hornBrowUp = 30; b.hornNasal = 0.13f; b.hornCheek = 0.19f; b.torso = 1.95f; b.depth = 1.27f; b.width = 1.1f; })
                .look(look(0x82705A, 0x605244, 0xD8CCB2, 0x463A2E, SADDLES, 1.1f, 0xC0602E, 0xF0D8A0, 0x2E1E0E, 0xE3D6B8, 0x2E2A25)).voice(CERATOPS)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("regaliceratops", "Regaliceratops peterhewsi", CERATOPSIAN, 5f).shortName("Regaliceratops").aka("hellboy").real(2000, 1.35f).shoulder(1.1f).speed(1.3f, 6f)
                .temper(DEFENDER, GORE, HERBIVORE).group(2, 6, 2).age("Late Cretaceous, 68 million years ago").site(ST_MARY_RIVER)
                .note("'Royal horned face', for the crown of big plates around its frill. Nicknamed Hellboy for its stubby brow horns and how hard it was to dig out.")
                .body(b -> { b.skull = 0.6f; b.frillH = 0.5f; b.frillW = 0.9f; b.frillUp = 65; b.frillSpikes = 10; b.frillSpikeL = 0.2f; b.hornNasal = 0.4f; b.hornNasalUp = 70; b.hornBrow = 0.08f; })
                .look(look(0x7A6650, 0x5A4A3C, 0xD6C8AE, 0x3E3226, BANDS, 1f, 0xB0402A, 0xF0D898, 0x2E1E0E, 0xE3D6B8, 0x2E2A25)).voice(CERATOPS)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));

        // ================================================================ batch 5 (0.7.4); numbers researched with sources first

        // ---- theropods
        l.add(sp("anzu", "Anzu wyliei", OVIRAPTOR, 3.5f).shortName("Anzu").real(250, 1.5f).speed(1.5f, 9f)
                .temper(DEFENDER, KICK, OMNIVORE).group(1, 3, 2).age("Late Cretaceous, 67-66 million years ago").site(HELL_CREEK)
                .note("Nicknamed the chicken from hell; named after Anzû, a feathered demon of Mesopotamian myth. It had a tall crest like a cassowary's.")
                .body(b -> { b.crest = Body.Crest.ROUND; b.crestH = 0.18f; b.crestL = 0.2f; b.armL = 0.9f; b.armW = 0.07f; b.clawL = 0.08f; })
                .look(look(0x3A3430, 0x262220, 0xCFC6BA, 0x5A4E44, BARS, 1.1f, 0xC0402E, 0x4A7AA8, 0xE0B030, 0x2E2A26, 0x2B2420)).voice(BIRD)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("limusaurus", "Limusaurus inextricabilis", SMALL_THEROPOD, 1.7f).shortName("Limusaurus").real(15, 0.5f).speed(1.4f, 8f)
                .temper(SKITTISH, KICK, HERBIVORE).group(3, 8, 3).age("Late Jurassic, 161-157 million years ago").site(SHISHUGOU)
                .note("'Mud lizard': its skeletons lie in mud pits, perhaps churned by giant sauropod footprints. It lost its teeth as it grew up.")
                .body(b -> { b.skull = 0.15f; b.neck = 0.3f; b.armL = 0.15f; b.armW = 0.02f; b.clawL = 0.01f; })
                .look(look(0xA88E68, 0x7E6A4C, 0xEAE0CC, 0x56462E, STRIPES, 0.8f, 0x9A6A3A, 0xEAE0CC, 0xE0B030, 0xD8CDB2, 0x2B2420)).voice(CHIRP)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE | DinoSpecies.ARID));
        l.add(sp("sinosaurus", "Sinosaurus triassicus", THEROPOD, 5.85f).shortName("Sinosaurus").real(895, 1.5f).speed(1.5f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 2, 2).age("Early Jurassic, 201-191 million years ago").site(LUFENG)
                .note("Named 'triassicus' because it was first thought to be Triassic. Two thin crests ran along its snout; one jaw shows lost teeth that grew back after an injury.")
                .body(b -> { b.skull = 0.525f; b.skullW = 0.28f; b.crest = Body.Crest.TWIN; b.crestH = 0.12f; b.crestL = 0.35f; b.armL = 0.8f; b.armW = 0.08f; b.clawL = 0.08f; })
                .look(look(0x8E7656, 0x665440, 0xE0D2B6, 0x423424, STRIPES, 1f, 0xC8582E, 0xF0C870, 0xE0A020, 0xD8CDB2, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.TROPICAL));
        l.add(sp("yangchuanosaurus", "Yangchuanosaurus shangyouensis", THEROPOD, 11f).shortName("Yangchuanosaurus").real(3000, 2.7f).speed(1.4f, 6.5f)
                .temper(APEX, BITE, CARNIVORE).group(1, 1, 1).age("Late Jurassic, 161-157 million years ago").site(SHAXIMIAO)
                .note("Found in 1977 by a worker building the Shangyou dam. A ridge ran along its nose, with small hornlets.")
                .body(b -> { b.skull = 1.1f; b.skullH = 0.45f; b.crest = Body.Crest.RIDGE; b.crestH = 0.06f; b.crestL = 0.5f; b.hornBrow = 0.06f; b.hornBrowUp = 70; b.hornBrowOut = 10;
                    b.armL = 1.1f; b.armW = 0.13f; b.clawL = 0.13f; b.legW = 0.65f; })
                .look(look(0x7A5E48, 0x564232, 0xDCC8AA, 0x36281C, STRIPES, 1.3f, 0xA8402A, 0xD8C8A8, 0xE0A020, 0xD8CDB2, 0x2B2420)).voice(BIG_ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.TROPICAL));
        l.add(sp("sinraptor", "Sinraptor dongi", THEROPOD, 8f).shortName("Sinraptor").real(1300, 2.0f).speed(1.5f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Late Jurassic, 161-157 million years ago").site(SHISHUGOU)
                .note("'Chinese robber', though no raptor. Its skull carries healed bite wounds, probably from another Sinraptor.")
                .body(b -> { b.skull = 0.9f; b.skullH = 0.4f; b.skullW = 0.3f; b.armL = 0.9f; b.armW = 0.1f; b.clawL = 0.1f; })
                .look(look(0x9A7656, 0x725640, 0xE2D0B4, 0x4A3424, SADDLES, 1.1f, 0xB0482E, 0xE2D0B4, 0xE0A020, 0xD8CDB2, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID | DinoSpecies.TEMPERATE));
        l.add(sp("monolophosaurus", "Monolophosaurus jiangi", THEROPOD, 5.5f).shortName("Monolophosaurus").real(475, 1.4f).speed(1.5f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Middle Jurassic, 168-163 million years ago").site(SHISHUGOU)
                .note("'Single-crested lizard': one tall crest ran from its nostrils to its eyes. Found in 1981 during oil exploration.")
                .body(b -> { b.skull = 0.8f; b.skullH = 0.4f; b.skullW = 0.3f; b.crest = Body.Crest.RIDGE; b.crestH = 0.12f; b.crestL = 0.6f; b.armL = 0.65f; b.armW = 0.07f; })
                .look(look(0xA07E5A, 0x765C42, 0xE6D6BA, 0x4A3826, BANDS, 1f, 0xD0602E, 0xF0D090, 0xE0A020, 0xD8CDB2, 0x2B2420)).voice(GROWL)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID | DinoSpecies.TEMPERATE));
        l.add(sp("megaraptor", "Megaraptor namunhuaiquii", THEROPOD, 8f).shortName("Megaraptor").real(1000, 2.2f).speed(1.6f, 9f)
                .temper(HUNTER, CLAW, CARNIVORE).group(1, 1, 1).age("Late Cretaceous, 90-88 million years ago").site(PORTEZUELO)
                .note("Its giant thumb claw was first thought to be a raptor's foot claw, until a hand turned up in 2004.")
                .body(b -> { b.skull = 0.8f; b.skullW = 0.28f; b.snout = 0.55f; b.armL = 1.5f; b.armW = 0.12f; b.clawL = 0.4f; b.shinF = 1.1f; b.metaF = 0.6f; })
                .look(look(0x8C6E52, 0x644E3A, 0xE0CEB0, 0x3E2E20, STRIPES, 1.1f, 0xB0502E, 0xE0CEB0, 0xE0A020, 0xD8CDB2, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("tyrannotitan", "Tyrannotitan chubutensis", THEROPOD, 13f).shortName("Tyrannotitan").real(7000, 3.2f).speed(1.4f, 6f)
                .temper(APEX, BITE, CARNIVORE).group(1, 1, 1).age("Early Cretaceous, 113-100.5 million years ago").site(CERRO_BARCINO)
                .note("'Tyrant titan': a giant hunter as big as Giganotosaurus, known from two partial skeletons.")
                .body(b -> { b.skull = 1.4f; b.skullW = 0.32f; b.skullH = 0.42f; b.snout = 0.5f; b.armL = 1.1f; b.armW = 0.16f; b.clawL = 0.12f; b.legW = 0.8f; })
                .look(look(0x8A7462, 0x62523F, 0xDCCDB6, 0x40342A, SADDLES, 1.3f, 0x9A4A3A, 0xCFC5B0, 0xD9A02A, 0xD8CDB2, 0x2B2420)).voice(BIG_ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("aucasaurus", "Aucasaurus garridoi", THEROPOD, 5.5f).shortName("Aucasaurus").real(700, 1.5f).speed(1.6f, 9f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Late Cretaceous, 83.5-79.5 million years ago").site(AUCA_MAHUEVO)
                .note("Found at Auca Mahuevo, a vast titanosaur nesting ground. It had low ridges over its eyes instead of horns.")
                .body(b -> { b.skull = 0.55f; b.skullH = 0.55f; b.skullW = 0.45f; b.snout = 0.38f; b.hornBrow = 0.05f; b.hornBrowUp = 60; b.hornBrowOut = 30; b.armL = 0.3f; b.armW = 0.07f; b.legW = 0.4f; })
                .look(look(0x9A6448, 0x704630, 0xE0C8A8, 0x4A2C1C, SPOTS, 0.9f, 0x3A2A22, 0xC9A07A, 0xE0A020, 0x4A3A30, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("buitreraptor", "Buitreraptor gonzalezorum", RAPTOR, 1.5f).shortName("Buitreraptor").real(3, 0.35f).speed(1.3f, 8f)
                .temper(SKULKER, BITE, CARNIVORE).group(1, 3, 2).age("Late Cretaceous, 98-97 million years ago").site(LA_BUITRERA)
                .note("'Vulture raider', after La Buitrera, the 'vulture roost' where it was found. A long, flat snout full of small teeth.")
                .body(b -> { b.skull = 0.2f; b.skullH = 0.28f; b.snout = 0.65f; b.snoutW = 0.45f; b.armL = 0.45f; b.sickle = 0.03f; })
                .look(look(0x8A7056, 0x62503E, 0xE4D8C4, 0x3A2E22, BARS, 0.8f, 0x8A4A2E, 0xEDE4D6, 0xE0A520, 0x2B2420, 0x1E1A16)).voice(RAPTOR_CALL)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("austroraptor", "Austroraptor cabazai", RAPTOR, 6f).shortName("Austroraptor").real(300, 1.4f).speed(1.4f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 2, 2).age("Late Cretaceous, 78-66 million years ago").site(ALLEN)
                .note("A giant southern raptor with short arms and a long, low snout of cone-shaped teeth, perhaps for catching fish.")
                .body(b -> { b.skull = 0.8f; b.skullH = 0.28f; b.snout = 0.6f; b.snoutW = 0.5f; b.armL = 0.6f; b.armW = 0.07f; b.sickle = 0.12f; })
                .look(look(0x6E6250, 0x4C4438, 0xDCD4C2, 0x2C2620, BARS, 1.2f, 0x9A4A2E, 0xE8E0D2, 0xE0A520, 0x2B2420, 0x1E1A16)).voice(RAPTOR_CALL)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("sinornithosaurus", "Sinornithosaurus millenii", RAPTOR, 1.2f).shortName("Sinornithosaurus").real(4, 0.3f).speed(1.3f, 8f)
                .temper(SKULKER, BITE, CARNIVORE).group(1, 3, 2).age("Early Cretaceous, 125 million years ago").site(LIAONING)
                .note("The first feathered dromaeosaur found (1999). Its feathers hint at black and reddish colours.")
                .look(look(0x3A2E2A, 0x241C1A, 0x8A6A58, 0x9A4A2A, BARS, 0.7f, 0x9A4A2A, 0xC8A088, 0xE0A520, 0x2B2420, 0x1E1A16)).voice(RAPTOR_CALL)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("saurornitholestes", "Saurornitholestes langstoni and S. sullivani", RAPTOR, 1.8f).shortName("Saurornitholestes").real(10, 0.6f).speed(1.4f, 9f)
                .temper(PACK, BITE, CARNIVORE).group(1, 3, 2).age("Late Cretaceous, 77-74 million years ago").site(DINOSAUR_PARK, TWO_MEDICINE, SAN_JUAN)
                .note("The commonest small hunter of Dinosaur Provincial Park. One of its teeth was found stuck in a giant pterosaur's wing bone.")
                .look(look(0x8A7860, 0x645646, 0xE8E0D0, 0x3A3026, BARS, 1f, 0x6A4A32, 0xEDE6DA, 0xE0A520, 0x2B2420, 0x1E1A16)).voice(RAPTOR_CALL)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("mononykus", "Mononykus olecranus", SMALL_THEROPOD, 1f).shortName("Mononykus").real(3.5f, 0.35f).speed(1.4f, 9f)
                .temper(SKULKER, BITE, CARNIVORE).group(1, 3, 2).age("Late Cretaceous, 70 million years ago").site(NEMEGT)
                .note("'One claw': each stubby arm ended in a single big claw, perhaps for breaking into insect nests.")
                .body(b -> { b.feathers = true; b.skull = 0.06f; b.armL = 0.08f; b.armW = 0.02f; b.clawL = 0.075f; b.shinF = 1.25f; b.metaF = 0.75f; })
                .look(look(0xB89E7A, 0x8E765A, 0xF2EADA, 0x5A4836, PLAIN, 1f, 0x6A5440, 0xF2EADA, 0xE0B030, 0xC9BBA0, 0x2B2420)).voice(CHIRP)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("halszkaraptor", "Halszkaraptor escuilliei", RAPTOR, 0.6f).shortName("Halszkaraptor").real(1, 0.2f).speed(1.0f, 6f)
                .temper(SKULKER, BITE, CARNIVORE).group(1, 3, 2).age("Late Cretaceous, 75-71 million years ago").site(DJADOCHTA)
                .note("A mallard-sized raptor with a swan-like neck, a flat snout and flipper-like arms; maybe a swimmer, though that is debated. Smuggled out of Mongolia, then returned.")
                .body(b -> { b.skull = 0.07f; b.snout = 0.6f; b.snoutW = 0.8f; b.snoutH = 0.5f; b.neck = 0.2f; b.neckUp = 45; b.neckS = 50; b.armL = 0.1f; b.sickle = 0.01f; })
                .look(look(0x5A5450, 0x3A3634, 0xE6E2DA, 0x1E1C1A, BARS, 0.6f, 0x2A2A2A, 0xF2F0EA, 0xE0B030, 0x2B2420, 0x1E1A16)).voice(CHIRP)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("beipiaosaurus", "Beipiaosaurus inexpectus", THERIZINOSAUR, 1.8f).shortName("Beipiaosaurus").real(50, 0.6f).speed(1.0f, 4f)
                .temper(DEFENDER, CLAW, HERBIVORE).group(1, 2, 2).age("Early Cretaceous, 125 million years ago").site(LIAONING)
                .note("A feathered cousin of Therizinosaurus, with long stiff quills over a downy coat. Found in 1996 by a farmer, Li Yinxian.")
                .look(look(0x6E5440, 0x4E3C2E, 0xC8B096, 0x3A2A1E, PLAIN, 1f, 0xD8C8A8, 0x2E2218, 0x2A1A0A, 0x9A8A70, 0xE8E0CC)).voice(HOOT)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("nothronychus", "Nothronychus mckinleyi and N. graffami", THERIZINOSAUR, 4.2f).shortName("Nothronychus").real(800, 1.3f).speed(1.0f, 3.5f)
                .temper(DEFENDER, CLAW, HERBIVORE).group(1, 1, 1).age("Late Cretaceous, 93.5-88.6 million years ago").site(MORENO_HILL, TROPIC_SHALE)
                .note("'Slothful claw', the first therizinosaur found in the Americas. One skeleton turned up in seabed rocks far from the old shore.")
                .body(b -> { b.clawL = 0.3f; b.armL = 1.2f; b.armW = 0.12f; })
                .look(look(0x7A5E44, 0x584432, 0xCCB494, 0x40301F, PLAIN, 1f, 0xD8C8A8, 0x30241A, 0x2A1A0A, 0x9A8A70, 0xE8E0CC)).voice(HOOT)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("pelecanimimus", "Pelecanimimus polyodon", ORNITHOMIMID, 2.5f).shortName("Pelecanimimus").real(25, 0.75f).speed(1.6f, 11f)
                .temper(SKITTISH, KICK, OMNIVORE).group(2, 6, 3).age("Early Cretaceous, 127-125 million years ago").site(LAS_HOYAS)
                .note("'Pelican mimic': a toothy ornithomimid with about 220 tiny teeth and a throat pouch, preserved with its soft tissues.")
                .body(b -> { b.crest = Body.Crest.COMB; b.crestH = 0.03f; b.crestL = 0.08f; b.wattle = 0.06f; })
                .look(look(0xA8987C, 0x7E705A, 0xF0E8DA, 0x4A4034, PLAIN, 1f, 0xC86A3A, 0xF0E8DA, 0xD08A20, 0x8A7A60, 0x2B2420)).voice(BIRD)
                .climates(DinoSpecies.WARM | DinoSpecies.TROPICAL));
        l.add(sp("lythronax", "Lythronax argestes", THEROPOD, 5f).shortName("Lythronax").real(500, 1.4f).speed(1.5f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Late Cretaceous, 81.9-81.5 million years ago").site(WAHWEAP)
                .note("'Gore king': one of the oldest known tyrannosaurids, with a wide skull and forward-facing eyes.")
                .body(b -> { b.skull = 0.6f; b.skullW = 0.45f; b.skullH = 0.5f; b.snout = 0.38f; b.armL = 0.35f; b.armW = 0.05f; })
                .look(look(0x8E6A50, 0x664C3A, 0xDCC6A8, 0x3E2A1E, STRIPES, 1.1f, 0xA8402E, 0xDCC6A8, 0xE0A020, 0xCFC2A6, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("teratophoneus", "Teratophoneus curriei", THEROPOD, 8f).shortName("Teratophoneus").real(2500, 2.2f).speed(1.5f, 7.5f)
                .temper(APEX, BITE, CARNIVORE).group(1, 2, 1).age("Late Cretaceous, 76.5-75.5 million years ago").site(KAIPAROWITS)
                .note("'Monster murderer'. A quarry in Utah holds four or five of them, aged 4 to 22, that died together.")
                .body(b -> { b.skull = 0.9f; b.skullH = 0.5f; b.skullW = 0.42f; b.snout = 0.4f; b.armL = 0.6f; b.armW = 0.08f; b.legW = 0.55f; })
                .look(look(0x7A5E4A, 0x544032, 0xD8C6AA, 0x34261C, SADDLES, 1.2f, 0x9A3A2A, 0xD8C6AA, 0xE0A020, 0xCFC2A6, 0x2B2420)).voice(BIG_ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("nanuqsaurus", "Nanuqsaurus hoglundi", THEROPOD, 6f).shortName("Nanuqsaurus").real(700, 1.7f).speed(1.5f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 1).age("Late Cretaceous, 73-69 million years ago").site(PRINCE_CREEK)
                .note("'Polar bear lizard', from the Iñupiaq nanuq. A far-northern tyrannosaur that lived through months of winter darkness.")
                .body(b -> { b.skull = 0.65f; b.armL = 0.4f; b.armW = 0.06f; })
                .look(look(0x8A7C6C, 0x645A4E, 0xE6E0D6, 0x3E3830, MOTTLED, 1f, 0x8A4A3A, 0xE6E0D6, 0xE0A020, 0xCFC2A6, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.COLD | DinoSpecies.TEMPERATE));
        l.add(sp("neovenator", "Neovenator salerii", THEROPOD, 7f).shortName("Neovenator").real(1000, 2.0f).speed(1.5f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Early Cretaceous, 125 million years ago").site(ISLE_OF_WIGHT)
                .note("Found after a cliff fall on the Isle of Wight in 1978. Its skeleton shows healed fractures and an old bone infection.")
                .body(b -> { b.skull = 0.7f; b.skullW = 0.3f; b.crest = Body.Crest.RIDGE; b.crestH = 0.03f; b.crestL = 0.35f; b.armL = 0.9f; b.armW = 0.1f; b.clawL = 0.12f; })
                .look(look(0x7E6A54, 0x5A4C3C, 0xDCD0BA, 0x3A3026, BANDS, 1.1f, 0xA84A30, 0xDCD0BA, 0xE0A020, 0xD8CDB2, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));

        // ---- long-necks
        l.add(sp("dicraeosaurus", "Dicraeosaurus hansemanni and D. sattleri", DIPLODOCID, 14f).shortName("Dicraeosaurus").real(5000, 2.4f).shoulder(2.1f).speed(1.3f, 3f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(2, 6, 3).age("Late Jurassic, 157-145 million years ago").site(TENDAGURU)
                .note("'Forked lizard': a short-necked sauropod whose Y-shaped spines made a double ridge along its neck and back.")
                .body(b -> { b.neck = 0.2f * 14f; b.neckUp = 15; b.neckSpines = 0.35f; b.pairedSpines = true; b.sailH = 0.45f; b.sailFrom = 0f; b.sailTo = 1f; b.sailOnTail = 0.2f; b.tailTaper = 0.08f; b.tailPow = 1.6f; })
                .look(look(0x8E7E64, 0x6A5C48, 0xDCD2BC, 0x544632, BANDS, 1.3f, 0x8A5A3A, 0xB0A07E, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.WARM));
        l.add(sp("brachytrachelopan", "Brachytrachelopan mesai", DIPLODOCID, 11f).shortName("Brachytrachelopan").real(5000, 2.0f).shoulder(1.8f).speed(1.3f, 3f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(2, 5, 2).age("Late Jurassic, 161-145 million years ago").site(CANADON_CALCAREO)
                .note("'Short-necked Pan': found by a shepherd, Daniel Mesa, looking for lost sheep. The shortest neck of any known sauropod.")
                .body(b -> { b.neck = 0.13f * 11f; b.neckN = 2; b.neckUp = 10; b.neckSpines = 0.35f; b.sailH = 0.3f; b.sailFrom = 0f; b.sailTo = 0.6f; b.tailTaper = 0.08f; b.tailPow = 1.6f; })
                .look(look(0x948268, 0x70604C, 0xE0D6C2, 0x584A38, MOTTLED, 1.2f, 0x8A6A44, 0xB8A888, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("supersaurus", "Supersaurus vivianae", DIPLODOCID, 33f).shortName("Supersaurus").real(35000, 4.5f).shoulder(4.0f).speed(1.5f, 3f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(1, 2, 1).age("Late Jurassic, 153-145 million years ago").site(MORRISON)
                .note("One of the longest dinosaurs known. Named after Vivian Jones, who found the first bones in 1972.")
                .body(b -> { b.neck = 0.42f * 33f; b.neckN = 6; })
                .look(look(0x7E7C6A, 0x5E5E50, 0xD2D2C2, 0x46483C, BANDS, 1.6f, 0x8A7A5A, 0xA8AC98, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID | DinoSpecies.TEMPERATE));
        l.add(sp("omeisaurus", "Omeisaurus junghsiensis, O. tianfuensis and others", DIPLODOCID, 20.2f).shortName("Omeisaurus").real(9800, 3.3f).shoulder(3.0f).speed(1.4f, 3.5f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(2, 5, 2).age("Middle Jurassic, 168-161 million years ago").site(SHAXIMIAO)
                .note("Named after Mount Emei. Its neck was nearly half its length, and a tail club has been credited to it.")
                .body(b -> { b.skull = 0.5f; b.neck = 0.44f * 20.2f; b.neckN = 6; b.neckUp = 26; b.neckS = 0; b.tail = 0.3f * 20.2f; b.tailTaper = 0.15f; b.tailPow = 1.3f; b.clubW = 0.3f; })
                .look(look(0x727A66, 0x56604C, 0xD0D4C0, 0x424C3A, BANDS, 1.4f, 0x8A6A40, 0x9EAA92, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.TROPICAL));
        l.add(sp("futalognkosaurus", "Futalognkosaurus dukei", TITANOSAUR, 24f).shortName("Futalognkosaurus").real(29500, 3.6f).shoulder(3.7f).speed(1.5f, 2.5f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(1, 3, 1).age("Late Cretaceous, 90-88 million years ago").site(PORTEZUELO)
                .note("'Giant chief lizard' in the Mapuche language. Its neck had tall, shark-fin spines; about 70 percent of the skeleton was found.")
                .body(b -> { b.skull = 0.75f; b.neckSpines = 0.6f; b.neckD = 1.0f; b.legW = 1.1f; b.frontW = 1.0f; b.width = 2.9f; b.depth = 3.2f; b.torso = 5.0f; })
                .look(look(0x8A826E, 0x6A6454, 0xD8D2C0, 0x585244, MOTTLED, 1.7f, 0x8A7A62, 0xB0A690, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("lufengosaurus", "Lufengosaurus huenei", PROSAUROPOD, 9f).shortName("Lufengosaurus").real(1700, 2.0f).speed(1.4f, 5f)
                .temper(SKITTISH, CLAW, HERBIVORE).group(3, 8, 3).age("Early Jurassic, 201-191 million years ago").site(LUFENG)
                .note("In 1958 it became the first dinosaur skeleton mounted in China, and the first dinosaur on a postage stamp.")
                .body(b -> { b.clawL = 0.12f; })
                .look(look(0x8A7652, 0x66563A, 0xE0D4B4, 0x483C28, BANDS, 1.1f, 0xA0603A, 0xD8C8A0, 0xD0A030, 0xD8CDB2, 0x2B2420)).voice(BELLOW)
                .climates(DinoSpecies.WARM | DinoSpecies.TROPICAL));

        // ---- plant-eaters on two legs
        l.add(sp("scutellosaurus", "Scutellosaurus lawleri", SMALL_ORNITHOPOD, 1.3f).shortName("Scutellosaurus").real(3, 0.3f).speed(1.2f, 7f)
                .temper(SKITTISH, KICK, HERBIVORE).group(2, 5, 3).age("Early Jurassic, about 197-184 million years ago (dating debated)").site(KAYENTA)
                .note("'Little shield lizard': a small runner covered in hundreds of little armour plates. Found in 1971 by an intern, David Lawler.")
                .body(b -> { b.armour = 0.02f; b.armourCols = 4; b.tail = 0.6f * 1.3f; })
                .look(look(0x86745A, 0x645642, 0xE0D6C0, 0x463A2C, MOTTLED, 0.8f, 0x8A7A5A, 0xD8CCB0, 0xD0A030, 0xBDB19A, 0x3A3530)).voice(SMALL_HERB)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("camptosaurus", "Camptosaurus dispar", IGUANODONT, 5f).shortName("Camptosaurus").real(500, 1.4f).shoulder(1.0f).speed(1.4f, 6f)
                .temper(SKITTISH, KICK, HERBIVORE).group(2, 6, 3).age("Late Jurassic, 156-147 million years ago").site(MORRISON)
                .note("First named Camptonotus; renamed in 1885 because a cricket already had the name.")
                .body(b -> { b.thumbSpike = 0.06f; })
                .look(look(0x8A7E5E, 0x6A5E44, 0xE0D8BE, 0x4E4430, STRIPES, 1f, 0x9A7A4E, 0xD8CCA8, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(BELLOW)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID | DinoSpecies.TEMPERATE));
        l.add(sp("dryosaurus", "Dryosaurus altus and D. elderae", SMALL_ORNITHOPOD, 3f).shortName("Dryosaurus").real(100, 0.9f).speed(1.5f, 9f)
                .temper(SKITTISH, KICK, HERBIVORE).group(3, 8, 3).age("Late Jurassic, 156-147 million years ago").site(MORRISON)
                .note("A quick, long-legged plant-eater. A site in Colorado held its embryos, hatchlings and eggshell.")
                .look(look(0x8A8460, 0x686446, 0xE2E0C6, 0x4A4632, STRIPES, 0.9f, 0x9A8A5A, 0xE2DAB0, 0xD0A030, 0xBDB19A, 0x3A3530)).voice(SMALL_HERB)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE | DinoSpecies.ARID));
        l.add(sp("yinlong", "Yinlong downsi", SMALL_ORNITHOPOD, 1.2f).shortName("Yinlong").real(10, 0.35f).speed(1.3f, 7f)
                .temper(SKITTISH, BITE, HERBIVORE).group(2, 6, 3).age("Late Jurassic, 161-156 million years ago").site(SHISHUGOU)
                .note("'Hidden dragon', after Crouching Tiger, Hidden Dragon, filmed near the site: one of the earliest horned dinosaurs, still on two legs.")
                .body(b -> { b.skull = 0.15f; b.dome = 0.015f; })
                .look(look(0x8E7A5C, 0x6A5A42, 0xE2D6BE, 0x4A3E2C, BANDS, 0.8f, 0x9A6A3A, 0xE2D6BE, 0xD0A030, 0xBDB19A, 0x3A3530)).voice(SMALL_HERB)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID | DinoSpecies.TEMPERATE));

        // ---- duckbills
        l.add(sp("prosaurolophus", "Prosaurolophus maximus", HADROSAUR, 8.5f).shortName("Prosaurolophus").real(3000, 2.2f).shoulder(1.6f).speed(1.5f, 8f)
                .temper(SKITTISH, KICK, HERBIVORE).group(4, 10, 4).age("Late Cretaceous, 75.5-74.8 million years ago").site(DINOSAUR_PARK, TWO_MEDICINE)
                .note("'Before Saurolophus', with a small solid crest in front of its eyes. A bonebed in Montana suggests it lived in herds.")
                .body(b -> { b.skull = 0.9f; b.crest = Body.Crest.SPIKE; b.crestH = 0.08f; b.crestL = 0.2f; })
                .look(look(0x767A5E, 0x585E46, 0xDCDCC6, 0x3E4432, STRIPES, 1.1f, 0xA8503A, 0xD8C8A8, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(BELLOW)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));

        // ---- horned
        l.add(sp("achelousaurus", "Achelousaurus horneri", CERATOPSIAN, 6f).shortName("Achelousaurus").real(3000, 1.5f).shoulder(1.25f).speed(1.3f, 6f)
                .temper(DEFENDER, GORE, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, 77-74.8 million years ago").site(TWO_MEDICINE)
                .note("Named for the river god Achelous, who lost a horn to Hercules: it had rough bony bosses where its cousins had horns.")
                .body(b -> { b.skull = 0.75f; b.nasalBoss = 0.2f; b.hornBrow = 0.06f; b.hornBrowUp = 40; b.frillH = 0.6f; b.frillW = 0.9f; b.frillUp = 60; b.frillSpikes = 2; b.frillSpikeL = 0.45f; })
                .look(look(0x76644E, 0x56483A, 0xD6C8AE, 0x3A3026, MOTTLED, 1f, 0x8A4E36, 0xD8C8A8, 0x2E1E0E, 0xD8CCB0, 0x2E2A25)).voice(CERATOPS)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("diabloceratops", "Diabloceratops eatoni", CERATOPSIAN, 4.5f).shortName("Diabloceratops").real(1300, 1.2f).shoulder(1.0f).speed(1.3f, 6f)
                .temper(DEFENDER, GORE, HERBIVORE).group(2, 6, 3).age("Late Cretaceous, 81.4-81 million years ago").site(WAHWEAP)
                .note("'Devil-horned face', for the two long spikes on top of its frill.")
                .body(b -> { b.skull = 0.55f; b.hornNasal = 0.1f; b.hornNasalUp = 60; b.hornBrow = 0.12f; b.frillH = 0.5f; b.frillW = 0.75f; b.frillUp = 62; b.frillSpikes = 2; b.frillSpikeL = 0.55f; })
                .look(look(0x82684E, 0x5E4A38, 0xD8C8AC, 0x40301F, BANDS, 1f, 0xB0402A, 0xF0D898, 0x2E1E0E, 0xE3D6B8, 0x2E2A25)).voice(CERATOPS)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("anchiceratops", "Anchiceratops ornatus", CERATOPSIAN, 4.3f).shortName("Anchiceratops").real(1200, 1.15f).shoulder(0.95f).speed(1.3f, 6f)
                .temper(DEFENDER, GORE, HERBIVORE).group(2, 6, 3).age("Late Cretaceous, 72.5-71 million years ago").site(HORSESHOE_CANYON)
                .note("'Near horned face', with a long frill edged in triangular points. Often found near old seashores, so it may have lived by estuaries (debated).")
                .body(b -> { b.skull = 0.5f; b.hornBrow = 0.4f; b.hornBrowUp = 25; b.hornNasal = 0.1f; b.frillH = 0.75f; b.frillW = 0.75f; b.frillUp = 58; b.frillSpikes = 8; b.frillSpikeL = 0.12f; })
                .look(look(0x7A705A, 0x5A5244, 0xD6CEB4, 0x3E382C, SADDLES, 1f, 0x9A5A36, 0xE6D0A0, 0x2E1E0E, 0xE3D6B8, 0x2E2A25)).voice(CERATOPS)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("leptoceratops", "Leptoceratops gracilis", PROTOCERATOPS, 2f).shortName("Leptoceratops").real(100, 0.6f).shoulder(0.48f).speed(1.3f, 6f)
                .temper(SKITTISH, BITE, HERBIVORE).group(2, 6, 3).age("Late Cretaceous, 67-66 million years ago").site(HELL_CREEK)
                .note("A hornless, nearly frill-less cousin of Triceratops that chewed more like a mammal than its relatives.")
                .body(b -> { b.skull = 0.45f; b.skullH = 0.72f; b.frillH = 0.08f; b.frillW = 0.22f; b.sailOnTail = 0.08f; })
                .look(look(0x8A7A60, 0x665A46, 0xE0D6C0, 0x463C2E, BANDS, 0.9f, 0x8A6A4A, 0xD8CCB0, 0x2E1E0E, 0xD8CCB0, 0x2E2A25)).voice(SMALL_HERB)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.COLD));

        // ---- armoured
        l.add(sp("polacanthus", "Polacanthus foxii", NODOSAUR, 5f).shortName("Polacanthus").real(2000, 1.1f).shoulder(0.95f).speed(1.0f, 2.7f)
                .temper(DEFENDER, GORE, HERBIVORE).group(1, 2, 2).age("Early Cretaceous, 130-125 million years ago").site(ISLE_OF_WIGHT)
                .note("'Many thorns'. Found in 1865 by the Reverend William Fox; it had a solid bony shield over its hips.")
                .body(b -> { b.shoulderSpikeL = 0.4f; b.sideSpikes = 0.15f; b.armour = 0.1f; b.armourCols = 5; })
                .look(look(0x7A6A52, 0x5A4C3A, 0xD2C4A8, 0x3E3226, MOTTLED, 1f, 0x8A7458, 0x5A4C3A, 0x2E1E0E, 0xD7C8A8, 0x2E2A25)).voice(ANKY)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("hylaeosaurus", "Hylaeosaurus armatus", NODOSAUR, 5f).shortName("Hylaeosaurus").real(2000, 1.1f).shoulder(0.95f).speed(1.0f, 2.7f)
                .temper(DEFENDER, GORE, HERBIVORE).group(1, 2, 2).age("Early Cretaceous, 137-136 million years ago").site(TILGATE)
                .note("One of the three dinosaurs Richard Owen used to define Dinosauria in 1842. Its skeleton is still in its block of rock at the Natural History Museum.")
                .body(b -> { b.shoulderSpikeL = 0.43f; b.shoulderSpikeA = -30; })
                .look(look(0x726652, 0x54483A, 0xD0C4A8, 0x3A3024, BANDS, 1f, 0x8A7458, 0x54483A, 0x2E1E0E, 0xD7C8A8, 0x2E2A25)).voice(ANKY)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("saichania", "Saichania chulsanensis", ANKYLOSAUR, 5.2f).shortName("Saichania").real(2000, 1.0f).shoulder(0.85f).speed(1.0f, 2.7f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(1, 2, 2).age("Late Cretaceous, 75-70 million years ago").site(BARUN_GOYOT)
                .note("'Beautiful one', for its fine preservation. Its winding nose passages may have cooled the desert air it breathed.")
                .body(b -> { b.skull = 0.455f; b.clubW = 0.4f; b.hornBrow = 0.08f; b.hornBrowUp = 0; b.hornBrowOut = 60; b.hornBrowZ = 0; b.hornCheek = 0.06f; })
                .look(look(0x9A8466, 0x76644C, 0xE0D2BA, 0x544634, MOTTLED, 1f, 0x8A7458, 0x76644C, 0x2E1E0E, 0xD7C8A8, 0x2E2A25)).voice(ANKY)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("tarchia", "Tarchia kielanae, T. teresae and T. tumanovae", ANKYLOSAUR, 4.5f).shortName("Tarchia").real(1500, 0.9f).shoulder(0.8f).speed(1.0f, 2.7f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(1, 2, 2).age("Late Cretaceous, 73-70 million years ago").site(BARUN_GOYOT, NEMEGT)
                .note("'Brainy one', for a brain once thought bigger than its cousins'. One skull bears Tarbosaurus tooth marks.")
                .body(b -> { b.skull = 0.4f; b.clubW = 0.35f; b.hornBrow = 0.07f; b.hornBrowUp = 0; b.hornBrowOut = 60; b.hornBrowZ = 0; })
                .look(look(0x8C7860, 0x6A5A48, 0xDCCEB6, 0x4A3E30, SPOTS, 1f, 0x8A7458, 0x6A5A48, 0x2E1E0E, 0xD7C8A8, 0x2E2A25)).voice(ANKY)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));

        // ================================================================ batch 6 (0.7.5); researched first, Paul 2016 pairs read from the book's scans

        // ---- theropods
        l.add(sp("dilong", "Dilong paradoxus", SMALL_THEROPOD, 1.3f).shortName("Dilong").real(6, 0.35f).speed(1.3f, 8f)
                .temper(SKULKER, BITE, CARNIVORE).group(1, 3, 2).age("Early Cretaceous, 126 million years ago").site(LIAONING)
                .note("'Emperor dragon': a small early cousin of Tyrannosaurus, the first found with direct evidence of feathers (simple fuzz).")
                .body(b -> { b.feathers = true; b.armL = 0.25f; b.armW = 0.025f; })
                .look(look(0x8A6E52, 0x644E3A, 0xE6D6BE, 0x40301F, BANDS, 0.7f, 0xA8502E, 0xE6D6BE, 0xE0A020, 0xD8CDB2, 0x2B2420)).voice(GROWL)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("zhenyuanlong", "Zhenyuanlong suni", RAPTOR, 2.3f).shortName("Zhenyuanlong").real(8, 0.5f).speed(1.3f, 8f)
                .temper(SKULKER, BITE, CARNIVORE).group(1, 2, 2).age("Early Cretaceous, 125 million years ago").site(LIAONING)
                .note("The biggest raptor known with large feathered wings, though its arms were too short and its body too heavy to fly.")
                .body(b -> { b.armL = 0.45f; })
                .look(look(0x5A4E46, 0x3C3430, 0xD8CEC0, 0x1E1A16, BARS, 1f, 0x4A5A6A, 0xE8E2D8, 0xE0A520, 0x2B2420, 0x1E1A16)).voice(RAPTOR_CALL)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("sinocalliopteryx", "Sinocalliopteryx gigas", SMALL_THEROPOD, 2.3f).shortName("Sinocalliopteryx").real(20, 0.6f).speed(1.4f, 9f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Early Cretaceous, 125 million years ago").site(LIAONING)
                .note("'Chinese beautiful feather'. One skeleton has a raptor's leg in its gut; another, two Confuciusornis birds.")
                .body(b -> { b.feathers = true; b.armL = 0.5f; b.armW = 0.03f; b.clawL = 0.05f; })
                .look(look(0x9A6A46, 0x704C32, 0xE8D6BC, 0x4A3020, STRIPES, 0.8f, 0xC0602E, 0xE8D6BC, 0xE0A020, 0xD8CDB2, 0x2B2420)).voice(GROWL)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("zhuchengtyrannus", "Zhuchengtyrannus magnus", THEROPOD, 11f).shortName("Zhuchengtyrannus").real(6000, 3.3f).speed(1.4f, 6f)
                .temper(APEX, BITE, CARNIVORE).group(1, 1, 1).age("Late Cretaceous, about 73.5 million years ago").site(WANGSHI)
                .note("A tyrannosaur close to the size of T. rex, known only from its jaws, dug from one of the world's densest dinosaur bonebeds.")
                .body(b -> { b.skull = 1.2f; b.skullH = 0.5f; b.skullW = 0.45f; b.armL = 0.9f; b.armW = 0.12f; b.legW = 0.75f; })
                .look(look(0x7A6450, 0x56463A, 0xD8C8AE, 0x382C22, SADDLES, 1.3f, 0x9A3E2A, 0xD8C9A8, 0xE0A020, 0xCFC2A6, 0x2B2420)).voice(BIG_ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("eustreptospondylus", "Eustreptospondylus oxoniensis", THEROPOD, 6f).shortName("Eustreptospondylus").real(500, 1.6f).speed(1.5f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Middle Jurassic, about 162 million years ago").site(OXFORD_CLAY)
                .note("Found near Oxford in 1870, in rocks laid down in the sea, and named only in 1964: 'true Streptospondylus'.")
                .body(b -> { b.skull = 0.7f; b.skullH = 0.4f; b.snout = 0.5f; b.armL = 0.7f; b.armW = 0.07f; })
                .look(look(0x8E765A, 0x665440, 0xE0D2B8, 0x42342A, BANDS, 1f, 0xA84A30, 0xE0D2B8, 0xE0A020, 0xD8CDB2, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("elaphrosaurus", "Elaphrosaurus bambergi", SMALL_THEROPOD, 6f).shortName("Elaphrosaurus").real(200, 1.46f).speed(1.6f, 10f)
                .temper(SKITTISH, KICK, OMNIVORE).group(1, 3, 2).age("Late Jurassic, 157-152 million years ago").site(TENDAGURU)
                .note("'Light-footed lizard': long-legged and shallow-chested. Its skull has never been found.")
                .body(b -> { b.skull = 0.4f; b.neck = 1.0f; b.depth = 0.5f; b.width = 0.4f; b.armL = 0.5f; b.armW = 0.05f; })
                .look(look(0xA88C6A, 0x7E6850, 0xEADCC4, 0x564432, STRIPES, 1f, 0xB86A3A, 0xEADCC4, 0xE0B030, 0xD8CDB2, 0x2B2420)).voice(CHIRP)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.WARM));
        l.add(sp("acheroraptor", "Acheroraptor temertyorum", RAPTOR, 3f).shortName("Acheroraptor").real(40, 0.8f).speed(1.4f, 9f)
                .temper(PACK, BITE, CARNIVORE).group(1, 3, 2).age("Late Cretaceous, 67-66 million years ago").site(HELL_CREEK)
                .note("Named for the Acheron, the 'river of pain', to fit Hell Creek: one of the last raptors, a neighbour of T. rex.")
                .look(look(0x6A5A4A, 0x483C32, 0xDCD2C2, 0x26201A, BARS, 1.1f, 0x8A3A2A, 0xEDE6DA, 0xE0A520, 0x2B2420, 0x1E1A16)).voice(RAPTOR_CALL)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("bambiraptor", "Bambiraptor feinbergi", RAPTOR, 1.3f).shortName("Bambiraptor").real(5, 0.35f).speed(1.3f, 8f)
                .temper(SKULKER, BITE, CARNIVORE).group(1, 2, 2).age("Late Cretaceous, 77-75 million years ago").site(TWO_MEDICINE)
                .note("Found in 1995 by 14-year-old Wes Linster: a youngster's skeleton, about 95 percent complete.")
                .look(look(0x8A7056, 0x62503E, 0xE8DCC8, 0x3A2E22, BARS, 0.8f, 0x9A5A3A, 0xEDE4D6, 0xE0A520, 0x2B2420, 0x1E1A16)).voice(RAPTOR_CALL)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("atrociraptor", "Atrociraptor marshalli", RAPTOR, 2f).shortName("Atrociraptor").real(15, 0.5f).speed(1.4f, 9f)
                .temper(PACK, BITE, CARNIVORE).group(1, 3, 2).age("Late Cretaceous, 72-71.5 million years ago").site(HORSESHOE_CANYON)
                .note("'Savage robber', with an unusually short, deep snout. Found in 1995 by Wayne Marshall.")
                .body(b -> { b.skullH = 0.48f; b.snout = 0.4f; })
                .look(look(0x6E5A48, 0x4A3C30, 0xDCD0BE, 0x2A221A, BARS, 1f, 0x7A3A2A, 0xE8E0D2, 0xE0A520, 0x2B2420, 0x1E1A16)).voice(RAPTOR_CALL)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.COLD));
        l.add(sp("tsaagan", "Tsaagan mangas", RAPTOR, 2f).shortName("Tsaagan").real(18, 0.5f).speed(1.4f, 9f)
                .temper(PACK, BITE, CARNIVORE).group(1, 3, 2).age("Late Cretaceous, about 75 million years ago").site(DJADOCHTA)
                .note("'White monster'. Taken for a Velociraptor until a CT scan in 1998 showed it was something new.")
                .look(look(0xB49A78, 0x8A7458, 0xF0E6D4, 0x5A4632, BARS, 1.1f, 0x6A4A32, 0xF2EEE6, 0xE0A520, 0x2B2420, 0x1E1A16)).voice(RAPTOR_CALL)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("avimimus", "Avimimus portentosus and A. nemegtensis", OVIRAPTOR, 1.2f).shortName("Avimimus").real(12, 0.5f).speed(1.4f, 9f)
                .temper(SKITTISH, KICK, OMNIVORE).group(3, 10, 3).age("Late Cretaceous, 75-70 million years ago").site(DJADOCHTA, NEMEGT)
                .note("'Bird mimic', with a beak, short wings and very long shins. A bonebed of at least ten suggests flocks.")
                .body(b -> { b.skull = 0.08f; b.neck = 0.3f; b.armL = 0.2f; b.shinF = 1.3f; b.metaF = 0.8f; })
                .look(look(0x9A8670, 0x706250, 0xEEE6D8, 0xF4EEE6, BARS, 0.7f, 0xB0402E, 0x3A5A7A, 0xE0B030, 0x2E2A26, 0x2B2420)).voice(BIRD)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("tawa", "Tawa hallae", SMALL_THEROPOD, 2.5f).shortName("Tawa").real(15, 0.6f).speed(1.4f, 9f)
                .temper(SKULKER, BITE, CARNIVORE).group(1, 3, 2).age("Late Triassic, 215-213 million years ago").site(GHOST_RANCH)
                .note("Named for the Hopi sun god. At least eight were found in one quarry at Ghost Ranch.")
                .look(look(0xA07A54, 0x765A3E, 0xE6D6BA, 0x4E3824, STRIPES, 0.8f, 0xC0602E, 0xE6D6BA, 0xE0B030, 0xD8CDB2, 0x2B2420)).voice(GROWL)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("liliensternus", "Liliensternus liliensterni", SMALL_THEROPOD, 5.2f).shortName("Liliensternus").real(130, 1.1f).speed(1.5f, 9f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Late Triassic, about 210 million years ago").site(GLEICHBERG, TROSSINGEN, HALBERSTADT)
                .note("Named for Hugo Rühle von Lilienstern, who kept its bones in his castle museum. Europe's best-known Triassic hunter.")
                .body(b -> { b.neck = 0.8f; b.armL = 0.7f; b.armW = 0.05f; b.clawL = 0.05f; })
                .look(look(0x9A7454, 0x70543C, 0xE4D0B4, 0x4A3220, BANDS, 1f, 0xB8582E, 0xE4D0B4, 0xE0A020, 0xD8CDB2, 0x2B2420)).voice(GROWL)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID | DinoSpecies.TEMPERATE));
        l.add(sp("ekrixinatosaurus", "Ekrixinatosaurus novasi", THEROPOD, 6.5f).shortName("Ekrixinatosaurus").real(800, 1.7f).speed(1.5f, 8f)
                .temper(HUNTER, BITE, CARNIVORE).group(1, 1, 2).age("Late Cretaceous, about 100-97 million years ago").site(NEUQUEN)
                .note("'Explosion-born lizard': its bones were blasted out during work on a gas pipeline.")
                .body(b -> { b.skull = 0.83f; b.skullH = 0.6f; b.skullW = 0.48f; b.snout = 0.36f; b.hornBrow = 0.05f; b.hornBrowUp = 60; b.hornBrowOut = 30; b.armL = 0.3f; b.armW = 0.08f; b.legW = 0.5f; })
                .look(look(0x8E5A40, 0x663E2C, 0xDCC2A2, 0x40261A, SPOTS, 0.9f, 0x3A2A22, 0xC9A07A, 0xE0A020, 0x4A3A30, 0x2B2420)).voice(ROAR)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));

        // ---- long-necks
        l.add(sp("sauroposeidon", "Sauroposeidon proteles", BRACHIOSAUR, 27f).shortName("Sauroposeidon").real(40000, 4.5f).shoulder(6.0f).speed(1.5f, 3f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(1, 2, 1).age("Early Cretaceous, 113-110 million years ago").site(ANTLERS, CLOVERLY)
                .note("Its neck bones were first taken for petrified tree trunks. It held its head about 17 metres up, and is Texas's state dinosaur.")
                .look(look(0x8A8670, 0x6A6654, 0xD8D6C2, 0x585844, BANDS, 1.6f, 0x8A7A5A, 0xB0B098, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE | DinoSpecies.TROPICAL));
        l.add(sp("cetiosaurus", "Cetiosaurus oxoniensis", TITANOSAUR, 16f).shortName("Cetiosaurus").real(11000, 3.2f).shoulder(3.0f).speed(1.4f, 3f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(2, 5, 2).age("Middle Jurassic, 171-165 million years ago").site(FOREST_MARBLE, RUTLAND)
                .note("'Whale lizard': the first long-necked dinosaur named, in 1841, by Richard Owen, who took it for a giant sea reptile.")
                .body(b -> { b.neck = 0.3f * 16f; b.neckUp = 25; })
                .look(look(0x8C8068, 0x6A6050, 0xDAD2BE, 0x544A3A, MOTTLED, 1.4f, 0x8A7A62, 0xB0A690, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("opisthocoelicaudia", "Opisthocoelicaudia skarzynskii", TITANOSAUR, 13f).shortName("Opisthocoelicaudia").real(8500, 2.5f).shoulder(2.0f).speed(1.3f, 3f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(2, 5, 2).age("Late Cretaceous, about 70 million years ago").site(NEMEGT)
                .note("Known from a headless skeleton found lying on its back, with bite marks. Its tail joints may have let it rear up on its tail.")
                .body(b -> { b.armour = 0.08f; b.armourCols = 2; })
                .look(look(0x948066, 0x72604C, 0xDED2BC, 0x544638, BANDS, 1.4f, 0x8A7A62, 0xB0A690, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("barapasaurus", "Barapasaurus tagorei", TITANOSAUR, 12f).shortName("Barapasaurus").real(7000, 3.0f).shoulder(3.0f).speed(1.4f, 3.5f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(2, 6, 3).age("Early Jurassic, 199-184 million years ago (dating debated)").site(KOTA)
                .note("'Big-legged lizard', named in honour of the poet Rabindranath Tagore. About 300 bones from at least six animals were found together.")
                .body(b -> { b.legW = 0.6f; b.frontW = 0.55f; })
                .look(look(0x9A8468, 0x76644E, 0xE2D8C4, 0x5A4C3A, BANDS, 1.3f, 0x8A6A4A, 0xC8B89C, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.TROPICAL | DinoSpecies.WARM));
        l.add(sp("atlasaurus", "Atlasaurus imelakei", BRACHIOSAUR, 15f).shortName("Atlasaurus").real(14000, 3.5f).shoulder(3.8f).speed(1.5f, 3.5f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(1, 3, 2).age("Middle Jurassic, 168-163 million years ago").site(GUETTIOUA)
                .note("Named for the Titan Atlas and the Atlas Mountains: very long legs, a short neck and a big head.")
                .body(b -> { b.neck = 3.86f; b.neckUp = 32; b.skull = 0.8f; })
                .look(look(0x9C826A, 0x786450, 0xE4D8C4, 0x5E4C3A, MOTTLED, 1.3f, 0x8A6A4A, 0xC8B89C, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("riojasaurus", "Riojasaurus incertus", TITANOSAUR, 6.6f).shortName("Riojasaurus").real(800, 1.7f).shoulder(1.6f).speed(1.3f, 4f)
                .temper(SKITTISH, STOMP, HERBIVORE).group(2, 6, 3).age("Late Triassic, 227-213 million years ago").site(LOS_COLORADOS)
                .note("In 1994, 56 of its tail bones were stolen from a university in La Rioja; they are still missing.")
                .body(b -> { b.skull = 0.25f; b.neck = 0.25f * 6.6f; b.neckUp = 20; b.tail = 0.42f * 6.6f; })
                .look(look(0x96805C, 0x726044, 0xE2D6BC, 0x56482E, BANDS, 1f, 0xA0703E, 0xE2D6BC, 0xD0A030, 0xD8CDB2, 0x2B2420)).voice(BELLOW)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("haplocanthosaurus", "Haplocanthosaurus priscus and H. delfsi", TITANOSAUR, 12f).shortName("Haplocanthosaurus").real(5000, 2.7f).shoulder(2.6f).speed(1.4f, 3f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(2, 5, 2).age("Late Jurassic, 155-152 million years ago").site(MORRISON)
                .note("'Simple-spined lizard', for the unforked spines along its backbone.")
                .look(look(0x8A8068, 0x686050, 0xD8D2BE, 0x504A3A, BANDS, 1.4f, 0x8A7A5A, 0xA8A890, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID | DinoSpecies.TEMPERATE));
        l.add(sp("thecodontosaurus", "Thecodontosaurus antiquus", PROSAUROPOD, 2.5f).shortName("Thecodontosaurus").real(20, 0.55f).speed(1.3f, 7f)
                .temper(SKITTISH, BITE, HERBIVORE).group(2, 6, 3).age("Late Triassic, 205-201 million years ago").site(BRISTOL)
                .note("One of the first dinosaurs ever named (1836), from Bristol. Much of its material was destroyed in the Bristol Blitz in 1940.")
                .body(b -> { b.tail = 0.55f * 2.5f; })
                .look(look(0x8E7A58, 0x6A5A40, 0xE2D8BE, 0x4A3E2A, STRIPES, 0.8f, 0xA0603A, 0xE2D8BE, 0xD0A030, 0xD8CDB2, 0x2B2420)).voice(SMALL_HERB)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE | DinoSpecies.ARID));
        l.add(sp("neuquensaurus", "Neuquensaurus australis", TITANOSAUR, 7.5f).shortName("Neuquensaurus").real(1800, 1.4f).shoulder(1.4f).speed(1.3f, 3f)
                .temper(DEFENDER, STOMP, HERBIVORE).group(3, 6, 3).age("Late Cretaceous, 83-79 million years ago").site(CINCO_SALTOS)
                .note("A short-legged titanosaur with bony armour in its skin. Named in 1893 as a Titanosaurus; given its own genus only in 1992.")
                .body(b -> { b.armour = 0.08f; b.armourCols = 3; })
                .look(look(0x8E7E64, 0x6A5C48, 0xDACEB8, 0x544636, MOTTLED, 1.2f, 0x8A7A62, 0x6A5A46, 0x2A2A1A, 0xCFC5B0, 0x3A3530)).voice(SAUROPOD)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));

        // ---- duckbills and small plant-eaters
        l.add(sp("amurosaurus", "Amurosaurus riabinini", HADROSAUR, 8f).shortName("Amurosaurus").real(3000, 2.3f).shoulder(1.65f).speed(1.5f, 8f)
                .temper(SKITTISH, KICK, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, 68-66 million years ago").site(BLAGOVESHCHENSK)
                .note("The most complete dinosaur known from Russia. One forearm bone healed badly from a break, so it walked with a limp.")
                .body(b -> { b.skull = 0.8f; b.crest = Body.Crest.HELMET; b.crestH = 0.15f; b.crestL = 0.3f; })
                .look(look(0x727862, 0x565C4A, 0xDAD8C8, 0x3E4436, MOTTLED, 1f, 0x9A5A3A, 0xD0C8A8, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(HORN)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.COLD));
        l.add(sp("charonosaurus", "Charonosaurus jiayinensis", HADROSAUR, 10f).shortName("Charonosaurus").real(5000, 2.9f).shoulder(2.1f).speed(1.5f, 8f)
                .temper(SKITTISH, KICK, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, 67-66 million years ago").site(JIAYIN)
                .note("Named for Charon, ferryman of the dead: one of the last crested duckbills, probably with a long tube crest like Parasaurolophus's.")
                .body(b -> { b.skull = 1.0f; b.crest = Body.Crest.TUBE; b.crestL = 1.0f; b.crestH = 0.18f; })
                .look(look(0x7A7A5E, 0x5C5E46, 0xDCDCC6, 0x404432, STRIPES, 1.1f, 0xC8572E, 0xF0DFA0, 0x2E2A10, 0xBDB19A, 0x3A3530)).voice(HORN)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.COLD));
        l.add(sp("kritosaurus", "Kritosaurus navajovius", HADROSAUR, 9f).shortName("Kritosaurus").real(4000, 2.6f).shoulder(1.85f).speed(1.5f, 8f)
                .temper(SKITTISH, KICK, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, 73.8-73.5 million years ago").site(SAN_JUAN)
                .note("Found by Barnum Brown in 1904. Its humped 'Roman nose' was first restored flat.")
                .body(b -> { b.skull = 0.88f; b.nasalBoss = 0.15f; })
                .look(look(0x82745A, 0x605644, 0xDED6C0, 0x463E30, BANDS, 1f, 0xA8503A, 0xD8C8A8, 0x2A2A1A, 0xBDB19A, 0x3A3530)).voice(BELLOW)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("gasparinisaura", "Gasparinisaura cincosaltensis", SMALL_ORNITHOPOD, 1.7f).shortName("Gasparinisaura").real(13, 0.45f).speed(1.4f, 8f)
                .temper(SKITTISH, KICK, HERBIVORE).group(3, 8, 3).age("Late Cretaceous, about 83 million years ago").site(CINCO_SALTOS)
                .note("Named for the palaeontologist Zulma Gasparini. One held about 140 polished stomach stones for grinding its food.")
                .look(look(0x8E8260, 0x6A6046, 0xE2DCC2, 0x4A4430, STRIPES, 0.8f, 0x9A8A5A, 0xE2DAB0, 0xD0A030, 0xBDB19A, 0x3A3530)).voice(SMALL_HERB)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("orodromeus", "Orodromeus makelai", SMALL_ORNITHOPOD, 2f).shortName("Orodromeus").real(50, 0.55f).speed(1.5f, 9f)
                .temper(SKITTISH, KICK, HERBIVORE).group(2, 6, 3).age("Late Cretaceous, about 77 million years ago").site(TWO_MEDICINE)
                .note("'Mountain runner', from Egg Mountain in Montana. The eggs once thought to be its turned out to be a troodontid's.")
                .look(look(0x86805E, 0x666044, 0xE0DCC0, 0x4A4430, BANDS, 0.8f, 0x8A7A54, 0xD8D0B0, 0xD0A030, 0xBDB19A, 0x3A3530)).voice(SMALL_HERB)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));

        // ---- horned, plated, armoured, domed
        l.add(sp("utahceratops", "Utahceratops gettyi", CERATOPSIAN, 5f).shortName("Utahceratops").real(2000, 1.5f).shoulder(1.2f).speed(1.3f, 6f)
                .temper(DEFENDER, GORE, HERBIVORE).group(2, 6, 3).age("Late Cretaceous, 76.4-75.5 million years ago").site(KAIPAROWITS)
                .note("Named for Mike Getty, the museum's collections manager, who found it. Its short brow horns stuck out sideways.")
                .body(b -> { b.skull = 0.8f; b.frillH = 1.2f; b.frillW = 1.1f; b.frillUp = 55; b.frillSpikes = 8; b.frillSpikeL = 0.1f; b.hornBrow = 0.12f; b.hornBrowUp = 0; b.hornBrowOut = 70; b.hornNasal = 0.12f; })
                .look(look(0x7E6A54, 0x5E4E3E, 0xD8CAB0, 0x40342A, SADDLES, 1.1f, 0xB0502E, 0xF0D8A0, 0x2E1E0E, 0xE3D6B8, 0x2E2A25)).voice(CERATOPS)
                .climates(DinoSpecies.WARM | DinoSpecies.TEMPERATE));
        l.add(sp("bagaceratops", "Bagaceratops rozhdestvenskyi", PROTOCERATOPS, 0.8f).shortName("Bagaceratops").real(7, 0.25f).shoulder(0.2f).speed(1.1f, 5f)
                .temper(SKITTISH, BITE, HERBIVORE).group(3, 8, 4).age("Late Cretaceous, 72-71 million years ago").site(BARUN_GOYOT, DJADOCHTA)
                .note("'Small-horned face': the commonest animal in the Barun Goyot rocks of the Gobi.")
                .body(b -> { b.hornNasal = 0.02f; b.hornNasalUp = 70; b.frillH = 0.06f; })
                .look(look(0xB0986E, 0x86724E, 0xEADEC2, 0x5A4A30, SPOTS, 0.7f, 0xA06A3A, 0xEADEC2, 0x2E1E0E, 0xD8CCB0, 0x2E2A25)).voice(SMALL_HERB)
                .climates(DinoSpecies.ARID | DinoSpecies.WARM));
        l.add(sp("zuniceratops", "Zuniceratops christopheri", PROTOCERATOPS, 2.2f).shortName("Zuniceratops").real(175, 0.7f).shoulder(0.55f).speed(1.2f, 5.5f)
                .temper(DEFENDER, GORE, HERBIVORE).group(2, 6, 3).age("Late Cretaceous, 90.9-88.6 million years ago").site(MORENO_HILL)
                .note("Found in 1996 by 8-year-old Christopher Wolfe: an early horned dinosaur with horns over its eyes but none on its nose.")
                .body(b -> { b.hornBrow = 0.18f; b.hornBrowUp = 30; b.frillH = 0.25f; })
                .look(look(0x8A7254, 0x66543E, 0xDECCAE, 0x463826, BANDS, 0.9f, 0xA0503A, 0xE0CCA8, 0x2E1E0E, 0xD8CCB0, 0x2E2A25)).voice(CERATOPS)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID));
        l.add(sp("hesperosaurus", "Hesperosaurus mjosi", STEGOSAUR, 6.5f).shortName("Hesperosaurus").real(3500, 1.9f).shoulder(1.2f).speed(1.1f, 2.5f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(1, 3, 2).age("Late Jurassic, about 156 million years ago").site(MORRISON)
                .note("One skeleton, nicknamed Victoria, gave the first direct evidence that stegosaur plates were sheathed in keratin.")
                .body(b -> { b.plates = 14; b.plateH = 0.45f; b.tailSpikes = 4; b.tailSpikeL = 0.6f; b.skull = 0.42f; })
                .look(look(0x7E7A58, 0x5E5A40, 0xDCD8B8, 0x44402C, MOTTLED, 1f, 0xB8582E, 0x5E5A40, 0x2A1A0A, 0xD9CDB1, 0x2E2A25)).voice(STEGO)
                .climates(DinoSpecies.WARM | DinoSpecies.ARID | DinoSpecies.TEMPERATE));
        l.add(sp("wuerhosaurus", "Wuerhosaurus homheni", STEGOSAUR, 7f).shortName("Wuerhosaurus").real(4000, 2.0f).shoulder(1.3f).speed(1.1f, 2.5f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(1, 3, 2).age("Early Cretaceous, 135-130 million years ago").site(LIANMUQIN)
                .note("One of the last stegosaurs, with very broad hips.")
                .body(b -> { b.plates = 12; b.plateH = 0.3f; b.width = 1.3f; b.skull = 0.42f; })
                .look(look(0x86745A, 0x645642, 0xDCD0B4, 0x483C2C, BANDS, 1f, 0x9A5A30, 0x645642, 0x2A1A0A, 0xD9CDB1, 0x2E2A25)).voice(STEGO)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("scolosaurus", "Scolosaurus cutleri and S. thronus", ANKYLOSAUR, 5.6f).shortName("Scolosaurus").real(2200, 1.2f).shoulder(1.0f).speed(1.0f, 2.7f)
                .temper(DEFENDER, TAIL, HERBIVORE).group(1, 1, 1).age("Late Cretaceous, 77-75.5 million years ago").site(DINOSAUR_PARK)
                .note("Its collector, W. E. Cutler, was badly hurt when the specimen fell on him in 1914.")
                .body(b -> { b.skull = 0.5f; b.clubW = 0.45f; b.hornBrow = 0.15f; b.hornBrowUp = -20; b.hornBrowOut = 40; b.hornBrowZ = 0; })
                .look(look(0x86745E, 0x645644, 0xDCCEB6, 0x4A3E30, MOTTLED, 1f, 0x8A7458, 0x645644, 0x2E1E0E, 0xD7C8A8, 0x2E2A25)).voice(ANKY)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));
        l.add(sp("sphaerotholus", "Sphaerotholus goodwini, S. buchholtzae, S. edmontonensis, S. lyonsi and S. triregnum", PACHYCEPHALOSAUR, 2f).shortName("Sphaerotholus").real(40, 0.6f).speed(1.4f, 7f)
                .temper(DEFENDER, HEADBUTT, HERBIVORE).group(1, 4, 2).age("Late Cretaceous, 76-66 million years ago").site(SAN_JUAN, HELL_CREEK, HORSESHOE_CANYON, DINOSAUR_PARK)
                .note("'Ball dome': a dome-head with rows of bony knobs at the back of its skull. The genus lasted seven or eight million years.")
                .body(b -> { b.dome = 0.12f; })
                .look(look(0x8A7860, 0x665846, 0xE2D6C0, 0x463A2C, BANDS, 0.9f, 0x8A6A4A, 0xE2D6C0, 0xD0A030, 0xD8CDB2, 0x2E2A25)).voice(GOAT)
                .climates(DinoSpecies.TEMPERATE | DinoSpecies.WARM));

        ALL = Collections.unmodifiableList(l);
        for (DinoSpecies s : l) if (BY_ID.put(s.id, s) != null) throw new IllegalStateException("two dinosaurs called " + s.id);
    }

    public static DinoSpecies get(String id) { return BY_ID.get(id); }
}
