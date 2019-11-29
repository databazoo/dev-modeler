
package com.databazoo.devmodeler.conn;

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
import com.databazoo.devmodeler.model.User;
import com.databazoo.devmodeler.model.View;
import com.databazoo.tools.Dbg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.databazoo.devmodeler.conn.ConnectionUtils.ADD_COLUMN;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ADD_CONSTRAINT;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ALTER_COLUMN;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ALTER_DATABASE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ALTER_INDEX;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ALTER_SCHEMA;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ALTER_SEQUENCE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ALTER_TABLE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ALTER_TRIGGER;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ALTER_USER;
import static com.databazoo.devmodeler.conn.ConnectionUtils.COMMA;
import static com.databazoo.devmodeler.conn.ConnectionUtils.COMMENT_ON_COLUMN;
import static com.databazoo.devmodeler.conn.ConnectionUtils.COMMENT_ON_CONSTRAINT;
import static com.databazoo.devmodeler.conn.ConnectionUtils.COMMENT_ON_DATABASE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.COMMENT_ON_FUNCTION;
import static com.databazoo.devmodeler.conn.ConnectionUtils.COMMENT_ON_INDEX;
import static com.databazoo.devmodeler.conn.ConnectionUtils.COMMENT_ON_SCHEMA;
import static com.databazoo.devmodeler.conn.ConnectionUtils.COMMENT_ON_SEQUENCE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.COMMENT_ON_TABLE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.COMMENT_ON_TRIGGER;
import static com.databazoo.devmodeler.conn.ConnectionUtils.COMMENT_ON_USER;
import static com.databazoo.devmodeler.conn.ConnectionUtils.COMMENT_ON_VIEW;
import static com.databazoo.devmodeler.conn.ConnectionUtils.CONSTRAINT;
import static com.databazoo.devmodeler.conn.ConnectionUtils.CREATE_DATABASE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.CREATE_MATERIALIZED_VIEW;
import static com.databazoo.devmodeler.conn.ConnectionUtils.CREATE_OR_REPLACE_FUNCTION;
import static com.databazoo.devmodeler.conn.ConnectionUtils.CREATE_OR_REPLACE_VIEW;
import static com.databazoo.devmodeler.conn.ConnectionUtils.CREATE_SCHEMA;
import static com.databazoo.devmodeler.conn.ConnectionUtils.CREATE_SEQUENCE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.CREATE_TABLE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.CREATE_TRIGGER;
import static com.databazoo.devmodeler.conn.ConnectionUtils.CREATE_USER;
import static com.databazoo.devmodeler.conn.ConnectionUtils.CYCLE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DISABLE_TRIGGER;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_COLUMN;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_CONSTRAINT;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_DATABASE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_FUNCTION_IF_EXISTS;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_INDEX;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_MATERIALIZED_VIEW;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_SCHEMA;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_SEQUENCE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_TABLE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_TRIGGER;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_USER;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_VIEW;
import static com.databazoo.devmodeler.conn.ConnectionUtils.EXECUTE_PROCEDURE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.INCREMENT_BY;
import static com.databazoo.devmodeler.conn.ConnectionUtils.IS;
import static com.databazoo.devmodeler.conn.ConnectionUtils.MAXVALUE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.MINVALUE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ON;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ORIGINAL_ATTRIBUTE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ORIGINAL_CONSTRAINT;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ORIGINAL_DATABASE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ORIGINAL_FUNCTION;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ORIGINAL_SCHEMA;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ORIGINAL_SEQUENCE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ORIGINAL_TABLE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ORIGINAL_TRIGGER;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ORIGINAL_USER;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ORIGINAL_VIEW;
import static com.databazoo.devmodeler.conn.ConnectionUtils.RENAME_TO;
import static com.databazoo.devmodeler.conn.ConnectionUtils.RESTART_WITH;
import static com.databazoo.devmodeler.conn.ConnectionUtils.SET_SCHEMA;
import static com.databazoo.devmodeler.conn.ConnectionUtils.START_WITH;

/**
 * Forward engineering setup for PostgreSQL
 *
 * @author bobus
 */
abstract class ConnectionPgForward extends Connection {

	static final String PUBLIC_SCHEMA = "public";

	ConnectionPgForward(String name, String host, String user, String pass) {
		super(name, host, user, pass);
	}

	@Override
	public String getCleanDef(Index ind) {
		if (ind.getBehavior().isPrimary()) {
			return ind.getBehavior().getDef().replaceAll(".*PRIMARY KEY\\s+\\(", "").replaceAll("\\)$", "");
		} else if (ind.getBehavior().isConstraint()) {
			return ind.getBehavior().getDef().replaceAll(".*UNIQUE\\s+\\(", "").replaceAll("\\)$", "");
		} else {
			String ret = ind.getBehavior().getDef();
			if (ind.getBehavior().getAccessMethod() != null) {
				ret = ret.replaceAll(".*USING\\s+" + ind.getBehavior().getAccessMethod() + "\\s+\\(", "");
			} else {
				ret = ret.replaceAll("^\\s+\\(", "");
			}
			if (ind.getBehavior().getWhere() != null) {
				ret = ret.replaceAll("\\)(\\s+WHERE\\s+" + Pattern.quote(ind.getBehavior().getWhere()) + ")?$", "");
			} else {
				ret = ret.replaceAll("\\)$", "");
			}
			return ret;
		}
	}

	@Override
	public String getCleanDef(Constraint con) {
		return con.getBehavior().getDef().replaceAll(".*CHECK\\s+\\(", "").replaceAll("\\)$", "");
	}

	@Override
	public String getQueryLocks() {
		return "SELECT count(*), (SELECT count(*) FROM pg_lock_status() WHERE database IS NOT NULL) FROM pg_stat_get_activity(NULL);";
	}

	@Override
	public String getQueryLimitOffset(int limit, int offset) {
		if (limit > 0) {
			return (offset > 0 ? "OFFSET " + offset + " " : "") + "LIMIT " + limit;
		} else {
			return "";
		}
	}

	@Override
	public String getQueryExplain(String sql) {
		return "EXPLAIN " + sql;
	}

	//////////
	// USER //
	//////////
	@Override
	public String getQueryCreate(User user, SQLOutputConfig config) {
		if (config == null) {
			config = SQLOutputConfig.DEFAULT;
		}

        SQLBuilder ret = new SQLBuilder(config);
        if(config.exportOriginal){
            ret.a(ORIGINAL_USER).nl();
        }
        if(config.exportDrop){
            ret.a(config.getDropComment(getQueryDrop(user))).nl();
        }

        ret.a(CREATE_USER).a(escape(user.getName()))
				.a(" PASSWORD ").quotedEscaped(user.getBehavior().getPassword())
				.semicolon();

		if (config.exportOriginal) {
			ret.nl().commentLine(ORIGINAL_USER);
		}
		return ret.toString();
	}

