
package com.databazoo.devmodeler.conn;

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
 * Forward + reverse engineering setup for MySQL
 *
 * @author bobus
 */
public class ConnectionMy extends ConnectionMyReverse {
	private static final DataTypes DATATYPES = new DataTypesMy();
	static final List<String> ENGINES = new ArrayList<>(Arrays.asList("MyISAM", "InnoDB", "CSV", "MEMORY", "ARCHIVE"));
	static final List<String> COLLATIONS = new ArrayList<>(Arrays.asList(
		"armscii8_bin", "armscii8_general_ci", "ascii_bin", "ascii_general_ci",
		"big5_bin", "big5_chinese_ci", "binary",
		"cp1250_bin", "cp1250_croatian_ci", "cp1250_czech_cs", "cp1250_general_ci",
		"cp1251_bin", "cp1251_bulgarian_ci", "cp1251_general_ci", "cp1251_general_cs", "cp1251_ukrainian_ci",
		"cp1256_bin", "cp1256_general_ci",
		"cp1257_bin", "cp1257_general_ci", "cp1257_lithuanian_ci",
		"cp850_bin", "cp850_general_ci",
		"cp852_bin", "cp852_general_ci",
		"cp866_bin", "cp866_general_ci",
		"cp932_bin", "cp932_japanese_ci",
		"dec8_bin", "dec8_swedish_ci",
		"eucjpms_bin", "eucjpms_japanese_ci",
		"euckr_bin", "euckr_korean_ci",
		"gb2312_bin", "gb2312_chinese_ci",
		"gbk_bin", "gbk_chinese_ci",
		"geostd8_bin", "geostd8_general_ci",
		"greek_bin", "greek_general_ci",
		"hebrew_bin", "hebrew_general_ci",
		"hp8_bin", "hp8_english_ci",
		"keybcs2_bin", "keybcs2_general_ci",
		"koi8r_bin", "koi8r_general_ci", "koi8u_bin", "koi8u_general_ci",
		"latin1_bin", "latin1_danish_ci", "latin1_general_ci", "latin1_general_cs", "latin1_german1_ci", "latin1_german2_ci", "latin1_spanish_ci", "latin1_swedish_ci",
		"latin2_bin", "latin2_croatian_ci", "latin2_czech_cs", "latin2_general_ci", "latin2_hungarian_ci",
		"latin5_bin", "latin5_turkish_ci",
		"latin7_bin", "latin7_estonian_cs", "latin7_general_ci", "latin7_general_cs",
		"macce_bin", "macce_general_ci", "macroman_bin", "macroman_general_ci",
		"sjis_bin", "sjis_japanese_ci",
		"swe7_bin", "swe7_swedish_ci",
		"tis620_bin", "tis620_thai_ci",
		"ucs2_bin", "ucs2_czech_ci", "ucs2_danish_ci", "ucs2_esperanto_ci", "ucs2_estonian_ci", "ucs2_general_ci", "ucs2_hungarian_ci", "ucs2_icelandic_ci", "ucs2_latvian_ci", "ucs2_lithuanian_ci", "ucs2_persian_ci", "ucs2_polish_ci", "ucs2_romanian_ci", "ucs2_roman_ci", "ucs2_slovak_ci", "ucs2_slovenian_ci", "ucs2_spanish2_ci", "ucs2_spanish_ci", "ucs2_swedish_ci", "ucs2_turkish_ci", "ucs2_unicode_ci",
		"ujis_bin", "ujis_japanese_ci",
		"utf8_bin", "utf8_czech_ci", "utf8_danish_ci", "utf8_esperanto_ci", "utf8_estonian_ci", "utf8_general_ci", "utf8_hungarian_ci", "utf8_icelandic_ci", "utf8_latvian_ci", "utf8_lithuanian_ci", "utf8_persian_ci", "utf8_polish_ci", "utf8_romanian_ci", "utf8_roman_ci", "utf8_slovak_ci", "utf8_slovenian_ci", "utf8_spanish2_ci", "utf8_spanish_ci", "utf8_swedish_ci", "utf8_turkish_ci", "utf8_unicode_ci"
	));
	private static final String ROWS_EQ = "rows=";

	public ConnectionMy(String name, String host, String user, String pass) {
		super(name, host, user, pass);
	}
	public ConnectionMy(String name, String host, String user, String pass, int type) {
		super(name, host, user, pass);
		this.type = type;
	}

