
package com.databazoo.devmodeler.model.reference;

import javax.swing.*;

import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.model.Workspace;

/**
 * Reference to a trigger (implemented as a line component)
 *
 * @author bobus
 */
public class TriggerReference extends LineComponentReference {
	protected final Trigger trig;

	public TriggerReference(Workspace w, Trigger trig, RelationReference relShadow1, FunctionReference relShadow2) {
		super(w);
		this.trig = trig;
		this.rel1 = relShadow1;
		this.rel2 = relShadow2;
		this.isDashed = false;
		this.lineColor = Trigger.LINE_COLOR;
		assignToRels();
	}

	@Override public void clicked(){ if(trig != null){ trig.clicked(); } }
	@Override public void doubleClicked(){ if(trig != null){ trig.doubleClicked(); } }
	@Override public void rightClicked(){ if(trig != null){ trig.rightClicked(workspace != null ? workspace.toString() : null); } }
	@Override
	public String toString(){
		return trig.toString();
	}
	@Override
	public Icon getIcon16(){
		return trig.getIcon16();
	}

	@Override
	public Trigger getElement(){
		return trig;
	}

	@Override
	public void unSelect(){
		isSelected = false;
		repaint();
	}
}