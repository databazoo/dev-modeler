
package com.databazoo.devmodeler.conn;

import java.awt.*;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.devmodeler.gui.window.ProgressWindow;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.CheckConstraint;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.DataTypes;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Package;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Sequence;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.model.View;
import com.databazoo.devmodeler.model.explain.ExplainOperation;

/**
 * These methods have to be implemented to create a communication setup for new database type
 *
 * @author bobus
 */
public interface IConnection extends IColoredConnection, Serializable {
	boolean isSupported(SupportedElement elemType);
	String getDefaultDB();
	String[] getLanguages();
	String[] getAccessMethods();
	String[] getConstraintUpdateActions();
	String[] getConstraintDeleteActions();
	String[] getStorageEngines();
	String[] getCollations();
	DataTypes getDataTypes();
	String getCleanError(String message);
	Point getErrorPosition(String query, String message);

	String getTypeName();
	String getProtocol();
	String getConnURL(String dbName);

	void afterCreate(IModelElement elem);
	void afterAlter(IModelElement elem);
	void afterDrop(IModelElement elem);

	String getConnHash(String dbName);
	String getQueryLocks();
	String getQueryLimitOffset(int limit, int offset);
	String getQueryExplain(String selectedText);
	String getQueryTerminate(int selectedPID);
	String getQueryCancel(int selectedPID);

	String getQueryCreate(DB db, SQLOutputConfig config);
	String getQueryDrop(DB db);

	String getQueryCreate(Schema schema, SQLOutputConfig config);
	String getQueryDrop(Schema schema);
	String getQueryChanged(Schema schema);

	String getQueryCreate(Relation rel, SQLOutputConfig config);
	String getQueryDrop(Relation rel);
	String getQueryChanged(Relation rel);

	String getQueryCreate(Attribute attr, SQLOutputConfig config);
	String getQueryDef(Attribute attr, SQLOutputConfig config);
	String getQueryDrop(Attribute attr);
	String getQueryChanged(Attribute attr);

	String getQueryCreateWithoutComment(CheckConstraint con, SQLOutputConfig config);		// TODO: remove
	String getQueryCreate(CheckConstraint con, SQLOutputConfig config);
	String getQueryChanged(CheckConstraint con);

	String getQueryCreateWithoutComment(Constraint con, SQLOutputConfig config);			// TODO: remove
	String getQueryCreate(Constraint con, SQLOutputConfig config);
	String getQueryDef(Constraint con);
	String getQueryDrop(Constraint con);
	String getQueryChanged(Constraint con);
	String getCleanDef(Constraint con);

	String getQueryCreate(Index ind, SQLOutputConfig config);
	String getQueryDef(Index ind);
	String getQueryDrop(Index ind);
	String getQueryChanged(Index ind);
	String getCleanDef(Index ind);

	String getQueryExecFunction(Function func);
	String getQueryCreate(Function func, SQLOutputConfig config);
	String getQueryDrop(Function func);
	String getQueryChanged(Function func);

	String getQueryCreate(Package pack, SQLOutputConfig config);
	String getQueryDrop(Package pack);
	String getQueryChanged(Package pack);

	String getQueryCreate(Sequence func, SQLOutputConfig config);
	String getQueryDrop(Sequence func);
	String getQueryChanged(Sequence func);

	String getQueryCreate(View view, SQLOutputConfig config);
	String getQueryDrop(View view);
	String getQueryChanged(View view);

	String getQueryCreate(Trigger trig, SQLOutputConfig config);
	String getQueryDrop(Trigger trig);
	String getQueryChanged(Trigger trig);

	ExplainOperation getExplainTree(Result r);
	String getQueryVacuum(String s, List<Relation> relations);

	/**
	 * Controlled by {@link SupportedElement#GENERATE_DDL}. Currently only supported in Oracle.
	 */
	String loadDDLFromDB(IModelElement element) throws DBCommException;

	Result getServerStatus() throws DBCommException;
	Result run(String sql) throws DBCommException;
	Result run(String sql, DB db) throws DBCommException;
	Result getAllRows(Relation rel) throws DBCommException;

	void runManualRollback(String revertSQL, DB database);
	void runManualRollbackAndWait(String sql, DB db);

	List<DB> getDatabases() throws DBCommException;

	void loadEnvironment() throws DBCommException;
	void loadDB(DB db) throws DBCommException;
	void loadSchemas(DB db) throws DBCommException;
	void loadRelations(DB db) throws DBCommException;
	void loadRelationInfo(Relation rel) throws DBCommException;
	void loadAttributes(DB db) throws DBCommException;
	void loadFunctions(DB db) throws DBCommException;
	void loadViews(DB db) throws DBCommException;
	void loadIndexes(DB db) throws DBCommException;
	void loadConstraints(DB db) throws DBCommException;
	void loadTriggers(DB db) throws DBCommException;
	void loadSequences(DB db) throws DBCommException;

	String getName();
	String getFullName();
	String getHost();
	String getUser();
	String getPass();
	int getType();
	String getDefaultSchema();
	String getDbAlias();

	void setName(String val);
	void setHost(String val);
	void setUser(String val);
	void setPass(String val);
	void setDefaultSchema(String val);
	void setDbAlias(String name);

	ProgressWindow getProgressChecker();
	void setProgressChecker(ProgressWindow progress);

	IConnectionQuery prepare(String sql, DB database) throws DBCommException;
	IConnectionQuery prepareWithBegin(String text, DB db) throws DBCommException;

	String getQuerySelect(Constraint con);
	String getQuerySelect(DraggableComponent rel, String where, String order, int limit);
	String getQuerySelect(String text, String columns);
	String getQueryOrder(String text, String orderBy);
	String getQueryWhere(String text, String where);


	String getWhereFromDef(Index instance);

	String getQueryInsert(Relation rel, Map<String, String> values);
	String getQueryInsert(String relName, Map<String, String> values);
	String getQueryInsert(Relation rel, ResultRow row);
	String getQueryDelete(Relation rel, Map<String, String> pkHashMap);

	String getQueryUpdate(Relation rel, Map<String, String> pkValues, String column, String value);
	String getQueryUpdate(Relation rel, Map<String, String> pkValues, Map<String, String> colValues);
	String getQueryUpdate(String relName, Map<String, String> pkValues, Map<String, String> colValues);

	SQLBuilder getQueryData(Relation rel, SQLOutputConfig config, SQLBuilder ret) throws DBCommException;

	boolean getStatusChecked();
	boolean getStatus();
	String getStatusHTML();
	void setAutocheckEnabled(boolean doCheck);

	String escape(String name);
	String escapeFullName(String fullName);
	String getEscapedFullName(IModelElement elem);

	void runStatusCheck();
	void runStatusCheck(Runnable resultListener);

	String getDefaultDataType();
	String getDefaultStorageEngine();
	String getDefaultCollation();

	boolean isCollatable(String datatype);
	String getPresetFucntionArgs(boolean isTrigger);
	String getPresetFucntionReturn(boolean isTrigger);
	String getPresetFucntionSource(boolean isTrigger);
	String getPresetPackageDefinition();
	String getPresetPackageBody();
	String getPresetTriggerBody();
	String getPrecisionForType(String type);
	String getQueryConnect();
}