	@Override
	public boolean isSupported(SupportedElement elemType) {
		switch(elemType){
			case DATABASE_RENAME: return false;
			case SCHEMA: return false;
			case SCHEMA_CREATE: return false;
			case RELATION: return true;
			case RELATION_OID: return false;
			case RELATION_REPAIR: return true;
			case RELATION_VACUUM: return false;
			case RELATION_INHERIT: return false;
			case RELATION_STORAGE: return true;
			case RELATION_COLLATION: return true;
			case ATTRIBUTE: return true;
			case ATTRIBUTE_STORAGE: return false;
			case ATTRIBUTE_COLLATION: return true;
			case PRIMARY_KEY: return true;
			case PRIMARY_KEY_NAMED: return false;
			case PRIMARY_KEY_SERIAL: return true;
			case FOREIGN_KEY: return true;
			case FOREIGN_KEY_COMMENT: return false;
			case FOREIGN_KEY_ON_UPDATE: return true;
			case CHECK_CONSTRAINT: return false;
			case UNIQUE_CONSTRAINT: return false;
			case INDEX: return true;
			case INDEX_UNIQUE: return true;
			case INDEX_CONDITIONAL: return false;
			case INDEX_COMMENT: return true;
			case TRIGGER: return true;
			case TRIGGER_BODY: return true;
			case TRIGGER_STATEMENT: return false;
			case TRIGGER_ATTRIBUTES: return false;
			case TRIGGER_DISABLE: return false;
			case FUNCTION: return true;
			case FUNCTION_SQL_ONLY: return true;
			case PACKAGE: return false;
			case VIEW: return true;
			case VIEW_MATERIALIZED: return false;
			case VACUUM: return false;
			case OPTIMIZE: return true;
			case SEQUENCE: return false;
			case ALL_UPPER: return false;
			case TRANSACTION_AUTO_ROLLBACK: return false;
			case GRAPHICAL_EXPLAIN: return true;
			case GENERATE_DDL: return false;
			default: return true;
		}
	}

	@Override
	public String getPrecisionForType(String type){
		return DATATYPES.getPrecisionForType(type);
	}

	@Override
	public String getTypeName(){
		return "MySQL";
	}

	@Override
	public String getProtocol(){
		return "jdbc:mysql://";
	}

	@Override
	public String getConnURL(String dbName){
		return getProtocol() + host + "/" + dbName + "?connectTimeout="+Connection.INIT_TIMEOUT+"&allowMultiQueries=true&characterEncoding=UTF-8";
	}

	@Override
	public String getDefaultDB(){
		return "information_schema";
	}

	@Override
	public String getCleanError(String message) {
		return message.
				replaceAll("com.mysql.jdbc.exceptions.jdbc4.[a-zA-Z0-9]+Exception: (.*)", "$1").
				replaceAll("java.sql.SQLException: (.*)", "$1").
				replaceAll("java.sql.SQLWarning: (.*)", "$1");
	}

	@Override
	public Point getErrorPosition(String query, String message){
		String regex = "(?ims)^.*'(.+)'.+([0-9]+)$";
		if(message.matches(regex)){
			String errText = message.replaceAll(regex, "$1");
			int errLine = Integer.parseInt(message.replaceAll(regex, "$2"));

			int pos = 0;
			for(int i=1; i<errLine; i++){
				pos = query.indexOf('\n', pos)+1;
			}
			pos = query.indexOf(errText, pos);
			return new Point(pos, pos+errText.length());
		}
		return null;
	}

	@Override
	public String[] getLanguages(){
		throw new UnsupportedOperationException("Languages are not supported by "+getTypeName());
	}
	@Override
	public String[] getAccessMethods(){
		return new String[]{"btree", "hash", "fulltext"};
	}
	@Override
	public String[] getConstraintUpdateActions(){
		return new String[]{"NO ACTION", "RESTRICT", "CASCADE", "SET NULL"};
	}
	@Override
	public String[] getConstraintDeleteActions(){
		return new String[]{"NO ACTION", "RESTRICT", "CASCADE", "SET NULL"};
	}
	@Override
	public DataTypes getDataTypes(){
		/*if(DATATYPES == null){
			DATATYPES = new DataTypesMy();
		}*/
		return DATATYPES;
	}
	@Override
	public String[] getStorageEngines(){
		return ConnectionMy.ENGINES.toArray(new String[0]);
	}
	@Override
	public String[] getCollations(){
		return ConnectionMy.COLLATIONS.toArray(new String[0]);
	}
	@Override
	public String getDefaultStorageEngine(){
		return "InnoDB";
	}

