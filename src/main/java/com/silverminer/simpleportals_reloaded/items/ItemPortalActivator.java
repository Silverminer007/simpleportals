package com.silverminer.simpleportals_reloaded.items;

import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.dispenser.IDispenseItemBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Direction.AxisDirection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.lwjgl.glfw.GLFW;

import com.silverminer.simpleportals_reloaded.SimplePortals;
import com.silverminer.simpleportals_reloaded.blocks.BlockPortalFrame;
import com.silverminer.simpleportals_reloaded.registration.PortalRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The item used to activate portals.
 */
public class ItemPortalActivator extends Item
{
	private static final String toolTipKey = "item." + SimplePortals.MOD_ID + "." + SimplePortals.ITEM_PORTAL_ACTIVATOR_NAME + ".tooltip";
	private static final String toolTipDetailsKey = "item." + SimplePortals.MOD_ID + "." + SimplePortals.ITEM_PORTAL_ACTIVATOR_NAME + ".tooltip_details";
	
	public ItemPortalActivator()
	{
		super(new Item.Properties().stacksTo(1).tab(SimplePortals.creativeTab));

		setRegistryName(SimplePortals.ITEM_PORTAL_ACTIVATOR_NAME);
		DispenserBlock.registerBehavior(this, dispenserBehavior);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag)
	{
		long windowHandle = Minecraft.getInstance().getWindow().getWindow();
		int leftShiftState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT);
		int rightShiftState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT);

		// This does not work for some reason.
		//KeyBinding SneakKey = Minecraft.getInstance().gameSettings.keyBindSneak;
		//if (SneakKey.isKeyDown())

		if (leftShiftState == GLFW.GLFW_PRESS || rightShiftState == GLFW.GLFW_PRESS)
		{
			tooltip.add(new TranslationTextComponent(toolTipDetailsKey));
		}
		else
		{
			tooltip.add(new TranslationTextComponent(toolTipKey));
		}
	}

	@Override
	public boolean doesSneakBypassUse(ItemStack stack, IWorldReader world, BlockPos pos, PlayerEntity player)
	{
		return true;
	}

	@Override
	public ActionResultType useOn(ItemUseContext context)
	{
		if (context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof BlockPortalFrame) {
			context.getPlayer().swing(context.getHand());
		}

		return super.useOn(context);
	}

	/**
	 * Custom dispenser behavior that allows dispensers to activate portals with a contained
	 * portal activator.
	 */
	private final static IDispenseItemBehavior dispenserBehavior = new IDispenseItemBehavior()
	{
		private final DefaultDispenseItemBehavior defaultBehavior = new DefaultDispenseItemBehavior();
		
		@Override
		public ItemStack dispense(IBlockSource source, ItemStack stack)
		{
			if (ItemStack.isSame(stack, new ItemStack(SimplePortals.itemPortalActivator)))
			{
				World world = source.getLevel();
				BlockState dispenser = world.getBlockState(source.getPos());
				
				// Start searching for portal frame blocks in the direction the dispenser is facing.
				Direction dispenserFacing = dispenser.getValue(DispenserBlock.FACING);
				BlockPos searchStartPos = source.getPos().relative(dispenserFacing);
				
				if (world.isEmptyBlock(searchStartPos))
				{
					// Search along the other two axis besides the one the dispenser is facing in.
					// E.g. dispenser faces south: Search one block south of the dispenser, up, down,
					// east and west.
					List<Direction> searchDirections = new ArrayList<>();
					Axis dispenserAxis = dispenserFacing.getAxis();
					
					for (Axis axis : Axis.values())
					{
						if (axis != dispenserAxis)
						{
							searchDirections.add(Direction.get(AxisDirection.POSITIVE, axis));
							searchDirections.add(Direction.get(AxisDirection.NEGATIVE, axis));
						}
					}
					
					BlockPos currentPos;
					
					for (Direction facing : searchDirections)
					{
						currentPos = searchStartPos.relative(facing);
						
						if (world.getBlockState(currentPos).getBlock() instanceof BlockPortalFrame)
						{
							if (PortalRegistry.activatePortal(world, currentPos, facing.getOpposite()))
							{
								return stack;
							}
						}
					}
				}
			}
			
			return defaultBehavior.dispense(source, stack);
		}
	};
}