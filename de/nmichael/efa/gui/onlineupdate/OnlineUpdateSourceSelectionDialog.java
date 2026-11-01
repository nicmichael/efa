package de.nmichael.efa.gui.onlineupdate;

import java.awt.AWTEvent;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Vector;

import javax.swing.JDialog;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeButton;
import de.nmichael.efa.core.items.ItemTypeFile;
import de.nmichael.efa.core.items.ItemTypeLabel;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.core.update.EfaNMichaelDeUpdateProviderStrategy;
import de.nmichael.efa.core.update.GithubBetaReleaseUpdateProviderStrategy;
import de.nmichael.efa.core.update.GithubProductionReleaseUpdateProviderStrategy;
import de.nmichael.efa.core.update.LocalFileUpdateProviderStrategy;
import de.nmichael.efa.core.update.UpdateProviderStrategy;
import de.nmichael.efa.core.update.UpdateProviderStrategyDescriptor;
import de.nmichael.efa.core.update.UpdateProviderStrategyRegistry;
import de.nmichael.efa.gui.BaseTabbedDialog;
import de.nmichael.efa.gui.BrowserDialog;
import de.nmichael.efa.gui.EfaGuiUtils;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;

/**
 * Dialog to select the update source (EFA-NMichael.de, Github, local file).
 * 
 *
 */
public class OnlineUpdateSourceSelectionDialog extends BaseTabbedDialog implements IItemListener {

	// NEW: reference the strategies' own name constants instead of duplicating
	// (and potentially diverging from) the string literals here.
	private static final String SOURCE_EFA = EfaNMichaelDeUpdateProviderStrategy.EFA_NMICHAEL_DE;
	private static final String SOURCE_GITHUB_PROD = GithubProductionReleaseUpdateProviderStrategy.GITHUBPROD;
	private static final String SOURCE_GITHUB_BETA = GithubBetaReleaseUpdateProviderStrategy.GITHUBBETA;
	private static final String SOURCE_LOCALFILE = LocalFileUpdateProviderStrategy.LOCAL_FILE;
	private static final int GRID_WIDTH = 3;
	// same maxPixelWidth as passed to EfaGuiUtils.createHintWordWrap(...) below,
	// must stay in sync so that re-wrapping the hint text at runtime looks
	// identical.
	private static final int HINT_MAX_PIXEL_WIDTH = 550;

	private ItemTypeStringList sourceSelect;
	private ItemTypeLabel localFileInfo;
	private ItemTypeFile eouFile;
	private ItemTypeLabel urlGithubLabel0;
	private ItemTypeLabel urlGithubLabel1;
	private ItemTypeButton nextButton;

	private UpdateProviderStrategy selectedProvider = null; // null = abgebrochen

	public OnlineUpdateSourceSelectionDialog(JDialog parent) {
		super(parent, International.getString("Update-Quelle wählen"), International.getString("Abbruch"), null, true);
		iniItems();
	}

