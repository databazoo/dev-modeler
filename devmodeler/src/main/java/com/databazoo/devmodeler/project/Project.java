
package com.databazoo.devmodeler.project;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.conn.*;
import com.databazoo.devmodeler.conn.IColoredConnection.ConnectionColor;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Workspace;
import com.databazoo.devmodeler.tools.comparator.Comparator;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.GC;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Project core. Contains workspaces and databases. Provides information to Project Wizard.
 * @author bobus
 */
public abstract class Project extends VersionedProject implements Comparable, Serializable {
	private static final long serialVersionUID = 1905122041950000001L;

	public enum Type {
		TYPE_ABSTRACT(L_ABSTRACT_MODEL),
		TYPE_MY(L_MYSQL),
		TYPE_PG(L_POSTGRESQL),
		TYPE_MARIA(L_MARIA_DB);

		private final String label;

		Type(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}

		public static Type fromString(String s){
			if(s.length() == 1){
				return Type.values()[Integer.valueOf(s)];
			} else {
				return valueOf(s);
			}
		}
	}
	public static final int TYPE_MY = 1;
	public static final int TYPE_PG = 2;
	public static final int TYPE_MARIA = 4;
	public static final int TYPE_ABSTRACT = 0;

	public static final String L_MYSQL 			= "MySQL";
	public static final String L_POSTGRESQL 	= "PostgreSQL";
	public static final String L_MARIA_DB 		= "MariaDB";
	public static final String L_ABSTRACT_MODEL	= "Abstract model";

	public static Project getCurrent(){
		return ProjectManager.getInstance().getCurrentProject();
	}

	public static DB getCurrDB(){
		if(getCurrent() != null) {
			return getCurrent().getCurrentDB();
		} else {
			throw new IllegalStateException("Current database not available yet");
		}
	}

	public static IConnection getConnectionByType(int connType) {
		return getConnectionByType(connType, "", "", "", "");
	}

	public static IConnection getConnectionByType(String connType) {
		return getConnectionByType(connType, "", "", "", "");
	}

	public static IConnection getConnectionByType(int connType, String name, String host, String user, String pass) {
		switch (connType){
			case Project.TYPE_MY: return new ConnectionMy(name, host, user, pass);
			case Project.TYPE_MARIA: return new ConnectionMaria(name, host, user, pass);
			case Project.TYPE_PG: return new ConnectionPg(name, host, user, pass, false);
		}
		return null;
	}

	private static IConnection getConnectionByType(String connType, String name, String host, String user, String pass) {
		switch (connType){
			case Project.L_MYSQL: return new ConnectionMy(name, host, user, pass);
			case Project.L_MARIA_DB: return new ConnectionMaria(name, host, user, pass);
			case Project.L_POSTGRESQL: return new ConnectionPg(name, host, user, pass, false);
		}
		return null;
	}

	public static String[] getConnectionTypeList() {
		return new String[]{Project.L_MYSQL, Project.L_MARIA_DB, Project.L_POSTGRESQL};
	}

	public static String[] getConnectionTypeListWithAbstract() {
		return new String[]{Project.L_MYSQL, Project.L_MARIA_DB, Project.L_POSTGRESQL, Project.L_ABSTRACT_MODEL};
	}

	public static String getConnectionTypeString(int connType) {
		switch (connType){
			case Project.TYPE_PG: return Project.L_POSTGRESQL;
			case Project.TYPE_MY: return Project.L_MYSQL;
			case Project.TYPE_MARIA: return Project.L_MARIA_DB;
			default: return Project.L_ABSTRACT_MODEL;
		}
	}

	private boolean isSyncing = false;
	private boolean isLoaded = false;

	Integer ordNumber;

	private Workspace currentWorkspace, oldWorkspace;
	private Date lastOpen;
	DB currentDB;
	private IConnection currentConn;
	protected Comparator comparator;

	public final transient Map<String,Integer> usedDataTypes = new HashMap<>();

	public Project(String name){
		this.projectName = name;
		this.lastOpen = new Date();
	}

	protected abstract void addConnection(String name, String host, String user, String pass, int type, ConnectionColor color);
	protected abstract void addDedicatedConnection(String dbName, String name, String host, String user, String pass, int type, String alias, String defaultSchema);

	public synchronized Workspace getCurrentWorkspace() {
		return currentWorkspace;
	}

