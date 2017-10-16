package com.databazoo.devmodeler.gui.window;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import javax.swing.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.databazoo.components.GCFrame;

public class ProgressWindowTest {
    @BeforeClass
    public static void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Test
    public void getTest() throws Exception {
        final ProgressWindow window = new ProgressWindow.Builder().withParentWindow(null).withParts(3).withTitle("test 1").build();
        window.partLoaded();
        window.partLoaded();
        window.partLoaded();
        window.done();

        assertEquals(3, window.progressbar.getMaximum());
        assertEquals(3, window.progressbar.getValue());
        assertEquals(3, window.lastPart);

        SwingUtilities.invokeAndWait(() -> assertFalse(window.win.isVisible()));
    }

}