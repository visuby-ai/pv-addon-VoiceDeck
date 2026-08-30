package com.plasmosurveillance.network;

import com.plasmosurveillance.item.ModItems;
import com.plasmosurveillance.item.TapePlayerItem;
import com.plasmosurveillance.tape.TapeData;
import com.plasmosurveillance.voice.PlaybackManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Client -> server controls for the Tape Player settings screen. */
public class TapePlayerActionPacket {
    public static final int SELECT = 0, RANGE = 1, PLAY = 2;
    private final int action;
    private final InteractionHand hand;
    private final UUID tapeId;
    private final int range;

    public TapePlayerActionPacket(int action, InteractionHand hand, UUID tapeId, int range) {
        this.action = action; this.hand = hand; this.tapeId = tapeId; this.range = range;
    }

    public static void encode(TapePlayerActionPacket p, FriendlyByteBuf b) {
        b.writeVarInt(p.action); b.writeEnum(p.hand); b.writeBoolean(p.tapeId != null);
        if (p.tapeId != null) b.writeUUID(p.tapeId); b.writeVarInt(p.range);
    }
    public static TapePlayerActionPacket decode(FriendlyByteBuf b) {
        int a=b.readVarInt(); InteractionHand h=b.readEnum(InteractionHand.class);
        UUID id=b.readBoolean()?b.readUUID():null; int r=b.readVarInt();
        return new TapePlayerActionPacket(a,h,id,r);
    }
    public static void handle(TapePlayerActionPacket p, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx=ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player=ctx.getSender(); if(player==null) return;
            ItemStack playerStack=player.getItemInHand(p.hand);
            if(!playerStack.is(ModItems.TAPE_PLAYER.get())) return;
            if(p.action==SELECT && p.tapeId!=null) {
                playerStack.getOrCreateTag().putUUID(TapePlayerItem.SELECTED_TAPE, p.tapeId);
            } else if(p.action==RANGE) {
                playerStack.getOrCreateTag().putInt(TapePlayerItem.RANGE, Math.max(1, Math.min(64,p.range)));
            } else if(p.action==PLAY) {
                UUID id=playerStack.getTag()!=null && playerStack.getTag().hasUUID(TapePlayerItem.SELECTED_TAPE)
                        ? playerStack.getTag().getUUID(TapePlayerItem.SELECTED_TAPE) : null;
                ItemStack tape=findTape(player,id);
                if(!tape.isEmpty()) {
                    TapeData data=TapeData.readFromStack(tape);
                    int range=playerStack.getTag()!=null && playerStack.getTag().contains(TapePlayerItem.RANGE)
                            ? playerStack.getTag().getInt(TapePlayerItem.RANGE) : 16;
                    if(data!=null) PlaybackManager.startPlayback(player,data,range,()->{});
                }
            }
        });
        ctx.setPacketHandled(true);
    }

    public static ItemStack findTape(ServerPlayer player, UUID id) {
        for(ItemStack s:player.getInventory().items) {
            if(!s.is(ModItems.RECORDED_TAPE.get())) continue;
            TapeData d=TapeData.readFromStack(s);
            if(d!=null && (id==null || id.equals(d.tapeId))) return s;
        }
        return ItemStack.EMPTY;
    }
}
