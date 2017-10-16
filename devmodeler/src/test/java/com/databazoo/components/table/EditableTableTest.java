package com.databazoo.components.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.swing.table.AbstractTableModel;
import javax.swing.text.JTextComponent;

import org.junit.BeforeClass;
import org.junit.Test;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.ConnectionPg;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;

public class EditableTableTest {

    @BeforeClass
    public static void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @BeforeClass
    public static void setProjectUp() {
        Settings.init();
        ProjectManager.getInstance().setCurrentProject(ProjectManager.getInstance().createNew("test1", Project.TYPE_PG));
        Project.getCurrent().getConnections().add(new ConnectionPg("", "", "", "", false));
    }

    @Test
    public void testCellEditors() throws Exception {
        AbstractTableModel model = new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return 1;
            }

            @Override
            public int getColumnCount() {
                return 2;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                return "TEST";
            }
        };
        EditableTable table = new EditableTable(model) {
            @Override
            protected boolean isColEditable(int colIndex) {
                return colIndex == 0;
            }
        };
        GCFrame frame = new GCFrame("test");
        frame.getContentPane().add(new LineNumberScrollPane(table));
        frame.pack();
        frame.setVisible(true);

        table.row = 0;
        table.col = 0;
        assertTrue(table.isCellEditable(0, 0));
        assertFalse(table.isCellEditable(0, 5));
        table.col = 1;
        assertFalse(table.isCellEditable(0, 1));

        BigTextTableCellEditor cellEditor = new BigTextTableCellEditor(frame, table, "TEST");

        assertEquals("TEST", cellEditor.editor.getText());
        assertEquals("TEST", cellEditor.editor.getSelectedText());

        cellEditor.cancel();
        frame.dispose();

        JTextComponent cellEditor2 = (JTextComponent) new UnfocusableTableCellEditor().getTableCellEditorComponent(table, "TEST", true, 0, 0);
        Thread.sleep(500);

        assertEquals("TEST", cellEditor2.getText());
        assertEquals("TEST", cellEditor2.getSelectedText());
    }

}