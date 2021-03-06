
package com.databazoo.components;

import javax.swing.*;
import java.awt.*;

/**
 * A separator panel for popup menu and data window.
 *
 * @author bobus
 */
public class Separator extends JPanel {

	private final int SIDE_SPACE = 4;
	private final boolean isLine;

	/**
	 * Constructor
	 */
	public Separator(){
		setLayout(null);
		setBackground(null);
		setPreferredSize(new Dimension(10,10));
		isLine = true;
	}

	/**
	 * Paint override
	 *
	 * @param g Graphics
	 */
	@Override
	public void paintComponent(Graphics g){
		if(isLine){
			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g.setColor(Color.GRAY);
			g.drawLine(SIDE_SPACE, getHeight()/2, getWidth()-SIDE_SPACE, getHeight()/2);
		}
	}
}
