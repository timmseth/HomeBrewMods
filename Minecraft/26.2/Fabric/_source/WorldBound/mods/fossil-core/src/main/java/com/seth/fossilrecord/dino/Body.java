package com.seth.fossilrecord.dino;

/**
 * A species' build, in metres for a grown animal: the lengths of its skull, neck, ribcage and tail, how high its hips
 * and shoulders stand, how thick its limbs are, and its ornaments (horns, frills, crests, sails, plates, spikes,
 * clubs, feathers). Each body plan starts from proportions measured off its family's skeletons (as fractions of the
 * total length); a species then sets what is its own. {@link Rig} turns this into model geometry, and the server
 * takes its hitbox, eye height and reach from the same numbers.
 */
public final class Body {
    public enum Plan {
        THEROPOD, RAPTOR, ORNITHOMIMID, THERIZINOSAUR, OVIRAPTOR, SPINOSAUR, SMALL_THEROPOD,
        DIPLODOCID, BRACHIOSAUR, TITANOSAUR, PROSAUROPOD,
        HADROSAUR, IGUANODONT, SMALL_ORNITHOPOD,
        CERATOPSIAN, PROTOCERATOPS, STEGOSAUR, ANKYLOSAUR, NODOSAUR, PACHYCEPHALOSAUR
    }

    /** Head crests. */
    public enum Crest { NONE, TUBE, HELMET, HATCHET, TWIN, ROUND, BLADE, COMB, SPIKE, RIDGE }

    public final Plan plan;
    /** Snout to tail tip. */
    public final float L;

    public boolean quad, biped = true;
    /** Hip and (four-legged) shoulder joint heights. */
    public float hip, shoulder;
    /** Ribcage front to hip joint; ribcage depth and width; body pitch (degrees, front up) for upright two-leggers. */
    public float torso, depth, width, pitch;
    /** Head: skull length, height and width (as fractions of the skull length), the share that is snout, its width share, nose-down angle. */
    public float skull, skullH, skullW, snout, snoutW = 0.7f, snoutH = 0.75f, headDown;
    /** Neck: length, thickness, rise above horizontal (degrees), S-curve (base minus head end, degrees), segments. */
    public float neck, neckD, neckUp, neckS;
    public int neckN = 2;
    /** Tail: length (0 = what L leaves), base depth, width/depth ratio, rise of the base, bend toward the tip, tip/base thickness. */
    public float tail, tailD, tailW = 0.8f, tailUp, tailCurve, tailTaper = 0.18f, tailPow = 1f;
    public int tailN = 5;
    /** Hind legs: thigh thickness; segment proportions; segment angles from vertical (degrees, + = lower end forward). */
    public float legW, thighF = 1f, shinF = 1f, metaF = 0.5f, thighA, shinA, metaA, footL, footH, footW = 0.9f;
    /** Arms of two-leggers: length, thickness. Front legs of four-leggers: thickness and segment proportions. */
    public float armL, armW, frontW, upperF = 1f, foreF = 0.85f, handF = 0.3f, upperA = -10f, foreA = 12f;

    // ---- ornaments (metres; 0 = none)
    public float hornNasal, hornNasalUp = 40f, hornBrow, hornBrowUp = 20f, hornBrowOut = 15f, hornCheek;
    /** Where along the cranium the brow horns sit: 0 its back corners, 1 its front. */
    public float hornBrowZ = 0.5f;
    public float frillH, frillW, frillUp = 55f, frillSpikeL;
    public int frillSpikes;
    public Crest crest = Crest.NONE;
    public float crestH, crestL;
    public float dome;
    public float sailH, sailFrom = 0.1f, sailTo = 0.9f, sailOnTail;
    public int plates;
    public float plateH;
    public int tailSpikes;
    public float tailSpikeL, shoulderSpikeL, sideSpikes;
    public float clubW;
    public float thumbSpike, clawL, sickle;
    public boolean feathers, wings, tailFan;
    public float armour, neckSpines, tailFin, nasalBoss, hump, wattle;
    /** Neck spines in two rows leaning apart (Amargasaurus), not one midline row. */
    public boolean pairedSpines;
    /** Long feathers on the hind legs too (Microraptor's four wings). */
    public boolean legWings;
    /** Osteoderm columns across the back (0 = by plan). */
    public int armourCols;
    /** Eye size relative to the usual (big-eyed hunters of the dusk). */
    public float eyeK = 1f;
    /** Shoulder spikes: angle of their points (degrees; + forward, - back, 0 straight out and up). */
    public float shoulderSpikeA = -40f;
    /** Enlarged killing claws per foot (Balaur had two). */
    public int sickles = 1;

