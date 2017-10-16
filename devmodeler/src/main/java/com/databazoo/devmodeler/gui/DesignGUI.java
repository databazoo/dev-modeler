package com.databazoo.devmodeler.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.databazoo.components.GCFrame;
import com.databazoo.components.UIConstants;
import com.databazoo.components.containers.VerticalContainer;
import com.databazoo.components.text.SelectableText;
import com.databazoo.devmodeler.DevModeler;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.ConnectionUtils;
import com.databazoo.devmodeler.gui.view.DifferenceView;
import com.databazoo.devmodeler.gui.view.OptimizerView;
import com.databazoo.devmodeler.gui.view.ViewMode;
import com.databazoo.devmodeler.gui.window.Splash;
import com.databazoo.devmodeler.project.ProjectManager;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

/**
 * Main GUI controller.
 *
 * @author bobus
 */
public class DesignGUI {
	public static final int SCROLL_AMOUNT = 40;

	//private int viewOld;
	private static ViewMode view;
	private static ViewMode viewOld;
	private static IInfoPanel infoPanel;
	public static DesignGUI instance;

	public static DesignGUI get(){
		if(instance == null){
			instance = new DesignGUI();
		}
		return instance;
	}

	public static ViewMode getView(){
		return view;
	}

	public static IInfoPanel getInfoPanel(){
		return infoPanel != null ? infoPanel : new InfoPanel();
	}

