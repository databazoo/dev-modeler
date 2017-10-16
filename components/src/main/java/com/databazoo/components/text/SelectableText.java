package com.databazoo.components.text;

import javax.swing.*;

/**
 * Display text that can be selected and copied.
 */
public class SelectableText extends JTextPane {
    public SelectableText(String text, boolean htmlMode) {
        if (htmlMode) {
            setContentType("text/html");
        }
        setText(text);
        setEditable(false);
        setBackground(null);
        setBorder(null);
        UIDefaults paneDefaults = new UIDefaults();
        paneDefaults.put("TextPane.borderPainter", null);
        paneDefaults.put("TextPane.backgroundPainter", null);
        putClientProperty("Nimbus.Overrides", paneDefaults);
        putClientProperty("Nimbus.Overrides.InheritDefaults", false);
    }
}
