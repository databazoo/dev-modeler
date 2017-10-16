package com.databazoo.devmodeler.wizards.relation;

import org.junit.Before;
import org.junit.Test;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.TestProjectSetup;

public class PagesWizardTest extends TestProjectSetup {

    @Before
    public void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Test
    public void loadIntros() throws Exception {
        RelationWizard wizard = RelationWizard.get(null);
        wizard.loadAttributesIntro();
        wizard.loadConstraintsIntro();
        wizard.loadIndexesIntro();
        wizard.loadTriggersIntro();
    }
}