
package com.databazoo.components.table;

import com.databazoo.components.GCFrame;
import com.databazoo.components.containers.VerticalContainer;
import com.databazoo.components.textInput.TextScrollPane;
import com.databazoo.components.textInput.UndoableTextField;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serializable;

/**
 * Table cell text editor that opens in a dialog window instead of inline text field.
 *
 * @author bobus
 */
public class BigTextTableCellEditor implements Serializable {
	private static final long serialVersionUID = 1905122041950000001L;

	private final EditableTable editedTable;
	private final JDialog frame;
	public final UndoableTextField editor = new UndoableTextField();

	/**
	 * Constructor
	 *
	 * @param mainWindow parent window to lock
	 * @param table edited table
	 * @param text text from edited cell
	 */
	public BigTextTableCellEditor(Frame mainWindow, EditableTable table, String text) {
		editedTable = table;

		editor.setPreferredSize(new Dimension(600, 400));
		editor.setText(text);
		editor.addKeyListener(new KeyAdapter(){
			@Override public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ESCAPE){
					cancel();
					e.consume();
				}else if(e.getKeyCode() == KeyEvent.VK_S && (e.isControlDown() || e.isMetaDown())){
					done();
					e.consume();
				}
			}
		});
		selectAll();

		frame = new JDialog(mainWindow, "Edit cell", Dialog.ModalityType.MODELESS);
		frame.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter(){
			@Override public void windowClosing(WindowEvent e) {
				cancel();
			}
		});
		JButton btnSave = new JButton("Save");
		btnSave.setFocusable(false);
		btnSave.addActionListener(ae -> done());
		JButton btnCancel = new JButton("Cancel");
		btnSave.setFocusable(false);
		btnCancel.addActionListener(ae -> cancel());

		JPanel buttonPane = new JPanel();
		buttonPane.add(btnSave);
		buttonPane.add(btnCancel);

		frame.setContentPane(new VerticalContainer(null, new TextScrollPane(editor), buttonPane));
		frame.pack();
		frame.setLocationRelativeTo(mainWindow);
		frame.setVisible(GCFrame.SHOW_GUI);
	}

	private void done() {
		editedTable.editingStopped(null);
		frame.dispose();
	}

	void cancel() {
		editedTable.editingCanceled(null);
		frame.dispose();
	}

	/**
	 * Select all text on open
	 */
	private void selectAll(){
		Schedule.inEDT(() -> {
            if(!editedTable.isEditingFromKeyboard){
                editor.setSelectionStart(0);
                editor.setSelectionEnd(editor.getText().length());
            }else{
                editor.setText(editedTable.charFromKeyBoard != 8 ? String.valueOf(editedTable.charFromKeyBoard) : "");
            }
            editor.requestFocusInWindow();
        });
	}
}