	@Override
	public ExplainOperation getExplainTree(Result result){
		ExplainOperation root = super.getExplainTree(result);
		ExplainOperation lastElem, currElem;
		int rowCount = result.getRowCount();
		for(int i=0; i<rowCount; i++){
			ResultRow r = result.getRow(i);
			String table = (String) r.vals.get(2);
			String joinType = (String) r.vals.get(3);
			String keyUsed = (String) r.vals.get(5);
			String keyLength = (String) r.vals.get(6);
			String reference = (String) r.vals.get(7);
			int readRows = r.vals.get(8)!=null ? Integer.parseInt((String) r.vals.get(8)) : 0;
			String operations = (String) r.vals.get(9);

			lastElem = root;
			int level = 1;
			if(operations != null && !operations.isEmpty()){
				String[] using = operations.split("; ");
				for(int j=using.length-1; j>=0; j--){
					if(using[j].equals("Using index") && (joinType.equals("index") || keyUsed != null)){
						continue;
					}
					currElem = new ExplainOperation(
							using[j].replace("Using ", ""),
							new String[]{ ROWS_EQ + readRows},
							level);
					lastElem.append(currElem);
					lastElem = currElem;
					level++;
				}
			}

			currElem = new ExplainOperation(
					(joinType.equals("const") && keyUsed!=null ? " const " : joinType) + (keyUsed==null ? "" : " using "+keyUsed) + " on "+table,
					(keyUsed==null ? new String[]{ROWS_EQ +readRows} : new String[]{ ROWS_EQ +readRows, "key_length="+keyLength, "reference="+reference}),
					level);
			lastElem.append(currElem);
		}
		return root;
	}

	@Override
	public Result run(String sql) throws DBCommException {
		if(Connection.MYSQL_SUPPORTED){
			return new Result(new Query(sql).run());
		}else{
			DesignGUI.getInfoPanel().writeFailed(DesignGUI.getInfoPanel().write(""), getTypeName()+" driver not loaded");
			return new Result(null);
		}
	}

