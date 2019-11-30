package com.databazoo.devmodeler.conn;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.window.ProgressWindow;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.wizards.CreateDbWizard;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.GC;
import com.databazoo.tools.SQLUtils;
import com.databazoo.tools.Schedule;

import static com.databazoo.devmodeler.conn.ConnectionUtils.COMMA;

/**
 * Connection's internal classes like Query, Row, Column and Result.
 *
 * @author bobus
 */
public abstract class ConnectionBase implements IConnection {

	static final Map<String, Float> CONN_VERSIONS = new HashMap<>();
	static final Map<String, java.sql.Connection> CONN_CACHE = new HashMap<>();
	static final Map<String, Date> CONN_LAST_USE = new HashMap<>();

	/**
	 * Thread pool for parallel loading of database objects. It is limited to only allow several open connections to target DB.
	 *
	 * Implementation is static, so all connections share the limit even if they are connecting to different servers.
	 */
	private static final Executor THREAD_POOL = new ThreadPoolExecutor(1, 5, 10L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(250));

	protected String name = "";
	protected String host;
	protected String user;
	protected String pass;
	protected String version;
	protected String defaultSchema;
	protected String dbAlias;
	protected ConnectionColor color;
	public Float versionMajor;

	protected boolean status = true;
	protected boolean statusChecked = false;
	protected int type = 0;
	ProgressWindow progressChecker;

	boolean doCheck = true;

	protected abstract void runVersionCheck();

	@Override
	public void loadDB(DB db) throws DBCommException {
		Dbg.toFile();

		long startTime = System.currentTimeMillis();

		loadSchemas(db);
		if(progressChecker != null){
			progressChecker.partLoaded();
		}

		loadRelations(db);
		if(progressChecker != null){
			progressChecker.partLoaded();
		}

		loadAttributes(db);
		if(progressChecker != null){
			progressChecker.partLoaded();
		}

		final DBCommException[] dbCommException = { null };
		if(Settings.getBool(Settings.L_PERFORM_LOAD_PARALLEL)) {

			// Parallel load the rest
			CountDownLatch latch = new CountDownLatch(5);

			THREAD_POOL.execute(() -> loadFunctions(db, latch, dbCommException));
			THREAD_POOL.execute(() -> loadIndexes(db, latch, dbCommException));
			THREAD_POOL.execute(() -> loadViews(db, latch, dbCommException));
			THREAD_POOL.execute(() -> loadSequences(db, latch, dbCommException));
			THREAD_POOL.execute(() -> loadConstraints(db, latch, dbCommException));

			// Wait for workers to finish
			try {
				latch.await();
			} catch (Exception e) {
				Dbg.fixme("Interrupted DB load", e);
			}

		} else {
			loadFunctions(db, null, dbCommException);
			loadIndexes(db, null, dbCommException);
			loadViews(db, null, dbCommException);
			loadSequences(db, null, dbCommException);
			loadConstraints(db, null, dbCommException);
		}

		Dbg.info("Database " + db.getName() + " loaded from server in " + ((System.currentTimeMillis() - startTime)/1000.0) + "s");

		// Check for exceptions
		if(dbCommException[0] != null){
			throw dbCommException[0];
		}
	}

	private void loadConstraints(DB db, CountDownLatch latch, DBCommException[] dbCommException) {
		try {
            loadConstraints(db);
            if(progressChecker != null){
                progressChecker.partLoaded();
            }
        } catch (DBCommException ex){
            dbCommException[0] = ex;
        } finally {
			if(latch != null) {
				latch.countDown();
			}
        }
	}

	private void loadSequences(DB db, CountDownLatch latch, DBCommException[] dbCommException) {
		try {
            loadSequences(db);
            if(progressChecker != null){
                progressChecker.partLoaded();
            }
        } catch (DBCommException ex){
            dbCommException[0] = ex;
        } finally {
			if(latch != null) {
				latch.countDown();
			}
        }
	}

