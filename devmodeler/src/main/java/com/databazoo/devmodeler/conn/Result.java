package com.databazoo.devmodeler.conn;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

import plugins.api.IDataWindowResult;

public class Result extends AbstractTableModel implements IDataWindowResult {
	final static int CHAR_WIDTH = 8;
	final transient List<ResultColumn> cols = new ArrayList<>();
	final transient List<ResultRow> rows = new ArrayList<>();
	final double time = 0.0;
	final boolean noResult;
	final int affectedRows;

	boolean newLine = false;

	Result(ConnectionBase.Query q, Boolean noResult) {
		if (q != null) {
			cols.addAll(q.getColumns());
			while (q.next()) {
				ResultRow r = new ResultRow(cols);
				for (int i = 1; i <= cols.size(); i++) {
					String val = q.getString(i);
					r.vals.add(val);
					if (val != null) {
						cols.get(i - 1).tryMaxW(val.length());
					}
				}
				rows.add(r);
			}
			q.close();
			this.noResult = noResult != null ? noResult : q.noResult;
			affectedRows = q.affectedRows;
		} else {
			this.noResult = noResult != null ? noResult : true;
			affectedRows = 0;
		}
	}

	public Result(ConnectionBase.Query q) {
		this(q, null);
	}

	@Override
	public int getRowCount() {
		return rows.size();
	}

	@Override
	public int getColumnCount() {
		return cols.size();
	}

	@Override
	public String getColumnName(int i) {
		return cols.get(i).name;
	}

	public int getColumnMaxW(int i) {
		return cols.get(i).getLongest() * CHAR_WIDTH;
	}

	@Override
	public Object getValueAt(int r, int c) {
		return rows.get(r).vals.get(c);
	}

	@Override
	public void setValueAt(Object val, int r, int c) {
		rows.get(r).vals.set(c, val);
	}

	@Override
	public double getTime() {
		return time;
	}

	public void showNewLine(boolean show) {
		if (newLine != show) {
			newLine = show;
			if (show) {
				ResultRow r = new ResultRow(cols);
				for (int i = 1; i <= cols.size(); i++) {
					r.vals.add("");
				}
				rows.add(r);
			} else {
				rows.remove(rows.size() - 1);
			}
		}
	}

	public boolean isNewLine(int row) {
		return rows.size() - 1 == row;
	}

	public ResultRow getRow(int row) {
		return rows.get(row);
	}

	@Override
	public int getAffectedRows() {
		return affectedRows;
	}

	@Override
	public boolean isEmpty() {
		return noResult;
	}

	void setColW(int[] widths) {
		if (cols.size() > widths.length) {
			throw new IllegalArgumentException("There are " + cols.size() + " columns in table, but received widths for only " + widths.length);
		}
		for (int i = 0; i < cols.size(); i++) {
			cols.get(i).setLongest(widths[i] / Result.CHAR_WIDTH);
		}
	}
}
