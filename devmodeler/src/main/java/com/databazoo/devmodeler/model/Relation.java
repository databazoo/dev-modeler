package com.databazoo.devmodeler.model;

import com.databazoo.components.GCFrame;
import com.databazoo.components.UIConstants;
import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.components.elements.LineComponent;
import com.databazoo.components.elements.ReferenceListener;
import com.databazoo.components.text.SelectableText;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.ConnectionUtils;
import com.databazoo.devmodeler.conn.DBCommException;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.Result;
import com.databazoo.devmodeler.conn.SQLOutputConfig;
import com.databazoo.devmodeler.conn.SQLOutputConfigExport;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.DBTree;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.Neighborhood;
import com.databazoo.devmodeler.gui.Neighborhood.NeighborhoodRelation;
import com.databazoo.devmodeler.gui.RightClickMenu;
import com.databazoo.devmodeler.gui.SearchPanel;
import com.databazoo.devmodeler.gui.view.ViewMode;
import com.databazoo.devmodeler.gui.window.datawindow.DataWindow;
import com.databazoo.devmodeler.model.reference.RelationReference;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.tools.Geometry;
import com.databazoo.devmodeler.tools.comparator.Comparator;
import com.databazoo.devmodeler.tools.comparator.DataDifference;
import com.databazoo.devmodeler.wizards.relation.RelationWizard;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;
import com.databazoo.tools.Usage;
import plugins.api.IModelTable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.databazoo.devmodeler.gui.UsageElement.ATTRIBUTE_DOUBLE_CLICKED;
import static com.databazoo.devmodeler.gui.UsageElement.COPY_FULL;
import static com.databazoo.devmodeler.gui.UsageElement.SRC_REMOTE;
import static com.databazoo.devmodeler.gui.UsageElement.TABLE_CONTEXT_COPY;
import static com.databazoo.devmodeler.gui.UsageElement.TABLE_CONTEXT_DATA;
import static com.databazoo.devmodeler.gui.UsageElement.TABLE_CONTEXT_DROP;
import static com.databazoo.devmodeler.gui.UsageElement.TABLE_CONTEXT_EDIT;
import static com.databazoo.devmodeler.gui.UsageElement.TABLE_CONTEXT_MAINTAIN;
import static com.databazoo.devmodeler.gui.UsageElement.TABLE_CONTEXT_SOURCE;
import static com.databazoo.devmodeler.gui.UsageElement.TABLE_CONTEXT_TRUNCATE;
import static com.databazoo.devmodeler.gui.UsageElement.TABLE_CONTEXT_WORKSPACE;
import static com.databazoo.devmodeler.gui.UsageElement.TABLE_DOUBLE_CLICKED;
import static com.databazoo.devmodeler.gui.UsageElement.WS_REMOVE;

/**
 * Model representation of tables.
 * Displayed on canvas as a draggable component.
 *
 * @author bobus
 */
public class Relation extends DraggableComponent implements IModelElement, IModelTable {
	public static final String L_CLASS = "Table";
	public static final Icon ico16 = Theme.getSmallIcon(Theme.ICO_TABLE);

	public static Color LOW_COLOR = Color.decode("#E9EFF8");

	public static final String L_ATTRIBUTES = "Attributes";
	public static final String L_CONSTRAINTS = "Constraints";
	public static final String L_INDEXES = "Indexes";
	public static final String L_TRIGGERS = "Triggers";

	public static final String L_NEW_SCHEMA = "Add new schema";
	public static final String L_NEW_TABLE = "Add new table";
	public static final String L_NEW_SEQUENCE = "Add new sequence";
	public static final String L_NEW_FUNCTION = "Add new function";
	public static final String L_NEW_PACKAGE = "Add new package";
	public static final String L_NEW_VIEW = "Add new view";
	public static final String L_NEW_ATTRIBUTE = "Add new attribute";
	public static final String L_NEW_PK = "Add primary key";
	public static final String L_NEW_FK = "Add foreign key";
	public static final String L_NEW_UNIQUE_C = "Add unique constraint";
	public static final String L_NEW_CHECK_C = "Add check constraint";
	public static final String L_NEW_INDEX = "Add new index";
	public static final String L_NEW_TRIGGER = "Add new trigger";

	private static final String UNKNOWN = "Unknown";

	public static final int TRIGGER_COUNT_LIMIT = 5;
	public static final int INDEX_COUNT_LIMIT = 10;
	public static final int ATTRIBUTE_COUNT_LIMIT = 50;

	private Behavior behavior = new Behavior();
	private int[] pkCols = new int[0];
	private final List<Attribute> attributes = new ArrayList<>();
	private final List<Constraint> constraints = new ArrayList<>();
	private final List<Trigger> triggers = new ArrayList<>();
	private final List<Index> indexes = new ArrayList<>();
	private final List<Attribute> infoVals = new ArrayList<>();
	private final List<Inheritance> inheritances = new ArrayList<>();
	private IndexLines indexLines;

	private int isDifferent = Comparator.NO_DIFF;

	private int countAttributes = 0;
	private long sizeTotal = 0;
	private long sizeIndexes = 0;
	private int countRows = 0;

	private transient DataDifference dataDifference;
	private boolean haveAddedElement;
	private boolean isTemp;

	public Relation(Schema parent, String fullName) {
		super();
		fullName = fullName.replaceAll("\"", "");
		if(fullName.contains(".")){
			behavior.name = fullName.replaceFirst(Pattern.quote(parent.getSchemaPrefix()), "");
			behavior.schemaName = parent.getName();
		}else{
			behavior.name = fullName;
		}
		this.schemaContainer = parent;
		draw();
	}

