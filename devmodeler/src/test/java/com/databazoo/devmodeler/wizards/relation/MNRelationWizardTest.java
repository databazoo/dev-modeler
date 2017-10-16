package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.Revision;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MNRelationWizardTest extends TestProjectSetup {

    @Before
    public void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Test
    public void draw1N() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.add(revision);

        MNRelationWizard wizard = MNRelationWizard.get();

        wizard.drawRelation(relation, relation.getAttributes().get(0), relation2, relation2.getAttributes().get(0));
        Thread.sleep(200);

        wizard.notifyChange(Constraint.Behavior.L_REM_ATTR, relation2.getAttributes().get(1).getName());
        wizard.notifyChange(Constraint.Behavior.L_ON_DELETE, "SET NULL");

        String expectedForward = "ALTER TABLE \"test 2\".\"test 3\"\n" +
                "\tADD CONSTRAINT fk_test3_testattr1\n" +
                "\t\tFOREIGN KEY (\"test attr 1\")\n" +
                "\t\tREFERENCES \"test 2\".\"test 4\" (\"test attr 4\") MATCH SIMPLE\n" +
                "\t\tON UPDATE CASCADE ON DELETE SET NULL;";
        String expectedRevert = "ALTER TABLE \"test 2\".\"test 3\"\n\tDROP CONSTRAINT fk_test3_testattr1;";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(true);
        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        Constraint constraint1 = database.getConstraintByFullName("test 2.test 3.fk_test3_testattr1");
        assertEquals("SET NULL", constraint1.getBehavior().getOnDelete());
        assertEquals("test 3", constraint1.getRel1().getName());
        assertEquals("test 4", constraint1.getRel2().getName());
        assertEquals("test attr 1", constraint1.getAttr1().getName());
        assertEquals("test attr 4", constraint1.getAttr2().getName());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void drawMN() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.add(revision);

        MNRelationWizard wizard = MNRelationWizard.get();

        wizard.drawRelation(relation, relation.getAttributes().get(0), relation2, relation2.getAttributes().get(0));
        Thread.sleep(100);
        wizard.tree.selectRow("M:N relation");
        Thread.sleep(100);

        wizard.notifyChange(Constraint.Behavior.L_REM_ATTR, relation2.getAttributes().get(1).getName());
        wizard.notifyChange(Constraint.Behavior.L_ON_DELETE, "SET NULL");

        String expectedForward = "CREATE TABLE \"test 2\".\"test 3_test 4\" (\n" +
                "\t\"test attr 1\" character varying NOT NULL,\n" +
                "\t\"test attr 3\" character varying NOT NULL,\n" +
                "\tCONSTRAINT \"pkey_test 3_test 4\"\n" +
                "\t\tPRIMARY KEY (test attr 1,test attr 3),\n" +
                "\tCONSTRAINT fk_test3_test4_testattr1 \n" +
                "\t\tFOREIGN KEY (\"test attr 1\")\n" +
                "\t\tREFERENCES \"test 2\".\"test 3\" (\"test attr 1\") MATCH SIMPLE\n" +
                "\t\tON UPDATE CASCADE ON DELETE CASCADE,\n" +
                "\tCONSTRAINT fk_test3_test4_testattr3 \n" +
                "\t\tFOREIGN KEY (\"test attr 3\")\n" +
                "\t\tREFERENCES \"test 2\".\"test 4\" (\"test attr 3\") MATCH SIMPLE\n" +
                "\t\tON UPDATE CASCADE ON DELETE CASCADE\n" +
                ") WITHOUT OIDS;";
        String expectedRevert = "DROP TABLE \"test 2\".\"test 3_test 4\";";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(true);
        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        Relation mnRel = database.getRelationByFullName("test 2.test 3_test 4");
        assertEquals(2, mnRel.getConstraints().size());
        assertEquals("fk_test3_test4_testattr1", mnRel.getConstraints().get(0).getName());
        assertEquals("fk_test3_test4_testattr3", mnRel.getConstraints().get(1).getName());
        assertEquals("pkey_test 3_test 4", mnRel.getIndexes().get(0).getName());

        assertEquals("test 3_test 4", ((Constraint)relation.getConstraintByName("fk_test3_test4_testattr1")).getRel1().getName());
        assertEquals("test 3_test 4", ((Constraint)relation2.getConstraintByName("fk_test3_test4_testattr3")).getRel1().getName());

        assertEquals(1, revision.getCntChanges());
    }

}