	private void loadViews(DB db, CountDownLatch latch, DBCommException[] dbCommException) {
		try {
            loadViews(db);
            if(progressChecker != null){
                progressChecker.partLoaded();
            }
        } catch (DBCommException ex){
            dbCommException[0] = ex;
        } finally {
			if(latch != null) {
				latch.countDown();
			}
        }
	}

	private void loadIndexes(DB db, CountDownLatch latch, DBCommException[] dbCommException) {
		try {
            loadIndexes(db);
            if(progressChecker != null){
                progressChecker.partLoaded();
            }
        } catch (DBCommException ex){
            dbCommException[0] = ex;
        } finally {
			if(latch != null) {
				latch.countDown();
			}
        }
	}

	private void loadFunctions(DB db, CountDownLatch latch, DBCommException[] dbCommException) {
		try {
            loadFunctions(db);
            if (progressChecker != null) {
                progressChecker.partLoaded();
            }
            loadTriggers(db);
            if (progressChecker != null) {
                progressChecker.partLoaded();
            }
        } catch (DBCommException ex){
            dbCommException[0] = ex;
        } finally {
        	if(latch != null) {
				latch.countDown();
			}
        }
	}

	public class Query implements IConnectionQuery {

		private static final String QUERY_FAILED_WILL_BE_LOGGED_ON_HIGHER_LEVEL = "Query failed. Will be logged on higher level.";
		static final String UNKNOWN_DATABASE = "Unknown database";
		static final String UNKNOWN_DATABASE_ORA_CODE = "3D000";
		static final String NO_DATA_SQL_STATE = "02000";

		protected transient java.sql.Connection con;
		protected String dbName;

		transient PreparedStatement pst;
		transient ResultSet rs;
		String failedReason;
		float runTime;
		long startTime;
		StringBuilder warnString;
		boolean noResult = false;
		SQLWarning lastWarning;
		int affectedRows;

		boolean useExecuteUpdate = false;

		protected Query() throws DBCommException {
		}

		protected Query(String sql) throws DBCommException {
			initConnection(sql, dbAlias == null ? Project.getCurrDB().getName() : dbAlias);
		}

		Query(String sql, String dbName) throws DBCommException {
			initConnection(sql, dbAlias == null ? dbName : dbAlias);
		}

		final void initConnection(String sql, final String dbName) throws DBCommException {
			if (host.isEmpty()) {
				return;
			}
			if (version == null && !sql.contains("version")) {
				runVersionCheck();
			}
			this.dbName = dbName;

			// clean up
			sql = sql.replaceAll(";?\\s*$", "");
			sql = SQLUtils.escapeQM(sql);

			try {
				con = getCachedConnection(dbName);
				pst = con.prepareStatement(sql);
				useExecuteUpdate = checkExecuteFunction(sql);
			} catch (SQLException ex) {
				Dbg.notImportantAtAll(QUERY_FAILED_WILL_BE_LOGGED_ON_HIGHER_LEVEL, ex);
				failedReason = ex.getMessage();
				Dbg.info("SQL state: " + ex.getSQLState() + ", reason: " + failedReason);
				if ((ex.getSQLState() != null && ex.getSQLState().equals(UNKNOWN_DATABASE_ORA_CODE)) || failedReason.contains(UNKNOWN_DATABASE) || failedReason.matches(".*atabase.*does not exist.*")) {
					Schedule.inEDT(Schedule.CLICK_DELAY, () -> CreateDbWizard.get().drawCreateDB(ConnectionBase.this, dbAlias != null ? dbAlias : dbName));
					throw new DBCommException(failedReason, sql);
				} else if (ex.getSQLState() != null && !ex.getSQLState().equals(NO_DATA_SQL_STATE)) {
					setConAvailable(con);
					throw new DBCommException(failedReason, sql);
				}
			}
		}

