package com.databazoo.devmodeler.gui.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import com.databazoo.components.UIConstants;
import com.databazoo.components.combo.IconableComboBox;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.tools.optimizer.ModelFlaw;
import com.databazoo.devmodeler.tools.optimizer.StaticFlawOptimizer;
import com.databazoo.tools.Schedule;

/**
 * Optimizer module.
 *
 * @author bobus
 */
public class OptimizerView extends AbstractView {
    public static final OptimizerView instance = new OptimizerView();
    private static final String L_ALL_ELEMS = "All elements";
    private static final String L_REFERENCES = "References";
    private static final String L_PRIMARY_KEYS = "Primary keys";
    private static final String L_UNIQUE_DATA = "Unique data";
    private static final String L_EMPTY_VALUES = "Empty values";
    private static final String L_NUMERIC_VALUES = "Numeric values";
    private IconableComboBox staticFlawsDBs, dataBasedDBs, staticFlawsElems, dataBasedElems, staticFlawsSeverity, dataBasedSeverity;
    private JTable staticFlawTable;
    private ResultPanel staticFlawsResultPanel, dataBasedResultPanel;

    @Override
    protected void drawWindow() {
        outputTabs.addTab("Static model flaws",
                drawFlawsPanel(drawStaticFlawsFilter(), drawStaticFlawsTable()));
        // FIXME: disabled for now
        /*outputTabs.addTab("Data-based analysis",
                drawFlawsPanel(drawDataBasedFilter(), drawDataBasedTable()));*/
        drawDataBasedFilter();
    }

    private Component drawFlawsPanel(Component filter, Component table) {
        JPanel staticFlawsPanel = new JPanel(new BorderLayout());
        staticFlawsPanel.add(filter, BorderLayout.NORTH);
        staticFlawsPanel.add(table, BorderLayout.CENTER);
        return staticFlawsPanel;
    }

    @Override
    public void updateFilters() {
        Project p = Project.getCurrent();
        if (p != null) {
            updateElementsLists(p);
            updateDatabaseLists(p);
            updateSeverity(staticFlawsSeverity);
            updateSeverity(dataBasedSeverity);
            // FIXME: disabled for now
            /*if (p.getType() == Project.TYPE_ABSTRACT) {
                outputTabs.setSelectedIndex(0);
                outputTabs.setEnabledAt(1, false);
            } else {
                outputTabs.setEnabledAt(1, true);
            }*/
        }
    }

    private void updateElementsLists(Project p) {
        IConnection conn = p.getCurrentConn();
        if (conn != null) {
            staticFlawsElems.removeAllItems();
            dataBasedElems.removeAllItems();
            staticFlawsElems.addItem(L_ALL_ELEMS);
            dataBasedElems.addItem(L_ALL_ELEMS);
            if (conn.isSupported(SupportedElement.FOREIGN_KEY)) {
                staticFlawsElems.addItem(L_REFERENCES, Constraint.ico16);
            }
            if (conn.isSupported(SupportedElement.INDEX)) {
                staticFlawsElems.addItem(Relation.L_INDEXES, Index.ico16);
                dataBasedElems.addItem(Relation.L_INDEXES, Index.ico16);
            }
            if (conn.isSupported(SupportedElement.PRIMARY_KEY)) {
                staticFlawsElems.addItem(L_PRIMARY_KEYS, Index.icoPkey16);
            }
            if (conn.isSupported(SupportedElement.ATTRIBUTE)) {
                staticFlawsElems.addItem(Relation.L_ATTRIBUTES, Attribute.ico16);
                dataBasedElems.addItem(Relation.L_ATTRIBUTES, Attribute.ico16);
            }
            if (conn.isSupported(SupportedElement.TRIGGER)) {
                staticFlawsElems.addItem(Relation.L_TRIGGERS, Trigger.ico16);
            }
            dataBasedElems.addItem(L_UNIQUE_DATA, Constraint.ico16);
            dataBasedElems.addItem(L_EMPTY_VALUES, Theme.getSmallIcon(Theme.ICO_DATA));
            dataBasedElems.addItem(L_NUMERIC_VALUES, Attribute.ico16);
        }
    }

    private void updateSeverity(IconableComboBox comboBox) {
        comboBox.removeAllItems();
        comboBox.addItem(ModelFlaw.L_SEVERITY_ERR_WARN);
        comboBox.addItem(ModelFlaw.L_SEVERITY_ERROR);
        comboBox.addItem(ModelFlaw.L_SEVERITY_WARNING);
        comboBox.addItem(ModelFlaw.L_SEVERITY_NOTICE);
    }

    private void updateDatabaseLists(Project p) {
        staticFlawsDBs.removeAllItems();
        dataBasedDBs.removeAllItems();
        staticFlawsDBs.addItem(L_ALL_DATABASES);
        dataBasedDBs.addItem(L_ALL_DATABASES);
        if (p.getDatabases().size() > 1) {
            for (DB db : p.getDatabases()) {
                staticFlawsDBs.addItem(db.getName());
                dataBasedDBs.addItem(db.getName());
            }
            staticFlawsDBs.setVisible(true);
            dataBasedDBs.setVisible(true);
        } else {
            staticFlawsDBs.setSelectedIndex(0);
            dataBasedDBs.setSelectedIndex(0);
            staticFlawsDBs.setVisible(false);
            dataBasedDBs.setVisible(false);
        }
    }

