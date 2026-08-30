package com.plasmosurveillance.item;

import com.plasmosurveillance.PlasmoSurveillance;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, PlasmoSurveillance.MODID);

    public static final RegistryObject<Item> PORTABLE_RECORDER = ITEMS.register("portable_recorder",
            () -> new PortableRecorderItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BLANK_TAPE = ITEMS.register("blank_tape",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RECORDED_TAPE = ITEMS.register("recorded_tape",
            () -> new RecordedTapeItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> TAPE_PLAYER = ITEMS.register("tape_player",
            () -> new TapePlayerItem(new Item.Properties().stacksTo(1)));

    /**
     * Convenience helper - call from CreativeModeTab registration if you add
     * a dedicated tab, or fold these into an existing vanilla tab.
     */
    public static void addToTab(CreativeModeTab.Output output) {
        output.accept(PORTABLE_RECORDER.get());
        output.accept(BLANK_TAPE.get());
        output.accept(RECORDED_TAPE.get());
        output.accept(TAPE_PLAYER.get());
    }
}
