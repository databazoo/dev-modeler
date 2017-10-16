
package com.databazoo.devmodeler.model;

import com.databazoo.components.GCFrame;
import com.databazoo.components.elements.ClickableComponent;
import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.components.icons.IIconable;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.gui.*;
import com.databazoo.devmodeler.model.reference.ConstraintReference;
import com.databazoo.devmodeler.model.reference.FunctionReference;
import com.databazoo.devmodeler.model.reference.InheritanceReference;
import com.databazoo.devmodeler.model.reference.RelationReference;
import com.databazoo.devmodeler.model.reference.SchemaReference;
import com.databazoo.devmodeler.model.reference.SequenceReference;
import com.databazoo.devmodeler.model.reference.TriggerReference;
import com.databazoo.devmodeler.model.reference.ViewReference;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.tools.organizer.OrganizerFactory;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representation of workspaces.
 *
 * @author bobus
 */
public class Workspace extends ClickableComponent implements IIconable, Serializable {
	private static final long serialVersionUID = 1905122041950000001L;

	public static final String L_CLASS = "Workspace";
	public static final Icon ico16 = Theme.getSmallIcon(Theme.ICO_WORKSPACE);

	public static Workspace create (DB db) {
		String newName = JOptionPane.showInputDialog(GCFrame.getActiveWindow(), "New workspace name", "Workspace " + (db.getProject().getWorkspaces().size() + 1));
		if(newName != null && !newName.isEmpty()){
			Workspace ws = new Workspace(newName, db);
			db.getProject().getWorkspaces().add(ws);
			SearchPanel.instance.updateDbTree();
			return ws;
		}else{
			return null;
		}
	}

	public static Workspace create (DraggableComponent comp) {
		Workspace ws = create(((IModelElement) comp).getDB());
		if (ws != null) {
			if (comp instanceof Relation) {
				ws.add((Relation)comp);
				for(Constraint con : ((Relation)comp).getConstraints()){
					if(con.getRel1() != null){
						ws.add((Relation)con.getRel1());
					}
					if(con.getRel2() != null){
						ws.add((Relation)con.getRel2());
					}
				}

			} else if (comp instanceof Function) {
				ws.add((Function)comp);

			} else if (comp instanceof View) {
				ws.add((View)comp);

			} else if (comp instanceof Sequence) {
				ws.add((Sequence)comp);
			}
			ws.organize();
		}
		return ws;
	}

	public static void select (Workspace ws) {
		Schedule.inEDT(Schedule.TYPE_DELAY + Schedule.CLICK_DELAY, () -> DBTree.instance.selectWorkspaceByName(ws.toString()));
	}

	private String name;
	private final DB database;

	private final List<SchemaReference> schemas = new ArrayList<>();
	private final List<RelationReference> relations = new ArrayList<>();
	private final List<FunctionReference> functions = new ArrayList<>();
	private final List<ViewReference> views = new ArrayList<>();
	private final List<SequenceReference> sequences = new ArrayList<>();
	private final List<ConstraintReference> constraints = new ArrayList<>();
	private final List<TriggerReference> triggers = new ArrayList<>();
	private final List<InheritanceReference> inheritances = new ArrayList<>();

	public Workspace(String name, DB database){
		this.name = name;
		this.database = database;
	}

	@Override
	public String getName() {
		return name;
	}

	public DB getDatabase() {
		return database;
	}

	public List<SchemaReference> getSchemas() {
        return schemas;
    }

    public List<RelationReference> getRelations() {
        return relations;
    }

    public List<FunctionReference> getFunctions() {
        return functions;
    }

    public List<ViewReference> getViews() {
        return views;
    }

    public List<SequenceReference> getSequences() {
        return sequences;
    }

    public List<ConstraintReference> getConstraints() {
        return constraints;
    }

    public List<TriggerReference> getTriggers() {
        return triggers;
    }

    public List<InheritanceReference> getInheritances() {
        return inheritances;
    }

