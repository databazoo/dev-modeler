
package com.databazoo.devmodeler.model;

import com.databazoo.components.GCFrame;
import com.databazoo.components.UIConstants;
import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.components.text.SelectableText;
import com.databazoo.devmodeler.config.Settings;
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
import com.databazoo.devmodeler.gui.view.ViewMode;
import com.databazoo.devmodeler.gui.window.datawindow.DataWindow;
import com.databazoo.devmodeler.model.reference.ViewReference;
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
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.databazoo.devmodeler.gui.UsageElement.COPY_FULL;
import static com.databazoo.devmodeler.gui.UsageElement.SRC_REMOTE;
import static com.databazoo.devmodeler.gui.UsageElement.VIEW_CONTEXT_COPY;
import static com.databazoo.devmodeler.gui.UsageElement.VIEW_CONTEXT_DATA;
import static com.databazoo.devmodeler.gui.UsageElement.VIEW_CONTEXT_DROP;
import static com.databazoo.devmodeler.gui.UsageElement.VIEW_CONTEXT_EDIT;
import static com.databazoo.devmodeler.gui.UsageElement.VIEW_CONTEXT_SOURCE;
import static com.databazoo.devmodeler.gui.UsageElement.VIEW_CONTEXT_WORKSPACE;
import static com.databazoo.devmodeler.gui.UsageElement.VIEW_DOUBLE_CLICKED;
import static com.databazoo.devmodeler.gui.UsageElement.WS_REMOVE;

/**
 * Model representation of views.
 * Displayed on canvas as a draggable component.
 *
 * @author bobus
 */
public class View extends DraggableComponent implements IModelElement {
	public static final String L_CLASS = "View";
	public static final Icon ico16 = Theme.getSmallIcon(Theme.ICO_VIEW);
	public static final Color BG_COLOR = Color.decode("#8899FF");

	private final Set<IModelElement> elements = new HashSet<>();
	private Behavior behavior = new Behavior();
	private int isDifferent = Comparator.NO_DIFF;