		java.sql.Connection getCachedConnection(String dbName) throws SQLException {
			Calendar now = Calendar.getInstance();
			now.add(Calendar.SECOND, -Settings.getInt(Settings.L_PERFORM_REOPEN_CONN));

			java.sql.Connection c;
			Date last;
			String connHash = getConnHash(dbName);
			synchronized (CONN_CACHE) {
				c = CONN_CACHE.get(connHash);
				last = CONN_LAST_USE.get(connHash);
			}

			//Dbg.info("conn: "+c+" last: "+last);

			if (c == null || last == null || last.before(now.getTime())) {
				//Dbg.info("Opening new connection to " + getConnURL(dbName));
				c = DriverManager.getConnection(getConnURL(dbName), user, pass);
				if (!isSupported(SupportedElement.TRANSACTION_AUTO_ROLLBACK)) {
					c.setAutoCommit(false);
				}
			}
			synchronized (CONN_CACHE) {
				CONN_CACHE.remove(connHash);
				CONN_LAST_USE.remove(connHash);
			}
			return c;
		}

		private void setConAvailable(java.sql.Connection c) {
			synchronized (CONN_CACHE) {
				if (CONN_CACHE.get(getConnHash(dbName)) != null) {
					GC.invoke();
				}
				if (c != null) {
					CONN_CACHE.put(getConnHash(dbName), c);
					CONN_LAST_USE.put(getConnHash(dbName), new Date());
				}
			}
		}

		@Override
		public Query run() throws DBCommException {
			if (pst == null) {
				runTime = 0;
				rs = null;
				noResult = true;
				return this;
			}
			startTime = System.currentTimeMillis();
			warnString = new StringBuilder();
			try {
				affectedRows = 0;
				if (useExecuteUpdate) {
					pst.executeUpdate();
					int count;
					while ((count = pst.getUpdateCount()) >= 0) {
						affectedRows += count;
						warnString.append("\n").append("Affected ").append(count).append(count == 1 ? " row." : " rows.");
						pst.getMoreResults();
					}
					rs = null;
					noResult = true;
				} else {
					rs = pst.executeQuery();
				}
				if (!isSupported(SupportedElement.TRANSACTION_AUTO_ROLLBACK)) {
					con.commit();
				}
				checkWarnings();
			} catch (SQLException ex) {
				Dbg.notImportant(QUERY_FAILED_WILL_BE_LOGGED_ON_HIGHER_LEVEL, ex);
				if (!isSupported(SupportedElement.TRANSACTION_AUTO_ROLLBACK)) {
					try {
						con.rollback();
					} catch (SQLException e) {
						Dbg.notImportant("Transaction rollback failed. There's nothing we can do.", e);
					}
				}
				checkWarnings();
				failedReason = ex.getMessage();
				Dbg.info("SQL state: " + ex.getSQLState() + ", reason: " + failedReason);
				if (ex.getSQLState() != null && !ex.getSQLState().equals(NO_DATA_SQL_STATE)) {
					setConAvailable(con);
					throw new DBCommException(failedReason, "");
				} else {
					noResult = true;
				}
			}
			runTime = (System.currentTimeMillis() - startTime) * .001f;
			return this;
		}

		@Override
		public Result fetchResult() throws DBCommException {
			return new Result(this);
		}

		@Override
		public void cancel() {
			try {
				if (pst != null && !pst.isClosed()) {
					pst.cancel();
					setConAvailable(con);
				}
			} catch (SQLException e) {
				Dbg.notImportant("Cancel failed, not much we can do.", e);
			}
		}

		@Override
		public void commit() throws DBCommException {
			executeCaughtSQL("COMMIT");
		}

		@Override
		public void rollback() throws DBCommException {
			executeCaughtSQL("ROLLBACK");
		}

		private void executeCaughtSQL(String sql) throws DBCommException {
			try {
				con.prepareStatement(sql).executeQuery();
			} catch (SQLException ex) {
				logFailReason(ex);
				if (!ex.getSQLState().equals(NO_DATA_SQL_STATE)) {
					throw new DBCommException(failedReason, sql);
				}
			}
		}

