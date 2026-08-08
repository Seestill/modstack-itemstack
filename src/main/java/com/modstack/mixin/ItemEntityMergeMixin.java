package com.modstack.mixin;

import com.modstack.config.ModStackConfig;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

// Periodically scans for other dropped-item piles of the same item (and same
// NBT/components) within ITEM_DROP_MERGE_RADIUS and merges them into one
// pile, up to ITEM_DROP_MAX_STACK. This is purely about items lying on the
// ground — inventory stack sizes are untouched.
@Mixin(ItemEntity.class)
public abstract class ItemEntityMergeMixin {

    private int modstack_dropMergeCooldown =
            (int) (Math.random() * ModStackConfig.ITEM_DROP_MERGE_INTERVAL_TICKS);

    @Inject(method = "tick", at = @At("TAIL"))
    private void modstack$mergeNearbyDrops(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.getWorld().isClient) return;
        if (!self.isAlive()) return;
        if (!(self.getWorld() instanceof ServerWorld serverWorld)) return;

        ItemStack selfStack = self.getStack();
        if (selfStack.getCount() >= ModStackConfig.ITEM_DROP_MAX_STACK) return;

        if (modstack_dropMergeCooldown-- > 0) return;
        modstack_dropMergeCooldown = ModStackConfig.ITEM_DROP_MERGE_INTERVAL_TICKS;

        Box searchBox = self.getBoundingBox().expand(ModStackConfig.ITEM_DROP_MERGE_RADIUS);
        List<ItemEntity> nearby = serverWorld.getEntitiesByClass(ItemEntity.class, searchBox,
                other -> other != self && other.isAlive());

        for (ItemEntity other : nearby) {
            ItemStack otherStack = other.getStack();
            if (!ItemStack.areItemsAndComponentsEqual(selfStack, otherStack)) continue;

            int combined = selfStack.getCount() + otherStack.getCount();
            if (combined > ModStackConfig.ITEM_DROP_MAX_STACK) continue;

            selfStack.setCount(combined);
            self.setStack(selfStack);
            other.discard();

            if (selfStack.getCount() >= ModStackConfig.ITEM_DROP_MAX_STACK) break;
        }
    }
}