	@Override
	public String getQueryDrop(User user) {
		return DROP_USER + escape(user.getName()) + ";";
	}

	@Override
	public String getQueryChanged(User user) {
		String fullNameEscaped = escapeFullName(user.getName());
		User.Behavior o = user.getBehavior();
		User.Behavior n = user.getBehavior().getValuesForEdit();
		if (n.isDropped()) {
			return getQueryDrop(user);
		} else {
			SQLBuilder ret = new SQLBuilder();
			if (!o.getName().equals(n.getName())) {
				ret.a(ALTER_USER).a(fullNameEscaped).a(RENAME_TO).a(escape(n.getName())).semicolon();
			}
			if (!o.getPassword().equals(n.getPassword())) {
				ret.nlIfNotEmpty().a(ALTER_USER).a(escape(n.getName())).a(" PASSWORD ").quotedEscaped(n.getPassword()).semicolon();
			}
			if (!n.getDescr().equals(o.getDescr())) {
				ret.nlIfNotEmpty().a(COMMENT_ON_USER).a(escape(n.getName())).a(IS).quotedEscapedOrNull(n.getDescr()).semicolon();
			}
			return ret.toString();
		}
	}

	//////////////
	// DATABASE //
	//////////////
	@Override
	public String getQueryCreate(DB db, SQLOutputConfig config) {
		if (config == null) {
			config = SQLOutputConfig.DEFAULT;
		}

        SQLBuilder ret = new SQLBuilder(config);
        if(config.exportOriginal){
            ret.a(ORIGINAL_DATABASE).nl();
        }
        if(config.exportDrop){
            ret.a(config.getDropComment(getQueryDrop(db))).nl();
        }
        if(config.exportComments && !db.getDescr().isEmpty()){
			appendDescription(ret, db);
        }

        ret.a(CREATE_DATABASE).a(escape(db.getName())).semicolon();

		if (!db.getDescr().isEmpty()) {
            ret.nl().a(COMMENT_ON_DATABASE).a(escapeFullName(db.getFullName())).a(IS).quotedEscapedOrNull(db.getDescr()).semicolon();
		}
		if (config.exportOriginal) {
			ret.nl().commentLine(ORIGINAL_DATABASE);
		}
		return ret.toString();
	}

	@Override
	public String getQueryDrop(DB db) {
		return DROP_DATABASE + escape(db.getName()) + ";";
	}

	@Override
	public String getQueryChanged(DB db) {
		String fullNameEscaped = escapeFullName(db.getFullName());
		DB.Behavior o = db.getBehavior();
		DB.Behavior n = db.getBehavior().getValuesForEdit();
		if (n.isDropped()) {
			return getQueryDrop(db);
		} else {
			SQLBuilder ret = new SQLBuilder();
			if (!o.getName().equals(n.getName())) {
				ret.a(ALTER_DATABASE).a(fullNameEscaped).a(RENAME_TO).a(escape(n.getName())).semicolon();
			}
			if (!n.getDescr().equals(o.getDescr())) {
				ret.nlIfNotEmpty().a(COMMENT_ON_DATABASE).a(fullNameEscaped).a(IS).quotedEscapedOrNull(n.getDescr()).semicolon();
			}
			return ret.toString();
		}
	}

	////////////
	// SCHEMA //
	////////////
	@Override
	public String getQueryCreate(Schema schema, SQLOutputConfig config) {
		if (config == null) {
			config = SQLOutputConfig.DEFAULT;
		}

        SQLBuilder ret = new SQLBuilder(config);
        if(config.exportOriginal){
            ret.a(ORIGINAL_SCHEMA).nl();
        }
        if(config.exportDrop){
            ret.a(config.getDropComment(getQueryDrop(schema))).nl();
        }
		if (config.exportComments && !schema.getDescr().isEmpty()) {
			appendDescription(ret, schema);
		}

        ret.a(CREATE_SCHEMA).a(escapeFullName(schema.getFullName())).semicolon();

		if (!schema.getDescr().isEmpty()) {
			ret.nl().a(COMMENT_ON_SCHEMA).a(escapeFullName(schema.getFullName())).a(IS).quotedEscaped(schema.getDescr().replaceAll("'", "''")).semicolon();
		}
        if(config.exportOriginal){
            ret.nl().commentLine(ORIGINAL_SCHEMA);
        }
        return ret.toString();
	}

	@Override
	public String getQueryDrop(Schema schema) {
		return DROP_SCHEMA + escapeFullName(schema.getFullName()) + ";";
	}

	@Override
	public String getQueryChanged(Schema schema) {
		String schemaFullNameEscaped = escapeFullName(schema.getFullName());
		Schema.Behavior o = schema.getBehavior();
		Schema.Behavior n = schema.getBehavior().getValuesForEdit();
		if (n.isDropped()) {
			return getQueryDrop(schema);
		} else {
            SQLBuilder ret = new SQLBuilder();
			if (!o.getName().equals(n.getName())) {
				ret.a(ALTER_SCHEMA).a(schemaFullNameEscaped).a(RENAME_TO).a(escape(n.getName())).semicolon();
			}
			if (!n.getDescr().equals(o.getDescr())) {
				ret.nlIfNotEmpty().a(COMMENT_ON_SCHEMA).a(schemaFullNameEscaped).a(IS).quotedEscapedOrNull(n.getDescr()).semicolon();
			}
            return ret.toString();
		}
	}

