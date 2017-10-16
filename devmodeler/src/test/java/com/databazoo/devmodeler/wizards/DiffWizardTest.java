package com.databazoo.devmodeler.wizards;

import java.io.File;
import java.util.HashMap;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.conn.Result;
import com.databazoo.devmodeler.conn.VirtualConnection;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.Revision;
import com.databazoo.devmodeler.tools.comparator.Comparator;
import com.databazoo.devmodeler.wizards.relation.RelationWizardTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DiffWizardTest extends TestProjectSetup {
    @BeforeClass
    public static void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Test
    public void drawRevision() throws Exception {
        RelationWizardTest.editAttribute(relation);

        Revision revision = Project.getCurrent().revisions.get(0);

        DiffWizard diffWizard = DiffWizard.get();
        diffWizard.drawRevision(revision);
        Thread.sleep(400);
        diffWizard.tree.setSelectionRow(1);
        Thread.sleep(100);
        assertEquals("ALTER TABLE \"test 2\".\"test 3\" RENAME \"test attr 2\" TO newTestAttributeName;", diffWizard.queryForward.getText());
        assertEquals("ALTER TABLE \"test 2\".\"test 3\" RENAME newTestAttributeName TO \"test attr 2\";", diffWizard.queryRevert.getText());
        diffWizard.tree.setSelectionRow(0);
        Thread.sleep(100);
        diffWizard.prepareApply("test 1");
        assertEquals("\n\n-- Change 1\nALTER TABLE \"test 2\".\"test 3\" RENAME \"test attr 2\" TO newTestAttributeName;", diffWizard.queryInputs[0].getText());
        diffWizard.prepareRevert("test 1");
        assertEquals("\n\n-- Change 1\nALTER TABLE \"test 2\".\"test 3\" RENAME newTestAttributeName TO \"test attr 2\";", diffWizard.queryInputs[0].getText());
        diffWizard.executeAction(MigWizard.CLOSE_WINDOW);
    }

    @Test
    public void drawDifference() throws Exception {
        Attribute.Behavior editable = attribute4.getBehavior().prepareForEdit();
        editable.setName("newTestAttributeName");
        attribute4.setDifferent(Comparator.DEF_CHANGE);

        DiffWizard diffWizard = DiffWizard.get();
        diffWizard.drawDifference(attribute4, "server1", "server2");
        Thread.sleep(400);
        assertEquals("ALTER TABLE \"test 2\".\"test 4\" RENAME \"test attr 4\" TO newTestAttributeName;", diffWizard.queryForward.getText());
        assertEquals("ALTER TABLE \"test 2\".\"test 4\" RENAME newTestAttributeName TO \"test attr 4\";", diffWizard.queryRevert.getText());
    }

    @Test
    public void drawAddChange() throws Exception {
        DiffWizard diffWizard = DiffWizard.get();
        diffWizard.drawAddChange("SELECT test 123;", database, Project.getCurrent().getCurrentConn());
        Thread.sleep(400);

        assertEquals(1, diffWizard.databaseCombo.getItemCount());
        assertEquals("SELECT test 123;", diffWizard.queryForward.getText());
        assertEquals("-- no revert needed", diffWizard.queryRevert.getText());
    }

    @Test
    public void getFlywayExportFile() throws Exception {
        RelationWizardTest.editAttribute(relation);

        Revision revision = Project.getCurrent().revisions.get(0);
        revision.setName("#51 something changed (by me, admittedly)");

        DiffWizard diffWizard = DiffWizard.get();
        diffWizard.drawRevision(revision);
        Thread.sleep(400);

        final File path = new File("/tmp/.devmodeler/");
        assertEquals("/tmp/.devmodeler/V0004__Something_changed_by_me_admittedly.sql", diffWizard.getFlywayExportFile(path, new File[] {
                new File(path, "V0001__Init_script.sql"),
                new File(path, "V0003__Some_update_2.sql"),
                new File(path, "R0002__Some_update.sql"),
        }).getCanonicalPath());
        assertEquals("/tmp/.devmodeler/V0001__Something_changed_by_me_admittedly.sql", diffWizard.getFlywayExportFile(path, null)
                .getCanonicalPath());
        assertEquals("/tmp/.devmodeler/V3.0.0__Something_changed_by_me_admittedly.sql", diffWizard.getFlywayExportFile(path, new File[] {
                new File(path, "V2.36.0__Init_script.sql"),
                new File(path, "V2.37.5__Some_update_2.sql"),
                new File(path, "R2.36.1__Some_update.sql"),
        }).getCanonicalPath());
    }

    @Test
    public void drawDataDifference() throws Exception {
        VirtualConnection conn = VirtualConnection.prepareTableContentData(relation);

        Result res1 = conn.getAllRows(relation);
        Result res2 = conn.getAllRows(relation);

        HashMap<Integer, Integer> columnMap = new HashMap<>();
        for (int i = 0; i < res1.getColumnCount(); i++) {
            String colName = res1.getColumnName(i);
            for (int j = 0; j < res2.getColumnCount(); j++) {
                if (res2.getColumnName(j).equalsIgnoreCase(colName)) {
                    columnMap.put(i, j);
                    break;
                }
            }
        }
        relation.setDataChanged(res1, res2, columnMap);

        DiffWizard diffWizard = DiffWizard.get();
        diffWizard.drawDataDifference(relation, "server1", "server2");
        Thread.sleep(400);
        diffWizard.tree.selectRow("Deleted");
        Thread.sleep(400);
        assertEquals("DELETE FROM \"test 2\".\"test 3\" WHERE \"test attr 1\" = 'AAA';\n", diffWizard.queryForward.getText());
        assertEquals("INSERT INTO \"test 2\".\"test 3\" (\"test attr 1\", \"test attr 2\") VALUES ('AAA', 'AAA');\n", diffWizard.queryRevert.getText());
    }

}