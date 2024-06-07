/**
 * <p>Title: efa - Elektronisches Fahrtenbuch</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2001</p>
 * <p>Company: </p>
 * @author Nicolas Michael
 * @version 1.0
 */
package de.nmichael.efa.util;

import java.awt.event.*;
import javax.swing.*;
import java.lang.reflect.*;
import java.util.*;

// @i18n complete
/**
 * General helper class which convieniently allows
 * user-interface handling. Components or
 * keystrokes are coupled with function calls; in
 * this they are Java methods via reflection like
 * C function pointers.
@author $Author: Siebenborn $
@version $Revision: 1.2 $ $Date: 2003/04/27 16:57:32 $
 */
public class ActionHandler extends AbstractAction {

    private HashMap methods = null;
    private HashMap fields = null;
    private Object methodObject = null;
    private int keyIndex = 0;

    /**
     * Constructor
     * Last change: 25-04-2003
    @param methodContainer class containing the functions to call; in most
    cases <this>
     */
    public ActionHandler(Object methodContainer) {
        super();
        methods = new HashMap();
        fields = new HashMap();
        methodObject = methodContainer;
    }//ActionHandler

    /**
     * Assign swing components with function calls aka methods.
     * The argument is an twodimensional object array;
     * the first index of the first dimension
     * contain the JComponents, the second index of
     * the first dimension the name of the method as
     * String.
     * For example: The object array
     * new Object[][]{{new JButton("Test")},{"testRoutine"}}
     * as argument assign the Button named "Test" with an
     * routine <public testRoutine(ActionEvent evt)>
     * protected and private methods are allowed, too.
     * Last change: 25-04-2003
    @params aObj   Object array[2][Number of entries]
    @throws IllegalArgumentException  - if the number of entries differ in
    the
    seperate dimensions
    - if so. try to reassign the same
    component

    @throws NoSuchMethodException      if the method is not found.
     */
    public void addActionListenerTable(Object[][] aObj)
            throws IllegalArgumentException, NoSuchMethodException {
        int iLength = -1;
        if (aObj.length == 2 && aObj[0].length > 0 && aObj[0].length == aObj[1].length) {
            iLength = aObj[0].length;
        } else {
            throw new IllegalArgumentException("Different number of entries");
        }

        String routineName;
        for (int i = 0; i < iLength; i++) {
            if (!(aObj[1][i] instanceof String)) {
                throw new IllegalArgumentException();
            } else {
                routineName = (String) aObj[1][i];
            }

            if (aObj[0][i] instanceof JButton) {
                ((JButton) aObj[0][i]).addActionListener(this);
            } else if (aObj[0][i] instanceof JComboBox) {
                ((JComboBox) aObj[0][i]).addActionListener(this);
            } else if (aObj[0][i] instanceof AbstractButton) {
                ((AbstractButton) aObj[0][i]).addActionListener(this);
            } else if (aObj[0][i] instanceof JTextField) {
                ((JTextField) aObj[0][i]).addActionListener(this);
            }
            if (!methods.containsKey(aObj[0][i])) {
                Method m = methodObject.getClass().getDeclaredMethod(routineName, new Class[]{ActionEvent.class});
                //Double entry ?
                if (methods.containsValue(m)) {
                    Collection c = methods.values();
                    Iterator it = c.iterator();
                    while (it.hasNext()) {
                        Method old = (Method) it.next();
                        if (m.equals(old)) {
                            methods.put(aObj[0][i], old);
                            break;
                        }//if
                    }//while
                }//if
                else {
                    methods.put(aObj[0][i], m);
                }
            }//if
            else {
                throw new IllegalArgumentException("Duplicate definition");
            }
        }//for
    }//addActionListener

    /**
     * Assign swing components with swing actions
     * The argument is an twodimensional object array
     * like the argument in addActionListener
     * In this case the second index of the first dimension
     * contain swing actions.
     * Last change: 25-04-2003
    @params aObj   Object array[2][Number of entries]
    @throws IllegalArgumentException  - if the number of entries differ in
    the
    seperate dimensions
    - if so. try to reassign the same
    component
     */
    public void addActionTable(Object[][] aObj)
            throws IllegalArgumentException {
        int iLength = -1;
        if (aObj.length == 2 && aObj[0].length > 0 && aObj[0].length == aObj[1].length) {
            iLength = aObj[0].length;
        } else {
            throw new IllegalArgumentException("Different number of entries");
        }

        for (int i = 0; i < iLength; i++) {
            if (aObj[0][i] instanceof JButton) {
                ((JButton) aObj[0][i]).setAction((AbstractAction) aObj[1][i]);
            } else if (aObj[0][i] instanceof JComboBox) {
                ((JComboBox) aObj[0][i]).setAction((AbstractAction) aObj[1][i]);
            } else if (aObj[0][i] instanceof AbstractButton) {
                ((AbstractButton) aObj[0][i]).setAction((AbstractAction) aObj[1][i]);
            } else if (aObj[0][i] instanceof JTextField) {
                ((JTextField) aObj[0][i]).setAction((AbstractAction) aObj[1][i]);
            }

            if (!fields.containsKey(aObj[0][i])) {
                fields.put(aObj[0][i], aObj[1][i]);
            } else {
                throw new IllegalArgumentException("Duplicate definition");
            }
        }//for
    }//addActionTable

