package com.plasmosurveillance.tape;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Metadata + Opus audio frames for a single recorded tape.
 * Stored inside an ItemStack's NBT under the "PlasmoTape" key.
 */
public class TapeData {

    public enum SourceType { RECORDER, BUG, IMPORTED }

    public UUID tapeId = UUID.randomUUID();
    public UUID ownerId;
    public long createdTimeMillis;
    public int lengthSeconds;
    public SourceType sourceType = SourceType.RECORDER;
    public final List<byte[]> audioFrames = new ArrayList<>();
    public final List<Integer> bookmarkSeconds = new ArrayList<>();

    public static final String NBT_KEY = "PlasmoTape";

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("TapeId", tapeId);
        if (ownerId != null) tag.putUUID("OwnerId", ownerId);
        tag.putLong("CreatedTime", createdTimeMillis);
        tag.putInt("LengthSeconds", lengthSeconds);
        tag.putString("SourceType", sourceType.name());

        ListTag frameList = new ListTag();
        for (byte[] frame : audioFrames) {
            frameList.add(new ByteArrayTag(frame));
        }
        tag.put("AudioFrames", frameList);

        ListTag bookmarks = new ListTag();
        for (Integer sec : bookmarkSeconds) {
            CompoundTag b = new CompoundTag();
            b.putInt("t", sec);
            bookmarks.add(b);
        }
        tag.put("Bookmarks", bookmarks);

        return tag;
    }

    public static TapeData load(CompoundTag tag) {
        TapeData data = new TapeData();
        data.tapeId = tag.hasUUID("TapeId") ? tag.getUUID("TapeId") : UUID.randomUUID();
        if (tag.hasUUID("OwnerId")) data.ownerId = tag.getUUID("OwnerId");
        data.createdTimeMillis = tag.getLong("CreatedTime");
        data.lengthSeconds = tag.getInt("LengthSeconds");
        try {
            data.sourceType = SourceType.valueOf(tag.getString("SourceType"));
        } catch (IllegalArgumentException ignored) {
            data.sourceType = SourceType.RECORDER;
        }

        ListTag frameList = tag.getList("AudioFrames", 7); // 7 = ByteArrayTag id
        for (int i = 0; i < frameList.size(); i++) {
            data.audioFrames.add(((ByteArrayTag) frameList.get(i)).getAsByteArray());
        }

        ListTag bookmarks = tag.getList("Bookmarks", 10); // 10 = CompoundTag id
        for (int i = 0; i < bookmarks.size(); i++) {
            data.bookmarkSeconds.add(bookmarks.getCompound(i).getInt("t"));
        }

        return data;
    }

    public void writeToStack(ItemStack stack) {
        stack.getOrCreateTag().put(NBT_KEY, save());
    }

    public static TapeData readFromStack(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(NBT_KEY)) return null;
        return load(stack.getTag().getCompound(NBT_KEY));
    }
}
