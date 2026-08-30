package com.plasmosurveillance.network;

import com.plasmosurveillance.PlasmoSurveillance;
import com.plasmosurveillance.Config;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import su.plo.voice.api.audio.codec.AudioEncoder;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.Supplier;

/** 48 kHz, mono, signed 16-bit little-endian PCM. */
public class TapeImportChunkPacket {
    private final UUID importId;
    private final int sequence;
    private final byte[] pcm;
    public TapeImportChunkPacket(UUID importId,int sequence,byte[] pcm){this.importId=importId;this.sequence=sequence;this.pcm=pcm;}
    public static void encode(TapeImportChunkPacket p,FriendlyByteBuf b){b.writeUUID(p.importId);b.writeVarInt(p.sequence);b.writeByteArray(p.pcm);}
    public static TapeImportChunkPacket decode(FriendlyByteBuf b){return new TapeImportChunkPacket(b.readUUID(),b.readVarInt(),b.readByteArray(32768));}
    public static void handle(TapeImportChunkPacket p,Supplier<NetworkEvent.Context> sup){
        NetworkEvent.Context ctx=sup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player=ctx.getSender(); if(player==null || p.pcm.length==0 || p.pcm.length>32768) return;
            TapeImportBeginPacket.ImportSession s=TapeImportBeginPacket.SESSIONS.get(player.getUUID());
            if(s==null || !s.id.equals(p.importId) || p.sequence!=s.chunks) return;
            // The import limit is time-based, not byte-based. PCM is only transient network data;
            // completed audio is encoded to Opus and stored in the tape NBT.
            final long maxSamples=(long)Config.MAX_IMPORT_LENGTH_SECONDS.get()*48000L;
            try {
                int old=s.pending.length, add=p.pcm.length/2;
                if(s.totalSamples + old + add > maxSamples) {
                    TapeImportEndPacket.abort(player);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cAudio is longer than the " + Config.MAX_IMPORT_LENGTH_SECONDS.get() / 60 + " minute import limit."));
                    return;
                }
                short[] merged=Arrays.copyOf(s.pending,old+add);
                for(int i=0;i<add;i++) merged[old+i]=(short)((p.pcm[i*2]&255)|((p.pcm[i*2+1]&255)<<8));
                int pos=0;
                while(merged.length-pos>=960){
                    short[] frame=Arrays.copyOfRange(merged,pos,pos+960);
                    s.tape.audioFrames.add(s.encoder.encode(frame));
                    s.totalSamples+=960; pos+=960;
                }
                s.pending=Arrays.copyOfRange(merged,pos,merged.length);
                s.bytes+=p.pcm.length; s.chunks++;
            } catch(Throwable t){
                PlasmoSurveillance.LOGGER.error("Tape import chunk failed",t);
                TapeImportEndPacket.abort(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
