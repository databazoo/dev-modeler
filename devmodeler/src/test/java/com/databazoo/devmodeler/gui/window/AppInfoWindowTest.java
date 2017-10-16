package com.databazoo.devmodeler.gui.window;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.tools.Schedule;
import org.junit.BeforeClass;
import org.junit.Test;


public class AppInfoWindowTest extends TestProjectSetup {
    @BeforeClass
    public static void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Test
    public void drawInfo() throws Exception {
        final AppInfoWindow window = AppInfoWindow.get();
        Schedule.inEDT(200, window::dispose);
        window.drawInfo();
    }

}