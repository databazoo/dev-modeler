package com.databazoo.devmodeler.gui.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.databazoo.components.GCFrame;
import com.databazoo.components.UIConstants;
import com.databazoo.components.combo.IconableComboBox;
import com.databazoo.components.textInput.UndoableTextField;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.gui.Menu;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Sequence;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.model.View;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;
import com.databazoo.devmodeler.project.Revision;
import com.databazoo.devmodeler.project.Revision.Diff;
import com.databazoo.devmodeler.tools.comparator.Comparator;
import com.databazoo.devmodeler.wizards.DiffWizard;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

/**
 * Difference module.
 *
 * @author bobus
 */
public class DifferenceView extends AbstractView {
    public static final DifferenceView instance = new DifferenceView();
    //private static final Font AUTHOR_FONT = FontFactory.getSans(Font.ITALIC, 12);
    private static final String L_ALL_ELEMS = "All elements (w/o data)";
    private static final String L_SCHEMATA = "Schemas";
    private static final String L_TABLES = "Tables";
    private static final String L_FUNCTIONS = "Functions";
    private static final String L_VIEWS = "Views";
    private static final String L_SEQUENCES = "Sequences";
    private static final String L_DATA = "Data & sequences";

    private int connSelected = 1;
    private boolean isCompareSelected = false;

    private JCheckBox showArchivedCB;
    private JTable revisionTable, differenceTable;
    private String remoteName, localName;
    private IconableComboBox compareFilterElems, compareFilterDBs;
    private String author;
    private UndoableTextField searchRevisionInput;

    public void removeDifference(IModelElement difference) {
        DiffTableModel model = (DiffTableModel) differenceTable.getModel();
        model.differences.remove(difference);
        model.fireTableDataChanged();
    }

    private void triggerRevisionSearch() {
        Schedule.reInvokeInWorker(Schedule.Named.DIFFERENCE_VIEW_REVISION_SEARCH, UIConstants.TYPE_TIMEOUT, this::updateRevisionTable);
    }

    @Override
    protected void drawWindow() {
        JPanel differencePanel = new JPanel(new BorderLayout());
        differencePanel.add(drawCompareFilter(), BorderLayout.NORTH);
        differencePanel.add(drawCompareTable(), BorderLayout.CENTER);

        JPanel revisionPanel = new JPanel(new BorderLayout());
        revisionPanel.add(drawRevisionFilter(), BorderLayout.NORTH);
        revisionPanel.add(drawRevisionTable(), BorderLayout.CENTER);

        outputTabs.addTab("Revisions", revisionPanel);
        outputTabs.addTab("Compare databases", differencePanel);
        outputTabs.addChangeListener(ce -> {
            isCompareSelected = outputTabs.getSelectedIndex() == 1;
            Menu.redrawRightMenu();
        });
    }

    public synchronized int getConnSelected() {
        return connSelected;
    }

    public synchronized void setConnSelected(int connSelected) {
        this.connSelected = connSelected;
    }

    public boolean isCompareSelected() {
        return isCompareSelected;
    }

    private Component drawRevisionFilter() {
        JPanel filterPanel = getTitledPanel("Revisions", false, new Point(10, 0));
        filterPanel.setPreferredSize(new Dimension(100, 28));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        filterPanel.add(drawShowArchivedCheckbox());
        filterPanel.add(drawSearchRevisionInput());

        return filterPanel;
    }

    private Component drawSearchRevisionInput() {
        searchRevisionInput = new UndoableTextField();
        searchRevisionInput.setBordered(true);
        searchRevisionInput.setPreferredSize(new Dimension(150, 24));
        searchRevisionInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent ke) {
                triggerRevisionSearch();
            }

