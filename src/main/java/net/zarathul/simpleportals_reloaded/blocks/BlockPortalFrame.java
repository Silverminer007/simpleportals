package net.zarathul.simpleportals_reloaded.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.zarathul.simpleportals_reloaded.SimplePortals;
import net.zarathul.simpleportals_reloaded.registration.Portal;
import net.zarathul.simpleportals_reloaded.registration.PortalRegistry;

import java.util.List;

/**
 * Represents the frame of the portal mutliblock.
 */
public class BlockPortalFrame extends Block
{
	public BlockPortalFrame()
	{
		this(SimplePortals.BLOCK_PORTAL_FRAME_NAME);
	}
	
	public BlockPortalFrame(String registryName)
	{
		super(Block.Properties.of(Material.STONE, MaterialColor.COLOR_BLACK)
				.strength(50.0f, 200.0f)
				.sound(SoundType.STONE)
				.harvestTool(ToolType.PICKAXE)
				.harvestLevel(3));

		setRegistryName(registryName);
	}

	@SuppressWarnings("deprecation")
	@Override
	public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
	{
		if (!world.isClientSide)
		{
			ItemStack heldStack = player.getItemInHand(hand);
			Item usedItem = heldStack.getItem();

			if (usedItem == SimplePortals.itemPortalActivator)
			{
				// Another case of shitty renaming. Hey let's rename isSneaking() to isShiftKeyDown() because nobody
				// would ever rebind buttons or would they?  -sigh-
				if (player.isShiftKeyDown())
				{
					world.destroyBlock(pos, true);
				}
				else if (!PortalRegistry.isPortalAt(pos, player.level.dimension()))
				{
					PortalRegistry.activatePortal(world, pos, hit.getDirection());
				}
			}
		}

		return super.use(state, world, pos, player, hand, hit);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onRemove(BlockState oldState, World world, BlockPos pos, BlockState newState, boolean isMoving)
	{
		if (!world.isClientSide)
		{
			// Deactivate damaged portals.

			List<Portal> affectedPortals = PortalRegistry.getPortalsAt(pos, world.dimension());

			if (affectedPortals == null || affectedPortals.size() < 1) return;

			Portal firstPortal = affectedPortals.get(0);

			if (firstPortal.isDamaged(world))
			{
				PortalRegistry.deactivatePortal(world, pos);
			}
		}

		super.onRemove(oldState, world, pos, newState, isMoving);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void neighborChanged(BlockState state, World world, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMoving)
	{
		if (!world.isClientSide	&&
				// This line changes in 1.17: Remove every parameter and it will work again
			!neighborBlock.defaultBlockState().isAir(world, neighborPos) &&	// I'd like to supply a proper BlockState here but the new block has already been placed, so there's no way.
			neighborBlock != SimplePortals.blockPortalFrame	&&
			neighborBlock != SimplePortals.blockPowerGauge &&
			neighborBlock != SimplePortals.blockPortal)
		{
			// Deactivate all portals that share this frame block if an address block was removed or changed.

			List<Portal> affectedPortals = PortalRegistry.getPortalsAt(pos, world.dimension());

			if (affectedPortals == null || affectedPortals.size() < 1) return;

			Portal firstPortal = affectedPortals.get(0);

			if (firstPortal.hasAddressChanged(world))
			{
				PortalRegistry.deactivatePortal(world, pos);
			}
		}

		super.neighborChanged(state, world, pos, neighborBlock, neighborPos, isMoving);
	}
}