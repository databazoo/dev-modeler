package com.databazoo.devmodeler.model;

import com.databazoo.components.GCFrame;
import com.databazoo.components.UIConstants;
import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.components.text.SelectableText;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.DBCommException;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.SQLOutputConfig;
import com.databazoo.devmodeler.conn.SQLOutputConfigExport;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.DBTree;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.RightClickMenu;
import com.databazoo.devmodeler.gui.SearchPanel;
import com.databazoo.devmodeler.model.reference.SequenceReference;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.tools.Geometry;
import com.databazoo.devmodeler.tools.comparator.Comparator;
import com.databazoo.devmodeler.wizards.relation.RelationWizard;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;
import com.databazoo.tools.Usage;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.databazoo.devmodeler.gui.UsageElement.COPY_FULL;
import static com.databazoo.devmodeler.gui.UsageElement.SEQUENCE_CONTEXT_COPY;
import static com.databazoo.devmodeler.gui.UsageElement.SEQUENCE_CONTEXT_DROP;
import static com.databazoo.devmodeler.gui.UsageElement.SEQUENCE_CONTEXT_EDIT;
import static com.databazoo.devmodeler.gui.UsageElement.SEQUENCE_CONTEXT_SOURCE;
import static com.databazoo.devmodeler.gui.UsageElement.SEQUENCE_CONTEXT_WORKSPACE;
import static com.databazoo.devmodeler.gui.UsageElement.SEQUENCE_DOUBLE_CLICKED;
import static com.databazoo.devmodeler.gui.UsageElement.SRC_REMOTE;
import static com.databazoo.devmodeler.gui.UsageElement.WS_REMOVE;

/**
 * Model representation of sequences.
 *
 * @author bobus
 */
public class Sequence extends DraggableComponent implements IModelElement {
	public static final String L_CLASS = "Sequence";
	public static final Icon ico16 = Theme.getSmallIcon(Theme.ICO_SEQUENCE);
	public static final Color BG_COLOR = Color.decode("#E565E5");

	private Behavior behavior = new Behavior();
	private final List<Attribute> attributes = new ArrayList<>();
	private final Set<IModelElement> elements = new HashSet<>();
	private int isDifferent = Comparator.NO_DIFF;

	public Sequence(Schema parent, String fullName, String[] depend, String increment, String min, String max, String currentValue, boolean cycle, String descr) {
		super();
		if(fullName.contains(".")){
			behavior.name = fullName.replaceFirst(Pattern.quote(parent.getSchemaPrefix()), "");
			behavior.schemaName = parent.getName();
		}else{
			behavior.name = fullName;
		}
		behavior.increment = increment;
		behavior.min = min;
		behavior.max = max;
		behavior.current = currentValue;
		behavior.descr = descr;
		behavior.cycle = cycle;
		this.schemaContainer = parent;

		setAttributesByDependance(depend);

		draw();
	}

	public List<Attribute> getAttributes() {
		return attributes;
	}

	private void draw(){
		setLayout(null);
		setSize(new Dimension(Canvas.DEFAULT_ENTITY_WIDTH, 20));
		setBackground(BG_COLOR);
		setForeground(Color.BLACK);
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		checkSize();
	}

	@Override
	public void checkConstraints(){
		schemaContainer.checkSize(this);
	}

	@Override
	public void clicked(){
		if(!isSelected){
			if(attributes.isEmpty()){
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
				scrollIntoView();
				Canvas.instance.setSelectedElement(this);
				DBTree.instance.selectRelationByName(schemaContainer.getDB().getName(), schemaContainer.getName(), toString());
			}else{
				attributes.get(0).clicked();
			}
		}
	}

	@Override
	public void doubleClicked(){
		Usage.log(SEQUENCE_DOUBLE_CLICKED, "View: " + DesignGUI.getView().name());
		if(!attributes.isEmpty()){
			RelationWizard.get(null).drawProperties(attributes.get(0).getRel(), toString());
		}else{
			RelationWizard.get(null).drawProperties(this);
		}
	}

