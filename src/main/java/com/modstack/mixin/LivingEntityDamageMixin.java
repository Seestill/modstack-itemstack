package com.modstack.mixin;

import com.modstack.entity.StackAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// When a stacked mob actually dies (lethal hit reaches onDeath), the
// original entity dies for real through full vanilla logic: death
// animation, sound, particles, and its real loot table.
//
// Before it's gone, we snapshot ONLY its color/pattern-related NBT (by
// stripping out Attributes, Health, and other per-individual fields) and
// spawn a replacement to continue representing the remaining stack count.
// The replacement goes through the normal initialize() a freshly spawned
// mob would (so its speed/health/jump attributes are freshly randomized
// like any new horse/mob would be), then the snapshot is applied on top to
// restore the same color/pattern the dead one had — so it still counts as
// the same stack, but isn't a stat-for-stat clone.
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void modstack$onDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof StackAccess stack)) return;
        if (self.getWorld().isClient) return;
        if (!(self.getWorld() instanceof ServerWorld serverWorld)) return;
        if (!(self instanceof MobEntity)) return;

        int remaining = stack.modstack$getCount() - 1;
        if (remaining < 1) return; // this was the last one in the stack — just die normally

        // Snapshot the dying mob's data, then strip everything that should
        // NOT carry over to a "freshly spawned" replacement: per-individual
        // stats, health, and bookkeeping fields. What's left is mostly
        // color/pattern/variant-type data.
        NbtCompound snapshot = new NbtCompound();
        self.writeNbt(snapshot);
        snapshot.remove("UUID");
        snapshot.remove("ModStackCount");
        snapshot.remove("CustomName");
        snapshot.remove("CustomNameVisible");
        snapshot.remove("Attributes");
        snapshot.remove("Health");
        snapshot.remove("HurtTime");
        snapshot.remove("DeathTime");
        snapshot.remove("Motion");

        EntityType<?> type = self.getType();
        Entity spawned = type.create(serverWorld);
        if (spawned instanceof MobEntity replacement) {
            replacement.refreshPositionAndAngles(self.getX(), self.getY(), self.getZ(), self.getYaw(), self.getPitch());

            // Let the game randomize stats (speed/health/jump/etc.) exactly
            // like a naturally spawned mob would get.
            replacement.initialize(serverWorld, serverWorld.getLocalDifficulty(replacement.getBlockPos()),
                    SpawnReason.MOB_SUMMONED, null, null);

            // Now overlay the dead mob's color/pattern data on top.
            replacement.readNbt(snapshot);
            replacement.refreshPositionAndAngles(self.getX(), self.getY(), self.getZ(), self.getYaw(), self.getPitch());
            replacement.setHealth(replacement.getMaxHealth());

            if (replacement instanceof StackAccess replacementStack) {
                replacementStack.modstack$setCount(remaining);
            }
            serverWorld.spawnEntity(replacement);
        }
    }
}
