
package com.databazoo.components.elements;

import com.databazoo.components.UIConstants;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.tools.Geometry;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Line-type components.
 *
 * @author bobus
 */
public abstract class LineComponent extends EnvironmentComponent {
	public static final int CLICK_TOLERANCE = 15;
	public static final int LEFT_TOP_RIGHT_BOTTOM = 1;
	public static final int LEFT_BOTTOM_RIGHT_TOP = 2;
	public static final int RIGHT_TOP_LEFT_BOTTOM = 3;
	public static final int RIGHT_BOTTOM_LEFT_TOP = 4;

	public static final int TWO_PLUS_HALF_TOLERANCE = 2 + CLICK_TOLERANCE / 2;
    public static final int ONE_PLUS_HALF_TOLERANCE = 1 + CLICK_TOLERANCE / 2;

	protected Color lineColor = UIConstants.Colors.getLabelForeground();
	protected DraggableComponent rel1, rel2;
	protected int direction = LEFT_TOP_RIGHT_BOTTOM;
	protected boolean isFlipped = false;
	protected boolean isDashed = true;
	protected boolean isSelected = false;
	protected int lineWidth = 2;

	public transient BufferedImage arrow1;
	public transient BufferedImage arrow2;
	public Point arrow1Location;
	public Point arrow2Location;
	public boolean respectsZoom = true;

	/**
	 * Set one of the objects to connect
	 *
	 * @param rel Draggable Component
	 */
	public void setRel1(DraggableComponent rel){
		rel1 = rel;
	}

	/**
	 * Set one of the objects to connect
	 *
	 * @param rel Draggable Component
	 */
	public void setRel2(DraggableComponent rel){
		rel2 = rel;
	}

	/**
	 * Get one of the connected objects
	 */
	public DraggableComponent getRel1(){
		return rel1;
	}

	/**
	 * Get one of the connected objects
	 */
	public DraggableComponent getRel2(){
		return rel2;
	}

	/**
	 * Check if both objects are available
	 */
	public boolean isReady(){
		return rel1 != null && rel2 != null;
	}

	/**
	 * Direction identified by a constant
	 *
	 * @return LEFT_TOP_RIGHT_BOTTOM | LEFT_BOTTOM_RIGHT_TOP | RIGHT_TOP_LEFT_BOTTOM | RIGHT_BOTTOM_LEFT_TOP
	 */
	public int getDirection(){
		return direction;
	}

	public boolean isDashed() {
		return isDashed;
	}

	/**
	 * Update size, location, direction, etc. on object move
	 */
	public void checkSize(){
		if(rel1 != null && rel2 != null){
			Point loc1 = rel1.getAbsCenter();
			Point loc2 = rel2.getAbsCenter();
			if(loc1 != null && loc2 != null){
				if(respectsZoom){
					loc1 = Geometry.getZoomed(loc1);
					loc2 = Geometry.getZoomed(loc2);
				}
				loc1.x -= 3;
				loc1.y -= 3;
				loc2.x -= 3;
				loc2.y -= 3;

				// if difference is low we can ignore it and draw a straight line
				int diff = loc1.y-loc2.y;
				if(diff >= -10 && diff <= 19){
					loc1.y -= diff/2;
					loc2.y = loc1.y;
				}

				if (loc1.x < loc2.x) {
					if (loc1.y < loc2.y) {
						// Left top - right bottom
						setSize(loc2.x - loc1.x +2+CLICK_TOLERANCE, loc2.y - loc1.y +2+CLICK_TOLERANCE);
						setLocation(loc1.x -1-CLICK_TOLERANCE/2, loc1.y -1-CLICK_TOLERANCE/2);
						isFlipped = false;
						direction = LEFT_TOP_RIGHT_BOTTOM;
					} else {
						// Left bottom - right top
						setSize(loc2.x - loc1.x +2+CLICK_TOLERANCE, loc1.y - loc2.y +2+CLICK_TOLERANCE);
						setLocation(loc1.x -1-CLICK_TOLERANCE/2, loc2.y -1-CLICK_TOLERANCE/2);
						isFlipped = true;
						direction = LEFT_BOTTOM_RIGHT_TOP;
					}
				} else {
					if (loc1.y < loc2.y) {
						// Right top - left bottom
						setSize(loc1.x - loc2.x +2+CLICK_TOLERANCE, loc2.y - loc1.y +2+CLICK_TOLERANCE);
						setLocation(loc2.x -1-CLICK_TOLERANCE/2, loc1.y -1-CLICK_TOLERANCE/2);
						isFlipped = true;
						direction = RIGHT_TOP_LEFT_BOTTOM;
					} else {
						// Right bottom - left top
						setSize(loc1.x - loc2.x +2+CLICK_TOLERANCE, loc1.y - loc2.y +2+CLICK_TOLERANCE);
						setLocation(loc2.x -1-CLICK_TOLERANCE/2, loc2.y -1-CLICK_TOLERANCE/2);
						isFlipped = false;
						direction = RIGHT_BOTTOM_LEFT_TOP;
					}
				}
			}
			if (Settings.getBool(Settings.L_PERFORM_CARDINALITY) && Canvas.getZoomNotTooSmall()){
				setArrowPosition();
			} else {
				arrow1Location = null;
				arrow2Location = null;
			}
		}
	}

