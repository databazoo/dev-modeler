
package com.databazoo.devmodeler.model;

import com.databazoo.components.icons.IIconable;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.SQLOutputConfigExport;

import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.io.Serializable;
import java.util.Set;

/**
 * All model elements implement this interface.
 *
 * @author bobus
 */
public interface IModelElement extends IIconable, Comparable<IModelElement>, Serializable {
	String getName();
	String getFullName();
	String getEditedFullName();
	String getFullPath();
	String getClassName();
	String getDescr();

	Set<IModelElement> getAllSubElements();

	String getQueryCreate(IConnection conn);
	String getQueryCreateClear(IConnection conn);
	String getQueryChanged(IConnection conn);
	String getQueryChangeRevert(IConnection conn);
	String getQueryRecursive(SQLOutputConfigExport config) throws SQLOutputConfigExport.LimitReachedException;
	String getQueryDrop(IConnection conn);

	DefaultMutableTreeNode getTreeView(boolean showCreateIcons);
	DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching);

	void setDifferent(int isDifferent);
	int getDifference();
	void drop();
	void checkSize();
	Dimension getSize();

	void setSelected(boolean sel);
	void unSelect();
	void clicked();
	void doubleClicked();
	void rightClicked();
	void rightClicked(String workspaceName);
	void repaint();

	IConnection getConnection();
	DB getDB();

	boolean isNew();

	IModelBehavior getBehavior();
	void setBehavior(IModelBehavior behavior);

	boolean isInEnvironment(IConnection env);
	void setInEnvironment(IConnection env, boolean isAvailable);

}