    public RelationReference add(Relation rel){
		if(rel == null) {
			return null;
		}
		RelationReference reference = find(rel);
		if (reference == null) {
			SchemaReference sShadow = find(rel.getSchema());
			if(sShadow == null){
				sShadow = new SchemaReference(this, rel.getSchema());
				schemas.add(sShadow);
			}
			reference = new RelationReference(this, rel);
			relations.add(reference);
			sShadow.getRelations().add(reference);
			addConstraints(rel);
		}
		return reference;
	}

	public FunctionReference add(Function func){
		if(func == null) {
			return null;
		}
		FunctionReference reference = find(func);
		if (reference == null) {
			SchemaReference sShadow = find(func.getSchema());
			if(sShadow == null){
				sShadow = new SchemaReference(this, func.getSchema());
				schemas.add(sShadow);
			}
			reference = new FunctionReference(this, func);
			functions.add(reference);
			sShadow.getFunctions().add(reference);
			addTriggers(func);
		}
		return reference;
	}

	public ViewReference add(View view){
		if(view == null) {
			return null;
		}
		ViewReference reference = find(view);
		if(reference == null){
			SchemaReference sShadow = find(view.getSchema());
			if(sShadow == null){
				sShadow = new SchemaReference(this, view.getSchema());
				schemas.add(sShadow);
			}
			reference = new ViewReference(this, view);
			views.add(reference);
			sShadow.getViews().add(reference);
		}
		return reference;
	}

	public SequenceReference add(Sequence seq){
		if(seq == null) {
			return null;
		}
		SequenceReference reference = find(seq);
		if(reference == null){
			SchemaReference sShadow = find(seq.getSchema());
			if(sShadow == null){
				sShadow = new SchemaReference(this, seq.getSchema());
				schemas.add(sShadow);
			}
			reference = new SequenceReference(this, seq);
			sequences.add(reference);
			sShadow.getSequences().add(reference);
		}
		return reference;
	}

	public void remove(Relation rel){
		RelationReference relRef = find(rel);
		if(relRef != null){
			relRef.drop();
		}
	}

	public void remove(Function func){
		FunctionReference funcRef = find(func);
		if(funcRef != null){
			funcRef.drop();
		}
	}

	public void remove(View view){
		ViewReference viewRef = find(view);
		if(viewRef != null){
			viewRef.drop();
		}
	}

	public void remove(Sequence seq){
		SequenceReference seqRef = find(seq);
		if(seqRef != null){
			seqRef.drop();
		}
	}

	public SchemaReference find(Schema schema){
		for (SchemaReference schemaRef : schemas) {
			if (schemaRef.getElement().equals(schema)) {
				return schemaRef;
			}
		}
		return null;
	}
	public RelationReference find(Relation relation){
		for (RelationReference relRef : relations) {
			if (relRef.getElement().equals(relation)) {
				return relRef;
			}
		}
		return null;
	}
	public FunctionReference find(Function function){
		for (FunctionReference funcRef : functions) {
			if (funcRef.getElement().equals(function)) {
				return funcRef;
			}
		}
		return null;
	}
	public ViewReference find(View view){
		for (ViewReference viewRef : views) {
			if (viewRef.getElement().equals(view)) {
				return viewRef;
			}
		}
		return null;
	}
	public SequenceReference find(Sequence seq){
		for (SequenceReference seqRef : sequences) {
			if (seqRef.getElement().equals(seq)) {
				return seqRef;
			}
		}
		return null;
	}

	final void organize(){
		OrganizerFactory.getAlphabetical().organize(this);
	}

	@Override
	public Icon getIcon16(){
		return ico16;
	}
	@Override
	public String toString(){
		if(database != null && database.getProject() != null && database.getProject().getDatabases().size() > 1){
			return name+" ("+database.getName()+")";
		}else{
			return name;
		}
	}
	@Override public void clicked(){ database.clicked(); }
	@Override public void doubleClicked(){ database.doubleClicked(); }
	@Override public void rightClicked(){
		RightClickMenu.get((type, selectedValue) -> {
            switch(type){
                case 10: rename(); break;
                case 61:
                    drop();
                    SearchPanel.instance.updateDbTree();
                    break;
				default: throw new IllegalArgumentException("Menu option " + type + " is not known");
            }
        }).
		addItem("Rename", RightClickMenu.ICO_EDIT, 10).
		separator().
		addItem("DROP...", RightClickMenu.ICO_DELETE, 61);
	}

