/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 */

package de.nmichael.efa.core.items;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.EfaGuiUtils;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Logger;

/**
 * Two-column cell renderer for {@link ItemTypeList}.
 * <p>
 * All content is painted manually (no HTML rendering). This is necessary for
 * performance on low-end hardware such as a Raspberry Pi 3b with many list entries.
 * <p>
 * The renderer does not access the owning {@link ItemTypeList} directly. Layout
 * values are passed in via {@link #configure(int, int, int, Color)}.
 * 
 * The renderer does not access the {@link ItemTypeList.ItemTypeListData} directly, but instead
 * uses the values passed to {@link #getListCellRendererComponent(JList, ItemTypeList.ItemTypeListData, 
 * int, boolean, boolean)}.
 * This is necessary because the renderer may be used in a different list than the one that owns
 * the data, and the renderer may be reused for multiple lists.
 * 
 * The renderer shows a separator entry (bold, centered, with a different background color) 
 * if the data has the {@code separator} flag set.
 * Otherwise, it shows a 
 * - standard entry with an optional icon on the left, primary text on the left, 
 * - optional tertiary text on the left (after three spaces), 
 * - and secondary text on the right.
 * 
 * Usually, the primary text is the most important information, 
 * where the secondary text provides extended informations (like, reservation info 
 * or target of the session).
 * The tertiary text is additional information which extends the primary text, 
 * but is less important than the secondary text.
 * 
 * @see ItemTypeList
 * @see ItemTypeList.ItemTypeListData
 */
class ItemTypeListThreeColumnCellRenderer extends JPanel implements ListCellRenderer<ItemTypeList.ItemTypeListData> {

    private static final long serialVersionUID = 1L;

    // Spacings for pretty rendering
    private static final int HORZ_SINGLE_BORDER = 5;
    private static final Border EMPTY_BORDER = new EmptyBorder(2, HORZ_SINGLE_BORDER, 2, HORZ_SINGLE_BORDER);

    private final static int ICON_GAP = 6;
    private final static int SELECTION_BAR_PADDING = 6;   // padding inside the selection bar for better visuals
    private final static int RIGHT_PADDING = 12;
    private final static int LEFT_PADDING = 12;
    private final static int VERTICAL_PADDING_BETWEEN_LIST = 2; // shall be a round number, 2 means one pixel above, one below the text
    private final static int RESERVED_GAP_BETWEEN_SIDES = 40;   // desired whitespace between left and right text
    private static final int SELECTION_ARC = 14;                // larger value for separators so they stand out better

    // Layout values, set via configure(...)
    private int iconWidth;
    private int iconHeight;
    private int fieldWidth;
    private Color separatorBackground;

    // Per-cell state, set in getListCellRendererComponent(...)
    private ItemTypeList.ItemTypeListData currentItem;
    private boolean isSelected;
    private boolean cellHasFocus;
    private Color listBackground;

    private Font fontStandard;
    private Font fontBold;
    private Font fontPlain;
    private FontMetrics fmStandard;
    private FontMetrics fmBold;

    private Color selBg = UIManager.getColor("List.selectionBackground");
    private Color selFg = UIManager.getColor("List.selectionForeground");
    private Color inActiveSelBg = UIManager.getColor("List.selectionInactiveBackground");

    private Color grayColor = new Color(136, 136, 136); // #888888

    // Cache for the default icon, used if an item has no image and no colors
    private ImageIcon defaultIconForCellRenderer;
    private int defaultIconW = -1;
    private int defaultIconH = -1;

    // Cache of ready-to-draw icons, keyed by source and size.
    // Why: decoding a PNG and generating a color pie is far more expensive than drawing
    // the result, and paintComponent runs for every visible cell on each repaint.
    private final Map<String, ImageIcon> iconCache = new HashMap<>();

    /**
     * Creates the renderer. Layout values must be set afterwards via {@link #configure(int, int, int, Color)}.
     */
    ItemTypeListThreeColumnCellRenderer() {
        setOpaque(true);
        setBorder(EMPTY_BORDER);

        updateCachedColors();

        if (Daten.lookAndFeel.endsWith(Daten.LAF_METAL) || Daten.lookAndFeel.endsWith(Daten.LAF_WINDOWS)
                || Daten.lookAndFeel.endsWith(Daten.LAF_WINDOWS_CLASSIC) || Daten.lookAndFeel.endsWith(Daten.LAF_NIMBUS)) {
            inActiveSelBg = selBg;
        }
    }

