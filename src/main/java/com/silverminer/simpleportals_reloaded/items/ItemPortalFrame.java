package com.silverminer.simpleportals_reloaded.items;

import net.minecraft.world.level.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.lwjgl.glfw.GLFW;

import com.silverminer.simpleportals_reloaded.SimplePortals;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Portal frame in item form.
 */
public class ItemPortalFrame extends BlockItem
{
	private static final String toolTipKey = "item." + SimplePortals.MOD_ID + "." + SimplePortals.ITEM_PORTAL_FRAME_NAME + ".tooltip";
	private static final String toolTipDetailsKey = "item." + SimplePortals.MOD_ID + "." + SimplePortals.ITEM_PORTAL_FRAME_NAME + ".tooltip_details";

	public ItemPortalFrame(Block block)
	{
		super(block, new Item.Properties().tab(SimplePortals.creativeTab));

		setRegistryName(SimplePortals.ITEM_PORTAL_FRAME_NAME);
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
}