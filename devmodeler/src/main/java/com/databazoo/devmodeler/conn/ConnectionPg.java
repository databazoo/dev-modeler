package com.databazoo.devmodeler.conn;

import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.DataTypes;
import com.databazoo.devmodeler.model.explain.ExplainOperation;
import com.databazoo.tools.Dbg;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Forward + reverse engineering setup for PostgreSQL
 *
 * @author bobus
 */
public class ConnectionPg extends ConnectionPgReverse {
	private static final DataTypes DATATYPES = new DataTypesPg();
	static final List<String> LANGUAGES = new ArrayList<>(Arrays.asList("sql", "plpgsql", "c", "internal"));

	public ConnectionPg(String name, String host, String user, String pass) {
		super(name, host, user, pass);
		setDefaultSchema(PUBLIC_SCHEMA);
	}
	public ConnectionPg(String name, String host, String user, String pass, boolean autocheck) {
		super(name, host, user, pass);
		if(autocheck) {
			runStatusCheck();
		}
		setDefaultSchema(PUBLIC_SCHEMA);
	}
	public ConnectionPg(String name, String host, String user, String pass, int type, boolean autocheck) {
		super(name, host, user, pass);
		if(autocheck) {
			runStatusCheck();
		}
		this.type = type;
		setDefaultSchema(PUBLIC_SCHEMA);
	}

	@Override
	public boolean isSupported(SupportedElement elemType) {
		switch(elemType){
			case SCHEMA: return true;
			case SCHEMA_CREATE: return true;
			case RELATION: return true;
			case RELATION_OID: return true;
			case RELATION_REPAIR: return false;
			case RELATION_VACUUM: return true;
			case RELATION_INHERIT: return true;
			case RELATION_STORAGE: return false;
			case RELATION_COLLATION: return false;
			case ATTRIBUTE: return true;
			case ATTRIBUTE_STORAGE: return true;
			case ATTRIBUTE_COLLATION: return false;
			case PRIMARY_KEY: return true;
			case PRIMARY_KEY_NAMED: return true;
			case PRIMARY_KEY_SERIAL: return true;
			case FOREIGN_KEY: return true;
			case FOREIGN_KEY_COMMENT: return true;
			case FOREIGN_KEY_ON_UPDATE: return true;
			case CHECK_CONSTRAINT: return true;
			case UNIQUE_CONSTRAINT: return true;
			case INDEX: return true;
			case INDEX_UNIQUE: return true;
			case INDEX_CONDITIONAL: return true;
			case INDEX_COMMENT: return true;
			case TRIGGER: return true;
			case TRIGGER_BODY: return false;
			case TRIGGER_STATEMENT: return true;
			case TRIGGER_ATTRIBUTES: return true;
			case TRIGGER_DISABLE: return true;
			case FUNCTION: return true;
			case FUNCTION_SQL_ONLY: return false;
			case PACKAGE: return false;
			case VIEW: return true;
			case VIEW_MATERIALIZED: return true;
			case VACUUM: return true;
			case OPTIMIZE: return false;
			case SEQUENCE: return true;
			case ALL_UPPER: return false;
			case TRANSACTION_AUTO_ROLLBACK: return true;
			case GRAPHICAL_EXPLAIN: return true;
			case GENERATE_DDL: return false;
			default: return true;
		}
	}

	@Override
	public String getPrecisionForType(String type){
		return null;
	}

	@Override
	public String getTypeName(){
		return "PostgreSQL";
	}

	@Override
	public String getProtocol(){
		return "jdbc:postgresql://";
	}

	@Override
	public String getConnURL(String dbName){
		return getProtocol() + host + "/" + dbName + "?connectTimeout="+INIT_TIMEOUT+"&allowMultiQueries=true&characterEncoding=UTF-8&ApplicationName="+Config.APP_NAME_BASE;
	}

	@Override
	public String getDefaultDB(){
		return "postgres";
	}

