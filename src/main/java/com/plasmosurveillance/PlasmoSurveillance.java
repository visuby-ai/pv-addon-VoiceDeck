package com.plasmosurveillance;

import com.plasmosurveillance.item.ModCreativeTabs;
import com.plasmosurveillance.item.ModItems;
import com.plasmosurveillance.network.NetworkHandler;
import com.plasmosurveillance.voice.VoiceIntegration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Plasmo Surveillance
 *
 * Roleplay evidence system built on top of PlasmoVoice.
 * Phase 1: Portable Recorder -> Blank Tape -> Recorded Tape -> Tape Player loop.
 *
 * This is a TRANSPARENT recording system only: recording is something a player
 * actively starts/stops with a visible item, never a hidden/undetectable capture.
 */
@Mod(PlasmoSurveillance.MODID)
public class PlasmoSurveillance {

    public static final String MODID = "plasmosurveillance";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public PlasmoSurveillance() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register deferred registries (items, etc.)
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // Config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Network channel for client<->server sync (e.g. recording HUD state)
        NetworkHandler.register();

        // Hook PlasmoVoice's addon system to capture real audio when a
        // recording is actively (and visibly) in progress.
        VoiceIntegration.init();

        LOGGER.info("Plasmo Surveillance initializing");
    }
}
