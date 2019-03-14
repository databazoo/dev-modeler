package com.databazoo.devmodeler.wizards.server;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.stream.Collectors;

import com.databazoo.components.GCFrame;
import com.databazoo.components.WizardTree;
import com.databazoo.components.combo.IconableComboBox;
import com.databazoo.components.icons.ModelIconRenderer;
import com.databazoo.components.table.EditableTable;
import com.databazoo.components.table.UnfocusableTableCellEditor;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.DBCommException;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.User;
import com.databazoo.devmodeler.tools.Geometry;
import com.databazoo.devmodeler.wizards.ConnectionChecker;
import com.databazoo.devmodeler.wizards.SQLEnabledWizard;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

import static com.databazoo.components.table.EditableTable.L_DOUBLECLICK_TO_EDIT;
import static com.databazoo.devmodeler.conn.SupportedElement.DATABASE_RENAME;
import static com.databazoo.devmodeler.wizards.ConnectionChecker.L_HOST;
import static com.databazoo.devmodeler.wizards.ConnectionChecker.L_PASS;
import static com.databazoo.devmodeler.wizards.ConnectionChecker.L_USER;

/**
 * Use-cases described in HTML
 */
public class ServerAdministrationWizard extends SQLEnabledWizard {

    private static final int REFRESH_DB = 31;
    private static final int REFRESH_USER = 32;

    private static final String L_SERVER_ADMINISTRATION = "Server administration";
    private static final String L_DATABASES = "Databases";
    private static final String L_USERS = "Users";
    private static final String L_REFRESH = "Refresh";
    private static final String L_TEMPLATE = "Template";
    private static final String L_LOAD_REFRESH = "Load / Refresh";

    public static synchronized ServerAdministrationWizard getInstance() {
        return new ServerAdministrationWizard();
    }

    private WizardTree tree;
    private final ConnectionChecker checker;
    private IconableComboBox inputTemplate;
    private JTextField inputHost;
    private JTextField inputUser;
    private JPasswordField inputPass;
    private boolean isApplyingTemplate = false;

    private DbTableModel dbsTableModel = new DbTableModel(this);
    private EditableTable dbsTable = new EditableTable(dbsTableModel) {
        @Override
        protected boolean isColEditable(int colIndex) {
            return colIndex == 1 && project.getCurrentConn().isSupported(DATABASE_RENAME);
        }
    };

    private UserTableModel userTableModel = new UserTableModel(this);
    private EditableTable userTable = new EditableTable(userTableModel) {
        @Override
        protected boolean isColEditable(int colIndex) {
            return 1 <= colIndex && colIndex <= 2;
        }
    };

    private JScrollPane dbScrollPane = new JScrollPane(dbsTable);
    private JScrollPane userScrollPane = new JScrollPane(userTable);

    private JButton dbLoadButton;
    private JButton userLoadButton;

    private boolean isLoadedUsers = false;
    private boolean isLoadedDBs = false;

    private ServerAdministrationWizard() {
        this.connection = project.copyConnection(connection);
        this.checker = new ConnectionChecker(this, connection);

        createUI();
    }

