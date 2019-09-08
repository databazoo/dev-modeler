package com.databazoo.devmodeler.model;

import com.databazoo.components.FontFactory;
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

import static com.databazoo.devmodeler.gui.UsageElement.COPY_FULL;
import static com.databazoo.devmodeler.gui.UsageElement.PACKAGE_CONTEXT_COPY;
import static com.databazoo.devmodeler.gui.UsageElement.PACKAGE_CONTEXT_DROP;
import static com.databazoo.devmodeler.gui.UsageElement.PACKAGE_CONTEXT_EDIT;
import static com.databazoo.devmodeler.gui.UsageElement.PACKAGE_CONTEXT_SOURCE;
import static com.databazoo.devmodeler.gui.UsageElement.PACKAGE_DOUBLE_CLICKED;
import static com.databazoo.devmodeler.gui.UsageElement.SRC_REMOTE;
import static com.databazoo.devmodeler.model.Schema.RESIZE_INITIAL_VALUE;

public class Package extends DraggableComponent implements IModelElement {
	public static final String L_CLASS = "Package";
	public static final Icon ico16 = Theme.getSmallIcon(Theme.ICO_SCHEMA);
	private static final Font HEAD_FONT = FontFactory.getSans(Font.BOLD + Font.ITALIC, 13);

	public final transient List<Function> functions = new ArrayList<>();

	private int isDifferent = Comparator.NO_DIFF;
	private Behavior behavior = new Behavior();

	public Package(Schema parent, String fullName, String descr) {
		super();
		this.schemaContainer = parent;
		if(fullName.contains(".")){
			behavior.name = fullName.replaceFirst(Pattern.quote(parent.getSchemaPrefix()), "");
			behavior.schemaName = parent.getName();
		}else{
			behavior.name = fullName;
		}
		behavior.schemaName = parent.getName();
		behavior.definition = "";
		behavior.body = "";
		behavior.descr = descr;
		draw();
	}

	public Package(Schema parent, String fullName, String src, String body, String descr) {
		super();
		this.schemaContainer = parent;
		if(fullName.contains(".")){
			behavior.name = fullName.replaceFirst(Pattern.quote(parent.getSchemaPrefix()), "");
			behavior.schemaName = parent.getName();
		}else{
			behavior.name = fullName;
		}
		behavior.schemaName = parent.getName();
		behavior.definition = src;
		behavior.body = body;
		behavior.descr = descr;
		draw();
	}

	private void draw() {
		setToolTipText("Package " + behavior.name);
		setLayout(null);
		overbearingZOrder = 1;
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D graphics = (Graphics2D) g;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int width = getWidth()-SHADOW_GAP+1;
		int height = getHeight();
		int gap = 1;

		if(isSelected){
			graphics.setColor(UIConstants.Colors.getSelectionBackground());
			graphics.drawRoundRect(1, 1, width - gap - 2, height - gap - 2, arcs.width-1, arcs.height-1);
			graphics.setColor(UIConstants.Colors.getSelectionBackground());
			graphics.drawRoundRect(2, 2, width - gap - 4, height - gap - 4, arcs.width-2, arcs.height-2);
			graphics.setColor(UIConstants.Colors.getSelectionBackground());
			graphics.drawRoundRect(3, 3, width - gap - 6, height - gap - 6, arcs.width-3, arcs.height-3);
		}
		graphics.setColor(Function.BG_COLOR);
		graphics.setStroke(Canvas.getBasicStroke());
		graphics.drawRoundRect(0, 0, width - gap, height - gap, arcs.width, arcs.height);

		graphics.setFont(HEAD_FONT);
		graphics.drawString(behavior.name, 16, 16);
	}

	public void assignToSchema(){
		schemaContainer.getPackages().add(this);
	}

	public void assignToSchema(Schema schema){
		this.schemaContainer = schema;
		assignToSchema();
	}

	/**
	 * Notify line components of location change
	 */
	@Override
	public void checkConstraints() {
		functions.forEach(Function::checkConstraints);
		schemaContainer.checkSize(this);
	}

	@Override
	public String getFullPath() {
		return getDB().getName()+"."+behavior.schemaName;
	}

	@Override
	public String getClassName() {
		return L_CLASS;
	}

	@Override
	public String getDescr() {
		if(behavior.descr != null && !behavior.descr.isEmpty()){
			return behavior.descr;
		}else{
			return "";
		}
	}

