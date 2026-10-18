package de.nmichael.efa.gui.onlineupdate;

import java.awt.AWTEvent;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
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
import de.nmichael.efa.core.update.GithubReleaseUpdateProviderStrategy;
import de.nmichael.efa.core.update.LocalFileUpdateProviderStrategy;
import de.nmichael.efa.core.update.UpdateProviderStrategy;
import de.nmichael.efa.gui.BaseTabbedDialog;
import de.nmichael.efa.gui.BrowserDialog;
import de.nmichael.efa.gui.EfaGuiUtils;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;

public class OnlineUpdateSourceSelectionDialog extends BaseTabbedDialog implements IItemListener {

    private static final String SOURCE_EFA       = "efaNMichaelDe";
    private static final String SOURCE_GITHUB    = "GitHubRelease";
    private static final String SOURCE_LOCALFILE = "LocalFile";
    private static final int GRID_WIDTH = 3;

    private ItemTypeStringList sourceSelect;
    private ItemTypeLabel localFileInfo;
    private ItemTypeFile eouFile;
    private ItemTypeLabel urlGithubLabel0;
    private ItemTypeLabel urlGithubLabel1;
    private ItemTypeButton nextButton;
    
    private UpdateProviderStrategy selectedProvider = null; // null = abgebrochen

    public OnlineUpdateSourceSelectionDialog(JDialog parent) {
    	super(parent,
                International.getString("Update-Quelle wählen"),
                International.getString("Abbruch"),
                null, true);
        iniItems();
    }

    private void iniItems() {
        Vector<IItemType> guiItems = new Vector<IItemType>();
        String cat = "%01%" + International.getString("Update-Quelle");
        IItemType item;

        guiItems.add(EfaGuiUtils.createHeader("UPDATE_SOURCE_HEADER", IItemType.TYPE_PUBLIC, cat,
				International.getString("Update-Quelle wählen"),GRID_WIDTH));
        
        guiItems.add(item = new ItemTypeStringList("UPDATE_SOURCE", SOURCE_GITHUB,
                new String[] { SOURCE_GITHUB, SOURCE_LOCALFILE, SOURCE_EFA, },
                new String[] { 	International.getString("Github Releases (Standard)"),
                				International.getString("Lokale Datei vom USB-Stick (eou.xml)"), 
                				International.getString("efa.nmichael.de (Historisch)")},
                IItemType.TYPE_PUBLIC, cat,
                International.getString("Update-Quelle")));
        sourceSelect = (ItemTypeStringList) item;
        sourceSelect.registerItemListener(this);

        String localfilehint = International.getString("Zum Update von einem Verzeichnis (z.B. auf einem USB-Stick) müssen sich darin Download-Elemente von GitHub befinden: "+
        "\n\n1) Die eou.xml des aktuellsten efa-Releases \n2) mindestens ein efa*.zip Programmpaket-Download ihrer Wahl. \nEs können sich auch mehrere efa*.zip Dateien darin befinden.");
        guiItems.add(localFileInfo = EfaGuiUtils.createHintWordWrap("LOCALFILEHINT", IItemType.TYPE_PUBLIC, cat, localfilehint, GRID_WIDTH, 10, 10, 550));
        localFileInfo.setVisible(false);

        guiItems.add(eouFile = new ItemTypeFile("LOCALFILE_EOU", "",
                "eou.xml",
                International.getString("XML-Datei") + " (eou.xml)",
                "xml", ItemTypeFile.MODE_OPEN, ItemTypeFile.TYPE_FILE,
                IItemType.TYPE_PUBLIC, cat,
                International.getString("eou.xml auswählen")));
        eouFile.setVisible(false);
        
        guiItems.add(urlGithubLabel0 = new ItemTypeLabel("URL_GITHUB_LABEL0", IItemType.TYPE_PUBLIC, cat, 
        		International.getString("Github: Releases & Source") + ": " ));
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
        //stretch the button to the right, so that it is aligned with the other fields
        nextButton.setFieldGrid(GRID_WIDTH, 1, java.awt.GridBagConstraints.CENTER, java.awt.GridBagConstraints.HORIZONTAL);
        nextButton.setPadding(0, 0, 50, 30);
        setItems(guiItems);
    }
    
    @Override
    public boolean prepareDialog() {
        // Elemente kurz einblenden, damit die Dialoggröße die LocalFile-Maximalhöhe bekommt
        localFileInfo.setVisible(true);
        eouFile.setVisible(true);

        boolean ok = super.prepareDialog();

        // Danach auf den aktuellen Auswahlzustand zurücksetzen
        boolean local = SOURCE_LOCALFILE.equals(sourceSelect.getValue());
        localFileInfo.setVisible(local);
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
        // Nur reagieren, wenn ein neuer Eintrag ausgewählt wurde (nicht bei Abwahl oder anderen Events).

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
            localFileInfo.setVisible(local);
            eouFile.setVisible(local);
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

        if (SOURCE_GITHUB.equals(source)) {
            return new GithubReleaseUpdateProviderStrategy(
                    Daten.ONLINEUPDATE_INFO_GITHUB_OWNER,
                    Daten.ONLINEUPDATE_INFO_GITHUB_REPOSITORY,
                    false);
        }

        if (SOURCE_LOCALFILE.equals(source)) {
            String path = eouFile.getValue();
            
            if (path == null || path.trim().isEmpty()) {
                Dialog.error(International.getString("Bitte eou.xml-Datei auswählen."));
                eouFile.requestFocus();
                return null;
            } else if (!EfaUtil.canOpenFile(path.trim())) {
				Dialog.error(International.getMessage("{filedescription} '{filename}' existiert nicht",
                        International.getString("Datei"),path)+".");
				return null;
			}
            return new LocalFileUpdateProviderStrategy(path.trim());
        }

        return new EfaNMichaelDeUpdateProviderStrategy(Daten.ONLINEUPDATE_INFO);
    }

    /**
     * @return die gewählte UpdateProviderStrategy oder null, wenn der Anwender abgebrochen hat.
     */
    public UpdateProviderStrategy getSelectedProviderStrategy() {
        return selectedProvider;
    }
    
    void githubLabel_mouseClicked(MouseEvent e) {
        BrowserDialog.openExternalBrowser(this, Daten.EFAGITHUBURL);
    }
    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }
}
