package com.silverminer.simpleportals_reloaded.common;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.StringUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.ITeleporter;

import java.util.ArrayList;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.silverminer.simpleportals_reloaded.configuration.Config;

/**
 * General utility class.
 */
public final class Utils {
	protected static final Logger LOGGER = LogManager.getLogger(Utils.class);
	private static final LanguageMap I18N = LanguageMap.getInstance();

	/**
	 * Gets the localized formatted string for the specified key.
	 *
	 * @param key        The key for the localized string.
	 * @param parameters Formatting arguments.
	 * @return The localized formatted string.
	 */
	public static String translate(String key, Object... parameters) {
		return String.format(I18N.getOrDefault(key), parameters);
	}

	/**
	 * Gets the localized formatted strings for the specified key and formatting
	 * arguments.
	 *
	 * @param key  The base key without an index (e.g. "myKey" gets "myKey0",
	 *             "myKey1" ... etc.).
	 * @param args Formatting arguments.
	 * @return A list of localized strings for the specified key, or an empty list
	 *         if the key was not found.
	 */
	public static ArrayList<ITextComponent> multiLineTranslate(String key, Object... args) {
		ArrayList<ITextComponent> lines = new ArrayList<>();

		if (key != null) {
			int x = 0;
			String currentKey = key + x;

			ITextComponent text;
			while ((text = new TranslationTextComponent(currentKey, args)).getString() != "") {
				lines.add(text);
				currentKey = key + ++x;
			}
		}

		return lines;
	}

	/**
	 * Gets the coordinate component of a BlockPos for the specified axis.
	 *
	 * @param pos  The coordinate to choose the component from.
	 * @param axis The axis representing the coordinate component to choose.
	 * @return <code>0</code> if either pos or axis are <code>null</code>, otherwise
	 *         the chosen coordinate component.
	 */
	public static int getAxisValue(BlockPos pos, Axis axis) {
		if (pos == null || axis == null)
			return 0;

		if (axis == Axis.X)
			return pos.getX();
		if (axis == Axis.Y)
			return pos.getY();
		if (axis == Axis.Z)
			return pos.getZ();

		return 0;
	}

	/**
	 * Gets the relative direction from one {@link BlockPos} to another.
	 *
	 * @param from The starting point.
	 * @param to   The end point.
	 * @return One of the {@link Direction} values or <code>null</code> if one of
	 *         the arguments was <code>null</code>.
	 */
	public static Direction getRelativeDirection(BlockPos from, BlockPos to) {
		if (from == null || to == null)
			return null;

		BlockPos directionVec = to.subtract(from);
		directionVec = new BlockPos(directionVec.getX() / Math.max(1, Math.abs(directionVec.getX())),
				directionVec.getY() / Math.max(1, Math.abs(directionVec.getY())),
				directionVec.getZ() / Math.max(1, Math.abs(directionVec.getZ())));

		Direction d = Direction.fromNormal(directionVec.getX(), directionVec.getY(), directionVec.getZ());
		return d;
	}

	/**
	 * Gets the axis that is orthogonal to, and on the same plane as the specified
	 * one.
	 *
	 * @param axis The starting axis.
	 * @return One of the {@link Axis} values or <code>null</code> if the specified
	 *         axis was <code>null</code> or there is no other axis on the same
	 *         plane.
	 */
	public static Axis getOrthogonalTo(Axis axis) {
		if (axis == null || axis == Axis.Y)
			return null;

		return (axis == Axis.X) ? Axis.Z : Axis.X;
	}

	/**
	 * Teleport an entity to the specified position in the specified dimensionId
	 * facing the specified direction.
	 *
	 * @param entity      The entity to teleport. Can be any entity (item, mob,
	 *                    player).
	 * @param dimension   The dimension to port to.
	 * @param destination The position to port to.
	 * @param facing      The direction the entity should face after porting.
	 */
	public static void teleportTo(Entity entity, RegistryKey<World> dimension, BlockPos destination, Direction facing) {
		if (entity == null || dimension == null || destination == null || entity.isVehicle()
				|| entity.hasOnePlayerPassenger() || !entity.canChangeDimensions())
			return;

		ServerPlayerEntity player = (entity instanceof ServerPlayerEntity) ? (ServerPlayerEntity) entity : null;
		boolean interdimensional = (entity.level.dimension() != dimension);
		entity.setDeltaMovement(Vector3d.ZERO);

		if (player != null) {
			if (interdimensional) {
				MinecraftServer server = entity.getServer();
				if (server == null)
					return;
				teleportPlayerToDimension(server, player, dimension, destination, getYaw(facing));
			} else {
				player.connection.teleport(destination.getX() + 0.5d, destination.getY(), destination.getZ() + 0.5d,
						getYaw(facing), 0.0f);
			}

			// Play teleportation sound.
			if (Config.teleportationSoundEnabled.get())
				player.connection.send(new SPlaySoundEventPacket(1032, BlockPos.ZERO, 0, false));
		} else {
			if (interdimensional) {
				MinecraftServer server = entity.getServer();
				if (server == null)
					return;
				teleportEntityToDimension(server, entity, dimension, destination, getYaw(facing));
			} else {
				entity.moveTo(destination.getX() + 0.5d, destination.getY(), destination.getZ() + 0.5d, getYaw(facing),
						0.0f);
			}
		}
	}

