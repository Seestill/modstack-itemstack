package com.modstack.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

// Central configuration for ModStack ItemStack.
// Values are loaded from <minecraft>/config/modstack.json at startup
// (created with defaults on first run) so they can be tweaked without
// recompiling. Edit the file, then run "/modstack reload" in-game (needs
// op/permission level 2) to apply changes without restarting.
public final class ModStackConfig {

    private ModStackConfig() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;

    // ---------- Mob stacking ----------
    public static int MAX_MOB_STACK = 128;
    public static double MERGE_RADIUS = 4.0D;
    public static double NAMETAG_VISIBLE_RADIUS = 6.0D;
    public static int MERGE_INTERVAL_TICKS = 40;
    public static int MERGE_MIN_AGE_TICKS = 60;
    public static boolean ALLOW_BABY_STACKING = true;

    // ---------- Item DROP stacking (items lying on the ground) ----------
    public static int ITEM_DROP_MAX_STACK = 6400;
    public static double ITEM_DROP_MERGE_RADIUS = 3.0D;
    public static int ITEM_DROP_MERGE_INTERVAL_TICKS = 20;

    // When a stack representative dies, spawn a replacement to keep the
    // stack's color/pattern visible. Turn this OFF for mob-grinder farms
    // that rely on fall damage leaving mobs at low health — a replacement
    // always spawns at full health, which breaks weak auto-kill mechanisms
    // tuned for near-dead mobs. With this off, killing just shrinks the
    // stack count; fresh, not-yet-merged mobs from the spawner keep the
    // farm fed instead.
    public static boolean SPAWN_REPLACEMENT_ON_DEATH = true;

    // ---------- Breeding ----------
    public static boolean BREEDING_ADDS_TO_STACK = true;
    public static double BONUS_OFFSPRING_CHANCE = 0.1D;

    // Plain data holder mirrored to/from JSON. Field names here become the
    // JSON keys in config/modstack.json.
    private static final class Data {
        int maxMobStack = MAX_MOB_STACK;
        double mergeRadius = MERGE_RADIUS;
        double nametagVisibleRadius = NAMETAG_VISIBLE_RADIUS;
        int mergeIntervalTicks = MERGE_INTERVAL_TICKS;
        int mergeMinAgeTicks = MERGE_MIN_AGE_TICKS;
        boolean allowBabyStacking = ALLOW_BABY_STACKING;

        int itemDropMaxStack = ITEM_DROP_MAX_STACK;
        double itemDropMergeRadius = ITEM_DROP_MERGE_RADIUS;
        int itemDropMergeIntervalTicks = ITEM_DROP_MERGE_INTERVAL_TICKS;

        boolean spawnReplacementOnDeath = SPAWN_REPLACEMENT_ON_DEATH;

        boolean breedingAddsToStack = BREEDING_ADDS_TO_STACK;
        double bonusOffspringChance = BONUS_OFFSPRING_CHANCE;
    }

    public static void load() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("modstack.json");
        Data data = new Data();
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                Data loaded = GSON.fromJson(reader, Data.class);
                if (loaded != null) data = loaded;
            } catch (IOException | JsonSyntaxException e) {
                com.modstack.ModStackMod.LOGGER.warn("[ModStack] Failed to read config, using defaults: {}", e.getMessage());
            }
        }
        applyData(data);
        save(data); // rewrite so any new/missing fields get filled in with defaults
    }

    // Re-reads the file from disk; used by "/modstack reload".
    public static void reload() {
        load();
    }

    // Writes the CURRENT in-memory values back to disk; used by the Mod
    // Menu / Cloth Config screen's save button.
    public static void saveCurrent() {
        Data data = new Data();
        data.maxMobStack = MAX_MOB_STACK;
        data.mergeRadius = MERGE_RADIUS;
        data.nametagVisibleRadius = NAMETAG_VISIBLE_RADIUS;
        data.mergeIntervalTicks = MERGE_INTERVAL_TICKS;
        data.mergeMinAgeTicks = MERGE_MIN_AGE_TICKS;
        data.allowBabyStacking = ALLOW_BABY_STACKING;
        data.itemDropMaxStack = ITEM_DROP_MAX_STACK;
        data.itemDropMergeRadius = ITEM_DROP_MERGE_RADIUS;
        data.itemDropMergeIntervalTicks = ITEM_DROP_MERGE_INTERVAL_TICKS;
        data.spawnReplacementOnDeath = SPAWN_REPLACEMENT_ON_DEATH;
        data.breedingAddsToStack = BREEDING_ADDS_TO_STACK;
        data.bonusOffspringChance = BONUS_OFFSPRING_CHANCE;
        save(data);
    }

    private static void applyData(Data data) {
        MAX_MOB_STACK = data.maxMobStack;
        MERGE_RADIUS = data.mergeRadius;
        NAMETAG_VISIBLE_RADIUS = data.nametagVisibleRadius;
        MERGE_INTERVAL_TICKS = data.mergeIntervalTicks;
        MERGE_MIN_AGE_TICKS = data.mergeMinAgeTicks;
        ALLOW_BABY_STACKING = data.allowBabyStacking;

        ITEM_DROP_MAX_STACK = data.itemDropMaxStack;
        ITEM_DROP_MERGE_RADIUS = data.itemDropMergeRadius;
        ITEM_DROP_MERGE_INTERVAL_TICKS = data.itemDropMergeIntervalTicks;

        SPAWN_REPLACEMENT_ON_DEATH = data.spawnReplacementOnDeath;

        BREEDING_ADDS_TO_STACK = data.breedingAddsToStack;
        BONUS_OFFSPRING_CHANCE = data.bonusOffspringChance;
    }

    private static void save(Data data) {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            com.modstack.ModStackMod.LOGGER.warn("[ModStack] Failed to write config: {}", e.getMessage());
        }
    }
}
