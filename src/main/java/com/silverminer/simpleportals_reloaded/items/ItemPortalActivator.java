package com.silverminer.simpleportals_reloaded.items;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
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
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag)
	{
		long windowHandle = Minecraft.getInstance().getWindow().getWindow();
		int leftShiftState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT);
		int rightShiftState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT);

		// This does not work for some reason.
		//KeyBinding SneakKey = Minecraft.getInstance().gameSettings.keyBindSneak;
		//if (SneakKey.isKeyDown())

		if (leftShiftState == GLFW.GLFW_PRESS || rightShiftState == GLFW.GLFW_PRESS)
		{
			tooltip.add(new TranslatableComponent(toolTipDetailsKey));
		}
		else
		{
			tooltip.add(new TranslatableComponent(toolTipKey));
		}
	}

	@Override
	public boolean doesSneakBypassUse(ItemStack stack, LevelReader world, BlockPos pos, Player player)
	{
		return true;
	}

	@Override
	public InteractionResult useOn(UseOnContext context)
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
	private final static DispenseItemBehavior dispenserBehavior = new DispenseItemBehavior()
	{
		private final DefaultDispenseItemBehavior defaultBehavior = new DefaultDispenseItemBehavior();
		
		@Override
		public ItemStack dispense(BlockSource source, ItemStack stack)
		{
			if (ItemStack.isSame(stack, new ItemStack(SimplePortals.itemPortalActivator)))
			{
				Level world = source.getLevel();
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