    /**
     * Passes the current layout values from the owning list to this renderer.
     * Must be called whenever one of these values changes.
     *
     * @param iconWidth          width of the group icon in pixels, 0 if no icons are shown
     * @param iconHeight         height of the group icon in pixels, 0 if no icons are shown
     * @param fieldWidth         width of the list field in pixels
     * @param separatorBackground background color for separator entries
     */
    void configure(int iconWidth, int iconHeight, int fieldWidth, Color separatorBackground) {
        this.iconWidth = iconWidth;
        this.iconHeight = iconHeight;
        this.fieldWidth = fieldWidth;
        this.separatorBackground = separatorBackground;
        this.grayColor = EfaUtil.darker(separatorBackground, 36);
    }

    /**
	 * Updates the cached selection colors from the current Look and Feel.
	 * Should be called whenever the Look and Feel changes.
	 * Is used when the UIManager.getColor("List.selectionBackground"); methods return null 
	 * (e.g. on Linux with GTK+ Look and Feel).
	 */
    @SuppressWarnings("rawtypes")
    private void updateCachedColors() {
        selBg = (selBg != null ? selBg : new JList().getSelectionBackground());
        selFg = (selFg != null ? selFg : new JList().getSelectionForeground());
    }

    /**
	 * Returns the current layout values for the renderer.
	 * @return an array of four elements: {iconWidth, iconHeight, fieldWidth, separatorBackground}
	 */
    @Override
    public Component getListCellRendererComponent(JList<? extends ItemTypeList.ItemTypeListData> list,
            ItemTypeList.ItemTypeListData value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        this.currentItem = value;
        this.isSelected = isSelected;
        this.cellHasFocus = cellHasFocus;
        this.listBackground = list.getBackground();

        // Fonts and metrics are derived only when the list font changes, not for every cell.
        // Why: deriving fonts and metrics per cell repeats the same work on every repaint.
        Font listFont = list.getFont();
        if (!listFont.equals(fontStandard)) {
            fontStandard = listFont;
            fontPlain = listFont.deriveFont(Font.PLAIN);
            fontBold = listFont.deriveFont(Font.BOLD);
            fmStandard = getFontMetrics(fontStandard);
            fmBold = getFontMetrics(fontBold);
        }
        // Separators are bold, all other entries plain
        setFont(value != null && value.separator ? fontBold : fontPlain);

        int prefH = Math.max(fmStandard.getHeight() + 6, (iconHeight > 0 ? iconHeight + 4 : 0));
        setPreferredSize(new Dimension(fieldWidth, prefH + VERTICAL_PADDING_BETWEEN_LIST));

        return this;
    }

