
package com.databazoo.devmodeler.wizards.project;

import java.util.HashMap;
import java.util.Map;

import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;
import com.databazoo.tools.Dbg;

import static com.databazoo.devmodeler.wizards.ConnectionChecker.L_HOST;
import static com.databazoo.devmodeler.wizards.ConnectionChecker.L_PASS;
import static com.databazoo.devmodeler.wizards.ConnectionChecker.L_SERVER;
import static com.databazoo.devmodeler.wizards.ConnectionChecker.L_USER;

/**
 * Table model for detailed configuration (connections per database)
 * @author bobus
 */
class DedicatedTableModel extends ProjectTableModel {

	private static final String L_DB_ALIAS = "DB Alias";
	private static final String L_DEFAULT_SCHEMA = "Default schema";

	private final String[] cols = {"Database", L_SERVER, L_DB_ALIAS, L_HOST, L_USER, L_PASS, ""};
	transient Map<String, IConnection> dedicatedConnections;
	private boolean doCheck = true;

	DedicatedTableModel(){
		super();
		reset();
	}

	final void reset(){
		dedicatedConnections = new HashMap<>();
		cols[2] = L_DB_ALIAS;
	}

	@Override
	public String getColumnName(int col) {
		return cols[col];
	}

	@Override
	public int getRowCount(){
		ProjectWizard wizard = getWizard();
		if (wizard == null) {
			return 1;
		}
		int dbsCount = wizard.tablesUI.dbsTableModel.getRowCount()-1;
		int connsCount = wizard.tablesUI.connectionsTableModel.getRowCount()-1;
		return dbsCount * connsCount;
	}

	@Override
	public int getColumnCount(){
		return cols.length;
	}

	@Override
	public Object getValueAt(int row, int col) {
		if(row >= getRowCount()){
			return "";
		}else{
			int connections = (getWizard().tablesUI.connectionsTableModel.getRowCount()-1);
			DB currDB = getWizard().tablesUI.dbsTableModel.databases.get(row/connections);
			IConnection currConn = getWizard().tablesUI.connectionsTableModel.conns.get(row % connections);

			IConnection dC = dedicatedConnections.get(currDB.getName()+"~"+currConn.getName());
			if(dC == null){
				if(getWizard().listedProject != null){
					dC = getWizard().listedProject.copyConnection(currConn);
					dC.setDbAlias(currDB.getName());
				}else{
					dC = Project.getConnectionByType(getWizard().projectType,
							currConn.getName(),
							currConn.getHost(),
							currConn.getUser(),
							currConn.getPass());
					if(dC != null) {
						dC.setDbAlias(currDB.getName());
					}else{
						return "";
					}
				}
				dedicatedConnections.put(currDB.getName()+"~"+currConn.getName(), dC);
			}
			switch (col) {
				case 0: return currDB.getName();
				case 1: return currConn.getName();
				case 2: return cols[2].equals(L_DB_ALIAS) ? dC.getDbAlias() : dC.getDefaultSchema();
				case 3: return dC.getHost();
				case 4: return dC.getUser();
				case 5: return dC.getPass().replaceAll(".{1}", "*");
				case 6: return dC.getStatusChecked() ? dC.getStatusHTML() : "";
				default:
					return "";
			}
		}
	}

	@Override
	public synchronized void setValueAt(Object value, int row, int col) {
		try {
			String val = (String) value;

			int connectionCount = getWizard().tablesUI.connectionsTableModel.getRowCount()-1;
			DB currentDatabase = getWizard().tablesUI.dbsTableModel.databases.get(row/connectionCount);
			IConnection currentConnection = getWizard().tablesUI.connectionsTableModel.conns.get(row % connectionCount);

			IConnection dedicatedConnection = dedicatedConnections.get(currentDatabase.getName()+"~"+currentConnection.getName());
			if(dedicatedConnection != null){
				dedicatedConnection.setAutocheckEnabled(doCheck);
				switch (col) {
					case 2: if(cols[2].equals(L_DB_ALIAS)){ dedicatedConnection.setDbAlias(val); }else{ dedicatedConnection.setDefaultSchema(val); } break;
					case 3: dedicatedConnection.setHost(val); break;
					case 4: dedicatedConnection.setUser(val); break;
					case 5: dedicatedConnection.setPass(val); break;
				}
				if(getWizard().listedProject != null) {
					getWizard().listedProject.save();
					ProjectManager.getInstance().saveProjects();
				}
				dedicatedConnection.setAutocheckEnabled(true);
			}
		} catch (Exception e){
			Dbg.fixme("Setting value in dedicated table failed", e);
		}
	}

	void setValueWoCheck(Object value, int row, int col) {
		doCheck = false;
		setValueAt(value, row, col);
		doCheck = true;
	}

	void removeDB(String dbName) {
		for (IConnection conn : getWizard().tablesUI.connectionsTableModel.conns) {
			dedicatedConnections.remove(dbName+"~" + conn.getName());
		}
	}
}