	@Override
	public String getPresetFucntionArgs(boolean isTrigger){
		return "";
	}
	@Override
	public String getPresetFucntionReturn(boolean isTrigger){
		return isTrigger ? DataTypesPg.L_TRIGGER : "TABLE ()";
	}
	@Override
	public String getPresetFucntionSource(boolean isTrigger){
		return "\nDECLARE\n\nBEGIN\n\t"+(isTrigger ? "RETURN NEW;" : "/** TODO **/")+"\nEND\n";
	}

	@Override
	public String getCleanError(String message) {
		return message.replaceAll("org.postgresql.util.PSQLException: (.*)", "$1");
	}

	@Override
	public Point getErrorPosition(String query, String message){
		String regex = "(?ims)^.*: ([0-9]+)$";
		if(message.matches(regex)){
			return new Point(Integer.parseInt(message.replaceAll(regex, "$1"))-1, 0);
		}
		return null;
	}

	@Override
	public String[] getLanguages(){
		return LANGUAGES.toArray(new String[0]);
	}
	@Override
	public String[] getAccessMethods(){
		return new String[]{"btree", "hash", "gist", "gin"};
	}
	@Override
	public String[] getConstraintUpdateActions(){
		return new String[]{"NO ACTION", "RESTRICT", "CASCADE", "SET NULL", "SET DEFAULT"};
	}
	@Override
	public String[] getConstraintDeleteActions(){
		return new String[]{"NO ACTION", "RESTRICT", "CASCADE", "SET NULL", "SET DEFAULT"};
	}
	@Override
	public DataTypes getDataTypes(){
		/*if(DATATYPES == null){
			DATATYPES = new DataTypesPg();
		}*/
		return DATATYPES;
	}
	@Override
	public String[] getStorageEngines(){
		return new String[0];
	}
	@Override
	public String[] getCollations(){
		return new String[0];
	}

	@Override
	public ExplainOperation getExplainTree(Result r){
		ExplainOperation root = super.getExplainTree(r);
		ExplainOperation lastElem = root;
		ExplainOperation currElem;
		for(int i=0; i<r.getRowCount(); i++){
			String value = (String)r.getRow(i).vals.get(0);
			if(i==0){
				currElem = new ExplainOperation(value.replaceAll("(.*)  \\((.*)\\)", "$1"), value.replaceAll("(.*)  \\((.*)\\)", "$2").split(" "), 1);
				lastElem.append(currElem);
				lastElem = currElem;

			}else if(value.contains("  ->  ")){
				String match = "( *)  ->  (.*)  \\((.*)\\)";

				int level = value.replaceAll(match, "$1").length()/6+2;
				currElem = new ExplainOperation(value.replaceAll(match, "$2"), value.replaceAll(match, "$3").split(" "), level);
				lastElem.getParentNode(level-1).append(currElem);
				lastElem = currElem;
			}else{
				lastElem.extraInfo.add(value);
			}
		}
		return root;
	}

	@Override
	public Result run(String sql) throws DBCommException {
		if(Connection.POSTGRES_SUPPORTED){
			return new Result(new Query(sql).run());
		}else{
			DesignGUI.getInfoPanel().writeFailed(DesignGUI.getInfoPanel().write(""), getTypeName()+" driver not loaded");
			return new Result(null);
		}
	}

	@Override
	public Result run(String sql, DB db) throws DBCommException {
		if(Connection.POSTGRES_SUPPORTED){
			return new Result(new Query(sql, db.getName()).run());
		}else{
			DesignGUI.getInfoPanel().writeFailed(DesignGUI.getInfoPanel().write(""), getTypeName()+" driver not loaded");
			return new Result(null);
		}
	}

