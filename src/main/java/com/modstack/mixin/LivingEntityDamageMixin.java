package com.modstack.mixin;

import com.modstack.entity.StackAccess;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// damage() and dropLoot() are actually implemented in LivingEntity, not
// MobEntity, so this handles the "pop one member off the stack" logic
// that MobEntityStackMixin can't inject into directly.
// damage() returns boolean in 1.20.1, so we must use CallbackInfoReturnable<Boolean>.
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @Invoker("dropLoot")
    public abstract void modstack$invokeDropLoot(DamageSource source, boolean causedByPlayer);

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void modstack$onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof StackAccess stack)) return;
        if (self.getWorld().isClient) return;
        if (stack.modstack$getCount() <= 1) return;
        if (!self.isAlive()) return;

        float healthAfter = self.getHealth() - amount;
        if (healthAfter > 0) return;

        stack.modstack$setCount(stack.modstack$getCount() - 1);
        self.setHealth(self.getMaxHealth());

        modstack$invokeDropLoot(source, source.getAttacker() != null && source.getAttacker().isPlayer());

        cir.setReturnValue(true);
    }
}
