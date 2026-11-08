package com.seth.worldbound.api;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Lets add-ons hang their own subcommands under /earth (e.g. Fossil Record's /earth fauna ...). */
public final class EarthCommandsApi {
    private EarthCommandsApi() {}

    private static final List<Consumer<LiteralArgumentBuilder<CommandSourceStack>>> ADDERS = new CopyOnWriteArrayList<>();

    /** Called with the /earth literal before it's registered: add your {@code .then(...)} to it. Register during mod init. */
    public static void register(Consumer<LiteralArgumentBuilder<CommandSourceStack>> adder) {
        ADDERS.add(adder);
    }

    public static void apply(LiteralArgumentBuilder<CommandSourceStack> earth) {
        for (var a : ADDERS) a.accept(earth);
    }

    /** Whether debug/developer subcommands are on: config {@code debug_commands} or -Dworldbound.debug=true. */
    public static boolean debug() {
        return Boolean.getBoolean("worldbound.debug") || com.seth.worldbound.Earth.baseConfig().debug_commands;
    }
}