	private static void teleportPlayerToDimension(MinecraftServer server, ServerPlayerEntity player,
			RegistryKey<World> destinationDimension, BlockPos destination, float yaw) {
		if (!net.minecraftforge.common.ForgeHooks.onTravelToDimension(player, destinationDimension))
			return;

		RegistryKey<World> originDimension = player.level.dimension();
		teleportEntityToDimension(server, player, destinationDimension, destination, yaw);

		net.minecraftforge.fml.hooks.BasicEventHooks.firePlayerChangedDimensionEvent(player, originDimension,
				destinationDimension);
	}

	/**
	 * Teleport a non-player entity to the specified position in the specified
	 * dimension facing the specified direction.
	 * ({@link Entity#changeDimension(DimensionType)} without the hardcoded
	 * dimension specific vanilla code)
	 *
	 * @param entity      The entity to teleport. Can be any entity except players
	 *                    (e.g. item, mob).
	 * @param dimension   The dimension to port to.
	 * @param destination The position to port to.
	 * @param yaw         The rotation yaw the entity should have after porting.
	 */
	private static void teleportEntityToDimension(MinecraftServer server, Entity entity, RegistryKey<World> dimension,
			BlockPos destination, float yaw) {
		if (dimension == null || server == null || entity == null || destination == null
				|| server.getLevel(dimension) == null) {
			return;
		}
		try {
			entity.changeDimension(server.getLevel(dimension), new ITeleporter() {
				@Override
				public Entity placeEntity(Entity entity, ServerWorld currentWorld, ServerWorld destWorld, float yaw,
						Function<Boolean, Entity> repositionEntity) {
					Entity repositionedEntity = repositionEntity.apply(false);
					repositionedEntity.moveTo(destination.getX(), destination.getY(), destination.getZ());
					return repositionedEntity;
				}
			});
		} catch (Throwable e) {
		}
	}

	/**
	 * Converts the specified facing to a degree value.
	 *
	 * @param facing The facing to convert.
	 * @return <code>0</code> if facing is <code>null</code>, otherwise a value
	 *         between <code>0</code> and <code>270</code> that is a multiple of
	 *         <code>90</code>.
	 */
	public static float getYaw(Direction facing) {
		if (facing == null)
			return 0;

		float yaw;

		switch (facing) {
		case EAST:
			yaw = 270.0f;
			break;
		case WEST:
			yaw = 90.0f;
			break;
		case NORTH:
			yaw = 180.0f;
			break;
		default:
			yaw = 0.0f;
			break;
		}

		return yaw;
	}

	/**
	 * Checks if a string is a valid representation of a ResourceLocation. Allowed
	 * characters in the namespace are: [a-z0-9_.-] Allowed characters in the path
	 * are: [a-z0-9._-/] Namespace and path are separated by [:].
	 *
	 * @param locationString The string to check.
	 * @return <code>true</code> if only valid characters where found, otherwise
	 *         <code>false</code>.
	 */
	public static boolean isValidResourceLocation(String locationString) {
		if (StringUtils.isNullOrEmpty(locationString))
			return false;

		String[] components = locationString.split(":", 2);
		if (components.length != 2 || components[0].length() == 0 || components[1].length() == 0)
			return false;

		return (components[0].chars()
				.allMatch(c -> (c == 95 || c == 45 || c >= 97 && c <= 122 || c >= 48 && c <= 57 || c == 46))
				&& components[1].chars().allMatch(
						c -> (c == 95 || c == 45 || c >= 97 && c <= 122 || c >= 48 && c <= 57 || c == 46 || c == 47)));
	}
}
