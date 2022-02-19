package com.silverminer.simpleportals_reloaded.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.silverminer.simpleportals_reloaded.common.Utils;

import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;

public class CommandTeleport
{
	private enum TeleportMode
	{
		ToPlayer,
		ToPosition
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		dispatcher.register(
			Commands.literal("tpd").requires((commandSource) -> {
				return commandSource.hasPermission(2);
			})
			.executes(context -> {
				SendTranslatedMessage(context.getSource(), "commands.tpd.info");
				return 1;
			})
			.then(
				Commands.argument("dimension", DimensionArgument.dimension())
					.executes(context -> {
						return tp(context.getSource(), TeleportMode.ToPosition, DimensionArgument.getDimension(context, "dimension").dimension(), null, null, null);
					})
					.then(
						Commands.argument("position", BlockPosArgument.blockPos())
							.executes(context -> {
								return tp(context.getSource(), TeleportMode.ToPosition, DimensionArgument.getDimension(context, "dimension").dimension(), BlockPosArgument.getLoadedBlockPos(context, "position"), null, null);
							})
							.then(
								Commands.argument("player", EntityArgument.player())		// tpd <dimension> [<x> <y> <z>] [player]
									.executes(context -> {
										return tp(context.getSource(), TeleportMode.ToPosition, DimensionArgument.getDimension(context, "dimension").dimension(), BlockPosArgument.getLoadedBlockPos(context, "position"), null, EntityArgument.getPlayer(context, "player"));
									})
							)
					)
			)
			.then(
				Commands.argument("targetPlayer", EntityArgument.player())
					.executes(context -> {
						return tp(context.getSource(), TeleportMode.ToPlayer, null, null, EntityArgument.getPlayer(context, "targetPlayer"), null);
					})
					.then(
						Commands.argument("player", EntityArgument.player())		// tpd <targetPlayer> [player]
							.executes(context -> {
								return tp(context.getSource(), TeleportMode.ToPlayer, null, null, EntityArgument.getPlayer(context, "targetPlayer"), EntityArgument.getPlayer(context, "player"));
							})
					)
			)
		);
	}

	private static int tp(CommandSourceStack source, TeleportMode mode, ResourceKey<Level> dimension, BlockPos destination, ServerPlayer targetPlayer, ServerPlayer player)
	{
		if (player == null)
		{
			try
			{
				player = source.getPlayerOrException();
			}
			catch (CommandSyntaxException ex)
			{
				throw new CommandRuntimeException(new TranslatableComponent("commands.errors.unknown_sender"));
			}
		}

		switch (mode)
		{
			case ToPosition:
				if (destination == null) destination = player.blockPosition();
				break;

			case ToPlayer:
				destination = targetPlayer.blockPosition();
				dimension = targetPlayer.level.dimension();

				break;
		}

		Utils.teleportTo(player, dimension, destination, Direction.NORTH);
		SendTranslatedMessage(source, "commands.tpd.success", player.getName(), destination.getX(), destination.getY(), destination.getZ(), dimension.getRegistryName());

		return 1;
	}

	private static void SendTranslatedMessage(CommandSourceStack source, String message, Object... args)
	{
		source.sendSuccess(new TranslatableComponent(message, args), true);
	}
}
