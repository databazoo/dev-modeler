package com.databazoo.devmodeler.gui.window.datawindow;

import com.databazoo.components.UIConstants;
import com.databazoo.components.table.BigTextTableCellEditor;
import com.databazoo.components.table.EditableTable;
import com.databazoo.components.table.LineNumberScrollPane;
import com.databazoo.components.table.UnfocusableTableCellEditor;
import com.databazoo.components.text.SelectableText;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.ConnectionUtils;
import com.databazoo.devmodeler.conn.DBCommException;
import com.databazoo.devmodeler.conn.Result;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.OperationCancelException;
import com.databazoo.devmodeler.gui.RightClickMenu;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.project.RecentQuery;
import com.databazoo.devmodeler.project.RevisionFactory;
import com.databazoo.devmodeler.wizards.DataExportWizard;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;
import com.databazoo.tools.XMLWriter;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.databazoo.devmodeler.conn.ConnectionUtils.ORDER_BY;
import static com.databazoo.devmodeler.conn.ConnectionUtils.SELECT;
import static com.databazoo.devmodeler.conn.ConnectionUtils.WHERE;

abstract class DataWindowOutputData extends DataWindowBase {

	static final String L_SELECTED_CELLS = "Selected cells";
	static final String L_ALL_CELLS = "All cells";
	static final String L_SELECTED_ROWS_ALL_COLUMNS = "Selected rows (all columns)";
	static final String L_SELECTED_COLUMNS_ALL_ROWS = "Selected columns (all rows)";

	static final Dimension BUTTON_SIZE = new Dimension(36,32);

	protected Result result;

	LineNumberScrollPane outputScrollData;
	EditableTable outputData;

	JButton btnSave, btnExport;
	String lastSQL;

	private final Map<String,Integer> savedWidths = new HashMap<>();
	private final StringCellEditor stringCellEditor = new StringCellEditor();
	private final DataScriptGenerator dataScriptGenerator;

	private Map<String,Attribute> constraintColumnUsage;

	int editingRow;
	private boolean removingEditor = true;

	DataWindowOutputData() {
		dataScriptGenerator = new DataScriptGenerator(this);
	}

	protected abstract boolean getReloadAfterSave();
	protected abstract boolean getAddToRevision();

	protected void setEditing(int row){
		outputScrollData.header.setEditing(row);
		editingRow = row;
	}

	void prepareButtons(){
		btnSave = new JButton(Theme.getSmallIcon(Theme.ICO_SAVE));
		btnSave.setToolTipText("Save edited row");
		btnSave.setPreferredSize(BUTTON_SIZE);
		btnSave.setFocusable(false);
		btnSave.setEnabled(false);
		btnSave.addActionListener(ev -> {
			outputData.editingStopped(null);
			if(result.isNewLine(editingRow)){
				Schedule.inWorker(Schedule.CLICK_DELAY, this::saveNewRow);
			}
		});

		btnExport = new JButton(Theme.getSmallIcon(Theme.ICO_EXPORT));
		btnExport.setToolTipText("Export selected data");
		btnExport.setPreferredSize(BUTTON_SIZE);
		btnExport.setFocusable(false);
		btnExport.setEnabled(false);
		btnExport.addActionListener(ev -> DataExportWizard.get().drawExport(outputData.getModel()));
	}

	void updateModel(){
		if(result != null){
			Schedule.inEDT(() -> {
                outputData.setModel(result);
                btnExport.setEnabled(result.getRowCount() > 1 || !result.isNewLine(0));
                if(result.getColumnCount() > 3){
                    outputData.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                    Font nameFont = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
                    for(int i=0; i<result.getColumnCount(); i++)
                    {
                        TableColumn col = outputData.getColumnModel().getColumn(i);
                        col.setCellEditor(stringCellEditor);

                        Integer colWidth = savedWidths.get(result.getColumnName(i));
                        if(colWidth == null){
                            FontMetrics fm = UIConstants.GRAPHICS.getFontMetrics(nameFont);
                            int width = fm.stringWidth(result.getColumnName(i));
                            int colMaxW = result.getColumnMaxW(i);

                            if(colMaxW >= Config.DATA_COL_WIDTH_CHAR_LIMIT){
                                colMaxW = Config.DATA_COL_WIDTH_CHAR_LIMIT;
                            }
                            if(width < colMaxW){
                                width = colMaxW;
                            }
                            col.setPreferredWidth(width+20);
                        }else{
                            col.setPreferredWidth(colWidth);
                        }
                    }
                }else{
                    outputData.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
                    for(int i=0; i<result.getColumnCount(); i++){
                        outputData.getColumnModel().getColumn(i).setCellEditor(stringCellEditor);
                    }
                }
            });
		}
	}

