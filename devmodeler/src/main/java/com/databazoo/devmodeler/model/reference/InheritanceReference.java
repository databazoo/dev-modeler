
package com.databazoo.devmodeler.model.reference;

import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.model.Inheritance;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Workspace;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Reference to a trigger (implemented as a line component)
 *
 * @author bobus
 */
public class InheritanceReference extends LineComponentReference {
	private final Inheritance inheritance;

	public InheritanceReference(Workspace w, Inheritance inheritance,RelationReference relShadow1, RelationReference relShadow2) {
		super(w);
		this.inheritance = inheritance;
		this.rel1 = relShadow1;
		this.rel2 = relShadow2;
		this.isDashed = false;
		this.lineColor = Color.GRAY;
		assignToRels();
	}

	@Override public void clicked(){ inheritance.clicked(); }
	@Override public void doubleClicked(){ inheritance.doubleClicked(); }
	@Override public void rightClicked(){ inheritance.rightClicked(); }
	@Override
	public String toString(){
		return inheritance.toString();
	}
	@Override
	public Icon getIcon16(){
		return null;
	}

	@Override
	public IModelElement getElement(){
		return (IModelElement)inheritance.getRel1();
	}

	@Override
	public void unSelect(){
		isSelected = false;
		repaint();
	}

	@Override
	protected void setArrowPosition(){
		double angle;

		Dimension relSize = new Dimension(rel2.getWidth()-Relation.SHADOW_GAP, rel2.getHeight()-Relation.SHADOW_GAP);
		Dimension conSize = new Dimension(getWidth()-2-CLICK_TOLERANCE, getHeight()-2-CLICK_TOLERANCE);

		double relSideRatio = relSize.height * 1.0 / relSize.width;
		double conSideRatio = conSize.height * 1.0 / conSize.width;

		if(direction == RIGHT_BOTTOM_LEFT_TOP){
			if(relSideRatio > conSideRatio){
				arrow2Location = new Point(relSize.width/2+CLICK_TOLERANCE/2, (int)(relSize.width*conSideRatio/2)+CLICK_TOLERANCE/2);
			}else{
				arrow2Location = new Point((int)(relSize.height/conSideRatio/2)+CLICK_TOLERANCE/2, relSize.height/2+CLICK_TOLERANCE/2);
			}
			arrow2Location.x -= 14+16;
			arrow2Location.y -= 15+16;
			angle = Math.atan(conSideRatio);

		}else if(direction == LEFT_TOP_RIGHT_BOTTOM){
			if(relSideRatio > conSideRatio){
				arrow2Location = new Point(conSize.width-(relSize.width/2)+CLICK_TOLERANCE/2, conSize.height-((int)(relSize.width*conSideRatio/2))+CLICK_TOLERANCE/2);
			}else{
				arrow2Location = new Point(conSize.width-((int)(relSize.height/conSideRatio/2))+CLICK_TOLERANCE/2, conSize.height-(relSize.height/2)+CLICK_TOLERANCE/2);
			}
			arrow2Location.x -= 16+16;
			arrow2Location.y -= 15+16;
			angle = Math.atan(conSideRatio)+Math.toRadians(180);

		}else if(direction == RIGHT_TOP_LEFT_BOTTOM){
			if(relSideRatio > conSideRatio){
				arrow2Location = new Point((relSize.width/2)+CLICK_TOLERANCE/2, conSize.height-((int)(relSize.width*conSideRatio/2))+CLICK_TOLERANCE/2);
			}else{
				arrow2Location = new Point(((int)(relSize.height/conSideRatio/2))+CLICK_TOLERANCE/2, conSize.height-(relSize.height/2)+CLICK_TOLERANCE/2);
			}
			arrow2Location.x -= 15+16;
			arrow2Location.y -= 16+16;
			angle = Math.atan(1/conSideRatio)+Math.toRadians(-90);

		}else{
			if(relSideRatio > conSideRatio){
				arrow2Location = new Point(conSize.width-(relSize.width/2)+CLICK_TOLERANCE/2, ((int)(relSize.width*conSideRatio/2))+CLICK_TOLERANCE/2);
			}else{
				arrow2Location = new Point(conSize.width-((int)(relSize.height/conSideRatio/2))+CLICK_TOLERANCE/2, (relSize.height/2)+CLICK_TOLERANCE/2);
			}
			arrow2Location.x -= 15+16;
			arrow2Location.y -= 15+16;
			angle = Math.atan(1/conSideRatio)+Math.toRadians(90);
		}
		arrow2 = rotateArrow(angle);
	}

	private BufferedImage rotateArrow(double rads){
		BufferedImage dimg = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = dimg.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		graphics.rotate(rads, 32, 32);

		graphics.setColor(Color.WHITE);
		graphics.fillPolygon(new int[]{32, 48, 48}, new int[]{33, 26, 38}, 3);

		graphics.setColor(lineColor);
		graphics.drawLine(32, 32, 48, 26);
		graphics.drawLine(32, 33, 48, 38);
		graphics.drawLine(48, 26, 48, 38);

		return dimg;
	}
}
