package com.databazoo.components.textInput;

import com.databazoo.tools.Dbg;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Clipboard listener to handle tabular data pasted.
 *
 * @author bobus
 */
class ClipboardTableKeyAdapter implements KeyListener {

	private static final String LINE_BREAK = "\n";
	private static final String CELL_BREAK = "\t";
	private static final Clipboard CLIPBOARD = Toolkit.getDefaultToolkit().getSystemClipboard();
	private final FormattedTextField input;
	private boolean ctrlWasDown;


	/**
	 * Constructor
	 *
	 * @param input text field
	 */
	ClipboardTableKeyAdapter(FormattedTextField input) {
		this.input = input;
	}

	/**
	 * Key press - do nothing
	 */
	@Override
	public void keyPressed(KeyEvent event) {}

	/**
	 * Process CTRL+V event
	 */
	@Override
	public void keyReleased(KeyEvent event) {
		if (ctrlWasDown) {
			if (event.getKeyCode()==KeyEvent.VK_V) {
				pasteFromClipboard();
			}
		}
	}

	/**
	 * Process CTRL down event
	 */
	@Override
	public void keyTyped(KeyEvent event) {
		if(event != null){
			ctrlWasDown = event.isControlDown() || event.isMetaDown();
		}
	}

	/**
	 * Format pasted table
	 */
	void pasteFromClipboard(){
		final String pasteString;
		try {
			pasteString = (String)(CLIPBOARD.getContents(this).getTransferData(DataFlavor.stringFlavor));
		} catch (UnsupportedFlavorException | IOException e) {
			Dbg.notImportant("System stuff. Nothing we can do about it.", e);
			return;
		}

		String[] lines = pasteString.split(LINE_BREAK);
		List<Boolean> escapeColumn = getColumnsToEscape(lines);
		if(!escapeColumn.isEmpty()){
			StringBuilder out = new StringBuilder();
			for (int i=0; i<lines.length; i++) {
				String[] cells = lines[i].split(CELL_BREAK);
				if(i==0 /*&& cells.length == 1*/){
					out.append("(");
				}
				for (int j = 0; j < cells.length; j++) {
					if(i>0 && j==0 && cells.length>1){
						out.append("),\n(");
					}
					if(escapeColumn.get(j)){
						out.append("'").append(cells[j].replace("'", "''")).append("'");
					}else{
						out.append(cells[j].isEmpty() ? "NULL" : cells[j]);
					}
					if(j<cells.length-1){
						out.append(",");
					}
				}
				if(cells.length == 1 && i<lines.length-1){
					out.append(",");
					if(escapeColumn.get(0)){
						out.append("\n");
					}
				}
			}
			out.append(")");
			int caretPos = input.getCaretPosition();
			int start = caretPos - pasteString.length();
			Document doc = input.getDocument();
			try {
				input.undoEnabled = false;
				doc.remove(start, pasteString.length());
				input.undoEnabled = true;
				doc.insertString(start, out.toString(), null);
				input.format();
			} catch (BadLocationException e) {
				Dbg.fixme("Table paste failed", e);
			}
		}
	}

	/**
	 * Decide which columns are to be escaped
	 *
	 * @param lines lines from clipboard
	 * @return TRUE/FALSE for every column
	 */
	private List<Boolean> getColumnsToEscape(String[] lines) {
		ArrayList<Boolean> escapeColumn = new ArrayList<>();
		boolean singleLine = lines.length == 1;
		Integer colNum = null;

		// parse first 50 lines
		for (int i=0 ; i<lines.length && i<50; i++) {
			String[] cells = lines[i].split(CELL_BREAK);

			if(singleLine && cells.length <= 3){
				return Collections.emptyList();
			}
			if(colNum != null){
				if(colNum.compareTo(cells.length) != 0) {
					return Collections.emptyList();
				}
			}else{
				colNum = cells.length;
			}

			for (int j=0 ; j<cells.length; j++) {
				if(escapeColumn.size() <= j){
					escapeColumn.add(false);
				}
				if(!escapeColumn.get(j)){
					boolean escape = cells[j].matches(".*[^0-9.].*");
					if(escape){
						escapeColumn.set(j, true);
					}
				}
				boolean isSQL = cells[j].matches("(.*(INSERT |UPDATE |DELETE |SELECT |CREATE |WHERE |ORDER BY |(LEFT |RIGHT |INNER )?JOIN |[!@#$^*;]).*|\\s*[('].*)");
				if(isSQL && allPreviousAreEmpty(cells, j)){
					return Collections.emptyList();
				}
			}
		}
		return escapeColumn;
	}

	private boolean allPreviousAreEmpty(String[] cells, int j) {
		for (int i = 0; i < j; i++) {
			if (!cells[i].isEmpty()) {
				return false;
			}
		}
		return true;
	}
}
