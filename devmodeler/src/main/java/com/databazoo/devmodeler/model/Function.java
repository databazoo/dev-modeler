package com.databazoo.devmodeler.model;

import com.databazoo.components.GCFrame;
import com.databazoo.components.UIConstants;
import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.components.elements.LineComponent;
import com.databazoo.components.text.SelectableText;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.*;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.*;
import com.databazoo.devmodeler.gui.window.datawindow.DataWindow;
import com.databazoo.devmodeler.model.reference.FunctionReference;
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
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.databazoo.devmodeler.gui.UsageElement.*;

/**
 * Model representation of procedures.
 * Displayed on canvas as a draggable component.
 *
 * @author bobus
 */
public class Function extends DraggableComponent implements IModelElement {
	public static final String L_CLASS = "Function";
	public static final Icon ico16 = Theme.getSmallIcon(Theme.ICO_FUNCTION);
	public static final Color BG_COLOR = Color.decode("#6FCC66");
	public static final String TRIGGER = "trigger";

	public static Function lastCreatedTriggerFunction;

	public static String[] getVolatilityOptions(){
		return new String[]{"VOLATILE", "STABLE", "IMMUTABLE"};
	}

	private Behavior behavior = new Behavior();
	private final List<Trigger> triggers = new ArrayList<>();
	private int isDifferent = Comparator.NO_DIFF;

	public Function(Schema parent, String fullName, String retType, String args, String src, String lang, String volatility, boolean isSecDefiner, int cost, int rows, String descr) {
		super();
		if(fullName.contains(".")){
			behavior.name = fullName.replaceFirst(Pattern.quote(parent.getSchemaPrefix()), "");
			behavior.schemaName = parent.getName();
		}else{
			behavior.name = fullName;
		}
		behavior.retType = retType;
		behavior.args = args;
		behavior.src = src;
		behavior.lang = lang;
		behavior.volatility = volatility;
		behavior.isSecDefiner = isSecDefiner;
		behavior.cost = cost;
		behavior.rows = rows;
		behavior.descr = descr;
		this.schemaContainer = parent;

		String forceEOL = Settings.getStr(Settings.L_FONT_TEXT_EOL);
		if(forceEOL != null && !forceEOL.isEmpty()) {
			behavior.src = behavior.src.replaceAll("\r?\n", forceEOL);
		}
		draw();
	}

	public List<Trigger> getTriggers() {
		return triggers;
	}

	private void draw(){
		setLayout(null);
		setSize(new Dimension(/*Canvas.getZoomed*/(Canvas.DEFAULT_ENTITY_WIDTH), /*Canvas.getZoomed*/ (20)));
		//setToolTipText(getRetType() + " " + behavior.name + "(" + getArgs() + ")");
		setBackground(BG_COLOR);
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		checkSize();
	}

