package com.databazoo.components.icons;

import javax.swing.*;
import java.awt.*;

public class PlainColorIcon implements Icon {

    private final Color color;
    private final int width;
    private final int height;

    public PlainColorIcon(Color color) {
        this(color, 16, 16);
    }

    public PlainColorIcon(Color color, int width, int height) {
        this.color = color == null ? Color.WHITE : color;
        this.width = width;
        this.height = height;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Color old_color = g.getColor();
        g.setColor(color);
        g.fillRect(x, y, width, height);
        g.setColor(old_color);
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

}