    /**
     * Assign keystrokes of a component with focus to
     * function calls aka methods.
     * the first index of the first dimension
     * contain the JComponents, the second index of
     * the first dimension the name of the method as
     * String.
     * For example: The object array
     * new Object[][]{{new JButton("Test")},{"testRoutine"}}
     * as argument assign the Button named "Test" with an
     * routine <public testRoutine(ActionEvent evt)>
     * protected and private methods are allowed, too.
     * Last change: 25-04-2003
    @params comp             JComponent with focus.
    @params inputcondition   Change condition when the action is triggered;
    use e.g. JComponent.WHEN_IN_FOCUSED_WINDOW
    @params keystrokes       String array with names which must be accepted
    by KeyStroke.getKeyStroke()
    @params methodNames      String array with function names: same syntax
    as in addActionListener
    @throws IllegalArgumentException  - if the number of entries differ in
    the
    seperate dimensions
    - if so. try to reassign the same
    component
    - if inputcondition is not valid

    @throws NoSuchMethodException     -if the method is not found.
     */
    public void addKeyActions(JComponent comp, int inputcondition, String[] keystrokes, String[] methodNames)
            throws IllegalArgumentException, NoSuchMethodException {
        if (!(inputcondition == JComponent.WHEN_IN_FOCUSED_WINDOW
                || inputcondition == JComponent.WHEN_FOCUSED
                || inputcondition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)) {
            throw new IllegalArgumentException("Unknown input condition for keys");
        }
        if (keystrokes.length != methodNames.length || keystrokes.length < 1) {
            throw new IllegalArgumentException("Different/invalid number of entries");
        }

        for (int i = 0; i < keystrokes.length; i++) {
            AbstractAction a = new KeyAction(this);
            StringBuffer identifier = new StringBuffer("KEYSTROKE_ACTION_");
            identifier.append(Integer.toString(keyIndex++));
            String id = identifier.toString();
            a.putValue(Action.ACTION_COMMAND_KEY, id);
            comp.getInputMap(inputcondition).put(KeyStroke.getKeyStroke(keystrokes[i]), id);
            comp.getActionMap().put(id, a);
            Method m = methodObject.getClass().getDeclaredMethod(methodNames[i], new Class[]{ActionEvent.class});
            methods.put(id, m);
        }//for
    }//addKeyActions

    // modified version by Nicolas Michael
    public String addKeyAction(JComponent comp, int inputcondition, String keystroke, String methodName)
            throws IllegalArgumentException, NoSuchMethodException {
        if (!(inputcondition == JComponent.WHEN_IN_FOCUSED_WINDOW
                || inputcondition == JComponent.WHEN_FOCUSED
                || inputcondition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)) {
            throw new IllegalArgumentException("Unknown input condition for keys");
        }

        AbstractAction a = new KeyAction(this);
        StringBuffer identifier = new StringBuffer("KEYSTROKE_ACTION_");
        identifier.append(Integer.toString(keyIndex++));
        String id = identifier.toString();
        a.putValue(Action.ACTION_COMMAND_KEY, id);
        comp.getInputMap(inputcondition).put(KeyStroke.getKeyStroke(keystroke), id);
        comp.getActionMap().put(id, a);
        Method m = methodObject.getClass().getDeclaredMethod(methodName, new Class[]{ActionEvent.class});
        methods.put(id, m);
        return id;
    }

    /**
     * Action listener interface which invoke the stored matching method.
     * Bugfix: 1.1 no return in KEYSTROKE_ACTION if-branch
     *             resulting in throwing an IllegalArgumentException
     * Last change: 25-04-2003
     */
    public void actionPerformed(ActionEvent evt) {
        String command = evt.getActionCommand();
        int modifiers = evt.getModifiers();
        Object src = evt.getSource();
        String methodName;
        try {
            if (command.startsWith("KEYSTROKE_ACTION")) {
                Method m = (Method) methods.get(command);
                m.invoke(methodObject, new Object[]{evt});
                return;
            }//if

            if (methods.containsKey(src)) {
                Method m = (Method) methods.get(src);
                m.invoke(methodObject, new Object[]{evt});
            } else if (fields.containsKey(src)) {
                AbstractAction a = (AbstractAction) fields.get(src);
                a.actionPerformed(evt);
            }//else if
            else {
                //throw new IllegalArgumentException();
            }
        }//try
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }//actionPerformed

    /**
     * Helper class for assigning keystrokes to
     * the ActionHandler
     * Last change: 25-04-2003
     */
    private class KeyAction extends AbstractAction {

        private ActionHandler ah;

        public KeyAction(ActionHandler handler) {
            ah = handler;
        }

        public void actionPerformed(ActionEvent evt) {
            try {
                ah.actionPerformed(evt);
            } catch (Exception e) {
                // Exceptions in Logdatei vom RaW festgestellt (wahrscheinlich wegen Out-Of-Memory)
            }
        }
    }//KeyAction
}//ActionHandler

