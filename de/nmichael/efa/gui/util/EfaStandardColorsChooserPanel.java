package de.nmichael.efa.gui.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.colorchooser.AbstractColorChooserPanel;

import de.nmichael.efa.util.International;

// =====================================================================
// efa-Standardfarben tab
// =====================================================================

/**
 * The color chooser tab "efa-Standardfarben". It renders the color groups
 * delivered by its {@link IColorGroupProvider}s. Each group shows its standard
 * colors on the left and the colors in use on the right, as swatches only.
 * The panel does not know where the colors come from.
 */
public class EfaStandardColorsChooserPanel extends AbstractColorChooserPanel {

	private static final long serialVersionUID = 1L;

	/** Number of swatches per row inside one column of a group. */
	private static final int SWATCHES_PER_ROW = 7;

	/**
	 * Height of the visible area; the panel scrolls vertically if its content is
	 * taller.
	 */
	private static final int VISIBLE_HEIGHT = 360;

	/** Vertical gap between the "Standard" and "Verwendet" line of the same group. */
	private static final int ROW_GAP_WITHIN_GROUP = 8;

	/** Vertical gap after the last line of a group, before the next group starts. */
	private static final int GROUP_GAP = 16;

	private final RecentColorsModel recentColors;

	/** Providers of all color groups, in display order. */
	private final List<IColorGroupProvider> groupProviders;

	/**
	 * Color groups, computed once per panel instance, as building them reads the
	 * efa configuration and is too expensive to repeat every time Swing
	 * (re)installs the tab.
	 */
	private List<ColorGroup> colorGroups = null;

	/**
	 * Creates the efa-Standardfarben panel.
	 *
	 * @param recentColors   the shared "recently used colors" model
	 * @param groupProviders the providers of the color groups, in display order
	 */
	public EfaStandardColorsChooserPanel(RecentColorsModel recentColors, List<IColorGroupProvider> groupProviders) {
		this.recentColors = Objects.requireNonNull(recentColors, "recentColors");
		this.groupProviders = Objects.requireNonNull(groupProviders, "groupProviders");
	}

	/**
	 * @return the cached color groups, building them on first access
	 */
	private List<ColorGroup> getColorGroups() {
		if (colorGroups == null) {
			List<ColorGroup> groups = new ArrayList<>();
			for (IColorGroupProvider provider : groupProviders) {
				if (provider == null) {
					continue;
				}
				ColorGroup group = provider.getColorGroup();
				if (group != null) {
					groups.add(group);
				}
			}
			colorGroups = groups;
		}
		return colorGroups;
	}

	/** {@inheritDoc} */
	@Override
	public String getDisplayName() {
		return International.getString("efa-Standardfarben");
	}