	public DefaultMutableTreeNode getTreeView(){
		DefaultMutableTreeNode db = new DefaultMutableTreeNode(this);
		for (RelationReference relation : new ArrayList<>(relations)) {
			db.add(relation.getTreeView());
		}
		for (FunctionReference function : new ArrayList<>(functions)) {
			db.add(function.getTreeView());
		}
		for (ViewReference view : new ArrayList<>(views)) {
			db.add(view.getTreeView());
		}
		for (SequenceReference seq : new ArrayList<>(sequences)) {
			db.add(seq.getTreeView());
		}
		return db;
	}

	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching) {
		DefaultMutableTreeNode schemaNode = new DefaultMutableTreeNode(this);

		for(RelationReference rel: new ArrayList<>(relations)){
			DefaultMutableTreeNode child = rel.getTreeView(search, fulltext, searchNotMatching);
			if(child != null) {
				schemaNode.add(child);
			}
		}
		for(FunctionReference func: new ArrayList<>(functions)){
			DefaultMutableTreeNode child = func.getTreeView(search, fulltext, searchNotMatching);
			if(child != null) {
				schemaNode.add(child);
			}
		}
		for(ViewReference view: new ArrayList<>(views)){
			DefaultMutableTreeNode child = view.getTreeView(search, fulltext, searchNotMatching);
			if(child != null) {
				schemaNode.add(child);
			}
		}
		for(SequenceReference seq : new ArrayList<>(sequences)){
			DefaultMutableTreeNode child = seq.getTreeView(search, fulltext, searchNotMatching);
			if(child != null) {
				schemaNode.add(child);
			}
		}

		boolean nameMatch = name.matches(search);
		boolean excludedByName = searchNotMatching && nameMatch;
		if (!excludedByName && (!schemaNode.isLeaf() || searchNotMatching || nameMatch)) {
			return schemaNode;
		} else {
			return null;
		}
	}

	public DB getDB(){
		return database;
	}

	public void addConstraints(Relation rel){
		for(Constraint con : rel.getConstraints()){
			if(con.getRel1() != null && con.getRel2() != null){
				RelationReference rel1 = find((Relation) con.getRel1());
				RelationReference rel2 = find((Relation) con.getRel2());
				if(rel1 != null && rel2 != null){
					constraints.add(new ConstraintReference(this, con, rel1, rel2));
				}
			}
		}
		for(Inheritance inh : rel.getInheritances()){
			if(inh.getRel1() != null && inh.getRel2() != null){
				RelationReference rel1 = find((Relation) inh.getRel1());
				RelationReference rel2 = find((Relation) inh.getRel2());
				if(rel1 != null && rel2 != null){
					inheritances.add(new InheritanceReference(this, inh, rel1, rel2));
				}
			}
		}
	}

	public void addTriggers(Function func){
		for(Trigger trig : func.getTriggers()){
			if(trig.getRel1() != null && trig.getRel2() != null){
				RelationReference rel1 = find((Relation) trig.getRel1());
				FunctionReference rel2 = find((Function) trig.getRel2());
				if(rel1 != null && rel2 != null){
					triggers.add(new TriggerReference(this, trig, rel1, rel2));
				}
			}
		}
	}

	public void drop(){
		Project.getCurrent().getWorkspaces().remove(this);
		Canvas.instance.setSelectedElement(null);
		DBTree.instance.selectCurrentDB();
	}

	private void rename(){
		String val = JOptionPane.showInputDialog(DesignGUI.get().frame, "New name", name);
		if(val!=null && !val.isEmpty()){
			name = val;
			for(DraggableComponent comp : new ArrayList<>(schemas)){
				comp.setLocation(comp.getLocation());
			}
			for(DraggableComponent comp : new ArrayList<>(relations)){
				comp.setLocation(comp.getLocation());
			}
			for(DraggableComponent comp : new ArrayList<>(functions)){
				comp.setLocation(comp.getLocation());
			}
			getDB().getProject().save();
			SearchPanel.instance.updateDbTree();
		}
	}

}
