
package com.databazoo.devmodeler.gui.window;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import com.databazoo.components.GCFrame;
import com.databazoo.components.UIConstants;
import com.databazoo.components.containers.HorizontalContainer;
import com.databazoo.components.containers.VerticalContainer;
import com.databazoo.components.text.LinkText;
import com.databazoo.components.text.SelectableText;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.gui.DesignGUI;

/**
 * Application info window.
 *
 * @author bobus
 */
public class AppInfoWindow {
	private static AppInfoWindow instance;

	public static AppInfoWindow get() {
		if (instance == null) {
			instance = new AppInfoWindow();
		}
		return instance;
	}

	private final JDialog frame;

	private AppInfoWindow() {
		frame = new JDialog(DesignGUI.get().frame, "About " + Config.APP_NAME, Dialog.ModalityType.APPLICATION_MODAL);
		frame.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
	}

	public void drawInfo() {
		/*JPanel iconPanel = new JPanel(new GridLayout(1,0,5,5));
		iconPanel.add(new JLabel(Theme.getLargeIcon(Theme.ICO_ABSTRACT)));
		iconPanel.add(new JLabel(Theme.getLargeIcon(Theme.ICO_MYSQL)));
		iconPanel.add(new JLabel(Theme.getLargeIcon(Theme.ICO_MARIADB)));
		iconPanel.add(new JLabel(Theme.getLargeIcon(Theme.ICO_POSTGRESQL)));
		iconPanel.add(new JLabel(Theme.getLargeIcon(Theme.ICO_ORACLE)));*/
		JLabel head = new JLabel("<html><h1>&nbsp;&nbsp;About " + Config.APP_NAME_BASE + "</h1></html>", Theme.getLargeIcon(Theme.ICO_LOGO), JLabel.CENTER);
		head.setBorder(new EmptyBorder(20, 0, 20, 0));

		JPanel statusPanel = new JPanel(new GridLayout(0, 2, 5, 5));
		/*statusPanel.add(new JLabel());
		statusPanel.add(new JLabel());*/

		/*UndoableTextField keyField = new UndoableTextField(Config.APP_KEY, true);
		keyField.disableFinder();
		keyField.setBordered(true);
		keyField.setEnabled(false);*/

		statusPanel.add(new JLabel("Application"));
		statusPanel.add(new JLabel(Config.APP_NAME_BASE));
		statusPanel.add(new JLabel("Version"));
		statusPanel.add(new JLabel(Config.APP_VERSION));
		statusPanel.add(new JLabel("Edition"));
		statusPanel.add(new JLabel(Config.EDITION_COMMUNITY));
		statusPanel.add(new JLabel("JRE"));
		statusPanel.add(new JLabel(UIConstants.getJREVersion()));

		JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		linkPanel.add(new JLabel("<html><br><br><br></html>"));
		linkPanel.add(new LinkText(Config.APP_DEFAULT_URL));

		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.PAGE_AXIS));
		leftPanel.setBorder(new EmptyBorder(0, 40, 20, 40));
		//leftPanel.add(iconPanel);
		leftPanel.add(statusPanel);
		leftPanel.add(linkPanel);

		frame.setContentPane(new VerticalContainer(
				head,
				new HorizontalContainer(leftPanel, addAboutText(), null),
				new JLabel("<html><font size=2><br>&copy; " + Config.APP_COPYRIGHT + ". Licensed under GNU AGPL v3.0. "
						+ "Logos and other trademarks are property of their respective owners.</font><br><br><br></html>", JLabel.CENTER)
		));
		frame.pack();
		frame.setLocationRelativeTo(DesignGUI.get().frame);
		frame.setVisible(GCFrame.SHOW_GUI);
	}

	void dispose(){
		frame.dispose();
	}

	private JComponent addAboutText() {
		//JLabel head = new JLabel("<html><h1><i>About...</i></h1></html>", JLabel.CENTER);
		JTextPane text = new SelectableText("<i>" + Config.APP_NAME_BASE + " is a database modeling and management tool. It's main purpose is to allow access to data, provide a simple way to design and visualize databases, and support replication process between servers.<br><br>It does not matter whether you are a database designer, analyst, programmer, release manager or database maintenance technician - once you get in touch with databases " + Config.APP_NAME_BASE + " will support you in your activities.</i>", true);

		//head.setPreferredSize(new Dimension(350, 104));
		text.setPreferredSize(new Dimension(350, 80));

		//return new VerticalContainer(head, text, null);
		return text;
	}

}
