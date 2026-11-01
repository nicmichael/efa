package de.nmichael.efa.gui.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.EfaConfig;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeConfigButton;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;

/**
 * Color groups of the efa-Standardfarben tab that are based on the efa
 * configuration (everything except the widgets). The colors in use are read
 * from {@link Daten#efaConfig} each time a group is requested.
 */
public final class StandardColorGroups {

	/** Names of the boathouse buttons whose background color is configurable. */
	private static final String[] BUTTON_NAMES = { "ButtonStartSession", "ButtonFinishSession", "ButtonCancelSession",
			"ButtonLateEntry", "ButtonBoatReservations", "ButtonShowLogbook", "ButtonCreateStatistics",
			"ButtonClubwork", "ButtonMessageToAdmin", "ButtonAdminMode", "ButtonSpecial" };

	/** Default background colors of the boathouse buttons (hex values without "#"). */
	private static final String[] BUTTON_DEFAULT_HEX = { "CCFFCC", "FFCCCC", "CCFFFF", "FFFFCC", "CCCCFF", "FFF197",
			"CCCCCC" };

	private StandardColorGroups() {
	}

	/**
	 * Creates all color groups in display order. The widget groups are inserted
	 * before the newsticker group.
	 *
	 * @param widgetGroups the color groups of the widgets (may be empty)
	 * @return the providers of all groups in display order
	 */
	public static List<IColorGroupProvider> create(List<IColorGroupProvider> widgetGroups) {
		List<IColorGroupProvider> groups = new ArrayList<>();

		groups.add(group(International.getString("Erscheinungsbild"),
				Arrays.asList(EfaConfig.standardFlatLafBackgroundColor, EfaConfig.standardFlatLafAccentColor,
						EfaConfig.standardFlatLafFocusColor),
				cfg -> Arrays.asList(cfg.getEfaGuiflatLaf_Background(), cfg.getEfaGuiflatLaf_AccentColor(),
						cfg.getEfaGuiflatLaf_FocusColor())));

		groups.add(group(International.getString("Überschriften"),
				Arrays.asList(EfaConfig.standardHeaderBackgroundColor, EfaConfig.standardHeaderForegroundColor),
				cfg -> Arrays.asList(cfg.getHeaderBackgroundColor(), cfg.getHeaderForegroundColor())));

		groups.add(group(International.getString("Tabellen"),
				Arrays.asList(EfaConfig.standardTableHeaderBackgroundColor, EfaConfig.standardTableHeaderForegroundColor,
						EfaConfig.standardTableAlternatingRowColor, EfaConfig.standardTableSelectionBackgroundColor,
						EfaConfig.standardTableSelectionForegroundColor),
				cfg -> Arrays.asList(cfg.getTableHeaderBackgroundColor(), cfg.getTableHeaderHeaderColor(),
						cfg.getTableAlternatingRowColor(), cfg.getTableSelectionBackgroundColor(),
						cfg.getTableSelectionForegroundColor())));

		groups.add(group(International.getString("Tooltip"),
				Arrays.asList(EfaConfig.standardToolTipBackgroundColor, EfaConfig.standardToolTipForegroundColor,
						EfaConfig.standardToolTipHeaderBackgroundColor, EfaConfig.standardToolTipHeaderForegroundColor),
				cfg -> Arrays.asList(cfg.getToolTipBackgroundColor(), cfg.getToolTipForegroundColor(),
						cfg.getToolTipHeaderBackgroundColor(), cfg.getToolTipHeaderForegroundColor())));

		groups.add(new IColorGroupProvider() {
			@Override
			public ColorGroup getColorGroup() {
				return new ColorGroup(International.getString("Button"), buttonDefaultColors(), buttonColorsInUse());
			}
		});

		if (widgetGroups != null) {
			groups.addAll(widgetGroups);
		}

		groups.add(group(International.getString("Newsticker"),
				Arrays.asList(EfaConfig.standardHeaderBackgroundColor, EfaConfig.standardHeaderForegroundColor),
				cfg -> Arrays.asList(cfg.getValueEfaDirekt_newsBackgroundColor(),
						cfg.getValueEfaDirekt_newsForegroundColor())));

		return groups;
	}

	/**
	 * Creates a provider whose colors in use are read from the efa configuration.
	 * If no configuration is available, the colors in use stay empty.
	 */
	private static IColorGroupProvider group(String title, List<Color> standard,
			Function<EfaConfig, List<Color>> colorsInUse) {
		return () -> {
			EfaConfig cfg = Daten.efaConfig;
			return new ColorGroup(title, standard, cfg == null ? null : colorsInUse.apply(cfg));
		};
	}

	private static List<Color> buttonDefaultColors() {
		List<Color> colors = new ArrayList<>();
		for (String hex : BUTTON_DEFAULT_HEX) {
			colors.add(new Color(Integer.parseInt(hex, 16)));
		}
		return colors;
	}

	private static List<Color> buttonColorsInUse() {
		List<Color> colors = new ArrayList<>();
		EfaConfig cfg = Daten.efaConfig;
		if (cfg == null) {
			return colors;
		}
		for (String name : BUTTON_NAMES) {
			IItemType item = cfg.getExternalGuiItem(name);
			if (item instanceof ItemTypeConfigButton) {
				Color c = EfaUtil.getColor(((ItemTypeConfigButton) item).getValueColor());
				if (c != null) {
					colors.add(c);
				}
			}
		}
		return colors;
	}
}
