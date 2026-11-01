package de.nmichael.efa.gui.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.nmichael.efa.gui.EfaGuiUtils;
import de.nmichael.efa.util.International;

/**
 * The single color chooser tab that renders a {@link PaletteData} palette. The
 * active palette is selectable at runtime via a drop-down box placed directly
 * above the swatch grid; the default selection is "Fresh Bright Pastels
 * (Light)".
 * <p>
 * Layout (via {@link GridBagLayout}):
 * <ul>
 * <li>Top-left: the palette selector drop-down.</li>
 * <li>Left side, below the drop-down: the palette swatches, arranged in an
 * anti-aliased grid and painted on a rounded-corner background panel.</li>
 * <li>Right side: exactly one saturation control (a slider paired with a
 * synchronized numeric spinner - together forming a single logical control), a
 * read-only hex code field, and the shared "recently used" history.</li>
 * </ul>
 */
public class EfaPastelPaletteChooserPanel extends AbstractColorChooserPanel {

	private static final long serialVersionUID = 1L;

	/** Corner radius used for the rounded palette background. */
	private static final int CORNER_ARC = 12;

	/**
	 * Maximum number of swatch rows visible without scrolling; beyond this, a
	 * vertical scrollbar appears.
	 */
	private static final int VISIBLE_ROWS = 6;

	/**
	 * Vertical gap between swatch rows in the grid, matching the {@link GridLayout}
	 * vgap used in {@link #buildSwatchGrid()}.
	 */
	private static final int SWATCH_ROW_GAP = 4;

	/** Top+bottom empty border applied around the swatch grid. */
	private static final int SWATCH_GRID_BORDER = 8;

	/** Maximum number of "recently used" swatches per row; further swatches wrap to a new row. */
	private static final int RECENT_COLORS_PER_ROW = 7;

	private final RecentColorsModel recentColors;

	/** Listener instance kept as field so that the same object can be removed again. */
	private final Runnable recentListener = this::refreshRecentPanel;


	/**
	 * All available palettes; built once so the combo box model and the initial
	 * selection share the same instances.
	 */
	private PaletteData[] palettes = PaletteData.values();

	/**
	 * The palette currently displayed; changes when the drop-down selection
	 * changes.
	 */
	private PaletteData data;

	private JComboBox<PaletteData> paletteSelector;
	private RoundedBackgroundPanel swatchGrid;
	private RoundedBackgroundPanel recentPanel;
	private JSlider saturationSlider;
	private JSpinner saturationSpinner;
	private JTextField hexField;
	private ColorSwatch[][] swatches;

	/** Guard flag to prevent feedback loops between the slider and the spinner. */
	private boolean adjustingSaturationControls = false;

	/**
	 * Creates a new palette panel, initially showing the default palette ("Fresh
	 * Bright Pastels (Light)").
	 *
	 * @param recentColors the shared "recently used colors" model
	 */
	EfaPastelPaletteChooserPanel(RecentColorsModel recentColors) {
		this.recentColors = recentColors;
		this.data = palettes[0];
	}

	/**
	 * Registers the recent-colors listener while the panel is installed in a
	 * chooser. The model is shared, so listeners must be removed again on
	 * uninstall, otherwise panels of closed dialogs would be kept alive and
	 * refreshed on every change.
	 */
	@Override
	public void installChooserPanel(JColorChooser enclosingChooser) {
		super.installChooserPanel(enclosingChooser);
		recentColors.addListener(recentListener);
	}

	/** Removes the recent-colors listener when the panel is uninstalled. */
	@Override
	public void uninstallChooserPanel(JColorChooser enclosingChooser) {
		recentColors.removeListener(recentListener);
		super.uninstallChooserPanel(enclosingChooser);
	}

