
package com.databazoo.devmodeler.conn;

import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.model.*;
import com.databazoo.devmodeler.project.Project;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.databazoo.devmodeler.conn.ConnectionUtils.*;

/**
 * Forward engineering setup for MySQL
 *
 * @author bobus
 */
abstract class ConnectionMyReverse extends ConnectionMyForward {

	private static final String FROM_INFORMATION_SCHEMA_PARAMETERS = "FROM information_schema.parameters ";

	private int serverActivityTotalRows;

	/*ConnectionMyReverse(String host, String user, String pass) {
		super(host, user, pass);
	}*/

	ConnectionMyReverse(String name, String host, String user, String pass) {
		super(name, host, user, pass);
	}

	@Override
	public void loadEnvironment() throws DBCommException {
		int log = DesignGUI.getInfoPanel().write("Loading server environment...");

		// load available storage engines
		Query q = new Query("SHOW ENGINES", getDefaultDB()).run();
		while(q.next()){
			if(!ConnectionMy.ENGINES.contains(q.getString("ENGINE"))) {
				ConnectionMy.ENGINES.add(q.getString("ENGINE"));
			}
		}
		q.close();

		// load available collations
		q = new Query("SELECT COLLATION_NAME FROM COLLATIONS", getDefaultDB()).run();
		while(q.next()){
			if(!ConnectionMy.COLLATIONS.contains(q.getString("COLLATION_NAME"))) {
				ConnectionMy.COLLATIONS.add(q.getString("COLLATION_NAME"));
			}
		}
		Collections.sort(ConnectionMy.COLLATIONS);
		q.close();

		q.log(log);
	}

	@Override
	public Result getAllRows(Relation rel) throws DBCommException {
		String pk = rel.getPKey();
		if(pk.isEmpty()){
			return null;
		}else{
			String sql = "SELECT concat_ws(',',"+pk+") AS ddmpkcol, t.* FROM " + rel.getFullName() + " t ORDER BY "+pk;
			return new Result(new Query(sql, rel.getDB().getFullName()).run());
		}
	}

	@Override
	public List<DB> getDatabases() throws DBCommException {
		int log = DesignGUI.getInfoPanel().write("Loading databases...");
		List<DB> dbs = new ArrayList<>();
		Query q = new Query("SHOW DATABASES", getDefaultDB()).run();
		while (q.next()) {
			dbs.add(new DB(Project.getCurrent(), this, q.getString(1)));
		}
		q.close();
		q.log(log);
		return new CopyOnWriteArrayList<>(dbs);
	}

	@Override
	public List<User> getUsers() throws DBCommException {
		int log = DesignGUI.getInfoPanel().write("Loading users...");
		List<User> users = new ArrayList<>();
		Query q = new Query("SELECT User FROM mysql.user", getDefaultDB()).run();
		while (q.next()) {
			users.add(new User(q.getString(1)));
		}
		q.close();
		q.log(log);
		return new CopyOnWriteArrayList<>(users);
	}

	@Override
	public void loadSchemas(DB db) {
		db.getSchemas().add(new Schema(db, db.getName(), ""));
	}

	@Override
	public void loadRelations(DB db) throws DBCommException {
		int log = DesignGUI.getInfoPanel().write("Loading tables of "+db.getFullName()+"...");
		String sql = SELECT +
								"t.table_schema AS "+TABLE_SCHEMA+", " +
								"t.table_name AS "+TABLE_NAME+", " +
								"table_rows AS reltuples, " +
								"table_comment AS "+DESCRIPTION+", " +
								"engine, " +
								"count(*) AS relnatts, " +
								"table_collation AS collation\n" +
							"FROM information_schema.tables t\n" +
							"JOIN information_schema.columns c USING (table_schema, table_name)\n" +
							"WHERE t.table_schema = '"+(dbAlias != null ? dbAlias : db.getName())+"' AND table_type != 'VIEW'\n" +
							"GROUP BY t.table_schema, t.table_name, table_rows, table_comment, engine, table_collation\n" +
							"ORDER BY t.table_name";
		Query q = new Query(sql, db.getFullName()).run();
		while (q.next()) {
			Relation rel = new Relation(db.getSchemas().get(0),
				q.getString(TABLE_NAME),
				q.getString(TABLE_NAME),
				q.getInt("relnatts"),
				q.getInt("reltuples"),
				false,
				new String[0],
				q.getString(DESCRIPTION),
				null
			);
			rel.getBehavior().setStorage(q.getString("engine"));
			rel.getBehavior().setCollation(q.getString("collation"));
			rel.assignToSchema();
		}
		q.close();
		q.log(log);
	}

