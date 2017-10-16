
package com.databazoo.devmodeler.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.tools.Geometry;

/**
 *
 * @author bobus
 */
public abstract class DataTypes extends HashMap<String,String> {

	protected static final String BASE_TYPE_BOOLEAN 	= "boolean";
	protected static final String BASE_TYPE_CHAR 		= "char";
	protected static final String BASE_TYPE_VARCHAR 	= "varchar";
	protected static final String BASE_TYPE_TEXT	 	= "text";
	protected static final String BASE_TYPE_BLOB 		= "blob";
	protected static final String BASE_TYPE_FLOAT		= "float";
	protected static final String BASE_TYPE_DOUBLE 		= "double";
	protected static final String BASE_TYPE_DECIMAL 	= "decimal";
	protected static final String BASE_TYPE_NUMERIC 	= "numeric";
	protected static final String BASE_TYPE_SMALLINT 	= "smallint";
	protected static final String BASE_TYPE_INTEGER		= "integer";
	protected static final String BASE_TYPE_BIGINT 		= "bigint";
	protected static final String BASE_TYPE_DATE 		= "date";
	protected static final String BASE_TYPE_TIME 		= "time";
	protected static final String BASE_TYPE_INTERVAL 	= "interval";
	protected static final String BASE_TYPE_TIMESTAMP 	= "timestamp";
	protected static final String BASE_TYPE_ROWID	 	= "rowid";
	protected static final String BASE_TYPE_BINARY	 	= "binary";
	protected static final String BASE_TYPE_XML 		= "xml";

	protected static final String L_ABSTIME 			= "abstime";
	protected static final String L_BFILE 				= "bfile";
	protected static final String L_BIGSERIAL 			= "bigserial";
	protected static final String L_BINARY_DOUBLE		= "binary_double";
	protected static final String L_BINARY_FLOAT		= "binary_float";
	protected static final String L_BIT 				= "bit";
	protected static final String L_BIT_VARYING 		= "bit varying";
	protected static final String L_BOOL 				= "bool";
	protected static final String L_BPCHAR 				= "bpchar";
	protected static final String L_BYTEA 				= "bytea";
	protected static final String L_CHARACTER			= "character";
	protected static final String L_CHARACTER_VARYING	= "character varying";
	protected static final String L_CLOB 				= "clob";
	protected static final String L_DATETIME 			= "datetime";
	protected static final String L_DATETIME_2 			= "datetime2";
	protected static final String L_DATETIME_OFFSET 	= "datetimeoffset";
	protected static final String L_DBCLOB 				= "dbclob";
	protected static final String L_DEC 				= "dec";
	protected static final String L_DECFLOAT			= "decfloat";
	protected static final String L_DOUBLE_PRECISION 	= "double precision";
	protected static final String L_ENUM				= "enum";
	protected static final String L_FIXED				= "fixed";
	protected static final String L_FLOAT4 				= "float4";
	protected static final String L_FLOAT8 				= "float8";
	protected static final String L_GRAPHIC 			= "graphic";
	protected static final String L_IMAGE 				= "image";
	protected static final String L_INT 				= "int";
	protected static final String L_INT2 				= "int2";
	protected static final String L_INT4 				= "int4";
	protected static final String L_INT8 				= "int8";
	protected static final String L_LONG				= "long";
	protected static final String L_LONGBLOB 			= "longblob";
	protected static final String L_LONG_RAW 			= "long raw";
	protected static final String L_LONGTEXT 			= "longtext";
	protected static final String L_MEDIUMBLOB 			= "mediumblob";
	protected static final String L_MEDIUMINT 			= "mediumint";
	protected static final String L_MEDIUMTEXT 			= "mediumtext";
	protected static final String L_MONEY				= "money";
	protected static final String L_NAME 				= "name";
	protected static final String L_NCLOB 				= "nclob";
	protected static final String L_NCHAR 				= "nchar";
	protected static final String L_NTEXT 				= "ntext";
	protected static final String L_NUMBER				= "number";
	protected static final String L_NVARCHAR 			= "nvarchar";
	protected static final String L_NVARCHAR2 			= "nvarchar2";
	protected static final String L_OID = "oid";
	protected static final String L_PATH 				= "path";
	protected static final String L_RAW 				= "raw";
	protected static final String L_REAL 				= "real";
	protected static final String L_RELTIME 			= "reltime";
	protected static final String L_SERIAL 				= "serial";
	protected static final String L_SERIAL4 			= "serial4";
	protected static final String L_SERIAL8 			= "serial8";
	protected static final String L_SMALLDATETIME 		= "smalldatetime";
	protected static final String L_SMALLMONEY 			= "smallmoney";
	protected static final String L_TIMETZ 				= "timetz";
	protected static final String L_TIME_WITH_TIME_ZONE = "time with time zone";
	protected static final String L_TIMESTAMPTZ 		= "timestamptz";
	protected static final String L_TIMESTAMP_WITH_TIME_ZONE = "timestamp with time zone";
	protected static final String L_TIMESTAMP_WITH_LOCAL_TIME_ZONE = "timestamp with local time zone";
	protected static final String L_TINTERVAL 			= "tinterval";
	protected static final String L_TINYBLOB 			= "tinyblob";
	protected static final String L_TINYINT 			= "tinyint";
	protected static final String L_TINYTEXT 			= "tinytext";
	protected static final String L_URITYPE 			= "uritype";
	protected static final String L_UROWID 				= "urowid";
	protected static final String L_UUID 				= "uuid";
	protected static final String L_VARBINARY 			= "varbinary";
	protected static final String L_VARBIT 				= "varbit";
	protected static final String L_VARCHAR2 			= "varchar2";
	protected static final String L_VARGRAPHIC 			= "vargraphic";
	protected static final String L_XMLTYPE 			= "xmltype";
	protected static final String L_YEAR 				= "year";

