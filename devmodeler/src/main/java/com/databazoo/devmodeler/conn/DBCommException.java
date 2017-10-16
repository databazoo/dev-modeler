
package com.databazoo.devmodeler.conn;

/**
 * Database communication failure exception.
 *
 * @author bobus
 */
public class DBCommException extends Exception {
	//private final String sql;

	public DBCommException(String failedReason, String sql) {
		super(failedReason);
		//this.sql = sql;
	}

}
