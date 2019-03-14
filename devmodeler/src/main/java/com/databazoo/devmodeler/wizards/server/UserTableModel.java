package com.databazoo.devmodeler.wizards.server;

import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.User;
import com.databazoo.tools.Schedule;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

import static com.databazoo.tools.Schedule.Named.SERVER_ADMIN_TABLE_EDIT;

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
                return User.Behavior.L_NAME;
            case 2:
                return User.Behavior.L_PASSWORD;
            case 3:
                return User.Behavior.L_EXTRA;
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
                    return users.get(rowIndex).getBehavior().getPassword().replaceAll(".", "*");
                case 3:
                    return users.get(rowIndex).getBehavior().getExtra();
            }
        }
        return "";
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (aValue != null && !aValue.toString().isEmpty()) {
            Schedule.reInvokeInWorker(SERVER_ADMIN_TABLE_EDIT, Schedule.CLICK_DELAY, () -> {
                if (rowIndex == users.size()) {
                    String name;
                    String password;
                    if (columnIndex == 1) {
                        name = aValue.toString();
                        password = "";
                    } else {
                        name = "";
                        password = aValue.toString();
                    }

                    User user = new User(name, password);
                    user.getBehavior().setNew();
                    users.add(user);

                    if (!name.isEmpty()) {
                        wizard.createUser(user);
                    } else {
                        fireTableDataChanged();
                    }
                } else {
                    User user = users.get(rowIndex);
                    if (user.getBehavior().isNew()) {
                        if (columnIndex == 1) {
                            user.setName(aValue.toString());
                            wizard.createUser(user);
                        } else {
                            user.getBehavior().setPassword(aValue.toString());
                            fireTableDataChanged();
                        }
                    } else {
                        User.Behavior behavior = user.getBehavior().prepareForEdit();
                        if (columnIndex == 1) {
                            behavior.notifyChange(User.Behavior.L_NAME, aValue.toString());
                        } else {
                            behavior.notifyChange(User.Behavior.L_PASSWORD, aValue.toString());
                        }
                        wizard.updateUser(user);
                    }
                }
            });
        }
    }

    synchronized void setUsers(List<User> newRows) {
        users.clear();
        users.addAll(newRows);

        fireTableDataChanged();
    }
}
