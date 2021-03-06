
package com.databazoo.devmodeler.model.explain;

import java.awt.*;

import com.databazoo.components.elements.LineComponent;

/**
 * Line component describing load generated by given operation in EXPLAIN diagram.
 *
 * @author bobus
 */
public class ExplainLoad extends LineComponent {

	ExplainLoad(){
		isDashed = false;
		lineColor = Color.GRAY;
		respectsZoom = false;
	}

	void assignToOperations(){
		ExplainOperation opParent = (ExplainOperation) rel1;
		ExplainOperation opChild = (ExplainOperation) rel2;

		opParent.childrenLinks.add(this);
		opChild.parentLink = this;
	}

	public void checkLineWidth(){
		lineWidth = (int)Math.pow(((ExplainOperation) rel2).getCost(), 0.225);
		if(lineWidth < 1){
			lineWidth = 1;
		}else if(lineWidth > 72){
			lineWidth = 72;
		}
	}

	@Override public void clicked(){}
	@Override public void doubleClicked(){}
	@Override public void rightClicked(){}

}