    public Body(Plan plan, float L) {
        this.plan = plan;
        this.L = L;
        switch (plan) {
            case THEROPOD -> {
                skull = 0.115f * L; skullH = 0.42f; skullW = 0.34f; snout = 0.45f;
                neck = 0.1f * L; neckD = 0.075f * L; neckUp = 25; neckS = 35;
                torso = 0.24f * L; depth = 0.15f * L; width = 0.105f * L;
                tailD = 0.09f * L; tailUp = -3; tailCurve = 4;
                hip = 0.31f * L; legW = 0.065f * L; shinF = 0.95f; metaF = 0.5f; thighA = 22; shinA = -32; metaA = 22;
                footL = 0.07f * L; footH = 0.012f * L; armL = 0.12f * L; armW = 0.022f * L; clawL = 0.012f * L;
            }
            case SMALL_THEROPOD -> {
                skull = 0.1f * L; skullH = 0.42f; skullW = 0.36f; snout = 0.45f;
                neck = 0.13f * L; neckD = 0.05f * L; neckUp = 35; neckS = 40; neckN = 2;
                torso = 0.2f * L; depth = 0.12f * L; width = 0.085f * L;
                tailD = 0.065f * L; tailUp = 0; tailCurve = 3;
                hip = 0.27f * L; legW = 0.05f * L; shinF = 1.15f; metaF = 0.65f; thighA = 28; shinA = -42; metaA = 28;
                footL = 0.07f * L; footH = 0.012f * L; armL = 0.16f * L; armW = 0.018f * L; clawL = 0.012f * L;
            }
            case RAPTOR -> {
                skull = 0.12f * L; skullH = 0.36f; skullW = 0.32f; snout = 0.5f; snoutW = 0.6f;
                neck = 0.13f * L; neckD = 0.045f * L; neckUp = 40; neckS = 45;
                torso = 0.2f * L; depth = 0.12f * L; width = 0.085f * L;
                tailD = 0.055f * L; tailUp = 2; tailCurve = 0; tailTaper = 0.3f;
                hip = 0.26f * L; legW = 0.05f * L; shinF = 1.15f; metaF = 0.6f; thighA = 30; shinA = -45; metaA = 30;
                footL = 0.06f * L; footH = 0.012f * L; armL = 0.24f * L; armW = 0.017f * L; clawL = 0.015f * L;
                sickle = 0.03f * L; feathers = true; wings = true; tailFan = true;
            }
            case ORNITHOMIMID -> {
                skull = 0.055f * L; skullH = 0.42f; skullW = 0.42f; snout = 0.5f; snoutW = 0.55f;
                neck = 0.2f * L; neckD = 0.035f * L; neckUp = 50; neckS = 55; neckN = 3;
                torso = 0.2f * L; depth = 0.14f * L; width = 0.09f * L;
                tailD = 0.06f * L; tailUp = 0; tailTaper = 0.22f;
                hip = 0.33f * L; legW = 0.05f * L; shinF = 1.15f; metaF = 0.8f; thighA = 28; shinA = -42; metaA = 26;
                footL = 0.05f * L; footH = 0.01f * L; armL = 0.17f * L; armW = 0.016f * L; clawL = 0.015f * L;
                feathers = true; wings = true;
            }
            case THERIZINOSAUR -> {
                skull = 0.055f * L; skullH = 0.45f; skullW = 0.4f; snout = 0.45f; snoutW = 0.6f;
                neck = 0.19f * L; neckD = 0.045f * L; neckUp = 45; neckS = 50; neckN = 3;
                torso = 0.26f * L; depth = 0.2f * L; width = 0.14f * L; pitch = 25;
                tailD = 0.075f * L; tailUp = -5; tailTaper = 0.3f; tailN = 4;
                hip = 0.3f * L; legW = 0.07f * L; shinF = 0.85f; metaF = 0.4f; thighA = 15; shinA = -20; metaA = 18;
                footL = 0.05f * L; footH = 0.012f * L; footW = 1.1f; armL = 0.3f * L; armW = 0.025f * L; clawL = 0.07f * L;
                feathers = true;
            }
            case OVIRAPTOR -> {
                skull = 0.095f * L; skullH = 0.7f; skullW = 0.42f; snout = 0.35f; snoutW = 0.7f; snoutH = 0.8f;
                neck = 0.18f * L; neckD = 0.045f * L; neckUp = 45; neckS = 45;
                torso = 0.22f * L; depth = 0.15f * L; width = 0.1f * L;
                tailD = 0.06f * L; tailUp = 0; tailTaper = 0.35f; tailN = 4;
                hip = 0.3f * L; legW = 0.06f * L; shinF = 1.1f; metaF = 0.6f; thighA = 26; shinA = -40; metaA = 26;
                footL = 0.06f * L; footH = 0.012f * L; armL = 0.22f * L; armW = 0.018f * L; clawL = 0.015f * L;
                feathers = true; wings = true; tailFan = true;
            }
            case SPINOSAUR -> {
                skull = 0.12f * L; skullH = 0.3f; skullW = 0.22f; snout = 0.62f; snoutW = 0.6f; snoutH = 0.6f;
                neck = 0.13f * L; neckD = 0.05f * L; neckUp = 30; neckS = 30;
                torso = 0.21f * L; depth = 0.12f * L; width = 0.085f * L;
                tailD = 0.075f * L; tailW = 0.35f; tailUp = -4; tailCurve = 2; tailTaper = 0.35f; tailN = 6;
                hip = 0.19f * L; legW = 0.05f * L; shinF = 0.9f; metaF = 0.45f; thighA = 20; shinA = -30; metaA = 22;
                footL = 0.06f * L; footH = 0.01f * L; armL = 0.15f * L; armW = 0.025f * L; clawL = 0.03f * L;
            }
            case DIPLODOCID -> {
                quad = true; biped = false;
                skull = 0.025f * L; skullH = 0.42f; skullW = 0.5f; snout = 0.4f; snoutW = 0.9f;
                neck = 0.29f * L; neckD = 0.035f * L; neckUp = 18; neckS = -8; neckN = 5;
                torso = 0.17f * L; depth = 0.11f * L; width = 0.085f * L;
                tailD = 0.06f * L; tailW = 0.85f; tailUp = -16; tailCurve = 18; tailTaper = 0.03f; tailPow = 2.2f; tailN = 7;
                hip = 0.145f * L; shoulder = 0.125f * L; legW = 0.045f * L; shinF = 0.68f; metaF = 0.18f; thighA = 4; shinA = -3; metaA = 12;
                footL = 0.03f * L; footH = 0.008f * L; footW = 1.15f; frontW = 0.036f * L; upperF = 1f; foreF = 0.75f; handF = 0.35f; upperA = -6; foreA = 6;
            }
            case BRACHIOSAUR -> {
                quad = true; biped = false;
                skull = 0.04f * L; skullH = 0.55f; skullW = 0.42f; snout = 0.45f; snoutW = 0.85f; snoutH = 0.65f;
                neck = 0.42f * L; neckD = 0.048f * L; neckUp = 58; neckS = 4; neckN = 5;
                torso = 0.26f * L; depth = 0.155f * L; width = 0.11f * L;
                tailD = 0.07f * L; tailUp = -12; tailCurve = 8; tailTaper = 0.1f; tailPow = 1.4f; tailN = 5;
                hip = 0.195f * L; shoulder = 0.23f * L; legW = 0.055f * L; shinF = 0.65f; metaF = 0.18f; thighA = 4; shinA = -3; metaA = 12;
                footL = 0.04f * L; footH = 0.008f * L; footW = 1.15f; frontW = 0.052f * L; upperF = 1f; foreF = 0.8f; handF = 0.4f; upperA = -5; foreA = 5;
            }
            case TITANOSAUR -> {
                quad = true; biped = false;
                skull = 0.03f * L; skullH = 0.48f; skullW = 0.5f; snout = 0.45f; snoutW = 0.85f;
                neck = 0.32f * L; neckD = 0.04f * L; neckUp = 28; neckS = 6; neckN = 5;
                torso = 0.2f * L; depth = 0.13f * L; width = 0.12f * L;
                tailD = 0.065f * L; tailUp = -10; tailCurve = 10; tailTaper = 0.06f; tailPow = 1.6f; tailN = 6;
                hip = 0.16f * L; shoulder = 0.165f * L; legW = 0.055f * L; shinF = 0.65f; metaF = 0.18f; thighA = 4; shinA = -3; metaA = 12;
                footL = 0.04f * L; footH = 0.008f * L; footW = 1.15f; frontW = 0.05f * L; upperF = 1f; foreF = 0.8f; handF = 0.38f; upperA = -6; foreA = 6;
            }
            case PROSAUROPOD -> {
                skull = 0.04f * L; skullH = 0.45f; skullW = 0.42f; snout = 0.45f;
                neck = 0.22f * L; neckD = 0.045f * L; neckUp = 30; neckS = 25; neckN = 3;
                torso = 0.25f * L; depth = 0.15f * L; width = 0.1f * L;
                tailD = 0.07f * L; tailUp = -2; tailCurve = 2; tailTaper = 0.15f; tailN = 6;
                hip = 0.27f * L; legW = 0.06f * L; shinF = 0.85f; metaF = 0.45f; thighA = 18; shinA = -25; metaA = 20;
                footL = 0.06f * L; footH = 0.012f * L; armL = 0.2f * L; armW = 0.025f * L; clawL = 0.025f * L;
            }
            case HADROSAUR -> {
                quad = true;
                skull = 0.105f * L; skullH = 0.42f; skullW = 0.32f; snout = 0.48f; snoutW = 0.95f; snoutH = 0.55f; headDown = 12;
                neck = 0.11f * L; neckD = 0.06f * L; neckUp = 32; neckS = 40;
                torso = 0.27f * L; depth = 0.16f * L; width = 0.1f * L;
                tailD = 0.1f * L; tailW = 0.45f; tailUp = 2; tailCurve = -4; tailTaper = 0.2f; tailN = 5;
                hip = 0.29f * L; shoulder = 0.2f * L; legW = 0.065f * L; shinF = 0.95f; metaF = 0.42f; thighA = 18; shinA = -28; metaA = 26;
                footL = 0.045f * L; footH = 0.012f * L; frontW = 0.03f * L; upperF = 1f; foreF = 0.9f; handF = 0.38f; upperA = -12; foreA = 10;
            }
            case IGUANODONT -> {
                quad = true;
                skull = 0.085f * L; skullH = 0.48f; skullW = 0.34f; snout = 0.45f; snoutW = 0.75f; snoutH = 0.65f; headDown = 10;
                neck = 0.11f * L; neckD = 0.06f * L; neckUp = 30; neckS = 35;
                torso = 0.27f * L; depth = 0.16f * L; width = 0.1f * L;
                tailD = 0.095f * L; tailW = 0.5f; tailUp = 0; tailCurve = -2; tailTaper = 0.2f; tailN = 5;
                hip = 0.28f * L; shoulder = 0.21f * L; legW = 0.07f * L; shinF = 0.9f; metaF = 0.4f; thighA = 16; shinA = -24; metaA = 24;
                footL = 0.045f * L; footH = 0.012f * L; frontW = 0.035f * L; upperF = 1f; foreF = 0.85f; handF = 0.35f; upperA = -12; foreA = 10;
                thumbSpike = 0.025f * L;
            }
            case SMALL_ORNITHOPOD -> {
                skull = 0.08f * L; skullH = 0.5f; skullW = 0.4f; snout = 0.45f; snoutW = 0.65f;
                neck = 0.1f * L; neckD = 0.045f * L; neckUp = 30; neckS = 35;
                torso = 0.22f * L; depth = 0.13f * L; width = 0.09f * L;
                tailD = 0.06f * L; tailUp = 2; tailTaper = 0.15f; tailN = 5;
                hip = 0.27f * L; legW = 0.055f * L; shinF = 1.15f; metaF = 0.6f; thighA = 26; shinA = -40; metaA = 26;
                footL = 0.055f * L; footH = 0.01f * L; armL = 0.12f * L; armW = 0.018f * L; clawL = 0.008f * L;
            }
            case CERATOPSIAN -> {
                quad = true; biped = false;
                skull = 0.17f * L; skullH = 0.55f; skullW = 0.48f; snout = 0.42f; snoutW = 0.62f; snoutH = 0.8f; headDown = 18;
                neck = 0.05f * L; neckD = 0.12f * L; neckUp = 5; neckS = 0; neckN = 1;
                torso = 0.33f * L; depth = 0.22f * L; width = 0.18f * L;
                tail = 0.3f * L; tailD = 0.1f * L; tailW = 0.75f; tailUp = -10; tailCurve = 4; tailTaper = 0.22f; tailN = 4;
                hip = 0.245f * L; shoulder = 0.19f * L; legW = 0.075f * L; shinF = 0.8f; metaF = 0.3f; thighA = 10; shinA = -10; metaA = 20;
                footL = 0.05f * L; footH = 0.012f * L; footW = 1.1f; frontW = 0.065f * L; upperF = 1f; foreF = 0.8f; handF = 0.3f; upperA = -14; foreA = 14;
                frillH = 0.11f * L; frillW = 0.17f * L;
            }
            case PROTOCERATOPS -> {
                quad = true; biped = false;
                skull = 0.2f * L; skullH = 0.62f; skullW = 0.44f; snout = 0.42f; snoutW = 0.6f; snoutH = 0.85f; headDown = 15;
                neck = 0.05f * L; neckD = 0.11f * L; neckUp = 8; neckS = 0; neckN = 1;
                torso = 0.3f * L; depth = 0.18f * L; width = 0.13f * L;
                tail = 0.36f * L; tailD = 0.11f * L; tailW = 0.45f; tailUp = -4; tailCurve = 2; tailTaper = 0.25f; tailN = 4;
                hip = 0.26f * L; shoulder = 0.21f * L; legW = 0.07f * L; shinF = 0.95f; metaF = 0.4f; thighA = 16; shinA = -18; metaA = 22;
                footL = 0.055f * L; footH = 0.012f * L; frontW = 0.055f * L; upperF = 1f; foreF = 0.85f; handF = 0.3f; upperA = -14; foreA = 14;
                frillH = 0.1f * L; frillW = 0.15f * L; frillUp = 50;
            }
            case STEGOSAUR -> {
                quad = true; biped = false;
                skull = 0.05f * L; skullH = 0.42f; skullW = 0.42f; snout = 0.5f; snoutW = 0.7f; headDown = 8;
                neck = 0.12f * L; neckD = 0.06f * L; neckUp = -12; neckS = -6; neckN = 2;
                torso = 0.3f * L; depth = 0.19f * L; width = 0.12f * L;
                tailD = 0.085f * L; tailW = 0.75f; tailUp = 8; tailCurve = -6; tailTaper = 0.18f; tailN = 5;
                hip = 0.28f * L; shoulder = 0.17f * L; legW = 0.065f * L; shinF = 0.7f; metaF = 0.25f; thighA = 6; shinA = -6; metaA = 16;
                footL = 0.04f * L; footH = 0.01f * L; footW = 1.1f; frontW = 0.05f * L; upperF = 1f; foreF = 0.8f; handF = 0.28f; upperA = -10; foreA = 10;
                plates = 17; plateH = 0.08f * L; tailSpikes = 4; tailSpikeL = 0.09f * L;
            }
            case ANKYLOSAUR -> {
                quad = true; biped = false;
                skull = 0.085f * L; skullH = 0.52f; skullW = 1.05f; snout = 0.3f; snoutW = 0.7f; snoutH = 0.8f; headDown = 6;
                neck = 0.05f * L; neckD = 0.09f * L; neckUp = 2; neckS = 0; neckN = 1;
                torso = 0.38f * L; depth = 0.15f * L; width = 0.24f * L;
                tailD = 0.06f * L; tailW = 0.85f; tailUp = -2; tailCurve = 2; tailTaper = 0.45f; tailN = 4;
                hip = 0.19f * L; shoulder = 0.16f * L; legW = 0.065f * L; shinF = 0.65f; metaF = 0.25f; thighA = 6; shinA = -6; metaA = 16;
                footL = 0.04f * L; footH = 0.01f * L; footW = 1.15f; frontW = 0.055f * L; upperF = 1f; foreF = 0.8f; handF = 0.28f; upperA = -12; foreA = 12;
                clubW = 0.09f * L; armour = 0.022f * L; sideSpikes = 0.02f * L;
            }
            case NODOSAUR -> {
                quad = true; biped = false;
                skull = 0.08f * L; skullH = 0.5f; skullW = 0.75f; snout = 0.4f; snoutW = 0.7f; snoutH = 0.8f; headDown = 8;
                neck = 0.08f * L; neckD = 0.08f * L; neckUp = 4; neckS = 0; neckN = 1;
                torso = 0.36f * L; depth = 0.15f * L; width = 0.2f * L;
                tailD = 0.065f * L; tailW = 0.85f; tailUp = -3; tailCurve = 2; tailTaper = 0.15f; tailN = 4;
                hip = 0.2f * L; shoulder = 0.17f * L; legW = 0.065f * L; shinF = 0.65f; metaF = 0.25f; thighA = 6; shinA = -6; metaA = 16;
                footL = 0.04f * L; footH = 0.01f * L; footW = 1.15f; frontW = 0.055f * L; upperF = 1f; foreF = 0.8f; handF = 0.28f; upperA = -12; foreA = 12;
                armour = 0.02f * L; shoulderSpikeL = 0.06f * L; sideSpikes = 0.02f * L;
            }
            case PACHYCEPHALOSAUR -> {
                skull = 0.13f * L; skullH = 0.62f; skullW = 0.52f; snout = 0.38f; snoutW = 0.6f; snoutH = 0.65f;
                neck = 0.1f * L; neckD = 0.06f * L; neckUp = 30; neckS = 40;
                torso = 0.24f * L; depth = 0.15f * L; width = 0.11f * L;
                tailD = 0.075f * L; tailUp = 0; tailCurve = 2; tailTaper = 0.2f; tailN = 5;
                hip = 0.27f * L; legW = 0.06f * L; shinF = 1.0f; metaF = 0.5f; thighA = 24; shinA = -36; metaA = 24;
                footL = 0.055f * L; footH = 0.01f * L; armL = 0.1f * L; armW = 0.018f * L; clawL = 0.008f * L;
                dome = 0.06f * L;
            }
        }
        if (frontW == 0) frontW = legW * 0.6f;
    }
}
