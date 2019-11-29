package com.databazoo.devmodeler.gui;

import static org.junit.Assert.assertEquals;

import java.awt.*;

import javax.swing.*;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.gui.view.ViewMode;
import com.databazoo.devmodeler.model.CheckConstraint;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.model.reference.ConstraintReference;
import com.databazoo.devmodeler.model.reference.SchemaReference;
import com.databazoo.devmodeler.project.Project;


public class CanvasTest extends TestProjectSetup {
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
        initializeCanvas();
        DesignGUI.get().switchView(ViewMode.DESIGNER, false);
        Thread.sleep(500);
    }

    public static void initializeCanvas() {
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setSize(800, 600);
        Canvas.instance.setScrolls(scrollPane);
        Canvas.instance.setOverview(Navigator.instance);
        DBTree.instance.checkDB(true);
    }

    @After
    public void cleanup(){
        Project.getCurrent().setCurrentWorkspace(null);
        Canvas.instance.quirksMode = false;
    }

    @Test
    public void drawQuirks() throws Exception {
        Canvas.instance.quirksMode = true;
        DesignGUI.get().drawProject(true);
        Canvas.instance.repaint();

        SwingUtilities.invokeAndWait(() -> {
            Component[] components = Canvas.instance.getComponents();
            assertEquals(2, components.length);
            for (Component component : components) {
                Schema schema1 = (Schema) component;
                if (schema1.getName().equals("public")) {
                    assertEquals(0, schema1.getComponents().length);
                } else {
                    assertEquals(7, schema1.getComponents().length);
                }
            }
        });
    }

    @Test
    public void drawProject() throws Exception {
        DesignGUI.get().drawProject(true);
        DesignGUI.get().drawProject(true);
        Canvas.instance.repaint();

        SwingUtilities.invokeAndWait(() -> {
            Component[] components = Canvas.instance.getComponents();
            assertEquals(6, components.length);
            for (Component component : Canvas.instance.getComponents()) {
                if (component instanceof Schema) {
                    Schema schema1 = (Schema) component;
                    Component[] schemaComponents = schema1.getComponents();
                    if (schema1.getName().equals("public")) {
                        assertEquals(0, schemaComponents.length);
                    } else {
                        assertEquals(13, schemaComponents.length);
                    }
                } else if (component instanceof CheckConstraint) {
                    CheckConstraint con = (CheckConstraint) component;
                    assertEquals(checkConstraint.getName(), con.getName());
                } else if (component instanceof Constraint) {
                    Constraint con = (Constraint) component;
                    assertEquals(constraint.getName(), con.getName());
                } else if (component instanceof Trigger) {
                    Trigger trig = (Trigger) component;
                    assertEquals(trigger.getName(), trig.getName());
                } else if (!(component instanceof Navigator)) {
                    throw new IllegalStateException("Unknown component " + component);
                }
            }
        });
    }

    @Test
    public void drawWorkspace() throws Exception {
        Project.getCurrent().setCurrentWorkspace(workspace);
        DesignGUI.get().drawProject(true);
        Canvas.instance.repaint();

        Component[] components = Canvas.instance.getComponents();
        assertEquals(3, components.length);
        for (Component component : components) {
            if (component instanceof SchemaReference) {
                SchemaReference schemaReference = (SchemaReference) component;
                assertEquals(schema.getName(), schemaReference.getElement().getName());
                Component[] schemaComponents = schemaReference.getComponents();
                assertEquals(8, schemaComponents.length);
            } else if (component instanceof ConstraintReference) {
                ConstraintReference constraintReference = (ConstraintReference) component;
                assertEquals(constraint.getName(), constraintReference.getElement().getName());
            } else if (!(component instanceof Navigator)) {
                throw new IllegalStateException("Unknown component " + component);
            }
        }
    }

    @Test
    public void drawData() throws Exception {
        DesignGUI.get().switchView(ViewMode.DATA, false);
        Canvas.instance.drawProjectLater(true);
        Canvas.instance.drawProjectLater(true);
        Thread.sleep(500);
        Canvas.instance.repaint();
        Thread.sleep(100);

        SwingUtilities.invokeAndWait(() -> {
            Component[] components = Canvas.instance.getComponents();
            assertEquals(5, components.length);
            for (Component component : Canvas.instance.getComponents()) {
                if (component instanceof Schema) {
                    Schema schema1 = (Schema) component;
                    Component[] schemaComponents = schema1.getComponents();
                    if (schema1.getName().equals("public")) {
                        assertEquals(0, schemaComponents.length);
                    } else {
                        assertEquals(2, schemaComponents.length);
                    }
                } else if (component instanceof CheckConstraint) {
                    CheckConstraint con = (CheckConstraint) component;
                    assertEquals(checkConstraint.getName(), con.getName());
                } else if (component instanceof Constraint) {
                    Constraint con = (Constraint) component;
                    assertEquals(constraint.getName(), con.getName());
                } else if (!(component instanceof Navigator)) {
                    throw new IllegalStateException("Unknown component " + component);
                }
            }
        });
    }

}
