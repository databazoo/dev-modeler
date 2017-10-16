
package com.databazoo.devmodeler.project.impl;

import com.databazoo.devmodeler.conn.ConnectionMy;
import com.databazoo.devmodeler.conn.IColoredConnection;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.project.Project;


/**
 * Project implementation for MySQL
 * @author bobus
 */
public class ProjectMy extends Project {

	public ProjectMy(String name) {
		super(name);
	}

	@Override
	public int getType(){
		return TYPE_MY;
	}

	@Override
	public String getTypeString(){
		return L_MYSQL;
	}

	@Override
	public ConnectionMy copyConnection(IConnection conn){
		ConnectionMy c = new ConnectionMy(
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
		ConnectionMy c = new ConnectionMy(name, host, user, pass, type);
		c.setColor(color);
		connections.add(c);
	}

	@Override
	protected void addDedicatedConnection(String dbName, String name, String host, String user, String pass, int type, String alias, String defaultSchema) {
		ConnectionMy c = new ConnectionMy(
				name,
				host,
				user,
				pass,
				type);
		c.setDbAlias(alias);
		addDedicatedConnection(dbName, name, c);
	}

}
