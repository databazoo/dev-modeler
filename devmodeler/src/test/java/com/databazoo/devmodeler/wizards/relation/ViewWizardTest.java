package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.components.GCFrame;
import com.databazoo.components.GCFrameWithObservers;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.View;
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

public class ViewWizardTest extends TestProjectSetup {

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

        wizard.drawProperties(view);
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        wizard.notifyChange(View.Behavior.L_BODY, "new body");

        String expectedForward = "DROP VIEW \"test view\";\n\n" +
                "-- test view\n" +
                "CREATE OR REPLACE VIEW \"test view\"\n" +
                "\tAS new body;\n\n" +
                "COMMENT ON VIEW \"test view\" IS 'test view';";
        String expectedRevert = "DROP VIEW \"test view\";\n\n" +
                "-- test view\n" +
                "CREATE OR REPLACE VIEW \"test view\"\n" +
                "\tAS SELECT * FROM \"test 4\";\n\n" +
                "COMMENT ON VIEW \"test view\" IS 'test view';";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_ALTER));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals("temporaryTable", wizard.rel.getName());
        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        assertEquals("new body", view.getBehavior().getSrc());
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

        wizard.loadNewViewPage1();
        Thread.sleep(100);
        assertEquals(true, wizard.checkSQLChanges());

        wizard.notifyChange(View.Behavior.L_BODY, "new body");

        String expectedForward = "CREATE OR REPLACE VIEW \"test 2\".new_view_2\n\tAS new body;";
        String expectedRevert = "DROP VIEW \"test 2\".new_view_2;";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_CREATE));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals("temporaryTable", wizard.rel.getName());
        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        View view1 = schema.getViews().get(schema.getViews().size() - 1);
        view1.checkSize();

        assertEquals("new body", view1.getBehavior().getSrc());
        assertEquals("new_view_2", view1.getName());
        assertEquals("new_view_2", view1.toString());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveDrop() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.enableDrop();

        wizard.drawProperties(view);
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        String expectedForward = "DROP VIEW \"test view\";";
        String expectedRevert = "-- test view\n" +
                "CREATE OR REPLACE VIEW \"test view\"\n" +
                "\tAS SELECT * FROM \"test 4\";\n" +
                "\n" +
                "COMMENT ON VIEW \"test view\" IS 'test view';";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_DROP));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertNull(wizard.rel);
        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        assertEquals(false, schema.getViews().contains(view));
        assertEquals(1, revision.getCntChanges());
    }
}