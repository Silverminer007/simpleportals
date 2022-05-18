package com.silverminer.simpleportals_reloaded.blocks;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.silverminer.simpleportals_reloaded.SimplePortals;
import com.silverminer.simpleportals_reloaded.common.TeleportTask;
import com.silverminer.simpleportals_reloaded.common.Utils;
import com.silverminer.simpleportals_reloaded.configuration.Config;
import com.silverminer.simpleportals_reloaded.registration.Portal;
import com.silverminer.simpleportals_reloaded.registration.PortalRegistry;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

/**
 * Represents the actual portals in the center of the portal multiblock.
 */
public class BlockPortal extends HalfTransparentBlock {
   private static final VoxelShape X_AABB = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);
   private static final VoxelShape Y_AABB = Block.box(0.0D, 6.0D, 0.0D, 16.0D, 10.0D, 16.0D);
   private static final VoxelShape Z_AABB = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);

   public static final EnumProperty<Axis> AXIS = EnumProperty.create("axis", Axis.class, Axis.X, Axis.Y, Axis.Z);

   public BlockPortal() {
      super(Block.Properties.of(Material.PORTAL).noCollission().noDrops().strength(-1.0F) // indestructible by normal
            // means
            .lightLevel((state) -> 11).sound(SoundType.GLASS));

      setRegistryName(SimplePortals.BLOCK_PORTAL_NAME);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> stateBuilder) {
      stateBuilder.add(AXIS);
   }

   @Override
   public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter blockReader, @NotNull BlockPos pos, @NotNull CollisionContext selection) {
      Axis portalAxis = state.getValue(AXIS);

      switch (portalAxis) {
         case Y:
            return Y_AABB;
         case Z:
            return Z_AABB;
         case X:
         default:
            return X_AABB;
      }
   }

   @Override
   public void entityInside(@NotNull BlockState state, Level world, @NotNull BlockPos pos, @NotNull Entity entity) {
      if (!world.isClientSide && entity.isAlive() && !entity.isPassenger() && !entity.isVehicle()
            && entity.canChangeDimensions()
            && Shapes.joinIsNotEmpty(
            Shapes.create(entity.getBoundingBox().move(-pos.getX(),
                  -pos.getY(), -pos.getZ())),
            state.getShape(world, pos), BooleanOp.AND)) {

         List<Portal> portals = PortalRegistry.getPortalsAt(pos, entity.level.dimension());

         if (portals == null || portals.size() < 1)
            return;

         Portal start = portals.get(0);

         // Handle power source entering the portal

         if (entity instanceof ItemEntity && Config.powerCost.get() > 0 && Config.powerCapacity.get() > 0) {
            ItemStack item = ((ItemEntity) entity).getItem();

            ITagManager<Item> tagManager = ForgeRegistries.ITEMS.tags();
            if ((PortalRegistry.getPower(start) < Config.powerCapacity.get())
                  && tagManager != null && tagManager.isKnownTagName(Config.getPowerSourceTag())) {
               int surplus = PortalRegistry.addPower(start, item.getCount());

               PortalRegistry.updatePowerGauges(world, start);

               if (surplus > 0) {
                  item.setCount(surplus);
               } else {
                  entity.remove(Entity.RemovalReason.DISCARDED);
               }

               return;
            }
         }

         // Bypass the power cost for players in creative mode
         boolean bypassPowerCost = (entity instanceof ServerPlayer
               && ((ServerPlayer) entity).isCreative());

         // Check if portal has enough power for a port
         if (!bypassPowerCost && PortalRegistry.getPower(start) < Config.powerCost.get())
            return;

         portals = PortalRegistry.getPortalsWithAddress(start.getAddress());

         if (portals == null || portals.size() < 2)
            return;

         // Get a shuffled list of possible destination portals (portals with the same
         // address)
         List<Portal> destinations = portals.stream().filter(e -> !e.equals(start)).collect(Collectors.toList());

         if (destinations.size() > 0) {
            Collections.shuffle(destinations);

            MinecraftServer mcServer = entity.getServer();
            if (mcServer == null)
               return;

            int entityHeight = Mth.ceil(entity.getBbHeight());
            ServerLevel serverWorld;
            ResourceKey<Level> dimension;
            BlockPos destinationPos = null;
            Portal destinationPortal = null;

            // Pick the first not blocked destination portal
            for (Portal portal : destinations) {
               dimension = portal.getDimension();
               if (dimension == null)
                  continue;

               serverWorld = mcServer.getLevel(dimension);
               destinationPos = portal.getPortDestination(serverWorld, entityHeight);

               if (destinationPos != null) {
                  destinationPortal = portal;
                  break;
               }
            }

            if ((destinationPos != null) && (bypassPowerCost || Config.powerCost.get() == 0
                  || PortalRegistry.removePower(start, Config.powerCost.get()))) {
               // Get a facing pointing away from the destination portal. After porting, the
               // portal
               // will always be behind the entity. When porting to a horizontal portal the
               // initial
               // facing is not changed.
               Direction entityFacing = (destinationPortal.getAxis() == Axis.Y) ? entity.getMotionDirection()
                     : (destinationPortal.getAxis() == Axis.Z)
                     ? (destinationPos.getZ() > destinationPortal.getCorner1().getPos().getZ())
                     ? Direction.SOUTH
                     : Direction.NORTH
                     : (destinationPos.getX() > destinationPortal.getCorner1().getPos().getX())
                     ? Direction.EAST
                     : Direction.WEST;

               if (entity instanceof ServerPlayer) {
                  // Player teleportations are queued to avoid at least some of the problems that
                  // arise from
                  // handling player teleportation inside an entity collision handler. There seem
                  // to be all
                  // kinds of weird race conditions of movement packets that trigger the dreaded
                  // "moved wrongly"
                  // and "moved to quickly" checks in ServerPlayNetHandler.processPlayer(). No
                  // idea why end portals
                  // don't have these problems, considering that I use the same copy and pasted
                  // code minus the
                  // platform generation stuff.
                  try {
                     SimplePortals.TELEPORT_QUEUE
                           .put(new TeleportTask(mcServer.getTickCount(), (ServerPlayer) entity,
                                 destinationPortal.getDimension(), destinationPos, entityFacing));
                  } catch (InterruptedException ex) {
                     SimplePortals.log.error(
                           "Failed to enqueue teleportation task for player '{}' to dimension '{}'.",
                           entity.getName(),
                           destinationPortal.getDimension().getRegistryName());
                  }
               } else {
                  Utils.teleportTo(entity, destinationPortal.getDimension(), destinationPos, entityFacing);
               }

               PortalRegistry.updatePowerGauges(world, start);
            }
         }
      }
   }

   @Override
   public void onRemove(@NotNull BlockState oldState, Level world, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
      if (!world.isClientSide) {
         // Deactivate damaged portals.

         List<Portal> affectedPortals = PortalRegistry.getPortalsAt(pos, world.dimension());
         if (affectedPortals == null || affectedPortals.size() < 1)
            return;

         Portal firstPortal = affectedPortals.get(0);

         if (firstPortal.isDamaged(world)) {
            PortalRegistry.deactivatePortal(world, pos);
         }
      }
   }

   @Override
   public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter world, BlockPos pos, Player player) {
      return ItemStack.EMPTY;
   }

   @Override
   @OnlyIn(Dist.CLIENT)
   public void animateTick(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Random rand) {
      if (Config.ambientSoundEnabled.get() && rand.nextInt(100) == 0) {
         world.playLocalSound((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D,
               SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.5F, rand.nextFloat() * 0.4F + 0.8F,
               false);
      }

      if (Config.particlesEnabled.get()) {
         for (int i = 0; i < 4; ++i) {
            double d0 = (float) pos.getX() + rand.nextFloat();
            double d1 = (float) pos.getY() + rand.nextFloat();
            double d2 = (float) pos.getZ() + rand.nextFloat();
            double d3 = ((double) rand.nextFloat() - 0.5D) * 0.5D;
            double d4 = ((double) rand.nextFloat() - 0.5D) * 0.5D;
            double d5 = ((double) rand.nextFloat() - 0.5D) * 0.5D;
            int j = rand.nextInt(2) * 2 - 1;

            if (world.getBlockState(pos.west()).getBlock() != this
                  && world.getBlockState(pos.east()).getBlock() != this) {
               d0 = (double) pos.getX() + 0.5D + 0.25D * (double) j;
               d3 = rand.nextFloat() * 2.0F * (float) j;
            } else {
               d2 = (double) pos.getZ() + 0.5D + 0.25D * (double) j;
               d5 = rand.nextFloat() * 2.0F * (float) j;
            }

            world.addParticle(ParticleTypes.PORTAL, d0, d1, d2, d3, d4, d5);
         }
      }
   }
}