    /**
     * Paints the cell. The cell is either a separator (bold, centered, with a different background color)
     * or a standard entry.
     */
    @Override
    protected void paintComponent(Graphics g0) {
        /* Two Column Lists
         * Data is put together in ItemTypeBoatStatusList.sortBootsList and sortMemberList
         *
         * - Center separator texts, with grey background
         * - Left side: original data
         *
         * Available boats: (right side)
         * - show today's next reservation of a boat
         *
         * Boats on water: (right side)
         * - show destination of a boat
         *
         * Unavailable boats:
         * - show "boat damage" if the boat comment begins with this text
         * - show destination of boat
         * - show reservation end if boat is reserved
         *
         * Performance
         * - excellent, only 100-150 msec on Raspberry Pi 3b for reloading the whole boat list with ~300 boats
         */
        Graphics2D g = (Graphics2D) g0;

        EfaGuiUtils.setStandardRenderingHints(g);
        if (currentItem == null) {
            super.paintComponent(g);
            return;
        }

        int w = getWidth();
        int h = getHeight();

        // Draw background
        if (currentItem.separator) {
            // Separators use a different background color and are drawn as rounded rectangles.
            // The text is drawn centered and in bold.
            if (!isSelected) {
                g.setColor(separatorBackground);
                g.fillRoundRect(SELECTION_BAR_PADDING, 1, w - (SELECTION_BAR_PADDING * 2), h - 1, SELECTION_ARC, SELECTION_ARC);
            } else {
                if (cellHasFocus) {
                    g.setColor(selBg != null ? selBg : this.getBackground());
                } else {
                    g.setColor(inActiveSelBg != null ? inActiveSelBg : this.getBackground());
                }
                // Draw rounded selection
                g.fillRoundRect(SELECTION_BAR_PADDING, 1, w - (SELECTION_BAR_PADDING * 2), h - 1, SELECTION_ARC, SELECTION_ARC);
            }
        } else {
            // Standard entry: show rounded selection if selected, otherwise normal background.
            if (isSelected) {
                // Draw white background first to avoid color bleeding on rounded corners when selection background is not opaque
                g.setColor(listBackground);
                g.fillRect(0, 1, w, h - 1);
                if (cellHasFocus) {
                    g.setColor(selBg != null ? selBg : getBackground());
                } else {
                    g.setColor(inActiveSelBg != null ? inActiveSelBg : this.getBackground());
                }
                g.fillRoundRect(SELECTION_BAR_PADDING, 1, w - (SELECTION_BAR_PADDING * 2), h - 1, SELECTION_ARC, SELECTION_ARC);
            } else {
                g.setColor(listBackground);
                g.fillRect(0, 0, w, h);
            }
        }

        // Baseline for vertical centering
        int yText = (h + fmStandard.getAscent() - fmStandard.getDescent()) / 2;

        int x = LEFT_PADDING;

        // Icon rendering (only if icon present and not a separator)
        if (!currentItem.separator && iconWidth > 0 && iconHeight > 0) {
            // The icon comes from the cache, so it is only created on the first paint
            ImageIcon icon = getCachedIcon(currentItem, iconWidth, iconHeight);
            int iconY = (h - icon.getIconHeight()) / 2;
            g.drawImage(icon.getImage(), x, iconY, this);
            x += iconWidth + ICON_GAP;
        }

        // Separator text: centered, bold
        if (currentItem.separator) {
            String s = currentItem.text != null ? currentItem.text : "";
            g.setFont(fontBold);
            int sw = fmBold.stringWidth(s);
            int sx = Math.max(0, (w - sw) / 2);
            Color fg = isSelected ? selFg : getForeground();
            g.setColor(fg != null ? fg : getForeground());
            g.drawString(s, sx, yText);
            return;
        }

        // Standard entry: primary text (left), optional tertiary text (left, after 3 spaces),
        // secondary text (right, truncated if needed)
        String primary = currentItem.text != null ? currentItem.text : "";
        String tertiary = currentItem.tertiaryElement != null ? currentItem.tertiaryElement : "";
        String secondary = currentItem.secondaryElement != null ? currentItem.secondaryElement : "";

        // Left group must remain intact as it is the primary information
        String leftToDraw = primary;
        if (!tertiary.isEmpty()) {
            leftToDraw = primary + "   " + tertiary; // three spaces exactly
        }

        int leftWidth = fmStandard.stringWidth(leftToDraw);

        // Right boundary for secondary (absolute right, considering padding)
        int rightX = w - RIGHT_PADDING;

        // Maximum pixels available for secondary given the reserved gap.
        // Secondary must start at xSecLeft >= x + leftWidth + RESERVED_GAP_BETWEEN_SIDES
        int maxSecPx = rightX - (x + leftWidth + RESERVED_GAP_BETWEEN_SIDES + SELECTION_BAR_PADDING);
        String secToDraw = secondary;

        if (maxSecPx <= 0) {
            // no space for secondary (even zero), don't draw it
            secToDraw = "";
        } else if (fmStandard.stringWidth(secondary) > maxSecPx) {
            secToDraw = truncateToWidth(fmStandard, secondary, maxSecPx);
        }

        // Draw left text (primary + tertiary), preserving colors:
        // primary in normal foreground; tertiary in gray. If selected -> selection foreground.
        g.setFont(fontStandard);

        Color normalFg = isSelected ? (selFg != null ? selFg : getForeground()) : getForeground();
        Color tertiaryColor = isSelected ? (selFg != null ? selFg : getForeground()) : grayColor;

        // If there is a tertiary text, primary and tertiary are drawn separately in different colors.
        if (!tertiary.isEmpty()) {
            int xPrimary = x;
            g.setColor(normalFg);
            g.drawString(primary, xPrimary, yText);

            int xAfterPrimary = xPrimary + fmStandard.stringWidth(primary);
            // add width of three spaces
            int spacesW = fmStandard.stringWidth("   ");
            int xTertiary = xAfterPrimary + spacesW;

            String tertToDraw = tertiary;
            int maxTertPx = rightX - (xTertiary + SELECTION_BAR_PADDING);
            if (maxTertPx <= 0) {
                // no space for tertiary (even zero), don't draw it
                tertToDraw = "";
            } else if (fmStandard.stringWidth(tertiary) > maxTertPx) {
                tertToDraw = truncateToWidth(fmStandard, tertiary, maxTertPx);
            }
            g.setColor(tertiaryColor);
            g.drawString(tertToDraw, xTertiary, yText);
        } else {
            // only primary
            g.setColor(normalFg);
            g.drawString(primary, x, yText);
        }

        // Draw secondary (right-aligned) if any
        if (!secToDraw.isEmpty()) {
            int secW = fmStandard.stringWidth(secToDraw);
            int sx = rightX - secW;
            // maxSecPx already guarantees that secondary does not overlap the left group
            g.setColor(tertiaryColor);
            g.drawString(secToDraw, sx, yText);
        }
    }

