package com.plasmosurveillance.network;

import com.plasmosurveillance.PlasmoSurveillance;
import com.plasmosurveillance.item.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class TapeImportEndPacket {
    private final UUID importId;
    public TapeImportEndPacket(UUID importId){this.importId=importId;}
    public static void encode(TapeImportEndPacket p,FriendlyByteBuf b){b.writeUUID(p.importId);}
    public static TapeImportEndPacket decode(FriendlyByteBuf b){return new TapeImportEndPacket(b.readUUID());}
    public static void handle(TapeImportEndPacket p,Supplier<NetworkEvent.Context> sup){
        NetworkEvent.Context ctx=sup.get();
        ctx.enqueueWork(() -> finish(ctx.getSender(),p.importId));
        ctx.setPacketHandled(true);
    }
    public static void finish(ServerPlayer player, UUID id){
        if(player==null)return;
        TapeImportBeginPacket.ImportSession s=TapeImportBeginPacket.SESSIONS.remove(player.getUUID());
        if(s==null || !s.id.equals(id))return;
        try{
            if(s.pending.length>0){
                short[] frame=new short[960]; System.arraycopy(s.pending,0,frame,0,s.pending.length);
                s.tape.audioFrames.add(s.encoder.encode(frame)); s.totalSamples+=s.pending.length;
            }
            s.encoder.close();
            if(s.tape.audioFrames.isEmpty()){player.sendSystemMessage(Component.literal("§cAudio import failed: no audio data."));return;}
            s.tape.lengthSeconds=(int)Math.max(1,(s.totalSamples+47999)/48000);
            ItemStack stack=new ItemStack(ModItems.RECORDED_TAPE.get());
            s.tape.writeToStack(stack); stack.setHoverName(Component.literal(s.fileName));
            if(!player.getInventory().add(stack)) player.drop(stack,false);
            // If the player is currently holding a Tape Player, make the newly imported tape the selected tape.
            for(net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
                ItemStack playerItem=player.getItemInHand(hand);
                if(playerItem.is(ModItems.TAPE_PLAYER.get())) {
                    playerItem.getOrCreateTag().putUUID(com.plasmosurveillance.item.TapePlayerItem.SELECTED_TAPE, s.tape.tapeId);
                }
            }
            player.sendSystemMessage(Component.literal("§aImported tape: §f"+s.fileName+" §7("+s.tape.lengthSeconds+"s)"));
        }catch(Throwable t){PlasmoSurveillance.LOGGER.error("Tape import finalize failed",t); player.sendSystemMessage(Component.literal("§cAudio import failed."));}
    }
    public static void abort(ServerPlayer player){
        if(player==null)return; TapeImportBeginPacket.ImportSession s=TapeImportBeginPacket.SESSIONS.remove(player.getUUID());
        if(s!=null)try{s.encoder.close();}catch(Exception ignored){}
    }
}
