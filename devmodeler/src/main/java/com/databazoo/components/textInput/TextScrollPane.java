package com.databazoo.components.textInput;

import com.databazoo.components.UIConstants;
import com.databazoo.devmodeler.config.Settings;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class TextScrollPane extends JScrollPane {

    public static TextScrollPane withLineNumbers(FormattedTextField input) {
        return new TextScrollPane(input, true);
    }

    public TextScrollPane(UndoableTextField input) {
        this(input, false);
    }

    private TextScrollPane(UndoableTextField input, boolean addLineNumHeader) {
        super(input);
		getVerticalScrollBar().setUnitIncrement(Settings.getInt(Settings.L_FONT_MONO_SIZE)*2);
        if (addLineNumHeader) {
            if (!(input instanceof FormattedTextField)) {
                throw new IllegalArgumentException("Input has to be of type FormattedTextField to be able to draw line numbers.");
            }
            setRowHeaderView(new LineNumberRowHeader((FormattedTextField) input, this));
        }
        if (UIConstants.isLafRequiresBorderedTextFields()) {
            setBorder(new CompoundBorder(new EmptyBorder(0, 2, 0, 2), new LineBorder(UIConstants.Colors.getTableBorders(), 1)));
        }
    }
}
