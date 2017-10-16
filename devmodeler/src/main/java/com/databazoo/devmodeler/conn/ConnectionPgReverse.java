
package com.databazoo.devmodeler.conn;

import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.model.*;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.tools.Dbg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.databazoo.devmodeler.conn.ConnectionUtils.*;

/**
 * Reverse engineering setup for PostgreSQL
 *
 * @author bobus
 */
abstract class ConnectionPgReverse extends ConnectionPgForward {

	private static final String JOIN_PG_TYPE = "JOIN pg_type ct ON ct.oid = c.reltype\n";
	private static final String FROM_PG_CLASS_JOIN_PG_TYPE = "FROM pg_class c\n" + JOIN_PG_TYPE;
	private static final String RELPERSISTENCE_P = "c.relpersistence = 'p'";
	private static final String NOT_RELISTEMP = "NOT c.relistemp";
	private static final String ORDER_BY_RELNAME = "ORDER BY c.relname";
	private static final String SELECT_FULL_RELNAME_AS = "COALESCE((SELECT nspname||'.' FROM pg_namespace WHERE oid = ct.typnamespace AND nspname != 'public'), '')||c.relname AS ";
	private static final String SELECT_SCHEMA_NAME_AS = "(SELECT nspname FROM pg_namespace WHERE oid = ct.typnamespace) AS ";

	ConnectionPgReverse(String name, String host, String user, String pass) {
		super(name, host, user, pass);
	}

	@Override
	public void loadEnvironment() throws DBCommException {
		// load available languages
		int log = DesignGUI.getInfoPanel().write("Loading server environment...");
		Query q = new Query("SELECT lanname FROM pg_language", getDefaultDB()).run();
		while (q.next()) {
			if(!ConnectionPg.LANGUAGES.contains(q.getString("lanname"))){
				ConnectionPg.LANGUAGES.add(q.getString("lanname"));
			}
		}
		q.close();

		// load available datatypes
		q = new Query("SELECT n.nspname, typname AS " + TYPE + ", pg_catalog.format_type(t.oid, NULL) AS typefull " +
					"FROM pg_catalog.pg_type t " +
					"LEFT JOIN pg_catalog.pg_namespace n ON n.oid = t.typnamespace " +
					"WHERE (t.typrelid = 0 OR (SELECT c.relkind = 'c' FROM pg_catalog.pg_class c WHERE c.oid = t.typrelid)) AND " +
						"NOT EXISTS(SELECT 1 FROM pg_catalog.pg_type el WHERE el.oid = t.typelem AND el.typarray = t.oid) AND " +
						"pg_catalog.pg_type_is_visible(t.oid)", getDefaultDB()).run();
		while (q.next()) {
			if(!getDataTypes().containsKey(q.getString(TYPE))){
				//Dbg.info("Adding datatype "+q.getString("typefull")+" ("+q.getString(TYPE)+") to the list");
				getDataTypes().put(q.getString(TYPE), q.getString("typefull"));
			}
		}
		q.close();
		q.log(log);
	}

	@Override
	public Result getAllRows(Relation rel) throws DBCommException {
		String pk = rel.getPKey();
		if(pk.isEmpty()){
			return null;
		}else{
			String sql = "SELECT "+(pk.replace(COMMA, "||','||"))+" AS ddmpkcol, t.* FROM " + rel.getFullName() + " t ORDER BY "+pk;
			return new Result(new Query(sql, rel.getDB().getFullName()).run());
		}
	}

	@Override
	public CopyOnWriteArrayList<DB> getDatabases() throws DBCommException {
		int log = DesignGUI.getInfoPanel().write("Loading databases...");
		List<DB> dbs = new ArrayList<>();
		Query q = new Query("SELECT datname FROM pg_database WHERE NOT datistemplate ORDER BY datname", getDefaultDB()).run();
		while (q.next()) {
			dbs.add(new DB(Project.getCurrent(), this, q.getString(1)));
		}
		q.close();
		q.log(log);
		return new CopyOnWriteArrayList<>(dbs);
	}

