
package com.databazoo.devmodeler.wizards.project;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.project.ProjectManager;

/**
 * Databases list model
 * @author bobus
 */
class DBsTableModel extends ProjectTableModel {

	private final String[] cols = {"", "Database"};
	protected transient List<DB> databases = new CopyOnWriteArrayList<>();

	DBsTableModel(){
		super();
		reset();
	}

	final void reset(){
		databases = new CopyOnWriteArrayList<>();
		databases.add(new DB(null, "new_database"));
	}

	@Override
	public String getColumnName(int col) {
		return cols[col];
	}

	@Override
	public int getRowCount(){
		return databases.size()+1;
	}

	@Override
	public int getColumnCount(){
		return cols.length;
	}

	@Override
	public Object getValueAt(int row, int col) {
		if (col == 0) {
			if(row >= databases.size()){
				return " + ";
			}else{
				return " "+(row+1);
			}
		} else if(row < databases.size()){
			return databases.get(row).getName();
		}else{
			return "";
		}
	}

	@Override
	public void setValueAt(Object value, int row, int col){
		if(row >= 0){
			if(row < databases.size()){
				databases.get(row).setName((String) value);
			}else{
				databases.add(new DB(null, (String) value));
				resetProjectGUI();
			}
			fireTableDataChanged();
			getWizard().tablesUI.dedicatedTableModel.fireTableDataChanged();
			if(getWizard().listedProject != null){
				getWizard().listedProject.setDatabases(databases);
				getWizard().listedProject.save();
				getWizard().tablesUI.connectionsTableModel.resetProjectGUI();
				ProjectManager.getInstance().saveProjects();
			}
		}
	}

	String getDatabasesCommaSeparated(){
		StringBuilder out = new StringBuilder();
		boolean comma = false;
		for(DB db : databases){
			if(comma){
				out.append(", ");
			}else{
				comma = true;
			}
			out.append(db.getName());
		}
		return out.toString();
	}

	void setDatabasesCommaSeparated(String input){
		databases.clear();
		String[] vals = input.split("\\s*[,;]\\s*");
		for(String val : vals){
			if(!val.isEmpty()) {
				databases.add(new DB(null, val));
			}
		}
	}
}