	//////////////
	// RELATION //
	//////////////
	@Override
	public String getQueryCreate(Relation rel, SQLOutputConfig config) {
		if (config == null) {
			config = SQLOutputConfig.DEFAULT;
		}

        SQLBuilder ret = new SQLBuilder(config);
        if(config.exportOriginal){
            ret.a(ORIGINAL_TABLE).nl();
        }
        if(config.exportDrop){
            ret.a(config.getDropComment(getQueryDrop(rel))).nl();
        }
        if(config.exportComments && !rel.getDescr().isEmpty()){
			appendDescription(ret, rel);
        }

		SQLBuilder attrComments = new SQLBuilder(config);
		String prevAttrComment = "";
		String relFullNameEscaped = escapeFullName(rel.getFullName());
		String elemNameEscaped;
		String comma = "";

        ret.a(CREATE_TABLE).a(relFullNameEscaped).a(" (");
		ArrayList<String> inheritedAttributes = new ArrayList<>();
		if (rel.getBehavior().getInheritParentName() != null && !rel.getBehavior().getInheritParentName().isEmpty()) {
			Relation parent = rel.getDB().getRelationByFullName(rel.getBehavior().getInheritParentName());
			if (parent != null) {
				inheritedAttributes.addAll(rel.getAttributes().stream()
						.filter(attr -> parent.getAttributeByName(attr.getName()) != null)
						.map(Attribute::getName)
						.collect(Collectors.toList()));
			}
		}
		for (Attribute attr : rel.getAttributes()) {
			elemNameEscaped = escape(attr.getName());
			ret.a(comma).a(prevAttrComment).nt().a(inheritedAttributes.contains(attr.getName()) ? "-- INHERITED: " : "").a(elemNameEscaped).space().a(getQueryDef(attr, config));
			if (!attr.getDescr().isEmpty()) {
				if (config.exportComments) {
					prevAttrComment = getPrevComment(attr);
				}
				attrComments.nl().a(COMMENT_ON_COLUMN).a(relFullNameEscaped).a(".").a(elemNameEscaped)
						.a(IS).quotedEscaped(attr.getDescr()).semicolon();
			} else {
				prevAttrComment = "";
			}
			comma = COMMA;
		}
		for (Index ind : rel.getIndexes()) {
			elemNameEscaped = escape(ind.getName());
			if (ind.getBehavior().isConstraint()) {
				ret.a(comma).a(prevAttrComment).nt().a(CONSTRAINT).a(elemNameEscaped).ntt().a(getQueryDef(ind));
				if (!ind.getDescr().isEmpty()) {
					if (config.exportComments) {
						prevAttrComment = getPrevComment(ind);
					}
					attrComments.nl().a(COMMENT_ON_CONSTRAINT).a(elemNameEscaped).a(ON).a(relFullNameEscaped)
							.a(IS).quotedEscaped(ind.getDescr()).semicolon();
				} else {
					prevAttrComment = "";
				}
				comma = COMMA;
			}
		}
		if (!config.exportSkipTriggersConstraints) {
			for (Constraint con : rel.getConstraints()) {
				if (con.getRel1().getFullName().equals(rel.getFullName())) {
					elemNameEscaped = escape(con.getName());
					ret.a(comma).a(prevAttrComment).nt().a(CONSTRAINT).a(elemNameEscaped).space().a(con.getRel2() == null ? ((CheckConstraint) con).getDef() : "\n\t\t" + getQueryDef(con));
					if (!con.getDescr().isEmpty()) {
						if (config.exportComments) {
							prevAttrComment = getPrevComment(con);
						}
						attrComments.nl().a(COMMENT_ON_CONSTRAINT).a(elemNameEscaped).a(ON).a(relFullNameEscaped)
								.a(IS).quotedEscaped(con.getDescr()).semicolon();
					} else {
						prevAttrComment = "";
					}
					comma = COMMA;
				}
			}
		}
		String inherits = " ";
		if (rel.getBehavior().getInheritParentName() != null && !rel.getBehavior().getInheritParentName().isEmpty()) {
			inherits = "\nINHERITS (" + rel.getBehavior().getInheritParentName() + ")\n";
		}
		ret.a(prevAttrComment).a(comma.equals(COMMA) ? "\n" : "").a(")").a(inherits);

		if (rel.getBehavior().getOptions().length == 0) {
			if (rel.getBehavior().hasOIDs()) {
				ret.a("WITH OIDS");
			}
		} else {
			ret.a("WITH (\n\toids=").a(rel.getBehavior().hasOIDs() ? "true" : "false");
			for (String opt : rel.getBehavior().getOptions()) {
				if (!opt.isEmpty()) {
					ret.a(COMMA).nt().a(opt);
				}
			}
			ret.n().a(")");
		}
		ret.semicolon();

		if (!rel.getDescr().isEmpty()) {
			ret.nl().a(COMMENT_ON_TABLE).a(relFullNameEscaped).a(IS).quotedEscaped(rel.getDescr()).semicolon();
		}
		ret.a(attrComments.toString());

		if(config.exportData){
			try {
				getQueryData(rel, config, ret);
			} catch (DBCommException ex) {
				Dbg.fixme("COULD NOT LOAD DATA FOR TABLE", ex);
				ret.nl().a("/** COULD NOT LOAD DATA FOR TABLE **/");
			}
		}

		if (!config.exportSkipTriggersConstraints) {
			rel.getIndexes().stream()
					.filter(ind -> !ind.getBehavior().isConstraint())
					.forEachOrdered(ind -> ret.nl().a(getQueryCreate(ind, null)));
			for (Trigger trig : rel.getTriggers()) {
				ret.nl().a(getQueryCreate(trig, null));
			}
		}
		if (config.exportOriginal) {
			ret.nl().commentLine(ORIGINAL_TABLE);
		}
		return ret.toString();
	}

	@Override
	public String getQueryDrop(Relation rel) {
		return DROP_TABLE + escapeFullName(rel.getFullName()) + ";";
	}

	@Override
	public String getQueryChanged(Relation rel) {
		SQLBuilder ret = new SQLBuilder();
		String relFullNameEscaped = escapeFullName(rel.getFullName());
		Relation.Behavior o = rel.getBehavior();
		Relation.Behavior n = rel.getBehavior().getValuesForEdit();
		if (n.isDropped()) {
			ret.a(getQueryDrop(rel));
		} else {
			String oInherit = o.getInheritParentName() == null ? "" : o.getInheritParentName();
			String nInherit = n.getInheritParentName() == null ? "" : n.getInheritParentName();
			if (!oInherit.equals(nInherit)) {
				if (!oInherit.isEmpty()) {
					ret.nlIfNotEmpty().a(ALTER_TABLE).a(relFullNameEscaped).a(" NO INHERIT ").a(escapeFullName(oInherit)).semicolon();
				}
				if (!nInherit.isEmpty()) {
					ret.nlIfNotEmpty().a(ALTER_TABLE).a(relFullNameEscaped).a(" INHERIT ").a(escapeFullName(nInherit)).semicolon();
				}
			}
			if (!Arrays.toString(o.getOptions()).equals(Arrays.toString(n.getOptions()))) {
				ret.nlIfNotEmpty().a(ALTER_TABLE).a(relFullNameEscaped).a(" SET (OIDs=").a(n.hasOIDs() ? "TRUE" : "FALSE");
				if(n.getOptions().length > 0) {
					for (String opt : n.getOptions()) {
						if (!opt.isEmpty()) {
							ret.a(",\n\t").a(opt);
						}
					}
					ret.n();
				}
				ret.a(")").semicolon();
			} else if (o.hasOIDs() != n.hasOIDs()) {
				ret.nlIfNotEmpty().a(ALTER_TABLE).a(relFullNameEscaped).a(" SET WITH").a(n.hasOIDs() ? "" : "OUT").a(" OIDS").semicolon();
			}
			if (!n.getDescr().equals(o.getDescr())) {
				ret.nlIfNotEmpty().a(COMMENT_ON_TABLE).a(relFullNameEscaped).a(IS).quotedEscapedOrNull(n.getDescr()).semicolon();
			}
			if (!o.getName().equals(n.getName())) {
				ret.nlIfNotEmpty().a(ALTER_TABLE).a(relFullNameEscaped).a(RENAME_TO).a(escape(n.getName())).semicolon();
			}
			if (!o.getSchemaName().equals(n.getSchemaName())) {
				ret.nlIfNotEmpty().a(ALTER_TABLE).a(escapeFullName((o.getSchemaName().isEmpty() ? "" : o.getSchemaName() + ".") + n.getName()))
						.a(SET_SCHEMA).a(escape(n.getSchemaName().isEmpty() ? PUBLIC_SCHEMA : n.getSchemaName())).semicolon();
			}
		}
		return ret.toString();
	}

