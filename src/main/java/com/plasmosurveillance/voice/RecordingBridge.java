package com.plasmosurveillance.voice;

import com.plasmosurveillance.PlasmoSurveillance;
import com.plasmosurveillance.item.ModItems;
import com.plasmosurveillance.tape.RecordingSession;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Server-side glue between "who is speaking right now" (PlasmoVoice) and
 * "which nearby players currently have a running Portable Recorder"
 * (our RecordingSession registry).
 */
public class RecordingBridge {

    /**
     * Calls the given consumer for every player currently running a recorder
     * within `radius` blocks of the speaker, in the same dimension.
     */
    public static void forEachActiveRecorderNear(UUID speakerId, int radius,
                                                   BiConsumer<UUID, RecordingSession> consumer) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        ServerPlayer speaker = server.getPlayerList().getPlayer(speakerId);
        if (speaker == null) return;

        List<ServerPlayer> allPlayers = server.getPlayerList().getPlayers();
        double radiusSq = (double) radius * radius;

        for (ServerPlayer candidate : allPlayers) {
            UUID candidateId = candidate.getUUID();
            RecordingSession session = RecordingSession.get(candidateId);
            if (session == null) continue;

            // Must actually still be holding the recorder (either hand) to keep capturing -
            // dropping/stowing it does not silently keep the mic hot.
            if (!isHoldingRecorder(candidate)) continue;

            if (candidate.level() != speaker.level()) continue;
            if (candidate.distanceToSqr(speaker) > radiusSq) continue;

            consumer.accept(candidateId, session);
        }
    }

    private static boolean isHoldingRecorder(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.is(ModItems.PORTABLE_RECORDER.get()) || off.is(ModItems.PORTABLE_RECORDER.get());
    }

    public static void forceStop(UUID recorderOwnerId, String reason) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(recorderOwnerId);
        if (player != null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§7[Recorder] Recording stopped automatically: " + reason));
        }
        PlasmoSurveillance.LOGGER.debug("Force-stopping recording for {} ({})", recorderOwnerId, reason);
        // Actual tape finalization is handled by PortableRecorderItem on next interaction,
        // or immediately here if you want auto-finalize - left as a hook point for Phase 1.5.
        com.plasmosurveillance.item.PortableRecorderItem.finalizeRecording(player);
    }
}
