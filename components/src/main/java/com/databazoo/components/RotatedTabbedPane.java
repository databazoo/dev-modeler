package com.databazoo.components;

import javax.swing.*;
import java.awt.*;

import com.databazoo.components.icons.VerticalTextIcon;

/**
 * JTabbedPane with support for proper text rotation on various OS's
 */
public class RotatedTabbedPane extends JTabbedPane {

    public RotatedTabbedPane(int tabPlacement) {
        super(tabPlacement);
    }

    public RotatedTabbedPane(int tabPlacement, int tabLayoutPolicy) {
        super(tabPlacement, tabLayoutPolicy);
    }

    @Override public void addTab(String title, Icon icon, Component component, String tip) {
        throw new UnsupportedOperationException("Iconed tabs are not supported by RotatedTabbedPane");
    }

    @Override public void addTab(String title, Icon icon, Component component) {
        throw new UnsupportedOperationException("Iconed tabs are not supported by RotatedTabbedPane");
    }

    @Override public void addTab(String title, Component component) {
        if(!(UIConstants.isRetina() && UIConstants.isMac())) {
            switch (getTabPlacement()) {
            case JTabbedPane.RIGHT:
                super.addTab("", VerticalTextIcon.rotateClockwiseAlways(title), component);
                return;
            case JTabbedPane.LEFT:
                super.addTab("", VerticalTextIcon.rotateCounterClockwiseAlways(title), component);
                return;
            default: throw new IllegalArgumentException("Tab placement " + getTabPlacement() + " is not supported.");
            }
        }
        super.addTab(title, component);
    }
}
