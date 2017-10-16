package com.databazoo.devmodeler.conn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.databazoo.devmodeler.TestProjectSetup;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.IModelBehavior;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.model.View;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;

/**
 * MySQL static test.
 */
public class ConnectionMyTest extends TestProjectSetup {

	private static ConnectionMy CONNECTION = new ConnectionMy("test", "test", "test", "test");

	@BeforeClass
	public static void setProjectUp() {
		Settings.init();
		Settings.put(Settings.L_NAMING_ESCAPE_MY, false);

		ProjectManager.getInstance().setCurrentProject(ProjectManager.getInstance().createNew("test1", Project.TYPE_MY));
		Project.getCurrent().getConnections().add(CONNECTION);
	}

	@Before
	public void finalizeDataMy(){
		relation.getBehavior().setCollation("utf8_unicode_ci");
		attribute1.getBehavior().setDefaultValue(attribute1.getBehavior().getDefaultValue().replace("'", ""));
		attribute1.getBehavior().setCollation("utf8_general_ci");
		primaryKey.getBehavior().setDef(primaryKey.getBehavior().getDef().replace("\"", "`"));
		uniqueIndex.getBehavior().setDef(uniqueIndex.getBehavior().getDef().replace("\"", "`"));
		uniqueConstraint.getBehavior().setDef(uniqueConstraint.getBehavior().getDef().replace("\"", "`"));
		index.getBehavior().setDef("`test attr 4`");
		function.getBehavior().setSrc("BEGIN\n\tRETURN CONCAT('Hello ', addressee);\nEND;");
		trigger.getBehavior().setDef("BEGIN\n\tSET NEW.value = 5;\nEND;");
	}

	@Test
	public void getPresetFucntionArgs() throws Exception {
		assertEquals("", CONNECTION.getPresetFucntionArgs(true));
		assertEquals("", CONNECTION.getPresetFucntionArgs(false));
	}

	@Test
	public void getPresetFucntionReturn() throws Exception {
		assertEquals("", CONNECTION.getPresetFucntionReturn(true));
		assertEquals("", CONNECTION.getPresetFucntionReturn(false));

	}

	@Test
	public void getPresetFucntionSource() throws Exception {
		assertEquals("\nBEGIN\n\n\t/** TODO **/\n\nEND;\n", CONNECTION.getPresetFucntionSource(true));
		assertEquals("\nBEGIN\n\n\t/** TODO **/\n\nEND;\n", CONNECTION.getPresetFucntionSource(false));
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
		assertEquals("test 123", CONNECTION.getCleanError("java.sql.SQLException: test 123"));
	}

	@Test
	public void getErrorPosition() throws Exception {
		Point res = CONNECTION.getErrorPosition("SELECT contents FROM trololo", "Incorrect string value: '\\xE4\\xC5\\xCC\\xC9\\xD3\\xD8...' for column 'contents' at row 1");
		assertEquals(7, res.x);
		assertEquals(15, res.y);
	}