	@Override
	protected void runVersionCheck(){
		if(CONN_VERSIONS.get(getProtocol() + getHost()) == null){
			try {
				Query q = new Query("SELECT version()", getDefaultDB()).run();
				while(q.next()){
					version = q.getString(1).replaceAll("PostgreSQL ", "").replaceAll("([0-9.]+).*", "$1");
					versionMajor = Float.parseFloat(version.replaceAll("([0-9]+.[0-9]+).*", "$1"));
				}
				q.close();
				CONN_VERSIONS.put(getProtocol() + getHost(), versionMajor);
				if(DesignGUI.getInfoPanel() != null && !host.contains("devmodeler.com")){
					DesignGUI.getInfoPanel().write(getTypeName()+" version: "+versionMajor);
				}
			} catch (DBCommException ex) {
				Dbg.info(ex);
			}
		}else{
			versionMajor = CONN_VERSIONS.get(getProtocol() + getHost());
		}
	}

	private static class DataTypesPg extends DataTypes {

		// God knows what
		private static final String L_ACLITEM = "aclitem";
		private static final String L_CID = "cid";
		private static final String L_CIDR = "cidr";
		private static final String L_PG_NODE_TREE = "pg_node_tree";
		private static final String L_REFCURSOR = "refcursor";
		private static final String L_REGCLASS = "regclass";
		private static final String L_REGCONFIG = "regconfig";
		private static final String L_REGDICTIONARY = "regdictionary";
		private static final String L_REGOPER = "regoper";
		private static final String L_REGOPERATOR = "regoperator";
		private static final String L_REGPROC = "regproc";
		private static final String L_REGPROCEDURE = "regprocedure";
		private static final String L_REGTYPE = "regtype";
		private static final String L_SMGR = "smgr";
		private static final String L_TID = "tid";
		private static final String L_TSQUERY = "tsquery";
		private static final String L_TSVECTOR = "tsvector";
		private static final String L_TXID_SNAPSHOT = "txid_snapshot";
		private static final String L_XID = "xid";

		// Geometry
		private static final String L_BOX = "box";
		private static final String L_CIRCLE = "circle";
		private static final String L_GTSVECTOR = "gtsvector";
		private static final String L_INET = "inet";
		private static final String L_LINE = "line";
		private static final String L_LSEG = "lseg";
		private static final String L_MACADDR = "macaddr";
		private static final String L_POINT = "point";
		private static final String L_POLYGON = "polygon";
		private static final String L_RECORD = "record";
		private static final String L_TRIGGER = "trigger";


