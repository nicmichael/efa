package de.nmichael.efa.gui.widgets;

import javax.swing.JComponent;
import javax.swing.JFrame;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.items.ItemTypeItemList;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.EfaBaseFrame;
import de.nmichael.efa.gui.NotificationDialog;
import de.nmichael.efa.util.Logger;

public class AlertWidgetInstance extends WidgetInstance implements IWidgetInstance {

    private ItemTypeItemList alertList;
	
    @Override
    public void runWidgetWarnings(int mode, boolean actionBegin, LogbookRecord r) {
        try {
            ItemTypeItemList list = getAlertList();
            if (list == null || r == null) {
                return;
            }
            BoatRecord b = r.getBoatRecord(System.currentTimeMillis());
            String bType = null;
            String bSeats = null;
            if (b != null) {
                if (b.getNumberOfVariants() > 1) {
                    bType = b.getTypeType(b.getVariantIndex(r.getBoatVariant()));
                    bSeats = b.getTypeSeats(b.getVariantIndex(r.getBoatVariant()));
                } else {
                    bType = b.getTypeType(0);
                    bSeats = b.getTypeSeats(0);
                }
            }
            for (int i = 0; i < list.size(); i++) {
                if ((((mode == EfaBaseFrame.MODE_BOATHOUSE_START
                        || mode == EfaBaseFrame.MODE_BOATHOUSE_START_CORRECT
                        || mode == EfaBaseFrame.MODE_BOATHOUSE_START_MULTISESSION) && !actionBegin
                        && AlertWidget.isShowOnSessionStart(list, i))
                        || (mode == EfaBaseFrame.MODE_BOATHOUSE_FINISH && !actionBegin
                        && AlertWidget.isShowOnSessionFinish(list, i))) && AlertWidget.isShowForBoat(list, i, bType, bSeats)
                        && Daten.efaConfig.getValueNotificationWindowTimeout() > 0) {
                    String text = AlertWidget.getText(list, i);
                    String color = "0000ff";
                    String image = BaseDialog.BIGIMAGE_INFO;
                    if (AlertWidget.TYPE_WARN.equals(AlertWidget.getType(list, i))) {
                        color = "ff0000";
                        image = BaseDialog.BIGIMAGE_WARNING;
                    }
                    if (text != null && text.length() > 0) {
                        NotificationDialog dlg = new NotificationDialog((JFrame) null,
                                text, image, "ffffff", color, Daten.efaConfig.getValueNotificationWindowTimeout());
                        dlg.showDialog();
                    }
                }
            }
        } catch(Exception eignore) {
            Logger.logdebug(eignore);
        }
    }

    @Override
    public void construct() {
    }

    @Override
    public JComponent getComponent() {
        return null;
    }

    @Override
    public void stop() {
    }

	public ItemTypeItemList getAlertList() {
		return alertList;
	}

	public void setAlertList(ItemTypeItemList alertList) {
		this.alertList = alertList;
	}

}
