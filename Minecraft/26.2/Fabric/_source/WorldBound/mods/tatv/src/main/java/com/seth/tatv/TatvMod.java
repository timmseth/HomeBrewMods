package com.seth.tatv;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class TatvMod implements ModInitializer {
    public static final String ID = "tatv";
    public static final Logger LOG = LoggerFactory.getLogger("TATV");

    private static final Identifier TATV_ID = Identifier.fromNamespaceAndPath(ID, "tatv");
    public static final EntityType<TatvEntity> ENTITY = Registry.register(BuiltInRegistries.ENTITY_TYPE, TATV_ID,
            EntityType.Builder.<TatvEntity>of(TatvEntity::new, MobCategory.MISC).sized(1.8f, 0.8f).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, TATV_ID)));
    public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, TATV_ID,
            new TatvItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, TATV_ID)).stacksTo(1)));

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(TatvAction.TYPE, TatvAction.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TatvAction.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            if (!(p.level().getEntity(payload.entityId()) instanceof TatvEntity t) || p.getVehicle() != t) return;
            if (payload.action() == TatvEntity.ACT_EXIT) {
                p.stopRiding();
                return;
            }
            if (t.getControllingPassenger() != p) return;
            switch (payload.action()) {
                case TatvEntity.ACT_MODE -> {
                    t.setMode(TatvEntity.Mode.values()[(t.mode().ordinal() + 1) % TatvEntity.Mode.values().length]);
                    t.setSubmerged(false);
                }
                case TatvEntity.ACT_ANCHOR -> t.setAnchored(!t.anchored());
                case TatvEntity.ACT_SUBMERGE -> {
                    if (t.submerged()) t.setSubmerged(false);
                    else if (t.isInWater() || t.overWater()) {
                        t.setSubmerged(true);
                        if (t.mode() == TatvEntity.Mode.GLIDE || t.mode() == TatvEntity.Mode.VTOL) t.setMode(TatvEntity.Mode.HOVER);
                    } else p.sendOverlayMessage(Component.literal("Get over water to dive"));
                }
                case TatvEntity.ACT_LAND -> { if (t.mode() == TatvEntity.Mode.GLIDE) t.setMode(TatvEntity.Mode.DRIVE); }
                default -> {}
            }
        });

        // /tatv: a TATV appears under you, in any world. Your previous one (if loaded) is recalled.
        CommandRegistrationCallback.EVENT.register((d, ctx, sel) -> d.register(Commands.literal("tatv").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            summon(p);
            c.getSource().sendSuccess(() -> Component.literal("Your TATV is here. R: mode (Drive › Hover › VTOL › Glide) · Alt: dive/surface · G: anchor · [ ]: speed · Sprint: boost · V: get out"), false);
            return 1;
        })));

        // Earth worlds: everyone starts with one.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!isEarthWorld(server)) return;
            ServerPlayer p = handler.player;
            if (alreadyGiven(server, p)) return;
            p.getInventory().add(new ItemStack(ITEM));
            p.sendSystemMessage(Component.literal("You've been given a TATV (Truly All-Terrain Vehicle). Place it and climb in, or type /tatv any time."));
            markGiven(server, p);
        });
    }

    static void summon(ServerPlayer p) {
        ServerLevel level = (ServerLevel) p.level();
        for (TatvEntity old : level.getEntities(ENTITY, e -> p.getUUID().equals(e.owner()))) {
            if (old.getPassengers().isEmpty()) old.discard();
        }
        TatvEntity t = ENTITY.create(level, EntitySpawnReason.COMMAND);
        if (t == null) return;
        t.snapTo(p.getX(), p.getY() + 0.2, p.getZ(), p.getYRot(), 0);
        t.setOwner(p.getUUID());
        level.addFreshEntity(t);
        p.stopRiding();
        p.startRiding(t);
    }

    static boolean isEarthWorld(MinecraftServer server) {
        return server.overworld().getChunkSource().getGenerator().getBiomeSource().getClass().getName().endsWith(".EarthBiomeSource");
    }

    private static Path givenFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("tatv_given.txt");
    }

    private static boolean alreadyGiven(MinecraftServer server, ServerPlayer p) {
        try {
            Path f = givenFile(server);
            return Files.exists(f) && Files.readAllLines(f).contains(p.getUUID().toString());
        } catch (IOException e) {
            return false;
        }
    }

    private static void markGiven(MinecraftServer server, ServerPlayer p) {
        try {
            Files.writeString(givenFile(server), p.getUUID() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.warn("tatv_given: {}", e.toString());
        }
    }
}
