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
// Fire and active potion effects (poison, etc.) are deliberately NOT carried
// over to the replacement — otherwise a single fire/poison tick that kills
// the current representative would immediately also be "on fire"/poisoned
// on the replacement, chain-killing the whole stack in one tick.
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
        if (remaining < 1) return;

        NbtCompound snapshot = new NbtCompound();
        self.writeNbt(snapshot);
        snapshot.remove("UUID");
        snapshot.remove("ModStackCount");
        snapshot.remove("ModStackExempt");
        snapshot.remove("CustomName");
        snapshot.remove("CustomNameVisible");
        snapshot.remove("Attributes");
        snapshot.remove("Health");
        snapshot.remove("HurtTime");
        snapshot.remove("DeathTime");
        snapshot.remove("Motion");
        snapshot.remove("Fire");
        snapshot.remove("ActiveEffects");

        EntityType<?> type = self.getType();
        Entity spawned = type.create(serverWorld);
        if (spawned instanceof MobEntity replacement) {
            replacement.refreshPositionAndAngles(self.getX(), self.getY(), self.getZ(), self.getYaw(), self.getPitch());

            replacement.initialize(serverWorld, serverWorld.getLocalDifficulty(replacement.getBlockPos()),
                    SpawnReason.MOB_SUMMONED, null, null);

            replacement.readNbt(snapshot);
            replacement.refreshPositionAndAngles(self.getX(), self.getY(), self.getZ(), self.getYaw(), self.getPitch());
            replacement.setHealth(replacement.getMaxHealth());
            replacement.setFireTicks(0);

            if (replacement instanceof StackAccess replacementStack) {
                replacementStack.modstack$setCount(remaining);
            }
            serverWorld.spawnEntity(replacement);
        }
    }
}
