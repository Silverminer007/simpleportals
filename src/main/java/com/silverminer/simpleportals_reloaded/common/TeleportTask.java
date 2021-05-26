package com.silverminer.simpleportals_reloaded.common;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TeleportTask
{
	public int creationTickCount;
	public ServerPlayerEntity player;
	public RegistryKey<World> dimension;
	public BlockPos pos;
	public Direction facing;

	public TeleportTask(int creationTickCount, ServerPlayerEntity player, RegistryKey<World> dimension, BlockPos pos, Direction facing)
	{
		this.creationTickCount = creationTickCount;
		this.player = player;
		this.dimension = dimension;
		this.pos = pos;
		this.facing = facing;
	}
}
