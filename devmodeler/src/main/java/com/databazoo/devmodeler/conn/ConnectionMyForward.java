
package com.databazoo.devmodeler.conn;

import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.CheckConstraint;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Package;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Sequence;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.model.User;
import com.databazoo.devmodeler.model.View;
import com.databazoo.tools.Dbg;

import static com.databazoo.devmodeler.conn.ConnectionUtils.ADD_COLUMN;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ADD_CONSTRAINT;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ALTER_DATABASE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ALTER_SCHEMA;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ALTER_TABLE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ALTER_USER;
import static com.databazoo.devmodeler.conn.ConnectionUtils.COLLATE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.COMMA;
import static com.databazoo.devmodeler.conn.ConnectionUtils.COMMENT;
import static com.databazoo.devmodeler.conn.ConnectionUtils.COMMENT_ON_DATABASE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.COMMENT_ON_USER;
import static com.databazoo.devmodeler.conn.ConnectionUtils.CONSTRAINT;
import static com.databazoo.devmodeler.conn.ConnectionUtils.CREATE_DATABASE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.CREATE_OR_REPLACE_VIEW;
import static com.databazoo.devmodeler.conn.ConnectionUtils.CREATE_SCHEMA;
import static com.databazoo.devmodeler.conn.ConnectionUtils.CREATE_TABLE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.CREATE_TRIGGER;
import static com.databazoo.devmodeler.conn.ConnectionUtils.CREATE_USER;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_COLUMN;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_DATABASE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_INDEX;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_SCHEMA;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_TABLE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_TRIGGER;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_USER;
import static com.databazoo.devmodeler.conn.ConnectionUtils.DROP_VIEW;
import static com.databazoo.devmodeler.conn.ConnectionUtils.FULLTEXT_LC;
import static com.databazoo.devmodeler.conn.ConnectionUtils.FUNCTION;
import static com.databazoo.devmodeler.conn.ConnectionUtils.IS;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ORIGINAL_ATTRIBUTE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ORIGINAL_CONSTRAINT;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ORIGINAL_DATABASE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ORIGINAL_FUNCTION;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ORIGINAL_SCHEMA;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ORIGINAL_TABLE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ORIGINAL_TRIGGER;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ORIGINAL_USER;
import static com.databazoo.devmodeler.conn.ConnectionUtils.ORIGINAL_VIEW;
import static com.databazoo.devmodeler.conn.ConnectionUtils.PROCEDURE;
import static com.databazoo.devmodeler.conn.ConnectionUtils.RENAME_TO;
import static com.databazoo.devmodeler.conn.ConnectionUtils.SELECT;

/**
 * Forward engineering setup for MySQL
 *
 * @author bobus
 */
abstract class ConnectionMyForward extends Connection {
	private static final String ENGINE = " ENGINE=";

	ConnectionMyForward(String name, String host, String user, String pass) {
		super(name, host, user, pass);
	}

	@Override
	public String escape(String name) {
		if(Settings.getBool(Settings.L_NAMING_ESCAPE_MY) ||
			name.matches("(?ims).*[^a-z0-9_]+.*") ||
			name.matches(ConnectionUtils.ESC_KEYWORDS)
		){
			return "`"+name+"`";
		}else{
			return name;
		}
	}

	@Override
	public String escapeFullName(String fullName) {
		fullName = fullName.replaceAll("`", "");
		if(fullName.matches(ConnectionUtils.FULL_NAME_REGEX)){
			return escape(fullName.replaceAll(ConnectionUtils.FULL_NAME_REGEX, "$1"))+"."+escape(fullName.replaceAll(ConnectionUtils.FULL_NAME_REGEX, "$2"));
		}else{
			return escape(fullName);
		}
	}

	@Override
	public String getCleanDef(Index ind) {
		if(ind.getBehavior().isPrimary()){
			return ind.getBehavior().getDef().replaceAll(".*PRIMARY KEY\\s+\\(", "").replaceAll("\\)$", "");
		}else if(ind.getBehavior().isUnique()){
			return ind.getBehavior().getDef().replaceAll(".*UNIQUE\\s+\\(", "").replaceAll("\\)$", "");
		}else{
			return ind.getBehavior().getDef();
		}
	}
	@Override public String getCleanDef(Constraint con) {
		return con.getBehavior().getDef().replaceAll(".*CHECK\\s*\\((.*)\\)", "$1");
	}

	@Override
	public String getQueryLimitOffset(int limit, int offset) {
		if(limit > 0) {
			return "LIMIT "+(offset>0 ? offset+", " : "")+limit;
		}else{
			return "";
		}
	}

