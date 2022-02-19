package com.silverminer.simpleportals_reloaded.configuration.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class CheckboxButtonEx extends AbstractButton {
    public boolean value;

    private static final ResourceLocation CHECKBOX_TEXTURE = new ResourceLocation("textures/gui/checkbox.png");

    public CheckboxButtonEx(int x, int y, int width, int height, Component message, boolean initialValue) {
        super(x, y, width, height, message);

        this.value = initialValue;
    }

    @Override
    public void onPress() {
        this.value = !this.value;
    }

    @Override
    public void render(@NotNull PoseStack ms, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.setShaderTexture(0, CHECKBOX_TEXTURE);
        RenderSystem.enableDepthTest();
        Font fontRenderer = minecraft.font;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        if (mouseX > this.x && mouseY > this.y &&
                mouseX < this.x + 10 && mouseY < this.y + this.height)
            blit(ms, this.x, this.y, 10.0F, this.value ? 20.0F : 0.0F, 10, this.height, 32, 64);
        else
            blit(ms, this.x, this.y, 0.0F, this.value ? 20.0F : 0.0F, 10, this.height, 32, 64);
        this.renderBg(ms, minecraft, mouseX, mouseY);
        GuiComponent.drawString(ms, fontRenderer, this.getMessage(), this.x + 24, this.y + (this.height - 8) / 2, 14737632 | Mth.ceil(this.alpha * 255.0F) << 24);
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput pNarrationElementOutput) {

    }
}