	@Override
	public void loadSchemas(DB db) throws DBCommException {
		int log = DesignGUI.getInfoPanel().write("Loading schemata...");
		Query q = new Query("SELECT nspname, description "
				+ "FROM pg_namespace nsp "
				+ "LEFT JOIN pg_description des ON des.objoid=nsp.oid "
				+ "WHERE nspname != 'information_schema' AND nspname NOT LIKE 'pg\\_%' "
				+ "ORDER BY nspname", db.getName()).run();
		while (q.next()) {
			db.getSchemas().add(new Schema(db, q.getString("nspname"), q.getString(DESCRIPTION)));
		}
		q.close();
		q.log(log);
	}

	@Override
	public void loadRelations(DB db) throws DBCommException {
		int log = DesignGUI.getInfoPanel().write("Loading tables of "+db.getFullName()+"...");
		Query q = new Query(SELECT
				+ SELECT_SCHEMA_NAME_AS + TABLE_SCHEMA + ", "
				+ SELECT_FULL_RELNAME_AS + FULL_NAME + ", "
				+ "c.relname AS " + TABLE_NAME + ",\n"
				+ "c.relhasoids,\n"
				+ "COALESCE(c.reloptions, '{}') AS reloptions,\n"
				+ "c.relnatts,\n"
				+ "c.reltuples,\n"
				+ "obj_description(c.oid, 'pg_class') AS " + DESCRIPTION + ",\n"
				+ "COALESCE((SELECT nspname||'.' FROM pg_namespace WHERE oid = par_ct.typnamespace AND nspname != 'public'), '')||par_c.relname AS parent_full\n"
				+ FROM_PG_CLASS_JOIN_PG_TYPE
				+ "LEFT JOIN pg_inherits inh ON inhrelid = c.oid\n"
				+ "LEFT JOIN pg_class par_c ON inhparent = par_c.oid\n"
				+ "LEFT JOIN pg_type par_ct ON par_ct.oid = par_c.reltype\n"
				+ WHERE + (versionMajor >= 9.1 ? RELPERSISTENCE_P : NOT_RELISTEMP) + " AND c.relkind = 'r'\n"
				+ ORDER_BY_RELNAME, db.getFullName()).run();
		while (q.next()) {
			for(Schema schema: db.getSchemas()){
				if(schema.getName().equals(q.getString(TABLE_SCHEMA))) {
					Relation rel = new Relation(schema,
						q.getString(TABLE_NAME),
						q.getString(FULL_NAME),
						q.getInt("relnatts"),
						q.getInt("reltuples"),
						q.getBool("relhasoids"),
						q.getStringArray("reloptions"),
						q.getString(DESCRIPTION),
						q.getString("parent_full")
					);
					rel.assignToSchema();
					break;
				}
			}
		}
		q.close();
		q.log(log);
	}

	@Override
	public void loadRelationInfo(Relation rel) throws DBCommException {
		int log = DesignGUI.getInfoPanel().write("Loading table info...");
		final String sql = "SELECT\n"
				+ "pg_relation_size(COALESCE((SELECT nspname||'.' FROM pg_namespace WHERE oid = ct.typnamespace AND nspname != 'public'), '')||c.relname),\n"
				+ "pg_total_relation_size(COALESCE((SELECT nspname||'.' FROM pg_namespace WHERE oid = ct.typnamespace AND nspname != 'public'), '')||c.relname),\n"
				+ "(SELECT con1.conkey\n"
				+ "FROM pg_class cl\n"
				+ "JOIN pg_namespace ns ON cl.relnamespace = ns.oid\n"
				+ "JOIN pg_constraint con1 ON con1.conrelid = cl.oid\n"
				+ "WHERE\n"
				+ "cl.relname = c.relname AND\n"
				+ "ns.nspname = (SELECT nspname FROM pg_namespace WHERE oid = ct.typnamespace) AND\n"
				+ "con1.contype = 'p'\n"
				+ ") AS pkey,\n"
					/*+ "(SELECT count(*)\n"
						+ "FROM pg_class c1\n"
						+ "JOIN pg_type ct1 ON ct1.oid = c1.reltype\n"
						+ "JOIN pg_attribute a1 ON a1.attrelid = c1.oid\n"
						+ WHERE
						+ "c1.relkind = 'r' AND "
						+ "a1.attnum > 0 AND "
						+ "NOT a1.attisdropped AND "
						+ "COALESCE((SELECT nspname||'.' FROM pg_namespace WHERE oid = ct1.typnamespace AND nspname != 'public'), '')||c1.relname = COALESCE((SELECT nspname||'.' FROM pg_namespace WHERE oid = ct.typnamespace AND nspname != 'public'), '')||c.relname) AS cnt_attributes,\n"*/
				+ "CASE WHEN c.reltuples > " + Settings.getInt(Settings.L_PERFORM_REL_SIZE_LIMIT)
				+ " THEN c.reltuples ELSE (SELECT count(*) FROM ONLY " + escapeFullName(rel.getFullName()) + ") END AS reltuples\n"
				+ FROM_PG_CLASS_JOIN_PG_TYPE
				+ "WHERE COALESCE((SELECT nspname||'.' FROM pg_namespace WHERE oid = ct.typnamespace AND nspname != 'public'), '')||c.relname = '"
				+ rel.getFullName() + "' AND c.relkind = 'r'";
		Query q = new Query(sql, rel.getDB().getFullName()).run();
		while (q.next()) {
			rel.setCountRows(q.getInt("reltuples"));
			rel.setSizeTotal(q.getLong("pg_total_relation_size"));
			rel.setSizeIndexes(rel.getSizeTotal() - q.getLong("pg_relation_size"));
			rel.setPkCols(q.getIntArray("pkey"));
		}
		q.close();
		q.log(log);
	}