	/** {@inheritDoc} */
	@Override
	public String getDisplayName() {
		return International.getString("Pastell-Paletten");
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
	 * Builds the panel UI: the palette selector and swatch grid on the left, and
	 * the control column (saturation, hex field, recent colors) on the right, using
	 * a {@link GridBagLayout}.
	 */
	@Override
	protected void buildChooser() {
		setLayout(new GridBagLayout());

		JComboBox<PaletteData> selector = buildPaletteSelector();
		JScrollPane scrollSwatchPane = buildSwatchGrid();
		JPanel controlsPanel = buildControlsPanel();

		GridBagConstraints gbc = new GridBagConstraints();

		// Top-left: palette selector, directly above the swatch grid.
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.6;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(8, 8, 4, 4);
		add(selector, gbc);

		// Left side, below the selector: palette swatches, takes most of the space.
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(4, 8, 8, 4);
		add(scrollSwatchPane, gbc);

		// Right column: saturation control, hex field, recent colors.
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridheight = 2;
		gbc.weightx = 0.4;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(8, 4, 8, 8);
		add(controlsPanel, gbc);
	}

	/**
	 * Creates the drop-down box used to pick the active palette at runtime. The
	 * default selection is "Fresh Bright Pastels (Light)". Selecting a new entry
	 * triggers {@link #reloadPalette()}.
	 *
	 * @return the palette selector combo box
	 */
	private JComboBox<PaletteData> buildPaletteSelector() {
		paletteSelector = new JComboBox<>(palettes);
		paletteSelector.setSelectedItem(data);
		paletteSelector.addActionListener(e -> {
			data = (PaletteData) paletteSelector.getSelectedItem();
			reloadPalette();
		});
		return paletteSelector;
	}

	/**
	 * Creates the scrollable, rounded-corner grid of color swatches that represents
	 * the currently selected palette on the left side of the panel. Rendering
	 * quality hints (anti-aliasing) are applied by the individual swatches and the
	 * rounded background for the crispest possible display.
	 * <p>
	 * The scroll pane never shows a horizontal scrollbar (the grid always fits its
	 * available width); a vertical scrollbar automatically appears whenever the
	 * available viewport height is smaller than the palette's natural height. The
	 * initial preferred viewport height corresponds to {@link #VISIBLE_ROWS} swatch
	 * rows, but once the surrounding dialog/window is resized larger by the user,
	 * the grid grows to fill the extra space (see {@link ScrollableRoundedPanel}),
	 * shrinking back and showing the scrollbar again if space becomes scarce.
	 *
	 * @return a scroll pane containing the swatch grid
	 */
	private JScrollPane buildSwatchGrid() {
		int rows = data.familyNames.length;
		int cols = data.columnNames.length;

		swatchGrid = new ScrollableRoundedPanel(new GridLayout(rows, cols, 4, SWATCH_ROW_GAP), CORNER_ARC);
		swatchGrid.setBackground(data.panelBackground);
		swatchGrid.setBorder(BorderFactory.createEmptyBorder(SWATCH_GRID_BORDER, SWATCH_GRID_BORDER, SWATCH_GRID_BORDER,
				SWATCH_GRID_BORDER));

		swatches = new ColorSwatch[rows][cols];
		for (int r = 0; r < rows; r++) {
			for (int cIdx = 0; cIdx < cols; cIdx++) {
				ColorSwatch sw = new ColorSwatch(data.colors[r][cIdx]);
				sw.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						onSwatchClicked(sw.getCurrentColor());
					}
				});
				swatches[r][cIdx] = sw;
				swatchGrid.add(sw);
			}
		}

		JScrollPane scrollPane = new JScrollPane(swatchGrid, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setOpaque(false);
		scrollPane.getViewport().setOpaque(false);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		// Set only the *initial* preferred viewport height (VISIBLE_ROWS swatch
		// rows); this merely influences the dialog's initial size. Actual growth
		// or shrinking when the user resizes the surrounding window is handled
		// by ScrollableRoundedPanel's Scrollable implementation, which stretches
		// the grid to fill any extra viewport space (or reverts to its natural
		// size - triggering the scrollbar - when space is scarce).
		int swatchHeight = swatches[0][0].getPreferredSize().height;
		int visibleHeight = VISIBLE_ROWS * swatchHeight + (VISIBLE_ROWS - 1) * SWATCH_ROW_GAP + 2 * SWATCH_GRID_BORDER;
		Dimension gridPreferred = swatchGrid.getPreferredSize();
		scrollPane.getViewport().setPreferredSize(new Dimension(gridPreferred.width, visibleHeight));

		return scrollPane;
	}

	/**
	 * Rebuilds the swatch colors, grid background, and recent-colors background
	 * after the user selects a different palette from the drop-down box, and resets
	 * the single saturation control back to its neutral (0%) position.
	 */
	private void reloadPalette() {
		swatchGrid.setBackground(data.panelBackground);
		for (int r = 0; r < data.familyNames.length; r++) {
			for (int cIdx = 0; cIdx < data.columnNames.length; cIdx++) {
				swatches[r][cIdx].setColorSilently(data.colors[r][cIdx]);
			}
		}
		if (recentPanel != null) {
			recentPanel.setBackground(data.panelBackground);
		}

		// Reset the one-and-only saturation control (slider + spinner) to neutral.
		adjustingSaturationControls = true;
		saturationSlider.setValue(0);
		saturationSpinner.setValue(0);
		adjustingSaturationControls = false;

		swatchGrid.revalidate();
		swatchGrid.repaint();
		if (recentPanel != null) {
			recentPanel.repaint();
		}
	}

	/**
	 * Creates the right-hand control column containing the saturation control, the
	 * read-only hex code field, and the recent colors history.
	 *
	 * @return the assembled controls panel
	 */
	private JPanel buildControlsPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 8, 0);

		gbc.gridy = 0;
		gbc.weighty = 0;
		panel.add(buildSaturationPanel(), gbc);

		gbc.gridy = 1;
		panel.add(buildHexFieldPanel(), gbc);

		gbc.gridy = 2;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		panel.add(buildRecentColorsPanel(), gbc);

		return panel;
	}

	/**
	 * Creates the saturation control group - the single user interface element for
	 * adjusting color saturation. It consists of a slider and a spinner (numeric
	 * field with up/down buttons, like the standard RGB chooser panel) that are
	 * kept perfectly in sync with each other: changing either one updates the other
	 * and immediately re-applies the saturation to every swatch in the grid.
	 *
	 * @return the saturation control panel
	 */
	private JPanel buildSaturationPanel() {
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(EfaGuiUtils.createRoundedTitledBorder(International.getString("Sättigung")));

		saturationSlider = new JSlider(-100, 100, 0);
		saturationSlider.setMajorTickSpacing(50);
		saturationSlider.setPaintTicks(true);
		saturationSlider.setPaintLabels(true);

		saturationSpinner = new JSpinner(new SpinnerNumberModel(0, -100, 100, 1));
		// Keep the spinner reasonably narrow, like a classic up/down numeric field.
		((JSpinner.DefaultEditor) saturationSpinner.getEditor()).getTextField().setColumns(4);

		saturationSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (adjustingSaturationControls) {
					return;
				}
				adjustingSaturationControls = true;
				int value = saturationSlider.getValue();
				saturationSpinner.setValue(value);
				applySaturation(value);
				adjustingSaturationControls = false;
			}
		});

		saturationSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (adjustingSaturationControls) {
					return;
				}
				adjustingSaturationControls = true;
				int value = (Integer) saturationSpinner.getValue();
				saturationSlider.setValue(value);
				applySaturation(value);
				adjustingSaturationControls = false;
			}
		});

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(2, 4, 2, 4);
		p.add(saturationSlider, gbc);

		gbc.gridx = 1;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;
		p.add(saturationSpinner, gbc);

		return p;
	}

	/**
	 * Creates the read-only hex code field that displays the currently selected
	 * swatch color, ready to be copied via copy &amp; paste.
	 *
	 * @return the hex field panel
	 */
	private JPanel buildHexFieldPanel() {
		JPanel p = new JPanel(new BorderLayout(4, 0));
		p.setBorder(EfaGuiUtils.createRoundedTitledBorder(International.getString("Farbcode")));
		hexField = new JTextField();
		hexField.setEditable(false);
		hexField.setFocusable(true);
		hexField.setHorizontalAlignment(JTextField.CENTER);
		p.add(hexField, BorderLayout.CENTER);
		return p;
	}

	/**
	 * Creates the "recently used colors" panel, rendered on the same rounded
	 * background style as the main palette, and registers this panel as a listener
	 * on the shared {@link RecentColorsModel}.
	 *
	 * @return the recent colors panel
	 */
	private JPanel buildRecentColorsPanel() {
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBorder(EfaGuiUtils.createRoundedTitledBorder(International.getString("Zuletzt verwendet")));

		recentPanel = new RoundedBackgroundPanel(null, CORNER_ARC);
		// Stack rows vertically; each row holds up to RECENT_COLORS_PER_ROW swatches.
		recentPanel.setLayout(new BoxLayout(recentPanel, BoxLayout.Y_AXIS));
		recentPanel.setBackground(data.panelBackground);
		refreshRecentPanel();

		JScrollPane scrollPane = new JScrollPane(recentPanel);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setOpaque(false);
		scrollPane.getViewport().setOpaque(false);

		wrapper.add(scrollPane, BorderLayout.CENTER);
		return wrapper;
	}

	/**
	 * Handles a swatch (or recent-color) click: publishes the hex code to the
	 * read-only text field, updates the shared color selection model, and records
	 * the color in the recent colors history.
	 *
	 * @param c the color that was clicked
	 */
	private void onSwatchClicked(Color c) {
		hexField.setText(String.format("#%06X", c.getRGB() & 0xFFFFFF));
		hexField.selectAll();
		getColorSelectionModel().setSelectedColor(c);
		recentColors.addColor(c);
	}

	/**
	 * Re-computes every swatch color based on the palette's base colors and the
	 * given saturation percentage, then repaints the grid.
	 *
	 * @param percent saturation adjustment in the range -100..+100
	 */
	private void applySaturation(int percent) {
		for (int r = 0; r < data.familyNames.length; r++) {
			for (int cIdx = 0; cIdx < data.columnNames.length; cIdx++) {
				Color base = data.colors[r][cIdx];
				Color adjusted = adjustSaturation(base, percent);
				swatches[r][cIdx].setColorSilently(adjusted);
			}
		}
		swatchGrid.repaint();
	}

	/**
	 * Adjusts the saturation (HSB "S" component) of a color by a relative
	 * percentage, without modifying its hue or brightness.
	 * <p>
	 * A positive percentage moves the saturation towards full saturation (1.0), a
	 * negative percentage moves it towards gray (0.0).
	 *
	 * @param base    the original color
	 * @param percent desired adjustment in the range -100..+100
	 * @return the resulting color
	 */
	private Color adjustSaturation(Color base, int percent) {
		float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
		float s = hsb[1];
		if (percent >= 0) {
			s = s + (1f - s) * (percent / 100f);
		} else {
			s = s * (1f + percent / 100f);
		}
		s = Math.max(0f, Math.min(1f, s));
		return Color.getHSBColor(hsb[0], s, hsb[2]);
	}

	/**
	 * Rebuilds the "recently used" swatch row from the shared model. Invoked both
	 * at initialization and whenever the shared model notifies this panel of a
	 * change.
	 */
	private void refreshRecentPanel() {
		if (recentPanel == null) {
			return;
		}
		recentPanel.removeAll();
		JPanel row = null;
		int count = 0;
		for (Color c : recentColors.getColors()) {
			if (count % RECENT_COLORS_PER_ROW == 0) {
				row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
				row.setOpaque(false);
				row.setAlignmentX(Component.LEFT_ALIGNMENT);
				recentPanel.add(row);
			}
			ColorSwatch sw = new ColorSwatch(c);
			sw.setPreferredSize(new Dimension(52, 36));
			sw.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					onSwatchClicked(c);
				}
			});
			row.add(sw);
			// Keep each row at its natural height so rows do not stretch vertically.
			row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
			count++;
		}
		revalidateChooserLayout();
		recentPanel.repaint();
	}

	/**
	 * Schedules a layout and repaint of the whole dialog. A plain
	 * {@code revalidate()} on the recent panel only lays out inside the enclosing
	 * {@link JScrollPane} (a validate root), and nothing repaints the dialog until
	 * the next mouse event. Revalidating and repainting the dialog's
	 * {@link JRootPane} (also a validate root) updates the whole dialog right away.
	 */
	private void revalidateChooserLayout() {
		JRootPane root = SwingUtilities.getRootPane(this);
		if (root != null) {
			root.revalidate();
			root.repaint();
		} else {
			recentPanel.revalidate();
			recentPanel.repaint();
		}
	}

	/** {@inheritDoc} */
	@Override
	public void updateChooser() {
		// No external synchronization required: selection changes made outside
		// this panel do not need to be reflected back into the swatch grid.
	}

	// =====================================================================
	// Palette data
	// =====================================================================

	/**
	 * Immutable description of a single color palette: a title, the names of the
	 * color families (rows), the names of the shade columns, the actual color
	 * matrix, and the background color to be used behind the swatches.
	 */
	static class PaletteData {
		String title;
		String[] familyNames;
		String[] columnNames;
		Color[][] colors; // [family][column]
		Color panelBackground;

		/** Color family names (rows), shared by all palettes. */
		static final String[] FAMILY_NAMES = { "Red", "Coral", "Orange", "Yellow", "Green", "Mint", "Blue", "Azure",
				"Navy", "Violet", "Pink", "Sand", "Brown" };
		/** Shade column names of the light palettes. */
		static final String[] LIGHT_COLUMN_NAMES = { "Very Light", "Light", "Accent", "Primary", "Hover" };
		/** Shade column names of the "Fresh Bright Pastels (Dark)" palette. */
		static final String[] DARK_FRESH_COLUMN_NAMES = { "Deep", "Surface", "Accent", "Primary", "Glow" };
		/** Shade column names of the "Professional Pastels (Dark)" palette. */
		static final String[] DARK_PROFESSIONAL_COLUMN_NAMES = { "Surface Dark", "Light Accent", "Accent", "Primary",
				"Strong" };

		PaletteData(String title, String[] familyNames, String[] columnNames, Color[][] colors, Color panelBackground) {
			this.title = title;
			this.familyNames = familyNames;
			this.columnNames = columnNames;
			this.colors = colors;
			this.panelBackground = panelBackground;
		}

		/** Used by the palette drop-down's default renderer. */
		@Override
		public String toString() {
			return title;
		}

		/**
		 * @return all available palettes, in the order they should appear in the
		 *         drop-down box; index 0 is the default selection.
		 */
		static PaletteData[] values() {
			return new PaletteData[] { freshBrightLight(), professionalLight(), freshBrightDark(), professionalDark() };
		}

		/** Shorthand for {@link Color#decode(String)}. */
		private static Color c(String hex) {
			return Color.decode(hex);
		}

		/**
		 * @return the "Fresh Bright Pastels (Light)" palette definition. Families are
		 *         ordered along the rainbow spectrum (Rot -&gt; Koralle -&gt; Gelb
		 *         -&gt; Gruen -&gt; Mint -&gt; Blau -&gt; Navy -&gt; Violett -&gt;
		 *         Pink), followed by the non-spectral neutrals Sand and Braun.
		 */
		static PaletteData freshBrightLight() {
			String[] families = FAMILY_NAMES;
			String[] cols = LIGHT_COLUMN_NAMES;
			Color[][] colors = { { c("#FFF4F4"), c("#FFE0E0"), c("#FFB3B3"), c("#FF7A7A"), c("#FF5C5C") },
					{ c("#FFF6F4"), c("#FFE7E1"), c("#FFCFC4"), c("#FFB6A5"), c("#FFA08A") },
					{ c("#FFF7F0"), c("#FFE8D4"), c("#FFC98F"), c("#FFA85C"), c("#FF9238") },
					{ c("#FFFDF3"), c("#FFF8D8"), c("#FFF2B8"), c("#FFE98E"), c("#FFE06D") },
					{ c("#F6FFF2"), c("#E8FBDD"), c("#C8F5B0"), c("#9EEA7A"), c("#82E058") },
					{ c("#F2FFF9"), c("#DFFBF0"), c("#BFF5D8"), c("#95EDC0"), c("#78E3B0") },
					{ c("#F4FBFF"), c("#EAF7FF"), c("#AEE2FF"), c("#73CAFF"), c("#5CBEFF") },
					{ c("#F2FCFF"), c("#DBF6FF"), c("#9EE9FF"), c("#58D6FF"), c("#2EC8FF") },
					{ c("#F3F6FF"), c("#E6ECFF"), c("#B0C4FF"), c("#6F8EFF"), c("#5579FF") },
					{ c("#FAF7FF"), c("#EFE8FF"), c("#DDD0FF"), c("#C2AEFF"), c("#AD93FF") },
					{ c("#FFF6FC"), c("#FFE7F7"), c("#FFCDED"), c("#FFA9DD"), c("#FF8CD3") },
					{ c("#FFFDF7"), c("#FDF6E8"), c("#F7E9C9"), c("#EFDBAF"), c("#E7CE9A") },
					{ c("#FBF8F5"), c("#F1E8DF"), c("#E4D4C4"), c("#D0B69D"), c("#BF9E7F") } };
			return new PaletteData("Fresh Bright Pastels (Light)", families, cols, colors, Color.WHITE);
		}

		/**
		 * @return the "Professional Pastels (Light)" palette definition. Families are
		 *         ordered along the rainbow spectrum (Rot -&gt; Koralle -&gt; Gelb
		 *         -&gt; Gruen -&gt; Mint -&gt; Blau -&gt; Navy -&gt; Violett -&gt;
		 *         Pink), followed by the non-spectral neutrals Sand and Braun.
		 */
		static PaletteData professionalLight() {
			String[] families = FAMILY_NAMES;
			String[] cols = LIGHT_COLUMN_NAMES;
			Color[][] colors = { { c("#FDF5F5"), c("#F9E6E6"), c("#EEC2C2"), c("#E3A0A0"), c("#D68888") },
					{ c("#FFF8F6"), c("#F7E6E1"), c("#EDC8BE"), c("#F2B19E"), c("#E39A84") },
					{ c("#FDF6EF"), c("#F7E6D0"), c("#F0C890"), c("#E8A95C"), c("#DB9339") },					
					{ c("#FFFDF9"), c("#FBF4DE"), c("#F4E6B0"), c("#F8DF88"), c("#EBCF68") },
					{ c("#F6FBF3"), c("#E9F3E0"), c("#CFE6BC"), c("#B3D99A"), c("#9BCB81") },
					{ c("#F5FCF8"), c("#E8F5EE"), c("#C8E8D7"), c("#A5E3C5"), c("#8FD1B1") },
					{ c("#F5FAFD"), c("#E9F3F9"), c("#BEDAEC"), c("#7DBFEA"), c("#69AFDC") },
					{ c("#F3FAFD"), c("#DEF0F8"), c("#A7D9ED"), c("#67C0E0"), c("#4CADCE") },
					{ c("#F4F6FA"), c("#E3E8F2"), c("#B9C6DE"), c("#8AA0C9"), c("#7290BD") },
					{ c("#FAF8FD"), c("#EDE8F6"), c("#D6CAE8"), c("#BBA8DA"), c("#A28CC6") },
					{ c("#FFF8FB"), c("#F8E8EF"), c("#EDC8D9"), c("#E5AFC4"), c("#D493AB") },
					{ c("#FDFBF8"), c("#F6EEE2"), c("#EBDDBF"), c("#E5D0A8"), c("#D7BE92") },
					{ c("#FAF9F7"), c("#EFE8DF"), c("#DCCDBE"), c("#C4AF9B"), c("#AE9782") } };
			return new PaletteData("Professional Pastels (Light)", families, cols, colors, Color.WHITE);
		}

		/**
		 * @return the "Fresh Bright Pastels (Dark)" palette definition. Families are
		 *         ordered along the rainbow spectrum (Red -&gt; Coral -&gt; Yellow
		 *         -&gt; Green -&gt; Mint -&gt; Blue -&gt; Navy -&gt; Violet -&gt;
		 *         Pink), followed by the non-spectral neutrals Sand and Brown.
		 */
		static PaletteData freshBrightDark() {
			String[] families = FAMILY_NAMES;
			String[] cols = DARK_FRESH_COLUMN_NAMES;
			Color[][] colors = { { c("#441A1A"), c("#732E2E"), c("#C94C4C"), c("#FF7A7A"), c("#FFADAD") },
					{ c("#44261F"), c("#734136"), c("#C87862"), c("#F2B19E"), c("#FFD8CC") },
					{ c("#45301A"), c("#78522A"), c("#CB8A3F"), c("#FFA85C"), c("#FFCB96") },					
					{ c("#473B17"), c("#7B6527"), c("#C9A53A"), c("#F8DF88"), c("#FFF0AE") },
					{ c("#2B3A17"), c("#4D6B28"), c("#7DAE3F"), c("#9EEA7A"), c("#C7F5A8") },
					{ c("#173D34"), c("#276856"), c("#52B694"), c("#95EDC0"), c("#C8F9E0") },
					{ c("#183044"), c("#255273"), c("#4C9FD9"), c("#73CAFF"), c("#A9E3FF") },
					{ c("#143742"), c("#1F5F71"), c("#3BA5C0"), c("#58D6FF"), c("#AEEEFF") },
					{ c("#12203A"), c("#1F3660"), c("#3E6FB0"), c("#6F9FE0"), c("#A8CDFF") },
					{ c("#332B4A"), c("#574977"), c("#8D78B8"), c("#BBA8DA"), c("#E1D7F3") },
					{ c("#442A38"), c("#75485F"), c("#C17898"), c("#E5AFC4"), c("#FFD9E8") },
					{ c("#403626"), c("#6B5A3F"), c("#B89B5B"), c("#E5D0A8"), c("#F5E7C8") },
					{ c("#362D26"), c("#635245"), c("#987F6B"), c("#C4AF9B"), c("#E5D6C7") } };
			return new PaletteData("Fresh Bright Pastels (Dark)", families, cols, colors, new Color(0x2B2B2B));
		}

		/**
		 * @return the "Professional Pastels (Dark)" palette definition. Families are
		 *         ordered along the rainbow spectrum (Red -&gt; Coral -&gt; Yellow
		 *         -&gt; Green -&gt; Mint -&gt; Blue -&gt; Navy -&gt; Violet -&gt;
		 *         Pink), followed by the non-spectral neutrals Sand and Brown.
		 */
		static PaletteData professionalDark() {
			String[] families = FAMILY_NAMES;
			String[] cols = DARK_PROFESSIONAL_COLUMN_NAMES;
			Color[][] colors = { { c("#301F1F"), c("#6B3A3A"), c("#9C5C5C"), c("#E3A0A0"), c("#F0C4C4") },
					{ c("#31231F"), c("#8A6052"), c("#C18673"), c("#F2B19E"), c("#F7D0C1") },
					{ c("#302418"), c("#75502F"), c("#AD7A42"), c("#E8A95C"), c("#F4CB95") },
					{ c("#2D291D"), c("#7A6840"), c("#BFA052"), c("#F8DF88"), c("#FCECB9") },
					{ c("#232B1D"), c("#4A5A3A"), c("#78915C"), c("#B3D99A"), c("#D6ECC4") },
					{ c("#1F2A25"), c("#44695A"), c("#72A68D"), c("#A5E3C5"), c("#C7F0DA") },
					{ c("#1B2832"), c("#385A70"), c("#5F8FB0"), c("#7DBFEA"), c("#A5D7F4") },
					{ c("#172A30"), c("#305F6E"), c("#51939E"), c("#67C0E0"), c("#A9E0F0") },
					{ c("#151F2E"), c("#2E4766"), c("#4F6F95"), c("#7FA3C8"), c("#A9C6E0") },
					{ c("#262230"), c("#6E608D"), c("#9887BC"), c("#BBA8DA"), c("#D6CAF0") },
					{ c("#2D2328"), c("#8B6674"), c("#BC8FA0"), c("#E5AFC4"), c("#F1CEDB") },
					{ c("#2A2620"), c("#6A5E49"), c("#A89166"), c("#E5D0A8"), c("#F1E2C6") },
					{ c("#292421"), c("#746355"), c("#9E8673"), c("#C4AF9B"), c("#E0D1C4") } };
			return new PaletteData("Professional Pastels (Dark)", families, cols, colors, new Color(0x2B2B2B));
		}
	}

	// =====================================================================
	// Palette panel (one tab in the color chooser)
	// =====================================================================

}