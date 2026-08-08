package com.modstack.config;

// Central configuration for ModStack ItemStack.
public final class ModStackConfig {

    private ModStackConfig() {}

    // ---------- Mob stacking ----------

    // Max mobs allowed in a single stack.
    public static final int MAX_MOB_STACK = 64;

    // Radius (blocks) mobs merge within, checked every MERGE_INTERVAL_TICKS.
    public static final double MERGE_RADIUS = 4.0D;

    // How close a player must be (blocks) before the stack count nametag shows up.
    public static final double NAMETAG_VISIBLE_RADIUS = 6.0D;

    // How often (in ticks) the server scans for mergeable mobs. 20 ticks = 1s.
    public static final int MERGE_INTERVAL_TICKS = 40;

    // Baby mobs never stack (keeps growth logic sane).
    public static final boolean ALLOW_BABY_STACKING = false;

    // ---------- Item DROP stacking (items lying on the ground) ----------
    // This does NOT affect inventory stack sizes — only how big a pile of
    // dropped items on the ground can get, and how far apart drops can be
    // and still merge into one pile automatically.

    // Max combined count for a single pile of dropped items on the ground.
    public static final int ITEM_DROP_MAX_STACK = 6400;

    // Radius (blocks) dropped items scan for other matching drops to merge with.
    public static final double ITEM_DROP_MERGE_RADIUS = 3.0D;

    // How often (in ticks) each dropped item scans for nearby matches.
    public static final int ITEM_DROP_MERGE_INTERVAL_TICKS = 20;

    // ---------- Breeding ----------

    // When true, a successful breed between two stacked mobs of the same type
    // does NOT spawn a separate baby entity walking around; instead it adds
    // +1 to the parent stack's count.
    public static final boolean BREEDING_ADDS_TO_STACK = true;

    // Chance (0.0-1.0) that breeding grants a bonus extra stack member, for variety.
    public static final double BONUS_OFFSPRING_CHANCE = 0.1D;
}