    @Override
    protected void setColWidths() {
        TableColumnModel colModel = staticFlawTable.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(24);
        colModel.getColumn(1).setPreferredWidth(70);
        colModel.getColumn(2).setPreferredWidth(90);
        colModel.getColumn(3).setPreferredWidth(90);
        colModel.getColumn(4).setPreferredWidth(800);
        colModel.getColumn(0).setMaxWidth(24);
        colModel.getColumn(1).setMaxWidth(70);
        colModel.getColumn(0).setMinWidth(24);
        colModel.getColumn(1).setMinWidth(70);
        staticFlawTable.getColumn("").setCellRenderer(new IconCellRenderer());
    }

    private Component drawStaticFlawsFilter() {
        JButton actionButton = new JButton("Analyze");
        actionButton.addActionListener(ae -> Schedule.inWorker(this::runStaticFlawsAnalysis));
        actionButton.setPreferredSize(new Dimension(84, 26));
        JPanel filterPanel = getTitledPanel("Static model flaws");

        staticFlawsSeverity = new IconableComboBox();
        staticFlawsSeverity.setPreferredSize(new Dimension(149, 26));
        staticFlawsSeverity.setFocusable(false);

        staticFlawsDBs = new IconableComboBox();
        staticFlawsDBs.setPreferredSize(new Dimension(149, 26));
        staticFlawsDBs.setFocusable(false);

        staticFlawsElems = new IconableComboBox();
        staticFlawsElems.setPreferredSize(new Dimension(149, 26));
        staticFlawsElems.setFocusable(false);

        filterPanel.add(staticFlawsResultPanel = new ResultPanel());
        filterPanel.add(staticFlawsSeverity);
        filterPanel.add(staticFlawsDBs);
        filterPanel.add(staticFlawsElems);
        filterPanel.add(actionButton);
        filterPanel.setPreferredSize(new Dimension(100, 28));
        return filterPanel;
    }

