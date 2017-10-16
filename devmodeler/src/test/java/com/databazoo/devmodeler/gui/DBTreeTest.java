package com.databazoo.devmodeler.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.swing.*;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.project.Project;

public class DBTreeTest extends TestProjectSetup {
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
        DBTree.instance.checkDB(true);
        Canvas.instance.drawProject(true);
        Thread.sleep(500);
    }

    @Test
    public void selectCurrentDB() throws Exception {
        DBTree.instance.selectCurrentDB();
        assertNull(Canvas.instance.getSelectedElement());
    }

    @Test
    public void selectSchemaByName() throws Exception {
        DBTree.instance.selectSchemaByName(schema.getName());

        assertNotNull(Canvas.instance.getSelectedElement());
        assertEquals(schema, Canvas.instance.getSelectedElement());
        assertTrue(schema.isSelected());
    }

    @Test
    public void selectRelationByName() throws Exception {

        DBTree.instance.selectRelationByName(relation2.getDB().getName(), relation2.getSchema().getName(), relation2.getName());

        assertNotNull(Canvas.instance.getSelectedElement());
        assertEquals(relation2, Canvas.instance.getSelectedElement());
        assertTrue(relation2.isSelected());
    }

}