	@Override
	public Set<IModelElement> getAllSubElements() {
		Set<IModelElement> elements = new HashSet<>();
		elements.add(this);
		functions.forEach(elem -> elements.addAll(elem.getAllSubElements()));
		return elements;
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
		if(config.exportFunctions){
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
	public void drop() {
		for(int i=0; i<10000; i++){
			if(functions.isEmpty()){
				break;
			}
			functions.get(0).drop();
		}
		schemaContainer.remove(this);
		schemaContainer.getPackages().remove(this);
	}

	/**
	 * Recheck the size of the component
	 */
	@Override
	public void checkSize() {
		int minX = RESIZE_INITIAL_VALUE;
		int minY = RESIZE_INITIAL_VALUE;
		int maxX = 0;
		int maxY = 0;
		for(Function func: functions){
			Dimension s = func.getSize();
			Point l = func.getLocation();
			if(s.width+l.x > maxX){ maxX = s.width+l.x; }
			if(s.height+l.y > maxY){ maxY = s.height+l.y; }
			if(l.x < minX){ minX = l.x; }
			if(l.y < minY){ minY = l.y; }
		}

		if((minX != RESIZE_INITIAL_VALUE && minX > Canvas.DEFAULT_ENTITY_WIDTH) || (minY != RESIZE_INITIAL_VALUE && minY > Canvas.DEFAULT_ENTITY_WIDTH)){
			minX -= Canvas.GRID_SIZE;
			minY -= Canvas.GRID_SIZE;
			for(Component elem : getComponents()){
				elem.setLocation(Geometry.getSnappedPosition(elem.getLocation().x - minX, elem.getLocation().y - minY));
			}
			setLocation(Geometry.getSnappedPosition(getLocation().x+minX, getLocation().y+minY));
			setSize(new Dimension(maxX + Geometry.getZoomed(Canvas.GRID_SIZE - 5)-minX, maxY + 11-minY));
			Canvas.instance.checkWhitespace();
		} else {
			setSize(new Dimension(maxX + Geometry.getZoomed(Canvas.GRID_SIZE - 5), maxY + 11));
		}
		getDB().getProject().save();
		if(!getDescr().isEmpty()){
			setToolTipText(getDescr());
		}
	}

	@Override
	public void setLocation(Point loc){
		super.setLocation(loc);
		//parent.checkSize();
	}

	@Override
	public void setSize(Dimension d){
		setSizeWoChecks(d);
		schemaContainer.checkSize();
	}


	private void setSizeWoChecks(Dimension d){
		if(d == null){
			d = new Dimension(Canvas.ZOOMED_ENTITY_WIDTH, 40);
		}else{
			if(d.height < 40) {
				d.height = 40;
			}
			if(d.width < Canvas.ZOOMED_ENTITY_WIDTH) {
				d.width = Canvas.ZOOMED_ENTITY_WIDTH;
			}
		}
		super.setSize(d);
	}

	@Override
	protected void mouseUp(){
		schemaContainer.checkSize();
	}

	@Override
	public DefaultMutableTreeNode getTreeView(boolean showCreateIcons){
		return new DefaultMutableTreeNode(this);
	}

	@Override
	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching) {
		boolean nameMatch = getFullName().matches(search);
		boolean excludedByName = searchNotMatching && nameMatch;
		if(!excludedByName && (searchNotMatching || nameMatch || (
				fulltext && getDB().getProject().getCurrentConn().getQueryCreate(this, null).matches(search)
		))){
			return getTreeView(true);
		}else{
			return null;
		}
	}

	@Override
	public void unSelect() {
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	/**
	 * Component clicked event handling - to be overridden
	 */
	@Override
	public void clicked() {
		if(!isSelected){
			if(getDB().getConnection().isSupported(SupportedElement.PACKAGE)){
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
				scrollIntoView();
				Canvas.instance.setSelectedElement(this);
				DBTree.instance.selectRelationByName(schemaContainer.getDB().getName(), schemaContainer.getName(), toString());
			}else{
				Canvas.instance.setSelectedElement(null);
			}
		}
	}

	/**
	 * Component double-clicked event handling - to be overridden
	 */
	@Override
	public void doubleClicked() {
		Usage.log(PACKAGE_DOUBLE_CLICKED, "View: " + DesignGUI.getView().name());
		RelationWizard.get(null).drawProperties(this);
	}

	/**
	 * Component right-clicked event handling - to be overridden
	 */
	@Override
	public void rightClicked() {
		rightClicked(null);
	}

	@Override
	public void rightClicked(final String workspaceName){
		RightClickMenu menu = RightClickMenu.get((type, selectedValue) -> {
            switch(type){
                case 10:
                	Usage.log(PACKAGE_CONTEXT_EDIT);
                	RelationWizard.get(workspaceName).drawProperties(Package.this);
                	break;
                case 30:
					Usage.log(PACKAGE_CONTEXT_COPY);
                	DesignGUI.toClipboard(Package.this.getName());
                	break;
                case 31:
					Usage.log(PACKAGE_CONTEXT_COPY, COPY_FULL);
                	DesignGUI.toClipboard(Package.this.getFullName());
                	break;
                case 32:
					Usage.log(PACKAGE_CONTEXT_SOURCE);
                	DesignGUI.toClipboard(Package.this.getQueryCreateClear(getConnection()));
                	break;
                case 39:
					Usage.log(PACKAGE_CONTEXT_SOURCE, SRC_REMOTE);
                    if(selectedValue.equals("Model")){
                        DesignGUI.toClipboard(Package.this.getQueryCreateClear(getConnection()));
                    }else{
						Schedule.inWorker(() -> {
                            try {
                                Project p = getDB().getProject();
                                IConnection c = p.getDedicatedConnection(getDB().getName(), p.getConnectionByName(selectedValue).getName());
                                if (c != null) {
                                    DesignGUI.toClipboard(c.loadDDLFromDB(Package.this));
                                }
                            } catch (DBCommException ex) {
								Dbg.fixme("Loading package DDL failed", ex);
								JOptionPane.showMessageDialog(GCFrame.getActiveWindow(), new SelectableText("Loading DDL failed\n\n" + ex.getMessage(), false), "Loading DDL failed", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                    break;
                case 61:
					Usage.log(PACKAGE_CONTEXT_DROP);
                	RelationWizard.get(workspaceName).enableDrop().drawProperties(Package.this);
                	break;
                /*case 70:
                    String wsName = selectedValue.replace("Remove from ", "");
                    Project.getCurrent().getWorkspaceByName(wsName).remove(Package.this);
                    break;
                case 71:
                    if(selectedValue.equals("Create new workspace")){
                        ws = Workspace.create(Package.this);
                    }else{
                        ws = Project.getCurrent().getWorkspaceByName(selectedValue);
                        ws.add(Package.this);
                    }
                    SearchPanel.instance.updateDbTree();
                    Workspace.select(ws);
                    break;*/
				default: throw new IllegalArgumentException("Menu option " + type + " is not known");
            }
        }).
				addItem("Properties", RightClickMenu.ICO_EDIT, 10);
		menu.separator().
				addItem("Copy name", RightClickMenu.ICO_COPY, 30).
				addItem("Copy name with schema", RightClickMenu.ICO_COPY, 31);

		if(!getDB().getProject().getCurrentConn().isSupported(SupportedElement.GENERATE_DDL)) {
			menu.addItem("Copy source code", RightClickMenu.ICO_COPY, 32);
		}else{
			menu.addItem("Copy source code from", RightClickMenu.ICO_COPY, 39, Geometry.concat(new String[]{"Model"}, Project.getCurrent().getConnectionNames()));
		}

		/*if(workspaceName != null){
			menu.separator().
					addItem("Remove from " + workspaceName, RightClickMenu.ICO_DELETE, 70);
		}else{
			menu.separator().
					addItem("Add to workspace", Workspace.ico16, 71, Geometry.concat(new String[]{"Create new workspace"}, Project.getCurrent().getWorkspaceNames()));
		}*/
		menu.
				separator().
				addItem("DROP...", RightClickMenu.ICO_DELETE, 61);
	}

	@Override
	public IConnection getConnection() {
		return schemaContainer.getConnection();
	}

	@Override
	public DB getDB() {
		return schemaContainer.getDB();
	}

	@Override
	public boolean isNew() {
		return behavior.isNew;
	}

	@Override
	public Behavior getBehavior() {
		return behavior;
	}

	@Override
	public void setBehavior(IModelBehavior behavior) {
		this.behavior = (Behavior) behavior;
	}

	@Override
	public int compareTo(IModelElement o) {
		return getName().compareTo(o.getName());
	}

	/**
	 * Get a 16x16 icon.
	 *
	 * @return icon
	 */
	@Override
	public Icon getIcon16() {
		return ico16;
	}

	@Override
	public String toString(){
		return behavior.name;
	}

	public String getSrc() {
		return behavior.definition;
	}

	public String getBody() {
		return behavior.body;
	}

	public class Behavior extends AbstractModelBehavior<Behavior> {

		public static final String L_NAME = "Name";
		public static final String L_OPERATIONS = "Actions";
		public static final String L_DESCR = "Comment";
		public static final String L_SCHEMA = "Schema";
		public static final String L_DEFINITION = "Package";
		public static final String L_BODY = "Package body";

		private String name;
		private String schemaName;
		private String descr = "";
		private String definition = "";
		private String body = "";

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

		public String getDescr() {
			return descr;
		}

		public void setDescr(String descr) {
			this.descr = descr;
		}

		public String getDefinition() {
			return definition;
		}

		public void setDefinition(String definition) {
			this.definition = definition;
		}

		public String getBody() {
			return body;
		}

		public void setBody(String body) {
			this.body = body;
		}

		@Override
		public Behavior prepareForEdit(){
			valuesForEdit = new Behavior();
			valuesForEdit.name = name;
			valuesForEdit.schemaName = schemaName;
			valuesForEdit.descr = descr;
			valuesForEdit.definition = definition;
			valuesForEdit.body = body;
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
				behavior.descr = descr;
				behavior.definition = definition;
				behavior.body = body;
				if(behavior.isNew){
					setSchemaByName(schemaName);
					assignToSchema();
					behavior.isNew = false;
					behavior.isDropped = false;
				}else{
					if(schemaChanged){
						getSchema().getPackages().remove(Package.this);
						setSchemaByName(schemaName);
						assignToSchema();
					}
					draw();
				}
			}
			getDB().getProject().save();
		}

		@Override
		public void notifyChange(String elementName, String value) {
			switch (elementName) {
				case L_NAME:
					name = value;
					break;
				case L_SCHEMA:
					schemaName = value;
					break;
				case L_DESCR:
					descr = value;
					break;
				case L_BODY:
					body = value;
					break;
				case L_DEFINITION:
					definition = value;
					break;
			}
		}

		@Override
		public void notifyChange(String elementName, boolean value) {}

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
