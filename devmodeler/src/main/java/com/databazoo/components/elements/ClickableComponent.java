package com.databazoo.components.elements;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import com.databazoo.devmodeler.gui.RightClickMenu;
import com.databazoo.devmodeler.tools.Geometry;
import com.databazoo.devmodeler.wizards.relation.RelationWizardConfig;
import com.databazoo.tools.Schedule;

/**
 * Click-enabled components
 *
 * @author bobus
 */
public abstract class ClickableComponent extends JComponent implements MouseListener {

	protected static final int CLICK_TYPE_LEFT = 1;
	protected static final int CLICK_TYPE_RIGHT = 2;
	protected static final int CLICK_TYPE_DOUBLE = 3;

	protected final transient List<ReferenceListener> referenceListeners = new ArrayList<>();

	/**
	 * Constructor
	 */
	public ClickableComponent(){
		setMouseListener();
	}

	/**
	 * Add mouse listeners
	 */
	private void setMouseListener(){
		addMouseListener(this);
	}

	public void addReferenceListener(ReferenceListener listener){
		referenceListeners.add(listener);
	}

	/**
	 * Click handling
	 *
	 * @param e standard mouse event
	 */
	@Override
	public void mouseClicked(MouseEvent e) {
		if(e.getButton() == 3){
			RelationWizardConfig.setNewElementLocation(Geometry.getSnappedPosition(e.getX(), e.getY()));
			RightClickMenu.setLocationTo(e.getComponent(), new Point(e.getX(), e.getY()));
			if(!haveClickableChild(e.getPoint(), CLICK_TYPE_RIGHT)){
				rightClicked();
			}
			e.consume();
		}else if (e.getButton() == 1 && e.getClickCount() == 2 && !e.isConsumed()) {
			if(!haveClickableChild(e.getPoint(), CLICK_TYPE_DOUBLE)){
				Schedule.inEDT(this::doubleClicked);
			}
			e.consume();
		}else if (e.getButton() == 1){
			if(!haveClickableChild(e.getPoint(), CLICK_TYPE_LEFT)){
				clicked();
			}
		}else if (e.getButton() == 4 || e.getButton() == 5){
			if(getParent() != null) {
				if(getParent() instanceof ClickableComponent){
					((MouseListener)getParent()).mouseClicked(e);
				}else if(getParent().getParent() != null && getParent().getParent() instanceof JScrollPane){
					JScrollBar scrollBar = ((JScrollPane)getParent().getParent()).getHorizontalScrollBar();
					int val = scrollBar.getValue();
					int increment = scrollBar.getUnitIncrement();
					if(e.getButton()==5){
						val += increment;
					}else{
						val -= increment;
					}
					if(val < scrollBar.getMinimum()){
						val = scrollBar.getMinimum();
					}else if(val > scrollBar.getMaximum()){
						val = scrollBar.getMaximum();
					}
					scrollBar.setValue(val);
				}
			}
		}
	}

	/**
	 * Click handling
	 *
	 * @param e standard mouse event
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.getButton() == 1){
			mouseUp();
		}
	}

	/**
	 * Click handling
	 *
	 * @param e standard mouse event
	 */
	@Override public void mousePressed(MouseEvent e) {}

	/**
	 * Click handling
	 *
	 * @param e standard mouse event
	 */
	@Override public void mouseEntered(MouseEvent e) {}

	/**
	 * Click handling
	 *
	 * @param e standard mouse event
	 */
	@Override public void mouseExited(MouseEvent e) {}

	/**
	 * Mouse up event handling - to be overridden
	 */
	protected void mouseUp(){}

	/**
	 * Decide if we should forward the click to some component below. Useful for forwarding the click to Line Components
	 * lying under a Draggable Component.
	 *
	 * @param p click location from MouseEvent.getPoint()
	 * @param clickMask CLICK_TYPE_*
	 * @return was the click forwarded to another component?
	 */
	protected boolean haveClickableChild(Point p, int clickMask){
		return false;
	}

	/**
	 * Component clicked event handling - to be overridden
	 */
	public abstract void clicked();

	/**
	 * Component double-clicked event handling - to be overridden
	 */
	public abstract void doubleClicked();

	/**
	 * Component right-clicked event handling - to be overridden
	 */
	public abstract void rightClicked();
}
