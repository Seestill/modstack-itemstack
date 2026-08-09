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

    // A mob must exist for at least this many ticks before it's eligible to
    // merge into (or absorb) a stack. Prevents mobs merging instantly
    // mid-air, which breaks mob grinders/farms that rely on individual mobs.
    public static final int MERGE_MIN_AGE_TICKS = 60;

    // Baby mobs never stack (keeps growth logic sane).
    public static final boolean ALLOW_BABY_STACKING = false;

    // ---------- Item DROP stacking (items lying on the ground) ----------

    // Max combined count for a single pile of dropped items on the ground.
    public static final int ITEM_DROP_MAX_STACK = 6400;

    // Radius (blocks) dropped items scan for other matching drops to merge with.
    public static final double ITEM_DROP_MERGE_RADIUS = 3.0D;

    // How often (in ticks) each dropped item scans for nearby matches.
    public static final int ITEM_DROP_MERGE_INTERVAL_TICKS = 20;

    // ---------- Breeding ----------

    public static final boolean BREEDING_ADDS_TO_STACK = true;
    public static final double BONUS_OFFSPRING_CHANCE = 0.1D;
}