	/**
	 * Initializes the dialog's items (dropdown, labels, file picker, button).
	 */
	private void iniItems() {
		Vector<IItemType> guiItems = new Vector<IItemType>();
		String cat = "%01%" + International.getString("Update-Quelle");
		IItemType item;

		guiItems.add(EfaGuiUtils.createHeader("UPDATE_SOURCE_HEADER", IItemType.TYPE_PUBLIC, cat,
				International.getString("Update-Quelle wählen"), GRID_WIDTH));

		// Build the dropdown's values/display names dynamically from the registry,
		// instead of hard-coding them here. This way, every UpdateProviderStrategy
		// implementation that self-registers becomes automatically selectable,
		// without having to touch this dialog again.
		List<UpdateProviderStrategyDescriptor> descriptors = UpdateProviderStrategyRegistry.getAll();
		String[] sourceValues = new String[descriptors.size()];
		String[] sourceDisplayNames = new String[descriptors.size()];
		for (int i = 0; i < descriptors.size(); i++) {
			UpdateProviderStrategyDescriptor descriptor = descriptors.get(i);
			sourceValues[i] = descriptor.getName();
			sourceDisplayNames[i] = descriptor.getDisplayName();
		}

		guiItems.add(item = new ItemTypeStringList("UPDATE_SOURCE", SOURCE_GITHUB_PROD, sourceValues,
				sourceDisplayNames, IItemType.TYPE_PUBLIC, cat, International.getString("Update-Quelle")));
		sourceSelect = (ItemTypeStringList) item;
		sourceSelect.registerItemListener(this);

		// initialize the hint text from the initially selected source's user hint
		String initialHint = UpdateProviderStrategyRegistry.get(SOURCE_GITHUB_PROD).getUserHint();
		guiItems.add(localFileInfo = EfaGuiUtils.createHintWordWrap("LOCALFILEHINT", IItemType.TYPE_PUBLIC, cat,
				initialHint, GRID_WIDTH - 1, 10, 10, HINT_MAX_PIXEL_WIDTH));
		localFileInfo.setVisible(hasText(initialHint));
		localFileInfo.setIndent(true);

		//parameter only visible when LocalFile is selected.
		guiItems.add(eouFile = new ItemTypeFile("LOCALFILE_EOU", "", "eou.xml",
				International.getString("XML-Datei") + " (eou.xml)", "xml", ItemTypeFile.MODE_OPEN,
				ItemTypeFile.TYPE_FILE, IItemType.TYPE_PUBLIC, cat, International.getString("eou.xml auswählen")));
		eouFile.setVisible(false);

		guiItems.add(urlGithubLabel0 = new ItemTypeLabel("URL_GITHUB_LABEL0", IItemType.TYPE_PUBLIC, cat,
				International.getString("GitHub: Releases & Source") + ": "));
		guiItems.add(urlGithubLabel1 = new ItemTypeLabel("URL_GITHUB_LABEL0", IItemType.TYPE_PUBLIC, cat,
				Daten.EFAGITHUBURL));

		urlGithubLabel0.setIndent(true);
		urlGithubLabel1.setIndent(true);
		urlGithubLabel1.setColor(EfaGuiUtils.getUrlLabelColor());
		urlGithubLabel1.activateMouseClickListener();
		urlGithubLabel1.registerItemListener(this);

		guiItems.add(nextButton = new ItemTypeButton("NEXT", IItemType.TYPE_PUBLIC, cat,
				International.getStringWithMnemonic("Update-Quelle wählen und fortfahren")));
		nextButton.registerItemListener(this);
		// stretch the button to the right, so that it is aligned with the other fields
		nextButton.setFieldGrid(GRID_WIDTH, 1, java.awt.GridBagConstraints.CENTER,
				java.awt.GridBagConstraints.HORIZONTAL);
		nextButton.setPadding(0, 0, 50, 30);
		setItems(guiItems);
	}

	@Override
	public boolean prepareDialog() {
		// Elemente kurz einblenden, damit die Dialoggröße die LocalFile-Maximalhöhe
		// bekommt
		localFileInfo.setVisible(true);
		eouFile.setVisible(true);

		boolean ok = super.prepareDialog();

		// Danach auf den aktuellen Auswahlzustand zurücksetzen
		boolean local = SOURCE_LOCALFILE.equals(sourceSelect.getValue());
		String hint = UpdateProviderStrategyRegistry.get(sourceSelect.getValue()).getUserHint();
		localFileInfo.setVisible(hasText(hint));
		eouFile.setVisible(local);
		if (ok) {
			updateGui(true);
		}
		return ok;
	}