	@Override
	public void loadRelationInfo(Relation rel) throws DBCommException {
		int log = DesignGUI.getInfoPanel().write("Loading table info...");
		Query q = new Query(SELECT
					+ "index_length,\n"
					+ "data_length,\n"
					//+ "concat('{', auto_increment, '}') AS pkey,\n"
					+ "table_rows\n"
				+ "FROM information_schema.tables t\n"
				+ "WHERE (t.table_schema, t.table_name) = ('"+(dbAlias != null ? dbAlias : rel.getDB().getName())+"', '"+rel.getName()+"')", rel.getDB().getFullName()).run();
		while (q.next()) {
			rel.setCountRows(q.getInt("table_rows"));
			rel.setSizeTotal(q.getLong("index_length") + q.getLong("data_length"));
			rel.setSizeIndexes(q.getLong("index_length"));
			//rel.pkCols = q.getIntArray("pkey");
		}
		q.close();
		q.log(log);
	}

	@Override
	public void loadViews(DB db) throws DBCommException {
		int log = DesignGUI.getInfoPanel().write("Loading views of "+db.getFullName()+"...");
		String sql = SELECT +
								"t.table_schema AS "+TABLE_SCHEMA+", " +
								"t.table_name AS "+TABLE_NAME+", " +
								"view_definition AS source\n" +
							"FROM information_schema.VIEWS t\n" +
							//"JOIN information_schema.columns c USING (table_schema, table_name)\n" +
							"WHERE t.table_schema = '"+(dbAlias != null ? dbAlias : db.getName())+"'\n" +
							"GROUP BY t.table_schema, t.table_name, source\n" +
							"ORDER BY t.table_name";
		//Dbg.info(sql);
		Query q = new Query(sql, db.getFullName()).run();
		while (q.next()) {
			Schema schema = db.getSchemas().get(0);
			View view = new View(schema, q.getString(TABLE_NAME), false, q.getString("source"), "");
			view.assignToSchema();
		}
		q.close();
		q.log(log);
	}

	@Override
	public void loadAttributes(DB db) throws DBCommException {
		int log = DesignGUI.getInfoPanel().write("Loading attributes of "+db.getFullName()+"...");
		Query q = new Query(SELECT +
								"c.table_schema AS "+TABLE_SCHEMA+",\n" +
								"c.table_name AS "+TABLE_NAME+",\n" +
								"c.column_name AS attname,\n" +
								"c.column_type AS typname,\n" +
								"c.ordinal_position AS attnum,\n" +
								"c.is_nullable AS is_null,\n" +
								" CASE WHEN extra = 'auto_increment' THEN extra ELSE column_default END AS def,\n" +
								"COLLATION_NAME AS collation,\n" +
								"column_comment AS "+DESCRIPTION+"\n" +
							"FROM information_schema.columns c\n" +
							"WHERE c.table_schema = '"+(dbAlias != null ? dbAlias : db.getName())+"'\n" +
							"ORDER BY c.ordinal_position", db.getFullName()).run();
		while (q.next()) {
			for(Relation rel: db.getSchemas().get(0).getRelations()){
				if(rel.getFullName().equals(q.getString(TABLE_NAME))) {
					Attribute attr = new Attribute(rel,
							q.getString("attname"),
							q.getString("typname"),
							q.getBool("is_null"),
							q.getInt("attnum"),
							q.getString("def"),
							"p",
							q.getString(DESCRIPTION));
					attr.getBehavior().setCollation(q.getString("collation"));
					attr.assignToRels();
					break;
				}
			}
		}
		q.close();
		q.log(log);
	}

