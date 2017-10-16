package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.components.GCFrame;
import com.databazoo.components.GCFrameWithObservers;
import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.Relation;
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

public class FunctionWizardTest extends TestProjectSetup {

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

        wizard.drawProperties(function);
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        wizard.notifyChange(Function.Behavior.L_ARGUMENTS, "new arguments");

        String expectedForward = "DROP FUNCTION IF EXISTS \"test function\"(int input);\n" +
                "\n" +
                "-- test function\n" +
                "CREATE OR REPLACE FUNCTION \"test function\"(new arguments)\n" +
                "\tRETURNS varchar AS\n" +
                "$BODY$\n" +
                "RETURN CAST(input AS varchar);\n" +
                "$BODY$\n" +
                "\tLANGUAGE plpgsql stable\n" +
                "\tCOST 10 ROWS 100;\n" +
                "\n" +
                "COMMENT ON FUNCTION \"test function\"(new arguments) IS 'test function';";
        String expectedBackward = "DROP FUNCTION IF EXISTS \"test function\"(new arguments);\n" +
                "\n" +
                "-- test function\n" +
                "CREATE OR REPLACE FUNCTION \"test function\"(int input)\n" +
                "\tRETURNS varchar AS\n" +
                "$BODY$\n" +
                "RETURN CAST(input AS varchar);\n" +
                "$BODY$\n" +
                "\tLANGUAGE plpgsql stable\n" +
                "\tCOST 10 ROWS 100;\n" +
                "\n" +
                "COMMENT ON FUNCTION \"test function\"(int input) IS 'test function';";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedBackward, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_ALTER));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals("temporaryTable", wizard.rel.getName());
        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedBackward, wizard.revertSQL);

        assertEquals("new arguments", function.getBehavior().getArgs());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveNew() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.schema = schema;
        wizard.rel = new Relation(schema, schema.getSchemaPrefix() + "temporaryTable");
        wizard.rel.setTemp();

        wizard.loadNewFunctionPage1();
        Thread.sleep(100);
        assertEquals(true, wizard.checkSQLChanges());

        wizard.notifyChange(Function.Behavior.L_ARGUMENTS, "new arguments");

        String expectedForward = "CREATE OR REPLACE FUNCTION \"test 2\".new_function_3(new arguments)\n" +
                "\tRETURNS TABLE () AS\n" +
                "$BODY$\n" +
                "DECLARE\n" +
                "\n" +
                "BEGIN\n" +
                "\t/** TODO **/\n" +
                "END\n" +
                "$BODY$\n" +
                "\tLANGUAGE plpgsql VOLATILE\n" +
                "\tCOST 100;";
        String expectedBackward = "DROP FUNCTION IF EXISTS \"test 2\".new_function_3(new arguments);";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedBackward, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_CREATE));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals("temporaryTable", wizard.rel.getName());
        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedBackward, wizard.revertSQL);

        Function function1 = schema.getFunctions().get(schema.getFunctions().size() - 1);
        function1.checkSize();
        assertEquals("new arguments", function1.getBehavior().getArgs());
        assertEquals("new_function_3", function1.getName());
        assertEquals("new_function_3(new arguments)", function1.toString());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveNewPublic() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.schema = publicSchema;
        wizard.rel = new Relation(publicSchema, publicSchema.getSchemaPrefix() + "temporaryTable");
        wizard.rel.setTemp();

        wizard.loadNewFunctionPage1();
        Thread.sleep(100);
        assertEquals(true, wizard.checkSQLChanges());

        wizard.notifyChange(Function.Behavior.L_ARGUMENTS, "new arguments");

        String expectedForward = "CREATE OR REPLACE FUNCTION new_function_1(new arguments)\n" +
                "\tRETURNS TABLE () AS\n" +
                "$BODY$\n" +
                "DECLARE\n" +
                "\n" +
                "BEGIN\n" +
                "\t/** TODO **/\n" +
                "END\n" +
                "$BODY$\n" +
                "\tLANGUAGE plpgsql VOLATILE\n" +
                "\tCOST 100;";
        String expectedBackward = "DROP FUNCTION IF EXISTS new_function_1(new arguments);";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedBackward, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_CREATE));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertEquals("temporaryTable", wizard.rel.getName());
        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedBackward, wizard.revertSQL);

        Function function1 = publicSchema.getFunctions().get(publicSchema.getFunctions().size() - 1);
        function1.checkSize();
        assertEquals("new arguments", function1.getBehavior().getArgs());
        assertEquals("new_function_1", function1.getName());
        assertEquals("new_function_1(new arguments)", function1.toString());
        assertEquals(1, revision.getCntChanges());
    }

    @Test
    public void saveDrop() throws Exception {
        Revision revision = new Revision("test", false);
        Project.getCurrent().revisions.clear();
        Project.getCurrent().revisions.add(revision);

        RelationWizardImpl wizard = new RelationWizardImpl(new GCFrameWithObservers("test"));
        wizard.enableDrop();

        wizard.drawProperties(function);
        Thread.sleep(100);
        assertEquals(false, wizard.checkSQLChanges());
        Thread.sleep(500);

        String expectedForward = "DROP FUNCTION IF EXISTS \"test function\"(int input);";
        String expectedBackward = "-- test function\n" +
                "CREATE OR REPLACE FUNCTION \"test function\"(int input)\n" +
                "\tRETURNS varchar AS\n" +
                "$BODY$\n" +
                "RETURN CAST(input AS varchar);\n" +
                "$BODY$\n" +
                "\tLANGUAGE plpgsql stable\n" +
                "\tCOST 10 ROWS 100;\n" +
                "\n" +
                "COMMENT ON FUNCTION \"test function\"(int input) IS 'test function';";

        assertEquals(true, wizard.checkSQLChanges());
        assertEquals(expectedForward, wizard.queryInput.getText());
        assertEquals(expectedBackward, wizard.revertSQL);

        wizard.saveEdited(false);
        assertTrue(wizard.getConnection().isCalled(AFTER_DROP));
        revision = Project.getCurrent().revisions.get(Project.getCurrent().revisions.size()-1);

        assertNull(wizard.rel);
        assertEquals(expectedForward, wizard.forwardSQL);
        assertEquals(expectedBackward, wizard.revertSQL);

        assertEquals(false, schema.getFunctions().contains(function));
        assertEquals(1, revision.getCntChanges());
    }

}