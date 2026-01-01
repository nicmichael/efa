package de.nmichael.efa.gui.widgets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.gui.ImagesAndIcons;
import de.nmichael.efa.gui.util.RoundedPanel;

public abstract class WidgetInstance implements IWidgetInstance {

	private JPanel myPanel;

    public WidgetInstance() {
    }
	
	@Override
    public void show(JPanel panel, int x, int y) {
        myPanel = panel;
        construct();
        JComponent comp = getComponent();
        if (comp != null) {
            panel.add(comp, new GridBagConstraints(x, y, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        }
    }

	@Override
	public void show(JPanel panel, String orientation, boolean onMultiWidget) {
        myPanel = panel;
        construct();
        JComponent comp = getComponent();
        if (comp != null) {
        	if (!onMultiWidget) {
	        	if (orientation.equals(BorderLayout.CENTER)) {
		            if (panel.getComponentCount()==0) {
		            	panel.add(comp, BorderLayout.NORTH);
		            } else if (panel.getComponentCount()==1){
		            	panel.add(comp, BorderLayout.CENTER);
		            } else {
		            	panel.add(comp, BorderLayout.SOUTH);
		            }
	        	} else {
	        		panel.add(comp, orientation);
	        	}
        	} else {
        		panel.add(comp, orientation);
        	}
        }
    }

	@Override
	public void stop() {
		// TODO Auto-generated method stub
	}

	@Override
	public void runWidgetWarnings(int mode, boolean actionBegin, LogbookRecord r) {
		// TODO Auto-generated method stub
	}

	
    public abstract void construct();
    public abstract JComponent getComponent();

	public Vector <WidgetInstance> createInstances(){
		Vector <WidgetInstance> returnList = new Vector <WidgetInstance>();
		return returnList;
	}

	protected static JPanel getLocationHeader(String caption, Boolean isError, Boolean showMaximize) {
		return getLocationHeader(caption, isError, showMaximize, null, null);
	}	

	protected static JPanel getLocationHeader(String caption, Boolean isError, Boolean showMaximize, Color bg, Color fg) {
		RoundedPanel titlePanel = new RoundedPanel();
		titlePanel.setLayout(new GridBagLayout());
		titlePanel.setBackground(isError ? Daten.efaConfig.getErrorBackgroundColor() : (bg == null ? Daten.efaConfig.getToolTipHeaderBackgroundColor() : bg));
		titlePanel.setForeground(isError ? Daten.efaConfig.getErrorForegroundColor() : (fg == null ? Daten.efaConfig.getToolTipHeaderForegroundColor() : fg));
	
		JLabel titleLabel = new JLabel();
		titleLabel.setText(caption);
		titleLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		titleLabel.setForeground(titlePanel.getForeground());
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
		
		titlePanel.add(titleLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
		
		if (showMaximize) {
	        JLabel iconLabel=new JLabel();
	        iconLabel.setBackground(Daten.efaConfig.getToolTipHeaderBackgroundColor());
	        iconLabel.setForeground(Daten.efaConfig.getToolTipHeaderForegroundColor());
	        iconLabel.setIcon(ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_MAXIMIZE));
	        //iconLabel.setHorizontalTextPosition(SwingConstants.LEADING);

	        titlePanel.add(iconLabel, new GridBagConstraints(1, 0, 0, 0, 0.0, 0.0, GridBagConstraints.EAST,
					GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
		}
		
		return titlePanel;
	}	
    
}
