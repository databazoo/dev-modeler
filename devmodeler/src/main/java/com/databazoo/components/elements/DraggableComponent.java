
package com.databazoo.components.elements;

import com.databazoo.components.UIConstants;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.DBTree;
import com.databazoo.devmodeler.gui.HotMenu;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.reference.IReferenceElement;
import com.databazoo.devmodeler.model.reference.SchemaReference;
import com.databazoo.devmodeler.tools.Geometry;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * Makes it possible to create a JComponent that can be clicked and dragged.
 *
 * @author bobus
 */
public abstract class DraggableComponent extends EnvironmentComponent {
	public final static int SHADOW_GAP = 6;
	private final static int SHADOW_OFFSET = 2;
	private final static int SHADOW_ALPHA = 60;
	private final static Color SHADOW_COLOR = Color.BLACK;


	protected int overbearingZOrder = 0;
	protected Schema schemaContainer;

	public String displayName;
	public int displayNameWidth;

	protected Dimension arcs = new Dimension(10, 10);
	private Dimension rememberedSize;
	private Point rememberedLocation;
	protected boolean isSelected = false;
	public boolean isOrganized = false;

	/**
	 * Constructor
	 */
	public DraggableComponent(){
		addDragListeners();
		setOpaque(false);
	}

	/**
	 * Getter for selection status
	 *
	 * @return is currently selected?
	 */
	public boolean isSelected(){
		return isSelected;
	}

	/**
	 * Getter for component size in 1:1 scale
	 *
	 * @return component size
	 */
	public Dimension getRememberedSize(){
		return rememberedSize;
	}

	/**
	 * Getter for component location in 1:1 scale
	 *
	 * @return component location
	 */
	public Point getRememberedLocation(){
		return rememberedLocation;
	}

	/**
	 * Get component center location in 1:1 scale
	 *
	 * @return component center location
	 */
	public Point getCenter(){
		Point l = getRememberedLocation();
		l.x += (int)Math.round(getRememberedSize().width/2.0);
		l.y += (int)Math.round(getRememberedSize().height/2.0);
		return l;
	}

	/**
	 * Get component center location on canvas in 1:1 scale
	 *
	 * @return component center location on canvas
	 */
	public Point getAbsCenter(){
		if(getParent() != null){
			Point parentL = Geometry.getUnZoomed(getParent().getLocation());
			Point l = getRememberedLocation();
			return new Point(l.x + (int)Math.round(getRememberedSize().width/2.0) + parentL.x,
							l.y + (int)Math.round(getRememberedSize().height/2.0) + parentL.y);
		}else{
			return null;
		}
	}

	/**
	 * Set component location
	 *
	 * @param location zoomed location
	 */
	@Override
	public void setLocation(Point location){
		super.setLocation(location);
		rememberedLocation = Geometry.getUnZoomed(location);

		if(this instanceof IModelElement){
			IModelElement elem = (IModelElement) this;
			DB db = elem.getDB();
			if(db != null){
				db.saveKnownLocation(getFullName(), rememberedLocation);
			}
		}else if(this instanceof IReferenceElement){
			IReferenceElement elem = (IReferenceElement) this;
			DB db = elem.getWorkspace().getDB();
			if(db != null){
				db.saveKnownLocation(elem.getWorkspace().getName()+"."+elem.getElement().getFullName(), rememberedLocation);
			}
		}
	}

	/**
	 * Set component size
	 *
	 * @param size zoomed size
	 */
	@Override
	public void setSize(Dimension size){
		super.setSize(size);
		rememberedSize = Geometry.getUnZoomed(size);
	}

	/**
	 * Notify line components of location change
	 */
	public abstract void checkConstraints();

	/**
	 * Recheck the size of the component
	 */
	public abstract void checkSize();

	/**
	 * Add Mouse Motion Listener with drag function
	 */
	private void addDragListeners()
	{
		final DraggableComponent handle = this;
		addMouseMotionListener(new DraggableComponentMouseListener(){
			@Override
			public void mouseDragged(MouseEvent e) {
				if(isSelected || !Settings.getBool(Settings.L_DND_DRAG_ONLY_SELECTED) || DraggableComponent.this instanceof SchemaReference)
				{
					HotMenu.instance.setVisible(false);

					Point parentOnScreen = getParent().getLocationOnScreen();
					Point mouseOnScreen = e.getLocationOnScreen();

					Point position = new Point(mouseOnScreen.x - parentOnScreen.x - getAnchorX(), mouseOnScreen.y - parentOnScreen.y - getAnchorY());

					if(Canvas.SNAP_TO_GRID){
						if((position.x % Canvas.GRID_SIZE) < Canvas.SNAPPINESS || Canvas.GRID_SIZE - (position.x % Canvas.GRID_SIZE) < Canvas.SNAPPINESS){
							position.x = (int)Math.round((double)position.x / Canvas.GRID_SIZE) * Canvas.GRID_SIZE;
						}
						if(position.y % Canvas.GRID_SIZE < Canvas.SNAPPINESS || Canvas.GRID_SIZE - (position.y % Canvas.GRID_SIZE) < Canvas.SNAPPINESS){
							position.y = (int)Math.round((double)position.y / Canvas.GRID_SIZE) * Canvas.GRID_SIZE;
						}
					}

					if(position.x < 0){position.x = 0;}
					if(position.y < 0){position.y = 0;}

					setLocation(position);

					getParent().setComponentZOrder(handle, overbearingZOrder);
					repaint();

					checkConstraints();
				}else{
					super.mouseDragged(e);
				}
			}
		});
	}

	@Override
	protected void paintComponent(Graphics g) {
		paintComponent(Canvas.getTitleFont(), (Graphics2D) g);
	}

