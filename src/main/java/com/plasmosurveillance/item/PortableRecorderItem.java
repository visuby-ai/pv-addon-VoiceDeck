package com.plasmosurveillance.item;

import com.plasmosurveillance.network.NetworkHandler;
import com.plasmosurveillance.network.RecordingStatePacket;
import com.plasmosurveillance.tape.RecordingSession;
import com.plasmosurveillance.tape.TapeData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Portable Recorder: a plainly visible item the player holds and toggles on/off.
 * This is a transparent recording device (like a handheld dictaphone) - it is
 * never hidden, and it only records for as long as it is actively held and running.
 */
public class PortableRecorderItem extends Item {

    public PortableRecorderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }

        UUID playerId = player.getUUID();
        if (RecordingSession.isRecording(playerId)) {
            stopRecording(serverPlayer);
        } else {
            startRecording(serverPlayer);
        }

        return InteractionResultHolder.success(stack);
    }

    private void startRecording(ServerPlayer player) {
        RecordingSession.start(player.getUUID());
        player.sendSystemMessage(Component.literal("§c● Recording started."));
        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new RecordingStatePacket(true, 0)
        );
    }

    private void stopRecording(ServerPlayer player) {
        finalizeRecording(player);
    }

    /**
     * Stops any active session for this player and, if they have a Blank Tape,
     * converts it into a Recorded Tape carrying the captured audio + metadata.
     * Safe to call even if no session is active.
     */
    public static void finalizeRecording(ServerPlayer player) {
        UUID playerId = player.getUUID();
        RecordingSession session = RecordingSession.stop(playerId);
        if (session == null) return;

        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new RecordingStatePacket(false, session.elapsedSeconds())
        );

        int length = session.elapsedSeconds();
        if (session.frames.isEmpty() || length <= 0) {
            player.sendSystemMessage(Component.literal("§7Recording stopped - nothing was captured."));
            com.plasmosurveillance.PlasmoSurveillance.LOGGER.info(
                    "finalizeRecording: {} frames captured over {}s for {} - nothing to save",
                    session.frames.size(), length, playerId);
            return;
        }

        long totalBytes = 0;
        for (byte[] f : session.frames) totalBytes += f.length;
        com.plasmosurveillance.PlasmoSurveillance.LOGGER.info(
                "finalizeRecording: {} frames, {} total bytes ({} avg/frame), {}s for {}",
                session.frames.size(), totalBytes,
                session.frames.isEmpty() ? 0 : totalBytes / session.frames.size(), length, playerId);

        ItemStack blankTape = findBlankTape(player);
        if (blankTape.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "§eRecording stopped (" + length + "s), but you have no Blank Tape to save it to! Audio lost."));
            return;
        }

        TapeData data = new TapeData();
        data.ownerId = playerId;
        data.createdTimeMillis = System.currentTimeMillis();
        data.lengthSeconds = length;
        data.sourceType = TapeData.SourceType.RECORDER;
        data.audioFrames.addAll(session.frames);

        blankTape.shrink(1);
        ItemStack recordedTape = new ItemStack(ModItems.RECORDED_TAPE.get());
        data.writeToStack(recordedTape);

        if (!player.getInventory().add(recordedTape)) {
            player.drop(recordedTape, false);
        }

        player.sendSystemMessage(Component.literal(
                "§a■ Recording saved to tape (" + length + "s)."));
    }

    private static ItemStack findBlankTape(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.BLANK_TAPE.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