    private Component drawStaticFlawsTable() {
        staticFlawTable = new JTable(new StaticFlawTableModel()) {
            @Override
            public Class getColumnClass(int column) {
                return column == 0 ? ImageIcon.class : String.class;
            }
        };
        staticFlawTable.addMouseListener(new StaticFlawMouseHandler());
        staticFlawTable.getSelectionModel().addListSelectionListener(new StaticFlawSelectionListener());
        JScrollPane scroll = new JScrollPane(staticFlawTable);
        scroll.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(0, 0, 1, 2), BorderFactory.createLineBorder(UIConstants.Colors.getTableBorders())));
        return scroll;
    }

    public void runStaticFlawsAnalysis() {
        StaticFlawOptimizer optimizer = new StaticFlawOptimizer();
        optimizer.setSeverity(staticFlawsSeverity.getSelectedItem().toString());
        if (staticFlawsDBs.getSelectedItem().equals(L_ALL_DATABASES)) {
            optimizer.setDatabases(Project.getCurrent().getDatabases());
        } else {
            optimizer.setDatabase(Project.getCurrent().getDatabaseByName(staticFlawsDBs.getSelectedItem().toString()));
        }
        String selectedElem = staticFlawsElems.getSelectedItem().toString();
        if (selectedElem.equals(L_ALL_ELEMS) || selectedElem.equals(L_REFERENCES)) {
            optimizer.analyzeReferences();
        }
        if (selectedElem.equals(L_ALL_ELEMS) || selectedElem.equals(Relation.L_INDEXES)) {
            optimizer.analyzeIndexes();
        }
        if (selectedElem.equals(L_ALL_ELEMS) || selectedElem.equals(L_PRIMARY_KEYS)) {
            optimizer.analyzePrimaryKeys();
        }
        if (selectedElem.equals(L_ALL_ELEMS) || selectedElem.equals(Relation.L_ATTRIBUTES)) {
            optimizer.analyzeAttributes();
        }
        if (selectedElem.equals(L_ALL_ELEMS) || selectedElem.equals(Relation.L_TRIGGERS)) {
            optimizer.analyzeTriggers();
        }
        StaticFlawTableModel model = (StaticFlawTableModel) staticFlawTable.getModel();
        model.flaws = optimizer.flaws;
        model.fireTableDataChanged();

        staticFlawsResultPanel.setErrors(optimizer.flaws.stream().filter(modelFlaw -> modelFlaw.severity.equals(ModelFlaw.L_SEVERITY_ERROR)).count());
        staticFlawsResultPanel.setWarnings(optimizer.flaws.stream().filter(modelFlaw -> modelFlaw.severity.equals(ModelFlaw.L_SEVERITY_WARNING)).count());
        staticFlawsResultPanel.setNotices(optimizer.flaws.stream().filter(modelFlaw -> modelFlaw.severity.equals(ModelFlaw.L_SEVERITY_NOTICE)).count());

        DesignGUI.getInfoPanel().writeGreen("Static model analysis completed");
    }

    private Component drawDataBasedFilter() {
        JButton actionButton = new JButton("Analyze");
        actionButton.addActionListener(ae -> Schedule.inWorker(this::runDataBasedAnalysis));
        actionButton.setPreferredSize(new Dimension(84, 26));
        JPanel filterPanel = getTitledPanel("Data-based analysis");

        dataBasedSeverity = new IconableComboBox();
        dataBasedSeverity.setPreferredSize(new Dimension(149, 26));
        dataBasedSeverity.setFocusable(false);

        dataBasedDBs = new IconableComboBox();
        dataBasedDBs.setPreferredSize(new Dimension(149, 26));
        dataBasedDBs.setFocusable(false);

        dataBasedElems = new IconableComboBox();
        dataBasedElems.setPreferredSize(new Dimension(149, 26));
        dataBasedElems.setFocusable(false);

        filterPanel.add(dataBasedResultPanel = new ResultPanel());
        filterPanel.add(dataBasedSeverity);
        filterPanel.add(dataBasedDBs);
        filterPanel.add(dataBasedElems);
        filterPanel.add(actionButton);
        filterPanel.setPreferredSize(new Dimension(100, 28));
        return filterPanel;
    }

    private Component drawDataBasedTable() {
        return new JPanel();
    }

    private void runDataBasedAnalysis() {
        dataBasedResultPanel.setErrors(0);
        dataBasedResultPanel.setWarnings(0);
        dataBasedResultPanel.setNotices(0);

        DesignGUI.getInfoPanel().writeGreen("Data-based analysis completed");
    }

    private static class ResultPanel extends JComponent {
        JLabel errors = new JLabel();
        JLabel warnings = new JLabel();
        JLabel notices = new JLabel();

        ResultPanel() {
            setBorder(new EmptyBorder(3, 0, 0, 0));
            setLayout(new GridLayout(1, 0));
            add(new JLabel(ModelFlaw.L_SEVERITY_ERROR + "s:"));
            add(errors);
            add(new JLabel(ModelFlaw.L_SEVERITY_WARNING + "s:"));
            add(warnings);
            add(new JLabel(ModelFlaw.L_SEVERITY_NOTICE + "s:"));
            add(notices);
        }

        void setErrors(long value){
            errors.setText(Long.toString(value));
        }
        void setWarnings(long value){
            warnings.setText(Long.toString(value));
        }
        void setNotices(long value){
            notices.setText(Long.toString(value));
        }
    }

    private static class StaticFlawMouseHandler extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                JTable table = OptimizerView.instance.staticFlawTable;
                Runnable onClick = ((StaticFlawTableModel) table.getModel()).flaws.get(table.getSelectedRow()).onClick;
                if (onClick != null) {
                    Schedule.inEDT(onClick);
                }
            }
        }
    }

    private static class StaticFlawTableModel extends AbstractTableModel {
        private final String[] cols = { "", "Severity", "Element", "Problem", "Description" };
        transient List<ModelFlaw> flaws = new ArrayList<>();

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
            return flaws.size();
        }

        @Override
        public Object getValueAt(int row, int col) {
            ModelFlaw flaw = flaws.get(row);
            if (col == 0) {
                return flaw.element.getIcon16();
            } else if (col == 1) {
                // Disable colors for selected rows
                if(OptimizerView.instance.staticFlawTable.getSelectedRow() == row) {
                    return flaw.severity;
                }

                switch (flaw.severity) {
                case ModelFlaw.L_SEVERITY_ERROR:
                    return "<html><font color='#D20000'>" + flaw.severity + "</font></html>";
                case ModelFlaw.L_SEVERITY_WARNING:
                    return "<html><font color='#770000'>" + flaw.severity + "</font></html>";
                default:
                    return flaw.severity;
                }
            } else if (col == 2) {
                return flaw.element.getName();

            } else if (col == 3) {
                // Disable colors for selected rows
                if(OptimizerView.instance.staticFlawTable.getSelectedRow() == row) {
                    return flaw.title;
                }

                switch (flaw.severity) {
                case ModelFlaw.L_SEVERITY_ERROR:
                    return "<html><font color='#D20000'>" + flaw.title + "</font></html>";
                case ModelFlaw.L_SEVERITY_WARNING:
                    return "<html><font color='#770000'>" + flaw.title + "</font></html>";
                default:
                    return flaw.title;
                }
            } else if (col == 4) {
                // Disable colors for selected rows
                if(OptimizerView.instance.staticFlawTable.getSelectedRow() == row) {
                    return flaw.descriptionWoColors;
                } else {
                    return flaw.description;
                }
            }
            return "";
        }
    }

    private static class StaticFlawSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent lse) {
            if (OptimizerView.instance.staticFlawTable.getSelectedRow() >= 0) {
                ((StaticFlawTableModel) OptimizerView.instance.staticFlawTable.getModel()).flaws.get(OptimizerView.instance.staticFlawTable.getSelectedRow()).element.clicked();
            }
        }
    }
}