	public static void toClipboard (String myString) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection (myString), null);
	}

	public GCFrame frame;
	private JSplitPane splitPane;
	private JScrollPane dbScrl;
	private JScrollPane canvasScroll;

	//private HashMap<KeyStroke, Action> actionMap = new HashMap<KeyStroke, Action>();
	private boolean dbViewOn = !Settings.getBool(Settings.L_LAYOUT_DB_TREE);

	private JSplitPane diffSplitPane, optimizerSplitPane;
	private final Map<KeyStroke,Action> actionMap = new HashMap<>();

	private void setLAF(){
		if(UIConstants.isRetina()){
			return;
		}

		boolean found = false;
		try {
			for(String desiredName : new String[]{/*"Windows", "CDE/Motif", "GTK+",*/ "Nimbus"}){	// ALL NATIVE LOOKS ARE PRETTY SHITTY, USING NIMBUS INSTEAD
				if(found){
					break;
				}
				for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
					//System.out.println(info.getName());
					if (desiredName.equals(info.getName())) {
						UIManager.setLookAndFeel(info.getClassName());
						found = true;
						break;
					}
				}
			}
		} catch (Exception er) {
			Dbg.fixme("Switching to Nimbus failed, trying system LAF.", er);
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				Dbg.fixme("If this fails there's something very wrong with the installation. Not much we can do about it.", e);
			}
		}
	}

	public void drawMainWindow(){
		setLAF();
		Splash.get().partLoaded();
		try {
			SwingUtilities.invokeAndWait(() -> {

                // Window
                frame = new GCFrame(Config.APP_NAME);
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                frame.getContentPane().setLayout(new BorderLayout(0,0));
                frame.addWindowListener(new WindowAdapter(){ @Override public void windowClosing(WindowEvent e) { askIfClose(); } });

                // Top menu panel
                frame.getContentPane().add(Menu.getInstance(), BorderLayout.NORTH);
                //frame.getContentPane().setBorder(new EmptyBorder(0, 0, 0, 0));

                infoPanel = new InfoPanel();

                // Canvas
                canvasScroll = new JScrollPane(Canvas.instance);
                canvasScroll.setMinimumSize(new Dimension(0, 0));
                if(Settings.getBool(Settings.L_LAYOUT_SCROLLS)){
                    canvasScroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
                    canvasScroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));
                }
                Canvas.instance.setScrolls(canvasScroll);
                Canvas.instance.setInfoPanel((Component) infoPanel);
                Canvas.instance.setOverview(Navigator.instance);

                // DB preview
                dbScrl = new JScrollPane(DBTree.instance);
                dbScrl.setMinimumSize(new Dimension(0, 0));
                DBTree.instance.setScrollPaneSize(dbScrl, frame.getContentPane());

                // Pack split panes
                diffSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, DifferenceView.instance, canvasScroll);
                diffSplitPane.setBorder(new EmptyBorder(0, 0, 0, 0));
                diffSplitPane.setDividerSize(3);
                diffSplitPane.setResizeWeight(0.0);

                optimizerSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, OptimizerView.instance, diffSplitPane);
                optimizerSplitPane.setBorder(new EmptyBorder(0, 0, 0, 0));
                optimizerSplitPane.setDividerSize(3);
                optimizerSplitPane.setResizeWeight(0.0);

                splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                        new VerticalContainer.Builder()
                                .top(SearchPanel.instance)
                                .center(dbScrl)
                                .bottom(Neighborhood.instance)
                                .min(new Dimension(0, 0))
                                .build(),
                        optimizerSplitPane);
                splitPane.setBorder(new EmptyBorder(0, 0, 0, 0));
                splitPane.setDividerSize(0);
                splitPane.setResizeWeight(0.0);
                splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, pce -> {
                    int val = Integer.parseInt(pce.getNewValue().toString());
                    if(val > 10){
                        DBTree.instance.splitWidth = val;
                    }
                });
                frame.getContentPane().add(splitPane, BorderLayout.CENTER);

                Schedule.inWorker(Schedule.TYPE_DELAY, () -> {
                    toggleDBView();
                    switchView(ViewMode.DESIGNER);
                });

                // Show window
                frame.setIconImages(Theme.getAllSizes(Theme.ICO_LOGO));
                frame.setVisible(true);
                frame.pack();
                frame.setSize(GCFrame.DEFAULT_WIDTH+200, GCFrame.DEFAULT_HEIGHT);
                frame.setLocationRelativeTo(null);
                if(!UIConstants.DEBUG){
                    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                }

                Canvas.instance.drawInfoPanel();
                registerKeys();

                Splash.get().partLoaded();

                Schedule.inWorker(200, () -> {

                    int log = DesignGUI.getInfoPanel().write("Loading database drivers...");
                    ConnectionUtils.checkDrivers();
                    Splash.get().partLoaded();

                    DesignGUI.getInfoPanel().writeOK(log);
                    Dbg.toFile("GUI v" + Config.APP_VERSION + " created on " + UIConstants.OS + " " + UIConstants.getJREVersion());
                    Splash.get().partLoaded();
                });

                Schedule.inEDT(600, Canvas.instance::scrollToCenter);
            });
		} catch (Exception ex){
			Dbg.fixme("Could not build GUI!", ex);
			DevModeler.exit(1001);
		}
	}

	private void registerKeys(){
		AbstractAction aaAlt = new AbstractAction("alt") {
			@Override public void actionPerformed(ActionEvent e) {
				if(Settings.getBool(Settings.L_KEYS_CANVAS_ALT_MODES)){
					viewOld = view;
					if(view != ViewMode.DATA){
						switchView(ViewMode.DATA, true);
					}else{
						switchView(ViewMode.DESIGNER, true);
					}
				}
			}
		};
		AbstractAction aaAltReleased = new AbstractAction("rel alt") {
			@Override public void actionPerformed(ActionEvent e) {
				if(Settings.getBool(Settings.L_KEYS_CANVAS_ALT_MODES)){
					switchView(viewOld, true);
				}
			}
		};
		actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ALT, InputEvent.ALT_DOWN_MASK), aaAlt);
		actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ALT, 0, true), aaAltReleased);

		AbstractAction aaMeta = new AbstractAction("meta") {
			@Override public void actionPerformed(ActionEvent e) {
				if(Settings.getBool(Settings.L_KEYS_CANVAS_META_MODES)){
					viewOld = view;
					if(view != ViewMode.DATA){
						switchView(ViewMode.DATA, true);
					}else{
						switchView(ViewMode.DESIGNER, true);
					}
				}
			}
		};
		AbstractAction aaMetaReleased = new AbstractAction("rel meta") {
			@Override public void actionPerformed(ActionEvent e) {
				if(Settings.getBool(Settings.L_KEYS_CANVAS_META_MODES)){
					switchView(viewOld, true);
				}
			}
		};
		actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_META, InputEvent.META_DOWN_MASK), aaMeta);
		actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_META, 0, true), aaMetaReleased);

		KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		kfm.addKeyEventDispatcher(e -> {
			final KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
			if (actionMap.containsKey(keyStroke)) {
				Schedule.inWorker(() -> actionMap.get(keyStroke).actionPerformed(new ActionEvent(e.getSource(), e.getID(), null)));
				return true;
			}
			return false;
		});
	}

	void askIfClose(){
		boolean close = true;
		if(Settings.getBool(Settings.L_NOTICE_CLOSE)){
			Object[] options = {"Close application",  "Cancel"};
			int n = JOptionPane.showOptionDialog(frame, "Close "+Config.APP_NAME+" now?", "Close "+Config.APP_NAME, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			close = (n == 0);
		}
		if(close){
			ProjectManager.getInstance().checkZoomBeforeProjectClose();
			ProjectManager.getInstance().closeCurrentProject();
			Dbg.toFile("Closing main window with"+(Settings.getBool(Settings.L_NOTICE_CLOSE) ? "" : "out")+" confirmation");
			DevModeler.exit(0);
		}
	}

	private void switchView(ViewMode view) {
		if(diffSplitPane != null) {
			if (view == ViewMode.DIFF) {
				diffSplitPane.setDividerSize(3);
				diffSplitPane.setDividerLocation(Config.VIEW_DIVIDER_LOCATION);
			} else {
				diffSplitPane.setDividerSize(0);
				diffSplitPane.setDividerLocation(0);
			}
		}

		if(optimizerSplitPane != null) {
			if (view == ViewMode.OPTIMIZE) {
				optimizerSplitPane.setDividerSize(3);
				optimizerSplitPane.setDividerLocation(Config.VIEW_DIVIDER_LOCATION);
			} else {
				optimizerSplitPane.setDividerSize(0);
				optimizerSplitPane.setDividerLocation(0);
			}
		}

		DesignGUI.view = view;
		Menu.getInstance().switchView(view);
	}

	public void switchView(ViewMode view, boolean redraw) {
		switchView(view);
		if(redraw){
			Canvas.instance.setSelectedElement(null);
			Schedule.inWorker(() -> Canvas.instance.drawProject(true));
		}
	}

	public void setTitle(String projectName) {
		if(frame != null) {
			if(projectName.isEmpty()){
				frame.setTitle(Config.APP_NAME);
			}else{
				frame.setTitle(projectName + " - " + Config.APP_NAME);
			}
		}
	}

	void toggleDBView(){
		if(dbViewOn){
			dbViewOn = false;
			splitPane.setDividerLocation(0);
			splitPane.setDividerSize(0);
		}else{
			dbViewOn = true;
			splitPane.setDividerLocation(DBTree.instance.splitWidth);
			splitPane.setDividerSize(3);
			//splitPane.setDividerSize(10);
		}
	}

	public void openURL(String url){
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			try {
				Desktop.getDesktop().browse(new URI(url));
			} catch(Exception e) {
				Dbg.notImportant("No native libraries. Not much we can do.", e);
			}
		}else{
			final JDialog longQueryWindow = new JDialog(frame, "Default browser could not be opened", false);

			JButton btnClose = new JButton("Close");
			btnClose.addActionListener(ae -> longQueryWindow.dispose());
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			buttonPanel.add(btnClose);

			JPanel contPanel = new JPanel(new BorderLayout(10,10));
			contPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 10, 40));
			contPanel.add(new SelectableText("<html>Your default browser could not be opened.<br><br>Please open <a href="+url+">"+url+"</a> manually.<br><br></html>", true), BorderLayout.CENTER);
			contPanel.add(buttonPanel, BorderLayout.SOUTH);

			longQueryWindow.add(contPanel);
			longQueryWindow.pack();
			longQueryWindow.setLocationRelativeTo(frame);
			longQueryWindow.setVisible(true);
		}
	}

}
