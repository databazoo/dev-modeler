
package com.databazoo.devmodeler.model.reference;

import com.databazoo.components.elements.LineComponent;
import com.databazoo.components.icons.IIconable;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.model.Workspace;

import java.awt.*;


/**
 *
 * @author bobus
 */
public abstract class LineComponentReference extends LineComponent implements IIconable, IReferenceElement {
	protected final Workspace workspace;

	public LineComponentReference(Workspace workspace) {
		this.workspace = workspace;
	}
	protected void assignToRels(){
		((DraggableComponentReference)rel1).getLinks().add(this);
		((DraggableComponentReference)rel2).getLinks().add(this);
	}

	@Override
	public void drop(){
		((DraggableComponentReference)rel1).getLinks().remove(this);
		((DraggableComponentReference)rel2).getLinks().remove(this);
		if(this instanceof ConstraintReference){
			((DraggableComponentReference)rel1).workspace.getConstraints().remove((ConstraintReference)this);
		}else if(this instanceof TriggerReference){
			((DraggableComponentReference)rel1).workspace.getTriggers().remove((TriggerReference)this);
		}
	}

	@Override
	protected boolean haveClickableChild(Point p, int clickMask){
		int x = p.x + getLocation().x;
		int y = p.y + getLocation().y;
		if(workspace != null){
			for(ConstraintReference conRef: workspace.getConstraints()){
				if(conRef.clickedOnLine(x, y))
				{
					if(clickMask == CLICK_TYPE_LEFT) {
						conRef.clicked();
					}else if(clickMask == CLICK_TYPE_DOUBLE) {
						conRef.doubleClicked();
					}else{
						conRef.rightClicked();
					}
					return true;
				}
			}
			for(TriggerReference trig: workspace.getTriggers()){
				if(trig.clickedOnLine(x, y))
				{
					if(clickMask == CLICK_TYPE_LEFT) {
						trig.clicked();
					}else if(clickMask == CLICK_TYPE_DOUBLE) {
						trig.doubleClicked();
					}else{
						trig.rightClicked();
					}
					return true;
				}
			}
		}
		Canvas.instance.setSelectedElement(null);
		return true;
	}

	@Override
	public Workspace getWorkspace(){
		return workspace;
	}
}
