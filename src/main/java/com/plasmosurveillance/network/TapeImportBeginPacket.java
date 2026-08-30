package com.plasmosurveillance.network;

import com.plasmosurveillance.PlasmoSurveillance;
import com.plasmosurveillance.item.ModItems;
import com.plasmosurveillance.tape.TapeData;
import com.plasmosurveillance.voice.VoiceIntegration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import su.plo.voice.api.audio.codec.AudioEncoder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Starts a client -> server PCM audio import. PCM is transient; audio is immediately encoded to Opus server-side. */
public class TapeImportBeginPacket {
    public static final Map<UUID, ImportSession> SESSIONS = new ConcurrentHashMap<>();
    private final UUID importId;
    private final String fileName;

    public TapeImportBeginPacket(UUID importId, String fileName) { this.importId = importId; this.fileName = fileName; }

    public static void encode(TapeImportBeginPacket p, FriendlyByteBuf b) {
        b.writeUUID(p.importId);
        b.writeUtf(p.fileName, 128);
    }
    public static TapeImportBeginPacket decode(FriendlyByteBuf b) {
        return new TapeImportBeginPacket(b.readUUID(), b.readUtf(128));
    }
    public static void handle(TapeImportBeginPacket p, Supplier<NetworkEvent.Context> sup) {
        NetworkEvent.Context ctx=sup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player=ctx.getSender(); if(player==null) return;
            if(SESSIONS.containsKey(player.getUUID())) return;
            try {
                if(VoiceIntegration.voiceServer == null) return;
                AudioEncoder encoder=VoiceIntegration.voiceServer.createOpusEncoder(false);
                encoder.open();
                ImportSession session=new ImportSession(player, p.importId, cleanName(p.fileName), encoder);
                SESSIONS.put(player.getUUID(), session);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7Importing audio: §f"+session.fileName));
            } catch(Throwable t) {
                PlasmoSurveillance.LOGGER.error("Could not start tape import", t);
            }
        });
        ctx.setPacketHandled(true);
    }

    private static String cleanName(String s) {
        String n=s.replace('\\','_').replace('/','_').replaceAll("[\\\\:*?\"<>|]", "_").trim();
        if(n.isEmpty()) n="Imported Audio";
        return n.length()>80?n.substring(0,80):n;
    }

    public static class ImportSession {
        public final ServerPlayer player;
        public final UUID id;
        public final String fileName;
        public final AudioEncoder encoder;
        public final TapeData tape=new TapeData();
        public short[] pending=new short[0];
        public long totalSamples=0;
        public int chunks=0;
        public long bytes=0;
        public ImportSession(ServerPlayer player, UUID id, String fileName, AudioEncoder encoder) {
            this.player=player; this.id=id; this.fileName=fileName; this.encoder=encoder;
            tape.ownerId=player.getUUID(); tape.createdTimeMillis=System.currentTimeMillis();
            tape.sourceType=TapeData.SourceType.IMPORTED;
        }
    }
}