	@Override
	public void loadConstraints(DB db) throws DBCommException {
		HashMap<String,Character> act = new HashMap<>();
		act.put("NO ACTION", Constraint.Behavior.NO_ACTION);
		act.put("RESTRICT", Constraint.Behavior.RESTRICT);
		act.put("CASCADE", Constraint.Behavior.CASCADE);
		act.put("SET NULL", Constraint.Behavior.SET_NULL);

		int log = DesignGUI.getInfoPanel().write("Loading constraints of "+db.getFullName()+"...");
		String sql;
		if(versionMajor >= 5.1f){
			sql = SELECT
					+ "rc.constraint_name AS full_name,\n"
					+ "rc.table_name AS rel1,\n"
					+ "rc.referenced_table_name AS rel2,\n"
					+ "column_name AS att1,\n"
					+ "referenced_column_name AS att2,\n"
					+ "update_rule AS confupdtype,\n"
					+ "delete_rule AS confdeltype,\n"
					+ "'' AS description\n"
				+ "FROM information_schema.REFERENTIAL_CONSTRAINTS AS rc\n"
				+ "JOIN information_schema.KEY_COLUMN_USAGE USING (CONSTRAINT_SCHEMA, CONSTRAINT_NAME)\n"
				+ "WHERE rc.constraint_schema = '"+(dbAlias != null ? dbAlias : db.getName())+"'\n"
				+ "ORDER BY rc.table_name, rc.constraint_name";
		}else{
			sql = SELECT
					+ "constraint_name AS full_name,\n"
					+ "table_name AS rel1,\n"
					+ "referenced_table_name AS rel2,\n"
					+ "column_name AS att1,\n"
					+ "referenced_column_name AS att2,\n"
					+ "'CASCADE' AS confupdtype,\n"
					+ "'CASCADE' AS confdeltype,\n"
					+ "'' AS "+DESCRIPTION+"\n"
				+ "FROM information_schema.KEY_COLUMN_USAGE\n"
				+ "WHERE constraint_schema = '"+(dbAlias != null ? dbAlias : db.getName())+"' AND referenced_table_name IS NOT NULL\n"
				+ "ORDER BY table_name, constraint_name";
		}
		//Dbg.info(sql);
		Query q = new Query(sql, db.getFullName()).run();
		while (q.next()) {
			Constraint c;
			//if(q.getBool("isforeign")){
				c = new Constraint(db,
						q.getString("full_name"),
						act.get(q.getString("confupdtype")).toString(),
						act.get(q.getString("confdeltype")).toString(),
						q.getString(DESCRIPTION));
				c.setRelsAttrsByName(db, q.getString("rel1"), q.getString("rel2"), q.getString("att1"), q.getString("att2"), !db.getAssignLines());
				c.checkSize();
			/*}else{
				CheckConstraint c1 = new CheckConstraint(db, q.getString("full_name"), "CHECK "+q.getString("consrc"), q.getString("description"));
				c1.setRelByName(db, q.getString("rel1"), !db.getAssignLines());
				c = c1;
			}*/
		}
		q.close();
		q.log(log);
	}

