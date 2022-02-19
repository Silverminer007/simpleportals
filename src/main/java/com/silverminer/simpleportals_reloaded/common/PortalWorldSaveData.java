package com.silverminer.simpleportals_reloaded.common;

import com.silverminer.simpleportals_reloaded.SimplePortals;
import com.silverminer.simpleportals_reloaded.registration.PortalRegistry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

/**
 * Responsible for saving/loading {@link PortalRegistry} data.
 */
public class PortalWorldSaveData extends SavedData
{
	private static final String DATA_NAME = SimplePortals.MOD_ID;

	public static PortalWorldSaveData load(CompoundTag nbt)
	{
		PortalRegistry.readFromNBT(nbt);
		return new PortalWorldSaveData();
	}

	@Override
	public @NotNull CompoundTag save(@NotNull CompoundTag nbt)
	{
		PortalRegistry.writeToNBT(nbt);
		return nbt;
	}

	public static PortalWorldSaveData get(ServerLevel world)
	{
		if (world == null) return null;
		
		DimensionDataStorage storage = world.getDataStorage();

		return storage.computeIfAbsent(PortalWorldSaveData::load, PortalWorldSaveData::new, DATA_NAME);
	}
}