	private static final Set<String> ESCAPED_TYPES = new HashSet<>(Arrays.asList(
			BASE_TYPE_CHAR, BASE_TYPE_VARCHAR, BASE_TYPE_TEXT, BASE_TYPE_BLOB,
			BASE_TYPE_DATE, BASE_TYPE_TIME, BASE_TYPE_TIMESTAMP, BASE_TYPE_INTERVAL,
			BASE_TYPE_BINARY, BASE_TYPE_XML));

	protected final transient Map<String,String> precisions = new HashMap<>();

	public static String[] getTypeNames(){
		return Project.getCurrent() == null ? new String[0] : getTypeNames(Project.getCurrent().getCurrentConn());
	}

	public static String[] getTypeNames (IConnection conn) {
		if(Settings.getBool(Settings.L_AUTOCOMPLETE_NATIVE_DT)) {
			return Geometry.concat(conn.getDataTypes().getKeys(), conn.getDataTypes().getVals());
		} else {
			return conn != null ? conn.getDataTypes().getVals() : new String[0];
		}
	}

	/**
	 * Translate any known type to some base type.
	 */
	public static String getTranslatedBaseType (String type, String precision) {
		switch (type){
			case L_BOOL:
			case BASE_TYPE_BOOLEAN: return BASE_TYPE_BOOLEAN;

			case L_BIT:
			case L_BPCHAR:
			case L_CHARACTER:
			case L_NCHAR:
			case BASE_TYPE_CHAR: return BASE_TYPE_CHAR + precision;

			case L_CHARACTER_VARYING:
			case L_NAME:
			case L_PATH:
			case L_NVARCHAR:
			case L_NVARCHAR2:
			case L_VARCHAR2:
			case L_ENUM:
			case L_URITYPE:
			case BASE_TYPE_VARCHAR: return BASE_TYPE_VARCHAR + precision;

			case L_TINYBLOB:
			case L_MEDIUMBLOB:
			case L_LONGBLOB:
			case L_CLOB:
			case L_NCLOB:
			case L_DBCLOB:
			case L_BFILE:
			case L_BYTEA:
			case BASE_TYPE_BLOB: return BASE_TYPE_BLOB + precision;

			case L_LONG:
			case L_TINYTEXT:
			case L_MEDIUMTEXT:
			case L_LONGTEXT:
			case L_NTEXT:
			case BASE_TYPE_TEXT: return BASE_TYPE_TEXT + precision;

			case L_TINYINT:
			case L_INT2:
			case BASE_TYPE_SMALLINT: return BASE_TYPE_SMALLINT;

			case L_MEDIUMINT:
			case L_SERIAL4:
			case L_SERIAL:
			case L_INT4:
			case L_INT:
			case BASE_TYPE_INTEGER: return BASE_TYPE_INTEGER;

			case L_SERIAL8:
			case L_BIGSERIAL:
			case L_INT8:
			case BASE_TYPE_BIGINT: return BASE_TYPE_BIGINT;

			case L_DEC:
			case L_DECFLOAT:
			case L_FIXED:
			case BASE_TYPE_DECIMAL: return BASE_TYPE_DECIMAL + precision;

			case L_NUMBER:
			case L_MONEY:
			case L_SMALLMONEY:
			case BASE_TYPE_NUMERIC: return BASE_TYPE_NUMERIC + precision;

			case L_REAL:
			case L_FLOAT4:
			case L_BINARY_FLOAT:
			case BASE_TYPE_FLOAT: return BASE_TYPE_FLOAT;

			case L_FLOAT8:
			case L_DOUBLE_PRECISION:
			case L_BINARY_DOUBLE:
			case BASE_TYPE_DOUBLE: return BASE_TYPE_DOUBLE;

			case BASE_TYPE_DATE: return BASE_TYPE_DATE;

			case L_ABSTIME:
			case L_RELTIME:
			case L_TIMETZ:
			case L_TIME_WITH_TIME_ZONE:
			case BASE_TYPE_TIME: return BASE_TYPE_TIME;

			case L_DATETIME_OFFSET:
			case L_TINTERVAL:
			case BASE_TYPE_INTERVAL: return BASE_TYPE_INTERVAL;

			case L_YEAR:
			case L_DATETIME:
			case L_DATETIME_2:
			case L_SMALLDATETIME:
			case L_TIMESTAMP_WITH_TIME_ZONE:
			case L_TIMESTAMPTZ:
			case L_TIMESTAMP_WITH_LOCAL_TIME_ZONE:
			case BASE_TYPE_TIMESTAMP: return BASE_TYPE_TIMESTAMP + precision;

			case L_VARBINARY:
			case L_IMAGE:
			case L_GRAPHIC:
			case L_VARGRAPHIC:
			case L_RAW:
			case L_VARBIT:
			case L_BIT_VARYING:
			case L_LONG_RAW:
			case BASE_TYPE_BINARY: return BASE_TYPE_BINARY + precision;

			case L_UUID:
			case L_OID:
			case L_UROWID:
			case BASE_TYPE_ROWID: return BASE_TYPE_ROWID + precision;

			case L_XMLTYPE:
			case BASE_TYPE_XML: return BASE_TYPE_XML;

			default: return type + precision;
		}
	}