	/** {@inheritDoc} */
	@Override
	public Icon getSmallDisplayIcon() {
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public Icon getLargeDisplayIcon() {
		return null;
	}

	/**
	 * Builds one titled group per color area, stacked vertically inside a scroll
	 * pane.
	 */
	@Override
	protected void buildChooser() {
		setLayout(new BorderLayout());

		JPanel content = new JPanel(new GridBagLayout());
		// 4 px space above the first and below the last group, inside the scroll box
		content.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
		int row = 0;
		List<ColorGroup> groups = getColorGroups();
		for (int i = 0; i < groups.size(); i++) {
			row = addGroup(content, row, groups.get(i));
			if (i < groups.size() - 1) {
				row = addSeparator(content, row);
			}
		}

		// filler, so that the groups stay at their natural height
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		content.add(new JPanel(), gbc);

		JScrollPane scrollPane = new JScrollPane(content, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		// Limit the preferred height of the viewport. Without this, the scroll pane
		// reports the full height of its content as preferred size, the surrounding
		// dialog grows to that size and no scrollbar is ever shown.
		scrollPane.getViewport().setPreferredSize(new Dimension(content.getPreferredSize().width, VISIBLE_HEIGHT));
		add(scrollPane, BorderLayout.CENTER);
	}

	/**
	 * Adds one titled group with three columns: the group title, the row heading
	 * ("Standard"/"Verwendet") and the swatches. All components are added
	 * directly to the shared {@link GridBagLayout} of {@code content}, so that
	 * the columns line up across all groups.
	 *
	 * @param content the container the group is added to (GridBagLayout)
	 * @param row     the first grid row for the group
	 * @param group   the group to add
	 * @return the next free grid row
	 */
	private int addGroup(JPanel content, int row, ColorGroup group) {
		boolean hasInUse = !group.inUse.isEmpty();
		int rows = hasInUse ? 2 : 1;

		GridBagConstraints titleGbc = new GridBagConstraints();
		titleGbc.gridx = 0;
		titleGbc.gridy = row;
		titleGbc.anchor = GridBagConstraints.EAST;
		titleGbc.insets = new Insets(0, 8, hasInUse ? ROW_GAP_WITHIN_GROUP : GROUP_GAP, 12);

		// Column 1: row heading (right before the swatches), column 2: swatches.
		// All groups share the same grid, so the labels and swatches line up.
		GridBagConstraints labelGbc = new GridBagConstraints();
		labelGbc.gridx = 1;
		labelGbc.gridy = row;
		labelGbc.anchor = GridBagConstraints.WEST;
		labelGbc.insets = new Insets(0, 8, hasInUse ? ROW_GAP_WITHIN_GROUP : GROUP_GAP, 12);

		GridBagConstraints swatchGbc = new GridBagConstraints();
		swatchGbc.gridx = 2;
		swatchGbc.gridy = row;
		swatchGbc.weightx = 1.0;
		swatchGbc.fill = GridBagConstraints.HORIZONTAL;
		swatchGbc.anchor = GridBagConstraints.NORTHWEST;
		swatchGbc.insets = new Insets(0, 0, hasInUse ? ROW_GAP_WITHIN_GROUP : GROUP_GAP, 8);

		// "Standard" row
		content.add(createColumnHeader(group.title, true), titleGbc);
		//actually, we skip the label as it makes the screen look confusing.
		//content.add(createColumnHeader(International.getString("Standard"), false), labelGbc);
		content.add(createSwatchArea(group.standard), swatchGbc);

		// "Verwendet" row, below the standard row; only if there is at least one extra color
		if (hasInUse) {
			GridBagConstraints labelGbc2 = (GridBagConstraints) labelGbc.clone();
			labelGbc2.gridy = row + 1;
			labelGbc2.insets = new Insets(0, 8, GROUP_GAP, 12);

			GridBagConstraints swatchGbc2 = (GridBagConstraints) swatchGbc.clone();
			swatchGbc2.gridy = row + 1;
			swatchGbc2.insets = new Insets(0, 0, GROUP_GAP, 8);
			//actually, we skip the label as it makes the screen look confusing.
			//content.add(createColumnHeader(International.getString("Verwendet"), false), labelGbc2);
			content.add(createSwatchArea(group.inUse), swatchGbc2);
		}

		return row + rows;
	}

	/**
	 * Adds a horizontal separator line spanning the full width of all three
	 * columns, used to visually divide one group from the next.
	 *
	 * @param content the container the separator is added to (GridBagLayout)
	 * @param row     the grid row for the separator
	 * @return the next free grid row
	 */
	private int addSeparator(JPanel content, int row) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 3;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, GROUP_GAP, 0);
		content.add(new JSeparator(), gbc);
		return row + 1;
	}

	/**
	 * @param text the column heading
	 * @param bold whether the heading is bold
	 * @return a label used as heading of a column
	 */
	private static JLabel createColumnHeader(String text, boolean bold) {
		JLabel label = new JLabel(text);
		if (bold) {
			label.setFont(label.getFont().deriveFont(Font.BOLD));
		}
		return label;
	}

	/**
	 * Creates the swatch area of one column.
	 *
	 * @param colors the colors to show as swatches
	 * @return the swatch area
	 */
	private JComponent createSwatchArea(List<Color> colors) {
		JPanel area = new JPanel(new GridLayout(0, SWATCHES_PER_ROW, 6, 6));
		for (Color c : colors) {
			ColorSwatch sw = new ColorSwatch(c);
			sw.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					onSwatchClicked(c);
				}
			});
			area.add(sw);
		}
		return area;
	}

	/**
	 * Handles a swatch click: updates the selected color of the chooser and records
	 * the color in the recent colors history.
	 *
	 * @param c the color that was clicked
	 */
	private void onSwatchClicked(Color c) {
		getColorSelectionModel().setSelectedColor(c);
		recentColors.addColor(c);
	}

	/** {@inheritDoc} */
	@Override
	public void updateChooser() {
		// The swatches show fixed standard colors; nothing to synchronize.
	}
}
