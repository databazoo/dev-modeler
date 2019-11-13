
package com.databazoo.devmodeler.wizards.project;

import com.databazoo.components.GCFrame;
import com.databazoo.components.UIConstants;
import com.databazoo.components.WizardTree;
import com.databazoo.components.combo.IconableComboBox;
import com.databazoo.components.containers.HorizontalContainer;
import com.databazoo.components.containers.VerticalContainer;
import com.databazoo.components.icons.ProjectIconRenderer;
import com.databazoo.components.textInput.FormattedClickableTextField;
import com.databazoo.components.textInput.UndoableTextField;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.DBCommException;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.SearchPanel;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;
import com.databazoo.devmodeler.wizards.MigWizard;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.FileFilterFactory;
import com.databazoo.tools.Schedule;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.databazoo.components.table.EditableTable.L_DOUBLECLICK_TO_EDIT;


public class ProjectWizard extends MigWizard implements ActionListener {
	static final String EMPTY_PROJECT_NAME = "My test project";
	static final String EMPTY_DB_NAME = "My test database";

	private static final String L_STORE_REVISION_TO			= "Store revisions to";
	private static final String L_SHOW_CHANGE_NOTIFICATION	= "Show change notification";
	private static final String L_CHECKOUT_URL				= "Repository URL";
	private static final String L_CHECKOUT_USER				= "Username";
	private static final String L_CHECKOUT_PASS				= "Password";
	private static final String L_CHECKOUT_FOLDER			= "Checkout folder";
	private static final String L_REVISIONS_FOLDER			= "Revisions folder";
	private static final String L_AUTOCOMMIT				= "Commit";
	private static final String L_EXECUTABLE_GIT			= "GIT executable";
	private static final String L_EXECUTABLE_SVN			= "SVN executable";

	static final String L_SIMPLE_DATABASE_NAME				= "Database name ";
	static final String L_SIMPLE_PASS_TITLE 				= "Password ";
	static final String L_SIMPLE_USER						= "User ";
	static final String L_SIMPLE_HOST						= "Host ";
	static final String L_SIMPLE_PROJECT_NAME				= "Project name ";
	private static final String L_SIMPLE_DATABASE_TYPE		= "Database type ";
	static final String L_NEW_PROJECT						= "New Project";

	static ProjectWizard instance;

	public static synchronized ProjectWizard getInstance(){
		if(instance == null) {
			instance = new ProjectWizard();
		}else{
			instance.frame.toFront();
		}
		return instance;
	}

	public static void updateConnTable(){
		if(instance != null){
			final Point lastEditedCell = new Point(instance.tablesUI.dedicatedTable.getSelectedColumn(), instance.tablesUI.dedicatedTable.getSelectedRow());

			instance.tablesUI.connectionsTable.setModel(instance.tablesUI.connectionsTableModel);
			instance.tablesUI.connectionsTableModel.fireTableDataChanged();
			instance.tablesUI.dedicatedTableModel.fireTableDataChanged();
			if(instance.listedProject != null){
				ProjectManager.getInstance().saveProjects();
			}
			instance.tablesUI.dedicatedTable.changeSelection(lastEditedCell.y, lastEditedCell.x, false, false);
			if(instance.tablesUI.connectionsTableModel instanceof SimpleConnectionsTableModel){
				((SimpleConnectionsTableModel) instance.tablesUI.connectionsTableModel).checkStatus();
			}
		}
	}
	private String simpleProjectName = L_NEW_PROJECT;
	private JTextField simpleHostInput;
	private JTextField simpleUserInput;
	private JPasswordField simplePassInput;
	JTextField simpleDbInput;
	JLabel simpleIconLabel;
	JLabel simpleConnectionStatusLabel;
	JButton simpleDbChooseButton;

	final ProjectWizardUI ui;
	final ProjectWizardTables tablesUI;

	int projectType;

	Project listedProject;
	private UndoableTextField projectRevPathField;
	UndoableTextField projectNameField;
	final WizardTree wizardTree;
	private JPanel versionTab;
	private JPanel inputPanelManual, inputPanelGIT, inputPanelSVN;
	private JLabel revisionsFoundGIT, revisionsFoundSVN;
	//private FormattedClickableTextField revisionPathInput;
	private FormattedClickableTextField revisionPathSVN;
	private FormattedClickableTextField revisionPathGIT;
	private FormattedClickableTextField revisionURLSVN;
	private FormattedClickableTextField revisionURLGIT;
	private FormattedClickableTextField revisionUserSVN;
	private FormattedClickableTextField revisionUserGIT;
	private String outputStd;
	private String outputErr;

	private ProjectWizard(){
		super();
		wizardTree = new WizardTree(ProjectManager.getInstance().getTreeView(), new ProjectIconRenderer(), this);
		ui = new ProjectWizardUI(this);
		tablesUI = new ProjectWizardTables(this);
		drawWindow("Projects", wizardTree, Settings.getBool(Settings.L_MAXIMIZE_PROJECTS), false);

		ui.drawTreeButtons();
		drawTreeControls(ui.getTreeButtonPanel());

		prepareNewProjectElements();
		frame.addWindowListener(
			new WindowAdapter(){
				@Override public void windowClosed(WindowEvent e) {
					instance = null;
					checkEmptyProjectList();
				}
			}
		);
	}

	void checkEmptyProjectList(){
		ProjectManager pm = ProjectManager.getInstance();
		if(pm.getProjectList().isEmpty()){
			pm.createNew(EMPTY_PROJECT_NAME, Project.TYPE_ABSTRACT);
			Project p = pm.getProjectList().get(0);
			DB db = new DB(p, EMPTY_DB_NAME);
			p.getDatabases().add(db);
			pm.openCreatedProject();

			db.getSchemas().clear();
			p.loadDbFromXML(db, dumpResourceToFile("/emptyDB.xml", ProjectManager.getSettingsDirectory("emptyDB.xml")));
			p.setLoaded();
			Canvas.instance.drawProjectLater(true);
			SearchPanel.instance.updateDbTree();

			p.save();
		}
	}

