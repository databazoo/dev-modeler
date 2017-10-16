package global;

import org.junit.Before;
import org.junit.BeforeClass;

import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.ConnectionPg;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.CheckConstraint;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Package;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Sequence;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.model.View;
import com.databazoo.devmodeler.model.Workspace;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;

public abstract class TestProjectSetup {

    protected final DB database = new DB(null, "test 1");
    protected final Workspace workspace = new Workspace("test workspace", database);

    protected final Schema publicSchema = new Schema(database, "public", "public schema");
    protected final Schema schema = new Schema(database, "test 2", "test 2");

    protected final Relation relation = new Relation(schema, schema.getName()+".test 3");
    protected final Attribute attribute1 = new Attribute(relation, "test attr 1", "varchar", false, 1, "'defval'", "x", "test attr 1");
    protected final Attribute attribute2 = new Attribute(relation, "test attr 2", "varchar", false, 2, "", "auto", "test attr 2");
    protected final Index primaryKey = new Index(relation, "test PK", "PRIMARY KEY (\"test attr 1\")", "", "btree", new String[0], true, true, true, "test PK");
    protected final Index uniqueIndex = new Index(relation, "test UX", "\"test attr 2\"", "", "btree", new String[0], true, false, false, "test UX");

    protected final CheckConstraint checkConstraint = new CheckConstraint(database, "test 6", "CHECK ( id > 0 )", "test 6");
    protected final Constraint constraint = new Constraint(database, "test 5", "CASCADE", "CASCADE", "test 5");

    protected final Relation relation2 = new Relation(schema, schema.getName()+".test 4");
    protected final Attribute attribute3 = new Attribute(relation2, "test attr 3", "varchar", false, 1, "'defval'", "auto", "test attr 3");
    protected final Attribute attribute4 = new Attribute(relation2, "test attr 4", "varchar", true, 2, "", "auto", "test attr 4");
    protected final Index uniqueConstraint = new Index(relation2, "test UC", "UNIQUE (\"test attr 3\")", "", "btree", new String[0], true, false, true, "test UC");
    protected final Index index = new Index(relation2, "test index", " USING btree (\"test attr 4\") WHERE \"test attr 4\" IS NOT NULL", "\"test attr 4\" IS NOT NULL", "btree", new String[0], false, false, false, "test index");

    protected final Function function = new Function(schema, "test function", "varchar", "int input", "\nRETURN CAST(input AS varchar);\n", "plpgsql", "stable", false, 10, 100, "test function");
    protected final Function triggerFunction = new Function(schema, "test trigger function", "trigger", "", "\nRETURN NEW;\n", "plpgsql", "volatile", false, 0, 0, "test trigger function");
    protected final Package pack = new Package(schema, "test package", "TEST PACKAGE SOURCE;\n\n", "TEST PACKAGE BODY;\n\n", "test package");
    protected final Sequence sequence = new Sequence(schema, "test sequence", new String[]{"\"test 3\".\"test attr 1\""}, "1", "0", "1000000", "5", true, "test sequence");
    protected final Trigger trigger = new Trigger(database, "test trigger", "EXECUTE PROCEDURE \""+triggerFunction.getName()+"\"()", attribute3.getName() + ", " + attribute4.getName(), "\""+attribute4.getName()+"\" IS NOT NULL", false, "test trigger");
    protected final View view = new View(schema, "test view", false, "SELECT * FROM \""+relation2.getName()+"\"", "test view");

    @Before
    public void finalizeData(){
        database.getSchemas().clear();
        database.getConstraints().clear();
        database.getTriggers().clear();
        publicSchema.getRelations().clear();
        publicSchema.getSequences().clear();
        publicSchema.getFunctions().clear();
        publicSchema.getPackages().clear();
        publicSchema.getViews().clear();
        schema.getRelations().clear();
        schema.getSequences().clear();
        schema.getFunctions().clear();
        schema.getPackages().clear();
        schema.getViews().clear();
        relation.getAttributes().clear();
        relation.getConstraints().clear();
        relation.getIndexes().clear();
        relation.getInfoVals().clear();
        relation.getInheritances().clear();
        relation2.getAttributes().clear();
        relation2.getConstraints().clear();
        relation2.getIndexes().clear();
        relation2.getInfoVals().clear();
        relation2.getInheritances().clear();
        if (Project.getCurrent().getDatabases().isEmpty()) {
            Project.getCurrent().getDatabases().add(database);
        }
        publicSchema.assignToDB(database);

        schema.assignToDB(database);
        relation.assignToSchema();
        attribute1.assignToRels();
        attribute2.assignToRels();
        attribute3.assignToRels();
        attribute4.assignToRels();
        primaryKey.assignToRelation(relation);
        uniqueIndex.assignToRelation(relation);
        checkConstraint.setRelByName(database, relation.getFullName(), false);
        sequence.assignToSchemaAndAttributes();

        relation2.assignToSchema();
        uniqueConstraint.assignToRelation(relation2);
        index.assignToRelation(relation2);
        view.assignToSchema();
        function.assignToSchema();
        triggerFunction.assignToSchema();
        trigger.setRelFuncByNameDef(database, relation2.getFullName(), "EXECUTE PROCEDURE \""+triggerFunction.getName()+"\"()", false);
        trigger.getBehavior().setTiming(19);
        constraint.setRelsAttrsByName(relation, relation2, attribute1.getName(), attribute3.getName(), false);

        pack.assignToSchema();

        Project.getCurrent().getWorkspaces().add(workspace);
        workspace.add(relation);
        workspace.add(relation2);
    }

    @BeforeClass
    public static void setProjectUp() {
        Settings.init();

        ProjectManager.getInstance().setCurrentProject(ProjectManager.getInstance().createNew("test1", Project.TYPE_PG));
        Project.getCurrent().setLoaded();
        Project.getCurrent().getConnections().add(new ConnectionPg("", "", "", "", false));
    }
}
