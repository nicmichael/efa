package de.nmichael.efa.gui.widgets;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.gui.ImagesAndIcons;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Logger;

public class MultiWidgetContainerInstance extends WidgetInstance implements IWidgetInstance {

	private JPanel mainPanel;
	private JPanel cardPanel = new JPanel(new CardLayout());
	private MultiWidgetPanelUpdater panelUpdater = null;
	private int updateInterval = 0;

	@Override
	public void runWidgetWarnings(int mode, boolean actionBegin, LogbookRecord r) {
		// TODO Auto-generated method stub
	}

	@Override
	public void stop() {
		try {
			// stopHTML also lets the thread die, and efaBths is responsible to set up a new thread.
			panelUpdater.stopRunning();
		} catch (Exception eignore) {
			// nothing to do, might not be initialized
		}
	}

	@Override
	public void construct() {

		mainPanel = new JPanel();
		mainPanel.setName("MultiWidget-MainPanel");
		CardLayout cardLayout = new CardLayout();
		cardPanel = new JPanel(cardLayout);
		cardPanel.setName("MultiWidget-Card");
		// mainpanel:
		// left: button, vertically centered
		// middle: cards
		// right: button, vertically centered

		mainPanel.setLayout(new GridBagLayout());
		JButton leftButton = new JButton();
		JButton rightButton = new JButton();
		leftButton.setIcon(ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_WIDGET_ARROW_LEFT));
		rightButton.setIcon(ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_WIDGET_ARROW_RIGHT));
		leftButton.setMinimumSize(new Dimension(30, 60));
		leftButton.setPreferredSize(leftButton.getMinimumSize());
		rightButton.setMinimumSize(leftButton.getMinimumSize());
		rightButton.setPreferredSize(rightButton.getMinimumSize());
		mainPanel.add(leftButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
		mainPanel.add(cardPanel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
		mainPanel.add(rightButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));

		// Clicking on left/right buttons selects a new card and restarts the
		// auto-update timer
		leftButton.addActionListener(e -> {
			cardLayout.previous(cardPanel);
			panelUpdater.interrupt();
		});
		rightButton.addActionListener(e -> {
			cardLayout.next(cardPanel);
			panelUpdater.interrupt();
		});
		mainPanel.addMouseListener(new MouseAdapter() {
			// entering the control with the mouse shall restart the auto-update-timer, so
			// the user has time to run some action...
			@Override
			public void mouseEntered(MouseEvent e) {
				panelUpdater.interrupt();
			}
		});

		leftButton.addMouseListener(new MouseAdapter() {
			// entering the control with the mouse shall restart the auto-update-timer, so
			// the user has time to run some action...
			@Override
			public void mouseEntered(MouseEvent e) {
				panelUpdater.interrupt();
			}
		});

		rightButton.addMouseListener(new MouseAdapter() {
			// entering the control with the mouse shall restart the auto-update-timer, so
			// the user has time to run some action...
			@Override
			public void mouseEntered(MouseEvent e) {
				panelUpdater.interrupt();
			}
		});

		try {
			panelUpdater = new MultiWidgetPanelUpdater(cardLayout, cardPanel, this.getUpdateInterval());
			panelUpdater.start();
		} catch (Exception e) {
			Logger.log(e);
		}
	}

	@Override
	public JComponent getComponent() {
		return mainPanel;
	}

	public void addWidget(JPanel panel) {
		cardPanel.add(panel);
	}

	public void clearWidgets() {
		cardPanel.removeAll();
	}

	public int getUpdateInterval() {
		return updateInterval;
	}

	public void setUpdateInterval(int updateInterval) {
		this.updateInterval = updateInterval;
	}

	class MultiWidgetPanelUpdater extends Thread {

		private volatile boolean keepRunning = true;
		private volatile int updateIntervalInSeconds = 15;

		private CardLayout cardLayout;
		private JPanel cardPanel;

		public MultiWidgetPanelUpdater(CardLayout cardLayout, JPanel cardPanel, int updateInterval) {
			this.cardLayout = cardLayout;
			this.cardPanel = cardPanel;
			this.updateIntervalInSeconds = updateInterval * 1000;
		}

		public void run() {
			this.setName("MultiWidgetContainer.MultiWidgetPanelUpdater" + " " + DataTypeTime.now().toString());

			while (keepRunning) {

				try {
					if (updateIntervalInSeconds > 0) {
						Thread.sleep(updateIntervalInSeconds);

						// Use invokelater as swing threadsafe ways
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								cardLayout.next(cardPanel);
							}
						});
					} else {
						// no automatic switch configured- we just wait a minute.
						Thread.sleep(60 * 1000); // wait a minute
					}

				} catch (InterruptedException e) {
					// This is when the thread gets interrupted when it is sleeping.
					EfaUtil.foo();
				} catch (Exception e) {
					Throwable t = e.getCause();
					if (t.getClass().getName().equalsIgnoreCase("java.lang.InterruptedException")) {
						EfaUtil.foo();
					} else {
						Logger.logdebug(e);
					}
				}

			}
		}

		public synchronized void stopRunning() {
			keepRunning = false;
			interrupt(); // wake up thread
		}

	}
}
