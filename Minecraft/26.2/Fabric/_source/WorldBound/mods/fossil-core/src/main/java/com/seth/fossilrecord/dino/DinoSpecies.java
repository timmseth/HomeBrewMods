package com.seth.fossilrecord.dino;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * One real dinosaur: its size (length, mass, hip height from published estimates for a grown animal), how it moves
 * and behaves, what it eats, where its fossils come from (the places it lives in an Earth world), its build
 * ({@link Body}), colours ({@link Look}) and voice. Built with the chained setters in {@link Roster}.
 */
public final class DinoSpecies {
    /** How it treats others: big hunters, mid-sized hunters, pack hunters, small skulkers, herbivores that stand and fight, herbivores that run. */
    public enum Temper { APEX, HUNTER, PACK, SKULKER, DEFENDER, SKITTISH }

    /** Its blow: a bite, a horn charge, a tail swing (club, spikes, whip), a head-butt, claws, a kick, a stamp. */
    public enum Attack { BITE, GORE, TAIL, HEADBUTT, CLAW, KICK, STOMP }

    public enum Diet { CARNIVORE, PISCIVORE, OMNIVORE, HERBIVORE }

    /** Skin markings. */
    public enum Pattern { PLAIN, STRIPES, SPOTS, BANDS, MOTTLED, SADDLES, BARS, TAIL_BANDS }

    /** Climate groups for generated planets (Earthish), as a bit set. */
    public static final int TROPICAL = 1, ARID = 2, WARM = 4, TEMPERATE = 8, COLD = 16;

    /** A fossil locality or formation, as a latitude/longitude box. */
    public record Site(String name, double lat0, double lat1, double lon0, double lon1) {
        public boolean contains(double lat, double lon) { return lat >= lat0 && lat <= lat1 && lon >= lon0 && lon <= lon1; }
        public double lat() { return (lat0 + lat1) / 2; }
        public double lon() { return (lon0 + lon1) / 2; }
    }

    /** Colours (0xRRGGBB): flank, back, belly, markings, ornaments (frill/crest/sail), second ornament colour, eye, horn/beak, claws. */
    public record Look(int base, int back, int belly, int mark, Pattern pattern, float markScale, int accent, int accent2,
                       int eye, int horn, int claw) {}

    /** Vanilla sound events (pitched) standing in for its calls. */
    public record Voice(String verb, String ambient, float ambientPitch, String hurt, float hurtPitch, String death, float deathPitch,
                        String attack, float attackPitch, String roar, float roarPitch, float volume) {}

    public final String id, name;
    public String shortName, age = "", note = "", aka = "";
    public Body body;
    public Temper temper = Temper.SKITTISH;
    public Attack attack = Attack.BITE;
    public Diet diet = Diet.HERBIVORE;
    /** Kilograms, metres per second (an easy walk and a flat-out run, from published estimates). */
    public float mass, walk = 1.4f, run = 5f;
    public int groupMin = 1, groupMax = 1, weight = 2;
    public final List<Site> sites = new ArrayList<>();
    public Look look;
    public Voice voice;
    /** Swims well and keeps near water (spinosaurids). */
    public boolean aquatic;
    /** Climate groups it takes to on a generated planet. */
    public int climates = WARM | TEMPERATE;
    /** Egg colours (spawn egg item). */
    public int eggBase, eggSpot;

    private Rig rig;

    DinoSpecies(String id, String name, Body.Plan plan, float length) {
        this.id = id;
        this.name = name;
        this.shortName = name.contains(" ") ? name.substring(0, name.indexOf(' ')) : name;
        this.body = new Body(plan, length);
    }

    // ---- chained setters for the roster

    DinoSpecies real(float massKg, float hipM) { mass = massKg; body.hip = hipM; return this; }
    DinoSpecies shoulder(float m) { body.shoulder = m; return this; }
    DinoSpecies speed(float walkMs, float runMs) { walk = walkMs; run = runMs; return this; }
    DinoSpecies temper(Temper t, Attack a, Diet d) { temper = t; attack = a; diet = d; return this; }
    DinoSpecies group(int min, int max, int w) { groupMin = min; groupMax = max; weight = w; return this; }
    DinoSpecies age(String s) { age = s; return this; }
    DinoSpecies note(String s) { note = s; return this; }
    DinoSpecies site(Site... s) { sites.addAll(List.of(s)); return this; }
    DinoSpecies body(Consumer<Body> f) { f.accept(body); return this; }
    DinoSpecies look(Look l) { look = l; eggBase = l.base(); eggSpot = l.mark(); return this; }
    DinoSpecies voice(Voice v) { voice = v; return this; }
    DinoSpecies aquatic() { aquatic = true; return this; }
    DinoSpecies climates(int c) { climates = c; return this; }
    DinoSpecies shortName(String s) { shortName = s; return this; }
    /** Another name it goes by (an older or popular name); /earth fauna commands accept it. */
    DinoSpecies aka(String s) { aka = s; return this; }

