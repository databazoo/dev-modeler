package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.components.GCFrame;
import com.databazoo.components.GCFrameWithObservers;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Sequence;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.Revision;
import org.junit.Before;
import org.junit.Test;

import static com.databazoo.devmodeler.wizards.relation.RelationWizardImpl.RWTestConnection.AFTER_ALTER;
import static com.databazoo.devmodeler.wizards.relation.RelationWizardImpl.RWTestConnection.AFTER_CREATE;
import static com.databazoo.devmodeler.wizards.relation.RelationWizardImpl.RWTestConnection.AFTER_DROP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SequenceWizardTest extends TestProjectSetup {

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

        wizard.drawProperties(sequence);
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        wizard.notifyChange(Sequence.Behavior.L_MIN, "1000");
        wizard.notifyChange(Sequence.Behavior.L_START, "1000");

        String expectedForward = "ALTER SEQUENCE \"test sequence\"\n" +
                "\tMINVALUE 1000\n" +
                "\tSTART WITH 1000 RESTART WITH 1000;";
        String expectedRevert = "ALTER SEQUENCE \"test sequence\"\n" +
                "\tMINVALUE 0\n" +
                "\tSTART WITH 5 RESTART WITH 5;";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_ALTER));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals("temporaryTable", wizard.rel.getName());
        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        assertEquals("1000", sequence.getBehavior().getMin());
        assertEquals("1000", sequence.getBehavior().getCurrent());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveNew() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.schema = schema;
        wizard.rel = new Relation(schema, schema.getName()+".temporaryTable");
        wizard.rel.setTemp();

        wizard.loadNewSequencePage1();
        Thread.sleep(100);
        assertEquals(true, wizard.checkSQLChanges());

        wizard.notifyChange(Sequence.Behavior.L_MIN, "1000");
        wizard.notifyChange(Sequence.Behavior.L_START, "1000");

        String expectedForward = "CREATE SEQUENCE \"test 2\".new_sequence_2\n" +
                "\tSTART WITH 1000\n" +
                "\tINCREMENT BY 1\n" +
                "\tMINVALUE 1000\n" +
                "\tMAXVALUE 9223372036854775807\n" +
                "\tCYCLE;";
        String expectedRevert = "DROP SEQUENCE \"test 2\".new_sequence_2;";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_CREATE));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals("temporaryTable", wizard.rel.getName());
        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        Sequence sequence1 = schema.getSequences().get(schema.getSequences().size() - 1);
        sequence1.checkSize();

        assertEquals("1000", sequence1.getBehavior().getMin());
        assertEquals("1000", sequence1.getBehavior().getCurrent());
        assertEquals("new_sequence_2", sequence1.toString());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveDrop() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.enableDrop();

        wizard.drawProperties(sequence);
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        String expectedForward = "DROP SEQUENCE \"test sequence\";";
        String expectedRevert = "-- test sequence\n" +
                "CREATE SEQUENCE \"test sequence\"\n" +
                "\tSTART WITH 5\n" +
                "\tINCREMENT BY 1\n" +
                "\tMINVALUE 0\n" +
                "\tMAXVALUE 1000000\n" +
                "\tCYCLE;\n" +
                "\n" +
                "COMMENT ON SEQUENCE \"test sequence\" IS 'test sequence';";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_DROP));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertNull(wizard.rel);
        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        assertEquals(false, schema.getSequences().contains(sequence));
        assertEquals(1, revision.getCntChanges());
    }
}