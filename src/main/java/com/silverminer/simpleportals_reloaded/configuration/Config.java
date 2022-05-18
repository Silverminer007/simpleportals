package com.silverminer.simpleportals_reloaded.configuration;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.silverminer.simpleportals_reloaded.common.Utils;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;

import java.nio.file.Path;

/**
 * Provides helper methods to load the mods config.
 */

public final class Config
{
	// Configs

	public static ForgeConfigSpec CommonConfigSpec;
	public static ForgeConfigSpec ClientConfigSpec;

	// Config builders

	private static final ForgeConfigSpec.Builder CommonConfigBuilder = new ForgeConfigSpec.Builder();
	private static final ForgeConfigSpec.Builder ClientConfigBuilder = new ForgeConfigSpec.Builder();

	// Default values
	
	private static final int defaultMaxSize = 7;
	private static final int defaultPowerCost = 1;
	private static final int defaultPowerCapacity = 64;
	private static final boolean defaultParticlesEnabled = true;
	private static final boolean defaultAmbientSoundEnabled = false;
	private static final boolean defaultTeleportationSoundEnabled = true;
	private static final int defaultRequiredPermissionLevelToActivate = 0;
	private static final String defaultPowerSource = "forge:ender_pearls";

	// Settings

	public static ForgeConfigSpec.IntValue maxSize;
	public static ForgeConfigSpec.IntValue powerCost;
	public static ForgeConfigSpec.IntValue powerCapacity;
	public static ForgeConfigSpec.BooleanValue particlesEnabled;
	public static ForgeConfigSpec.BooleanValue ambientSoundEnabled;
	public static ForgeConfigSpec.BooleanValue teleportationSoundEnabled;
	public static ForgeConfigSpec.IntValue requiredPermissionLevelToActivate;
	public static ResourceLocation powerSource;
	public static TagKey<Item> getPowerSourceTag(){
		return TagKey.create(Registry.ITEM_REGISTRY, powerSource);
	}

	private static ForgeConfigSpec.ConfigValue<String> powerSourceString;

	static
	{
		// Common

		CommonConfigBuilder.push("common");

		maxSize = CommonConfigBuilder.translation("config.max_size")
				.comment("The maximum size of the portal including the frame")
				.defineInRange("maxSize", defaultMaxSize, 3, 128);

		powerCost = CommonConfigBuilder.translation("config.power_cost")
				.comment("The power cost per port. Set to 0 for no cost.")
				.defineInRange("powerCost", defaultPowerCost, -1, Integer.MAX_VALUE);

		powerCapacity = CommonConfigBuilder.translation("config.power_capacity")
				.comment("The amount of power a portal can store.")
				.defineInRange("powerCapacity", defaultPowerCapacity, 0, Integer.MAX_VALUE);

		powerSourceString = CommonConfigBuilder.translation("config.power_source")
				.comment("The tag that items must have to be able to power portals (1 power per item).")
				.define("powerSource", defaultPowerSource, o -> Utils.isValidResourceLocation((String)o));

		requiredPermissionLevelToActivate = CommonConfigBuilder.translation("config.perm_level")
				.comment("The Required Level to activate a portal")
				.defineInRange("perm_level", defaultRequiredPermissionLevelToActivate, 0, 4);

		CommonConfigBuilder.pop();

		CommonConfigSpec = CommonConfigBuilder.build();

		// Client

		ClientConfigBuilder.push("client");

		particlesEnabled = ClientConfigBuilder.translation("config.particles_enabled")
				.comment("If enabled, portals emit particles.")
				.define("particlesEnabled", defaultParticlesEnabled);

		ambientSoundEnabled = ClientConfigBuilder.translation("config.ambient_sound_enabled")
				.comment("If enabled, portals emit an ambient sound.")
				.define("ambientSoundEnabled", defaultAmbientSoundEnabled);

		teleportationSoundEnabled = ClientConfigBuilder.translation("config.teleportation_sound_enabled")
				.comment("If enabled, a sound effect is played to the player after a successful teleportation.")
				.define("teleportationSoundEnabled", defaultTeleportationSoundEnabled);

		ClientConfigBuilder.pop();

		ClientConfigSpec = ClientConfigBuilder.build();
	}

	/**
	 * Loads the mods settings from the specified file.
	 * 
	 * @param configSpec
	 * The specification for the contents of the config file.
	 * @param path
	 * The path to the config file.
	 */

	public static void load(ForgeConfigSpec configSpec, Path path)
	{
		final CommentedFileConfig configData = CommentedFileConfig.builder(path)
				.sync()
				.writingMode(WritingMode.REPLACE)
				.build();

		configData.load();
		configSpec.setConfig(configData);
	}

	public static void updatePowerSource()
	{
		String powerTag = powerSourceString.get();

		powerSource = (Utils.isValidResourceLocation(powerTag)) ?
					  new ResourceLocation(powerTag) :
					  new ResourceLocation("", "");
	}
}
