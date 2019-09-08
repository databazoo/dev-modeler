
package com.databazoo.components.textInput;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import com.databazoo.components.GCFrame;
import com.databazoo.components.UIConstants;
import com.databazoo.components.containers.HorizontalContainer;
import com.databazoo.components.text.SelectableText;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;


/**
 * Search implementation (window / inline) for text fields.
 *
 * @author bobus
 */
class TextFieldFinder extends JFrame {
	private static final ImageIcon ico = Theme.getSmallIcon(Theme.ICO_SEARCH);
	private static final int MIN_HEIGHT = 60;
	private static final int MIN_WIDTH = 400;

	/**
	 * Open the search window.
	 *
	 * @param inputComponent text field
	 * @return TextFieldFinder
	 */
	static TextFieldFinder getFinder(UndoableTextField inputComponent) {
		Container parent = inputComponent.getParent();
		if (parent != null &&
				parent instanceof JViewport &&
				parent.getParent().getHeight() >= MIN_HEIGHT &&
				parent.getParent().getWidth() >= MIN_WIDTH
				) {
			return new TextFieldFinder(inputComponent, true);
		} else {
			return new TextFieldFinder(inputComponent, false);
		}
	}

	private final UndoableTextField inputComponent;

	private JButton buttonReplace;
	private JButton buttonReplaceAll;
	private JCheckBox checkboxCase;
	private UndoableTextField inputSearch;
	private UndoableTextField inputReplace;

	private JPanel contentPane;
	private final boolean inline;

	/**
	 * Constructor
	 *
	 * @param inputComponent text field
	 * @param inline         show inline?
	 */
	private TextFieldFinder(UndoableTextField inputComponent, boolean inline) {
		super("Find");
		this.inputComponent = inputComponent;
		this.inline = inline;
		if (inline) {
			drawInline();
		} else {
			drawStandalone();
		}
	}

	/**
	 * Prepare the window
	 */
	private void drawStandalone() {
		setAlwaysOnTop(true);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setIconImage(ico.getImage());

		getContentPane().add(contentPane = new HorizontalContainer(null, prepareFindPanel(), prepareButtonPanel()));

		pack();
		setSize(inputComponent.getWidth() < MIN_WIDTH ? MIN_WIDTH : inputComponent.getWidth(), 55);

		Point loc = inputComponent.getLocationOnScreen();
		if (inputComponent.getParent() instanceof JViewport) {
			JViewport par = (JViewport) inputComponent.getParent();
			loc.x += par.getViewPosition().x;
			loc.y += par.getViewPosition().y;
		}
		loc.y -= getHeight() + 1;
		setLocation(loc);
		setVisible(GCFrame.SHOW_GUI);
		GCFrame.addChildWindowByComponent(inputComponent, this);
	}

