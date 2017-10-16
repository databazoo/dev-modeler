package com.databazoo.devmodeler.wizards.project;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;

import com.databazoo.components.WizardTree;
import com.databazoo.components.containers.HorizontalContainer;
import com.databazoo.components.table.EditableTable;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.gui.Menu;
import com.databazoo.devmodeler.gui.SearchPanel;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectExportImport;
import com.databazoo.devmodeler.project.ProjectManager;
import com.databazoo.tools.Schedule;

import static com.databazoo.tools.Schedule.Named.PROJECT_WIZARD_CONN_SELECTION_LISTENER;

class ProjectWizardUI {

    private static final int CONNECTION_TABLE_BUTTON_SIZE = 28;

    private final ProjectWizard projectWizard;
    private final WizardTree wizardTree;

    ButtonGroup connGroup;
    JRadioButton myRadio, pgRadio, mariaRadio;
    JRadioButton defRadio, conn1Radio, conn2Radio, conn3Radio, conn4Radio;

    JButton btnCreate, btnDelete, btnUp, btnDown;
    private JButton btnExport, btnImport, btnConnUp, btnConnDown, btnConnEdit, btnConnDelete;

    ProjectWizardUI(ProjectWizard projectWizard) {
        this.projectWizard = projectWizard;
        this.wizardTree = projectWizard.wizardTree;
    }

    void drawTreeButtons() {
        btnCreate = new JButton("New", Theme.getSmallIcon(Theme.ICO_CREATE_NEW));
        btnDelete = new JButton("Delete", Theme.getSmallIcon(Theme.ICO_DELETE));
        btnUp = new JButton("Up", Theme.getSmallIcon(Theme.ICO_SORT_UP));
        btnDown = new JButton("Down", Theme.getSmallIcon(Theme.ICO_SORT_DOWN));
        btnExport = new JButton("Export", Theme.getSmallIcon(Theme.ICO_EXPORT));
        btnImport = new JButton("Import", Theme.getSmallIcon(Theme.ICO_IMPORT));

        btnCreate.addActionListener(ae -> wizardTree.setSelectionRow(wizardTree.getRowCount() - 1));
        btnDelete.addActionListener(ae -> projectWizard.removeSelectedProject());

        btnUp.addActionListener(ae -> {
            int selectedRow = wizardTree.getLeadSelectionRow();

            ProjectManager.getInstance().moveUp(projectWizard.listedProject);

            wizardTree.assignNewModel(ProjectManager.getInstance().getTreeView());
            wizardTree.setSelectionRow(selectedRow - 1);
        });
        btnDown.addActionListener(ae -> {
            int selectedRow = wizardTree.getLeadSelectionRow();

            ProjectManager.getInstance().moveDown(projectWizard.listedProject);

            wizardTree.assignNewModel(ProjectManager.getInstance().getTreeView());
            wizardTree.setSelectionRow(selectedRow + 1);
        });

        btnExport.addActionListener(ae -> {
            int selectedRow = wizardTree.getLeadSelectionRow();

            if (selectedRow > 0 && selectedRow <= ProjectManager.getInstance().getProjectList().size()) {
                ProjectExportImport.getInstance().runExport(projectWizard.listedProject);
            } else {
                ProjectExportImport.getInstance().runExportAllProjects();
            }
        });
        btnImport.addActionListener(ae -> {
            int rows = wizardTree.getRowCount();
            int selectedRow = wizardTree.getLeadSelectionRow();

            ProjectExportImport.getInstance().runImport();

            wizardTree.assignNewModel(ProjectManager.getInstance().getTreeView());
            if (rows != wizardTree.getRowCount()) {
                wizardTree.setSelectionRow(wizardTree.getRowCount() - 2);
            } else {
                wizardTree.setSelectionRow(selectedRow);
            }
        });
    }

    JPanel getTreeButtonPanel() {
        JPanel buttonPanel = new JPanel(new GridLayout(0, 3));

        buttonPanel.add(btnCreate);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnUp);