	@Test
	public void getQueryLimitOffset() throws Exception {
		assertEquals("LIMIT 10, 11", CONNECTION.getQueryLimitOffset(11, 10));
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
		assertEquals("CREATE DATABASE `test 1`;", val);
		assertTrue(val.endsWith(";"));

		val = database.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL DATABASE **/\n\n" +
				"DROP DATABASE `test 1`;\n\n" +
				"CREATE DATABASE `test 1`;\n\n" +
				"/***********************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryDropDatabase() throws Exception {
		String val = CONNECTION.getQueryDrop(database);
		assertEquals("DROP DATABASE `test 1`;", val);
		assertTrue(val.endsWith(";"));

		val = database.getQueryDrop(CONNECTION);
		assertEquals("DROP DATABASE `test 1`;", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreateSchema() throws Exception {
		String val = CONNECTION.getQueryCreate(schema, null);
		assertEquals("CREATE SCHEMA `test 2`;", val);
		assertTrue(val.endsWith(";"));

		val = schema.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL SCHEMA **/\n\n" +
				"DROP SCHEMA `test 2`;\n\n" +
				"CREATE SCHEMA `test 2`;\n\n" +
				"/*********************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryDropSchema() throws Exception {
		String val = CONNECTION.getQueryDrop(schema);
		assertEquals("DROP SCHEMA `test 2`;", val);
		assertTrue(val.endsWith(";"));

		val = schema.getQueryDrop(CONNECTION);
		assertEquals("DROP SCHEMA `test 2`;", val);
		assertTrue(val.endsWith(";"));
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
		assertEquals("ALTER SCHEMA `test 2` RENAME TO `new name`;", val);
		assertTrue(val.endsWith(";"));
		val = schema.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER SCHEMA `new name` RENAME TO `test 2`;", val);
		assertTrue(val.endsWith(";"));

		// Change DESCRIPTION
		valuesForEdit = schema.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Schema.Behavior.L_DESCR, "new descr");

		val = schema.getQueryChanged(CONNECTION);
		assertEquals("ALTER SCHEMA `test 2` COMMENT 'new descr';", val);
		assertTrue(val.endsWith(";"));
		val = schema.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER SCHEMA `test 2` COMMENT 'test 2';", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreateTable() throws Exception {
		String val = CONNECTION.getQueryCreate(relation, null);
		assertEquals("CREATE TABLE `test 3` (\n" +
						"\t`test attr 1` varchar NOT NULL DEFAULT 'defval' COLLATE utf8_general_ci COMMENT 'test attr 1',\n" +
						"\t`test attr 2` varchar NOT NULL COMMENT 'test attr 2',\n" +
						"\tUNIQUE INDEX `test UX` (`test attr 2`),\n" +
						"\tCONSTRAINT `test 6` CHECK ( id > 0 ),\n" +
						"\tCONSTRAINT `test 5` \n" +
						"\t\tFOREIGN KEY (`test attr 1`)\n" +
						"\t\tREFERENCES `test 4` (`test attr 3`)\n" +
						"\t\tON UPDATE CASCADE ON DELETE CASCADE\n" +
						") COLLATE utf8_unicode_ci ENGINE=MyISAM;",
				val);
		assertTrue(val.endsWith(";"));

		val = relation.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL TABLE **/\n\n" +
						"DROP TABLE `test 3`;\n\n" +
						"CREATE TABLE `test 3` (\n" +
						"\t`test attr 1` varchar NOT NULL DEFAULT 'defval' COLLATE utf8_general_ci COMMENT 'test attr 1',\n" +
						"\t`test attr 2` varchar NOT NULL COMMENT 'test attr 2',\n" +
						"\tUNIQUE INDEX `test UX` (`test attr 2`),\n" +
						"\tCONSTRAINT `test 6` CHECK ( id > 0 ),\n" +
						"\tCONSTRAINT `test 5` \n" +
						"\t\tFOREIGN KEY (`test attr 1`)\n" +
						"\t\tREFERENCES `test 4` (`test attr 3`)\n" +
						"\t\tON UPDATE CASCADE ON DELETE CASCADE\n" +
						") COLLATE utf8_unicode_ci ENGINE=MyISAM;\n\n" +
						"/********************/",
				val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryDropTable() throws Exception {
		String val = CONNECTION.getQueryDrop(relation);
		assertEquals("DROP TABLE `test 3`;", val);
		assertTrue(val.endsWith(";"));

		val = relation.getQueryDrop(CONNECTION);
		assertEquals("DROP TABLE `test 3`;", val);
		assertTrue(val.endsWith(";"));
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
		assertEquals("ALTER TABLE `test 3` RENAME TO `new name`;", val);
		assertTrue(val.endsWith(";"));
		val = relation.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE `new name` RENAME TO `test 3`;", val);
		assertTrue(val.endsWith(";"));

		// Change DEFINITION
		valuesForEdit = relation.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Relation.Behavior.L_COLLATION, "central_european");

		val = relation.getQueryChanged(CONNECTION);
		assertEquals("ALTER TABLE `test 3` COLLATE central_european;", val);
		assertTrue(val.endsWith(";"));
		val = relation.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE `test 3` COLLATE utf8_unicode_ci;", val);
		assertTrue(val.endsWith(";"));

		// Change DESCRIPTION
		valuesForEdit = relation.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Relation.Behavior.L_DESCR, "new descr");

		val = relation.getQueryChanged(CONNECTION);
		assertEquals("ALTER TABLE `test 3` COMMENT 'new descr';", val);
		assertTrue(val.endsWith(";"));
		val = relation.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE `test 3` COMMENT '';", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreateAttribute() throws Exception {
		String val = CONNECTION.getQueryCreate(attribute1, null);
		assertEquals("ALTER TABLE `test 3`\n\tADD COLUMN `test attr 1` varchar NOT NULL DEFAULT 'defval' COLLATE utf8_general_ci COMMENT 'test attr 1';", val);
		assertTrue(val.endsWith(";"));

		val = attribute1.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL ATTRIBUTE **/\n\n" +
				"ALTER TABLE `test 3`\n" +
				"\tDROP COLUMN `test attr 1`;\n\n" +
				"ALTER TABLE `test 3`\n\tADD COLUMN `test attr 1` varchar NOT NULL DEFAULT 'defval' COLLATE utf8_general_ci COMMENT 'test attr 1';\n\n" +
				"/************************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryDefAttribute() throws Exception {
		assertEquals("varchar NOT NULL DEFAULT 'defval' COLLATE utf8_general_ci COMMENT 'test attr 1'", CONNECTION.getQueryDef(attribute1, null));
	}

