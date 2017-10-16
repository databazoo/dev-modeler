package com.databazoo.devmodeler.conn;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.window.ProgressWindow;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.DataTypes;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.explain.ExplainOperation;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.wizards.project.ProjectWizard;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.GC;
import com.databazoo.tools.SQLUtils;
import com.databazoo.tools.Schedule;

import static com.databazoo.devmodeler.conn.ConnectionUtils.FROM;

/**
 * Base connection provides database call services and some general properties for detailed implementations.
 *
 * @author bobus
 */
abstract class Connection extends ConnectionBase {
	static final Pattern VIEW_DEFINITION_PATTERN = Pattern.compile("^.*?\\sAS\\s*(.*)", Pattern.DOTALL);

	private static final String L_UPDATE = "UPDATE ";
	private static final String L_SET = " SET ";
	private static final String L_WHERE = " WHERE ";
	private static final String L_NULL = "NULL";

	private static final String ONE_TWO = "$1_$2";

	//static final String[] TYPE_COMBO = {TYPE_DIRECT, TYPE_PHP, TYPE_ASP};
	static final int INIT_TIMEOUT = 3000; // milliseconds
	static boolean POSTGRES_SUPPORTED = false;
	static boolean MYSQL_SUPPORTED = false;
	static boolean ORACLE_SUPPORTED = false;
	static boolean MSSQL_SUPPORTED = false;
	static boolean DB2_SUPPORTED = false;

	public static IConnection getCurrent(String dbName) {
		Project p = Project.getCurrent();
		IConnection dedicatedConn = p.getDedicatedConnection(dbName, p.getCurrentConn().getName());
		//Dbg.info("Dedicated connection for "+dbName+" and "+p.getCurrentConn().getName()+" "+(dedicatedConn != null ? "found" : "NOT found"));
		if (dedicatedConn != null) {
			return dedicatedConn;
			// THIS SHOULD NEVER HAPPEN, BUT IS USED THIS WAY IN TESTS
		} else {
			return p.getCurrentConn();
		}
	}