	@Override
	public void loadViews(DB db) throws DBCommException {
		int log = DesignGUI.getInfoPanel().write("Loading views of "+db.getFullName()+"...");
		Query q = new Query(SELECT
					+ SELECT_SCHEMA_NAME_AS + TABLE_SCHEMA + ", "
					+ SELECT_FULL_RELNAME_AS + FULL_NAME + ", "
					+ "c.relnatts,\n"
					+ "pg_get_viewdef(c.oid) AS def,\n"
					+ "obj_description(c.oid, 'pg_class') AS "+DESCRIPTION+",\n"
					+ "c.relkind AS type\n"
				+ FROM_PG_CLASS_JOIN_PG_TYPE
				+ WHERE + (versionMajor>=9.1 ? RELPERSISTENCE_P : NOT_RELISTEMP) + " AND c.relkind IN ('v', 'm')\n"
				+ ORDER_BY_RELNAME, db.getFullName()).run();
		while (q.next()) {
			for(Schema schema: db.getSchemas()){
				if(schema.getName().equals(q.getString(TABLE_SCHEMA))) {
					View view = new View(schema, q.getString(FULL_NAME), q.getString("type").equals("m"), q.getString("def"), q.getString(DESCRIPTION));
					view.assignToSchema();
					break;
				}
			}
		}
		q.close();
		q.log(log);
	}

