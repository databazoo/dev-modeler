
package com.databazoo.components.icons;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

import com.databazoo.components.UIConstants;

/**
 * Icon that displays given text rotated by 90 degrees.
 *
 * @author bobus
 */
public class VerticalTextIcon implements Icon, SwingConstants {

    private final Font font = UIManager.getFont("Label.font");
    private final FontMetrics fm = UIConstants.GRAPHICS.getFontMetrics(font);

    private final String text;
    private final int width, height;
    private final boolean clockwise;
    private final boolean rotate;

    /**
     * Get a vertical text. Rotate clockwise.
     *
     * @param text given text
     * @return icon with text on it
     */
    public static Icon rotateClockwiseAlways(String text){
        return new VerticalTextIcon(text, true, true);
    }

    /**
     * Get a vertical text. Rotate counter-clockwise.
     *
     * @param text given text
     * @return icon with text on it
     */
    public static Icon rotateCounterClockwiseAlways(String text){
        return new VerticalTextIcon(text, false, true);
    }

    /**
     * Get a vertical text. Rotate clockwise.
     *
     * @param text given text
     * @return icon with text on it
     */
    public static Icon rotateClockwiseIfNotNative(String text){
        return new VerticalTextIcon(text, true, !(UIConstants.isRetina() && UIConstants.isMac()));
    }

    /**
     * Get a vertical text. Rotate counter-clockwise.
     *
     * @param text given text
     * @return icon with text on it
     */
    public static Icon rotateCounterClockwiseIfNotNative(String text){
        return new VerticalTextIcon(text, false, !(UIConstants.isRetina() && UIConstants.isMac()));
    }

	/**
	 * Constructor
	 *
	 * @param text given text
	 * @param clockwise rotate clockwise?
	 * @param rotate makes it possible to prevent rotation on natively rotating LAFs
	 */
    private VerticalTextIcon(String text, boolean clockwise, boolean rotate){
        this.text = text;
        this.rotate = rotate;
        this.clockwise = clockwise;
        width = fm.stringWidth(text)+4;
        height = fm.getHeight()-8;
    }

	/**
	 * Paint overridden
	 *
	 * @param c Component
	 * @param g Graphics
	 * @param x X
	 * @param y Y
	 */
	@Override
    public void paintIcon(Component c, Graphics g, int x, int y){
        Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(Color.BLACK);
        if(rotate) {
            AffineTransform oldTransform = g2.getTransform();
            if (clockwise) {
                g2.translate(x + getIconWidth(), y);
                g2.rotate(Math.PI / 2);
            } else {
                g2.translate(x, y + getIconHeight());
                g2.rotate(-Math.PI / 2);
            }
            g.drawString(text, 2, fm.getLeading() + fm.getAscent() - 4);
            g2.setTransform(oldTransform);
        } else {
            g.drawString(text, 2, fm.getLeading() + fm.getAscent() - 4);
        }
    }

	/**
	 * Swap width and height.
	 *
	 * @return height instead of width
	 */
	@Override
    public int getIconWidth(){
        return rotate ? height : width;
    }


	/**
	 * Swap width and height.
	 *
	 * @return width instead of height
	 */
	@Override
    public int getIconHeight(){
        return rotate ? width : height;
    }
}
