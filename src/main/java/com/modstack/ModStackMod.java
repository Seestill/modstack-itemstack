package com.modstack;

import com.modstack.config.ModStackConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModStackMod implements ModInitializer {

    public static final String MOD_ID = "modstack";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[ModStack] Initializing mob & item-drop stacking system...");
        LOGGER.info("[ModStack] Ready. Mob merge radius={} blocks, max stack={}, item-drop merge radius={} blocks, item-drop max pile={}, breeding-into-stack={}",
                ModStackConfig.MERGE_RADIUS, ModStackConfig.MAX_MOB_STACK,
                ModStackConfig.ITEM_DROP_MERGE_RADIUS, ModStackConfig.ITEM_DROP_MAX_STACK,
                ModStackConfig.BREEDING_ADDS_TO_STACK);
    }
}