	@Override
	public void loadSequences(DB db) throws DBCommException {
		if(versionMajor>=9.1) {
			int log = DesignGUI.getInfoPanel().write("Loading sequences of " + db.getFullName() + "...");
			String sql = SELECT
					+ "cs.nspname AS " + TABLE_SCHEMA + ", "
					+ "CASE WHEN cs.nspname = 'public' THEN c.relname ELSE cs.nspname||'.'||c.relname END AS " + FULL_NAME + ", "
					+ "obj_description(c.oid, 'pg_class') AS "+DESCRIPTION+",\n"
					+ "COALESCE((\n"
					+ "SELECT array_to_string(array_agg(attr), ',')\n"
					+ "FROM (\n"
					+ "SELECT ts.nspname||'.'||t.relname||'.'||a.attname AS attr\n"
					+ "FROM pg_attrdef ad\n"
					+ "JOIN pg_attribute a ON (attrelid, attnum) = (adrelid, adnum)\n"
					+ "JOIN pg_class t ON ad.adrelid = t.oid\n"
					+ "JOIN pg_type ct ON ct.oid = t.reltype\n"
					+ "JOIN pg_namespace ts ON ts.oid = ct.typnamespace\n"
					+ "WHERE ad.adsrc LIKE 'nextval('''|| CASE WHEN sequence_schema = 'public' THEN sequence_name ELSE sequence_schema||'.'||sequence_name END ||'''%'\n"
					+ ") AS k\n"
					+ "), '') AS depend,\n"
					+ "s.minimum_value,\n"
					+ "s.maximum_value,\n"
					+ "s.increment,\n"
					+ "s.cycle_option\n"
					+ FROM_PG_CLASS_JOIN_PG_TYPE
					+ "JOIN pg_namespace cs ON cs.oid = ct.typnamespace\n"
					+ "JOIN information_schema.sequences s ON (sequence_schema, sequence_name) = (cs.nspname, c.relname)\n"
					+ WHERE + (versionMajor >= 9.1 ? RELPERSISTENCE_P : NOT_RELISTEMP) + " AND c.relkind = 'S'\n"
					+ ORDER_BY_RELNAME;
			Query q = new Query(sql, db.getFullName()).run();
			while (q.next()) {
				for (Schema schema : db.getSchemas()) {
					if (schema.getName().equals(q.getString(TABLE_SCHEMA))) {
						Query lastValQuery = new Query(
								"SELECT last_value + increment_by AS current_value FROM " + escapeFullName(q.getString(FULL_NAME))).run();
						Sequence seq = new Sequence(schema,
								q.getString(FULL_NAME),
								q.getString("depend").isEmpty() ? new String[0] : q.getString("depend").split(COMMA),
								q.getString("increment"),
								q.getString("minimum_value"),
								q.getString("maximum_value"),
								lastValQuery.next() ? lastValQuery.getString("current_value") : "1",
								q.getString("cycle_option").equals("YES"),
								q.getString(DESCRIPTION));
						seq.assignToSchemaAndAttributes();
						break;
					}
				}
			}
			q.close();
			q.log(log);
		}
	}

	@Override
	public void loadAttributes(DB db) throws DBCommException {
		int log = DesignGUI.getInfoPanel().write("Loading attributes of "+db.getFullName()+"...");
		Query q = new Query(SELECT
					+ SELECT_SCHEMA_NAME_AS + TABLE_SCHEMA + ",\n"
					+ SELECT_FULL_RELNAME_AS + FULL_NAME + ",\n"
					+ "a.attname,\n"
					+ "CASE "
						+ "WHEN information_schema._pg_char_max_length(information_schema._pg_truetypid(a.*, t.*), information_schema._pg_truetypmod(a.*, t.*)) IS NOT NULL "
						+ "THEN t.typname || '('||information_schema._pg_char_max_length(information_schema._pg_truetypid(a.*, t.*), information_schema._pg_truetypmod(a.*, t.*))||')' "
						+ "ELSE t.typname "
						+ "END "
					+ "AS " + TYPE + ",\n"
					+ "NOT a.attnotnull AS is_null,\n"
					+ "(CASE WHEN a.atthasdef THEN (SELECT adsrc FROM pg_attrdef WHERE adrelid = c.oid AND adnum = a.attnum) ELSE '' END) AS def,\n"
					+ "a.attnum,"
					+ "a.attstorage,"
					+ "col_description(a.attrelid, a.attnum) AS "+DESCRIPTION+"\n"
				+ FROM_PG_CLASS_JOIN_PG_TYPE
				+ "JOIN pg_attribute a ON a.attrelid = c.oid\n"
				+ "JOIN pg_type t ON t.oid = a.atttypid\n"
				+ WHERE
					+ "c.relkind = 'r' AND "
					+ "a.attnum > 0 AND "
					+ "NOT a.attisdropped AND "
					+ "ct.typnamespace IN (SELECT oid FROM pg_namespace WHERE nspname='public' OR nspname IN (SELECT schema_name FROM information_schema.schemata))\n"
				+ "ORDER BY a.attnum", db.getFullName()).run();
		while (q.next()) {
			for(Schema schema: db.getSchemas()){
				if(schema.getName().equals(q.getString(TABLE_SCHEMA))) {
					for(Relation rel: schema.getRelations()){
						if(rel.getFullName().equals(q.getString(FULL_NAME))) {
							Attribute attr = new Attribute(rel,
									q.getString("attname"),
									q.getString(TYPE),
									q.getBool("is_null"),
									q.getInt("attnum"),
									q.getString("def"),
									q.getString("attstorage"),
									q.getString(DESCRIPTION));
							attr.assignToRels();
							break;
						}
					}
				}
			}
		}
		q.close();
		q.log(log);
	}

