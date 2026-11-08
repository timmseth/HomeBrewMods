package com.seth.fossilrecord;

/**
 * Where each animal lives: its real range on Earth, and the ground it wants inside that range (Köppen class, land
 * cover, height). Pure functions of the data the Earth layers give for a column, so they are tested without a world.
 * On a generated planet (Earthish) there is no Australia or Amazon, so only the ground counts there.
 */
public final class Habitat {
    private Habitat() {}

    // Köppen-Geiger classes as the climate layer stores them (Beck et al. numbering)
    static final int AF = 1, AM = 2, AW = 3, BWH = 4, BWK = 5, BSH = 6, BSK = 7, CSA = 8, CSB = 9, CWA = 11, CWB = 12,
            CFA = 14, CFB = 15;
    // ESA WorldCover classes as the land-cover layer stores them
    static final int TREE = 10, SHRUB = 20, GRASS = 30, CROP = 40, BUILT = 50, BARE = 60, SNOW = 70, WATER = 80, WETLAND = 90,
            MANGROVE = 95, MOSS = 100;

    public static final int RED = 0, GREY = 1;

    /** Mainland Australia and Tasmania (south of New Guinea, west of New Zealand). */
    public static boolean australia(double lat, double lon) {
        if (lat >= -39.3 && lat <= -10.6 && lon >= 112.9 && lon <= 153.7) return true;
        return lat >= -43.7 && lat < -39.3 && lon >= 143.8 && lon <= 148.5;         // Tasmania and the Bass Strait islands
    }

    /**
     * The green anaconda's range: the Amazon and Orinoco basins and the Guianas, east of the Andes (the Pacific
     * lowlands of Colombia and Ecuador are left out), down to the Pantanal's northern edge.
     */
    public static boolean amazonia(double lat, double lon) {
        if (lat < -20 || lat > 10.8 || lon < -79.5 || lon > -44) return false;
        return !(lon < -76.5 && lat > -4.5);                                          // the Chocó and the Ecuadorian coast
    }

    /** Kangaroo ground: savanna, dry grass and shrubland, open woodland and farmland; not rainforest, mountains or town. */
    public static boolean kangarooGround(int climate, int cover, double elevM) {
        if (elevM < 0 || elevM > 1600) return false;
        boolean climateOk = climate == AW || (climate >= BWH && climate <= CSB) || climate == CWA || climate == CFA || climate == CFB;
        // cover 0 = no land-cover data here (only packs and streaming carry it): go by the climate alone
        boolean coverOk = cover == 0 || cover == GRASS || cover == SHRUB || cover == BARE || cover == CROP || cover == TREE || cover == MOSS;
        return climateOk && coverOk;
    }

    /** Anaconda ground: hot wet lowland (rainforest, monsoon forest, wet savanna) where there is water. */
    public static boolean anacondaGround(int climate, int cover, double elevM) {
        if (elevM < -5 || elevM > 500) return false;
        boolean climateOk = climate == AF || climate == AM || climate == AW;
        return climateOk && cover != BUILT && cover != SNOW && cover != BARE;
    }

    /** On a generated planet: the same ground, minus the geography (kangaroos keep to the drier side). */
    public static boolean kangarooGroundPlanet(int climate, int cover, double elevM) {
        return kangarooGround(climate, cover, elevM) && (climate == AW || climate == BSH || climate == BSK || climate == BWH);
    }

    public static boolean anacondaGroundPlanet(int climate, int cover, double elevM) {
        return anacondaGround(climate, cover, elevM) && climate != AW;
    }

    /** Red kangaroos in the arid interior, eastern greys in the wetter east and south. */
    public static int kangarooVariant(int climate) {
        return climate >= BWH && climate <= BSK ? RED : GREY;
    }
}