	@Override
	public void loadFunctions(DB db) throws DBCommException {
		if (versionMajor > 5.0f ) {
            int log = DesignGUI.getInfoPanel().write("Loading functions of " + db.getFullName() + "...");
            Query q = new Query("SELECT ROUTINE_SCHEMA, ROUTINE_NAME, DTD_IDENTIFIER AS DATA_TYPE, " +
                    "ROUTINE_DEFINITION, SECURITY_TYPE, " +
                    "CASE " +
                    "WHEN EXISTS " +
                    "(" +
                    "SELECT * " +
                    FROM_INFORMATION_SCHEMA_PARAMETERS +
                    "WHERE PARAMETER_MODE IS NOT NULL AND SPECIFIC_SCHEMA = r.ROUTINE_SCHEMA AND SPECIFIC_NAME = r.ROUTINE_NAME AND ROUTINE_TYPE = r.ROUTINE_TYPE AND PARAMETER_MODE != 'IN'" +
                    ") " +
                    "THEN " +
                    "(" +
                    "SELECT group_concat(concat(PARAMETER_MODE, ' ', PARAMETER_NAME, ' ', DTD_IDENTIFIER) ORDER BY ORDINAL_POSITION) " +
                    FROM_INFORMATION_SCHEMA_PARAMETERS +
                    "WHERE PARAMETER_MODE IS NOT NULL AND SPECIFIC_SCHEMA = r.ROUTINE_SCHEMA AND SPECIFIC_NAME = r.ROUTINE_NAME AND ROUTINE_TYPE = r.ROUTINE_TYPE" +
                    ") " +
                    "ELSE " +
                    "(" +
                    "SELECT group_concat(concat(PARAMETER_NAME, ' ', DTD_IDENTIFIER) ORDER BY ORDINAL_POSITION) " +
                    FROM_INFORMATION_SCHEMA_PARAMETERS +
                    "WHERE PARAMETER_MODE IS NOT NULL AND SPECIFIC_SCHEMA = r.ROUTINE_SCHEMA AND SPECIFIC_NAME = r.ROUTINE_NAME AND ROUTINE_TYPE = r.ROUTINE_TYPE" +
                    ") " +
                    "END AS args," +
                    "ROUTINE_BODY, " +
                    "SQL_DATA_ACCESS, ROUTINE_COMMENT " +
                    "FROM information_schema.routines r ORDER BY routine_name", db.getFullName()).run();
            while (q.next()) {
                for (Schema schema : db.getSchemas()) {
                    if (schema.getName().equals(q.getString("ROUTINE_SCHEMA"))) {

                        Function func = new Function(schema,
                                q.getString("ROUTINE_SCHEMA") + "." + q.getString("ROUTINE_NAME"),
                                q.getString("DATA_TYPE") == null ? "" : q.getString("DATA_TYPE"),
                                q.getString("args") == null ? "" : q.getString("args"),
                                q.getString("ROUTINE_DEFINITION"),
                                q.getString("ROUTINE_BODY"),
                                q.getString("SQL_DATA_ACCESS"),
                                q.getString("SECURITY_TYPE").matches("DEFINER"),
                                0,
                                0,
                                q.getString("ROUTINE_COMMENT"));
                        func.assignToSchema();
                        break;
                    }
                }
            }
            q.close();
            q.log(log);
        }
	}

	@Override
	public void loadTriggers(DB db) throws DBCommException {
		db.getTriggers().clear();
		int log = DesignGUI.getInfoPanel().write("Loading triggers of "+db.getFullName()+"...");
		Query q = new Query(SELECT
					+ "TRIGGER_NAME AS tgname_full,\n"
					+ "EVENT_OBJECT_TABLE AS relname_full,\n"
					+ "TRIGGER_NAME AS tgname,\n"
					+ "ACTION_STATEMENT AS def,\n"
					+ "ACTION_ORIENTATION,\n"
					+ "ACTION_TIMING,\n"
					+ "EVENT_MANIPULATION\n"
				+ "FROM information_schema.TRIGGERS\n"
				+ "WHERE TRIGGER_SCHEMA = '"+(dbAlias != null ? dbAlias : db.getName())+"'", db.getFullName()).run();
		while (q.next()) {
			Trigger t = new Trigger(db, q.getString("tgname_full"), q.getString("def"), "", "", true, "");

			String timing = q.getString("EVENT_MANIPULATION");
			boolean[] tVals = new boolean[]{false,false,false,false};
			tVals[0] = timing.contains("INSERT");
			tVals[1] = timing.contains("UPDATE");
			tVals[2] = timing.contains("DELETE");
			t.getBehavior().notifyChange(Trigger.Behavior.L_EVENT, tVals);
			t.getBehavior().notifyChange(Trigger.Behavior.L_TRIGGER, q.getString("ACTION_TIMING"));
			t.getBehavior().notifyChange(Trigger.Behavior.L_FOR_EACH, q.getString("ACTION_ORIENTATION"));

			t.setRelFuncByNameDef(db, q.getString("relname_full"), null, !db.getAssignLines());
		}
		q.close();
		q.log(log);
	}


	@Override
	public void loadSequences(DB db){}

