
package com.databazoo.devmodeler.project.impl;

import com.databazoo.devmodeler.conn.ConnectionMaria;
import com.databazoo.devmodeler.conn.IColoredConnection;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.project.Project;


/**
 * Project implementation for MariaDB
 * @author bobus
 */
public class ProjectMaria extends Project {

	public ProjectMaria(String name) {
		super(name);
	}

	@Override
	public int getType(){
		return TYPE_MARIA;
	}

	@Override
	public String getTypeString(){
		return L_MARIA_DB;
	}

	@Override
	public ConnectionMaria copyConnection(IConnection conn){
		ConnectionMaria c = new ConnectionMaria(
				conn.getName(),
				conn.getHost(),
				conn.getUser(),
				conn.getPass(),
				conn.getType()
			);
		c.setColor(conn.getColor());
		return c;
	}

	@Override
	protected void addConnection(String name, String host, String user, String pass, int type, IColoredConnection.ConnectionColor color) {
		ConnectionMaria c = new ConnectionMaria(name, host, user, pass, type);
		c.setColor(color);
		connections.add(c);
	}

	@Override
	protected void addDedicatedConnection(String dbName, String name, String host, String user, String pass, int type, String alias, String defaultSchema) {
		ConnectionMaria c = new ConnectionMaria(
				name,
				host,
				user,
				pass,
				type);
		c.setDbAlias(alias);
		addDedicatedConnection(dbName, name, c);
	}

}
