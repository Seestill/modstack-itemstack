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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

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
    public static int MAX_MOB_STACK = 64;
    public static double MERGE_RADIUS = 4.0D;
    public static double NAMETAG_VISIBLE_RADIUS = 6.0D;
    public static int MERGE_INTERVAL_TICKS = 40;
    public static int MERGE_MIN_AGE_TICKS = 60;
    public static boolean ALLOW_BABY_STACKING = false;

    // ---------- Item DROP stacking (items lying on the ground) ----------
    public static int ITEM_DROP_MAX_STACK = 6400;
    public static double ITEM_DROP_MERGE_RADIUS = 3.0D;
    public static int ITEM_DROP_MERGE_INTERVAL_TICKS = 20;

    public static boolean SPAWN_REPLACEMENT_ON_DEATH = true;

    // ---------- Breeding ----------
    public static boolean BREEDING_ADDS_TO_STACK = true;
    public static double BONUS_OFFSPRING_CHANCE = 0.1D;

    // ---------- Per-species stacking toggle ----------
    // Full entity id ("minecraft:zombie") -> whether that species is
    // allowed to stack at all. Missing entries default to true. Villagers
    // and wandering traders are always excluded regardless of this map
    // (their unique trades would be destroyed on merge), and bosses
    // (Ender Dragon, Wither) are left out of the list entirely since only
    // one of each normally exists.
    public static Map<String, Boolean> MOB_STACKING_ENABLED = new LinkedHashMap<>();

    private static final String[] DEFAULT_MOB_IDS = {
        "minecraft:allay", "minecraft:axolotl", "minecraft:bat", "minecraft:bee",
        "minecraft:blaze", "minecraft:camel", "minecraft:cat", "minecraft:cave_spider",
        "minecraft:chicken", "minecraft:cod", "minecraft:cow", "minecraft:creeper",
        "minecraft:dolphin", "minecraft:donkey", "minecraft:drowned",
        "minecraft:elder_guardian", "minecraft:enderman", "minecraft:endermite",
        "minecraft:evoker", "minecraft:fox", "minecraft:frog", "minecraft:ghast",
        "minecraft:glow_squid", "minecraft:goat", "minecraft:guardian",
        "minecraft:hoglin", "minecraft:horse", "minecraft:husk",
        "minecraft:illusioner", "minecraft:iron_golem", "minecraft:llama",
        "minecraft:magma_cube", "minecraft:mooshroom", "minecraft:mule",
        "minecraft:ocelot", "minecraft:panda", "minecraft:parrot",
        "minecraft:phantom", "minecraft:pig", "minecraft:piglin",
        "minecraft:piglin_brute", "minecraft:pillager", "minecraft:polar_bear",
        "minecraft:pufferfish", "minecraft:rabbit", "minecraft:ravager",
        "minecraft:salmon", "minecraft:sheep", "minecraft:shulker",
        "minecraft:silverfish", "minecraft:skeleton", "minecraft:skeleton_horse",
        "minecraft:slime", "minecraft:sniffer", "minecraft:snow_golem",
        "minecraft:spider", "minecraft:squid", "minecraft:stray",
        "minecraft:strider", "minecraft:tadpole", "minecraft:trader_llama",
        "minecraft:tropical_fish", "minecraft:turtle", "minecraft:vex",
        "minecraft:vindicator", "minecraft:warden", "minecraft:witch",
        "minecraft:wither_skeleton", "minecraft:wolf", "minecraft:zoglin",
        "minecraft:zombie", "minecraft:zombie_villager", "minecraft:zombified_piglin"
    };

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

        Map<String, Boolean> mobStackingEnabled = new LinkedHashMap<>();
    }

    private static Map<String, Boolean> defaultMobMap() {
        Map<String, Boolean> map = new TreeMap<>();
        for (String id : DEFAULT_MOB_IDS) {
            map.put(id, true);
        }
        return map;
    }

    public static void load() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("modstack.json");
        Data data = new Data();
        data.mobStackingEnabled = defaultMobMap();

        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                Data loaded = GSON.fromJson(reader, Data.class);
                if (loaded != null) {
                    Map<String, Boolean> merged = defaultMobMap();
                    if (loaded.mobStackingEnabled != null) {
                        merged.putAll(loaded.mobStackingEnabled);
                    }
                    loaded.mobStackingEnabled = merged;
                    data = loaded;
                }
            } catch (IOException | JsonSyntaxException e) {
                com.modstack.ModStackMod.LOGGER.warn("[ModStack] Failed to read config, using defaults: {}", e.getMessage());
            }
        }
        applyData(data);
        save(data);
    }

    public static void reload() {
        load();
    }

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
        data.mobStackingEnabled = new TreeMap<>(MOB_STACKING_ENABLED);
        save(data);
    }

    public static boolean isSpeciesStackingEnabled(String entityId) {
        Boolean enabled = MOB_STACKING_ENABLED.get(entityId);
        return enabled == null || enabled;
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

        MOB_STACKING_ENABLED = new LinkedHashMap<>(data.mobStackingEnabled != null ? data.mobStackingEnabled : defaultMobMap());
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
