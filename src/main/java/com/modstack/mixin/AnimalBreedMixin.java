package com.modstack.mixin;

import com.modstack.config.ModStackConfig;
import com.modstack.entity.StackAccess;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * Replaces vanilla baby-spawning with "grow the stack" behaviour when both
 * parents are ModStack-tracked mobs of the same type. This avoids breeding
 * farms filling the world with hundreds of individual baby entities that
 * each need to grow up (and each get merged back in 5 minutes later anyway).
 *
 * If either side isn't eligible (different mod handling it, config off, or
 * the entity type doesn't implement StackAccess for some reason) we fall
 * back to vanilla breeding untouched.
 */
@Mixin(AnimalEntity.class)
public abstract class AnimalBreedMixin {

    private static final Random RANDOM = new Random();

    @Inject(method = "breed", at = @At("HEAD"), cancellable = true)
    private void modstack$breedIntoStack(ServerWorld world, AnimalEntity other, CallbackInfo ci) {
        if (!ModStackConfig.BREEDING_ADDS_TO_STACK) return;

        AnimalEntity self = (AnimalEntity) (Object) this;
        if (self.getType() != other.getType()) return; // let vanilla handle any cross-breed logic
        if (!(self instanceof StackAccess selfStack)) return;

        int bonus = RANDOM.nextDouble() < ModStackConfig.BONUS_OFFSPRING_CHANCE ? 2 : 1;
        int newCount = selfStack.modstack$getCount() + bonus;
        if (newCount > ModStackConfig.MAX_MOB_STACK) {
            newCount = ModStackConfig.MAX_MOB_STACK;
        }
        selfStack.modstack$setCount(newCount);

        // Standard post-breed cooldown/reset so the pair doesn't breed every tick.
        self.setBreedingAge(6000);
        other.setBreedingAge(6000);
        self.resetLoveTicks();
        other.resetLoveTicks();

        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                self.getX(), self.getBodyY(0.5), self.getZ(),
                8, 0.3, 0.3, 0.3, 0.0);

        ci.cancel(); // stop vanilla from also creating+spawning a separate baby entity
    }
}
