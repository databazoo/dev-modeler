
package com.databazoo.devmodeler.tools.formatter;

import com.databazoo.devmodeler.gui.DBTree;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.tools.Geometry;

import java.util.Arrays;

/**
 * Formatter implementation working with SQL.
 *
 * @author bobus
 */
public class FormatterSQL extends FormatterBase {
	public FormatterSQL(){
		BEGIN_CLAUSES.add( "raise" );
		BEGIN_CLAUSES.add( "left" );
		BEGIN_CLAUSES.add( "right" );
		BEGIN_CLAUSES.add( "full" );
		BEGIN_CLAUSES.add( "inner" );
		BEGIN_CLAUSES.add( "outer" );
		BEGIN_CLAUSES.add( "cross" );
		BEGIN_CLAUSES.add( "group" );
		BEGIN_CLAUSES.add( "order" );
		BEGIN_CLAUSES.add( "partition" );
		BEGIN_CLAUSES.add("offset");
		BEGIN_CLAUSES.add("vacuum");

		BEGIN_CLAUSES.add("create");
		BEGIN_CLAUSES.add("replace");
		BEGIN_CLAUSES.add("user");
		BEGIN_CLAUSES.add("database");
		BEGIN_CLAUSES.add("schema");
		BEGIN_CLAUSES.add("table");
		BEGIN_CLAUSES.add("sequence");
		BEGIN_CLAUSES.add("column");
		BEGIN_CLAUSES.add("trigger");
		BEGIN_CLAUSES.add("function");
		BEGIN_CLAUSES.add("package");
		BEGIN_CLAUSES.add("body");
		BEGIN_CLAUSES.add("view");
		BEGIN_CLAUSES.add("constraint");
		BEGIN_CLAUSES.add("index");
		BEGIN_CLAUSES.add("primary");
		BEGIN_CLAUSES.add("foreign");
		BEGIN_CLAUSES.add("key");
		BEGIN_CLAUSES.add("references");
		BEGIN_CLAUSES.add("match");
		BEGIN_CLAUSES.add("simple");
		BEGIN_CLAUSES.add("comment");
		BEGIN_CLAUSES.add("alter");
		BEGIN_CLAUSES.add("add");
		BEGIN_CLAUSES.add("drop");
		BEGIN_CLAUSES.add("before");
		BEGIN_CLAUSES.add("after");
		BEGIN_CLAUSES.add("check");
		BEGIN_CLAUSES.add("unique");
		BEGIN_CLAUSES.add("default");
		BEGIN_CLAUSES.add("rename");
		BEGIN_CLAUSES.add("change");
		BEGIN_CLAUSES.add("modify");
		BEGIN_CLAUSES.add("collate");
		BEGIN_CLAUSES.add("exec");
		BEGIN_CLAUSES.add("grant");
		BEGIN_CLAUSES.add("revoke");
		BEGIN_CLAUSES.add("generated");
		BEGIN_CLAUSES.add("always");

		END_CLAUSES.add( "where" );
		END_CLAUSES.add( "set" );
		END_CLAUSES.add( "having" );
		END_CLAUSES.add( "join" );
		END_CLAUSES.add( "from" );
		END_CLAUSES.add( "to" );
		END_CLAUSES.add( "by" );
		END_CLAUSES.add( "join" );
		END_CLAUSES.add( "into" );
		END_CLAUSES.add( "union" );
		END_CLAUSES.add( "limit" );
		END_CLAUSES.add("desc");
		END_CLAUSES.add("asc");
		END_CLAUSES.add("except");
		END_CLAUSES.add("top");
		END_CLAUSES.add("for");
		END_CLAUSES.add("foreach");
		END_CLAUSES.add("loop");
		END_CLAUSES.add("using");
		END_CLAUSES.add("query");
		END_CLAUSES.add("start");
		END_CLAUSES.add("begin");
		END_CLAUSES.add("commit");
		END_CLAUSES.add("rollback");
		END_CLAUSES.add("declare");
		END_CLAUSES.add("increment");
		END_CLAUSES.add("return");
		END_CLAUSES.add("returns");
		END_CLAUSES.add("returning");
		END_CLAUSES.add("perform");
		END_CLAUSES.add("execute");
		END_CLAUSES.add("verbose");
		END_CLAUSES.add("analyze");
		END_CLAUSES.add("optimize");
		END_CLAUSES.add("repair");
		END_CLAUSES.add("cycle");
		END_CLAUSES.add("restart");
		END_CLAUSES.add("minvalue");
		END_CLAUSES.add("maxvalue");
		END_CLAUSES.add("nulls");
		END_CLAUSES.add("first");
		END_CLAUSES.add("last");

		END_CLAUSES.add("action");
		END_CLAUSES.add("restrict");
		END_CLAUSES.add("cascade");
		END_CLAUSES.add("storage");
		END_CLAUSES.add("plain");
		END_CLAUSES.add("main");
		END_CLAUSES.add("external");
		END_CLAUSES.add("extended");
		END_CLAUSES.add("security");
		END_CLAUSES.add("definer");
		END_CLAUSES.add("language");
		END_CLAUSES.add("volatile");
		END_CLAUSES.add("stable");
		END_CLAUSES.add("immutable");
		END_CLAUSES.add("cost");
		END_CLAUSES.add("rows");
		END_CLAUSES.add("each");
		END_CLAUSES.add("row");
		END_CLAUSES.add("statement");
		END_CLAUSES.add("procedure");
		END_CLAUSES.add("oids");
		END_CLAUSES.add("materialized");

		LOGICAL.add( "and" );
		LOGICAL.add( "or" );
		LOGICAL.add( "case" );
		LOGICAL.add( "when" );
		LOGICAL.add( "then" );
		LOGICAL.add( "else" );
		LOGICAL.add( "end" );
		LOGICAL.add( "if" );
		LOGICAL.add( "elsif" );
		LOGICAL.add( "of" );

		QUANTIFIERS.add( "between" );
		QUANTIFIERS.add( "values" );
		QUANTIFIERS.add( "window" );
		QUANTIFIERS.add( "like" );
		QUANTIFIERS.add( "ilike" );
		QUANTIFIERS.add( "in" );
		QUANTIFIERS.add( "all" );
		QUANTIFIERS.add( "exists" );
		QUANTIFIERS.add( "some" );
		QUANTIFIERS.add( "any" );

		DML.add( "select" );
		DML.add( "insert" );
		DML.add( "update" );
		DML.add( "delete" );
		DML.add( "truncate" );

		QUANTIFIERS.add( "on" );
		QUANTIFIERS.add( "as" );
		QUANTIFIERS.add( "is" );
		QUANTIFIERS.add( "with" );
		QUANTIFIERS.add( "without" );
		QUANTIFIERS.add( "null" );
		QUANTIFIERS.add( "not" );
		QUANTIFIERS.add( "no" );
		QUANTIFIERS.add( "only" );
		QUANTIFIERS.add( "distinct" );
		QUANTIFIERS.add( "recursive" );
		QUANTIFIERS.add( "notice" );
		QUANTIFIERS.add( "exception" );
		QUANTIFIERS.add( "sqlstate" );
		QUANTIFIERS.add( "fulltext" );
		QUANTIFIERS.add( "found" );
		QUANTIFIERS.add( "authorization" );
		QUANTIFIERS.add( "engine" );
		QUANTIFIERS.add( "role" );
		QUANTIFIERS.add( "identified" );
		QUANTIFIERS.add( "password" );

		ELEMENT_NAMES.addAll(Arrays.asList(DBTree.instance.getElementNames()));
		DATATYPE_NAMES.addAll(Arrays.asList(
			Geometry.concat(
				FormatterDataType.EXTRA, Geometry.concat(
				Project.getCurrent().getCurrentConn().getDataTypes().getKeys(),
				Project.getCurrent().getCurrentConn().getDataTypes().getVals()
			))));
	}

}