	void prepareOutputData(){
		outputData = new EditableTable(getDefaultTableModel()) {
			@Override
			protected boolean isColEditable(int colIndex) {
				return rel != null && rel.getPkCols().length > 0 && rel.getColType(outputData.getColumnName(colIndex)) != null;
			}

			@Override
			public void removeEditor(){
				super.removeEditor();
				if(removingEditor) {
					setEditing(-1);
				}else{
					removingEditor = true;
				}

			}
		};
		outputData.addMouseListener(new CellContextMenu());
		final TableCellRenderer hr = outputData.getTableHeader().getDefaultRenderer();
		outputData.getTableHeader().setDefaultRenderer((table, value, isSelected, hasFocus, row, column) -> {
            JLabel colLabel = (JLabel) hr.getTableCellRendererComponent(table, value, false, false, row, column);

            if(rel != null){
                String type = rel.getColType(value.toString());
                Attribute reference = constraintColumnUsage == null ? null : constraintColumnUsage.get(value.toString());
                if (rel.pKeyContains(value.toString())) {
                    colLabel.setText("<html><font color=#B04800>"+value+"</font><br><font size=2 color=gray>[PK] "+type+"</font></html>");

                } else if (reference != null) {
                    colLabel.setText("<html><font color=#009C00>"+value+"</font><br><font size=2 color=gray>[R] "+type+"</font></html>");

                } else if (type != null) {
                    colLabel.setText("<html><font color=black>"+value+"</font><br><font size=2 color=gray>"+type+"</font></html>");

                } else {
                    colLabel.setText("<html><font color=gray>"+value+"</font><br>&nbsp;</html>");
                }
            }else{
                colLabel.setText("<html><font color=gray>"+value+"</font><br>&nbsp;</html>");
            }
            return colLabel;
        });
		outputData.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, true), "removeSelectedRows");
		outputData.getActionMap().put("removeSelectedRows", new AbstractAction("del") { @Override public void actionPerformed(ActionEvent e) {
			deleteRows(outputData.getSelectedRows());
		} });
		outputData.getTableHeader().addMouseListener(new TableColumnWidthListener());
		outputData.setCellSelectionEnabled(true);

		outputScrollData = new LineNumberScrollPane(outputData);
		outputScrollData.setPreferredSize(new Dimension(frame.getSize().width, frame.getSize().height - 60));
		outputScrollData.getVerticalScrollBar().setBlockIncrement(40);

		JPanel pane = new JPanel(new GridLayout(1,0,0,0));
		pane.add(btnSave);
		pane.add(btnExport);
		outputScrollData.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, pane);
	}

	private void deleteRows(final int[] selectedRows){
		StringBuilder sql = new StringBuilder();
		Result model = (Result) outputData.getModel();
		for (int selectedRow : selectedRows) {
			if (model.isNewLine(selectedRow)) {
				continue;
			}
			sql.append("\n").append(connection.getQueryDelete(rel, getPKeyValues(model, selectedRow)));
		}
		if(sql.length() > 0){
			Object[] options = {"Delete", "Cancel"};
			Component message = new SelectableText(sql.toString(), false);

			// Check size
			if(selectedRows.length > Config.DELETE_WINDOW_MAX_ROWS){
				JScrollPane scrollPane = new JScrollPane(message);
				Dimension oldPreferredSize = message.getPreferredSize();
				scrollPane.setPreferredSize(new Dimension(oldPreferredSize.width < Config.DELETE_WINDOW_MAX_WIDTH ? oldPreferredSize.width+25 : Config.DELETE_WINDOW_MAX_WIDTH, Config.DELETE_WINDOW_MAX_ROWS*18));
				message = scrollPane;
			}

			// Show confirmation window
			int n = JOptionPane.showOptionDialog(frame, message, "Delete rows", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			if(n == 0){
				saveRow(sql.toString(), "-- N/A");
			}
		}
	}

	void saveNewRow(){
		saveRow(connection.getQueryInsert(rel, ((Result) outputData.getModel()).getRow(editingRow)), "/** Not required **/");
	}

	private void saveRow(final String sql, final String revert){
		saveRow(sql, revert, -1, -1, null, null);
	}

	private void saveRow(final String sql, final String revert, final int row, final int col, final String newValue, final String oldValue) {
		Schedule.inWorker(() -> {
            int log = DesignGUI.getInfoPanel().write(sql);
            int res;
            try {
				if(getAddToRevision()) {
					RevisionFactory.getCurrent(connection, "Manual data changes" + (rel != null ? " in " + rel.getFullName() : ""))
							.addDifference(database, new Date(), "Data", "Manual data change", sql, revert);
				}
                connection.run(sql, database);
                DesignGUI.getInfoPanel().writeOK(log);
                res = RecentQuery.RESULT_EMPTY;
                setEditing(-1);
                if(getReloadAfterSave()) {
                    runQuery(lastSQL);
                }
            } catch (DBCommException ex) {
            	Dbg.notImportant("Saving data failed", ex);
                res = RecentQuery.RESULT_FAILED;
                String message = "SQL could not be executed correctly. Data was not saved.\n\n"
                        + /*"QUERY: "+*/sql+"\n\n"
                        + connection.getCleanError(ex.getLocalizedMessage());
                JOptionPane.showMessageDialog(frame, new SelectableText(message, false), "Error while saving row", JOptionPane.ERROR_MESSAGE);
                DesignGUI.getInfoPanel().writeFailed(log, ex.getMessage());

                // restore edit
                if(row != -1 && col != -1){
                    outputData.setValueAt(oldValue, row, col);
                    outputData.changeSelection(row, col, false, false);
                    Dbg.info("Neglecting "+newValue);
                    /*
                        // THIS JUST DOES NOT WORK...
                        setEditing(row);
                        Dbg.info(outputData.editCellAt(row, col));
                    */
                }
            } catch (OperationCancelException e) {
				Dbg.notImportantAtAll("Revision cancelled.", e);
				if(getReloadAfterSave()) {
					runQuery(lastSQL);
				}
				return;
			}
			RecentQuery.add(new RecentQuery(rel == null ? "" : rel.getFullName(), sql, res, 0));
        });
	}

	private Map<String,String> getPKeyValues(Result model, int selectedRow){
		if(rel == null){
			return new HashMap<>();
		}
		Map<String,String> editorLinePKeyValues = new HashMap<>();
		for(int j=0; j < model.getColumnCount(); j++){
			if(rel.pKeyContains(model.getColumnName(j))){
				editorLinePKeyValues.put(model.getColumnName(j), (String) model.getValueAt(selectedRow, j));
			}
		}
		return editorLinePKeyValues;
	}

	void cacheFKsForColumns(){
		constraintColumnUsage = new HashMap<>();
		new ArrayList<>(rel.getConstraints()).stream()
				.filter(con -> con.getRel1().equals(rel) && con.getRel2() != null)
				.forEach(con -> constraintColumnUsage.put(con.getAttr1().getName(), con.getAttr2()));
	}

	private AbstractTableModel getDefaultTableModel(){
		return new AbstractTableModel(){
			@Override
			public String getColumnName(int col) {
				if(rel == null){
					return "";
				}
				try {
					return rel.getAttributes().get(col).getName();
				} catch (Exception ex) {
					Dbg.fixme("Getting column name failed", ex);
					return "";
				}
			}

			@Override
			public int getColumnCount(){
				if(rel == null){
					return 0;
				}
				try {
					return rel.getAttributes().size();
				} catch (Exception ex) {
					Dbg.fixme("Getting column count failed", ex);
					return 0;
				}
			}

			@Override
			public int getRowCount(){
				return 0;
			}

			@Override
			public Object getValueAt(int row, int col) {
				return "";
			}
		};
	}

	private class StringCellEditor extends UnfocusableTableCellEditor {

		private int col;
		private int row;
		private String editorOldValue;
		private Map<String,String> editorLinePKeyValues;
		private BigTextTableCellEditor bigTextEditor;

		@Override
		public Object getCellEditorValue(){
			String text = bigTextEditor == null ? editor.getText() : bigTextEditor.editor.getText();

			outputData.getModel().setValueAt(text, row, col);
			if(!((Result)outputData.getModel()).isNewLine(row)){
				if(!text.equals(editorOldValue) && !editorLinePKeyValues.isEmpty()){
					saveRow(
							connection.getQueryUpdate(rel, editorLinePKeyValues, outputData.getModel().getColumnName(col), text),
							connection.getQueryUpdate(rel, editorLinePKeyValues, outputData.getModel().getColumnName(col), editorOldValue),
							row,
							col,
							text,
							editorOldValue);
				}
			}else{
				removingEditor = false;
			}
			return text;
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object val, boolean isSelected, int row, int col) {
			super.getTableCellEditorComponent(table, val, isSelected, row, col);
			this.col = col;
			this.row = row;
			this.editorOldValue = (String) val;

			Result model = (Result) table.getModel();
			Dbg.toFile("Text edit invoked. Rel: " + rel + " Model: " + model + " PK cols: " + XMLWriter.getString(rel.getPkCols()));
			editorLinePKeyValues = new HashMap<>();
			for(int j=0; j < model.getColumnCount(); j++){
				if(rel.pKeyContains(model.getColumnName(j))){
					editorLinePKeyValues.put(model.getColumnName(j), (String) model.getValueAt(row, j));
				}
			}
			// No primary key available - stop editing
			if(editorLinePKeyValues.isEmpty()){
				Schedule.inEDT(this::stopCellEditing);
				return new JLabel((String) val);
			}
			//Dbg.info("PK found: "+XMLWriter.getString(editorLinePKeyValues));
			Dbg.toFile("Set edited row to "+row);
			setEditing(row);
			if(rel.getAttributeByName(model.getColumnName(col)).getBehavior().getAttType().matches(".*(varchar|text|bytea|blob|clob).*") &&
					(Settings.getStr(Settings.L_DATA_BIG_TEXT_EDITOR).equals("a") ||
							(Settings.getStr(Settings.L_DATA_BIG_TEXT_EDITOR).equals("l") && val != null && (((CharSequence) val).length() >= 100 || ((String) val).contains("\n")))
					)){
				Dbg.toFile("Big text editor will be used");
				bigTextEditor = new BigTextTableCellEditor(frame, (EditableTable)table, (String) val);
				return new JLabel((String) val);
			}else{
				Dbg.toFile("In-line editor will be used");
				bigTextEditor = null;
				return editor;
			}
		}
	}

	private class TableColumnWidthListener extends MouseAdapter
	{
		private int resizingColumn;
		private boolean resizing;
		@Override
		public void mousePressed(MouseEvent e) {
			// capture start of resize
			if(e.getSource() instanceof JTableHeader) {
				TableColumn tc = ((JTableHeader)e.getSource()).getResizingColumn();
				if(tc != null) {
					resizing = true;
					resizingColumn = tc.getModelIndex();
				} else {
					resizingColumn = -1;
				}
			}
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			// column resized
			if(resizing) {
				if(e.getSource() instanceof JTableHeader) {
					TableColumn tc = ((JTableHeader)e.getSource()).getColumnModel().getColumn(resizingColumn);
					if(tc != null) {
						savedWidths.put(result.getColumnName(resizingColumn), tc.getPreferredWidth());
					}
				}
			}
			resizing = false;
			resizingColumn = -1;
		}
	}

	private class CellContextMenu extends MouseAdapter {

		private final String[] scriptOptionString1 = new String[]{L_SELECTED_CELLS, L_ALL_CELLS, L_SELECTED_ROWS_ALL_COLUMNS, L_SELECTED_COLUMNS_ALL_ROWS};
		private final String[] scriptOptionString2 = new String[]{L_SELECTED_CELLS+" ", L_ALL_CELLS+" ", L_SELECTED_ROWS_ALL_COLUMNS+" ", L_SELECTED_COLUMNS_ALL_ROWS+" "};

		private RightClickMenu menu;
		private Result model;

		private int row;
		private int column;

		private String columnName;
		private String cellValue;
		private String cellValueEscaped;
		private boolean isEscapedType;

		private String conditionSelect;
		private String conditionEq;
		private String conditionNeq;
		private String conditionNull;
		private String conditionNotNull;
		private String conditionIn;
		private String conditionNotIn;
		private String conditionLike;
		private String conditionNotLike;

		@Override public void mouseClicked(MouseEvent e) {
            if((e.getModifiers() & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK){
				updateSelectedCellInfo(e);
				createSQLs();
				createMenu(e);
				createMenuItems();

			}else if((e.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK){
                if(outputData.getRowSelectionAllowed()){
                    outputData.setCellSelectionEnabled(true);
                }
            }
        }

		private void createSQLs() {
			if(cellValue != null) {
				conditionEq = columnName + " = " + cellValueEscaped;
				conditionNeq = columnName + " != " + cellValueEscaped;
			} else {
				conditionEq = null;
				conditionNeq = null;
			}

			conditionNull = columnName + " IS NULL";
			conditionNotNull = columnName + " IS NOT NULL";

			if (outputData.getSelectedColumnCount() > 1) {
				StringBuilder sqlParts = new StringBuilder();
				String comma = "";
				for (int i : outputData.getSelectedColumns()) {
					String colName = outputData.getModel().getColumnName(i);
					if(colName != null){
						sqlParts.append(comma).append(colName);
						comma = ", ";
					}
				}
				if (!comma.isEmpty()) {
					conditionSelect = sqlParts.toString();
				} else {
					conditionSelect = columnName;
				}
			} else {
				conditionSelect = columnName;
			}

			if (outputData.getSelectedRowCount() > 1) {
				StringBuilder sqlParts = new StringBuilder();
				Set<String> valueSet = new LinkedHashSet<>();
				for (int i : outputData.getSelectedRows()) {
					Object valueAt = outputData.getModel().getValueAt(i, column);
					if (valueAt != null) {
						valueSet.add(valueAt.toString());
					}
				}
				String comma = "";
				for (String data : valueSet) {
					sqlParts
							.append(comma)
							.append("'")
							.append(data)
							.append("'");
					comma = ",";
				}
				if (!comma.isEmpty()) {
					conditionIn = columnName + " IN (" + sqlParts + ")";
					conditionNotIn = columnName + " NOT" + conditionIn;
				} else {
					conditionIn = null;
					conditionNotIn = null;
				}
			} else {
				conditionIn = null;
				conditionNotIn = null;
			}

			if (isEscapedType && cellValue != null) {
				conditionLike = columnName + " LIKE '%" + cellValue.replace("'", "''") + "%'";
				conditionNotLike = columnName + " NOT LIKE '%" + cellValue.replace("'", "''") + "%'";
			} else {
				conditionLike = null;
				conditionNotLike = null;
			}

		}

		private void createMenu(MouseEvent e) {
			RightClickMenu.setLocationTo(outputData, e.getPoint());
			menu = RightClickMenu.get((type, selectedValue) -> {
				switch(type){
				case 11: deleteRows(new int[]{row}); break;
				case 12: deleteRows(outputData.getSelectedRows()); break;
				case 20: setQueryWhere(selectedValue); break;
				case 30: setQueryOrder(selectedValue); break;
				case 40: DataWindow.get().drawRelationData(connection, constraintColumnUsage.get(columnName).getRel(), false); break;
				case 41:
					final DataWindow dataWindow = DataWindow.get();
					final Attribute attribute = constraintColumnUsage.get(columnName);
					if (selectedValue.equals("Row")) {
						dataWindow.setWhere(attribute + " = " + cellValueEscaped);
					}
					dataWindow.drawRelationData(connection, attribute.getRel(), false);
					break;
				case 50: appendQuery(dataScriptGenerator.generateInsertScript(
						dataScriptGenerator.getSelectedRows(selectedValue, row),
						dataScriptGenerator.getSelectedCols(selectedValue, column),
						model
				));
					break;
				case 51: appendQuery(dataScriptGenerator.generateUpdateScript(
						dataScriptGenerator.getSelectedRows(selectedValue.substring(0, selectedValue.length()-1), row),
						dataScriptGenerator.getSelectedCols(selectedValue.substring(0, selectedValue.length()-1), column),
						model
				));
					break;
				case 60: setQuerySelect(conditionSelect); break;
				default: throw new IllegalArgumentException("Menu option " + type + " is not known");
				}
			});
		}

		private void createMenuItems() {
			if(constraintColumnUsage != null){
				Attribute attr = constraintColumnUsage.get(columnName);
				if(attr != null){
					if(cellValue != null) {
						menu.addItem("Show referenced ...", Relation.ico16, 41, new String[]{"Row", "Table"});
					} else {
						menu.addItem("Show referenced table", Relation.ico16, 40);
					}
					menu.separator();
				}
			}

			menu.addItem(SELECT + conditionSelect, RightClickMenu.ICO_SQL, 60)
					.separator();

			if (conditionIn != null) {
				menu.addItem(WHERE + " ... IN", RightClickMenu.ICO_SQL, 20, new String[] { conditionIn, conditionNotIn });
			} else if (conditionEq != null) {
				menu.addItem(WHERE + " ... =", RightClickMenu.ICO_SQL, 20, new String[] { conditionEq, conditionNeq });
			}

			if(conditionLike != null) {
				menu.addItem(WHERE + " ... LIKE", RightClickMenu.ICO_SQL, 20, new String[] { conditionLike, conditionNotLike });
			}

			menu.addItem(WHERE + " ... NULL", RightClickMenu.ICO_SQL, 20, new String[]{ conditionNull, conditionNotNull });
			menu.separator().addItem(ORDER_BY + " ...", RightClickMenu.ICO_ORDER, 30, new String[]{ columnName + " ASC", columnName + " DESC" });
			menu.separator().
					addItem("Generate INSERT", RightClickMenu.ICO_SQL, 50, scriptOptionString1).
					addItem("Generate UPDATE", RightClickMenu.ICO_SQL, 51, scriptOptionString2);

			if(!getPKeyValues((Result) outputData.getModel(), 0).isEmpty()){
				menu.separator();
				final int rowCount = outputData.getSelectedRows().length;
				if(rowCount > 1){
					menu.addItem("Delete " + rowCount + " rows", RightClickMenu.ICO_DELETE, 12);
				}else{
					menu.addItem("Delete row", RightClickMenu.ICO_DELETE, 11);
				}
			}
		}

		private void updateSelectedCellInfo(MouseEvent e) {
			// Locate selected cell
			row = outputData.rowAtPoint( e.getPoint() );
			column = outputData.columnAtPoint( e.getPoint() );

			// Get column name and cell value
			model = (Result) outputData.getModel();
			columnName = model.getColumnName(column);
			cellValue = model.getValueAt(row, column) == null ? null : model.getValueAt(row, column).toString();

			isEscapedType = cellValue != null && !cellValue.matches(ConnectionUtils.IS_NUMBER_REGEX);
			if (rel != null && !isEscapedType) {
                Attribute attr = rel.getAttributeByName(columnName);
                if (attr != null) {
					isEscapedType = getDB().getConnection().getDataTypes().isEscapedType(attr.getBehavior().getAttType());
				}
            }

			cellValueEscaped = cellValue != null && isEscapedType ? "'" + cellValue.replace("'", "''") + "'" : cellValue;
		}
	}
}