	@Override
	public void loadConstraints(DB db) throws DBCommException {
		int log = DesignGUI.getInfoPanel().write("Loading constraints of "+db.getFullName()+"...");
		Query q = new Query(SELECT
					+ "cc.conname,\n"
					+ "COALESCE((SELECT nspname||'.' FROM pg_namespace WHERE oid = ct.typnamespace AND nspname != 'public'), '')||cc.conname AS "+FULL_NAME+",\n"
					+ SELECT_FULL_RELNAME_AS + TABLE_NAME + ",\n"
					+ "(SELECT COALESCE((SELECT nspname||'.' FROM pg_namespace WHERE oid = ct.typnamespace AND nspname != 'public'), '')||c.relname FROM pg_class c JOIN pg_type ct ON ct.oid = c.reltype WHERE c.oid = cc.confrelid) AS rel2,\n"
					+ "(SELECT attname FROM pg_attribute WHERE attnum = ANY (cc.conkey) AND attrelid = cc.conrelid LIMIT 1) AS att1,\n"
					+ "(SELECT attname FROM pg_attribute WHERE attnum = ANY (cc.confkey) AND attrelid = cc.confrelid LIMIT 1) AS att2,\n"
					+ "cc.contype = 'f' AS isforeign,\n"
					+ "cc.confupdtype,\n"
					+ "cc.confdeltype,\n"
					+ "cc.consrc,\n"
					+ "obj_description(cc.oid, 'pg_constraint') AS description\n"
				+ "FROM pg_constraint cc\n"
				+ "JOIN pg_class c ON c.oid = cc.conrelid\n"
				+ JOIN_PG_TYPE
				+ "WHERE cc.connamespace IN (SELECT oid FROM pg_namespace WHERE nspname='public' OR nspname IN (SELECT schema_name FROM information_schema.schemata)) "
				+ "AND c.relkind = 'r' AND cc.contype IN ('f', 'c')\n"
				+ "ORDER BY c.relname, cc.conname", db.getFullName()).run();
		while (q.next()) {
			Constraint c;
			String fullName = q.getString(FULL_NAME);
			String description = q.getString(DESCRIPTION);
			String rel1 = q.getString(TABLE_NAME);
			String rel2 = q.getString("rel2");
			if(q.getBool("isforeign")){
				c = new Constraint(db, fullName, q.getString("confupdtype"), q.getString("confdeltype"), description);
				c.setRelsAttrsByName(db, rel1, rel2, q.getString("att1"), q.getString("att2"), !db.getAssignLines());
				c.checkSize();
			}else{
				CheckConstraint cc = new CheckConstraint(db, fullName, "CHECK "+q.getString("consrc"), description);
				cc.setRelByName(db, rel1, !db.getAssignLines());
			}
		}
		q.close();
		q.log(log);
	}

	@Override
	public void loadFunctions(DB db) throws DBCommException {
		int log = DesignGUI.getInfoPanel().write("Loading functions of "+db.getFullName()+"...");
		Query q = new Query(SELECT
					+ "(SELECT nspname FROM pg_namespace WHERE oid = pp.pronamespace) AS " + TABLE_SCHEMA + ", "
					+ "COALESCE((SELECT nspname||'.' FROM pg_namespace WHERE oid = pp.pronamespace AND nspname != 'public'), '')||proname AS "+FULL_NAME+","
					+ "proname, pg_get_function_arguments(pp.oid) AS proargs, prosrc, procost, prorows, prosecdef,\n"
					+ "pg_get_function_result(pp.oid) AS prorettype,\n"
					+ "(SELECT lanname FROM pg_language WHERE oid = pp.prolang) AS prolang,\n"
					+ "(CASE WHEN provolatile='v' THEN 'VOLATILE' WHEN provolatile='i' THEN 'IMMUTABLE' ELSE 'STABLE' END) AS volatility,\n"
					+ "obj_description(pp.oid, 'pg_proc') AS description\n"
				+ "FROM pg_proc pp\n"
				+ "WHERE pp.pronamespace IN (SELECT oid FROM pg_namespace WHERE nspname='public' OR nspname IN (SELECT schema_name FROM information_schema.schemata)) "
				+ "AND NOT pp.proisagg AND NOT pp.proiswindow\n"
				+ "ORDER BY proname, proargnames, proargtypes", db.getFullName()).run();
		while (q.next()) {
			for(Schema schema: db.getSchemas()){
				if(schema.getName().equals(q.getString(TABLE_SCHEMA))) {
					Function func = new Function(schema,
								 q.getString(FULL_NAME),
								 q.getString("prorettype"),
								 q.getString("proargs"),
								 q.getString("prosrc"),
								 q.getString("prolang"),
								 q.getString("volatility"),
								 q.getBool("prosecdef"),
								 q.getInt("procost"),
								 q.getInt("prorows"),
								 q.getString(DESCRIPTION));
					func.assignToSchema();
					break;
				}
			}
		}
		q.close();
		q.log(log);
	}

