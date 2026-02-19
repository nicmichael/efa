package de.nmichael.efa.gui.widgets;

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
import de.nmichael.efa.util.EfaUtil;

public abstract class WidgetInstance implements IWidgetInstance {

	protected JPanel myPanel;
	private String position;

    public WidgetInstance() {
    }
	
	
    private void show(JPanel panel, int x, int y, boolean verticalAlignment, int insetTop, int insetBottom, int insetLeft, int insetRight) {
        myPanel = panel;
        JComponent comp = getComponent();
        if (comp != null) {
            panel.add(comp, new GridBagConstraints(x, y, 1, 1, 
            		1.0,
            		1.0,
            		GridBagConstraints.CENTER, 
            		GridBagConstraints.BOTH, //(verticalAlignment ? GridBagConstraints.VERTICAL : GridBagConstraints.HORIZONTAL), 
            		new Insets(insetTop, insetLeft, insetBottom, insetRight), 0, 0));
            
        }
    }

	@Override
	public void show(JPanel panel, String panelPosition) {
		/*
		 * Top, left, right, bottom position use GridBagLayout, others use Borderlayout
		 */
        myPanel = panel;
        construct();
        JComponent comp = getComponent();
        if (comp != null) {
        	if (panelPosition.equals(IWidget.POSITION_MULTIWIDGET)) {
        		//Multiwidget itself takes care of layout if multiple elements are put on it.
        		panel.add(comp);        		
        	} else if (panelPosition.equals(IWidget.POSITION_CENTER)) {
        		Boolean verticalAlignment=true;
        		int compCount = panel.getComponentCount();
        		int insetLeft = 3;
        		int insetTop = 3;
        		int insetRight = 3;
        		int insetBottom = 3;
        		
        		this.show(panel, (verticalAlignment ? 0 : compCount), 
		            			(verticalAlignment ? compCount : 0),
		            			verticalAlignment, 
		            			insetTop, insetBottom,
		            			insetLeft, insetRight);

        	} else { //top,bottom,left,right position: Gridbag
        		Boolean verticalAlignment= (panelPosition.equals(IWidget.POSITION_LEFT)|| panelPosition.equals(IWidget.POSITION_RIGHT));
        		int compCount = panel.getComponentCount();
        		int insetLeft = 3;
        		int insetTop = 3;
        		int insetRight = 3;
        		int insetBottom = 3;
        		
        		this.show(panel, (verticalAlignment ? 0 : compCount), 
		            			(verticalAlignment ? compCount : 0),
		            			verticalAlignment, 
		            			insetTop, insetBottom,
		            			insetLeft, insetRight);

        	}
        }
    }

	@Override
	public void stop() {
		EfaUtil.foo();
	}

	@Override
	public void runWidgetWarnings(int mode, boolean actionBegin, LogbookRecord r) {
		EfaUtil.foo();
	}

	
    public abstract void construct();
    public abstract JComponent getComponent();

	public Vector <WidgetInstance> createInstances(){
		Vector <WidgetInstance> returnList = new Vector <WidgetInstance>();
		return returnList;
	}

	protected static JPanel getLocationHeader(String caption, Boolean showMaximize) {
		return getLocationHeader(caption, showMaximize, null, null);
	}	

	protected static JPanel getLocationHeader(String caption, Boolean showMaximize, Color bg, Color fg) {
		RoundedPanel titlePanel = new RoundedPanel();
		titlePanel.setLayout(new GridBagLayout());
		titlePanel.setBackground((bg == null ? Daten.efaConfig.getToolTipHeaderBackgroundColor() : bg));
		titlePanel.setForeground((fg == null ? Daten.efaConfig.getToolTipHeaderForegroundColor() : fg));
	
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


	public String getPosition() {
		return position;
	}


	public void setPosition(String position) {
		this.position = position;
	}	
    
}
