package com.databazoo.components.elements;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.tools.Geometry;

/**
 * Provides mouse fixation for relative drag events.
 */
public class DraggableComponentMouseListener extends MouseAdapter {
    private Point anchorPoint;

    int getAnchorX(){ return anchorPoint.x; }
    int getAnchorY(){ return anchorPoint.y; }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point newPos = new Point(
                Canvas.instance.getScrollPosition().x - e.getPoint().x + anchorPoint.x,
                Canvas.instance.getScrollPosition().y - e.getPoint().y + anchorPoint.y);
        Geometry.fitPointToLimits(newPos, new Point(0, 0), Canvas.instance.getScrollMaxSize());
        Canvas.instance.scrollTo(newPos);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        anchorPoint = e.getPoint();
    }
}