	@Override
	public Result run(String sql, DB db) throws DBCommException {
		if(Connection.MYSQL_SUPPORTED){
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
				Query q = new Query("SELECT version();", getDefaultDB()).run();
				while(q.next()){
					version = q.getString(1).replaceAll("([0-9.]+).*", "$1");
					versionMajor = Float.parseFloat(version.replaceAll("([0-9]+.[0-9]+).*", "$1"));
				}
				q.close();
				CONN_VERSIONS.put(getProtocol() + getHost(), versionMajor);
				if(DesignGUI.getInfoPanel() != null){
					DesignGUI.getInfoPanel().write(getTypeName()+" version: "+versionMajor);
				}
			} catch (DBCommException ex) {
				Dbg.info(ex);
			}
		}else{
			versionMajor = CONN_VERSIONS.get(getProtocol() + getHost());
		}
	}

	private static class DataTypesMy extends DataTypes {
		DataTypesMy(){
			put(L_BOOL,					BASE_TYPE_BOOLEAN);

			put(L_BIT,					L_BIT);
			put(L_TINYINT,				L_TINYINT);
			put(BASE_TYPE_SMALLINT,		BASE_TYPE_SMALLINT);
			put(L_MEDIUMINT,			L_MEDIUMINT);
			put(L_INT,					BASE_TYPE_INTEGER);
			put(BASE_TYPE_BIGINT,		BASE_TYPE_BIGINT);

			put(L_DEC,					BASE_TYPE_DECIMAL);
			put(L_FIXED,				BASE_TYPE_DECIMAL);
			put(BASE_TYPE_DECIMAL,		BASE_TYPE_DECIMAL);
			put(BASE_TYPE_FLOAT,		BASE_TYPE_FLOAT);
			put(BASE_TYPE_DOUBLE,		BASE_TYPE_DOUBLE);

			put(BASE_TYPE_DATE,			BASE_TYPE_DATE);
			put(L_DATETIME,				L_DATETIME);
			put(BASE_TYPE_TIME,			BASE_TYPE_TIME);
			put(BASE_TYPE_TIMESTAMP,	BASE_TYPE_TIMESTAMP);
			put(L_YEAR,					L_YEAR);

			put(BASE_TYPE_CHAR,			BASE_TYPE_CHAR);
			put(BASE_TYPE_VARCHAR,		BASE_TYPE_VARCHAR);
			put(L_TINYTEXT,				L_TINYTEXT);
			put(L_TINYBLOB,				L_TINYBLOB);
			put(BASE_TYPE_TEXT,			BASE_TYPE_TEXT);
			put(BASE_TYPE_BLOB,			BASE_TYPE_BLOB);
			put(L_MEDIUMTEXT,			L_MEDIUMTEXT);
			put(L_MEDIUMBLOB,			L_MEDIUMBLOB);
			put(L_LONGTEXT,				L_LONGTEXT);
			put(L_LONGBLOB,				L_LONGBLOB);

			put(L_ENUM,					L_ENUM);

			precisions.put(BASE_TYPE_BIGINT, "");
			precisions.put(L_BIT, "");
			precisions.put(BASE_TYPE_BLOB, "");
			precisions.put(L_BOOL, "");
			precisions.put(BASE_TYPE_CHAR, "");
			precisions.put(BASE_TYPE_DATE, "");
			precisions.put(L_DATETIME, "");
			precisions.put(BASE_TYPE_DECIMAL, "10,2");
			precisions.put(L_FIXED, "10,2");
			precisions.put(BASE_TYPE_DOUBLE, "");
			precisions.put(L_ENUM, "''");
			precisions.put(BASE_TYPE_FLOAT, "");
			precisions.put(L_INT, "");
			precisions.put(L_LONGBLOB, "");
			precisions.put(L_LONGTEXT, "");
			precisions.put(L_MEDIUMBLOB, "");
			precisions.put(L_MEDIUMINT, "");
			precisions.put(L_MEDIUMTEXT, "");
			precisions.put(BASE_TYPE_SMALLINT, "");
			precisions.put(BASE_TYPE_TEXT, "");
			precisions.put(BASE_TYPE_TIME, "");
			precisions.put(BASE_TYPE_TIMESTAMP, "");
			precisions.put(L_TINYBLOB, "");
			precisions.put(L_TINYINT, "");
			precisions.put(L_TINYTEXT, "");
			precisions.put(BASE_TYPE_VARCHAR, "255");
			precisions.put(L_YEAR, "");
		}

		@Override
		public String getTranslatedType(String type, String precision) {
			switch (type){
			case BASE_TYPE_BOOLEAN: return BASE_TYPE_BOOLEAN;

			case BASE_TYPE_CHAR: return BASE_TYPE_CHAR + precision;

			case BASE_TYPE_VARCHAR: return BASE_TYPE_VARCHAR + precision;

			case BASE_TYPE_BLOB: return BASE_TYPE_BLOB + precision;
			case BASE_TYPE_TEXT: return BASE_TYPE_TEXT + precision;

			case BASE_TYPE_FLOAT: return BASE_TYPE_FLOAT;
			case BASE_TYPE_DOUBLE: return BASE_TYPE_DOUBLE;

			case BASE_TYPE_DECIMAL: return BASE_TYPE_DECIMAL;
			case BASE_TYPE_SMALLINT: return BASE_TYPE_SMALLINT;
			case BASE_TYPE_INTEGER: return BASE_TYPE_INTEGER;
			case BASE_TYPE_BIGINT: return BASE_TYPE_BIGINT;
			case BASE_TYPE_NUMERIC: return BASE_TYPE_DECIMAL;

			case BASE_TYPE_DATE: return BASE_TYPE_DATE;
			case BASE_TYPE_TIME: return BASE_TYPE_TIME;

			case BASE_TYPE_INTERVAL:
			case BASE_TYPE_TIMESTAMP: return BASE_TYPE_TIMESTAMP;

			case BASE_TYPE_ROWID:
			case BASE_TYPE_BINARY: return BASE_TYPE_BLOB + precision;

			case BASE_TYPE_XML: return L_LONGBLOB;

			default: return type + precision;
			}
		}
	}
}