	@Override
	public String getQueryExplain(String sql) {
		return "EXPLAIN "+sql;
	}

	@Override
	public void afterCreate(IModelElement elem) {
		super.afterCreate(elem);
		if(elem instanceof Relation){
			Relation rel = (Relation)elem;
			Attribute attr = new Attribute(rel, "id", "int(11)", false, 1, "auto_increment", "a", "");
			attr.assignToRels();

			String newName = "PRIMARY";
			Index ind = new Index(rel,
					rel.getSchema().getSchemaPrefix()+newName,
					attr.getName(),
					"",
					getAccessMethods()[0],
					new String[0],
					true,
					true,
					false,
					""
				);
			ind.assignToRelation(rel);
		}
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

		ret.a(CREATE_USER).quotedEscaped(user.getName())
				.a(" IDENTIFIED BY ").quotedEscaped(user.getBehavior().getPassword())
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
		User.Behavior o = user.getBehavior();
		User.Behavior n = user.getBehavior().getValuesForEdit();
		if (n.isDropped()) {
			return getQueryDrop(user);
		} else {
			SQLBuilder ret = new SQLBuilder();
			if (!o.getName().equals(n.getName())) {
				ret.a("RENAME USER ").quotedEscaped(o.getName()).a(" TO ").quotedEscaped(n.getName()).semicolon();
			}
			if (!o.getPassword().equals(n.getPassword())) {
				ret.a(ALTER_USER).quotedEscaped(o.getName()).a(" IDENTIFIED BY ").quotedEscaped(n.getPassword()).semicolon();
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
		if(config == null){
			config = SQLOutputConfig.DEFAULT;
		}

		SQLBuilder ret = new SQLBuilder(config);
		if(config.exportOriginal){
			ret.a(ORIGINAL_SCHEMA).nl();
		}
		if(config.exportDrop){
			ret.a(config.getDropComment(getQueryDrop(schema))).nl();
		}

		ret.a(CREATE_SCHEMA).a(escapeFullName(schema.getFullName())).semicolon();

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
		if(n.isDropped()){
			return getQueryDrop(schema);
		}else{
			SQLBuilder ret = new SQLBuilder();
			if(!o.getName().equals(n.getName())){
				ret.a(ALTER_SCHEMA).a(schemaFullNameEscaped).a(RENAME_TO).a(escape(n.getName())).semicolon();
			}
			if(!n.getDescr().equals(o.getDescr())){
				ret.nlIfNotEmpty().a(ALTER_SCHEMA).a(schemaFullNameEscaped).a(COMMENT).quotedEscaped(n.getDescr()).semicolon();
			}
			return ret.toString();
		}
	}

	//////////////
	// RELATION //
	//////////////
	@Override
	public String getQueryCreate(Relation rel, SQLOutputConfig config) {
		if(config == null){
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
			ret.a("-- ").a(rel.getDescr().replace("\n", "\n-- ")).n();
		}

		String relFullNameEscaped = escapeFullName(rel.getFullName());
		String elemNameEscaped;
		String comma = "";

		if(rel.getBehavior().isNew() && rel.getAttributes().isEmpty()){
			// TODO: fit the first column in
			return CREATE_TABLE + escape(rel.getName()) + " (\n\tid integer NOT NULL auto_increment PRIMARY KEY\n)" +
					(rel.getBehavior().getCollation()==null ? "" : COLLATE + rel.getBehavior().getCollation()) +
					ENGINE + rel.getBehavior().getStorage() +
					(rel.getBehavior().getDescr() != null && !rel.getBehavior().getDescr().isEmpty() ? "\nCOMMENT '"+rel.getBehavior().getDescr().replace("'", "''") +"'" : "")+";";
		}

		Index pk = null;
		for(Index ind : rel.getIndexes()){
			if(ind.getBehavior().isPrimary()){
				pk = ind;
				break;
			}
		}

		ret.a(CREATE_TABLE).a(relFullNameEscaped).a(" (");
		for (Attribute attr : rel.getAttributes()) {
			ret.a(comma).nt().a(escape(attr.getName())).space().a(getQueryDef(attr, config));
			if(pk != null && pk.getBehavior().getDef().equalsIgnoreCase(attr.getName())){
				ret.a(" PRIMARY KEY");
			}
			comma = COMMA;
		}
		// PRIMARY KEY IS ALREADY SET IN COLUMN DEFINITION
        /*for (Index ind : rel.getIndexes()) {
            if (ind.getBehavior().isPrimary) {
                ret += comma + "\n\t" + getQueryDef(ind);
                comma = ',';
                break;
            }
        }*/
		if(!config.exportSkipTriggersConstraints){
			for (Index ind : rel.getIndexes()) {
				elemNameEscaped = escape(ind.getName());
				if (!ind.getBehavior().isPrimary()) {
					ret.a(comma).nt()
						.a(ind.getBehavior().isUnique() ? "UNIQUE " : "")
						.a(ind.getBehavior().getAccessMethod().equalsIgnoreCase(FULLTEXT_LC) ? "FULLTEXT " : "")
						.a("INDEX ").a(elemNameEscaped)
						.a(ind.getBehavior().getAccessMethod().isEmpty() || ind.getBehavior().getAccessMethod().equalsIgnoreCase(FULLTEXT_LC) || ind.getBehavior().getAccessMethod().equalsIgnoreCase("btree") ? "" :
							" USING " + ind.getBehavior().getAccessMethod())
						.a(" (").a(ind.getBehavior().getDef()).a(")");
					comma = COMMA;
				}
			}
			for (Constraint con: rel.getConstraints()) {
				if (con.getRel1().getFullName().equals(rel.getFullName())) {
					elemNameEscaped = escape(con.getName());
					ret.a(comma).nt().a(CONSTRAINT).a(elemNameEscaped).space().a(con.getRel2() == null ? ((CheckConstraint)con).getDef() :
							"\n\t\t"+getQueryDef(con));
					comma = COMMA;
				}
			}
		}
		ret.a(comma.equals(COMMA) ? "\n" : "").a(")")
				.a(rel.getBehavior().getCollation()==null ? "" : COLLATE +rel.getBehavior().getCollation())
				.a(ENGINE).a(rel.getBehavior().getStorage());

		if(!rel.getDescr().isEmpty()){
			ret.n().a("COMMENT '").a(rel.getDescr().replaceAll("'", "''")).a("'");
		}
		ret.semicolon();

		if(config.exportData){
			try {
				getQueryData(rel, config, ret);
			} catch (DBCommException ex) {
				Dbg.fixme("COULD NOT LOAD DATA FOR TABLE", ex);
				ret.nl().a("/** COULD NOT LOAD DATA FOR TABLE **/");
			}
		}

		if(!config.exportSkipTriggersConstraints){
			for (Trigger trig: rel.getTriggers()) {
				ret.nl().a(getQueryCreate(trig, null));
			}
		}
		if(config.exportOriginal){
			ret.nl().commentLine(ORIGINAL_TABLE);
		}
		return ret.toString();
	}

	@Override
	public String getQueryDrop(Relation rel) {
		return DROP_TABLE + escapeFullName(rel.getName()) + ";";
	}

	@Override
	public String getQueryChanged(Relation rel) {
		String relFullNameEscaped = escape(rel.getName());
		Relation.Behavior o = rel.getBehavior();
		Relation.Behavior n = rel.getBehavior().getValuesForEdit();
		if(n.isDropped()){
			return getQueryDrop(rel);
		}else{
			SQLBuilder ret = new SQLBuilder();
			if(!o.getName().equals(n.getName())){
				ret.a(ALTER_TABLE).a(relFullNameEscaped).a(RENAME_TO).a(escape(n.getName())).semicolon();
			}
			if(n.getCollation() != null && !n.getCollation().equals(o.getCollation())){
				ret.nlIfNotEmpty().a(ALTER_TABLE).a(relFullNameEscaped).a(COLLATE).a(n.getCollation()).semicolon();
			}
			if(n.getStorage() != null && !n.getStorage().equals(o.getStorage())){
				ret.nlIfNotEmpty().a(ALTER_TABLE).a(relFullNameEscaped).a(ENGINE).a(n.getStorage()).semicolon();
			}
			if(!n.getDescr().equals(o.getDescr())){
				ret.nlIfNotEmpty().a(ALTER_TABLE).a(relFullNameEscaped).a(COMMENT).quotedEscaped(n.getDescr()).semicolon();
			}
			return ret.toString();
		}
	}

	///////////////
	// ATTRIBUTE //
	///////////////
	@Override
	public String getQueryCreate(Attribute attr, SQLOutputConfig config) {
		if(config == null){
			config = SQLOutputConfig.DEFAULT;
		}

		SQLBuilder ret = new SQLBuilder(config);
		if(config.exportOriginal){
			ret.a(ORIGINAL_ATTRIBUTE).nl();
		}
		if(config.exportDrop){
			ret.a(config.getDropComment(getQueryDrop(attr))).nl();
		}

		String relFullNameEscaped = escapeFullName(attr.getRel().getFullName());
		String attrNameEscaped = escape(attr.getName());

		ret.a(ALTER_TABLE).a(relFullNameEscaped).nt().a(ADD_COLUMN).a(attrNameEscaped).space().a(getQueryDef(attr, config)).semicolon();

		/*
		if(!attr.getDescr().isEmpty()){
			ret.nl().a("COMMENT ON COLUMN ").a(relFullNameEscaped).a(".").a(attrNameEscaped).a(" IS ").quotedEscaped(attr.getDescr()).eoc();
		}*/
		if(config.exportOriginal){
			ret.nl().commentLine(ORIGINAL_ATTRIBUTE);
		}
		return ret.toString();
	}

	@Override
	public String getQueryDef(Attribute attr, SQLOutputConfig config) {
		String typeStr = attr.getFullType();

		// Type comes from a different DB
		if(config instanceof SQLOutputConfigExport){
			if(((SQLOutputConfigExport) config).conn != null){
				typeStr = getTranslatedType(typeStr);
			}
		}

		return typeStr +
				(attr.getBehavior().isAttNull() ? "" : " NOT NULL") +
				(attr.getBehavior().getDefaultValue() != null && !attr.getBehavior().getDefaultValue().isEmpty() ?
					(attr.getBehavior().getDefaultValue().equalsIgnoreCase("auto_increment") ? " auto_increment" + (attr.isNew() ? " PRIMARY KEY" : "") : " DEFAULT "+getEscapedDefault(attr.getBehavior())) : "") +
				(attr.getBehavior().getCollation() != null && !attr.getBehavior().getCollation().equals(attr.getRel().getBehavior().getCollation()) ? COLLATE
						+attr.getBehavior().getCollation() : "") +
				(!attr.getBehavior().getDescr().isEmpty() ? " COMMENT '"+attr.getBehavior().getDescr().replace("'", "''") +"'" : "");
	}

	private String getEscapedDefault(Attribute.Behavior behavior){
		String lowerType = behavior.getAttType().toLowerCase();
		if(lowerType.endsWith("char") || lowerType.endsWith("text") || lowerType.equals("enum")){
			return "'"+behavior.getDefaultValue().replace("'", "''")+"'";
		}else{
			return behavior.getDefaultValue();
		}
	}

	@Override
	public String getQueryDrop(Attribute attr) {
		return ALTER_TABLE + escape(attr.getRel().getName()) + "\n\t" + DROP_COLUMN + escape(attr.getName())+";";
	}

	@Override
	public String getQueryChanged(Attribute attr) {
		String relFullNameEscaped = escapeFullName(attr.getRel().getFullName());
		String attrNameEscaped = escape(attr.getName());
		Attribute.Behavior o = attr.getBehavior();
		Attribute.Behavior n = attr.getBehavior().getValuesForEdit();
		if(n.isDropped()){
			return getQueryDrop(attr);
		}else{
			SQLBuilder ret = new SQLBuilder();
			if(!attr.getFullType(o).equals(attr.getFullType(n)) ||
				o.isAttNull() != n.isAttNull() ||
				o.getStorageChar() != n.getStorageChar() ||
				!o.getDefaultValue().equals(n.getDefaultValue()) ||
				!o.getName().equals(n.getName()) ||
				(n.getCollation() != null && !n.getCollation().equals(o.getCollation())) ||
				(!n.getDescr().equals(o.getDescr()))
			){
				attr.setBehavior(n);
				ret.a(ALTER_TABLE).a(relFullNameEscaped).a(" CHANGE ").a(attrNameEscaped).space().a(escape(n.getName())).space().a(getQueryDef(attr, null)).semicolon();
				attr.setBehavior(o);
			}
			return ret.toString();
		}
	}

	//////////////////////
	// CHECK CONSTRAINT //
	//////////////////////
	@Override public String getQueryCreateWithoutComment(CheckConstraint con, SQLOutputConfig config) { return ""; }
	@Override public String getQueryCreate(CheckConstraint con, SQLOutputConfig config) { return ""; }
	@Override public String getQueryChanged(CheckConstraint con) { return ""; }

	////////////////
	// CONSTRAINT //
	////////////////
	private String getQueryCreateConstraintBase(String relName, String conName, String def){
		return ALTER_TABLE + relName + "\n\t" + ADD_CONSTRAINT + conName + "\n\t\t" + def + ";";
	}

	@Override
	public String getQueryCreateWithoutComment(Constraint con, SQLOutputConfig config) {
		if(config == null){
			config = SQLOutputConfig.DEFAULT;
		}

		SQLBuilder ret = new SQLBuilder(config);
		if(config.exportOriginal){
			ret.a(ORIGINAL_CONSTRAINT).nl();
		}
		if(config.exportDrop){
			ret.a(config.getDropComment(getQueryDrop(con))).nl();
		}

		ret.a(getQueryCreateConstraintBase(escapeFullName(con.getRel1().getFullName()), escape(con.getName()), getQueryDef(con)));

		if(config.exportOriginal){
			ret.nl().commentLine(ORIGINAL_CONSTRAINT);
		}
		return ret.toString();
	}

	@Override
	public String getQueryCreate(Constraint con, SQLOutputConfig config) {
		if(config == null){
			config = SQLOutputConfig.DEFAULT;
		}

		SQLBuilder ret = new SQLBuilder(config);
		if(config.exportOriginal){
			ret.a(ORIGINAL_CONSTRAINT).nl();
		}
		if(config.exportDrop){
			ret.a(config.getDropComment(getQueryDrop(con))).nl();
		}

		if(!(con instanceof CheckConstraint)){
			if(con.getBehavior().isCreateNewAttribute()){
				String relFullNameEscaped = escapeFullName(con.getRel1().getFullName());
				String attrNameEscaped = escape(con.getBehavior().getNewColName());
				ret.a(ALTER_TABLE).a(relFullNameEscaped).nt()
						.a(ADD_COLUMN).a(attrNameEscaped).space().a(con.getBehavior().getNewColType()).a(con.getBehavior().isReferenceNullable() ? "" : " NOT").a(" NULL").semicolon();

			}else if(con.getBehavior().isReferenceNullable() != con.getBehavior().getAttr1().getBehavior().isAttNull()){
				con.getBehavior().getAttr1().getBehavior().prepareForEdit().notifyChange(Attribute.Behavior.L_NULLABLE, con.getBehavior().isReferenceNullable());
				ret.a(getQueryChanged(con.getBehavior().getAttr1())).nl();
			}
		}

		ret.a(getQueryCreateConstraintBase(escapeFullName(con.getRel1().getFullName()), escape(con.getName()), getQueryDef(con)));

		/*if(!con.getDescr().isEmpty()){
			ret.nl().a("COMMENT ON CONSTRAINT ").a(escape(con.getName())).a(" ON ").a(escapeFullName(con.getRel1().getFullName())).a(" IS ").quotedEscaped(con.getDescr()).eoc();
		}*/
		if(config.exportOriginal){
			ret.nl().commentLine(ORIGINAL_CONSTRAINT);
		}
		return ret.toString();
	}

	@Override
	public String getQueryDef(Constraint con) {
		if(con instanceof CheckConstraint){
			return ((CheckConstraint)con).getDef();
		}else{
			String attr1Name = con.getBehavior().isCreateNewAttribute() ? con.getBehavior().getNewColName() : con.getAttr1().getName();
			return "FOREIGN KEY (" + escape(attr1Name) + ")\n\t\tREFERENCES " + escapeFullName(con.getRel2().getFullName()) + " (" + escape(con.getAttr2().getName()) + ")\n\t\tON UPDATE " + con.getBehavior().getOnUpdate() + " ON DELETE " + con.getBehavior().getOnDelete();
		}
	}

	@Override
	public String getQueryDrop(Constraint con) {
		String createColumn = "";
		if(con.getBehavior().isCreateNewAttribute()){
			String relFullNameEscaped = escapeFullName(con.getRel1().getFullName());
			String attrNameEscaped = escape(con.getBehavior().getNewColName());
			createColumn = "\n\n" + ALTER_TABLE + relFullNameEscaped + "\n\t" + DROP_COLUMN + attrNameEscaped + ";";

		}else if(con.getBehavior().getAttr1() != null && con.getBehavior().isReferenceNullable() != con.getBehavior().getAttr1().getBehavior().isAttNull()){
			con.getBehavior().getAttr1().getBehavior().prepareForEdit();
			con.getBehavior().getAttr1().getBehavior().setAttNull(con.getBehavior().isReferenceNullable());
			createColumn = "\n\n"+(getQueryChanged(con.getBehavior().getAttr1()));
			con.getBehavior().getAttr1().getBehavior().setAttNull(!con.getBehavior().isReferenceNullable());
		}
		return getQueryDropConstraint(escapeFullName(con.getRel1().getFullName()), escape(con.getName())) + createColumn;
	}
	private String getQueryDropConstraint(String relName, String conName){
		return ALTER_TABLE + relName + "\n\tDROP FOREIGN KEY " + conName + ";";
	}

	@Override
	public String getQueryChanged(Constraint con) {
		Constraint.Behavior o = con.getBehavior();
		Constraint.Behavior n = con.getBehavior().getValuesForEdit();
		if(n.isDropped()){
			return getQueryDrop(con);
		}else{
			SQLBuilder ret = new SQLBuilder();
			String oSQL = getQueryCreateWithoutComment(con, null);
			con.setBehavior(n);
			String nSQL = getQueryCreateWithoutComment(con, null);
			con.setBehavior(o);
			if(!oSQL.equals(nSQL)){
				ret.a(getQueryDrop(con)).nl().a(nSQL);
			}
			return ret.toString();
		}
	}

	///////////
	// INDEX //
	///////////
	@Override
	public String getQueryCreate(Index ind, SQLOutputConfig config) {
		if(config == null){
			config = SQLOutputConfig.DEFAULT;
		}
		SQLBuilder ret = new SQLBuilder(config);

		if(config.exportOriginal){
				String descr;
				if(ind.getBehavior().isPrimary()){
					descr = "PRIMARY KEY";
				/*}else if(ind.getBehavior().isConstraint){
					descr = "CONSTRAINT";*/
				}else{
					descr = "INDEX";
				}
			ret.a("/** ORIGINAL ").a(descr).a(" **/").nl();
		}
		if(config.exportDrop){
			ret.a(config.getDropComment(getQueryDrop(ind))).nl();
		}
		if(config.exportComments && !ind.getDescr().isEmpty()){
			appendDescription(ret, ind);
		}

		String relFullNameEscaped = escapeFullName(ind.getRel1().getFullName());
		String indNameEscaped = escape(ind.getName());
		/*if(ind.getBehavior().isConstraint){
			return getQueryCreateConstraint(relFullNameEscaped, indNameEscaped, getQueryDef(ind), ind.getDescr());*/
		if(ind.getBehavior().isPrimary()){
			ret.a(ALTER_TABLE).a(relFullNameEscaped).nt().a("ADD PRIMARY KEY (").a(ind.getBehavior().getDef()).a(")").semicolon();
			if(config.exportOriginal){
				ret.nl().commentLine(26);
			}
			return ret.toString();
		}else{
			ret.a("CREATE ")
					.a(ind.getBehavior().isUnique() ? "UNIQUE " : "")
					.a(ind.getBehavior().getAccessMethod().equalsIgnoreCase(FULLTEXT_LC) ? "FULLTEXT " : "").a("INDEX ").a(indNameEscaped)
					.a(ind.getBehavior().getAccessMethod().isEmpty() || ind.getBehavior().getAccessMethod().equalsIgnoreCase(FULLTEXT_LC) || ind.getBehavior().getAccessMethod().equalsIgnoreCase("btree") ? "" :
						"\n\tUSING "+ind.getBehavior().getAccessMethod())
					.nt().a("ON ").a(relFullNameEscaped).a(" (").a(ind.getBehavior().getDef()).a(")");

			if(!ind.getDescr().isEmpty()){
				ret.a("\n\tCOMMENT ").quotedEscaped(ind.getDescr());
			}
			ret.semicolon();

			if(config.exportOriginal){
				ret.nl().commentLine(20);
			}
			return ret.toString();
		}
	}

	@Override
	public String getQueryDef(Index ind) {
		if(ind.getBehavior().isPrimary()){
			return "PRIMARY KEY ("+ind.getBehavior().getDef()+")";
		}else{
			return ind.getBehavior().getDef();
		}
	}

	@Override
	public String getQueryDrop(Index ind) {
		if(ind.getBehavior().isPrimary()){
			// TODO: check if col is autoincrement and modify it
			return ALTER_TABLE + escape(ind.getRel1().getName())+"\n\tDROP PRIMARY KEY;";
		}else{
			return ALTER_TABLE + escape(ind.getRel1().getName())+"\n\t" + DROP_INDEX + escape(ind.getName())+";";
		}
	}

	@Override
	public String getQueryChanged(Index ind) {
		Index.Behavior o = ind.getBehavior();
		Index.Behavior n = ind.getBehavior().getValuesForEdit();
		if(n.isDropped()){
			return getQueryDrop(ind);
		}else{
			SQLBuilder ret = new SQLBuilder();
			if(!o.getName().equals(n.getName()) ||
				!o.getDef().equals(n.getDef()) ||
				!o.getWhere().equals(n.getWhere()) ||
				!o.getAccessMethod().equals(n.getAccessMethod()) ||
				o.isUnique() != n.isUnique() ||
				!n.getDescr().equals(o.getDescr())
			){
				/*if(ind.getBehavior().isPrimary){
					for(Attribute attr : ind.getAttributes()){
						if(attr.getBehavior().def.equalsIgnoreCase("auto_increment"))
						{
							Attribute.Behavior attrO = attr.getBehavior();
							Attribute.Behavior attrN = attr.getBehavior().valuesForEdit;
							if(isForwardChange){
								attrN = (Attribute.Behavior) attr.getBehavior().prepareForEdit();
								attrN.notifyChange(Attribute.Behavior.L_DEFAULT, "");
								change += "\n\n"+getQueryChanged(attr);
							}else{
								attr.getBehavior() = attrN;
								attr.getBehavior().valuesForEdit = attrO;
								attrN.isNew = true;
								change += "\n\n"+getQueryChanged(attr);
								attrN.isNew = false;
								attr.getBehavior() = attrO;
							}
						}
					}
				}*/
				ret.a(getQueryDrop(ind));
				ind.setBehavior(n);
				ret.nl().a(getQueryCreate(ind, null));
				ind.setBehavior(o);
			}
			return ret.toString();
		}
	}

	//////////////
	// FUNCTION //
	//////////////
	@Override
	public String getQueryExecFunction(Function func) {
		if(func.getRetType() == null || func.getRetType().isEmpty()) {
			return "CALL " + escapeFullName(func.getFullName()) + "(" + func.getCleanArgs() + ");";
		}else{
			return SELECT + escapeFullName(func.getFullName()) + "(" + func.getCleanArgs() + ");";
		}
	}

	@Override
	public String getQueryCreate(Function func, SQLOutputConfig config){
		if(config == null){
			config = SQLOutputConfig.DEFAULT;
		}

		SQLBuilder ret = new SQLBuilder(config);
		if(config.exportOriginal){
			ret.a(ORIGINAL_FUNCTION).nl();
		}
		if(config.exportDrop){
			ret.a(config.getDropComment(getQueryDrop(func))).nl();
		}
		if(config.exportComments && !func.getDescr().isEmpty()){
			appendDescription(ret, func);
		}

		ret.a("CREATE ").a(func.getRetType() == null || func.getRetType().isEmpty() ? PROCEDURE : FUNCTION).space()
				.a(escapeFullName(func.getFullName())).a("(").a(func.getArgs()).a(")")
				.a(func.getRetType() == null || func.getRetType().isEmpty() ? "" : "\n\tRETURNS " + func.getRetType())
				.n().a(func.getSrc());
		if(!func.getSrc().endsWith(";")){
			ret.semicolon();
		}

		if(!func.getDescr().isEmpty()){
			ret.nl().a("ALTER ").a(func.getRetType() == null || func.getRetType().isEmpty() ? PROCEDURE : FUNCTION).space()
					.a(escapeFullName(func.getFullName())).space()
					.a("COMMENT ").quotedEscaped(func.getDescr()).semicolon();
		}
		if(config.exportOriginal){
			ret.nl().commentLine(ORIGINAL_FUNCTION);
		}
		return ret.toString();
	}

	@Override
	public String getQueryDrop(Function func) {
		return "DROP " +
				(func.getRetType() == null || func.getRetType().isEmpty() ? PROCEDURE : FUNCTION) +
				" IF EXISTS " + escapeFullName(func.getFullName()) + ";";
	}

	@Override
	public String getQueryChanged(Function func){
		Function.Behavior o = func.getBehavior();
		Function.Behavior n = func.getBehavior().getValuesForEdit();
		if(n.isDropped()){
			return getQueryDrop(func);
		}else{
			SQLBuilder ret = new SQLBuilder();
			if(
				!o.getName().equals(n.getName()) ||
				!o.getSchemaName().equals(n.getSchemaName()) ||
				!o.getRetType().equals(n.getRetType()) ||
				!o.getArgs().equals(n.getArgs()) ||
				!o.getSrc().equals(n.getSrc()) ||
				!o.getVolatility().equals(n.getVolatility()) ||
				o.isSecDefiner() != n.isSecDefiner() ||
				o.isStrict() != n.isStrict()
			){
				ret.a(getQueryDrop(func));
				func.setBehavior(n);
				ret.nl().a(getQueryCreate(func, null));
                func.setBehavior(o);
		}
			if(!n.getDescr().equals(o.getDescr())){
				func.setBehavior(n);
				ret.nlIfNotEmpty().a("ALTER ").a(func.getRetType() == null || func.getRetType().isEmpty() ? PROCEDURE : FUNCTION).space()
						.a(escapeFullName(func.getFullName())).a(COMMENT).quotedEscaped(n.getDescr()).semicolon();
				func.setBehavior(o);
	}
			return ret.toString();
		}
	}

	//////////////
	// PACKAGE //
	//////////////
	@Override public String getQueryCreate(com.databazoo.devmodeler.model.Package pack, SQLOutputConfig config){ return ""; }
	@Override public String getQueryDrop(Package pack) { return ""; }
	@Override public String getQueryChanged(Package pack){ return ""; }

	//////////////
	// SEQUENCE //
	//////////////
	@Override public String getQueryCreate(Sequence func, SQLOutputConfig config) { return ""; }
	@Override public String getQueryDrop(Sequence func) { return ""; }
	@Override public String getQueryChanged(Sequence func) { return ""; }

	//////////
	// VIEW //
	//////////
	@Override public String getQueryCreate(View view, SQLOutputConfig config) {
		if(config == null){
			config = SQLOutputConfig.DEFAULT;
		}

		SQLBuilder ret = new SQLBuilder();
		if(config.exportOriginal){
			ret.a(ORIGINAL_VIEW).nl();
		}
		if(config.exportDrop){
			ret.a(config.getDropComment(getQueryDrop(view))).nl();
		}

		String src = view.getSrc();
		ret.a(CREATE_OR_REPLACE_VIEW).a(escapeFullName(view.getFullName())).nt().a("AS ").a(src);
		if(src.charAt(src.length()-1) != ';'){
			ret.semicolon();
		}

		if(config.exportOriginal){
			ret.nl().commentLine(ORIGINAL_VIEW);
		}
		return ret.toString();
	}
	@Override public String getQueryDrop(View view) {
		return DROP_VIEW + escapeFullName(view.getFullName()) + ";";
	}
	@Override public String getQueryChanged(View view) {
		View.Behavior o = view.getBehavior();
		View.Behavior n = view.getBehavior().getValuesForEdit();
		if(n.isDropped()){
			return getQueryDrop(view);
		}else{
			SQLBuilder ret = new SQLBuilder();
			if(!o.getName().equals(n.getName())){
				ret.a(getQueryDrop(view));
				view.setBehavior(n);
				ret.nl().a(getQueryCreate(view, null));
				view.setBehavior(o);
			}else if(!o.getSrc().equals(n.getSrc())){
				view.setBehavior(n);
				ret.a(getQueryCreate(view, null));
				view.setBehavior(o);
			}
			return ret.toString();
		}
	}

	/////////////
	// TRIGGER //
	/////////////
	@Override
	public String getQueryCreate(Trigger trig, SQLOutputConfig config) {
		if(config == null){
			config = SQLOutputConfig.DEFAULT;
		}

		SQLBuilder ret = new SQLBuilder();
		if(config.exportOriginal){
			ret.a(ORIGINAL_TRIGGER).nl();
		}
		if(config.exportDrop){
			ret.a(config.getDropComment(getQueryDrop(trig))).nl();
		}

		String relFullNameEscaped = escapeFullName(trig.getRel1().getFullName());
		String trigNameEscaped = escape(trig.getName());
		ret.a(CREATE_TRIGGER).a(trigNameEscaped).nt().a(trig.getBehavior().getTiming())
				.nt().a("ON ").a(relFullNameEscaped)
				.nt().a("FOR EACH ").a(trig.getBehavior().getRowType())
				.n().a(trig.getBehavior().getDef());
		if(trig.getBehavior().getDef().charAt(trig.getBehavior().getDef().length()-1) != ';'){
			ret.semicolon();
		}
		if(config.exportOriginal){
			ret.nl().commentLine(ORIGINAL_TRIGGER);
		}
		return ret.toString();
	}

	@Override
	public String getQueryDrop(Trigger trig) {
		return DROP_TRIGGER + escape(trig.getName()) + ";";
	}

	@Override
	public String getQueryChanged(Trigger trig) {
		Trigger.Behavior o = trig.getBehavior();
		Trigger.Behavior n = trig.getBehavior().getValuesForEdit();
		if(n.isDropped()){
			return getQueryDrop(trig);
		}else{
			SQLBuilder ret = new SQLBuilder();
			if(!o.getName().equals(n.getName()) || !o.getTiming().equals(n.getTiming()) || !o.getRowType().equals(n.getRowType()) || !o.getDef().equals(n.getDef())){
				ret.a(getQueryDrop(trig));
				trig.setBehavior(n);
				ret.nl().a(getQueryCreate(trig, null));
				trig.setBehavior(o);
			}
			return ret.toString();
		}
	}

	@Override
	public String getQueryTerminate(int selectedPID) {
		return "KILL CONNECTION "+selectedPID+";";
	}

	@Override
	public String getQueryCancel(int selectedPID) {
		return "KILL QUERY "+selectedPID+";";
	}

}
