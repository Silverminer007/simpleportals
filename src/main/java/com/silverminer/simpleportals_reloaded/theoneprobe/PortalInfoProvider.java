package com.silverminer.simpleportals_reloaded.theoneprobe;

import mcjty.theoneprobe.api.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.Tag;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.Collection;
import java.util.List;

import com.silverminer.simpleportals_reloaded.SimplePortals;
import com.silverminer.simpleportals_reloaded.configuration.Config;
import com.silverminer.simpleportals_reloaded.registration.Portal;
import com.silverminer.simpleportals_reloaded.registration.PortalRegistry;

/**
 * Provides TheOneProbe tooltip information for portals.
 */
public class PortalInfoProvider implements IProbeInfoProvider
{
	// I18N keys
	private static final String PORTAL_INFO = IProbeInfo.STARTLOC + "interop.top.";
	private static final String POWER_CAPACITY = PORTAL_INFO + "power_capacity" + IProbeInfo.ENDLOC;
	private static final String POWER_SOURCES = PORTAL_INFO + "power_sources" + IProbeInfo.ENDLOC;
	private static final String INVALID_POWER_SOURCE = PORTAL_INFO + "invalid_power_source" + IProbeInfo.ENDLOC;
	private static final String ADDRESS = PORTAL_INFO + "address" + IProbeInfo.ENDLOC;
	private static final String REDSTONE_POWER = PORTAL_INFO + "redstone_power" + IProbeInfo.ENDLOC;

	private static final IForgeRegistry<Block> BLOCK_REGISTRY = ForgeRegistries.BLOCKS;

	@Override
	public String getID()
	{
		return SimplePortals.MOD_ID + ":PortalInfoProvider";
	}

	@Override
	public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, Player player, Level world, BlockState blockState, IProbeHitData data)
	{
		// Note: Text translations will only work in singleplayer. On a dedicated server everything will be english only unfortunately.

		List<Portal> portals = PortalRegistry.getPortalsAt(data.getPos(), world.dimension());
		if (portals == null) return;

		for (Portal portal : portals)
		{
			// Add power capacity info

			if (Config.powerCost.get() > 0)
			{
				int power = PortalRegistry.getPower(portal);
				int percentage = (Config.powerCapacity.get() > 0)
								 ? Mth.clamp((int) ((long) power * 100 / Config.powerCapacity.get()), 0, 100)
								 : 100;

				probeInfo.text(POWER_CAPACITY + String.format(" %,d/%,d (%d%%)", power, Config.powerCapacity.get(), percentage));
				probeInfo.progress(power, Config.powerCapacity.get(), probeInfo.defaultProgressStyle().showText(false));

				if (mode == ProbeMode.EXTENDED)
				{
					// Add a list of items that are considered valid power sources for the portal (4 max)

					probeInfo.text(POWER_SOURCES);
					IProbeInfo powerSourceInfo =  probeInfo.horizontal();
					Tag<Item> powerTag = ItemTags.getAllTags().getTag(Config.powerSource);

					if (powerTag != null)
					{
						Collection<Item> itemsWithPowerTag = powerTag.getValues();
						int powerItemCount = 0;

						for (Item powerSource : itemsWithPowerTag)
						{
							powerSourceInfo.item(new ItemStack(powerSource));
							powerItemCount++;

							if (powerItemCount == 4) break;
						}
					}
					else
					{
						powerSourceInfo.text(INVALID_POWER_SOURCE + String.format(" (%s)", Config.powerSource));
					}
				}
			}

			if (mode == ProbeMode.EXTENDED)
			{
				// Add the address as block icons

				probeInfo.text(ADDRESS);
				IProbeInfo addressInfo = probeInfo.horizontal();

				String address = portal.getAddress().toString();
				String[] addressComponents = address.split(",");

				for (String component : addressComponents)
				{
					// Extract the block name and count from the address
					String trimmedComponent = component.trim();
					int blockCount = Integer.parseInt(trimmedComponent.substring(0, 1));
					String blockName = trimmedComponent.substring(2);

					// Get an ItemStack corresponding to the extracted data
					ItemStack addressItem = ItemStack.EMPTY;
					Block addressBlock = BLOCK_REGISTRY.getValue(new ResourceLocation(blockName));

					if (addressBlock != null)
					{
						@SuppressWarnings("deprecation")
						Item blockItem = Item.byBlock(addressBlock);

						if (blockItem != Items.AIR)
						{
							// Note: There used to be code here to deal with sub-items based on meta data.
							// Since wool, logs and all the other blocks are all separate blocks now we don't
							// care about sub-items anymore I guess ?.
							addressItem = new ItemStack(blockItem, 1);
						}
					}

					if (!addressItem.isEmpty())
					{
						// Add the icon for the ItemStack as many times as it occurs in the address
						for (int x = 0; x < blockCount; x++ ) addressInfo.item(addressItem);
					}
					else
					{
						// If no ItemStack could be found for the current address component, show the raw text instead
						addressInfo.text(component);
						addressInfo = probeInfo.horizontal();
					}
				}

				// Add redstone power level if the inspected block is a power gauge

				Block block = blockState.getBlock();

				if (block == SimplePortals.blockPowerGauge)
				{
					int gaugeLevel = SimplePortals.blockPowerGauge.getAnalogOutputSignal(blockState, world, data.getPos());

					probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
							.item(new ItemStack(Items.REDSTONE))
							.text(REDSTONE_POWER + " " + gaugeLevel);

				}
			}
			else if (mode == ProbeMode.DEBUG)
			{
				// Add the address in plain text in debug mode

				probeInfo.text(ADDRESS);

				String address = portal.getAddress().toString();
				String[] addressComponents = address.split(",");

				for (String component : addressComponents) probeInfo.vertical().text(component.trim());
			}
		}
	}
}
