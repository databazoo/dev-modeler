package com.databazoo.devmodeler.wizards.project;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import com.databazoo.components.GCFrame;
import com.databazoo.components.combo.IconableComboBox;
import com.databazoo.components.icons.PlainColorIcon;
import com.databazoo.devmodeler.conn.IColoredConnection.ConnectionColor;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.project.ProjectManager;
import com.databazoo.devmodeler.wizards.ConnectionChecker;
import com.databazoo.devmodeler.wizards.MigWizard;
import com.databazoo.tools.Schedule;

import static com.databazoo.devmodeler.wizards.ConnectionChecker.L_HOST;
import static com.databazoo.devmodeler.wizards.ConnectionChecker.L_PASS;
import static com.databazoo.devmodeler.wizards.ConnectionChecker.L_SERVER;
import static com.databazoo.devmodeler.wizards.ConnectionChecker.L_USER;

class ProjectWizardConnectionEditor extends MigWizard {

    private static final String L_COLOR = "Color";

    private final ConnectionChecker checker;
    private final ProjectWizard projectWizard;
    private final IConnection connection;
    final IConnection testConnection;
    private final int selectedRow;
    private final Map<String, String> changeMap = new HashMap<>();

    private JTextField jdbcString;

    ProjectWizardConnectionEditor(ProjectWizard projectWizard, IConnection connection, int selectedRow) {
        super();
        this.projectWizard = projectWizard;
        this.selectedRow = selectedRow;
        this.connection = connection;
        this.testConnection = projectWizard.listedProject.copyConnection(connection);
        this.checker = new ConnectionChecker(this, testConnection, this::saveChangesInTestConnection);
        drawWindow();
    }

    private void drawWindow() {
        if (GCFrame.SHOW_GUI) {
            String[] options = new String[] { "Save", "Cancel" };
            int choice = JOptionPane.showOptionDialog(
                    GCFrame.getActiveWindow(),
                    preparePanel(),
                    "Edit connection",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[0]
            );
            if (choice == 0) {
                saveChanges();
            }
        }
    }

    void saveChanges() {
        for (Map.Entry<String, String> entry : changeMap.entrySet()) {
            switch (entry.getKey()) {
            case L_SERVER:
                setValueInConnectionsTable(entry, 1);
                break;
            case L_HOST:
                setValueInConnectionsTable(entry, 2);
                break;
            case L_USER:
                setValueInConnectionsTable(entry, 3);
                break;
            case L_PASS:
                setValueInConnectionsTable(entry, 4);
                break;
            case L_COLOR:
                setValueInConnectionsTable(entry, 11);
                break;
            }
        }
        ProjectManager.getInstance().saveProjects();
    }

    JPanel preparePanel() {
        final JPanel panel = createPlacementPanel(10);
        panel.setPreferredSize(new Dimension(400, 380));
        panel.setBorder(new EmptyBorder(0, 0, 0, 5));

        final JTextField serverInput = addPlainTextInput(L_SERVER, connection.getName(), SPAN);
        Schedule.inEDT(Schedule.CLICK_DELAY, serverInput::requestFocusInWindow);

        addColorOptions();

        addEmptyLine();
        addConnectionOptions();
        addJdbcString();
        checker.addConnectionCheck();

        addEmptyLine();
        addSshOptions();
        return panel;
    }

    private void addColorOptions() {
        ConnectionColor selected = (ConnectionColor) projectWizard.tablesUI.connectionsTableModel.getValueAt(selectedRow, 11);
        final IconableComboBox comboBox = addCombo(L_COLOR, new String[0], null, SPAN);

        for (ConnectionColor connectionColor : ConnectionColor.values()) {
            comboBox.addItem(connectionColor.getColorName(), new PlainColorIcon(connectionColor.getColor()));
            if (selected == connectionColor) {
                comboBox.setSelectedItem(connectionColor.getColorName());
            }
        }

    }

    private void addSshOptions() {
        addText("SSH tunneling options", SPAN_CENTER);
        addPlainTextInput(L_HOST + " ", "", SPAN).setEnabled(false);
        addPlainTextInput(L_USER + " ", "", SPAN).setEnabled(false);
        addPlainTextInput(L_PASS + " ", "", SPAN).setEnabled(false);
    }

    private void addConnectionOptions() {
        addText("Connection options", SPAN_CENTER);
        addPlainTextInput(L_HOST, connection.getHost(), SPAN);
        addPlainTextInput(L_USER, connection.getUser(), SPAN);
        addPasswordInput(L_PASS, connection.getPass(), SPAN);
    }

    private void addJdbcString() {
        jdbcString = addPlainTextInput("JDBC string", connection.getProtocol() + connection.getHost(), SPAN);
        jdbcString.setEditable(false);
    }

    void saveChangesInTestConnection() {
        testConnection.setAutocheckEnabled(false);
        for (Map.Entry<String, String> entry : changeMap.entrySet()) {
            switch (entry.getKey()) {
            case L_HOST:
                testConnection.setHost(entry.getValue());
                break;
            case L_USER:
                testConnection.setUser(entry.getValue());
                break;
            case L_PASS:
                testConnection.setPass(entry.getValue());
                break;
            }
        }
        testConnection.setAutocheckEnabled(true);
    }

    private void setValueInConnectionsTable(Map.Entry<String, String> entry, int col) {
        projectWizard.tablesUI.connectionsTableModel.setValueAt(entry.getValue(), selectedRow, col);
    }

    @Override public void valueChanged(TreeSelectionEvent tse) {
    }

    @Override public void notifyChange(String elementName, String value) {
        if (elementName.equals(L_HOST)) {
            jdbcString.setText(connection.getProtocol() + value);
        }
        if (elementName.equals(L_HOST) || elementName.equals(L_USER) || elementName.equals(L_PASS)) {
            checker.reset();
        }
        changeMap.put(elementName, value);
    }

    @Override public void notifyChange(String elementName, boolean value) {
    }

    @Override public void notifyChange(String elementName, boolean[] values) {
    }
}
