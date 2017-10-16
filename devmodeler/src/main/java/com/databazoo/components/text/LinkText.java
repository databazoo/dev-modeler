package com.databazoo.components.text;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;

import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.gui.DesignGUI;

/**
 * Clickable URL
 */
public class LinkText extends JLabel {

    public LinkText(String url) {
        super("<html><a href=" + url + ">" + url + "</a></html>");
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                DesignGUI.get().openURL("http://" + Config.APP_DEFAULT_URL);
            }
        });
    }
}