	static void initConnectionChecker() {
		new Timer("Connection.initConnectionChecker").schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					Calendar now = Calendar.getInstance();
					now.add(Calendar.SECOND, -Settings.getInt(Settings.L_PERFORM_REOPEN_CONN));
					synchronized (CONN_CACHE) {
						for (String key : new ArrayList<>(CONN_CACHE.keySet())) {
							Date last = CONN_LAST_USE.get(key);
							if (last != null && last.before(now.getTime())) {
								CONN_CACHE.remove(key);
								CONN_LAST_USE.remove(key);
								GC.invoke();
							}
						}
					}
				} catch (Exception e) {
					Dbg.fixme("Unused connection cleanup failed. Flushing connection cache.", e);
					e.printStackTrace();
					synchronized (CONN_CACHE) {
						CONN_CACHE.clear();
						CONN_LAST_USE.clear();
					}
				}
				int currThreadCnt = ManagementFactory.getThreadMXBean().getThreadCount();
				int currConnCnt = CONN_CACHE.size();
				Runtime runtime = Runtime.getRuntime();
				long maxMemory = runtime.maxMemory();
				long allocatedMemory = runtime.totalMemory();
				long freeMemory = runtime.freeMemory();
				String status = "Threads: " + currThreadCnt +
						" Connections: " + currConnCnt +
						" Memory: " + ((int) ((allocatedMemory - freeMemory) / 1024f / 1024f)) + "M / " + ((int) (allocatedMemory / 1024f / 1024f))
						+ "M (" + ((int) (maxMemory / 1024f / 1024f)) + "M)";
				if (Settings.getBool(Settings.L_ERRORS_SHOW_DBG_INFO)) {
					DesignGUI.getInfoPanel().writeGray(status);
				}
				Dbg.toFile(status);
			}
		}, 2000, 20000);
	}

	public Connection(String host, String user, String pass) {
		this.host = host;
		this.user = user;
		this.pass = pass;
	}

	public Connection(String name, String host, String user, String pass) {
		this.name = name;
		this.host = host;
		this.user = user;
		this.pass = pass;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public String getUser() {
		return user;
	}

	@Override
	public String getPass() {
		return pass;
	}

	@Override
	public String getDbAlias() {
		return dbAlias;
	}

	@Override
	public String getDefaultSchema() {
		if (defaultSchema == null || defaultSchema.isEmpty()) {
			defaultSchema = user.toLowerCase();
		}
		return defaultSchema;
	}

	@Override
	public int getType() {
		return type;
	}

	@Override
	public String getFullName() {
		return name + " <" + (user != null && !user.isEmpty() ? user + "@" : "") + host + ">";
	}

	@Override
	public void setName(String val) {
		name = val;
		synchronized (CONN_CACHE) {
			CONN_CACHE.clear();
			CONN_LAST_USE.clear();
		}
		runStatusCheck();
	}

	@Override
	public void setHost(String val) {
		host = val;
		synchronized (CONN_CACHE) {
			CONN_CACHE.clear();
			CONN_LAST_USE.clear();
		}
		runStatusCheck();
	}

	@Override
	public void setUser(String val) {
		user = val;
		synchronized (CONN_CACHE) {
			CONN_CACHE.clear();
			CONN_LAST_USE.clear();
		}
		runStatusCheck();
	}

	@Override
	public void setPass(String val) {
		pass = val;
		synchronized (CONN_CACHE) {
			CONN_CACHE.clear();
			CONN_LAST_USE.clear();
		}
		runStatusCheck();
	}

	@Override
	public ConnectionColor getColor() {
		return color != null ? color : ConnectionColor.DEFAULT;
	}

	@Override
	public void setColor(ConnectionColor color) {
		this.color = color;
	}

	@Override
	public void setDbAlias(String val) {
		dbAlias = val;
	}

	@Override
	public void setDefaultSchema(String val) {
		defaultSchema = val.toLowerCase();
	}

	/*public void setType(String val) {
		for(int i=0; i<TYPE_COMBO.length; i++){
			if(TYPE_COMBO[i].equals(val)){
				type = i;
				break;
			}
		}
		runStatusCheck();
	}*/
	@Override
	public boolean getStatusChecked() {
		return statusChecked;
	}

	@Override
	public boolean getStatus() {
		return status;
	}

	@Override
	public String getStatusHTML() {
		return "<html>" + (status ? "<font color=green>OK</font>" : "<font color=red>Failed</font>") + "</html>";
	}

	@Override
	public String escape(String name) {
		if (name.matches("(?ims).*[^a-z0-9_]+.*") || name.matches(ConnectionUtils.ESC_KEYWORDS)) {
			return "\"" + name + "\"";
		} else {
			return name;
		}
	}

	String getPrevComment(IModelElement attr) {
		return "\t-- "+attr.getDescr().replace("\n", "\n\t\t\t\t\t\t\t-- ");
	}

	void appendDescription(SQLBuilder ret, IModelElement seq) {
		ret.a("-- ").a(seq.getDescr().replace("\n", "\n-- ")).n();
	}

	public final String escapeFullNameAndCheckDefaultSchema(String fullName){
		String cleanName = fullName.replaceAll("\"", "");
		if(cleanName.matches(".+\\..+")){
			String schemaName = cleanName.replaceAll("(.*)\\..*", "$1");
			String schemaNameLC = schemaName.toLowerCase();
			String elementName = cleanName.replaceAll(".*\\.(.*)", "$1");

			// We should use the correct project here. Ideally, find an overall better way to do this.

			Project p = Project.getCurrent();
			String schemaNames = "";
			if(p != null){
				Map<String, IConnection> conns = p.getDedicatedConnections();
				String dbName = getDbNameFromDedicatedConnection(conns);
				if(dbName != null){
					schemaNames = getDefaultSchemaNamesFromDedicatedConnections(conns, dbName);
				}
			}

			return ((schemaNames.isEmpty() || !schemaNameLC.matches("("+schemaNames+")")) && !schemaNameLC.equals(getDefaultSchema()) ?
					escape(schemaName) + "." : "") +
					escape(elementName);
		}else{
			return escape(cleanName);
		}
	}

	private String getDefaultSchemaNamesFromDedicatedConnections(Map<String, IConnection> conns, String dbName) {
		StringBuilder defaultSchemaNames = new StringBuilder();
		for(Map.Entry<String, IConnection> data : conns.entrySet()){
            if(data.getKey().startsWith(dbName+"~")){
                if(defaultSchemaNames.length() > 0){
                    defaultSchemaNames.append("|");
                }
                defaultSchemaNames.append(data.getValue().getDefaultSchema());
            }
        }
        return defaultSchemaNames.toString();
	}

	private String getDbNameFromDedicatedConnection(Map<String, IConnection> conns) {
		for(Map.Entry<String, IConnection> data : conns.entrySet()){
            if(data.getValue() == this){
                return data.getKey().replaceFirst("(.+)~.+", "$1");
            }
        }
		return null;
	}

	@Override
	public String escapeFullName(String fullName) {
		fullName = fullName.replaceAll("\"", "");
		if (fullName.matches(ConnectionUtils.FULL_NAME_REGEX)) {
			return escape(fullName.replaceAll(ConnectionUtils.FULL_NAME_REGEX, "$1")) + "." + escape(fullName.replaceAll(ConnectionUtils.FULL_NAME_REGEX, "$2"));
		} else {
			return escape(fullName);
		}
	}

	@Override
	public String getEscapedFullName(IModelElement elem) {
		return escapeFullName(elem.getFullName());
	}

	@Override
	public final void runStatusCheck() {
		runStatusCheck(null);
	}

	@Override
	public final void runStatusCheck(Runnable resultListener) {
		if (getHost() != null && !getHost().isEmpty() &&
				(getUser() != null && !getUser().isEmpty() &&
						getPass() != null && !getPass().isEmpty())
				&&
				doCheck) {
			Dbg.info("Requested status check for " + getProtocol() + getUser() + "@" + getHost());
			Schedule.inWorker(() -> {
                int log = DesignGUI.getInfoPanel().write("Connection check: " + getFullName() + " ");
                try {
                    Query q = new Query(getQueryConnect(), getDefaultDB()).run();
                    q.close();
                    q.log(log);
                    status = true;
                    runVersionCheck();
                } catch (DBCommException ex) {
					Dbg.notImportant("Status check failed", ex);
                    DesignGUI.getInfoPanel().writeFailed(log, getCleanError(ex.getMessage()));
                    status = false;
                }
                statusChecked = true;

                if (resultListener != null) {
					resultListener.run();
				} else {
					ProjectWizard.updateConnTable();
				}
            });
		}
	}

	@Override
	public String getDefaultDataType() {
		return "varchar";
	}

	@Override
	public String getDefaultStorageEngine() {
		return "MyISAM";
	}

	@Override
	public String getDefaultCollation() {
		return "utf8_general_ci";
	}

	@Override
	public boolean isCollatable(String datatype) {
		return datatype.toLowerCase().matches("(char|varchar|(tiny|medium|long)?(text|blob))");
	}

	@Override
	public String getPresetFucntionArgs(boolean isTrigger) {
		return "";
	}

	@Override
	public String getPresetFucntionReturn(boolean isTrigger) {
		return "";
	}

	@Override
	public String getPresetFucntionSource(boolean isTrigger) {
		return "\nBEGIN\n\n\t/** TODO **/\n\nEND;\n";
	}

	@Override
	public String getPresetPackageDefinition() {
		return "\n\tFUNCTION my_func(my_input number DEFAULT 0) RETURN number;\n";
	}

	@Override
	public String getPresetPackageBody() {
		return "\n\tFUNCTION my_func(my_input number DEFAULT 0) RETURN number IS\n\tBEGIN\n\t\tRETURN 1;\n\tEND;\n";
	}

	@Override
	public String getPresetTriggerBody() {
		return "BEGIN\n\tSET \nEND";
	}

	@Override
	public String getQueryConnect() {
		return "SELECT 1";
	}

	@Override
	public String getQuerySelect(String currentQuery, String columns) {
		int loc = currentQuery.indexOf("SELECT");
		if (loc >= 0) {
			currentQuery = currentQuery.substring(0, loc + 6) + " " + columns + "," + currentQuery.substring(loc + 6);
		} else {
			currentQuery += "\n\nSELECT " + columns + " FROM ";
		}
		return currentQuery;
	}

	@Override
	public String getQueryWhere(String currentQuery, String condition) {
		currentQuery = currentQuery.replace("\r\n", "\n");
		String[] terminators = new String[]{"GROUP BY", "HAVING", "UNION", "ORDER BY", "OFFSET", "LIMIT"};
		int locTerm = -1;
		for (String term : terminators) {
			int loc = currentQuery.indexOf(term);
			if (loc >= 0) {
				//Dbg.info("Found terminating statement "+term+" at "+loc);
				locTerm = loc;
				break;
			}
		}
		// No terminator found, check there is a \n at the end
		if (locTerm < 0) {
			if (currentQuery.charAt(currentQuery.length() - 1) != '\n') {
				currentQuery += "\n";
			}
			locTerm = currentQuery.length();
		}
		int loc = currentQuery.lastIndexOf("WHERE");
		if (loc >= 0) {
			//Dbg.info("Found existing WHERE at "+loc+", terminating statement is at "+locTerm);
			String currentCondition = currentQuery.substring(loc + 6, locTerm - 1);
			if (currentCondition.matches("(?is).*[^a-zA-Z0-9]OR[^a-zA-Z0-9].*")) {
				currentCondition = "(" + currentCondition + ")";
			}
			currentQuery = currentQuery.substring(0, loc + 6) + condition + " AND " + currentCondition
					+ (!currentQuery.substring(locTerm - 1).equals("\n") ? currentQuery.substring(locTerm - 1) : "");
		} else {
			//Dbg.info("WHERE not found, terminating statement is at "+locTerm);
			currentQuery = currentQuery.substring(0, locTerm) + "WHERE " + condition
					+ (currentQuery.length() > locTerm ? "\n" + currentQuery.substring(locTerm) : "");
		}
		return currentQuery;
	}

	@Override
	public String getQueryOrder(String currentQuery, String condition) {
		currentQuery = currentQuery.replace("\r\n", "\n");
		String[] terminators = new String[]{"OFFSET", "LIMIT", ";"};
		int locTerm = -1;
		for (String term : terminators) {
			int loc = currentQuery.indexOf(term);
			if (loc >= 0) {
				//Dbg.info("Found terminating statement "+term+" at "+loc);
				locTerm = loc;
				break;
			}
		}
		// No terminator found, check there is a \n at the end
		if (locTerm < 0) {
			if (currentQuery.charAt(currentQuery.length() - 1) != '\n') {
				currentQuery += "\n";
			}
			locTerm = currentQuery.length();
		}
		int loc = currentQuery.lastIndexOf("ORDER BY");
		if (loc >= 0) {
			String currentCondition = currentQuery.substring(loc + 9, locTerm - 1);
			currentQuery = currentQuery.substring(0, loc + 9) + condition + ", " + currentCondition
					+ (!currentQuery.substring(locTerm - 1).equals("\n") ? currentQuery.substring(locTerm - 1) : "");
		} else {
			if (locTerm > 0) {
				char lastChar = currentQuery.charAt(locTerm - 1);
				String resChars;
				if (lastChar == ' ') {
					resChars = "\n";
				} else {
					resChars = lastChar + "\n";
				}
				currentQuery = currentQuery.substring(0, locTerm - 1) + resChars + "ORDER BY " + condition
						+ (currentQuery.length() > locTerm ? (currentQuery.charAt(locTerm) == ';' ? "" : "\n")
						+ currentQuery.substring(locTerm) : "");
			} else {
				currentQuery = currentQuery.substring(0, locTerm) + "ORDER BY " + condition
						+ (currentQuery.length() > locTerm ? "\n" + currentQuery.substring(locTerm) : "");
			}
		}
		return currentQuery;
	}

	@Override
	public String getQueryDelete(Relation rel, Map<String, String> pkValues) {
		StringBuilder sql = new StringBuilder("DELETE FROM ").append(getEscapedFullName(rel));
		appendPkValues(sql, rel, pkValues, L_WHERE);
		return sql.append(";").toString();
	}

	@Override
	public String getQueryUpdate(Relation rel, Map<String, String> pkValues, String column, String value) {
		final HashMap<String, String> colValues = new HashMap<>();
		colValues.put(column, value);
		return getQueryUpdate(rel, pkValues, colValues);
	}

	@Override
	public String getQueryUpdate(Relation rel, Map<String, String> pkValues, Map<String, String> colValues) {
		StringBuilder sql = new StringBuilder(L_UPDATE).append(getEscapedFullName(rel)).append(L_SET);
		String comma = "";
		for (Map.Entry<String, String> data : colValues.entrySet()) {
			String column = data.getKey();
			String value = data.getValue();

			boolean isEscapedType = getDataTypes().isEscapedType(rel.getAttributeByName(column).getBehavior().getAttType());

			sql.append(comma).append(escape(column)).append(" = ");
			Attribute attr = rel.getAttributeByName(column);
			if (value == null || (attr != null && value.isEmpty() && attr.getBehavior().isAttNull())) {
				sql.append(L_NULL);
			} else if (!isEscapedType && value.matches(ConnectionUtils.IS_NUMBER_REGEX)) {
				sql.append(value);
			} else {
				sql.append(value.equals("''") ? "''" : "'" + value.replace("'", "''") + "'");
			}
			comma = ", ";
		}
		comma = L_WHERE;
		appendPkValues(sql, rel, pkValues, comma);
		return sql.append(";").toString();
	}

	@Override
	public String getQueryUpdate(String relName, Map<String, String> pkValues, Map<String, String> colValues) {
		StringBuilder sql = new StringBuilder(L_UPDATE).append(escapeFullName(relName)).append(L_SET);
		String comma = "";
		for (Map.Entry<String, String> data : colValues.entrySet()) {
			String column = data.getKey();
			String value = data.getValue();
			sql.append(comma).append(escape(column)).append(" = ");
			if (value == null || value.isEmpty()) {
				sql.append(L_NULL);
			} else if (value.matches(ConnectionUtils.IS_NUMBER_REGEX)) {
				sql.append(value);
			} else {
				sql.append("'").append(value.replace("'", "''")).append("'");
			}
			comma = ", ";
		}
		comma = L_WHERE;
		if (pkValues == null || pkValues.keySet().isEmpty()) {
			sql.append(" WHERE /** TODO: ADD CONDITIONS HERE **/");
		} else {
			appendPkValues(sql, null, pkValues, comma);
		}
		return sql.append(";").toString();
	}

	private void appendPkValues(StringBuilder sql, Relation rel, Map<String, String> pkValues, String comma) {
		// Go through all PK values
		for (Map.Entry<String, String> data : pkValues.entrySet()) {
			String key = data.getKey();
			String value = data.getValue();

			boolean isEscapedType = rel != null && getDataTypes().isEscapedType(rel.getAttributeByName(key).getBehavior().getAttType());

			sql.append(comma).append(escape(key)).append(" = ");
			if (value != null && !isEscapedType && value.matches(ConnectionUtils.IS_NUMBER_REGEX)) {
				sql.append(value);
			} else {
				sql.append("'").append(value == null ? null : value.replace("'", "''")).append("'");
			}
			comma = " AND ";
		}
	}

	@Override
	public SQLBuilder getQueryData(Relation rel, SQLOutputConfig config, SQLBuilder ret) throws DBCommException {
		IConnection currentConn = rel.getDB().getConnection();
		if(currentConn != this){
			return currentConn.getQueryData(rel, config, ret);
		} else {
			if (ret == null) {
				ret = new SQLBuilder(config);
			}
			SQLOutputConfigExport exportConfig = config instanceof SQLOutputConfigExport ? (SQLOutputConfigExport) config : null;
			String pk = rel.getPKey();
			String sql = getQuerySelect(rel, null, !pk.isEmpty() ? pk : null, exportConfig != null && exportConfig.isPreviewMode() ? SQLOutputConfigExport.PREVIEW_DATA_ROWS_LIMIT : 0);
			Result res = new Result(new Query(sql, rel.getDB().getFullName()).run());
			if (res.getRowCount() > 0 && config.exportEmptyLines) {
				ret.n();
			}
			for (int i = 0; i < res.getRowCount(); i++) {
				ret.n().a(getQueryInsert(rel, res.getRow(i)));
			}
			if (exportConfig != null && exportConfig.progressWindow != null) {
				exportConfig.progressWindow.partLoaded();
			}
			return ret;
		}
	}

	@Override
	public String getQueryInsert(Relation rel, ResultRow row) {
		SQLBuilder cols = new SQLBuilder();
		SQLBuilder vals = new SQLBuilder();
		String comma = "";
		for (int i = 0; i < row.cols.size(); i++) {
			String colName = row.cols.get(i).name;
			Attribute attr = rel.getAttributeByName(colName);
			if (attr != null) {
				comma = appendColsValsToInsert(cols, vals, comma, colName, attr, row.vals.get(i));
			}
		}
		return "INSERT INTO " + escapeFullName(getEscapedFullName(rel)) + " (" + cols.toString() + ") VALUES (" + vals.toString() + ");";
	}

	@Override
	public String getQueryInsert(Relation rel, Map<String, String> values) {
		SQLBuilder cols = new SQLBuilder();
		SQLBuilder vals = new SQLBuilder();
		String comma = "";
		for (Map.Entry<String, String> data : values.entrySet()) {
			String colName = data.getKey();
			Attribute attr = rel.getAttributeByName(colName);
			if (attr != null) {
				comma = appendColsValsToInsert(cols, vals, comma, colName, attr, data.getValue());
			}
		}
		return "INSERT INTO " + escapeFullName(getEscapedFullName(rel)) + " (" + cols.toString() + ") VALUES (" + vals.toString() + ");";
	}

	private String appendColsValsToInsert(SQLBuilder cols, SQLBuilder vals, String comma, String colName, Attribute attr, Object value) {
		if (value == null){
			return comma;
        }
		boolean notNullType = !attr.getBehavior().isAttNull();
		boolean noDefault = attr.getBehavior().getDefaultValue().isEmpty();
		boolean valueRequired = notNullType && noDefault && !attr.getBehavior().getAttType().matches("(big)?serial[0-9]?");
		if (!value.equals("") || valueRequired) {
            cols.a(comma).a(escape(colName));
            vals.a(comma);

            boolean isEscapedType = getDataTypes().isEscapedType(attr.getBehavior().getAttType());
            if (!isEscapedType && value.toString().matches(ConnectionUtils.IS_NUMBER_REGEX)) {
                vals.a(value);
            } else {
                vals.quotedEscaped(value);
            }
            comma = ", ";
        }
		return comma;
	}

	@Override
	public String getQueryInsert(String relName, Map<String, String> values) {
		SQLBuilder cols = new SQLBuilder();
		SQLBuilder vals = new SQLBuilder();
		String comma = "";
		for (Map.Entry<String, String> data : values.entrySet()) {
			String value = data.getValue();
			cols.a(comma).a(escape(data.getKey()));
			vals.a(comma);
			if (value == null || value.isEmpty()) {
				vals.a(L_NULL);
			} else if (value.matches(ConnectionUtils.IS_NUMBER_REGEX)) {
				vals.a(value);
			} else {
				vals.quotedEscaped(value);
			}
			comma = ", ";
		}
		return "INSERT INTO " + escapeFullName(relName) + " (" + cols.toString() + ") VALUES (" + vals.toString() + ");";
	}

	@Override
	public String getWhereFromDef(Index ind) {
		if (ind.getBehavior().getDef().matches(".*WHERE.*")) {
			return ind.getBehavior().getDef().replaceAll(".+WHERE\\s+(.+)$", "$1");
		} else {
			return "";
		}
	}

	@Override
	public Query prepare(String sql, DB database) throws DBCommException {
		return new Query(sql, database != null ? database.getFullName() : null);
	}

	@Override
	public Query prepareWithBegin(String sql, DB database) throws DBCommException {
		return new Query("BEGIN;" + sql, database != null ? database.getFullName() : null);
	}

	@Override
	public void runManualRollback(final String revertSQL, final DB database) {
		Schedule.inWorker(() -> runManualRollbackAndWait(revertSQL, database));
	}

	@Override
	public void runManualRollbackAndWait(String revertSQL, DB database) {
		for (String q : SQLUtils.explodeWithSlash(revertSQL)) {
			try {
				prepare(q, database).run();
			} catch (Exception e) {
				Dbg.notImportant("Reverts may fail on individual lines depending on which queries were successfully executed in the first place.",	e);
			}
		}
	}

	@Override
	public ExplainOperation getExplainTree(Result r) {
		return new ExplainOperation("Output", new String[0], 0);
	}

	@Override
	public String getQueryVacuum(String req, List<Relation> relations) {
		StringBuilder ret = new StringBuilder();
		for (Relation rel : relations) {
			ret.append("\n").append(req).append(" ").append(escapeFullName(rel.getFullName())).append(";");
		}
		return ret.length() > 1 ? ret.toString().substring(1) : "";
	}

	@Override
	public void afterCreate(IModelElement elem) {
	}

	@Override
	public void afterAlter(IModelElement elem) {
	}

	@Override
	public void afterDrop(IModelElement elem) {
	}

	@Override
	public String getConnHash(String dbName) {
		return getProtocol() + user + "@" + host + "/" + (dbName != null ? dbName : "");
	}

	@Override
	public void setProgressChecker(ProgressWindow progress) {
		progressChecker = progress;
	}

	@Override
	public ProgressWindow getProgressChecker() {
		return progressChecker;
	}

	@Override
	public String getQuerySelect(DraggableComponent rel, String where, String order, int limit) {
		String limitString = getQueryLimitOffset(limit, 0);
		return "SELECT *\n" +
				FROM + escapeFullName(rel.getFullName()) +
				(where != null && !where.isEmpty() ? "\nWHERE " + where : "") +
				(order != null && !order.isEmpty() ? "\nORDER BY " + order : "") +
				(!limitString.isEmpty() ? "\n" + limitString : "") +
				";";
	}

	@Override
	public String getQuerySelect(Constraint con) {
        String rel1FullName = con.getRel1().getFullName();
        String rel2FullName = con.getRel2().getFullName();

        String rel1Alias = getTableAlias(con.getRel1().getName());
        String rel2Alias = getTableAlias(con.getRel2().getName());

        if(rel1Alias.equals(rel2Alias)){
            rel1Alias += "1";
            rel2Alias += "2";
        }

        return "SELECT *\n" +
				FROM + escapeFullName(rel1FullName) + " " + rel1Alias + "\n" +
				(con.getAttr1().getBehavior().isAttNull() ? "LEFT " : "") +
				"JOIN " + escapeFullName(rel2FullName) + " " + rel2Alias + " " +
				(con.getAttr1().getName().equalsIgnoreCase(con.getAttr2().getName()) ? "USING (" + escape(con.getAttr1().getName()) + ")"
						: "ON " + rel1Alias + "." + escape(con.getAttr1().getName()) + " = " + rel2Alias + "." + escape(con.getAttr2().getName()))
				+ "\n" +
				getQueryLimitOffset(Settings.getInt(Settings.L_DATA_DEFAULT_LIMIT), 0) +
				";";
	}

    String getTableAlias(String name) {

        // Splitting camel case and inline alphanumerics
        name = name
                .replaceAll("([a-z])([A-Z0-9])", ONE_TWO)
                .replaceAll("([0-9])([a-z0-9])", ONE_TWO)
                .replaceAll("([0-9])([a-z0-9])", ONE_TWO);

        // Splitting by special chars
        String[] split = name.toLowerCase().split("[^a-z0-9]+");

        StringBuilder result = new StringBuilder();
        for(String part : split){
            if(part.length() > 0){
                result.append(part.charAt(0));
            }
        }
        return result.toString();
    }

    @Override
	public void setAutocheckEnabled(boolean doCheck) {
		this.doCheck = doCheck;
	}

	protected String getTranslatedType(String type) {
		type = getTranslatedBaseType(type);
		String precision = "";
		if (type.contains("(")) {
			precision = type.replaceAll(".*(\\(.*\\))", "$1");
			type = type.replace(precision, "");
		}
		return getDataTypes().getTranslatedType(type, precision);
	}

	private String getTranslatedBaseType(String type) {
		type = type.toLowerCase();
		String precision = "";
		if (type.contains("(")) {
			precision = type.replaceAll(".*(\\(.*\\))", "$1");
			type = type.replace(precision, "");
		}
		return DataTypes.getTranslatedBaseType(type, precision);
	}
}
