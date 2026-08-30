package com.plasmosurveillance.item;

import com.plasmosurveillance.PlasmoSurveillance;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PlasmoSurveillance.MODID);

    public static final RegistryObject<CreativeModeTab> SURVEILLANCE_TAB = CREATIVE_MODE_TABS.register(
            "surveillance_tab",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> new ItemStack(ModItems.PORTABLE_RECORDER.get()))
                    .title(Component.translatable("itemGroup.plasmosurveillance"))
                    .displayItems((parameters, output) -> ModItems.addToTab(output))
                    .build()
    );
}
