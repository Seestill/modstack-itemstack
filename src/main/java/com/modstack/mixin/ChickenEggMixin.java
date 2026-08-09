package com.modstack.mixin;

import com.modstack.entity.StackAccess;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

// When a chicken is part of a stack (count > 1), it lays a bonus batch of
// eggs on its own timer, roughly matching vanilla's average laying interval,
// so a stack of 20 chickens actually produces eggs like 20 chickens instead
// of just the one egg vanilla's own per-entity timer would give.
@Mixin(ChickenEntity.class)
public abstract class ChickenEggMixin {

    private static final Random MODSTACK_RANDOM = new Random();
    private int modstack_bonusEggTimer = -1;

    @Inject(method = "mobTick", at = @At("TAIL"))
    private void modstack$layBonusEggs(CallbackInfo ci) {
        ChickenEntity self = (ChickenEntity) (Object) this;
        if (self.getWorld().isClient) return;
        if (!self.isAlive()) return;
        if (self.isBaby()) return;
        if (!(self instanceof StackAccess stack)) return;

        int count = stack.modstack$getCount();
        if (count <= 1) return;
        if (!(self.getWorld() instanceof ServerWorld serverWorld)) return;

        if (modstack_bonusEggTimer < 0) {
            modstack_bonusEggTimer = 6000 + MODSTACK_RANDOM.nextInt(6000);
        }
        if (modstack_bonusEggTimer-- > 0) return;
        modstack_bonusEggTimer = 6000 + MODSTACK_RANDOM.nextInt(6000);

        // The "+1" chicken in the stack already lays its own egg through
        // vanilla's normal per-entity timer, so this covers the rest.
        int eggsToLay = Math.min(count - 1, 64);
        ItemStack eggs = new ItemStack(Items.EGG, eggsToLay);
        ItemEntity eggEntity = new ItemEntity(serverWorld, self.getX(), self.getY(), self.getZ(), eggs);
        serverWorld.spawnEntity(eggEntity);
        self.playSound(SoundEvents.ENTITY_CHICKEN_EGG, 1.0F,
                (self.getRandom().nextFloat() - self.getRandom().nextFloat()) * 0.2F + 1.0F);
    }
}
