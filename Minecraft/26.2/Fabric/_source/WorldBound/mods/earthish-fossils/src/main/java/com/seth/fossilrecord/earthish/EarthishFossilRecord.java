package com.seth.fossilrecord.earthish;

import com.seth.fossilrecord.FossilRecord;
import com.seth.fossilrecord.Habitat;
import com.seth.fossilrecord.dino.DinoSpecies;
import com.seth.fossilrecord.dino.Dinos;
import com.seth.worldbound.Earth;
import net.fabricmc.api.ModInitializer;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Earthish Fossil Record: on a generated planet each dinosaur gets three home ranges drawn from the planet's seed
 * and lives where the climate there suits it; kangaroos and anacondas wherever their ground and climate are.
 */
public final class EarthishFossilRecord implements ModInitializer {
    @Override
    public void onInitialize() {
        FossilRecord.init();
        FossilRecord.registerHomes(new FossilRecord.Homes() {
            @Override public String kind() { return "earthish"; }

            @Override public boolean dino(DinoSpecies s, double lat, double lon) {
                return Dinos.planetHome(s, Earth.get().planetSeed(), lat, lon);
            }

            @Override public boolean dinoClimate(DinoSpecies s, Earth.Surface c) {
                return (Dinos.climateGroup(c.climate()) & s.climates) != 0;
            }

            @Override public List<Map.Entry<String, double[]>> dinoHomes(DinoSpecies s) {
                List<Map.Entry<String, double[]>> out = new ArrayList<>();
                int i = 1;
                for (double[] h : Dinos.planetHomes(s, Earth.get().planetSeed()))
                    out.add(new AbstractMap.SimpleEntry<>("home range " + i++ + String.format(Locale.ROOT, " (%.0f-degree radius, where the climate suits it)", h[2]),
                            new double[]{h[0], h[1]}));
                return out;
            }

            @Override public boolean kangaroo(Earth.Surface c) { return Habitat.kangarooGroundPlanet(c.climate(), c.cover(), c.elevM()); }

            @Override public boolean anaconda(Earth.Surface c) { return Habitat.anacondaGroundPlanet(c.climate(), c.cover(), c.elevM()); }

            @Override public String othersLine() { return "Kangaroos and green anacondas: wherever the ground and climate suit them."; }
        });
    }
}
