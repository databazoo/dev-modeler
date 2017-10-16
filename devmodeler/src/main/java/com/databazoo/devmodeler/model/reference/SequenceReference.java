
package com.databazoo.devmodeler.model.reference;

import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.SearchPanel;
import com.databazoo.devmodeler.model.Sequence;
import com.databazoo.devmodeler.model.Workspace;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;


/**
 *
 * @author bobus
 */
public class SequenceReference extends DraggableComponentReference {

	protected final Sequence sequence;

	public SequenceReference(Workspace workspace, Sequence sequence) {
		super(workspace);
		this.sequence = sequence;
		draw();
	}
	private void draw(){
		setLayout(null);
		setBackground(Sequence.BG_COLOR);
		displayName = sequence.displayName;
		displayNameWidth = sequence.displayNameWidth;
		checkKnownLocation();
		checkSize();
	}

	@Override
	public Sequence getElement(){
		return sequence;
	}
	@Override
	public String toString(){
		return sequence.toString();
	}

	@Override
	public Icon getIcon16(){
		return sequence.getIcon16();
	}

	@Override
	public void checkSize(){
		sequence.checkSize();
		setSize(sequence.getSize());
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
		SchemaReference schemaRef = workspace.find(sequence.getSchema());
		schemaRef.remove(this);
		schemaRef.getSequences().remove(this);
		workspace.getSequences().remove(this);
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
		return sequence.getTreeView(new DefaultMutableTreeNode(this));
	}
	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching) {
		return sequence.getTreeView(search, fulltext, searchNotMatching, new DefaultMutableTreeNode(this));
	}
}