		DataTypesPg(){
			put(L_ABSTIME, 				L_ABSTIME);
			put(L_ACLITEM, 				L_ACLITEM);
			put(L_BIT,					L_BIT);
			put(L_BOOL,					BASE_TYPE_BOOLEAN);
			put(L_BOX, 					L_BOX);
			put(L_BPCHAR,				L_CHARACTER);
			put(L_BYTEA, 				L_BYTEA);
			put(BASE_TYPE_CHAR,			L_CHARACTER);
			put(L_CID, 					L_CID);
			put(L_CIDR, 				L_CIDR);
			put(L_CIRCLE, 				L_CIRCLE);
			put(BASE_TYPE_DATE,			BASE_TYPE_DATE);
			put(L_FLOAT4,				L_REAL);
			put(L_FLOAT8, 				L_DOUBLE_PRECISION);
			put(L_GTSVECTOR, 			L_GTSVECTOR);
			put(L_INET, 				L_INET);
			put(L_INT,					BASE_TYPE_INTEGER);
			put(L_INT2,					BASE_TYPE_SMALLINT);
			put(L_INT4,					BASE_TYPE_INTEGER);
			put(L_INT8,					BASE_TYPE_BIGINT);
			put(BASE_TYPE_INTERVAL,		BASE_TYPE_INTERVAL);
			put(L_LINE, 				L_LINE);
			put(L_LSEG, 				L_LSEG);
			put(L_MACADDR, 				L_MACADDR);
			put(L_MONEY,				L_MONEY);
			put(L_NAME, 				L_NAME);
			put(BASE_TYPE_NUMERIC,		BASE_TYPE_NUMERIC);
			put(L_OID, 					L_OID);
			put(L_PATH, 				L_PATH);
			put(L_PG_NODE_TREE, 		L_PG_NODE_TREE);
			put(L_POINT, 				L_POINT);
			put(L_POLYGON, 				L_POLYGON);
			put(L_RECORD, 				L_RECORD);
			put(L_REFCURSOR, 			L_REFCURSOR);
			put(L_REGCLASS, 			L_REGCLASS);
			put(L_REGCONFIG, 			L_REGCONFIG);
			put(L_REGDICTIONARY, 		L_REGDICTIONARY);
			put(L_REGOPER, 				L_REGOPER);
			put(L_REGOPERATOR, 			L_REGOPERATOR);
			put(L_REGPROC, 				L_REGPROC);
			put(L_REGPROCEDURE, 		L_REGPROCEDURE);
			put(L_REGTYPE, 				L_REGTYPE);
			put(L_RELTIME, 				L_RELTIME);
			put(L_SERIAL4, 				L_SERIAL);
			put(L_SERIAL8, 				L_BIGSERIAL);
			put(L_SMGR, 				L_SMGR);
			put(BASE_TYPE_TEXT,			BASE_TYPE_TEXT);
			put(L_TID, 					L_TID);
			put(BASE_TYPE_TIME,			BASE_TYPE_TIME);
			put(BASE_TYPE_TIMESTAMP,	BASE_TYPE_TIMESTAMP);
			put(L_TIMESTAMPTZ, 			L_TIMESTAMP_WITH_TIME_ZONE);
			put(L_TIMETZ, 				L_TIME_WITH_TIME_ZONE);
			put(L_TINTERVAL, 			L_TINTERVAL);
			put(L_TRIGGER, 				L_TRIGGER);
			put(L_TSQUERY, 				L_TSQUERY);
			put(L_TSVECTOR, 			L_TSVECTOR);
			put(L_TXID_SNAPSHOT, 		L_TXID_SNAPSHOT);
			put(L_UUID, 				L_UUID);
			put(L_VARBIT, 				L_BIT_VARYING);
			put(BASE_TYPE_VARCHAR,		L_CHARACTER_VARYING);
			put(L_XID, 					L_XID);
			put(BASE_TYPE_XML,			BASE_TYPE_XML);
		}

		@Override
		public String getTranslatedType(String type, String precision) {
			switch (type){
				case BASE_TYPE_BOOLEAN: return BASE_TYPE_BOOLEAN;

				case BASE_TYPE_CHAR: return L_CHARACTER + precision;

				case BASE_TYPE_VARCHAR: return L_CHARACTER_VARYING + precision;

				case BASE_TYPE_BLOB: return L_BYTEA + precision;
				case BASE_TYPE_TEXT: return BASE_TYPE_TEXT + precision;

				case BASE_TYPE_FLOAT: return BASE_TYPE_FLOAT;
				case BASE_TYPE_DOUBLE: return L_DOUBLE_PRECISION;

				case BASE_TYPE_DECIMAL: return BASE_TYPE_NUMERIC;
				case BASE_TYPE_SMALLINT: return BASE_TYPE_SMALLINT;
				case BASE_TYPE_INTEGER: return BASE_TYPE_INTEGER;
				case BASE_TYPE_BIGINT: return BASE_TYPE_BIGINT;
				case BASE_TYPE_NUMERIC: return BASE_TYPE_NUMERIC;

				case BASE_TYPE_DATE: return BASE_TYPE_DATE;
				case BASE_TYPE_TIME: return BASE_TYPE_TIME;

				case BASE_TYPE_INTERVAL: return BASE_TYPE_INTERVAL;

				case BASE_TYPE_TIMESTAMP: return BASE_TYPE_TIMESTAMP;

				case BASE_TYPE_ROWID: return L_SERIAL;
				case BASE_TYPE_BINARY: return L_BYTEA + precision;

				case BASE_TYPE_XML: return BASE_TYPE_XML;

				default: return type + precision;
			}
		}
	}
}
