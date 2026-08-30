package com.plasmosurveillance.network;

import com.plasmosurveillance.client.ClientRecordingState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RecordingStatePacket {

    private final boolean recording;
    private final int elapsedSeconds;

    public RecordingStatePacket(boolean recording, int elapsedSeconds) {
        this.recording = recording;
        this.elapsedSeconds = elapsedSeconds;
    }

    public static void encode(RecordingStatePacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.recording);
        buf.writeVarInt(packet.elapsedSeconds);
    }

    public static RecordingStatePacket decode(FriendlyByteBuf buf) {
        return new RecordingStatePacket(buf.readBoolean(), buf.readVarInt());
    }

    public static void handle(RecordingStatePacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientRecordingState.set(packet.recording, packet.elapsedSeconds))
        );
        ctx.setPacketHandled(true);
    }
}
