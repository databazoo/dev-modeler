package com.databazoo.devmodeler.wizards.project;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import com.databazoo.components.GCFrame;
import com.databazoo.components.table.EditableTable;
import com.databazoo.components.table.UnfocusableTableCellEditor;
import com.databazoo.devmodeler.project.ProjectManager;
import com.databazoo.tools.Schedule;

class ProjectWizardTables {

    private final ProjectWizard projectWizard;

    /**
     * Connections table
     */
    ConnectionsTableModel connectionsTableModel = new ConnectionsTableModel();
    final EditableTable connectionsTable = new EditableTable(connectionsTableModel) {

        @Override protected boolean isColEditable(int colIndex) {
            return colIndex > 0 && colIndex < connectionsTableModel.getColumnCount() - 1;
        }
    };

    /**
     * DBs table
     */
    final DBsTableModel dbsTableModel = new DBsTableModel();
    final EditableTable dbsTable = new EditableTable(dbsTableModel) {

        @Override protected boolean isColEditable(int colIndex) {
            return colIndex > 0;
        }
    };

    /**
     * Dedicated connections table
     */
    final DedicatedTableModel dedicatedTableModel = new DedicatedTableModel();
    final EditableTable dedicatedTable = new EditableTable(dedicatedTableModel) {

        @Override protected boolean isColEditable(int colIndex) {
            return colIndex > 1 && colIndex < dedicatedTableModel.getColumnCount() - 1;
        }
    };

    ProjectWizardTables(ProjectWizard projectWizard) {
        this.projectWizard = projectWizard;
    }

    void prepareNewProjectDatabasesTable() {
        dbsTable.setRowSelectionAllowed(true);
        dbsTable.setColumnSelectionAllowed(false);
        dbsTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, true), "deleteSelectedDB");
        dbsTable.getActionMap().put("deleteSelectedDB", new AbstractAction("del") {

            @Override public void actionPerformed(ActionEvent e) {
                deleteSelectedDB();
            }
        });
    }

    void deleteSelectedDB() {
        if (dbsTable.getSelectedRowCount() > 0) {
            if (projectWizard.listedProject != null) {
                int selectedRow = dbsTable.getSelectedRow();
                String dbName = dbsTableModel.databases.get(selectedRow).getName();
                Object[] options = { "Remove", "Cancel" };
                int n = GCFrame.SHOW_GUI ? JOptionPane.showOptionDialog(GCFrame.getActiveWindow(),
                        "Remove database " + dbName + "?",
                        "Remove database",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]
                ) : 0;
                if (n == 0) {
                    dbsTableModel.databases.remove(selectedRow);
                    dbsTable.setModel(dbsTableModel);
                    dbsTableModel.fireTableDataChanged();
                    dedicatedTableModel.removeDB(dbName);
                    dedicatedTableModel.fireTableDataChanged();
                    projectWizard.listedProject.databases.remove(projectWizard.listedProject.getDatabaseByName(dbName));
                    projectWizard.listedProject.save();
                    ProjectManager.getInstance().saveProjects();
                    connectionsTableModel.resetProjectGUI();
                }
            } else {
                int[] selectedRows = dbsTable.getSelectedRows();
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    if (selectedRows[i] < dbsTableModel.databases.size()) {
                        dbsTableModel.databases.remove(selectedRows[i]);
                    }
                }
                dbsTable.setModel(dbsTableModel);
                dbsTableModel.fireTableDataChanged();
                dedicatedTableModel.fireTableDataChanged();
            }
        }
    }

    void prepareNewProjectConnectionTable() {
        connectionsTable.setCellSelectionEnabled(true);
        connectionsTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, true), "removeSelectedConnection");
        connectionsTable.getActionMap().put("removeSelectedConnection", new AbstractAction("del") {

            @Override public void actionPerformed(ActionEvent e) {
                deleteSelectedConnection();
            }
        });
    }

    public void deleteSelectedConnection() {
        if (connectionsTable.getSelectedRow() > 0) {
            Object[] options = { "Remove", "Cancel" };
            final int selectedRow = connectionsTable.getSelectedRow();
            int n = JOptionPane.showOptionDialog(GCFrame.getActiveWindow(),
                    "Remove connection " + connectionsTableModel.conns.get(selectedRow).getFullName() + "?",
                    "Remove connection",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );
            if (n == 0) {
                connectionsTableModel.conns.remove(selectedRow);
                connectionsTableModel.setConnections(connectionsTableModel.conns);
                if (projectWizard.listedProject != null) {
                    projectWizard.listedProject.getConnections().remove(selectedRow);
                    projectWizard.listedProject.save();
                    connectionsTableModel.resetProjectGUI();
                    ProjectManager.getInstance().saveProjects();
                }
            }
        }
    }

    void setConnTableCols() {
        TableColumnModel columnModel = connectionsTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(35);
        columnModel.getColumn(0).setMaxWidth(35);
        //columnModel.getColumn(1).setPreferredWidth(100);
        //columnModel.getColumn(2).setPreferredWidth(40);
        columnModel.getColumn(5).setPreferredWidth(50);
        columnModel.getColumn(5).setMaxWidth(50);
        applyCellEditors(columnModel);
    }

    void setDBsTableCols() {
        TableColumnModel columnModel = dbsTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(35);
        columnModel.getColumn(0).setMaxWidth(35);
        columnModel.getColumn(1).setCellEditor(new UnfocusableTableCellEditor());
    }

    void setDedicatedTableCols() {
        Schedule.inEDT(() -> {
            // TOTAL WIDTH IS ABOUT 725px

            //dedicatedTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
            TableColumnModel columnModel = dedicatedTable.getColumnModel();
            columnModel.getColumn(0).setPreferredWidth(120);
            columnModel.getColumn(1).setPreferredWidth(100);
            columnModel.getColumn(2).setPreferredWidth(120);
            columnModel.getColumn(3).setPreferredWidth(140);
            columnModel.getColumn(4).setPreferredWidth(90);
            columnModel.getColumn(5).setPreferredWidth(90);
            columnModel.getColumn(6).setPreferredWidth(30);
            applyCellEditors(columnModel);
        });
    }

    private void applyCellEditors(TableColumnModel columnModel) {
        for (int i = 1; i < columnModel.getColumnCount(); i++) {
            UnfocusableTableCellEditor editor;
            if (i == columnModel.getColumnCount() - 2) {
                editor = new UnfocusableTableCellEditor(new JPasswordField());
            } else {
                editor = new UnfocusableTableCellEditor();
            }
            columnModel.getColumn(i).setCellEditor(editor);
        }
    }
}
