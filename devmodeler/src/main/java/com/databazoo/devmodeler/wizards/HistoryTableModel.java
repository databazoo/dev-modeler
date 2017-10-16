
package com.databazoo.devmodeler.wizards;

import javax.swing.table.AbstractTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.databazoo.components.table.EditableTable;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.Revision;

/**
 * Table model for detailed changes list in Relation Wizard
 * @author bobus
 */
public class HistoryTableModel extends AbstractTableModel {

	private final String[] cols = {"Revision", "Author", "Date"};
	private final transient List<Revision.Diff> rows = new ArrayList<>();

	public HistoryTableModel(IModelElement editableElement) {
		loadRevisions(editableElement);
	}

	private void loadRevisions(IModelElement editableElement) {
		Set<IModelElement> elements = editableElement.getAllSubElements();
		for(Revision rev : Project.getCurrent().revisions){
			List<Revision.Diff> diffs = rev.getChanges();
			for(int i = diffs.size()-1; i >= 0; i--){
				Revision.Diff diff = diffs.get(i);
				String className = diff.getElementClass();
				String fullName = diff.getElementFullName();
				elements.stream()
						.filter(element -> className.equalsIgnoreCase(element.getClassName()) && fullName.equalsIgnoreCase(element.getFullName()))
						.forEach(element -> {
							diff.setRevision(rev);
							rows.add(diff);
						});
			}
		}
		Collections.sort(rows, Collections.reverseOrder());
	}

	@Override
	public String getColumnName(int col) {
		return cols[col];
	}

	@Override
	public int getRowCount(){
		return rows.size();
	}

	@Override
	public int getColumnCount(){
		return cols.length;
	}

	@Override
	public Object getValueAt(int row, int col) {
		if(row >= getRowCount()){
			return "";
		}else{
			switch (col) {
				case 0: return rows.get(row).getRevision().getName();
				case 1: return rows.get(row).getRevision().getAuthor();
				case 2: return Config.DATE_TIME_FORMAT.format(rows.get(row).getCreated());
				default:
					return "";
			}
		}
	}

	@Override
	public synchronized void setValueAt(Object value, int row, int col) {
		throw new IllegalStateException("No updates to history table");
	}

	public MouseListener getMouseListener(EditableTable table){
		return new RevisionMouseHandler(table);
	}

	private class RevisionMouseHandler extends MouseAdapter {
		private final EditableTable table;

		private RevisionMouseHandler(EditableTable table) {
			this.table = table;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				Revision.Diff diff = rows.get(table.getSelectedRow());
				DiffWizard diffWizard = DiffWizard.get();
				diffWizard.drawRevision(diff.getRevision());
				diffWizard.tree.selectRow(diff.toString());
			}
		}
	}
}
