package com.databazoo.devmodeler.gui.window;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.*;
import java.awt.event.MouseListener;

import org.junit.BeforeClass;
import org.junit.Test;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.TestProjectSetup;

public class ExplainContextWindowTest extends TestProjectSetup {
    @BeforeClass
    public static void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Test
    public void get() throws Exception {
        ExplainContextWindow window = ExplainContextWindow.get();
        window.draw(new Point(10, 10));
        assertTrue(window.isVisible());

        MouseListener[] listeners = window.getMouseListeners();
        assertTrue(listeners.length > 0);
        for(MouseListener mouseListener : listeners){
            mouseListener.mouseClicked(null);
        }
        assertFalse(window.isVisible());
    }

}