package com.databazoo.components.table;

import javax.swing.*;

public class LineNumberScrollPane extends JScrollPane {
    public final LineNumberTableRowHeader header;

    public LineNumberScrollPane(JTable table) {
        super(table);
        header = new LineNumberTableRowHeader(this, table);
    }
}
