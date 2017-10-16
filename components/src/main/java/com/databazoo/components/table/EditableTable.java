
package com.databazoo.components.table;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.databazoo.components.UIConstants;
import com.databazoo.tools.Dbg;

/**
 * JTable with configurable editable cells
 *
 * @author bobus
 */
public abstract class EditableTable extends JTable {
	public static final String L_DOUBLECLICK_TO_EDIT = "<i>&nbsp;Note: doubleclick a cell to edit it's content. Hit DELETE to remove a line.</i>";

	public int row = -1;
	public int col = -1;

	private Point lastEditedCell;
	boolean isEditingFromKeyboard = false;
	char charFromKeyBoard;

	/**
	 * Constructor
	 *
	 * @param tableModel AbstractTableModel
	 */
	public EditableTable(AbstractTableModel tableModel) {
		super(tableModel);
		ListSelectionListener listener = lse -> {
            if(getSelectedRow() >= 0) {
                lastEditedCell = new Point(getSelectedColumn(), getSelectedRow());
            }
        };
		getColumnModel().getSelectionModel().addListSelectionListener(listener);
		getSelectionModel().addListSelectionListener(listener);
		getTableHeader().setReorderingAllowed(false);
		addKeyListener(new KeyAdapter(){

			@Override
			public void keyPressed(KeyEvent ke) {
				if(!isEditing() && getSelectedColumn() >= 0 && getSelectedRow() >= 0 && isColEditable(getSelectedColumn())){
					if(ke.getKeyCode() == KeyEvent.VK_DELETE){
						final Timer timer = new Timer(10, null);
						timer.addActionListener(e -> {
                            timer.stop();
                            editingCanceled(null);
                        });
						timer.start();
					}else if(!(ke.isAltDown() || ke.isControlDown() || ke.isMetaDown()) && ke.getKeyCode() != 524 && ke.getKeyCode() != 16){
						isEditingFromKeyboard = true;
						charFromKeyBoard = ke.getKeyChar();
						editCellAt(getSelectedRow(), getSelectedColumn());
						final Timer timer = new Timer(UIConstants.TYPE_TIMEOUT, null);
						timer.addActionListener(e -> {
                            timer.stop();
                            isEditingFromKeyboard = false;
                        });
						timer.start();
					}
				}
			}
		});
	}

	/**
	 * Decide if the cell is editable
	 *
	 * @param nRow row number
	 * @param nCol column number
	 * @return is editable?
	 */
	@Override
	public boolean isCellEditable(int nRow, int nCol) {
		if(row == nRow && col == nCol){
			return isColEditable(col);
		}else{
			col = nCol;
			row = nRow;
			final Timer timer = new Timer(UIConstants.TYPE_TIMEOUT, null);
			timer.addActionListener(e -> {
                timer.stop();
                col = -1;
                row = -1;
            });
			timer.start();
			return false;
		}
	}

	/**
	 * Swap the model
	 *
	 * @param dataModel AbstractTableModel
	 */
	@Override
	public void setModel(TableModel dataModel) {
		super.setModel(dataModel);
		final Timer timer = new Timer(UIConstants.TYPE_TIMEOUT, null);
		timer.addActionListener(e -> {
            timer.stop();
            if(lastEditedCell != null){
                try {
                    changeSelection(lastEditedCell.y, lastEditedCell.x, false, false);
                    //clearLastEditedCell();
                    requestFocusInWindow();
                } catch (Exception ex){
                    Dbg.notImportant("Refocus of selected table cell after model reload failed. User will hardly notice.", ex);
                }
            }
        });
		timer.start();
	}

	/**
	 * Operate last edited cell
	 */
	public void clearLastEditedCell(){
		lastEditedCell = null;
	}

	/**
	 * Decide if the column is editable
	 *
	 * @param colIndex column index
	 * @return is editable?
	 */
	abstract protected boolean isColEditable(int colIndex);

}
