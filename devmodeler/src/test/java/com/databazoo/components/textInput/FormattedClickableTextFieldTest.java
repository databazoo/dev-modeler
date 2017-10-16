package com.databazoo.components.textInput;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import com.databazoo.components.AutocompletePopupMenu;
import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.view.ViewMode;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.project.Project;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FormattedClickableTextFieldTest extends TestProjectSetup {
    @BeforeClass
    public static void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Test
    public void drawOptionWindow() throws Exception {
        FormattedClickableTextField textField = new FormattedClickableTextField(Project.getCurrent(), "SELECT test 3");
        textField.drawOptionWindow(textField.findElements("test 3"), 9, new Point(80, 500));
        textField.drawOptionWindow(textField.findElements("test 3"), 9, new Point(80, 10));
        assertEquals(1, AutocompletePopupMenu.get().getContentPane().getComponentCount());
        assertTrue(AutocompletePopupMenu.get().getContentPane().getComponent(0) instanceof JMenuItem);
        AutocompletePopupMenu.get().dispose();
    }

    @Test
    public void findElements() throws Exception {
        FormattedClickableTextField textField = new FormattedClickableTextField(Project.getCurrent(), "SELECT test 3");

        List<IModelElement> list = textField.findElements("test");
        assertEquals(0, list.size());

        list = textField.findElements("test 3");
        assertEquals(1, list.size());
    }

    @Test
    public void doubleClick() throws Exception {
        DesignGUI.get().switchView(ViewMode.DATA, false);
        Thread.sleep(100);
        FormattedClickableTextField textField = new FormattedClickableTextField(Project.getCurrent(), "SELECT test 3");
        textField.doubleClick(relation);

        assertEquals(ViewMode.DATA, DesignGUI.getView());
    }

}