	@Override
	public void loadTriggers(DB db) throws DBCommException {
		int log = DesignGUI.getInfoPanel().write("Loading triggers of "+db.getFullName()+"...");
		Query q = new Query(SELECT
					+ "COALESCE((SELECT nspname||'.' FROM pg_namespace WHERE oid = ct.typnamespace AND nspname != 'public'), '')||tgname AS " + FULL_NAME + ",\n"
					+ SELECT_FULL_RELNAME_AS + TABLE_NAME + ",\n"
					+ "tgname,\n"
					+ "pg_get_triggerdef(tt.oid"+(versionMajor>=9.0 ? ", true" : "")+") AS def,\n"
					+ "tgenabled = 'D' AS disabled,\n"
					+ "tt.tgtype,\n"
					+ "tt.tgattr,\n"
					+ (versionMajor>=9.0 ? "tt.tgqual" : "NULL AS tgqual") + ",\n"
					+ "obj_description(tt.oid, 'pg_trigger') AS "+DESCRIPTION+"\n"
				+ "FROM pg_trigger tt\n"
				+ "JOIN pg_class c ON c.oid = tt.tgrelid\n"
				+ JOIN_PG_TYPE
				+ WHERE+(versionMajor>=9.0 ? "NOT tgisinternal" : "COALESCE((SELECT nspname FROM pg_namespace WHERE oid = ct.typnamespace AND nspname != 'public'), '') != 'pg_catalog' AND NOT tgisconstraint"), db.getFullName()).run();
		while (q.next()) {
			String when = "";
			if(q.getString("tgqual") != null && !q.getString("tgqual").isEmpty()){
				when = q.getString("def").replaceAll("(?i).*?WHEN\\s*\\((.*?)\\)\\s*EXECUTE PROCEDURE.*", "$1");
				Dbg.info("Orig: "+q.getString("def")+" Parsed: "+when);
			}
			Trigger t = new Trigger(db, q.getString(FULL_NAME), q.getString("def"), "", when, !q.getBool("disabled"), q.getString(DESCRIPTION));
			t.getBehavior().setTiming(q.getInt("tgtype"));
			t.setRelFuncByNameDef(db, q.getString(TABLE_NAME), q.getString("def"), !db.getAssignLines());
			t.checkColumns(q.getIntVector("tgattr"));
			t.checkSize();
		}
		q.close();
		q.log(log);
	}

