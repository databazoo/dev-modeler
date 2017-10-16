package com.databazoo.components.textInput;

import javax.swing.*;
import java.awt.*;

import com.databazoo.components.AutocompletePopupMenu;
import com.databazoo.components.GCFrameWithObservers;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.ConnectionPg;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UndoableTextFieldTest {
    @BeforeClass
    public static void setProjectUp() {
        Settings.init();
        ProjectManager.getInstance().setCurrentProject(ProjectManager.getInstance().createNew("test1", Project.TYPE_PG));
        Project.getCurrent().getConnections().add(new ConnectionPg("", "", "", "", false));
    }

    @Test
    public void getPopupMenu() throws Exception {
        UndoableTextField textField = new UndoableTextField("SELECT count(*)\nFROM myTable tab\nORDER BY tab.myColu");
        textField.setCaretPosition(textField.getText().length());
        textField.setAutocomplete(new GCFrameWithObservers("test"), new String[0], new String[]{"SELECT", "FROM", "ORDER BY"}, new String[]{"varchar", "integer"});
        textField.updateAutocomplete(new String[]{"myTable", "myColumn"});

        AutocompletePopupMenu menu = textField.getPopupMenu();
        assertNotNull(menu);
        Container contentPane = menu.getContentPane();
        assertEquals(1, contentPane.getComponents().length);
        assertTrue(contentPane.getComponent(0) instanceof JMenuItem);
        assertEquals("myColumn", ((JMenuItem)contentPane.getComponent(0)).getText());
    }

    @Test
    public void moveTabs() throws Exception {
        UndoableTextField textField = new UndoableTextField("\tSELECT count(*)\n\tFROM myTable\n\tORDER BY myColumn;\n");
        textField.setCaretPosition(textField.getText().length());
        textField.setSelectionStart(0);
        textField.setSelectionEnd(textField.getText().length());

        textField.moveTabs(false);
        assertEquals("\t\tSELECT count(*)\n\t\tFROM myTable\n\t\tORDER BY myColumn;\n", textField.getText());

        textField.moveTabs(true);
        textField.moveTabs(true);
        assertEquals("SELECT count(*)\nFROM myTable\nORDER BY myColumn;\n", textField.getText());

        textField = new UndoableTextField("SELECT \n\t\tcount(*)\n    FROM \n\t\tmyTable");
        textField.setCaretPosition(textField.getText().length());
        textField.setSelectionStart(4);
        textField.setSelectionEnd(textField.getText().length());

        textField.moveTabs(false);
        assertEquals("\tSELECT \n\t\t\tcount(*)\n\t\tFROM \n\t\t\tmyTable\n", textField.getText());

        textField.moveTabs(true);
        assertEquals("SELECT \n\t\tcount(*)\n\tFROM \n\t\tmyTable\n", textField.getText());

        textField.moveTabs(true);
        assertEquals("SELECT \n\tcount(*)\nFROM \n\tmyTable\n", textField.getText());

        textField.moveTabs(true);
        assertEquals("SELECT \ncount(*)\nFROM \nmyTable\n", textField.getText());

        textField = new UndoableTextField("\nSELECT count(*)\n    FROM myTable");
        textField.setCaretPosition(textField.getText().length());
        textField.setSelectionStart(4);
        textField.setSelectionEnd(textField.getText().length());

        textField.moveTabs(false);
        assertEquals("\n\tSELECT count(*)\n\t\tFROM myTable\n", textField.getText());

        textField.moveTabs(true);
        textField.moveTabs(true);
        assertEquals("\nSELECT count(*)\nFROM myTable\n", textField.getText());
    }

    @Test
    public void selectByDoubleClick() throws Exception {
        UndoableTextField textField = new UndoableTextField("SELECT count(*)\nFROM myTable tab\nORDER BY tab.myColumn");

        textField.setCaretPosition(textField.getText().length()-3);
        textField.selectByDoubleClick();
        assertEquals("myColumn", textField.getSelectedText());

        textField.setCaretPosition(textField.getText().length()-8);
        textField.selectByDoubleClick();
        assertEquals("myColumn", textField.getSelectedText());

        textField.setCaretPosition(textField.getText().length()-9);
        textField.selectByDoubleClick();
        assertEquals("tab", textField.getSelectedText());

        textField.setCaretPosition(textField.getText().length()-12);
        textField.selectByDoubleClick();
        assertEquals("tab", textField.getSelectedText());

        textField.setCaretPosition(textField.getText().length()-13);
        textField.selectByDoubleClick();
        assertEquals("BY", textField.getSelectedText());
    }

}