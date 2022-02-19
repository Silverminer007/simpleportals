package com.silverminer.simpleportals_reloaded;

import com.silverminer.simpleportals_reloaded.blocks.BlockPortal;
import com.silverminer.simpleportals_reloaded.blocks.BlockPortalFrame;
import com.silverminer.simpleportals_reloaded.blocks.BlockPowerGauge;
import com.silverminer.simpleportals_reloaded.commands.arguments.BlockArgument;
import com.silverminer.simpleportals_reloaded.common.PortalWorldSaveData;
import com.silverminer.simpleportals_reloaded.common.TeleportTask;
import com.silverminer.simpleportals_reloaded.configuration.Config;
import com.silverminer.simpleportals_reloaded.configuration.gui.ConfigGuiFactory;
import com.silverminer.simpleportals_reloaded.items.ItemPortalActivator;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

@Mod(SimplePortals.MOD_ID)
public class SimplePortals {
	// block and item names
	public static final String BLOCK_PORTAL_NAME = "portal";
	public static final String BLOCK_PORTAL_FRAME_NAME = "portal_frame";
	public static final String BLOCK_POWER_GAUGE_NAME = "power_gauge";
	public static final String ITEM_PORTAL_FRAME_NAME = "portal_frame";
	public static final String ITEM_POWER_GAUGE_NAME = "power_gauge";
	public static final String ITEM_PORTAL_ACTIVATOR_NAME = "portal_activator";

	// blocks
	public static BlockPortal blockPortal;
	public static BlockPortalFrame blockPortalFrame;
	public static BlockPowerGauge blockPowerGauge;

	// items
	public static ItemPortalActivator itemPortalActivator;
	public static BlockItem itemPortalFrame;
	public static BlockItem itemPowerGauge;

	// creative tab
	public static CreativeModeTab creativeTab = MakeCreativeTab();
	// world save data handler
	public static PortalWorldSaveData portalSaveData;

	// constants
	public static final String MOD_ID = "simpleportals_reloaded";
	public static final String SIMPLE_MODS_ID = "simplemods";

	// logger
	public static final Logger log = LogManager.getLogger(MOD_ID);

	public static LinkedBlockingQueue<TeleportTask> TELEPORT_QUEUE = new LinkedBlockingQueue<>();

	public SimplePortals() {
		// Register custom argument types for command parser
		ArgumentTypes.register("sportals_block", BlockArgument.class, new EmptyArgumentSerializer<>(BlockArgument::block));

		// Setup configs
		ModLoadingContext Mlc = ModLoadingContext.get();
		Mlc.registerConfig(ModConfig.Type.COMMON, Config.CommonConfigSpec, MOD_ID + "-common.toml");
		Mlc.registerConfig(ModConfig.Type.CLIENT, Config.ClientConfigSpec, MOD_ID + "-client.toml");
		Config.load(Config.CommonConfigSpec, FMLPaths.CONFIGDIR.get().resolve(MOD_ID + "-common.toml"));
		Config.load(Config.ClientConfigSpec, FMLPaths.CONFIGDIR.get().resolve(MOD_ID + "-client.toml"));

		// Setup config UI
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			ConfigGuiFactory.setConfigHolder("com.silverminer.simpleportals_reloaded.configuration.Config");
			ModLoadingContext.get().registerExtensionPoint(
					ConfigGuiHandler.ConfigGuiFactory.class,
					() -> new ConfigGuiHandler.ConfigGuiFactory(ConfigGuiFactory::getConfigGui));
		});

		// Setup event listeners
		IEventBus SetupEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		SetupEventBus.register(EventHub.ModEventBus.class);
		MinecraftForge.EVENT_BUS.register(EventHub.ForgeEventBus.class);
	}

	static CreativeModeTab MakeCreativeTab() {
		// Checks if a "Simple Mods" tab already exists, otherwise makes one.
		return Arrays.stream(CreativeModeTab.TABS)
				.filter(tab -> tab.getRecipeFolderName().equals(SimplePortals.SIMPLE_MODS_ID)).findFirst()
				.orElseGet(() -> new CreativeModeTab(SimplePortals.SIMPLE_MODS_ID) {
					@OnlyIn(Dist.CLIENT)
					private ItemStack iconStack;

					@Override
					@OnlyIn(Dist.CLIENT)
					public @NotNull ItemStack makeIcon() {
						if (iconStack == null)
							iconStack = new ItemStack(itemPortalFrame);

						return iconStack;
					}
				});
	}
}