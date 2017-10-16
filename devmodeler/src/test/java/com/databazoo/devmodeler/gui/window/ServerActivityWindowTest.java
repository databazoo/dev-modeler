package com.databazoo.devmodeler.gui.window;

import javax.swing.*;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.conn.VirtualConnection;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ServerActivityWindowTest extends TestProjectSetup {
    @BeforeClass
    public static void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Test
    public void basicDraw() throws Exception {
        VirtualConnection data = VirtualConnection.prepareServerActivityData();
        final ServerActivityWindow window = ServerActivityWindow.get(data, database.getName());
        Thread.sleep(100);

        window.setProfilerEnabled(true);
        window.updateProfilerUI();
        assertEquals(1, window.tabbedPane.getSelectedIndex());

        window.loadServerStatus();
        data.reset();

        Thread.sleep(100);
        window.togglePause();
        window.togglePause();
        data.reset();

        Thread.sleep(100);
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(6, window.processTable.getColumnCount());
            assertEquals(1, window.processTable.getRowCount());
            assertEquals(2, window.profilerTable.getColumnCount());
            assertEquals(1, window.profilerTable.getRowCount());
        });

        window.clearProfiler();
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(1, window.processTable.getRowCount());
            assertEquals(0, window.profilerTable.getRowCount());
        });

        window.setProfilerEnabled(false);
        window.updateProfilerUI();
        assertEquals(0, window.tabbedPane.getSelectedIndex());

        window.frame.dispose();
    }

}