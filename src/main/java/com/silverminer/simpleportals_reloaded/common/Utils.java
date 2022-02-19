package com.silverminer.simpleportals_reloaded.common;

import com.silverminer.simpleportals_reloaded.configuration.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.function.Function;

/**
 * General utility class.
 */
public final class Utils {
    private static final Logger LOGGER = LogManager.getLogger(Utils.class);
    private static final Language I18N = Language.getInstance();

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
     * if the key was not found.
     */
    public static ArrayList<Component> multiLineTranslate(String key, Object... args) {
        ArrayList<Component> lines = new ArrayList<>();

        if (key != null) {
            int x = 0;
            String currentKey = key + x;

            Component text;
            while (!(text = new TranslatableComponent(currentKey, args)).getString().equals("")) {
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
     * the chosen coordinate component.
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
     * the arguments was <code>null</code>.
     */
    public static Direction getRelativeDirection(BlockPos from, BlockPos to) {
        if (from == null || to == null)
            return null;

        BlockPos directionVec = to.subtract(from);
        directionVec = new BlockPos(directionVec.getX() / Math.max(1, Math.abs(directionVec.getX())),
                directionVec.getY() / Math.max(1, Math.abs(directionVec.getY())),
                directionVec.getZ() / Math.max(1, Math.abs(directionVec.getZ())));

        return Direction.fromNormal(directionVec.getX(), directionVec.getY(), directionVec.getZ());
    }

    /**
     * Gets the axis that is orthogonal to, and on the same plane as the specified
     * one.
     *
     * @param axis The starting axis.
     * @return One of the {@link Axis} values or <code>null</code> if the specified
     * axis was <code>null</code> or there is no other axis on the same
     * plane.
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
    public static void teleportTo(Entity entity, ResourceKey<Level> dimension, BlockPos destination, Direction facing) {
        if (entity == null || dimension == null || destination == null || entity.isVehicle()
                || entity.hasExactlyOnePlayerPassenger() || !entity.canChangeDimensions())
            return;

        ServerPlayer player = (entity instanceof ServerPlayer) ? (ServerPlayer) entity : null;
        boolean interdimensional = (entity.level.dimension() != dimension);
        entity.setDeltaMovement(Vec3.ZERO);

        if (player != null) {
            if (interdimensional) {
                MinecraftServer server = entity.getServer();
                if (server == null)
                    return;
                teleportPlayerToDimension(server, player, dimension, destination);
            } else {
                player.connection.teleport(destination.getX() + 0.5d, destination.getY(), destination.getZ() + 0.5d,
                        getYaw(facing), 0.0f);
            }

            // Play teleportation sound.
            if (Config.teleportationSoundEnabled.get())
                player.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
        } else {
            if (interdimensional) {
                MinecraftServer server = entity.getServer();
                if (server == null)
                    return;
                teleportEntityToDimension(server, entity, dimension, destination);
            } else {
                entity.moveTo(destination.getX() + 0.5d, destination.getY(), destination.getZ() + 0.5d, getYaw(facing),
                        0.0f);
            }
        }
    }

    private static void teleportPlayerToDimension(MinecraftServer server, ServerPlayer player,
                                                  ResourceKey<Level> destinationDimension, BlockPos destination) {
        //if (!net.minecraftforge.common.ForgeHooks.onTravelToDimension(player, destinationDimension))
        //    return;

        //ResourceKey<Level> originDimension = player.level.dimension();
        teleportEntityToDimension(server, player, destinationDimension, destination);

        //net.minecraftforge.fml.hooks.BasicEventHooks.firePlayerChangedDimensionEvent(player, originDimension,
        //        destinationDimension);
    }

    /**
     * Teleport a non-player entity to the specified position in the specified
     * dimension facing the specified direction.
     * ({@link Entity#changeDimension(ServerLevel, ITeleporter)} without the hardcoded
     * dimension specific vanilla code)
     *
     * @param entity      The entity to teleport. Can be any entity except players
     *                    (e.g. item, mob).
     * @param dimension   The dimension to port to.
     * @param destination The position to port to.
     */
    private static void teleportEntityToDimension(MinecraftServer server, Entity entity, ResourceKey<Level> dimension,
                                                  BlockPos destination) {
        if (dimension == null || server == null || entity == null || destination == null
                || server.getLevel(dimension) == null) {
            return;
        }
        try {
            ServerLevel serverLevel = server.getLevel(dimension);
            if (serverLevel != null) {
                entity.changeDimension(serverLevel, new ITeleporter() {
                    @Override
                    public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw,
                                              Function<Boolean, Entity> repositionEntity) {
                        Entity repositionedEntity = repositionEntity.apply(false);
                        repositionedEntity.moveTo(destination.getX(), destination.getY(), destination.getZ());
                        return repositionedEntity;
                    }
                });
            } else {
                LOGGER.error("Tried to teleport entity to dimension that doesn't exist");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to teleport entity to another dimension");
        }
    }

    /**
     * Converts the specified facing to a degree value.
     *
     * @param facing The facing to convert.
     * @return <code>0</code> if facing is <code>null</code>, otherwise a value
     * between <code>0</code> and <code>270</code> that is a multiple of
     * <code>90</code>.
     */
    public static float getYaw(Direction facing) {
        if (facing == null)
            return 0;

        return switch (facing) {
            case EAST -> 270.0f;
            case WEST -> 90.0f;
            case NORTH -> 180.0f;
            default -> 0.0f;
        };
    }

    /**
     * Checks if a string is a valid representation of a ResourceLocation. Allowed
     * characters in the namespace are: [a-z0-9_.-] Allowed characters in the path
     * are: [a-z0-9._-/] Namespace and path are separated by [:].
     *
     * @param locationString The string to check.
     * @return <code>true</code> if only valid characters where found, otherwise
     * <code>false</code>.
     */
    public static boolean isValidResourceLocation(String locationString) {
        if (StringUtil.isNullOrEmpty(locationString))
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
