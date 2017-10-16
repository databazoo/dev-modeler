
package com.databazoo.devmodeler.model.reference;

import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.SearchPanel;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.Workspace;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;


/**
 *
 * @author bobus
 */
public class FunctionReference extends DraggableComponentReference {

	protected final Function func;

	public FunctionReference(Workspace workspace, Function func) {
		super(workspace);
		this.func = func;
		draw();
	}
	private void draw(){
		setLayout(null);
		setBackground(Function.BG_COLOR);
		displayName = func.displayName;
		displayNameWidth = func.displayNameWidth;
		checkKnownLocation();
		checkSize();
	}

	@Override
	public Function getElement(){
		return func;
	}
	@Override
	public String toString(){
		return func.toString();
	}

	@Override
	public Icon getIcon16(){
		return func.getIcon16();
	}

	@Override
	public void checkSize(){
		func.checkSize();
		setSize(func.getSize());
	}
	@Override
	public void checkConstraints(){
		super.checkConstraints();
		//getIndexLines().checkSize();
		((SchemaReference)getParent()).checkSize(this);
	}
	@Override
	protected void mouseUp(){
		((DraggableComponent)getParent()).checkSize();
	}

	@Override
	public void drop(){
		if(isSelected) {
			Canvas.instance.setSelectedElement(null);
		}
		links.forEach(LineComponentReference::drop);
		SchemaReference schemaRef = workspace.find(func.getSchema());
		schemaRef.remove(this);
		schemaRef.getFunctions().remove(this);
		workspace.getFunctions().remove(this);
		workspace.getDB().getProject().save();
		schemaRef.checkEmpty();

		Canvas.instance.drawProject(true);
		SearchPanel.instance.updateDbTree();
	}

	@Override
	public void unSelect(){
		isSelected = false;
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		repaint();
	}

	public DefaultMutableTreeNode getTreeView(){
		return func.getTreeView(new DefaultMutableTreeNode(this));
	}

	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching) {
		return func.getTreeView(search, fulltext, searchNotMatching, new DefaultMutableTreeNode(this));
	}
}
