package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.components.GCFrame;
import com.databazoo.components.GCFrameWithObservers;
import com.databazoo.devmodeler.TestProjectSetup;
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

public class IndexWizardTest extends TestProjectSetup {

    @Before
    public void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Test
    public void saveEditedPK() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));

        wizard.drawProperties(relation, primaryKey.getName());
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        wizard.notifyChange(Index.Behavior.L_NAME, "new PK name");

        String expectedForward = "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tDROP CONSTRAINT \"test PK\";\n" +
                "\n" +
                "-- test PK\n" +
                "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tADD CONSTRAINT \"new PK name\"\n" +
                "\t\tPRIMARY KEY (\"test attr 1\");\n" +
                "\n" +
                "COMMENT ON CONSTRAINT \"new PK name\" ON \"test 2\".\"test 3\" IS 'test PK';";
        String expectedRevert = "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tDROP CONSTRAINT \"new PK name\";\n" +
                "\n" +
                "-- test PK\n" +
                "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tADD CONSTRAINT \"test PK\"\n" +
                "\t\tPRIMARY KEY (\"test attr 1\");\n" +
                "\n" +
                "COMMENT ON CONSTRAINT \"test PK\" ON \"test 2\".\"test 3\" IS 'test PK';";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_ALTER));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        assertEquals("new PK name", primaryKey.getName());
        assertEquals("new PK name", primaryKey.getBehavior().getName());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveEditedUX() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));

        wizard.drawProperties(relation, uniqueIndex.getName());
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        wizard.notifyChange(Index.Behavior.L_NAME, "new index name");

        String expectedForward = "ALTER INDEX \"test UX\" RENAME TO \"new index name\";";
        String expectedRevert = "ALTER INDEX \"new index name\" RENAME TO \"test UX\";";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_ALTER));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        assertEquals("new index name", uniqueIndex.getName());
        assertEquals("new index name", uniqueIndex.getBehavior().getName());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveNewPK() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        primaryKey.drop();

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.schema = schema;
        wizard.rel = relation;

        wizard.loadNewPrimaryKeyPage1();
        Thread.sleep(100);
        assertEquals(true, wizard.checkSQLChanges());

        wizard.notifyChange(Index.Behavior.L_NAME, "new PK");
        wizard.notifyChange(Index.Behavior.L_COLUMNS, "\"test attr 1\"");

        String expectedForward = "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tADD CONSTRAINT \"new PK\"\n" +
                "\t\tPRIMARY KEY (\"test attr 1\");";
        String expectedRevert = "ALTER TABLE \"test 2\".\"test 3\"\n\tDROP CONSTRAINT \"new PK\";";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_CREATE));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        Index index1 = relation.getIndexes().get(relation.getIndexes().size() - 1);
        index1.checkSize();

        assertEquals(1, index1.getAttributes().size());
        assertEquals("test attr 1", index1.getAttributes().get(0).getName());
        assertEquals("new PK", index1.getName());
        assertEquals("new PK", index1.toString());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveNewIX() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.schema = schema;
        wizard.rel = relation;

        wizard.loadNewIndexPage1();
        Thread.sleep(100);
        assertEquals(true, wizard.checkSQLChanges());

        wizard.notifyChange(Index.Behavior.L_NAME, "new index name");
        wizard.notifyChange(Index.Behavior.L_COLUMNS, "\"test attr 1\"");

        String expectedForward = "CREATE INDEX \"new index name\"\n" +
                "\tON \"test 2\".\"test 3\"\n" +
                "\tUSING btree\n" +
                "\t(\"test attr 1\");";
        String expectedRevert = "DROP INDEX \"test 2\".\"new index name\";";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_CREATE));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        Index index1 = relation.getIndexes().get(relation.getIndexes().size() - 1);
        index1.checkSize();

        assertEquals(1, index1.getAttributes().size());
        assertEquals("test attr 1", index1.getAttributes().get(0).getName());
        assertEquals("new index name", index1.getName());
        assertEquals("new index name", index1.toString());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveDropPK() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.enableDrop();

        wizard.drawProperties(relation, primaryKey.getName());
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        String expectedForward = "ALTER TABLE \"test 2\".\"test 3\"\n\tDROP CONSTRAINT \"test PK\";";
        String expectedRevert = "-- test PK\n" +
                "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tADD CONSTRAINT \"test PK\"\n" +
                "\t\tPRIMARY KEY (\"test attr 1\");\n" +
                "\n" +
                "COMMENT ON CONSTRAINT \"test PK\" ON \"test 2\".\"test 3\" IS 'test PK';";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_DROP));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        assertEquals(false, relation.getIndexes().contains(primaryKey));
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveDropUX() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.enableDrop();

        wizard.drawProperties(relation, uniqueIndex.getName());
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        String expectedForward = "DROP INDEX \"test UX\";";
        String expectedRevert = "-- test UX\n" +
                "CREATE UNIQUE INDEX \"test UX\"\n" +
                "\tON \"test 2\".\"test 3\"\n" +
                "\tUSING btree\n" +
                "\t(\"test attr 2\");\n\n" +
                "COMMENT ON INDEX \"test UX\" IS 'test UX';";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_DROP));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        assertEquals(false, relation.getIndexes().contains(uniqueIndex));
        assertEquals(1, revision.getCntChanges());
    }
}