	@Override
	public void loadIndexes(DB db) throws DBCommException {
		int log = DesignGUI.getInfoPanel().write("Loading indexes of "+db.getFullName()+"...");
		String sql = SELECT
							+ "TABLE_SCHEMA AS "+TABLE_SCHEMA+",\n"
							+ "TABLE_NAME AS "+TABLE_NAME+",\n"
							+ "INDEX_NAME AS index_name_full,\n"
							+ "CASE\n"
								+ "WHEN INDEX_NAME = 'PRIMARY' THEN concat('PRIMARY KEY (', group_concat(COLUMN_NAME ORDER BY SEQ_IN_INDEX), ')')\n"
								+ "WHEN NON_UNIQUE = 0 THEN concat('UNIQUE (', group_concat(COLUMN_NAME ORDER BY SEQ_IN_INDEX), ')')\n"
								+ "ELSE group_concat(COLUMN_NAME ORDER BY SEQ_IN_INDEX)\n"
							+ "END AS def,\n"
							+ "INDEX_TYPE AS amname,\n"
							+ "NON_UNIQUE = 0 AS indisunique,\n"
							+ "INDEX_NAME = 'PRIMARY' AS indisprimary,\n"
							+ (versionMajor >= 5.5f ? "INDEX_COMMENT" : "''")+" AS "+DESCRIPTION+"\n"
						+ "FROM information_schema.STATISTICS\n"
						+ "WHERE TABLE_SCHEMA = '"+(dbAlias != null ? dbAlias : db.getName())+"'\n"
						+ "GROUP BY TABLE_SCHEMA, TABLE_NAME, INDEX_NAME, NON_UNIQUE, INDEX_TYPE" +
				(versionMajor >= 5.5f ? ", INDEX_COMMENT" : "");
		Query q = new Query(sql, db.getFullName()).run();
		while (q.next()) {
			db.getSchemas().get(0).getRelations().stream()
					.filter(rel -> rel.getFullName().equals(q.getString(TABLE_NAME)))
					.forEach(rel -> new Index(rel,
							q.getString("index_name_full"),
							q.getString("def"),
							"",
							q.getString("amname"),
							new String[0],
							q.getBool("indisunique"),
							q.getBool("indisprimary"),
							false,
							q.getString(DESCRIPTION)
					).assignToRelation(rel));
		}
		q.close();
		q.log(log);
	}

	@Override
	public Result getServerStatus() throws DBCommException {
		Result ret;
		/*if(versionMajor >= 5.5f){
			ret = run("SELECT ID, DB, STATE, FROM_UNIXTIME(UNIX_TIMESTAMP(now())-TIME) AS START_TIME, TIME AS RUN_TIME, INFO "
					+ "FROM information_schema.PROCESSLIST "
					+ "WHERE INFO NOT LIKE 'SELECT ID, DB, STATE, FROM_UNIXTIME%'"
					+ "ORDER BY ID ASC");
		}else{*/
			ret = new Result(null, false);
			ret.cols.add(new ResultColumn("ID", "int"));
			ret.cols.add(new ResultColumn("DB", "text"));
			ret.cols.add(new ResultColumn("STATE", "text"));
			ret.cols.add(new ResultColumn("START_TIME", "text"));
			ret.cols.add(new ResultColumn("RUN_TIME", "int"));
			ret.cols.add(new ResultColumn("INFO", "text"));

			serverActivityTotalRows = 0;
			Query q = new Query("SHOW PROCESSLIST").run();
			while(q.next()){
				if(q.getString("Info") != null){
					ResultRow r = new ResultRow(ret.cols);

					r.add(q.getString("Id"));
					r.add(q.getString("db"));
					r.add(q.getString("State") == null ? "" : q.getString("State"));
					r.add(Config.DATE_TIME_FORMAT.format( new Date( new Date().getTime() - q.getInt("Time"))));
					r.add(q.getString("Time"));
					r.add(q.getString("Info"));

					ret.rows.add(r);
				}
				serverActivityTotalRows++;
			}
			q.close();
		//}
		ret.setColW(new int[]{80, 200, 80, 150, 80, 100});
		return ret;
	}

	@Override
	public String getQueryLocks(){
		return /*versionMajor >= 5.5f ? "SELECT count(*), 0 FROM information_schema.PROCESSLIST" : */ String.valueOf(serverActivityTotalRows);
	}

	@Override
	public String loadDDLFromDB(IModelElement element) throws DBCommException {
		return "";
	}

}