	public Relation(Schema parent, String name, String fullName, int countAttributes, int countRows, boolean hasOIDs, String[] options, String descr, String fullParent) {
		super();
		name = name.replaceAll("\"", "");
		fullName = fullName.replaceAll("\"", "");
		behavior.name = name;
		if(!fullName.equals(name)){
			behavior.schemaName = fullName.split("\\.")[0];
		}
		behavior.hasOIDs = hasOIDs;
		behavior.options = options;
		behavior.descr = descr;

		if(fullParent != null && !fullParent.isEmpty()){
			behavior.setInheritParentName(fullParent);
		}

		this.schemaContainer = parent;
		this.countAttributes = countAttributes;
		this.countRows = countRows;
		draw();
	}

	public int[] getPkCols() {
		return pkCols;
	}

	public void setPkCols(int[] pkCols) {
		this.pkCols = pkCols;
	}

	public List<Attribute> getAttributes() {
		return attributes;
	}

	public List<Constraint> getConstraints() {
		return constraints;
	}

	public List<Trigger> getTriggers() {
		return triggers;
	}

	public List<Index> getIndexes() {
		return indexes;
	}

	public List<Attribute> getInfoVals() {
		return infoVals;
	}

	public List<Inheritance> getInheritances() {
		return inheritances;
	}

	public int getCountAttributes() {
		return countAttributes;
	}

	public void setCountAttributes(int countAttributes) {
		this.countAttributes = countAttributes;
	}

	public long getSizeTotal() {
		return sizeTotal;
	}

	public void setSizeTotal(long sizeTotal) {
		this.sizeTotal = sizeTotal;
	}

	public long getSizeIndexes() {
		return sizeIndexes;
	}

	public void setSizeIndexes(long sizeIndexes) {
		this.sizeIndexes = sizeIndexes;
	}

	public int getCountRows() {
		return countRows;
	}

	public void setCountRows(int countRows) {
		this.countRows = countRows;
	}

	public boolean isTemp() {
		return isTemp;
	}

	public void setTemp() {
		isTemp = true;
	}
	/*public void setNotTemp() {
		isTemp = false;
	}*/

