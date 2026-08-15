package dev.xkmc.youkaishomecoming.events;

import dev.xkmc.youkaishomecoming.init.GensokyoLegacy;
import dev.xkmc.youkaishomecoming.init.data.GLModConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.util.ArrayList;

@EventBusSubscriber(modid = GensokyoLegacy.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CompositeBlockTabHandler {

	private static final ResourceLocation COMPOSITE_TAB_ID =
			ResourceLocation.fromNamespaceAndPath(GensokyoLegacy.MODID, "composite_blocks");

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onBuildTab(BuildCreativeModeTabContentsEvent event) {
		if (!GLModConfig.CLIENT.disableCompositeBlocks.get()) return;
		if (!COMPOSITE_TAB_ID.equals(event.getTabKey().location())) return;
		for (ItemStack stack : new ArrayList<>(event.getParentEntries())) {
			event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		}
		for (ItemStack stack : new ArrayList<>(event.getSearchEntries())) {
			event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		}
	}

}
