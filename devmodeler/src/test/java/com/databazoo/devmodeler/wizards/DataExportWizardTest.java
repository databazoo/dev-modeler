package com.databazoo.devmodeler.wizards;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.conn.Result;
import com.databazoo.devmodeler.conn.VirtualConnection;

public class DataExportWizardTest extends TestProjectSetup {

    private static final File FILE = new File("/tmp/testXLS.out");

    @Test
    public void exportToXLS() throws Exception {
        Result data = VirtualConnection.prepareTableContentData(relation).getAllRows(relation);
        data.showNewLine(true);
        DataExportWizard dataExportWizard = DataExportWizard.get();
        dataExportWizard.exportToXLS(FILE, data);

        String result = new String(Files.readAllBytes(Paths.get(FILE.getAbsolutePath())));

        assertEquals("\uFEFF<table><tr>\n" +
                "\t<th>test attr 1</th>\n" +
                "\t<th>test attr 2</th>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "\t<td>AAA</td>\n" +
                "\t<td>AAA</td>\n" +
                "</tr>\n" +
                "</table>", result);
    }

    @Test
    public void exportToCSV() throws Exception {
        Result data = VirtualConnection.prepareTableContentData(relation).getAllRows(relation);
        data.showNewLine(true);
        DataExportWizard dataExportWizard = DataExportWizard.get();
        dataExportWizard.exportToCSV(FILE, data);

        String result = new String(Files.readAllBytes(Paths.get(FILE.getAbsolutePath())));

        assertEquals("test attr 1;test attr 2\n" +
                "AAA;AAA\n", result);
    }

}