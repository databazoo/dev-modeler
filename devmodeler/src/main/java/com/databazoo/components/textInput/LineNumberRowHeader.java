
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
     * @param input      text field
     * @param scrollPane scroll pane around text field
     */
    public LineNumberRowHeader(FormattedTextField input, JScrollPane scrollPane) {
        this.textField = input;
        this.scrollPane = scrollPane;
        setPreferredSize(new Dimension(32, 20));
    }

    /**
     * Paint override
     *
     * @param g Graphics
     */
    @Override
    protected void paintComponent(Graphics g) {
        // Check size
        Point viewPosition = scrollPane.getViewport().getViewPosition();
        Dimension viewSize = scrollPane.getViewport().getViewSize();
        if (getHeight() < viewSize.height) {
            Dimension size = getPreferredSize();
            size.height = viewSize.height;
            setSize(size);
            setPreferredSize(size);
        }
        int viewTopBorder = viewPosition.y;
        int viewBottomBorder = viewPosition.y + scrollPane.getHeight();

        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(UIConstants.Colors.getLabelBackground());
        g.fillRect(0, viewTopBorder, getWidth(), scrollPane.getHeight());

        // Configure graphics
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(UIConstants.Colors.getLabelForeground());
        g.setFont(headerFont);

		// Draw first line number
		try {
			drawLineNumber(g, 1, 0, viewTopBorder);
		} catch (BadLocationException e) {
			Dbg.notImportant("JTextComponent.modelToView() failed. How is that possible?", e);
		}

        // Draw line numbers for other lines
        int pos = 0;
        int i = 2;
        String text = textField.getText();
        try {
            while (true) {
                pos = text.indexOf('\n', pos) + 1;
                if (pos > 0) {
                    int top = drawLineNumber(g, i, pos, viewTopBorder);

                    // Make sure we're not beyond bottom border
                    if (viewBottomBorder < top) {
                        break;
                    }
                    i++;
                } else {
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
     * @param g             Graphics
     * @param line          line number
     * @param pos           position of the first char in the line
     * @param viewTopBorder height from which line numbers will be drawn (performance optimization)
     * @throws BadLocationException propagated exception from JTextComponent.modelToView()
     */
    private int drawLineNumber(Graphics g, int line, int pos, int viewTopBorder) throws BadLocationException {
        int top = (int) textField.modelToView2D(pos).getY() + headerFont.getSize();
        if (top >= viewTopBorder) {
            String lineStr = String.valueOf(line);
            g.drawString(lineStr, getWidth() - fm.stringWidth(lineStr) - 2, top);
        }

        return top;
    }
}
