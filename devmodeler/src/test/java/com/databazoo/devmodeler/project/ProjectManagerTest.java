package com.databazoo.devmodeler.project;

import java.io.File;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProjectManagerTest {

    private static final File TEST_HOME_FOLDER = new File("/tmp/.devmodeler");

    @BeforeClass
    public static void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Before
    public void setUp() throws Exception {
        if(TEST_HOME_FOLDER.exists()){
            new File(TEST_HOME_FOLDER, "config.dat").delete();
            new File(TEST_HOME_FOLDER, "emptyDB.xml").delete();

            File test1folder = new File(TEST_HOME_FOLDER, "test1");
            if(test1folder.exists()) {
                new File(test1folder, "database1.xml").delete();
                new File(test1folder, "queries.xml").delete();
                new File(test1folder, "workspaces.xml").delete();
                new File(test1folder, "revisions").delete();
                test1folder.delete();
            }

            assertTrue(TEST_HOME_FOLDER.delete());
        }
        ProjectManager.TOO_EARLY_TO_WRITE_CONFIGS = false;
        ProjectManager.APP_HOME_FOLDER = TEST_HOME_FOLDER;
    }

    @After
    public void tearDown() throws Exception {
        Thread.sleep(2000);
        ProjectManager.TOO_EARLY_TO_WRITE_CONFIGS = true;
    }

    @Test
    public void writeAndRead() throws Exception {
        Settings.init();
        Config.init();
        ProjectManager.getInstance().getProjectList().clear();
        ProjectManager.getInstance().init(true);
        ProjectManager.getInstance().createNew("testProject 123", Project.TYPE_MARIA);
        ProjectManager.getInstance().saveProjects();
        assertEquals(1, ProjectManager.getInstance().getProjectList().size());

        ProjectManager.getInstance().getProjectList().clear();
        ProjectManager.getInstance().init(true);
        assertEquals(1, ProjectManager.getInstance().getProjectList().size());
        assertEquals("testProject 123", Project.getCurrent().getProjectName());

        Thread.sleep(2000);
    }

}