
package com.databazoo.devmodeler.gui.view;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

import com.databazoo.components.FontFactory;
import com.databazoo.components.RotatedTabbedPane;
import com.databazoo.components.UIConstants;

/**
 * Base of all modules.
 *
 * @author bobus
 */
abstract class AbstractView extends JComponent {
	private static final Font FILTER_FONT = FontFactory.getSans(Font.BOLD + Font.ITALIC, 16);

	static final String L_ALL_DATABASES = "All databases";

	final RotatedTabbedPane outputTabs = new RotatedTabbedPane(JTabbedPane.LEFT);

	AbstractView() {
		setLayout(new BorderLayout(0, 0));
		setMinimumSize(new Dimension(0, 0));
		drawWindow();
		add(outputTabs);
		setColWidths();
	}

	protected abstract void drawWindow();

	protected abstract void setColWidths();

	public abstract void updateFilters();

	JPanel getTitledPanel(final String title) {
		return getTitledPanel(title, false);
	}

	JPanel getTitledPanel(final String title, final boolean filterIncludesServers) {
		return getTitledPanel(title, filterIncludesServers, new Point(2, 2));
	}

	JPanel getTitledPanel(final String title, final boolean filterIncludesServers, Point padding) {
		return new JPanel(new FlowLayout(FlowLayout.RIGHT, padding.x, padding.y)) {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D graphics = (Graphics2D) g;
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				graphics.setPaint(UIConstants.COLOR_BG_LIGHT);
				graphics.fillRect(0, 2, getWidth() - 3, getHeight() - 2);
				graphics.setPaint(UIConstants.COLOR_BG_DARK);
				graphics.drawRect(0, 2, getWidth() - 3, getHeight() - 2);

				if (filterIncludesServers) {
					graphics.fillRect(getWidth() - 389 - 1, 0, 389 - 3 + 2, 2);
					graphics.setPaint(UIConstants.COLOR_BG_LIGHT);
					//graphics.drawLine(getWidth()-389, 0, getWidth()-4, 0);
					graphics.fillRect(getWidth() - 389, 0, 389 - 3, 3);
				}

				graphics.setFont(FILTER_FONT);
				graphics.setPaint(Color.GRAY);
				graphics.drawString(title, 10, 22);
			}
		};
	}

	class IconCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JLabel comp = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value != null && value instanceof Icon) {
				comp.setIcon((Icon) value);
				comp.setText(null);
			} else {
				comp.setIcon(null);
				comp.setText((String) value);
			}
			return comp;
		}
	}
}
