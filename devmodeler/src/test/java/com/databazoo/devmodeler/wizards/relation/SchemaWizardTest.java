package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.components.GCFrame;
import com.databazoo.components.GCFrameWithObservers;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.gui.CanvasTest;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.view.ViewMode;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.Revision;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.databazoo.devmodeler.wizards.relation.RelationWizardImpl.RWTestConnection.AFTER_ALTER;
import static com.databazoo.devmodeler.wizards.relation.RelationWizardImpl.RWTestConnection.AFTER_CREATE;
import static com.databazoo.devmodeler.wizards.relation.RelationWizardImpl.RWTestConnection.AFTER_DROP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SchemaWizardTest extends TestProjectSetup {

    @Before
    public void hideGUI(){
        GCFrame.SHOW_GUI = false;
    }

    @Before
    public void init() throws InterruptedException {
        setProjectUp();
        finalizeData();
        Project.getCurrent().setCurrentDB(database);
        CanvasTest.initializeCanvas();
        DesignGUI.get().switchView(ViewMode.DESIGNER, false);
        Thread.sleep(500);
    }

    @Test
    public void saveEdited() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));

        wizard.drawProperties(schema, 1);
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        wizard.notifyChange(Schema.Behavior.L_NAME, "new name");

        String expectedForward = "ALTER SCHEMA \"test 2\" RENAME TO \"new name\";";
        String expectedRevert = "ALTER SCHEMA \"new name\" RENAME TO \"test 2\";";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_ALTER));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals("temporaryTable", wizard.rel.getName());
        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        assertEquals("new name", schema.getBehavior().getName());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveNew() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);
        database.setProject(Project.getCurrent());

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));

        wizard.drawProperties(schema, 6);
        Thread.sleep(200);
        assertEquals(true, wizard.checkSQLChanges());
        Thread.sleep(500);

        wizard.notifyChange(Schema.Behavior.L_NAME, "new name");

        String expectedForward = "CREATE SCHEMA \"new name\";";
        String expectedRevert = "DROP SCHEMA \"new name\";";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_CREATE));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        List<Schema> schemas = database.getSchemas();
        Schema schema1 = schemas.get(schemas.size() - 1);
        schema1.checkSize();

        assertEquals("new name", schema1.getBehavior().getName());
        assertEquals("new name", schema1.getName());
        assertEquals("new name", schema1.toString());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveDrop() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.enableDrop();

        wizard.drawProperties(schema, 1);
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        String expectedForward = "DROP SCHEMA \"test 2\";";
        String expectedRevert = "-- test 2\n" +
                "CREATE SCHEMA \"test 2\";\n\n" +
                "COMMENT ON SCHEMA \"test 2\" IS 'test 2';";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedRevert, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_DROP));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertNull(wizard.rel);
        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedRevert, wizard.revertSQL);

        assertEquals(false, schema.getDB().getSchemas().contains(schema));
        assertEquals(1, revision.getCntChanges());
    }
}