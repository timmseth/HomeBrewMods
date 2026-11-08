package com.seth.tatv;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Right-click to set your TATV down where you're looking (or just ahead of you). */
public class TatvItem extends Item {
    public TatvItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(6)), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
        Vec3 at = hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(3)) : hit.getLocation();
        TatvEntity t = TatvMod.ENTITY.create(sl, net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
        if (t == null) return InteractionResult.FAIL;
        t.snapTo(at.x, at.y + 0.1, at.z, player.getYRot(), 0);
        t.setOwner(player.getUUID());
        sl.addFreshEntity(t);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
