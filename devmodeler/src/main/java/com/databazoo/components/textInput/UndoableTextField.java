package com.databazoo.components.textInput;

import com.databazoo.components.AutocompletePopupMenu;
import com.databazoo.components.FontFactory;
import com.databazoo.components.GCFrameWithObservers;
import com.databazoo.components.UIConstants;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.gui.DBTree;
import com.databazoo.devmodeler.model.DataTypes;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.tools.Geometry;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * Undo-enabled text field with autocompletion support.
 *
 * @author bobus
 */
public class UndoableTextField extends JTextPane {
	// Double space places the caret there.
	public static final String[] KEYWORDS_SQL = new String[]{
			"SELECT ", "SELECT * FROM ", "SELECT count(*) FROM ",
			"INSERT INTO ", "INSERT INTO  () VALUES ();",
			"UPDATE ", "UPDATE  SET  WHERE ;",
			"DELETE FROM ", "DELETE FROM  WHERE ;",
			"FROM ", "INNER JOIN ", "OUTER JOIN ", "FULL OUTER JOIN ", "JOIN ", "LEFT JOIN ", "RIGHT JOIN ", "CROSS JOIN ",
			"WHERE ", "HAVING ", "GROUP BY ", "ORDER BY ", "ASC", "DESC", "OFFSET ", "LIMIT ", "UNION ", "UNION ALL ",
			"DISTINCT ", "DISTINCT ON ()", "NULL", "IS NULL", "IS NOT NULL", "IS DISTINCT FROM ", "IS NOT DISTINCT FROM ",
			"OR ", "AND ", "AS ", "ON ", "USING ()", "IN ()", "NOT IN ()", "INTO ",
			"LIKE '%%'", "ILIKE '%%'", "BETWEEN ", "TOP ",
			"TRUE", "FALSE",
			"VACUUM VERBOSE ", "VACUUM VERBOSE ANALYZE ", "ANALYZE VERBOSE ", "VACUUM FULL VERBOSE ", "VACUUM FULL VERBOSE ANALYZE ",
			"BEGIN", "END", "LOOP", "LOOP  END LOOP;", "END IF;", "END LOOP;", "CASE WHEN  THEN  END", "WHEN  THEN  ", "IF  THEN  ELSE  END IF;", "THEN ", "ELSE ",
			"FOUND ", "NOT FOUND ", "VALUES ()", "RETURNING ",
			"EXECUTE ", "RETURN ", "DECLARE ",
			"COMMIT;", "ROLLBACK;",
			"CASCADE", "RESTRICT", "SET NULL",
			"RAISE NOTICE '%', ", "RAISE EXCEPTION '%', ",
			"MATERIALIZED ",

			"date_trunc('seconds', now())", "date_trunc('months', now())", "now()", "count(*)", "coalesce()", "nullif()", "quote_nullable()", "quote_literal()",
			"auto_increment", "CURRENT_DATE", "CURRENT_TIMESTAMP", "SYSDATE", "from_unixtime()", "unix_timestamp()", //"PROCESSLIST", "MASTER", "SLAVE", "DATABASES",
			"ARRAY[]", "ARRAY_UPPER()", "TG_OP", "ROWNUM ", "ROWNUM <= 100",
			"TRUNCATE TABLE ", //"SHOW",
			"SHOW PROCESSLIST", "SHOW DATABASES", "SHOW MASTER STATUS", "SHOW SLAVE STATUS",

			"CREATE ", "CREATE OR REPLACE ",
			"CREATE DATABASE ", "CREATE USER ", "CREATE ROLE ", "CREATE SCHEMA ", "CREATE TABLE ", "CREATE COLUMN ", "CREATE INDEX ", "CREATE VIEW ",
			"CREATE MATERIALIZED VIEW ", "CREATE FUNCTION ", "CREATE PROCEDURE ", "CREATE PACKAGE ", "CREATE TRIGGER ", "CREATE CONSTRAINT ", "CREATE SEQUENCE ",
			"ALTER ",
			"ALTER DATABASE ", "ALTER USER ", "ALTER ROLE ", "ALTER SCHEMA ", "ALTER TABLE ", "ALTER COLUMN ", "ALTER INDEX ", "ALTER VIEW ",
			"ALTER FUNCTION ", "ALTER PROCEDURE ", "ALTER PACKAGE ", "ALTER TRIGGER ", "ALTER CONSTRAINT ", "ALTER SEQUENCE ",
			"DROP ",
			"DROP DATABASE ", "DROP USER ", "DROP ROLE ", "DROP SCHEMA ", "DROP TABLE ", "DROP COLUMN ", "DROP INDEX ", "DROP VIEW ",
			"DROP FUNCTION ", "DROP PROCEDURE ", "DROP PACKAGE ", "DROP TRIGGER ", "DROP CONSTRAINT ", "DROP SEQUENCE ",
			"IDENTIFIED BY ", "pwrd ",
			"GRANT ", "GRANT ALL ON ", "GRANT ALL ON DATABASE  TO ;",

			"PERFORM ", "FOREACH ", "WINDOW ", "PARTITION BY ", "EXCEPT ", "NULLS FIRST", "NULLS LAST", "WITH ", "WITH RECURSIVE ", "ONLY "
	};