	///////////////
	// ATTRIBUTE //
	///////////////
	@Override
	public String getQueryCreate(Attribute attr, SQLOutputConfig config) {
		if (config == null) {
			config = SQLOutputConfig.DEFAULT;
		}

        SQLBuilder ret = new SQLBuilder(config);
		if (config.exportOriginal) {
			ret.a(ORIGINAL_ATTRIBUTE).nl();
		}
		if (config.exportDrop) {
			ret.a(config.getDropComment(getQueryDrop(attr))).nl();
		}
		if (config.exportComments && !attr.getDescr().isEmpty()) {
			appendDescription(ret, attr);
		}

		String relFullNameEscaped = escapeFullName(attr.getRel().getFullName());
		String attrNameEscaped = escape(attr.getName());

		ret.a(ALTER_TABLE).a(relFullNameEscaped).nt().a(ADD_COLUMN).a(attrNameEscaped).space().a(getQueryDef(attr, config)).semicolon();

		if (!Objects.equals(attr.getBehavior().getStorage(), Attribute.Behavior.L_STORAGE_AUTO)) {
			ret.nl().a(ALTER_TABLE).a(relFullNameEscaped).nt().a(ALTER_COLUMN).a(attrNameEscaped).a(" SET STORAGE ").a(attr.getBehavior().getStorage()).semicolon();
		}
		if (!attr.getDescr().isEmpty()) {
			ret.nl().a(COMMENT_ON_COLUMN).a(relFullNameEscaped).a(".").a(attrNameEscaped).a(IS).quotedEscaped(attr.getDescr()).semicolon();
		}
		if (config.exportOriginal) {
			ret.nl().commentLine(ORIGINAL_ATTRIBUTE);
		}
		return ret.toString();
	}

	@Override
	public String getQueryDef(Attribute attr, SQLOutputConfig config) {
		String typeStr = getDataTypes().toFullType(attr.getFullType());

		// Type comes from a different DB
		if (config instanceof SQLOutputConfigExport) {
			if (((SQLOutputConfigExport) config).conn != null) {
				typeStr = getTranslatedType(typeStr);
			}
		}

		return typeStr +
				(attr.getBehavior().isAttNull() ? "" : " NOT NULL") +
				(attr.getBehavior().getDefaultValue() != null && !attr.getBehavior().getDefaultValue().isEmpty() ? " DEFAULT " + attr.getBehavior().getDefaultValue() : "");
	}

	@Override
	public String getQueryDrop(Attribute attr) {
		return ALTER_TABLE + escapeFullName(attr.getRel().getFullName()) + "\n\t" + DROP_COLUMN + escape(attr.getName()) + ";";
	}

	@Override
	public String getQueryChanged(Attribute attr) {
		SQLBuilder ret = new SQLBuilder();
		String relFullNameEscaped = escapeFullName(attr.getRel().getFullName());
		String attrNameEscaped = escape(attr.getName());
		Attribute.Behavior o = attr.getBehavior();
		Attribute.Behavior n = attr.getBehavior().getValuesForEdit();
		if (n.isDropped()) {
			ret.a(getQueryDrop(attr));
		} else {
			if (!attr.getFullType(o).equals(attr.getFullType(n))) {
				ret.a(ALTER_TABLE).a(relFullNameEscaped).space().a(ALTER_COLUMN).a(attrNameEscaped).a(" SET DATA TYPE ").a(getDataTypes().toFullType
						(attr.getFullType(n))).semicolon();
			}
			if (o.isAttNull() != n.isAttNull()) {
				ret.nlIfNotEmpty().a(ALTER_TABLE).a(relFullNameEscaped).space().a(ALTER_COLUMN).a(attrNameEscaped).space().a(n.isAttNull() ? "DROP" : "SET").a(" NOT NULL").semicolon();
			}
			if (!o.getDefaultValue().equals(n.getDefaultValue())) {
				ret.nlIfNotEmpty().a(ALTER_TABLE).a(relFullNameEscaped).space().a(ALTER_COLUMN).a(attrNameEscaped).space();
				if (n.getDefaultValue().isEmpty()) {
					ret.a("DROP DEFAULT").semicolon();
				} else {
					ret.a("SET DEFAULT ").a(n.getDefaultValue()).semicolon();
				}
			}
			if (!Objects.equals(o.getStorage(), n.getStorage()) && !Objects.equals(n.getStorage(), Attribute.Behavior.L_STORAGE_AUTO)) {
				ret.nlIfNotEmpty().a(ALTER_TABLE).a(relFullNameEscaped).space().a(ALTER_COLUMN).a(attrNameEscaped).a(" SET STORAGE ").a(n.getStorage()).semicolon();
			}
			if (!o.getName().equals(n.getName())) {
				ret.nlIfNotEmpty().a(ALTER_TABLE).a(relFullNameEscaped).a(" RENAME ").a(escape(o.getName())).a(" TO ").a(escape(n.getName())).semicolon();
			}
			if (!n.getDescr().equals(o.getDescr())) {
				ret.nlIfNotEmpty().a(COMMENT_ON_COLUMN).a(relFullNameEscaped).a(".").a(attrNameEscaped).a(IS).quotedEscapedOrNull(n.getDescr()).semicolon();
			}
		}
		return ret.toString();
	}

	//////////////////////
	// CHECK CONSTRAINT //
	//////////////////////
	@Override
	public String getQueryCreateWithoutComment(CheckConstraint con, SQLOutputConfig config) {
		return getQueryCreateWithoutComment((Constraint) con, config);
	}

	@Override
	public String getQueryCreate(CheckConstraint con, SQLOutputConfig config) {
		return getQueryCreate((Constraint) con, config);
	}

	@Override
	public String getQueryChanged(CheckConstraint con) {
		SQLBuilder ret = new SQLBuilder();
		Constraint.Behavior o = con.getBehavior();
		Constraint.Behavior n = con.getBehavior().getValuesForEdit();
		if (n.isDropped()) {
			ret.a(getQueryDrop(con));
		} else {
			String oSQL = getQueryCreateWithoutComment(con, null);
			con.setBehavior(n);
			String nSQL = getQueryCreateWithoutComment(con, null);
			con.setBehavior(o);
			if (!oSQL.equals(nSQL)) {
				ret.a(getQueryDrop(con)).nl().a(nSQL);
			}
			if (!n.getDescr().equals(o.getDescr()) || !oSQL.equals(nSQL)) {
				ret.nlIfNotEmpty().a(COMMENT_ON_CONSTRAINT).a(escape(n.getName()))
						.a(ON).a(escapeFullName(con.getRel1().getFullName()))
						.a(IS).quotedEscapedOrNull(n.getDescr()).semicolon();
			}
		}
		return ret.toString();
	}

