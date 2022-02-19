package com.silverminer.simpleportals_reloaded.blocks;

import com.silverminer.simpleportals_reloaded.SimplePortals;
import com.silverminer.simpleportals_reloaded.configuration.Config;
import com.silverminer.simpleportals_reloaded.registration.Portal;
import com.silverminer.simpleportals_reloaded.registration.PortalRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents the frame of the portal mutliblock.
 */
public class BlockPortalFrame extends Block {
	public BlockPortalFrame() {
		this(SimplePortals.BLOCK_PORTAL_FRAME_NAME);
	}

	public BlockPortalFrame(String registryName) {
		super(Block.Properties.of(Material.STONE, MaterialColor.COLOR_BLACK).strength(50.0f, 200.0f)
				.sound(SoundType.STONE).requiresCorrectToolForDrops());

		setRegistryName(registryName);
	}

	@SuppressWarnings("deprecation")
	@Override
	public @NotNull InteractionResult use(@NotNull BlockState state, Level world, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand,
										  @NotNull BlockHitResult hit) {
		if (!world.isClientSide) {
			ItemStack heldStack = player.getItemInHand(hand);
			Item usedItem = heldStack.getItem();

			if (usedItem == SimplePortals.itemPortalActivator) {
				if (player.hasPermissions(Config.requiredPermissionLevelToActivate.get())) {
					// Another case of shitty renaming. Hey let's rename isSneaking() to
					// isShiftKeyDown() because nobody
					// would ever rebind buttons or would they? -sigh-
					if (player.isShiftKeyDown()) {
						world.destroyBlock(pos, true);
					} else if (!PortalRegistry.isPortalAt(pos, player.level.dimension())) {
						PortalRegistry.activatePortal(world, pos, hit.getDirection());
					}
				} else {
					player.sendMessage(new TranslatableComponent("info.simpleportals_reloaded.no_permission"), player.getUUID());
				}
			}
		}

		return super.use(state, world, pos, player, hand, hit);
	}

	@SuppressWarnings("deprecation")
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

		super.onRemove(oldState, world, pos, newState, isMoving);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void neighborChanged(@NotNull BlockState state, Level world, @NotNull BlockPos pos, @NotNull Block neighborBlock, @NotNull BlockPos neighborPos,
								boolean isMoving) {
		if (!world.isClientSide &&
				!neighborBlock.defaultBlockState().isAir() && // I'd like to supply a proper
																				// BlockState here but the new block has
																				// already been placed, so there's no
																				// way.
				neighborBlock != SimplePortals.blockPortalFrame && neighborBlock != SimplePortals.blockPowerGauge
				&& neighborBlock != SimplePortals.blockPortal) {
			// Deactivate all portals that share this frame block if an address block was
			// removed or changed.

			List<Portal> affectedPortals = PortalRegistry.getPortalsAt(pos, world.dimension());

			if (affectedPortals == null || affectedPortals.size() < 1)
				return;

			Portal firstPortal = affectedPortals.get(0);

			if (firstPortal.hasAddressChanged(world)) {
				PortalRegistry.deactivatePortal(world, pos);
			}
		}

		super.neighborChanged(state, world, pos, neighborBlock, neighborPos, isMoving);
	}
}