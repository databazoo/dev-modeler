package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.components.GCFrame;
import com.databazoo.components.GCFrameWithObservers;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.Revision;
import org.junit.Before;
import org.junit.Test;

import static com.databazoo.devmodeler.wizards.relation.RelationWizardImpl.RWTestConnection.AFTER_ALTER;
import static com.databazoo.devmodeler.wizards.relation.RelationWizardImpl.RWTestConnection.AFTER_CREATE;
import static com.databazoo.devmodeler.wizards.relation.RelationWizardImpl.RWTestConnection.AFTER_DROP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TriggerWizardTest extends TestProjectSetup {

    @Before
    public void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Test
    public void saveEdited() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));

        wizard.drawProperties(relation2, trigger.getName());
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        wizard.notifyChange(Trigger.Behavior.L_TRIGGER, "AFTER");

        String expectedForward = "DROP TRIGGER \"test trigger\" ON \"test 2\".\"test 4\";\n\n" +
                "-- test trigger\n" +
                "CREATE TRIGGER \"test trigger\"\n" +
                "\tAFTER UPDATE OF test attr 3, test attr 4\n" +
                "\tON \"test 2\".\"test 4\"\n" +
                "\tFOR EACH ROW\n" +
                "\tWHEN (\"test attr 4\" IS NOT NULL)\n" +
                "\tEXECUTE PROCEDURE \"test trigger function\"();\n\n" +
                "ALTER TABLE \"test 2\".\"test 4\" DISABLE TRIGGER \"test trigger\";\n\n" +
                "COMMENT ON TRIGGER \"test trigger\" ON \"test 2\".\"test 4\" IS 'test trigger';";
        String expectedRevert = "DROP TRIGGER \"test trigger\" ON \"test 2\".\"test 4\";\n\n" +
                "-- test trigger\n" +
                "CREATE TRIGGER \"test trigger\"\n" +
                "\tBEFORE UPDATE OF test attr 3, test attr 4\n" +
                "\tON \"test 2\".\"test 4\"\n" +
                "\tFOR EACH ROW\n" +
                "\tWHEN (\"test attr 4\" IS NOT NULL)\n" +
                "\tEXECUTE PROCEDURE \"test trigger function\"();\n\n" +
                "ALTER TABLE \"test 2\".\"test 4\" DISABLE TRIGGER \"test trigger\";\n\n" +
                "COMMENT ON TRIGGER \"test trigger\" ON \"test 2\".\"test 4\" IS 'test trigger';";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_ALTER));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        assertEquals("AFTER UPDATE", trigger.getBehavior().getTiming());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveNew() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.schema = relation2.getSchema();
        wizard.rel = relation2;

        wizard.loadNewTriggerPage1();
        Thread.sleep(100);
        assertEquals(true, wizard.checkSQLChanges());

        wizard.notifyChange(Trigger.Behavior.L_EXECUTE, triggerFunction.getName());
        wizard.notifyChange(Trigger.Behavior.L_TRIGGER, "AFTER");
        wizard.notifyChange(Trigger.Behavior.L_EVENT, new boolean[]{true, true, true, false});

        String expectedForward = "CREATE TRIGGER tr_test4_2\n" +
                "\tAFTER INSERT OR UPDATE OR DELETE\n" +
                "\tON \"test 2\".\"test 4\"\n" +
                "\tFOR EACH ROW\n" +
                "\tEXECUTE PROCEDURE \"test trigger function\"();";
        String expectedRevert = "DROP TRIGGER tr_test4_2 ON \"test 2\".\"test 4\";";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_CREATE));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        Trigger trigger1 = database.getTriggerByFullName("test 2.test 4.tr_test4_2");
        trigger1.checkSize();

        assertEquals("AFTER INSERT UPDATE DELETE", trigger1.getBehavior().getTiming());
        assertEquals("tr_test4_2", trigger1.getName());
        assertEquals("tr_test4_2", trigger1.toString());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveDrop() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.enableDrop();

        wizard.drawProperties(relation2, trigger.getName());
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        String expectedForward = "DROP TRIGGER \"test trigger\" ON \"test 2\".\"test 4\";";
        String expectedRevert = "-- test trigger\n" +
                "CREATE TRIGGER \"test trigger\"\n" +
                "\tBEFORE UPDATE OF test attr 3, test attr 4\n" +
                "\tON \"test 2\".\"test 4\"\n" +
                "\tFOR EACH ROW\n" +
                "\tWHEN (\"test attr 4\" IS NOT NULL)\n" +
                "\tEXECUTE PROCEDURE \"test trigger function\"();\n\n" +
                "ALTER TABLE \"test 2\".\"test 4\" DISABLE TRIGGER \"test trigger\";\n\n" +
                "COMMENT ON TRIGGER \"test trigger\" ON \"test 2\".\"test 4\" IS 'test trigger';";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_DROP));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        assertEquals(false, database.getTriggers().contains(trigger));
        assertEquals(1, revision.getCntChanges());
    }
}