	////////////////
	// CONSTRAINT //
	////////////////
	private String getQueryCreateConstraintBase(String relName, String conName, String def) {
		return ALTER_TABLE + relName + "\n\t" + ADD_CONSTRAINT + conName + "\n\t\t" + def + ";";
	}

	@Override
	public String getQueryCreateWithoutComment(Constraint con, SQLOutputConfig config) {
		if (config == null) {
			config = SQLOutputConfig.DEFAULT;
		}

		SQLBuilder ret = new SQLBuilder();
		if (config.exportOriginal) {
			ret.a(ORIGINAL_CONSTRAINT).nl();
		}
		if (config.exportDrop) {
			ret.a(config.getDropComment(getQueryDrop(con))).nl();
		}

		ret.a(getQueryCreateConstraintBase(escapeFullName(con.getRel1().getFullName()), escape(con.getName()), getQueryDef(con)));

		if (config.exportOriginal) {
			ret.nl().commentLine(ORIGINAL_CONSTRAINT);
		}
		return ret.toString();
	}

	@Override
	public String getQueryCreate(Constraint con, SQLOutputConfig config) {
		if (config == null) {
			config = SQLOutputConfig.DEFAULT;
		}

		SQLBuilder ret = new SQLBuilder(config);
		if (config.exportOriginal) {
			ret.a(ORIGINAL_CONSTRAINT).nl();
		}
		if (config.exportDrop) {
			ret.a(config.getDropComment(getQueryDrop(con))).nl();
		}
		if (config.exportComments && !con.getDescr().isEmpty()) {
			appendDescription(ret, con);
		}

		if (!(con instanceof CheckConstraint)) {
			String createColumn = "";
			if (con.getBehavior().isCreateNewAttribute()) {
				String relFullNameEscaped = escapeFullName(con.getRel1().getFullName());
				String attrNameEscaped = escape(con.getBehavior().getNewColName());
				createColumn = ALTER_TABLE + relFullNameEscaped +
						"\n\t" + ADD_COLUMN + attrNameEscaped + " " + con.getBehavior().getNewColType() +
						(con.getBehavior().isReferenceNullable() ? "" : " NOT") + " NULL;" + config.getNL();

			} else if (con.getBehavior().isReferenceNullable() != con.getBehavior().getAttr1().getBehavior().isAttNull()) {
				con.getBehavior().getAttr1().getBehavior().prepareForEdit().notifyChange(Attribute.Behavior.L_NULLABLE, con.getBehavior().isReferenceNullable());
				createColumn = getQueryChanged(con.getBehavior().getAttr1()) + config.getNL();
			}
			ret.a(createColumn.replaceAll("^\\s+", ""));
		}

		ret.a(getQueryCreateConstraintBase(escapeFullName(con.getRel1().getFullName()), escape(con.getName()), getQueryDef(con)));

		if (!con.getDescr().isEmpty()) {
			ret.nl().a(COMMENT_ON_CONSTRAINT).a(escape(con.getName())).a(ON).a(escapeFullName(con.getRel1().getFullName())).a(IS).quotedEscaped(con.getDescr()).semicolon();
		}
		if (config.exportOriginal) {
			ret.nl().commentLine(ORIGINAL_CONSTRAINT);
		}
		return ret.toString();
	}

	@Override
	public String getQueryDef(Constraint con) {
		if (con instanceof CheckConstraint) {
			return ((CheckConstraint) con).getDef();
		} else {
			String attr1Name = con.getBehavior().isCreateNewAttribute() ? con.getBehavior().getNewColName() : con.getAttr1().getName();
			return "FOREIGN KEY (" + escape(attr1Name) + ")\n\t\tREFERENCES " + escapeFullName(con.getRel2().getFullName()) + " (" + escape(con.getAttr2().getName()) + ") MATCH SIMPLE\n\t\tON UPDATE " + con.getBehavior().getOnUpdate() + " ON DELETE " + con.getBehavior().getOnDelete();
		}
	}

	@Override
	public String getQueryDrop(Constraint con) {
		SQLBuilder createColumn = new SQLBuilder();
		if (con.getBehavior().isCreateNewAttribute()) {
			String relFullNameEscaped = escapeFullName(con.getRel1().getFullName());
			String attrNameEscaped = escape(con.getBehavior().getNewColName());
			createColumn.nl().a(ALTER_TABLE).a(relFullNameEscaped).nt().a(DROP_COLUMN).a(attrNameEscaped).semicolon();

		} else if (con.getBehavior().getAttr1() != null && con.getBehavior().isReferenceNullable() != con.getBehavior().getAttr1().getBehavior().isAttNull()) {
			con.getBehavior().getAttr1().getBehavior().prepareForEdit();
			con.getBehavior().getAttr1().getBehavior().setAttNull(con.getBehavior().isReferenceNullable());
			createColumn.nl().a(getQueryChanged(con.getBehavior().getAttr1()).replaceAll("^\\s+", ""));
			con.getBehavior().getAttr1().getBehavior().setAttNull(!con.getBehavior().isReferenceNullable());
		}
		return getQueryDropConstraint(escapeFullName(con.getRel1().getFullName()), escape(con.getName())) + createColumn.toString();
	}

	private String getQueryDropConstraint(String relName, String conName) {
		return ALTER_TABLE + relName + "\n\t" + DROP_CONSTRAINT + conName + ";";
	}

	@Override
	public String getQueryChanged(Constraint con) {
		SQLBuilder ret = new SQLBuilder();
		Constraint.Behavior o = con.getBehavior();
		Constraint.Behavior n = con.getBehavior().getValuesForEdit();
		if (n.isDropped()) {
			ret.a(getQueryDrop(con));
		} else {
			String oSQL = getQueryCreateWithoutComment(con, null);
			con.setBehavior(n);
			String nSQL = getQueryCreateWithoutComment(con, null);
			con.setBehavior(o);
			if (!oSQL.equals(nSQL)) {
				ret.a(getQueryDrop(con)).nl().a(nSQL);
			}
			if (!n.getDescr().equals(o.getDescr()) || !oSQL.equals(nSQL)) {
				ret.nlIfNotEmpty().a(COMMENT_ON_CONSTRAINT).a(escape(n.getName()))
						.a(ON).a(escapeFullName(con.getRel1().getFullName()))
						.a(IS).quotedEscapedOrNull(n.getDescr()).semicolon();
			}
		}
		return ret.toString();
	}

