package com.silverminer.simpleportals_reloaded.commands;

import com.google.common.collect.ImmutableListMultimap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.silverminer.simpleportals_reloaded.SimplePortals;
import com.silverminer.simpleportals_reloaded.commands.arguments.BlockArgument;
import com.silverminer.simpleportals_reloaded.configuration.Config;
import com.silverminer.simpleportals_reloaded.registration.Address;
import com.silverminer.simpleportals_reloaded.registration.Portal;
import com.silverminer.simpleportals_reloaded.registration.PortalRegistry;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CommandPortals {
	private enum ListMode {
		All, Address, Dimension
	}

	private enum DeactiveMode {
		Address, Position
	}

	private enum PowerMode {
		Add, Remove, Get, Items
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("sportals").requires((commandSource) -> {
			return commandSource.hasPermission(4);
		}).executes(context -> {
			SendTranslatedMessage(context.getSource(), "commands.sportals.info");
			return 1;
		}).then(Commands.literal("list").executes(context -> {
			SendTranslatedMessage(context.getSource(), "commands.sportals.list.info");
			return 1;
		}).then(Commands.literal("all") // sportals list all
				.executes(context -> {
					return list(context.getSource(), ListMode.All, null, null);
				}))
				.then(Commands.argument("address1", BlockArgument.block())
						.then(Commands.argument("address2", BlockArgument.block())
								.then(Commands.argument("address3", BlockArgument.block())
										.then(Commands.argument("address4", BlockArgument.block()) // sportals list
																									// <addressBlockId>
																									// <addressBlockId>
																									// <addressBlockId>
																									// <addressBlockId>
												.executes(context -> {
													Address address = new Address(
															PortalRegistry.getAddressBlockId(
																	BlockArgument.getBlock(context, "address1")),
															PortalRegistry.getAddressBlockId(
																	BlockArgument.getBlock(context, "address2")),
															PortalRegistry.getAddressBlockId(
																	BlockArgument.getBlock(context, "address3")),
															PortalRegistry.getAddressBlockId(
																	BlockArgument.getBlock(context, "address4")));

													return list(context.getSource(), ListMode.Address, address, null);
												})))))
				.then(Commands.argument("dimension", DimensionArgument.dimension()) // sportals list <dimension>
						.executes(context -> {
							return list(context.getSource(), ListMode.Dimension, null,
									DimensionArgument.getDimension(context, "dimension").dimension());
						})))
				.then(Commands.literal("deactivate").executes(context -> {
					SendTranslatedMessage(context.getSource(), "commands.sportals.deactivate.info");
					return 1;
				}).then(Commands.argument("address1", BlockArgument.block())
						.then(Commands.argument("address2", BlockArgument.block())
								.then(Commands.argument("address3", BlockArgument.block())
										.then(Commands.argument("address4", BlockArgument.block()).executes(context -> {
											Address address = new Address(
													PortalRegistry.getAddressBlockId(
															BlockArgument.getBlock(context, "address1")),
													PortalRegistry.getAddressBlockId(
															BlockArgument.getBlock(context, "address2")),
													PortalRegistry.getAddressBlockId(
															BlockArgument.getBlock(context, "address3")),
													PortalRegistry.getAddressBlockId(
															BlockArgument.getBlock(context, "address4")));

											return deactivate(context.getSource(), DeactiveMode.Address, address, null,
													null);
										}).then(Commands.argument("dimension", DimensionArgument.dimension()) // sportals
																												// deactivate
																												// <addressBlockId>
																												// <addressBlockId>
																												// <addressBlockId>
																												// <addressBlockId>
																												// [dimension]
												.executes(context -> {
													Address address = new Address(
															PortalRegistry.getAddressBlockId(
																	BlockArgument.getBlock(context, "address1")),
															PortalRegistry.getAddressBlockId(
																	BlockArgument.getBlock(context, "address2")),
															PortalRegistry.getAddressBlockId(
																	BlockArgument.getBlock(context, "address3")),
															PortalRegistry.getAddressBlockId(
																	BlockArgument.getBlock(context, "address4")));

													ResourceKey<Level> dimension = DimensionArgument
															.getDimension(context, "dimension").dimension();

													return deactivate(context.getSource(), DeactiveMode.Address,
															address, null, dimension);
												}))))))
						.then(Commands.argument("position", BlockPosArgument.blockPos()).executes(context -> {
							return deactivate(context.getSource(), DeactiveMode.Position, null,
									BlockPosArgument.getLoadedBlockPos(context, "position"), null);
						}).then(Commands.argument("dimension", DimensionArgument.dimension()) // sportals deactivate <x>
																								// <y> <z> [dimension]
								.executes(context -> {
									return deactivate(context.getSource(), DeactiveMode.Position, null,
											BlockPosArgument.getLoadedBlockPos(context, "position"),
											DimensionArgument.getDimension(context, "dimension").dimension());
								}))))
				.then(Commands.literal("power").executes(context -> {
					SendTranslatedMessage(context.getSource(), "commands.sportals.power.info");
					return 1;
				}).then(Commands.literal("add").then(Commands.argument("amount", IntegerArgumentType.integer(1))
						.then(Commands.argument("position", BlockPosArgument.blockPos()).executes(context -> {
							return power(context.getSource(), PowerMode.Add,
									IntegerArgumentType.getInteger(context, "amount"),
									BlockPosArgument.getLoadedBlockPos(context, "position"), null);
						}).then(Commands.argument("dimension", DimensionArgument.dimension()) // sportals power add
																								// <amount> <x> <y> <z>
																								// [dimension]
								.executes(context -> {
									return power(context.getSource(), PowerMode.Add,
											IntegerArgumentType.getInteger(context, "amount"),
											BlockPosArgument.getLoadedBlockPos(context, "position"),
											DimensionArgument.getDimension(context, "dimension").dimension());
								})))))
						.then(Commands.literal("remove")
								.then(Commands.argument("amount", IntegerArgumentType.integer(1)).then(
										Commands.argument("position", BlockPosArgument.blockPos()).executes(context -> {
											return power(context.getSource(), PowerMode.Remove,
													IntegerArgumentType.getInteger(context, "amount"),
													BlockPosArgument.getLoadedBlockPos(context, "position"), null);
										}).then(Commands.argument("dimension", DimensionArgument.dimension()) // sportals
																												// power
																												// remove
																												// <amount>
																												// <x>
																												// <y>
																												// <z>
																												// [dimension]
												.executes(context -> {
													return power(context.getSource(), PowerMode.Remove,
															IntegerArgumentType.getInteger(context, "amount"),
															BlockPosArgument.getLoadedBlockPos(context, "position"),
															DimensionArgument.getDimension(context, "dimension")
																	.dimension());
												})))))
						.then(Commands.literal("get")
								.then(Commands.argument("position", BlockPosArgument.blockPos()).executes(context -> {
									return power(context.getSource(), PowerMode.Get, 0,
											BlockPosArgument.getLoadedBlockPos(context, "position"), null);
								}).then(Commands.argument("dimension", DimensionArgument.dimension()) // sportals power
																										// get <x> <y>
																										// <z>
																										// [dimension]
										.executes(context -> {
											return power(context.getSource(), PowerMode.Get, 0,
													BlockPosArgument.getLoadedBlockPos(context, "position"),
													DimensionArgument.getDimension(context, "dimension").dimension());
										}))))
						.then(Commands.literal("items").executes(context -> {
							ITagManager<Item> tagManager = ForgeRegistries.ITEMS.tags();
							ITag<Item> powerTag = tagManager == null ? null : tagManager.getTag(Config.getPowerSourceTag());

							if (powerTag == null || powerTag.isEmpty()) {
								SendTranslatedMessage(context.getSource(), "commands.errors.no_power_items",
										Config.powerSource);
								return 1;
							}

							if (powerTag.size() == 0) {
								SendTranslatedMessage(context.getSource(), "commands.errors.no_power_items",
										Config.powerSource);
								return 1;
							}

							SendTranslatedMessage(context.getSource(), "commands.sportals.power.items.success",
									powerTag.size());

							for (Item powerSource : powerTag) {
								SendTranslatedMessage(context.getSource(), powerSource.getDescriptionId());
							}

							return 1;
						})))
				.then(Commands.literal("clear").executes(context -> {
					SendTranslatedMessage(context.getSource(), "commands.sportals.clear.info");
					return 1;
				}).then(Commands.literal("confirmed") // sportals clear confirmed
						.executes(context -> clear(context.getSource())))));
	}

	private static int list(CommandSourceStack source, ListMode mode, Address address, ResourceKey<Level> dimension) {
		List<Portal> portals;

		switch (mode) {
			case All -> {
				// sportals list all
				ImmutableListMultimap<Address, Portal> addresses = PortalRegistry.getAddresses();
				Set<Address> uniqueAddresses = new HashSet<>(addresses.keys());
				List<Portal> portalsForAddress;
				portals = new ArrayList<>();
				for (Address addr : uniqueAddresses) {
					portalsForAddress = addresses.get(addr);
					portals.addAll(portalsForAddress);
				}
			}
			case Address ->
					// sportals list <addressBlockId> <addressBlockId> <addressBlockId>
					// <addressBlockId>
					portals = PortalRegistry.getPortalsWithAddress(address);
			case Dimension ->
					// sportals list <dimension>
					portals = PortalRegistry.getPortalsInDimension(dimension);
			default -> portals = new ArrayList<>();
		}

		SimplePortals.log.info("Registered portals");
		SimplePortals.log.info(
				"|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|");
		SimplePortals.log.info(
				"| Dimension                                | Position                    | Power | Address                                                                                                                                                |");
		SimplePortals.log.info(
				"|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|");

		BlockPos portalBlockPos;
		String formattedPortalBlockPos;

		for (Portal portal : portals) {
			portalBlockPos = portal.getCorner1().getPos();
			formattedPortalBlockPos = String.format("x=%d, y=%d, z=%d", portalBlockPos.getX(), portalBlockPos.getY(),
					portalBlockPos.getZ());

			SimplePortals.log
					.info(String.format("| %40s | %27s | %5d | %-150s |", portal.getDimension().getRegistryName(),
							formattedPortalBlockPos, PortalRegistry.getPower(portal), portal.getAddress()));
		}

		SimplePortals.log.info(
				"|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|");

		SendTranslatedMessage(source, "commands.sportals.list.success");

		return 1;
	}

	private static int deactivate(CommandSourceStack source, DeactiveMode mode, Address address, BlockPos pos,
			ResourceKey<Level> dimension) {
		List<Portal> portals = null;

		switch (mode) {
			case Address -> {
				// sportals deactivate <addressBlockId> <addressBlockId> <addressBlockId>
				// <addressBlockId> [dimension]
				portals = PortalRegistry.getPortalsWithAddress(address);
				if (portals == null || portals.size() == 0) {
					if (dimension != null) {
						SendTranslatedMessage(source, "commands.errors.portal_not_found_with_address_in_dimension", address,
								dimension.getRegistryName());
					} else {
						SendTranslatedMessage(source, "commands.errors.portal_not_found_with_address", address);
					}

					return 0;
				}
				if (dimension != null) {
					// filter out all portals that are not in the specified dimension
					final ResourceKey<Level> dimensionCopy = dimension; // This is necessary because Java wants closures in
					// lambda expressions to be effectively final.
					portals = portals.stream().filter((portal -> portal.getDimension() == dimensionCopy))
							.collect(Collectors.toList());
				}
			}
			case Position -> {
				// sportals deactivate <x> <y> <z> [dimension]
				if (dimension == null) {
					try {
						// Get the dimension the command sender is currently in.
						ServerPlayer player = source.getPlayerOrException();
						dimension = player.level.dimension();
					} catch (CommandSyntaxException ex) {
						throw new CommandRuntimeException(
								new TranslatableComponent("commands.errors.unknown_sender_dimension"));
					}
				}
				portals = PortalRegistry.getPortalsAt(pos, dimension);
				if (portals == null || portals.size() == 0)
					throw new CommandRuntimeException(
							new TranslatableComponent("commands.errors.portal_not_found_at_pos_in_dimension", pos.getX(),
									pos.getY(), pos.getZ(), dimension.getRegistryName()));
			}
		}

		BlockPos portalPos;

		for (Portal portal : portals) {
			portalPos = portal.getCorner1().getPos();
			ResourceKey<Level> dimType = portal.getDimension();
			if (dimType == null)
				throw new CommandRuntimeException(
						new TranslatableComponent("commands.errors.missing_dimension", portal.getDimension()));

			PortalRegistry.deactivatePortal(source.getServer().getLevel(dimType), portalPos);
			SendTranslatedMessage(source, "commands.sportals.deactivate.success", portalPos.getX(), portalPos.getY(),
					portalPos.getZ(), dimType.getRegistryName());
		}

		return 1;
	}

	private static int power(CommandSourceStack source, PowerMode mode, int amount, BlockPos pos,
			ResourceKey<Level> dimension) {
		if (dimension == null) {
			// Get the dimension the command sender is currently in.
			try {
				ServerPlayer player = source.getPlayerOrException();
				dimension = player.level.dimension();
			} catch (CommandSyntaxException ex) {
				throw new CommandRuntimeException(new TranslatableComponent("commands.errors.unknown_sender_dimension"));
			}
		}

		List<Portal> portals = PortalRegistry.getPortalsAt(pos, dimension);

		if (portals == null || portals.size() == 0) {
			throw new CommandRuntimeException(
					new TranslatableComponent("commands.errors.portal_not_found_at_pos_in_dimension", pos.getX(),
							pos.getY(), pos.getZ(), dimension.getRegistryName()));
		} else if (portals.size() > 1) {
			throw new CommandRuntimeException(
					new TranslatableComponent("commands.errors.multiple_portals_found_at_pos_in_dimension",
							pos.getX(), pos.getY(), pos.getZ(), dimension.getRegistryName()));
		}

		Portal portal = portals.get(0);

		switch (mode) {
		case Add:
			// sportals power add <amount> <x> <y> <z> [dimension]
			amount = amount - PortalRegistry.addPower(portal, amount);
			PortalRegistry.updatePowerGauges(source.getLevel(), portal);
			SendTranslatedMessage(source, "commands.sportals.power.add.success", amount, pos.getX(), pos.getY(),
					pos.getZ(), dimension.getRegistryName());
			break;

		case Remove:
			// sportals power remove <amount> <x> <y> <z> [dimension]
			amount = Math.min(amount, PortalRegistry.getPower(portal));
			amount = (PortalRegistry.removePower(portal, amount)) ? amount : 0;
			PortalRegistry.updatePowerGauges(source.getLevel(), portal);
			SendTranslatedMessage(source, "commands.sportals.power.remove.success", amount, pos.getX(), pos.getY(),
					pos.getZ(), dimension.getRegistryName());
			break;

		case Get:
			// sportals power get <x> <y> <z> [dimension]
			amount = PortalRegistry.getPower(portal);
			SendTranslatedMessage(source, "commands.sportals.power.get.success", pos.getX(), pos.getY(), pos.getZ(),
					dimension.getRegistryName(), amount);
			break;
		case Items:
			break;
		default:
			break;
		}

		return 1;
	}

	private static int clear(CommandSourceStack source) {
		// sportals clear confirmed
		PortalRegistry.clear();
		SendTranslatedMessage(source, "commands.sportals.clear.success");

		return 1;
	}

	private static void SendTranslatedMessage(CommandSourceStack source, String message, Object... args) {
		source.sendSuccess(new TranslatableComponent(message, args), true);
	}
}
