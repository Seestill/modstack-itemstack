package com.modstack.entity;

import net.minecraft.item.ItemStack;

// Implemented by AnimalBreedMixin so other classes can check whether an
// ItemStack is that animal's real breeding food, without duplicating the
// species-specific list ourselves.
public interface BreedingCheck {
    boolean modstack$isBreedingItem(ItemStack stack);
}