	public View(Schema parent, String fullName, boolean isMaterialized, String src, String descr) {
		super();
		if(fullName.contains(".")){
			behavior.name = fullName.replaceFirst(Pattern.quote(parent.getSchemaPrefix()), "");
			behavior.schemaName = parent.getName();
		}else{
			behavior.name = fullName;
		}
		behavior.isMaterialized = isMaterialized;
		behavior.src = src.trim();
		behavior.descr = descr;
		this.schemaContainer = parent;

		String forceEOL = Settings.getStr(Settings.L_FONT_TEXT_EOL);
		if(forceEOL != null && !forceEOL.isEmpty()) {
			behavior.src = behavior.src.replaceAll("\r?\n", forceEOL);
		}
		draw();
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
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			scrollIntoView();
			Canvas.instance.setSelectedElement(this);
			DBTree.instance.selectRelationByName(schemaContainer.getDB().getName(), schemaContainer.getName(), toString());
		}
	}

	@Override
	public void doubleClicked(){
		Usage.log(VIEW_DOUBLE_CLICKED, "View: " + DesignGUI.getView().name());
		if(DesignGUI.getView() == ViewMode.DATA){
			DataWindow.get().setLimit(Settings.getInt(Settings.L_DATA_CONTEXT_LIMIT)).drawViewData(this);
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
					Usage.log(VIEW_CONTEXT_EDIT);
                	RelationWizard.get(workspaceName).drawProperties(View.this);
                	break;
                case 20:
					Usage.log(VIEW_CONTEXT_DATA);
                	DataWindow.get().setLimit(Settings.getInt(Settings.L_DATA_CONTEXT_LIMIT)).drawViewData(View.this);
                	break;
                case 30:
					Usage.log(VIEW_CONTEXT_COPY);
                	DesignGUI.toClipboard(View.this.getName());
                	break;
                case 31:
					Usage.log(VIEW_CONTEXT_COPY, COPY_FULL);
                	DesignGUI.toClipboard(View.this.getFullName());
                	break;
                case 32:
					Usage.log(VIEW_CONTEXT_SOURCE);
                	DesignGUI.toClipboard(View.this.getQueryCreateClear(getConnection()));
                	break;
                case 39:
					Usage.log(VIEW_CONTEXT_SOURCE, SRC_REMOTE);
                    if(selectedValue.equals("Model")){
                        DesignGUI.toClipboard(View.this.getQueryCreateClear(getConnection()));
                    }else{
						Schedule.inWorker(() -> {
                            try {
                                Project p = getDB().getProject();
                                IConnection c = p.getDedicatedConnection(getDB().getName(), p.getConnectionByName(selectedValue).getName());
                                if (c != null) {
                                    DesignGUI.toClipboard(c.loadDDLFromDB(View.this));
                                }
                            } catch (DBCommException ex) {
								Dbg.fixme("Getting view DDL failed.", ex);
                                JOptionPane.showMessageDialog(GCFrame.getActiveWindow(), new SelectableText("Loading DDL failed\n\n" + ex.getMessage(), false), "Loading DDL failed", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                    break;
                case 61:
					Usage.log(VIEW_CONTEXT_DROP);
                	RelationWizard.get(workspaceName).enableDrop().drawProperties(View.this);
                	break;
                case 70:
					Usage.log(VIEW_CONTEXT_WORKSPACE, WS_REMOVE);
                    String wsName = selectedValue.replace("Remove from ", "");
                    Project.getCurrent().getWorkspaceByName(wsName).remove(View.this);
                    break;
                case 71:
					Usage.log(VIEW_CONTEXT_WORKSPACE);
                    if(selectedValue.equals("Create new workspace")){
                        ws = Workspace.create(View.this);
                    }else{
                        ws = Project.getCurrent().getWorkspaceByName(selectedValue);
                        ws.add(View.this)
								.setLocation(Geometry.getSnappedPosition(Canvas.GRID_SIZE, ws.find(getSchema()).getHeight()));
                    }
                    SearchPanel.instance.updateDbTree();
                    Workspace.select(ws);
                    break;
				default: throw new IllegalArgumentException("Menu option " + type + " is not known");
            }
        }).
			addItem("Properties", RightClickMenu.ICO_EDIT, 10);
		if(getDB().getProject().getType() != Project.TYPE_ABSTRACT){
			menu.separator().
			addItem("First "+Settings.getInt(Settings.L_DATA_CONTEXT_LIMIT)+" rows", RightClickMenu.ICO_DATA, 20);
		}
		menu.separator().
		addItem("Copy name", RightClickMenu.ICO_COPY, 30).
		addItem("Copy name with schema", RightClickMenu.ICO_COPY, 31);

		if(!getDB().getProject().getCurrentConn().isSupported(SupportedElement.GENERATE_DDL)) {
			menu.addItem("Copy source code", RightClickMenu.ICO_COPY, 32);
		}else{
			menu.addItem("Copy source code from", RightClickMenu.ICO_COPY, 39, Geometry.concat(new String[]{"Model"}, Project.getCurrent().getConnectionNames()));
		}

		if(workspaceName != null){
			menu.separator().
			addItem("Remove from " + workspaceName, RightClickMenu.ICO_DELETE, 70);
		}else{
			menu.separator().
			addItem("Add to workspace", Workspace.ico16, 71, Geometry.concat(new String[]{"Create new workspace"}, Project.getCurrent().getWorkspaceNames(getDB())));
		}
		menu.
		separator().
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

	public DefaultMutableTreeNode getTreeView(DefaultMutableTreeNode viewNode){
		return viewNode;
	}

	@Override
	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching) {
		return getTreeView(search, fulltext, searchNotMatching, new DefaultMutableTreeNode(this));
	}

	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching, DefaultMutableTreeNode viewNode) {
		boolean nameMatch = getFullName().matches(search);
		boolean excludedByName = searchNotMatching && nameMatch;
		if (!excludedByName && (searchNotMatching || nameMatch || (
				fulltext && getDB().getProject().getCurrentConn().getQueryCreate(this, null).matches(search)
		))) {
			return getTreeView(viewNode);
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
	public String getEditedFullName(){
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
			String ret =  conn.getQueryCreate(this, null);
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
		}else if(behavior.valuesForEdit.isDropped()){
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
		if(config.exportViews){
			config.updateLimit();
			config.append((config.conn != null ? config.conn : getConnection()).getQueryCreate(this, config));
		}

		return config.getText();
	}

	@Override
	public String getQueryDrop(IConnection conn) {
		return conn.getQueryDrop(this);
	}

	public boolean getMaterialized(){
		return behavior.isMaterialized;
	}

	public String getSrc(){
		return behavior.src;
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
		schemaContainer.getViews().remove(this);
		getDB().getProject().getWorkspaces()
				.forEach(workspace -> workspace.getViews().stream()
						.filter(reference -> reference.getElement() == this)
						.collect(Collectors.toList())
						.forEach(ViewReference::drop)
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
			for(ViewReference rel : ws.getViews()){
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

	public void assignToSchema(){
		schemaContainer.getViews().add(this);
	}

	public void assignToSchema(Schema schema){
		this.schemaContainer = schema;
		assignToSchema();
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

	public class Behavior extends AbstractModelBehavior<Behavior> {

		public static final String L_NAME = "Name";
		public static final String L_SCHEMA = "Schema";
		//public static final String L_BEHAVIOR = "Behavior";
		public static final String L_OPERATIONS = "Actions";
		public static final String L_DESCR = "Comment";
		public static final String L_BODY = "Source";
		public static final String L_MATERIALIZED = "Materialized";

		private String name;
		private String schemaName = "";
		private boolean isMaterialized;
		private String src = "";
		private String descr = "";

		private Behavior() {}
		private Behavior(String schemaName, String name, String src, String descr){
			this.name = name;
			this.schemaName = schemaName;
			this.src = src;
			this.descr = descr;
		}

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

		public boolean getMaterialized() {
			return isMaterialized;
		}

		public void setMaterialized(boolean isMaterialized) {
			this.isMaterialized = isMaterialized;
		}

		public String getSrc() {
			return src;
		}

		public void setSrc(String src) {
			this.src = src;
		}

		public String getDescr() {
			return descr != null ? descr : "";
		}

		public void setDescr(String descr) {
			this.descr = descr;
		}

		@Override
		public Behavior prepareForEdit(){
			return valuesForEdit = new Behavior(schemaName, name, src, descr);
		}

		@Override
		public void saveEdited(){
			if(isDropped){
				drop();
			}else{
				boolean schemaChanged = !behavior.schemaName.equals(schemaName);
				behavior.name = name;
				behavior.schemaName = schemaName;
				behavior.isMaterialized = isMaterialized;
				behavior.src = src;
				behavior.descr = descr;
				if(behavior.isNew){
					setSchemaByName(schemaName);
					assignToSchema();
					behavior.isNew = false;
					behavior.isDropped = false;
				}else{
					if(schemaChanged){
						getSchema().getViews().remove(View.this);
						setSchemaByName(schemaName);
						assignToSchema();
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
				case L_BODY:
					src = value;
					break;
			}
		}

		@Override
		public void notifyChange(String elementName, boolean value) {
			switch (elementName) {
				case L_MATERIALIZED:
					isMaterialized = value;
					break;
			}
		}

		@Override
		public void notifyChange(String elementName, boolean[] values) {
			if(elementName.equals(L_OPERATIONS)){
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