	///////////
	// INDEX //
	///////////
	@Override
	public String getQueryCreate(Index ind, SQLOutputConfig config) {
		if (config == null) {
			config = SQLOutputConfig.DEFAULT;
		}
        SQLBuilder ret = new SQLBuilder(config);

		if (config.exportOriginal) {
			String descr;
			if (ind.getBehavior().isPrimary()) {
				descr = "PRIMARY KEY";
			} else if (ind.getBehavior().isConstraint()) {
				descr = "CONSTRAINT";
			} else {
				descr = "INDEX";
			}
			ret.a("/** ORIGINAL ").a(descr).a(" **/").nl();
		}
		if (config.exportDrop) {
			ret.a(config.getDropComment(getQueryDrop(ind))).nl();
		}
		if (config.exportComments && !ind.getDescr().isEmpty()) {
			appendDescription(ret, ind);
		}

		String relFullNameEscaped = escapeFullName(ind.getRel1().getFullName());
		String indFullNameEscaped = escapeFullName(ind.getFullName());
		String indNameEscaped = escape(ind.getName());
		if (ind.getBehavior().isConstraint()) {
			ret.a(getQueryCreateConstraintBase(relFullNameEscaped, indNameEscaped, getQueryDef(ind)));

			if (!ind.getDescr().isEmpty()) {
				ret.nl().a(COMMENT_ON_CONSTRAINT).a(indNameEscaped).a(ON).a(relFullNameEscaped).a(IS).quotedEscaped(ind.getDescr()).semicolon();
			}
			if (config.exportOriginal) {
				ret.nl().commentLine(ind.getBehavior().isPrimary() ? 26 : 25);
			}
			return ret.toString();

		} else {
			ret.a("CREATE ").a(ind.getBehavior().isUnique() ? "UNIQUE " : "").a("INDEX ").a(indNameEscaped).nt()
					.a("ON ").a(relFullNameEscaped).nt()
					.a("USING ").a(ind.getBehavior().getAccessMethod()).nt()
					.a("(").a(ind.getBehavior().getDef()).a(")");

			if (ind.getBehavior().getOptions().length > 0) {
				ret.a(" WITH (");
				String comma = "";
				for (String opt : ind.getBehavior().getOptions()) {
					if (!opt.isEmpty()) {
						ret.a(comma).nt().a(opt);
						comma = COMMA;
					}
				}
				ret.n().a(")");
			}
			if (ind.getBehavior().getWhere() != null && !ind.getBehavior().getWhere().isEmpty()) {
				ret.n().a("WHERE ").a(ind.getBehavior().getWhere());
			}
			ret.semicolon();

			if (!ind.getDescr().isEmpty()) {
				ret.nl().a(COMMENT_ON_INDEX).a(indFullNameEscaped).a(IS).quotedEscaped(ind.getDescr()).semicolon();
			}
			if (config.exportOriginal) {
				ret.nl().commentLine(20);
			}
			return ret.toString();
		}
	}

	@Override
	public String getQueryDef(Index ind) {
		if (ind.getBehavior().isPrimary()) {
			return "PRIMARY KEY (" + ind.getBehavior().getDef() + ")";
		} else if (ind.getBehavior().isConstraint()) {
			return "UNIQUE (" + ind.getBehavior().getDef() + ")";
		} else {
			return ind.getBehavior().getDef();
		}
	}

	@Override
	public String getQueryDrop(Index ind) {
		if (ind.getBehavior().isConstraint()) {
			return getQueryDropConstraint(escapeFullName(ind.getRel1().getFullName()), escape(ind.getName()));
		} else {
			return DROP_INDEX + escapeFullName(ind.getFullName()) + ";";
		}
	}

	@Override
	public String getQueryChanged(Index ind) {
		SQLBuilder ret = new SQLBuilder();
		String relFullNameEscaped = escapeFullName(ind.getRel1().getFullName());
		String indFullNameEscaped = escapeFullName(ind.getFullName());
		String indNameEscaped = escape(ind.getName());
		Index.Behavior o = ind.getBehavior();
		Index.Behavior n = ind.getBehavior().getValuesForEdit();
		if (n.isDropped()) {
			ret.a(getQueryDrop(ind));
		} else {
			if ((!o.getName().equals(n.getName()) && (ind.getBehavior().isConstraint() || ind.getBehavior().isPrimary())) ||
					!o.getDef().equals(n.getDef()) ||
					!o.getWhere().equals(n.getWhere()) ||
					!o.getAccessMethod().equals(n.getAccessMethod()) ||
					o.isUnique() != n.isUnique()
					) {
				ret.a(getQueryDrop(ind));
				ind.setBehavior(n);
				ret.nl().a(getQueryCreate(ind, null));
				ind.setBehavior(o);

			} else if (!o.getName().equals(n.getName())) {
				ret.nlIfNotEmpty().a(ALTER_INDEX).a(indFullNameEscaped).a(RENAME_TO).a(escape(n.getName())).semicolon();

			}
			if (!n.getDescr().equals(o.getDescr())) {
				ret.nlIfNotEmpty().a("COMMENT ON ")
						.a(n.isConstraint() ? CONSTRAINT + indNameEscaped + ON + relFullNameEscaped : "INDEX " + indFullNameEscaped)
						.a(IS).quotedEscapedOrNull(n.getDescr()).semicolon();

			}
		}
		return ret.toString();
	}

	//////////////
	// FUNCTION //
	//////////////
	@Override
	public String getQueryExecFunction(Function func) {
		return "SELECT * FROM " + escapeFullName(func.getFullName()) + "(" + func.getCleanArgs() + ");";
	}

	@Override
	public String getQueryCreate(Function func, SQLOutputConfig config) {
		if (config == null) {
			config = SQLOutputConfig.DEFAULT;
		}

		SQLBuilder ret = new SQLBuilder(config);
		if (config.exportOriginal) {
			ret.a(ORIGINAL_FUNCTION).nl();
		}
		if (config.exportDrop) {
			ret.a(config.getDropComment(getQueryDrop(func))).nl();
		}
		if (config.exportComments && !func.getDescr().isEmpty()) {
			appendDescription(ret, func);
		}

		ret.a(CREATE_OR_REPLACE_FUNCTION).a(escapeFullName(func.getFullName())).a("(").a(func.getArgs()).a(")")
				.nt().a("RETURNS ").a(func.getRetType()).a(" AS\n$BODY$").a(func.getSrc()).a("$BODY$")
				.nt().a("LANGUAGE ").a(func.getLang()).space().a(func.getVolatility()).a(func.isSecurityDefiner() ? " SECURITY DEFINER" : "")
				.nt().a("COST ").a(func.getCost())
				.a(func.getRows() > 0 ? " ROWS " + func.getRows() : "")
				.semicolon();

		if (!func.getDescr().isEmpty()) {
			ret.nl().a(COMMENT_ON_FUNCTION).a(escapeFullName(func.getFullName())).a("(").a(func.getCleanArgs()).a(") IS ").quotedEscaped(func.getDescr()).semicolon();
		}
		if (config.exportOriginal) {
			ret.nl().commentLine(ORIGINAL_FUNCTION);
		}
		return ret.toString();
	}

