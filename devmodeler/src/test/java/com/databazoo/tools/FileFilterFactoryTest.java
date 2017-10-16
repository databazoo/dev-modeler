package com.databazoo.tools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import org.junit.BeforeClass;
import org.junit.Test;

import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.ConnectionPg;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;


public class FileFilterFactoryTest {
    @BeforeClass
    public static void setProjectUp() {
        Settings.init();
        ProjectManager.getInstance().setCurrentProject(ProjectManager.getInstance().createNew("test1", Project.TYPE_PG));
        Project.getCurrent().getConnections().add(new ConnectionPg("", "", "", "", false));
    }

    @Test
    public void getImageFilter() throws Exception {
        FileFilter[] filter = FileFilterFactory.getImagesFilter(true);

        assertTrue(filter[0].getDescription().contains(".png"));
        assertTrue(filter[0].getDescription().contains(".jpg"));
        assertTrue(filter[0].accept(new TestFile("file.png")));
        assertTrue(filter[0].accept(new TestFile("file.jpg")));
        assertTrue(filter[0].accept(new TestFile("file.jpeg")));
        assertFalse(filter[0].accept(new TestFile("file.gif")));

        assertTrue(filter[1].getDescription().contains(".png"));
        assertTrue(filter[1].accept(new TestFile("file.png")));
        assertFalse(filter[1].accept(new TestFile("file.gif")));

        assertTrue(filter[2].getDescription().contains(".jpg"));
        assertTrue(filter[2].accept(new TestFile("file.jpg")));
        assertTrue(filter[2].accept(new TestFile("file.jpeg")));
        assertFalse(filter[2].accept(new TestFile("file.gif")));

        filter = FileFilterFactory.getImagesFilter(false);

        assertTrue(filter[0].accept(new TestFile("file.png")));
        assertFalse(filter[0].accept(new TestFile("file.gif")));

        assertTrue(filter[1].accept(new TestFile("file.jpg")));
        assertTrue(filter[1].accept(new TestFile("file.jpeg")));
        assertFalse(filter[1].accept(new TestFile("file.gif")));
    }

    @Test
    public void getXMLFilter() throws Exception {
        FileFilter filter = FileFilterFactory.getXmlFilter();

        assertTrue(filter.getDescription().contains(".xml"));
        assertTrue(filter.accept(new TestFile("file.xml")));
        assertFalse(filter.accept(new TestFile("file")));
    }

    @Test
    public void getSQLFilter() throws Exception {
        FileFilter filter = FileFilterFactory.getSqlFilter();

        assertTrue(filter.getDescription().contains(".sql"));
        assertTrue(filter.accept(new TestFile("file.sql")));
        assertFalse(filter.accept(new TestFile("file")));
    }

    @Test
    public void getImportFilter() throws Exception {
        FileFilter[] filters = FileFilterFactory.getImportFilter(true);

        assertTrue(filters[0].getDescription().contains(".dproj"));
        assertTrue(filters[0].getDescription().contains(".xml"));
        assertTrue(filters[0].accept(new TestFile("file.dproj")));
        assertTrue(filters[0].accept(new TestFile("file.xml")));
        assertFalse(filters[0].accept(new TestFile("file")));

        assertTrue(filters[1].getDescription().contains(".dproj"));
        assertTrue(filters[1].accept(new TestFile("file.dproj")));
        assertFalse(filters[1].accept(new TestFile("file")));

        assertTrue(filters[2].getDescription().contains(".xml"));
        assertTrue(filters[2].accept(new TestFile("file.xml")));
        assertFalse(filters[2].accept(new TestFile("file")));

        filters = FileFilterFactory.getImportFilter(false);

        assertTrue(filters[0].accept(new TestFile("file.dproj")));
        assertFalse(filters[0].accept(new TestFile("file")));

        assertTrue(filters[1].accept(new TestFile("file.xml")));
        assertFalse(filters[1].accept(new TestFile("file")));
    }

    @Test
    public void getExecutableFilter() throws Exception {
        FileFilter filter = FileFilterFactory.getExecutableFilter();

        assertTrue(filter.accept(new TestFile("file.exe")));
        assertTrue(filter.accept(new TestFile("file.bat")));
    }

    @Test
    public void getCSVFilter() throws Exception {
        FileFilter filter = FileFilterFactory.getCsvFilter();

        assertTrue(filter.getDescription().contains(".csv"));
        assertTrue(filter.accept(new TestFile("file.csv")));
        assertFalse(filter.accept(new TestFile("file")));
    }

    @Test
    public void getCSVorXLSFilter() throws Exception {
        FileFilter[] filter = FileFilterFactory.getXlsCsvFilter(true);

        assertTrue(filter[0].getDescription().contains(".xls"));
        assertTrue(filter[0].getDescription().contains(".csv"));
        assertTrue(filter[0].accept(new TestFile("file.xls")));
        assertTrue(filter[0].accept(new TestFile("file.csv")));
        assertFalse(filter[0].accept(new TestFile("file")));

        assertTrue(filter[1].getDescription().contains(".xls"));
        assertTrue(filter[1].accept(new TestFile("file.xls")));
        assertFalse(filter[1].accept(new TestFile("file")));

        assertTrue(filter[2].getDescription().contains(".csv"));
        assertTrue(filter[2].accept(new TestFile("file.csv")));
        assertFalse(filter[2].accept(new TestFile("file")));

        filter = FileFilterFactory.getXlsCsvFilter(false);

        assertTrue(filter[0].accept(new TestFile("file.xls")));
        assertFalse(filter[0].accept(new TestFile("file")));

        assertTrue(filter[1].accept(new TestFile("file.csv")));
        assertFalse(filter[1].accept(new TestFile("file")));
    }

    private class TestFile extends File {

        TestFile(String fileName) {
            super(fileName);
        }

        @Override
        public boolean isDirectory(){
            return false;
        }
    }

}