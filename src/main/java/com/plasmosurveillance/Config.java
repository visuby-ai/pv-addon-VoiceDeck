package com.plasmosurveillance;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue MAX_TAPE_LENGTH_SECONDS;
    public static final ForgeConfigSpec.IntValue RECORDER_CAPTURE_RADIUS;
    public static final ForgeConfigSpec.IntValue MAX_IMPORT_LENGTH_SECONDS;
    public static final ForgeConfigSpec.BooleanValue SHOW_RECORDING_HUD;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Plasmo Surveillance - Portable Recorder settings").push("recorder");

        MAX_TAPE_LENGTH_SECONDS = builder
                .comment("Maximum length of a single recording, in seconds.")
                .defineInRange("max_tape_length_seconds", 300, 5, 3600);

        MAX_IMPORT_LENGTH_SECONDS = builder
                .comment("Maximum length of an imported WAV tape, in seconds. Imported audio is stored as Opus frames, not raw PCM.")
                .defineInRange("max_import_length_seconds", 300, 5, 3600);

        RECORDER_CAPTURE_RADIUS = builder
                .comment("Radius (in blocks) around the recorder within which PlasmoVoice speech is captured.")
                .defineInRange("recorder_capture_radius", 16, 1, 64);

        SHOW_RECORDING_HUD = builder
                .comment("Whether to show the '● REC 00:00' overlay while recording.")
                .define("show_recording_hud", true);

        builder.pop();

        SPEC = builder.build();
    }
}
