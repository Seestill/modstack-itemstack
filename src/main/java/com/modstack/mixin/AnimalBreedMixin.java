package com.modstack.mixin;

import com.modstack.entity.BreedingCheck;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// Exposes AnimalEntity's protected isBreedingItem() check (via BreedingCheck)
// so ModStackMod's interaction handler can tell whether a held item is valid
// breeding food for that species. Actual breeding itself is left 100%
// vanilla — ModStackMod splits one individual out of a stack and puts it in
// love mode when fed, so two real, separate entities pair up and breed
// normally (real baby, real vanilla mechanics). Once done, the parents
// naturally re-merge into the stack through the normal periodic merge tick,
// same as the baby will once it grows up — no special-case code needed for
// either.
@Mixin(AnimalEntity.class)
public abstract class AnimalBreedMixin implements BreedingCheck {

    @Invoker("isBreedingItem")
    public abstract boolean modstack$isBreedingItemInvoker(ItemStack stack);

    @Override
    public boolean modstack$isBreedingItem(ItemStack stack) {
        return modstack$isBreedingItemInvoker(stack);
    }
}