	/**
	 * For use with Next Field Observer
	 */
	boolean moveKeysDisabled = false;
	boolean undoEnabled = true;

	final transient Object formatLock = new Object();

	private final CompoundUndoManager undoManager = new CompoundUndoManager(this);
	private TextFieldFinder finder;
	private String[] autocompleteOptionList = new String[0];
	private String[] autocompleteNameList = new String[0];
	private String[] autocompleteSQLList = new String[0];
	String[] autocompleteSimpleList = new String[0];

	private Timer popupTimer;
	private int popupCorrectionX = 0;
	private final PopupActionListener popupActionListener = new PopupActionListener();
	private int caretOld;
	private String selectedWord;

	public UndoableTextField(String text, boolean monoFont) {
		super();
		setEditorKit(new TabSizeEditorKit(true));
		setText(text);
		setKeyListeners();
		if (monoFont) {
			setFont(FontFactory.getMonospaced(Font.PLAIN, Settings.getInt(Settings.L_FONT_MONO_SIZE)));
		}
		setCaretPosition(0);
	}

	public UndoableTextField(String text) {
		super();
		setEditorKit(new TabSizeEditorKit(true));
		setText(text);
		setKeyListeners();
		setCaretPosition(0);
	}

	public UndoableTextField() {
		super();
		setEditorKit(new TabSizeEditorKit(true));
		setKeyListeners();
		setCaretPosition(0);
	}

	public UndoableTextField(boolean isWrapEnabled) {
		super();
		setEditorKit(new TabSizeEditorKit(isWrapEnabled));
		setKeyListeners();
		setCaretPosition(0);
	}

	@Override
	public String getText() {
		String forceEOL = Settings.getStr(Settings.L_FONT_TEXT_EOL);
		String text = super.getText();
		if (text != null && forceEOL != null && !forceEOL.isEmpty()) {
			return text.replaceAll("\r?\n", forceEOL);
		} else {
			return text;
		}
	}

	/**
	 * Set text and notify caret handlers.
	 *
	 * @param text text
	 */
	@Override
	public void setText(String text) {
		super.setText(text);
		caretMoved();
	}

