
package com.databazoo.devmodeler.tools;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;

/**
 * A tool to create names for new elements.
 *
 * @author bobus
 */
public interface NameGenerator {

	static String createSchemaName(DB parent){
		return translate(Settings.L_NAMING_SCHEMA, parent.getName(), null, null, null, parent.getSchemas().size());
	}

	static String createRelationName(Schema parent){
		return translate(Settings.L_NAMING_TABLE, parent.getDB().getName(), parent.getName(), null, null, parent.getRelations().size());
	}

	static String createAttributeName(Relation parent){
		return translate(Settings.L_NAMING_COLUMN, parent.getDB().getName(), parent.getSchema().getName(), parent.getName(), null, parent.getAttributes().size());
	}

	static String createPrimaryKeyName(Relation parent){
		return translate(Settings.L_NAMING_PK, parent.getDB().getName(), parent.getSchema().getName(), parent.getName(), null, parent.getConstraints().size());
	}

	static String createForeignKeyName(Attribute parent){
		return translate(Settings.L_NAMING_FK, parent.getDB().getName(), parent.getRel().getSchema().getName(), parent.getRel().getName(), parent.getName(), parent.getRel().getConstraints().size());
	}

	static String createUniqueConstraintName(Relation parent){
		return translate(Settings.L_NAMING_UNIQUE, parent.getDB().getName(), parent.getSchema().getName(), parent.getName(), null, parent.getConstraints().size());
	}

	static String createCheckConstraintName(Relation parent){
		return translate(Settings.L_NAMING_CHECK, parent.getDB().getName(), parent.getSchema().getName(), parent.getName(), null, parent.getConstraints().size());
	}

	static String createIndexName(Relation parent){
		return translate(Settings.L_NAMING_INDEX, parent.getDB().getName(), parent.getSchema().getName(), parent.getName(), null, parent.getIndexes().size());
	}

	static String createSequenceName(Schema parent){
		return translate(Settings.L_NAMING_SEQUENCE, parent.getDB().getName(), parent.getName(), null, null, parent.getSequences().size());
	}

	static String createTriggerName(Relation parent){
		return translate(Settings.L_NAMING_TRIGGER, parent.getDB().getName(), parent.getSchema().getName(), parent.getName(), null, parent.getTriggers().size());
	}

	static String createViewName(Schema parent){
		return translate(Settings.L_NAMING_VIEW, parent.getDB().getName(), parent.getName(), null, null, parent.getViews().size());
	}

	static String createFunctionName(Schema parent){
		return translate(Settings.L_NAMING_FUNCTION, parent.getDB().getName(), parent.getName(), null, null, parent.getFunctions().size());
	}

	static String createPackageName(Schema parent){
		return translate(Settings.L_NAMING_PACKAGE, parent.getDB().getName(), parent.getName(), null, null, parent.getPackages().size());
	}

	static String translate(String settingKey, String databaseName, String schemaName, String tableName, String columnName, int serial){

		databaseName = databaseName != null ? databaseName.replaceAll("\\s+", "") : "";
		schemaName = schemaName != null ? schemaName.replaceAll("\\s+", "") : "";
		tableName = tableName != null ? tableName.replaceAll("\\s+", "") : "";
		columnName = columnName != null ? columnName.replaceAll("\\s+", "") : "";

		HashMap<String,String> replacements = new LinkedHashMap<>();

		replacements.put("database", databaseName);
		replacements.put("uc_database", databaseName.toUpperCase());
		replacements.put("lc_database", databaseName.toLowerCase());

		replacements.put("schema", schemaName);
		replacements.put("uc_schema", schemaName.toUpperCase());
		replacements.put("lc_schema", schemaName.toLowerCase());

		replacements.put("table", tableName);
		replacements.put("uc_table", tableName.toUpperCase());
		replacements.put("lc_table", tableName.toLowerCase());

		replacements.put("column", columnName);
		replacements.put("uc_column", columnName.toUpperCase());
		replacements.put("lc_column", columnName.toLowerCase());

		replacements.put("serial", Integer.toString(serial+1));

		return translate(settingKey, replacements);
	}

	static String translate(String settingKey, Map<String,String> replacements){
		String val = Settings.getStr(settingKey);
		for(Map.Entry<String, String> data : replacements.entrySet()){
			val = val.replaceAll("%"+data.getKey()+"%?", data.getValue());
		}
		return val;
	}
}
