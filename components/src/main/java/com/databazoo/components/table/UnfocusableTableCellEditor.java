
package com.databazoo.components.table;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;

import com.databazoo.tools.Schedule;

/**
 * Table cell editor that disappears on focus loss.
 *
 * @author bobus
 */
public class UnfocusableTableCellEditor extends AbstractCellEditor implements TableCellEditor {

	protected JTextComponent editor = new JTextField();
	private EditableTable editedTable;

	/**
	 * Constructor
	 */
	public UnfocusableTableCellEditor(){
		this(new JTextField());
	}

	/**
	 * Constructor
	 *
	 * @param editor existing text component
	 */
	public UnfocusableTableCellEditor(JTextComponent editor){
		super();
		this.editor = editor;
		editor.setBorder(BorderFactory.createCompoundBorder( BorderFactory.createLineBorder(Color.ORANGE, 1), BorderFactory.createEmptyBorder(0,4,0,4)));
		editor.addFocusListener(new FocusAdapter(){
			@Override public void focusLost(FocusEvent fe) { editedTable.editingStopped(changeEvent); }
		});

		// ^A ^C ^V ^X fix for MacOS
		editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), DefaultEditorKit.selectAllAction);
		editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), DefaultEditorKit.copyAction);
		editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), DefaultEditorKit.pasteAction);
		editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), DefaultEditorKit.cutAction);
	}

	/**
	 * Returns the value contained in the editor.
	 * @return the value contained in the editor
	 */
	@Override
	public Object getCellEditorValue(){
		return editor.getText();
	}

	/**
	 *  Sets an initial <code>value</code> for the editor.  This will cause
	 *  the editor to <code>stopEditing</code> and lose any partially
	 *  edited value if the editor is editing when this method is called. <p>
	 *
	 *  Returns the component that should be added to the client's
	 *  <code>Component</code> hierarchy.  Once installed in the client's
	 *  hierarchy this component will then be able to draw and receive
	 *  user input.
	 *
	 * @param   table           the <code>JTable</code> that is asking the
	 *                          editor to edit; can be <code>null</code>
	 * @param   val             the value of the cell to be edited; it is
	 *                          up to the specific editor to interpret
	 *                          and draw the value.  For example, if value is
	 *                          the string "true", it could be rendered as a
	 *                          string or it could be rendered as a check
	 *                          box that is checked.  <code>null</code>
	 *                          is a valid value
	 * @param   isSelected      true if the cell is to be rendered with
	 *                          highlighting
	 * @param   row             the row of the cell being edited
	 * @param   col             the column of the cell being edited
	 * @return  the component for editing
	 */
	@Override
	public Component getTableCellEditorComponent(JTable table, Object val, boolean isSelected, int row, int col) {
		editedTable = (EditableTable) table;
		editor.setText((String) val);
		selectAll();
		return editor;
	}

	/**
	 * Select all text on open.
	 */
	protected void selectAll(){
		Schedule.inEDT(() -> {
            if(!editedTable.isEditingFromKeyboard){
                editor.setSelectionStart(0);
                editor.setSelectionEnd(editor.getText().length());
            }else{
                editor.setText(editedTable.charFromKeyBoard != 8 ? String.valueOf(editedTable.charFromKeyBoard) : "");
                editor.requestFocusInWindow();
            }
        });
	}
}
