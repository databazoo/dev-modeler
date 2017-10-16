
package com.databazoo.devmodeler.wizards.project;

import java.util.ArrayList;
import java.util.List;

import com.databazoo.devmodeler.conn.ConnectionMaria;
import com.databazoo.devmodeler.conn.ConnectionMy;
import com.databazoo.devmodeler.conn.ConnectionPg;
import com.databazoo.devmodeler.conn.IColoredConnection;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.gui.Menu;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.tools.Dbg;

import static com.databazoo.devmodeler.wizards.ConnectionChecker.L_HOST;
import static com.databazoo.devmodeler.wizards.ConnectionChecker.L_PASS;
import static com.databazoo.devmodeler.wizards.ConnectionChecker.L_SERVER;
import static com.databazoo.devmodeler.wizards.ConnectionChecker.L_USER;

/**
 * Servers table model
 * @author bobus
 */
class ConnectionsTableModel extends ProjectTableModel {

	static final String DEFAULT_URL	= "127.0.0.1";
    private static final String DEFAULT_USER	= "root";
    private static final String DEFAULT_PASS	= "";

	private static final String L_CONN_DEVELOPMENT	= "Development";
    private static final String L_CONN_TEST			= "Test";
    private static final String L_CONN_PRESENTATION	= "Presentation";
    private static final String L_CONN_PRODUCTION	= "Production";

	private final String[] cols = {"", L_SERVER, L_HOST, L_USER, L_PASS, ""};
	private int rows = 3;
	transient List<IConnection> conns;
	private IConnection newConn = new ConnectionMy("", "", "", "");
	private boolean isPreset = true;

	ConnectionsTableModel(){
		reset();
	}

	private void reset(){
		conns = new ArrayList<>();
		conns.add(new ConnectionPg(L_CONN_DEVELOPMENT, DEFAULT_URL, DEFAULT_USER, DEFAULT_PASS));
		conns.add(new ConnectionPg(L_CONN_TEST, DEFAULT_URL, DEFAULT_USER, DEFAULT_PASS));
		conns.add(new ConnectionPg(L_CONN_PRESENTATION, DEFAULT_URL, DEFAULT_USER, DEFAULT_PASS));
		conns.add(new ConnectionPg(L_CONN_PRODUCTION, DEFAULT_URL, DEFAULT_USER, DEFAULT_PASS));
	}

	/*protected boolean isPreset(){
		return isPreset;
	}*/

	void checkDBType(){
		List<IConnection> newConns = new ArrayList<>(conns.size());
		for (IConnection conn : conns) {
			if (getWizard().ui.myRadio.isSelected()) {
				newConns.add(new ConnectionMy(conn.getName(), conn.getHost(), conn.getUser(), conn.getPass(), conn.getType()));
			}else if (getWizard().ui.pgRadio.isSelected()) {
				newConns.add(new ConnectionPg(conn.getName(), conn.getHost(), conn.getUser(), conn.getPass(), conn.getType(), false));
			}else if (getWizard().ui.mariaRadio.isSelected()) {
				newConns.add(new ConnectionMaria(conn.getName(), conn.getHost(), conn.getUser(), conn.getPass(), conn.getType()));
			}else {
				newConns.add(new ConnectionPg(conn.getName(), conn.getHost(), conn.getUser(), conn.getPass(), conn.getType(), false));
			}
		}
		conns = newConns;
		if(getWizard().ui.myRadio.isSelected()){
			newConn = new ConnectionMy("", "", "", "");
		}else if(getWizard().ui.pgRadio.isSelected()){
			newConn = new ConnectionPg("", "", "", "", false);
		}else if(getWizard().ui.mariaRadio.isSelected()){
			newConn = new ConnectionMaria("", "", "", "");
		}else{
			newConn = new ConnectionPg("", "", "", "");
		}
	}

	@Override
	public String getColumnName(int col) {
		return cols[col];
	}

	@Override
	public int getColumnCount(){
		return cols.length;
	}

	@Override
	public int getRowCount(){
		return rows+1;
	}