    public Rig rig() {
        if (rig == null) rig = new Rig(body);
        return rig;
    }

    // ---- what follows from the numbers

    public boolean carnivore() { return diet == Diet.CARNIVORE || diet == Diet.PISCIVORE; }

    /** The raptors (dromaeosaurs and their kin): tamed with meat, follow and defend their owner, hatch tame to whoever hatches them. */
    public boolean tameable() { return body.plan == Body.Plan.RAPTOR; }

    /** Fights at all: hunters, and plant-eaters that stand their ground (the runners never do). */
    public boolean fights() { return temper != Temper.SKITTISH; }

    /** Health: grows as mass^0.36 (a 15 kg raptor ~16, a cow-sized animal ~70, T. rex ~150, a 70 t titanosaur ~330). */
    public float health() { return Math.max(4f, Math.min(500f, Math.round(6f * (float) Math.pow(mass, 0.36)))); }

    /** Damage of one blow. Hunters bite harder for their size than plant-eaters strike. */
    public float damage() {
        float k = carnivore() ? 1.15f : 0.75f;
        if (attack == Attack.TAIL || attack == Attack.GORE || attack == Attack.STOMP) k = Math.max(k, 0.95f);
        return Math.max(1f, Math.min(28f, Math.round(1.5f + k * (float) Math.pow(mass, 0.33))));
    }

    public float knockback() {
        float base = switch (attack) { case GORE, HEADBUTT -> 1.6f; case TAIL, STOMP -> 1.4f; default -> 0.5f; };
        return base * Math.max(0.2f, Math.min(2.5f, (float) Math.log10(Math.max(2, mass)) / 2.2f));
    }

    public float knockbackResistance() { return Math.max(0f, Math.min(1f, (float) Math.log10(Math.max(1, mass)) / 4.3f)); }

    public float armor() {
        return switch (body.plan) {
            case ANKYLOSAUR, NODOSAUR -> 14f;
            case STEGOSAUR -> 6f;
            case CERATOPSIAN -> 6f;
            case DIPLODOCID, BRACHIOSAUR, TITANOSAUR -> 6f;
            case PROTOCERATOPS, PACHYCEPHALOSAUR -> 3f;
            default -> mass > 1000 ? 3f : 0f;
        };
    }

    /**
     * Movement attribute for its walk. A mob on ordinary ground settles at about 44 * s^2 blocks per second when its
     * speed is s (attribute times goal modifier), measured for every species walking and running on a test pad
     * (/earth fauna dinospeed), so s = sqrt(v / 44).
     */
    public float speedAttribute() { return (float) Math.sqrt(Math.max(0.3f, walk) / 43.9f); }

    /** Goal speed modifier that gives its running speed. */
    public float runModifier() { return (float) Math.sqrt(Math.max(walk, run) / Math.max(0.3f, walk)); }

    /** Step height, blocks: long legs step up a block or two without jumping. */
    public float stepHeight() { return Math.max(0.6f, Math.min(2.5f, body.hip * 0.55f)); }

    /** Bigger animals carry their calls further. */
    public float volume() { return Math.max(0.5f, Math.min(4f, 0.6f + (float) Math.log10(Math.max(1, mass)) * 0.55f)) * (voice == null ? 1 : voice.volume()); }

    /** Grows up over 20 minutes for the smallest, up to about an hour and a half for the giants. */
    public int growTicks() { return (int) (24000 * (1 + Math.max(0, Math.log10(Math.max(1, mass) / 30.0)))); }

    /** How small a hatchling is drawn, relative to the adult (about half a metre long, never under an eighth). */
    public float hatchlingScale() { return Math.max(0.12f, Math.min(0.5f, 0.55f / body.L * 1.6f)); }

    /** The heaviest prey it goes for; the lightest worth its while. */
    public float maxPrey() {
        return switch (temper) { case APEX -> mass * 1.2f; case HUNTER -> mass * 0.7f; case PACK -> mass * 5f; case SKULKER -> mass * 1.2f; default -> 0; };
    }

    public float minPrey() { return temper == Temper.APEX ? mass * 0.004f : temper == Temper.HUNTER ? mass * 0.01f : 0f; }

    /** Is this point on Earth one of its fossil sites? */
    public boolean ranges(double lat, double lon) {
        for (Site s : sites) if (s.contains(lat, lon)) return true;
        return false;
    }

    @Override public String toString() { return id; }
}
