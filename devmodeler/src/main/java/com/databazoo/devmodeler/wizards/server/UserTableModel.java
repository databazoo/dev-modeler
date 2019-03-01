package com.databazoo.devmodeler.wizards.server;

import com.databazoo.devmodeler.model.User;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class UserTableModel extends AbstractTableModel {

    private final ServerAdministrationWizard wizard;
    List<User> users = new ArrayList<>();

    UserTableModel(ServerAdministrationWizard wizard) {
        this.wizard = wizard;
    }

    @Override
    public int getRowCount() {
        return users.size() + 1;
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 1:
                return "User";
            case 2:
                return "Password";
            case 3:
                return "Info";
            default:
                return "";
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < users.size()) {
            switch (columnIndex) {
                case 1:
                    return users.get(rowIndex).getName();
                case 2:
                    return users.get(rowIndex).getPassword();
                case 3:
                    return users.get(rowIndex).getExtra();
            }
        }
        return "";
    }

    synchronized void setUsers(List<User> newRows) {
        users.clear();
        users.addAll(newRows);

        fireTableDataChanged();
    }
}