	@Test
	public void getQueryDropAttribute() throws Exception {
		String val = CONNECTION.getQueryDrop(attribute1);
		assertEquals("ALTER TABLE `test 3`\n\tDROP COLUMN `test attr 1`;", val);
		assertTrue(val.endsWith(";"));

		val = attribute1.getQueryDrop(CONNECTION);
		assertEquals("ALTER TABLE `test 3`\n\tDROP COLUMN `test attr 1`;", val);
		assertTrue(val.endsWith(";"));
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
		assertEquals("ALTER TABLE `test 3` CHANGE `test attr 1` `new name` varchar NOT NULL DEFAULT 'defval' COLLATE utf8_general_ci COMMENT 'test attr 1';", val);
		assertTrue(val.endsWith(";"));
		val = attribute1.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE `test 3` CHANGE `new name` `test attr 1` varchar NOT NULL DEFAULT 'defval' COLLATE utf8_general_ci COMMENT 'test attr 1';", val);
		assertTrue(val.endsWith(";"));

		// Change DEFINITION
		valuesForEdit = attribute1.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Attribute.Behavior.L_DEFAULT, "");
		valuesForEdit.notifyChange(Attribute.Behavior.L_PRECISION, "255");
		valuesForEdit.notifyChange(Relation.Behavior.L_COLLATION, "central_european");

		val = attribute1.getQueryChanged(CONNECTION);
		assertEquals("ALTER TABLE `test 3` CHANGE `test attr 1` `test attr 1` varchar(255) NOT NULL COLLATE central_european COMMENT 'test attr 1';", val);
		assertTrue(val.endsWith(";"));
		val = attribute1.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE `test 3` CHANGE `test attr 1` `test attr 1` varchar NOT NULL DEFAULT 'defval' COLLATE utf8_general_ci COMMENT 'test attr 1';", val);
		assertTrue(val.endsWith(";"));

