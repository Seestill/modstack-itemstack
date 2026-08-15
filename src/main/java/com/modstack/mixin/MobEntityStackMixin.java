package com.modstack.mixin;

import com.modstack.config.ModStackConfig;
import com.modstack.entity.StackAccess;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.FrogEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.TropicalFishEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
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

@Mixin(MobEntity.class)
public abstract class MobEntityStackMixin implements StackAccess {

    private int modstack_count = 1;
    private int modstack_mergeCooldown = (int) (Math.random() * ModStackConfig.MERGE_INTERVAL_TICKS);
    private int modstack_bonusEggTimer = -1;
    private boolean modstack_exempt = false;

    @Override
    public int modstack$getCount() {
        return modstack_count;
    }

    @Override
    public void modstack$setCount(int count) {
        this.modstack_count = Math.max(1, Math.min(count, ModStackConfig.MAX_MOB_STACK));
        modstack$refreshName();
    }

    @Override
    public boolean modstack$isExempt() {
        return modstack_exempt;
    }

    private void modstack$refreshName() {
        if (modstack_exempt) return;
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

    private boolean modstack$isTamed(MobEntity mob) {
        if (mob instanceof TameableEntity t && t.isTamed()) return true;
        if (mob instanceof AbstractHorseEntity h && h.isTame()) return true;
        return false;
    }

    // Adults and babies must never merge into the same stack representative,
    // even when baby stacking is enabled — otherwise the survivor's own
    // baby/adult model "wins" and visually swallows the other's growth stage.
    private boolean modstack$sameGrowthStage(MobEntity a, MobEntity b) {
        boolean aBaby = a instanceof AnimalEntity aa && aa.isBaby();
        boolean bBaby = b instanceof AnimalEntity bb && bb.isBaby();
        return aBaby == bBaby;
    }

    private boolean modstack$isSpecialState(MobEntity mob) {
        if (mob instanceof ZombieVillagerEntity zv && zv.isConverting()) return true;
        if (mob instanceof CreeperEntity) {
            NbtCompound temp = new NbtCompound();
            mob.writeNbt(temp);
            if (temp.getBoolean("Powered")) return true;
        }
        // Villagers (and wandering traders) each carry their own unique
        // profession/trades — merging them would silently delete a villager's
        // entire trade inventory, so they never stack at all.
        if (mob instanceof net.minecraft.entity.passive.VillagerEntity) return true;
        if (mob instanceof net.minecraft.entity.passive.WanderingTraderEntity) return true;
        return false;
    }

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
            return ra.getVariant() == rb.getVariant();
        }
        if (a instanceof CatEntity ca && b instanceof CatEntity cb) {
            return Objects.equals(ca.getVariant(), cb.getVariant());
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
        nbt.putBoolean("ModStackExempt", modstack_exempt);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void modstack$readNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("ModStackCount")) {
            modstack_count = Math.max(1, nbt.getInt("ModStackCount"));
        }
        if (nbt.contains("ModStackExempt")) {
            modstack_exempt = nbt.getBoolean("ModStackExempt");
        }
        modstack$refreshName();
    }

    @Inject(method = "mobTick", at = @At("HEAD"))
    private void modstack$tickMerge(CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;
        if (self.getWorld().isClient) return;
        if (!self.isAlive()) return;

        if (!modstack_exempt && modstack_count == 1 && self.hasCustomName()) {
            modstack_exempt = true;
        }
        if (!modstack_exempt && modstack$isTamed(self)) {
            modstack_exempt = true;
        }

        if (modstack_count > 1) {
            boolean nearPlayer = self.getWorld().getClosestPlayer(self, ModStackConfig.NAMETAG_VISIBLE_RADIUS) != null;
            self.setCustomNameVisible(nearPlayer);
            self.setAttacker(null);

            if (self instanceof ChickenEntity chicken) {
                modstack$tickBonusEggs(chicken);
            }
        }

        if (modstack_exempt) return;
        if (modstack$isSpecialState(self)) return;
        if (self.age < ModStackConfig.MERGE_MIN_AGE_TICKS) return;
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
                        && other.age >= ModStackConfig.MERGE_MIN_AGE_TICKS
                        && other.getType() == self.getType()
                        && modstack$sameVariant(self, other)
                        && !modstack$isSpecialState(other)
                        && !modstack$isTamed(other)
                        && !(other instanceof StackAccess os && os.modstack$isExempt())
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

    private void modstack$tickBonusEggs(ChickenEntity chicken) {
        if (chicken.isBaby()) return;
        if (!(chicken.getWorld() instanceof ServerWorld serverWorld)) return;

        if (modstack_bonusEggTimer < 0) {
            modstack_bonusEggTimer = 6000 + chicken.getRandom().nextInt(6000);
        }
        if (modstack_bonusEggTimer-- > 0) return;
        modstack_bonusEggTimer = 6000 + chicken.getRandom().nextInt(6000);

        int eggsToLay = Math.min(modstack_count - 1, 64);
        if (eggsToLay <= 0) return;

        ItemStack eggs = new ItemStack(Items.EGG, eggsToLay);
        ItemEntity eggEntity = new ItemEntity(serverWorld, chicken.getX(), chicken.getY(), chicken.getZ(), eggs);
        serverWorld.spawnEntity(eggEntity);
        chicken.playSound(SoundEvents.ENTITY_CHICKEN_EGG, 1.0F,
                (chicken.getRandom().nextFloat() - chicken.getRandom().nextFloat()) * 0.2F + 1.0F);
    }
}
