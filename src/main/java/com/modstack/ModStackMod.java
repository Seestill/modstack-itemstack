package com.modstack;

import com.modstack.config.ModStackConfig;
import com.modstack.entity.BreedingCheck;
import com.modstack.entity.StackAccess;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModStackMod implements ModInitializer {

    public static final String MOD_ID = "modstack";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[ModStack] Initializing mob & item-drop stacking system...");
        ModStackConfig.load();
        registerInteractions();
        registerCommands();
        LOGGER.info("[ModStack] Ready. Mob merge radius={} blocks, max stack={}, item-drop merge radius={} blocks, item-drop max pile={}, breeding-into-stack={}",
                ModStackConfig.MERGE_RADIUS, ModStackConfig.MAX_MOB_STACK,
                ModStackConfig.ITEM_DROP_MERGE_RADIUS, ModStackConfig.ITEM_DROP_MAX_STACK,
                ModStackConfig.BREEDING_ADDS_TO_STACK);
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("modstack")
                    .then(CommandManager.literal("reload")
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(ctx -> {
                                ModStackConfig.reload();
                                ctx.getSource().sendFeedback(() -> Text.literal("[ModStack] Config reloaded from modstack.json"), true);
                                return 1;
                            })));
        });
    }

    private void registerInteractions() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(entity instanceof MobEntity mob)) return ActionResult.PASS;
            if (!(mob instanceof StackAccess stack)) return ActionResult.PASS;
            if (!(world instanceof ServerWorld serverWorld)) return ActionResult.PASS;

            ItemStack heldStack = player.getStackInHand(hand);

            if (mob instanceof AnimalEntity animal
                    && mob instanceof BreedingCheck breedCheck
                    && stack.modstack$getCount() > 1
                    && !animal.isBaby()
                    && animal.getBreedingAge() == 0
                    && breedCheck.modstack$isBreedingItem(heldStack)) {

                int count = stack.modstack$getCount();

                NbtCompound snapshot = new NbtCompound();
                mob.writeNbt(snapshot);
                snapshot.remove("UUID");
                snapshot.remove("ModStackCount");
                snapshot.remove("ModStackExempt");
                snapshot.remove("CustomName");
                snapshot.remove("CustomNameVisible");

                stack.modstack$setCount(count - 1);
                if (count - 1 <= 1) {
                    mob.setCustomName(null);
                    mob.setCustomNameVisible(false);
                }

                if (!player.getAbilities().creativeMode) {
                    heldStack.decrement(1);
                }

                EntityType<?> type = mob.getType();
                Entity spawned = type.create(serverWorld);
                if (spawned instanceof AnimalEntity splitAnimal) {
                    double offsetX = (player.getRandom().nextDouble() - 0.5) * 1.5;
                    double offsetZ = (player.getRandom().nextDouble() - 0.5) * 1.5;
                    splitAnimal.refreshPositionAndAngles(mob.getX() + offsetX, mob.getY(), mob.getZ() + offsetZ, mob.getYaw(), mob.getPitch());
                    splitAnimal.initialize(serverWorld, serverWorld.getLocalDifficulty(splitAnimal.getBlockPos()),
                            SpawnReason.MOB_SUMMONED, null, null);
                    splitAnimal.readNbt(snapshot);
                    splitAnimal.refreshPositionAndAngles(mob.getX() + offsetX, mob.getY(), mob.getZ() + offsetZ, mob.getYaw(), mob.getPitch());

                    if (splitAnimal instanceof StackAccess splitStack) {
                        splitStack.modstack$setCount(1);
                    }
                    splitAnimal.setCustomName(null);
                    splitAnimal.setCustomNameVisible(false);
                    serverWorld.spawnEntity(splitAnimal);

                    splitAnimal.lovePlayer(player);
                    player.sendMessage(Text.literal("Split one out to breed — feed the stack again to split a mate."), true);
                }
                return ActionResult.SUCCESS;
            }

            if (heldStack.getItem() instanceof SpawnEggItem && stack.modstack$getCount() > 1) {
                return ActionResult.FAIL;
            }

            if (player.isSneaking() && heldStack.isEmpty() && hand == Hand.MAIN_HAND) {
                int count = stack.modstack$getCount();
                if (count <= 1) return ActionResult.PASS;

                NbtCompound snapshot = new NbtCompound();
                mob.writeNbt(snapshot);
                snapshot.remove("UUID");
                snapshot.remove("ModStackCount");
                snapshot.remove("ModStackExempt");
                snapshot.remove("CustomName");
                snapshot.remove("CustomNameVisible");

                stack.modstack$setCount(count - 1);
                if (count - 1 <= 1) {
                    mob.setCustomName(null);
                    mob.setCustomNameVisible(false);
                }

                EntityType<?> type = mob.getType();
                Entity spawned = type.create(serverWorld);
                if (spawned instanceof MobEntity split) {
                    double offsetX = (player.getRandom().nextDouble() - 0.5) * 1.5;
                    double offsetZ = (player.getRandom().nextDouble() - 0.5) * 1.5;
                    split.refreshPositionAndAngles(mob.getX() + offsetX, mob.getY(), mob.getZ() + offsetZ, mob.getYaw(), mob.getPitch());
                    split.initialize(serverWorld, serverWorld.getLocalDifficulty(split.getBlockPos()),
                            SpawnReason.MOB_SUMMONED, null, null);
                    split.readNbt(snapshot);
                    split.refreshPositionAndAngles(mob.getX() + offsetX, mob.getY(), mob.getZ() + offsetZ, mob.getYaw(), mob.getPitch());

                    if (split instanceof StackAccess splitStack) {
                        splitStack.modstack$setCount(1);
                    }
                    split.setCustomName(null);
                    split.setCustomNameVisible(false);
                    serverWorld.spawnEntity(split);
                    player.sendMessage(Text.literal("Split 1 out — " + (count - 1) + " left in the stack."), true);
                }
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });
    }
}
