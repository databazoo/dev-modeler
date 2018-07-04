package com.databazoo.components.table;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

import com.databazoo.components.FontFactory;
import com.databazoo.components.UIConstants;

/**
 * Renders percentage columns as progress bar values.
 */
public class PercentageCellRenderer extends JProgressBar implements TableCellRenderer {

    private static final int FONT_SIZE = 10;
    private static final Font HEADER_FONT = FontFactory.getMonospaced(Font.BOLD, FONT_SIZE);
    private static final FontMetrics METRICS = UIConstants.GRAPHICS.getFontMetrics(HEADER_FONT);

    private String textValue = "";
    private int textWidth = 0;

    @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setValue(value instanceof Number ? ((Number)value).intValue() : 0);
        updateTextValue();
        return this;
    }

    private void updateTextValue() {
        textValue = getValue() + "%";
        textWidth = METRICS.stringWidth(textValue);
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.setColor(Color.BLACK);
        graphics.setFont(HEADER_FONT);
        graphics.drawString(textValue, getWidth()/2 - textWidth/2, getHeight()/2 + FONT_SIZE/2 - 1);
    }
}