	/**
	 * Zeigt bei LocalFile den Infotext und das Dateiauswahlfeld für die eou.xml an.
	 */
	@Override
	public void itemListenerAction(IItemType itemType, AWTEvent event) {
		// Nur reagieren, wenn ein neuer Eintrag ausgewählt wurde (nicht bei Abwahl oder
		// anderen Events).

		if (event instanceof FocusEvent) {
			return;
		}
		if (itemType == sourceSelect) {
			if ((event instanceof java.awt.event.ItemEvent)
					&& ((java.awt.event.ItemEvent) event).getStateChange() != java.awt.event.ItemEvent.SELECTED) {
				return;
			}
			sourceSelect.getValueFromGui();
			boolean local = SOURCE_LOCALFILE.equals(sourceSelect.getValue());
			boolean efanmichael = SOURCE_EFA.equals(sourceSelect.getValue());
			// Update the hint text for the newly selected source and show it
			// whenever the strategy provides a non-empty user hint, regardless
			// of which source is currently selected. The raw hint text must be
			// re-wrapped into HTML with explicit line breaks (same as at creation
			// time via EfaGuiUtils.createHintWordWrap), otherwise it would be
			// rendered as a single, non-wrapped line.
			String hint = UpdateProviderStrategyRegistry.get(sourceSelect.getValue()).getUserHint();
			localFileInfo.setDescription(EfaGuiUtils.wrapCaptionAsHtml(hint, HINT_MAX_PIXEL_WIDTH));
			localFileInfo.setVisible(hasText(hint));
			eouFile.setVisible(local); // file picker stays local-file-only
			urlGithubLabel0.setVisible(!efanmichael);
			urlGithubLabel1.setVisible(!efanmichael);
			return;
		}
		if (itemType == urlGithubLabel1) {
			BrowserDialog.openExternalBrowser(this, Daten.EFAGITHUBURL);
		}
		if (itemType == eouFile) {
			eouFile.getValueFromGui();
		}
		if (itemType == sourceSelect || itemType == eouFile) {
			updateGui(false);
		}
		if (itemType == nextButton) {
			if ((event instanceof ActionEvent)) {
				nextButton_actionPerformed(null);
			}
		}
	}

	/**
	 * "Weiter zur Versionsauswahl": erzeugt die Strategie und schließt den Dialog.
	 */
	public void nextButton_actionPerformed(ActionEvent e) {
		getValuesFromGui();
		UpdateProviderStrategy provider = createProvider();
		if (provider == null) {
			return; // Dialog bleibt offen
		}
		selectedProvider = provider;
		setDialogResult(true);
		cancel(); // schließt den Dialog
	}

	private UpdateProviderStrategy createProvider() {
		String source = sourceSelect.getValue();

		// Local file source needs UI-specific validation (mandatory field, file
		// must exist) before an instance can be created, so it is handled here
		// rather than purely generically.
		if (SOURCE_LOCALFILE.equals(source)) {
			String path = eouFile.getValue();

			if (path == null || path.trim().isEmpty()) {
				Dialog.error(International.getString("Bitte eou.xml-Datei auswählen."));
				eouFile.requestFocus();
				return null;
			} else if (!EfaUtil.canOpenFile(path.trim())) {
				Dialog.error(International.getMessage("{filedescription} '{filename}' existiert nicht",
						International.getString("Datei"), path) + ".");
				return null;
			}
			return UpdateProviderStrategyRegistry.get(source).createInstance(path.trim());
		}

		// All other sources need no extra configuration from the GUI and can be
		// instantiated generically via the registry's factory.
		UpdateProviderStrategyDescriptor descriptor = UpdateProviderStrategyRegistry.get(source);
		return descriptor.createInstance(null);
	}

	/**
	 * @return die gewählte UpdateProviderStrategy oder null, wenn der Anwender
	 *         abgebrochen hat.
	 */
	public UpdateProviderStrategy getSelectedProviderStrategy() {
		return selectedProvider;
	}

	/**
	 * Small helper to check whether a user hint string actually contains
	 * displayable text, so that empty/blank hints don't show up as an empty (but
	 * space-reserving) widget in the dialog.
	 *
	 * @param s the string to check, may be {@code null}
	 * @return {@code true} if s is non-null and contains non-whitespace characters
	 */
	private static boolean hasText(String s) {
		return s != null && !s.trim().isEmpty();
	}

	void githubLabel_mouseClicked(MouseEvent e) {
		BrowserDialog.openExternalBrowser(this, Daten.EFAGITHUBURL);
	}

	public void keyAction(ActionEvent evt) {
		_keyAction(evt);
	}
}