    private void createUI() {
        dbsTable.setRowSelectionAllowed(true);
        dbsTable.setColumnSelectionAllowed(false);
        dbsTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, true), "deleteSelectedDB");
        dbsTable.getActionMap().put("deleteSelectedDB", new AbstractAction("del") {
            @Override public void actionPerformed(ActionEvent e) {
                deleteSelectedDB();
            }
        });

        userTable.setRowSelectionAllowed(true);
        userTable.setColumnSelectionAllowed(false);
        userTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, true), "deleteSelectedUser");
        userTable.getActionMap().put("deleteSelectedUser", new AbstractAction("del") {
            @Override public void actionPerformed(ActionEvent e) {
                deleteSelectedUser();
            }
        });

        dbLoadButton = new JButton(L_LOAD_REFRESH, Theme.getSmallIcon(Theme.ICO_RUN));
        dbLoadButton.addActionListener(e -> refreshDatabases());
        dbScrollPane.setPreferredSize(new Dimension(50, 450));

        userLoadButton = new JButton(L_LOAD_REFRESH, Theme.getSmallIcon(Theme.ICO_RUN));
        userLoadButton.addActionListener(e -> refreshUsers());
        userScrollPane.setPreferredSize(new Dimension(50, 450));

        setTableCols();
    }

    private void setTableCols() {
        TableColumnModel dbsColumnModel = dbsTable.getColumnModel();
        dbsColumnModel.getColumn(0).setPreferredWidth(35);
        dbsColumnModel.getColumn(0).setMaxWidth(35);
        dbsColumnModel.getColumn(1).setCellEditor(new UnfocusableTableCellEditor());

        TableColumnModel userColumnModel = userTable.getColumnModel();
        userColumnModel.getColumn(0).setPreferredWidth(35);
        userColumnModel.getColumn(0).setMaxWidth(35);
        userColumnModel.getColumn(1).setCellEditor(new UnfocusableTableCellEditor());
        userColumnModel.getColumn(2).setCellEditor(new UnfocusableTableCellEditor());
    }

    private void deleteSelectedDB() {
        if (dbsTable.getSelectedRowCount() > 0) {
            int selectedRow = dbsTable.getSelectedRow();
            DB db = dbsTableModel.databases.get(selectedRow);
            String sql = connection.getQueryDrop(db);
            Object[] options = { "Remove", "Cancel" };
            int n = GCFrame.SHOW_GUI ? JOptionPane.showOptionDialog(GCFrame.getActiveWindow(),
                    "Remove database " + db.getName() + " from server " + connection.getHost() + "?\n\n" + sql,
                    "Remove database",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            ) : 0;
            if (n == 0) {
                runSingleQuery(connection, sql, database, this::refreshDatabases, null, "Failed to remove a DB from server " + connection.getHost());
            }
        }
    }

    private void deleteSelectedUser() {
        if (userTable.getSelectedRowCount() > 0) {
            int selectedRow = userTable.getSelectedRow();
            User user = userTableModel.users.get(selectedRow);
            String sql = connection.getQueryDrop(user);
            Object[] options = { "Remove", "Cancel" };
            int n = GCFrame.SHOW_GUI ? JOptionPane.showOptionDialog(GCFrame.getActiveWindow(),
                    "Remove user " + user.getName() + " from server " + connection.getHost() + "?\n\n" + sql,
                    "Remove user",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            ) : 0;
            if (n == 0) {
                runSingleQuery(connection, sql, database, this::refreshDatabases, null, "Failed to remove a DB from server " + connection.getHost());
            }
        }
    }

    void createDB(DB db) {
        // TODO: allow altering the SQL
        runSingleQuery(connection, connection.getQueryCreate(db, null), database, this::refreshDatabases, null, "Failed to create a DB on server " + connection.getHost());
    }

    void updateDB(DB db) {
        String changed = connection.getQueryChanged(db);
        if (!changed.isEmpty()) {
            runSingleQuery(connection, changed, database, this::refreshDatabases, null, "Failed to update a DB on server " + connection.getHost());
        }
    }

    /**
     * Draw the Server Administration wizard.
     *
     * Must be started in EDT.
     */
    public void drawUsersRoles() {
        drawWindow(L_SERVER_ADMINISTRATION, createTree(2), Settings.getBool(Settings.L_MAXIMIZE_WIZARDS), false);
    }

    public void drawDatabases() {
        drawWindow(L_SERVER_ADMINISTRATION, createTree(1), Settings.getBool(Settings.L_MAXIMIZE_WIZARDS), false);
    }

    private JComponent createTree(int plusRow) {
        Set<String> servers = project.getDedicatedConnections()
                .values().stream()
                .map(IConnection::getHost)
                .collect(Collectors.toSet());

        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        servers.forEach(s -> {
            DefaultMutableTreeNode serverNode = new DefaultMutableTreeNode(s);
            serverNode.add(new DefaultMutableTreeNode(L_DATABASES));
            serverNode.add(new DefaultMutableTreeNode(L_USERS));
            // Optional: tablespaces, aliases, db links
            root.add(serverNode);
        });

        tree = new WizardTree(root, 1, new ModelIconRenderer(), this);
        tree.setRootVisible(false);
        Schedule.inEDT(() -> tree.selectRow(connection.getHost(), plusRow));
        return tree;
    }

    @Override
    public void valueChanged(final TreeSelectionEvent tse) {
        Schedule.inEDT(() -> {
            if (tse.getNewLeadSelectionPath() != null) {
                switch (tse.getNewLeadSelectionPath().getLastPathComponent().toString()) {
                case L_DATABASES:
                    loadDatabasesPage();
                    break;
                case L_USERS:
                    loadUsersPage();
                    break;
                default:
                    loadWelcomePage();
                    break;
                }
            } else {
                loadWelcomePage();
            }
        });
    }

    private void loadDatabasesPage() {
        resetContent();
        addTitle(L_DATABASES);
        addAdminPanel(dbLoadButton);
        addPanel(dbScrollPane, SPAN_GROW);
        addText(L_DOUBLECLICK_TO_EDIT, SPAN);

        setNextButton(L_REFRESH, true, REFRESH_DB);

        if (!isLoadedDBs) {
            Schedule.inWorker(this::refreshDatabases);
            isLoadedDBs = true;
        }
    }

    private void loadUsersPage() {
        resetContent();
        addTitle(L_USERS);
        addAdminPanel(userLoadButton);
        addPanel(userScrollPane, SPAN_GROW);
        addText(L_DOUBLECLICK_TO_EDIT, SPAN);

        setNextButton(L_REFRESH, true, REFRESH_USER);

        if (!isLoadedUsers) {
            Schedule.inWorker(this::refreshUsers);
            isLoadedUsers = true;
        }
    }

    private void addAdminPanel(JButton loadButton) {
        inputTemplate = addCombo(L_TEMPLATE,
                Geometry.concat(new String[] { " " },
                        project.getDedicatedConnections().values().stream()
                                .map(this::getTemplateName)
                                .distinct()
                                .toArray(String[]::new)
                ),
                getTemplateName(connection));
        inputHost = addPlainTextInput(L_HOST, connection.getHost());
        inputUser = addPlainTextInput(L_USER, connection.getUser());
        inputPass = addPasswordInput(L_PASS, connection.getPass());

        checker.addConnectionCheck("");
        addPanel(loadButton, SPAN2_CENTER);
    }

    private String getTemplateName(IConnection conn) {
        return conn.getUser() + '@' + conn.getHost();
    }

    private void matchTemplateToConnection() {
        final int itemCount = inputTemplate.getItemCount();
        for (int i = 0; i < itemCount; i++) {
            final String item = inputTemplate.getItemAt(i);
            if (item.equals(getTemplateName(connection))) {
                inputTemplate.setEnabled(false);
                inputTemplate.setSelectedIndex(i);
                inputTemplate.setEnabled(true);
                return;
            }
        }
        inputTemplate.setSelectedIndex(0);
    }

    private void loadWelcomePage() {
        resetContent();
        addTitle(L_SERVER_ADMINISTRATION);

        /*JPanel content = new JPanel(new GridLayout(0, 2, 20, 20));
        for(final UseCase useCase : UseCase.values()){
            JButton button = new JButton(useCase.toString());
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    tree.selectRow(useCase.toString());
                }
            });
            content.add(button);
        }
        addPanel(content, "span, align center, width 450px, height 520px");*/

        setNextButton(L_REFRESH, false, REFRESH_DB);
    }

    @Override
    protected void executeAction(int type) {
        if (type == CLOSE_WINDOW) {
            frame.dispose();

        } else if (type == REFRESH_DB) {
            refreshDatabases();

        } else if (type == REFRESH_USER) {
            refreshUsers();
        }
    }

    private void refreshDatabases() {
        Schedule.inWorker(() -> {
            try {
                ((DbTableModel) dbsTable.getModel()).setDatabases(connection.getDatabases());
            } catch (DBCommException e) {
                Dbg.fixme("Server Admin Wizard failed", e);
            }
        });

    }

    private void refreshUsers() {
        Schedule.inWorker(() -> {
            try {
                ((UserTableModel) userTable.getModel()).setUsers(connection.getUsers());
            } catch (DBCommException e) {
                Dbg.fixme("Server Admin Wizard failed", e);
            }
        });
    }

    @Override public void notifyChange(String elementName, String value) {
        connection.setAutocheckEnabled(false);
        switch (elementName) {
        case L_TEMPLATE:
            if (!inputTemplate.isEnabled()) {
                return;
            }
            for (IConnection conn : project.getDedicatedConnections().values()) {
                if (getTemplateName(conn).equals(value)) {
                    isApplyingTemplate = true;
                    notifyChange(L_HOST, conn.getHost());
                    notifyChange(L_USER, conn.getUser());
                    notifyChange(L_PASS, conn.getPass());
                    inputHost.setText(conn.getHost());
                    inputUser.setText(conn.getUser());
                    inputPass.setText(conn.getPass());
                    isApplyingTemplate = false;
                    connection.setAutocheckEnabled(true);
                    return;
                }
            }
            break;

        case L_HOST:
            connection.setHost(value);
            break;

        case L_USER:
            connection.setUser(value);
            break;

        case L_PASS:
            connection.setPass(value);
            break;
        }
        connection.setAutocheckEnabled(true);
        if (!isApplyingTemplate) {
            matchTemplateToConnection();
        }
    }

    @Override public void notifyChange(String elementName, boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override public void notifyChange(String elementName, boolean[] values) {
        throw new UnsupportedOperationException();
    }
}
