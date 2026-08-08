package com.modstack;

import com.modstack.config.ModStackConfig;
import com.modstack.mixin.ItemAccessorMixin;
import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModStackMod implements ModInitializer {

    public static final String MOD_ID = "modstack";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[ModStack] Initializing mob & item stacking system...");
        applyItemStackOverrides();
        LOGGER.info("[ModStack] Ready. Mob merge radius={} blocks, max stack={}, breeding-into-stack={}",
                ModStackConfig.MERGE_RADIUS, ModStackConfig.MAX_MOB_STACK, ModStackConfig.BREEDING_ADDS_TO_STACK);
    }

    /**
     * Rewrites Item.maxCount for every item listed in ModStackConfig, using
     * the accessor mixin so we don't need to re-register items.
     */
    private void applyItemStackOverrides() {
        int changed = 0;
        for (var entry : ModStackConfig.ITEM_STACK_OVERRIDES.entrySet()) {
            Identifier id = new Identifier(entry.getKey());
            Item item = Registries.ITEM.get(id);
            if (item == Registries.ITEM.get(Identifier.of("minecraft", "air")) && !entry.getKey().equals("minecraft:air")) {
                LOGGER.warn("[ModStack] Unknown item id in config: {}", entry.getKey());
                continue;
            }
            ((ItemAccessorMixin) item).modstack$setMaxCount(entry.getValue());
            changed++;
        }
        LOGGER.info("[ModStack] Applied {} item stack-size overrides.", changed);
    }
}
