package com.silverminer.simpleportals_reloaded.configuration.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.silverminer.simpleportals_reloaded.SimplePortals;

/**
 * The factory providing the in-game config UI.
 */
@OnlyIn(Dist.CLIENT)
public final class ConfigGuiFactory
{
	private static ForgeConfigSpec[] configSpecs;

	public static Screen getConfigGui(Minecraft mc, Screen parent)
	{
		return new ConfigGui(new TextComponent("Simple Portals Config"), parent, configSpecs);
	}

	public static void setConfigHolder(String classPath)
	{
		Class<?> classHolder = null;

		try
		{
			classHolder = Class.forName(classPath);

		}
		catch (ClassNotFoundException ex)
		{
			SimplePortals.log.error("Config holder class not found.");
		}

		if (classHolder == null) return;

		List<ForgeConfigSpec> specs = new ArrayList<>();

		for (Field field : classHolder.getFields())
		{
			if (field.getType() == ForgeConfigSpec.class)
			{
				try
				{
					specs.add((ForgeConfigSpec)field.get(classHolder));
				}
				catch (IllegalAccessException ex)
				{
					SimplePortals.log.error("Could not access ForgeConfigSpec fields of the config holder class.");
				}
			}
		}

		configSpecs = new ForgeConfigSpec[specs.size()];
		specs.toArray(configSpecs);
	}
}
