package com.databazoo.devmodeler.conn;

import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.CheckConstraint;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.IModelBehavior;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Sequence;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.model.View;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Pg static test.
 */
public class ConnectionPgTest extends TestProjectSetup {

	private static ConnectionPg CONNECTION = new ConnectionPg("test", "test", "test", "test", false);

	@BeforeClass
	public static void setProjectUp() {
		Settings.init();

		ProjectManager.getInstance().setCurrentProject(ProjectManager.getInstance().createNew("test1", Project.TYPE_PG));
		Project.getCurrent().getConnections().add(CONNECTION);
	}

	@Test
	public void getTableAliasTest(){
		assertEquals("m", ConnectionUtils.getTableAlias("mytable"));
		assertEquals("mt", ConnectionUtils.getTableAlias("myTable"));
		assertEquals("mt", ConnectionUtils.getTableAlias("MyTable"));
		assertEquals("mt", ConnectionUtils.getTableAlias("my_table"));
		assertEquals("mt", ConnectionUtils.getTableAlias("My_Table"));
		assertEquals("mt", ConnectionUtils.getTableAlias("MY_TABLE"));
		assertEquals("msr1", ConnectionUtils.getTableAlias("my_special_relation1"));
        assertEquals("msr1", ConnectionUtils.getTableAlias("my_special_relation_1"));
        assertEquals("s23r54", ConnectionUtils.getTableAlias("schema23relation54"));
	}

	@Test
	public void getPresetFucntionArgs() throws Exception {
		assertEquals("", CONNECTION.getPresetFucntionArgs(true));
		assertEquals("", CONNECTION.getPresetFucntionArgs(false));
	}

	@Test
	public void getPresetFucntionReturn() throws Exception {
		assertEquals("trigger", CONNECTION.getPresetFucntionReturn(true));
		assertEquals("TABLE ()", CONNECTION.getPresetFucntionReturn(false));

	}

	@Test
	public void getPresetFucntionSource() throws Exception {
		assertEquals("\nDECLARE\n\nBEGIN\n\tRETURN NEW;\nEND\n", CONNECTION.getPresetFucntionSource(true));
		assertEquals("\nDECLARE\n\nBEGIN\n\t/** TODO **/\nEND\n", CONNECTION.getPresetFucntionSource(false));
	}

	@Test
	public void getPresetPackageDefinition() throws Exception {
		assertEquals("\n\tFUNCTION my_func(my_input number DEFAULT 0) RETURN number;\n", CONNECTION.getPresetPackageDefinition());
	}

	@Test
	public void getPresetPackageBody() throws Exception {
		assertEquals("\n\tFUNCTION my_func(my_input number DEFAULT 0) RETURN number IS\n\tBEGIN\n\t\tRETURN 1;\n\tEND;\n", CONNECTION.getPresetPackageBody());

	}

	@Test
	public void getPresetTriggerBody() throws Exception {
		assertEquals("BEGIN\n\tSET \nEND", CONNECTION.getPresetTriggerBody());
	}

	@Test
	public void getCleanError() throws Exception {
		assertEquals("test 123", CONNECTION.getCleanError("org.postgresql.util.PSQLException: test 123"));
	}

	@Test
	public void getErrorPosition() throws Exception {
		Point res = CONNECTION.getErrorPosition("SELECT * FROM trololo", "org.postgresql.util.PSQLException: Unexpected trololo char: 15");
		assertEquals(14, res.x);
		assertEquals(0, res.y);
	}

	@Test
	public void getQueryLimitOffset() throws Exception {
		assertEquals("OFFSET 10 LIMIT 11", CONNECTION.getQueryLimitOffset(11, 10));
		assertEquals("LIMIT 11", CONNECTION.getQueryLimitOffset(11, 0));
		assertEquals("", CONNECTION.getQueryLimitOffset(0, 10));
	}

	@Test
	public void getQueryExplain() throws Exception {
		assertEquals("EXPLAIN SELECT * FROM trololo", CONNECTION.getQueryExplain("SELECT * FROM trololo"));
	}

	@Test
	public void getQueryCreateDatabase() throws Exception {
		String val = CONNECTION.getQueryCreate(database, null);
		assertEquals("CREATE DATABASE \"test 1\";", val);
		assertTrue(val.endsWith(";"));

		val = database.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL DATABASE **/\n\n" +
				"DROP DATABASE \"test 1\";\n\n" +
				"CREATE DATABASE \"test 1\";\n\n" +
				"/***********************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryDropDatabase() throws Exception {
		String val = CONNECTION.getQueryDrop(database);
		assertEquals("DROP DATABASE \"test 1\";", val);
		assertTrue(val.endsWith(";"));

		val = database.getQueryDrop(CONNECTION);
		assertEquals("DROP DATABASE \"test 1\";", val);
	}

	@Test
	public void getQueryCreateSchema() throws Exception {
		String val = CONNECTION.getQueryCreate(schema, null);
		assertEquals("-- test 2\n" +
						"CREATE SCHEMA \"test 2\";\n\n" +
						"COMMENT ON SCHEMA \"test 2\" IS 'test 2';",
				val);
		assertTrue(val.endsWith(";"));

		val = schema.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL SCHEMA **/\n\n" +
						"DROP SCHEMA \"test 2\";\n\n" +
						"-- test 2\n" +
						"CREATE SCHEMA \"test 2\";\n\n" +
						"COMMENT ON SCHEMA \"test 2\" IS 'test 2';\n\n" +
						"/*********************/",
				val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryDropSchema() throws Exception {
		String val = CONNECTION.getQueryDrop(schema);
		assertEquals("DROP SCHEMA \"test 2\";", val);
		assertTrue(val.endsWith(";"));

		val = schema.getQueryDrop(CONNECTION);
		assertEquals("DROP SCHEMA \"test 2\";", val);
	}

	@Test
	public void getQueryChangedSchema() throws Exception {
		IModelBehavior valuesForEdit;
		String val;

		schema.getBehavior().prepareForEdit();
		assertEquals("", schema.getQueryChanged(CONNECTION));
		assertEquals("", schema.getQueryChangeRevert(CONNECTION));

		// Change NAME
		valuesForEdit = schema.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Schema.Behavior.L_NAME, "new name");

		val = schema.getQueryChanged(CONNECTION);
		assertEquals("ALTER SCHEMA \"test 2\" RENAME TO \"new name\";", val);
		assertTrue(val.endsWith(";"));
		val = schema.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER SCHEMA \"new name\" RENAME TO \"test 2\";", val);
		assertTrue(val.endsWith(";"));

