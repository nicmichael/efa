/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui;

import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.gui.util.RoundedBorder;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public abstract class StepwiseDialog extends BaseDialog {

	private static final long serialVersionUID = 7125589712864431436L;
	private static final int INPUTSECTION_WIDTH = 600;
	private static final int INPUTSECTION_HEIGHT = 360;
	JPanel stepPanel;
    JPanel controlPanel;
    JPanel descriptionPanel;
    JPanel inputPanelWrapper;
    JScrollPane inputScrollPane;
    JPanel inputPanel;
    JTextArea descriptionText;
    private JButton backButton;
    private JButton nextButton;
    private JButton finishButton;
    int step = 0;
    String[] steps;
    JLabel[] stepsLabel;
    ArrayList<IItemType> items;
    IItemType[] _thisStepItems;

    public StepwiseDialog(Frame parent, String title) {
        super(parent, title, null);
    }

    public StepwiseDialog(JDialog parent, String title) {
        super(parent, title, null);
    }

    protected void iniDialog() throws Exception {
        // StepPanel
        JPanel stepPanelWrapper = new JPanel();
        stepPanelWrapper.setLayout(new BorderLayout());
        stepPanel = new JPanel();
        stepPanel.setLayout(new GridBagLayout());
        stepPanel.setBorder(new javax.swing.border.EmptyBorder(20,20,20,20));
        JLabel stepHeaderLabel = new JLabel();
        stepHeaderLabel.setText(International.getString("Schritte")+":");
        stepHeaderLabel.setFont(stepHeaderLabel.getFont().deriveFont(Font.BOLD));
        stepPanel.add(stepHeaderLabel,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 10, 0), 0, 0));
        steps = getSteps();
        if (steps != null) {
            stepsLabel = new JLabel[steps.length];
            for (int i=0; steps != null && i<steps.length; i++) {
                JLabel l = new JLabel();
                l.setText((i+1) + ". " + steps[i]);
                l.setForeground(Color.black);
                stepPanel.add(l,  new GridBagConstraints(0, i+1, 1, 1, 0.0, 0.0
                    ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
                stepsLabel[i] = l;
            }
        }

        // ControlPanel
        controlPanel = new JPanel();
        controlPanel.setLayout(new GridBagLayout());
        backButton = new JButton();
        Mnemonics.setButton(this, backButton, International.getStringWithMnemonic("Zurück"),
                IMAGE_BACK);
        backButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                backButton_actionPerformed(e);
            }
        });
        nextButton = new JButton();
        Mnemonics.setButton(this, nextButton, International.getStringWithMnemonic("Weiter"),
                IMAGE_FORWARD);
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                nextButton_actionPerformed(e);
            }
        });
        finishButton = new JButton();
        Mnemonics.setButton(this, finishButton, International.getStringWithMnemonic("Fertig"),
                IMAGE_ACCEPT);
        finishButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                finishButton_actionPerformed(e);
            }
        });
        JButton cancelButton = new JButton();
        Mnemonics.setButton(this, cancelButton, International.getStringWithMnemonic("Abbruch"),
                IMAGE_CANCEL);
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelButton_actionPerformed(e);
            }
        });
        JButton helpButton = new JButton();
        Mnemonics.setButton(this, helpButton, International.getStringWithMnemonic("Hilfe"),
                IMAGE_HELP);
        Mnemonics.setButton(this, cancelButton, International.getStringWithMnemonic("Abbruch"),
                IMAGE_CANCEL);
        helpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                helpButton_actionPerformed(e);
            }
        });
        controlPanel.add(backButton,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
        controlPanel.add(nextButton,  new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
        controlPanel.add(finishButton,  new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
        controlPanel.add(cancelButton,  new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
        controlPanel.add(helpButton,  new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));

        // DescriptionPanel
        descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new BorderLayout());
        descriptionPanel.setBorder(new javax.swing.border.EmptyBorder(20,20,20,20));
        JLabel descriptionLabel = new JLabel();
        descriptionLabel.setText(International.getString("Beschreibung")+":");
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(Font.BOLD));
        descriptionPanel.add(descriptionLabel, BorderLayout.NORTH);

        JScrollPane descriptionScrollPane = new JScrollPane();
        descriptionScrollPane.setPreferredSize(new Dimension(200,100));
        descriptionScrollPane.setMinimumSize(new Dimension(200,100));
        descriptionPanel.add(descriptionScrollPane, BorderLayout.CENTER);
        descriptionText = new JTextArea();
        descriptionText.setEditable(false);
        descriptionText.setWrapStyleWord(true);
        descriptionText.setLineWrap(true);
        descriptionScrollPane.getViewport().add(descriptionText, null);

        // Input Panel
        inputPanelWrapper = new JPanel();
        inputPanelWrapper.setLayout(new BorderLayout());
        inputPanelWrapper.setBorder(new javax.swing.border.EmptyBorder(20,20,20,20));
        inputPanelWrapper.setMinimumSize(new Dimension(INPUTSECTION_WIDTH,INPUTSECTION_HEIGHT));
        inputScrollPane = new JScrollPane();
        inputScrollPane.setPreferredSize(new Dimension(INPUTSECTION_WIDTH+60,INPUTSECTION_HEIGHT+20));
        inputPanelWrapper.add(inputScrollPane, BorderLayout.CENTER);

        // Add Panels to basePanel and mainPanel
        stepPanelWrapper.add(stepPanel, BorderLayout.NORTH);
        basePanel.add(controlPanel, BorderLayout.SOUTH);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(stepPanelWrapper, BorderLayout.WEST);
        mainPanel.add(descriptionPanel,BorderLayout.SOUTH);
        mainPanel.add(inputPanelWrapper, BorderLayout.CENTER);

        updateGui();
    }

    public void updateGui() {
        backButton.setEnabled(step > 0);
        nextButton.setEnabled(steps != null && step+1 < steps.length);
        finishButton.setEnabled(steps != null && step+1 == steps.length);
        for (int i=0; i<stepsLabel.length; i++) {
            stepsLabel[i].setForeground( (i == step ? Color.blue : Color.black) );
            stepsLabel[i].setFont( (i == step ? mainPanel.getFont().deriveFont(Font.BOLD): mainPanel.getFont()) );
        }

        descriptionText.setText(getDescription(step));
        if (getDescription(step).length()>0) { descriptionText.setCaretPosition(0);}
        if (inputPanel != null) {
            // inputPanelWrapper.remove(inputPanel);
            inputScrollPane.getViewport().remove(inputPanel);
        }

        inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        JLabel titleLabel = new JLabel();
        titleLabel.setText(steps[step]);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setForeground(Color.blue);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        inputPanel.add(titleLabel,  new GridBagConstraints(0, 0, 10, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 20, 0), 0, 0));

        _thisStepItems = getInputItems(step);
        IItemType itemToBeFocused = null;
        if (_thisStepItems != null) {
            int y = 1;
            for (IItemType item : _thisStepItems) {
                if (item.getType() != IItemType.TYPE_PUBLIC) {
                    continue;
                }
                y += item.displayOnGui(this, inputPanel, y);
                if (item.isVisible() && item.isEnabled() && item.isEditable() && itemToBeFocused == null) {
                    itemToBeFocused = item;
                }
            }
            RoundedBorder myBorder=new RoundedBorder(inputPanel.getBackground().darker());
            inputPanel.setBorder(myBorder); // new javax.swing.border.LineBorder(Color.black)
            // inputPanelWrapper.add(inputPanel, BorderLayout.CENTER);
            inputScrollPane.getViewport().add(inputPanel, null);
            inputScrollPane.setBorder(BorderFactory.createEmptyBorder());
        }
        this.validate();
        if (itemToBeFocused != null) {
            itemToBeFocused.requestFocus();
        } else {
            if (nextButton.isEnabled()) {
                nextButton.requestFocus();
            } else {
                finishButton.requestFocus();
            }
        }
    }

    public IItemType[] getInputItems(int step) {
        if (items == null) {
            initializeItems();
        }
        ArrayList<IItemType> stepItems = new ArrayList<IItemType>();
        for (IItemType item : items) {
            if (EfaUtil.string2int(item.getCategory(),-1) == step) {
                stepItems.add(item);
            }
        }
        return stepItems.toArray(new ItemType[0]);
    }

    public IItemType getItemByName(String name) {
        if (items == null) {
            return null;
        }
        for (IItemType item : items) {
            if (item.getName().equals(name)) {
                return item;
            }
        }
        return null;
    }

    abstract String[] getSteps();
    abstract String getDescription(int step);
    abstract void initializeItems();

    boolean checkInput(int direction) {
        if (_thisStepItems == null) {
            return true;
        }
        if (direction < 0) {
            return true;
        }
        for (IItemType item : _thisStepItems) {
            if (!item.isValidInput()) {
                Dialog.error(International.getMessage("Ungültige Eingabe im Feld '{field}'",item.getDescription()));
                item.requestFocus();
                return false;
            } else {
                item.getValueFromGui();
            }
        }
        return true;
    }

    protected boolean backButton_actionPerformed(ActionEvent e) {
        if (!checkInput(-1)) {
            return false;
        }
        if (step > 0) {
            do {
                step--;
            } while(step > 0 && (getInputItems(step) == null || getInputItems(step).length == 0));
            updateGui();
        }
        return true;
    }

    protected boolean nextButton_actionPerformed(ActionEvent e) {
        if (!checkInput(+1)) {
            return false;
        }
        if (steps != null && step+1 < steps.length) {
            do {
                step++;
            } while(step+1 < steps.length && (getInputItems(step) == null || getInputItems(step).length == 0));
            updateGui();
        }
        return true;
    }

    boolean finishButton_actionPerformed(ActionEvent e) {
        if (!checkInput(0)) {
            return false;
        }
        cancel();
        return true;
    }

    void cancelButton_actionPerformed(ActionEvent e) {
        cancel();
    }

    void helpButton_actionPerformed(ActionEvent e) {
        Help.showHelp(getHelpTopics());
    }

}
