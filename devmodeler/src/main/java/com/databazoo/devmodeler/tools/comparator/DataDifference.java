
package com.databazoo.devmodeler.tools.comparator;

import static com.databazoo.devmodeler.tools.comparator.Comparator.DATA_CHANGED;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.tree.DefaultMutableTreeNode;

import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.Result;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.tools.Dbg;


public class DataDifference {

	public static final String L_INSERTED = "Inserted";
	public static final String L_UPDATED = "Updated";
	public static final String L_DELETED = "Deleted";

	/**
	 *
	 * @param local local table
	 * @param remote remote table
	 * @param res1 local table rows
	 * @param res2 remote table rows
	 * @param columnMap local to remote column map
	 * @return change found?
	 */
	public static boolean compareResultSets (Relation local, Relation remote, Result res1, Result res2, HashMap<Integer, Integer> columnMap)
	{
		// row count has obviously changed
		if(res1.getRowCount() != res2.getRowCount()){
			remote.setDifferent(DATA_CHANGED);
			remote.setDataChanged(res1, res2, columnMap);
			return true;
		}

		int j = 0;
		for(int i=0; i<res1.getRowCount(); i++){
			String pkValue = res1.getValueAt(i, 0).toString();
			boolean found = false;
			for(int k=j; k<res2.getRowCount(); k++){
				if(res2.getValueAt(k, 0).equals(pkValue)){
					found = true;
					j = k;

					// check for individual cell differences
					for(int m=1; m<res1.getColumnCount(); m++){
						Object value = res1.getValueAt(i, m);
						Object val_2 = res2.getValueAt(i, columnMap.get(m));
						if (!Objects.equals(value, val_2)) {
							Dbg.info("Row "+pkValue+" cell "+i+"x"+m+" is different: "+value+" VS "+val_2);
							remote.setDifferent(DATA_CHANGED);
							remote.setDataChanged(res1, res2, columnMap);
							return true;
						}
					}
					break;
				}
			}
			if(!found){
				Dbg.info("Row "+pkValue+" was not found in remote table");
				remote.setDifferent(DATA_CHANGED);
				remote.setDataChanged(res1, res2, columnMap);
				return true;
			}
		}

		j = 0;
		for(int i=0; i<res2.getRowCount(); i++){
			String pkValue = res2.getValueAt(i, 0).toString();
			boolean found = false;
			for(int k=j; k<res1.getRowCount(); k++){
				if(res1.getValueAt(k, 0).equals(pkValue)){
					found = true;
					j = k;

					/* check for individual cell differences
					for(int m=1; m<res2.getColumnCount(); m++){
						String value = res2.getValueAt(i, m).toString();
						String val_2 = res1.getValueAt(columnMap.get(i), m).toString();
						if(
							(value == null && val_2 != null) ||
							(value != null && val_2 == null) ||
							(value != null && val_2 != null && !value.equals(val_2))
						){
							Dbg.info("Row "+pkValue+" cell "+i+"x"+m+" is different: "+value+" VS "+val_2);
							remote.setDifferent(DATA_CHANGED);
							remote.setDataChanged(res1, res2, columnMap);
							return true;
						}
					}*/
					break;
				}
			}
			if(!found){
				Dbg.info("Row "+pkValue+" was not found in remote table");
				remote.setDifferent(DATA_CHANGED);
				remote.setDataChanged(res1, res2, columnMap);
				return true;
			}
		}
		return false;
	}

	private final Relation rel;
	private final Result res1;
	private final Result res2;
	private final Map<Integer, Integer> columnMap;

	public final Map<String, String> inserted = new LinkedHashMap<>();
	public final Map<String, String> updated = new LinkedHashMap<>();
	public final Map<String, String> deleted = new LinkedHashMap<>();

	public DataDifference (Relation rel, Result res1, Result res2, HashMap<Integer, Integer> columnMap) {
		this.rel = rel;
		this.res1 = res1;
		this.res2 = res2;
		this.columnMap = columnMap;
	}

	public DefaultMutableTreeNode getTreeView(){
		if(inserted.isEmpty() && updated.isEmpty() && deleted.isEmpty()){
			compareResultSets();
		}
		DefaultMutableTreeNode ret = new DefaultMutableTreeNode(rel);
		if(!inserted.isEmpty()) {
			ret.add(new DefaultMutableTreeNode(L_INSERTED));
		}
		if(!updated.isEmpty()) {
			ret.add(new DefaultMutableTreeNode(L_UPDATED));
		}
		if(!deleted.isEmpty()) {
			ret.add(new DefaultMutableTreeNode(L_DELETED));
		}
		return ret;
	}

	private void compareResultSets()
	{
		// TODO: use correct connection
		IConnection conn = rel.getConnection();

		int j = 0;
		for(int i=0; i<res1.getRowCount(); i++){
			String pkValue = res1.getValueAt(i, 0).toString();
			boolean found = false;
			for(int k=j; k<res2.getRowCount(); k++){
				if(res2.getValueAt(k, 0).equals(pkValue)){
					found = true;
					j = k;

					HashMap<String, String> changedForward = new LinkedHashMap<>();
					HashMap<String, String> changedReverse = new LinkedHashMap<>();

					// check for individual cell differences
					for(int m=1; m<res1.getColumnCount(); m++){
						Object value = res1.getValueAt(i, m);
						Object val_2 = res2.getValueAt(i, columnMap.get(m));
						if (!Objects.equals(value, val_2)) {
							Dbg.info("Row "+pkValue+" cell "+i+"x"+m+" is different: "+value+" VS "+val_2);
							changedForward.put(res1.getColumnName(m), (String)value);
							changedReverse.put(res1.getColumnName(m), (String)val_2);
						}
					}

					if(!changedForward.isEmpty()){
						updated.put(
							conn.getQueryUpdate(rel, getPkHashMap(pkValue), changedForward),
							conn.getQueryUpdate(rel, getPkHashMap(pkValue), changedReverse)
						);
					}

					break;
				}
			}
			if(!found){
				Dbg.info("Row "+pkValue+" was not found in remote table");
				deleted.put(
					conn.getQueryInsert(rel, res1.getRow(i)),
					conn.getQueryDelete(rel, getPkHashMap(pkValue))
				);
			}
		}

		j = 0;
		for(int i=0; i<res2.getRowCount(); i++){
			String pkValue = res2.getValueAt(i, 0).toString();
			boolean found = false;
			for(int k=j; k<res1.getRowCount(); k++){
				if(res1.getValueAt(k, 0).equals(pkValue)){
					found = true;
					j = k;
					break;
				}
			}
			if(!found){
				Dbg.info("Row "+pkValue+" was not found in local table");
				inserted.put(
					conn.getQueryDelete(rel, getPkHashMap(pkValue)),
					conn.getQueryInsert(rel, res2.getRow(i))
				);
			}
		}
	}

	private Map<String, String> getPkHashMap (String pkValue) {
		String[] pkKeys = rel.getPKey().split(", ?");		// TODO: sure?
		String[] pkValues = pkValue.split(", ?");

		Map<String, String> ret = new HashMap<>();
		for(int i=0; i<pkKeys.length; i++){
			ret.put(pkKeys[i], pkValues[i]);
		}
		return ret;
	}
}
