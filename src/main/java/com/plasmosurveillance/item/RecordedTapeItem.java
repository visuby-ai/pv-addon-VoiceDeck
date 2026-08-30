package com.plasmosurveillance.item;

import com.plasmosurveillance.tape.TapeData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class RecordedTapeItem extends Item {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public RecordedTapeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        TapeData data = TapeData.readFromStack(stack);
        if (data == null) {
            tooltip.add(Component.literal("§7(empty tape data)"));
            return;
        }

        int minutes = data.lengthSeconds / 60;
        int seconds = data.lengthSeconds % 60;
        tooltip.add(Component.literal("§7Length: §f" + String.format("%d:%02d", minutes, seconds)));
        tooltip.add(Component.literal("§7Recorded: §f" + DATE_FORMAT.format(new Date(data.createdTimeMillis))));
        tooltip.add(Component.literal("§7Source: §f" + data.sourceType.name()));
        tooltip.add(Component.literal("§8ID: " + data.tapeId.toString().substring(0, 8)));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return TapeData.readFromStack(stack) != null;
    }
}
