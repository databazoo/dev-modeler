
package com.databazoo.devmodeler.model.reference;

import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.components.elements.ReferenceListener;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.SearchPanel;
import com.databazoo.devmodeler.gui.view.ViewMode;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Workspace;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author bobus
 */
public class RelationReference extends DraggableComponentReference implements ReferenceListener {
	protected final Relation rel;
	protected final transient List<Attribute> attributes = new ArrayList<>();
	private Relation.IndexLines indexLines;

	public RelationReference(Workspace workspace, Relation rel) {
		super(workspace);
		this.rel = rel;
		if(workspace != null) {
			rel.addReferenceListener(this);
		}
		draw();
		checkKnownLocation();
		checkSize();
	}

	public RelationReference(Relation rel) {
		super(null);
		this.rel = rel;
		draw();
	}

	@Override
	public void notifyReference() {
		setSize(rel.getSize());
		displayNameWidth = rel.displayNameWidth;
		final Relation.IndexLines lines = getIndexLines();
		final Relation.IndexLines relationLines = rel.getIndexLinePanel();
		lines.setSize(relationLines.getSize());
		lines.setImageCache(relationLines.getImageCache());
		lines.repaint();
	}

	public List<Attribute> getAttributes() {
		return attributes;
	}

	private void draw(){
		setBackground(Relation.LOW_COLOR);
		displayName = rel.displayName;
		displayNameWidth = rel.displayNameWidth;
	}
	@Override public void clicked(){
		super.clicked();
		if(DesignGUI.getView() == ViewMode.DATA){
			if(rel.getCountAttributes() < 6){
				rel.setCountAttributes(6);
				rel.checkComponentSize();
			}
			setSize(rel.getSize());
			((DraggableComponent)getParent()).checkSize();
			Canvas.instance.drawRelationDataInfo(this);
			rel.syncInfoWithServer();
		}
	}

	@Override
	public Relation getElement(){
		return rel;
	}
	@Override
	public void unSelect(){
		setSize(rel.getSize());
		isSelected = false;
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		if(DesignGUI.getView() == ViewMode.DATA){
			Canvas.instance.drawRelationDataInfo(this);
		}else{
			repaint();
		}
	}
	@Override
	public String toString(){
		return rel.toString();
	}
	@Override
	public Icon getIcon16(){
		return rel.getIcon16();
	}

	@Override
	public void checkSize(){
		rel.checkSize();
		setSize(rel.getSize());
	}
	@Override
	public void checkConstraints(){
		super.checkConstraints();
		getIndexLines().checkSize();
		for(Index ind: rel.getIndexes()){
			ind.checkSize(this);
		}
		if(getParent() != null) {
			((SchemaReference)getParent()).checkSize(this);
		}
	}
	@Override
	protected void mouseUp(){
		if(getParent() instanceof SchemaReference) {
			((DraggableComponent)getParent()).checkSize();
		}
	}

	@Override
	public void drop(){
		if(isSelected) {
			Canvas.instance.setSelectedElement(null);
		}
		links.forEach(LineComponentReference::drop);
		SchemaReference schemaRef = workspace.find(rel.getSchema());
		schemaRef.remove(this);
		schemaRef.getRelations().remove(this);
		workspace.getRelations().remove(this);
		workspace.getDB().getProject().save();
		schemaRef.checkEmpty();

		DesignGUI.get().drawProjectLater(true);
		SearchPanel.instance.updateDbTree();
	}

	public DefaultMutableTreeNode getTreeView(){
		return rel.getTreeView(false, new DefaultMutableTreeNode(this));
	}
	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching) {
		return rel.getTreeView(search, fulltext, searchNotMatching, new DefaultMutableTreeNode(this));
	}

	private Relation.IndexLines getIndexLines(){
		if(indexLines == null){
			indexLines = rel.createIndexLinePanel(this);
		}
		return indexLines;
	}
	public JComponent getIndexLinePanel(){
		return getIndexLines();
	}
	public void checkIndexLinesSize(){
		getIndexLines().checkSize();
	}
}
