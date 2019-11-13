package com.databazoo.components.table;

import javax.swing.*;
import java.awt.event.MouseListener;

public class LineNumberScrollPane extends JScrollPane {
    public final LineNumberTableRowHeader header;

    public LineNumberScrollPane(JTable table) {
        this(table, null);
    }

    public LineNumberScrollPane(JTable table, MouseListener listener) {
        super(table);
        header = new LineNumberTableRowHeader(this, table);
        if (listener != null) {
            header.addMouseListener(listener);
        }
    }
}
