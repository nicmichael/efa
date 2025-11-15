package de.nmichael.efa.gui.widgets;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JPanel;

import de.nmichael.efa.data.LogbookRecord;

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


    
}
