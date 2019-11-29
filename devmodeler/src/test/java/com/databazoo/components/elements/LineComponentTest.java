package com.databazoo.components.elements;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.CanvasTest;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.view.ViewMode;
import com.databazoo.devmodeler.project.Project;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.assertEquals;

public class LineComponentTest extends TestProjectSetup {
    @BeforeClass
    public static void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Before
    public void init() throws InterruptedException {
        setProjectUp();
        finalizeData();
        cleanup();
        Project.getCurrent().setCurrentDB(database);
        CanvasTest.initializeCanvas();
        DesignGUI.get().switchView(ViewMode.DESIGNER, false);
        Thread.sleep(500);
    }

    @After
    public void cleanup(){
        Project.getCurrent().setCurrentWorkspace(null);
        Canvas.instance.quirksMode = false;
    }

    @Test
    public void getAbsCenter() throws Exception {
        DesignGUI.get().drawProject(true);

        relation.setSize(200, 200);
        relation.setLocation(new Point(30, 30));

        relation2.setSize(200, 400);
        relation2.setLocation(new Point(30, 430));

        schema.checkConstraints();

        Thread.sleep(600);

        assertEquals(30, relation.getLocation().x);
        assertEquals(30, relation.getLocation().y);
        assertEquals(30, relation2.getLocation().x);
        assertEquals(430, relation2.getLocation().y);
        assertEquals(300, schema.getLocation().x);
        assertEquals(300, schema.getLocation().y);

        Point center = constraint.getAbsCenter();
        Point schemaLocation = schema.getLocation();

        assertEquals(schemaLocation.x + relation.getLocation().x + relation.getWidth()/2 + 5, center.x);
        assertEquals(503, center.y);
    }

    @Test
    public void getAbsCenter2() throws Exception {
        DesignGUI.get().drawProject(true);

        relation.setSize(200, 400);
        relation.setLocation(new Point(30, 30));

        relation2.setSize(200, 200);
        relation2.setLocation(new Point(30, 630));

        schema.checkConstraints();

        Thread.sleep(600);

        assertEquals(30, relation.getLocation().x);
        assertEquals(30, relation.getLocation().y);
        assertEquals(30, relation2.getLocation().x);
        assertEquals(630, relation2.getLocation().y);
        assertEquals(300, schema.getLocation().x);
        assertEquals(300, schema.getLocation().y);

        Point center = constraint.getAbsCenter();
        Point schemaLocation = schema.getLocation();

        assertEquals(schemaLocation.x + relation.getLocation().x + relation.getWidth()/2 + 5, center.x);
        assertEquals(703, center.y);
    }
}