	@Override
	public void loadIndexes(DB db) throws DBCommException {
		int log = DesignGUI.getInfoPanel().write("Loading indexes of "+db.getFullName()+"...");
		String sql;
		/*if(versionMajor >= 9.0){
			sql = "SELECT\n"
					+ SELECT_SCHEMA_NAME_AS + TABLE_SCHEMA + ",\n"
					+ SELECT_FULL_RELNAME_AS + FULL_NAME + ",\n"
					+ "COALESCE((SELECT nspname||'.' FROM pg_namespace WHERE oid = i_ct.typnamespace AND nspname != 'public'), '')||i_c.relname AS index_name_full,\n"
					+ "i_c.relname AS index_name,\n"
					+ "i.indisunique,\n"
					+ "i.indisprimary,\n"
					+ "i.indexrelid IN (SELECT conindid FROM pg_constraint) AS indisconstraint,\n"
					+ "CASE WHEN i.indexrelid IN (SELECT conindid FROM pg_constraint) THEN pg_get_constraintdef((SELECT oid FROM pg_constraint WHERE conindid = i.indexrelid AND conrelid = indrelid AND (contype = 'p') = i.indisprimary)) ELSE pg_get_indexdef(i.indexrelid) END AS def,\n"
					+ "CASE WHEN i.indexrelid IN (SELECT conindid FROM pg_constraint) THEN obj_description((SELECT oid FROM pg_constraint WHERE conindid = i.indexrelid AND conrelid = indrelid AND (contype = 'p') = i.indisprimary), 'pg_constraint') ELSE obj_description(i.indexrelid, 'pg_class') END AS description,\n"
					+ "i.indkey,\n"
					+ "i.indpred,\n"
					+ "am.amname,\n"
					+ "COALESCE(i_c.reloptions, '{}') AS reloptions,\n"
					+ "idx_scan AS number_of_scans,\n"
					+ "idx_tup_read AS tuples_read,\n"
					+ "idx_tup_fetch AS tuples_fetched\n"
				+ "FROM pg_index i\n"
				+ "JOIN pg_class c ON c.oid = i.indrelid\n"
				+ JOIN_PG_TYPE
				+ "JOIN pg_class i_c ON i_c.oid = i.indexrelid\n"
				+ "JOIN pg_type i_ct ON i_ct.oid = c.reltype\n"
				+ "JOIN pg_stat_all_indexes psai ON psai.indexrelid = i.indexrelid\n"
				+ "JOIN pg_am am ON am.oid = i_c.relam\n"
				+ "WHERE\n"
				+ "ct.typnamespace IN (SELECT oid FROM pg_namespace WHERE nspname='public' OR nspname IN (SELECT schema_name FROM information_schema.schemata)) AND\n"
				+ "c.relkind = 'r' AND\n"
				+ "i_c.relnamespace >= 2200 AND\n"
				+ "ct.typnamespace IN (SELECT oid FROM pg_namespace WHERE nspname='public' OR nspname IN (SELECT schema_name FROM information_schema.schemata))\n"
				+ "ORDER BY COALESCE((SELECT nspname||'.' FROM pg_namespace WHERE oid = ct.typnamespace AND nspname != 'public'), '')||c.relname, i_c.relname";
		}else{*/
			sql = "SELECT *,\n"
					+ "indisconstraint_oid IS NOT NULL AS indisconstraint,\n"
					+ "CASE WHEN indisconstraint_oid IS NOT NULL THEN pg_get_constraintdef(indisconstraint_oid) ELSE pg_get_indexdef(indexrelid) END AS def,\n"
					+ "CASE WHEN indisconstraint_oid IS NOT NULL THEN obj_description(indisconstraint_oid, 'pg_constraint') ELSE obj_description(indexrelid, 'pg_class') END AS "+DESCRIPTION+"\n"
				+ "FROM (\n"
					+ SELECT
						+ "(SELECT nspname FROM pg_namespace WHERE oid = i_ct.typnamespace) AS " + TABLE_SCHEMA + ",\n"
						+ SELECT_FULL_RELNAME_AS + TABLE_NAME + ",\n"
						+ "COALESCE((SELECT nspname||'.' FROM pg_namespace WHERE oid = i_ct.typnamespace AND nspname != 'public'), '')||i_c.relname AS "+FULL_NAME+",\n"
						+ "i_c.relname AS index_name,\n"
						+ "i.indexrelid,\n"
						+ "i.indisunique,\n"
						+ "i.indisprimary,\n"
						+ "(\n"
							+ "SELECT pgcc.oid\n"
							+ "FROM pg_constraint pgcc\n"
							+ "JOIN pg_class pgc ON pgc.oid = pgcc.conrelid\n"
							+ "JOIN pg_type ct ON ct.oid = pgc.reltype\n"
							+ "WHERE ct.typnamespace = i_ct.typnamespace AND pgcc.conname = i_c.relname\n"
							+ "LIMIT 1\n"
						+ ") AS indisconstraint_oid,\n"
						+ "i.indkey,\n"
						+ "i.indpred,\n"
						+ "am.amname,\n"
						+ "COALESCE(i_c.reloptions, '{}') AS reloptions,\n"
						+ "idx_scan AS number_of_scans,\n"
						+ "idx_tup_read AS tuples_read,\n"
						+ "idx_tup_fetch AS tuples_fetched\n"
					+ "FROM pg_index i\n"
					+ "JOIN pg_class c ON c.oid = i.indrelid\n"
					+ JOIN_PG_TYPE
					+ "JOIN pg_class i_c ON i_c.oid = i.indexrelid\n"
					+ "JOIN pg_type i_ct ON i_ct.oid = c.reltype\n"
					+ "JOIN pg_stat_all_indexes psai ON psai.indexrelid = i.indexrelid\n"
					+ "JOIN pg_am am ON am.oid = i_c.relam\n"
					+ "WHERE\n"
					+ "i_ct.typnamespace IN (SELECT oid FROM pg_namespace WHERE nspname='public' OR nspname IN (SELECT schema_name FROM information_schema.schemata)) AND\n"
					+ "c.relkind = 'r' AND\n"
					+ "i_c.relnamespace >= 2200\n"
				+ ") AS k\n"
				+ "ORDER BY " + TABLE_NAME + ", index_name";
		//}
		Query q = new Query(sql, db.getFullName()).run();
		while (q.next()) {
			db.getSchemas().stream()
					.filter(schema -> schema.getName().equals(q.getString(TABLE_SCHEMA)))
					.forEach(schema -> schema.getRelations().stream()
							.filter(rel -> rel.getFullName().equals(q.getString(TABLE_NAME)))
							.forEach(rel -> new Index(rel,
									q.getString(FULL_NAME),
									q.getString("def"),
									q.getString("indpred"),
									q.getString("amname"),
									q.getStringArray("reloptions"),
									q.getBool("indisunique"),
									q.getBool("indisprimary"),
									q.getBool("indisconstraint"),
									q.getString(DESCRIPTION)
							).assignToRelation(rel)));
		}
		q.close();
		q.log(log);
	}

