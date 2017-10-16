package com.databazoo.devmodeler.gui;

import java.awt.*;
import java.awt.event.ActionEvent;

import com.databazoo.components.GCFrame;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.databazoo.devmodeler.gui.RightClickMenu.ICO_NEW;
import static com.databazoo.devmodeler.gui.RightClickMenu.ICO_VACUUM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RightClickMenuTest {
    @BeforeClass
    public static void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    private boolean actionInvoked;

    @Test
    public void get() throws Exception {
        actionInvoked = false;
        RightClickMenu.setLocationTo(null, new Point(10, 10));
        RightClickMenu menu = RightClickMenu.get((type, selectedValue) -> {
            actionInvoked = true;
            assertEquals(10, type);
            assertEquals("test 1", selectedValue);
        });
        menu
                .addItem("test 1", 10)
                .separator()
                .addItem("test 2", ICO_NEW, 20)
                .addItem("test 3", ICO_VACUUM, 30, new String[]{"Create test 3.1", "test 3.2"});
        assertEquals(4, menu.getComponents().length);

        menu.actionPerformed(new ActionEvent(this, 10, "test 1"));
        assertTrue(actionInvoked);

        menu.setVisible(false);
    }

}