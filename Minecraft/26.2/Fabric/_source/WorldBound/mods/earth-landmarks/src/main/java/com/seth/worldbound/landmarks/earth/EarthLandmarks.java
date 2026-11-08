package com.seth.worldbound.landmarks.earth;

import com.seth.worldbound.landmarks.LandmarksMod;
import net.fabricmc.api.ModInitializer;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Earth Landmarks: obelisks at the real Earth's famous landmarks, city discovery (cities of 100,000+) for map
 * travel, and walled cities with keeps around every city of 500,000+.
 */
public final class EarthLandmarks implements ModInitializer {
    @Override
    public void onInitialize() {
        LandmarksMod.init();
        LandmarksMod.registerKind("earth");
        Registry.register(BuiltInRegistries.FEATURE, Identifier.fromNamespaceAndPath("worldbound", "city_walls"), new com.seth.worldbound.worldgen.CityWallsFeature());
        net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(net.fabricmc.fabric.api.biome.v1.BiomeSelectors.all(),
                net.minecraft.world.level.levelgen.GenerationStep.Decoration.SURFACE_STRUCTURES,
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath("worldbound", "city_walls")));
        com.seth.worldbound.api.WorldOptions.register(new com.seth.worldbound.api.WorldOptions.Option("city_walls", "City walls",
                java.util.List.of("on", "off"), java.util.List.of("On (cities of 500,000+)", "Off"),
                "Ring walls, towers, gates and a keep with an obelisk around every city of 500,000 people or more.", false));
        com.seth.worldbound.api.EarthCommandsApi.register(earth -> earth
                .then(Commands.literal("city").then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(c -> {
                    String q = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "name").toLowerCase(java.util.Locale.ROOT);
                    for (var city : com.seth.worldbound.worldgen.CityWalls.cities()) {
                        if (!com.seth.worldbound.api.GeoText.plain(city.name()).toLowerCase(java.util.Locale.ROOT).startsWith(q)) continue;
                        StringBuilder sb = new StringBuilder(String.format(java.util.Locale.ROOT, "%s: keep at %.0f, %.0f; wall radius ~%.0f blocks; style %s; gates at",
                                com.seth.worldbound.api.GeoText.plain(city.name()), city.cx(), city.cz(), city.radius(), city.style().name()));
                        for (double g : city.gates()) sb.append(String.format(java.util.Locale.ROOT, " (%.0f, %.0f)", city.cx() + Math.cos(g) * city.r(g), city.cz() + Math.sin(g) * city.r(g)));
                        String m = sb.toString();
                        c.getSource().sendSuccess(() -> Component.literal(m), false);
                        return 1;
                    }
                    c.getSource().sendSuccess(() -> Component.literal("No walled city starts with \"" + q + "\" (cities of 500k+)"), false);
                    return 0;
                }))));
    }
}
