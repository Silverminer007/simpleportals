package com.silverminer.simpleportals_reloaded;

import com.silverminer.simpleportals_reloaded.blocks.BlockPortal;
import com.silverminer.simpleportals_reloaded.blocks.BlockPortalFrame;
import com.silverminer.simpleportals_reloaded.blocks.BlockPowerGauge;
import com.silverminer.simpleportals_reloaded.commands.CommandPortals;
import com.silverminer.simpleportals_reloaded.commands.CommandTeleport;
import com.silverminer.simpleportals_reloaded.common.PortalWorldSaveData;
import com.silverminer.simpleportals_reloaded.common.TeleportTask;
import com.silverminer.simpleportals_reloaded.common.Utils;
import com.silverminer.simpleportals_reloaded.configuration.Config;
import com.silverminer.simpleportals_reloaded.items.ItemPortalActivator;
import com.silverminer.simpleportals_reloaded.items.ItemPortalFrame;
import com.silverminer.simpleportals_reloaded.items.ItemPowerGauge;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent.Load;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Hosts Forge event handlers on both the server and client side.
 */
public final class EventHub {

	public static class ModEventBus {

		@SubscribeEvent
		public static void onConfigLoaded(ModConfigEvent.Loading event) {
			if (event.getConfig().getType() == ModConfig.Type.COMMON) {
				Config.updatePowerSource();
			}
		}

		@SubscribeEvent
		public static void onConfigChanged(ModConfigEvent.Reloading event) {
			if (event.getConfig().getType() == ModConfig.Type.COMMON) {
				Config.updatePowerSource();
			}
		}

		@SubscribeEvent
		public static void OnBlockRegistration(RegistryEvent.Register<Block> event) {
			SimplePortals.blockPortal = new BlockPortal();
			SimplePortals.blockPortalFrame = new BlockPortalFrame();
			SimplePortals.blockPowerGauge = new BlockPowerGauge();

			event.getRegistry().registerAll(SimplePortals.blockPortal, SimplePortals.blockPortalFrame,
					SimplePortals.blockPowerGauge);

			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ItemBlockRenderTypes.setRenderLayer(SimplePortals.blockPortal, RenderType.translucent()));
		}

		@SubscribeEvent
		public static void OnItemRegistration(RegistryEvent.Register<Item> event) {
			SimplePortals.itemPortalFrame = new ItemPortalFrame(SimplePortals.blockPortalFrame);
			SimplePortals.itemPowerGauge = new ItemPowerGauge(SimplePortals.blockPowerGauge);
			SimplePortals.itemPortalActivator = new ItemPortalActivator();

			event.getRegistry().registerAll(SimplePortals.itemPortalFrame, SimplePortals.itemPowerGauge,
					SimplePortals.itemPortalActivator);
		}
	}

	public static class ForgeEventBus {
		@SubscribeEvent
		public static void onCommandsRegister(RegisterCommandsEvent event) {
			CommandPortals.register(event.getDispatcher());
			CommandTeleport.register(event.getDispatcher());
		}

		@SubscribeEvent
		public static void onServerTick(TickEvent.ServerTickEvent event) {
			if (event.phase == TickEvent.Phase.END) {
				TeleportTask task;
				MinecraftServer mcServer;

				while (true) {
					task = SimplePortals.TELEPORT_QUEUE.peek();
					if (task == null)
						return;

					mcServer = task.player.getServer();
					if (mcServer == null) {
						// No point in keeping the task if there's no server. Should never happen but
						// who knows.
						SimplePortals.TELEPORT_QUEUE.poll();
					} else if (mcServer
							.getTickCount() > (task.creationTickCount)) {
						// Task is due.
						SimplePortals.TELEPORT_QUEUE.poll();
						Utils.teleportTo(task.player, task.dimension, task.pos, task.facing);
					} else {
						// Task was not due yet, so if there are others they won't be either.
						return;
					}
				}
			}
		}

		@SubscribeEvent
		public static void OnWorldLoad(Load event) {
			LevelAccessor iworld = event.getWorld();
			if (!(iworld instanceof Level world)) {
				return;
			}

			// WorldSavedData can no longer be stored per map but only per dimension. So
			// store the registry in the overworld.
			if (!world.isClientSide() && world.dimension() == Level.OVERWORLD && world instanceof ServerLevel) {
				SimplePortals.portalSaveData = PortalWorldSaveData.get((ServerLevel) world);
			}
		}
	}
}