	/**
	 * Prepare the inline component
	 */
	private void drawInline() {
		final JScrollPane scrollPane = (JScrollPane) inputComponent.getParent().getParent();

		contentPane = new HorizontalContainer(null, prepareFindPanel(), prepareButtonPanel());

		JButton closeButton = new JButton(Theme.getSmallIcon(Theme.ICO_CANCEL));
		closeButton.setPreferredSize(new Dimension(24, 24));
		closeButton.setSize(new Dimension(24, 24));
		closeButton.addActionListener(e -> scrollPane.setColumnHeaderView(null));
		JPanel closePane = new JPanel(new GridBagLayout());
		closePane.add(closeButton);

		scrollPane.setColumnHeaderView(contentPane);
		scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, closePane);
		focus();
	}

	/**
	 * Prepare the button panel
	 *
	 * @return JPanel
	 */
	private JPanel prepareButtonPanel() {
		JPanel buttonPane = new JPanel(new GridLayout(0, 3));

		JButton buttonPrev = new JButton("Previous", ico);
		buttonPrev.setEnabled(true);
		buttonPrev.addActionListener(ae -> {
            search(false);
            inputComponent.requestFocus();
        });
		buttonPane.add(buttonPrev);

		JButton buttonNext = new JButton("Next", ico);
		buttonNext.setEnabled(true);
		buttonNext.addActionListener(ae -> {
            search(true);
            inputComponent.requestFocus();
        });
		buttonPane.add(buttonNext);

		checkboxCase = new JCheckBox("Match case?", false);
		buttonPane.add(checkboxCase);

		buttonReplace = new JButton("Replace");
		buttonReplace.setEnabled(false);
		buttonReplace.addActionListener(e -> replace());
		buttonPane.add(buttonReplace);

		buttonReplaceAll = new JButton("Replace all");
		buttonReplaceAll.setEnabled(false);
		buttonReplaceAll.addActionListener(e -> replaceAll());
		buttonPane.add(buttonReplaceAll);

		//checkboxRegexp = new JCheckBox("Regexp?", false);
		//checkboxRegexp.setEnabled(false);
		//buttonPane.add(checkboxRegexp);

		return buttonPane;
	}

	/**
	 * Pack search and replace panels
	 *
	 * @return JPanel
	 */
	private JPanel prepareFindPanel() {
		JPanel findPane = new JPanel(new GridLayout(0, 1));
		findPane.add(prepareSearchInput());
		findPane.add(prepareReplaceInput());
		return findPane;
	}

	/**
	 * Prepare search input text field
	 *
	 * @return UndoableTextField
	 */
	private UndoableTextField prepareSearchInput() {
		inputSearch = new UndoableTextField(inputComponent.getSelectedText());
		inputSearch.setBordered(true);
		inputSearch.disableFinder();
		inputSearch.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent ke) {
				Schedule.reInvokeInEDT(Schedule.Named.TEXT_FIELD_KEY_LISTENER, UIConstants.TYPE_TIMEOUT, () -> search(true));
			}

			@Override
			public void keyPressed(KeyEvent ke) {
				if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
					ke.consume();
				}
			}
		});
		return inputSearch;
	}

	/**
	 * Prepare replace input text field
	 *
	 * @return UndoableTextField
	 */
	private UndoableTextField prepareReplaceInput() {
		inputReplace = new UndoableTextField();
		inputReplace.setBordered(true);
		inputReplace.disableFinder();
		return inputReplace;
	}

	/**
	 * Close search
	 */
	@Override
	public void dispose() {
		inputComponent.clearFinder();
		super.dispose();
	}

	/**
	 * Activate and deactivate buttons according to search result
	 */
	private void checkReplaceButtons() {
		String searchText = inputSearch.getText();
		if (!searchText.isEmpty() && !inputSearch.getForeground().equals(UIConstants.Colors.RED)) {
			buttonReplace.setEnabled(true);
			buttonReplaceAll.setEnabled(true);
		} else {
			buttonReplace.setEnabled(false);
			buttonReplaceAll.setEnabled(false);
		}
	}

	/**
	 * Run search
	 *
	 * @param isForward search forward?
	 */
	void search(boolean isForward) {
		inputSearch.setForeground(Color.BLACK);
		int cursorPos = inputComponent.getCaretPosition();

		String text = inputComponent.getText().replace("\r\n", "\n");
		String searchText = inputSearch.getText();

		if (searchText.isEmpty()) {
			checkReplaceButtons();
			return;
		}

		if (!checkboxCase.isSelected()) {
			text = text.toLowerCase();
			searchText = searchText.toLowerCase();
		}

		if (isForward) {
			int loc = text.indexOf(searchText, cursorPos);
			if (loc >= 0) {
				inputComponent.setSelectionStart(loc);
				inputComponent.setSelectionEnd(loc + searchText.length());
			} else {
				loc = text.indexOf(searchText, 0);
				if (loc >= 0) {
					inputComponent.setSelectionStart(loc);
					inputComponent.setSelectionEnd(loc + searchText.length());
				} else {
					inputSearch.setForeground(UIConstants.Colors.RED);
				}
			}
		} else {
			int loc = text.lastIndexOf(searchText, cursorPos - searchText.length() - 1);
			if (loc >= 0) {
				inputComponent.setSelectionStart(loc);
				inputComponent.setSelectionEnd(loc + searchText.length());
			} else {
				loc = text.lastIndexOf(searchText, text.length());
				if (loc >= 0) {
					inputComponent.setSelectionStart(loc);
					inputComponent.setSelectionEnd(loc + searchText.length());
				} else {
					inputSearch.setForeground(UIConstants.Colors.RED);
				}
			}
		}
		checkReplaceButtons();
	}

	/**
	 * Run replace
	 */
	void replace() {
		int selStart = inputComponent.getSelectionStart();
		String selText = inputComponent.getSelectedText();
		if (selText == null || selText.isEmpty()) {
			search(true);
		} else {
			try {
				Document doc = inputComponent.getDocument();
				doc.remove(selStart, selText.length());
				doc.insertString(selStart, inputReplace.getText(), null);
				inputComponent.format();
				search(true);
			} catch (BadLocationException ex) {
				Dbg.fixme("Find-replace failed", ex);
			}
		}
	}

	/**
	 * Run replace all
	 */
	void replaceAll() {
		int cursorPos = 0;
		int replacements = 0;

		String text = inputComponent.getText().replace("\r\n", "\n");
		String searchText = inputSearch.getText();
		String replacementText = inputReplace.getText();

		if (!checkboxCase.isSelected()) {
			text = text.toLowerCase();
			searchText = searchText.toLowerCase();
		}

		int corr = searchText.length() - replacementText.length();

		while (true) {
			int loc = text.indexOf(searchText, cursorPos);
			if (loc >= 0) {
				try {
					Document doc = inputComponent.getDocument();
					doc.remove(loc - (replacements * corr), searchText.length());
					doc.insertString(loc - (replacements * corr), replacementText, null);
					cursorPos = loc + replacementText.length() + 1;
				} catch (BadLocationException ex) {
					Dbg.fixme("Find-replace failed", ex);
					break;
				}
				replacements++;
			} else {
				break;
			}
		}
		inputComponent.format();
		search(true);
		if (inputComponent.getParent() != null && inputComponent.getParent().getParent() != null && inputComponent.getParent().getParent().getParent() != null) {
			JOptionPane.showMessageDialog(inputComponent, new SelectableText(replacements + " replacements done", false), replacements + " replacements done", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/**
	 * Move focus to search text field
	 */
	protected void focus() {
		if (inline) {
			JScrollPane scrollPane = (JScrollPane) inputComponent.getParent().getParent();
			scrollPane.setColumnHeaderView(contentPane);
		}
		inputSearch.setSelectionStart(0);
		inputSearch.setSelectionEnd(inputSearch.getText().length());
		inputSearch.requestFocus();
	}

	/**
	 * Update search field text to currently selected value in the input component.
	 */
	void updateTextToSelection() {
		inputSearch.setText(inputComponent.getSelectedText());
	}
}
