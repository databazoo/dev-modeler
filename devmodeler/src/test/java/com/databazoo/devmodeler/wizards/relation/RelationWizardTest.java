package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.components.GCFrame;
import com.databazoo.components.GCFrameWithObservers;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.Revision;
import org.junit.Before;
import org.junit.Test;

import static com.databazoo.devmodeler.wizards.relation.RelationWizardImpl.RWTestConnection.AFTER_CREATE;
import static com.databazoo.devmodeler.wizards.relation.RelationWizardImpl.RWTestConnection.AFTER_DROP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RelationWizardTest extends TestProjectSetup {

    @Before
    public void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Test
    public void editVisibly() throws Exception {
        GCFrame.SHOW_GUI = true;
        editAttribute(relation);
    }

    @Test
    public void saveEditedTable() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizard wizard = new RelationWizard(new GCFrameWithObservers("test"));
        wizard.enableDrop();

        wizard.drawProperties(relation);
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals("DROP TABLE \"test 2\".\"test 3\";", wizard.queryInput.getText());
        assertEquals("CREATE TABLE \"test 2\".\"test 3\" (\n" +
                "\t\"test attr 1\" character varying NOT NULL DEFAULT 'defval',\t-- test attr 1\n" +
                "\t\"test attr 2\" character varying NOT NULL,\t-- test attr 2\n" +
                "\tCONSTRAINT \"test PK\"\n" +
                "\t\tPRIMARY KEY (\"test attr 1\"),\t-- test PK\n" +
                "\tCONSTRAINT \"test 6\" CHECK ( id > 0 ),\t-- test 6\n" +
                "\tCONSTRAINT \"test 5\" \n" +
                "\t\tFOREIGN KEY (\"test attr 1\")\n" +
                "\t\tREFERENCES \"test 2\".\"test 4\" (\"test attr 3\") MATCH SIMPLE\n" +
                "\t\tON UPDATE CASCADE ON DELETE CASCADE\t-- test 5\n" +
                ") WITHOUT OIDS;\n" +
                "\n" +
                "COMMENT ON COLUMN \"test 2\".\"test 3\".\"test attr 1\" IS 'test attr 1';\n" +
                "\n" +
                "COMMENT ON COLUMN \"test 2\".\"test 3\".\"test attr 2\" IS 'test attr 2';\n" +
                "\n" +
                "COMMENT ON CONSTRAINT \"test PK\" ON \"test 2\".\"test 3\" IS 'test PK';\n" +
                "\n" +
                "COMMENT ON CONSTRAINT \"test 6\" ON \"test 2\".\"test 3\" IS 'test 6';\n" +
                "\n" +
                "COMMENT ON CONSTRAINT \"test 5\" ON \"test 2\".\"test 3\" IS 'test 5';\n" +
                "\n" +
                "-- test UX\n" +
                "CREATE UNIQUE INDEX \"test UX\"\n" +
                "\tON \"test 2\".\"test 3\"\n" +
                "\tUSING btree\n" +
                "\t(\"test attr 2\");\n" +
                "\n" +
                "COMMENT ON INDEX \"test UX\" IS 'test UX';", wizard.revertSQL);

        wizard.saveEdited(true);
        assertNull(wizard.rel);
        assertEquals("DROP TABLE \"test 2\".\"test 3\";", wizard.forwardSQL);
        assertEquals("CREATE TABLE \"test 2\".\"test 3\" (\n" +
                "\t\"test attr 1\" character varying NOT NULL DEFAULT 'defval',\t-- test attr 1\n" +
                "\t\"test attr 2\" character varying NOT NULL,\t-- test attr 2\n" +
                "\tCONSTRAINT \"test PK\"\n" +
                "\t\tPRIMARY KEY (\"test attr 1\"),\t-- test PK\n" +
                "\tCONSTRAINT \"test 6\" CHECK ( id > 0 ),\t-- test 6\n" +
                "\tCONSTRAINT \"test 5\" \n" +
                "\t\tFOREIGN KEY (\"test attr 1\")\n" +
                "\t\tREFERENCES \"test 2\".\"test 4\" (\"test attr 3\") MATCH SIMPLE\n" +
                "\t\tON UPDATE CASCADE ON DELETE CASCADE\t-- test 5\n" +
                ") WITHOUT OIDS;\n" +
                "\n" +
                "COMMENT ON COLUMN \"test 2\".\"test 3\".\"test attr 1\" IS 'test attr 1';\n" +
                "\n" +
                "COMMENT ON COLUMN \"test 2\".\"test 3\".\"test attr 2\" IS 'test attr 2';\n" +
                "\n" +
                "COMMENT ON CONSTRAINT \"test PK\" ON \"test 2\".\"test 3\" IS 'test PK';\n" +
                "\n" +
                "COMMENT ON CONSTRAINT \"test 6\" ON \"test 2\".\"test 3\" IS 'test 6';\n" +
                "\n" +
                "COMMENT ON CONSTRAINT \"test 5\" ON \"test 2\".\"test 3\" IS 'test 5';\n" +
                "\n" +
                "-- test UX\n" +
                "CREATE UNIQUE INDEX \"test UX\"\n" +
                "\tON \"test 2\".\"test 3\"\n" +
                "\tUSING btree\n" +
                "\t(\"test attr 2\");\n" +
                "\n" +
                "COMMENT ON INDEX \"test UX\" IS 'test UX';", wizard.revertSQL);

        assertEquals(false, schema.getRelations().contains(relation));
        assertNull(Project.getCurrent().getWorkspaces().get(0).find(relation));
        assertEquals(false, database.getConstraints().contains(constraint));
        assertEquals(false, database.getConstraints().contains(checkConstraint));
        assertEquals(false, relation.getIndexes().contains(primaryKey));
        assertEquals(false, relation.getIndexes().contains(uniqueIndex));
        assertEquals(0, relation.getIndexes().size());
        assertEquals(0, relation.getAttributes().size());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveEditedNewTable() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.schema = schema;
        wizard.rel = new Relation(schema, schema.getName()+".temporaryTable");
        wizard.rel.setTemp();

        wizard.loadNewTablePage1();
        Thread.sleep(100);
        assertEquals(true, wizard.checkSQLChanges());

        wizard.notifyChange(Relation.Behavior.L_NAME, "new_table_123");

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals("CREATE TABLE \"test 2\".new_table_123 () WITHOUT OIDS;", wizard.queryInput.getText());
        assertEquals("DROP TABLE \"test 2\".new_table_123;", wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_CREATE));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals("new_table_123", wizard.rel.getName());
        assertEquals("CREATE TABLE \"test 2\".new_table_123 () WITHOUT OIDS;", wizard.forwardSQL);
        assertEquals("DROP TABLE \"test 2\".new_table_123;", wizard.revertSQL);

        Relation relation1 = schema.getRelations().get(schema.getRelations().size() - 1);
        relation1.checkSize();
        assertEquals("new_table_123", relation1.getName());
        assertEquals(1, revision.getCntChanges());
    }

    public static void editAttribute(Relation rel) throws InterruptedException {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizard relationWizard = RelationWizard.get(null);
        relationWizard.drawProperties(rel, 1);
        Thread.sleep(200);

        relationWizard.notifyChange(Attribute.Behavior.L_NAME, "newTestAttributeName");
        relationWizard.executeAction(RelationWizardPages.SAVE_IN_MODEL);
        relationWizard.executeAction(RelationWizard.CLOSE_WINDOW);

        assertEquals(1, revision.getCntChanges());
        assertEquals("newTestAttributeName", rel.getAttributes().get(1).getName());
    }

    @Test
    public void saveDrop() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.enableDrop();

        wizard.drawProperties(relation);
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals("DROP TABLE \"test 2\".\"test 3\";", wizard.queryInput.getText());
        assertEquals("CREATE TABLE \"test 2\".\"test 3\" (\n" +
                "\t\"test attr 1\" character varying NOT NULL DEFAULT 'defval',\t-- test attr 1\n" +
                "\t\"test attr 2\" character varying NOT NULL,\t-- test attr 2\n" +
                "\tCONSTRAINT \"test PK\"\n" +
                "\t\tPRIMARY KEY (\"test attr 1\"),\t-- test PK\n" +
                "\tCONSTRAINT \"test 6\" CHECK ( id > 0 ),\t-- test 6\n" +
                "\tCONSTRAINT \"test 5\" \n" +
                "\t\tFOREIGN KEY (\"test attr 1\")\n" +
                "\t\tREFERENCES \"test 2\".\"test 4\" (\"test attr 3\") MATCH SIMPLE\n" +
                "\t\tON UPDATE CASCADE ON DELETE CASCADE\t-- test 5\n" +
                ") WITHOUT OIDS;\n" +
                "\n" +
                "COMMENT ON COLUMN \"test 2\".\"test 3\".\"test attr 1\" IS 'test attr 1';\n" +
                "\n" +
                "COMMENT ON COLUMN \"test 2\".\"test 3\".\"test attr 2\" IS 'test attr 2';\n" +
                "\n" +
                "COMMENT ON CONSTRAINT \"test PK\" ON \"test 2\".\"test 3\" IS 'test PK';\n" +
                "\n" +
                "COMMENT ON CONSTRAINT \"test 6\" ON \"test 2\".\"test 3\" IS 'test 6';\n" +
                "\n" +
                "COMMENT ON CONSTRAINT \"test 5\" ON \"test 2\".\"test 3\" IS 'test 5';\n" +
                "\n" +
                "-- test UX\n" +
                "CREATE UNIQUE INDEX \"test UX\"\n" +
                "\tON \"test 2\".\"test 3\"\n" +
                "\tUSING btree\n" +
                "\t(\"test attr 2\");\n" +
                "\n" +
                "COMMENT ON INDEX \"test UX\" IS 'test UX';", wizard.revertSQL);

        wizard.saveEdited(false);
        assertNull(wizard.rel);
        assertTrue(wizard.getConnection().isCalled(AFTER_DROP));

        assertEquals(false, schema.getRelations().contains(relation));
        assertNull(Project.getCurrent().getWorkspaces().get(0).find(relation));
        assertEquals(false, database.getConstraints().contains(constraint));
        assertEquals(false, database.getConstraints().contains(checkConstraint));
        assertEquals(false, relation.getIndexes().contains(primaryKey));
        assertEquals(false, relation.getIndexes().contains(uniqueIndex));
        assertEquals(0, relation.getIndexes().size());
        assertEquals(0, relation.getAttributes().size());
        assertEquals(1, Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1).getCntChanges());
    }

}