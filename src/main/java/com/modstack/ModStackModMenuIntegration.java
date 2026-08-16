package com.modstack;

import com.modstack.config.ModStackConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

// Renders a Cloth Config screen (opened from Mod Menu) for editing
// ModStackConfig live. Saving here writes straight to config/modstack.json
// (same file "/modstack reload" reads) so both ways of editing stay in sync.
public class ModStackModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("ModStack ItemStack"));

            ConfigEntryBuilder entry = builder.entryBuilder();

            ConfigCategory mobs = builder.getOrCreateCategory(Text.literal("Mob Stacking"));
            mobs.addEntry(entry.startIntField(Text.literal("Max stack size"), ModStackConfig.MAX_MOB_STACK)
                    .setMin(1).setMax(10000)
                    .setSaveConsumer(v -> ModStackConfig.MAX_MOB_STACK = v)
                    .build());
            mobs.addEntry(entry.startDoubleField(Text.literal("Merge radius (blocks)"), ModStackConfig.MERGE_RADIUS)
                    .setMin(0.5).setMax(64.0)
                    .setSaveConsumer(v -> ModStackConfig.MERGE_RADIUS = v)
                    .build());
            mobs.addEntry(entry.startDoubleField(Text.literal("Nametag visible radius (blocks)"), ModStackConfig.NAMETAG_VISIBLE_RADIUS)
                    .setMin(0.5).setMax(64.0)
                    .setSaveConsumer(v -> ModStackConfig.NAMETAG_VISIBLE_RADIUS = v)
                    .build());
            mobs.addEntry(entry.startIntField(Text.literal("Merge scan interval (ticks)"), ModStackConfig.MERGE_INTERVAL_TICKS)
                    .setMin(1).setMax(2000)
                    .setSaveConsumer(v -> ModStackConfig.MERGE_INTERVAL_TICKS = v)
                    .build());
            mobs.addEntry(entry.startIntField(Text.literal("Minimum age before merging (ticks)"), ModStackConfig.MERGE_MIN_AGE_TICKS)
                    .setMin(0).setMax(2000)
                    .setSaveConsumer(v -> ModStackConfig.MERGE_MIN_AGE_TICKS = v)
                    .build());
            mobs.addEntry(entry.startBooleanToggle(Text.literal("Allow baby mobs to stack"), ModStackConfig.ALLOW_BABY_STACKING)
                    .setSaveConsumer(v -> ModStackConfig.ALLOW_BABY_STACKING = v)
                    .build());
            mobs.addEntry(entry.startBooleanToggle(Text.literal("Spawn replacement on death"), ModStackConfig.SPAWN_REPLACEMENT_ON_DEATH)
                    .setTooltip(Text.literal("Turn OFF for mob-grinder farms relying on fall damage — a replacement always spawns at full health."))
                    .setSaveConsumer(v -> ModStackConfig.SPAWN_REPLACEMENT_ON_DEATH = v)
                    .build());

            ConfigCategory items = builder.getOrCreateCategory(Text.literal("Item Drops"));
            items.addEntry(entry.startIntField(Text.literal("Max pile size on ground"), ModStackConfig.ITEM_DROP_MAX_STACK)
                    .setMin(1).setMax(1000000)
                    .setSaveConsumer(v -> ModStackConfig.ITEM_DROP_MAX_STACK = v)
                    .build());
            items.addEntry(entry.startDoubleField(Text.literal("Merge radius (blocks)"), ModStackConfig.ITEM_DROP_MERGE_RADIUS)
                    .setMin(0.5).setMax(64.0)
                    .setSaveConsumer(v -> ModStackConfig.ITEM_DROP_MERGE_RADIUS = v)
                    .build());
            items.addEntry(entry.startIntField(Text.literal("Merge scan interval (ticks)"), ModStackConfig.ITEM_DROP_MERGE_INTERVAL_TICKS)
                    .setMin(1).setMax(2000)
                    .setSaveConsumer(v -> ModStackConfig.ITEM_DROP_MERGE_INTERVAL_TICKS = v)
                    .build());

            ConfigCategory breeding = builder.getOrCreateCategory(Text.literal("Breeding"));
            breeding.addEntry(entry.startBooleanToggle(Text.literal("Breeding grows the stack"), ModStackConfig.BREEDING_ADDS_TO_STACK)
                    .setSaveConsumer(v -> ModStackConfig.BREEDING_ADDS_TO_STACK = v)
                    .build());
            breeding.addEntry(entry.startDoubleField(Text.literal("Bonus offspring chance (0.0-1.0)"), ModStackConfig.BONUS_OFFSPRING_CHANCE)
                    .setMin(0.0).setMax(1.0)
                    .setSaveConsumer(v -> ModStackConfig.BONUS_OFFSPRING_CHANCE = v)
                    .build());

            ConfigCategory perMob = builder.getOrCreateCategory(Text.literal("Per-Mob Stacking"));
            for (String mobId : ModStackConfig.MOB_STACKING_ENABLED.keySet()) {
                boolean current = ModStackConfig.MOB_STACKING_ENABLED.getOrDefault(mobId, true);
                perMob.addEntry(entry.startBooleanToggle(Text.literal(mobId), current)
                        .setSaveConsumer(v -> ModStackConfig.MOB_STACKING_ENABLED.put(mobId, v))
                        .build());
            }

            builder.setSavingRunnable(ModStackConfig::saveCurrent);
            return builder.build();
        };
    }
}
