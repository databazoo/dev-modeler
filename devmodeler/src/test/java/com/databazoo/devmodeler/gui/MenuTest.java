package com.databazoo.devmodeler.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.TestProjectSetup;


public class MenuTest extends TestProjectSetup {

    @BeforeClass
    public static void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Test
    public void switchView() throws Exception {
        Menu.getInstance().menuBtnViewData.doClick();
        Thread.sleep(50);
        relation.doubleClicked();
        Thread.sleep(100);

        GCFrame activeWindow = GCFrame.getActiveWindow();
        assertNotNull(activeWindow);
        assertEquals("test 2.test 3 in test 1 -  <>", activeWindow.getTitle());
        activeWindow.dispose();

        Menu.getInstance().menuBtnViewOptimizer.doClick();
        Thread.sleep(50);
        relation.doubleClicked(relation.getAttributes().get(0));
        Thread.sleep(100);

        activeWindow = GCFrame.getActiveWindow();
        assertNotNull(activeWindow);
        assertEquals("test 2.test 3", activeWindow.getTitle());
        activeWindow.dispose();

        Menu.getInstance().menuBtnViewDesigner.doClick();
        Thread.sleep(50);
        relation.doubleClicked();
        Thread.sleep(100);

        activeWindow = GCFrame.getActiveWindow();
        assertNotNull(activeWindow);
        assertEquals("test 2.test 3", activeWindow.getTitle());
        activeWindow.dispose();

        Menu.getInstance().menuBtnViewDiff.doClick();
        Thread.sleep(50);
        relation.doubleClicked(relation.getAttributes().get(0));
        Thread.sleep(100);

        activeWindow = GCFrame.getActiveWindow();
        assertNotNull(activeWindow);
        assertEquals("test 2.test 3", activeWindow.getTitle());
        activeWindow.dispose();
    }
}