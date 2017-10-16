package com.databazoo.devmodeler.wizards.project;

import javax.swing.*;
import java.awt.*;

import com.databazoo.components.GCFrame;
import com.databazoo.components.combo.IconableComboBox;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.IColoredConnection;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.gui.CanvasTest;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.databazoo.devmodeler.wizards.ConnectionChecker.L_HOST;
import static com.databazoo.devmodeler.wizards.ConnectionChecker.L_PASS;
import static com.databazoo.devmodeler.wizards.ConnectionChecker.L_USER;
import static org.junit.Assert.assertEquals;

public class ProjectWizardConnectionEditorTest extends TestProjectSetup {

    @BeforeClass
    public static void hideGUI() {
        GCFrame.SHOW_GUI = false;
    }

    @Before
    public void init() throws InterruptedException {
        setProjectUp();
        finalizeData();
        Project.getCurrent().setCurrentDB(database);
        CanvasTest.initializeCanvas();
        ProjectManager.TOO_EARLY_TO_WRITE_CONFIGS = true;
    }

    @Test
    public void edit() throws Exception {
        Settings.put(Settings.L_LAYOUT_PROJECT_SIMPLE, true);

        final IConnection connection = Project.getCurrent().getConnections().get(0);
        connection.setName("Prod");
        connection.setHost("Host");
        connection.setUser("User");
        connection.setPass("Pass");
        connection.setColor(IColoredConnection.ConnectionColor.ORANGE);

        ProjectWizard.instance = null;
        ProjectWizard projectWizard = ProjectWizard.getInstance();
        Thread.sleep(100);

        assertEquals(Project.getCurrent().getProjectName(), projectWizard.listedProject.getProjectName());

        projectWizard.tablesUI.connectionsTable.changeSelection(0, 2, false, false);

        ProjectWizardConnectionEditor editor = new ProjectWizardConnectionEditor(projectWizard,
                projectWizard.tablesUI.connectionsTableModel.conns.get(
                        projectWizard.tablesUI.connectionsTable.getSelectedRow()
                ),
                projectWizard.tablesUI.connectionsTable.getSelectedRow());
        JPanel panel = editor.preparePanel();
        final Component[] components = panel.getComponents();

        assertEquals(24, components.length);

        JTextField inputName = (JTextField) components[1];
        IconableComboBox inputColor = (IconableComboBox) components[3];
        JTextField inputHost = (JTextField) components[7];
        JTextField inputUser = (JTextField) components[9];
        JTextField inputPass = (JTextField) components[11];
        JTextField inputJDBC = (JTextField) components[13];

        assertEquals("Prod", inputName.getText());
        assertEquals("Orange", inputColor.getSelectedItem());
        assertEquals("Host", inputHost.getText());
        assertEquals("User", inputUser.getText());
        assertEquals("Pass", inputPass.getText());
        assertEquals("jdbc:postgresql://Host", inputJDBC.getText());

        editor.notifyChange(L_HOST, "myNewHostName:9876");
        editor.notifyChange(L_USER, "myNewUser");
        editor.notifyChange(L_PASS, "myNewPass");
        editor.saveChangesInTestConnection();

        assertEquals("jdbc:postgresql://myNewHostName:9876", inputJDBC.getText());
        assertEquals("jdbc:postgresql://myNewHostName:9876/test"
                + "?connectTimeout=3000"
                + "&allowMultiQueries=true"
                + "&characterEncoding=UTF-8"
                + "&ApplicationName=Databazoo Dev Modeler", editor.testConnection.getConnURL("test"));
        assertEquals("myNewUser", editor.testConnection.getUser());
        assertEquals("myNewPass", editor.testConnection.getPass());

        assertEquals("jdbc:postgresql://Host/test"
                + "?connectTimeout=3000"
                + "&allowMultiQueries=true"
                + "&characterEncoding=UTF-8"
                + "&ApplicationName=Databazoo Dev Modeler", connection.getConnURL("test"));
        assertEquals("User", connection.getUser());
        assertEquals("Pass", connection.getPass());

        editor.saveChanges();

        assertEquals("jdbc:postgresql://myNewHostName:9876/test"
                + "?connectTimeout=3000"
                + "&allowMultiQueries=true"
                + "&characterEncoding=UTF-8"
                + "&ApplicationName=Databazoo Dev Modeler", connection.getConnURL("test"));
        assertEquals("myNewUser", connection.getUser());
        assertEquals("myNewPass", connection.getPass());
    }

}