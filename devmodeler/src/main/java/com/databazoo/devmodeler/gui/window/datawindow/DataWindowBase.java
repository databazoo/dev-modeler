package com.databazoo.devmodeler.gui.window.datawindow;

import com.databazoo.components.GCFrameWithObservers;
import com.databazoo.devmodeler.conn.ConnectionUtils;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.project.Project;
import plugins.api.IDataWindow;

abstract class DataWindowBase implements IDataWindow {
	protected GCFrameWithObservers frame;
	protected Relation rel;
	protected DB database = Project.getCurrDB();
	protected IConnection connection = ConnectionUtils.getCurrent(database.getName());

	protected abstract DataWindow getInstance();

	public DB getDB() {
		return database;
	}
}
