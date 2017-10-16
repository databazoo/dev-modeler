
package com.databazoo.devmodeler.model;

import javax.swing.tree.DefaultMutableTreeNode;

import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.SQLOutputConfig;
import com.databazoo.tools.Dbg;

/**
 * Model representation of check constraints.
 * Has no visual representation.
 *
 * @author bobus
 */
public class CheckConstraint extends Constraint {

	public CheckConstraint(DB parent, String name, String def, String descr) {
		super(parent, name);
		behavior.def = def;
		behavior.descr = descr;
		clearDef();
	}

	private void clearDef() {
		behavior.def = getDB().getProject().getCurrentConn().getCleanDef(this);
	}

	@Override
	public String getQueryCreate(IConnection conn) {
		return conn.getQueryCreate(this, behavior.isNew ? null : SQLOutputConfig.WIZARD);
	}

	@Override
	public String getQueryChanged(IConnection conn) {
		if (behavior.isNew) {
			Behavior o = behavior;
			behavior = behavior.valuesForEdit;
			String ret = conn.getQueryCreate(this, null);
			behavior = o;
			return ret;
		} else {
			return conn.getQueryChanged(this);
		}
	}

	@Override
	public String getQueryChangeRevert(IConnection conn) {
		if (behavior.isNew) {
			behavior.isDropped = true;
		} else if (behavior.valuesForEdit.isDropped) {
			return conn.getQueryCreateWithoutComment(this, null);
		}
		Behavior o = behavior;
		behavior = behavior.valuesForEdit;
		behavior.valuesForEdit = o;
		String change = conn.getQueryChanged(this);
		behavior = o;
		return change;
	}

	public String getDef() {
		return "CHECK (" + behavior.def + ")";
	}

	public void setRelByName(DB db, String rel1FullName, boolean isTemp) {
		rel1 = null;
		rel2 = null;
		//rel1FullName = Connection.escapeFullName(rel1FullName);
		for (Schema schema : db.getSchemas()) {
			for (Relation rel : schema.getRelations()) {
				if (rel.getFullName().equals(rel1FullName)) {
					setRel1(rel);
					break;
				}
			}
			if (isReady()) {
				break;
			}
		}
		if (!isReady()) {
			Dbg.info("Constraint " + getFullName() + " incomplete: " + rel1FullName + " not found");
		} else if (!isTemp) {
			assignToRels();
		}
	}

	@Override
	public void assignToRels() {
		((Relation) rel1).getConstraints().add(this);
		((IModelElement) rel1).getDB().getConstraints().add(this);
	}

	@Override
	public boolean isReady() {
		return rel1 != null;
	}

	@Override
	public void checkSize() {
	}

	@Override
	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching) {
		boolean nameMatch = getName().matches(search);
		boolean excludedByName = searchNotMatching && nameMatch;
		if (!excludedByName && (searchNotMatching || nameMatch || (
				fulltext && getDB().getProject().getCurrentConn().getQueryCreate(this, null).matches(search)
		))) {
			return new DefaultMutableTreeNode(this);
		} else {
			return null;
		}
	}
}
