package com.databazoo.devmodeler.wizards;

import com.databazoo.components.GCFrameWithObservers;
import com.databazoo.components.UIConstants;
import com.databazoo.components.combo.IconableComboBox;
import com.databazoo.components.containers.HorizontalContainer;
import com.databazoo.components.containers.VerticalContainer;
import com.databazoo.components.textInput.AutocompleteObserver;
import com.databazoo.components.textInput.FormattedClickableTextField;
import com.databazoo.components.textInput.NextFieldObserver;
import com.databazoo.components.textInput.UndoableTextField;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.ConnectionUtils;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;
import com.databazoo.devmodeler.tools.formatter.FormatterBase;
import com.databazoo.devmodeler.tools.formatter.FormatterDataType;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;

import static java.awt.Frame.MAXIMIZED_BOTH;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;


public abstract class MigWizard implements TreeSelectionListener, Serializable {
	private static final long serialVersionUID = 1905122041950000001L;

	protected static final String ERROR_WHILE_SAVING = "Error while saving ";

	public static final int CLOSE_WINDOW = 1;
	protected static final int OPEN_PROJECT = 2;
	protected static final int CREATE_PROJECT_FINAL = 3;
	protected static final int CREATE_PROJECT_PAGE_2 = 4;
	protected static final int CREATE_PROJECT_PAGE_3 = 5;
	public static final int CREATE_PROJECT_SIMPLE = 6;

	protected static final String SPAN2				= "span 2";
	protected static final String SPAN2_CENTER 		= "span 2, align center";
	protected static final String SPAN2_GROW		= "span 2, grow";
	protected static final String SPAN2_H100_W5050	= "span 2, height 100%, width :50%:50%";
	protected static final String SPAN				= "span";
	protected static final String SPAN_CENTER		= "span, align center";
	protected static final String SPAN_GROW			= "span, grow";

	protected static String notificationText;

	protected Project project;
	protected DB database;
	protected IConnection connection;

	protected GCFrameWithObservers frame;
	protected JPanel buttonPane;
	private JScrollPane treeScroll;

	private JPanel pageContent = new JPanel();
	private JPanel placementPanel;

	protected JButton btnSave = new JButton("Save"), btnCancel = new JButton("Close");

	protected int pageFails = 0;
	private boolean isContentScrollable;
	private JLabel lastTitle;

	public MigWizard(){
		this(1, 20);
	}
	MigWizard(int insets, int middleGap){
		if(ProjectManager.getInstance() != null && !ProjectManager.getInstance().getProjectList().isEmpty() && Project.getCurrDB() != null){
			database = Project.getCurrDB();
			connection = ConnectionUtils.getCurrent(database.getName());
			project = Project.getCurrent();
		}
		preparePageLayout(insets, middleGap);
	}

	private void preparePageLayout(int insets, int middleGap){
		JPanel panel = getPanel();

		int left = 80;
		int minLeft = left-middleGap/2;
		panel.setLayout(new MigLayout(
				"insets "+insets+", wrap 4",
				"["+minLeft+"px::][100::50%-"+left+"px,grow,fill]"+middleGap+"["+minLeft+"px::][100::50%-"+left+"px,grow,fill]", "[]1[]"
		));
	}

	public JPanel getPanel() {
		return placementPanel==null ? pageContent : placementPanel;
	}

	protected JPanel createPlacementPanel(){
		return createPlacementPanel(20);
	}

	protected JPanel createPlacementPanel(int insets){
		placementPanel = new JPanel();
		preparePageLayout(insets, 20);
		return placementPanel;
	}

	protected void setPlacementPanel(JPanel panel){
		placementPanel = panel;
	}

	public void setDatabase(DB instance) {
		database = instance;
		connection = ConnectionUtils.getCurrent(database.getName());
	}

	void setContentScrollable(boolean scrollable){
		isContentScrollable = scrollable;
	}

	public String getLastTitle(){
		return lastTitle == null ? "" : lastTitle.getText();
	}