		// Change DESCRIPTION
		valuesForEdit = attribute1.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Attribute.Behavior.L_DESCR, "new descr");

		val = attribute1.getQueryChanged(CONNECTION);
		assertEquals("ALTER TABLE `test 3` CHANGE `test attr 1` `test attr 1` varchar NOT NULL DEFAULT 'defval' COLLATE utf8_general_ci COMMENT 'new descr';", val);
		assertTrue(val.endsWith(";"));
		val = attribute1.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE `test 3` CHANGE `test attr 1` `test attr 1` varchar NOT NULL DEFAULT 'defval' COLLATE utf8_general_ci COMMENT 'test attr 1';", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreateWithoutCommentConstraint() throws Exception {
		String val = CONNECTION.getQueryCreateWithoutComment(constraint, null);
		assertEquals("ALTER TABLE `test 3`\n" +
				"\tADD CONSTRAINT `test 5`\n" +
				"\t\tFOREIGN KEY (`test attr 1`)\n" +
				"\t\tREFERENCES `test 4` (`test attr 3`)\n" +
				"\t\tON UPDATE CASCADE ON DELETE CASCADE;", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreateConstraint() throws Exception {
		String val = CONNECTION.getQueryCreate(constraint, null);
		assertEquals("ALTER TABLE `test 3`\n" +
				"\tADD CONSTRAINT `test 5`\n" +
				"\t\tFOREIGN KEY (`test attr 1`)\n" +
				"\t\tREFERENCES `test 4` (`test attr 3`)\n" +
				"\t\tON UPDATE CASCADE ON DELETE CASCADE;", val);
		assertTrue(val.endsWith(";"));

		val = constraint.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL CONSTRAINT **/\n\n" +
				"ALTER TABLE `test 3`\n" +
				"\tDROP FOREIGN KEY `test 5`;\n\n" +
				"ALTER TABLE `test 3`\n" +
				"\tADD CONSTRAINT `test 5`\n" +
				"\t\tFOREIGN KEY (`test attr 1`)\n" +
				"\t\tREFERENCES `test 4` (`test attr 3`)\n" +
				"\t\tON UPDATE CASCADE ON DELETE CASCADE;\n\n" +
				"/*************************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryDefConstraint() throws Exception {
		assertEquals("FOREIGN KEY (`test attr 1`)\n" +
				"\t\tREFERENCES `test 4` (`test attr 3`)\n" +
				"\t\tON UPDATE CASCADE ON DELETE CASCADE", CONNECTION.getQueryDef(constraint));
	}

	@Test
	public void getQueryDropConstraint() throws Exception {
		String val = CONNECTION.getQueryDrop(constraint);
		assertEquals("ALTER TABLE `test 3`\n" +
				"\tDROP FOREIGN KEY `test 5`;", val);
		assertTrue(val.endsWith(";"));

		val = constraint.getQueryDrop(CONNECTION);
		assertEquals("ALTER TABLE `test 3`\n" +
				"\tDROP FOREIGN KEY `test 5`;", val);
		assertTrue(val.endsWith(";"));
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
		assertEquals("ALTER TABLE `test 3`\n" +
				"\tDROP FOREIGN KEY `test 5`;\n\n" +
				"ALTER TABLE `test 3`\n" +
				"\tADD CONSTRAINT `new name`\n" +
				"\t\tFOREIGN KEY (`test attr 1`)\n" +
				"\t\tREFERENCES `test 4` (`test attr 3`)\n" +
				"\t\tON UPDATE CASCADE ON DELETE CASCADE;", val);
		assertTrue(val.endsWith(";"));
		val = constraint.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE `test 3`\n" +
				"\tDROP FOREIGN KEY `new name`;\n\n" +
				"ALTER TABLE `test 3`\n" +
				"\tADD CONSTRAINT `test 5`\n" +
				"\t\tFOREIGN KEY (`test attr 1`)\n" +
				"\t\tREFERENCES `test 4` (`test attr 3`)\n" +
				"\t\tON UPDATE CASCADE ON DELETE CASCADE;", val);
		assertTrue(val.endsWith(";"));

		// Change DEFINITION
		valuesForEdit = constraint.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Constraint.Behavior.L_REM_ATTR, attribute4.getName());

		val = constraint.getQueryChanged(CONNECTION);
		assertEquals("ALTER TABLE `test 3`\n" +
				"\tDROP FOREIGN KEY `test 5`;\n\n" +
				"ALTER TABLE `test 3`\n" +
				"\tADD CONSTRAINT `test 5`\n" +
				"\t\tFOREIGN KEY (`test attr 1`)\n" +
				"\t\tREFERENCES `test 4` (`test attr 4`)\n" +
				"\t\tON UPDATE CASCADE ON DELETE CASCADE;", val);
		assertTrue(val.endsWith(";"));
		val = constraint.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE `test 3`\n" +
				"\tDROP FOREIGN KEY `test 5`;\n\n" +
				"ALTER TABLE `test 3`\n" +
				"\tADD CONSTRAINT `test 5`\n" +
				"\t\tFOREIGN KEY (`test attr 1`)\n" +
				"\t\tREFERENCES `test 4` (`test attr 3`)\n" +
				"\t\tON UPDATE CASCADE ON DELETE CASCADE;", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreateFunction() throws Exception {
		String val = CONNECTION.getQueryCreate(function, null);
		assertEquals("-- test function\n" +
				"CREATE FUNCTION `test function`(int input)\n" +
				"\tRETURNS varchar\n" +
				"BEGIN\n" +
				"\tRETURN CONCAT('Hello ', addressee);\n" +
				"END;\n\n" +
				"ALTER FUNCTION `test function` COMMENT 'test function';", val);
		assertTrue(val.endsWith(";"));

		val = function.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL FUNCTION **/\n\n" +
				"DROP FUNCTION IF EXISTS `test function`;\n\n" +
				"-- test function\n" +
				"CREATE FUNCTION `test function`(int input)\n" +
				"\tRETURNS varchar\n" +
				"BEGIN\n" +
				"\tRETURN CONCAT('Hello ', addressee);\n" +
				"END;\n\n" +
				"ALTER FUNCTION `test function` COMMENT 'test function';\n\n" +
				"/***********************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryDropFunction() throws Exception {
		String val = CONNECTION.getQueryDrop(function);
		assertEquals("DROP FUNCTION IF EXISTS `test function`;", val);
		assertTrue(val.endsWith(";"));

		val = function.getQueryDrop(CONNECTION);
		assertEquals("DROP FUNCTION IF EXISTS `test function`;", val);
		assertTrue(val.endsWith(";"));
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
		assertEquals("DROP FUNCTION IF EXISTS `test function`;\n\n" +
				"-- test function\n" +
				"CREATE FUNCTION `new name`(int input)\n" +
				"\tRETURNS varchar\n" +
				"BEGIN\n" +
				"\tRETURN CONCAT('Hello ', addressee);\n" +
				"END;\n\n" +
				"ALTER FUNCTION `new name` COMMENT 'test function';", val);
		assertTrue(val.endsWith(";"));
		val = function.getQueryChangeRevert(CONNECTION);
		assertEquals("DROP FUNCTION IF EXISTS `new name`;\n\n" +
				"-- test function\n" +
				"CREATE FUNCTION `test function`(int input)\n" +
				"\tRETURNS varchar\n" +
				"BEGIN\n" +
				"\tRETURN CONCAT('Hello ', addressee);\n" +
				"END;\n\n" +
				"ALTER FUNCTION `test function` COMMENT 'test function';", val);
		assertTrue(val.endsWith(";"));

		// Change IS UNIQUE, WHERE
		valuesForEdit = function.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Function.Behavior.L_ARGUMENTS, "new args");
		valuesForEdit.notifyChange(Function.Behavior.L_RETURNS, "");
		valuesForEdit.notifyChange(Function.Behavior.L_LANGUAGE, "new language");
		valuesForEdit.notifyChange(Function.Behavior.L_BODY, "BEGIN\n\tRETURN CONCAT('Well, hello ', addressee);\nEND;");

		val = function.getQueryChanged(CONNECTION);
		assertEquals("DROP FUNCTION IF EXISTS `test function`;\n\n" +
				"-- test function\n" +
				"CREATE PROCEDURE `test function`(new args)\n" +
				"BEGIN\n" +
				"\tRETURN CONCAT('Well, hello ', addressee);\n" +
				"END;\n\n" +
				"ALTER PROCEDURE `test function` COMMENT 'test function';", val);
		assertTrue(val.endsWith(";"));
		val = function.getQueryChangeRevert(CONNECTION);
		assertEquals("DROP PROCEDURE IF EXISTS `test function`;\n\n" +
				"-- test function\n" +
				"CREATE FUNCTION `test function`(int input)\n" +
				"\tRETURNS varchar\n" +
				"BEGIN\n" +
				"\tRETURN CONCAT('Hello ', addressee);\n" +
				"END;\n\n" +
				"ALTER FUNCTION `test function` COMMENT 'test function';", val);
		assertTrue(val.endsWith(";"));

		// Change DESCRIPTION
		valuesForEdit = function.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Function.Behavior.L_DESCR, "new descr");

		val = function.getQueryChanged(CONNECTION);
		assertEquals("ALTER FUNCTION `test function` COMMENT 'new descr';", val);
		assertTrue(val.endsWith(";"));
		val = function.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER FUNCTION `test function` COMMENT 'test function';", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryExecFunction() throws Exception {
		String val = CONNECTION.getQueryExecFunction(function);
		assertEquals("SELECT `test function`(int input);", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreateIndex() throws Exception {
		String val = CONNECTION.getQueryCreate(primaryKey, null);
		assertEquals("-- test PK\n" +
				"ALTER TABLE `test 3`\n" +
				"\tADD PRIMARY KEY (`test attr 1`);", val);
		assertTrue(val.endsWith(";"));

		val = primaryKey.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL PRIMARY KEY **/\n\n" +
				"ALTER TABLE `test 3`\n" +
				"\tDROP PRIMARY KEY;\n\n" +
				"-- test PK\n" +
				"ALTER TABLE `test 3`\n" +
				"\tADD PRIMARY KEY (`test attr 1`);\n\n" +
				"/**************************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));

		val = CONNECTION.getQueryCreate(index, null);
		assertEquals("-- test index\n" +
				"CREATE INDEX `test index`\n" +
				"\tON `test 4` (`test attr 4`)\n" +
				"\tCOMMENT 'test index';", val);
		assertTrue(val.endsWith(";"));

		val = index.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL INDEX **/\n\n" +
				"ALTER TABLE `test 4`\n" +
				"\tDROP INDEX `test index`;\n\n" +
				"-- test index\n" +
				"CREATE INDEX `test index`\n" +
				"\tON `test 4` (`test attr 4`)\n" +
				"\tCOMMENT 'test index';\n\n" +
				"/********************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));

		val = CONNECTION.getQueryCreate(uniqueIndex, null);
		assertEquals("-- test UX\n" +
				"CREATE UNIQUE INDEX `test UX`\n" +
				"\tON `test 3` (`test attr 2`)\n" +
				"\tCOMMENT 'test UX';", val);
		assertTrue(val.endsWith(";"));

		val = uniqueIndex.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL INDEX **/\n\n" +
				"ALTER TABLE `test 3`\n" +
				"\tDROP INDEX `test UX`;\n" +
				"\n-- test UX\n" +
				"CREATE UNIQUE INDEX `test UX`\n" +
				"\tON `test 3` (`test attr 2`)\n" +
				"\tCOMMENT 'test UX';\n\n" +
				"/********************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));

		val = CONNECTION.getQueryCreate(uniqueConstraint, null);
		assertEquals("-- test UC\n" +
				"CREATE UNIQUE INDEX `test UC`\n" +
				"\tON `test 4` (`test attr 3`)\n" +
				"\tCOMMENT 'test UC';", val);
		assertTrue(val.endsWith(";"));

		val = uniqueConstraint.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL INDEX **/\n\n" +
				"ALTER TABLE `test 4`\n" +
				"\tDROP INDEX `test UC`;\n\n" +
				"-- test UC\n" +
				"CREATE UNIQUE INDEX `test UC`\n" +
				"\tON `test 4` (`test attr 3`)\n" +
				"\tCOMMENT 'test UC';\n\n" +
				"/********************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryDropIndex() throws Exception {
		String val = CONNECTION.getQueryDrop(primaryKey);
		assertEquals("ALTER TABLE `test 3`\n\tDROP PRIMARY KEY;", val);
		assertTrue(val.endsWith(";"));

		val = primaryKey.getQueryDrop(CONNECTION);
		assertEquals("ALTER TABLE `test 3`\n\tDROP PRIMARY KEY;", val);
		assertTrue(val.endsWith(";"));

		val = CONNECTION.getQueryDrop(uniqueIndex);
		assertEquals("ALTER TABLE `test 3`\n\tDROP INDEX `test UX`;", val);
		assertTrue(val.endsWith(";"));

		val = uniqueIndex.getQueryDrop(CONNECTION);
		assertEquals("ALTER TABLE `test 3`\n\tDROP INDEX `test UX`;", val);
		assertTrue(val.endsWith(";"));

		val = CONNECTION.getQueryDrop(uniqueConstraint);
		assertEquals("ALTER TABLE `test 4`\n\tDROP INDEX `test UC`;", val);
		assertTrue(val.endsWith(";"));

		val = uniqueConstraint.getQueryDrop(CONNECTION);
		assertEquals("ALTER TABLE `test 4`\n\tDROP INDEX `test UC`;", val);
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
		assertEquals("ALTER TABLE `test 4`\n" +
				"\tDROP INDEX `test index`;\n\n" +
				"-- test index\n" +
				"CREATE INDEX `new name`\n" +
				"\tON `test 4` (`test attr 4`)\n" +
				"\tCOMMENT 'test index';", val);
		assertTrue(val.endsWith(";"));
		val = index.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE `test 4`\n" +
				"\tDROP INDEX `new name`;\n\n" +
				"-- test index\n" +
				"CREATE INDEX `test index`\n" +
				"\tON `test 4` (`test attr 4`)\n" +
				"\tCOMMENT 'test index';", val);
		assertTrue(val.endsWith(";"));

		// Change IS UNIQUE
		valuesForEdit = index.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Index.Behavior.L_UNIQUE, true);

		val = index.getQueryChanged(CONNECTION);
		assertEquals("ALTER TABLE `test 4`\n" +
				"\tDROP INDEX `test index`;\n\n" +
				"-- test index\n" +
				"CREATE UNIQUE INDEX `test index`\n" +
				"\tON `test 4` (`test attr 4`)\n" +
				"\tCOMMENT 'test index';", val);
		assertTrue(val.endsWith(";"));
		val = index.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE `test 4`\n" +
				"\tDROP INDEX `test index`;\n\n" +
				"-- test index\n" +
				"CREATE INDEX `test index`\n" +
				"\tON `test 4` (`test attr 4`)\n" +
				"\tCOMMENT 'test index';", val);
		assertTrue(val.endsWith(";"));

		// Change DESCRIPTION
		valuesForEdit = index.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Index.Behavior.L_DESCR, "new descr");

		val = index.getQueryChanged(CONNECTION);
		assertEquals("ALTER TABLE `test 4`\n" +
				"\tDROP INDEX `test index`;\n\n" +
				"-- new descr\n" +
				"CREATE INDEX `test index`\n" +
				"\tON `test 4` (`test attr 4`)\n" +
				"\tCOMMENT 'new descr';", val);
		assertTrue(val.endsWith(";"));
		val = index.getQueryChangeRevert(CONNECTION);
		assertEquals("ALTER TABLE `test 4`\n" +
				"\tDROP INDEX `test index`;\n" +
				"\n" +
				"-- test index\n" +
				"CREATE INDEX `test index`\n" +
				"\tON `test 4` (`test attr 4`)\n" +
				"\tCOMMENT 'test index';", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreateTrigger() throws Exception {
		String val = CONNECTION.getQueryCreate(trigger, null);
		assertEquals("CREATE TRIGGER `test trigger`\n" +
				"\tBEFORE UPDATE\n" +
				"\tON `test 4`\n" +
				"\tFOR EACH ROW\n" +
				"BEGIN\n" +
				"\tSET NEW.value = 5;\n" +
				"END;", val);
		assertTrue(val.endsWith(";"));

		val = trigger.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL TRIGGER **/\n\n" +
				"DROP TRIGGER `test trigger`;\n\n" +
				"CREATE TRIGGER `test trigger`\n" +
				"\tBEFORE UPDATE\n" +
				"\tON `test 4`\n" +
				"\tFOR EACH ROW\n" +
				"BEGIN\n" +
				"\tSET NEW.value = 5;\n" +
				"END;\n\n" +
				"/**********************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryDropTrigger() throws Exception {
		String val = CONNECTION.getQueryDrop(trigger);
		assertEquals("DROP TRIGGER `test trigger`;", val);
		assertTrue(val.endsWith(";"));

		val = trigger.getQueryDrop(CONNECTION);
		assertEquals("DROP TRIGGER `test trigger`;", val);
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
		assertEquals("DROP TRIGGER `test trigger`;\n\n" +
				"CREATE TRIGGER `new name`\n" +
				"\tBEFORE UPDATE\n" +
				"\tON `test 4`\n" +
				"\tFOR EACH ROW\n" +
				"BEGIN\n" +
				"\tSET NEW.value = 5;\n" +
				"END;", val);
		assertTrue(val.endsWith(";"));
		val = trigger.getQueryChangeRevert(CONNECTION);
		assertEquals("DROP TRIGGER `new name`;\n" +
				"\n" +
				"CREATE TRIGGER `test trigger`\n" +
				"\tBEFORE UPDATE\n" +
				"\tON `test 4`\n" +
				"\tFOR EACH ROW\n" +
				"BEGIN\n" +
				"\tSET NEW.value = 5;\n" +
				"END;", val);
		assertTrue(val.endsWith(";"));

		// Change CONDITION
		valuesForEdit = trigger.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(Trigger.Behavior.L_BODY, "BEGIN\n\tnew exec;\nEND");

		val = trigger.getQueryChanged(CONNECTION);
		assertEquals("DROP TRIGGER `test trigger`;\n\n" +
				"CREATE TRIGGER `test trigger`\n" +
				"\tBEFORE UPDATE\n" +
				"\tON `test 4`\n" +
				"\tFOR EACH ROW\n" +
				"BEGIN\n" +
				"\tnew exec;\n" +
				"END;", val);
		assertTrue(val.endsWith(";"));
		val = trigger.getQueryChangeRevert(CONNECTION);
		assertEquals("DROP TRIGGER `test trigger`;\n\n" +
				"CREATE TRIGGER `test trigger`\n" +
				"\tBEFORE UPDATE\n" +
				"\tON `test 4`\n" +
				"\tFOR EACH ROW\n" +
				"BEGIN\n" +
				"\tSET NEW.value = 5;\n" +
				"END;", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCreateView() throws Exception {
		String val = CONNECTION.getQueryCreate(view, null);
		assertEquals("CREATE OR REPLACE VIEW `test view`\n\tAS SELECT * FROM \"test 4\";", val);
		assertTrue(val.endsWith(";"));

		val = view.getQueryCreate(CONNECTION);
		assertEquals("/** ORIGINAL VIEW **/\n\n" +
				"DROP VIEW `test view`;\n\n" +
				"CREATE OR REPLACE VIEW `test view`\n\tAS SELECT * FROM \"test 4\";\n\n" +
				"/*******************/", val);
		assertTrue(val.startsWith("/**"));
		assertTrue(val.endsWith("**/"));
	}

	@Test
	public void getQueryDropView() throws Exception {
		String val = CONNECTION.getQueryDrop(view);
		assertEquals("DROP VIEW `test view`;", val);
		assertTrue(val.endsWith(";"));

		val = view.getQueryDrop(CONNECTION);
		assertEquals("DROP VIEW `test view`;", val);
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
		assertEquals("DROP VIEW `test view`;\n\n" +
				"CREATE OR REPLACE VIEW `new name`\n\tAS SELECT * FROM \"test 4\";", val);
		assertTrue(val.endsWith(";"));
		val = view.getQueryChangeRevert(CONNECTION);
		assertEquals("DROP VIEW `new name`;\n\n" +
				"CREATE OR REPLACE VIEW `test view`\n\tAS SELECT * FROM \"test 4\";", val);
		assertTrue(val.endsWith(";"));

		// Change BODY
		valuesForEdit = view.getBehavior().prepareForEdit();
		valuesForEdit.notifyChange(View.Behavior.L_BODY, "new body");

		val = view.getQueryChanged(CONNECTION);
		assertEquals("CREATE OR REPLACE VIEW `test view`\n\tAS new body;", val);
		assertTrue(val.endsWith(";"));
		val = view.getQueryChangeRevert(CONNECTION);
		assertEquals("CREATE OR REPLACE VIEW `test view`\n\tAS SELECT * FROM \"test 4\";", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryTerminate() throws Exception {
		String val = CONNECTION.getQueryTerminate(123);
		assertEquals("KILL CONNECTION 123;", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryCancel() throws Exception {
		String val = CONNECTION.getQueryCancel(123);
		assertEquals("KILL QUERY 123;", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void escape() throws Exception {
		assertEquals("test", CONNECTION.escape("test"));
		assertEquals("`test 1`", CONNECTION.escape("test 1"));
		assertEquals("`test.`", CONNECTION.escape("test."));
	}

	@Test
	public void escapeFullName() throws Exception {
		assertEquals("test", CONNECTION.escapeFullName("test"));
		assertEquals("`test 1`", CONNECTION.escapeFullName("test 1"));
		assertEquals("`test.`", CONNECTION.escapeFullName("test."));
		assertEquals("test.`asdf 1`", CONNECTION.escapeFullName("test.asdf 1"));
		assertEquals("`test 1`.`asdf 1`", CONNECTION.escapeFullName("test 1.asdf 1"));
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
		assertEquals("DELETE FROM `test 3` WHERE `test attr 1` = 'abc';", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryUpdate() throws Exception {
		Map<String, String> pkValues = new HashMap<>();
		pkValues.put(attribute3.getName(), "abc");

		String val = CONNECTION.getQueryUpdate(relation2, pkValues, attribute4.getName(), "this won't work I guess");
		assertEquals("UPDATE `test 4` SET `test attr 4` = 'this won''t work I guess' WHERE `test attr 3` = 'abc';", val);
		assertTrue(val.endsWith(";"));

		val = CONNECTION.getQueryUpdate(relation2, pkValues, attribute4.getName(), null);
		assertEquals("UPDATE `test 4` SET `test attr 4` = NULL WHERE `test attr 3` = 'abc';", val);
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
		assertEquals("UPDATE `test 4` SET `test attr 4` = NULL, `test attr 3` = 'this won''t work I guess' WHERE `test attr 3` = 'abc';", val);
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
		assertEquals("UPDATE `test 4` SET `test attr 4` = NULL, `test attr 3` = 'this won''t work I guess' WHERE `test attr 3` = 'abc';", val);
		assertTrue(val.endsWith(";"));

		val = CONNECTION.getQueryUpdate(relation2.getFullName(), null, colValues);
		assertEquals("UPDATE `test 4` SET `test attr 4` = NULL, `test attr 3` = 'this won''t work I guess' WHERE /** TODO: ADD CONDITIONS HERE **/;", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryInsert() throws Exception {
		Map<String, String> colValues = new HashMap<>();
		colValues.put(attribute3.getName(), "this won't work I guess");
		colValues.put(attribute4.getName(), null);

		String val = CONNECTION.getQueryInsert(relation2.getFullName(), colValues);
		assertEquals("INSERT INTO `test 4` (`test attr 4`, `test attr 3`) VALUES (NULL, 'this won''t work I guess');", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQueryVacuum() throws Exception {
		String val = CONNECTION.getQueryVacuum("OPTIMIZE TABLE", Arrays.asList(relation, relation2));
		assertEquals("OPTIMIZE TABLE `test 3`;\nOPTIMIZE TABLE `test 4`;", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQuerySelect() throws Exception {
		String val = CONNECTION.getQuerySelect(relation, null, null, 10);
		assertEquals("SELECT *\n" +
				"FROM `test 3`\n" +
				"LIMIT 10;", val);
		assertTrue(val.endsWith(";"));

		val = CONNECTION.getQuerySelect(relation, "id = 2", null, 10);
		assertEquals("SELECT *\n" +
				"FROM `test 3`\n" +
				"WHERE id = 2\n" +
				"LIMIT 10;", val);
		assertTrue(val.endsWith(";"));

		val = CONNECTION.getQuerySelect(relation, null, "id", 10);
		assertEquals("SELECT *\n" +
				"FROM `test 3`\n" +
				"ORDER BY id\n" +
				"LIMIT 10;", val);
		assertTrue(val.endsWith(";"));

		val = CONNECTION.getQuerySelect(relation, "id = 2", "id", 10);
		assertEquals("SELECT *\n" +
				"FROM `test 3`\n" +
				"WHERE id = 2\n" +
				"ORDER BY id\n" +
				"LIMIT 10;", val);
		assertTrue(val.endsWith(";"));
	}

	@Test
	public void getQuerySelectConstraint() throws Exception {
		String val = CONNECTION.getQuerySelect(constraint);
		assertEquals("SELECT *\n" +
				"FROM `test 3` t3\n" +
				"JOIN `test 4` t4 ON t3.`test attr 1` = t4.`test attr 3`\n" +
				"LIMIT 200;", val);
		assertTrue(val.endsWith(";"));
	}

}