		// Change DESCRIPTION
		valuesForEdit = schema.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Schema.Behavior.L_DESCR, "new descr");

		val = schema.getQueryChanged(CONNECTION);
		assertEquals("COMMENT ON SCHEMA \"test 2\" IS 'new descr';", val);
		assertTrue(val.endsWith(";"));
		val = schema.getQueryChangeRevert(CONNECTION);
		assertEquals("COMMENT ON SCHEMA \"test 2\" IS 'test 2';", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreateTable() throws Exception {
		String val = CONNECTION.getQueryCreate(relation, null);
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
						") WITHOUT OIDS;\n\n" +
						"COMMENT ON COLUMN \"test 2\".\"test 3\".\"test attr 1\" IS 'test attr 1';\n\n" +
						"COMMENT ON COLUMN \"test 2\".\"test 3\".\"test attr 2\" IS 'test attr 2';\n\n" +
						"COMMENT ON CONSTRAINT \"test PK\" ON \"test 2\".\"test 3\" IS 'test PK';\n\n" +
						"COMMENT ON CONSTRAINT \"test 6\" ON \"test 2\".\"test 3\" IS 'test 6';\n\n" +
						"COMMENT ON CONSTRAINT \"test 5\" ON \"test 2\".\"test 3\" IS 'test 5';\n\n" +
						"-- test UX\n" +
						"CREATE UNIQUE INDEX \"test UX\"\n" +
						"\tON \"test 2\".\"test 3\"\n" +
						"\tUSING btree\n" +
						"\t(\"test attr 2\");\n\n" +
						"COMMENT ON INDEX \"test UX\" IS 'test UX';",
				val);
		assertTrue(val.endsWith(";"));

		val = relation.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL TABLE **/\n\n" +
						"DROP TABLE \"test 2\".\"test 3\";\n\n" +
						"CREATE TABLE \"test 2\".\"test 3\" (\n" +
						"\t\"test attr 1\" character varying NOT NULL DEFAULT 'defval',\t-- test attr 1\n" +
						"\t\"test attr 2\" character varying NOT NULL,\t-- test attr 2\n" +
						"\tCONSTRAINT \"test PK\"\n" +
						"\t\tPRIMARY KEY (\"test attr 1\"),\t-- test PK\n" +
						"\tCONSTRAINT \"test 6\" CHECK ( id > 0 ),\t-- test 6\n" +
						"\tCONSTRAINT \"test 5\" \n" +
						"\t\tFOREIGN KEY (\"test attr 1\")\n" +
						"\t\tREFERENCES \"test 2\".\"test 4\" (\"test attr 3\") MATCH SIMPLE\n" +
						"\t\tON UPDATE CASCADE ON DELETE CASCADE\t-- test 5\n" +
						") WITHOUT OIDS;\n\n" +
						"COMMENT ON COLUMN \"test 2\".\"test 3\".\"test attr 1\" IS 'test attr 1';\n\n" +
						"COMMENT ON COLUMN \"test 2\".\"test 3\".\"test attr 2\" IS 'test attr 2';\n\n" +
						"COMMENT ON CONSTRAINT \"test PK\" ON \"test 2\".\"test 3\" IS 'test PK';\n\n" +
						"COMMENT ON CONSTRAINT \"test 6\" ON \"test 2\".\"test 3\" IS 'test 6';\n\n" +
						"COMMENT ON CONSTRAINT \"test 5\" ON \"test 2\".\"test 3\" IS 'test 5';\n\n" +
						"-- test UX\n" +
						"CREATE UNIQUE INDEX \"test UX\"\n" +
						"\tON \"test 2\".\"test 3\"\n" +
						"\tUSING btree\n" +
						"\t(\"test attr 2\");\n\n" +
						"COMMENT ON INDEX \"test UX\" IS 'test UX';\n\n" +
						"/********************/",
				val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryDropTable() throws Exception {
		String val = CONNECTION.getQueryDrop(relation);
		assertEquals("DROP TABLE \"test 2\".\"test 3\";", val);
		assertTrue(val.endsWith(";"));

		val = relation.getQueryDrop(CONNECTION);
		assertEquals("DROP TABLE \"test 2\".\"test 3\";", val);
	}

	@Test
	public void getQueryChangedTable() throws Exception {
		IModelBehavior valuesForEdit;
		String val;

		relation.getBehavior().prepareForEdit();
		assertEquals("", relation.getQueryChanged(CONNECTION));
		assertEquals("", relation.getQueryChangeRevert(CONNECTION));

		// Change NAME
		valuesForEdit = relation.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Relation.Behavior.L_NAME, "new name");

		val = relation.getQueryChanged(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\" RENAME TO \"new name\";", val);
		assertTrue(val.endsWith(";"));
		val = relation.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"new name\" RENAME TO \"test 3\";", val);
		assertTrue(val.endsWith(";"));

		// Change SCHEMA
		valuesForEdit = relation.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Relation.Behavior.L_SCHEMA, "new schema");

		val = relation.getQueryChanged(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\" SET SCHEMA \"new schema\";", val);
		assertTrue(val.endsWith(";"));
		val = relation.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE \"new schema\".\"test 3\" SET SCHEMA \"test 2\";", val);
		assertTrue(val.endsWith(";"));

		// Change DEFINITION
		valuesForEdit = relation.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Relation.Behavior.L_INHERITS, "other table name");
		valuesForEdit.notifyChange(Relation.Behavior.L_OPTIONS, "a, b");
		valuesForEdit.notifyChange(Relation.Behavior.L_STORAGE, Attribute.Behavior.L_STORAGE_PLAIN);

		val = relation.getQueryChanged(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\" INHERIT \"other table name\";\n\n" +
				"ALTER TABLE \"test 2\".\"test 3\" SET (OIDs=FALSE,\n" +
				"\ta,\n" +
				"\tb\n" +
				");", val);
		assertTrue(val.endsWith(";"));
		val = relation.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\" NO INHERIT \"other table name\";\n\n" +
				"ALTER TABLE \"test 2\".\"test 3\" SET (OIDs=FALSE);", val);
		assertTrue(val.endsWith(";"));

		// Change DESCRIPTION
		valuesForEdit = relation.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Relation.Behavior.L_DESCR, "new descr");

		val = relation.getQueryChanged(CONNECTION);
		assertEquals("COMMENT ON TABLE \"test 2\".\"test 3\" IS 'new descr';", val);
		assertTrue(val.endsWith(";"));
		val = relation.getQueryChangeRevert(CONNECTION);
		assertEquals("COMMENT ON TABLE \"test 2\".\"test 3\" IS NULL;", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreateAttribute() throws Exception {
		String val = CONNECTION.getQueryCreate(attribute1, null);
		assertEquals("-- test attr 1\n" +
						"ALTER TABLE \"test 2\".\"test 3\"\n" +
						"\tADD COLUMN \"test attr 1\" character varying NOT NULL DEFAULT 'defval';\n\n" +
						"ALTER TABLE \"test 2\".\"test 3\"\n" +
						"\tALTER COLUMN \"test attr 1\" SET STORAGE EXTENDED;\n\n" +
						"COMMENT ON COLUMN \"test 2\".\"test 3\".\"test attr 1\" IS 'test attr 1';",
				val);
		assertTrue(val.endsWith(";"));

		val = attribute1.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL ATTRIBUTE **/\n\n" +
						"ALTER TABLE \"test 2\".\"test 3\"\n" +
						"\tDROP COLUMN \"test attr 1\";\n\n" +
						"-- test attr 1\n" +
						"ALTER TABLE \"test 2\".\"test 3\"\n" +
						"\tADD COLUMN \"test attr 1\" character varying NOT NULL DEFAULT 'defval';\n\n" +
						"ALTER TABLE \"test 2\".\"test 3\"\n" +
						"\tALTER COLUMN \"test attr 1\" SET STORAGE EXTENDED;\n\n" +
						"COMMENT ON COLUMN \"test 2\".\"test 3\".\"test attr 1\" IS 'test attr 1';\n\n" +
						"/************************/",
				val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryDefAttribute() throws Exception {
		assertEquals("character varying NOT NULL DEFAULT 'defval'", CONNECTION.getQueryDef(attribute1, null));
	}

	@Test
	public void getQueryDropAttribute() throws Exception {
		String val = CONNECTION.getQueryDrop(attribute1);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tDROP COLUMN \"test attr 1\";", val);
		assertTrue(val.endsWith(";"));

		val = attribute1.getQueryDrop(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tDROP COLUMN \"test attr 1\";", val);
	}

	@Test
	public void getQueryChangedAttribute() throws Exception {
		IModelBehavior valuesForEdit;
		String val;

		attribute1.getBehavior().prepareForEdit();
		assertEquals("", attribute1.getQueryChanged(CONNECTION));
		assertEquals("", attribute1.getQueryChangeRevert(CONNECTION));

		// Change NAME
		valuesForEdit = attribute1.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Attribute.Behavior.L_NAME, "new name");

		val = attribute1.getQueryChanged(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\" RENAME \"test attr 1\" TO \"new name\";", val);
		assertTrue(val.endsWith(";"));
		val = attribute1.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\" RENAME \"new name\" TO \"test attr 1\";", val);
		assertTrue(val.endsWith(";"));

		// Change DEFINITION
		valuesForEdit = attribute1.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Attribute.Behavior.L_DEFAULT, "");
		valuesForEdit.notifyChange(Attribute.Behavior.L_PRECISION, "255");
		valuesForEdit.notifyChange(Attribute.Behavior.L_STORAGE, Attribute.Behavior.L_STORAGE_PLAIN);

		val = attribute1.getQueryChanged(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\" ALTER COLUMN \"test attr 1\" SET DATA TYPE character varying(255);\n\n" +
				"ALTER TABLE \"test 2\".\"test 3\" ALTER COLUMN \"test attr 1\" DROP DEFAULT;\n\n" +
				"ALTER TABLE \"test 2\".\"test 3\" ALTER COLUMN \"test attr 1\" SET STORAGE PLAIN;", val);
		assertTrue(val.endsWith(";"));
		val = attribute1.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\" ALTER COLUMN \"test attr 1\" SET DATA TYPE character varying;\n\n" +
				"ALTER TABLE \"test 2\".\"test 3\" ALTER COLUMN \"test attr 1\" SET DEFAULT 'defval';\n\n" +
				"ALTER TABLE \"test 2\".\"test 3\" ALTER COLUMN \"test attr 1\" SET STORAGE EXTENDED;", val);
		assertTrue(val.endsWith(";"));

		// Change DESCRIPTION
		valuesForEdit = attribute1.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Attribute.Behavior.L_DESCR, "new descr");

		val = attribute1.getQueryChanged(CONNECTION);
		assertEquals("COMMENT ON COLUMN \"test 2\".\"test 3\".\"test attr 1\" IS 'new descr';", val);
		assertTrue(val.endsWith(";"));
		val = attribute1.getQueryChangeRevert(CONNECTION);
		assertEquals("COMMENT ON COLUMN \"test 2\".\"test 3\".\"test attr 1\" IS 'test attr 1';", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreateWithoutCommentCheckConstraint() throws Exception {
		String val = CONNECTION.getQueryCreateWithoutComment(checkConstraint, null);
		assertEquals(
				"ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tADD CONSTRAINT \"test 6\"\n" +
				"\t\tCHECK ( id > 0 );", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreateCheckConstraint() throws Exception {
		String val = CONNECTION.getQueryCreate(checkConstraint, null);
		assertEquals("-- test 6\n" +
				"ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tADD CONSTRAINT \"test 6\"\n" +
				"\t\tCHECK ( id > 0 );\n\n" +
				"COMMENT ON CONSTRAINT \"test 6\" ON \"test 2\".\"test 3\" IS 'test 6';", val);
		assertTrue(val.endsWith(";"));

		val = checkConstraint.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL CONSTRAINT **/\n\n" +
				"ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tDROP CONSTRAINT \"test 6\";\n\n" +
				"-- test 6\n" +
				"ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tADD CONSTRAINT \"test 6\"\n" +
				"\t\tCHECK ( id > 0 );\n\n" +
				"COMMENT ON CONSTRAINT \"test 6\" ON \"test 2\".\"test 3\" IS 'test 6';\n\n" +
				"/*************************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryChangedCheckConstraint() throws Exception {
		IModelBehavior valuesForEdit;
		String val;

		checkConstraint.getBehavior().prepareForEdit();
		assertEquals("", checkConstraint.getQueryChanged(CONNECTION));
		assertEquals("", checkConstraint.getQueryChangeRevert(CONNECTION));

		// Change NAME
		valuesForEdit = checkConstraint.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(CheckConstraint.Behavior.L_NAME, "new name");

		val = checkConstraint.getQueryChanged(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tDROP CONSTRAINT \"test 6\";\n\n" +
				"ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tADD CONSTRAINT \"new name\"\n" +
				"\t\tCHECK ( id > 0 );\n\n" +
				"COMMENT ON CONSTRAINT \"new name\" ON \"test 2\".\"test 3\" IS 'test 6';", val);
		assertTrue(val.endsWith(";"));
		val = checkConstraint.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tDROP CONSTRAINT \"new name\";\n\n" +
				"ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tADD CONSTRAINT \"test 6\"\n" +
				"\t\tCHECK ( id > 0 );\n\n" +
				"COMMENT ON CONSTRAINT \"test 6\" ON \"test 2\".\"test 3\" IS 'test 6';", val);
		assertTrue(val.endsWith(";"));

		// Change DEFINITION
		valuesForEdit = checkConstraint.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(CheckConstraint.Behavior.L_DEFINITION, "new definition");

		val = checkConstraint.getQueryChanged(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tDROP CONSTRAINT \"test 6\";\n\n" +
				"ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tADD CONSTRAINT \"test 6\"\n" +
				"\t\tCHECK (new definition);\n\n" +
				"COMMENT ON CONSTRAINT \"test 6\" ON \"test 2\".\"test 3\" IS 'test 6';", val);
		assertTrue(val.endsWith(";"));
		val = checkConstraint.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tDROP CONSTRAINT \"test 6\";\n\n" +
				"ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tADD CONSTRAINT \"test 6\"\n" +
				"\t\tCHECK ( id > 0 );\n\n" +
				"COMMENT ON CONSTRAINT \"test 6\" ON \"test 2\".\"test 3\" IS 'test 6';", val);
		assertTrue(val.endsWith(";"));

		// Change DESCRIPTION
		valuesForEdit = checkConstraint.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(CheckConstraint.Behavior.L_DESCR, "new descr");

		val = checkConstraint.getQueryChanged(CONNECTION);
		assertEquals("COMMENT ON CONSTRAINT \"test 6\" ON \"test 2\".\"test 3\" IS 'new descr';", val);
		assertTrue(val.endsWith(";"));
		val = checkConstraint.getQueryChangeRevert(CONNECTION);
		assertEquals("COMMENT ON CONSTRAINT \"test 6\" ON \"test 2\".\"test 3\" IS 'test 6';", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreateWithoutCommentConstraint() throws Exception {
		String val = CONNECTION.getQueryCreateWithoutComment(constraint, null);
		assertEquals(
				"ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tADD CONSTRAINT \"test 5\"\n" +
				"\t\tFOREIGN KEY (\"test attr 1\")\n" +
				"\t\tREFERENCES \"test 2\".\"test 4\" (\"test attr 3\") MATCH SIMPLE\n" +
				"\t\tON UPDATE CASCADE ON DELETE CASCADE;", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreateConstraint() throws Exception {
		String val = CONNECTION.getQueryCreate(constraint, null);
		assertEquals("-- test 5\n" +
				"ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tADD CONSTRAINT \"test 5\"\n" +
				"\t\tFOREIGN KEY (\"test attr 1\")\n" +
				"\t\tREFERENCES \"test 2\".\"test 4\" (\"test attr 3\") MATCH SIMPLE\n" +
				"\t\tON UPDATE CASCADE ON DELETE CASCADE;\n\n" +
				"COMMENT ON CONSTRAINT \"test 5\" ON \"test 2\".\"test 3\" IS 'test 5';", val);
		assertTrue(val.endsWith(";"));

		val = constraint.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL CONSTRAINT **/\n\n" +
				"ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tDROP CONSTRAINT \"test 5\";\n\n" +
				"-- test 5\n" +
				"ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tADD CONSTRAINT \"test 5\"\n" +
				"\t\tFOREIGN KEY (\"test attr 1\")\n" +
				"\t\tREFERENCES \"test 2\".\"test 4\" (\"test attr 3\") MATCH SIMPLE\n" +
				"\t\tON UPDATE CASCADE ON DELETE CASCADE;\n\n" +
				"COMMENT ON CONSTRAINT \"test 5\" ON \"test 2\".\"test 3\" IS 'test 5';\n\n" +
				"/*************************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryDefConstraint() throws Exception {
		assertEquals("FOREIGN KEY (\"test attr 1\")\n" +
				"\t\tREFERENCES \"test 2\".\"test 4\" (\"test attr 3\") MATCH SIMPLE\n" +
				"\t\tON UPDATE CASCADE ON DELETE CASCADE", CONNECTION.getQueryDef(constraint));
	}

	@Test
	public void getQueryDropConstraint() throws Exception {
		String val = CONNECTION.getQueryDrop(constraint);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tDROP CONSTRAINT \"test 5\";", val);
		assertTrue(val.endsWith(";"));

		val = constraint.getQueryDrop(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tDROP CONSTRAINT \"test 5\";", val);
	}

	@Test
	public void getQueryChangedConstraint() throws Exception {
		IModelBehavior valuesForEdit;
		String val;

		constraint.getBehavior().prepareForEdit();
		assertEquals("", constraint.getQueryChanged(CONNECTION));
		assertEquals("", constraint.getQueryChangeRevert(CONNECTION));

		// Change NAME
		valuesForEdit = constraint.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Constraint.Behavior.L_NAME, "new name");

		val = constraint.getQueryChanged(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tDROP CONSTRAINT \"test 5\";\n\n" +
				"ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tADD CONSTRAINT \"new name\"\n" +
				"\t\tFOREIGN KEY (\"test attr 1\")\n" +
				"\t\tREFERENCES \"test 2\".\"test 4\" (\"test attr 3\") MATCH SIMPLE\n" +
				"\t\tON UPDATE CASCADE ON DELETE CASCADE;\n\n" +
				"COMMENT ON CONSTRAINT \"new name\" ON \"test 2\".\"test 3\" IS 'test 5';", val);
		assertTrue(val.endsWith(";"));
		val = constraint.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tDROP CONSTRAINT \"new name\";\n\n" +
				"ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tADD CONSTRAINT \"test 5\"\n" +
				"\t\tFOREIGN KEY (\"test attr 1\")\n" +
				"\t\tREFERENCES \"test 2\".\"test 4\" (\"test attr 3\") MATCH SIMPLE\n" +
				"\t\tON UPDATE CASCADE ON DELETE CASCADE;\n\n" +
				"COMMENT ON CONSTRAINT \"test 5\" ON \"test 2\".\"test 3\" IS 'test 5';", val);
		assertTrue(val.endsWith(";"));

		// Change DEFINITION
		valuesForEdit = constraint.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Constraint.Behavior.L_REM_ATTR, attribute4.getName());

		val = constraint.getQueryChanged(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tDROP CONSTRAINT \"test 5\";\n\n" +
				"ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tADD CONSTRAINT \"test 5\"\n" +
				"\t\tFOREIGN KEY (\"test attr 1\")\n" +
				"\t\tREFERENCES \"test 2\".\"test 4\" (\"test attr 4\") MATCH SIMPLE\n" +
				"\t\tON UPDATE CASCADE ON DELETE CASCADE;\n\n" +
				"COMMENT ON CONSTRAINT \"test 5\" ON \"test 2\".\"test 3\" IS 'test 5';", val);
		assertTrue(val.endsWith(";"));
		val = constraint.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tDROP CONSTRAINT \"test 5\";\n\n" +
				"ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tADD CONSTRAINT \"test 5\"\n" +
				"\t\tFOREIGN KEY (\"test attr 1\")\n" +
				"\t\tREFERENCES \"test 2\".\"test 4\" (\"test attr 3\") MATCH SIMPLE\n" +
				"\t\tON UPDATE CASCADE ON DELETE CASCADE;\n\n" +
				"COMMENT ON CONSTRAINT \"test 5\" ON \"test 2\".\"test 3\" IS 'test 5';", val);
		assertTrue(val.endsWith(";"));

		// Change DESCRIPTION
		valuesForEdit = constraint.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Constraint.Behavior.L_DESCR, "new descr");

		val = constraint.getQueryChanged(CONNECTION);
		assertEquals("COMMENT ON CONSTRAINT \"test 5\" ON \"test 2\".\"test 3\" IS 'new descr';", val);
		assertTrue(val.endsWith(";"));
		val = constraint.getQueryChangeRevert(CONNECTION);
		assertEquals("COMMENT ON CONSTRAINT \"test 5\" ON \"test 2\".\"test 3\" IS 'test 5';", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreateFunction() throws Exception {
		String val = CONNECTION.getQueryCreate(function, null);
		assertEquals("-- test function\n" +
				"CREATE OR REPLACE FUNCTION \"test function\"(int input)\n" +
				"\tRETURNS varchar AS\n" +
				"$BODY$\n" +
				"RETURN CAST(input AS varchar);\n" +
				"$BODY$\n" +
				"\tLANGUAGE plpgsql stable\n" +
				"\tCOST 10 ROWS 100;\n\n" +
				"COMMENT ON FUNCTION \"test function\"(int input) IS 'test function';", val);
		assertTrue(val.endsWith(";"));

		val = function.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL FUNCTION **/\n\n" +
				"DROP FUNCTION IF EXISTS \"test function\"(int input);\n\n" +
				"-- test function\n" +
				"CREATE OR REPLACE FUNCTION \"test function\"(int input)\n" +
				"\tRETURNS varchar AS\n" +
				"$BODY$\n" +
				"RETURN CAST(input AS varchar);\n" +
				"$BODY$\n" +
				"\tLANGUAGE plpgsql stable\n" +
				"\tCOST 10 ROWS 100;\n\n" +
				"COMMENT ON FUNCTION \"test function\"(int input) IS 'test function';\n\n" +
				"/***********************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryDropFunction() throws Exception {
		String val = CONNECTION.getQueryDrop(function);
		assertEquals("DROP FUNCTION IF EXISTS \"test function\"(int input);", val);
		assertTrue(val.endsWith(";"));

		val = function.getQueryDrop(CONNECTION);
		assertEquals("DROP FUNCTION IF EXISTS \"test function\"(int input);", val);
	}

	@Test
	public void getQueryChangedFunction() throws Exception {
		IModelBehavior valuesForEdit;
		String val;

		function.getBehavior().prepareForEdit();
		assertEquals("", function.getQueryChanged(CONNECTION));
		assertEquals("", function.getQueryChangeRevert(CONNECTION));

		// Change NAME
		valuesForEdit = function.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Function.Behavior.L_NAME, "new name");

		val = function.getQueryChanged(CONNECTION);
		assertEquals("DROP FUNCTION IF EXISTS \"test function\"(int input);\n\n" +
				"-- test function\n" +
				"CREATE OR REPLACE FUNCTION \"new name\"(int input)\n" +
				"\tRETURNS varchar AS\n" +
				"$BODY$\n" +
				"RETURN CAST(input AS varchar);\n" +
				"$BODY$\n" +
				"\tLANGUAGE plpgsql stable\n" +
				"\tCOST 10 ROWS 100;\n\n" +
				"COMMENT ON FUNCTION \"new name\"(int input) IS 'test function';", val);
		assertTrue(val.endsWith(";"));
		val = function.getQueryChangeRevert(CONNECTION);
		assertEquals("DROP FUNCTION IF EXISTS \"new name\"(int input);\n\n" +
				"-- test function\n" +
				"CREATE OR REPLACE FUNCTION \"test function\"(int input)\n" +
				"\tRETURNS varchar AS\n" +
				"$BODY$\n" +
				"RETURN CAST(input AS varchar);\n" +
				"$BODY$\n" +
				"\tLANGUAGE plpgsql stable\n" +
				"\tCOST 10 ROWS 100;\n\n" +
				"COMMENT ON FUNCTION \"test function\"(int input) IS 'test function';", val);
		assertTrue(val.endsWith(";"));

		// Change SCHEMA
		valuesForEdit = function.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Function.Behavior.L_SCHEMA, "new schema");

		val = function.getQueryChanged(CONNECTION);
		assertEquals("DROP FUNCTION IF EXISTS \"test function\"(int input);\n\n" +
				"-- test function\n" +
				"CREATE OR REPLACE FUNCTION \"new schema\".\"test function\"(int input)\n" +
				"\tRETURNS varchar AS\n" +
				"$BODY$\n" +
				"RETURN CAST(input AS varchar);\n" +
				"$BODY$\n" +
				"\tLANGUAGE plpgsql stable\n" +
				"\tCOST 10 ROWS 100;\n\n" +
				"COMMENT ON FUNCTION \"new schema\".\"test function\"(int input) IS 'test function';", val);
		assertTrue(val.endsWith(";"));
		val = function.getQueryChangeRevert(CONNECTION);
		assertEquals("DROP FUNCTION IF EXISTS \"new schema\".\"test function\"(int input);\n\n" +
				"-- test function\n" +
				"CREATE OR REPLACE FUNCTION \"test function\"(int input)\n" +
				"\tRETURNS varchar AS\n" +
				"$BODY$\n" +
				"RETURN CAST(input AS varchar);\n" +
				"$BODY$\n" +
				"\tLANGUAGE plpgsql stable\n" +
				"\tCOST 10 ROWS 100;\n\n" +
				"COMMENT ON FUNCTION \"test function\"(int input) IS 'test function';", val);
		assertTrue(val.endsWith(";"));

		// Change IS UNIQUE, WHERE
		valuesForEdit = function.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Function.Behavior.L_ARGUMENTS, "new args");
		valuesForEdit.notifyChange(Function.Behavior.L_RETURNS, "new ret value");
		valuesForEdit.notifyChange(Function.Behavior.L_LANGUAGE, "new language");
		valuesForEdit.notifyChange(Function.Behavior.L_VOLATILITY, "immutable");
		valuesForEdit.notifyChange(Function.Behavior.L_STRICT, true);
		valuesForEdit.notifyChange(Function.Behavior.L_SEC_DEFINER, true);
		valuesForEdit.notifyChange(Function.Behavior.L_COST, "55");
		valuesForEdit.notifyChange(Function.Behavior.L_ROWS, "56");
		valuesForEdit.notifyChange(Function.Behavior.L_BODY, "\n\n\tnew body\n\n");

		val = function.getQueryChanged(CONNECTION);
		assertEquals("DROP FUNCTION IF EXISTS \"test function\"(int input);\n\n" +
				"-- test function\n" +
				"CREATE OR REPLACE FUNCTION \"test function\"(new args)\n" +
				"\tRETURNS new ret value AS\n" +
				"$BODY$\n" +
				"\n" +
				"\tnew body\n" +
				"\n" +
				"$BODY$\n" +
				"\tLANGUAGE new language immutable SECURITY DEFINER\n" +
				"\tCOST 55 ROWS 56;\n\n" +
				"COMMENT ON FUNCTION \"test function\"(new args) IS 'test function';", val);
		assertTrue(val.endsWith(";"));
		val = function.getQueryChangeRevert(CONNECTION);
		assertEquals("DROP FUNCTION IF EXISTS \"test function\"(new args);\n\n" +
				"-- test function\n" +
				"CREATE OR REPLACE FUNCTION \"test function\"(int input)\n" +
				"\tRETURNS varchar AS\n" +
				"$BODY$\n" +
				"RETURN CAST(input AS varchar);\n" +
				"$BODY$\n" +
				"\tLANGUAGE plpgsql stable\n" +
				"\tCOST 10 ROWS 100;\n\n" +
				"COMMENT ON FUNCTION \"test function\"(int input) IS 'test function';", val);
		assertTrue(val.endsWith(";"));

		// Change DESCRIPTION
		valuesForEdit = function.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Function.Behavior.L_DESCR, "new descr");

		val = function.getQueryChanged(CONNECTION);
		assertEquals("COMMENT ON FUNCTION \"test function\"(int input) IS 'new descr';", val);
		assertTrue(val.endsWith(";"));
		val = function.getQueryChangeRevert(CONNECTION);
		assertEquals("COMMENT ON FUNCTION \"test function\"(int input) IS 'test function';", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryExecFunction() throws Exception {
		String val = CONNECTION.getQueryExecFunction(function);
		assertEquals("SELECT * FROM \"test function\"(int input);", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreateIndex() throws Exception {
		String val = CONNECTION.getQueryCreate(primaryKey, null);
		assertEquals("-- test PK\n" +
				"ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tADD CONSTRAINT \"test PK\"\n" +
				"\t\tPRIMARY KEY (\"test attr 1\");\n\n" +
				"COMMENT ON CONSTRAINT \"test PK\" ON \"test 2\".\"test 3\" IS 'test PK';", val);
		assertTrue(val.endsWith(";"));

		val = primaryKey.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL PRIMARY KEY **/\n\n" +
				"ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tDROP CONSTRAINT \"test PK\";\n\n" +
				"-- test PK\n" +
				"ALTER TABLE \"test 2\".\"test 3\"\n" +
				"\tADD CONSTRAINT \"test PK\"\n" +
				"\t\tPRIMARY KEY (\"test attr 1\");\n\n" +
				"COMMENT ON CONSTRAINT \"test PK\" ON \"test 2\".\"test 3\" IS 'test PK';\n\n" +
				"/**************************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));

		val = CONNECTION.getQueryCreate(index, null);
		assertEquals("-- test index\n" +
				"CREATE INDEX \"test index\"\n" +
				"\tON \"test 2\".\"test 4\"\n" +
				"\tUSING btree\n" +
				"\t(\"test attr 4\")\n" +
				"WHERE \"test attr 4\" IS NOT NULL;\n\n" +
				"COMMENT ON INDEX \"test index\" IS 'test index';", val);
		assertTrue(val.endsWith(";"));

		val = index.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL INDEX **/\n\n" +
				"DROP INDEX \"test index\";\n\n" +
				"-- test index\n" +
				"CREATE INDEX \"test index\"\n" +
				"\tON \"test 2\".\"test 4\"\n" +
				"\tUSING btree\n" +
				"\t(\"test attr 4\")\n" +
				"WHERE \"test attr 4\" IS NOT NULL;\n\n" +
				"COMMENT ON INDEX \"test index\" IS 'test index';\n\n" +
				"/********************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));

		val = CONNECTION.getQueryCreate(uniqueIndex, null);
		assertEquals("-- test UX\n" +
				"CREATE UNIQUE INDEX \"test UX\"\n" +
				"\tON \"test 2\".\"test 3\"\n" +
				"\tUSING btree\n" +
				"\t(\"test attr 2\");\n\n" +
				"COMMENT ON INDEX \"test UX\" IS 'test UX';", val);
		assertTrue(val.endsWith(";"));

		val = uniqueIndex.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL INDEX **/\n\n" +
				"DROP INDEX \"test UX\";\n\n" +
				"-- test UX\n" +
				"CREATE UNIQUE INDEX \"test UX\"\n" +
				"\tON \"test 2\".\"test 3\"\n" +
				"\tUSING btree\n" +
				"\t(\"test attr 2\");\n\n" +
				"COMMENT ON INDEX \"test UX\" IS 'test UX';\n\n" +
				"/********************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));

		val = CONNECTION.getQueryCreate(uniqueConstraint, null);
		assertEquals("-- test UC\n" +
				"ALTER TABLE \"test 2\".\"test 4\"\n" +
				"\tADD CONSTRAINT \"test UC\"\n" +
				"\t\tUNIQUE (\"test attr 3\");\n" +
				"\n" +
				"COMMENT ON CONSTRAINT \"test UC\" ON \"test 2\".\"test 4\" IS 'test UC';", val);
		assertTrue(val.endsWith(";"));

		val = uniqueConstraint.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL CONSTRAINT **/\n\n" +
				"ALTER TABLE \"test 2\".\"test 4\"\n" +
				"\tDROP CONSTRAINT \"test UC\";\n\n" +
				"-- test UC\n" +
				"ALTER TABLE \"test 2\".\"test 4\"\n" +
				"\tADD CONSTRAINT \"test UC\"\n" +
				"\t\tUNIQUE (\"test attr 3\");\n\n" +
				"COMMENT ON CONSTRAINT \"test UC\" ON \"test 2\".\"test 4\" IS 'test UC';\n\n" +
				"/*************************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryDropIndex() throws Exception {
		String val = CONNECTION.getQueryDrop(primaryKey);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\"\n\tDROP CONSTRAINT \"test PK\";", val);
		assertTrue(val.endsWith(";"));

		val = primaryKey.getQueryDrop(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 3\"\n\tDROP CONSTRAINT \"test PK\";", val);
		assertTrue(val.endsWith(";"));

		val = CONNECTION.getQueryDrop(uniqueIndex);
		assertEquals("DROP INDEX \"test UX\";", val);
		assertTrue(val.endsWith(";"));

		val = uniqueIndex.getQueryDrop(CONNECTION);
		assertEquals("DROP INDEX \"test UX\";", val);
		assertTrue(val.endsWith(";"));

		val = CONNECTION.getQueryDrop(uniqueConstraint);
		assertEquals("ALTER TABLE \"test 2\".\"test 4\"\n\tDROP CONSTRAINT \"test UC\";", val);
		assertTrue(val.endsWith(";"));

		val = uniqueConstraint.getQueryDrop(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 4\"\n\tDROP CONSTRAINT \"test UC\";", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryChangedIndex() throws Exception {
		IModelBehavior valuesForEdit;
		String val;

		index.getBehavior().prepareForEdit();
		assertEquals("", index.getQueryChanged(CONNECTION));
		assertEquals("", index.getQueryChangeRevert(CONNECTION));

		// Change NAME
		valuesForEdit = index.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Index.Behavior.L_NAME, "new name");

		val = index.getQueryChanged(CONNECTION);
		assertEquals("ALTER INDEX \"test index\" RENAME TO \"new name\";", val);
		assertTrue(val.endsWith(";"));
		val = index.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER INDEX \"new name\" RENAME TO \"test index\";", val);
		assertTrue(val.endsWith(";"));

		// Change IS UNIQUE, WHERE
		valuesForEdit = index.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Index.Behavior.L_CONDITION, "new condition");
		valuesForEdit.notifyChange(Index.Behavior.L_UNIQUE, true);

		val = index.getQueryChanged(CONNECTION);
		assertEquals("DROP INDEX \"test index\";\n\n" +
				"-- test index\n" +
				"CREATE UNIQUE INDEX \"test index\"\n" +
				"\tON \"test 2\".\"test 4\"\n" +
				"\tUSING btree\n" +
				"\t(\"test attr 4\")\n" +
				"WHERE new condition;\n\n" +
				"COMMENT ON INDEX \"test index\" IS 'test index';", val);
		assertTrue(val.endsWith(";"));
		val = index.getQueryChangeRevert(CONNECTION);
		assertEquals("DROP INDEX \"test index\";\n\n" +
				"-- test index\n" +
				"CREATE INDEX \"test index\"\n" +
				"\tON \"test 2\".\"test 4\"\n" +
				"\tUSING btree\n" +
				"\t(\"test attr 4\")\n" +
				"WHERE \"test attr 4\" IS NOT NULL;\n\n" +
				"COMMENT ON INDEX \"test index\" IS 'test index';", val);
		assertTrue(val.endsWith(";"));

		// Change DESCRIPTION
		valuesForEdit = index.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Index.Behavior.L_DESCR, "new descr");

		val = index.getQueryChanged(CONNECTION);
		assertEquals("COMMENT ON INDEX \"test index\" IS 'new descr';", val);
		assertTrue(val.endsWith(";"));
		val = index.getQueryChangeRevert(CONNECTION);
		assertEquals("COMMENT ON INDEX \"test index\" IS 'test index';", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreatePackage() throws Exception {
		// Not supported yet
	}

	@Test
	public void getQueryDropPackage() throws Exception {
		// Not supported yet
	}

	@Test
	public void getQueryChangedPackage() throws Exception {
		// Not supported yet
	}

	@Test
	public void getQueryCreateSequence() throws Exception {
		String val = CONNECTION.getQueryCreate(sequence, null);
		assertEquals("-- test sequence\n" +
				"CREATE SEQUENCE \"test sequence\"\n" +
				"\tSTART WITH 5\n" +
				"\tINCREMENT BY 1\n" +
				"\tMINVALUE 0\n" +
				"\tMAXVALUE 1000000\n" +
				"\tCYCLE;\n" +
				"\n" +
				"COMMENT ON SEQUENCE \"test sequence\" IS 'test sequence';", val);
		assertTrue(val.endsWith(";"));

		val = sequence.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL SEQUENCE **/\n\n" +
				"DROP SEQUENCE \"test sequence\";\n\n" +
				"-- test sequence\n" +
				"CREATE SEQUENCE \"test sequence\"\n" +
				"\tSTART WITH 5\n" +
				"\tINCREMENT BY 1\n" +
				"\tMINVALUE 0\n" +
				"\tMAXVALUE 1000000\n" +
				"\tCYCLE;\n\n" +
				"COMMENT ON SEQUENCE \"test sequence\" IS 'test sequence';\n\n" +
				"/***********************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryDropSequence() throws Exception {
		String val = CONNECTION.getQueryDrop(sequence);
		assertEquals("DROP SEQUENCE \"test sequence\";", val);
		assertTrue(val.endsWith(";"));

		val = sequence.getQueryDrop(CONNECTION);
		assertEquals("DROP SEQUENCE \"test sequence\";", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryChangedSequence() throws Exception {
		IModelBehavior valuesForEdit;
		String val;

		sequence.getBehavior().prepareForEdit();
		assertEquals("", sequence.getQueryChanged(CONNECTION));
		assertEquals("", sequence.getQueryChangeRevert(CONNECTION));

		// Change NAME
		valuesForEdit = sequence.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Sequence.Behavior.L_NAME, "new name");

		val = sequence.getQueryChanged(CONNECTION);
		assertEquals("ALTER SEQUENCE \"test sequence\" RENAME TO \"new name\";", val);
		assertTrue(val.endsWith(";"));
		val = sequence.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER SEQUENCE \"new name\" RENAME TO \"test sequence\";", val);
		assertTrue(val.endsWith(";"));

		// Change SCHEMA
		valuesForEdit = sequence.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Sequence.Behavior.L_SCHEMA, "new schema");

		val = sequence.getQueryChanged(CONNECTION);
		assertEquals("ALTER SEQUENCE \"test sequence\" SET SCHEMA \"new schema\";", val);
		assertTrue(val.endsWith(";"));
		val = sequence.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER SEQUENCE \"new schema\".\"test sequence\" SET SCHEMA public;", val);
		assertTrue(val.endsWith(";"));

		// Change MIN, MAX, INCREMENT, CYCLE
		valuesForEdit = sequence.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Sequence.Behavior.L_MIN, "25");
		valuesForEdit.notifyChange(Sequence.Behavior.L_START, "25");
		valuesForEdit.notifyChange(Sequence.Behavior.L_MAX, "250");
		valuesForEdit.notifyChange(Sequence.Behavior.L_INCREMENT, "5");
		valuesForEdit.notifyChange(Sequence.Behavior.L_CYCLE, false);

		val = sequence.getQueryChanged(CONNECTION);
		assertEquals("ALTER SEQUENCE \"test sequence\"\n" +
				"\tINCREMENT BY 5\n" +
				"\tMINVALUE 25\n" +
				"\tMAXVALUE 250\n" +
				"\tSTART WITH 25 RESTART WITH 25\n" +
				"\tNO CYCLE;", val);
		assertTrue(val.endsWith(";"));
		val = sequence.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER SEQUENCE \"test sequence\"\n" +
				"\tINCREMENT BY 1\n" +
				"\tMINVALUE 0\n" +
				"\tMAXVALUE 1000000\n" +
				"\tSTART WITH 5 RESTART WITH 5\n" +
				"\tCYCLE;", val);
		assertTrue(val.endsWith(";"));

		// Change DESCRIPTION
		valuesForEdit = sequence.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Sequence.Behavior.L_DESCR, "new descr");

		val = sequence.getQueryChanged(CONNECTION);
		assertEquals("COMMENT ON SEQUENCE \"test sequence\" IS 'new descr';", val);
		assertTrue(val.endsWith(";"));
		val = sequence.getQueryChangeRevert(CONNECTION);
		assertEquals("COMMENT ON SEQUENCE \"test sequence\" IS 'test sequence';", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreateTrigger() throws Exception {
		String val = CONNECTION.getQueryCreate(trigger, null);
		assertEquals("-- test trigger\n" +
				"CREATE TRIGGER \"test trigger\"\n" +
				"\tBEFORE UPDATE OF test attr 3, test attr 4\n" +
				"\tON \"test 2\".\"test 4\"\n" +
				"\tFOR EACH ROW\n" +
				"\tWHEN (\"test attr 4\" IS NOT NULL)\n" +
				"\tEXECUTE PROCEDURE \"test trigger function\"();\n" +
				"\n" +
				"ALTER TABLE \"test 2\".\"test 4\" DISABLE TRIGGER \"test trigger\";\n" +
				"\n" +
				"COMMENT ON TRIGGER \"test trigger\" ON \"test 2\".\"test 4\" IS 'test trigger';", val);
		assertTrue(val.endsWith(";"));

		val = trigger.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL TRIGGER **/\n\n" +
				"DROP TRIGGER \"test trigger\" ON \"test 2\".\"test 4\";\n\n" +
				"-- test trigger\n" +
				"CREATE TRIGGER \"test trigger\"\n" +
				"\tBEFORE UPDATE OF test attr 3, test attr 4\n" +
				"\tON \"test 2\".\"test 4\"\n" +
				"\tFOR EACH ROW\n" +
				"\tWHEN (\"test attr 4\" IS NOT NULL)\n" +
				"\tEXECUTE PROCEDURE \"test trigger function\"();\n\n" +
				"ALTER TABLE \"test 2\".\"test 4\" DISABLE TRIGGER \"test trigger\";\n\n" +
				"COMMENT ON TRIGGER \"test trigger\" ON \"test 2\".\"test 4\" IS 'test trigger';\n\n" +
				"/**********************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryDropTrigger() throws Exception {
		String val = CONNECTION.getQueryDrop(trigger);
		assertEquals("DROP TRIGGER \"test trigger\" ON \"test 2\".\"test 4\";", val);
		assertTrue(val.endsWith(";"));

		val = trigger.getQueryDrop(CONNECTION);
		assertEquals("DROP TRIGGER \"test trigger\" ON \"test 2\".\"test 4\";", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryChangedTrigger() throws Exception {
		IModelBehavior valuesForEdit;
		String val;

		trigger.getBehavior().prepareForEdit();
		assertEquals("", trigger.getQueryChanged(CONNECTION));
		assertEquals("", trigger.getQueryChangeRevert(CONNECTION));

		// Change NAME
		valuesForEdit = trigger.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Trigger.Behavior.L_NAME, "new name");

		val = trigger.getQueryChanged(CONNECTION);
		assertEquals("ALTER TRIGGER \"test trigger\" ON \"test 2\".\"test 4\" RENAME TO \"new name\";", val);
		assertTrue(val.endsWith(";"));
		val = trigger.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TRIGGER \"new name\" ON \"test 2\".\"test 4\" RENAME TO \"test trigger\";", val);
		assertTrue(val.endsWith(";"));

		// Change CONDITION
		valuesForEdit = trigger.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Trigger.Behavior.L_WHEN, "new condition");

		val = trigger.getQueryChanged(CONNECTION);
		assertEquals("DROP TRIGGER \"test trigger\" ON \"test 2\".\"test 4\";\n\n" +
				"-- test trigger\n" +
				"CREATE TRIGGER \"test trigger\"\n" +
				"\tBEFORE UPDATE OF test attr 3, test attr 4\n" +
				"\tON \"test 2\".\"test 4\"\n" +
				"\tFOR EACH ROW\n" +
				"\tWHEN (new condition)\n" +
				"\tEXECUTE PROCEDURE \"test trigger function\"();\n\n" +
				"ALTER TABLE \"test 2\".\"test 4\" DISABLE TRIGGER \"test trigger\";\n\n" +
				"COMMENT ON TRIGGER \"test trigger\" ON \"test 2\".\"test 4\" IS 'test trigger';", val);
		assertTrue(val.endsWith(";"));
		val = trigger.getQueryChangeRevert(CONNECTION);
		assertEquals("DROP TRIGGER \"test trigger\" ON \"test 2\".\"test 4\";\n\n" +
				"-- test trigger\n" +
				"CREATE TRIGGER \"test trigger\"\n" +
				"\tBEFORE UPDATE OF test attr 3, test attr 4\n" +
				"\tON \"test 2\".\"test 4\"\n" +
				"\tFOR EACH ROW\n" +
				"\tWHEN (\"test attr 4\" IS NOT NULL)\n" +
				"\tEXECUTE PROCEDURE \"test trigger function\"();\n\n" +
				"ALTER TABLE \"test 2\".\"test 4\" DISABLE TRIGGER \"test trigger\";\n\n" +
				"COMMENT ON TRIGGER \"test trigger\" ON \"test 2\".\"test 4\" IS 'test trigger';", val);
		assertTrue(val.endsWith(";"));

		// Change ENABLED
		valuesForEdit = trigger.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Trigger.Behavior.L_ENABLED, true);

		val = trigger.getQueryChanged(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 4\" ENABLE TRIGGER \"test trigger\";", val);
		assertTrue(val.endsWith(";"));
		val = trigger.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE \"test 2\".\"test 4\" DISABLE TRIGGER \"test trigger\";", val);
		assertTrue(val.endsWith(";"));

		// Change DESCRIPTION
		valuesForEdit = trigger.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Trigger.Behavior.L_DESCR, "new descr");

		val = trigger.getQueryChanged(CONNECTION);
		assertEquals("COMMENT ON TRIGGER \"test trigger\" ON \"test 2\".\"test 4\" IS 'new descr';", val);
		assertTrue(val.endsWith(";"));
		val = trigger.getQueryChangeRevert(CONNECTION);
		assertEquals("COMMENT ON TRIGGER \"test trigger\" ON \"test 2\".\"test 4\" IS 'test trigger';", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreateView() throws Exception {
		String val = CONNECTION.getQueryCreate(view, null);
		assertEquals("-- test view\n" +
				"CREATE OR REPLACE VIEW \"test view\"\n" +
				"\tAS SELECT * FROM \"test 4\";\n\n" +
				"COMMENT ON VIEW \"test view\" IS 'test view';", val);
		assertTrue(val.endsWith(";"));

		val = view.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL VIEW **/\n\n" +
				"DROP VIEW \"test view\";\n\n" +
				"-- test view\n" +
				"CREATE OR REPLACE VIEW \"test view\"\n" +
				"\tAS SELECT * FROM \"test 4\";\n\n" +
				"COMMENT ON VIEW \"test view\" IS 'test view';\n\n" +
				"/*******************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryDropView() throws Exception {
		String val = CONNECTION.getQueryDrop(view);
		assertEquals("DROP VIEW \"test view\";", val);
		assertTrue(val.endsWith(";"));

		val = view.getQueryDrop(CONNECTION);
		assertEquals("DROP VIEW \"test view\";", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryChangedView() throws Exception {
		IModelBehavior valuesForEdit;
		String val;

		view.getBehavior().prepareForEdit();
		assertEquals("", view.getQueryChanged(CONNECTION));
		assertEquals("", view.getQueryChangeRevert(CONNECTION));

		// Change NAME
		valuesForEdit = view.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(View.Behavior.L_NAME, "new name");

		val = view.getQueryChanged(CONNECTION);
		assertEquals("ALTER TABLE \"test view\" RENAME TO \"new name\";", val);
		assertTrue(val.endsWith(";"));
		val = view.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE \"new name\" RENAME TO \"test view\";", val);
		assertTrue(val.endsWith(";"));

		// Change SCHEMA
		valuesForEdit = view.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(View.Behavior.L_SCHEMA, "new schema");

		val = view.getQueryChanged(CONNECTION);
		assertEquals("ALTER TABLE \"test view\" SET SCHEMA \"new schema\";", val);
		assertTrue(val.endsWith(";"));
		val = view.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE \"new schema\".\"test view\" SET SCHEMA public;", val);
		assertTrue(val.endsWith(";"));

		// Change BODY
		valuesForEdit = view.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(View.Behavior.L_BODY, "new\nbody;");

		val = view.getQueryChanged(CONNECTION);
		assertEquals("DROP VIEW \"test view\";\n\n" +
				"-- test view\n" +
				"CREATE OR REPLACE VIEW \"test view\"\n" +
				"\tAS new\nbody;\n\n" +
				"COMMENT ON VIEW \"test view\" IS 'test view';", val);
		assertTrue(val.endsWith(";"));
		val = view.getQueryChangeRevert(CONNECTION);
		assertEquals("DROP VIEW \"test view\";\n\n" +
				"-- test view\n" +
				"CREATE OR REPLACE VIEW \"test view\"\n" +
				"\tAS SELECT * FROM \"test 4\";\n\n" +
				"COMMENT ON VIEW \"test view\" IS 'test view';", val);
		assertTrue(val.endsWith(";"));

		// Change DESCRIPTION
		valuesForEdit = view.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(View.Behavior.L_DESCR, "new descr");

		val = view.getQueryChanged(CONNECTION);
		assertEquals("COMMENT ON VIEW \"test view\" IS 'new descr';", val);
		assertTrue(val.endsWith(";"));
		val = view.getQueryChangeRevert(CONNECTION);
		assertEquals("COMMENT ON VIEW \"test view\" IS 'test view';", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryTerminate() throws Exception {
		String val = CONNECTION.getQueryTerminate(123);
		assertEquals("SELECT pg_terminate_backend(123);", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCancel() throws Exception {
		String val = CONNECTION.getQueryCancel(123);
		assertEquals("SELECT pg_cancel_backend(123);", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void escape() throws Exception {
		assertEquals("test", CONNECTION.escape("test"));
		assertEquals("\"test 1\"", CONNECTION.escape("test 1"));
		assertEquals("\"test.\"", CONNECTION.escape("test."));
	}

	@Test
	public void escapeFullName() throws Exception {
		assertEquals("test", CONNECTION.escapeFullName("test"));
		assertEquals("\"test 1\"", CONNECTION.escapeFullName("test 1"));
		assertEquals("\"test.\"", CONNECTION.escapeFullName("test."));
		assertEquals("test.\"asdf 1\"", CONNECTION.escapeFullName("test.asdf 1"));
		assertEquals("\"test 1\".\"asdf 1\"", CONNECTION.escapeFullName("test 1.asdf 1"));
	}

	@Test
	public void getQueryConnect() throws Exception {
		assertEquals("SELECT 1", CONNECTION.getQueryConnect());
	}

	@Test
	public void getQueryWhere() throws Exception {
		assertEquals("SELECT * FROM table WHERE column = 1 AND id = 2", CONNECTION.getQueryWhere("SELECT * FROM table WHERE id = 2", "column = 1"));
		assertEquals("SELECT * FROM table WHERE column = 1 AND (id = 2 OR id = 1)", CONNECTION.getQueryWhere("SELECT * FROM table WHERE id = 2 OR id = 1", "column = 1"));
	}

	@Test
	public void getQueryOrder() throws Exception {
		String val = CONNECTION.getQueryOrder("SELECT * FROM table LIMIT 10", "column1 ASC");
		assertEquals("SELECT * FROM table\nORDER BY column1 ASC\nLIMIT 10", val);

		val = CONNECTION.getQueryOrder("SELECT * FROM table WHERE id = 2;", "column1 ASC");
		assertEquals("SELECT * FROM table WHERE id = 2\nORDER BY column1 ASC;", val);
		assertTrue(val.endsWith(";"));

		val = CONNECTION.getQueryOrder("SELECT * FROM table WHERE id = 2 ORDER BY id;", "column1 ASC");
		assertEquals("SELECT * FROM table WHERE id = 2 ORDER BY column1 ASC, id;", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryDelete() throws Exception {
		Map<String, String> pkValues = new HashMap<>();
		pkValues.put(attribute1.getName(), "abc");

		String val = CONNECTION.getQueryDelete(relation, pkValues);
		assertEquals("DELETE FROM \"test 2\".\"test 3\" WHERE \"test attr 1\" = 'abc';", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryUpdate() throws Exception {
		Map<String, String> pkValues = new HashMap<>();
		pkValues.put(attribute3.getName(), "abc");

		String val = CONNECTION.getQueryUpdate(relation2, pkValues, attribute4.getName(), "this won't work I guess");
		assertEquals("UPDATE \"test 2\".\"test 4\" SET \"test attr 4\" = 'this won''t work I guess' WHERE \"test attr 3\" = 'abc';", val);
		assertTrue(val.endsWith(";"));

		val = CONNECTION.getQueryUpdate(relation2, pkValues, attribute4.getName(), null);
		assertEquals("UPDATE \"test 2\".\"test 4\" SET \"test attr 4\" = NULL WHERE \"test attr 3\" = 'abc';", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryUpdateMap() throws Exception {
		Map<String, String> pkValues = new HashMap<>();
		pkValues.put(attribute3.getName(), "abc");

		Map<String, String> colValues = new HashMap<>();
		colValues.put(attribute3.getName(), "this won't work I guess");
		colValues.put(attribute4.getName(), null);

		String val = CONNECTION.getQueryUpdate(relation2, pkValues, colValues);
		assertEquals("UPDATE \"test 2\".\"test 4\" SET \"test attr 4\" = NULL, \"test attr 3\" = 'this won''t work I guess' WHERE \"test attr 3\" = 'abc';", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryUpdateByName() throws Exception {
		Map<String, String> pkValues = new HashMap<>();
		pkValues.put(attribute3.getName(), "abc");

		Map<String, String> colValues = new HashMap<>();
		colValues.put(attribute3.getName(), "this won't work I guess");
		colValues.put(attribute4.getName(), null);

		String val = CONNECTION.getQueryUpdate(relation2.getFullName(), pkValues, colValues);
		assertEquals("UPDATE \"test 2\".\"test 4\" SET \"test attr 4\" = NULL, \"test attr 3\" = 'this won''t work I guess' WHERE \"test attr 3\" = 'abc';", val);
		assertTrue(val.endsWith(";"));

		val = CONNECTION.getQueryUpdate(relation2.getFullName(), null, colValues);
		assertEquals("UPDATE \"test 2\".\"test 4\" SET \"test attr 4\" = NULL, \"test attr 3\" = 'this won''t work I guess' WHERE /** TODO: ADD CONDITIONS HERE **/;", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryInsert() throws Exception {
		Map<String, String> colValues = new HashMap<>();
		colValues.put(attribute3.getName(), "this won't work I guess");
		colValues.put(attribute4.getName(), null);

		String val = CONNECTION.getQueryInsert(relation2.getFullName(), colValues);
		assertEquals("INSERT INTO \"test 2\".\"test 4\" (\"test attr 4\", \"test attr 3\") VALUES (NULL, 'this won''t work I guess');", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getWhereFromDef() throws Exception {
		assertEquals("\"test attr 4\" IS NOT NULL", index.getBehavior().getWhere());
		assertEquals("\"test attr 4\"", index.getBehavior().getDef());
	}

	@Test
	public void getQueryVacuum() throws Exception {
		String val = CONNECTION.getQueryVacuum("VACUUM FULL", Arrays.asList(relation, relation2));
		assertEquals("VACUUM FULL \"test 2\".\"test 3\";\nVACUUM FULL \"test 2\".\"test 4\";", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQuerySelect() throws Exception {
		String val = CONNECTION.getQuerySelect(relation, null, null, 10);
		assertEquals("SELECT *\n" +
				"FROM \"test 2\".\"test 3\"\n" +
				"LIMIT 10;", val);
		assertTrue(val.endsWith(";"));

		val = CONNECTION.getQuerySelect(relation, "id = 2", null, 10);
		assertEquals("SELECT *\n" +
				"FROM \"test 2\".\"test 3\"\n" +
				"WHERE id = 2\n" +
				"LIMIT 10;", val);
		assertTrue(val.endsWith(";"));

		val = CONNECTION.getQuerySelect(relation, null, "id", 10);
		assertEquals("SELECT *\n" +
				"FROM \"test 2\".\"test 3\"\n" +
				"ORDER BY id\n" +
				"LIMIT 10;", val);
		assertTrue(val.endsWith(";"));

		val = CONNECTION.getQuerySelect(relation, "id = 2", "id", 10);
		assertEquals("SELECT *\n" +
				"FROM \"test 2\".\"test 3\"\n" +
				"WHERE id = 2\n" +
				"ORDER BY id\n" +
				"LIMIT 10;", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQuerySelectConstraint() throws Exception {
		String val = CONNECTION.getQuerySelect(constraint);
		assertEquals("SELECT *\n" +
				"FROM \"test 2\".\"test 3\" t3\n" +
				"JOIN \"test 2\".\"test 4\" t4 ON t3.\"test attr 1\" = t4.\"test attr 3\"\n" +
				"LIMIT 200;", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryLocks() throws Exception {
		String val = CONNECTION.getQueryLocks();
		assertEquals("SELECT count(*), (SELECT count(*) FROM pg_lock_status() WHERE database IS NOT NULL) FROM pg_stat_get_activity(NULL);", val);
		assertTrue(val.endsWith(";"));
	}

}