        buttonPanel.add(btnImport);
        buttonPanel.add(btnExport);
        buttonPanel.add(btnDown);
        return buttonPanel;
    }

    void checkTreeButtons() {
        if (wizardTree.getLeadSelectionRow() == 0 ||
                wizardTree.getLeadSelectionPath().getLastPathComponent().toString().equals(ProjectManager.L_CREATE_NEW_PROJECT)) {
            btnDelete.setEnabled(false);
            btnUp.setEnabled(false);
            btnDown.setEnabled(false);

        } else {
            btnDelete.setEnabled(true);
            btnUp.setEnabled(wizardTree.getLeadSelectionRow() > 1);
            btnDown.setEnabled(wizardTree.getLeadSelectionRow() < wizardTree.getRowCount() - 2);
        }
        btnExport.setEnabled(!ProjectManager.getInstance().getProjectList().isEmpty());
    }

    void prepareNewProjectDbTypes() {
        myRadio = new JRadioButton(Project.L_MYSQL, false);
        pgRadio = new JRadioButton(Project.L_POSTGRESQL, false);
        mariaRadio = new JRadioButton(Project.L_MARIA_DB, false);
        defRadio = new JRadioButton(Project.L_ABSTRACT_MODEL, true);
        ButtonGroup group = new ButtonGroup();
        group.add(myRadio);
        group.add(pgRadio);
        group.add(mariaRadio);
        group.add(defRadio);

		/*if(Config.APP_EDITION.equals(Config.EDITION_COMMUNITY)){
            oracleRadio.setEnabled(false);
			oracleRadio.setToolTipText("Not available in "+Config.EDITION_COMMUNITY+" edition");
			msRadio.setEnabled(false);
			msRadio.setToolTipText("Not available in "+Config.EDITION_COMMUNITY+" edition");
			db2Radio.setEnabled(false);
			db2Radio.setToolTipText("Not available in "+Config.EDITION_COMMUNITY+" edition");
		}*/
    }

    void prepareNewProjectServers() {
        conn1Radio = new JRadioButton("Single server", false);
        conn2Radio = new JRadioButton("2 servers", false);
        conn3Radio = new JRadioButton("3 servers", true);
        conn4Radio = new JRadioButton("4+ servers", true);
        connGroup = new ButtonGroup();
        connGroup.add(conn1Radio);
        connGroup.add(conn2Radio);
        connGroup.add(conn3Radio);
        connGroup.add(conn4Radio);
        conn1Radio.setActionCommand("1");
        conn2Radio.setActionCommand("2");
        conn3Radio.setActionCommand("3");
        conn4Radio.setActionCommand("4");
        conn1Radio.addActionListener(projectWizard);
        conn2Radio.addActionListener(projectWizard);
        conn3Radio.addActionListener(projectWizard);
        conn4Radio.addActionListener(projectWizard);
    }

    JPanel createConnectionsTableButtonPanel() {
        JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 2, 2));

        buttonPanel.add(createConnectionEditButton());
        buttonPanel.add(createConnectionUpButton());
        buttonPanel.add(createConnectionDownButton());
        buttonPanel.add(createConnectionDeleteButton());

        updateConnectionsButtons();
        projectWizard.tablesUI.connectionsTable.getSelectionModel()
                .addListSelectionListener(e -> Schedule.reInvokeInEDT(
                        PROJECT_WIZARD_CONN_SELECTION_LISTENER,
                        Schedule.CLICK_DELAY,
                        this::updateConnectionsButtons
                ));

        return buttonPanel;
    }

    private JButton createConnectionUpButton() {
        btnConnUp = new JButton(Theme.getSmallIcon(Theme.ICO_SORT_UP));
        btnConnUp.setPreferredSize(new Dimension(CONNECTION_TABLE_BUTTON_SIZE, CONNECTION_TABLE_BUTTON_SIZE));
        btnConnUp.setFocusable(false);
        btnConnUp.addActionListener(e -> moveRow(true));
        btnConnUp.setToolTipText("Move row up");
        return btnConnUp;
    }

    private JButton createConnectionDownButton() {
        btnConnDown = new JButton(Theme.getSmallIcon(Theme.ICO_SORT_DOWN));
        btnConnDown.setPreferredSize(new Dimension(CONNECTION_TABLE_BUTTON_SIZE, CONNECTION_TABLE_BUTTON_SIZE));
        btnConnDown.setFocusable(false);
        btnConnDown.addActionListener(e -> moveRow(false));
        btnConnDown.setToolTipText("Move row down");
        return btnConnDown;
    }

    private void moveRow(boolean moveUp) {
        int selectedRow = projectWizard.tablesUI.connectionsTable.getSelectedRow();
        int selectedColumn = projectWizard.tablesUI.connectionsTable.getSelectedColumn();
        final IConnection[] connections = projectWizard.tablesUI.connectionsTableModel.conns.toArray(new IConnection[0]);
        int currentRow, previousRow;
        if (moveUp) {
            previousRow = selectedRow - 1;
            currentRow = selectedRow;
        } else {
            previousRow = selectedRow;
            currentRow = selectedRow + 1;
        }
        IConnection prevRow = connections[previousRow];
        connections[previousRow] = connections[currentRow];
        connections[currentRow] = prevRow;
        projectWizard.tablesUI.connectionsTableModel.setConnections(Arrays.asList(connections));
        projectWizard.tablesUI.connectionsTableModel.fireTableDataChanged();
        projectWizard.tablesUI.dedicatedTableModel.fireTableDataChanged();
        projectWizard.tablesUI.connectionsTable.changeSelection(moveUp ? selectedRow - 1 : selectedRow + 1, selectedColumn, false, false);

        if (projectWizard.listedProject != null) {
            projectWizard.listedProject.getConnections().clear();
            projectWizard.listedProject.getConnections().addAll(projectWizard.tablesUI.connectionsTableModel.conns);
            projectWizard.listedProject.save();
            ProjectManager.getInstance().saveProjects();
            SearchPanel.instance.clearSearch();
            SearchPanel.instance.updateDbTree();
            Menu.redrawRightMenu();
            Menu.getInstance().checkSyncCheckbox();
        }
    }

    private JButton createConnectionDeleteButton() {
        btnConnDelete = new JButton(Theme.getSmallIcon(Theme.ICO_DELETE));
        btnConnDelete.setPreferredSize(new Dimension(CONNECTION_TABLE_BUTTON_SIZE, CONNECTION_TABLE_BUTTON_SIZE));
        btnConnDelete.setFocusable(false);
        btnConnDelete.addActionListener(e -> projectWizard.tablesUI.deleteSelectedConnection());
        btnConnDelete.setToolTipText("Delete selected row");
        return btnConnDelete;
    }

    private JButton createConnectionEditButton() {
        btnConnEdit = new JButton(Theme.getSmallIcon(Theme.ICO_EDIT));
        btnConnEdit.setPreferredSize(new Dimension(CONNECTION_TABLE_BUTTON_SIZE, CONNECTION_TABLE_BUTTON_SIZE));
        btnConnEdit.setFocusable(false);
        btnConnEdit.addActionListener(e -> new ProjectWizardConnectionEditor(projectWizard,
                projectWizard.tablesUI.connectionsTableModel.conns.get(
                        projectWizard.tablesUI.connectionsTable.getSelectedRow()
                ),
                projectWizard.tablesUI.connectionsTable.getSelectedRow()));
        btnConnEdit.setToolTipText("Edit selected row");
        return btnConnEdit;
    }

    private void updateConnectionsButtons() {
        final EditableTable table = projectWizard.tablesUI.connectionsTable;
        boolean validRowSelected = table.getSelectedRow() >= 0 && table.getSelectedRow() < table.getRowCount() - 1;
        btnConnEdit.setEnabled(validRowSelected);
        btnConnUp.setEnabled(validRowSelected && table.getSelectedRow() > 0);
        btnConnDown.setEnabled(validRowSelected && table.getSelectedRow() < table.getRowCount() - 2);
        btnConnDelete.setEnabled(validRowSelected && table.getSelectedRow() > 0);
    }

    HorizontalContainer getRadioContainer(JRadioButton button, Icon ico) {
        JLabel lab = new JLabel(ico);
        lab.setBorder(new EmptyBorder(0, 0, 0, 6));
        return new HorizontalContainer(lab, button, null);
    }
}