            @Override
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                    ke.consume();
                }
            }
        });
        return searchRevisionInput;
    }

    private Component drawShowArchivedCheckbox() {
        showArchivedCB = new JCheckBox("Include archived revisions");
        showArchivedCB.setSelected(false);
        showArchivedCB.addActionListener(ae -> Schedule.inEDT(this::updateRevisionTable));
        return showArchivedCB;
    }

    private Component drawRevisionTable() {
        revisionTable = new JTable(new RevisionTableModel());
        revisionTable.addMouseListener(new RevisionMouseHandler());
        revisionTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, true), "removeSelectedRevision");
        revisionTable.getActionMap().put("removeSelectedRevision", new DeleteRevisionAction());

        JScrollPane revScroll = new JScrollPane(revisionTable);
        revScroll.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(0, 0, 1, 2), BorderFactory.createLineBorder(UIConstants.COLOR_BG_DARK)));
        return revScroll;
    }

    private Component drawCompareFilter() {
        JPanel filterPanel = getTitledPanel("Compare databases", true, new Point(2, 0));
        filterPanel.setPreferredSize(new Dimension(100, 28));

        filterPanel.add(drawCompareFilterDbCombo());
        filterPanel.add(drawCompareFilterElemsCombo());
        filterPanel.add(getCompareFilterButton());

        return filterPanel;
    }

    private Component getCompareFilterButton() {
        JButton actionButton = new JButton("Compare");
        actionButton.addActionListener(ae -> Schedule.inWorker(this::runCompareDBs));
        actionButton.setPreferredSize(new Dimension(84, 26));
        return actionButton;
    }

    private Component drawCompareFilterElemsCombo() {
        compareFilterElems = new IconableComboBox();
        compareFilterElems.setPreferredSize(new Dimension(149, 26));
        compareFilterElems.setFocusable(false);
        return compareFilterElems;
    }

    private Component drawCompareFilterDbCombo() {
        compareFilterDBs = new IconableComboBox();
        compareFilterDBs.setPreferredSize(new Dimension(149, 26));
        compareFilterDBs.setFocusable(false);
        return compareFilterDBs;
    }

    private Component drawCompareTable() {
        differenceTable = new JTable(new DiffTableModel()) {
            @Override
            public Class getColumnClass(int column) {
                return column == 0 ? ImageIcon.class : String.class;
            }
        };
        differenceTable.addMouseListener(new DifferenceTableMouseHandler());
        differenceTable.getSelectionModel().addListSelectionListener(new DifferenceTableSelectionHandler());
        JScrollPane diffScroll = new JScrollPane(differenceTable);
        diffScroll.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(0, 0, 1, 2), BorderFactory.createLineBorder(UIConstants.COLOR_BG_DARK)));
        return diffScroll;
    }

    @Override
    public void updateFilters() {
        Project p = Project.getCurrent();
        if (p != null) {
            IConnection conn = p.getCurrentConn();
            if (conn != null) {
                compareFilterElems.removeAllItems();
                compareFilterElems.addItem(L_ALL_ELEMS);
                compareFilterElems.addItem(L_DATA, Theme.getSmallIcon(Theme.ICO_DATA));
                if (conn.isSupported(SupportedElement.SCHEMA)) {
                    compareFilterElems.addItem(L_SCHEMATA, Schema.ico16);
                }
                if (conn.isSupported(SupportedElement.RELATION)) {
                    compareFilterElems.addItem(L_TABLES, Relation.ico16);
                }
                if (conn.isSupported(SupportedElement.ATTRIBUTE)) {
                    compareFilterElems.addItem(Relation.L_ATTRIBUTES, Attribute.ico16);
                }
                if (conn.isSupported(SupportedElement.INDEX)) {
                    compareFilterElems.addItem(Relation.L_INDEXES, Index.ico16);
                }
                if (conn.isSupported(SupportedElement.FOREIGN_KEY)) {
                    compareFilterElems.addItem(Relation.L_CONSTRAINTS, Constraint.ico16);
                }
                if (conn.isSupported(SupportedElement.TRIGGER)) {
                    compareFilterElems.addItem(Relation.L_TRIGGERS, Trigger.ico16);
                }
                if (conn.isSupported(SupportedElement.FUNCTION)) {
                    compareFilterElems.addItem(L_FUNCTIONS, Function.ico16);
                }
                if (conn.isSupported(SupportedElement.VIEW)) {
                    compareFilterElems.addItem(L_VIEWS, View.ico16);
                }
                if (conn.isSupported(SupportedElement.SEQUENCE)) {
                    compareFilterElems.addItem(L_SEQUENCES, Sequence.ico16);
                }
            }
            compareFilterDBs.removeAllItems();
            compareFilterDBs.addItem(L_ALL_DATABASES);
            if (p.getDatabases().size() > 1) {
                for (DB db : p.getDatabases()) {
                    compareFilterDBs.addItem(db.getName());
                }
                compareFilterDBs.setVisible(true);
            } else {
                compareFilterDBs.setSelectedIndex(0);
                compareFilterDBs.setVisible(false);
            }
            if (p.getType() == Project.TYPE_ABSTRACT) {
                outputTabs.setSelectedIndex(0);
                outputTabs.setEnabledAt(1, false);
            } else {
                outputTabs.setEnabledAt(1, true);
            }
        }
    }

    private void runCompareDBs() {
        final Object sel = compareFilterElems.getSelectedItem();
        final Project p = Project.getCurrent();
        final Comparator c = Comparator.withReportOnly();
        c.checkSchemata     = L_ALL_ELEMS.equals(sel) || L_SCHEMATA.equals(sel);
        c.checkRelations    = L_ALL_ELEMS.equals(sel) || L_TABLES.equals(sel);
        c.checkAttributes   = L_ALL_ELEMS.equals(sel) || Relation.L_ATTRIBUTES.equals(sel);
        c.checkIndexes      = L_ALL_ELEMS.equals(sel) || Relation.L_INDEXES.equals(sel);
        c.checkConstraints  = L_ALL_ELEMS.equals(sel) || Relation.L_CONSTRAINTS.equals(sel);
        c.checkTriggers     = L_ALL_ELEMS.equals(sel) || Relation.L_TRIGGERS.equals(sel);
        c.checkFunctions    = L_ALL_ELEMS.equals(sel) || L_FUNCTIONS.equals(sel);
        c.checkViews        = L_ALL_ELEMS.equals(sel) || L_VIEWS.equals(sel);
        c.checkSequences    = L_ALL_ELEMS.equals(sel) || L_SEQUENCES.equals(sel);
        c.checkSequenceNums = L_DATA.equals(sel) || L_SEQUENCES.equals(sel);
        c.checkData         = L_DATA.equals(sel);
        localName = (String) Menu.getInstance().getConnCombos()[0].getSelectedItem();
        remoteName = (String) Menu.getInstance().getConnCombos()[1].getSelectedItem();
        final IConnection localConn = p.getConnectionByName(localName);
        final IConnection remoteConn = p.getConnectionByName(remoteName);
        if (L_ALL_DATABASES.equals(compareFilterDBs.getSelectedItem())) {
            final CountDownLatch latch = new CountDownLatch(p.getDatabases().size());
            for (final DB db : p.getDatabases()) {
                Schedule.inWorker(() -> {
                    compare(c, db, localConn, remoteConn, p);
                    latch.countDown();
                });
            }
            try {
                latch.await();
            } catch (Exception e) {
                Dbg.notImportant("Nothing we can do.", e);
            }
        } else {
            compare(c, p.getDatabaseByName((String) compareFilterDBs.getSelectedItem()), localConn, remoteConn, p);
        }
        c.checkIsDifferent();
        DiffTableModel model = (DiffTableModel) differenceTable.getModel();
        model.local = localConn == null ? "Model" : localConn.getName();
        model.remote = remoteConn == null ? "Model" : remoteConn.getName();
        model.differences = c.getDifferences();
        model.fireTableDataChanged();
    }

    private void compare(Comparator c, DB db, IConnection localConn, IConnection remoteConn, Project p) {
        final DB localDB, remoteDB;
        if (localConn == null) {
            localDB = db;
        } else {
            IConnection dedicatedConn = p.getDedicatedConnection(db.getName(), localConn.getName());
            localDB = new DB(p, dedicatedConn == null ? localConn : dedicatedConn, db.getName());
        }
        if (remoteConn == null) {
            remoteDB = db;
        } else {
            IConnection dedicatedConn = p.getDedicatedConnection(db.getName(), remoteConn.getName());
            remoteDB = new DB(p, dedicatedConn == null ? remoteConn : dedicatedConn, db.getName());
        }
        final boolean[] loadOK = new boolean[] { false, false };
        final CountDownLatch latch = new CountDownLatch(2);
        Schedule.inWorker(() -> {
            loadOK[0] = localDB.load();
            latch.countDown();
        });
        Schedule.inWorker(() -> {
            loadOK[1] = remoteDB.load();
            latch.countDown();
        });
        try {
            latch.await();
        } catch (Exception e) {
            Dbg.notImportant("Nothing we can do.", e);
        }
        if (loadOK[0] && loadOK[1]) {
            c.compareDBs(localDB, remoteDB);
        }
    }

    public void updateRevisionTable() {
        RevisionTableModel model = (RevisionTableModel) revisionTable.getModel();
        model.checkRevisionChanges(searchRevisionInput.getText(), true);
        model.fireTableDataChanged();
    }

    @Override
    protected void setColWidths() {
        TableColumnModel colModel = differenceTable.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(24);
        colModel.getColumn(2).setPreferredWidth(180);
        colModel.getColumn(0).setMaxWidth(24);
        //colModel.getColumn(2).setMaxWidth(180);
        colModel = revisionTable.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(24);
        colModel.getColumn(3).setPreferredWidth(90);
        colModel.getColumn(4).setPreferredWidth(90);
        colModel.getColumn(5).setPreferredWidth(70);
        colModel.getColumn(0).setMaxWidth(24);
        colModel.getColumn(3).setMaxWidth(90);
        colModel.getColumn(4).setMaxWidth(90);
        colModel.getColumn(5).setMaxWidth(70);
        colModel.getColumn(0).setMinWidth(24);
        colModel.getColumn(3).setMinWidth(90);
        colModel.getColumn(4).setMinWidth(90);
        colModel.getColumn(5).setMinWidth(70);
        revisionTable.getColumn("").setCellRenderer(new IconCellRenderer());
        revisionTable.getColumn("Author").setCellRenderer(new AuthorCellRenderer());
        author = Settings.getStr(Settings.L_REVISION_AUTHOR);
    }

    private static class AuthorCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel comp = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value != null && value.equals(DifferenceView.instance.author)) {
                comp.setForeground(Color.BLACK);
            } else {
                comp.setForeground(UIConstants.COLOR_GRAY);
            }
            return comp;
        }
    }

    private static class RevisionTableModel extends AbstractTableModel {
        private final String[] cols = { "", "Revision", "Author", "Created", "Last change", "Changes", "Applied in" };
        private transient List<Revision> revs = new ArrayList<>();
        private int projectRevsCount = 0;

        @Override
        public String getColumnName(int col) {
            return cols[col];
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public int getRowCount() {
            if (ProjectManager.getInstance() != null) {
                checkRevisionChanges(DifferenceView.instance.searchRevisionInput.getText(), false);
                return revs.size();
            } else {
                return 0;
            }
        }

        @Override
        public Object getValueAt(int row, int col) {
            checkRevisionChanges(DifferenceView.instance.searchRevisionInput.getText(), false);
            row = revs.size() - row - 1;
            if (col == 0) {
                return revs.get(row).isArchived ? Revision.ICO_ARCHIVED
                        : (revs.get(row).isApproved ? Revision.ICO_APPROVED : (revs.get(row).isClosed ? Revision.ICO_LOCKED : ""));
            } else if (col == 1) {
                return revs.get(row).toString();
            } else if (col == 2) {
                return revs.get(row).getAuthor();
            } else if (col == 3) {
                return revs.get(row).getDate();
            } else if (col == 4) {
                return revs.get(row).getChangeDate();
            } else if (col == 5) {
                return revs.get(row).getCntChanges();
            } else if (col == 6) {
                return revs.get(row).getAppliedIn();
            } else {
                return "";
            }
        }

        Revision getRevision(int row) {
            return revs.get(revs.size() - row - 1);
        }

        private void checkRevisionChanges(String search, boolean forced) {
            if (Project.getCurrent() != null) {
                List<Revision> projectRevs = Project.getCurrent().revisions;
                if (projectRevs.size() != projectRevsCount || forced) {
                    Collections.sort(projectRevs);
                    revs = new ArrayList<>();
                    for (Revision rev : projectRevs) {
                        if (rev.isArchived && !DifferenceView.instance.showArchivedCB.isSelected()) {
                            continue;
                        }
                        if (!search.isEmpty() && !rev.getAppliedIn().contains(search) && !rev.getAuthor().contains(search)
                                && !rev.getChangeDate().contains(search) && !rev.getName().contains(search)) {
                            boolean found = false;
                            for (Diff diff : rev.getChanges()) {
                                if (diff.getElementFullName().contains(search)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                continue;
                            }
                        }
                        revs.add(rev);
                    }
                    projectRevsCount = projectRevs.size();
                }
            }
        }
    }

    private static class DiffTableModel extends AbstractTableModel {
        private final String[] cols = { "", "Element", "Difference", "Location"/*, "Apply 1", "Apply 2", "Revision"*/ };
        transient List<IModelElement> differences = new ArrayList<>();
        String local = "";
        String remote = "";

        @Override
        public String getColumnName(int col) {
            return cols[col];
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public int getRowCount() {
            return differences.size();
        }

        @Override
        public Object getValueAt(int row, int col) {
            IModelElement elem = differences.get(row);
            String ret = "";
            if (col == 0) {
                return elem.getIcon16();
            } else if (col == 1) {
                ret = /*elem.getClassName() + " " +*/ elem.getName();
            } else if (col == 2) {
                switch (elem.getDifference()) {
                case Comparator.IS_MISSING:
                    ret = "<html><font color='#990000'>missing in " + local + "</font></html>";
                    break;
                case Comparator.IS_ADDED:
                    ret = "<html><font color='#009900'>added in " + local + "</font></html>";
                    break;
                case Comparator.DEF_CHANGE:
                    ret = "has changed";
                    break;
                case Comparator.DATA_CHANGED:
                    ret = "<html><font color='#000099'>content has changed</font></html>";
                    break;
                case Comparator.DATA_FAILED_DIFFERENT_COLS:
                    ret = "<html><font color='#999999'>content not compared: different columns</font></html>";
                    break;
                case Comparator.DATA_FAILED_NO_PRIMARY_KEY:
                    ret = "<html><font color='#999999'>content not compared: no primary key</font></html>";
                    break;
                case Comparator.DATA_FAILED_QUERY_FAILED:
                    ret = "<html><font color='#999999'>content not compared: query failed</font></html>";
                    break;
                case Comparator.DATA_FAILED_TOO_BIG:
                    ret = "<html><font color='#999999'>content not compared: table is too big</font></html>";
                    break;
                }
            } else if (col == 3) {
                ret = elem.getFullPath().replace(".", " . ").replaceAll("^(.*) \\. $", "$1");
            }
            return ret;
        }

        public IModelElement getDifference(int row) {
            return differences.get(row);
        }
    }

    private static class DeleteRevisionAction extends AbstractAction {
        DeleteRevisionAction() {
            super("del");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final int selectedRow = DifferenceView.instance.revisionTable.getSelectedRow();
            if (selectedRow >= 0) {
                List<Revision> revs = ((RevisionTableModel) DifferenceView.instance.revisionTable.getModel()).revs;
                Revision rev = revs.get(revs.size() - 1 - selectedRow);
                Object[] options = { "Remove", "Cancel" };
                int n = JOptionPane.showOptionDialog(GCFrame.getActiveWindow(), "Remove revision " + rev.getName() + "?", "Remove revision",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                if (n == 0) {
                    rev.drop();
                    Project.getCurrent().revisions.remove(rev);
                    DifferenceView.instance.updateRevisionTable();
                    Project.getCurrent().save();
                }
            }
        }
    }

    private static class RevisionMouseHandler extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                RevisionTableModel model = (RevisionTableModel) DifferenceView.instance.revisionTable.getModel();
                DiffWizard.get().drawRevision(model.getRevision(DifferenceView.instance.revisionTable.getSelectedRow()));
            }
        }
    }

    private static class DifferenceTableMouseHandler extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            DiffTableModel model = (DiffTableModel) DifferenceView.instance.differenceTable.getModel();
            if (e.getClickCount() == 2) {
                IModelElement elem = model.getDifference(DifferenceView.instance.differenceTable.getSelectedRow());
                if (elem instanceof Relation && ((Relation) elem).getDataChanged() != null) {
                    DiffWizard.get().drawDataDifference((Relation) elem, DifferenceView.instance.localName, DifferenceView.instance.remoteName);
                } else {
                    DiffWizard.get().drawDifference(elem, DifferenceView.instance.localName, DifferenceView.instance.remoteName);
                }
            }
        }
    }

    private static class DifferenceTableSelectionHandler implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent lse) {
            /*final IModelElement elem = Canvas.instance.getSelectedElement();
            new Timer().schedule(new TimerTask(){
                @Override public void run(){
                    if(elem != null){
                        elem.unSelect();
                        elem.setSelected(false);
                    }
                }
            }, 2000);*/
            if (DifferenceView.instance.differenceTable.getSelectedRow() >= 0) {
                ((DiffTableModel) DifferenceView.instance.differenceTable.getModel()).getDifference(DifferenceView.instance.differenceTable.getSelectedRow()).clicked();
            }
        }
    }
}