	public synchronized void setCurrentWorkspace(Workspace currentWorkspace) {
		this.currentWorkspace = currentWorkspace;
	}

	public synchronized Workspace getOldWorkspace() {
		return oldWorkspace != null ? oldWorkspace : currentWorkspace;
	}

	public synchronized void setOldWorkspace(Workspace oldWorkspace) {
		this.oldWorkspace = oldWorkspace;
	}

	public List<DB> getDatabases(){
		return databases;
	}
	public List<Workspace> getWorkspaces(){
		/*if(workspaces.isEmpty()){
		DB db = databases.get(0);
		Workspace ws = new Workspace("my workspace 1", db);
		ws.add(db.getRelationByFullName(db.getRelationNames()[(int)(Math.random()*db.getRelationNames().length-1)]));
		ws.add(db.getRelationByFullName(db.getRelationNames()[(int)(Math.random()*db.getRelationNames().length-1)]));
		workspaces.add(ws);
		}*/
		return workspaces;
	}

	public String[] getDatabaseNames(){
		String[] ret = new String[databases.size()];
		for(int i=0; i<databases.size(); i++){
			ret[i] = databases.get(i).getFullName();
		}
		return ret;
	}

	public DB getCurrentDB(){
		if(currentDB == null && !databases.isEmpty()){
			currentDB = databases.get(0);
		}
		return currentDB;
	}
	public void setCurrentDB(String name){
		for(DB db: databases){
			if(db.getName().equals(name)){
				setCurrentDB(db);
				break;
			}
		}
	}
	public void setCurrentDB(DB db){
		currentDB = db;
		ProjectManager.getInstance().saveProjects();
		GC.invoke();
	}

	public IConnection getCurrentConn(){
		if(currentConn == null && !connections.isEmpty()){
			currentConn = connections.get(0);
		}
		return currentConn;
	}
	public void setCurrentConn(String name){
		for(IConnection c: connections){
			if(c.getName().equals(name)){
				currentConn = c;
				break;
			}
		}
	}

	private IConnection getPrimaryConn(){
		if(!connections.isEmpty()){
			return connections.get(0);
		}else {
			return null;
		}
	}

	public boolean isPrimary(IConnection c){
		IConnection primaryConn = getPrimaryConn();
		return primaryConn != null && primaryConn.getName().equals(c.getName());
	}

	public boolean isSyncing(){
		return isSyncing;
	}
	public boolean isLoaded(){
		return isLoaded;
	}

	/**
	 * Load the project.
	 * @return was project read from disk?
	 */
	protected boolean load(){
		long startTime = System.currentTimeMillis();
		ProjectManager.getInstance().setCurrentProject(this);
		lastOpen = new Date();

		if(isLoaded){
			ProjectManager.getInstance().removeProtectionAfterLoad();
			return false;
		}

		loadFromXML(new File(getProjectPath()), startTime);
		isLoaded = true;
		Schedule.inWorker(() -> {
            for(IConnection conn: connections){
                try {
                    conn.loadEnvironment();
                    break;
                } catch (Exception ex){
                    Dbg.notImportant("Failed to load server info", ex);
                }
            }
        });
		return true;
	}

	void unload(){
		isLoaded = false;
		revisions.clear();
		workspaces.clear();
		recentQueries.clear();
		databases.forEach(DB::unload);
	}

	public synchronized void save(){
		if(ProjectManager.TOO_EARLY_TO_WRITE_CONFIGS || !Canvas.isDefaultZoom()) {
			return;
		}

		Schedule.reInvokeInWorker(Schedule.Named.PROJECT_SAVE, 1000, () -> {
            if(isLoaded && Canvas.isDefaultZoom()){
                saveToXML();
            }
        });
	}

