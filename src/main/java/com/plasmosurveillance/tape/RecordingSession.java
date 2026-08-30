package com.plasmosurveillance.tape;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks an in-progress recording started by a player holding a Portable Recorder.
 * The recorder's owner is always known to be recording (they hold the visible item
 * and toggled it on) - this is NOT a hidden/covert capture mechanism.
 */
public class RecordingSession {

    // Recorder owner UUID -> active session
    private static final Map<UUID, RecordingSession> ACTIVE = new ConcurrentHashMap<>();

    public final UUID recorderOwner;
    public final long startTimeMillis;
    public final List<byte[]> frames = new ArrayList<>();

    private RecordingSession(UUID recorderOwner) {
        this.recorderOwner = recorderOwner;
        this.startTimeMillis = System.currentTimeMillis();
    }

    public static RecordingSession start(UUID recorderOwner) {
        RecordingSession session = new RecordingSession(recorderOwner);
        ACTIVE.put(recorderOwner, session);
        return session;
    }

    public static RecordingSession get(UUID recorderOwner) {
        return ACTIVE.get(recorderOwner);
    }

    public static boolean isRecording(UUID recorderOwner) {
        return ACTIVE.containsKey(recorderOwner);
    }

    /**
     * Cheap O(1) check for "is anyone on the server recording right now".
     * Used to short-circuit per-voice-frame processing (decryption, logging,
     * player-distance scans) when nobody is holding an active recorder -
     * which is the common case and was previously running unconditionally
     * on every single Opus frame from every speaking player.
     */
    public static boolean hasAnyActive() {
        return !ACTIVE.isEmpty();
    }

    public static RecordingSession stop(UUID recorderOwner) {
        return ACTIVE.remove(recorderOwner);
    }

    public int elapsedSeconds() {
        return (int) ((System.currentTimeMillis() - startTimeMillis) / 1000L);
    }

    public void addFrame(byte[] opusData) {
        frames.add(opusData);
    }
}
