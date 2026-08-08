package com.modstack.config;

import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Central configuration for ModStack ItemStack.
 * Later this can be swapped for a JSON-loaded config; for now it's simple
 * static data so the mod works out of the box.
 */
public final class ModStackConfig {

    private ModStackConfig() {}

    // ---------- Mob stacking ----------

    /** Max mobs allowed in a single stack. */
    public static final int MAX_MOB_STACK = 64;

    /** Radius (blocks) mobs merge within, checked every MERGE_INTERVAL_TICKS. */
    public static final double MERGE_RADIUS = 4.0D;

    /** How often (in ticks) the server scans for mergeable mobs. 20 ticks = 1s. */
    public static final int MERGE_INTERVAL_TICKS = 40;

    /** Passive/hostile mobs eligible for stacking. Empty = all LivingEntity subtypes handled by mixin apply. */
    public static final boolean STACK_HOSTILE_MOBS = true;
    public static final boolean STACK_PASSIVE_MOBS = true;

    /** Baby mobs never stack (keeps growth logic sane). */
    public static final boolean ALLOW_BABY_STACKING = false;

    // ---------- Item stacking ----------

    /**
     * Extra max-stack-size overrides, keyed by item registry id (e.g. "minecraft:diamond_sword").
     * Any item not listed keeps its vanilla max stack size.
     * Tools/armor/unstackables default to 1 in vanilla; set to something like 16 here
     * to let them stack.
     */
    public static final Map<String, Integer> ITEM_STACK_OVERRIDES = new HashMap<>();
    static {
        // Blocks/items that are already stackable get bumped from 64 -> 256
        ITEM_STACK_OVERRIDES.put("minecraft:cobblestone", 256);
        ITEM_STACK_OVERRIDES.put("minecraft:dirt", 256);
        ITEM_STACK_OVERRIDES.put("minecraft:oak_log", 256);
        ITEM_STACK_OVERRIDES.put("minecraft:iron_ingot", 256);
        ITEM_STACK_OVERRIDES.put("minecraft:gold_ingot", 256);
        ITEM_STACK_OVERRIDES.put("minecraft:diamond", 256);
        ITEM_STACK_OVERRIDES.put("minecraft:netherite_ingot", 256);

        // Normally unstackable (max 1) items now stack in small amounts
        ITEM_STACK_OVERRIDES.put("minecraft:diamond_sword", 16);
        ITEM_STACK_OVERRIDES.put("minecraft:iron_sword", 16);
        ITEM_STACK_OVERRIDES.put("minecraft:netherite_sword", 16);
        ITEM_STACK_OVERRIDES.put("minecraft:bow", 16);
        ITEM_STACK_OVERRIDES.put("minecraft:crossbow", 16);
        ITEM_STACK_OVERRIDES.put("minecraft:shield", 16);
        ITEM_STACK_OVERRIDES.put("minecraft:diamond_pickaxe", 16);
        ITEM_STACK_OVERRIDES.put("minecraft:iron_pickaxe", 16);
        ITEM_STACK_OVERRIDES.put("minecraft:elytra", 4);
    }

    public static boolean hasOverride(Identifier itemId) {
        return ITEM_STACK_OVERRIDES.containsKey(itemId.toString());
    }

    public static int getOverride(Identifier itemId) {
        return ITEM_STACK_OVERRIDES.getOrDefault(itemId.toString(), -1);
    }

    // ---------- Breeding ----------

    /**
     * When true, a successful breed between two stacked mobs of the same type
     * does NOT spawn a separate baby entity walking around; instead it adds
     * +1 to the parent stack's count (simulating an "instant growth" offspring