	public void syncWithServer(){
		Dbg.toFile("Synchronizing with database");
		if(!isPrimary(getCurrentConn()) && JOptionPane.showOptionDialog(GCFrame.getActiveWindow(), getCurrentConn().getName()+" is not your primary connection. Do you still want to synchronize with this server?", "Confirm sync", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Synchronize", "Cancel"}, "Synchronize") != 0){
			Dbg.toFile("Synchronization with non-primary server cancelled");
			return;
		}

		if(!isLoaded){
			load();
		}
		if(isSyncing){
			Dbg.toFile("Synchronization is already running, ignoring");
			isSyncing = false;
			return;
		}else{
			isSyncing = true;
		}
		comparator = Comparator.withAutoApply();
		if(!connections.isEmpty()){
			final CountDownLatch latch = new CountDownLatch(databases.size());
			final IConnection remote = getCurrentConn();
			for(final DB localDB: databases){
				Schedule.inWorker(() -> {
                    IConnection dC = getDedicatedConnection(localDB.getName(), remote.getName());
                    DB remoteDB = new DB(Project.this, (dC==null ? remote : dC) , localDB.getName());

                    if(remoteDB.load()){
                        comparator.compareDBs(localDB, remoteDB);
                    }
                    latch.countDown();
                });
			}
			try{
				latch.await();
			} catch(Exception e){
				Dbg.notImportant("Nothing we can do", e);
			}

			comparator.checkIsDifferent();
			Dbg.toFile("Comparison: difference "+(comparator.hadDifference() ? "merged in" : "not found"));
		}
		Schedule.reInvokeInEDT(Schedule.Named.PROJECT_SYNC_LOCK_RELEASE, 5000, () -> isSyncing = false);
	}

	void checkNewRevisions(){
		if(!isLoaded){
			return;
		}
		checkNewRevisionsXML();
	}

	public int getType(){
		return Project.TYPE_ABSTRACT;
	}

	public String getTypeString(){
		return Project.L_ABSTRACT_MODEL;
	}

	@Override
	public String toString(){
		return projectName;
	}

	public List<IConnection> getConnections(){
		return connections;
	}

	public String[] getConnectionNames(){
		String[] ret = new String[connections.size()];
		for(int i=0; i<ret.length; i++){
			ret[i] = connections.get(i).getFullName();
		}
		return ret;
	}

	public String getLastOpenString(){
		Calendar cal = Calendar.getInstance();
		String today = Config.DATE_FORMAT.format(cal.getTime());

		cal.add(Calendar.DATE, -1);
		String yesterday = Config.DATE_FORMAT.format(cal.getTime());

		//cal.add(Calendar.DATE, -12);

		if(lastOpen.getTime() == 0){
			return "Never";

		}else{
			String formatted = Config.DATE_FORMAT.format(lastOpen);
			if(formatted.equals(today)){
				return "Today";

			}else if(formatted.equals(yesterday)){
				return "Yesterday";

			}else /*if(lastOpen.after(cal.getTime()))*/{
				return Math.round((new Date().getTime()-lastOpen.getTime())*0.001f/60/60/24)+" days ago ("+formatted+")";

			/*}else{
				return formatted;*/
			}
		}
	}

	Date getLastOpen(){
		return lastOpen;
	}

	void setLastOpen(long timestamp) {
		lastOpen = new Date(timestamp);
	}

	public void setLoaded(){
		isLoaded = true;
	}

	public IConnection getConnectionByName(String connName){
		for(IConnection c: connections){
			if(c.getFullName().equals(connName) || c.getName().equals(connName)){
				return c;
			}
		}
		return null;
	}

	public IConnection getDedicatedConnection(String dbName, String currConnName) {
		return dedicatedConnections.get(dbName+"~"+currConnName);
	}

	public void addDedicatedConnection(String dbName, String currConnName, IConnection c) {
		dedicatedConnections.put(dbName+"~"+currConnName, c);
	}

	public void setDatabases(List<DB> dbList){
		databases = new CopyOnWriteArrayList<>(dbList);
	}

	public abstract IConnection copyConnection(IConnection c);

	public Workspace getWorkspaceByName(String workspaceName) {
		for(Workspace w: workspaces){
			if(w.toString().equals(workspaceName)){
				return w;
			}
		}
		return null;
	}

	public String[] getWorkspaceNames(){
		String[] ret = new String[workspaces.size()];
		for(int i=0; i<workspaces.size(); i++){
			ret[i] = workspaces.get(i).toString();
		}
		return ret;
	}

	public String[] getWorkspaceNames(DB db){
		return workspaces.stream()
				.filter(workspace -> workspace.getDB() == db)
				.map(Object::toString)
				.collect(Collectors.toList())
				.toArray(new String[0]);
	}

	public Revision getRevisionByName(String revisionName) {
		for(Revision r: revisions){
			if(r.getName().equals(revisionName)){
				return r;
			}
		}
		return null;
	}

	@Override
	public int compareTo(Object o){
		if(o instanceof Project){
			return ordNumber.compareTo(((Project)o).ordNumber);
		}
		return 0;
	}
}
