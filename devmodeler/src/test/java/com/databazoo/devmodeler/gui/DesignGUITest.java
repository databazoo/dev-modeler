package com.databazoo.devmodeler.gui;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.config.Config;

public class DesignGUITest extends TestProjectSetup {
    @BeforeClass
    public static void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Test
    public void drawMainWindow() throws Exception {
        DesignGUI.get().drawMainWindow();
        DesignGUI.get().frame.setVisible(false);
        assertEquals(Config.APP_NAME, DesignGUI.get().frame.getTitle());
    }

}