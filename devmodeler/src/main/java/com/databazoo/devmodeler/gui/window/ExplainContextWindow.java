package com.databazoo.devmodeler.gui.window;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import com.databazoo.devmodeler.model.explain.ExplainOperation;

/**
 * A tiny window displaying info on selected operation.
 *
 * @author bobus
 */
public class ExplainContextWindow extends JWindow {
    private static ExplainContextWindow instance;

    public static ExplainContextWindow get() {
        if (instance == null) {
            instance = new ExplainContextWindow();
        }
        return instance;
    }

    private ExplainContextWindow() {
        super();
        setVisible(false);
        setAlwaysOnTop(true);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
            }
        });
    }

    @Override
    public void dispose() {
        instance = null;
        setVisible(false);
        super.dispose();
    }

    public void draw(final Point loc) {
        if (!isVisible()) {
            getContentPane().setLayout(new GridLayout(0, 1));
            getContentPane().setBackground(Color.WHITE);
            ((JComponent) getContentPane()).setBorder(new CompoundBorder(new LineBorder(Color.GRAY), new EmptyBorder(10, 10, 10, 10)));
            setVisible(true);
        }
        pack();
        setLocation(loc.x - getWidth() / 2 + ExplainOperation.DEFAULT_WIDTH / 2, loc.y - getHeight());
    }

    public void clear() {
        getContentPane().removeAll();
    }

    public void text(String value) {
        getContentPane().add(new JLabel("<html>" + value + "</html>"));
    }
}
