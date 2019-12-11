
package com.databazoo.components.table;

import com.databazoo.components.FontFactory;
import com.databazoo.components.UIConstants;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.Result;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Implementation of line numbers for a table.
 *
 * @author bobus
 */
public class LineNumberTableRowHeader extends JComponent {
	private final transient Image img = Theme.getSmallIcon(Theme.ICO_SAVE).getImage();
	private final Font headerFont = FontFactory.getMonospaced(Font.PLAIN, Settings.getInt(Settings.L_FONT_MONO_SIZE));
	private final FontMetrics fm = UIConstants.GRAPHICS.getFontMetrics(headerFont);
    private final JTable table;
    private final JScrollPane scrollPane;
	private int editingRow = -1;

	/**
	 * Constructor
	 *
	 * @param jScrollPane table's scroll pane
	 * @param jTable JTable
	 */
    LineNumberTableRowHeader(JScrollPane jScrollPane, final JTable jTable) {
		table = jTable;
        scrollPane = jScrollPane;
		scrollPane.setRowHeaderView(this);

		addListeners();

        setPreferredSize(new Dimension(56, 20));
    }

	private void addListeners() {
		this.table.getModel().addTableModelListener(tme -> LineNumberTableRowHeader.this.repaint());

		this.table.getSelectionModel().addListSelectionListener(lse -> LineNumberTableRowHeader.this.repaint());

		this.scrollPane.getVerticalScrollBar().addAdjustmentListener(ae -> LineNumberTableRowHeader.this.repaint());

		MouseAdapter ma = new LineNumberMouseHandler(table);
		addMouseListener(ma);
		addMouseMotionListener(ma);
	}

	/**
	 * Paint override
	 *
	 * @param g Graphics
	 */
	@Override
    protected void paintComponent(Graphics g) {
		Point viewPosition = scrollPane.getViewport().getViewPosition();
		Dimension viewSize = scrollPane.getViewport().getViewSize();
		if (getHeight() < viewSize.height) {
			Dimension size = getPreferredSize();
			size.height = viewSize.height;
			setSize(size);
			setPreferredSize(size);
		}

		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g.setFont(headerFont);

		Rectangle cellRect;
		Result result = null;
		if(table.getModel() instanceof Result) {
			result = (Result) table.getModel();
		}
		for(int r=0; r < table.getRowCount(); r++) {
			cellRect = table.getCellRect(r, 0, false);

			/*boolean rowSelected = table.isRowSelected(r);

			if (rowSelected) {
				g.setColor(table.getSelectionBackground());
				g.fillRect(0, cellRect.y, getWidth(), cellRect.height);
			}*/
			//if(!result.isNewLine(r)){
				g.setColor(UIConstants.Colors.getTableBorders());
				g.drawLine(0, cellRect.y-1, getWidth(), cellRect.y-1);
			//}

			if ((cellRect.y + cellRect.height) - viewPosition.y >= 0 && cellRect.y < viewPosition.y + viewSize.height) {
				if(editingRow == r){
					g.drawImage(img, getWidth()/2-8, cellRect.y, null);
				}else{
					//g.setColor(Config.gui.COLOR_TABLE_BORDERS);
					g.setColor(/*rowSelected ? table.getSelectionForeground() :*/ getForeground());
					String s = result != null && result.isNewLine(r) ? "#" : Integer.toString(r+1);
					g.drawString(s, (int)((getWidth()-fm.stringWidth(s))*0.5), cellRect.y+cellRect.height - fm.getDescent() - 1);
				}
			}
		}
		cellRect = table.getCellRect(table.getRowCount()-1, 0, false);
		g.setColor(UIConstants.Colors.getTableBorders());
		g.drawLine(0, cellRect.y+cellRect.height-1, getWidth(), cellRect.y+cellRect.height-1);
	}

	/**
	 * Draw icon instead of line number when the line is being edited.
	 *
	 * @param row row number
	 */
	public void setEditing(int row){
		editingRow = row;
		repaint();
	}

	private static class LineNumberMouseHandler extends MouseAdapter {
		private final JTable table;
		private int initialRow;
		private int initialRowClicked;

		LineNumberMouseHandler(JTable table) {
			this.table = table;
		}

		@Override
        public void mouseClicked(MouseEvent me) {
            int row = table.rowAtPoint(me.getPoint());
            table.setColumnSelectionAllowed(false);
            table.setCellSelectionEnabled(false);
            table.setRowSelectionAllowed(true);
            if(me.isControlDown()){
                if(me.isShiftDown()){
                    table.addRowSelectionInterval(Math.min(row, initialRowClicked), Math.max(row, initialRowClicked));
                }else{
                    table.addRowSelectionInterval(row, row);
                }
            }else{
                if(me.isShiftDown()){
                    table.setRowSelectionInterval(Math.min(row, initialRowClicked), Math.max(row, initialRowClicked));
                }else{
                    table.setRowSelectionInterval(row, row);
                }
            }
            initialRowClicked = row;
        }

		@Override
        public void mouseDragged(MouseEvent me) {
            int row = table.rowAtPoint(me.getPoint());
            table.setColumnSelectionAllowed(false);
            table.setRowSelectionAllowed(true);
            if(me.isControlDown()){
                table.addRowSelectionInterval(Math.min(row, initialRow), Math.max(row, initialRow));
            }else{
                table.setRowSelectionInterval(Math.min(row, initialRow), Math.max(row, initialRow));
            }
        }

		@Override public void mousePressed(MouseEvent me) { initialRow = table.rowAtPoint(me.getPoint()); }
	}
}