	protected void drawWindow(String fullName, JComponent tree, boolean maximize, boolean thinButtonPane){
		frame = new GCFrameWithObservers(fullName);
		frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		frame.setIconImages(Theme.getAllSizes(Theme.ICO_LOGO));
		frame.setDefaultSize();
		frame.setLocationRelativeTo(DesignGUI.get().frame);
		frame.setVisible(true);
		if(maximize){
			frame.setExtendedState(MAXIMIZED_BOTH);
		}

		frame.setContentPane(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT));

		drawTree(tree);
		drawButtonPane(thinButtonPane);
		drawPageContent();

		JComponent pane = (JComponent) frame.getContentPane();
		pane.getActionMap().put("closeWin", new AbstractAction("closeWin") { @Override public void actionPerformed(ActionEvent evt) { Schedule.inEDT(() -> executeAction(CLOSE_WINDOW)); } });
		pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "closeWin");
	}

	protected void drawTree(JComponent tree){
		if(treeScroll != null){
			frame.getContentPane().remove(treeScroll);
		}
		JPanel leftPanel = new JPanel(new MigLayout("insets 1, fill"));
		leftPanel.add(treeScroll = new JScrollPane(tree), "height 100%, grow");
		((JSplitPane)frame.getContentPane()).setLeftComponent(leftPanel);
		setTreeVisible(tree != null);
	}

	protected void drawTreeControls(JComponent treeControls){
		((Container)((JSplitPane)frame.getContentPane()).getLeftComponent()).add(treeControls, "south");
	}

	private void drawPageContent(){
		Component rComponent;
		if(isContentScrollable){
			JScrollPane scrollPane = new JScrollPane(pageContent);
			scrollPane.setBorder(null);
			rComponent = scrollPane;
		}else{
			rComponent = pageContent;
		}
		((JSplitPane)frame.getContentPane()).setRightComponent(new VerticalContainer(null, rComponent, consumeTextPane()));
	}

	private Component consumeTextPane() {
		if (notificationText == null) {
			return buttonPane;
		}
		JLabel label = new JLabel(notificationText);
		label.setBorder(new EmptyBorder(10, 5, 10, 20));
		notificationText = null;
		return new VerticalContainer(label, null, buttonPane);
	}

	private void drawButtonPane(boolean thinButtonPane){
		if(thinButtonPane) {
			buttonPane = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
			buttonPane.setBorder(new EmptyBorder(1, 0, 0, 0));
		}else{
			buttonPane = new JPanel(new FlowLayout());
		}
		createButtons();
	}

	protected void createButtons(){
		btnSave.setEnabled(false);
		btnCancel.addActionListener(new WizardListener(CLOSE_WINDOW));

		buttonPane.add(btnSave);
		buttonPane.add(btnCancel);
	}

	public abstract void notifyChange(String elementName, String value);
	public abstract void notifyChange(String elementName, boolean value);
	public abstract void notifyChange(String elementName, boolean[] values);

	private void removeSaveListeners(){
		for(ActionListener l: btnSave.getActionListeners()){
			btnSave.removeActionListener(l);
		}
		btnSave.setText("");
	}


	// SIMPLE TEXT
	protected JLabel addText(String text){
		return addText(text, "");
	}
	protected JLabel addText(String text, String placement){
		JLabel lab = new JLabel("<html>"+text+"</html>");
		getPanel().add(lab, placement);
		return lab;
	}

	// TITLE TEXT
	protected JLabel addTitle(String text){
		return lastTitle = addText("<h1>"+text+"</h1>", "align center, span");
	}

	// COMMENTED TEXT
	protected JLabel addComment(String text){
		return addText("<font color=gray>"+text+"</font>");
	}
	protected JLabel addComment(String text, String placement){
		return addText("<font color=gray>"+text+"</font>", placement);
	}

	// EMPTY SPACES
	protected void addEmptyLine(){
		getPanel().add(new JLabel(" "), "span, height 16px!");
	}
	protected void addEmptySlot(){
		getPanel().add(new JLabel(" "), "span 2, height 16px!");
	}

	// TEXT FIELDS
	protected FormattedClickableTextField addTextInput(String title, String text){
		return addTextInput(title, text, "");
	}
	protected FormattedClickableTextField addTextInput(final String title, String val, String placement){
		return addTextInput(title, val, new FormatterDataType(), placement);
	}
	protected FormattedClickableTextField addTextInput(final String title, String val, FormatterBase formatter, String placement){
		final FormattedClickableTextField input = new FormattedClickableTextField(Project.getCurrent(), val, formatter);
		input.setAutocomplete(frame, connection);
		input.setBordered(true);
		input.addKeyListener(new KeyAdapter(){
			@Override
			public void keyTyped(KeyEvent ke) {
				Schedule.reInvokeInWorker(Schedule.Named.MIG_WIZARD_INPUT_NOTIFICATION, UIConstants.TYPE_TIMEOUT, () -> notifyChange(title, input.getText()));
			}
		});

		getPanel().add(new JLabel(title));
		getPanel().add(input, placement);

		NextFieldObserver.get(this).registerObserver(input);
		return input;
	}
	protected JTextField addPlainTextInput(String title, String text){
		return addPlainTextInput(title, text, "");
	}
	protected JTextField addPlainTextInput(final String title, String val, String placement){
		final JTextField input = new JTextField(val);
		input.addKeyListener(new KeyAdapter(){
			@Override
			public void keyTyped(KeyEvent ke) {
				Schedule.reInvokeInWorker(Schedule.Named.MIG_WIZARD_INPUT_NOTIFICATION, UIConstants.TYPE_TIMEOUT, () -> notifyChange(title, input.getText()));
			}
		});

		getPanel().add(new JLabel(title));
		getPanel().add(input, placement);

		NextFieldObserver.get(this).registerObserver(input);
		return input;
	}

	// PASSWORD FIELDS
	protected JPasswordField addPasswordInput(String title, String text){
		return addPasswordInput(title, text, "");
	}
	protected JPasswordField addPasswordInput(final String title, String val, String placement){
		final JPasswordField input = new JPasswordField(val);
		input.addKeyListener(new KeyAdapter(){
			@Override
			public void keyTyped(KeyEvent ke) {
				Schedule.reInvokeInWorker(Schedule.Named.MIG_WIZARD_INPUT_NOTIFICATION, UIConstants.TYPE_TIMEOUT, () -> notifyChange(title, new String(input.getPassword())));
			}
		});

		getPanel().add(new JLabel(title));
		getPanel().add(input, placement);

		NextFieldObserver.get(this).registerObserver(input);
		return input;
	}

	// NUMERIC FIELDS
	protected JSpinner addNumberInput(String title, int value, Point minMax){
		return addNumberInput(title, value, minMax, "");
	}
	protected JSpinner addNumberInput(final String title, int value, Point minMax, String placement){
		final JSpinner input = new JSpinner(new SpinnerNumberModel(value, minMax.x, minMax.y, minMax.y<100 ? 1 : 10));
		input.addChangeListener(ce -> Schedule.reInvokeInWorker(Schedule.Named.MIG_WIZARD_INPUT_NOTIFICATION, UIConstants.TYPE_TIMEOUT, () -> notifyChange(title, input.getValue().toString())));

		getPanel().add(new JLabel(title));
		getPanel().add(input, placement);

		NextFieldObserver.get(this).registerObserver(input);
		return input;
	}

	// PATH FIELDS
	protected FormattedClickableTextField addPathInput(String title, String text){
		return addPathInput(title, text, "");
	}
	protected FormattedClickableTextField addPathInput(final String title, String val, String placement){
		return addPathInput(title, val, placement, null);
	}
	protected FormattedClickableTextField addPathInput(final String title, String val, String placement, FileFilter typeFilter){
		final FormattedClickableTextField input = new FormattedClickableTextField(Project.getCurrent(), val, new FormatterDataType());
		input.setBordered(true);
		input.disableFinder();
		input.addKeyListener(new PathInputListener(title, input, typeFilter));
		//input.addMouseListener(new PathInputListener(title, input));

		JButton btnBrowse = new JButton("...");
		btnBrowse.addActionListener(new PathInputListener(title, input, typeFilter));

		getPanel().add(new JLabel(title));
		getPanel().add(new HorizontalContainer(null, input, btnBrowse), placement);

		NextFieldObserver.get(this).registerObserver(input);
		return input;
	}

	// CHECKBOXES
	protected JCheckBox addCheckbox(String title, boolean val){
		return addCheckbox(title, val, "");
	}
	protected JCheckBox addCheckbox(final String title, boolean val, String placement) {
		final JCheckBox input = new JCheckBox("", val);
		input.addActionListener(ae -> notifyChange(title, input.isSelected()));
		//cbComponent.setPreferredSize(new Dimension(cbComponent.getPreferredSize().width,14));

		getPanel().add(new JLabel(title));
		getPanel().add(input, placement);

		NextFieldObserver.get(this).registerObserver(input);
		return input;
	}

	// MULTIPLE CHECKBOXES
	protected void addCheckboxes(final String title, String[] options, boolean[] vals, boolean[] enabled){
		addCheckboxes(title, options, vals, enabled, "");
	}
	protected void addCheckboxes(final String title, String[] options, boolean[] vals, boolean[] enabled, String placement){
		final JPanel cont = new JPanel();
		for(int i=0; i<options.length; i++){
			final JCheckBox input = new JCheckBox(options[i]);
			input.addActionListener(ae -> {
                Component[] comps = cont.getComponents();
                boolean[] cbVals = new boolean[comps.length];
                for(int i1 = 0; i1 <comps.length; i1++){
                    if(comps[i1] instanceof JCheckBox){
                        cbVals[i1] = ((AbstractButton)comps[i1]).isSelected();
                    }
                }
                notifyChange(title, cbVals);
            });
			if(vals[i]){
				Schedule.inEDT(200, input::doClick);
			}
			if(i < options.length){
				input.setEnabled(enabled[i]);
			}
			//input.setPreferredSize(new Dimension(input.getPreferredSize().width,14));
			cont.add(input);

			//NextFieldObserver.get(this).registerObserver(input);
		}
		getPanel().add(new JLabel(title));
		getPanel().add(cont, placement);
	}

	// ANY CONTAINER
	protected void addPanel(Container panel) {
		addPanel(panel, SPAN);
	}
	protected void addPanel(Container panel, String placement) {
		getPanel().add(panel, placement);
	}
	protected void addPanel(String title, Container panel) {
		addPanel(title, panel, "");
	}
	protected void addPanel(String title, Container panel, String placement) {
		getPanel().add(new JLabel(title));
		getPanel().add(panel, placement);
	}

	// COMBOBOXES
	protected IconableComboBox addCombo(final String title, String[] options, String selectedOption) {
		return addCombo(title, options, selectedOption, "");
	}
	protected IconableComboBox addCombo(final String title, String[] options, String selectedOption, String placement) {
		int selected = -1;
		for(int i=0; i<options.length; i++){
			if(options[i].equals(selectedOption)){
				selected = i;
				break;
			}
		}
		final IconableComboBox input = new IconableComboBox(options);
		if (selected >= 0) {
			input.setSelectedIndex(selected);
		}
		input.addActionListener(e -> notifyChange(title, (String)input.getSelectedItem()));

		getPanel().add(new JLabel(title));
		getPanel().add(input, placement);

		NextFieldObserver.get(this).registerObserver(input);
		return input;
	}

	protected void setNextButton(String text, boolean isEnabled, int action){
		updateButton(text, isEnabled, new WizardListener(action));
	}
	protected void setNextButton(String text, boolean isEnabled, int action, String extra){
		updateButton(text, isEnabled, new WizardListener(action, extra));
	}

	private void updateButton(String text, boolean isEnabled, WizardListener listener) {
		removeSaveListeners();
		btnSave.setText(text);
		btnSave.setEnabled(isEnabled);
		btnSave.addActionListener(listener);
		setButtonsVisible(true);
	}

	protected void resetContent(){
		setPlacementPanel(null);
		pageContent.removeAll();
		pageContent.repaint();
		NextFieldObserver.get(this).clear();
		AutocompleteObserver.unregister(frame);
	}

	protected void executeAction(int type){
		if(type == MigWizard.CLOSE_WINDOW){
			frame.dispose();
		}else{
			Dbg.fixme("Wizard: unsupported action type: "+type);
		}
	}

	protected void executeAction(int type, String extra){
		if(type == MigWizard.CLOSE_WINDOW){
			frame.dispose();
		}else{
			Dbg.fixme("Wizard: unsupported action type: "+type+" extra: "+extra);
		}
	}


	public void close(){
		frame.dispose();
	}

	protected void setButtonsVisible(boolean b){
		if(buttonPane != null) {
			buttonPane.setVisible(b);
		}
	}

	void validate() {
		(placementPanel != null ? placementPanel : pageContent).validate();
	}

	protected void setTreeVisible(boolean b) {
		treeScroll.setVisible(b);
		JSplitPane splitPane = (JSplitPane)frame.getContentPane();
		splitPane.setResizeWeight(0d);
		if(b){
			splitPane.setDividerSize(3);
			splitPane.setDividerLocation(Config.DB_TREE_SPLIT_WIDTH + Settings.getInt(Settings.L_LAYOUT_WIZARD_TREE_W)*44);
		}else{
			splitPane.getLeftComponent().setMinimumSize(new Dimension(0,0));
			splitPane.setDividerSize(0);
			splitPane.setDividerLocation(0);
		}
	}

	public void clickSave(){
		btnSave.doClick();
	}

	public void clickCancel(){
		btnCancel.doClick();
	}

	public class WizardListener implements ActionListener {

		private final int type;
		private String extra;
		public WizardListener(int type) {
			this.type = type;
		}
		public WizardListener(int type, String extra) {
			this.type = type;
			this.extra = extra;
		}

		@Override
		public void actionPerformed(ActionEvent ae) {
			Schedule.inWorker(() -> {
                if(extra != null){
                    executeAction(type, extra);
                }else{
                    executeAction(type);
                }
            });
		}

	}

	private class PathInputListener implements ActionListener, MouseListener, KeyListener{
		private final UndoableTextField input;
		private final String inputName;
		private final FileFilter typeFilter;

		PathInputListener (String inputName, UndoableTextField input, FileFilter typeFilter) {
			this.input = input;
			this.inputName = inputName;
			this.typeFilter = typeFilter;
		}

		@Override
		public void actionPerformed(ActionEvent ae) {
			runAction();
		}

		@Override
		public void mouseClicked(MouseEvent me) {
			runAction();
		}

		@Override
		public void keyTyped (KeyEvent e) {
			Schedule.reInvokeInWorker(Schedule.Named.MIG_WIZARD_PATH_LISTENER, UIConstants.TYPE_TIMEOUT, () -> notifyChange(inputName, input.getText()));
		}

		@Override public void mousePressed(MouseEvent me) {}
		@Override public void mouseReleased(MouseEvent me) {}
		@Override public void mouseEntered(MouseEvent me) {}
		@Override public void mouseExited(MouseEvent me) {}
		@Override public void keyPressed (KeyEvent e) {}
		@Override public void keyReleased (KeyEvent e) {}

		private void runAction(){
			JFileChooser chooser = new JFileChooser(input.getText());
			chooser.setDialogTitle("Select import file");
			chooser.setMultiSelectionEnabled(false);
			if(typeFilter == null){
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				chooser.setAcceptAllFileFilterUsed(false);
			}else{
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				chooser.setFileFilter(typeFilter);
				chooser.setAcceptAllFileFilterUsed(true);
			}
			if(chooser.showOpenDialog(frame) == 0){
				input.setText(chooser.getSelectedFile().toString());
				notifyChange(inputName, input.getText());
			}
		}

	}
}
