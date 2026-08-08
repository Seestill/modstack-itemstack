package com.modstack.mixin;

import com.modstack.config.ModStackConfig;
import com.modstack.entity.StackAccess;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.FrogEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.TropicalFishEntity;
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
import java.util.Objects;

// Adds a persistent "stack count" to every MobEntity, periodically merges
// nearby identical mobs (same species AND same color/pattern variant, where
// applicable) into one stack, colors the nametag by stack size, and only
// shows the nametag when a player is close enough.
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

    // Returns false if these two mobs are the same species but a DIFFERENT
    // color/pattern variant (e.g. white sheep vs black sheep, red fox vs
    // snow fox) and therefore must NOT be merged into the same stack.
    // Species with no known variant check always return true.
    private boolean modstack$sameVariant(MobEntity a, MobEntity b) {
        if (a instanceof SheepEntity sa && b instanceof SheepEntity sb) {
            return sa.getColor() == sb.getColor();
        }
        if (a instanceof HorseEntity ha && b instanceof HorseEntity hb) {
            return ha.getVariant() == hb.getVariant();
        }
        if (a instanceof LlamaEntity la && b instanceof LlamaEntity lb) {
            return la.getVariant() == lb.getVariant();
        }
        if (a instanceof ParrotEntity pa && b instanceof ParrotEntity pb) {
            return pa.getVariant() == pb.getVariant();
        }
        if (a instanceof MooshroomEntity ma && b instanceof MooshroomEntity mb) {
            return ma.getVariant() == mb.getVariant();
        }
        if (a instanceof RabbitEntity ra && b instanceof RabbitEntity rb) {
            return ra.getRabbitType() == rb.getRabbitType();
        }
        if (a instanceof CatEntity ca && b instanceof CatEntity cb) {
            return Objects.equals(ca.getVariant(), cb.getVariant());
        }
        if (a instanceof FoxEntity fa && b instanceof FoxEntity fb) {
            return fa.getVariantType() == fb.getVariantType();
        }
        if (a instanceof AxolotlEntity xa && b instanceof AxolotlEntity xb) {
            return xa.getVariant() == xb.getVariant();
        }
        if (a instanceof FrogEntity ga && b instanceof FrogEntity gb) {
            return Objects.equals(ga.getVariant(), gb.getVariant());
        }
        if (a instanceof TropicalFishEntity ta && b instanceof TropicalFishEntity tb) {
            return ta.getVariant() == tb.getVariant();
        }
        if (a instanceof PandaEntity da && b instanceof PandaEntity db) {
            return da.getMainGene() == db.getMainGene();
        }
        if (a instanceof ShulkerEntity za && b instanceof ShulkerEntity zb) {
            return Objects.equals(za.getColor(), zb.getColor());
        }
        return true;
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
            boolean nearPlayer = self.getWorld().getClosestPlayer(self, ModStackConfig.NAMETAG_VISIBLE_RADIUS) != null;
            self.setCustomNameVisible(nearPlayer);
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
                        && modstack$sameVariant(self, other)
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
