package com.silverminer.simpleportals_reloaded.configuration.gui;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.StringUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class ConfigGui extends Screen {
	private final Screen parent;
	private final ForgeConfigSpec[] configSpecs;
	private ModOptionList optionList;

	private static final int PADDING = 5;

	public ConfigGui(Component title, Screen parent, ForgeConfigSpec[] configSpecs) {
		super(title);

		this.parent = parent;
		this.configSpecs = configSpecs;
	}

	@Override
	protected void init() {
		if(this.minecraft == null) return;
		int titleHeight = this.font.wordWrapHeight(title.getString(), width - 2 * PADDING);
		int paddedTitleHeight = titleHeight + PADDING * 2;

		addButton(width - 120 - 2 * PADDING, 0, 60, paddedTitleHeight, new TextComponent("Back"),
				button -> this.minecraft.setScreen(parent));
		addButton(width - 60 - PADDING, 0, 60, paddedTitleHeight, new TextComponent("Save"), button -> {
			this.optionList.commitChanges();
			for (ForgeConfigSpec spec : configSpecs)
				spec.save();

			this.minecraft.setScreen(parent);
		});

		int optionListHeaderHeight = titleHeight + 2 * PADDING;
		this.optionList = new ModOptionList(configSpecs, minecraft, width, height, optionListHeaderHeight,
				height - optionListHeaderHeight, 26);
		this.addRenderableWidget(this.optionList);
	}

	private void addButton(int x, int y, int width, int height, Component label, Button.OnPress pressHandler) {
		Button button = new ExtendedButton(x, y, width, height, label, pressHandler);

		this.addRenderableWidget(button);
	}

	@Override
	public void render(@NotNull PoseStack ms, int mouseX, int mouseY, float partialTicks) {
		if(this.minecraft == null) return;
		this.renderBackground(ms);
		this.optionList.render(ms, mouseX, mouseY, partialTicks);
		super.render(ms, mouseX, mouseY, partialTicks);
		minecraft.font.draw(ms, title.getString(), PADDING, PADDING, 16777215);
	}

	@Override
	public void tick() {
		super.tick();
		optionList.tick();
	}

	@OnlyIn(Dist.CLIENT)
	public class ModOptionList extends ContainerObjectSelectionList<ModOptionList.Entry> {
		private static final int LEFT_RIGHT_BORDER = 30;
		private static final String I18N_TOOLTIP_SUFFIX = ".tooltip";
		private static final String I18N_VALID = "config.input_valid";
		private static final String I18N_INVALID = "config.input_invalid";
		private static final String I18N_NEEDS_WORLD_RESTART = "config.needs_world_restart";

		public ModOptionList(ForgeConfigSpec[] configSpecs, Minecraft mc, int width, int height, int top, int bottom,
				int itemHeight) {
			super(mc, width, height, top, bottom, itemHeight);

			for (ForgeConfigSpec spec : configSpecs) {
				UnmodifiableConfig configValues = spec.getValues();
				generateEntries(spec, configValues, "");
			}
		}

		@Override
		public void render(@NotNull PoseStack ms, int mouseX, int mouseY, float partialTicks) {
			super.render(ms, mouseX, mouseY, partialTicks);

			String tooltip;

			for (Entry entry : this.children()) {
				tooltip = entry.getTooltip();

				if (!StringUtil.isNullOrEmpty(tooltip)) {
					List<Component> comment = Arrays.stream(tooltip.split("\n"))
							.map(TextComponent::new).collect(Collectors.toList());
					renderComponentTooltip(ms, comment, mouseX, mouseY);

					break;
				}
			}
		}

		public void tick() {
			for (Entry child : this.children()) {
				child.tick();
			}
		}

		@Override
		public int getRowWidth() {
			return width - LEFT_RIGHT_BORDER * 2;
		}

		@Override
		protected int getScrollbarPosition() {
			return width - LEFT_RIGHT_BORDER;
		}

		@Override
		public boolean mouseClicked(double x, double y, int button) {
			if (super.mouseClicked(x, y, button)) {
				GuiEventListener focusedChild = getFocused();

				for (Entry child : this.children()) {
					if (child != focusedChild)
						child.clearFocus();
				}

				return true;
			}

			return false;
		}

		public void commitChanges() {
			for (Entry entry : this.children()) {
				entry.commitChanges();
			}
		}

		private void generateEntries(UnmodifiableConfig spec, UnmodifiableConfig values, String path) {
			String currentPath;

			for (UnmodifiableConfig.Entry entry : spec.entrySet()) {
				currentPath = (path.length() > 0) ? path + "." + entry.getKey() : entry.getKey();

				if (entry.getValue() instanceof com.electronwill.nightconfig.core.Config) {
					String i18nKey = "config." + entry.getKey();
					String categoryLabel = (I18n.exists(i18nKey)) ? I18n.get(i18nKey) : entry.getKey();

					addEntry(new CategoryEntry(categoryLabel));
					generateEntries(spec.get(entry.getKey()), values, currentPath);
				} else if (entry.getValue() instanceof ForgeConfigSpec.ValueSpec) {
					ForgeConfigSpec.ConfigValue<?> value = values.get(currentPath);
					ForgeConfigSpec.ValueSpec valueSpec = entry.getValue();

					addEntry(new OptionEntry(valueSpec, value));
				}
			}
		}

		@OnlyIn(Dist.CLIENT)
		public abstract class Entry extends ContainerObjectSelectionList.Entry<ConfigGui.ModOptionList.Entry> {
			public abstract void clearFocus();

			public abstract void commitChanges();

			public abstract void tick();

			public abstract String getTooltip();
		}

		@OnlyIn(Dist.CLIENT)
		public class CategoryEntry extends Entry {
			private final String text;
			private final int width;

			public CategoryEntry(String text) {
				this.text = text;
				this.width = minecraft.font.width(text);
			}

			@Override
			public void render(@NotNull PoseStack ms, int index, int top, int left, int width, int height, int mouseX,
							   int mouseY, boolean isHot, float partialTicks) {
				if(minecraft.screen == null) return;
				minecraft.font.drawShadow(ms, this.text, minecraft.screen.width / 2f - this.width / 2f,
						top + height - 9 - 1, 16777215);
			}

			@Override
			public @NotNull List<? extends GuiEventListener> children() {
				return Collections.emptyList();
			}

			@Override
			public boolean changeFocus(boolean forward) {
				return false;
			}

			@Override
			public void clearFocus() {
			}

			@Override
			public void commitChanges() {
			}

			@Override
			public void tick() {
			}

			@Override
			public String getTooltip() {
				return null;
			}

			@Override
			public @NotNull List<? extends NarratableEntry> narratables() {
				return new ArrayList<>();
			}
		}

		@OnlyIn(Dist.CLIENT)
		public class OptionEntry extends Entry {
			private final ForgeConfigSpec.ValueSpec valueSpec;
			private final ForgeConfigSpec.ConfigValue<?> configValue;
			private EditBox editBox;
			private CheckboxButtonEx checkBox;
			private EnumOptionButton<?> enumButton;
			private final ImageButton needsWorldRestartButton;
			private final ValidationStatusButton validatedButton;
			private final List<GuiEventListener> children;
			private String tooltipText;

			public OptionEntry(ForgeConfigSpec.ValueSpec valueSpec, ForgeConfigSpec.ConfigValue<?> configValue) {
				this.valueSpec = valueSpec;
				this.configValue = configValue;

				this.validatedButton = new ValidationStatusButton(0, 0, button -> {
					if (this.editBox != null) {
						this.editBox.insertText(this.valueSpec.getDefault().toString());
						this.editBox.setFocus(false);
					} else if (this.checkBox != null) {
						this.checkBox.value = (boolean) this.valueSpec.getDefault();
					} else if (this.enumButton != null) {
						this.enumButton.setValue(this.valueSpec.getDefault());
					}
				});

				this.needsWorldRestartButton = new ImageButton(0, 0, 15, 12, 182, 24, 0, Button.WIDGETS_LOCATION, 256,
						256, (b) -> {
				});
				this.needsWorldRestartButton.active = false;
				this.needsWorldRestartButton.visible = valueSpec.needsWorldRestart();

				Object value = configValue.get();

				if (value instanceof Boolean) {
					this.checkBox = new CheckboxButtonEx(0, 0, 20, 20, new TextComponent(""), (boolean) value);

					this.children = ImmutableList.of(this.validatedButton, this.needsWorldRestartButton, this.checkBox);
				} else if (value instanceof Enum) {
					if(value.getClass().isInstance(Enum.class)) {
					this.enumButton = EnumOptionButton.create(value, value.toString(), 0, 0, 100,
							itemHeight - PADDING);}

					this.children = ImmutableList.of(this.validatedButton, this.needsWorldRestartButton,
							this.enumButton);
				} else {
					this.editBox = new EditBox(minecraft.font, 0, 0, 100, itemHeight - PADDING,
							new TextComponent(""));
					this.editBox.setTextColor(16777215);
					this.editBox.insertText(value.toString());
					this.editBox.setMaxLength(256);
					this.editBox.setCanLoseFocus(true);
					this.editBox.setFilter(this::validateTextFieldInput);

					this.children = ImmutableList.of(this.validatedButton, this.needsWorldRestartButton, this.editBox);
				}

				this.tooltipText = null;
			}

			@Override
			public void render(@NotNull PoseStack ms, int index, int top, int left, int width, int height, int mouseX,
							   int mouseY, boolean isHot, float partialTicks) {
				this.validatedButton.x = getScrollbarPosition() - this.validatedButton.getWidth()
						- this.needsWorldRestartButton.getWidth() - 2 * PADDING;
				this.validatedButton.y = top + ((itemHeight - this.validatedButton.getHeight()) / 2) - 1;
				this.validatedButton.render(ms, mouseX, mouseY, partialTicks);

				this.needsWorldRestartButton.x = getScrollbarPosition() - this.needsWorldRestartButton.getWidth()
						- PADDING;
				this.needsWorldRestartButton.y = top + ((itemHeight - this.needsWorldRestartButton.getHeight()) / 2)
						- 1;
				this.needsWorldRestartButton.render(ms, mouseX, mouseY, partialTicks);

				if (this.editBox != null) {
					this.editBox.x = left + (width / 2) + PADDING;
					this.editBox.y = top;
					this.editBox.setWidth((width / 2) - this.validatedButton.getWidth()
							- this.needsWorldRestartButton.getWidth() - 4 * PADDING - 6);
					this.editBox.render(ms, mouseX, mouseY, partialTicks);
				} else if (this.checkBox != null) {
					this.checkBox.x = left + (width / 2) + PADDING;
					this.checkBox.y = top;
					this.checkBox.render(ms, mouseX, mouseY, partialTicks);
				} else if (this.enumButton != null) {
					this.enumButton.x = left + (width / 2) + PADDING;
					this.enumButton.y = top;
					this.enumButton.setWidth((width / 2) - this.validatedButton.getWidth()
							- this.needsWorldRestartButton.getWidth() - 4 * PADDING - 6);
					this.enumButton.render(ms, mouseX, mouseY, partialTicks);
				}

				// Getting translations during rendering is not exactly a smart thing to do, but
				// it's just the config UI so .. meh.
				String description = I18n.get(valueSpec.getTranslationKey());
				int descriptionWidth = minecraft.font.width(description);
				int descriptionLeft = left + (width / 2) - descriptionWidth - PADDING;
				int descriptionTop = top + (itemHeight / 2) - PADDING - minecraft.font.lineHeight / 2 + 2;
				minecraft.font.drawShadow(ms, description, descriptionLeft, descriptionTop, 16777215);

				// Set tooltip to be rendered by the ModOptionList. This could be moved to
				// mouseMoved(), but either
				// the tooltip for the description text would have to stay here or its bounds
				// would have to be stored.
				// To not complicate things, keep everything here for now.
				if ((mouseX >= descriptionLeft) && (mouseX < (descriptionLeft + descriptionWidth))
						&& (mouseY >= descriptionTop) && (mouseY < (descriptionTop + minecraft.font.lineHeight))) {
					// Tooltip for the description
					String i18nTooltipKey = this.valueSpec.getTranslationKey() + I18N_TOOLTIP_SUFFIX;
					this.tooltipText = (I18n.exists(i18nTooltipKey)) ? I18n.get(i18nTooltipKey)
							: this.valueSpec.getComment();
				} else if ((mouseX >= this.validatedButton.x)
						&& (mouseX < (this.validatedButton.x + this.validatedButton.getWidth()))
						&& (mouseY >= this.validatedButton.y)
						&& (mouseY < (this.validatedButton.y + this.validatedButton.getHeight()))) {
					// Tooltip for the validation button.
					this.tooltipText = (this.validatedButton.isValid()) ? I18n.get(I18N_VALID)
							: I18n.get(I18N_INVALID);
				} else if (valueSpec.needsWorldRestart() && (mouseX >= this.needsWorldRestartButton.x)
						&& (mouseX < (this.needsWorldRestartButton.x + this.needsWorldRestartButton.getWidth()))
						&& (mouseY >= this.needsWorldRestartButton.y)
						&& (mouseY < (this.needsWorldRestartButton.y + this.needsWorldRestartButton.getHeight()))) {
					// Tooltip for the needs world restart button.
					this.tooltipText = I18n.get(I18N_NEEDS_WORLD_RESTART);
				} else {
					this.tooltipText = null;
				}
			}

			@Override
			public @NotNull List<? extends GuiEventListener> children() {
				return this.children;
			}

			@Override
			public void clearFocus() {
				if (this.editBox != null) {
					this.editBox.setFocus(false);
				}
			}

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void commitChanges() {
				Object value = this.configValue.get();

				if (value instanceof Boolean) {
					ForgeConfigSpec.BooleanValue cfg = (ForgeConfigSpec.BooleanValue) this.configValue;
					cfg.set(this.checkBox.value);
				} else if (value instanceof Enum) {
					ForgeConfigSpec.EnumValue cfg = (ForgeConfigSpec.EnumValue) this.configValue;
					cfg.set(this.enumButton.getValue());
				} else {
					String text = this.editBox.getValue();

					if (value instanceof Integer) {
						try {
							int parsedValue = Integer.parseInt(text);

							if (this.valueSpec.test(parsedValue)) {
								ForgeConfigSpec.IntValue cfg = (ForgeConfigSpec.IntValue) this.configValue;
								cfg.set(parsedValue);
							}
						} catch (NumberFormatException ignored) {
						}
					} else if (value instanceof Long) {
						try {
							long parsedValue = Long.parseLong(text);

							if (this.valueSpec.test(parsedValue)) {
								ForgeConfigSpec.LongValue cfg = (ForgeConfigSpec.LongValue) this.configValue;
								cfg.set(parsedValue);
							}
						} catch (NumberFormatException ignored) {
						}
					} else if (value instanceof Double) {
						try {
							double parsedValue = Double.parseDouble(text);

							if (this.valueSpec.test(parsedValue)) {
								ForgeConfigSpec.DoubleValue cfg = (ForgeConfigSpec.DoubleValue) this.configValue;
								cfg.set(parsedValue);
							}
						} catch (NumberFormatException ignored) {
						}
					} else if (value instanceof String) {
						if (this.valueSpec.test(text)) {
							ForgeConfigSpec.ConfigValue<String> cfg = (ForgeConfigSpec.ConfigValue<String>) this.configValue;
							cfg.set(text);
						}
					}
				}
			}

			@Override
			public void tick() {
				if (this.editBox != null) {
					this.editBox.tick();
				}
			}

			@Override
			public String getTooltip() {
				return this.tooltipText;
			}

			// Sets the state of the ValidationStatusButton button based on the input in the
			// TextFieldWidget.
			private boolean validateTextFieldInput(String text) {
				Object value = this.configValue.get();

				if (value instanceof Integer) {
					try {
						int parsedValue = Integer.parseInt(text);
						this.validatedButton.setValid(this.valueSpec.test(parsedValue));
					} catch (NumberFormatException ex) {
						this.validatedButton.setInvalid();
					}
				} else if (value instanceof Long) {
					try {
						long parsedValue = Long.parseLong(text);
						this.validatedButton.setValid(this.valueSpec.test(parsedValue));
					} catch (NumberFormatException ex) {
						this.validatedButton.setInvalid();
					}
				} else if (value instanceof Double) {
					try {
						double parsedValue = Double.parseDouble(text);
						this.validatedButton.setValid(this.valueSpec.test(parsedValue));
					} catch (NumberFormatException ex) {
						this.validatedButton.setInvalid();
					}
				} else if (value instanceof String) {
					this.validatedButton.setValid(this.valueSpec.test(text));
				}

				return true;
			}

			@Override
			public @NotNull List<? extends NarratableEntry> narratables() {
				return new ArrayList<>();
			}
		}
	}
}