	/**
	 * Update location and direction of cardinality and inheritance symbols (crows foot, arrow, etc.)
	 */
	protected void setArrowPosition(){
		// OVERRIDE IN CONSTRAINT FOR CARDINALITY / INHERITANCE SYMBOLS
	}

	/**
	 * Paint overridden
	 *
	 * @param g graphics reference
	 */
	@Override
	protected void paintComponent(Graphics g) {
		if(Geometry.shapeIntersectsDiagonal(isFlipped, getSize(), g.getClipBounds())) {
			Graphics2D graphics = (Graphics2D) g;
			if(Settings.getBool(Settings.L_PERFORM_ANTIALIASING)) {
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			}

			if (isSelected) {
				graphics.setPaint(Canvas.SELECTION_COLOR_A2);
				graphics.setStroke(Canvas.getLineStrokeFull(4));
				drawLine(graphics);
				graphics.setStroke(Canvas.getLineStrokeFull(2));
				drawLine(graphics);
			}

			graphics.setStroke(isDashed ? Canvas.getLineStrokeDashed() : Canvas.getLineStrokeFull(lineWidth));
			graphics.setPaint(lineColor);
			drawLine(graphics);

			if (arrow1Location != null) {
				graphics.drawImage(arrow1, arrow1Location.x, arrow1Location.y, null);
			}
			if (arrow2Location != null) {
				graphics.drawImage(arrow2, arrow2Location.x, arrow2Location.y, null);
			}
		}
	}

	/**
	 * Paint a straight line
	 *
	 * @param graphics graphics reference
	 */
    private void drawLine(Graphics2D graphics) {
		if (isFlipped) {
            graphics.drawLine(ONE_PLUS_HALF_TOLERANCE, getHeight() - TWO_PLUS_HALF_TOLERANCE, getWidth() - TWO_PLUS_HALF_TOLERANCE, ONE_PLUS_HALF_TOLERANCE);
        } else {
            graphics.drawLine(ONE_PLUS_HALF_TOLERANCE, ONE_PLUS_HALF_TOLERANCE, getWidth() - TWO_PLUS_HALF_TOLERANCE, getHeight() - TWO_PLUS_HALF_TOLERANCE);
        }
    }

	public boolean getSelected() {
		return isSelected;
	}

	public void setSelected(boolean sel) {
		isSelected = sel;
		referenceListeners.forEach(ReferenceListener::notifyReference);
		repaint();
	}

	/**
	 * Decide if the point of the click is located close to the line
	 *
	 * @param x click X
	 * @param y click Y
	 * @return hit the line?
	 */
	public boolean clickedOnLine(int x, int y){
		Point a, b;
		Point c = new Point(x, y);
		if(isFlipped){
			a = new Point(getLocation().x, getLocation().y+getHeight());
			b = new Point(getLocation().x+getWidth(), getLocation().y);
		}else{
			a = new Point(getLocation().x, getLocation().y);
			b = new Point(getLocation().x+getWidth(), getLocation().y+getHeight());
		}
		double distanceAB = Math.sqrt(Math.pow((double)(a.x-b.x), 2.0) + Math.pow((double)(a.y-b.y), 2.0));
		double distanceAC = Math.sqrt(Math.pow((double)(a.x-c.x), 2.0) + Math.pow((double)(a.y-c.y), 2.0));
		double distanceBC = Math.sqrt(Math.pow((double)(b.x-c.x), 2.0) + Math.pow((double)(b.y-c.y), 2.0));
		return distanceAC + distanceBC < distanceAB+1.25;
	}


	/**
	 * Add Mouse Motion Listener with drag function
	 */
	protected final void addDragListeners()
	{
		addMouseMotionListener(new DraggableComponentMouseListener());
	}

    /**
     * Get component center location on canvas
     *
     * @return component center location on canvas
     */
	public Point getAbsCenter() {
		Point location = getLocation();
		Point tableSizeCorrection = new Point(0, 0);

		int lineComponentHeight = getHeight();
		int lineComponentWidth = getWidth();

		if(lineComponentHeight * 2 > lineComponentWidth) {

			DraggableComponent upperComponent, lowerComponent;

			if(rel1.getAbsCenter().y < rel2.getAbsCenter().y) {
				upperComponent = rel1;
				lowerComponent = rel2;
			} else {
				upperComponent = rel2;
				lowerComponent = rel1;
			}

			int upperHeight = upperComponent.getHeight();
			int lowerHeight = lowerComponent.getHeight();

			tableSizeCorrection.y = (upperHeight - lowerHeight) / 4;
			tableSizeCorrection.x = tableSizeCorrection.y * (lineComponentWidth - CLICK_TOLERANCE) / (lineComponentHeight - CLICK_TOLERANCE) * (isFlipped ? -1 : 1);
		}
		return new Point(
				location.x + lineComponentWidth / 2 + tableSizeCorrection.x,
				location.y + lineComponentHeight / 2 + tableSizeCorrection.y
		);
    }
}