	@Override
	public void rightClicked(){
		rightClicked(null);
	}

	@Override
	public void rightClicked(final String workspaceName){
		RightClickMenu menu = RightClickMenu.get((type, selectedValue) -> {
            Workspace ws;
            switch(type){
                case 10:
					Usage.log(SEQUENCE_CONTEXT_EDIT);
                	RelationWizard.get(workspaceName).drawProperties(Sequence.this);
                	break;
                case 30:
					Usage.log(SEQUENCE_CONTEXT_COPY);
                	DesignGUI.toClipboard(Sequence.this.getName());
                	break;
                case 31:
					Usage.log(SEQUENCE_CONTEXT_COPY, COPY_FULL);
                	DesignGUI.toClipboard(Sequence.this.getFullName());
                	break;
                case 32:
					Usage.log(SEQUENCE_CONTEXT_SOURCE);
                	DesignGUI.toClipboard(Sequence.this.getQueryCreateClear(getConnection()));
                	break;
                case 39:
					Usage.log(SEQUENCE_CONTEXT_SOURCE, SRC_REMOTE);
                    if(selectedValue.equals("Model")){
                        DesignGUI.toClipboard(Sequence.this.getQueryCreateClear(getConnection()));
                    }else{
						Schedule.inWorker(() -> {
                            try {
                                Project p = getDB().getProject();
                                IConnection c = p.getDedicatedConnection(getDB().getName(), p.getConnectionByName(selectedValue).getName());
                                if (c != null) {
                                    DesignGUI.toClipboard(c.loadDDLFromDB(Sequence.this));
                                }
                            } catch (DBCommException ex) {
								Dbg.fixme("Getting sequence DDL failed.", ex);
                                JOptionPane.showMessageDialog(GCFrame.getActiveWindow(), new SelectableText("Loading DDL failed\n\n" + ex.getMessage(), false), "Loading DDL failed", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                    break;
                case 61:
					Usage.log(SEQUENCE_CONTEXT_DROP);
                	RelationWizard.get(workspaceName).enableDrop().drawProperties(Sequence.this);
                	break;
                case 70:
					Usage.log(SEQUENCE_CONTEXT_WORKSPACE, WS_REMOVE);
                    String wsName = selectedValue.replace("Remove from ", "");
                    Project.getCurrent().getWorkspaceByName(wsName).remove(Sequence.this);
                    break;
                case 71:
					Usage.log(SEQUENCE_CONTEXT_WORKSPACE);
                    if(selectedValue.equals("Create new workspace")){
                        ws = Workspace.create(Sequence.this);
                    }else{
                        ws = Project.getCurrent().getWorkspaceByName(selectedValue);
                        ws.add(Sequence.this)
								.setLocation(Geometry.getSnappedPosition(Canvas.GRID_SIZE, ws.find(getSchema()).getHeight()));
                    }
                    SearchPanel.instance.updateDbTree();
                    Workspace.select(ws);
                    break;
				default: throw new IllegalArgumentException("Menu option " + type + " is not known");
            }
        }).
		addItem("Properties", RightClickMenu.ICO_EDIT, 10).
		separator().
		addItem("Copy name", RightClickMenu.ICO_COPY, 30).
		addItem("Copy name with schema", RightClickMenu.ICO_COPY, 31);

		if(!getDB().getProject().getCurrentConn().isSupported(SupportedElement.GENERATE_DDL)) {
			menu.addItem("Copy source code", RightClickMenu.ICO_COPY, 32);
		}else {
			menu.addItem("Copy source code from", RightClickMenu.ICO_COPY, 39, Geometry.concat(new String[]{"Model"}, Project.getCurrent().getConnectionNames()));
		}

		if(workspaceName != null){
			menu.separator().
			addItem("Remove from " + workspaceName, RightClickMenu.ICO_DELETE, 70);
		}else{
			menu.separator().
			addItem("Add to workspace", Workspace.ico16, 71, Geometry.concat(new String[]{"Create new workspace"}, Project.getCurrent().getWorkspaceNames(getDB())));
		}
		menu.separator().
		addItem("DROP...", RightClickMenu.ICO_DELETE, 61);
	}

	@Override
	protected void mouseUp(){
		schemaContainer.checkSize();
	}

	@Override
	public DefaultMutableTreeNode getTreeView(boolean showCreateIcons){
		return getTreeView(new DefaultMutableTreeNode(this));
	}

	public DefaultMutableTreeNode getTreeView(DefaultMutableTreeNode sequenceNode){
		return sequenceNode;
	}

	@Override
	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching) {
		return getTreeView(search, fulltext, searchNotMatching, new DefaultMutableTreeNode(this));
	}

	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching, DefaultMutableTreeNode sequenceNode) {
		boolean nameMatch = getFullName().matches(search);
		boolean excludedByName = searchNotMatching && nameMatch;
		if (!excludedByName && (searchNotMatching || nameMatch || (
				fulltext && getDB().getProject().getCurrentConn().getQueryCreate(this, null).matches(search)
		))) {
			return getTreeView(sequenceNode);
		} else {
			return null;
		}
	}

	@Override
	public String getName(){
		return (behavior.name);
	}

	@Override
	public String getFullName(){
		return (behavior.schemaName.isEmpty() || !getDB().getConnection().isSupported(SupportedElement.SCHEMA) ? "" : behavior.schemaName+".") + behavior.name;
	}

	@Override
	public String getEditedFullName() {
		Behavior edit = behavior.valuesForEdit;
		return (edit.schemaName.isEmpty() || !getDB().getConnection().isSupported(SupportedElement.SCHEMA) ? "" : edit.schemaName+".") + edit.name;
	}

	@Override
	public String getQueryCreate(IConnection conn) {
		return conn.getQueryCreate(this, behavior.isNew ? null : SQLOutputConfig.WIZARD);
	}
	@Override
	public String getQueryCreateClear(IConnection conn) {
		return conn.getQueryCreate(this, null);
	}
	@Override
	public String getQueryChanged(IConnection conn) {
		if(behavior.isNew){
			Behavior o = behavior;
			behavior = behavior.valuesForEdit;
			String ret = conn.getQueryCreate(this, null);
			behavior = o;
			return ret;
		}else{
			return conn.getQueryChanged(this);
		}
	}
	@Override
	public String getQueryChangeRevert(IConnection conn) {
		if(behavior.isNew){
			behavior.isDropped = true;
		}else if(behavior.valuesForEdit.isDropped){
			return conn.getQueryCreate(this, null);
		}
		Behavior o = behavior;
		behavior = behavior.valuesForEdit;
		behavior.valuesForEdit = o;
		String change = conn.getQueryChanged(this);
		behavior = o;
		return change;
	}
	@Override
	public String getQueryRecursive(SQLOutputConfigExport config) throws SQLOutputConfigExport.LimitReachedException {
		if(config.exportSequences){
			config.updateLimit();
			config.append((config.conn != null ? config.conn : getConnection()).getQueryCreate(this, config));
		}

		return config.getText();
	}

	@Override
	public String getQueryDrop(IConnection conn) {
		return conn.getQueryDrop(this);
	}

	@Override
	public void setDifferent(int isDifferent) {
		this.isDifferent = isDifferent;
	}
	@Override
	public int getDifference(){
		return isDifferent;
	}

	@Override
	public void drop(){
		schemaContainer.remove(this);
		schemaContainer.getSequences().remove(this);
		getDB().getProject().getWorkspaces()
				.forEach(workspace -> workspace.getSequences().stream()
						.filter(reference -> reference.getElement() == this)
						.collect(Collectors.toList())
						.forEach(SequenceReference::drop)
				);
	}

	@Override
	public void checkSize(){
		displayName = behavior.name;
		displayNameWidth = UIConstants.GRAPHICS.getFontMetrics(Canvas.getTitleFont()).stringWidth(displayName);
	}

	@Override
	public void unSelect(){
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		Workspace ws = Project.getCurrent().getOldWorkspace();
		if(ws != null){
			for(SequenceReference rel : ws.getSequences()){
				if(rel.getElement().equals(this)){
					rel.unSelect();
					break;
				}
			}
		}
	}

	@Override
	public IConnection getConnection(){
		return schemaContainer.getConnection();
	}

	@Override
	public DB getDB(){
		return schemaContainer.getDB();
	}

	@Override
	public Icon getIcon16(){
		return ico16;
	}
	@Override
	public String toString(){
		return displayName;
	}

	@Override
	public String getDescr(){
		if(behavior.descr != null && !behavior.descr.isEmpty()){
			return behavior.descr;
		}else{
			return "";
		}
	}

	@Override
	public Set<IModelElement> getAllSubElements() {
		if(elements.isEmpty()) {
			synchronized (this) {
				if(elements.isEmpty()) {
					elements.add(this);
				}
			}
		}
		return elements;
	}

	@Override
	public boolean isNew(){
		return behavior.isNew;
	}

	public void assignToSchemaAndAttributes(){
		schemaContainer.getSequences().remove(this);
		schemaContainer.getSequences().add(this);
		String[] tmp = new String[attributes.size()];
		for(int i=0; i<tmp.length; i++){
			Attribute attr = attributes.get(i);
			tmp[i] = attr.getRel().getFullName()+"."+attr.getName();
			//Dbg.info("Will reassign to "+tmp[i]);
		}
		setAttributesByDependance(tmp);
		attributes.stream().filter(attr -> attr != null).forEach(attr -> attr.setSequence(this));
	}

	public void assignToSchemaAndAttributes(Schema schema){
		this.schemaContainer = schema;
		assignToSchemaAndAttributes();
	}

	@Override
	public int compareTo(IModelElement t) {
		return getName().compareTo(t.getName());
	}

	@Override
	public String getFullPath(){
		return getDB().getName()+"."+behavior.schemaName;
	}

	@Override
	public String getClassName(){
		return L_CLASS;
	}

	@Override
	public Behavior getBehavior(){
		return behavior;
	}

	@Override
	public void setBehavior(IModelBehavior behavior) {
		this.behavior = (Behavior) behavior;
	}

	public final void setAttributesByDependance(String[] depend) {
		attributes.clear();
		for(String depObj : depend) {
			//Dbg.toFile(depObj);
			String depSchema, depTable, depAttr;
			String[] depParts = depObj.split("\\.");
			if(depParts.length == 2){
				depSchema = "public";
				depTable = depParts[0];
				depAttr = depParts[1];
			}else if(depParts.length == 3){
				depSchema = depParts[0];
				depTable = depParts[1];
				depAttr = depParts[2];
			}else{
				Dbg.toFile("Can not set sequence dependency to "+depObj);
				return;
			}
			schemaSearch:
			for(Schema schema : getDB().getSchemas()){
				if(schema.getName().equals(depSchema)) {
					for(Relation rel : schema.getRelations()){
						if(rel.getName().equals(depTable)){
							attributes.add(rel.getAttributeByName(depAttr));
							break schemaSearch;
						}
					}
				}
			}
		}
	}

	public String getDependances(){
		String ret = "";
		String comma = "";
		for(Attribute attr: attributes){
			if(attr != null){
				ret += comma + attr.getRel().getFullName()+"."+attr.getName();
				comma = ",";
			}
		}
		return ret;
	}

	public class Behavior extends AbstractModelBehavior<Behavior> {

		public static final String L_NAME = "Name";
		public static final String L_SCHEMA = "Schema";
		public static final String L_OPERATIONS = "Actions";
		public static final String L_DESCR = "Comment";
		public static final String L_MIN = "Minimum";
		public static final String L_MAX = "Maximum";
		public static final String L_START = "Current value";
		public static final String L_INCREMENT = "Increment by";
		public static final String L_CYCLE = "Cycle on last";

		private String name;
		private String schemaName = "";
		private String increment = "1";
		private String min = "1";
		private String max = "9223372036854775807";
		private String current = "1";
		private boolean cycle = true;
		private String descr = "";

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getSchemaName() {
			return schemaName;
		}

		public void setSchemaName(String schemaName) {
			this.schemaName = schemaName;
		}

		public String getIncrement() {
			return increment;
		}

		public void setIncrement(String increment) {
			this.increment = increment;
		}

		public String getMin() {
			return min;
		}

		public void setMin(String min) {
			this.min = min;
		}

		public String getMax() {
			return max;
		}

		public void setMax(String max) {
			this.max = max;
		}

		public String getCurrent() {
			return current;
		}

		public void setCurrent(String current) {
			this.current = current;
		}

		public boolean isCycle() {
			return cycle;
		}

		public void setCycle(boolean cycle) {
			this.cycle = cycle;
		}

		public String getDescr() {
			return descr != null ? descr : "";
		}

		public void setDescr(String descr) {
			this.descr = descr;
		}

		@Override
		public Behavior prepareForEdit(){
			valuesForEdit = new Behavior();
			valuesForEdit.name = name;
			valuesForEdit.schemaName = schemaName;
			valuesForEdit.increment = increment;
			valuesForEdit.min = min;
			valuesForEdit.max = max;
			valuesForEdit.current = current;
			valuesForEdit.cycle = cycle;
			valuesForEdit.descr = descr;
			return valuesForEdit;
		}

		@Override
		public void saveEdited(){
			if(isDropped){
				drop();
			}else{
				boolean schemaChanged = !behavior.schemaName.equals(schemaName);
				behavior.name = name;
				behavior.schemaName = schemaName;
				behavior.increment = increment;
				behavior.min = min;
				behavior.max = max;
				behavior.current = current;
				behavior.cycle = cycle;
				behavior.descr = descr;
				if(behavior.isNew){
					setSchemaByName(schemaName);
					assignToSchemaAndAttributes();
					behavior.isNew = false;
					behavior.isDropped = false;
				}else{
					if(schemaChanged){
						getSchema().getSequences().remove(Sequence.this);
						setSchemaByName(schemaName);
						assignToSchemaAndAttributes();
					}
				}
			}
			getDB().getProject().save();
		}

		@Override
		public void notifyChange(String elementName, String value) {
			switch (elementName) {
				case L_NAME:
					name = getConnection().isSupported(SupportedElement.ALL_UPPER) ? value.toUpperCase() : value;
					break;
				case L_SCHEMA:
					schemaName = value.equals("public") ? "" : value;
					break;
				case L_DESCR:
					descr = value;
					break;
				case L_MIN:
					min = value.replaceAll("[^0-9]+", "");
					break;
				case L_MAX:
					max = value.replaceAll("[^0-9]+", "");
					break;
				case L_START:
					current = value.replaceAll("[^0-9]+", "");
					break;
				case L_INCREMENT:
					increment = value.replaceAll("[^0-9]+", "");
					break;
			}
		}

		@Override
		public void notifyChange(String elementName, boolean value) {
			if(elementName.equals(L_CYCLE)){
				cycle = value;
			}
		}

		@Override
		public void notifyChange(String elementName, boolean[] values) {
			if(elementName.equals(L_OPERATIONS)){
				cycle = values[0];
				isDropped = values[values.length-1];
			}
		}

		private void setSchemaByName(String schemaName){
			if(schemaName.isEmpty()){
				schemaName = "public";
			}
			Schema schema = getDB().getSchemaByFullName(schemaName);
			if (schema != null) {
				schemaContainer = schema;
			}
		}
	}
}
