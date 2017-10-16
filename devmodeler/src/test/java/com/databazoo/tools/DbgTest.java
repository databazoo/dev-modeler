package com.databazoo.tools;

import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.ConnectionPg;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;

import static com.databazoo.tools.Dbg.ERROR_DIR;
import static com.databazoo.tools.Dbg.LOG_FILE;
import static org.junit.Assert.assertEquals;


public class DbgTest {
    @BeforeClass
    public static void setProjectUp() {
        Settings.init();
        ProjectManager.getInstance().setCurrentProject(ProjectManager.getInstance().createNew("test1", Project.TYPE_PG));
        Project.getCurrent().getConnections().add(new ConnectionPg("", "", "", "", false));
    }

    @Test
    public void backupErrorOuter() throws Exception {
        Thread.sleep(200);

        if(!LOG_FILE.exists()){
            PrintWriter writer = new PrintWriter(LOG_FILE, "UTF-8");
            writer.println("Some");
            writer.println("Text");
            writer.close();
        }
        Dbg.toFile("Just a test with \\'apostrophes\\'");
        Thread.sleep(100);

        int cnt, newCnt;
        File[] files = ERROR_DIR.listFiles();
        cnt = files != null ? files.length : 0;

        Dbg.backupError(true);
        Thread.sleep(5000);

        files = ERROR_DIR.listFiles();
        newCnt = files != null ? files.length : 0;

        assertEquals(0, newCnt - cnt);
    }

    @Test
    public void notImportant(){
        Dbg.notImportant("Just a test", new IllegalStateException());
    }

}