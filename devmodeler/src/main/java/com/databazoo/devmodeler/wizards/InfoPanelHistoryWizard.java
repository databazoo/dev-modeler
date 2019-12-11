package com.databazoo.devmodeler.wizards;

import com.databazoo.components.UIConstants;
import com.databazoo.components.textInput.FormattedTextField;
import com.databazoo.components.textInput.UndoableTextField;
import com.databazoo.devmodeler.gui.HistorizingInfoPanel;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.text.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Info Panel History Wizard
 * @author bobus
 */
public class InfoPanelHistoryWizard extends MigWizard {
	public static final String L_NOTIFICATION_LOG = "Notification log";

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("\nHH:mm:ss ");
	private static final int REFRESH = 21;
	private static final int OUTPUT_LIMIT = 10000;
	private final List<HistorizingInfoPanel.InfoLine> lines;

	public static InfoPanelHistoryWizard getInstance(List<HistorizingInfoPanel.InfoLine> lines) {
		return new InfoPanelHistoryWizard(lines);
	}

	private InfoPanelHistoryWizard(List<HistorizingInfoPanel.InfoLine> lines) {
		super(1, 0);
		drawWindow("Notification log", null, false, true);
		setTreeVisible(false);

		this.lines = lines;
	}

	public void drawHistoryPage() {
		resetContent();
		addTitle(L_NOTIFICATION_LOG);

		UndoableTextField aboutText = new FormattedTextField();
		aboutText.disableFinder();
		aboutText.setEditable(false);
		aboutText.setBordered(true);

		addLines(aboutText, lines);

		addPanel(new JScrollPane(aboutText), "width 100%-6px!, span");

		setNextButton("Refresh", true, REFRESH);
	}

	private void addLines(JTextPane aboutText, List<HistorizingInfoPanel.InfoLine> lines){
		StyledDocument doc = aboutText.getStyledDocument();
		addStylesToDocument(doc);

		for (int i = lines.size() <= OUTPUT_LIMIT ? 0 : lines.size() - OUTPUT_LIMIT; i < lines.size(); i++) {
			HistorizingInfoPanel.InfoLine line = lines.get(i);
			try {
				doc.insertString(doc.getLength(), line.getCreated().format(TIME_FORMATTER), doc.getStyle("regular"));
				Style style = doc.getStyle(styleFromColor(line.getColor()));
				String prefix = "";
				for (String messageLine : line.getMessageLines()) {
					doc.insertString(doc.getLength(), prefix + messageLine, style);
					prefix = "\n         ";
				}

			} catch (BadLocationException e) {
				Dbg.notImportant(Dbg.MANIPULATING_TEXT_OUTSIDE, e);
			}
		}
	}

	private String styleFromColor(Color color) {
		if (color.equals(UIConstants.Colors.GRAY)) {
			return "gray";
		} else if (color.equals(UIConstants.Colors.GREEN)) {
			return "green";
		} else if (color.equals(UIConstants.Colors.RED)) {
			return "red";
		} else if (color.equals(UIConstants.Colors.BLUE)) {
			return "blue";
		}
		return "regular";
	}

	private void addStylesToDocument(StyledDocument doc){
		Style s, regular;
		Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

		regular = doc.addStyle("regular", def);
		StyleConstants.setFontFamily(regular, Font.DIALOG);
		StyleConstants.setFontSize(regular, 11);
		StyleConstants.setForeground(regular, Color.BLACK);
		StyleConstants.setItalic(regular, true);

		s = doc.addStyle("gray", regular);
		StyleConstants.setForeground(s, UIConstants.Colors.GRAY);

		s = doc.addStyle("green", regular);
		StyleConstants.setForeground(s, UIConstants.Colors.GREEN);

		s = doc.addStyle("red", regular);
		StyleConstants.setForeground(s, UIConstants.Colors.RED);

		s = doc.addStyle("blue", regular);
		StyleConstants.setForeground(s, UIConstants.Colors.BLUE);
	}

	@Override
	public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
		Schedule.inEDT(this::drawHistoryPage);
	}

	protected void executeAction(int type){
		if(type == REFRESH){
			Schedule.inEDT(this::drawHistoryPage);
		} else {
			super.executeAction(type);
		}
	}

	@Override
	public void notifyChange(String elementName, String value) {

	}

	@Override
	public void notifyChange(String elementName, boolean value) {

	}

	@Override
	public void notifyChange(String elementName, boolean[] values) {

	}
}