	private void setKeyListeners() {
		undoManager.updateOriginalText();
		getDocument().addUndoableEditListener(undoManager);
		getActionMap().put("Undo", undoManager.undoAction);
		getActionMap().put("Redo", undoManager.redoAction);
		getActionMap().put("Find",
				new AbstractAction("Find") {
					@Override
					public void actionPerformed(ActionEvent evt) {
						if (finder == null) {
							finder = TextFieldFinder.getFinder(UndoableTextField.this);
						} else {
							finder.updateTextToSelection();
							finder.focus();
						}
						if (UndoableTextField.this instanceof FormattedClickableTextField) {
							FormattedClickableTextField f = (FormattedClickableTextField) UndoableTextField.this;
							f.formatter.isControlDown = false;
							f.format();
						}
					}
				});

		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "Undo");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "Redo");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "Find");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "Find");

		if (UIConstants.isMac()) {
			// ^A ^C ^V ^X fix for MacOS
			getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), DefaultEditorKit.selectAllAction);
			getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), DefaultEditorKit.copyAction);
			getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), DefaultEditorKit.pasteAction);
			getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), DefaultEditorKit.cutAction);

			// HOME END PGUP PGDOWN fix for MacOS
			getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), DefaultEditorKit.endLineAction);
			getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), DefaultEditorKit.beginLineAction);
			getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), DefaultEditorKit.pageUpAction);
			getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), DefaultEditorKit.pageDownAction);
		}

		addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent fe) {
				AutocompletePopupMenu.get().dispose();
			}
		});

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent me) {
				AutocompletePopupMenu.get().dispose();
			}
		});

		int blinkRate = getCaret().getBlinkRate();
		setCaret(new DefaultCaret() {

			/**
			 * Handle selection by double click - split by fullstops, etc.
			 */
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!e.isConsumed() && e.getButton() == 1 && e.getClickCount() == 2) {
					selectByDoubleClick();
					e.consume();
					return;
				}

				super.mouseClicked(e);
			}

			/**
			 * Consume double mouse press events.
			 */
			@Override
			public void mousePressed(MouseEvent e) {
				if (!e.isConsumed() && e.getButton() == 1 && e.getClickCount() == 2) {
					e.consume();
					return;
				}

				super.mousePressed(e);
			}

			/**
			 * Default positioning of the caret is to the left of the clicked char. That means if you click the
			 * right-most pixel of a tab character the caret is actually placed about 20 pixels left.
			 *
			 * This override calculates a possible next char's location and splits the distance in half. The caret is
			 * then positioned properly - to the right or to the left of the clicked character.
			 */
			@Override
			protected void positionCaret(MouseEvent e) {
				if (!e.isConsumed()) {
					super.positionCaret(e);
					try {
						int mouseX = e.getPoint().x;
						int caretPos = getCaretPosition();

						// This is a hack to prevent BadLocationException without calling getText()
						boolean nextMayBeAvailable = !(UndoableTextField.this instanceof FormattedTextField) || ((FormattedTextField)UndoableTextField.this).textLen > caretPos;

						if(nextMayBeAvailable && (UndoableTextField.this instanceof FormattedTextField || getText().length() > caretPos)) {
							Rectangle2D curr = modelToView2D(caretPos);
							Rectangle2D next = modelToView2D(caretPos + 1);
							int currY = (int) curr.getY();
							int nextY = (int) next.getY();
							int currX = (int) curr.getX();
							int nextX = (int) next.getX();
							if (currY == nextY && currX <= mouseX && mouseX <= nextX) {
								int middle = currX + (nextX - currX) / 2;
								if (e.getPoint().x > middle) {
									setCaretPosition(caretPos + 1);
								}
							} else if (mouseX < currX && caretPos > 0) {
								Rectangle2D prev = modelToView2D(caretPos - 1);
								int prevX = (int) prev.getX();
								int prevY = (int) prev.getY();
								if (currY == prevY) {
									int middle = prevX + (currX - prevX) / 2;
									if (e.getPoint().x < middle) {
										setCaretPosition(caretPos - 1);
									}
								}
							}
						}
					} catch (BadLocationException ex) {
						Dbg.notImportant(Dbg.MANIPULATING_TEXT_OUTSIDE, ex);
					}

					// Caret moved event
					caretMoved();
				}
			}

			@Override
			protected void moveCaret(MouseEvent e) {
				if (!e.isConsumed()) {
					super.moveCaret(e);
					/*try {
						int caretPos = getCaretPosition();

						// This is a hack to prevent BadLocationException without calling getText()
						boolean nextMayBeAvailable = !(UndoableTextField.this instanceof FormattedTextField) || ((FormattedTextField)UndoableTextField.this).textLen > caretPos;

						if(nextMayBeAvailable) {
							Rectangle curr = modelToView(caretPos);
							Rectangle next = modelToView(caretPos + 1);
							if (curr.y == next.y) {
								int middle = curr.x + (next.x - curr.x) / 2;
								if (e.getPoint().x > middle) {
									int selStart = getSelectionStart();
									int selEnd = getSelectionEnd();
									if (caretPos == selEnd) {
										setCaretPosition(caretPos + 1);
										setSelectionStart(selStart);
										setSelectionEnd(caretPos + 1);
								// FIXME: this kills the selection
								}else{
									setSelectionStart(caretPos+1);
									setSelectionEnd(selEnd);
									}
								}
							}
						}
					} catch (BadLocationException ex) {
						Dbg.notImportant(Dbg.MANIPULATING_TEXT_OUTSIDE, ex);
					}*/

					// Caret moved event
					caretMoved();
				}
			}

			@Override
			public void setSelectionVisible(boolean visible) {
				super.setSelectionVisible(true);
			}
		});
		getCaret().setBlinkRate(blinkRate);

		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent ke) {
				if (isEditable()) {
					if (ke.getKeyCode() == KeyEvent.VK_DOWN) {
						if (AutocompletePopupMenu.isShown()) {
							AutocompletePopupMenu.get().processKeyDown();
							ke.consume();
						}
					} else if (ke.getKeyCode() == KeyEvent.VK_UP) {
						if (AutocompletePopupMenu.isShown()) {
							AutocompletePopupMenu.get().processKeyUp();
							ke.consume();
						}
					} else if (((!moveKeysDisabled || AutocompletePopupMenu.isShownAndSelected()) && ke.getKeyCode() == KeyEvent.VK_ENTER) || ke.getKeyCode() == KeyEvent.VK_SPACE) {
						if (AutocompletePopupMenu.isShownAndSelected()) {
							AutocompletePopupMenu.get().processKeyEnter();
							killPopupTimer();
							ke.consume();
						}
					} else if (!moveKeysDisabled && ke.getKeyCode() == KeyEvent.VK_TAB) {
						if (getSelectedText() != null || ke.getModifiersEx() == KeyEvent.SHIFT_DOWN_MASK) {
							moveTabs(ke.getModifiersEx() == KeyEvent.SHIFT_DOWN_MASK);
							ke.consume();
						}
					} else if (AutocompletePopupMenu.isShown() && ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
						AutocompletePopupMenu.get().dispose();
						killPopupTimer();
						ke.consume();
					}

					// Caret moved event
					if (!moveKeysDisabled || (ke.getKeyCode() != KeyEvent.VK_UP && ke.getKeyCode() != KeyEvent.VK_DOWN)) {
						caretMoved();
					}
				}
			}

			@Override
			public void keyTyped(KeyEvent ke) {
				if (isEditable()) {
					if (ke == null || (moveKeysDisabled && (ke.getKeyChar() == '\n' || ke.getKeyChar() == '\t'))) {
						return;
					} else if (ke.getKeyChar() == '\n'){
						checkTabsOnEnter();
					}
					if (popupTimer != null) {
						popupTimer.restart();
					}else {
						popupTimer = new Timer(150, e -> {
                            popupTimer.stop();
                            popupTimer = null;
                            final AutocompletePopupMenu menu = getPopupMenu();
                            if (menu != null) {
                                Point p = UndoableTextField.this.getCaret().getMagicCaretPosition();
                                if (p == null) {
                                    p = new Point(2, 0);
                                } else {
                                    p = new Point(p);
                                    p.x -= popupCorrectionX;
                                }
                                SwingUtilities.convertPointToScreen(p, UndoableTextField.this);
                                menu.draw(p.x - 16, p.y + 16);
                            } else {
                                AutocompletePopupMenu.get().dispose();
                            }
                        });
						popupTimer.start();
					}
				}
			}
		});
	}

	/**
	 * Checks if some TAB characters should be added after ENTER has been pressed.
	 */
	private void checkTabsOnEnter() {
		int caretPos = getCaretPosition();
		String text = getText();

		// Wasn't the Enter key used to select some autocomplete value?
		if(text.charAt(caretPos - 1) == '\n') {
			StringBuilder sb = new StringBuilder(2);

			// Find the beginning of previous line and store all whitespace characters from it.
			getWhitespacesFromPreviousLine(caretPos, text, sb);

			// Will add another TAB if last char of previous line is opening bracket
			addTabAfterOpeningBracket(caretPos, text, sb);

			// String Builder contains corrections - let's apply them
			if (sb.length() != 0) {
				try {
					getDocument().insertString(caretPos, sb.toString(), null);
				} catch (BadLocationException e) {
					Dbg.notImportant("Inserting prefix from previous line failed", e);
				}
			}
		}
	}

	/**
	 * Will add another TAB if last char of previous line is opening bracket.
	 *
	 * @param caretPos caret position
	 * @param text whole text
	 * @param sb string builder to store corrections
	 */
	private void addTabAfterOpeningBracket(int caretPos, String text, StringBuilder sb) {
		if (caretPos >= 2) {
            char charAt = text.charAt(caretPos - 2);
            if (charAt == '\r' && caretPos >= 3) {
                charAt = text.charAt(caretPos - 3);
            }
            if (charAt == '(' || charAt == '[' || charAt == '{') {
                sb.append('\t');
            }
        }
	}

	/**
	 * Find the beginning of previous line and store all whitespace characters from it.
	 *
	 * @param caretPos caret position
	 * @param text whole text
	 * @param sb string builder to store corrections
 	 */
	private void getWhitespacesFromPreviousLine(int caretPos, String text, StringBuilder sb) {
		int lastIndexOf = text.lastIndexOf('\n', caretPos - 2);
		for (int i = lastIndexOf + 1; i < text.length(); i++) {
            char charAt = text.charAt(i);
            if (charAt == '\t' || charAt == ' ') {
                sb.append(charAt);
            } else {
                break;
            }
        }
	}

	void selectByDoubleClick() {
		int pos = getCaretPosition();
		String text = getText();
		String regex;
		String regex1 = "[^a-zA-Z0-9_\\s]+";    // Special chars
		String regex2 = "[a-zA-Z0-9_]";            // Common text

		if (text.substring(pos > 0 ? pos - 1 : 0, pos < text.length() ? pos + 1 : pos).matches(regex1)) {
			regex = regex1;
		} else {
			regex = regex2;
		}

		for (int i = 1; i < 100; i++) {
			if (pos - i >= 0) {
				String substring = text.substring(pos - i, pos - i + 1);
				if (!substring.matches(regex)) {
					setSelectionStart(pos - i + 1);
					break;
				}
			} else {
				setSelectionStart(pos - i + 1);
				break;
			}
		}

		for (int i = 0; i < 100; i++) {
			if (pos + i < text.length()) {
				String substring = text.substring(pos + i, pos + i + 1);
				if (!substring.matches(regex)) {
					setSelectionEnd(pos + i);
					break;
				}
			} else {
				setSelectionEnd(pos + i);
				break;
			}
		}
	}

	protected void caretMoved() {
	}

	private void killPopupTimer() {
		Schedule.inEDT(() -> {
            if (popupTimer != null) {
                popupTimer.stop();
            }
        });
	}

	void moveTabs(boolean moveDown) {
		int len = getText().length();
		try {
			for (int i = getSelectionStart() - 1; i > 0; i--) {
				if (getText(i, 1).equals("\n")) {
					setSelectionStart(i + 1);
					break;
				}else if(i == 1){
					setSelectionStart(getText(0, 1).equals("\n") ? 1 : 0);
				}
			}
			for (int i = getSelectionEnd() - 1; i <= len; i++) {
				if (getText(i, 1).equals("\n")) {
					setSelectionEnd(i);
					break;
				}
			}
		} catch (BadLocationException e) {
			Dbg.notImportant("i should never be negative", e);
		}

		String[] chunks = getSelectedText() == null ? new String[0] : getSelectedText().split("\n");
		StringBuilder newTextBuilder = new StringBuilder();
		for (String chunk : chunks) {
			String chunkSpaces = chunk.replaceAll("(\\s*).*", "$1");
			int tabs = 0;
			int spaces = 0;
			for (int j = 0; j < chunkSpaces.length(); j++) {
				if (chunkSpaces.charAt(j) == ' ') {
					spaces++;
					if (spaces >= Settings.getInt(Settings.L_FONT_TAB_SIZE)) {
						tabs++;
						spaces = 0;
					}
				} else if (chunkSpaces.charAt(j) == '\t') {
					tabs++;
					spaces = 0;
				}
			}
			if (moveDown) {
				if (spaces == 0 && tabs > 0) {
					tabs--;
				}
			} else {
				tabs++;
			}
			StringBuilder tabSet = new StringBuilder();
			for (int j = 0; j < tabs; j++) {
				tabSet.append("\t");
			}
			newTextBuilder.append(chunk.replaceFirst("\\s*(.*)", tabSet.toString() + "$1")).append("\n");
		}
		String newText = newTextBuilder.toString();

		int start = getSelectionStart();
		StyledDocument doc = getStyledDocument();
		try {
			doc.remove(start, getSelectionEnd() - start + 1);
			doc.insertString(start, newText, null);
		} catch (BadLocationException ex) {
			Dbg.notImportant("Text manipulation failed, retrying.", ex);
			try {
				doc.remove(start, getSelectionEnd() - start);
				doc.insertString(start, newText, null);
			} catch (BadLocationException e) {
				Dbg.notImportant(Dbg.MANIPULATING_TEXT_OUTSIDE, e);
			}
		}
		len = start + newText.length() - 1;
		setCaretPosition(len);
		setSelectionStart(start);
		setSelectionEnd(len);
		format();
	}

	public void setBordered(boolean bordered) {
		if (bordered) {
			if (!UIConstants.isLafRequiresBorderedTextFields()) {
				//Transfer the Painter to a TextPane key
				UIDefaults paneDefaults = new UIDefaults();
				paneDefaults.put("TextPane.borderPainter", UIManager.get("TextArea[Enabled+NotInScrollPane].borderPainter"));
				paneDefaults.put("TextPane.backgroundPainter", UIManager.get("TextArea[Enabled+NotInScrollPane].backgroundPainter"));

				putClientProperty("Nimbus.Overrides", paneDefaults);
				putClientProperty("Nimbus.Overrides.InheritDefaults", false);

				setMargin(new Insets(6, 6, 6, 6));
			} else {
				setBorder(new CompoundBorder(new LineBorder(UIConstants.Colors.getTableBorders(), 1), new EmptyBorder(3, 3, 3, 3)));
			}
		} else {
			setBorder(null);
		}
	}

	AutocompletePopupMenu getPopupMenu() {
		popupCorrectionX = 0;
		caretOld = getCaretPosition();
		if (caretOld < Settings.getInt(Settings.L_AUTOCOMPLETE_CHARS) || autocompleteOptionList.length == 0) {
			return null;
		} else {
			String word = "";
			try {
				for (int i = 1; i < 500 && i <= caretOld; i++) {
					// TODO: replace with char comparison
					String letter = getText(caretOld - i, 1);
					if (letter.matches("(?ims)[a-z0-9_.]")) {
						word = letter + word;
					} else {
						break;
					}
				}

				// handle NEW. and OLD.
				if (word.toLowerCase().startsWith("new.") || word.toLowerCase().startsWith("old.")) {
					word = word.substring(4);

					// handle table aliases
				} else if (word.contains(".")) {
					boolean found = false;
					int pos = word.indexOf('.');
					if (pos >= 2) {
						String part = word.substring(0, pos);
						for (String opt : autocompleteNameList) {
							if (part.equals(opt)) {
								found = true;
								break;
							}
						}
					}
					if (!found) {
						word = word.substring(pos + 1);
					}
				}
			} catch (BadLocationException e) {
				Dbg.notImportant("(caretOld - i) should never be negative", e);
			}
			if (word.length() < Settings.getInt(Settings.L_AUTOCOMPLETE_CHARS)) {
				return null;
			} else {
				List<IConnection> conns = Project.getCurrent().getConnections();
				boolean toUpper = !conns.isEmpty() && conns.get(0).isSupported(SupportedElement.ALL_UPPER);

				selectedWord = word.toLowerCase();
				popupCorrectionX = UIConstants.GRAPHICS.getFontMetrics(getFont()).stringWidth(word);

				AutocompletePopupMenu.get().clear();

				int added = 0;
				boolean lastIsSeparator = true;
				boolean toAddSeparator = false;
				boolean ignoreToNextSeparator = false;

				JMenuItem item = null;
				HashMap<String, Boolean> addedItems = new HashMap<>();

				for (String opt : autocompleteOptionList) {
					if (opt != null) {
						if (opt.toLowerCase().matches(selectedWord + "[^.]*") && addedItems.get(opt) == null && !ignoreToNextSeparator) {
							addedItems.put(opt, true);
							if (toAddSeparator) {
								AutocompletePopupMenu.get().addSeparator();
							}
							item = new JMenuItem(toUpper ? opt.toUpperCase() : opt);
							item.addActionListener(popupActionListener);
							AutocompletePopupMenu.get().add(item);
							added++;
							lastIsSeparator = false;
							toAddSeparator = false;
							if (added > Settings.getInt(Settings.L_AUTOCOMPLETE_ELEMS)) {
								ignoreToNextSeparator = true;
							}
						} else if (!lastIsSeparator && opt.equals("~|~")) {
							lastIsSeparator = true;
							toAddSeparator = true;
							ignoreToNextSeparator = false;
							added = 0;
						}
					}
				}
				if (item == null) {
					return null;
				} else {
					return AutocompletePopupMenu.get();
				}
			}
		}
	}

	public void setAutocomplete(GCFrameWithObservers window) {
		setAutocomplete(window, DBTree.instance.getElementNames(), UndoableTextField.KEYWORDS_SQL, DataTypes.getTypeNames());
	}

	public void setAutocomplete(GCFrameWithObservers window, IConnection conn) {
		setAutocomplete(window, DBTree.instance.getElementNames(), UndoableTextField.KEYWORDS_SQL, DataTypes.getTypeNames(conn));
	}

	public void setAutocomplete(GCFrameWithObservers window, String[] elementNames, String[] sqlKeywords, String[] datatypeNames) {

		// ADD SQL KEYWORDS TO SQL LIST
		if (sqlKeywords != null && sqlKeywords.length > 0) {
			autocompleteSQLList = sqlKeywords;
		} else {
			autocompleteSQLList = new String[0];
		}

		// ADD DATATYPES TO SQL LIST
		if (datatypeNames != null && datatypeNames.length > 0) {
			autocompleteSQLList = Geometry.concat(autocompleteSQLList, new String[]{"~|~"});
			autocompleteSQLList = Geometry.concat(autocompleteSQLList, datatypeNames);
		}

		updateAutocomplete(elementNames);
		if (window != null) {
			AutocompleteObserver.registerObserver(window, this);
		}
	}

	void updateAutocomplete(String[] elementNames) {
		// ADD ELEMENT NAMES TO OPTION LIST
		if (elementNames != null && elementNames.length > 0) {
			autocompleteNameList = elementNames;
		} else {
			autocompleteNameList = new String[0];
		}
		updateAutocomplete();
	}

	void updateAutocomplete() {

		// ADD ELEMENT NAMES TO OPTION LIST
		autocompleteOptionList = autocompleteNameList;

		// ADD SIMPLE REPEATED WORDS LIST TO OPTION LIST
		if (autocompleteSimpleList != null && autocompleteSimpleList.length > 0) {
			autocompleteOptionList = Geometry.concat(autocompleteOptionList, new String[]{"~|~"});
			autocompleteOptionList = Geometry.concat(autocompleteOptionList, autocompleteSimpleList);
		}

		// ADD SQL LIST TO OPTION LIST
		if (autocompleteSQLList != null && autocompleteSQLList.length > 0) {
			autocompleteOptionList = Geometry.concat(autocompleteOptionList, new String[]{"~|~"});
			autocompleteOptionList = Geometry.concat(autocompleteOptionList, autocompleteSQLList);
		}
	}

	protected void format() {
		// RESERVED FOR COLORIZE / FORMAT
	}

	void clearFinder() {
		finder = null;
	}

	public void disableFinder() {
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "None");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "None");
	}

	void clearUndo() {
		undoManager.clear();
		// TODO: may want destroy the undo manager completely, but there are still references left from document, event handlers, etc.
		/*getInputMap().clear();
		getDocument().removeUndoableEditListener(undoManager);
		undoManager.destroy();
		undoManager = null;*/
	}

	private static class WrapLabelView extends LabelView {
		WrapLabelView(Element elem) {
			super(elem);
		}

		@Override
		public float getMinimumSpan(int axis) {
			return axis == View.X_AXIS ? 0 : super.getMinimumSpan(axis);
		}
	}

	private class CustomTabParagraphView extends ParagraphView {
		private int tabSize = 0;

		CustomTabParagraphView(Element elem) {
			super(elem);
		}

		@Override
		public float nextTabStop(float x, int tabOffset) {
			if (tabSize == 0) {
				tabSize = UIConstants.GRAPHICS.getFontMetrics(getFont()).stringWidth("W") * Settings.getInt(Settings.L_FONT_TAB_SIZE);
			}
			return getTabSet() == null ? getTabBase() + (((int) x / tabSize + 1) * tabSize) : super.nextTabStop(x, tabOffset);
		}
	}

	private class PopupActionListener implements ActionListener, Serializable {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			try {
				String command = actionEvent.getActionCommand();
				String comTrim = command.endsWith(" ") ? command.substring(0, command.length()-1) : command;

				switch (Settings.getStr(Settings.L_AUTOCOMPLETE_SPACE)) {
					case "y":
						command = comTrim + " ";
						break;
					case "n":
						command = comTrim;
						break;
				}
				int newCaretPos = caretOld - selectedWord.length() + command.length();
				int presetPos = comTrim.indexOf("  ");
				if (presetPos >= 0) {
					newCaretPos -= command.length() - presetPos - 1;
				} else if (comTrim.endsWith("()") || comTrim.endsWith("[]")) {
					newCaretPos -= command.length() - comTrim.length() + 1;
				}
				StyledDocument doc = getStyledDocument();
				doc.remove(caretOld - selectedWord.length(), selectedWord.length());
				doc.insertString(caretOld - selectedWord.length(), command, null);
				setCaretPosition(newCaretPos);
				caretMoved();
				format();
			} catch (BadLocationException e) {
				Dbg.notImportant(Dbg.MANIPULATING_TEXT_OUTSIDE, e);
			}
			requestFocus();
			for (KeyListener listener : getKeyListeners()) {
				listener.keyTyped(null);
			}
			AutocompletePopupMenu.get().dispose();
		}
	}

	private class TabSizeEditorKit extends StyledEditorKit {

		private final boolean wrapLines;

		private TabSizeEditorKit(boolean wrapEnabled) {
			wrapLines = wrapEnabled;
		}

		@Override
		public ViewFactory getViewFactory() {
			return new ViewFactory() {
				@Override
				public View create(Element elem) {
					String kind = elem.getName();
					if (kind != null) {
						switch (kind) {
							case AbstractDocument.ContentElementName:
								return wrapLines ? new WrapLabelView(elem) : new LabelView(elem);
							case AbstractDocument.ParagraphElementName:
								return new CustomTabParagraphView(elem);
							case AbstractDocument.SectionElementName:
								return new BoxView(elem, View.Y_AXIS);
							case StyleConstants.ComponentElementName:
								return new ComponentView(elem);
							case StyleConstants.IconElementName:
								return new IconView(elem);
						}
					}
					return new LabelView(elem);
				}
			};
		}


	}
}
