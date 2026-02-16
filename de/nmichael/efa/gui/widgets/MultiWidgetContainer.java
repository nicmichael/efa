package de.nmichael.efa.gui.widgets;

import java.util.Vector;

import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeInteger;
import de.nmichael.efa.util.International;

/*
 * The Multiwidgetcontainer is a widget that can contain multiple other widgets.
 * The contained widgets are put in a panel with cardlayout.
 * 
 * Functionality:
 * - handle multiple other widgets within this container
 * - automatically switching through all panels every x seconds
 * - provide left/right buttons so that the user can step through the panels manually
 * - stop automatic switching when mouse enters the panel
 * 
 */
public class MultiWidgetContainer extends Widget {

	static final String PARAM_AUTOCHANGE = "AutomaticChangeAfterSeconds";

	public MultiWidgetContainer() {
	    super(International.getString("Multi-Widget"), "Multi-Widget", International.getString("Multi-Widget"), false, false, false, true);
	    
        addHintWordWrap(NOT_STORED_ITEM_PREFIX+"MultiWidgetInfo1",IItemType.TYPE_PUBLIC, "", International.getString("Das Multi-Widget kann in einem Platzbereich mehrere Widgets anzeigen.")+
        		International.getString("Wählen Sie dazu jeweils in den anderen Widgets als Position \"MultiWidget\" aus."), 3,6,6,550);
        
        addParameterInternal(new ItemTypeInteger(PARAM_AUTOCHANGE, 10, 0, 90, false,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Wechsel zum nächsten Widget nach ... Sekunden")));
        
        super.setEnabled(true);
        super.setPosition(IWidget.POSITION_CENTER);
        setActiveParameterInvisible();
	}

	private void setActiveParameterInvisible() {
		IItemType activeParameter = getParameterInternal(PARAM_ENABLED);
		activeParameter.setVisible(false);
	}
	
    public int getUpdateInterval() {
        return ((ItemTypeInteger)getParameterInternal(PARAM_AUTOCHANGE)).getValue();
    }

	@Override
	public Vector<WidgetInstance> createInstances() {
		Vector <WidgetInstance> returnList = new Vector <WidgetInstance>();
		MultiWidgetContainerInstance wi = new MultiWidgetContainerInstance();
		
		wi.setUpdateInterval(getUpdateInterval());
		wi.setPosition(IWidget.POSITION_CENTER);
		
		returnList.add(wi);
		return returnList;
	}

    @Override 
    public boolean isGuiWidget() {
    	return true;
    }
}