	private String[] vals;

	public String[] getKeys(){
		Object[] keys = keySet().toArray();
		String[] ret = new String[keys.length];
		for(int i=0; i<keys.length; i++){
			ret[i] = keys[i].toString();
		}
		Arrays.sort(ret);
		return ret;
	}

	public String[] getVals(){
		if(vals == null){
			Object[] allVals = values().toArray();
			ArrayList<String> ret = new ArrayList<>();
			for (Object allVal : allVals) {
				if (!ret.contains(allVal.toString())) {
					ret.add(allVal.toString());
				}
			}
			vals = new String[ret.size()];
			for(int i=0; i<ret.size(); i++){
				vals[i] = ret.get(i);
			}
			Arrays.sort(vals);
		}
		return vals;
	}

	public String toFullType(String attType){
		String type, precision;
		if(attType.contains("(")){
			String[] parts = attType.split("\\(");
			type = parts[0];
			precision = "("+parts[1];
		}else{
			type = attType;
			precision = "";
		}
		if(type.toCharArray()[0] == '_'){
			type = type.substring(1);
			precision = "[]"+precision;
		}
		String full = get(type);
		return full == null ? attType : full+precision;
	}

	public String toShortType(String attType){
		for (Object key : keySet().toArray()) {
			if (get(key.toString()).equals(attType)) {
				return key.toString();
			}
		}
		return attType;
	}

	/**
	 * Set default precision for given type in the UI.
	 */
	public String getPrecisionForType(String type) {
		return precisions.get(type);
	}

	/**
	 * Translate any base type to some type in target dialect.
	 */
	public abstract String getTranslatedType(String type, String precision);

	/**
	 * Finds if given type requires escaping.
	 *
	 * @param type any type without precision
	 * @return is escaped type?
	 */
	public boolean isEscapedType(String type){
		return ESCAPED_TYPES.contains(getTranslatedBaseType(type, ""));
	}

}
