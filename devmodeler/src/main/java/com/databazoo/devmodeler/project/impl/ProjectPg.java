package com.databazoo.devmodeler.project.impl;

import com.databazoo.devmodeler.conn.ConnectionPg;
import com.databazoo.devmodeler.conn.IColoredConnection;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.project.Project;

/**
 * Project implementation for PostgreSQL
 * @author bobus
 */
public class ProjectPg extends Project {

	public ProjectPg(String name) {
		super(name);
	}

	@Override
	public int getType(){
		return TYPE_PG;
	}

	@Override
	public String getTypeString(){
		return L_POSTGRESQL;
	}

	@Override
	public ConnectionPg copyConnection(IConnection conn){
		ConnectionPg c = new ConnectionPg(
				conn.getName(),
				conn.getHost(),
				conn.getUser(),
				conn.getPass(),
				conn.getType(),
				false);
		c.setColor(conn.getColor());
		return c;
	}

	@Override
	protected void addConnection(String name, String host, String user, String pass, int type, IColoredConnection.ConnectionColor color) {
		final ConnectionPg c = new ConnectionPg(name, host, user, pass, type, false);
		c.setColor(color);
		connections.add(c);
	}

	@Override
	protected void addDedicatedConnection(String dbName, String name, String host, String user, String pass, int type, String alias, String defaultSchema) {
		ConnectionPg c = new ConnectionPg(
				name,
				host,
				user,
				pass,
				type,
				false);
		c.setDbAlias(alias);
		addDedicatedConnection(dbName, name, c);
	}

}