	private File dumpResourceToFile (String resourceName, File file) {
		try {
			try (InputStream i = ProjectWizard.class.getResourceAsStream(resourceName)) {
				BufferedReader r = new BufferedReader(new InputStreamReader(i));
				StringBuilder sb = new StringBuilder();
				String line;
				while((line = r.readLine()) != null) {
					sb.append(line);
				}

				try (Writer writer = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(file), "utf-8"))) {
					writer.write(sb.toString());
				}

				return file;
			}
		} catch (IOException ex) {
			Dbg.fixme("File could not be created from given resource", ex);
		}
		return null;
	}

	private void prepareNewProjectElements(){
		ui.prepareNewProjectDbTypes();
		ui.prepareNewProjectServers();

		tablesUI.setConnTableCols();
		tablesUI.setDBsTableCols();
		tablesUI.setDedicatedTableCols();

		tablesUI.prepareNewProjectConnectionTable();
		tablesUI.prepareNewProjectDatabasesTable();
		tablesUI.dedicatedTable.setCellSelectionEnabled(true);

		wizardTree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, true), "removeSelectedProject");
		wizardTree.getActionMap().put("removeSelectedProject", new AbstractAction("del") { @Override public void actionPerformed(ActionEvent e) {
				removeSelectedProject();
		}
		});
		wizardTree.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent e) {
				int selRow = wizardTree.getClosestRowForLocation(e.getX(), e.getY());
				if(selRow > 0 && selRow <= ProjectManager.getInstance().getProjectList().size()) {
					if(e.getClickCount() == 2) {
						executeAction(OPEN_PROJECT, wizardTree.getClosestPathForLocation(e.getX(), e.getY()).getLastPathComponent().toString());
					}
				}
			}
		});
	}

	void removeSelectedProject(){
		if(listedProject != null){
			Object[] options = {"Remove",  "Cancel"};
			final Project selectedProject = listedProject;
			int n = GCFrame.SHOW_GUI ? JOptionPane.showOptionDialog(frame, "Remove project "+selectedProject.getProjectName()+"?", "Remove project", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]) : 0;
			if(n == 0){
				ProjectManager.getInstance().remove(selectedProject);
				wizardTree.assignNewModel(ProjectManager.getInstance().getTreeView(), 0);
			}
		}
	}

	private void loadNewProjectPageSimple(){
		listedProject = null;
		resetContent();
		tablesUI.dbsTableModel.reset();

		addTitle("Create a new project");
		addText("");
		addText("Welcome!<br><br>You are about to create a new project.<br>Please fill in all necessary data.", "span 2");
		addText("");

		addText("", "height 60px, span");

		addText("<h2>Select project name, database type and connection</h2>", "align center, span");

		addText("");
		addPanel(getSimpleWizardPanel(), "span 2, width 100%-160px!");
		addText("");

		addText("", "height 60px, span");


		//addDBTypeSelection();
		addText("<h2>or use advanced wizard</h2>", "align center, span");
		JLabel input = addText("<a href=#>Switch to advanced wizard</a>", "align center, span");
		input.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		input.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				loadNewProjectPage1();
				Settings.put(Settings.L_LAYOUT_PROJECT_SIMPLE, false);
				Settings.save();
			}
		});

		setNextButton("Create", true, CREATE_PROJECT_SIMPLE);
	}

	private JPanel getSimpleWizardPanel () {
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 1, wrap 3", "[40px::][100::80%,grow,fill]10[72px!,align center]", "[]1[]"));

		setPlacementPanel(panel);

		addPlainTextInput(L_SIMPLE_PROJECT_NAME, L_NEW_PROJECT, "");
		addPanel(simpleIconLabel = new JLabel(Theme.getLargeIcon(Theme.ICO_POSTGRESQL)), "spany 2");

		final IconableComboBox combo = addCombo(L_SIMPLE_DATABASE_TYPE, Project.getConnectionTypeListWithAbstract(), Project.L_ABSTRACT_MODEL, "");
		combo.addActionListener(e -> setSimpleDBType(combo.getSelectedItem().toString()));

		tablesUI.connectionsTableModel = new SimpleConnectionsTableModel(this);
		tablesUI.connectionsTableModel.finalizePresetConnections();

		addEmptyLine();

		simpleHostInput = addPlainTextInput(L_SIMPLE_HOST, "", "");
		addText("");

		simpleUserInput = addPlainTextInput(L_SIMPLE_USER, "", "");
		addText("");

		simplePassInput = addPasswordInput(L_SIMPLE_PASS_TITLE, "", "");
		simpleConnectionStatusLabel = addText("");

		addEmptyLine();

		simpleDbInput = addPlainTextInput(L_SIMPLE_DATABASE_NAME, "", "");
		addPanel(getSimpleDbChooseButton());

		setPlacementPanel(null);
		setSimpleDBType(Project.L_ABSTRACT_MODEL);

		return panel;
	}

	private JButton getSimpleDbChooseButton () {
		simpleDbChooseButton = new JButton("Choose");
		simpleDbChooseButton.setFocusable(false);
		simpleDbChooseButton.setEnabled(false);
		simpleDbChooseButton.addActionListener(e -> showDatabasesFromServer());
		return simpleDbChooseButton;
	}

	void setSimpleDBType(String type){
		SimpleConnectionsTableModel model = (SimpleConnectionsTableModel)tablesUI.connectionsTableModel;
		model.resetURL();
		simpleConnectionStatusLabel.setText("");
		switch (type){
			case Project.L_MYSQL:
				simpleIconLabel.setIcon(Theme.getLargeIcon(Theme.ICO_MYSQL));
				ui.myRadio.setSelected(true);
				projectType = Project.TYPE_MY;
				break;
			case Project.L_POSTGRESQL:
				simpleIconLabel.setIcon(Theme.getLargeIcon(Theme.ICO_POSTGRESQL));
				ui.pgRadio.setSelected(true);
				projectType = Project.TYPE_PG;
				break;
			case Project.L_MARIA_DB:
				simpleIconLabel.setIcon(Theme.getLargeIcon(Theme.ICO_MARIADB));
				ui.mariaRadio.setSelected(true);
				projectType = Project.TYPE_MARIA;
				break;
			default:
				simpleIconLabel.setIcon(Theme.getLargeIcon(Theme.ICO_ABSTRACT));
				ui.defRadio.setSelected(true);
				projectType = Project.TYPE_ABSTRACT;
				break;
		}
		tablesUI.connectionsTableModel.checkDBType();
		simpleHostInput.setText(model.getHost());
		simpleUserInput.setText(model.getUser());
		simplePassInput.setText(model.getPassword());

		simpleHostInput.setEnabled(!type.equals(Project.L_ABSTRACT_MODEL));
		simpleUserInput.setEnabled(!type.equals(Project.L_ABSTRACT_MODEL));
		simplePassInput.setEnabled(!type.equals(Project.L_ABSTRACT_MODEL));
		//simpleDbInput.setEnabled(!type.equals(Project.L_ABSTRACT_MODEL));
	}

	private void loadNewProjectPage1(){
		listedProject = null;
		resetContent();
		tablesUI.dbsTableModel.reset();

		addTitle("Create a new project");
		//addText("Welcome!<br><br>You are about to create a new project.<br>Please fill in all necessary data in several simple steps.", "span");
		addText("<h2>Select database type</h2>", "align center, span");
		addDBTypeSelection();

		JLabel input = addText("<a href=#>Switch to simple wizard</a>", "align center, span");
		input.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		input.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				loadNewProjectPageSimple();
				Settings.put(Settings.L_LAYOUT_PROJECT_SIMPLE, true);
				Settings.save();
			}
		});
		setNextButton("Next >", true, CREATE_PROJECT_PAGE_2);
	}

	private void loadNewProjectPage2(){
		resetContent();
		addTitle("Create a new project");
		addText("Choose your environment.", "span");
		addServerStrategySelection();

		tablesUI.connectionsTableModel = new ConnectionsTableModel();
		tablesUI.connectionsTable.setModel(tablesUI.connectionsTableModel);
		tablesUI.connectionsTableModel.setRows(Integer.parseInt(ui.connGroup.getSelection().getActionCommand()));

		tablesUI.connectionsTableModel.checkDBType();
		addConnectionsSelection();

		setNextButton("Next >", true, CREATE_PROJECT_PAGE_3);
	}

	private void loadNewProjectPage3(){
		resetContent();
		addTitle("Create a new project");
		//addText("Choose a name for your new project.");

		tablesUI.dedicatedTableModel.reset();
		tablesUI.dedicatedTableModel.fireTableStructureChanged();
		tablesUI.setDedicatedTableCols();

		if(listedProject == null && projectType != Project.TYPE_ABSTRACT){
			tablesUI.connectionsTableModel.finalizePresetConnections();
			showDatabasesFromServer();
		}

		addProjectSummaryAndNameSelection();
		addDBNameSelection();
		setNextButton("Create project", true, CREATE_PROJECT_FINAL);
	}

	void showDatabasesFromServer(){
		Schedule.inWorker(() -> {
            try {
				tablesUI.dbsTableModel.databases = showDatabasesSelectModal(tablesUI.connectionsTableModel.getFirstConnection().getDatabases());
				tablesUI.dbsTableModel.fireTableDataChanged();
                if(simpleDbInput != null){
                    simpleDbInput.setText(tablesUI.dbsTableModel.getDatabasesCommaSeparated());
                }
            } catch (DBCommException ex) {
                Dbg.notImportant("Database list could not be loaded. " + ex.getMessage(), ex);
            }
        });
	}

	private List<DB> showDatabasesSelectModal (List<DB> databases) {
		if(databases.size() < 2){
			return databases;
		}

		final JPanel p = new JPanel(new GridLayout(0, 1));
		final JCheckBox allCheckbox = new JCheckBox("ALL", true);
		allCheckbox.addActionListener(e -> {
            for(Component comp : p.getComponents()){
                if(comp instanceof JCheckBox){
                    JCheckBox cb = (JCheckBox) comp;
                    cb.setSelected(allCheckbox.isSelected());
                }
    }
        });
		if(databases.size() > 1){
			p.add(allCheckbox);
			p.add(new JLabel());
		}

		for(DB db : databases){
			p.add(new JCheckBox(db.getName(), true));
		}

		JOptionPane.showMessageDialog(frame, p, "Available databases", JOptionPane.INFORMATION_MESSAGE);

		List<DB> list = new CopyOnWriteArrayList<>();
		for(Component comp : p.getComponents()){
			if(comp instanceof JCheckBox){
				JCheckBox cb = (JCheckBox) comp;
				if(cb.isSelected() && !cb.getText().equals("ALL")){
					list.add(new DB(Project.getCurrent(), tablesUI.connectionsTableModel.getFirstConnection(), cb.getText()));
				}
			}
		}
		return list;
	}

	private void loadProjectPage(final String name) {
		listedProject = ProjectManager.getInstance().getProjectByName(name);

		// PROJECT NAME
		JLabel projectName = new JLabel("<html><h1>"+name+"</h1></html>", JLabel.CENTER);
		projectName.setToolTipText("Click to rename project");
		projectName.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		LineBorder lineBorder = new LineBorder(UIConstants.Colors.getSelectionBackground());
		EmptyBorder emptyBorder = new EmptyBorder(1, 1, 1, 1);
		projectName.setBorder(emptyBorder);

		projectName.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked (MouseEvent e) {
				String val = JOptionPane.showInputDialog(frame, "New name", name);
				if(val!=null && !val.isEmpty()){
					val = ProjectManager.getInstance().checkName(val);
					if(val != null){
						listedProject.setProjectName(val);
						ProjectManager.getInstance().saveProjects();

						wizardTree.assignNewModel(ProjectManager.getInstance().getTreeView());
						wizardTree.selectRow(val);

						DesignGUI.get().setTitle(Project.getCurrent().getProjectName());
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				projectName.setIcon(Theme.getSmallIcon(Theme.ICO_EDIT));
				projectName.setBorder(lineBorder);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				projectName.setIcon(null);
				projectName.setBorder(emptyBorder);
			}
		});

		// PROJECT SUMMARY
		JLabel projectSummary = getProjectSummary(listedProject.getType(), listedProject.getDatabases().size(), listedProject.getConnections().size(), listedProject.getLastOpenString(), listedProject.getVerType());
		JPanel infoPanel = new VerticalContainer(projectName, projectSummary, null);

		// DATABASE LIST
		tablesUI.connectionsTableModel.setConnections(listedProject.getConnections());
		JComponent dbPanel = new JScrollPane(tablesUI.dbsTable);
		tablesUI.dbsTableModel.databases = listedProject.getDatabases();

		// DEDICATED CONNECTIONS
		tablesUI.dedicatedTableModel.reset();
		tablesUI.dedicatedTableModel.fireTableStructureChanged();
		tablesUI.setDedicatedTableCols();
		tablesUI.dedicatedTableModel.dedicatedConnections = listedProject.getDedicatedConnections();

		// TABS
		JTabbedPane tabs = new JTabbedPane();

		// SERVERS TAB
		if(listedProject.getType() != Project.TYPE_ABSTRACT) {
			final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			splitPane.setResizeWeight(1.0d);

			final JCheckBox dedicatedCB = new JCheckBox("Configure separate setting for each database and connection", false);
			dedicatedCB.addActionListener(ae -> dedicatedCBToggled(dedicatedCB, splitPane));
			JPanel dedicatedCBPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
			dedicatedCBPanel.add(dedicatedCB);

			splitPane.add(new VerticalContainer(
					null,
					new HorizontalContainer.Builder()
							.center(new JScrollPane(tablesUI.connectionsTable))
							.right(new VerticalContainer.Builder()
									.top(ui.createConnectionsTableButtonPanel())
									.center(new JLabel())
									.build())
							.build(),
					new JLabel("<html><i>&nbsp;Note: doubleclick a cell to edit it's content. Hit DELETE to remove a line.</i></html>")
			));
			splitPane.add(new VerticalContainer(dedicatedCBPanel, new JScrollPane(tablesUI.dedicatedTable), null));
			tabs.add("Servers", splitPane);

			Schedule.inEDT(() -> dedicatedCBToggled(dedicatedCB, splitPane));
		}

		// VERSIONING TAB
		versionTab = new JPanel(new BorderLayout(0,0));
		tabs.add("Versioning", versionTab);

		// VERSIONING RADIO BUTTONS
		JPanel radioPanel = new JPanel(new GridLayout(1, 0, 0, 0));
		radioPanel.setBorder(new EmptyBorder(32, 100, 16, 0));

		ButtonGroup group = new ButtonGroup();
		JRadioButton btn = new JRadioButton("SVN", listedProject.getVerType().equals(Project.VERSIONING_SVN));
		btn.addActionListener(e -> setVersioningType(Project.VERSIONING_SVN));
		//btn.setEnabled(false);
		group.add(btn);
		radioPanel.add(btn);

		btn = new JRadioButton("GIT", listedProject.getVerType().equals(Project.VERSIONING_GIT));
		btn.addActionListener(e -> setVersioningType(Project.VERSIONING_GIT));
		//btn.setEnabled(false);
		group.add(btn);
		radioPanel.add(btn);

		btn = new JRadioButton("Manual commit and update", listedProject.getVerType().equals(Project.VERSIONING_MANUAL));
		btn.addActionListener(e -> setVersioningType(Project.VERSIONING_MANUAL));
		group.add(btn);
		radioPanel.add(btn);

		versionTab.add(radioPanel, BorderLayout.NORTH);

		// VERSIONING CONFIGURATION PANELS
		drawManualConfigPanel();
		drawGITConfigPanel();
		drawSVNConfigPanel();

		setVersioningType(listedProject.getVerType());

		// DRAW CONTENT
		resetContent();

		addPanel(infoPanel, "span 2, grow, align center");
		addPanel(dbPanel, "span 2, height 150px!");
		addPanel(tabs, "span, height 100%, grow");

		setNextButton("Open project", project==null || !project.getProjectName().equals(name), OPEN_PROJECT, name);
	}

	@Override
	public void valueChanged(final TreeSelectionEvent tse) {
		Schedule.inEDT(() -> {
			try {
				String projectName = tse.getNewLeadSelectionPath().getLastPathComponent().toString();
				if (projectName.equals(ProjectManager.L_CREATE_NEW_PROJECT) || projectName.equals(ProjectManager.L_PROJECTS)) {
					if (Settings.getBool(Settings.L_LAYOUT_PROJECT_SIMPLE)) {
						loadNewProjectPageSimple();
					} else {
						loadNewProjectPage1();
					}
				} else {
					loadProjectPage(projectName);
				}
				ui.checkTreeButtons();
			} catch (Exception e) {
				Dbg.notImportant("Project Wizard value change failed", e);
				Schedule.inEDT(Schedule.CLICK_DELAY, () -> valueChanged(tse));
			}
		});
	}

	private void addDBTypeSelection() {
		JPanel selPane = new JPanel(new GridLayout(0,3,10,0));
		selPane.setBorder(new EmptyBorder(0, 30, 0, 0));

		selPane.add(ui.getRadioContainer(ui.myRadio, Config.SHOW_LARGE_ICONS_IN_PROJECT ? ProjectIconRenderer.MyIconLarge : ProjectIconRenderer.MyIcon));
		selPane.add(ui.getRadioContainer(ui.mariaRadio, Config.SHOW_LARGE_ICONS_IN_PROJECT ? ProjectIconRenderer.MariaIconLarge : ProjectIconRenderer.MariaIcon));
		selPane.add(ui.getRadioContainer(ui.pgRadio, Config.SHOW_LARGE_ICONS_IN_PROJECT ? ProjectIconRenderer.PgIconLarge : ProjectIconRenderer.PgIcon));

		selPane.add(new JLabel("<html>Simple database with<br>it's fast MyISAM engine.<br>Perfectly suitable for<br>front-end applications.<br></html>"));
		selPane.add(new JLabel("<html>An enhanced, drop-in<br>replacement for MySQL.<br><!--Fully open-source MySQL fork.--><br><br></html>"));
		selPane.add(new JLabel("<html>The world's most advanced<br>open source database.<br>Clear choice for large projects.<br><br></html>"));

		selPane.add(new JLabel());
		selPane.add(new JLabel());
		selPane.add(new JLabel());

		selPane.add(new JLabel());
		selPane.add(new JLabel());
		selPane.add(ui.getRadioContainer(ui.defRadio, Config.SHOW_LARGE_ICONS_IN_PROJECT ? ProjectIconRenderer.DefIconLarge : ProjectIconRenderer.DefIcon));

		selPane.add(new JLabel());
		selPane.add(new JLabel());
		selPane.add(new JLabel("<html>Design a model with no<br>database connection.<br><br><br></html>"));

		addPanel(selPane, "span, grow");
	}

	private void addServerStrategySelection() {
		JPanel selPane = new JPanel(new GridLayout(0,4,6,0));
		selPane.setBorder(new EmptyBorder(0, 30, 0, 0));
		selPane.add(ui.conn1Radio);
		selPane.add(ui.conn2Radio);
		selPane.add(ui.conn3Radio);
		selPane.add(ui.conn4Radio);
		selPane.add(new JLabel("<html>I only need to<br>access data in<br>my database and<br>optimize it.</html>"));
		selPane.add(new JLabel("<html>I have<br>a devel. server and<br>a production server.<br><br></html>"));
		selPane.add(new JLabel("<html>I have<br>a devel. server,<br>a test server and<br>a production server.</html>"));
		selPane.add(new JLabel("<html>I have a more<br>complex server<br>infrastructure.</html>"));
		addPanel(selPane, "span, grow");
	}

	private void addConnectionsSelection(){
		addText("<h2>Server connections</h2>", SPAN_CENTER);
		addPanel(new JScrollPane(tablesUI.connectionsTable), "height 91px::, grow, span");
		addText(L_DOUBLECLICK_TO_EDIT, SPAN);
	}

	private void addProjectSummaryAndNameSelection(){
		JLabel nameLabel = new JLabel("Project name");
		nameLabel.setPreferredSize(new Dimension(120, 20));

		projectNameField = new UndoableTextField(false);
		projectNameField.disableFinder();
		projectNameField.setBordered(true);
		projectNameField.setText(listedProject != null ? listedProject.getProjectName() : L_NEW_PROJECT);
		if(listedProject != null){
			projectNameField.addKeyListener(new KeyAdapter(){
				@Override
				public void keyTyped(KeyEvent ke) {
					Schedule.reInvokeInWorker(Schedule.Named.PROJECT_WIZARD_INPUT_LISTENER, UIConstants.TYPE_TIMEOUT, () -> {
                        listedProject.setProjectName(projectNameField.getText());
                        ProjectManager.getInstance().saveProjects();
                    });
				}
			});
		}

		JPanel inputPane = new JPanel(new GridLayout(0, 1));
		inputPane.setBorder(new EmptyBorder(5, 40, 5, 0));
		inputPane.add(new HorizontalContainer(nameLabel, projectNameField, null));

		JLabel revPathLabel = new JLabel("Store revisions to");
		revPathLabel.setPreferredSize(new Dimension(120, 20));
		inputPane.add(new HorizontalContainer(revPathLabel, drawProjectRevPath(), null));

		JLabel projectSummary;
		if(listedProject != null){
			projectSummary = getProjectSummary(listedProject.getType(), listedProject.getDatabases().size(), listedProject.getConnections().size(), listedProject.getLastOpenString(), listedProject.getVerType());
		}else{
			projectSummary = getProjectSummary(projectType, 1, tablesUI.connectionsTableModel.getRowCount(), "Never", Project.VERSIONING_MANUAL);
		}

		addPanel(projectSummary, "");
		addPanel(inputPane, "span, width :65%:");
		addText("", "span");
	}

	private JComponent drawProjectRevPath(){
		projectRevPathField = new UndoableTextField(false);
		projectRevPathField.disableFinder();
		//projectRevPathField.setBordered(true);

		if(listedProject != null){
			projectRevPathField.addKeyListener(new KeyAdapter(){
				@Override
				public void keyTyped(KeyEvent ke) {
					Schedule.reInvokeInWorker(Schedule.Named.PROJECT_WIZARD_INPUT_LISTENER, UIConstants.TYPE_TIMEOUT, () -> {
                        listedProject.setRevPath(projectRevPathField.getText());
                        ProjectManager.getInstance().saveProjects();
                    });
				}
			});
		}else{
			projectNameField.addKeyListener(new KeyAdapter(){
				@Override
				public void keyTyped(KeyEvent ke) {
					Schedule.reInvokeInWorker(Schedule.Named.PROJECT_WIZARD_INPUT_LISTENER, UIConstants.TYPE_TIMEOUT, ProjectWizard.this::checkRevisionFolder);
				}
			});
		}

		JButton btnRevPath = new JButton("...");
		btnRevPath.addActionListener(ae -> {
            File dir = new File(projectRevPathField.getText());
            if(!dir.exists() || !dir.isDirectory()){
                dir = ProjectManager.getSettingsDirectory();
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(dir);
            chooser.setDialogTitle("Store revisions to");
            chooser.setMultiSelectionEnabled(false);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            if(chooser.showOpenDialog(frame) == 0){
                projectRevPathField.setText(chooser.getSelectedFile().toString());
                if(listedProject != null){
                    listedProject.setRevPath(projectRevPathField.getText());
                    ProjectManager.getInstance().saveProjects();
                }
            }
        });

		if(listedProject != null){
			projectRevPathField.setText(listedProject.getRevPath());
		}else{
			checkRevisionFolder();
		}

		JScrollPane scrl = new JScrollPane(new HorizontalContainer(null, projectRevPathField, null));
		scrl.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
		scrl.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));
		return new HorizontalContainer(null, scrl, btnRevPath);
	}

	private void checkRevisionFolder(){
		projectRevPathField.setText(new File(ProjectManager.getSettingsDirectory(projectNameField.getText()), "revisions").toString());
	}

	private JLabel getProjectSummary(int type, int databasesCnt, int serversCnt, String lastOpen, String versioning) {
		return new JLabel("<html>"+
				"Type: "+Project.getConnectionTypeString(type)+"<br>"+
				(type != Project.TYPE_ABSTRACT ? "Servers: "+serversCnt+"<br>" : "")+
				"Databases: "+databasesCnt+"<br>"+
				"Last open: "+lastOpen+"<br>"+
				"Versioning: "+versioning+
				"</html>",
				ProjectIconRenderer.getIconForType(type), JLabel.CENTER);
	}

	private void addDBNameSelection(){
		/*if(listedProject == null) {
			addText("<h2>Databases</h2>", "align center, span");
		}else{
			addText("", "span");
		}*/

		int heightFix = UIConstants.isWindows() ? -40 : 0;
		int pType = listedProject != null ? listedProject.getType() : projectType;
		if(pType == Project.TYPE_ABSTRACT){
			addPanel(new JScrollPane(tablesUI.dbsTable), "span, height :"+(listedProject == null ? 340+heightFix : 360+heightFix)+":, grow");
			addText(L_DOUBLECLICK_TO_EDIT, SPAN);
			addText("", SPAN);
		}else{
			final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

			final JCheckBox dedicatedCB = new JCheckBox("Configure separate setting for each database and connection", false);
			dedicatedCB.addActionListener(ae -> dedicatedCBToggled(dedicatedCB, splitPane));
			JPanel dedicatedCBPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
			dedicatedCBPanel.add(dedicatedCB);

			splitPane.add(new VerticalContainer(null, new JScrollPane(tablesUI.dbsTable), new JLabel("<html><i>&nbsp;Note: doubleclick a cell to edit it's content. Hit DELETE to remove a line.</i></html>")));
			splitPane.add(new VerticalContainer(dedicatedCBPanel, new JScrollPane(tablesUI.dedicatedTable), null));
			addPanel(splitPane, "span, height 100%, grow");

			Schedule.inEDT(Schedule.CLICK_DELAY, () -> dedicatedCBToggled(dedicatedCB, splitPane));
		}
	}

	private void dedicatedCBToggled(JCheckBox dedicatedCB, JSplitPane splitPane){
		if(!dedicatedCB.isSelected()){
			splitPane.setDividerSize(0);
			splitPane.setDividerLocation(splitPane.getHeight()-20);
		}else{
			splitPane.setDividerSize(5);
			splitPane.setDividerLocation(splitPane.getHeight()/3+20);
		}
	}

	private void createProject(){
		Project p = ProjectManager.getInstance().createNew(projectNameField.getText(), projectType);
		p.setLoaded();
		ProjectManager.getInstance().saveProjects();
		if(projectRevPathField == null){
			projectRevPathField = new UndoableTextField();
			projectRevPathField.disableFinder();
			checkRevisionFolder();
		}
		p.setProjectPath(ProjectManager.getSettingsDirectory(projectNameField.getText()).toString());
		p.setRevPath(projectRevPathField.getText());
		if(projectType != Project.TYPE_ABSTRACT){
			tablesUI.connectionsTableModel.finalizePresetConnections();
			p.getConnections().addAll(tablesUI.connectionsTableModel.conns);
		}
		p.setDatabases(tablesUI.dbsTableModel.databases);
		for(DB db : p.getDatabases()){
			db.setProject(p);
		}
		p.setDedicatedConnections(tablesUI.dedicatedTableModel.dedicatedConnections);
		p.save();
		ProjectManager.getInstance().saveProjects();
	}

	private void createSimpleProject(){
		Project p = ProjectManager.getInstance().createNew(simpleProjectName, projectType);
		p.setLoaded();
		ProjectManager.getInstance().saveProjects();

		projectRevPathField = new UndoableTextField();
		projectNameField = new UndoableTextField(simpleProjectName);
		checkRevisionFolder();

		p.setProjectPath(ProjectManager.getSettingsDirectory(simpleProjectName).toString());
		p.setRevPath(projectRevPathField.getText());
		p.setDatabases(tablesUI.dbsTableModel.databases);
		for(DB db : p.getDatabases()){
			db.setProject(p);
		}
		if(projectType != Project.TYPE_ABSTRACT){
			p.getConnections().addAll(tablesUI.connectionsTableModel.conns);
			setDedicatedConnections(p);
		}
		p.save();
		ProjectManager.getInstance().saveProjects();
	}

	private void setDedicatedConnections(Project p) {
		HashMap<String, IConnection> dedicatedConnections = new HashMap<>();
		for(DB db : p.getDatabases()){
			for(IConnection con : p.getConnections()) {
				con.setDbAlias(db.getName());
				dedicatedConnections.put(db.getName()+"~"+con.getName(), con);
			}
		}
		p.setDedicatedConnections(dedicatedConnections);
	}

	@Override
	protected void executeAction(int type)
	{
		switch(type){
			case CLOSE_WINDOW:
				frame.dispose();
				break;

			case CREATE_PROJECT_FINAL:
				createProject();
				frame.dispose();
				ProjectManager.getInstance().openCreatedProject();
				break;

			case CREATE_PROJECT_SIMPLE:
				createSimpleProject();
				frame.dispose();
				ProjectManager.getInstance().openCreatedProject();
				break;

			case CREATE_PROJECT_PAGE_2:
				if(ui.myRadio.isSelected()){
					projectType = Project.TYPE_MY;
				}else if(ui.pgRadio.isSelected()){
					projectType = Project.TYPE_PG;
				}else if(ui.mariaRadio.isSelected()){
					projectType = Project.TYPE_MARIA;
				}else{
					projectType = Project.TYPE_ABSTRACT;
				}

				if(ui.defRadio.isSelected()){
					loadNewProjectPage3();
				}else{
					loadNewProjectPage2();
				}
				break;

			case CREATE_PROJECT_PAGE_3:
				loadNewProjectPage3();
				break;
		}
	}

	@Override
	protected void executeAction(int type, final String extra)
	{
		if(type == OPEN_PROJECT){
			setNextButton("Loading...", false, OPEN_PROJECT);
			Schedule.inWorker(Schedule.CLICK_DELAY, () -> {
                ProjectManager.getInstance().openProject(extra);
                frame.dispose();
            });
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		tablesUI.connectionsTableModel.setRows(Integer.parseInt(ae.getActionCommand()));
	}

	@Override
	public void notifyChange (String elementName, String value) {
		switch (elementName) {
			case L_STORE_REVISION_TO:
			case L_REVISIONS_FOLDER:
				if(listedProject != null){
					listedProject.setRevPath(value);
					ProjectManager.getInstance().saveProjects();
					if(!listedProject.getVerType().equals(Project.VERSIONING_MANUAL)){
						runRevisionSearch(listedProject.getVerType().equals(Project.VERSIONING_GIT));
					}
				}
				break;

			case L_CHECKOUT_URL:
				if(listedProject != null){
					listedProject.setVerURL(value);
					ProjectManager.getInstance().saveProjects();
					checkVerPath(listedProject.getVerType().equals(Project.VERSIONING_GIT));
				}
				break;

			case L_CHECKOUT_USER:
				if(listedProject != null){
					listedProject.setVerUser(value);
					ProjectManager.getInstance().saveProjects();
					checkVerPath(listedProject.getVerType().equals(Project.VERSIONING_GIT));
				}
				break;

			case L_CHECKOUT_PASS:
				if(listedProject != null){
					listedProject.setVerPass(value);
					ProjectManager.getInstance().saveProjects();
					checkVerPath(listedProject.getVerType().equals(Project.VERSIONING_GIT));
				}
				break;

			case L_CHECKOUT_FOLDER:
				if(listedProject != null){
					listedProject.setVerPath(value);
					ProjectManager.getInstance().saveProjects();
					checkVerPath(listedProject.getVerType().equals(Project.VERSIONING_GIT));
				}
				break;

			case L_AUTOCOMMIT:
				if(listedProject != null){
					listedProject.setVerCommitMode(value);
					ProjectManager.getInstance().saveProjects();
				}
				break;

			case L_EXECUTABLE_GIT:
				if(listedProject != null){
					listedProject.setVerExecutableGIT(value);
					ProjectManager.getInstance().saveProjects();
					checkVerPath(listedProject.getVerType().equals(Project.VERSIONING_GIT));
				}
				break;

			case L_EXECUTABLE_SVN:
				if(listedProject != null){
					listedProject.setVerExecutableSVN(value);
					ProjectManager.getInstance().saveProjects();
					checkVerPath(listedProject.getVerType().equals(Project.VERSIONING_GIT));
				}
				break;

			case L_SIMPLE_PROJECT_NAME:
				simpleProjectName = value;
				break;
			case L_SIMPLE_DATABASE_TYPE:
				break;
			case L_SIMPLE_HOST:
				((SimpleConnectionsTableModel)tablesUI.connectionsTableModel).setHost(value);
				break;
			case L_SIMPLE_USER:
				((SimpleConnectionsTableModel)tablesUI.connectionsTableModel).setUser(value);
				break;
			case L_SIMPLE_PASS_TITLE:
				((SimpleConnectionsTableModel)tablesUI.connectionsTableModel).setPassword(value);
				break;
			case L_SIMPLE_DATABASE_NAME:
				tablesUI.dbsTableModel.setDatabasesCommaSeparated(value);
				break;
		}
	}

	@Override
	public void notifyChange (String elementName, boolean value) {
		if(elementName.equals(L_SHOW_CHANGE_NOTIFICATION)){
			if(listedProject != null){
				listedProject.showChangeNote = value;
				ProjectManager.getInstance().saveProjects();
			}
		}
	}

	@Override
	public void notifyChange (String elementName, boolean[] values) {
		//throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private void setVersioningType(String type){
		versionTab.remove(inputPanelGIT);
		versionTab.remove(inputPanelSVN);
		versionTab.remove(inputPanelManual);
		switch(type){
			case Project.VERSIONING_GIT:
				versionTab.add(inputPanelGIT, BorderLayout.CENTER);
				checkVerPath(true);
				break;
			case Project.VERSIONING_SVN:
				versionTab.add(inputPanelSVN, BorderLayout.CENTER);
				checkVerPath(false);
				break;
			default:
				versionTab.add(inputPanelManual, BorderLayout.CENTER);
		}
		versionTab.repaint();
		listedProject.setVerType(type);
		ProjectManager.getInstance().saveProjects();
	}

	private void drawManualConfigPanel(){
		inputPanelManual = createPlacementPanel();
		addPathInput(L_REVISIONS_FOLDER, listedProject.getRevPath(), SPAN);
		addText(""); addComment("&nbsp;All your changes will be stored here. This folder should be under revision control.", SPAN);
		addEmptyLine();
		addCheckbox(L_SHOW_CHANGE_NOTIFICATION, listedProject.showChangeNote, SPAN);
		addText(""); addComment("&nbsp;Show \"Do not forget to commit\" message on project close?", SPAN);
	}

	private void drawGITConfigPanel(){
		inputPanelGIT = createPlacementPanel();

		revisionURLGIT = addTextInput(L_CHECKOUT_URL, listedProject.getVerURL(), SPAN);
		//addEmptyLine();

		revisionUserGIT = addTextInput(L_CHECKOUT_USER, listedProject.getVerUser());
		addPasswordInput(L_CHECKOUT_PASS, listedProject.getVerPass());
		addEmptySlot();
		//addComment("Status");
		//addComment("&nbsp;OK");
		addComment("Leave empty if you use ssh key or OS-wide certificate", SPAN2);
		addEmptyLine();

		addPathInput(L_CHECKOUT_FOLDER, listedProject.getVerPath(), SPAN);
		//addEmptyLine();

		revisionPathGIT = addPathInput(L_REVISIONS_FOLDER, listedProject.getRevPath(), SPAN);
		addText(""); revisionsFoundGIT = addComment("&nbsp;0 revisions found", SPAN);
		addEmptyLine();

		addCombo(L_AUTOCOMMIT, new String[]{Project.COMMIT_ON_CLOSE_ASK, Project.COMMIT_ON_CLOSE_SILENT, Project.COMMIT_AFTER_CHANGE}, listedProject.getVerCommitMode(), SPAN);
		addEmptyLine();

		addPathInput(L_EXECUTABLE_GIT, listedProject.getVerExecutableGIT(), SPAN, FileFilterFactory.getExecutableFilter());
		addPanel(new JLabel(), "");
		addComment("&nbsp;Provide full path if \"git\" does not work for you", SPAN);
	}

	private void drawSVNConfigPanel(){
		inputPanelSVN = createPlacementPanel();

		revisionURLSVN = addTextInput(L_CHECKOUT_URL, listedProject.getVerURL(), SPAN);
		//addEmptyLine();

		revisionUserSVN = addTextInput(L_CHECKOUT_USER, listedProject.getVerUser());
		addPasswordInput(L_CHECKOUT_PASS, listedProject.getVerPass());
		addEmptySlot();
		//addComment("Status");
		//addComment("&nbsp;OK");
		addComment("Leave empty if you use ssh key or OS-wide certificate", SPAN2);
		addEmptyLine();

		addPathInput(L_CHECKOUT_FOLDER, listedProject.getVerPath(), SPAN);
		//addEmptyLine();

		revisionPathSVN = addPathInput(L_REVISIONS_FOLDER, listedProject.getRevPath(), SPAN);
		addText(""); revisionsFoundSVN = addComment("&nbsp;0 revisions found", SPAN);
		addEmptyLine();

		addCombo(L_AUTOCOMMIT, new String[]{Project.COMMIT_ON_CLOSE_ASK, Project.COMMIT_ON_CLOSE_SILENT, Project.COMMIT_AFTER_CHANGE}, listedProject.getVerCommitMode(), SPAN);
		addEmptyLine();

		addPathInput(L_EXECUTABLE_SVN, listedProject.getVerExecutableSVN(), SPAN, FileFilterFactory.getExecutableFilter());
		addPanel(new JLabel(), "");
		addComment("&nbsp;Provide full path if \"svn\" does not work for you", SPAN);
	}

	private void checkVerPath(final boolean isGIT){
		if(!listedProject.getRevPath().startsWith(listedProject.getVerPath())){
			if(isGIT) {
				revisionPathGIT.setText(listedProject.getVerPath());
			}else{
				revisionPathSVN.setText(listedProject.getVerPath());
			}
			listedProject.setRevPath(listedProject.getVerPath());
		}
		Schedule.inWorker(() -> {
			runVerPathCheck(isGIT);
			runRevisionSearch(isGIT);
		});
	}

	private void runVerPathCheck(final boolean isGIT){
		String call;
		if(isGIT){
			call = listedProject.getVerExecutableGIT()+" status";
		}else{
			call = listedProject.getVerExecutableSVN()+" --username "+listedProject.getVerUser()+(listedProject.getVerPass()==null ? "" : " --password "+listedProject.getVerPass())+" --non-interactive status";
		}
		File dir = new File(listedProject.getVerPath());
		if (!dir.exists()){
			if(JOptionPane.showConfirmDialog(frame, listedProject.getVerPath()+" does not exist yet.\n\nCreate it now?", listedProject.getVerPath()+" does not exist", JOptionPane.YES_NO_OPTION)==0) {
				dir.mkdirs();
			}else{
				return;
			}
		}

		int res = testSystemCall(call, new File(listedProject.getVerPath()));

		// PROGRAM NOT FOUND
		if(res == 1001){
			JOptionPane.showMessageDialog(frame, (isGIT ? "GIT" : "SVN")+" could not be found on your machine.\n\nPlease make sure "+(isGIT ? "GIT" : "SVN")+" is installed correctly", (isGIT ? "GIT" : "SVN")+" not found", JOptionPane.ERROR_MESSAGE);

		// NO CHECKOUT - TRY ONE NOW?
		}else if(res == 128 || outputErr.contains("W155007")){
			if(!listedProject.isVerURLNull() && JOptionPane.showConfirmDialog(frame, "Folder "+listedProject.getVerPath()+" is not yet under revision control.\n\nRun checkout from "+listedProject.getVerURL()+" now?", "Folder is not under revision control", JOptionPane.YES_NO_OPTION)==0) {
				listedProject.runCheckout();
			}

		// LOAD CONFIG FROM EXISTING CHECKOUT
		}else if(res == 0){
			if(listedProject.isVerURLNull()){
				if(isGIT) {
					try {
						File gitDir = new File(listedProject.getVerPath());
						while(!new File(gitDir,".git").exists()){
							gitDir = gitDir.getParentFile();
						}
						File configFile = new File(new File(gitDir,".git"), "config");
						if(configFile.isFile()) {
							parseRepoURL(Files.readAllLines(configFile.toPath(), Charset.defaultCharset()).toArray(new String[100]), isGIT);
						}

					} catch (IOException e){
						Dbg.notImportant("This should be handled properly. Ignoring for now.", e);
					}
				}else{
					testSystemCall(listedProject.getVerExecutableSVN()+" info", new File(listedProject.getVerPath()));
					parseRepoURL(outputStd.split("[\r\n]+"), isGIT);
				}
			}
		}
	}

	private void parseRepoURL(String[] lines, boolean isGIT){
		String regex = isGIT ? ".*url *= *(.+)" : "URL: *([^\r\n]+).*";
		for(String line : lines){
			if(line.matches(regex)){
				String url = line.replaceAll(regex, "$1");
				if(isGIT) {
					revisionURLGIT.setText(url);
				} else {
					revisionURLSVN.setText(url);
				}
				listedProject.setVerURL(url);

				if(url.matches("([^/]+://)?([^/@]+)@.+")){
					String username = url.replaceAll("([^/]+://)?([^/@]+)@.+", "$2");
					if(isGIT){
						revisionUserGIT.setText(username);
					}else{
						revisionUserSVN.setText(username);
					}
					listedProject.setVerUser(username);
				}
				break;
			}
		}
	}

	private void runRevisionSearch(boolean isGIT){
		// TODO: search recursively in subfolders

		int count = 0;
		File[] revFiles = new File(listedProject.getRevPath()).listFiles();
		if(revFiles != null) {
			for (File revFile : revFiles) {
				if (revFile.getName().endsWith(".xml")){
					count++;
				}
			}
		}
		if(isGIT){
			revisionsFoundGIT.setText("<html><font color=gray>&nbsp;"+count+" revisions found</font></html>");
		}else{
			revisionsFoundSVN.setText("<html><font color=gray>&nbsp;"+count+" revisions found</font></html>");
		}
	}

	private int testSystemCall(String call, File dir){
		String s;
		outputStd = "";
		outputErr = "";
		try {
			Process p = Runtime.getRuntime().exec(call, new String[0], dir);

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

			while ((s = stdInput.readLine()) != null) {
				Dbg.info(s);
				outputStd += s+"\n";
			}

			while ((s = stdError.readLine()) != null) {
				Dbg.info(s);
				outputErr += s+"\n";
			}

			return p.waitFor();
		}
		catch (IOException ex) {
			Dbg.fixme("Call "+call+" failed", ex);
			if(ex.getMessage().contains("Cannot run program")){
				return 1001;
			}else{
				return 1002;
			}
		} catch (Exception ex) {
			Dbg.fixme("System call failed", ex);
		}
		return 1003;
	}
}
