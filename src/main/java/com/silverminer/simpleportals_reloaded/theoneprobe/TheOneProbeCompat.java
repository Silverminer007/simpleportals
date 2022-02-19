package com.silverminer.simpleportals_reloaded.theoneprobe;

import mcjty.theoneprobe.api.ITheOneProbe;

import javax.annotation.Nullable;

import com.silverminer.simpleportals_reloaded.SimplePortals;

import java.util.function.Function;

/**
 * Hosts the callback for TheOneProbe.
 */
public final class TheOneProbeCompat implements Function<ITheOneProbe, Void>
{
	@Nullable
	@Override
	public Void apply(ITheOneProbe theOneProbe)
	{
		theOneProbe.registerProvider(new PortalInfoProvider());
		SimplePortals.log.debug("TheOneProbe compatibility enabled.");

		return null;
	}
}
