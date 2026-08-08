package com.modstack.mixin;

import com.modstack.entity.StackAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// damage() is implemented in LivingEntity (not MobEntity), and it returns
// boolean in 1.20.1, so this needs CallbackInfoReturnable<Boolean>.
//
// When a lethal hit lands on a stacked mob, instead of just healing the
// entity back up (which looked like "nothing happened, it just ran off"),
// we spawn a real temporary clone at the same spot and let IT actually die
// through vanilla logic: death animation, death sound, particles, and its
// own loot table via kill(). The stack entity itself is healed and its
// count is reduced by one, but never touches "dying" itself.
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void modstack$onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof StackAccess stack)) return;
        if (self.getWorld().isClient) return;
        if (stack.modstack$getCount() <= 1) return;
        if (!self.isAlive()) return;

        float healthAfter = self.getHealth() - amount;
        if (healthAfter > 0) return; // not lethal yet, let vanilla damage() run normally

        if (!(self.getWorld() instanceof ServerWorld serverWorld)) return;
        if (!(self instanceof MobEntity)) return;

        // Reduce the stack and heal the surviving representative first.
        stack.modstack$setCount(stack.modstack$getCount() - 1);
        self.setHealth(self.getMaxHealth());

        // Spawn a throwaway corpse clone that actually dies through vanilla logic.
        EntityType<?> type = self.getType();
        Entity spawned = type.create(serverWorld);
        if (spawned instanceof MobEntity corpse) {
            corpse.refreshPositionAndAngles(self.getX(), self.getY(), self.getZ(), self.getYaw(), self.getPitch());
            corpse.setAiDisabled(true);
            corpse.setVelocity(0, 0, 0);
            if (corpse instanceof StackAccess corpseStack) {
                corpseStack.modstack$setCount(1); // never merges, just dies this tick
            }
            serverWorld.spawnEntity(corpse);
            corpse.kill(); // triggers full vanilla death: animation, sound, particles, loot
        }

        cir.setReturnValue(true); // the original stack member absorbed the hit, not killed
    }
}