		@Override
		public void checkWarnings() {
			try {
				if (lastWarning == null) {
					lastWarning = pst.getWarnings();
					if (lastWarning != null) {
						warnString.append("\n").append(lastWarning.toString());
					}
				}
				if (lastWarning != null) {
					while (true) {
						SQLWarning w = lastWarning.getNextWarning();
						if (w != null) {
							warnString.append("\n").append(w.toString());
							lastWarning = w;
						} else {
							break;
						}
					}
				}
			} catch (Exception e) {
				Dbg.notImportant("Warning read failed. Users will hardly notice unless this happens every time.", e);

			}
		}

		protected void log(int log) {
			if (failedReason != null) {
				DesignGUI.getInfoPanel().writeFailed(log, getCleanError(failedReason));
			} else {
				DesignGUI.getInfoPanel().writeOK(log);
			}
		}

		@Override
		public boolean next() {
			boolean ret = false;
			try {
				if (rs != null) {
					ret = rs.next();
				}
			} catch (SQLException ex) {
				logFailReason(ex);
			}
			return ret;
		}

		@Override
		public void close() {
			try {
				if (rs != null) {
					rs.close();
					rs = null;
				}
				if (pst != null) {
					pst.close();
					pst = null;
				}
				if (con != null) {
					setConAvailable(con);
				}
			} catch (SQLException ex) {
				logFailReason(ex);
			}
		}

		@Override
		public String getRunningTimeString() {
			return ConnectionUtils.formatTime(((System.currentTimeMillis() - startTime) * .001f));
		}

		@Override
		public float getTime() {
			return runTime;
		}

		@Override
		public String getTimeString() {
			return ConnectionUtils.formatTime(runTime);
		}

		@Override
		public String getWarnings() {
			return warnString != null ? warnString.toString() : "";
		}

		protected List<ResultColumn> getColumns() {
			List<ResultColumn> cols = new ArrayList<>();
			try {
				if (rs != null) {
					ResultSetMetaData rsmd = rs.getMetaData();
					int numColumns = rsmd.getColumnCount();
					for (int i = 1; i < numColumns + 1; i++) {
						cols.add(new ResultColumn(rsmd.getColumnName(i), rsmd.getColumnTypeName(i)));
					}
				}
			} catch (SQLException ex) {
				logFailReason(ex);
			}
			return cols;
		}

		@Override
		public String getString(int i) {
			return getString(() -> rs.getString(i));
		}

		@Override
		public String getString(String col) {
			return getString(() -> rs.getString(col));
		}

		private String getString(SupplierWithIO<String> function) {
			try {
				return function.getWithException();
			} catch (SQLException ex) {
				logFailReason(ex);
			}
			return "";
		}

		@Override
		public int getInt(int i) {
			return getInt(() -> rs.getInt(i));
		}

		@Override
		public int getInt(String col) {
			return getInt(() -> rs.getInt(col));
		}

		private int getInt(SupplierWithIO<Integer> function) {
			try {
				return function.getWithException();
			} catch (SQLException ex) {
				logFailReason(ex);
			}
			return 0;
		}

		@Override
		public long getLong(int i) {
			return getLong(() -> rs.getLong(i));
		}

		@Override
		public long getLong(String col) {
			return getLong(() -> rs.getLong(col));
		}

		private long getLong(SupplierWithIO<Long> function) {
			try {
				return function.getWithException();
			} catch (SQLException ex) {
				logFailReason(ex);
			}
			return 0;
		}

		@Override
		public double getDouble(int i) {
			return getDouble(() -> rs.getDouble(i));
		}

		@Override
		public double getDouble(String col) {
			return getDouble(() -> rs.getDouble(col));
		}

		private double getDouble(SupplierWithIO<Double> function) {
			try {
				return function.getWithException();
			} catch (SQLException ex) {
				logFailReason(ex);
			}
			return 0;
		}