	@Override
	public String getQueryDrop(Function func) {
		return DROP_FUNCTION_IF_EXISTS + escapeFullName(func.getFullName()) + "(" + func.getCleanArgs() + ");";
	}

	@Override
	public String getQueryChanged(Function func) {
		Function.Behavior o = func.getBehavior();
		Function.Behavior n = func.getBehavior().getValuesForEdit();
		if (n.isDropped()) {
			return getQueryDrop(func);
		} else {
			SQLBuilder ret = new SQLBuilder();
			if (!o.getName().equals(n.getName()) || !o.getSchemaName().equals(n.getSchemaName()) || !o.getRetType().equals(n.getRetType()) || !o.getArgs().equals(n.getArgs())) {
				ret.a(getQueryDrop(func));
				func.setBehavior(n);
				ret.nl().a(getQueryCreate(func, null));
				func.setBehavior(o);

			} else if (!o.getSrc().equals(n.getSrc()) || !o.getLang().equals(n.getLang()) || !o.getVolatility().equals(n.getVolatility()) || o.isSecDefiner() != n.isSecDefiner() || o.isStrict() != n.isStrict() || o.getCost() != n.getCost() || o.getRows() != n.getRows()) {
				func.setBehavior(n);
				ret.a(getQueryCreate(func, null));
				func.setBehavior(o);

			}
			if (!n.getDescr().equals(o.getDescr())) {
				func.setBehavior(n);
				ret.nlIfNotEmpty().a(COMMENT_ON_FUNCTION).a(escapeFullName(func.getFullName())).a("(").a(func.getCleanArgs()).a(")")
						.a(IS).quotedEscapedOrNull(n.getDescr()).semicolon();
				func.setBehavior(o);
			}
			return ret.toString();
		}
	}

	//////////////
	// PACKAGE //
	//////////////
	@Override
	public String getQueryCreate(com.databazoo.devmodeler.model.Package pack, SQLOutputConfig config) { return ""; }

	@Override
	public String getQueryDrop(com.databazoo.devmodeler.model.Package pack) { return ""; }

	@Override
	public String getQueryChanged(Package pack) { return ""; }

	//////////////
	// SEQUENCE //
	//////////////
	@Override
	public String getQueryCreate(Sequence seq, SQLOutputConfig config) {
		if (config == null) {
			config = SQLOutputConfig.DEFAULT;
		}

		SQLBuilder ret = new SQLBuilder(config);
		if (config.exportOriginal) {
			ret.a(ORIGINAL_SEQUENCE).nl();
		}
		if (config.exportDrop) {
			ret.a(config.getDropComment(getQueryDrop(seq))).nl();
		}
		if (config.exportComments && !seq.getDescr().isEmpty()) {
			appendDescription(ret, seq);
		}

		ret.a(CREATE_SEQUENCE).a(escapeFullName(seq.getFullName())).nt()
				.a(START_WITH).a(seq.getBehavior().getCurrent()).nt()
				.a(INCREMENT_BY).a(seq.getBehavior().getIncrement()).nt()
				.a(MINVALUE).a(seq.getBehavior().getMin()).nt()
				.a(MAXVALUE).a(seq.getBehavior().getMax()).nt()
				.a(seq.getBehavior().isCycle() ? "" : "NO ").a(CYCLE)
                .semicolon();

		if (!seq.getDescr().isEmpty()) {
			ret.nl().a(COMMENT_ON_SEQUENCE).a(escapeFullName(seq.getFullName())).a(IS).quotedEscaped(seq.getDescr()).semicolon();
		}
		if (config.exportOriginal) {
			ret.nl().commentLine(ORIGINAL_SEQUENCE);
		}
		return ret.toString();
	}

	@Override
	public String getQueryDrop(Sequence seq) {
		return DROP_SEQUENCE + escapeFullName(seq.getFullName()) + ";";
	}

	@Override
	public String getQueryChanged(Sequence seq) {
		Sequence.Behavior o = seq.getBehavior();
		Sequence.Behavior n = seq.getBehavior().getValuesForEdit();
		if (n.isDropped()) {
			return getQueryDrop(seq);
		} else {
			SQLBuilder ret = new SQLBuilder();
			if (!o.getIncrement().equals(n.getIncrement())) {
				ret.nt().a(INCREMENT_BY).a(n.getIncrement());
			}
			if (!o.getMin().equals(n.getMin())) {
				ret.nt().a(MINVALUE).a(n.getMin());
			}
			if (!o.getMax().equals(n.getMax())) {
				ret.nt().a(MAXVALUE).a(n.getMax());
			}
			if (!o.getCurrent().equals(n.getCurrent())) {
				ret.nt().a(START_WITH).a(n.getCurrent())
						.a(RESTART_WITH).a(n.getCurrent());
			}
			if (o.isCycle() != n.isCycle()) {
				ret.nt().a(n.isCycle() ? "" : "NO ").a(CYCLE);
			}
			if (!ret.isEmpty()) {
				String change = ret.toString();
				ret = new SQLBuilder();
				ret.a(ALTER_SEQUENCE).a(escapeFullName(seq.getFullName())).a(change).semicolon();
			}
			if (!n.getDescr().equals(o.getDescr())) {
				seq.setBehavior(n);
				ret.nlIfNotEmpty().a(COMMENT_ON_SEQUENCE).a(escapeFullName(seq.getFullName())).a(IS).quotedEscapedOrNull(n.getDescr()).semicolon();
				seq.setBehavior(o);
			}
			if (!o.getName().equals(n.getName())) {
				ret.nlIfNotEmpty().a(ALTER_SEQUENCE).a(escapeFullName(seq.getFullName())).a(RENAME_TO).a(escape(n.getName())).semicolon();
			}
			if (!o.getSchemaName().equals(n.getSchemaName())) {
				ret.nlIfNotEmpty().a(ALTER_SEQUENCE).a(escapeFullName((o.getSchemaName().isEmpty() ? "" : o.getSchemaName() + ".") + n.getName()))
						.a(SET_SCHEMA).a(escape(n.getSchemaName().isEmpty() ? PUBLIC_SCHEMA : n.getSchemaName())).semicolon();
			}
            return ret.toString();
		}
	}

