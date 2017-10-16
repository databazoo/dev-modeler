package com.databazoo.devmodeler.gui;

import com.databazoo.components.UIConstants;
import com.databazoo.components.elements.ClickableComponent;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.GC;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple information panel with colorful output. Hides on click.
 *
 * @author bobus
 */
class InfoPanel extends ClickableComponent implements IInfoPanel {

	private final List<InfoPanelLabel> labels = new ArrayList<>();
	private final Font nameFont = new Font(Font.SANS_SERIF, Font.ITALIC, Settings.getInt(Settings.L_FONT_TREE_SIZE));
	private int maxLabelCount = Config.INFO_PANEL_MAX_LABELS;
	private Timer hideTimer;


	InfoPanel(){
		setLayout(null);
	}

	@Override
	public synchronized int write(String message) {
		InfoPanelLabel label = new InfoPanelLabel(message, Color.BLACK);
		labels.add(label);
		drawLabels();

		return label.UID;
	}

	@Override
	public synchronized int writeGray(String message) {
		InfoPanelLabel label = new InfoPanelLabel(message, UIConstants.COLOR_GRAY);
		labels.add(label);
		drawLabels();

		return label.UID;
	}

	@Override
	public synchronized int writeGreen(String message) {
		InfoPanelLabel label = new InfoPanelLabel(message, UIConstants.COLOR_GREEN);
		labels.add(label);
		drawLabels();

		return label.UID;
	}

	@Override
	public synchronized int writeRed(String message) {
		InfoPanelLabel label = new InfoPanelLabel(message, UIConstants.COLOR_RED);
		labels.add(label);
		drawLabels();

		return label.UID;
	}

	@Override
	public synchronized int writeBlue(String message) {
		InfoPanelLabel label = new InfoPanelLabel(message, UIConstants.COLOR_BLUE);
		labels.add(label);
		drawLabels();

		return label.UID;
	}

	@Override
	public synchronized void writeOK(int uid) {
		InfoPanelLabel m = getLabel(uid);
		if(m != null){
			m.setText(m.getText() + " OK");
			m.setForeground(UIConstants.COLOR_GREEN);
			m.updateSize();
			m.setHideTimer(Config.INFO_PANEL_TIMEOUT_OK);
			drawLabels();
		}
	}

	@Override
	public synchronized void writeFailed(int uid, String failedReason) {
		InfoPanelLabel m = getLabel(uid);
		if(m != null){
			m.setText(m.getText() + " FAILED:");
			m.updateSize();
			m.setHideTimer(Config.INFO_PANEL_TIMEOUT_FAIL);

			int i = getLabelIndex(uid);
			ArrayList<InfoPanelLabel> tmp = new ArrayList<>();
			for (int j = i + 1; j < labels.size(); j++) {
				tmp.add(labels.get(j));
			}
			for (int j = i + 1; j < labels.size(); j++) {
				labels.remove(j);
			}
			m = new InfoPanelLabel(failedReason, UIConstants.COLOR_RED);
			m.setHideTimer(Config.INFO_PANEL_TIMEOUT_FAIL);
			labels.add(m);
			labels.addAll(tmp);
			drawLabels();
		}
	}

	private synchronized void drawLabels(){
		if (Settings.getBool(Settings.L_LAYOUT_INFOPANEL)) {
			if(hideTimer == null){
				hideTimer = new Timer(1000, e -> {
                    hideLabels();	// In EDT because of timer
                });
				hideTimer.start();
			}
			Schedule.inWorker(() -> {
                synchronized (InfoPanel.this) {
                    removeAll();
                    if (labels.size() > 0) {
                        while (labels.size() > maxLabelCount) {
                            labels.remove(0);
                        }
                        setVisible(true);
                        int maxW = 0;
                        for (int i = 0; i < labels.size(); i++) {
                            InfoPanelLabel m = labels.get(i);
                            m.setLocation(3, i * InfoPanelLabel.LINE_HEIGHT);
                            add(m);
                            if (m.getSize().width > maxW) {
                                maxW = m.getSize().width;
                            }
                        }
                        setSize(new Dimension(maxW + 5, labels.size() * InfoPanelLabel.LINE_HEIGHT + 5));
                    } else {
                        setVisible(false);
                        if (hideTimer != null) {
                            hideTimer.stop();
                            hideTimer = null;
                        }
                        GC.invoke();
                    }
                }
            });
		}
	}

	private synchronized void hideLabels(){
		boolean toRepaint = false;
		for(int i=0; i < labels.size(); i++){
			InfoPanelLabel label = labels.get(i);
			if(label.ttl <= 1){
				labels.remove(label);
				toRepaint = true;
				i--;
			}else{
				label.ttl--;
			}
		}
		if(toRepaint){
			drawLabels();
		}
	}

	@Override
	public synchronized void clicked(){
		labels.clear();
		drawLabels();
	}

	@Override
	public void doubleClicked(){ }

	private InfoPanelLabel getLabel(int uid) {
		for(InfoPanelLabel label: labels){
			if(label.UID == uid) {
				return label;
			}
		}
		return null;
	}

	@Override
	public void rightClicked(){
	}

	private int getLabelIndex(int uid) {
		for(int i=0; i<labels.size(); i++){
			if(labels.get(i).UID == uid) {
				return i;
			}
		}
		return -1;
	}

	int getMaxLabelCount() {
		return maxLabelCount;
	}

	void setMaxLabelCount(int maxLabelCount) {
		this.maxLabelCount = maxLabelCount;
	}

	List<InfoPanelLabel> getLabels() {
		return labels;
	}

	public class InfoPanelLabel extends JLabel
	{
		final static int LINE_HEIGHT = 14;
		final int UID = (int)(Math.random()*10000);
		private int ttl = Config.INFO_PANEL_TIMEOUT_WAIT;

		InfoPanelLabel(String message, Color color){
			super(message);
			setForeground(color);
			setBackground(Color.WHITE);
			setFont(nameFont);
			if(labels.size() > 20){
				labels.remove(0);
				drawLabels();
			}
			updateSize();
		}

		final synchronized void updateSize(){
			try {
				setSize(UIConstants.GRAPHICS.getFontMetrics(nameFont).stringWidth(getText())+2, LINE_HEIGHT);
			} catch (Exception e){
				Dbg.notImportant("Info Panel resize should never happen. Anyway...", e);
			}
		}

		private void setHideTimer(int delay) {
			ttl = delay;
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D graphics = (Graphics2D) g;
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			/*int width = getWidth();
			int height = getHeight();
			graphics.setColor(new Color(getBackground().getRed(), getBackground().getGreen(), getBackground().getBlue(), 180));
			graphics.fillRect(0, 0, width, height);*/

			graphics.setColor(getBackground());
			graphics.drawString(getText(), -1, 12);
			graphics.drawString(getText(), 1, 12);
			graphics.drawString(getText(), -1, 10);
			graphics.drawString(getText(), 1, 10);

			graphics.setColor(new Color(getBackground().getRed(), getBackground().getGreen(), getBackground().getBlue(), 160));
			graphics.drawString(getText(), -2, 12);
			graphics.drawString(getText(), 2, 12);
			graphics.drawString(getText(), -2, 10);
			graphics.drawString(getText(), 2, 10);

			graphics.setColor(getForeground());
			graphics.drawString(getText(), 0, 11);
		}

		@Override
		public String toString(){
			return getText();
		}
	}
}
