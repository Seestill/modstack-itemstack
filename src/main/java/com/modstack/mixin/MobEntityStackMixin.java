package com.modstack.mixin;

import com.modstack.config.ModStackConfig;
import com.modstack.entity.StackAccess;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

// Adds a persistent "stack count" to every MobEntity, periodically merges
// nearby identical mobs into one stack, and unwraps the stack one member at
// a time on death instead of always fully despawning.
@Mixin(MobEntity.class)
public abstract class MobEntityStackMixin implements StackAccess {

    @Shadow public abstract EntityType<?> getType();

    @Invoker("dropLoot")
    public abstract void modstack$invokeDropLoot(DamageSource source, boolean causedByPlayer);

    // @Unique-ish plain field is fine for a demo mixin; count starts at 1 (a "stack of one").
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
            self.setCustomName(Text.of(baseName + " x" + modstack_count));
            self.setCustomNameVisible(true);
        } else if (self.hasCustomName()) {
            // Only clear names WE set; if a player named it manually we'd want to
            // track that separately. Kept simple for this demo.
            self.setCustomNameVisible(false);
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void modstack$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putInt("ModStackCount", modstack_count);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void modstack$readNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("ModStackCount")) {
            modstack_count = Math.max(1, nbt.getInt("ModStackCount"));
        }
    }

    @Inject(method = "mobTick", at = @At("HEAD"))
    private void modstack$tickMerge(CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;
        if (self.getWorld().isClient) return;
        if (!self.isAlive()) return;
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
            other.discard(); // the merged-in mob is absorbed into this stack
            if (modstack_count >= ModStackConfig.MAX_MOB_STACK) break;
        }
    }

    // Intercept lethal damage: if this entity represents a stack of more than
    // one mob, "pop" a single member instead of letting the whole stack die.
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void modstack$onDamage(DamageSource source, float amount, CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;
        if (self.getWorld().isClient) return;
        if (modstack_count <= 1) return; // normal single-mob damage, let vanilla handle it
        if (!self.isAlive()) return;

        float healthAfter = self.getHealth() - amount;
        if (healthAfter > 0) return; // not lethal yet, let vanilla damage() apply normally

        // Lethal hit against a stack: remove one member, drop its loot, heal the rest.
        modstack$setCount(modstack_count - 1);
        self.setHealth(self.getMaxHealth());

        // Run the mob's real loot table for the "popped" member.
        modstack$invokeDropLoot(source, source.getAttacker() != null && source.getAttacker().isPlayer());

        ci.cancel(); // stop this damage call from killing/registering further; stack absorbed the hit
    }
}
