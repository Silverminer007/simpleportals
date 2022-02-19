package com.silverminer.simpleportals_reloaded.configuration.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
class ValidationStatusButton extends Button
{
	private boolean valid;

	public ValidationStatusButton(int x, int y, OnPress clickHandler)
	{
		super(x, y, 15, 15, new TextComponent(""), clickHandler);

		this.valid = true;
	}

	public void setValid(boolean isValid)
	{
		this.valid = isValid;
	}

	public void setInvalid()
	{
		this.valid = false;
	}

	public boolean isValid()
	{
		return this.valid;
	}

	@Override
	public void render(@NotNull PoseStack ms, int mouseX, int mouseY, float partialTicks)
	{
		RenderSystem.setShaderTexture(0, Button.WIDGETS_LOCATION);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.alpha);
		Icon icon = (this.valid) ? Icon.VALID : Icon.INVALID;

		this.blit(ms, this.x, this.y, icon.getX(), icon.getY(), this.width, this.height);
	}

	@Override
	public boolean changeFocus(boolean forward)
	{
		return false;
	}

	enum Icon
	{
		VALID(208, 0),
		INVALID(192, 0);

		private final int x;
		private final int y;

		Icon(int x, int y)
		{
			this.x = x;
			this.y = y;
		}

		public int getX()
		{
			return this.x;
		}

		public int getY()
		{
			return this.y;
		}
	}
}
