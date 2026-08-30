package com.plasmosurveillance.voice;

import com.plasmosurveillance.PlasmoSurveillance;
import com.plasmosurveillance.tape.TapeData;
import net.minecraft.server.level.ServerPlayer;
import su.plo.voice.api.audio.codec.AudioDecoder;
import su.plo.voice.api.server.audio.line.ServerSourceLine;
import su.plo.voice.api.server.audio.provider.ArrayAudioFrameProvider;
import su.plo.voice.api.server.audio.source.AudioSender;
import su.plo.voice.api.server.audio.source.ServerPlayerSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tape playback through a PlasmoVoice player source.
 *
 * A player source is attached to the player itself, so movement no longer
 * requires setPosition() every server tick. This avoids position-update
 * jitter while preserving normal proximity audio.
 */
public class PlaybackManager {
    private static ServerSourceLine tapeSourceLine;
    private static final Map<UUID, ServerPlayerSource> ACTIVE_PLAYBACKS = new HashMap<>();

    public static void registerSourceLine(Object addonInstance) {
        try {
            tapeSourceLine = (ServerSourceLine) VoiceIntegration.voiceServer
                    .getSourceLineManager()
                    .createBuilder(addonInstance, "tape_player", "Tape Player", "plasmosurveillance:textures/item/tape_player.png", 10)
                    .build();
            PlasmoSurveillance.LOGGER.info("Registered PlasmoVoice source line for tape playback.");
        } catch (Throwable t) {
            PlasmoSurveillance.LOGGER.error("Could not register PlasmoVoice source line.", t);
        }
    }

    public static boolean isPlaying(UUID playerId) { return ACTIVE_PLAYBACKS.containsKey(playerId); }

    public static boolean startPlayback(ServerPlayer player, TapeData tape, int range, Runnable onFinished) {
        if (tapeSourceLine == null || tape.audioFrames.isEmpty()) return false;
        if (ACTIVE_PLAYBACKS.containsKey(player.getUUID())) return false;
        final int playbackRange = Math.max(1, Math.min(64, range));

        try {
            AudioDecoder decoder = VoiceIntegration.voiceServer.createOpusDecoder(false);
            decoder.open();
            java.util.List<short[]> chunks = new java.util.ArrayList<>(tape.audioFrames.size());
            int totalSamples = 0;
            int failures = 0;
            for (byte[] frame : tape.audioFrames) {
                try {
                    short[] pcm = decoder.decode(frame);
                    if (pcm != null && pcm.length > 0) { chunks.add(pcm); totalSamples += pcm.length; }
                } catch (Exception e) { failures++; }
            }
            decoder.close();

            short[] allSamples = new short[totalSamples];
            int offset = 0;
            for (short[] chunk : chunks) { System.arraycopy(chunk, 0, allSamples, offset, chunk.length); offset += chunk.length; }
            if (allSamples.length == 0) return false;

            ArrayAudioFrameProvider provider = new ArrayAudioFrameProvider(VoiceIntegration.voiceServer, false);
            provider.addSamples(allSamples);
            provider.setLoop(false);

            su.plo.voice.proto.data.audio.codec.CodecInfo codecInfo =
                    new su.plo.voice.proto.data.audio.codec.CodecInfo("opus", java.util.Collections.emptyMap());

            // Entity/player-attached source: movement is handled by PlasmoVoice,
            // not by our server tick loop.
            su.plo.voice.api.server.player.VoiceServerPlayer voicePlayer =
                    VoiceIntegration.voiceServer.getPlayerManager().getPlayerByInstance(player);

            tapeSourceLine.createPlayerSource(voicePlayer, false, codecInfo, source -> {
                ACTIVE_PLAYBACKS.put(player.getUUID(), source);

                // Remove the default self-filter so the person holding the player
                // can hear the tape too. The distance is the user-selected range.
                source.getFilters().stream().findFirst().ifPresent(source::removeFilter);

                AudioSender sender = source.createAudioSender(provider, (short) playbackRange);
                sender.onStop(() -> {
                    provider.close();
                    source.remove();
                    ACTIVE_PLAYBACKS.remove(player.getUUID());
                    if (onFinished != null) onFinished.run();
                });
                sender.start();
            });
            return true;
        } catch (Throwable t) {
            PlasmoSurveillance.LOGGER.error("Tape playback failed", t);
            return false;
        }
    }

    public static void stopPlayback(UUID playerId) {
        ServerPlayerSource source = ACTIVE_PLAYBACKS.remove(playerId);
        if (source != null) source.remove();
    }
}
