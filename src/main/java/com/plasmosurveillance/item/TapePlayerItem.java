package com.plasmosurveillance.item;

import com.plasmosurveillance.tape.TapeData;
import com.plasmosurveillance.voice.PlaybackManager;
import net.minecraftforge.fml.DistExecutor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class TapePlayerItem extends Item {
    public static final String SELECTED_TAPE = "SelectedTape";
    public static final String RANGE = "PlaybackRange";

    public TapePlayerItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        if(level.isClientSide) {
            if(player.isShiftKeyDown()) DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> com.plasmosurveillance.client.ClientScreenOpener.openTapePlayerScreen(hand));
            return InteractionResultHolder.success(stack);
        }
        if(!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.success(stack);
        if(serverPlayer.isShiftKeyDown()) return InteractionResultHolder.success(stack);

        if(PlaybackManager.isPlaying(player.getUUID())) {
            PlaybackManager.stopPlayback(player.getUUID());
            player.sendSystemMessage(Component.literal("§7Playback stopped."));
            return InteractionResultHolder.success(stack);
        }

        UUID selected=stack.getTag()!=null && stack.getTag().hasUUID(SELECTED_TAPE) ? stack.getTag().getUUID(SELECTED_TAPE) : null;
        ItemStack tapeStack=findTape(serverPlayer, selected);
        if(tapeStack.isEmpty()) {
            tapeStack=findTape(serverPlayer, null);
            if(!tapeStack.isEmpty()) {
                TapeData d=TapeData.readFromStack(tapeStack);
                if(d!=null) stack.getOrCreateTag().putUUID(SELECTED_TAPE,d.tapeId);
            }
        }
        if(tapeStack.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7No Recorded Tape found. Sneak + right-click to configure."));
            return InteractionResultHolder.fail(stack);
        }
        TapeData data=TapeData.readFromStack(tapeStack);
        int range=stack.getTag()!=null && stack.getTag().contains(RANGE) ? stack.getTag().getInt(RANGE) : 16;
        if(data==null) return InteractionResultHolder.fail(stack);
        boolean started=PlaybackManager.startPlayback(serverPlayer,data,range,()->serverPlayer.sendSystemMessage(Component.literal("§7Playback finished.")));
        if(started) player.sendSystemMessage(Component.literal("§a▶ Playing tape §f"+data.tapeId.toString().substring(0,8)+" §7(range "+range+" blocks)"));
        return InteractionResultHolder.success(stack);
    }

    private static ItemStack findTape(ServerPlayer player, UUID id) {
        for(ItemStack s:player.getInventory().items) {
            if(!s.is(ModItems.RECORDED_TAPE.get())) continue;
            TapeData d=TapeData.readFromStack(s);
            if(d!=null && (id==null || id.equals(d.tapeId))) return s;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        int range=stack.getTag()!=null && stack.getTag().contains(RANGE)?stack.getTag().getInt(RANGE):16;
        tooltip.add(Component.literal("§7Range: §f"+range+"m"));
        tooltip.add(Component.literal("§8Shift + Right Click: settings"));
    }
}
