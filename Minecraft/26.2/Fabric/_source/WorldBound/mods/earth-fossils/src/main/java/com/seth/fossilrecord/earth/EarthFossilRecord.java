package com.seth.fossilrecord.earth;

import com.seth.fossilrecord.FossilRecord;
import com.seth.fossilrecord.Habitat;
import com.seth.fossilrecord.dino.DinoSpecies;
import com.seth.worldbound.Earth;
import net.fabricmc.api.ModInitializer;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Earth Fossil Record: on the real Earth, each dinosaur lives at its fossil sites, kangaroos in Australia, anacondas in the Amazon. */
public final class EarthFossilRecord implements ModInitializer {
    @Override
    public void onInitialize() {
        FossilRecord.init();
        FossilRecord.registerHomes(HOMES);
    }

    /** Fossil sites and real ranges (public for the tests). */
    public static final FossilRecord.Homes HOMES = new FossilRecord.Homes() {
            @Override public String kind() { return "earth"; }

            @Override public boolean dino(DinoSpecies s, double lat, double lon) { return s.ranges(lat, lon); }

            @Override public List<Map.Entry<String, double[]>> dinoHomes(DinoSpecies s) {
                List<Map.Entry<String, double[]>> out = new ArrayList<>();
                for (DinoSpecies.Site site : s.sites) out.add(new AbstractMap.SimpleEntry<>(site.name(), new double[]{site.lat(), site.lon()}));
                return out;
            }

            @Override public boolean kangaroo(Earth.Surface c) {
                return Habitat.australia(c.lat(), c.lon()) && Habitat.kangarooGround(c.climate(), c.cover(), c.elevM());
            }

            @Override public boolean anaconda(Earth.Surface c) {
                return Habitat.amazonia(c.lat(), c.lon()) && Habitat.anacondaGround(c.climate(), c.cover(), c.elevM());
            }

            @Override public String othersLine() { return "Australia: kangaroos. The Amazon and Orinoco: green anacondas."; }
        };
}
