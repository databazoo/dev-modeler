
package com.databazoo.devmodeler.model.reference;

import javax.swing.*;
import java.awt.*;

import com.databazoo.components.UIConstants;
import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.Workspace;
import com.databazoo.devmodeler.tools.Geometry;


/**
 *
 * @author bobus
 */
public class ConstraintReference extends LineComponentReference {
	protected final Constraint con;

	public ConstraintReference(Workspace w, Constraint con, RelationReference relShadow1, RelationReference relShadow2) {
		super(w);
		this.con = con;
		this.rel1 = relShadow1;
		this.rel2 = relShadow2;
		this.isDashed = (con != null && con.getAttr1().getBehavior().isAttNull());
		this.lineWidth = 1;
		assignToRels();
		if(con != null) {
			con.addReferenceListener(() -> {
				isSelected = con.getSelected();
				repaint();
			});
		}
	}

	@Override
	protected void setArrowPosition(){
		if(con != null){
			Constraint.calculateArrowPosition(this, con.getBehavior());
		}
	}

	@Override public void clicked(){
		if(con != null){
			if(!isSelected){
				if (con.getBehavior().getAttr1() != null) {
					setColorForAttribute(con.getBehavior().getAttr1().getName(), rel1, Canvas.SELECTION_COLOR);
				}
				if (con.getBehavior().getAttr2() != null) {
					setColorForAttribute(con.getBehavior().getAttr2().getName(), rel2, Canvas.SELECTION_COLOR);
				}
				Canvas.instance.setSelectedElement(con);
				isSelected = true;
				repaint();
			}
		}
	}

	private void setColorForAttribute(String name, DraggableComponent rel, Color selection_color) {
		for(Component c : rel.getComponents()) {
			if(c instanceof Attribute) {
				if (c.getName().equals(name)) {
					c.setForeground(selection_color);
					break;
				}
			}
		}
	}

	@Override public void doubleClicked(){ if(con != null){ con.doubleClicked(); } }
	@Override public void rightClicked(){ if(con != null){ con.rightClicked(workspace != null ? workspace.toString() : null); } }
	@Override
	public String toString(){
		return con.toString();
	}
	@Override
	public Icon getIcon16(){
		return con.getIcon16();
	}

	@Override
	public Constraint getElement(){
		return con;
	}

	@Override
	public void unSelect(){
		isSelected = false;
		repaint();
		if (con.getBehavior().getAttr1() != null) {
			setColorForAttribute(con.getBehavior().getAttr1().getName(), rel1, UIConstants.COLOR_FG_ATTRIBUTE);
		}
		if (con.getBehavior().getAttr2() != null) {
			setColorForAttribute(con.getBehavior().getAttr2().getName(), rel2, UIConstants.COLOR_FG_ATTRIBUTE);
		}
	}

	@Override
	public void checkSize(){
		if(con != null && con.isSelfRelation()){
			checkSizeSelfRelation();
		}else{
			super.checkSize();
		}
	}
	private void checkSizeSelfRelation(){
		int level = -1;
		for(Constraint con1 : ((RelationReference)getRel1()).rel.getConstraints()){
			if(con1.getFullName().equals(con.getFullName())){
				break;
			}
			if(con1.isSelfRelation()){
				level++;
			}
		}
		Point loc = getRel1().getAbsCenter();
		if(loc != null){
			loc.x -= (level*10);
			loc.y -= (level*10);
			setLocation(Geometry.getZoomed(loc));
			setSize(Geometry.getZoomed(new Dimension(getRel1().getWidth()/2+(level*20)+30, getRel1().getHeight()/2+(level*20)+30)));
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		if(con != null && con.isSelfRelation()){
			Graphics2D graphics = (Graphics2D) g;
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			graphics.setStroke(isDashed ? Canvas.getLineStrokeDashed() : Canvas.getLineStrokeFull(lineWidth));
			graphics.setPaint(lineColor);
			graphics.drawLine(1, 1, getWidth()-2, 1);
			graphics.drawLine(getWidth()-2, 1, getWidth()-2, getHeight()-2);
			graphics.drawLine(getWidth()-2, getHeight()-2, 1, getHeight()-2);
			graphics.drawLine(1, getHeight()-2, 1, 1);
		}else{
			super.paintComponent(g);
		}
	}
}