	//////////
	// VIEW //
	//////////
	@Override
	public String getQueryCreate(View view, SQLOutputConfig config) {
		if (config == null) {
			config = SQLOutputConfig.DEFAULT;
		}

		SQLBuilder ret = new SQLBuilder(config);
		if (config.exportOriginal) {
			ret.a(ORIGINAL_VIEW).nl();
		}
		if (config.exportDrop) {
			ret.a(config.getDropComment(getQueryDrop(view))).nl();
		}
		if (config.exportComments && !view.getDescr().isEmpty()) {
			appendDescription(ret, view);
		}

		ret
				.a(view.getMaterialized() ? CREATE_MATERIALIZED_VIEW : CREATE_OR_REPLACE_VIEW)
				.a(escapeFullName(view.getFullName()))
				.nt().a("AS ")
				.withConditionalSemicolon(view.getSrc());

		if (!view.getDescr().isEmpty()) {
			ret.nl().a(COMMENT_ON_VIEW).a(escapeFullName(view.getFullName())).a(IS).quotedEscaped(view.getDescr()).semicolon();
		}
		if (config.exportOriginal) {
			ret.nl().commentLine(ORIGINAL_VIEW);
		}
		return ret.toString();
	}

	@Override
	public String getQueryDrop(View view) {
		return (view.getMaterialized() ? DROP_MATERIALIZED_VIEW : DROP_VIEW) + escapeFullName(view.getFullName()) + ";";
	}

	@Override
	public String getQueryChanged(View view) {
		View.Behavior o = view.getBehavior();
		View.Behavior n = view.getBehavior().getValuesForEdit();
		if (n.isDropped()) {
			return getQueryDrop(view);
		} else {
			SQLBuilder ret = new SQLBuilder();
			if (!o.getSrc().equals(n.getSrc()) || o.getMaterialized() != n.getMaterialized()) {
				ret.a(getQueryDrop(view));
				view.setBehavior(n);
				ret.nl().a(getQueryCreate(view, null));
				view.setBehavior(o);
			}
			if (!n.getDescr().equals(o.getDescr())) {
				view.setBehavior(n);
				ret.nlIfNotEmpty().a(COMMENT_ON_VIEW).a(escapeFullName(view.getFullName())).a(IS).quotedEscapedOrNull(n.getDescr()).semicolon();
				view.setBehavior(o);
			}
			if (!o.getName().equals(n.getName())) {
				ret.nlIfNotEmpty().a(ALTER_TABLE).a(escapeFullName(view.getFullName())).a(RENAME_TO).a(escape(n.getName())).semicolon();
			}
			if (!o.getSchemaName().equals(n.getSchemaName())) {
				ret.nlIfNotEmpty().a(ALTER_TABLE).a(escapeFullName((o.getSchemaName().isEmpty() ? "" : o.getSchemaName() + ".") + n.getName()))
						.a(SET_SCHEMA).a(escape(n.getSchemaName().isEmpty() ? PUBLIC_SCHEMA : n.getSchemaName())).semicolon();
			}
			return ret.toString();
		}
	}

	/////////////
	// TRIGGER //
	/////////////
	@Override
	public String getQueryCreate(Trigger trig, SQLOutputConfig config) {
		if (config == null) {
			config = SQLOutputConfig.DEFAULT;
		}

		SQLBuilder ret = new SQLBuilder(config);
		if (config.exportOriginal) {
			ret.a(ORIGINAL_TRIGGER).nl();
		}
		if (config.exportDrop) {
			ret.a(config.getDropComment(getQueryDrop(trig))).nl();
		}
		if (config.exportComments && !trig.getDescr().isEmpty()) {
			appendDescription(ret, trig);
		}

		String relFullNameEscaped = escapeFullName(trig.getRel1().getFullName());
		String trigNameEscaped = escape(trig.getName());
		ret.a(CREATE_TRIGGER).a(trigNameEscaped).nt()
				.a(trig.getBehavior().getTiming(" OR "))
				.a(trig.getBehavior().getAttrs().isEmpty() ? "" : " OF " + trig.getBehavior().getAttrs()).nt()
				.a("ON ").a(relFullNameEscaped).nt()
				.a("FOR EACH ").a(trig.getBehavior().getRowType()).nt()
				.a(trig.getBehavior().getWhen().isEmpty() ? "" : "WHEN (" + trig.getBehavior().getWhen() + ")\n\t")
				.a(EXECUTE_PROCEDURE).a(escapeFullName(trig.getBehavior().getProcname())).a("()").semicolon();
		if (!trig.getBehavior().isEnabled()) {
			ret.nl().a(ALTER_TABLE).a(relFullNameEscaped).a(DISABLE_TRIGGER).a(trigNameEscaped).semicolon();
		}
		if (!trig.getDescr().isEmpty()) {
			ret.nl().a(COMMENT_ON_TRIGGER).a(trigNameEscaped).a(ON).a(relFullNameEscaped).a(IS).quotedEscaped(trig.getDescr()).semicolon();
		}
		if (config.exportOriginal) {
			ret.nl().commentLine(ORIGINAL_TRIGGER);
		}
		return ret.toString();
	}

	@Override
	public String getQueryDrop(Trigger trig) {
		return DROP_TRIGGER + escape(trig.getName()) + ON + escapeFullName(trig.getRel1().getFullName()) + ";";
	}

	@Override
	public String getQueryChanged(Trigger trig) {
		String relFullNameEscaped = escapeFullName(trig.getRel1().getFullName());
		String trigNameEscaped = escape(trig.getName());
		Trigger.Behavior o = trig.getBehavior();
		Trigger.Behavior n = trig.getBehavior().getValuesForEdit();
		if (n.isDropped()) {
			return getQueryDrop(trig);
		} else {
			SQLBuilder ret = new SQLBuilder();
			if (!o.getTiming().equals(n.getTiming()) || !o.getRowType().equals(n.getRowType()) || !o.getProcname().equals(n.getProcname()) || !o.getAttrs().equals(n.getAttrs()) || !o.getWhen().equals(n.getWhen())) {
				ret.a(getQueryDrop(trig));
				trig.setBehavior(n);
				ret.nlIfNotEmpty().a(getQueryCreate(trig, null));
				trig.setBehavior(o);
			} else if (!o.getName().equals(n.getName())) {
				ret.a(ALTER_TRIGGER).a(trigNameEscaped).a(ON).a(relFullNameEscaped).a(RENAME_TO).a(escape(n.getName())).semicolon();
			}
			if (o.isEnabled() != n.isEnabled()) {
				ret.nlIfNotEmpty().a(ALTER_TABLE).a(relFullNameEscaped).space().a(n.isEnabled() ? "ENABLE" : "DISABLE").a(" TRIGGER ").a(trigNameEscaped)
						.semicolon();
			}
			if (!n.getDescr().equals(o.getDescr())) {
				ret.nlIfNotEmpty().a(COMMENT_ON_TRIGGER).a(trigNameEscaped).a(ON).a(relFullNameEscaped).a(IS).quotedEscapedOrNull(n.getDescr()).semicolon();
			}
            return ret.toString();
		}
	}

	@Override
	public String getQueryTerminate(int selectedPID) {
		return "SELECT pg_terminate_backend(" + selectedPID + ");";
	}

	@Override
	public String getQueryCancel(int selectedPID) {
		return "SELECT pg_cancel_backend(" + selectedPID + ");";
	}
}
