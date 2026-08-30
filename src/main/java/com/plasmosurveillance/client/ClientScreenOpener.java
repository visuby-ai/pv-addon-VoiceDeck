package com.plasmosurveillance.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

/**
 * Client-only entry point for opening the Tape Player screen.
 *
 * IMPORTANT: This class must only ever be referenced from common code via a
 * method reference inside a DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...) call.
 * Never call this class's methods directly, and never import it into a class
 * that also needs to load on a dedicated server (e.g. item classes) — doing so
 * pulls client-only classes (Minecraft, Screen) into that class's bytecode and
 * crashes the server on load with a RuntimeDistCleaner error.
 */
public class ClientScreenOpener {

    public static void openTapePlayerScreen(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new TapePlayerScreen(hand));
    }
}
