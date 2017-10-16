package com.databazoo.devmodeler.gui;

import static org.junit.Assert.assertEquals;

import javax.swing.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.ConnectionPg;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;

public class InfoPanelTest {
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
    public void lenghtWithOKTest() throws Exception {
        final InfoPanel infoPanel = new InfoPanel();
        infoPanel.setMaxLabelCount(5);

        int uid = 0;
        for(int i=1; i<=20; i++) {
            uid = infoPanel.write("test info " + i);
        }
        infoPanel.writeOK(uid);


        SwingUtilities.invokeAndWait(() -> {
            assertEquals(infoPanel.getMaxLabelCount(), infoPanel.getLabels().size());
            assertEquals("test info 16", infoPanel.getLabels().get(0).toString());
            assertEquals("test info 17", infoPanel.getLabels().get(1).toString());
            assertEquals("test info 18", infoPanel.getLabels().get(2).toString());
            assertEquals("test info 19", infoPanel.getLabels().get(3).toString());
            assertEquals("test info 20 OK", infoPanel.getLabels().get(4).toString());

            infoPanel.clicked();
            assertEquals(0, infoPanel.getLabels().size());
        });
    }

    @Test
    public void lenghtWithFailedTest() throws Exception {
        final InfoPanel infoPanel = new InfoPanel();
        infoPanel.setMaxLabelCount(5);

        int uid = 0;
        for(int i=1; i<=20; i++) {
            uid = infoPanel.write("test info " + i);
        }
        infoPanel.writeFailed(uid, "Just failed");

        SwingUtilities.invokeAndWait(() -> {
            assertEquals(infoPanel.getMaxLabelCount(), infoPanel.getLabels().size());
            assertEquals("test info 17", infoPanel.getLabels().get(0).toString());
            assertEquals("test info 18", infoPanel.getLabels().get(1).toString());
            assertEquals("test info 19", infoPanel.getLabels().get(2).toString());
            assertEquals("test info 20 FAILED:", infoPanel.getLabels().get(3).toString());
            assertEquals("Just failed", infoPanel.getLabels().get(4).toString());
        });
    }
}