	@Override
	public void checkConstraints(){
		try{
			triggers.forEach(LineComponent::checkSize);
			schemaContainer.checkSize(this);
		} catch (ConcurrentModificationException e){
			Dbg.fixme("Trigger size update failed", e);
		}
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
		Usage.log(FUNCTION_DOUBLE_CLICKED, "View: " + DesignGUI.getView().name());
		if(!triggers.isEmpty()){
			RelationWizard.get(null).drawProperties((Relation)triggers.get(0).getRel1(), toString());
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
					Usage.log(FUNCTION_CONTEXT_EDIT);
					RelationWizard.get(workspaceName).drawProperties(Function.this);
					break;
                case 20:
					Usage.log(FUNCTION_CONTEXT_EXEC);
					DataWindow.get().drawQueryWindow(getConnection().getQueryExecFunction(Function.this), getDB());
					break;
                case 30:
					Usage.log(FUNCTION_CONTEXT_COPY);
					DesignGUI.toClipboard(Function.this.getName());
					break;
                case 31:
					Usage.log(FUNCTION_CONTEXT_COPY, COPY_FULL);
					DesignGUI.toClipboard(Function.this.getFullName());
					break;
                case 32:
					Usage.log(FUNCTION_CONTEXT_SOURCE);
					DesignGUI.toClipboard(Function.this.getQueryCreateClear(getConnection()));
					break;
                case 39:
					Usage.log(FUNCTION_CONTEXT_SOURCE, "remote");
                    if(selectedValue.equals("Model")){
                        DesignGUI.toClipboard(Function.this.getQueryCreateClear(getConnection()));
                    }else{
						Schedule.inWorker(() -> {
                            try {
                                Project p = getDB().getProject();
                                IConnection c = p.getDedicatedConnection(getDB().getName(), p.getConnectionByName(selectedValue).getName());
                                if (c != null) {
                                    DesignGUI.toClipboard(c.loadDDLFromDB(Function.this));
                                }
                            } catch (DBCommException ex) {
								Dbg.fixme("Getting function DDL failed.", ex);
                                JOptionPane.showMessageDialog(GCFrame.getActiveWindow(), new SelectableText("Loading DDL failed\n\n" + ex.getMessage(), false), "Loading DDL failed", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                    break;
                case 61:
					Usage.log(FUNCTION_CONTEXT_DROP);
					RelationWizard.get(workspaceName).enableDrop().drawProperties(Function.this);
					break;
                case 70:
					Usage.log(FUNCTION_CONTEXT_WORKSPACE, WS_REMOVE);
                    String wsName = selectedValue.replace("Remove from ", "");
                    Project.getCurrent().getWorkspaceByName(wsName).remove(Function.this);
                    break;
                case 71:
					Usage.log(FUNCTION_CONTEXT_WORKSPACE);
                    if(selectedValue.equals("Create new workspace")){
                        ws = Workspace.create(Function.this);
                    }else{
                        ws = Project.getCurrent().getWorkspaceByName(selectedValue);
                        ws.add(Function.this)
								.setLocation(Geometry.getSnappedPosition(Canvas.GRID_SIZE, ws.find(getSchema()).getHeight()));
                    }
                    SearchPanel.instance.updateDbTree();
                    Workspace.select(ws);
                    break;
				default: throw new IllegalArgumentException("Menu option " + type + " is not known");
            }
        }).
		addItem("Properties", RightClickMenu.ICO_EDIT, 10);
		if(getDB().getProject().getType() != Project.TYPE_ABSTRACT && !behavior.retType.equals(TRIGGER)){
			menu.separator().
			addItem("Execute...", RightClickMenu.ICO_RUN, 20);
		}
		menu.separator().
		addItem("Copy name", RightClickMenu.ICO_COPY, 30).
		addItem("Copy name with schema", RightClickMenu.ICO_COPY, 31);

		if(!getDB().getProject().getCurrentConn().isSupported(SupportedElement.GENERATE_DDL)) {
			menu.addItem("Copy source code", RightClickMenu.ICO_COPY, 32);
		}else {
			menu.addItem("Copy source code from", RightClickMenu.ICO_COPY, 39, Geometry.concat(new String[]{"Model"}, Project.getCurrent().getConnectionNames()));
		}

		if(workspaceName != null){
			menu.separator().
			addItem("Remove from "+workspaceName, RightClickMenu.ICO_DELETE, 70);
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

	public DefaultMutableTreeNode getTreeView(DefaultMutableTreeNode functionNode){
		return functionNode;
	}

	@Override
	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching) {
		return getTreeView(search, fulltext, searchNotMatching, new DefaultMutableTreeNode(this));
	}

	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching, DefaultMutableTreeNode functionNode) {
		boolean nameMatch = getFullName().matches(search);
		boolean excludedByName = searchNotMatching && nameMatch;
		if(!excludedByName && (searchNotMatching || nameMatch || (
				fulltext && getDB().getProject().getCurrentConn().getQueryCreate(this, null).matches(search)
		))) {
			return getTreeView(functionNode);
		}else{
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

	public String getArgs(){
		return behavior.args;
	}
	public String getCleanArgs(){
		return behavior.args.replaceAll("(?ims)\\s+DEFAULT\\s+[^,]+", "");
	}
	public String getRetType(){
		return behavior.retType;
	}
	public String getSrc(){
		return behavior.src;
	}
	public String getLang(){
		return behavior.lang;
	}
	public String getVolatility(){
		return behavior.volatility;
	}
	public boolean isSecurityDefiner(){
		return behavior.isSecDefiner;
	}
	public int getCost(){
		return behavior.cost;
	}
	public int getRows(){
		return behavior.rows;
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
		for(int i=0; i<10000; i++){
			if(triggers.isEmpty()){
				break;
			}
			triggers.get(0).drop();
		}
		schemaContainer.remove(this);
		schemaContainer.getFunctions().remove(this);
		getDB().getProject().getWorkspaces()
				.forEach(workspace -> workspace.getFunctions().stream()
						.filter(reference -> reference.getElement() == this)
						.collect(Collectors.toList())
						.forEach(FunctionReference::drop)
				);
	}

	@Override
	public void checkSize(){
		displayName = behavior.name+"("+getArgs()+")";
		if(!behavior.args.isEmpty()){
			if(displayName.length() > 40){
				int size = behavior.args.split(",").length;
				StringBuilder attrs = new StringBuilder();
				String comma = "";
				for(int i=1; i<=size; i++){
					attrs.append(comma).append("$").append(i);
					comma = ", ";
				}
				displayName = behavior.name+"("+attrs+")";
			}
			if(displayName.length() > 40){
				displayName = behavior.name+"("+behavior.args.split(",").length+")";
			}
		}
		displayNameWidth = UIConstants.GRAPHICS.getFontMetrics(Canvas.getTitleFont()).stringWidth(displayName);
	}

	@Override
	public void unSelect(){
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		Workspace ws = Project.getCurrent().getOldWorkspace();
		if(ws != null){
			for(FunctionReference rel : ws.getFunctions()){
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
		Set<IModelElement> elements = new HashSet<>();
		elements.add(this);
		triggers.forEach(elem -> elements.addAll(elem.getAllSubElements()));
		return elements;
	}

	@Override
	public boolean isNew(){
		return behavior.isNew;
	}

	public void assignToSchema(){
		schemaContainer.getFunctions().add(this);
	}

	public void assignToSchema(Schema schema){
		this.schemaContainer = schema;
		assignToSchema();
	}

	public boolean hasNoTriggers(){
		return !getBehavior().isTrigger() || getTriggers().isEmpty();
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
		public static final String L_ARGUMENTS = "Arguments";
		public static final String L_RETURNS = "Returns";
		public static final String L_LANGUAGE = "Language";
		public static final String L_VOLATILITY = "Volatility";
		public static final String L_STRICT = "Strict";
		public static final String L_SEC_DEFINER = "Security definer";
		public static final String L_COST = "Cost";
		public static final String L_ROWS = "Rows";
		public static final String L_BEHAVIOR = "Behavior";
		//public static final String L_PERFORMANCE = "Performance";
		public static final String L_OPERATIONS = "Actions";
		public static final String L_DESCR = "Comment";
		public static final String L_BODY = "Source";

		private String name;
		private String schemaName = "";
		private String retType = "";
		private String args = "";
		private String src = "";
		private String lang = "plpgsql";
		private String volatility = "VOLATILE";
		private String descr = "";
		private boolean isSecDefiner = false;
		private boolean isStrict = false;
		private int cost = 100;
		private int rows = 1000;

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

		public String getRetType() {
			return retType;
		}

		public void setRetType(String retType) {
			this.retType = retType;
		}

		public boolean isTrigger(){
			return retType.equalsIgnoreCase(TRIGGER);
		}

		public String getArgs() {
			return args;
		}

		public void setArgs(String args) {
			this.args = args;
		}

		public String getSrc() {
			return src;
		}

		public void setSrc(String src) {
			this.src = src;
		}

		public String getLang() {
			return lang;
		}

		public void setLang(String lang) {
			this.lang = lang;
		}

		public String getVolatility() {
			return volatility;
		}

		/*public void setVolatility(String volatility) {
			this.volatility = volatility;
		}*/

		public String getDescr() {
			return descr != null ? descr : "";
		}

		public void setDescr(String descr) {
			this.descr = descr;
		}

		public boolean isSecDefiner() {
			return isSecDefiner;
		}

		/*public void setSecDefiner(boolean secDefiner) {
			isSecDefiner = secDefiner;
		}*/

		public boolean isStrict() {
			return isStrict;
		}

		/*public void setStrict(boolean strict) {
			isStrict = strict;
		}*/

		public int getCost() {
			return cost;
		}

		public void setCost(int cost) {
			this.cost = cost;
		}

		public int getRows() {
			return rows;
		}

		public void setRows(int rows) {
			this.rows = rows;
		}

		@Override
		public Behavior prepareForEdit(){
			valuesForEdit = new Behavior();
			valuesForEdit.name = name;
			valuesForEdit.schemaName = schemaName;
			valuesForEdit.retType = retType;
			valuesForEdit.args = args;
			valuesForEdit.src = src;
			valuesForEdit.lang = lang;
			valuesForEdit.volatility = volatility;
			valuesForEdit.isSecDefiner = isSecDefiner;
			valuesForEdit.cost = cost;
			valuesForEdit.rows = rows;
			valuesForEdit.descr = descr;
			return valuesForEdit;
		}

		@Override
		public void saveEdited(){
			if(isDropped){
				drop();
			}else{
				boolean schemaChanged = !behavior.schemaName.equals(schemaName);
				boolean nameChanged = !behavior.name.equals(name);
				String oldName = behavior.name;
				behavior.name = name;
				behavior.schemaName = schemaName;
				behavior.retType = retType;
				behavior.args = args;
				behavior.src = src;
				behavior.lang = lang;
				behavior.volatility = volatility;
				behavior.isSecDefiner = isSecDefiner;
				behavior.cost = cost;
				behavior.rows = rows;
				behavior.descr = descr;
				if(behavior.isNew){
					setSchemaByName(schemaName);
					assignToSchema();
					behavior.isNew = false;
					behavior.isDropped = false;
					if(behavior.retType.contains(TRIGGER)){
						lastCreatedTriggerFunction = Function.this;
					}
				}else{
					if(schemaChanged){
						getSchema().getFunctions().remove(Function.this);
						setSchemaByName(schemaName);
						assignToSchema();
					}
					if(nameChanged){
						for(Trigger trig: triggers){
							trig.getBehavior().setProcname(trig.getBehavior().getProcname().replaceFirst(oldName, name));
						}
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
				case L_ARGUMENTS:
					args = value;
					break;
				case L_RETURNS:
					retType = value;
					break;
				case L_VOLATILITY:
					volatility = value;
					break;
				case L_LANGUAGE:
					lang = value;
					break;
				case L_COST:
					if(value.isEmpty()){
						cost = 100;
					}else{
						cost = Integer.parseInt(value);
					}	break;
				case L_ROWS:
					if(value.isEmpty()){
						rows = 0;
					}else{
						rows = Integer.parseInt(value);
					}	break;
				case L_BODY:
					src = value;
					break;
			}
		}

		@Override
		public void notifyChange(String elementName, boolean value) {
			switch (elementName) {
				case L_SEC_DEFINER:
					isSecDefiner = value;
					break;
				case L_STRICT:
					isStrict = value;
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
