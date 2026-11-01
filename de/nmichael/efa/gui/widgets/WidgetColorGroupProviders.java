package de.nmichael.efa.gui.widgets;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.EfaConfig;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.gui.util.ColorGroup;
import de.nmichael.efa.gui.util.IColorGroupProvider;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

/**
 * Color group providers of the configurable widgets for the efa-Standardfarben
 * chooser tab. The widgets are only created when their colors are requested.
 */
public final class WidgetColorGroupProviders {

	private WidgetColorGroupProviders() {
	}

	/**
	 * @return one color group provider per widget, in display order
	 */
	public static List<IColorGroupProvider> create() {
		List<IColorGroupProvider> providers = new ArrayList<>();
		providers.add(group(International.getString("Wetter-Widget"),
				Arrays.asList(EfaConfig.standardToolTipBackgroundColor, EfaConfig.standardToolTipForegroundColor,
						EfaConfig.standardToolTipHeaderBackgroundColor, EfaConfig.standardToolTipHeaderForegroundColor,
						EfaConfig.standardErrorBackgroundColor, EfaConfig.standardErrorForegroundColor,
						EfaConfig.standardErrorHeaderBackgroundColor, EfaConfig.standardErrorHeaderForegroundColor),
				WeatherWidget::new, WeatherWidget::getColorsInUse));
		providers.add(group(International.getString("HTML-Widget"),
				Arrays.asList(EfaConfig.standardToolTipBackgroundColor, EfaConfig.standardToolTipForegroundColor,
						EfaConfig.standardToolTipHeaderBackgroundColor, EfaConfig.standardToolTipHeaderForegroundColor),
				HTMLWidget::new, HTMLWidget::getColorsInUse));
		return providers;
	}

	/**
	 * Creates a provider for one widget type.
	 *
	 * @param title          the group title
	 * @param standard       the standard colors of the group
	 * @param widgetSupplier creates a new widget instance on demand
	 * @param colorsInUse    reads the colors in use from a widget instance
	 */
	private static <W extends Widget> IColorGroupProvider group(String title, List<Color> standard,
			Supplier<W> widgetSupplier, Function<W, Collection<Color>> colorsInUse) {
		return () -> new ColorGroup(title, standard, colorsInUse(widgetSupplier, colorsInUse));
	}

	/**
	 * Returns the colors in use of a widget. The widget is loaded with its
	 * configured parameters; if it is disabled or no efa configuration is
	 * available, the result is empty.
	 */
	private static <W extends Widget> Collection<Color> colorsInUse(Supplier<W> widgetSupplier,
			Function<W, Collection<Color>> colorsInUse) {
		EfaConfig cfg = Daten.efaConfig;
		if (cfg == null) {
			return Collections.emptyList();
		}
		try {
			W widget = widgetSupplier.get();
			// load the configured values of all widget parameters
			for (IItemType param : widget.getParameters()) {
				IItemType configured = cfg.getExternalGuiItem(param.getName());
				if (configured != null) {
					param.parseValue(configured.toString());
				}
			}
			if (!widget.isEnabled()) {
				return Collections.emptyList();
			}
			return colorsInUse.apply(widget);
		} catch (Exception e) {
			Logger.logdebug(e);
			return Collections.emptyList();
		}
	}
}
