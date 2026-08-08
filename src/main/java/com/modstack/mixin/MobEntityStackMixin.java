package com.modstack.mixin;

import com.modstack.config.ModStackConfig;
import com.modstack.entity.StackAccess;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

// Adds a persistent "stack count" to every MobEntity, periodically merges
// nearby identical mobs into one stack, colors the nametag by stack size,
// and only shows the nametag when a player is close enough.
@Mixin(MobEntity.class)
public abstract class MobEntityStackMixin implements StackAccess {

    private int modstack_count = 1;
    private int modstack_mergeCooldown = (int) (Math.random() * ModStackConfig.MERGE_INTERVAL_TICKS);

    @Override
    public int modstack$getCount() {
        return modstack_count;
    }

    @Override
    public void modstack$setCount(int count) {
        this.modstack_count = Math.max(1, Math.min(count, ModStackConfig.MAX_MOB_STACK));
        modstack$refreshName();
    }

    private void modstack$refreshName() {
        MobEntity self = (MobEntity) (Object) this;
        if (modstack_count > 1) {
            String baseName = self.getType().getName().getString();
            Formatting color = modstack$colorForCount(modstack_count);
            MutableText text = Text.literal(baseName + " x" + modstack_count).formatted(color, Formatting.BOLD);
            self.setCustomName(text);
        } else {
            self.setCustomName(null);
            self.setCustomNameVisible(false);
        }
    }

    private Formatting modstack$colorForCount(int count) {
        if (count >= 50) return Formatting.RED;
        if (count >= 25) return Formatting.GOLD;
        if (count >= 10) return Formatting.YELLOW;
        return Formatting.GREEN;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void modstack$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putInt("ModStackCount", modstack_count);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void modstack$readNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("ModStackCount")) {
            modstack_count = Math.max(1, nbt.getInt("ModStackCount"));
            modstack$refreshName();
        }
    }

    @Inject(method = "mobTick", at = @At("HEAD"))
    private void modstack$tickMerge(CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;
        if (self.getWorld().isClient) return;
        if (!self.isAlive()) return;

        if (modstack_count > 1) {
            // Only show/hide the stack nametag when a player is close enough.
            boolean nearPlayer = self.getWorld().getClosestPlayer(self, ModStackConfig.NAMETAG_VISIBLE_RADIUS) != null;
            self.setCustomNameVisible(nearPlayer);

            // Clear "who hit me" state every tick so panic/escape AI (sheep, cows,
            // pigs, etc.) never gets a chance to trigger while part of a stack.
            self.setAttacker(null);
        }

        if (!"minecraft".equals(net.minecraft.registry.Registries.ENTITY_TYPE.getId(self.getType()).getNamespace())) return;
        if (!ModStackConfig.ALLOW_BABY_STACKING && self instanceof AnimalEntity animal && animal.isBaby()) return;

        if (modstack_mergeCooldown-- > 0) return;
        modstack_mergeCooldown = ModStackConfig.MERGE_INTERVAL_TICKS;

        if (modstack_count >= ModStackConfig.MAX_MOB_STACK) return;
        if (!(self.getWorld() instanceof ServerWorld serverWorld)) return;

        Box searchBox = self.getBoundingBox().expand(ModStackConfig.MERGE_RADIUS);
        List<MobEntity> nearby = serverWorld.getEntitiesByClass(MobEntity.class, searchBox,
                other -> other != self
                        && other.isAlive()
                        && other.getType() == self.getType()
                        && (ModStackConfig.ALLOW_BABY_STACKING || !(other instanceof AnimalEntity a && a.isBaby())));

        for (MobEntity other : nearby) {
            if (!(other instanceof StackAccess otherStack)) continue;
            int combined = modstack_count + otherStack.modstack$getCount();
            if (combined > ModStackConfig.MAX_MOB_STACK) continue;

            modstack$setCount(combined);
            other.discard();
            if (modstack_count >= ModStackConfig.MAX_MOB_STACK) break;
        }
    }
}
