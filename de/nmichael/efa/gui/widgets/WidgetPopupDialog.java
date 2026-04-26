package de.nmichael.efa.gui.widgets;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.EfaGuiUtils;
import de.nmichael.efa.util.International;

public class WidgetPopupDialog extends BaseDialog {
	
	private static final long serialVersionUID = 5167039692898335001L;
	private WidgetInstance widgetInstance;
    private int width;
    private int height;
    private int closeTimeoutSeconds;
    private boolean isClosed = false;

    public WidgetPopupDialog(String title, WidgetInstance widget, int width, int height, int closeTimeoutSeconds) {
        super((JDialog) null, title, International.getStringWithMnemonic("Schließen"));
        this.widgetInstance = widget;
        this.width = width;
        this.height = height;
        this.closeTimeoutSeconds = closeTimeoutSeconds;
        this.setIconImage(EfaGuiUtils.getEfaMainIcon());
    }


    protected void iniDialog() throws Exception {
        JScrollPane scrollPane = new JScrollPane();
        mainPanel.setLayout(new BorderLayout());

        if (width > 0 && height > 0) {
            scrollPane.setPreferredSize(new Dimension(width, height));
        }
        widgetInstance.construct();
        scrollPane.getViewport().add(widgetInstance.getComponent(), null);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        new Thread() {

            public void run() {
                try {
                	this.setName("HtmlPopupDialog.Inidialog.CancelThread");
                    Thread.sleep(closeTimeoutSeconds * 1000);

                    SwingUtilities.invokeLater(new Runnable() {
                	      public void run() {
                              cancel();
                	      }
                  	});

                } catch (Exception e) {
                }
            }
        }.start();
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public boolean cancel() {
        if (!isClosed) {
            isClosed = true;
            return super.cancel();
        }
        return true;
    }

}
