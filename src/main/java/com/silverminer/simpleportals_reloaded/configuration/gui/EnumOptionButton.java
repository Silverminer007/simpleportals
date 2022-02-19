package com.silverminer.simpleportals_reloaded.configuration.gui;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class EnumOptionButton<E extends Enum<E>> extends ExtendedButton {
	private final Class<E> clazz;
	private int selectedIndex;
	private final String[] names;
	private final String[] i18nNames;

	private static final String I18N_ENUM_PREFIX = "config.enums.";

	/**
	 * The button message is automatically localized. Localization keys are of the
	 * following format: "config.enums.enum_name.enum_value" e.g.
	 * "config.enums.colors.red". Note that both enum_name and enum_value need to be
	 * lower case.
	 */
	public EnumOptionButton(Class<E> clazz, String value, int x, int y, int width, int height) {
		super(x, y, width, height,
				new TextComponent(
						I18n.get(I18N_ENUM_PREFIX + clazz.getSimpleName().toLowerCase() + "." + value.toLowerCase())),
				(button) -> {
				});

		this.clazz = clazz;
		this.selectedIndex = 0;

		int i = 0;

		E[] constants = clazz.getEnumConstants();
		this.names = new String[constants.length];
		this.i18nNames = new String[constants.length];

		for (E e : constants) {
			names[i] = e.name();
			i18nNames[i] = I18n
					.get(I18N_ENUM_PREFIX + clazz.getSimpleName().toLowerCase() + "." + e.name().toLowerCase());
			if (e.name().equals(value))
				selectedIndex = i;
			i++;
		}
	}

	@SuppressWarnings("unchecked")
	public static <E extends Enum<E>> EnumOptionButton<E> create(Object o, String value, int x, int y, int width,
			int height) {
		return new EnumOptionButton<>((Class<E>) o.getClass(), value, x, y, width, height);
	}

	@Override
	public void onPress() {
		super.onPress();
		nextValue();
	}

	public E getValue() {
		return Enum.valueOf(this.clazz, names[this.selectedIndex]);
	}

	public void setValue(Object value) {
		if (!(value instanceof Enum<?> e)) {
			return;
		}
		for (int i = 0; i < names.length; i++) {
			if (Objects.equals(names[i], e.name())) {
				this.selectedIndex = i;
				this.setMessage(new TextComponent(this.i18nNames[i]));
				break;
			}
		}
	}

	private void nextValue() {
		this.selectedIndex = (this.selectedIndex + 1) % this.names.length;
		this.setMessage(new TextComponent(this.i18nNames[this.selectedIndex]));
	}
}
