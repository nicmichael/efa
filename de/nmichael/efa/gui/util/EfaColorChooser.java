package de.nmichael.efa.gui.util;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.colorchooser.AbstractColorChooserPanel;


/**
 * A drop-in replacement / wrapper for the standard {@link JColorChooser} that
 * adds additional chooser tabs offering a set of curated pastel color
 * palettes (Fresh Bright Pastels and Professional Pastels, each in a Light
 * and a Dark variant), selectable via a drop-down box and a tab that shows 
 * efa standard colors.
 * <p>
 * The tab shows a palette selector directly above the palette swatches on the
 * left side, and a control area on the right side (saturation adjustment, a
 * read-only hex code field for copy &amp; paste, and a "recently used"
 * history).
 *
 * @author efa (generated)
 */
public class EfaColorChooser extends JColorChooser {

    private static final long serialVersionUID = 1L;

    /** Shared history of recently selected colors, used by all palette panels. */
    private static RecentColorsModel recentColors = new RecentColorsModel(16);

    /**
     * Color groups of the widgets, shown in the efa-Standardfarben tab. This class
     * does not depend on the widgets itself: the application registers the
     * providers once at startup via {@link #setWidgetColorGroupProviders(List)}.
     */
    private static List<IColorGroupProvider> widgetColorGroupProviders = new ArrayList<>();

    /**
     * Registers the color groups of the widgets for the efa-Standardfarben tab.
     * Should be called once at application startup, before any chooser is opened.
     *
     * @param providers the widget color group providers (null is treated as empty)
     */
    public static synchronized void setWidgetColorGroupProviders(List<IColorGroupProvider> providers) {
        widgetColorGroupProviders = providers == null ? new ArrayList<>() : new ArrayList<>(providers);
    }


    /**
     * Creates a new efa color chooser with white as the initially selected color.
     */
    public EfaColorChooser() {
        this(Color.WHITE);
    }

    /**
     * Creates a new efa color chooser and registers the additional 
     * palette panels of efa.
     *
     * @param initialColor the color that is initially selected
     */
    public EfaColorChooser(Color initialColor) {
        super(initialColor);
        // Add both custom panels in a single setChooserPanels() call instead of two
        // separate addChooserPanel() calls. Each addChooserPanel() call fires a
        // "chooserPanels" property change that makes the color chooser UI tear down
        // and rebuild the tabs for *all* already-installed panels (not just the new
        // one). Doing this twice in a row (once for the pastel panel, once for the
        // standard colors panel) made the UI rebuild the tab for the pastel panel a
        // second time while it was based on an intermediate/incomplete state,
        // which is what broke its layout. Installing both panels at once avoids
        // that intermediate rebuild.
        // To add further custom tabs later, simply extend AbstractColorChooserPanel
        // (as EfaPastelPaletteChooserPanel / EfaStandardColorsChooserPanel do) and
        // add an instance to this array. No further subclassing of JColorChooser
        // is required - addChooserPanel()/setChooserPanels() are the actual Swing
        // extension points, available on every JColorChooser instance.
        AbstractColorChooserPanel[] defaultPanels = getChooserPanels();
        AbstractColorChooserPanel[] customPanels = new AbstractColorChooserPanel[] {
                new EfaPastelPaletteChooserPanel(recentColors),
                new EfaStandardColorsChooserPanel(recentColors,
                        StandardColorGroups.create(widgetColorGroupProviders))
                // new MyAdditionalChooserPanel(recentColors),
        };
        AbstractColorChooserPanel[] allPanels = new AbstractColorChooserPanel[defaultPanels.length + customPanels.length];
        System.arraycopy(defaultPanels, 0, allPanels, 0, defaultPanels.length);
        System.arraycopy(customPanels, 0, allPanels, defaultPanels.length, customPanels.length);
        setChooserPanels(allPanels);
    }

    /**
     * Convenience method that displays an {@link EfaColorChooser} inside a modal
     * dialog, similar to {@link JColorChooser#showDialog(Component, String, Color)}.
     * <p>
     * Note: this does <b>not</b>, and cannot, delegate to
     * {@code JColorChooser.showDialog(...)} via {@code super}. That method is
     * itself {@code static} (static methods are not inherited/overridden, so
     * there is no {@code super} call for them), and - more importantly - it
     * internally constructs its own plain {@code new JColorChooser(...)}
     * instance rather than accepting one as a parameter. Reusing it would
     * therefore show the vanilla Swing chooser without our custom palette
     * tabs. Instead, we build an {@link EfaColorChooser} ourselves and reuse
     * {@link JColorChooser#createDialog}, which does accept an existing
     * chooser instance (of any subclass) as a parameter.
     *
     * @param parent       the parent component for the dialog
     * @param title        the dialog title
     * @param initialColor the initially selected color
     * @return the color selected by the user, or {@code null} if the dialog was cancelled
     */
    public static Color showDialog(Component parent, String title, Color initialColor) {
        EfaColorChooser chooser = new EfaColorChooser(initialColor != null ? initialColor : Color.WHITE);
        final Color[] result = new Color[1];
        JDialog dialog = JColorChooser.createDialog(parent, title, true, chooser,
                e -> result[0] = chooser.getColor(),
                null);
        dialog.setVisible(true);
        dialog.dispose();
        return result[0];
    }

}