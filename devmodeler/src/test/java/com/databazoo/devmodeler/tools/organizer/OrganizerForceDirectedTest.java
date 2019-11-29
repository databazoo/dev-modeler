package com.databazoo.devmodeler.tools.organizer;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.CanvasTest;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.project.Project;
import org.junit.BeforeClass;
import org.junit.Test;


public class OrganizerForceDirectedTest extends TestProjectSetup {
    @BeforeClass
    public static void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Test
    public void organize() throws Exception {
        Project.getCurrent().setCurrentDB(database);
        CanvasTest.initializeCanvas();
        DesignGUI.get().drawProject(true);

        Organizer organizer = OrganizerFactory.getForceDirected();
        organizer.organize(database);
    }

}
