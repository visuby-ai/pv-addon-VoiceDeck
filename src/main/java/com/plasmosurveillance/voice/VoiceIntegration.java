package com.plasmosurveillance.voice;

import com.plasmosurveillance.Config;
import com.plasmosurveillance.PlasmoSurveillance;
import com.plasmosurveillance.tape.RecordingSession;
import net.minecraftforge.fml.ModList;
import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.AddonLoaderScope;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.event.EventPriority;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEvent;
import su.plo.voice.api.server.player.VoicePlayer;

import java.util.UUID;

/**
 * Bridges PlasmoVoice's real-time speech events into our (transparent, player-initiated)
 * recording sessions. Only players holding an active Portable Recorder ever have
 * audio persisted, and only for as long as that recorder is visibly running.
 */
@Addon(id = "plasmosurveillance-voice", name = "plasmosurveillance-voice", version = "1.0.0",
        authors = {"plasmosurveillance"}, scope = AddonLoaderScope.SERVER)
public class VoiceIntegration implements AddonInitializer {

    @InjectPlasmoVoice
    public static PlasmoVoiceServer voiceServer;

    private static boolean registered = false;

    public static void init() {
        if (registered) return;
        registered = true;

        // Only attempt PlasmoVoice hooks if the mod is actually present -
        // avoids hard-crashing servers that don't run PlasmoVoice.
        if (!ModList.get().isLoaded("plasmovoice")) {
            PlasmoSurveillance.LOGGER.warn("PlasmoVoice not detected - surveillance items will be inert.");
            return;
        }

        try {
            su.plo.voice.api.addon.ServerAddonsLoader.INSTANCE.load(new VoiceIntegration());
        } catch (Throwable t) {
            PlasmoSurveillance.LOGGER.error("Failed to register PlasmoVoice addon", t);
        }
    }

    @Override
    public void onAddonInitialize() {
        voiceServer.getEventBus().register(
                this,
                PlayerSpeakEvent.class,
                EventPriority.NORMAL,
                this::onPlayerSpeak
        );
        PlaybackManager.registerSourceLine(this);
        PlasmoSurveillance.LOGGER.info("Plasmo Surveillance addon registered with PlasmoVoice.");
    }

    @Override
    public void onAddonShutdown() {
        voiceServer.getEventBus().unregister(this);
    }

    private void onPlayerSpeak(PlayerSpeakEvent event) {
        // Fast path: this fires on every Opus frame from every speaking player
        // (~50/sec per player). If nobody on the server is currently holding an
        // active recorder, there is nothing for us to do - skip decryption,
        // logging, and the player-distance scan entirely. This is the common
        // case and is what was previously running unconditionally on every
        // frame, causing log spam and TPS drops whenever anyone talked.
        if (!RecordingSession.hasAnyActive()) {
            return;
        }

        VoicePlayer speaker = event.getPlayer();
        UUID speakerId = speaker.getInstance().getUuid();

        // Every recorder that is currently running and within capture radius of the
        // speaker gets a copy of this Opus frame appended to its session buffer.
        int radius = Config.RECORDER_CAPTURE_RADIUS.get();
        byte[] encrypted = event.getPacket().getData();
        if (encrypted == null || encrypted.length == 0) return;

        // PlasmoVoice always AES-encrypts audio in transit (privacy against packet
        // sniffing) - getPacket().getData() is ciphertext, NOT raw Opus. We have to
        // decrypt it here before storing, since PlaybackManager later feeds stored
        // frames into addEncodedFrame(), which itself re-encrypts plain Opus for the
        // listener. Storing ciphertext directly means playback double-encrypts it,
        // which decodes as pure static - never actual speech.
        byte[] data;
        try {
            data = voiceServer.getDefaultEncryption().decrypt(encrypted);
        } catch (Exception e) {
            PlasmoSurveillance.LOGGER.warn("Failed to decrypt speech frame from {} (encrypted len={})",
                    speakerId, encrypted.length, e);
            return;
        }

        PlasmoSurveillance.LOGGER.debug("onPlayerSpeak: speaker={} isStereo={} encryptedLen={} decryptedLen={}",
                speakerId, event.getPacket().isStereo(), encrypted.length, data.length);

        final int[] recordersHit = {0};
        RecordingBridge.forEachActiveRecorderNear(speakerId, radius, (recorderOwnerId, session) -> {
            session.addFrame(data);
            recordersHit[0]++;

            int max = Config.MAX_TAPE_LENGTH_SECONDS.get();
            if (session.elapsedSeconds() >= max) {
                RecordingBridge.forceStop(recorderOwnerId, "Max tape length reached");
            }
        });
        if (recordersHit[0] == 0) {
            // Expected whenever a recorder is active somewhere on the server but not
            // within range of *this particular* speaker - not worth INFO-level noise.
            PlasmoSurveillance.LOGGER.debug("onPlayerSpeak: speaker={} - no active recorder in range (radius={})",
                    speakerId, radius);
        }
    }
}
