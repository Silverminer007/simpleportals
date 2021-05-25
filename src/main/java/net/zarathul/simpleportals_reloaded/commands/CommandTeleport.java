package net.zarathul.simpleportals_reloaded.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.zarathul.simpleportals_reloaded.common.Utils;

public class CommandTeleport
{
	private enum TeleportMode
	{
		ToPlayer,
		ToPosition
	}

	public static void register(CommandDispatcher<CommandSource> dispatcher)
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

	private static int tp(CommandSource source, TeleportMode mode, RegistryKey<World> dimension, BlockPos destination, ServerPlayerEntity targetPlayer, ServerPlayerEntity player)
	{
		if (player == null)
		{
			try
			{
				player = source.getPlayerOrException();
			}
			catch (CommandSyntaxException ex)
			{
				throw new CommandException(new TranslationTextComponent("commands.errors.unknown_sender"));
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

	private static void SendTranslatedMessage(CommandSource source, String message, Object... args)
	{
		source.sendSuccess(new TranslationTextComponent(message, args), true);
	}
}
