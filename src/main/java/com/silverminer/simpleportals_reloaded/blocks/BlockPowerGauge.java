package com.silverminer.simpleportals_reloaded.blocks;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.List;

import com.silverminer.simpleportals_reloaded.SimplePortals;
import com.silverminer.simpleportals_reloaded.configuration.Config;
import com.silverminer.simpleportals_reloaded.registration.Portal;
import com.silverminer.simpleportals_reloaded.registration.PortalRegistry;

/**
 * Represents a frame of the portal multiblock that supplies comparators with a redstone
 * signal. The signal strength is based on the amount of power stored inside the portal.
 */
public class BlockPowerGauge extends BlockPortalFrame
{
	public BlockPowerGauge()
	{
		super(SimplePortals.BLOCK_POWER_GAUGE_NAME);
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state)
	{
		return true;
	}
	
	@Override
	public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos)
	{
		if (!world.isClientSide)
		{
			List<Portal> portals = PortalRegistry.getPortalsAt(pos, world.dimension());

			if (portals != null && portals.size() > 0)
			{
				int signalSum = 0;

				for (Portal portal : portals)
				{
					signalSum += getSignalStrength(portal);
				}

				return Mth.floor(signalSum / (float)portals.size());		// combined signal strength
			}
		}

		return 0;
	}
	
	/**
	 * Calculates the comparator signal strength for the specified portal.
	 * 
	 * @param portal
	 * The portal to calculate the signal strength for.
	 * @return
	 * <code>0</code> if <i>portal</i> was <code>null</code> or the power system is disabled,
	 * otherwise a value between <code>0</code> and <code>15</code>.
	 */
	private int getSignalStrength(Portal portal)
	{
		if (portal != null && Config.powerCost.get() > 0 && Config.powerCapacity.get() > 0)
		{
			int maxUses = Mth.floor(Config.powerCapacity.get() / (float)Config.powerCost.get());
			
			if (maxUses > 0)
			{
				int power = PortalRegistry.getPower(portal);
				int uses = Mth.floor(power / (float)Config.powerCost.get());
				
				int signalStrength = Mth.floor((uses / (float)maxUses) * 14.0f) + ((uses > 0) ? 1 : 0);
				
				return Math.min(signalStrength, 15);
			}
		}
		
		return 0;
	}
}
