package net.zarathul.simpleportals_reloaded.common;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;
import net.zarathul.simpleportals_reloaded.SimplePortals;
import net.zarathul.simpleportals_reloaded.registration.PortalRegistry;

/**
 * Responsible for saving/loading {@link PortalRegistry} data.
 */
public class PortalWorldSaveData extends WorldSavedData
{
	private static final String DATA_NAME = SimplePortals.MOD_ID;
	
	public PortalWorldSaveData()
	{
		super(DATA_NAME);
	}
	
	@Override
	public void load(CompoundNBT nbt)
	{
		PortalRegistry.readFromNBT(nbt);
	}

	@Override
	public CompoundNBT save(CompoundNBT nbt)
	{
		PortalRegistry.writeToNBT(nbt);
		return nbt;
	}

	public static PortalWorldSaveData get(ServerWorld world)
	{
		if (world == null) return null;
		
		DimensionSavedDataManager storage = world.getDataStorage();

		return storage.computeIfAbsent(PortalWorldSaveData::new, DATA_NAME);
	}
}
