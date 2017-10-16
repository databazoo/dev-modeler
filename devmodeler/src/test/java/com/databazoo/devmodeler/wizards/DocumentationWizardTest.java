package com.databazoo.devmodeler.wizards;

import org.junit.BeforeClass;
import org.junit.Test;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.TestProjectSetup;


public class DocumentationWizardTest extends TestProjectSetup {
    @BeforeClass
    public static void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Test
    public void draw() throws Exception {
        DocumentationWizard wizard = DocumentationWizard.getInstance();
        wizard.draw();
        wizard.tree.selectRow(DocumentationWizard.UseCase.UC_01.toString());
        Thread.sleep(50);
        wizard.tree.selectRow(DocumentationWizard.UseCase.UC_02.toString());
        Thread.sleep(50);
        wizard.tree.selectRow(DocumentationWizard.UseCase.UC_03.toString());
        Thread.sleep(50);
        wizard.tree.selectRow(DocumentationWizard.UseCase.UC_04.toString());
        Thread.sleep(50);
        wizard.tree.selectRow(DocumentationWizard.UseCase.UC_05.toString());
        Thread.sleep(50);
        wizard.tree.selectRow(DocumentationWizard.UseCase.UC_06.toString());
        Thread.sleep(50);
        wizard.tree.selectRow(DocumentationWizard.UseCase.UC_07.toString());
        Thread.sleep(50);
        wizard.tree.selectRow(DocumentationWizard.UseCase.UC_08.toString());
        Thread.sleep(50);
        wizard.tree.selectRow(DocumentationWizard.UseCase.UC_09.toString());
        Thread.sleep(50);
        wizard.tree.selectRow(DocumentationWizard.UseCase.UC_10.toString());
        Thread.sleep(50);
        wizard.tree.selectRow(DocumentationWizard.UseCase.UC_11.toString());
        Thread.sleep(50);
        wizard.tree.selectRow(DocumentationWizard.UseCase.UC_12.toString());
        Thread.sleep(50);
        wizard.tree.selectRow(DocumentationWizard.UseCase.UC_13.toString());
        Thread.sleep(50);
        wizard.tree.selectRow(DocumentationWizard.UseCase.UC_14.toString());
        Thread.sleep(50);
        wizard.tree.selectRow(IntroWizard.L_TUTORIAL);
        Thread.sleep(50);
        wizard.tree.selectRow(IntroWizard.L_TUTORIAL_1);
        Thread.sleep(50);
        wizard.tree.selectRow(IntroWizard.L_TUTORIAL_2);
        Thread.sleep(50);
        wizard.tree.selectRow(IntroWizard.L_TUTORIAL_3);
        Thread.sleep(50);
        wizard.executeAction(1);
    }

}