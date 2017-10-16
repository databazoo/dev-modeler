
package com.databazoo.devmodeler.model.reference;

import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.SearchPanel;
import com.databazoo.devmodeler.model.View;
import com.databazoo.devmodeler.model.Workspace;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;


/**
 *
 * @author bobus
 */
public class ViewReference extends DraggableComponentReference {

	protected final View view;

	public ViewReference(Workspace workspace, View view) {
		super(workspace);
		this.view = view;
		draw();
	}
	private void draw(){
		setLayout(null);
		setBackground(View.BG_COLOR);
		displayName = view.displayName;
		displayNameWidth = view.displayNameWidth;
		checkKnownLocation();
		checkSize();
	}

	@Override
	public View getElement(){
		return view;
	}
	@Override
	public String toString(){
		return view.toString();
	}

	@Override
	public Icon getIcon16(){
		return view.getIcon16();
	}

	@Override
	public void checkSize(){
		view.checkSize();
		setSize(view.getSize());
	}
	@Override
	public void checkConstraints(){
		super.checkConstraints();
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
		SchemaReference schemaRef = workspace.find(view.getSchema());
		schemaRef.remove(this);
		schemaRef.getViews().remove(this);
		workspace.getViews().remove(this);
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
		return view.getTreeView(new DefaultMutableTreeNode(this));
	}
	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching) {
		return view.getTreeView(search, fulltext, searchNotMatching, new DefaultMutableTreeNode(this));
	}
}