	@Override
	public Object getValueAt(int row, int col) {
		if (col == 0) {
			if(row >= rows){
				return " + ";
			}else{
				return " "+(row+1);
			}
		} else {
			IConnection conn;
			if(row >= rows){
				conn = newConn;
			}else if(isPreset && row == rows-1){
				conn = conns.get(conns.size()-1);
			}else{
				conn = conns.get(row);
			}
			switch (col) {
				case 1: return conn.getName();
					//case 2: return conn.getName().equals("") && conn.getHost().equals("") && conn.getUser().equals("") && conn.getPass().equals("") ? "" : conn.getTypeName();
				case 2: return conn.getHost();
				case 3: return conn.getUser();
				case 4: return conn.getPass().replaceAll(".{1}", "*");
				case 5: return conn.getStatusChecked() ? conn.getStatusHTML() : "";
				case 11: return conn.getColor();
				default:
					return "";
			}
		}
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		try {
			String val = (String) value;
			IConnection conn;
			if(row >= rows){
				conn = newConn;
			}else if(isPreset && row == rows-1){
				conn = conns.get(conns.size()-1);
			}else{
				conn = conns.get(row);
			}
			switch (col) {
				case 1: conn.setName(val); break;
				//case 2: conn.setType(val); break;
				case 2: conn.setHost(val); break;
				case 3: conn.setUser(val); break;
				case 4: conn.setPass(val); break;
				case 11: conn.setColor(IColoredConnection.ConnectionColor.fromString(val));
			}
			if(row >= rows && col != 2)
			{
				finalizePresetConnections();
				conns.add(Project.getConnectionByType(getConnectionTypeByRadio(), conn.getName(), conn.getHost(), conn.getUser(), conn.getPass()));
				newConn = Project.getConnectionByType(getConnectionTypeByRadio());
				setRows(conns.size());

				// Send new connection over to project because conns != listedProject.getConnections()
				if(getWizard().listedProject != null){
					getWizard().listedProject.getConnections().add(conns.get(conns.size()-1));
					resetProjectGUI();
					Menu.getInstance().setCompareAvailable(true);
				}

			}else{
				repaintTable();
			}

			if(getWizard().listedProject != null && 2 <= col && col <= 4){
				for(int i=0; i<getWizard().tablesUI.dbsTableModel.getRowCount()-1; i++) {
					getWizard().tablesUI.dedicatedTableModel.setValueWoCheck(value, i*(getRowCount()-1)+(row), col+1);
				}
			}
		} catch (Exception e){
			Dbg.fixme("Setting value in connections table failed", e);
		}
	}

	private int getConnectionTypeByRadio() {
		if(getWizard().listedProject == null){
			if(getWizard().ui.myRadio.isSelected()){
				return Project.TYPE_MY;

			}else if(getWizard().ui.mariaRadio.isSelected()){
				return Project.TYPE_MARIA;

			}else{
				return Project.TYPE_PG;
			}
		}else{
			return getWizard().listedProject.getType();
		}
	}

	public void setRows(int cnt){
		rows = cnt;
		repaintTable();
	}

	public void setConnections(List<IConnection> c){
		conns = new ArrayList<>(c);
		isPreset = false;
		rows = conns.size();
		repaintTable();
	}

	private void repaintTable(){
		fireTableDataChanged();
		if(getWizard() != null) {
			if(getWizard().tablesUI.connectionsTable != null) {
				getWizard().tablesUI.connectionsTable.setModel(this);
			}
			getWizard().tablesUI.setConnTableCols();
		}
	}

	void finalizePresetConnections(){
		if(isPreset){
			ArrayList<IConnection> newConns = new ArrayList<>();
			for(int i=0; i<=rows-1; i++){
				if(i == rows-1){
					i = conns.size()-1;
				}
				newConns.add(Project.getConnectionByType(getConnectionTypeByRadio(), conns.get(i).getName(), conns.get(i).getHost(), conns.get(i).getUser(), conns.get(i).getPass()));
			}
			conns = newConns;
			isPreset = false;
		}
	}

	IConnection getFirstConnection(){
		if(conns.size() > 0){
			return conns.get(0);
		}
		return null;
	}
}