	protected final void paintComponent(Font titleFont, Graphics2D graphics) {
		Rectangle bounds = graphics.getClipBounds();
		boolean attributeRedraw = bounds.width == Canvas.ZOOMED_ENTITY_WIDTH - 7 && bounds.height == Attribute.V_SIZE;
		if(!attributeRedraw || isSelected) {
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

			int width = getWidth();
			int height = getHeight();
			int gap = SHADOW_GAP;
			Color shadowColorA = new Color(SHADOW_COLOR.getRed(), SHADOW_COLOR.getGreen(), SHADOW_COLOR.getBlue(), SHADOW_ALPHA);

			// Draw shadow borders.
			graphics.setColor(shadowColorA);
			graphics.fillRoundRect(SHADOW_OFFSET, SHADOW_OFFSET, width - SHADOW_GAP + SHADOW_OFFSET, height - SHADOW_GAP + SHADOW_OFFSET, arcs.width + 6, arcs.height + 6);

			// Draw the rounded opaque panel with borders.
			graphics.setColor(getBackground());
			graphics.fillRoundRect(0, 0, width - gap, height - gap, arcs.width, arcs.height);
			if (isSelected) {
				graphics.setColor(UIConstants.Colors.getSelectionBackground());
				graphics.drawRoundRect(1, 1, width - gap - 2, height - gap - 2, arcs.width - 1, arcs.height - 1);
				graphics.setColor(UIConstants.Colors.getSelectionBackground());
				graphics.drawRoundRect(2, 2, width - gap - 4, height - gap - 4, arcs.width - 2, arcs.height - 2);
				graphics.setColor(UIConstants.Colors.getSelectionBackground());
				graphics.drawRoundRect(3, 3, width - gap - 6, height - gap - 6, arcs.width - 3, arcs.height - 3);
			}
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(getForeground());
			graphics.setStroke(Canvas.getBasicStroke());
			graphics.drawRoundRect(0, 0, width - gap, height - gap, arcs.width, arcs.height);

			if (displayName != null) {
				graphics.setFont(titleFont);
				graphics.drawString(displayName, (width - displayNameWidth - gap) / 2, titleFont.getSize());
			}
		}
	}

	/**
	 * Remove drag listeners
	 */
	protected void removeDragListeners(){
		for (MouseMotionListener listener : this.getMouseMotionListeners()) {
			removeMouseMotionListener(listener);
		}
		setCursor(Cursor.getDefaultCursor());
	}

	/**
	 * Get database object's full name. Skeletal implementation that is usually overridden.
	 *
	 * @return String
	 */
	public String getFullName(){
		return "Abstract Draggable Component";
	}

	/**
	 * Get parent container (usually a schema)
	 *
	 * @return Schema
	 */
	public Schema getSchema(){
		return schemaContainer;
	}

	/**
	 * Setter for selected status
	 *
	 * @param sel is selected?
	 */
	public void setSelected(boolean sel) {
		isSelected = sel;
		repaint();
	}

	/**
	 * Scroll the canvas to center the object if the object is too close to border
	 */
	protected void scrollIntoView(){
		if(getAbsCenter() == null){
			return;
		}
		final Timer initTimer = new Timer(DBTree.animationDelay, null);
		initTimer.addActionListener(e -> {
            initTimer.stop();
            Point myCenter = Geometry.getZoomed(getAbsCenter());
            if (myCenter == null) {
                myCenter = Geometry.getZoomed(getCenter());
            }

            Dimension scrollSize = Canvas.instance.getScrollSize();
            Point scrollPosition = Canvas.instance.getScrollPosition();

            Dimension min = new Dimension(
                    scrollPosition.x + Config.SCROLL_THRESHOLD,
                    scrollPosition.y + Config.SCROLL_THRESHOLD);
            Dimension max = new Dimension(
                    scrollPosition.x + (int) scrollSize.getWidth() - Config.SCROLL_THRESHOLD,
                    scrollPosition.y + (int) scrollSize.getHeight() - Config.SCROLL_THRESHOLD);

            if (myCenter.x < min.width || myCenter.x > max.width || myCenter.y < min.height || myCenter.y > max.height) {
                final Point originalScrollPosition = Canvas.instance.getScrollPosition();
                final Point finalScrollPosition = new Point(myCenter.x - scrollSize.width / 2, myCenter.y - scrollSize.height / 2);

                if (Settings.getBool(Settings.L_PERFORM_ANIMATION)) {
                    final int steps = Settings.getInt(Settings.L_PERFORM_ANIMATE_STEPS);
                    final Point step = new Point((finalScrollPosition.x - originalScrollPosition.x) / steps, (finalScrollPosition.y - originalScrollPosition.y) / steps);
					Schedule.inWorker(() -> animateScroll(originalScrollPosition, finalScrollPosition, steps, step));
                } else {
                    Canvas.instance.scrollTo(finalScrollPosition);
                    Canvas.instance.checkInfoPanelAndOverviewLocation();
                }
            }
        });
		initTimer.start();
	}

	private void animateScroll(Point originalScrollPosition, Point finalScrollPosition, int steps, Point step) {
		for(int runs = 1; runs < steps; runs++) {
            Canvas.instance
                    .scrollTo(new Point(originalScrollPosition.x + step.x * runs, originalScrollPosition.y + step.y * runs));
            try {
                Thread.sleep(Settings.getInt(Settings.L_PERFORM_ANIMATE_TIME) / steps);
            } catch (Exception ex) {
                Dbg.notImportant("Animation interrupted", ex);
            }
        }
		Canvas.instance.scrollTo(finalScrollPosition);
	}
}
