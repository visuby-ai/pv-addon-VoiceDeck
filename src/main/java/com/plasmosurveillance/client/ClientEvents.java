package com.plasmosurveillance.client;

import com.plasmosurveillance.PlasmoSurveillance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PlasmoSurveillance.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("recording_hud", RecordingHudOverlay.INSTANCE);
    }
}
