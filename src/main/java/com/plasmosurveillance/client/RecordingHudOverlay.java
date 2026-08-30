package com.plasmosurveillance.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.plasmosurveillance.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraft.network.chat.Component;

public class RecordingHudOverlay implements IGuiOverlay {

    public static final RecordingHudOverlay INSTANCE = new RecordingHudOverlay();

    private RecordingHudOverlay() {}

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics,
                        float partialTick, int screenWidth, int screenHeight) {
        if (!Config.SHOW_RECORDING_HUD.get()) return;
        if (!ClientRecordingState.isRecording()) return;

        int seconds = ClientRecordingState.elapsedSeconds();
        String timer = String.format("%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);

        // Blink the dot roughly once per second, like a real recorder LED.
        boolean dotVisible = (System.currentTimeMillis() / 500L) % 2 == 0;
        String text = (dotVisible ? "\u25CF" : " ") + " REC " + timer;

        int x = 10;
        int y = 10;

        guiGraphics.drawString(
                net.minecraft.client.Minecraft.getInstance().font,
                Component.literal(text),
                x, y,
                0xFFFF3B30,
                true
        );
    }
}
