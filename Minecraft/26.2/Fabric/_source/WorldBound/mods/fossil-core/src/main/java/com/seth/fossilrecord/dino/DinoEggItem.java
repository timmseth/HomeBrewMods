package com.seth.fossilrecord.dino;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * A laid dinosaur egg (one item for every species; the species rides in the custom model data, which also picks
 * the egg's look). Set it down on a block and it hatches: a hatchling, and a raptor hatchling takes whoever hatched
 * it for its owner.
 */
public final class DinoEggItem extends Item {
    public DinoEggItem(Properties p) { super(p); }

    /** An egg of this species. */
    public static ItemStack of(DinoSpecies s, int n) {
        ItemStack st = new ItemStack(Dinos.EGG_ITEM, n);
        st.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(s.id), List.of()));
        st.set(DataComponents.ITEM_NAME, Component.literal(s.shortName + " Egg"));
        return st;
    }

    public static DinoSpecies species(ItemStack st) {
        CustomModelData d = st.get(DataComponents.CUSTOM_MODEL_DATA);
        String id = d == null ? null : d.getString(0);
        return id == null ? null : Roster.get(id);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        DinoSpecies s = species(ctx.getItemInHand());
        if (s == null) return InteractionResult.FAIL;
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos at = ctx.getClickedPos().relative(ctx.getClickedFace());
        Dino baby = Dinos.hatch(level, s, at, ctx.getPlayer());
        if (baby == null) return InteractionResult.FAIL;
        if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        level.playSound(null, at, SoundEvents.TURTLE_EGG_HATCH, SoundSource.NEUTRAL, 1f, 1.2f);
        return InteractionResult.SUCCESS_SERVER;
    }
}
