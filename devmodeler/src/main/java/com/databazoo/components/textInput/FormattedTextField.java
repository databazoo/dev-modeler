
package com.databazoo.components.textInput;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Set;

import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.tools.formatter.FormatterBase;
import com.databazoo.devmodeler.tools.formatter.FormatterSQL;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

/**
 * Undoable text field with support for text highlighting.
 *
 * @author bobus
 */
public class FormattedTextField extends UndoableTextField {

	protected FormatterBase formatter;
	private Timer formatTimer;
	int textLen = 0;

	public FormattedTextField(){
		super("", true);
		setFormatter(new FormatterSQL());
		addOptionsAndListeners();
	}

	public FormattedTextField(String text) {
		super(text, true);
		setFormatter(new FormatterSQL());
		addOptionsAndListeners();
		format();
	}
	public FormattedTextField(FormatterBase f) {
		super("", true);
		setFormatter(f);
		addOptionsAndListeners();
	}
	public FormattedTextField(String text, FormatterBase f) {
		super(text, true);
		setFormatter(f);
		addOptionsAndListeners();
		format();
	}

	private void addOptionsAndListeners(){
		//setAutocomplete(GCFrame.getActiveWindow());
		addKeyListener(new KeyAdapter(){
			@Override
			public void keyTyped(KeyEvent ke) {
				if(isEditable()){
					format();
				}
			}
		});
		addKeyListener(new ClipboardTableKeyAdapter(this));
	}

	/**
	 * Set text and invoke formatting.
	 *
	 * @param string text
	 */
	public final void setQuery(String string) {
		setText(string);
		format();
	}

	/**
	 * Set text and notify caret handlers.
	 *
	 * Synchronized to prevent text from being updated during formatting process.
	 *
	 * @param text text
	 */
	@Override
	public void setText(String text) {
		synchronized(formatLock){
			super.setText(text);
		}
	}

	/**
	 * Set formatting strategy.
	 *
	 * @param formattingStrategy formatting strategy.
	 */
	public final void setFormatter(FormatterBase formattingStrategy){
		formatter = formattingStrategy;
		if(formatter != null) {
			if(formatter.getHilighter() != null) {
				setHighlighter(formatter.getHilighter());
			}
			formatter.addStylesToDocument(getStyledDocument());
		}
	}

	/**
	 * Request code format. Delay is based on last known size of text.
	 *
	 * Synchronized because of possible need to reset the timer.
	 */
	@Override
	public final synchronized void format(){
		if(formatTimer != null) {
			formatTimer.restart();
		}else {
			int delay;
			if (textLen > Config.FORMATTER_OVERFLOW_LIMIT) {
				delay = 4000;
			} else if (textLen > Config.FORMATTER_OVERFLOW_LIMIT2) {
				delay = 1000;
			} else {
				delay = 150;
			}
			formatTimer = new Timer(delay, e -> {
                synchronized (FormattedTextField.this) {
                    formatTimer.stop();
                    formatTimer = null;
                    formatImmediately();
                }
            });
			formatTimer.start();
		}
    }

	/**
	 * Request code format without the delay.
	 *
	 * Performs in the same thread with all required locks.
	 */
	public final void formatImmediately(){
		doFormat();
		String text = getText();
		if(text != null) {
			textLen = text.length();
			//Dbg.info("Limit is "+Config.FORMATTER_OVERFLOW_LIMIT+", length is "+textLen);
		}
    }

	/**
	 * Thread-safe getText method.
	 *
	 * @return content
	 */
	@Override
	public String getText(){
		synchronized(formatLock){
			return super.getText();
		}
	}

	/**
	 * The main formatting method.
	 *
	 * Runs the formatting strategy and restores the caret and selection afterwards.
	 */
	private void doFormat(){
		if(formatter != null) {
			synchronized(formatLock){
				int caretOld = getCaretPosition();
				int selStartOld = getSelectionStart();
				int selEndOld = getSelectionEnd();

				undoEnabled = false;
				formatter.format(FormattedTextField.this);
				undoEnabled = true;

				try {
					setCaretPosition(caretOld);
					setSelectionStart(selStartOld);
					setSelectionEnd(selEndOld);
				} catch (Exception ex){
					Dbg.fixme("set caret position failed: "+ex.getMessage(), ex);
				}
			}
		}
	}

	/**
	 * Checks for parentheses to highlight.
	 *
	 * Runs in EDT.
	 */
	@Override
	protected void caretMoved(){
		if(formatter != null) {
			Schedule.inEDT(() -> {
                String text = getText();
                int pos = getCaretPosition();

                char charAtPos = pos < text.length() ? text.charAt(pos) : 0;
                char charAtPos1 = pos > 0 ? text.charAt(pos - 1) : 0;

                // Look to the left
                if(charAtPos1 == '(' || charAtPos1 == ')' || charAtPos1 == '[' || charAtPos1 == ']' || charAtPos1 == '{' || charAtPos1 == '}'){
                    formatIfDifferentAndAllowed(pos-1);

                    // Look to the right
                }else if(charAtPos == '(' || charAtPos == ')' || charAtPos == '[' || charAtPos == ']' || charAtPos == '{' || charAtPos == '}'){
                    formatIfDifferentAndAllowed(pos);

                    // Remove highlighting if not used any more
                }else if(formatter != null && formatter.getParenAtPos() != null){
                    formatIfDifferentAndAllowed(null);
                }
            });
		}
	}

	private void formatIfDifferentAndAllowed(Integer pos){
		if(formatter.getParenAtPos() == null || !formatter.getParenAtPos().equals(pos)){
			formatter.setParenAtPos(pos);

			// Do not format when some text is being selected, may deselect in some cases
			if(getSelectionStart()-getSelectionEnd() == 0){
				if(textLen > Config.FORMATTER_OVERFLOW_LIMIT/2){
					format();
				}else{
					doFormat();
				}
			}
		}
	}

	/**
	 * Store the keyword set returned by formatter.
	 *
	 * Formatter creates a set of repeated words so that they can be added as keywords. Such keywords are then
	 * highlighted and offered for autocompletion.
	 *
	 * @param simpleKeywords keyword set
	 */
	public void updateKeywordList(Set<String> simpleKeywords) {
		autocompleteSimpleList = new String[simpleKeywords.size()];
		int i=0;
		for(String key : simpleKeywords){
			autocompleteSimpleList[i] = key;
			i++;
		}
		updateAutocomplete();
	}
}