	@Override
	public Result getServerStatus() throws DBCommException {
		Result ret;
		String appname = "";
		int[] cols;
		if(versionMajor != null && versionMajor >= 9.0f){
			appname = ", application_name AS appname";
			cols = new int[]{70, 170, 200, 60, 150, 100, 100};
		}else{
			cols = new int[]{70, 200, 60, 150, 100, 100};
		}
		if(versionMajor != null && versionMajor >= 9.2f){
			String waiting;
			if (versionMajor >= 9.6f) {
				waiting = "wait_event_type || ' ' || wait_event AS waiting";
				cols[3] = 150;
			} else {
				waiting = "waiting";
			}
			ret = run("SELECT pid AS procpid" + appname + ", datname, " + waiting +
						", date_trunc('seconds', xact_start)::timestamp AS start_time, " +
						"CASE WHEN xact_start IS NOT NULL AND now()>xact_start THEN (now()-xact_start) END AS run_time, " +
						"query AS current_query " +
					"FROM pg_stat_get_activity(NULL) " +
					"LEFT JOIN pg_database ON oid = datid " +
					WHERE +
						"state = 'active' AND " +
						"query NOT LIKE 'SELECT pid AS procpid" + appname + ", datname, wait%' " +
					"ORDER BY xact_start ASC");
		}else{
			ret = run("SELECT procpid" + appname + ", datname, waiting, date_trunc('seconds', query_start)::timestamp AS start_time, " +
						"CASE WHEN query_start IS NOT NULL AND now()>query_start THEN (now()-query_start) END AS run_time, " +
						"current_query " +
					"FROM pg_stat_get_activity(NULL) "+
					"LEFT JOIN pg_database ON oid = datid "+
					WHERE+
						"current_query != '<IDLE>' AND "+
						"current_query NOT LIKE 'SELECT procpid"+appname+", datname, waiting, date_trunc%' "+
					"ORDER BY query_start ASC");
		}
		ret.setColW(cols);
		return ret;
	}

	@Override
	public String loadDDLFromDB(IModelElement element) throws DBCommException {
		return "";
	}
}
