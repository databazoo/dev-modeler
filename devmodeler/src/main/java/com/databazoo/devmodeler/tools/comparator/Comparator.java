
package com.databazoo.devmodeler.tools.comparator;

import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.ConnectionUtils;
import com.databazoo.devmodeler.conn.DBCommException;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.Result;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.SearchPanel;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.CheckConstraint;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Package;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Sequence;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.model.View;
import com.databazoo.devmodeler.project.RevisionFactory;
import com.databazoo.tools.Dbg;

import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class Comparator implements Serializable {
	private static final long serialVersionUID = 1905122041950000001L;

	public static final int NO_DIFF = 0;
	public static final int IS_MISSING = 1;
	public static final int IS_ADDED = 2;
	public static final int DEF_CHANGE = 3;
	public static final int AUTO_CHANGED = 4;

	public static final int DATA_CHANGED = 40;
	public static final int DATA_FAILED_DIFFERENT_COLS = 41;
	public static final int DATA_FAILED_QUERY_FAILED = 42;
	public static final int DATA_FAILED_NO_PRIMARY_KEY = 43;
	public static final int DATA_FAILED_TOO_BIG = 44;

	public boolean checkSchemata = true;
	public boolean checkRelations = true;
	public boolean checkAttributes = true;
	public boolean checkIndexes = true;
	public boolean checkConstraints = true;
	public boolean checkTriggers = true;
	public boolean checkFunctions = true;
	public boolean checkViews = true;
	public boolean checkSequences = true;
	public boolean checkSequenceNums = false;
	public boolean checkData = false;

	private boolean hadDifference = false;
	private boolean pendingDifference = false;
	private boolean autoApplyDifference = true;

	private final transient List<IModelElement> differences = new ArrayList<>();

	public static Comparator withAutoApply(){
		return new Comparator(true);
	}
	public static Comparator withReportOnly(){
		return new Comparator(false);
	}

	private Comparator(boolean autoApplyDifference) {
		this.autoApplyDifference = autoApplyDifference;

		// In auto-apply mode we're also updating sequence numbers by default
		if (autoApplyDifference) {
			this.checkSequenceNums = true;
		}
	}

	public boolean hadDifference() {
		return hadDifference;
	}

	public boolean pendingDifference() {
		return pendingDifference;
	}

	public List<IModelElement> getDifferences() {
		return differences;
	}

	public void checkIsDifferent() {
		DesignGUI.getInfoPanel().write("Comparison: difference " + (pendingDifference ? "found (" + differences.size() + ")" : (hadDifference ? "merged in" : "not found")));
		if (pendingDifference || hadDifference) {
			DesignGUI.get().drawProject(true);
			SearchPanel.instance.updateDbTree();
		}
		//Menu.get().setDifferenceCount(differences.size());
	}

	public void compareDBs(DB local, DB remote) {
		compareSchemata(local, local.getSchemas(), remote.getSchemas());
		if (checkConstraints) {
			compareConstraints(local, local.getConstraints(), remote.getConstraints());
		}
		if (checkTriggers) {
			compareTriggers(local, local.getTriggers(), remote.getTriggers());
		}
		Dbg.toFile("Comparison: difference " + (pendingDifference ? "found (" + differences.size() + ")" : (hadDifference ? "merged in" : "not found")));
	}

	void compareSchemata(DB db, List<Schema> localList, List<Schema> remoteList) {
		IConnection conn = ConnectionUtils.getCurrent(db.getName());
		for (int i = 0; i < localList.size(); i++) {
			Schema local = localList.get(i);
			boolean found = false;
			for (Schema remote : remoteList) {
				if (local.getFullName().equals(remote.getFullName())) {
					found = true;
					compareRelations(local, local.getRelations(), remote.getRelations());
					if (checkFunctions) {
						compareFunctions(local, local.getFunctions(), remote.getFunctions());
						comparePackages(local, local.getPackages(), remote.getPackages());
					}
					if (checkViews) {
						compareViews(local, local.getViews(), remote.getViews());
					}
					if (checkSequences || checkSequenceNums) {
						compareSequences(local, local.getSequences(), remote.getSequences());
					}
					break;
				}
			}
			if (checkSchemata && !found) {
				Dbg.info("Schema not found in remote DB: " + local.getFullName());
				local.getBehavior().prepareForEdit();
				local.getBehavior().getValuesForEdit().setDropped();
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(db, new Date(), local.getClassName(), local.getFullName(), local.getQueryChanged(local.getConnection()), local.getQueryChangeRevert(local.getConnection()));
					local.drop();
					i--;
				} else {
					pendingDifference = true;
					local.setDifferent(Comparator.IS_ADDED);
					differences.add(local);
				}
			}
		}
		if (checkSchemata) {
			for (int i = 0; i < remoteList.size(); i++) {
				Schema remote = remoteList.get(i);
				boolean found = false;
				for (Schema local : localList) {
					if (local.getFullName().equals(remote.getFullName())) {
						found = true;
						break;
					}
				}
				if (!found) {
					Dbg.info("Schema not found in local DB: " + remote.getFullName());
					remote.getBehavior().prepareForEdit();
					remote.getBehavior().setNew();
					remote.getBehavior().getValuesForEdit().setDropped();
					if (autoApplyDifference) {
						hadDifference = true;
						RevisionFactory.getCurrentIncoming(conn).addDifference(db, new Date(), remote.getClassName(), remote.getFullName(), remote.getQueryChanged(remote.getConnection()), remote.getQueryChangeRevert(remote.getConnection()));
						remote.getBehavior().setNotNew();
						remote.getBehavior().setNotDropped();
						remote.assignToDB(db);

						Point location = remote.getDB().getKnownLocation(remote.getFullName());
						if (location != null) {
							remote.setLocation(location);
						}
						for (Relation rel : remote.getRelations()) {
							location = remote.getDB().getKnownLocation(rel.getFullName());
							if (location != null) {
								rel.setLocation(location);
							}
						}
						for (Function rel : remote.getFunctions()) {
							location = remote.getDB().getKnownLocation(rel.getFullName());
							if (location != null) {
								rel.setLocation(location);
							}
						}
					} else {
						pendingDifference = true;
						remote.setDifferent(Comparator.IS_MISSING);
						differences.add(remote);
					}
				}
			}
		}
	}

	void compareRelations(Schema schema, List<Relation> localList, List<Relation> remoteList) {
		IConnection conn = ConnectionUtils.getCurrent(schema.getDB().getName());
		for (int i = 0; i < localList.size(); i++) {
			Relation local = localList.get(i);
			Relation remote = null;
			boolean found = false;
			boolean matches = true;
			for (Relation remoteList1 : remoteList) {
				remote = remoteList1;
				if (local.getFullName().equals(remote.getFullName())) {
					found = true;
					if (checkAttributes) {
						compareAttributes(local, local.getAttributes(), remote.getAttributes());
					}
					if (checkIndexes) {
						compareIndexes(local, local.getIndexes(), remote.getIndexes());
					}
					if (checkData) {
						compareData(local, remote);
					}
					if (
							local.getBehavior().hasOIDs() != remote.getBehavior().hasOIDs() ||
									local.getBehavior().getOptions().length != remote.getBehavior().getOptions().length ||
									!(local.getBehavior().getCollation() == null ? "" : local.getBehavior().getCollation()).equals(remote.getBehavior().getCollation() == null ? "" : remote.getBehavior().getCollation()) ||
									!(local.getBehavior().getStorage() == null ? "" : local.getBehavior().getStorage()).equals(remote.getBehavior().getStorage() == null ? "" : remote.getBehavior().getStorage()) ||
									!(local.getBehavior().getDescr() == null ? "" : local.getBehavior().getDescr()).equals(remote.getBehavior().getDescr() == null ? "" : remote.getBehavior().getDescr()) ||
									!(local.getBehavior().getInheritParentName() == null ? "" : local.getBehavior().getInheritParentName()).equals(remote.getBehavior().getInheritParentName() == null ? "" : remote.getBehavior().getInheritParentName())
							) {
						matches = false;
					} else {
						for (int k = 0; k < local.getBehavior().getOptions().length; k++) {
							if (!local.getBehavior().getOptions()[k].equals(remote.getBehavior().getOptions()[k])) {
								matches = false;
								break;
							}
						}
					}
					break;
				}
			}
			if (checkRelations) {
				if (!found) {
					Dbg.info("Relation not found in remote DB: " + local.getFullName());
					local.getBehavior().prepareForEdit();
					local.getBehavior().getValuesForEdit().setDropped();
					if (autoApplyDifference) {
						hadDifference = true;
						RevisionFactory.getCurrentIncoming(conn).addDifference(schema.getDB(), new Date(), local.getClassName(), local.getFullName(), local.getQueryChanged(local.getConnection()), local.getQueryChangeRevert(local.getConnection()));
						local.drop();
						i--;
					} else {
						pendingDifference = true;
						local.setDifferent(Comparator.IS_ADDED);
						differences.add(local);
					}
				} else if (!matches) {
					Dbg.info("Different relation in remote DB: " + local.getFullName());
					local.getBehavior().setValuesForEdit(remote.getBehavior());
					//Dbg.info(local.getQueryChanged(local.getConnection()));
					//Dbg.info(local.getQueryChangeRevert(local.getConnection()));
					if (autoApplyDifference) {
						hadDifference = true;
						RevisionFactory.getCurrentIncoming(conn).addDifference(schema.getDB(), new Date(), local.getClassName(), local.getFullName(), local.getQueryChanged(local.getConnection()), local.getQueryChangeRevert(local.getConnection()));
						local.setBehavior(remote.getBehavior());
					} else {
						pendingDifference = true;
						local.setDifferent(Comparator.DEF_CHANGE);
						differences.add(local);
					}
				}
			}
		}
		if (checkRelations) {
			for (int i = 0; i < remoteList.size(); i++) {
				Relation remote = remoteList.get(i);
				boolean found = false;
				for (Relation local : localList) {
					if (local.getFullName().equals(remote.getFullName())) {
						found = true;
						break;
					}
				}
				if (!found) {
					Dbg.info("Relation not found in local DB: " + remote.getFullName());
					remote.getBehavior().prepareForEdit();
					remote.getBehavior().setNew();
					remote.getBehavior().getValuesForEdit().setDropped();
					for (int k = remote.getConstraints().size(); k > 0; k--) {
						remote.getConstraints().remove(0);
					}
					if (autoApplyDifference) {
						hadDifference = true;
						RevisionFactory.getCurrentIncoming(conn).addDifference(schema.getDB(), new Date(), remote.getClassName(), remote.getFullName(), remote.getQueryChanged(remote.getConnection()), remote.getQueryChangeRevert(remote.getConnection()));
						remote.getBehavior().setNotNew();
						remote.getBehavior().setNotDropped();
						remote.assignToSchema(schema);

						Point location = remote.getDB().getKnownLocation(remote.getFullName());
						if (location != null) {
							remote.setLocation(location);
						}
					} else {
						pendingDifference = true;
						remote.setDifferent(Comparator.IS_MISSING);
						differences.add(remote);
					}
				}
			}
		}
	}

	void compareData(Relation local, Relation remote) {
		if (
				local.getCountRows() <= Settings.getInt(Settings.L_PERFORM_COMPARE_LIMIT) &&
						remote.getCountRows() <= Settings.getInt(Settings.L_PERFORM_COMPARE_LIMIT) &&
						local.getInheritances().isEmpty() &&
						remote.getInheritances().isEmpty()
				) {
			Dbg.info("Comparing table " + local.getFullName() + " with " + local.getCountRows() + "/" + remote.getCountRows() + " rows");
			try {
				int log = DesignGUI.getInfoPanel().write("Loading data of " + local.getFullName());
				Result res1 = local.getConnection().getAllRows(local);
				Result res2 = remote.getConnection().getAllRows(remote);

				if (res1 != null && res2 != null) {
					DesignGUI.getInfoPanel().writeOK(log);
					if (res1.getColumnCount() == res2.getColumnCount()) {

						HashMap<Integer, Integer> columnMap = new HashMap<>();
						for (int i = 0; i < res1.getColumnCount(); i++) {
							String colName = res1.getColumnName(i);
							boolean found = false;
							for (int j = 0; j < res2.getColumnCount(); j++) {
								if (res2.getColumnName(j).equalsIgnoreCase(colName)) {
									columnMap.put(i, j);
									found = true;
									break;
								}
							}
							if (!found) {
								Dbg.info("Skipping data comparison because tables have different columns");
								remote.setDifferent(DATA_FAILED_DIFFERENT_COLS);
								differences.add(remote);
								columnMap = null;
								break;
							}
						}

						if (columnMap != null && DataDifference.compareResultSets(local, remote, res1, res2, columnMap)) {
							differences.add(remote);
							pendingDifference = true;
						}

					} else {
						Dbg.info("Skipping data comparison because tables have different column count");
						remote.setDifferent(DATA_FAILED_DIFFERENT_COLS);
						differences.add(remote);
					}
				} else {
					DesignGUI.getInfoPanel().writeFailed(log, "No primary key");
					Dbg.info("Skipping data comparison because one of tables does not have a PK");
					remote.setDifferent(DATA_FAILED_NO_PRIMARY_KEY);
					differences.add(remote);
				}
			} catch (DBCommException ex) {
				Dbg.notImportant("Skipping data comparison because of query failure", ex);
				remote.setDifferent(DATA_FAILED_QUERY_FAILED);
				differences.add(remote);
			}
		} else {
			Dbg.info("Skipping data comparison because of table size or inheritances: " + local.getFullName() + " has " + local.getCountRows() + "/" + remote.getCountRows() + " rows");
			remote.setDifferent(DATA_FAILED_TOO_BIG);
			differences.add(remote);
		}
	}

	void compareFunctions(Schema schema, List<Function> localList, List<Function> remoteList) {
		IConnection conn = ConnectionUtils.getCurrent(schema.getDB().getName());
		for (int i = 0; i < localList.size(); i++) {
			Function local = localList.get(i);
			Function remote = null;
			boolean found = false;
			boolean matches = true;
			for (Function remoteList1 : remoteList) {
				remote = remoteList1;
				if (local.getFullName().equals(remote.getFullName()) && local.getArgs().equals(remote.getArgs())) {
					found = true;
					if (!conn.getQueryCreate(local, null).equals(conn.getQueryCreate(remote, null))) {
						matches = false;
					}
					break;
				}
			}
			if (!found) {
				Dbg.info("Function not found in remote DB: " + local.getFullName() + "(" + local.getArgs() + ")");
				local.getBehavior().prepareForEdit();
				local.getBehavior().getValuesForEdit().setDropped();
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(schema.getDB(), new Date(), local.getClassName(), local.getFullName(), local.getQueryChanged(local.getConnection()), local.getQueryChangeRevert(local.getConnection()));
					local.drop();
					local.getBehavior().getValuesForEdit().setNotDropped();
					i--;
				} else {
					pendingDifference = true;
					local.setDifferent(Comparator.IS_ADDED);
					differences.add(local);
				}
			} else if (!matches) {
				Dbg.info("Different function in remote DB: " + local.getFullName());
				local.getBehavior().setValuesForEdit(remote.getBehavior());
				//Dbg.info(local.getQueryChanged(local.getConnection()));
				//Dbg.info(local.getQueryChangeRevert(local.getConnection()));
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(schema.getDB(), new Date(), local.getClassName(), local.getFullName(), local.getQueryChanged(local.getConnection()), local.getQueryChangeRevert(local.getConnection()));
					local.setBehavior(remote.getBehavior());
				} else {
					//Dbg.info(conn.getQueryCreate(local));
					//Dbg.info(conn.getQueryCreate(remote));
					pendingDifference = true;
					local.setDifferent(Comparator.DEF_CHANGE);
					differences.add(local);
				}
			}
		}
		for (int i = 0; i < remoteList.size(); i++) {
			Function remote = remoteList.get(i);
			boolean found = false;
			for (Function local : localList) {
				if (local.getFullName().equals(remote.getFullName()) && local.getArgs().equals(remote.getArgs())) {
					found = true;
					break;
				}
			}
			if (!found) {
				Dbg.info("Function not found in local DB: " + remote.getFullName() + "(" + remote.getArgs() + ")");
				remote.getBehavior().prepareForEdit();
				remote.getBehavior().setNew();
				remote.getBehavior().getValuesForEdit().setDropped();
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(schema.getDB(), new Date(), remote.getClassName(), remote.getFullName(), remote.getQueryChanged(remote.getConnection()), remote.getQueryChangeRevert(remote.getConnection()));
					remote.getBehavior().setNotNew();
					remote.getBehavior().setNotDropped();
					remote.assignToSchema(schema);

					Point location = remote.getDB().getKnownLocation(remote.getFullName());
					if (location != null) {
						remote.setLocation(location);
					}
				} else {
					pendingDifference = true;
					remote.setDifferent(Comparator.IS_MISSING);
					differences.add(remote);
				}
			}
		}
	}

	void comparePackages(Schema schema, List<Package> localList, List<com.databazoo.devmodeler.model.Package> remoteList) {
		IConnection conn = ConnectionUtils.getCurrent(schema.getDB().getName());
		for (int i = 0; i < localList.size(); i++) {
			Package local = localList.get(i);
			Package remote = null;
			boolean found = false;
			boolean matches = true;
			for (Package remoteList1 : remoteList) {
				remote = remoteList1;
				if (local.getFullName().equals(remote.getFullName())) {
					found = true;
					if (!conn.getQueryCreate(local, null).equals(conn.getQueryCreate(remote, null))) {
						matches = false;
					}
					break;
				}
			}
			if (!found) {
				Dbg.info("Package not found in remote DB: " + local.getFullName());
				local.getBehavior().prepareForEdit();
				local.getBehavior().getValuesForEdit().setDropped();
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(schema.getDB(), new Date(), local.getClassName(), local.getFullName(), local.getQueryChanged(local.getConnection()), local.getQueryChangeRevert(local.getConnection()));
					local.drop();
					local.getBehavior().getValuesForEdit().setNotDropped();
					i--;
				} else {
					pendingDifference = true;
					local.setDifferent(Comparator.IS_ADDED);
					differences.add(local);
				}
			} else if (!matches) {
				Dbg.info("Different package in remote DB: " + local.getFullName());
				local.getBehavior().setValuesForEdit(remote.getBehavior());
				//Dbg.info(local.getQueryChanged(local.getConnection()));
				//Dbg.info(local.getQueryChangeRevert(local.getConnection()));
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(schema.getDB(), new Date(), local.getClassName(), local.getFullName(), local.getQueryChanged(local.getConnection()), local.getQueryChangeRevert(local.getConnection()));
					local.setBehavior(remote.getBehavior());
				} else {
					//Dbg.info(conn.getQueryCreate(local));
					//Dbg.info(conn.getQueryCreate(remote));
					pendingDifference = true;
					local.setDifferent(Comparator.DEF_CHANGE);
					differences.add(local);
				}
			}
		}
		for (int i = 0; i < remoteList.size(); i++) {
			Package remote = remoteList.get(i);
			boolean found = false;
			for (Package local : localList) {
				if (local.getFullName().equals(remote.getFullName())) {
					found = true;
					break;
				}
			}
			if (!found) {
				Dbg.info("Package not found in local DB: " + remote.getFullName());
				remote.getBehavior().prepareForEdit();
				remote.getBehavior().setNew();
				remote.getBehavior().getValuesForEdit().setDropped();
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(schema.getDB(), new Date(), remote.getClassName(), remote.getFullName(), remote.getQueryChanged(remote.getConnection()), remote.getQueryChangeRevert(remote.getConnection()));
					remote.getBehavior().setNotNew();
					remote.getBehavior().setNotDropped();
					remote.assignToSchema(schema);

					Point location = remote.getDB().getKnownLocation(remote.getFullName());
					if (location != null) {
						remote.setLocation(location);
					}
				} else {
					pendingDifference = true;
					remote.setDifferent(Comparator.IS_MISSING);
					differences.add(remote);
				}
			}
		}
	}

	void compareViews(Schema schema, List<View> localList, List<View> remoteList) {
		IConnection conn = ConnectionUtils.getCurrent(schema.getDB().getName());
		for (int i = 0; i < localList.size(); i++) {
			View local = localList.get(i);
			View remote = null;
			boolean found = false;
			boolean matches = true;
			for (View remoteList1 : remoteList) {
				remote = remoteList1;
				if (local.getFullName().equals(remote.getFullName())) {
					found = true;
					if (!conn.getQueryCreate(local, null).equals(conn.getQueryCreate(remote, null))) {
						matches = false;
					}
					break;
				}
			}
			if (!found) {
				Dbg.info("View not found in remote DB: " + local.getFullName());
				local.getBehavior().prepareForEdit();
				local.getBehavior().getValuesForEdit().setDropped();
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(schema.getDB(), new Date(), local.getClassName(), local.getFullName(), local.getQueryChanged(local.getConnection()), local.getQueryChangeRevert(local.getConnection()));
					local.drop();
					i--;
				} else {
					pendingDifference = true;
					local.setDifferent(Comparator.IS_ADDED);
					differences.add(local);
				}
			} else if (!matches) {
				Dbg.info("Different view in remote DB: " + local.getFullName());
				local.getBehavior().setValuesForEdit(remote.getBehavior());
				//Dbg.info(local.getQueryChanged(local.getConnection()));
				//Dbg.info(local.getQueryChangeRevert(local.getConnection()));
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(schema.getDB(), new Date(), local.getClassName(), local.getFullName(), local.getQueryChanged(local.getConnection()), local.getQueryChangeRevert(local.getConnection()));
					local.setBehavior(remote.getBehavior());
				} else {
					//Dbg.info(conn.getQueryCreate(local));
					//Dbg.info(conn.getQueryCreate(remote));
					pendingDifference = true;
					local.setDifferent(Comparator.DEF_CHANGE);
					differences.add(local);
				}
			}
		}
		for (int i = 0; i < remoteList.size(); i++) {
			View remote = remoteList.get(i);
			boolean found = false;
			for (View local : localList) {
				if (local.getFullName().equals(remote.getFullName())) {
					found = true;
					break;
				}
			}
			if (!found) {
				Dbg.info("View not found in local DB: " + remote.getFullName());
				remote.getBehavior().prepareForEdit();
				remote.getBehavior().setNew();
				remote.getBehavior().getValuesForEdit().setDropped();
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(schema.getDB(), new Date(), remote.getClassName(), remote.getFullName(), remote.getQueryChanged(remote.getConnection()), remote.getQueryChangeRevert(remote.getConnection()));
					remote.getBehavior().setNotNew();
					remote.getBehavior().setNotDropped();
					remote.assignToSchema(schema);

					Point location = remote.getDB().getKnownLocation(remote.getFullName());
					if (location != null) {
						remote.setLocation(location);
					}
				} else {
					pendingDifference = true;
					remote.setDifferent(Comparator.IS_MISSING);
					differences.add(remote);
				}
			}
		}
	}

	void compareSequences(Schema schema, List<Sequence> localList, List<Sequence> remoteList) {
		IConnection conn = ConnectionUtils.getCurrent(schema.getDB().getName());
		for (int i = 0; i < localList.size(); i++) {
			Sequence local = localList.get(i);
			Sequence remote = null;
			boolean found = false;
			boolean matches = true;
			for (Sequence remoteList1 : remoteList) {
				remote = remoteList1;
				if (local.getFullName().equals(remote.getFullName())) {
					found = true;
					if(!checkSequenceNums){
						remote.getBehavior().setCurrent(local.getBehavior().getCurrent());
					}
					if (!conn.getQueryCreate(local, null).equals(conn.getQueryCreate(remote, null))) {
						matches = false;
					}

					String depend = remote.getDependencies();
					if (!local.getDependencies().equals(depend)) {
						local.setAttributesByDependence(depend.split(","));
						local.assignToSchemaAndAttributes();
					}

					break;
				}
			}
			if (!found) {
				Dbg.info("Sequence not found in remote DB: " + local.getFullName());
				local.getBehavior().prepareForEdit();
				local.getBehavior().getValuesForEdit().setDropped();
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(schema.getDB(), new Date(), local.getClassName(), local.getFullName(), local.getQueryChanged(local.getConnection()), local.getQueryChangeRevert(local.getConnection()));
					local.drop();
					i--;
				} else {
					pendingDifference = true;
					local.setDifferent(Comparator.IS_ADDED);
					differences.add(local);
				}
			} else if (!matches) {
				Dbg.info("Different sequence in remote DB: " + local.getFullName());
				local.getBehavior().setValuesForEdit(remote.getBehavior());
				//Dbg.info(local.getQueryChanged(local.getConnection()));
				//Dbg.info(local.getQueryChangeRevert(local.getConnection()));
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(schema.getDB(), new Date(), local.getClassName(), local.getFullName(), local.getQueryChanged(local.getConnection()), local.getQueryChangeRevert(local.getConnection()));
					local.setBehavior(remote.getBehavior());
				} else {
					//Dbg.info(conn.getQueryCreate(local));
					//Dbg.info(conn.getQueryCreate(remote));
					pendingDifference = true;
					local.setDifferent(Comparator.DEF_CHANGE);
					differences.add(local);
				}
			}
		}
		for (int i = 0; i < remoteList.size(); i++) {
			Sequence remote = remoteList.get(i);
			boolean found = false;
			for (Sequence local : localList) {
				if (local.getFullName().equals(remote.getFullName())) {
					found = true;
					break;
				}
			}
			if (!found) {
				Dbg.info("Sequence not found in local DB: " + remote.getFullName());
				remote.getBehavior().prepareForEdit();
				remote.getBehavior().setNew();
				remote.getBehavior().getValuesForEdit().setDropped();
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(schema.getDB(), new Date(), remote.getClassName(), remote.getFullName(), remote.getQueryChanged(remote.getConnection()), remote.getQueryChangeRevert(remote.getConnection()));
					remote.getBehavior().setNotNew();
					remote.getBehavior().setNotDropped();
					remote.assignToSchemaAndAttributes(schema);

					Point location = remote.getDB().getKnownLocation(remote.getFullName());
					if (location != null) {
						remote.setLocation(location);
					}
				} else {
					pendingDifference = true;
					remote.setDifferent(Comparator.IS_MISSING);
					differences.add(remote);
				}
			}
		}
	}

	void compareAttributes(Relation rel, List<Attribute> localList, List<Attribute> remoteList) {
		IConnection conn = ConnectionUtils.getCurrent(rel.getDB().getName());
		for (int i = 0; i < localList.size(); i++) {
			Attribute local = localList.get(i);
			Attribute remote = null;
			boolean found = false;
			boolean matches = true;
			for (Attribute remoteList1 : remoteList) {
				remote = remoteList1;
				if (local.getFullName().equals(remote.getFullName())) {
					found = true;
					if (!Objects.equals(local.getAttNum(), remote.getAttNum())) {
						local.setAttNum(remote.getAttNum());
					}
					if (!local.getQueryCreate(local.getConnection()).equals(remote.getQueryCreate(remote.getConnection()))) {
						matches = false;
					} else if (local.getBehavior().getCollation() == null || !local.getBehavior().getCollation().equals(remote.getBehavior().getCollation())) {
						local.getBehavior().setCollation(remote.getBehavior().getCollation());
					}
					break;
				}
			}
			if (!found) {
				Dbg.info("Attribute not found in remote DB: " + local.getFullName());
				local.getBehavior().prepareForEdit();
				local.getBehavior().getValuesForEdit().setDropped();
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(rel.getDB(), new Date(), local.getClassName(), local.getFullName(), local.getQueryChanged(local.getConnection()), local.getQueryChangeRevert(local.getConnection()));
					local.drop();
					i--;
				} else {
					pendingDifference = true;
					local.setDifferent(Comparator.IS_ADDED);
					differences.add(local);
				}
			} else if (!matches) {
				Dbg.info("Different attribute in remote DB: " + local.getFullName());
				local.getBehavior().setValuesForEdit(remote.getBehavior());
				//Dbg.info(local.getQueryChanged(local.getConnection()));
				//Dbg.info(local.getQueryChangeRevert(local.getConnection()));
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(rel.getDB(), new Date(), local.getClassName(), local.getFullName(), local.getQueryChanged(local.getConnection()), local.getQueryChangeRevert(local.getConnection()));
					local.setBehavior(remote.getBehavior());
				} else {
					pendingDifference = true;
					local.setDifferent(Comparator.DEF_CHANGE);
					differences.add(local);
				}
			}
		}
		for (int i = 0; i < remoteList.size(); i++) {
			Attribute remote = remoteList.get(i);
			boolean found = false;
			for (Attribute local : localList) {
				if (local.getFullName().equals(remote.getFullName())) {
					found = true;
					break;
				}
			}
			if (!found) {
				Dbg.info("Attribute not found in local DB: " + remote.getFullName());
				remote.getBehavior().prepareForEdit();
				remote.getBehavior().setNew();
				remote.getBehavior().getValuesForEdit().setDropped();
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(rel.getDB(), new Date(), remote.getClassName(), remote.getFullName(), remote.getQueryChanged(remote.getConnection()), remote.getQueryChangeRevert(remote.getConnection()));
					remote.getBehavior().setNotNew();
					remote.getBehavior().setNotDropped();
					remote.setRel(rel);
					remote.assignToRels();
				} else {
					pendingDifference = true;
					remote.setDifferent(Comparator.IS_MISSING);
					differences.add(remote);
				}
			}
		}
		Collections.sort(rel.getAttributes());
	}

	void compareIndexes(Relation rel, List<Index> localList, List<Index> remoteList) {
		IConnection conn = ConnectionUtils.getCurrent(rel.getDB().getName());
		for (int i = 0; i < localList.size(); i++) {
			Index local = localList.get(i);
			Index remote = null;
			boolean found = false;
			boolean matches = true;
			for (Index remoteList1 : remoteList) {
				remote = remoteList1;
				if (local.getFullName().equals(remote.getFullName())) {
					found = true;
					if (!local.getQueryCreate(local.getConnection()).equals(remote.getQueryCreate(remote.getConnection()))) {
						matches = false;
					}
					break;
				}
			}
			if (!found) {
				Dbg.info("Index not found in remote DB: " + local.getFullName());
				local.getBehavior().prepareForEdit();
				local.getBehavior().getValuesForEdit().setDropped();
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(rel.getDB(), new Date(), local.getClassName(), local.getFullName(), local.getQueryChanged(local.getConnection()), local.getQueryChangeRevert(local.getConnection()));
					local.drop();
					i--;
				} else {
					pendingDifference = true;
					local.setDifferent(Comparator.IS_ADDED);
					differences.add(local);
				}
			} else if (!matches) {
				Dbg.info("Different index in remote DB: " + local.getFullName());
				local.getBehavior().setValuesForEdit(remote.getBehavior());
				//Dbg.info(local.getQueryChanged(local.getConnection()));
				//Dbg.info(local.getQueryChangeRevert(local.getConnection()));
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(rel.getDB(), new Date(), local.getClassName(), local.getFullName(), local.getQueryChanged(local.getConnection()), local.getQueryChangeRevert(local.getConnection()));
					local.setBehavior(remote.getBehavior());
				} else {
					pendingDifference = true;
					local.setDifferent(Comparator.DEF_CHANGE);
					differences.add(local);
				}
			}
		}
		for (int i = 0; i < remoteList.size(); i++) {
			Index remote = remoteList.get(i);
			boolean found = false;
			for (Index local : localList) {
				if (local.getFullName().equals(remote.getFullName())) {
					found = true;
					break;
				}
			}
			if (!found) {
				Dbg.info("Index not found in local DB: " + remote.getFullName());
				remote.getBehavior().prepareForEdit();
				remote.getBehavior().setNew();
				remote.getBehavior().getValuesForEdit().setDropped();
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(rel.getDB(), new Date(), remote.getClassName(), remote.getFullName(), remote.getQueryChanged(remote.getConnection()), remote.getQueryChangeRevert(remote.getConnection()));
					remote.getBehavior().setNotNew();
					remote.getBehavior().setNotDropped();
					remote.assignToRelation(rel);
				} else {
					pendingDifference = true;
					remote.setDifferent(Comparator.IS_MISSING);
					differences.add(remote);
				}
			}
		}
	}

	void compareConstraints(DB db, List<Constraint> localList, List<Constraint> remoteList) {
		IConnection conn = ConnectionUtils.getCurrent(db.getName());
		for (int i = 0; i < localList.size(); i++) {
			Constraint local = localList.get(i);
			Constraint remote = null;
			boolean found = false;
			boolean matches = true;
			String localName = local.getRel1().getFullName() + "." + local.getName();
			for (Constraint remoteList1 : remoteList) {
				remote = remoteList1;
				String remoteName = remote.getRel1().getFullName() + "." + remote.getName();
				if (localName.equals(remoteName)) {
					found = true;
					if (!local.getQueryCreate(local.getConnection()).equals(remote.getQueryCreate(local.getConnection()))) {
						matches = false;
					}
					break;
				}
			}
			if (!found) {
				Dbg.info("Constraint not found in remote DB: " + local.getFullName());
				local.getBehavior().prepareForEdit();
				local.getBehavior().getValuesForEdit().setDropped();
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(db, new Date(), local.getClassName(), local.getFullName(), local.getQueryChanged(local.getConnection()), local.getQueryChangeRevert(local.getConnection()));
					local.drop();
					i--;
				} else {
					pendingDifference = true;
					local.setDifferent(Comparator.IS_ADDED);
					differences.add(local);
				}
			} else if (!matches) {
				Dbg.info("Different constraint in remote DB: " + local.getFullName());
				local.getBehavior().setValuesForEdit(remote.getBehavior());
				//Dbg.info(local.getQueryChanged(local.getConnection()));
				//Dbg.info(local.getQueryChangeRevert(local.getConnection()));
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(db, new Date(), local.getClassName(), local.getFullName(), local.getQueryChanged(local.getConnection()), local.getQueryChangeRevert(local.getConnection()));
					local.setBehavior(remote.getBehavior());

					// REASSIGN
					String relName1, relName2, attrName1, attrName2;
					try {
						CheckConstraint loc = (CheckConstraint) local;
						relName1 = remote.getRel1().getFullName();
						loc.drop();
						loc.setRelByName(db, relName1, false);
					} catch (ClassCastException e) {
						Dbg.notImportant("Oops... Not a check constraint?", e);
						relName1 = remote.getRel1().getFullName();
						relName2 = remote.getRel2().getFullName();
						attrName1 = remote.getAttr1().getFullName();
						attrName2 = remote.getAttr2().getFullName();
						local.drop();
						local.setRelsAttrsByName(db, relName1, relName2, attrName1, attrName2, false);
					}
				} else {
					pendingDifference = true;
					local.setDifferent(Comparator.DEF_CHANGE);
					differences.add(local);
				}
			}
		}
		for (int i = 0; i < remoteList.size(); i++) {
			Constraint remote = remoteList.get(i);
			boolean found = false;
			String remoteName = remote.getRel1().getFullName() + "." + remote.getName();
			for (Constraint local : localList) {
				String localName = local.getRel1().getFullName() + "." + local.getName();
				if (localName.equals(remoteName)) {
					found = true;
					break;
				}
			}
			if (!found) {
				Dbg.info("Constraint not found in local DB: " + remote.getFullName());
				remote.getBehavior().prepareForEdit();
				remote.getBehavior().setNew();
				remote.getBehavior().getValuesForEdit().setDropped();
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(db, new Date(), remote.getClassName(), remote.getFullName(), remote.getQueryChanged(remote.getConnection()), remote.getQueryChangeRevert(remote.getConnection()));
					remote.getBehavior().setNotNew();
					remote.getBehavior().setNotDropped();
					if (remote.getRel2() == null) {
						CheckConstraint c = (CheckConstraint) remote;
						c.setRelByName(db, remote.getRel1().getFullName(), false);
					} else {
						remote.setRelsAttrsByName(db, remote.getRel1().getFullName(), remote.getRel2().getFullName(), remote.getAttr1().getFullName(), remote.getAttr2().getFullName(), false);
					}
				} else {
					pendingDifference = true;
					remote.setDifferent(Comparator.IS_MISSING);
					differences.add(remote);
				}
			}
		}
	}

	void compareTriggers(DB db, List<Trigger> localList, List<Trigger> remoteList) {
		IConnection conn = ConnectionUtils.getCurrent(db.getName());
		for (int i = 0; i < localList.size(); i++) {
			Trigger local = localList.get(i);
			Trigger remote = null;
			boolean found = false;
			boolean matches = true;
			for (Trigger remoteList1 : remoteList) {
				remote = remoteList1;
				if (local.getFullName().equals(remote.getFullName())) {
					found = true;
					if (!local.getQueryCreate(local.getConnection()).equals(remote.getQueryCreate(remote.getConnection()))) {
						//Dbg.info(local.getQueryCreate(local.getConnection()));
						//Dbg.info(local.getQueryCreate(local.getConnection()));
						matches = false;
					}
					break;
				}
			}
			if (!found) {
				Dbg.info("Trigger not found in remote DB: " + local.getFullName());
				local.getBehavior().prepareForEdit();
				local.getBehavior().getValuesForEdit().setDropped();
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(db, new Date(), local.getClassName(), local.getFullName(), local.getQueryChanged(local.getConnection()), local.getQueryChangeRevert(local.getConnection()));
					local.drop();
					i--;
				} else {
					pendingDifference = true;
					local.setDifferent(Comparator.IS_ADDED);
					differences.add(local);
				}
			} else if (!matches) {
				Dbg.info("Different trigger in remote DB: " + local.getFullName());
				local.getBehavior().setValuesForEdit(remote.getBehavior());
				//Dbg.info(local.getQueryChanged(local.getConnection()));
				//Dbg.info(local.getQueryChangeRevert(local.getConnection()));
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(db, new Date(), local.getClassName(), local.getFullName(), local.getQueryChanged(local.getConnection()), local.getQueryChangeRevert(local.getConnection()));
					local.setBehavior(remote.getBehavior());
				} else {
					pendingDifference = true;
					local.setDifferent(Comparator.DEF_CHANGE);
					differences.add(local);
				}
			}
		}
		for (int i = 0; i < remoteList.size(); i++) {
			Trigger remote = remoteList.get(i);
			boolean found = false;
			for (Trigger local : localList) {
				if (local.getFullName().equals(remote.getFullName())) {
					found = true;
					break;
				}
			}
			if (!found) {
				Dbg.info("Trigger not found in local DB: " + remote.getFullName());
				remote.getBehavior().prepareForEdit();
				remote.getBehavior().setNew();
				remote.getBehavior().getValuesForEdit().setDropped();
				if (autoApplyDifference) {
					hadDifference = true;
					RevisionFactory.getCurrentIncoming(conn).addDifference(db, new Date(), remote.getClassName(), remote.getFullName(), remote.getQueryChanged(remote.getConnection()), remote.getQueryChangeRevert(remote.getConnection()));
					remote.getBehavior().setNotNew();
					remote.getBehavior().setNotDropped();
					remote.setDB(db);
					remote.setRelFuncByNameDef(db, remote.getRel1().getFullName(), null, false);
				} else {
					pendingDifference = true;
					remote.setDifferent(Comparator.IS_MISSING);
					differences.add(remote);
				}
			}
		}
	}
}
