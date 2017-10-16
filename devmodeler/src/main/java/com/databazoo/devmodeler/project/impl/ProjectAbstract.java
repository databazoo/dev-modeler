
package com.databazoo.devmodeler.project.impl;

import java.util.concurrent.CopyOnWriteArrayList;

import com.databazoo.devmodeler.conn.ConnectionPg;
import com.databazoo.devmodeler.conn.IColoredConnection;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.project.Project;

/**
 * Project implementation that does not use physical database connections.
 * @author bobus
 */
public class ProjectAbstract extends Project {

	public ProjectAbstract(String name) {
		super(name);
	}

	/**
	 * Lazy loading of projects
	 */
	@Override
	protected boolean load(){
		setAbstractConnection();
		boolean ret = super.load();
		addPublicSchemaToAllDBs();

		return ret;
	}

	public void addPublicSchemaToAllDBs(){
		for(DB db: databases){
			if(db.getSchemas().isEmpty()){
				db.getSchemas().add(new Schema(db, "public", ""));
			}
		}
	}

	public void setAbstractConnection(){
		connections = new CopyOnWriteArrayList<>();
		connections.add(new ConnectionPg(
						"Model",
						"",
						"",
						"",
						0,
						false));
		setCurrentConn("Model");
	}

	@Override
	protected void addConnection(String name, String host, String user, String pass, int type, IColoredConnection.ConnectionColor color) {
		setAbstractConnection();
	}

	@Override
	protected void addDedicatedConnection(String dbName, String name, String host, String user, String pass, int type, String alias, String defaultSchema) {
		//throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public IConnection copyConnection(IConnection conn) {
		ConnectionPg c = new ConnectionPg(
				"Model",
				"",
				"",
				"",
				0,
				false);
		c.setColor(conn.getColor());
		return c;
	}

}
