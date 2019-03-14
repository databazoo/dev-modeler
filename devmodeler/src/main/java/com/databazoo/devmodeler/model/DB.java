package com.databazoo.devmodeler.model;

import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.ConnectionUtils;
import com.databazoo.devmodeler.conn.DBCommException;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.SQLOutputConfig;
import com.databazoo.devmodeler.conn.SQLOutputConfigExport;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.Menu;
import com.databazoo.devmodeler.gui.RightClickMenu;
import com.databazoo.devmodeler.gui.window.datawindow.DataWindow;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.tools.Geometry;
import com.databazoo.devmodeler.tools.organizer.Organizer;
import com.databazoo.devmodeler.tools.organizer.OrganizerFactory;
import com.databazoo.devmodeler.wizards.relation.RelationWizard;
import com.databazoo.tools.Dbg;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Model representation of databases.
 *
 * @author bobus
 */
public class DB implements IModelElement {
	public static final String L_CLASS = "Database";
	public static Icon ico16 = Theme.getSmallIcon(Theme.ICO_DATABASE);

	private static final Map<String, Map<String,Point>> KNOWN_LOCATIONS = new HashMap<>();

	private final List<Schema> schemas = new ArrayList<>();
	private final List<Constraint> constraints = new ArrayList<>();
	private final List<Trigger> triggers = new ArrayList<>();
	private IConnection conn;
	private boolean assignLines = false;
	private Project project;
	private Behavior behavior = new Behavior();

	public DB(Project p, String name) {
		behavior.name = name;
		this.project = p;
	}
	public DB(Project p, IConnection conn, String name) {
		behavior.name = name;
		this.conn = conn;
		this.project = p;
	}

	public List<Schema> getSchemas() {
		return schemas;
	}

	public List<Constraint> getConstraints() {
		return constraints;
	}

	public List<Trigger> getTriggers() {
		return triggers;
	}

	public Project getProject(){
		if(project == null) {
			project = Project.getCurrent();
		}
		return project;
	}

	public void setProject(Project p){
		project = p;
	}
	public boolean load(){
		Dbg.toFile("DB "+getName()+" is to be loaded from server, project is "+getProject());
		if(getProject() != null){
			int log = DesignGUI.getInfoPanel().write("Loading database details for "+getName());
			try {
				assignLines = true;
				getConnection().loadDB(this);
				for (Schema schema : schemas) {
					schema.getRelations().forEach(Relation::checkInheritance);
				}
				assignLines = false;
				DesignGUI.getInfoPanel().writeOK(log);
				return true;
			} catch (DBCommException e){
				Dbg.notImportantAtAll("Communication error. Already logged.", e);
				DesignGUI.getInfoPanel().writeFailed(log, getConnection().getCleanError(e.getMessage()));
			}
		}
		return false;
	}

	public void unload(){
		schemas.clear();
		constraints.clear();
		triggers.clear();
	}

	public boolean getAssignLines(){
		return assignLines;
	}

	@Override
	public DefaultMutableTreeNode getTreeView(boolean showCreateIcons){
		DefaultMutableTreeNode db = new DefaultMutableTreeNode(this);
		for (Schema schema : new ArrayList<>(schemas)) {
			DefaultMutableTreeNode child = schema.getTreeView(showCreateIcons);
			addSchemaNode(db, child);
		}
		return db;
	}

