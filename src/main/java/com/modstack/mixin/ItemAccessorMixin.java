package com.modstack.mixin;

import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes Item.maxCount (normally private, only settable via Item.Settings
 * before the item is built) so ModStackMod can rewrite stack sizes for
 * already-registered vanilla items at startup.
 */
@Mixin(Item.class)
public interface ItemAccessorMixin {

    @Accessor("maxCount")
    void modstack$setMaxCount(int maxCount);

    @Accessor("maxCount")
    int modstack$getMaxCount();
}
