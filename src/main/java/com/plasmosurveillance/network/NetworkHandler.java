package com.plasmosurveillance.network;

import com.plasmosurveillance.PlasmoSurveillance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PlasmoSurveillance.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    private static int nextId() {
        return id++;
    }

    private static boolean packetsRegistered = false;

    /** Call once from the mod constructor. */
    public static void register() {
        if (packetsRegistered) return;
        packetsRegistered = true;

        CHANNEL.registerMessage(
                nextId(),
                RecordingStatePacket.class,
                RecordingStatePacket::encode,
                RecordingStatePacket::decode,
                RecordingStatePacket::handle
        );

        CHANNEL.registerMessage(
                nextId(),
                TapePlayerActionPacket.class,
                TapePlayerActionPacket::encode,
                TapePlayerActionPacket::decode,
                TapePlayerActionPacket::handle
        );

        CHANNEL.registerMessage(nextId(), TapeImportBeginPacket.class,
                TapeImportBeginPacket::encode, TapeImportBeginPacket::decode, TapeImportBeginPacket::handle);
        CHANNEL.registerMessage(nextId(), TapeImportChunkPacket.class,
                TapeImportChunkPacket::encode, TapeImportChunkPacket::decode, TapeImportChunkPacket::handle);
        CHANNEL.registerMessage(nextId(), TapeImportEndPacket.class,
                TapeImportEndPacket::encode, TapeImportEndPacket::decode, TapeImportEndPacket::handle);
    }
}
