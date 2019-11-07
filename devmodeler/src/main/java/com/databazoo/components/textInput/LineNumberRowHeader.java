
package com.databazoo.components.textInput;

import com.databazoo.components.FontFactory;
import com.databazoo.components.UIConstants;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.tools.Dbg;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;

/**
 * A component to display line numbers.
 *
 * @author bobus
 */
public class LineNumberRowHeader extends JComponent {
	private final FormattedTextField textField;
	private final JScrollPane scrollPane;
	private final Font headerFont = FontFactory.getMonospaced(Font.PLAIN, Settings.getInt(Settings.L_FONT_MONO_SIZE));
	private final FontMetrics fm = UIConstants.GRAPHICS.getFontMetrics(headerFont);

	/**
	 * Constructor
	 *
	 * @param input text field
	 * @param scrollPane scroll pane around text field
	 */
	public LineNumberRowHeader(FormattedTextField input, JScrollPane scrollPane){
		this.textField = input;
		this.scrollPane = scrollPane;
		setPreferredSize(new Dimension(28,20));
	}

	/**
	 * Paint override
	 *
	 * @param g Graphics
	 */
	@Override
    protected void paintComponent(Graphics g){
		// Check size
		Dimension viewSize = scrollPane.getViewport().getViewSize();
		if (getHeight() < viewSize.height) {
			Dimension size = getPreferredSize();
			size.height = viewSize.height;
			setSize(size);
			setPreferredSize(size);
		}

		// Configure graphics
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.BLACK);
		g.setFont(headerFont);

		// Draw first line number
		try {
			drawLineNumber(g, 1, 0);
		} catch (BadLocationException e) {
			Dbg.notImportant("JTextComponent.modelToView() failed. How is that possible?", e);
		}

		// Draw line numbers for other lines
		int pos = 0;
		int i=2;
		String text = textField.getText();
		try {
			while (true) {
				pos = text.indexOf('\n', pos)+1;
				if(pos > 0){
					drawLineNumber(g, i, pos);
					i++;
				}else{
					break;
				}
			}
		} catch (BadLocationException ex) {
			Dbg.fixme("Reached last line? This should not happen.", ex);
		}
	}

	/**
	 * Position the line number
	 *
	 * @param g Graphics
	 * @param line line number
	 * @param pos position of the first char in the line
	 * @throws BadLocationException propagated exception from JTextComponent.modelToView()
	 */
	private void drawLineNumber(Graphics g, int line, int pos) throws BadLocationException {
		String lineStr = String.valueOf(line);

		int top = (int) textField.modelToView2D(pos).getY() + headerFont.getSize();
		int w = fm.stringWidth(lineStr);
		g.drawString(lineStr, getWidth()-w-2, top);
	}
}
