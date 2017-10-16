
package com.databazoo.devmodeler.model.reference;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.DBTree;
import com.databazoo.devmodeler.model.Workspace;

/**
 * Reference to a draggable component. Overrides default components behavior and adds implementation of IReferenceElement.
 *
 * @author bobus
 */
public abstract class DraggableComponentReference extends DraggableComponent implements IReferenceElement {
	protected final Workspace workspace;
	transient List<LineComponentReference> links = new CopyOnWriteArrayList<>();

	public DraggableComponentReference(Workspace workspace) {
		this.workspace = workspace;
	}

	public List<LineComponentReference> getLinks(){
		return links;
	}

	@Override
	public void checkConstraints(){
		for(LineComponentReference link : links){
			link.checkSize();
		}
	}

	@Override
	public Dimension getRememberedSize(){
		if(super.getRememberedSize() == null) {
			return getSize();
		} else {
			return super.getRememberedSize();
		}
	}
	@Override
	public Point getRememberedLocation(){
		if(super.getRememberedLocation() == null) {
			return getLocation();
		} else {
			return super.getRememberedLocation();
		}
	}
	@Override
	public Workspace getWorkspace(){
		return workspace;
	}

	void checkKnownLocation(){
		if(workspace != null){
			Point loc = workspace.getDB().getKnownLocation(workspace.getName()+"."+getElement().getFullName());
			if(loc != null){
				setLocation(loc);
			}
		}
	}

	@Override public void clicked(){
		if(!isSelected){
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			Canvas.instance.setSelectedElement(getElement());
			isSelected = true;
			DBTree.instance.selectRelationByName(DBTree.L_WORKSPACES, workspace.toString(), getElement().toString());
		}
		repaint();
	}
	@Override public void doubleClicked(){ getElement().doubleClicked(); }
	@Override public void rightClicked(){ getElement().rightClicked(workspace != null ? workspace.toString() : null); }
}
