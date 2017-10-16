package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.components.GCFrame;
import com.databazoo.components.GCFrameWithObservers;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.model.CheckConstraint;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.Revision;
import org.junit.Before;
import org.junit.Test;

import static com.databazoo.devmodeler.wizards.relation.RelationWizardImpl.RWTestConnection.AFTER_ALTER;
import static com.databazoo.devmodeler.wizards.relation.RelationWizardImpl.RWTestConnection.AFTER_CREATE;
import static com.databazoo.devmodeler.wizards.relation.RelationWizardImpl.RWTestConnection.AFTER_DROP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConstraintWizardTest extends TestProjectSetup {

    @Before
    public void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Test
    public void saveEditedFK() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));

        wizard.drawProperties(relation, constraint.getName());
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        wizard.notifyChange(Constraint.Behavior.L_ON_DELETE, "SET NULL");

        String expectedForward = "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tDROP CONSTRAINT \"test 5\";\n\n" +
                "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tADD CONSTRAINT \"test 5\"\n" +
                "\t\tFOREIGN KEY (\"test attr 1\")\n" +
                "\t\tREFERENCES \"test 2\".\"test 4\" (\"test attr 3\") MATCH SIMPLE\n" +
                "\t\tON UPDATE CASCADE ON DELETE SET NULL;\n\n" +
                "COMMENT ON CONSTRAINT \"test 5\" ON \"test 2\".\"test 3\" IS 'test 5';";
        String expectedRevert = "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tDROP CONSTRAINT \"test 5\";\n\n" +
                "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tADD CONSTRAINT \"test 5\"\n" +
                "\t\tFOREIGN KEY (\"test attr 1\")\n" +
                "\t\tREFERENCES \"test 2\".\"test 4\" (\"test attr 3\") MATCH SIMPLE\n" +
                "\t\tON UPDATE CASCADE ON DELETE CASCADE;\n\n" +
                "COMMENT ON CONSTRAINT \"test 5\" ON \"test 2\".\"test 3\" IS 'test 5';";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_ALTER));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        assertEquals("SET NULL", constraint.getBehavior().getOnDelete());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveEditedUC() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));

        wizard.drawProperties(relation2, uniqueConstraint.getName());
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        wizard.notifyChange(Index.Behavior.L_COLUMNS, "\"test attr 4\"");

        String expectedForward = "ALTER TABLE \"test 2\".\"test 4\"\n" +
                "\tDROP CONSTRAINT \"test UC\";\n\n" +
                "-- test UC\n" +
                "ALTER TABLE \"test 2\".\"test 4\"\n" +
                "\tADD CONSTRAINT \"test UC\"\n" +
                "\t\tUNIQUE (\"test attr 4\");\n\n" +
                "COMMENT ON CONSTRAINT \"test UC\" ON \"test 2\".\"test 4\" IS 'test UC';";
        String expectedRevert = "ALTER TABLE \"test 2\".\"test 4\"\n" +
                "\tDROP CONSTRAINT \"test UC\";\n\n" +
                "-- test UC\n" +
                "ALTER TABLE \"test 2\".\"test 4\"\n" +
                "\tADD CONSTRAINT \"test UC\"\n" +
                "\t\tUNIQUE (\"test attr 3\");\n\n" +
                "COMMENT ON CONSTRAINT \"test UC\" ON \"test 2\".\"test 4\" IS 'test UC';";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_ALTER));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        assertEquals(1, uniqueConstraint.getAttributes().size());
        assertEquals("test attr 4", uniqueConstraint.getAttributes().get(0).getName());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveEditedCC() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));

        wizard.drawProperties(relation, checkConstraint.getName());
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        wizard.notifyChange(Constraint.Behavior.L_DEFINITION, "id >= 1");

        String expectedForward = "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tDROP CONSTRAINT \"test 6\";\n\n" +
                "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tADD CONSTRAINT \"test 6\"\n" +
                "\t\tCHECK (id >= 1);\n\n" +
                "COMMENT ON CONSTRAINT \"test 6\" ON \"test 2\".\"test 3\" IS 'test 6';";
        String expectedRevert = "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tDROP CONSTRAINT \"test 6\";\n\n" +
                "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tADD CONSTRAINT \"test 6\"\n" +
                "\t\tCHECK ( id > 0 );\n\n" +
                "COMMENT ON CONSTRAINT \"test 6\" ON \"test 2\".\"test 3\" IS 'test 6';";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_ALTER));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        assertEquals("id >= 1", checkConstraint.getBehavior().getDef());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveNewFK() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.schema = schema;
        wizard.rel = relation;

        wizard.loadNewForeignKeyPage1();
        Thread.sleep(100);
        assertEquals(true, wizard.checkSQLChanges());

        wizard.notifyChange(Constraint.Behavior.L_ON_DELETE, "SET NULL");
        wizard.notifyChange(Constraint.Behavior.L_REM_REL, relation2.getFullName());
        wizard.notifyChange(Constraint.Behavior.L_REM_ATTR, relation2.getAttributes().get(0).getName());

        String expectedForward = "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tADD CONSTRAINT fk_test3_testattr1\n" +
                "\t\tFOREIGN KEY (\"test attr 1\")\n" +
                "\t\tREFERENCES \"test 2\".\"test 4\" (\"test attr 3\") MATCH SIMPLE\n" +
                "\t\tON UPDATE CASCADE ON DELETE SET NULL;";
        String expectedRevert = "ALTER TABLE \"test 2\".\"test 3\"\n\tDROP CONSTRAINT fk_test3_testattr1;";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_CREATE));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        Constraint constraint1 = database.getConstraintByFullName("test 2.test 3.fk_test3_testattr1");
        constraint1.checkSize();

        assertEquals("test 3", constraint1.getRel1().getName());
        assertEquals("test 4", constraint1.getRel2().getName());
        assertEquals("test attr 1", constraint1.getAttr1().getName());
        assertEquals("test attr 3", constraint1.getAttr2().getName());
        assertEquals("SET NULL", constraint1.getBehavior().getOnDelete());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveNewUC() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.schema = schema;
        wizard.rel = relation;

        wizard.loadNewUniqueConstraintPage1();
        Thread.sleep(100);
        assertEquals(true, wizard.checkSQLChanges());

        wizard.notifyChange(Index.Behavior.L_COLUMNS, "\"test attr 2\"");

        String expectedForward = "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tADD CONSTRAINT uc_test3_3\n" +
                "\t\tUNIQUE (\"test attr 2\");";
        String expectedRevert = "ALTER TABLE \"test 2\".\"test 3\"\n\tDROP CONSTRAINT uc_test3_3;";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_CREATE));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        Index constraint1 = relation.getIndexByName("uc_test3_3");
        constraint1.checkSize();

        assertEquals(1, constraint1.getAttributes().size());
        assertEquals("test attr 2", constraint1.getAttributes().get(0).getName());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveNewCC() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.schema = schema;
        wizard.rel = relation;

        wizard.loadNewCheckConstraintPage1();
        Thread.sleep(100);
        assertEquals(true, wizard.checkSQLChanges());

        wizard.notifyChange(Constraint.Behavior.L_DEFINITION, "id < 9999");

        String expectedForward = "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tADD CONSTRAINT cc_test3_3\n" +
                "\t\tCHECK (id < 9999);";
        String expectedRevert = "ALTER TABLE \"test 2\".\"test 3\"\n\tDROP CONSTRAINT cc_test3_3;";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_CREATE));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        CheckConstraint constraint1 = (CheckConstraint)relation.getConstraintByName("cc_test3_3");
        constraint1.checkSize();

        assertEquals("id < 9999", constraint1.getBehavior().getDef());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveDropFK() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.enableDrop();

        wizard.drawProperties(relation, constraint.getName());
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        String expectedForward = "ALTER TABLE \"test 2\".\"test 3\"\n\tDROP CONSTRAINT \"test 5\";";
        String expectedRevert = "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tADD CONSTRAINT \"test 5\"\n" +
                "\t\tFOREIGN KEY (\"test attr 1\")\n" +
                "\t\tREFERENCES \"test 2\".\"test 4\" (\"test attr 3\") MATCH SIMPLE\n" +
                "\t\tON UPDATE CASCADE ON DELETE CASCADE;";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_DROP));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        assertEquals(false, database.getConstraints().contains(constraint));
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveDropUC() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.enableDrop();

        wizard.drawProperties(relation2, uniqueConstraint.getName());
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        String expectedForward = "ALTER TABLE \"test 2\".\"test 4\"\n\tDROP CONSTRAINT \"test UC\";";
        String expectedRevert = "-- test UC\n" +
                "ALTER TABLE \"test 2\".\"test 4\"\n" +
                "\tADD CONSTRAINT \"test UC\"\n" +
                "\t\tUNIQUE (\"test attr 3\");\n\n" +
                "COMMENT ON CONSTRAINT \"test UC\" ON \"test 2\".\"test 4\" IS 'test UC';";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_DROP));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        assertEquals(false, relation2.getIndexes().contains(uniqueConstraint));
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveDropCC() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.enableDrop();

        wizard.drawProperties(relation, checkConstraint.getName());
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        String expectedForward = "ALTER TABLE \"test 2\".\"test 3\"\n\tDROP CONSTRAINT \"test 6\";";
        String expectedRevert = "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tADD CONSTRAINT \"test 6\"\n" +
                "\t\tCHECK ( id > 0 );";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_DROP));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        assertEquals(false, database.getConstraints().contains(checkConstraint));
        assertEquals(1, revision.getCntChanges());
    }
}