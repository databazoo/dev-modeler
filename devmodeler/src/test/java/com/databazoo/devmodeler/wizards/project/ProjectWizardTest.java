package com.databazoo.devmodeler.wizards.project;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.ConnectionMy;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.gui.CanvasTest;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.databazoo.devmodeler.wizards.MigWizard.CLOSE_WINDOW;
import static com.databazoo.devmodeler.wizards.project.ProjectWizard.CREATE_PROJECT_SIMPLE;
import static com.databazoo.devmodeler.wizards.project.ProjectWizard.L_NEW_PROJECT;
import static com.databazoo.devmodeler.wizards.project.ProjectWizard.L_SIMPLE_DATABASE_NAME;
import static com.databazoo.devmodeler.wizards.project.ProjectWizard.L_SIMPLE_HOST;
import static com.databazoo.devmodeler.wizards.project.ProjectWizard.L_SIMPLE_PASS_TITLE;
import static com.databazoo.devmodeler.wizards.project.ProjectWizard.L_SIMPLE_PROJECT_NAME;
import static com.databazoo.devmodeler.wizards.project.ProjectWizard.L_SIMPLE_USER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProjectWizardTest extends TestProjectSetup {

    private static final String TEST_PROJECT = "testProject";
    private static final String TEST_HOST = "testHost";
    private static final String TEST_USER = "testUser";
    private static final String TEST_PASSWORD = "testPassword";
    private static final String TEST_DATABASE_NAME = "testDatabaseName";

    @BeforeClass
    public static void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Before
    public void init() {
        setProjectUp();
        finalizeData();
        Project.getCurrent().setCurrentDB(database);
        CanvasTest.initializeCanvas();
        ProjectManager.TOO_EARLY_TO_WRITE_CONFIGS = true;
    }

    @Test
    public void projectUpdateTest() throws Exception {
        Settings.put(Settings.L_LAYOUT_PROJECT_SIMPLE, true);

        ProjectWizard.instance = null;
        ProjectWizard instance = ProjectWizard.getInstance();
        Thread.sleep(100);

        Project currentProject = Project.getCurrent();
        ConnectionsTableModel connectionsTableModel = instance.tablesUI.connectionsTableModel;

        assertEquals(3, instance.wizardTree.getRowCount());
        assertEquals("test1", currentProject.getProjectName());
        assertEquals("[Projects, test1]", instance.wizardTree.getSelectionPath().toString());
        assertEquals(1, instance.tablesUI.dbsTableModel.getRowCount()-1);
        assertEquals(1, connectionsTableModel.getRowCount()-1);
        assertEquals(1, instance.tablesUI.dedicatedTableModel.getRowCount());

        assertEquals("", instance.tablesUI.dbsTableModel.getValueAt(instance.tablesUI.dbsTableModel.getRowCount()-1, 1));
        instance.tablesUI.dbsTableModel.setValueAt(TEST_DATABASE_NAME, instance.tablesUI.dbsTableModel.getRowCount()-1, 1);
        assertEquals(2, currentProject.getDatabases().size());
        assertEquals(2, instance.tablesUI.dedicatedTableModel.getRowCount());

        instance.tablesUI.dbsTable.setRowSelectionInterval(instance.tablesUI.dbsTableModel.getRowCount()-1, 1);
        instance.tablesUI.deleteSelectedDB();
        Thread.sleep(100);
        assertEquals(1, currentProject.getDatabases().size());
        assertEquals(1, instance.tablesUI.dedicatedTableModel.getRowCount());

        instance.tablesUI.dbsTableModel.setValueAt(TEST_DATABASE_NAME, 0, 1);
        assertEquals(TEST_DATABASE_NAME, currentProject.getDatabases().get(0).getName());
        assertEquals(TEST_DATABASE_NAME, instance.tablesUI.dedicatedTableModel.getValueAt(0, 2));
    }

    @Test
    public void createSimpleProjectTest() throws Exception {
        Settings.put(Settings.L_LAYOUT_PROJECT_SIMPLE, true);
        Thread.sleep(100);

        ProjectWizard.instance = null;
        ProjectWizard instance = ProjectWizard.getInstance();
        Thread.sleep(100);

        instance.ui.btnCreate.doClick();
        Thread.sleep(100);
        assertEquals(3, instance.wizardTree.getRowCount());
        assertEquals("[Projects, Create a new project]", instance.wizardTree.getSelectionPath().toString());
        Thread.sleep(100);

        instance.setSimpleDBType(Project.L_POSTGRESQL);
        assertEquals(Theme.getLargeIcon(Theme.ICO_POSTGRESQL), instance.simpleIconLabel.getIcon());

        instance.setSimpleDBType(Project.L_ABSTRACT_MODEL);
        assertEquals(Theme.getLargeIcon(Theme.ICO_ABSTRACT), instance.simpleIconLabel.getIcon());

        instance.setSimpleDBType(Project.L_MARIA_DB);
        assertEquals(Theme.getLargeIcon(Theme.ICO_MARIADB), instance.simpleIconLabel.getIcon());

        instance.setSimpleDBType(Project.L_MYSQL);
        assertEquals(Theme.getLargeIcon(Theme.ICO_MYSQL), instance.simpleIconLabel.getIcon());

        instance.notifyChange(L_SIMPLE_PROJECT_NAME, TEST_PROJECT);
        instance.notifyChange(L_SIMPLE_HOST, TEST_HOST);
        instance.notifyChange(L_SIMPLE_USER, TEST_USER);
        instance.notifyChange(L_SIMPLE_PASS_TITLE, TEST_PASSWORD);
        instance.notifyChange(L_SIMPLE_DATABASE_NAME, TEST_DATABASE_NAME);

        instance.executeAction(CREATE_PROJECT_SIMPLE);
        Thread.sleep(100);

        Project currentProject = Project.getCurrent();
        assertEquals(TEST_PROJECT, currentProject.getProjectName());
        assertEquals(1, currentProject.getConnections().size());

        IConnection connection = currentProject.getConnections().get(0);
        assertTrue(connection instanceof ConnectionMy);
        assertEquals(TEST_HOST, connection.getHost());
        assertEquals(TEST_USER, connection.getUser());
        assertEquals(TEST_PASSWORD, connection.getPass());

        assertEquals(1, currentProject.getDatabases().size());
        assertEquals(TEST_DATABASE_NAME, currentProject.getDatabases().get(0).getName());

        // Simple project wizard does not create dedicated connections, but maybe it should for consistency?
        /*IConnection dedicatedConnection = currentProject.getDedicatedConnection(TEST_DATABASE_NAME, connection.getName());
        assertTrue(dedicatedConnection instanceof ConnectionMy);
        assertEquals(TEST_HOST, dedicatedConnection.getHost());
        assertEquals(TEST_USER, dedicatedConnection.getUser());
        assertEquals(TEST_PASSWORD, dedicatedConnection.getPass());*/
    }

    @Test
    public void createFullProjectTest() throws Exception {
        Settings.put(Settings.L_LAYOUT_PROJECT_SIMPLE, false);
        Thread.sleep(100);

        ProjectWizard.instance = null;
        ProjectWizard instance = ProjectWizard.getInstance();
        Thread.sleep(100);

        instance.ui.btnCreate.doClick();
        Thread.sleep(100);
        assertEquals(3, instance.wizardTree.getRowCount());
        assertEquals("[Projects, Create a new project]", instance.wizardTree.getSelectionPath().toString());
        Thread.sleep(100);

        instance.ui.pgRadio.doClick();
        instance.clickSave();
        Thread.sleep(100);

        instance.ui.conn2Radio.doClick();
        Thread.sleep(100);

        assertEquals(3, instance.tablesUI.connectionsTableModel.getRowCount());
        assertEquals(" + ", instance.tablesUI.connectionsTableModel.getValueAt(3, 0));

        instance.tablesUI.connectionsTableModel.setValueAt(TEST_HOST, 0, 2);
        instance.tablesUI.connectionsTableModel.setValueAt(TEST_HOST, 1, 2);

        instance.tablesUI.connectionsTableModel.setValueAt(TEST_USER, 0, 3);
        instance.tablesUI.connectionsTableModel.setValueAt(TEST_USER, 1, 3);

        instance.tablesUI.connectionsTableModel.setValueAt(TEST_PASSWORD, 0, 4);
        instance.tablesUI.connectionsTableModel.setValueAt(TEST_PASSWORD, 1, 4);
        Thread.sleep(100);

        instance.clickSave();
        Thread.sleep(100);

        assertEquals(L_NEW_PROJECT, instance.projectNameField.getText());
        instance.projectNameField.setText(ProjectWizard.EMPTY_PROJECT_NAME);

        assertEquals(2, instance.tablesUI.dbsTableModel.getRowCount());
        assertEquals("new_database", instance.tablesUI.dbsTableModel.getValueAt(0, 1));
        assertEquals(" + ", instance.tablesUI.dbsTableModel.getValueAt(1, 0));

        instance.tablesUI.dbsTableModel.setValueAt(ProjectWizard.EMPTY_DB_NAME, 0, 1);
        instance.tablesUI.dbsTableModel.setValueAt(TEST_DATABASE_NAME, 1, 1);

        assertEquals(3, instance.tablesUI.dbsTableModel.getRowCount());
        assertEquals(ProjectWizard.EMPTY_DB_NAME+", "+TEST_DATABASE_NAME, instance.tablesUI.dbsTableModel.getDatabasesCommaSeparated());

        assertEquals(4, instance.tablesUI.dedicatedTableModel.getRowCount());

        assertEquals(ProjectWizard.EMPTY_DB_NAME,           instance.tablesUI.dedicatedTableModel.getValueAt(0, 0));
        assertEquals("Development",                         instance.tablesUI.dedicatedTableModel.getValueAt(0, 1));
        assertEquals(ProjectWizard.EMPTY_DB_NAME,           instance.tablesUI.dedicatedTableModel.getValueAt(0, 2));
        assertEquals(TEST_HOST,                             instance.tablesUI.dedicatedTableModel.getValueAt(0, 3));
        assertEquals(TEST_USER,                             instance.tablesUI.dedicatedTableModel.getValueAt(0, 4));
        assertEquals(TEST_PASSWORD.replaceAll(".{1}", "*"), instance.tablesUI.dedicatedTableModel.getValueAt(0, 5));

        assertEquals(ProjectWizard.EMPTY_DB_NAME,           instance.tablesUI.dedicatedTableModel.getValueAt(1, 0));
        assertEquals("Production",                          instance.tablesUI.dedicatedTableModel.getValueAt(1, 1));
        assertEquals(ProjectWizard.EMPTY_DB_NAME,           instance.tablesUI.dedicatedTableModel.getValueAt(1, 2));
        assertEquals(TEST_HOST,                             instance.tablesUI.dedicatedTableModel.getValueAt(1, 3));
        assertEquals(TEST_USER,                             instance.tablesUI.dedicatedTableModel.getValueAt(1, 4));
        assertEquals(TEST_PASSWORD.replaceAll(".{1}", "*"), instance.tablesUI.dedicatedTableModel.getValueAt(1, 5));

        assertEquals(TEST_DATABASE_NAME,                    instance.tablesUI.dedicatedTableModel.getValueAt(2, 0));
        assertEquals("Development",                         instance.tablesUI.dedicatedTableModel.getValueAt(2, 1));
        assertEquals(TEST_DATABASE_NAME,                    instance.tablesUI.dedicatedTableModel.getValueAt(2, 2));
        assertEquals(TEST_HOST,                             instance.tablesUI.dedicatedTableModel.getValueAt(2, 3));
        assertEquals(TEST_USER,                             instance.tablesUI.dedicatedTableModel.getValueAt(2, 4));
        assertEquals(TEST_PASSWORD.replaceAll(".{1}", "*"), instance.tablesUI.dedicatedTableModel.getValueAt(2, 5));

        assertEquals(TEST_DATABASE_NAME,                    instance.tablesUI.dedicatedTableModel.getValueAt(3, 0));
        assertEquals("Production",                          instance.tablesUI.dedicatedTableModel.getValueAt(3, 1));
        assertEquals(TEST_DATABASE_NAME,                    instance.tablesUI.dedicatedTableModel.getValueAt(3, 2));
        assertEquals(TEST_HOST,                             instance.tablesUI.dedicatedTableModel.getValueAt(3, 3));
        assertEquals(TEST_USER,                             instance.tablesUI.dedicatedTableModel.getValueAt(3, 4));
        assertEquals(TEST_PASSWORD.replaceAll(".{1}", "*"), instance.tablesUI.dedicatedTableModel.getValueAt(3, 5));

        instance.tablesUI.dedicatedTableModel.setValueAt(TEST_DATABASE_NAME+"1", 2, 2);
        instance.tablesUI.dedicatedTableModel.setValueAt(TEST_DATABASE_NAME+"2", 3, 2);

        instance.clickSave();
        Thread.sleep(1000);

        Project currentProject = Project.getCurrent();
        assertEquals(ProjectWizard.EMPTY_PROJECT_NAME, currentProject.getProjectName());

        assertEquals(2, currentProject.getConnections().size());
        IConnection connection1 = currentProject.getConnections().get(0);
        IConnection connection2 = currentProject.getConnections().get(1);
        assertEquals("Development", connection1.getName());
        assertEquals("Production", connection2.getName());

        assertEquals(2, currentProject.getDatabases().size());
        DB db1 = currentProject.getDatabases().get(0);
        DB db2 = currentProject.getDatabases().get(1);
        assertEquals(ProjectWizard.EMPTY_DB_NAME, db1.getName());
        assertEquals(TEST_DATABASE_NAME, db2.getName());
        assertEquals(0, db1.getSchemas().size());
        assertEquals(0, db2.getSchemas().size());

        assertEquals(4, currentProject.getDedicatedConnections().size());
        IConnection dedicatedConnectionDevDb1 = currentProject.getDedicatedConnection(db1.getName(), connection1.getName());
        IConnection dedicatedConnectionDevDb2 = currentProject.getDedicatedConnection(db2.getName(), connection1.getName());
        IConnection dedicatedConnectionProdDb1 = currentProject.getDedicatedConnection(db1.getName(), connection2.getName());
        IConnection dedicatedConnectionProdDb2 = currentProject.getDedicatedConnection(db2.getName(), connection2.getName());
        assertEquals(ProjectWizard.EMPTY_DB_NAME, dedicatedConnectionDevDb1.getDbAlias());
        assertEquals(TEST_DATABASE_NAME+"1", dedicatedConnectionDevDb2.getDbAlias());
        assertEquals(ProjectWizard.EMPTY_DB_NAME, dedicatedConnectionProdDb1.getDbAlias());
        assertEquals(TEST_DATABASE_NAME+"2", dedicatedConnectionProdDb2.getDbAlias());

        assertEquals(TEST_HOST, dedicatedConnectionProdDb2.getHost());
        assertEquals(TEST_USER, dedicatedConnectionProdDb2.getUser());
        assertEquals(TEST_PASSWORD, dedicatedConnectionProdDb2.getPass());
    }

    @Test
    public void exampleProjectTest() throws Exception {
        Settings.put(Settings.L_LAYOUT_PROJECT_SIMPLE, true);

        ProjectWizard.instance = null;
        ProjectWizard instance = ProjectWizard.getInstance();
        Thread.sleep(100);

        assertEquals(3, instance.wizardTree.getRowCount());
        assertEquals("[Projects, test1]", instance.wizardTree.getSelectionPath().toString());

        instance.ui.btnDelete.doClick();
        Thread.sleep(100);
        assertEquals("[Projects]", instance.wizardTree.getSelectionPath().toString());

        instance.executeAction(CLOSE_WINDOW);
        instance.checkEmptyProjectList();

        Project currentProject = Project.getCurrent();
        assertEquals(ProjectWizard.EMPTY_PROJECT_NAME, currentProject.getProjectName());
        assertEquals(1, currentProject.getConnections().size());
        assertEquals(1, currentProject.getDatabases().size());
        assertEquals(ProjectWizard.EMPTY_DB_NAME, currentProject.getDatabases().get(0).getName());
        assertEquals(1, currentProject.getDatabases().get(0).getSchemas().size());
        assertEquals(9, currentProject.getDatabases().get(0).getSchemas().get(0).getRelations().size());
    }

    @Test
    public void twoProjectsTest() throws Exception {
        Settings.put(Settings.L_LAYOUT_PROJECT_SIMPLE, true);

        ProjectWizard.instance = null;
        ProjectWizard instance = ProjectWizard.getInstance();
        Thread.sleep(100);

        assertEquals(3, instance.wizardTree.getRowCount());
        assertEquals("[Projects, test1]", instance.wizardTree.getSelectionPath().toString());

        instance.ui.btnCreate.doClick();
        Thread.sleep(100);
        assertEquals(3, instance.wizardTree.getRowCount());
        assertEquals("[Projects, Create a new project]", instance.wizardTree.getSelectionPath().toString());
        Thread.sleep(100);

        instance.setSimpleDBType(Project.L_POSTGRESQL);
        assertEquals(Theme.getLargeIcon(Theme.ICO_POSTGRESQL), instance.simpleIconLabel.getIcon());

        instance.setSimpleDBType(Project.L_ABSTRACT_MODEL);
        assertEquals(Theme.getLargeIcon(Theme.ICO_ABSTRACT), instance.simpleIconLabel.getIcon());

        instance.setSimpleDBType(Project.L_MARIA_DB);
        assertEquals(Theme.getLargeIcon(Theme.ICO_MARIADB), instance.simpleIconLabel.getIcon());

        instance.setSimpleDBType(Project.L_MYSQL);
        assertEquals(Theme.getLargeIcon(Theme.ICO_MYSQL), instance.simpleIconLabel.getIcon());

        instance.notifyChange(L_SIMPLE_PROJECT_NAME, TEST_PROJECT);
        instance.notifyChange(L_SIMPLE_HOST, TEST_HOST);
        instance.notifyChange(L_SIMPLE_USER, TEST_USER);
        instance.notifyChange(L_SIMPLE_PASS_TITLE, TEST_PASSWORD);
        instance.notifyChange(L_SIMPLE_DATABASE_NAME, TEST_DATABASE_NAME);

        instance.executeAction(CREATE_PROJECT_SIMPLE);
        Thread.sleep(100);

        ProjectWizard.instance = null;
        instance = ProjectWizard.getInstance();
        Thread.sleep(200);

        assertEquals(4, instance.wizardTree.getRowCount());
        assertEquals(2, instance.wizardTree.getLeadSelectionRow());
        assertEquals("[Projects, "+TEST_PROJECT+"]", instance.wizardTree.getSelectionPath().toString());

        assertEquals(true, instance.ui.btnUp.isEnabled());
        assertEquals(false, instance.ui.btnDown.isEnabled());

        instance.ui.btnUp.doClick();
        Thread.sleep(100);
        instance.ui.btnUp.doClick();
        Thread.sleep(100);

        assertEquals(1, instance.wizardTree.getLeadSelectionRow());
        assertEquals("[Projects, "+TEST_PROJECT+"]", instance.wizardTree.getSelectionPath().toString());

        assertEquals(false, instance.ui.btnUp.isEnabled());
        assertEquals(true, instance.ui.btnDown.isEnabled());

        instance.ui.btnDown.doClick();
        Thread.sleep(100);

        assertEquals(2, instance.wizardTree.getLeadSelectionRow());
        assertEquals("[Projects, "+TEST_PROJECT+"]", instance.wizardTree.getSelectionPath().toString());

        assertEquals(true, instance.ui.btnUp.isEnabled());
        assertEquals(false, instance.ui.btnDown.isEnabled());

        instance.ui.btnCreate.doClick();
        Thread.sleep(100);
        assertEquals(4, instance.wizardTree.getRowCount());
        assertEquals("[Projects, Create a new project]", instance.wizardTree.getSelectionPath().toString());
        Thread.sleep(100);

        assertEquals("<html><h1>" + Config.EDITION_COMMUNITY + " edition</h1></html>", instance.getLastTitle());
    }
}