	private void draw(){
		displayName = behavior.name;
		if(!attributes.isEmpty()){
			countAttributes = attributes.size();
		}
		setLayout(null);
		checkSize();

		setBackground(UIConstants.Colors.getPanelBackground());
		setForeground(UIConstants.Colors.getLabelForeground());
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	@Override
	public DefaultMutableTreeNode getTreeView(boolean showCreateIcons){
		return getTreeView(showCreateIcons, new DefaultMutableTreeNode(this));
	}

	public DefaultMutableTreeNode getTreeView(boolean showCreateIcons, DefaultMutableTreeNode relationNode){
		IConnection c = getDB().getProject().getCurrentConn();

		DefaultMutableTreeNode attributeTree = new DefaultMutableTreeNode(L_ATTRIBUTES);
		for (Attribute attribute : attributes) {
			attributeTree.add(attribute.getTreeView(showCreateIcons));
		}
		DefaultMutableTreeNode constraintTree = new DefaultMutableTreeNode(L_CONSTRAINTS);
		DefaultMutableTreeNode indexTree = new DefaultMutableTreeNode(L_INDEXES);
		for(Index ind: indexes){
			if(ind.getBehavior().isPrimary()){
				constraintTree.add(ind.getTreeView(showCreateIcons));
				break;
			}
		}
		indexes.stream().filter(ind -> !ind.getBehavior().isPrimary()).forEachOrdered(ind -> {
			if (ind.getBehavior().isConstraint()) {
				constraintTree.add(ind.getTreeView(showCreateIcons));
			} else {
				indexTree.add(ind.getTreeView(showCreateIcons));
			}
		});
		constraints.stream().filter(con -> con.getRel1() != null && con.getRel1().getFullName().equals(getFullName()))
				.forEachOrdered(con -> constraintTree.add(con.getTreeView(showCreateIcons)));
		DefaultMutableTreeNode triggerTree = new DefaultMutableTreeNode(L_TRIGGERS);
		for(Trigger trig: triggers){
			triggerTree.add(trig.getTreeView(showCreateIcons));
		}
		if(showCreateIcons){
			attributeTree.add(new DefaultMutableTreeNode(L_NEW_ATTRIBUTE));
			if(pkCols.length == 0 && c.isSupported(SupportedElement.PRIMARY_KEY)){
				constraintTree.add(new DefaultMutableTreeNode(L_NEW_PK));
			}
			if(c.isSupported(SupportedElement.FOREIGN_KEY)) {
				constraintTree.add(new DefaultMutableTreeNode(L_NEW_FK));
			}
			if(c.isSupported(SupportedElement.UNIQUE_CONSTRAINT)) {
				constraintTree.add(new DefaultMutableTreeNode(L_NEW_UNIQUE_C));
			}
			if(c.isSupported(SupportedElement.CHECK_CONSTRAINT)) {
				constraintTree.add(new DefaultMutableTreeNode(L_NEW_CHECK_C));
			}
			if(c.isSupported(SupportedElement.INDEX) || c.isSupported(SupportedElement.INDEX_UNIQUE)) {
				indexTree.add(new DefaultMutableTreeNode(L_NEW_INDEX));
			}
			if(c.isSupported(SupportedElement.TRIGGER)) {
				triggerTree.add(new DefaultMutableTreeNode(L_NEW_TRIGGER));
			}
			if(c.isSupported(SupportedElement.FUNCTION)) {
				triggerTree.add(new DefaultMutableTreeNode(L_NEW_FUNCTION));
			}
		}
		if(c.isSupported(SupportedElement.ATTRIBUTE) && !attributeTree.isLeaf()) {
			relationNode.add(attributeTree);
		}
		if((c.isSupported(SupportedElement.PRIMARY_KEY) || c.isSupported(SupportedElement.FOREIGN_KEY) || c.isSupported(SupportedElement.UNIQUE_CONSTRAINT) || c.isSupported(SupportedElement.CHECK_CONSTRAINT)) && !constraintTree.isLeaf()) {
			relationNode.add(constraintTree);
		}
		if((c.isSupported(SupportedElement.INDEX) || c.isSupported(SupportedElement.INDEX_UNIQUE)) && !indexTree.isLeaf()) {
			relationNode.add(indexTree);
		}
		if((c.isSupported(SupportedElement.TRIGGER) || c.isSupported(SupportedElement.FUNCTION)) && !triggerTree.isLeaf()) {
			relationNode.add(triggerTree);
		}
		return relationNode;
	}

	@Override
	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching) {
		return getTreeView(search, fulltext, searchNotMatching, new DefaultMutableTreeNode(this));
	}

	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching, DefaultMutableTreeNode relationNode){

		// Fail fast for name exclusion
		boolean nameMatch = getFullName().matches(search);
		if(searchNotMatching && nameMatch){
			return null;
		}

		DefaultMutableTreeNode attributeTree = getAttributeTree(search, fulltext, searchNotMatching);
		if(!attributeTree.isLeaf()) {
			relationNode.add(attributeTree);
		}

		DefaultMutableTreeNode constraintTree = getConstraintTree(search, fulltext, searchNotMatching);
		if(!constraintTree.isLeaf()) {
			relationNode.add(constraintTree);
		}

		DefaultMutableTreeNode indexTree = getIndexTree(search, fulltext, searchNotMatching);
		if(!indexTree.isLeaf()) {
			relationNode.add(indexTree);
		}

		DefaultMutableTreeNode triggerTree = getTriggerTree(search, fulltext, searchNotMatching);
		if(!triggerTree.isLeaf()) {
			relationNode.add(triggerTree);
		}

		if(!relationNode.isLeaf() || searchNotMatching || nameMatch || (
				fulltext && getDB().getProject().getCurrentConn().getQueryCreate(this, null).matches(search)
		)) {
			return relationNode;
		}else{
			return null;
		}
	}

	private DefaultMutableTreeNode getAttributeTree(String search, boolean fulltext, boolean searchNotMatching) {
		DefaultMutableTreeNode attributeTree = new DefaultMutableTreeNode(L_ATTRIBUTES);
		for(Attribute attr: attributes){
			DefaultMutableTreeNode child = attr.getTreeView(search, fulltext, searchNotMatching);
			if(child != null) {
				attributeTree.add(child);
			}
		}
		return attributeTree;
	}

	private DefaultMutableTreeNode getIndexTree(String search, boolean fulltext, boolean searchNotMatching) {
		DefaultMutableTreeNode indexTree = new DefaultMutableTreeNode(L_INDEXES);
		indexes.stream().filter(ind -> !ind.getBehavior().isPrimary() && !ind.getBehavior().isConstraint()).forEachOrdered(ind -> {
			DefaultMutableTreeNode child = ind.getTreeView(search, fulltext, searchNotMatching);
			if (child != null) {
				indexTree.add(child);
			}
		});
		return indexTree;
	}

	private DefaultMutableTreeNode getTriggerTree(String search, boolean fulltext, boolean searchNotMatching) {
		DefaultMutableTreeNode triggerTree = new DefaultMutableTreeNode(L_TRIGGERS);
		for(Trigger trig: triggers){
			DefaultMutableTreeNode child = trig.getTreeView(search, fulltext, searchNotMatching);
			if(child != null) {
				triggerTree.add(child);
			}
		}
		return triggerTree;
	}

	private DefaultMutableTreeNode getConstraintTree(String search, boolean fulltext, boolean searchNotMatching) {
		DefaultMutableTreeNode constraintTree = new DefaultMutableTreeNode(L_CONSTRAINTS);
		indexes.stream().filter(ind -> ind.getBehavior().isPrimary()).limit(1).forEachOrdered(ind -> {
			DefaultMutableTreeNode child = ind.getTreeView(search, fulltext, searchNotMatching);
			if(child != null) {
				constraintTree.add(child);
			}
		});
		indexes.stream().filter(ind -> !ind.getBehavior().isPrimary() && ind.getBehavior().isConstraint()).forEachOrdered(ind -> {
			DefaultMutableTreeNode child = ind.getTreeView(search, fulltext, searchNotMatching);
			if (child != null) {
				constraintTree.add(child);
			}
		});
		constraints.stream().filter(con -> con.getRel1().getFullName().equals(getFullName())).forEachOrdered(con -> {
			DefaultMutableTreeNode child = con.getTreeView(search, fulltext, searchNotMatching);
			if (child != null) {
				constraintTree.add(child);
			}
		});
		return constraintTree;
	}

	@Override
	public void checkConstraints(){
		getIndexLines().checkSize();
		try {
			triggers.forEach(LineComponent::checkSize);
			constraints.forEach(Constraint::checkSize);
			indexes.forEach(Index::checkSize);
			inheritances.forEach(LineComponent::checkSize);
		} catch (ConcurrentModificationException e){
			Dbg.notImportant("This could be handled by defensive copies. Ignoring for now.", e);
		}
		schemaContainer.checkSize(this);
	}

	@Override
	public void clicked(){
		if(!isSelected){
			for(Attribute a: attributes){
				a.setDNDEnabled(true);
			}
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			Canvas.instance.setSelectedElement(this);
			DBTree.instance.selectRelationByName(schemaContainer.getDB().getName(), schemaContainer.getName(), behavior.name);
			if(DesignGUI.getView() == ViewMode.DATA) {
				if (countAttributes < 6) {
					countAttributes = 6;
					checkComponentSize();
					Schedule.inEDT(() -> {
						Canvas.instance.drawRelationDataInfo(Relation.this);
						schemaContainer.checkSize();
					});
				}
			}
			scrollIntoView();
		}
		if (DesignGUI.getView() == ViewMode.DATA) {
			Schedule.inEDT(this::syncInfoWithServer);
		}
	}

	@Override
	public void doubleClicked(){
		Usage.log(TABLE_DOUBLE_CLICKED, "View: " + DesignGUI.getView().name());
		if (DesignGUI.getView() == ViewMode.DATA) {
			DataWindow.get().drawRelationData(this, Settings.getBool(Settings.L_DATA_DEFAULT_DESC));
		} else {
			RelationWizard.get(null).drawProperties(this);
		}
	}

	public void doubleClicked(Attribute attr) {
		Usage.log(ATTRIBUTE_DOUBLE_CLICKED, "View: " + DesignGUI.getView().name());
		if (DesignGUI.getView() == ViewMode.DATA) {
			DataWindow.get().drawRelationData(this, Settings.getBool(Settings.L_DATA_DEFAULT_DESC));
		} else {
			RelationWizard.get(null).drawProperties(this, attr.toString());
		}
	}

	@Override
	public void rightClicked(){
		rightClicked(null);
	}

	@Override
	public void rightClicked(final String workspaceName){
		final List<Relation> rels = new ArrayList<>();
		final IConnection conn = getDB().getProject().getCurrentConn();

		rels.add(Relation.this);
		RightClickMenu menu = RightClickMenu.get((type, selectedValue) -> {
            Workspace ws;
            switch(type){
                case 10:
					Usage.log(TABLE_CONTEXT_EDIT);
					RelationWizard.get(workspaceName).drawProperties(Relation.this);
					break;
                case 20:
					Usage.log(TABLE_CONTEXT_DATA);
					DataWindow.get().setLimit(Settings.getInt(Settings.L_DATA_CONTEXT_LIMIT)).drawRelationData(Relation.this, false);
					break;
                case 21:
					Usage.log(TABLE_CONTEXT_DATA);
					DataWindow.get().setLimit(Settings.getInt(Settings.L_DATA_CONTEXT_LIMIT)).drawRelationData(Relation.this, true);
					break;
                case 30:
					Usage.log(TABLE_CONTEXT_COPY);
					DesignGUI.toClipboard(Relation.this.getName());
					break;
                case 31:
					Usage.log(TABLE_CONTEXT_COPY, COPY_FULL);
					DesignGUI.toClipboard(Relation.this.getFullName());
					break;
                case 32:
					Usage.log(TABLE_CONTEXT_SOURCE);
					DesignGUI.toClipboard(Relation.this.getQueryCreateClear(conn));
					break;
                case 39:
					Usage.log(TABLE_CONTEXT_SOURCE, SRC_REMOTE);
                    if(selectedValue.equals("Model")){
                        DesignGUI.toClipboard(Relation.this.getQueryCreateClear(conn));
                    }else{
						Schedule.inWorker(() -> {
                            try {
                                Project p = getDB().getProject();
                                IConnection c = p.getDedicatedConnection(getDB().getName(), p.getConnectionByName(selectedValue).getName());
                                if (c != null) {
                                    DesignGUI.toClipboard(c.loadDDLFromDB(Relation.this));
                                }
                            } catch (DBCommException ex) {
								Dbg.fixme("Getting relation DDL failed.", ex);
                                JOptionPane.showMessageDialog(GCFrame.getActiveWindow(), new SelectableText("Loading DDL failed\n\n" + ex.getMessage(), false), "Loading DDL failed", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                    break;
                case 50:
					Usage.log(TABLE_CONTEXT_MAINTAIN);
					DataWindow.get().drawQueryWindow(conn.getQueryVacuum("ANALYZE VERBOSE", rels), getDB());
					break;
                case 51:
					Usage.log(TABLE_CONTEXT_MAINTAIN);
					DataWindow.get().drawQueryWindow(conn.getQueryVacuum("VACUUM VERBOSE ANALYZE", rels), getDB());
					break;
                case 52:
					Usage.log(TABLE_CONTEXT_MAINTAIN);
					DataWindow.get().drawQueryWindow(conn.getQueryVacuum("VACUUM FULL VERBOSE ANALYZE", rels), getDB());
					break;
                case 55:
					Usage.log(TABLE_CONTEXT_MAINTAIN);
					DataWindow.get().drawQueryWindow(conn.getQueryVacuum("ANALYZE TABLE", rels), getDB());
					break;
                case 56:
					Usage.log(TABLE_CONTEXT_MAINTAIN);
					DataWindow.get().drawQueryWindow(conn.getQueryVacuum("OPTIMIZE TABLE", rels), getDB());
					break;
                case 57:
					Usage.log(TABLE_CONTEXT_MAINTAIN);
					DataWindow.get().drawQueryWindow(conn.getQueryVacuum("REPAIR TABLE", rels), getDB());
					break;
                case 60:
					Usage.log(TABLE_CONTEXT_TRUNCATE);
					DataWindow.get().drawQueryWindow("TRUNCATE TABLE "+Relation.this.getFullName(), getDB());
					break;
                case 61:
					Usage.log(TABLE_CONTEXT_DROP);
					RelationWizard.get(workspaceName).enableDrop().drawProperties(Relation.this);
					break;
                case 70:
					Usage.log(TABLE_CONTEXT_WORKSPACE, WS_REMOVE);
                    String wsName = selectedValue.replace("Remove from ", "");
                    Project.getCurrent().getWorkspaceByName(wsName).remove(Relation.this);
                    break;
                case 71:
					Usage.log(TABLE_CONTEXT_WORKSPACE);
                    if(selectedValue.equals("Create new workspace")){
                        ws = Workspace.create(Relation.this);
                    }else{
                        ws = Project.getCurrent().getWorkspaceByName(selectedValue);
                        ws.add(Relation.this)
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
			addItem("First "+Settings.getInt(Settings.L_DATA_CONTEXT_LIMIT)+" rows", RightClickMenu.ICO_DATA, 20).
			addItem("Last "+Settings.getInt(Settings.L_DATA_CONTEXT_LIMIT)+" rows", RightClickMenu.ICO_DATA, 21);
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
			addItem("Remove from "+workspaceName, RightClickMenu.ICO_DELETE, 70);
		}else{
			menu.separator().
			addItem("Add to workspace", Workspace.ico16, 71, Geometry.concat(new String[]{"Create new workspace"}, Project.getCurrent().getWorkspaceNames(getDB())));
		}
		if(getDB().getProject().getType() != Project.TYPE_ABSTRACT) {
			if(getDB().getProject().getCurrentConn().isSupported(SupportedElement.VACUUM)){
				menu.separator().
				addItem("Analyze...", RightClickMenu.ICO_VACUUM, 50).
				addItem("Vacuum...", RightClickMenu.ICO_VACUUM, 51).
				addItem("Vacuum full...", RightClickMenu.ICO_VACUUM, 52);
			}else if(getDB().getProject().getCurrentConn().isSupported(SupportedElement.OPTIMIZE)){
				menu.separator().
				addItem("Analyze...", RightClickMenu.ICO_VACUUM, 55).
				addItem("Optimize...", RightClickMenu.ICO_VACUUM, 56).
				addItem("Repair...", RightClickMenu.ICO_VACUUM, 57);
			}
		}
		menu.separator();
		if(getDB().getProject().getType() != Project.TYPE_ABSTRACT) {
			menu.addItem("TRUNCATE...", RightClickMenu.ICO_DELETE, 60);
		}
		menu.addItem("DROP...", RightClickMenu.ICO_DELETE, 61);
	}

	@Override
	protected void mouseUp(){
		schemaContainer.checkSize();
	}

	public String getPKey(){
		String ret = "";
		String comma = "";
		for (int pkCol : pkCols) {
			if (pkCol > 0) {
				for (Attribute attr : attributes) {
					if (attr.getAttNum() == pkCol) {
						ret += comma + attr.getName();
						comma = ", ";
					}
				}
			}
		}
		return ret;
	}

	@Override
	public String getName(){
		return behavior.name;
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
			behavior.isNew = true;
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
		if(config.exportTables){
			config.updateLimit();
			config.append((config.conn != null ? config.conn : getConnection()).getQueryCreate(this, config));
		}

		for(Index ind : indexes){
			if(!ind.getBehavior().isConstraint() && !ind.getBehavior().isPrimary()){
				ind.getQueryRecursive(config);
			}
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

	public Attribute getAttributeByName(String name) {
		for(Attribute attr: attributes){
			if(attr.getName().equals(name)){
				return attr;
			}
		}
		return null;
	}
	public IModelElement getConstraintByName(String name) {
		for(Constraint con: constraints){
			if(con.getName().equals(name)){
				return con;
			}
		}
		for(Index ind: indexes){
			if(ind.getName().equals(name) && (ind.getBehavior().isConstraint() || ind.getBehavior().isPrimary())){
				return ind;
			}
		}
		return null;
	}
	public Trigger getTriggerByName(String name) {
		for(Trigger trig: triggers){
			if(trig.getName().equals(name)){
				return trig;
			}
		}
		return null;
	}
	public Index getIndexByName(String name) {
		for(Index ind: indexes){
			if(ind.getName().equals(name) || ind.getFullName().equals(name)){
				return ind;
			}
		}
		return null;
	}

	@Override
	public void drop(){
		for(int i=0; i<10000; i++){
			if(constraints.isEmpty()){
				break;
			}
			constraints.get(0).drop();
		}
		for(int i=0; i<10000; i++){
			if(triggers.isEmpty()){
				break;
			}
			triggers.get(0).drop();
		}
		for(int i=0; i<10000; i++){
			if(indexes.isEmpty()){
				break;
			}
			indexes.get(0).drop();
		}
		for(int i=0; i<10000; i++){
			if(attributes.isEmpty()){
				break;
			}
			attributes.get(0).drop();
		}
		schemaContainer.remove(this);
		schemaContainer.getRelations().remove(this);
		getDB().getProject().getWorkspaces()
				.forEach(workspace -> workspace.getRelations().stream()
						.filter(reference -> reference.getElement() == this)
						.collect(Collectors.toList())
						.forEach(RelationReference::drop)
				);
	}

	/*public List<Attribute> getDataInfo(){
		if(infoVals.isEmpty()){
			getDataInfo(infoVals);
		}
		return infoVals;
	}*/

	public void getDataInfo(List<Attribute> list) {
		list.add(new Attribute(this, "Columns", UNKNOWN));
		list.add(new Attribute(this, "Rows", UNKNOWN));
		list.add(new Attribute(this, "Size (data / index)", UNKNOWN));
		//infoVals.add(new Attribute(this, "Size (toast table)", UNKNOWN));
		list.add(new Attribute(this, "Primary key", UNKNOWN));
		list.add(new Attribute(this, "Triggers", UNKNOWN));
		list.add(new Attribute(this, "Indexes", UNKNOWN));

		Attribute a;

		// Columns
		a = list.get(0);
		a.setTypeColor(UIConstants.Colors.getLabelForeground());
		a.getBehavior().setAttType(String.valueOf(attributes.size()));
		if(attributes.size() >= ATTRIBUTE_COUNT_LIMIT){
			a.setTypeColor(Color.red);
		}
		a.checkSize();

		// Rows
		a = list.get(1);
		a.setTypeColor(UIConstants.Colors.getLabelForeground());
		a.getBehavior().setAttType(Geometry.getReadableCountColorized(countRows, a));
		a.checkSize();

		// Size (data / indexes)
		a = list.get(2);
		a.setTypeColor(UIConstants.Colors.getLabelForeground());
		a.getBehavior().setAttType(Geometry.getReadableSizeColorized(sizeTotal, a)+" ("+
					Geometry.getReadableSizeColorized(sizeTotal-sizeIndexes, a)+" / "+
					Geometry.getReadableSizeColorized(sizeIndexes, a)+")");
		a.checkSize();

		// Primary key
		a = list.get(3);
		if(pkCols == null || pkCols.length == 0){
			a.getBehavior().setAttType("no");
			a.setTypeColor(Color.RED);
		}else{
			a.getBehavior().setAttType("yes");
			a.setTypeColor(UIConstants.Colors.GREEN);
		}
		a.checkSize();

		// Triggers
		a = list.get(4);
		a.getBehavior().setAttType(String.valueOf(triggers.size()));
		if(triggers.size() >= TRIGGER_COUNT_LIMIT){
			a.setTypeColor(Color.RED);
		}else{
			a.setTypeColor(UIConstants.Colors.getLabelForeground());
		}
		a.checkSize();

		// Triggers
		a = list.get(5);
		a.getBehavior().setAttType(Integer.toString(indexes.size()));
		if(indexes.size() >= INDEX_COUNT_LIMIT){
			a.setTypeColor(Color.RED);
		}else{
			a.setTypeColor(UIConstants.Colors.getLabelForeground());
		}
		a.checkSize();
	}

	public synchronized void syncInfoWithServer(){
		if(DesignGUI.getView() == ViewMode.DATA){
			setComponentsGray(getComponents());
			repaint();
			if(getDB().getProject().getCurrentWorkspace() != null){
				for(RelationReference r : getDB().getProject().getCurrentWorkspace().getRelations()){
					if(r.getElement().equals(Relation.this)){
						setComponentsGray(r.getComponents());
						r.repaint();
						break;
					}
				}
			}
		}
		if(isSelected){
			for(Component comp : Neighborhood.instance.canvas.getComponents()){
				if(comp instanceof NeighborhoodRelation /*&& ((DraggableComponent)comp).getFullName().equals(Relation.this.getFullName())*/){
					setComponentsGray(((RelationReference)comp).getAttributes().toArray());
					comp.repaint();
					break;
				}
			}
		}
		Schedule.reInvokeInWorker(Schedule.Named.RELATION_INFO, UIConstants.TYPE_TIMEOUT, () -> {
            try {
                ConnectionUtils.getCurrent(getDB().getName()).loadRelationInfo(Relation.this);
                getDB().getProject().save();

                if(DesignGUI.getView() == ViewMode.DATA){
                    infoVals.clear();
                    Canvas.instance.drawRelationDataInfo(Relation.this);
                    if(getDB().getProject().getCurrentWorkspace() != null){
                        for(RelationReference r : getDB().getProject().getCurrentWorkspace().getRelations()){
                            if(r.getElement().equals(Relation.this)){
                                Canvas.instance.drawRelationDataInfo(r);
                                break;
                            }
                        }
                    }
                }
                if(isSelected){
                    for(Component comp : Neighborhood.instance.canvas.getComponents()){
                        if(comp instanceof NeighborhoodRelation /*&& ((DraggableComponent)comp).getFullName().equals(Relation.this.getFullName())*/){
                            List<Attribute> attrs = ((RelationReference)comp).getAttributes();
                            attrs.clear();
                            getDataInfo(attrs);
							attrs.forEach(Attribute::checkSizeNoZoom);
                            comp.repaint();
                            break;
                        }
                    }
                }
            } catch (DBCommException e) {
                Dbg.notImportant("Loading table info failed. Leaving everything as is.", e);
            }
        });
	}

	private void setComponentsGray(Object[] components){
		for(Object comp: components){
			if(comp instanceof Attribute){
				Attribute attr = (Attribute) comp;
				attr.setTypeColor(Color.GRAY);
			}
		}
	}

	@Override
	public void checkSize(){
		countAttributes = attributes.size();
		checkComponentSize();
		if(!getDescr().isEmpty()){
			setToolTipText(getDescr());
		}
	}

	public void checkComponentSize(){
		displayName = behavior.name;
		displayNameWidth = UIConstants.GRAPHICS.getFontMetrics(Canvas.getTitleFont()).stringWidth(displayName);
		setSize(new Dimension(Canvas.ZOOMED_ENTITY_WIDTH, Geometry.getZoomed(24+countAttributes*Attribute.V_SIZE)));
		getIndexLines().checkSize();
	}

	@Override
	public void unSelect(){
		for(Attribute a: attributes){
			a.setDNDEnabled(false);
		}
		//int oldAttributes = countAttributes;
		//int newAttributes = attributes.size();
		boolean toRedrawProject = countAttributes > attributes.size();
		if(!attributes.isEmpty()){
			countAttributes = attributes.size();
		}
		checkSize();
		//Dbg.info("Unselecting "+(toRedrawProject?"with":"without")+" redraw, attrs: "+oldAttributes+" -> "+newAttributes);
		if(toRedrawProject && DesignGUI.getView() == ViewMode.DATA) {
			Schedule.inEDT(() -> {
                Canvas.instance.drawRelationDataInfo(Relation.this);
                schemaContainer.checkSize();
            });
		}
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		Workspace ws = Project.getCurrent().getOldWorkspace();
		if(ws != null){
			for(RelationReference rel : ws.getRelations()){
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
		return behavior.name;
	}

	@Override
	public String[] getAttributeNames(){
		String[] names = new String[attributes.size()];
		for(int i=0; i<attributes.size(); i++){
			names[i] = attributes.get(i).getName();
		}
		return names;
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
		attributes.forEach(elem -> elements.addAll(elem.getAllSubElements()));
		constraints.forEach(elem -> elements.addAll(elem.getAllSubElements()));
		triggers.forEach(elem -> elements.addAll(elem.getAllSubElements()));
		indexes.forEach(elem -> elements.addAll(elem.getAllSubElements()));
		return elements;
	}

	@Override
	public boolean isNew(){
		return behavior.isNew;
	}

	public boolean checkAddedElement(){
		if(haveAddedElement){
			haveAddedElement = false;
			return true;
		}else{
			return false;
		}
	}
	void setAddedElement(){
		haveAddedElement = true;
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

	public void assignToSchema(){
		schemaContainer.getRelations().add(this);
	}
	public void assignToSchema(Schema schema){
		this.schemaContainer = schema;
		assignToSchema();
	}

	public void checkInheritance(){
		for(Inheritance in: inheritances){
			if(in.getRel1()==this){
				inheritances.remove(in);
				((Relation)in.getRel2()).getInheritances().remove(in);
				break;
			}
		}
		if(behavior.getInheritParentName() != null && !behavior.getInheritParentName().isEmpty()){
			Relation inheritParent = schemaContainer.getDB().getRelationByFullName(behavior.getInheritParentName());
			if(inheritParent != null){
				Inheritance inh = new Inheritance(this, inheritParent);
				inheritParent.getInheritances().add(inh);
				inheritances.add(inh);
				Canvas.instance.drawProjectLater(true);
			}else{
				Dbg.fixme("Inherit parent not found: "+behavior.getInheritParentName());
			}
		}
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

	public boolean pKeyContains(String column) {
		for (int pkCol : pkCols) {
			for (Attribute attr : attributes) {
				if (attr.getAttNum() == pkCol && attr.getName().equals(column)) {
					return true;
				}
			}
		}
		return false;
	}
	public String getColType(String column) {
		for(Attribute attr: attributes){
			if(attr.getName().equals(column)){
				return attr.getBehavior().getAttType();
			}
		}
		return null;
	}

	private IndexLines getIndexLines(){
		if(indexLines == null){
			indexLines = createIndexLinePanel(this);
		}
		return indexLines;
	}
	public IndexLines getIndexLinePanel(){
		return getIndexLines();
	}
	public void checkIndexLinesSize(){
		getIndexLines().checkSize();
	}
	public IndexLines createIndexLinePanel(JComponent parent){
		return new IndexLines(parent);
	}

	@Override
	public Behavior getBehavior(){
		return behavior;
	}

	@Override
	public void setBehavior(IModelBehavior behavior) {
		this.behavior = (Behavior) behavior;
	}

	public void setDataChanged (Result res1, Result res2, HashMap<Integer, Integer> columnMap) {
		dataDifference = new DataDifference(this, res1, res2, columnMap);
	}

	public DataDifference getDataChanged(){
		return dataDifference;
	}

	public class Behavior extends AbstractModelBehavior<Behavior> {

		public static final String L_NAME = "Name";
		public static final String L_HAS_OIDS = "Has OIDs?";
		public static final String L_OPTIONS = "Options";
		public static final String L_OPERATIONS = "Actions";
		public static final String L_DESCR = "Comment";
		public static final String L_SCHEMA = "Schema";
		public static final String L_INHERITS = "Inherits";
		public static final String L_STORAGE = "Storage engine";
		public static final String L_COLLATION = "Collation";

		public static final String L_MN_REF_COL_1 = "Ref. column 1";
		public static final String L_MN_REF_COL_2 = "Ref. column 2";
		public static final String L_MN_PK_TYPE = "PKey type";
		//public static final String L_MN_PK_NAME = "PKey name";

		private String name;
		private String schemaName = "";
		private String inheritParentName;
		private String storage = "MyISAM";
		private String collation;
		private boolean hasOIDs = false;
		private String[] options = new String[0];
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

		public String getInheritParentName() {
			return inheritParentName;
		}

		public void setInheritParentName(String inheritParentName) {
			this.inheritParentName = inheritParentName;
		}

		public String getStorage() {
			return storage;
		}

		public void setStorage(String storage) {
			this.storage = storage;
		}

		public String getCollation() {
			return collation;
		}

		public void setCollation(String collation) {
			this.collation = collation;
		}

		public boolean hasOIDs() {
			return hasOIDs;
		}

		public void setHasOIDs(boolean hasOIDs) {
			this.hasOIDs = hasOIDs;
		}

		public String[] getOptions() {
			return options;
		}

		public void setOptions(String[] options) {
			this.options = options;
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
			valuesForEdit.inheritParentName = inheritParentName;
			valuesForEdit.storage = storage;
			valuesForEdit.collation = collation;
			valuesForEdit.hasOIDs = hasOIDs;
			valuesForEdit.options = options;
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
				behavior.inheritParentName = inheritParentName;
				behavior.storage = storage;
				behavior.collation = collation;
				behavior.hasOIDs = hasOIDs;
				behavior.options = options;
				behavior.descr = descr;
				if(behavior.isNew){
					setSchemaByName(schemaName);
					assignToSchema();
					behavior.isNew = false;
					behavior.isDropped = false;
				}else if(schemaChanged){
					getSchema().getRelations().remove(Relation.this);
					setSchemaByName(schemaName);
					assignToSchema();
				}
				checkInheritance();
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
				case L_INHERITS:
					inheritParentName = value;
					break;
				case L_STORAGE:
					storage = value;
					break;
				case L_COLLATION:
					collation = value;
					break;
				case L_DESCR:
					descr = value;
					break;
				case L_OPTIONS:
					options = value.split(",\\s*");
					break;
			}
		}

		@Override
		public void notifyChange(String elementName, boolean value) {
			if(elementName.equals(L_HAS_OIDS)){
				hasOIDs = value;
			}
		}

		@Override
		public void notifyChange(String elementName, boolean[] values) {
			if(elementName.equals(L_OPERATIONS)){
				isDropped = values[values.length-1];
			}
		}
	}

	public class IndexLines extends JComponent {
		private final JComponent rel;
		private transient BufferedImage imageCache;

		IndexLines(JComponent parent) {
			rel = parent;
		}

		public void checkSize(){
			setLocation(new Point(rel.getLocation().x - Index.DISTANCE_FROM_TABLE, rel.getLocation().y));
			setSize(new Dimension(Index.DISTANCE_FROM_TABLE, rel.getHeight()));
			updateCache();
		}

		void updateCache() {
			imageCache = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = imageCache.createGraphics();
			if(Settings.getBool(Settings.L_PERFORM_ANTIALIASING)) {
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			}
			if(Canvas.getZoomNotTooSmall() && !attributes.isEmpty()) {
				for(Index ind: indexes){
					paintIndex(graphics, ind);
				}
			}
			repaint();
			referenceListeners.forEach(ReferenceListener::notifyReference);
		}

		public BufferedImage getImageCache() {
			return imageCache;
		}

		public void setImageCache(BufferedImage imageCache) {
			this.imageCache = imageCache;
		}

		@Override
		protected void paintComponent(Graphics g) {
			g.drawImage(imageCache, 0, 0, null);
		}

		private void paintIndex(Graphics2D graphics, Index ind) {
			final int offset = Geometry.getZoomed(8);
			final int attrVSize = Geometry.getZoomed(Attribute.V_SIZE);

			try {
				int least = 100000;
				int most = 0;
				for (Attribute attribute : ind.getAttributes()) {
					int pos = attributes.indexOf(attribute) + 1;
					if(pos < least){
						least = pos;
					}
					if(pos > most){
						most = pos;
					}
					int top = offset + pos * attrVSize;

					// Draw selection background
					if (ind.isSelected()) {
						graphics.setColor(UIConstants.Colors.getSelectionBackground());
						graphics.setStroke(Index.STROKE_SELECT);
						graphics.drawLine(1, top, getWidth(), top);

					} else {
						// Draw connection to the attribute
						if (ind.getBehavior().isUnique()) {
							graphics.setColor(Index.COLOR_UNIQUE);
							graphics.setStroke(Index.STROKE_UNIQUE);
						} else {
							graphics.setColor(Index.COLOR_INDEX);
							graphics.setStroke(Index.STROKE_INDEX);
						}
						graphics.drawLine(1, top, getWidth(), top);
					}
				}
				if(least != most){
					// Draw selection between multiple attributes
					if (ind.isSelected()) {
						graphics.setColor(UIConstants.Colors.getSelectionBackground());
						graphics.setStroke(Index.STROKE_SELECT);
						graphics.drawLine(1, offset + least * attrVSize, 1, offset + most * attrVSize);

					} else {
						// Draw connection between multiple attributes
						if (ind.getBehavior().isUnique()) {
							graphics.setColor(Index.COLOR_UNIQUE);
							graphics.setStroke(Index.STROKE_UNIQUE);
						} else {
							graphics.setColor(Index.COLOR_INDEX);
							graphics.setStroke(Index.STROKE_INDEX);
						}
						graphics.drawLine(1, offset + least * attrVSize, 1, offset + most * attrVSize);
					}
				}
			} catch (ConcurrentModificationException e){
				Dbg.notImportant("This could be handled by defensive copies. Ignoring for now.", e);
			}
		}

		@Override
		public String toString() {
			return "IndexLines for " + Relation.this.getFullName();
		}
	}
}
