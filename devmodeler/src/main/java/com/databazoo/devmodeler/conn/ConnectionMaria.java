
package com.databazoo.devmodeler.conn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.databazoo.devmodeler.gui.DesignGUI;

/**
 * Forward + revers engineering setup for MariaDB
 *
 * @author bobus
 */
public class ConnectionMaria extends ConnectionMy {
	private static final List<String> STORAGE_ENGINES = new ArrayList<>(Arrays.asList("InnoDB", "XtraDB", "MyISAM", "Aria", "TokuDB", "MEMORY", "ARCHIVE", "Cassandra", "CONNECT", "SphinxSE", "Spider", "ScaleDB"));

	public ConnectionMaria (String name, String host, String user, String pass) {
		super(name, host, user, pass);
	}

	public ConnectionMaria (String name, String host, String user, String pass, int type) {
		super(name, host, user, pass, type);
	}

	@Override
	public String getTypeName(){
		return "MariaDB";
	}

	@Override
	public String[] getStorageEngines(){
		return ConnectionMaria.STORAGE_ENGINES.toArray(new String[0]);
	}

	@Override
	public String getDefaultStorageEngine(){
		return "XtraDB";
	}

	@Override
	public void loadEnvironment() throws DBCommException {
		// load available storage engines
		int log = DesignGUI.getInfoPanel().write("Loading server environment...");
		Query q = new Query("SHOW ENGINES", getDefaultDB()).run();
		while(q.next()){
			if(!ConnectionMaria.STORAGE_ENGINES.contains(q.getString("ENGINE"))) {
				ConnectionMaria.STORAGE_ENGINES.add(q.getString("ENGINE"));
			}
		}
		q.close();
		q.log(log);
	}
}
