package com.seth.fossilrecord;

import com.mojang.brigadier.context.CommandContext;
import com.seth.worldbound.Earth;
import com.seth.worldbound.api.EarthCommandsApi;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** /earth fauna [list | where | spawn | placement] (and the developer tests), hung under /earth through the engine API. */
final class FaunaCommands {
    private FaunaCommands() {}

    static void register() {
        var op = Commands.<CommandSourceStack>hasPermission(Commands.LEVEL_GAMEMASTERS);
        EarthCommandsApi.register(earth -> earth
                .then(Commands.literal("fauna").executes(c -> {
                    var p = c.getSource().getPlayerOrException();
                    for (String line : com.seth.fossilrecord.Fauna.report(c.getSource().getLevel(), p))
                        c.getSource().sendSuccess(() -> Component.literal(line), false);
                    return 1;
                }).then(Commands.literal("selftest").requires(s -> FossilRecord.debug() && op.test(s))
                        .executes(c -> { com.seth.fossilrecord.FaunaTests.run(c.getSource()); return 1; }))
                .then(Commands.literal("dinotest").requires(s -> FossilRecord.debug() && op.test(s))
                        .executes(c -> { com.seth.fossilrecord.DinoTests.run(c.getSource()); return 1; }))
                .then(Commands.literal("placement")
                        .executes(c -> { c.getSource().sendSuccess(() -> Component.literal(com.seth.fossilrecord.dino.Dinos.placementLine()), false); return 1; })
                        .then(Commands.literal("accurate").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> dinoPlacement(c, false)))
                        .then(Commands.literal("random").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> dinoPlacement(c, true))))
                .then(Commands.literal("dinospeed").requires(s -> FossilRecord.debug() && op.test(s))
                        .executes(c -> { com.seth.fossilrecord.DinoTests.speeds(c.getSource()); return 1; }))
                .then(Commands.literal("spawn").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("species", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(faunaNames(), b))
                                .executes(c -> {
                                    String name = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "species");
                                    var pos = net.minecraft.core.BlockPos.containing(c.getSource().getPosition());
                                    var dino = com.seth.fossilrecord.dino.Dinos.byName(name);
                                    if (dino != null) {
                                        int n = com.seth.fossilrecord.dino.Dinos.spawnGroup(c.getSource().getLevel(), dino,
                                                pos.getX() + (int) Math.ceil(3 + dino.body.L * 0.4), pos.getZ() + 3, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                                        c.getSource().sendSuccess(() -> Component.literal("placed " + n + " " + dino.shortName + (n == 0 ? " (no room here: big ones need open ground)" : "")), false);
                                        return n;
                                    }
                                    com.seth.fossilrecord.Fauna.Species s;
                                    try { s = com.seth.fossilrecord.Fauna.Species.valueOf(name.toUpperCase(java.util.Locale.ROOT)); }
                                    catch (IllegalArgumentException e) { c.getSource().sendFailure(Component.literal("no animal called " + name + " (see /earth fauna list)")); return 0; }
                                    int n = com.seth.fossilrecord.Fauna.spawnGroup(c.getSource().getLevel(), s, pos.getX() + 3, pos.getZ() + 3,
                                            net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                                    c.getSource().sendSuccess(() -> Component.literal("placed " + n + " " + name.toLowerCase(java.util.Locale.ROOT) + (n == 1 ? "" : "s")), false);
                                    return n;
                                })))
                .then(Commands.literal("where")
                        .then(Commands.argument("species", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(com.seth.fossilrecord.dino.Roster.ALL.stream().map(sp -> sp.id), b))
                                .executes(c -> {
                                    String name = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "species");
                                    var dino = com.seth.fossilrecord.dino.Dinos.byName(name);
                                    if (dino == null) { c.getSource().sendFailure(Component.literal("no dinosaur called " + name + " (see /earth fauna list)")); return 0; }
                                    var at = c.getSource().getPosition();
                                    for (String line : com.seth.fossilrecord.dino.Dinos.where(dino, at.x, at.z))
                                        c.getSource().sendSuccess(() -> Component.literal(line), false);
                                    return 1;
                                })))
                .then(Commands.literal("list").executes(c -> {
                    java.util.Map<String, java.util.List<String>> bySite = new java.util.LinkedHashMap<>();
                    for (var sp : com.seth.fossilrecord.dino.Roster.ALL)
                        for (var site : sp.sites) bySite.computeIfAbsent(site.name(), k -> new java.util.ArrayList<>()).add(sp.shortName);
                    if (com.seth.fossilrecord.dino.Dinos.randomPlacement())
                        c.getSource().sendSuccess(() -> Component.literal("This world places them at random: any of them can turn up anywhere they can live (/earth fauna placement)."), false);
                    c.getSource().sendSuccess(() -> Component.literal(com.seth.fossilrecord.dino.Roster.ALL.size() + " dinosaurs, by where their fossils were found (/earth fauna where <name> for coordinates):"), false);
                    for (var e : bySite.entrySet()) c.getSource().sendSuccess(() -> Component.literal("  " + e.getKey() + ": " + String.join(", ", e.getValue())), false);
                    var homes = FossilRecord.homes();
                    if (homes != null) c.getSource().sendSuccess(() -> Component.literal("  " + homes.othersLine()), false);
                    return 1;
                }))));
    }

    /** /earth fauna placement accurate|random: this world's dinosaur placement, saved in its worldbound.json. */
    private static int dinoPlacement(CommandContext<CommandSourceStack> c, boolean random) {
        if (!Earth.earthWorld()) {
            c.getSource().sendFailure(Component.literal("Dinosaur placement is a setting of Earth and Earthish worlds; this isn't one."));
            return 0;
        }
        com.seth.worldbound.api.WorldOptions.set(c.getSource().getServer(), "dino_placement", random ? "random" : "accurate");
        c.getSource().sendSuccess(() -> Component.literal(com.seth.fossilrecord.dino.Dinos.placementLine()), true);
        c.getSource().sendSuccess(() -> Component.literal("Dinosaurs already out there stay where they are; new land and the odd top-up near players"
                + (random ? " bring the rest." : " now follow their homes.")), false);
        return 1;
    }

    private static java.util.List<String> faunaNames() {
        java.util.List<String> out = new java.util.ArrayList<>(java.util.List.of("kangaroo", "anaconda"));
        for (var d : com.seth.fossilrecord.dino.Roster.ALL) out.add(d.id);
        return out;
    }
}
