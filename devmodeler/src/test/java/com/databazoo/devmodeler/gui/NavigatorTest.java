package com.databazoo.devmodeler.gui;

import static org.junit.Assert.assertEquals;

import javax.swing.*;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.project.Project;

public class NavigatorTest extends TestProjectSetup {
    @BeforeClass
    public static void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Before
    public void init() throws InterruptedException {
        setProjectUp();
        finalizeData();
        Project.getCurrent().setCurrentDB(database);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setSize(800, 600);
        Canvas.instance.setScrolls(scrollPane);
        Canvas.instance.setOverview(Navigator.instance);
        DBTree.instance.checkDB(true);
    }

    @Test
    public void zoomIn() throws Exception {
        Navigator.instance.resetZoom();
        Navigator.instance.zoomIn();
        assertEquals(1.25, Canvas.getZoom(), 0.001);
        Navigator.instance.resetZoom();
    }

    @Test
    public void zoomOut() throws Exception {
        Navigator.instance.resetZoom();
        Navigator.instance.zoomOut();
        assertEquals(0.7, Canvas.getZoom(), 0.001);
        Navigator.instance.resetZoom();
    }

    @Test
    public void resetZoom() throws Exception {
        Navigator.instance.zoomOut();
        Navigator.instance.resetZoom();
        assertEquals(1, Canvas.getZoom(), 0.001);
    }

}