	@Override
	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching) {
		// Fail fast for name exclusion
		boolean nameMatch = getFullName().matches(search);
		if(searchNotMatching && nameMatch){
			return null;
		}

		DefaultMutableTreeNode db = new DefaultMutableTreeNode(this);
		for (Schema schema : new ArrayList<>(schemas)) {
			DefaultMutableTreeNode schemaNode = schema.getTreeView(search, fulltext, searchNotMatching);
			if(schemaNode != null) {
				addSchemaNode(db, schemaNode);
			}
		}
		if(!db.isLeaf() || searchNotMatching || nameMatch){
			return db;
		}else{
			return null;
		}
	}

	private void addSchemaNode(DefaultMutableTreeNode db, DefaultMutableTreeNode schemaNode) {
		if(getProject().getCurrentConn().isSupported(SupportedElement.SCHEMA)) {
            db.add(schemaNode);
        }else{
            while(schemaNode.getChildCount() > 0){
                db.add((MutableTreeNode)schemaNode.getChildAt(0));
            }
        }
	}

	@Override
	public String getName(){
		return behavior.name;
	}

	@Override
	public String getFullName(){
		return behavior.name;
	}

	@Override
	public String getEditedFullName() {
		return behavior.name;
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
	public String getQueryRecursive(SQLOutputConfigExport config){

		try {
			if(config.exportDatabases){
				config.updateLimit();
				config.append((config.conn != null ? config.conn : getConnection()).getQueryCreate(this, config));
			}

			for(Schema schema : schemas){
				schema.getQueryRecursive(config);
			}

			for(Constraint con : constraints){
				con.getQueryRecursive(config);
			}

			for(Trigger trig : triggers){
				trig.getQueryRecursive(config);
			}
		} catch (SQLOutputConfigExport.LimitReachedException ex){
			Dbg.notImportant("END OF PREVIEW", ex);
			config.append("/*************************************************** END OF PREVIEW ***************************************************/\n");
		}

		return config.getText();
	}

	@Override
	public String getQueryDrop(IConnection conn) {
		return conn.getQueryDrop(this);
	}

	@Override public void setDifferent(int isDifferent) {}
	@Override public int getDifference(){ return 0; }
	@Override public void drop(){
		// Not supported
	}
	@Override public void setSelected(boolean sel) {}
	@Override public void clicked(){}
	@Override public void doubleClicked(){}
	@Override
	public void rightClicked(){
		rightClicked(null);
	}

	@Override
	public void rightClicked(final String workspaceName){
		RightClickMenu menu = RightClickMenu.get((type, selectedValue) -> {
            switch(type){
                case 30: DesignGUI.toClipboard(getName()); break;
                case 32: DesignGUI.toClipboard(getQueryCreate(getConnection())); break;
                case 40:
                    int row = 4;
                    if(getConnection().isSupported(SupportedElement.FUNCTION)) {
                        row++;
                    }
                    if(getConnection().isSupported(SupportedElement.SEQUENCE)) {
                        row++;
                    }
                    RelationWizard w = RelationWizard.get(workspaceName);
                    w.setDatabase(DB.this);
                    w.drawProperties(schemas.size() > 0 ? schemas.get(0) : null, row);
                    break;
                case 50: DataWindow.get().drawQueryWindow("ANALYZE VERBOSE", DB.this, getConnection()); break;
                case 51: DataWindow.get().drawQueryWindow("VACUUM VERBOSE ANALYZE", DB.this, getConnection()); break;
                case 52: DataWindow.get().drawQueryWindow("VACUUM FULL VERBOSE ANALYZE", DB.this, getConnection()); break;
                case 55: DataWindow.get().drawQueryWindow(getConnection().getQueryVacuum("ANALYZE TABLE", schemas.get(0).getRelations()), DB.this, getConnection()); break;
                case 56: DataWindow.get().drawQueryWindow(getConnection().getQueryVacuum("OPTIMIZE TABLE", schemas.get(0).getRelations()), DB.this, getConnection()); break;
                case 57: DataWindow.get().drawQueryWindow(getConnection().getQueryVacuum("REPAIR TABLE", schemas.get(0).getRelations()), DB.this, getConnection()); break;
                case 80:
					final Project p = Project.getCurrent();
					final Organizer organizer = OrganizerFactory.get(selectedValue);
                    if(p.getCurrentWorkspace() == null) {
						organizer.organize(DB.this);
                    }else{
						organizer.organize(p.getCurrentWorkspace());
                    }
                    Canvas.instance.drawProject(true);
                    break;
				default: throw new IllegalArgumentException("Menu option " + type + " is not known");
            }
        });
		if(getConnection().isSupported(SupportedElement.SCHEMA)) {
			menu.addItem("Add schema", RightClickMenu.ICO_NEW, 40);
		}
		menu.separator().
			addItem("Copy name", RightClickMenu.ICO_COPY, 30).
			addItem("Copy source code", RightClickMenu.ICO_COPY, 32);
		menu.separator().
				addItem(
						Menu.L_REARRANGE_ITEMS,
						Theme.getSmallIcon(Theme.ICO_ORGANIZE),
						80,
						new String[]{Menu.L_REARRANGE_ALPHABETICAL, Menu.L_REARRANGE_CIRCULAR, /*Menu.L_REARRANGE_FORCE_BASED,*/ Menu.L_REARRANGE_NATURAL, "|", Menu.L_REARRANGE_EXPLODE, Menu.L_REARRANGE_IMPLODE}
						);
		if(getProject().getType() != Project.TYPE_ABSTRACT) {
			if(getConnection().isSupported(SupportedElement.VACUUM)){
				menu.separator().
				addItem("Analyze...", RightClickMenu.ICO_VACUUM, 50).
				addItem("Vacuum...", RightClickMenu.ICO_VACUUM, 51).
				addItem("Vacuum full...", RightClickMenu.ICO_VACUUM, 52);
			}else if(getConnection().isSupported(SupportedElement.OPTIMIZE)){
				menu.separator().
				addItem("Analyze...", RightClickMenu.ICO_VACUUM, 55).
				addItem("Optimize...", RightClickMenu.ICO_VACUUM, 56).
				addItem("Repair...", RightClickMenu.ICO_VACUUM, 57);
			}
		}
	}

	@Override public void checkSize(){}
	@Override public Dimension getSize(){ return null; }
	@Override public void unSelect(){}

	@Override
	public IConnection getConnection(){
		if(conn == null){
			return ConnectionUtils.getCurrent(behavior.name);
		}else {
			//Dbg.info("Using pre-defined connection "+conn.getFullName());
			return conn;
		}
	}

	@Override
	public DB getDB(){
		return this;
	}

	@Override
	public Icon getIcon16(){
		return ico16;
	}
	@Override
	public String toString(){
		return getName();
	}

	public Schema getSchemaByFullName(String fullName) {
		for(Schema schema: schemas){
			if(schema.getFullName().equals(fullName)){
				return schema;
			}
		}
		return null;
	}
	public Relation getRelationByFullName(String fullName) {
		for(Schema schema: schemas){
			for(Relation rel: schema.getRelations()){
				if(rel.getFullName().equals(fullName)){
					return rel;
				}
			}
		}
		return null;
	}
	public Function getFunctionByFullName(String fullName) {
		for(Schema schema: schemas){
			for(Function func: schema.getFunctions()){
				if(func.getFullName().equals(fullName)){
					return func;
				}
			}
		}
		return null;
	}
	public Package getPackageByFullName(String fullName) {
		for(Schema schema: schemas){
			for(Package pack: schema.getPackages()){
				if(pack.getFullName().equals(fullName)){
					return pack;
				}
			}
		}
		return null;
	}
	public View getViewByFullName(String fullName) {
		for(Schema schema: schemas){
			for(View view: schema.getViews()){
				if(view.getFullName().equals(fullName)){
					return view;
				}
			}
		}
		return null;
	}
	public Sequence getSequenceByFullName(String fullName) {
		for(Schema schema: schemas){
			for(Sequence seq: schema.getSequences()){
				if(seq.getFullName().equals(fullName)){
					return seq;
				}
			}
		}
		return null;
	}
	public Constraint getConstraintByFullName(String fullName) {
		for(Constraint constraint : constraints){
			if(constraint.getFullName().equals(fullName)){
				return constraint;
			}
		}
		return null;
	}
	public Trigger getTriggerByFullName(String fullName) {
		for(Trigger trigger : triggers){
			if(trigger.getFullName().equals(fullName)){
				return trigger;
			}
		}
		return null;
	}

	public String[] getSchemaNames(){
		String[] names = new String[schemas.size()];
		for(int i=0; i<schemas.size(); i++){
			names[i] = schemas.get(i).getName();
		}
		return names;
	}

	public String[] getRelationNames(){
		String[] names = new String[]{};
		for(Schema schema: schemas){
			String[] relNames = schema.getRelationNames();
			names = Geometry.concat(names, relNames);
		}
		return names;
	}

	public String[] getTriggerFunctionNames(){
		String[] names = new String[]{};
		for(Schema schema: schemas){
			String[] relNames = schema.getTriggerFunctionNames();
			names = Geometry.concat(names, relNames);
		}
		return names;
	}


	public Map<String,Relation> getRelationMap(){
		Map<String,Relation> map = new HashMap<>();
		for(Schema schema: schemas){
			map.putAll(schema.getRelationMap());
		}
		return map;
	}

	@Override public void repaint(){}

	@Override
	public String getDescr(){
		/*if(behavior.descr != null && !behavior.descr.equals("")){
			return behavior.descr;
		}else{*/
			return "";
		//}
	}

	@Override
	public Set<IModelElement> getAllSubElements() {
		Set<IModelElement> elements = new HashSet<>();
		elements.add(this);
		schemas.forEach(elem -> elements.addAll(elem.getAllSubElements()));
		constraints.forEach(elem -> elements.addAll(elem.getAllSubElements()));
		triggers.forEach(elem -> elements.addAll(elem.getAllSubElements()));
		return elements;
	}

	@Override
	public boolean isNew(){
		return false;
	}

	@Override
	public int compareTo(IModelElement t) {
		return getName().compareTo(t.getName());
	}

	@Override
	public String getFullPath(){
		return "";
	}

	@Override
	public String getClassName(){
		return L_CLASS;
	}

	public void setName(String newName) {
		behavior.name = newName;
	}

	public Map<String, Point> getLocations(){
		Map<String, Point> locations = KNOWN_LOCATIONS.get(getName());
		return locations != null ? locations : new HashMap<String, Point>();
	}
	public void saveKnownLocation(String fullName, Point rememberedLocation) {
		Map<String, Point> locationsInDB = getLocations();
		if(locationsInDB.isEmpty()){
			KNOWN_LOCATIONS.put(getName(), locationsInDB);
		}
		locationsInDB.put(fullName, rememberedLocation);
	}
	public Point getKnownLocation(String fullName) {
		return getLocations().get(fullName);
	}

	@Override
	public boolean isInEnvironment(IConnection env){
		return true;
	}

	@Override
	public void setInEnvironment(IConnection env, boolean isAvailable){}

	Schema getSchemaFromElement (String fullName) {
		for(Schema s : schemas){
			if(fullName.startsWith(s.getSchemaPrefixWithPublic())){
				return s;
			}
		}
		return null;
	}

	@Override
	public Behavior getBehavior() {
		return behavior;
	}

	@Override
	public void setBehavior(IModelBehavior behavior) {
		this.behavior = (Behavior) behavior;
	}

	public class Behavior extends AbstractModelBehavior<Behavior> {

		public static final String L_NAME = "Name";
		public static final String L_DESCR = "Comment";

		private String name;
		private String descr = "";

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescr() {
			return descr != null ? descr : "";
		}

		public void setDescr(String descr) {
			this.descr = descr;
		}

		@Override
		public Behavior prepareForEdit() {
			valuesForEdit = new Behavior();
			valuesForEdit.name = name;
			valuesForEdit.descr = descr;
			return valuesForEdit;
		}

		@Override
		public void saveEdited() {
			if (isDropped) {
				drop();
			} else {
				behavior.name = name;
				behavior.descr = descr;
				if (behavior.isNew) {
					behavior.valuesForEdit.isNew = true;
					behavior.isNew = false;
					behavior.isDropped = false;
				}
			}
		}

		@Override
		public void notifyChange(String elementName, String value) {
			switch (elementName) {
				case L_NAME:
					name = getConnection().isSupported(SupportedElement.ALL_UPPER) ? value.toUpperCase() : value;
					break;
				case L_DESCR:
					descr = value;
					break;
			}
		}

		@Override
		public void notifyChange(String elementName, boolean value) {
		}

		@Override
		public void notifyChange(String elementName, boolean[] values) {
		}
	}
}
