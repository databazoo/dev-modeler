package com.databazoo.devmodeler.tools.comparator;

import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.ConnectionPg;
import com.databazoo.devmodeler.conn.ConnectionUtils;
import com.databazoo.devmodeler.conn.VirtualConnection;
import com.databazoo.devmodeler.model.*;
import com.databazoo.devmodeler.model.Package;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;
import com.databazoo.devmodeler.project.Revision;
import com.databazoo.devmodeler.project.RevisionFactory;
import com.databazoo.tools.Dbg;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ComparatorTest {
    private DB db1, db2;
    private Schema schema1;
    private Schema schema2;
    private Schema schema3;
    private Relation rel1, rel2;

    @Before
    public void init() {
        Settings.init();

        ProjectManager.getInstance().setCurrentProject(ProjectManager.getInstance().createNew("test1", Project.TYPE_PG));
        Project.getCurrent().getConnections().add(new ConnectionPg("", "", "", ""));

        db1 = new DB(Project.getCurrent(), "db1");
        db2 = new DB(Project.getCurrent(), "db2");

        schema1 = new Schema(db1, "schema1", "schema1");
        schema1.assignToDB(db1);
        schema2 = new Schema(db1, "schema2", "schema2");
        schema2.assignToDB(db1);
        schema3 = new Schema(db2, "schema3", "schema3");
        schema3.assignToDB(db2);
        Schema schema4 = new Schema(db2, "schema4", "schema4");
        schema4.assignToDB(db2);

        rel1 = new Relation(schema1, "schema1.rel1");
        rel1.assignToSchema();

        Attribute attr1 = new Attribute(rel1, "attr1", "int4", true, 1, "", "p", "");
        attr1.assignToRels();
        Attribute attr2 = new Attribute(rel1, "attr2", "varchar", true, 2, "", "p", "");
        attr2.assignToRels();

        rel2 = new Relation(schema1, "schema1.rel2");
        rel2.assignToSchema();

        Attribute attr3 = new Attribute(rel2, "attr3", "int4", true, 1, "", "p", "");
        attr3.assignToRels();
        Attribute attr4 = new Attribute(rel2, "attr4", "varchar", true, 2, "", "p", "");
        attr4.assignToRels();

        Function func1 = new Function(schema1, "schema1.func1", "trigger", "", "BEGIN\n\nRETURN NEW;\n\nEND", "plpgsql", "volatile", true, 100, 1000, "");
        func1.assignToSchema();
        Function func2 = new Function(schema1, "schema1.func2", "trigger", "", "BEGIN\n\nRETURN NEW;\n\nEND", "plpgsql", "volatile", true, 100, 1000, "");
        func2.assignToSchema();

        Package pack1 = new Package(schema1, "package1", "");
        pack1.assignToSchema();
        Package pack2 = new Package(schema1, "package2", "");
        pack2.assignToSchema();

        View view1 = new View(schema1, "schema1.view1", false, "BODY", "");
        view1.assignToSchema();
        View view2 = new View(schema1, "schema1.view2", false, "BODY", "");
        view2.assignToSchema();

        Sequence sequence1 = new Sequence(schema1, "schema1.sequence1", new String[0], "1", "1", "1000", "1", true, "");
        sequence1.assignToSchemaAndAttributes();
        Sequence sequence2 = new Sequence(schema1, "schema1.sequence2", new String[0], "1", "1", "1000", "1", true, "");
        sequence2.assignToSchemaAndAttributes();

        Index ind1 = new Index(rel1, "index1", attr1.getName(), "", "btree", new String[]{}, false, false, false, "index1");
        ind1.assignToRelation(rel1);
        Index ind2 = new Index(rel1, "index2", attr2.getName(), "", "btree", new String[]{}, false, false, false, "index2");
        ind2.assignToRelation(rel1);

        Constraint con1 = new Constraint(db1, "con1", "CASCADE", "CASCADE", "con1");
        con1.setRelsAttrsByName(db1, rel1.getFullName(), rel2.getFullName(), attr1.getName(), attr3.getName(), false);
        Constraint con2 = new Constraint(db1, "con2", "CASCADE", "CASCADE", "con2");
        con2.setRelsAttrsByName(db1, rel1.getFullName(), rel2.getFullName(), attr2.getName(), attr4.getName(), false);

        Trigger trig1 = new Trigger(db1, "trig1", func1.getFullName(), "", "", true, "");
        trig1.setRelFuncByNameDef(db1, rel1.getFullName(), "EXECUTE PROCEDURE "+ func1.getFullName()+"()", false);
        Trigger trig2 = new Trigger(db1, "trig2", func1.getFullName(), "", "", true, "");
        trig2.setRelFuncByNameDef(db1, rel1.getFullName(), "EXECUTE PROCEDURE "+ func1.getFullName()+"()", false);
    }

    @Test
    public void testCompareDBs() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareDBs(db1, db1);
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withReportOnly();
        comparator.compareDBs(db1, db2);
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareDBs(db1, db1);
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareDBs(db1, db2);
        assertTrue(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());
    }

    @Test
    public void testCompareSchemata() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareSchemata(db1, db1.getSchemas(), db1.getSchemas());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withReportOnly();
        comparator.compareSchemata(db1, db1.getSchemas(), db2.getSchemas());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareSchemata(db1, db1.getSchemas(), db1.getSchemas());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareSchemata(db1, db1.getSchemas(), db2.getSchemas());
        assertTrue(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());
    }

    @Test
    public void testCompareSchemataAddRemove() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareSchemata(db1, db1.getSchemas(), db2.getSchemas());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(4, comparator.getDifferences().size());

        comparator = Comparator.withAutoApply();
        comparator.compareSchemata(db1, db1.getSchemas(), db2.getSchemas());
        assertTrue(comparator.hadDifference());
        assertEquals(2, db1.getSchemas().size());
        assertTrue(db1.getSchemas().containsAll(db2.getSchemas()));
    }

    @Test
    public void testCompareRelations() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareRelations(schema1, schema1.getRelations(), schema1.getRelations());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withReportOnly();
        comparator.compareRelations(schema1, schema1.getRelations(), schema2.getRelations());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareRelations(schema1, schema1.getRelations(), schema1.getRelations());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareRelations(schema1, schema1.getRelations(), schema2.getRelations());
        assertTrue(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());
    }

    @Test
    public void testCompareRelationsAdd() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareRelations(schema3, schema3.getRelations(), schema1.getRelations());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(2, comparator.getDifferences().size());
        for(IModelElement element : comparator.getDifferences()){
            assertEquals(Comparator.IS_MISSING, element.getDifference());
        }

        comparator = Comparator.withAutoApply();
        comparator.compareRelations(schema3, schema3.getRelations(), schema1.getRelations());
        assertTrue(comparator.hadDifference());
        assertEquals(2, schema3.getRelations().size());
        assertTrue(schema3.getRelations().containsAll(schema1.getRelations()));
    }

    @Test
    public void testCompareRelationsRemove() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareRelations(schema1, schema1.getRelations(), schema3.getRelations());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(2, comparator.getDifferences().size());
        for(IModelElement element : comparator.getDifferences()){
            assertEquals(Comparator.IS_ADDED, element.getDifference());
        }

        comparator = Comparator.withAutoApply();
        comparator.compareRelations(schema1, schema1.getRelations(), schema3.getRelations());
        assertTrue(comparator.hadDifference());
        assertEquals(0, schema1.getRelations().size());
    }

    @Test
    public void testCompareRelationsDescription() {
        Schema schema_loc1 = new Schema(db1, "schema1", "schema1");
        Relation rel_loc1 = new Relation(schema_loc1, "schema1.rel1");
        rel_loc1.assignToSchema();
        rel_loc1.getBehavior().setDescr(null);

        Schema schema_loc2 = new Schema(db2, "schema1", "schema1");
        Relation rel_loc2 = new Relation(schema_loc2, "schema1.rel1");
        rel_loc2.assignToSchema();

        Comparator comparator = Comparator.withAutoApply();
        comparator.compareRelations(schema_loc1, schema_loc1.getRelations(), schema_loc2.getRelations());
        assertFalse(comparator.hadDifference());

        rel_loc1.getBehavior().setHasOIDs(true);

        comparator = Comparator.withAutoApply();
        comparator.compareRelations(schema_loc1, schema_loc1.getRelations(), schema_loc2.getRelations());
        assertTrue(comparator.hadDifference());

        Revision.Diff change = RevisionFactory.getCurrentIncoming(ConnectionUtils.getCurrent(schema_loc1.getDB().getName())).getChanges().get(0);
        assertFalse(change.getForwardSQL().contains("COMMENT ON "));
        assertFalse(change.getRevertSQL().contains("COMMENT ON "));
    }

    @Test
    public void testCompareRelationsWithConstraint() {
        Schema schema_loc1 = new Schema(db1, "schema_loc1", "schema_loc1");
        schema_loc1.assignToDB(db1);
        Relation rel_loc1 = new Relation(schema_loc1, "schema_loc1.rel_loc1");
        rel_loc1.assignToSchema();

        Attribute attr_loc1 = new Attribute(rel_loc1, "id", "int4", true, 0, "", "p", "");
        attr_loc1.assignToRels();

        Schema schema_loc2 = new Schema(db2, "schema_loc1", "schema_loc1");
        schema_loc2.assignToDB(db2);
        Relation rel_loc2 = new Relation(schema_loc2, "schema_loc1.rel_loc1");
        rel_loc2.assignToSchema();

        Attribute attr_loc2 = new Attribute(rel_loc2, "id", "int4", true, 0, "", "p", "");
        attr_loc2.assignToRels();

        Relation rel_loc3 = new Relation(schema_loc2, "schema_loc1.rel_loc3");
        rel_loc3.assignToSchema();

        Attribute attr_loc3 = new Attribute(rel_loc3, "id", "int4", true, 0, "", "p", "");
        attr_loc3.assignToRels();

        Constraint con_loc1 = new Constraint(db2, "con1", "CASCADE", "CASCADE", "con1");
        con_loc1.setRelsAttrsByName(db2, rel_loc2.getFullName(), rel_loc3.getFullName(), attr_loc2.getName(), attr_loc3.getName(), false);

        Comparator comparator = Comparator.withAutoApply();
        //comparator.checkConstraints = false;
        comparator.compareDBs(db1, db2);
        assertTrue(comparator.hadDifference());

        for(Relation rel : schema_loc1.getRelations()){
            Dbg.info("Rel: "+rel.getFullName()+" has "+rel.getConstraints().size()+" constraints");
            for(Constraint con : rel.getConstraints()){
                Dbg.info("Constraint "+con.getFullName());
            }
        }

        assertEquals(schema_loc1.getRelations().size(), 2);
        assertEquals(schema_loc1.getRelations().get(0).getConstraints().size(), 1);
        assertEquals(schema_loc1.getRelations().get(1).getConstraints().size(), 1);

    }

    @Test
    public void testCompareFunctions() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareFunctions(schema1, schema1.getFunctions(), schema1.getFunctions());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withReportOnly();
        comparator.compareFunctions(schema1, schema1.getFunctions(), schema2.getFunctions());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareFunctions(schema1, schema1.getFunctions(), schema1.getFunctions());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareFunctions(schema1, schema1.getFunctions(), schema2.getFunctions());
        assertTrue(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());
    }

    @Test
    public void testCompareFunctionsAdd() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareFunctions(schema3, schema3.getFunctions(), schema1.getFunctions());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(2, comparator.getDifferences().size());
        for(IModelElement element : comparator.getDifferences()){
            assertEquals(Comparator.IS_MISSING, element.getDifference());
        }

        comparator = Comparator.withAutoApply();
        comparator.compareFunctions(schema3, schema3.getFunctions(), schema1.getFunctions());
        assertTrue(comparator.hadDifference());
        assertEquals(2, schema3.getFunctions().size());
        assertTrue(schema3.getFunctions().containsAll(schema1.getFunctions()));
    }

    @Test
    public void testCompareFunctionsRemove() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareFunctions(schema1, schema1.getFunctions(), schema3.getFunctions());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(2, comparator.getDifferences().size());
        for(IModelElement element : comparator.getDifferences()){
            assertEquals(Comparator.IS_ADDED, element.getDifference());
        }

        comparator = Comparator.withAutoApply();
        comparator.compareFunctions(schema1, schema1.getFunctions(), schema3.getFunctions());
        assertTrue(comparator.hadDifference());
        assertEquals(0, schema1.getFunctions().size());
    }

    @Test
    public void testCompareFunctionsProperties() {
        Schema schema_loc1 = new Schema(db1, "schema1", "schema1");
        Function func_loc1 = new Function(schema_loc1, "schema1.func_loc", "trigger", "", "BEGIN\n\nRETURN NEW;\n\nEND", "plpgsql", "volatile", true, 100, 1000, null);
        func_loc1.assignToSchema();

        Schema schema_loc2 = new Schema(db2, "schema1", "schema1");
        Function func_loc2 = new Function(schema_loc2, "schema1.func_loc", "trigger", "", "BEGIN\n\nRETURN NEW;\n\nEND", "plpgsql", "volatile", true, 100, 1000, "");
        func_loc2.assignToSchema();

        Comparator comparator = Comparator.withAutoApply();
        comparator.compareFunctions(schema_loc1, schema_loc1.getFunctions(), schema_loc2.getFunctions());
        assertFalse(comparator.hadDifference());

        func_loc1.getBehavior().setCost(func_loc1.getBehavior().getCost()+1);

        comparator = Comparator.withAutoApply();
        comparator.compareFunctions(schema_loc1, schema_loc1.getFunctions(), schema_loc2.getFunctions());
        assertTrue(comparator.hadDifference());

        Revision.Diff change = RevisionFactory.getCurrentIncoming(ConnectionUtils.getCurrent(schema_loc1.getDB().getName())).getChanges().get(0);
        assertFalse(change.getForwardSQL().contains("COMMENT ON "));
        assertFalse(change.getRevertSQL().contains("COMMENT ON "));
    }

    @Test
    public void testCompareAttributes() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareAttributes(rel1, rel1.getAttributes(), rel1.getAttributes());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withReportOnly();
        comparator.compareAttributes(rel1, rel1.getAttributes(), rel2.getAttributes());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareAttributes(rel1, rel1.getAttributes(), rel1.getAttributes());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareAttributes(rel1, rel1.getAttributes(), rel2.getAttributes());
        assertTrue(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());
    }

    @Test
    public void testCompareAttributesAddRemove() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareAttributes(rel1, rel1.getAttributes(), rel2.getAttributes());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(4, comparator.getDifferences().size());

        comparator = Comparator.withAutoApply();
        comparator.compareAttributes(rel1, rel1.getAttributes(), rel2.getAttributes());
        assertTrue(comparator.hadDifference());
        assertEquals(2, rel1.getAttributes().size());
        assertTrue(rel1.getAttributes().containsAll(rel2.getAttributes()));
    }

    @Test
    public void testCompareAttributesProperties() {
        rel2.getBehavior().setName(rel1.getName());
        rel1.getAttributes().get(0).getBehavior().setName("attr1");
        rel2.getAttributes().get(0).getBehavior().setName("attr1");
        rel1.getAttributes().get(1).drop();
        rel2.getAttributes().get(1).drop();
        assertEquals(1, rel1.getAttributes().size());
        assertEquals(1, rel2.getAttributes().size());
        rel1.getAttributes().get(0).getBehavior().setAttType("int4");
        rel2.getAttributes().get(0).getBehavior().setAttType("int8");

        Comparator comparator = Comparator.withReportOnly();
        comparator.compareAttributes(rel1, rel1.getAttributes(), rel2.getAttributes());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(1, comparator.getDifferences().size());

        comparator = Comparator.withAutoApply();
        comparator.compareAttributes(rel1, rel1.getAttributes(), rel2.getAttributes());
        assertTrue(comparator.hadDifference());
        assertEquals("int8", rel1.getAttributes().get(0).getBehavior().getAttType());
    }

    @Test
    public void testCompareIndexes() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareIndexes(rel1, rel1.getIndexes(), rel1.getIndexes());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withReportOnly();
        comparator.compareIndexes(rel1, rel1.getIndexes(), rel2.getIndexes());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareIndexes(rel1, rel1.getIndexes(), rel1.getIndexes());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareIndexes(rel1, rel1.getIndexes(), rel2.getIndexes());
        assertTrue(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());
    }

    @Test
    public void testCompareIndexesAdd() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareIndexes(rel2, rel2.getIndexes(), rel1.getIndexes());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(2, comparator.getDifferences().size());
        for(IModelElement element : comparator.getDifferences()){
            assertEquals(Comparator.IS_MISSING, element.getDifference());
        }

        comparator = Comparator.withAutoApply();
        comparator.compareIndexes(rel2, rel2.getIndexes(), rel1.getIndexes());
        assertTrue(comparator.hadDifference());
        assertEquals(2, rel2.getIndexes().size());
        assertTrue(rel2.getIndexes().containsAll(rel1.getIndexes()));
    }

    @Test
    public void testCompareIndexesRemove() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareIndexes(rel1, rel1.getIndexes(), rel2.getIndexes());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(2, comparator.getDifferences().size());
        for(IModelElement element : comparator.getDifferences()){
            assertEquals(Comparator.IS_ADDED, element.getDifference());
        }

        comparator = Comparator.withAutoApply();
        comparator.compareIndexes(rel1, rel1.getIndexes(), rel2.getIndexes());
        assertTrue(comparator.hadDifference());
        assertEquals(0, rel1.getIndexes().size());
    }

    @Test
    public void testCompareIndexProperties() {
        rel2.getBehavior().setName(rel1.getName());
        rel2.getIndexes().add(rel1.getIndexes().get(1));
        rel1.getIndexes().remove(1);

        rel2.getIndexes().get(0).setBehavior(rel1.getIndexes().get(0).getBehavior().prepareForEdit());
        rel2.getIndexes().get(0).getBehavior().setWhere("NEW WHERE");

        Comparator comparator = Comparator.withReportOnly();
        comparator.compareIndexes(rel1, rel1.getIndexes(), rel2.getIndexes());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(1, comparator.getDifferences().size());

        comparator = Comparator.withAutoApply();
        comparator.compareIndexes(rel1, rel1.getIndexes(), rel2.getIndexes());
        assertTrue(comparator.hadDifference());
        assertEquals("NEW WHERE", rel1.getIndexes().get(0).getBehavior().getWhere());
    }

    @Test
    public void testCompareConstraints() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareConstraints(db1, db1.getConstraints(), db1.getConstraints());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withReportOnly();
        comparator.compareConstraints(db1, db1.getConstraints(), db2.getConstraints());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareConstraints(db1, db1.getConstraints(), db1.getConstraints());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareConstraints(db1, db1.getConstraints(), db2.getConstraints());
        assertTrue(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());
    }

    @Test
    public void testCompareConstraintsAdd() {
        Comparator comparator = Comparator.withAutoApply();
        comparator.compareSchemata(db2, db2.getSchemas(), db1.getSchemas());

        comparator = Comparator.withReportOnly();
        comparator.compareConstraints(db2, db2.getConstraints(), db1.getConstraints());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(2, comparator.getDifferences().size());
        for(IModelElement element : comparator.getDifferences()){
            assertEquals(Comparator.IS_MISSING, element.getDifference());
        }

        comparator = Comparator.withAutoApply();
        comparator.compareConstraints(db2, db2.getConstraints(), db1.getConstraints());
        assertTrue(comparator.hadDifference());
        assertEquals(2, db2.getConstraints().size());
        assertTrue(db2.getConstraints().containsAll(db1.getConstraints()));
    }

    @Test
    public void testCompareConstraintsRemove() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareConstraints(db1, db1.getConstraints(), db2.getConstraints());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(2, comparator.getDifferences().size());
        for(IModelElement element : comparator.getDifferences()){
            assertEquals(Comparator.IS_ADDED, element.getDifference());
        }

        comparator = Comparator.withAutoApply();
        comparator.compareConstraints(db1, db1.getConstraints(), db2.getConstraints());
        assertTrue(comparator.hadDifference());
        assertEquals(0, db1.getConstraints().size());
    }

    @Test
    public void testCompareConstraintProperties() {
        db1.getConstraints().get(0).getBehavior().setName(rel1.getName());
        db2.getConstraints().add(db1.getConstraints().get(1));
        db1.getConstraints().remove(1);

        db2.getConstraints().get(0).setBehavior(db1.getConstraints().get(0).getBehavior().prepareForEdit());
        db2.getConstraints().get(0).getBehavior().setDef("NEW DEFINITION");

        Comparator comparator = Comparator.withReportOnly();
        comparator.compareConstraints(db1, db1.getConstraints(), db2.getConstraints());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(1, comparator.getDifferences().size());

        comparator = Comparator.withAutoApply();
        comparator.compareConstraints(db1, db1.getConstraints(), db2.getConstraints());
        assertTrue(comparator.hadDifference());
        assertEquals("NEW DEFINITION", db1.getConstraints().get(0).getBehavior().getDef());
    }

    @Test
    public void testCompareTriggers() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareTriggers(db1, db1.getTriggers(), db1.getTriggers());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withReportOnly();
        comparator.compareTriggers(db1, db1.getTriggers(), db2.getTriggers());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareTriggers(db1, db1.getTriggers(), db1.getTriggers());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareTriggers(db1, db1.getTriggers(), db2.getTriggers());
        assertTrue(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());
    }

    @Test
    public void testCompareTriggersAdd() {
        Comparator comparator = Comparator.withAutoApply();
        comparator.compareSchemata(db2, db2.getSchemas(), db1.getSchemas());
        comparator.compareSchemata(db2, db2.getSchemas(), db1.getSchemas());

        Dbg.info(db2.getSchemas().get(0).getFunctions().get(0).getFullName());

        comparator = Comparator.withReportOnly();
        comparator.compareTriggers(db2, db2.getTriggers(), db1.getTriggers());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        for(IModelElement element : comparator.getDifferences()){
            assertEquals(Comparator.IS_MISSING, element.getDifference());
        }

        comparator = Comparator.withAutoApply();
        comparator.compareTriggers(db2, db2.getTriggers(), db1.getTriggers());
        assertTrue(comparator.hadDifference());
        assertEquals(2, db2.getTriggers().size());
        assertTrue(db2.getTriggers().containsAll(db1.getTriggers()));
    }

    @Test
    public void testCompareTriggersRemove() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareTriggers(db1, db1.getTriggers(), db2.getTriggers());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        for(IModelElement element : comparator.getDifferences()){
            assertEquals(Comparator.IS_ADDED, element.getDifference());
        }

        comparator = Comparator.withAutoApply();
        comparator.compareTriggers(db1, db1.getTriggers(), db2.getTriggers());
        assertTrue(comparator.hadDifference());
        assertEquals(0, db1.getTriggers().size());
    }

    @Test
    public void testCompareTriggerProperties() {
        db1.getTriggers().get(0).getBehavior().setName(rel1.getName());
        db2.getTriggers().add(db1.getTriggers().get(1));
        db1.getTriggers().remove(1);

        db2.getTriggers().get(0).setBehavior(db1.getTriggers().get(0).getBehavior().prepareForEdit());
        db2.getTriggers().get(0).getBehavior().setWhen("NEW CONDITION");

        Comparator comparator = Comparator.withReportOnly();
        comparator.compareTriggers(db1, db1.getTriggers(), db2.getTriggers());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(1, comparator.getDifferences().size());

        comparator = Comparator.withAutoApply();
        comparator.compareTriggers(db1, db1.getTriggers(), db2.getTriggers());
        assertTrue(comparator.hadDifference());
        assertEquals("NEW CONDITION", db1.getTriggers().get(0).getBehavior().getWhen());
    }

    @Test
    public void testComparePackages() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.comparePackages(schema1, schema1.getPackages(), schema1.getPackages());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withReportOnly();
        comparator.comparePackages(schema1, schema1.getPackages(), schema2.getPackages());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.comparePackages(schema1, schema1.getPackages(), schema1.getPackages());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.comparePackages(schema1, schema1.getPackages(), schema2.getPackages());
        assertTrue(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());
    }

    @Test
    public void testComparePackagesAdd() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.comparePackages(schema3, schema3.getPackages(), schema1.getPackages());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(2, comparator.getDifferences().size());
        for(IModelElement element : comparator.getDifferences()){
            assertEquals(Comparator.IS_MISSING, element.getDifference());
        }

        comparator = Comparator.withAutoApply();
        comparator.comparePackages(schema3, schema3.getPackages(), schema1.getPackages());
        assertTrue(comparator.hadDifference());
        assertEquals(2, schema3.getPackages().size());
        assertTrue(schema3.getPackages().containsAll(schema1.getPackages()));
    }

    @Test
    public void testComparePackagesRemove() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.comparePackages(schema1, schema1.getPackages(), schema3.getPackages());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(2, comparator.getDifferences().size());
        for(IModelElement element : comparator.getDifferences()){
            assertEquals(Comparator.IS_ADDED, element.getDifference());
        }

        comparator = Comparator.withAutoApply();
        comparator.comparePackages(schema1, schema1.getPackages(), schema3.getPackages());
        assertTrue(comparator.hadDifference());
        assertEquals(0, schema1.getPackages().size());
    }

    @Test
    public void testCompareViews() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareViews(schema1, schema1.getViews(), schema1.getViews());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withReportOnly();
        comparator.compareViews(schema1, schema1.getViews(), schema2.getViews());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareViews(schema1, schema1.getViews(), schema1.getViews());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareViews(schema1, schema1.getViews(), schema2.getViews());
        assertTrue(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());
    }

    @Test
    public void testCompareViewsAdd() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareViews(schema3, schema3.getViews(), schema1.getViews());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(2, comparator.getDifferences().size());
        for(IModelElement element : comparator.getDifferences()){
            assertEquals(Comparator.IS_MISSING, element.getDifference());
        }

        comparator = Comparator.withAutoApply();
        comparator.compareViews(schema3, schema3.getViews(), schema1.getViews());
        assertTrue(comparator.hadDifference());
        assertEquals(2, schema3.getViews().size());
        assertTrue(schema3.getViews().containsAll(schema1.getViews()));
    }

    @Test
    public void testCompareViewsRemove() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareViews(schema1, schema1.getViews(), schema3.getViews());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(2, comparator.getDifferences().size());
        for(IModelElement element : comparator.getDifferences()){
            assertEquals(Comparator.IS_ADDED, element.getDifference());
        }

        comparator = Comparator.withAutoApply();
        comparator.compareViews(schema1, schema1.getViews(), schema3.getViews());
        assertTrue(comparator.hadDifference());
        assertEquals(0, schema1.getViews().size());
    }

    @Test
    public void testCompareViewProperties() {
        Schema schemaLoc1 = new Schema(db1, "schema1", "schema1");
        View elem1 = new View(schemaLoc1, "schema1.package1", false, "OLD SOURCE;\n", "");
        elem1.assignToSchema();

        Schema schemaLoc2 = new Schema(db2, "schema1", "schema1");
        View elem2 = new View(schemaLoc2, "schema1.package1", false, "OLD SOURCE;\n", "");
        elem2.assignToSchema();

        Comparator comparator = Comparator.withAutoApply();
        comparator.compareViews(schemaLoc1, schemaLoc1.getViews(), schemaLoc2.getViews());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        elem2.getBehavior().setSrc("NEW SOURCE;\n");

        comparator = Comparator.withAutoApply();
        comparator.compareViews(schemaLoc1, schemaLoc1.getViews(), schemaLoc2.getViews());
        assertTrue(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        Revision.Diff change = RevisionFactory.getCurrentIncoming(ConnectionUtils.getCurrent(schemaLoc1.getDB().getName())).getChanges().get(0);
        assertTrue(change.getForwardSQL().contains("NEW SOURCE"));
        assertTrue(change.getRevertSQL().contains("OLD SOURCE"));
    }

    @Test
    public void testCompareSequences() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareSequences(schema1, schema1.getSequences(), schema1.getSequences());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withReportOnly();
        comparator.compareSequences(schema1, schema1.getSequences(), schema2.getSequences());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareSequences(schema1, schema1.getSequences(), schema1.getSequences());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        comparator = Comparator.withAutoApply();
        comparator.compareSequences(schema1, schema1.getSequences(), schema2.getSequences());
        assertTrue(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());
    }

    @Test
    public void testCompareSequencesAdd() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareSequences(schema3, schema3.getSequences(), schema1.getSequences());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(2, comparator.getDifferences().size());
        for(IModelElement element : comparator.getDifferences()){
            assertEquals(Comparator.IS_MISSING, element.getDifference());
        }

        comparator = Comparator.withAutoApply();
        comparator.compareSequences(schema3, schema3.getSequences(), schema1.getSequences());
        assertTrue(comparator.hadDifference());
        assertEquals(2, schema3.getSequences().size());
        assertTrue(schema3.getSequences().containsAll(schema1.getSequences()));
    }

    @Test
    public void testCompareSequencesRemove() {
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareSequences(schema1, schema1.getSequences(), schema3.getSequences());
        assertFalse(comparator.hadDifference());
        assertTrue(comparator.pendingDifference());
        assertEquals(2, comparator.getDifferences().size());
        for(IModelElement element : comparator.getDifferences()){
            assertEquals(Comparator.IS_ADDED, element.getDifference());
        }

        comparator = Comparator.withAutoApply();
        comparator.compareSequences(schema1, schema1.getSequences(), schema3.getSequences());
        assertTrue(comparator.hadDifference());
        assertEquals(0, schema1.getSequences().size());
    }

    @Test
    public void testCompareSequenceProperties() {
        Schema schemaLoc1 = new Schema(db1, "schema1", "schema1");
        Sequence elem1 = new Sequence(schemaLoc1, "schema1.package1", new String[0], "1", "1", "1001", "1", true, "");
        elem1.assignToSchemaAndAttributes();

        Schema schemaLoc2 = new Schema(db2, "schema1", "schema1");
        Sequence elem2 = new Sequence(schemaLoc2, "schema1.package1", new String[0], "1", "1", "1001", "1", true, "");
        elem2.assignToSchemaAndAttributes();

        Comparator comparator = Comparator.withAutoApply();
        comparator.compareSequences(schemaLoc1, schemaLoc1.getSequences(), schemaLoc2.getSequences());
        assertFalse(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        elem2.getBehavior().setMax("10000001");

        comparator = Comparator.withAutoApply();
        comparator.compareSequences(schemaLoc1, schemaLoc1.getSequences(), schemaLoc2.getSequences());
        assertTrue(comparator.hadDifference());
        assertFalse(comparator.pendingDifference());

        Revision.Diff change = RevisionFactory.getCurrentIncoming(ConnectionUtils.getCurrent(schemaLoc1.getDB().getName())).getChanges().get(0);
        assertTrue(change.getForwardSQL().contains("10000001"));
        assertTrue(change.getRevertSQL().contains("1001"));
    }


    @Test
    public void testCompareData() throws Exception {

        Relation relation1 = new Relation(schema1, "relation1");
        new Attribute(relation1, "col1", "varchar", false, 1, null, null, null).assignToRels();
        new Attribute(relation1, "col2", "varchar", false, 2, null, null, null).assignToRels();
        Relation relation2 = new Relation(schema1, "relation2");
        new Attribute(relation2, "col1", "varchar", false, 1, null, null, null).assignToRels();
        new Attribute(relation2, "col2", "varchar", false, 2, null, null, null).assignToRels();

        VirtualConnection conn;
        ProjectManager.getInstance().setCurrentProject(ProjectManager.getInstance().createNew("test1", Project.TYPE_PG));
        Project.getCurrent().getConnections().add(conn = VirtualConnection.prepareTableContentData(relation1));

        // Check tables with different data but missing PKs
        Comparator comparator = Comparator.withReportOnly();
        comparator.compareData(relation1, relation2);
        assertEquals(1, comparator.getDifferences().size());
        assertEquals(Comparator.DATA_FAILED_NO_PRIMARY_KEY, comparator.getDifferences().get(0).getDifference());

        conn.reset();

        new Index(relation1, "pk", "PRIMARY KEY (col1)", "", "", new String[0], true, true, true, "").assignToRelation(relation1);
        new Index(relation2, "pk", "PRIMARY KEY (col1)", "", "", new String[0], true, true, true, "").assignToRelation(relation2);

        // Check tables with different rows
        comparator = Comparator.withReportOnly();
        comparator.compareData(relation1, relation2);
        assertEquals(1, comparator.getDifferences().size());
        assertEquals(Comparator.DATA_CHANGED, comparator.getDifferences().get(0).getDifference());

        // Check tables with different content
        comparator = Comparator.withReportOnly();
        comparator.compareData(relation1, relation2);
        Relation relation = (Relation) comparator.getDifferences().get(0);
        assertEquals(1, comparator.getDifferences().size());
        assertEquals(Comparator.DATA_CHANGED, relation.getDifference());
        assertEquals(1, relation.getDataChanged().getTreeView().getChildCount());
        assertEquals(0, relation.getDataChanged().inserted.size());
        assertEquals(1, relation.getDataChanged().updated.size());
        assertEquals(0, relation.getDataChanged().deleted.size());

        // Check tables with same data
        comparator = Comparator.withReportOnly();
        comparator.compareData(relation1, relation2);
        assertEquals(0, comparator.getDifferences().size());

        // SQL exception
        comparator = Comparator.withReportOnly();
        comparator.compareData(relation1, relation2);
        assertEquals(1, comparator.getDifferences().size());
        assertEquals(Comparator.DATA_FAILED_QUERY_FAILED, comparator.getDifferences().get(0).getDifference());

        conn.reset();

        relation1.setCountRows(100000);

        // Check tables with too much data
        comparator = Comparator.withReportOnly();
        comparator.compareData(relation1, relation2);
        assertEquals(1, comparator.getDifferences().size());
        assertEquals(Comparator.DATA_FAILED_TOO_BIG, comparator.getDifferences().get(0).getDifference());
    }

}