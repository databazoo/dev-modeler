
package com.databazoo.devmodeler.model;

import java.awt.*;
import java.awt.image.BufferedImage;

import com.databazoo.components.elements.LineComponent;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.gui.Canvas;

/**
 *
 * @author bobus
 */
public class Inheritance extends LineComponent {

	Inheritance(Relation relLoc, Relation relRem) {
		rel1 = relLoc;
		rel2 = relRem;

		isDashed = false;
		lineColor = Color.GRAY;

		addDragListeners();
	}

	@Override public void clicked(){ rel1.clicked(); }
	@Override public void doubleClicked(){ rel1.doubleClicked(); }
	@Override public void rightClicked(){ rel1.rightClicked(); }
	@Override protected boolean haveClickableChild(Point p, int clickMask){
		int x = p.x + getLocation().x;
		int y = p.y + getLocation().y;
		for(Constraint con: getDB().getConstraints()){
			if(con.clickedOnLine(x, y))
			{
				if(clickMask == CLICK_TYPE_LEFT) {
					con.clicked();
				}else if(clickMask == CLICK_TYPE_DOUBLE) {
					con.doubleClicked();
				}else{
					con.rightClicked();
				}
				return true;
			}
		}
		for(Trigger trig: getDB().getTriggers()){
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
		Canvas.instance.setSelectedElement(null);
		if(clickMask == CLICK_TYPE_LEFT) {
			getDB().clicked();
		}else if(clickMask == CLICK_TYPE_DOUBLE) {
			getDB().doubleClicked();
		}else{
			getDB().rightClicked();
		}
		return true;
	}

	public IConnection getConnection(){
		return ((IModelElement)rel1).getConnection();
	}

	public DB getDB(){
		return ((IModelElement)rel1).getDB();
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
