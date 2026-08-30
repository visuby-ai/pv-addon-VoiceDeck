package com.plasmosurveillance.client;

public class ClientRecordingState {

    private static volatile boolean recording = false;
    private static volatile long localStartTimeMillis = 0L;

    public static void set(boolean isRecording, int elapsedSecondsAtServer) {
        recording = isRecording;
        if (isRecording) {
            // Approximate the start time locally so the HUD can count up smoothly
            // between server updates, offset by whatever elapsed time the server
            // already reported (handles the "stopped by max length" case too).
            localStartTimeMillis = System.currentTimeMillis() - (elapsedSecondsAtServer * 1000L);
        }
    }

    public static boolean isRecording() {
        return recording;
    }

    public static int elapsedSeconds() {
        if (!recording) return 0;
        return (int) ((System.currentTimeMillis() - localStartTimeMillis) / 1000L);
    }
}