    /**
     * Returns the icon for the item from the cache, creating it only on the first request.
     * The key contains the size, so a size change automatically produces a new entry.
     */
    private ImageIcon getCachedIcon(ItemTypeList.ItemTypeListData item, int w, int h) {
        String key = (item.image != null ? "img:" + item.image : "col:" + colorKey(item.colors))
                + "|" + w + "x" + h;
        ImageIcon icon = iconCache.get(key);
        if (icon == null) {
            icon = createIcon(item, w, h);
            iconCache.put(key, icon);
        }
        return icon;
    }

    /**
     * Creates the icon for an item. Only called on cache misses.
     * The fallback chain (image, color pie, default circle) is kept, and the default icon
     * is always returned so the cache never stores null.
     */
    private ImageIcon createIcon(ItemTypeList.ItemTypeListData item, int w, int h) {
        try {
            ImageIcon icon = null;
            if (item.image != null) {
                icon = BaseDialog.getIcon(item.image);
            }
            if (icon == null) {
                icon = (item.colors != null && item.colors.length > 0)
                        ? EfaUtil.createColorPieIcon(item.colors, w, h)
                        : buildDefaultIcon(w, h);
            }
            return icon;
        } catch (Exception e) {
            Logger.logdebug(e);
            return buildDefaultIcon(w, h);
        }
    }

    /**
     * Builds a value-based key from the RGB values, so equal color sets share one cache entry.
     * Color objects are not guaranteed to be identical instances, so identity cannot be used.
     * A null array yields an empty key; such items get the default icon anyway.
     */
    private static String colorKey(Color[] colors) {
        if (colors == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Color c : colors) {
            sb.append(Integer.toHexString(c.getRGB())).append(',');
        }
        return sb.toString();
    }

    /**
     * Returns a grey circle icon of the given size. The icon is cached and rebuilt only
     * if the size changes.
     */
    private ImageIcon buildDefaultIcon(int w, int h) {
        if (defaultIconForCellRenderer == null || defaultIconW != w || defaultIconH != h) {
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(230, 230, 230));
                g.fillOval(0, 0, w, h);
            } finally {
                g.dispose(); // always release graphics resources
            }
            defaultIconForCellRenderer = new ImageIcon(image);
            defaultIconW = w;
            defaultIconH = h;
        }
        return defaultIconForCellRenderer;
    }

    /**
     * Truncate a string to fit within maxPx pixels, appending an ellipsis if truncation occurs.
     *
     * @param fm    FontMetrics used to measure string width
     * @param s     String to truncate
     * @param maxPx Maximum pixel width allowed
     * @return Truncated string with ellipsis if needed
     */
    private String truncateToWidth(FontMetrics fm, String s, int maxPx) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        if (fm.stringWidth(s) <= maxPx) {
            return s;
        }

        String ell = "\u2026";
        int ellW = fm.stringWidth(ell);
        int lo = 0;
        int hi = s.length();

        // Binary search for the longest prefix that fits together with the ellipsis.
        // We know that truncation is needed because s is wider than maxPx.
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            int w = fm.stringWidth(s.substring(0, mid)) + ellW;
            if (w <= maxPx) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        if (lo <= 0) {
            return ell;
        }
        return s.substring(0, lo) + ell;
    }
}