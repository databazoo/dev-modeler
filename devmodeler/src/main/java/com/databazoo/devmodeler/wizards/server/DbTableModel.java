package com.databazoo.devmodeler.wizards.server;

import com.databazoo.devmodeler.model.DB;
import com.databazoo.tools.Schedule;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

import static com.databazoo.tools.Schedule.Named.SERVER_ADMIN_TABLE_EDIT;

public class DbTableModel extends AbstractTableModel {

    private final ServerAdministrationWizard wizard;
    List<DB> databases = new ArrayList<>();

    DbTableModel(ServerAdministrationWizard wizard) {
        this.wizard = wizard;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 1:
                return DB.Behavior.L_NAME;
            default:
                return "";
        }
    }

    @Override
    public int getRowCount() {
        return databases.size() + 1;
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < databases.size()) {
            switch (columnIndex) {
                case 1:
                    return databases.get(rowIndex).getName();
            }
        }
        return "";
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (aValue != null && !aValue.toString().isEmpty()) {
            Schedule.reInvokeInWorker(SERVER_ADMIN_TABLE_EDIT, Schedule.CLICK_DELAY, () -> {
                if (rowIndex == databases.size()) {
                    wizard.createDB(new DB(null, aValue.toString()));
                } else {
                    DB db = databases.get(rowIndex);
                    db.getBehavior().prepareForEdit()
                            .notifyChange(DB.Behavior.L_NAME, aValue.toString());
                    wizard.updateDB(db);
                }
            });
        }
    }

    synchronized void setDatabases(List<DB> newRows) {
        databases.clear();
        databases.addAll(newRows);

        fireTableDataChanged();
    }
}