		@Override
		public boolean getBool(int i) {
			return getBool(() -> rs.getBoolean(i));
		}

		@Override
		public boolean getBool(String col) {
			return getBool(() -> rs.getBoolean(col));
		}

		private boolean getBool(SupplierWithIO<Boolean> function) {
			try {
				return function.getWithException();
			} catch (SQLException ex) {
				logFailReason(ex);
			}
			return false;
		}

		@Override
		public int[] getIntVector(int i) {
			return getIntVector(() -> rs.getString(i));
		}

		@Override
		public int[] getIntVector(String col) {
			return getIntVector(() -> rs.getString(col));
		}

		private int[] getIntVector(SupplierWithIO<String> function){
			try {
				String retVal = function.getWithException();
				if (retVal != null && !retVal.isEmpty()) {
					String[] retStr = retVal.split(" ");
					int[] ret = new int[retStr.length];
					for (int j = 0; j < retStr.length; j++) {
						ret[j] = Integer.parseInt(retStr[j]);
					}
					return ret;
				}
			} catch (SQLException ex) {
				logFailReason(ex);
			}
			return new int[0];
		}

		@Override
		public int[] getIntArray(int i) {
			return getIntArray(() -> rs.getString(i));
		}

		@Override
		public int[] getIntArray(String col) {
			return getIntArray(() -> rs.getString(col));
		}

		private int[] getIntArray(SupplierWithIO<String> function){
			try {
				String retVal = function.getWithException();
				if (retVal != null && !retVal.isEmpty()) {
					String[] retStr = retVal.substring(1, retVal.length() - 1).split(COMMA);
					int[] ret = new int[retStr.length];
					for (int j = 0; j < retStr.length; j++) {
						ret[j] = Integer.parseInt(retStr[j]);
					}
					return ret;
				}
			} catch (SQLException ex) {
				logFailReason(ex);
			}
			return new int[0];
		}

		@Override
		public String[] getStringArray(int i) {
			return getStringArray(() -> rs.getString(i));
		}

		@Override
		public String[] getStringArray(String col) {
			return getStringArray(() -> rs.getString(col));
		}

		private String[] getStringArray(SupplierWithIO<String> function) {
			try {
				String retVal = function.getWithException();
				if (retVal != null && !retVal.equals("{}")) {
					return retVal.substring(1, retVal.length() - 1).split(COMMA);
				}
			} catch (SQLException ex) {
				logFailReason(ex);
			}
			return new String[0];
		}

		@Override
		public void useExecuteUpdate(boolean executeUpdate) {
			useExecuteUpdate = executeUpdate;
		}

		boolean checkExecuteFunction(String sql) {
			sql = sql.replaceAll("\r", "");
			sql = sql.replaceAll("(?ims)--[^\n]+[\n]?", "");
			sql = sql.replaceAll("(?ims)/\\*(.*?)\\*/", "");
			sql = sql.replaceAll("^[\t\n ]+", "");
			sql = sql.substring(0, sql.length() > 10 ? 10 : sql.length());
			return sql.contains("COMMENT ON") ||
					sql.contains("INSERT") ||
					sql.contains("UPDATE") ||
					(sql.contains("DELETE") && !sql.equals("DELETE PLA")) ||
					sql.contains("TRUNCATE") ||
					sql.contains("CREATE") ||
					sql.contains("DROP") ||
					sql.contains("ALTER") ||
					sql.contains("VACUUM") ||
					sql.contains("ANALYZE") ||
					sql.contains("OPTIMIZE") ||
					sql.contains("REPAIR");
		}

		private void logFailReason(SQLException ex) {
			Dbg.notImportant(QUERY_FAILED_WILL_BE_LOGGED_ON_HIGHER_LEVEL, ex);
			failedReason = ex.getMessage();
		}
	}

	@FunctionalInterface
	private interface SupplierWithIO<T> {
		T getWithException() throws SQLException;
	}
}
