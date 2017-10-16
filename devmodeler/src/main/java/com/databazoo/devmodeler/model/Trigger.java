
package com.databazoo.devmodeler.model;

import com.databazoo.components.elements.LineComponent;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.SQLOutputConfig;
import com.databazoo.devmodeler.conn.SQLOutputConfigExport;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.RightClickMenu;
import com.databazoo.devmodeler.model.reference.LineComponentReference;
import com.databazoo.devmodeler.tools.comparator.Comparator;
import com.databazoo.devmodeler.wizards.relation.RelationWizard;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Usage;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.databazoo.devmodeler.gui.UsageElement.COPY_FULL;
import static com.databazoo.devmodeler.gui.UsageElement.TRIGGER_CONTEXT_COPY;
import static com.databazoo.devmodeler.gui.UsageElement.TRIGGER_CONTEXT_DROP;
import static com.databazoo.devmodeler.gui.UsageElement.TRIGGER_CONTEXT_EDIT;
import static com.databazoo.devmodeler.gui.UsageElement.TRIGGER_CONTEXT_SOURCE;
import static com.databazoo.devmodeler.gui.UsageElement.TRIGGER_DOUBLE_CLICKED;

/**
 * Model representation of triggers.
 * Visually a line component that connects tables and functions.
 *
 * @author bobus
 */
public class Trigger extends LineComponent implements IModelElement {
	public static final String L_CLASS		= "Trigger";
	public static final Icon ico16			= Theme.getSmallIcon(Theme.ICO_TRIGGER_NA);
	private static final Icon ico16Enabled	= Theme.getSmallIcon(Theme.ICO_TRIGGER_ACT);
	public static final Color LINE_COLOR	= Color.decode("#40A040");

	public static final String[] STATEMENT_OPTIONS	= new String[]{"ROW", "STATEMENT"};
	public static final String[] TRIGGER_OPTIONS	= new String[]{"BEFORE", "AFTER", "INSTEAD OF"};
	public static final String[] EVENT_OPTIONS		= new String[]{"INSERT", "UPDATE", "DELETE", "TRUNCATE"};

	private int isDifferent = Comparator.NO_DIFF;
	private final boolean hasBody;

	private Behavior behavior = new Behavior();
	private DB database;
	private final Set<IModelElement> elements = new HashSet<>();

	public Trigger(DB parent, String fullName, String def, String cols, String when, boolean isEnabled, String descr) {
		//name = name.replaceAll("\"", "");
		//fullName = fullName.replaceAll("\"", "");
		if(fullName.contains(".")){
			Schema schema = parent.getSchemaFromElement(fullName);
			if(schema != null){
				behavior.name = fullName.replaceFirst(Pattern.quote(schema.getSchemaPrefixWithPublic()), "");
				behavior.schemaName = schema.getName();
			}else{
				behavior.name = fullName;
			}
		}else{
			behavior.name = fullName;
		}
		behavior.def = def;
		behavior.descr = descr;
		behavior.attrs = cols;
		behavior.when = when;
		behavior.isEnabled = isEnabled;
		this.database = parent;

		this.isDashed = false;
		this.lineColor = LINE_COLOR;

		this.hasBody = parent.getConnection().isSupported(SupportedElement.TRIGGER_BODY);

		addDragListeners();
	}

	public boolean hasBody() {
		return hasBody;
	}

	@Override
	public void clicked(){
		Canvas.instance.setSelectedElement(this);
	}

	@Override
	public void doubleClicked(){
		Usage.log(TRIGGER_DOUBLE_CLICKED, "View: " + DesignGUI.getView().name());
		RelationWizard.get(null).drawProperties((Relation)rel1, getName());
	}

	@Override
	public void rightClicked(){
		rightClicked(null);
	}

	@Override
	public void rightClicked(final String workspaceName){
		RightClickMenu.get((type, selectedValue) -> {
            switch(type){
                case 10:
					Usage.log(TRIGGER_CONTEXT_EDIT);
                	RelationWizard.get(workspaceName).drawProperties((Relation)rel1, getName());
                	break;
                case 30:
					Usage.log(TRIGGER_CONTEXT_COPY);
                	DesignGUI.toClipboard(Trigger.this.getName());
                	break;
                case 31:
					Usage.log(TRIGGER_CONTEXT_COPY, COPY_FULL);
                	DesignGUI.toClipboard(Trigger.this.getFullName());
                	break;
                case 32:
					Usage.log(TRIGGER_CONTEXT_SOURCE);
                	DesignGUI.toClipboard(Trigger.this.getQueryCreateClear(getConnection()));
                	break;
                case 61:
					Usage.log(TRIGGER_CONTEXT_DROP);
                	RelationWizard.get(workspaceName).enableDrop().drawProperties((Relation)rel1, getName());
                	break;
				default: throw new IllegalArgumentException("Menu option " + type + " is not known");
            }
        }).
		addItem("Properties", RightClickMenu.ICO_EDIT, 10).
		separator().
		addItem("Copy name", RightClickMenu.ICO_COPY, 30).
		addItem("Copy name with schema", RightClickMenu.ICO_COPY, 31).
		addItem("Copy source code", RightClickMenu.ICO_COPY, 32).
		separator().
		addItem("DROP...", RightClickMenu.ICO_DELETE, 61);
	}

	@Override
	protected boolean haveClickableChild(Point p, int clickMask){
		int x = p.x + getLocation().x;
		int y = p.y + getLocation().y;
		for(Constraint con: getDB().getConstraints()){
			if(con.clickedOnLine(x, y))
			{
				if(clickMask == CLICK_TYPE_LEFT) {
					con.clicked();
				}else if(clickMask == CLICK_TYPE_DOUBLE) {
					con.doubleClicked();
				}else{
					con.rightClicked();
				}
				return true;
			}
		}
		for(Trigger trig: getDB().getTriggers()){
			if(trig.clickedOnLine(x, y))
			{
				if(clickMask == CLICK_TYPE_LEFT) {
					trig.clicked();
				}else if(clickMask == CLICK_TYPE_DOUBLE) {
					trig.doubleClicked();
				}else{
					trig.rightClicked();
				}
				return true;
			}
		}
		Canvas.instance.setSelectedElement(null);
		if(clickMask == CLICK_TYPE_LEFT) {
			getDB().clicked();
		}else if(clickMask == CLICK_TYPE_DOUBLE) {
			getDB().doubleClicked();
		}else{
			getDB().rightClicked();
		}
		return true;
	}

	@Override
	public DefaultMutableTreeNode getTreeView(boolean showCreateIcons){
		DefaultMutableTreeNode trigger = new DefaultMutableTreeNode(this);
		if(!hasBody) {
			trigger.add(((IModelElement)rel2).getTreeView(showCreateIcons));
		}
		return trigger;
	}

	@Override
	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching) {
		DefaultMutableTreeNode trigger = new DefaultMutableTreeNode(this);
		if(!hasBody) {
			DefaultMutableTreeNode child = ((IModelElement)rel2).getTreeView(search, fulltext, searchNotMatching);
			if(child != null) {
				trigger.add(child);
			}
		}
		boolean nameMatch = getFullName().matches(search);
		boolean excludedByName = searchNotMatching && nameMatch;
		if (!excludedByName && (!trigger.isLeaf() || searchNotMatching || nameMatch || (
				fulltext && getDB().getProject().getCurrentConn().getQueryCreate(this, null).matches(search)
		))) {
			return trigger;
		} else {
			return null;
		}
	}

	@Override
	public String getName(){
		return behavior.name;
	}

	@Override
	public String getFullName(){
		return rel1 != null ? rel1.getFullName() + "." + behavior.name : behavior.name;
	}

	@Override
	public String getEditedFullName(){
		Behavior edit = behavior.valuesForEdit;
		return rel1 != null ? rel1.getFullName() + "." + edit.name : edit.name;
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
		if(config.exportTriggers){
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

	public void setRelFuncByNameDef(DB db, String relFullName, String funcDef, boolean isTemp) {
		if(!isTemp){
			this.database.getTriggers().remove(this);
		}
		if(rel1 != null){
			((Relation)rel1).getTriggers().remove(this);
		}
		if(rel2 != null){
			((Function)rel2).getTriggers().remove(this);
		}
		rel1 = null;
		rel2 = null;

		if(funcDef == null){
			funcDef = behavior.def;
		}
		//relFullName = Connection.escapeFullName(relFullName);
		for (Schema s: db.getSchemas()) {
			for(Relation rel: s.getRelations()){
				if(rel.getFullName().equals(relFullName)){
					setRel1(rel);
					break;
				}
			}
			if(!hasBody){
				for(Function f: s.getFunctions()){
					String pattern = ".*EXECUTE PROCEDURE\\s*\"?"+f.getFullName()+"\"?\\s*\\(.*";
					if(funcDef.matches(pattern) || funcDef.equals(f.getFullName())){
						setRel2(f);
						behavior.procname = f.getFullName();
						break;
					}
				}
			}
			if(isReady()){
				break;
			}
		}
		if(!isReady()){
			Dbg.fixme("Trigger "+getFullName()+" incomplete. Rel: "+relFullName+(rel1==null ? "" : " found")+" Func: "+funcDef+(rel2==null ? "" : " found"));
		}
		if(isReady() && !isTemp){
			assignToRels();
		}
	}

	@Override
	public boolean isReady(){
		return rel1 != null && (hasBody || rel2 != null);
	}

	public void assignToRels(){
		((Relation)rel1).getTriggers().add(this);
		if(!hasBody){
			((Function)rel2).getTriggers().add(this);
		}
		getDB().getTriggers().add(this);
	}

	@Override
	public void drop(){
		if(rel1 != null){
			((Relation)rel1).getTriggers().remove(this);
			((IModelElement)rel1).getDB().getTriggers().remove(this);
		}
		if(rel2 != null){
			((Function)rel2).getTriggers().remove(this);
			//((Function)rel2).getSchema().remove(this);
		}
		getDB().getProject().getWorkspaces()
				.forEach(workspace -> workspace.getTriggers().stream()
						.filter(reference -> reference.getElement() == this)
						.collect(Collectors.toList())
						.forEach(LineComponentReference::drop)
				);
	}

	@Override
	public void unSelect(){

	}

	@Override
	public IConnection getConnection(){
		return database.getConnection();
	}

	@Override
	public DB getDB(){
		return database;
	}
	public void setDB(DB parent) {
		database = parent;
	}

	public void checkColumns(int[] cols){
		String attrStr = "";
		String comma = "";
		List<Attribute> tableAttrs = ((Relation)rel1).getAttributes();
		for (int col : cols) {
			if (col > 0) {
				for (Attribute a : tableAttrs) {
					if (a.getAttNum() == col) {
						attrStr += comma + a.getName();
						comma = ", ";
						break;
					}
				}
			}
		}
		behavior.attrs = attrStr;
	}

	@Override
	public Icon getIcon16(){
		return behavior.isEnabled ? ico16Enabled : ico16;
	}
	@Override
	public String toString(){
		return behavior.name;
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

	@Override
	public int compareTo(IModelElement t) {
		return getName().compareTo(t.getName());
	}

	@Override
	public String getFullPath(){
		return getDB().getName()+"."+rel1.getFullName();
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

		private static final char INSERT = 'i';
		private static final char UPDATE = 'u';
		private static final char DELETE = 'd';
		private static final char TRUNCATE = 't';

		private static final char BEFORE = 'b';
		private static final char AFTER = 'a';
		private static final char INSTEAD = 'i';

		public static final String L_NAME = "Name";
		public static final String L_TRIGGER = "Trigger";
		public static final String L_EVENT = "Events";
		public static final String L_FOR_EACH = "For each";
		public static final String L_COLUMNS = "Change of";
		public static final String L_EXECUTE = "Execute";
		public static final String L_WHEN = "Only when";
		public static final String L_ENABLED = "Enabled?";
		public static final String L_OPERATIONS = "Actions";
		public static final String L_DESCR = "Comment";
		public static final String L_BODY = "Trigger body";

		private String name;
		private String schemaName;
		private String def;
		private String procname;
		private String attrs = "";
		private String when = "";
		private String descr = "";
		private boolean isEnabled = true;

		private char timing = AFTER;
		private char[] events = new char[0];
		private boolean isRowType = true;

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

		public String getDef() {
			return def;
		}

		public void setDef(String def) {
			this.def = def;
		}

		public String getProcname() {
			return procname;
		}

		void setProcname(String procname) {
			this.procname = procname;
		}

		/*public void setAttrs(String attrs) {
			this.attrs = attrs;
		}*/

		public void setWhen(String when) {
			this.when = when;
		}

		public String getDescr() {
			return descr != null ? descr : "";
		}

		public void setDescr(String descr) {
			this.descr = descr;
		}

		public boolean isEnabled() {
			return isEnabled;
		}

		public void setEnabled(boolean enabled) {
			isEnabled = enabled;
		}

		public void setTiming(char timing) {
			this.timing = timing;
		}

		public char[] getEvents() {
			return events;
		}

		public void setEvents(char[] events) {
			this.events = events;
		}

		public boolean isRowType() {
			return isRowType;
		}

		public void setRowType(boolean rowType) {
			isRowType = rowType;
		}

		/**
		 * Bits:
		 * 7 (64): INSTEAD OF?
		 * 6 (32): TRUNCATE?
		 * 5 (16): UPDATE?
		 * 4 (8): DELETE?
		 * 3 (4): INSERT?
		 * 2 (2): AFTER (0) or BEFORE (1)?
		 * 1 (1): Row trigger?
		 *
		 * @param tgtype PosgreSQL trigger behavior bitmap
		 */
		public void setTiming(int tgtype){
			isRowType = (1 & tgtype) > 0;

			if((64 & tgtype) > 0){
				timing = INSTEAD;
			}else{
				timing = (2 & tgtype) > 0 ? BEFORE : AFTER;
			}

			events = new char[
					(int)Math.signum(4 & tgtype) +
					(int)Math.signum(8 & tgtype) +
					(int)Math.signum(16 & tgtype) +
					(int)Math.signum(32 & tgtype)
				];
			int i = 0;
			if((4 & tgtype) > 0){
				events[i++] = INSERT;
			}
			if((8 & tgtype) > 0){
				events[i++] = DELETE;
			}
			if((16 & tgtype) > 0){
				events[i++] = UPDATE;
			}
			if((32 & tgtype) > 0){
				events[i] = TRUNCATE;
			}
		}

		public String getTiming(){
			return getTiming(" ");
		}

		public char getTimingChar(){
			return timing;
		}

		public String getTiming(String glue){
			StringBuilder ret = new StringBuilder();
			switch(timing){
				case INSTEAD: ret.append("INSTEAD OF"); break;
				case BEFORE: ret.append("BEFORE"); break;
				default: ret.append("AFTER");
			}
			ret.append(" ");
			for(int i=0; i < events.length; i++){
				if(i > 0) {
					ret.append(glue);
				}
				switch(events[i]){
					case INSERT: ret.append("INSERT"); break;
					case DELETE: ret.append("DELETE"); break;
					case UPDATE: ret.append("UPDATE"); break;
					case TRUNCATE: ret.append("TRUNCATE"); break;
				}
			}
			return ret.toString();
		}

		public String getRowType(){
			if(isRowType){
				return "ROW";
			}else{
				return "STATEMENT";
			}
		}
		public String getAttrs(){
			return attrs == null ? "" : attrs;
		}
		public String getWhen(){
			return when == null ? "" : when;
		}

		public String getTimingPart(){
			switch(timing){
				case INSTEAD: return "INSTEAD OF";
				case BEFORE: return "BEFORE";
				default: return "AFTER";
			}
		}
		public boolean[] getEventsPart(){
			boolean ret[] = new boolean[]{false, false, false, false};
			for (char event : events) {
				switch (event) {
					case INSERT:
						ret[0] = true;
						break;
					case UPDATE:
						ret[1] = true;
						break;
					case DELETE:
						ret[2] = true;
						break;
					case TRUNCATE:
						ret[3] = true;
						break;
				}
			}
			return ret;
		}
		/*public String[] getTimingEvents(){
			String ret[] = new String[events.length];
			for(int i=0; i < events.length; i++){
				switch(events[i]){
					case INSERT: ret[i] = "INSERT"; break;
					case DELETE: ret[i] = "DELETE"; break;
					case UPDATE: ret[i] = "UPDATE"; break;
					case TRUNCATE: ret[i] = "TRUNCATE"; break;
				}
			}
			return ret;
		}*/

		@Override
		public Behavior prepareForEdit(){
			valuesForEdit = new Behavior();
			valuesForEdit.name = name;
			valuesForEdit.schemaName = schemaName;
			valuesForEdit.def = def;
			valuesForEdit.timing = timing;
			valuesForEdit.isRowType = isRowType;
			valuesForEdit.events = events;
			valuesForEdit.procname = procname;
			valuesForEdit.attrs = attrs;
			valuesForEdit.when = when;
			valuesForEdit.isEnabled = isEnabled;
			valuesForEdit.descr = descr;
			return valuesForEdit;
		}

		@Override
		public void saveEdited(){
			if(isDropped){
				drop();
			}else{
				if(hasBody){
					if(behavior.isNew){
						setRelFuncByNameDef(getDB(), rel1.getFullName(), null, false);
					}
				}else if(!behavior.procname.equals(procname) || behavior.isNew){
					setRelFuncByNameDef(getDB(), rel1.getFullName(), procname, false);
					((Relation)rel1).setAddedElement();
				}
				behavior.name = name;
				behavior.schemaName = schemaName;
				behavior.def = def;
				behavior.timing = timing;
				behavior.isRowType = isRowType;
				behavior.events = events;
				behavior.procname = procname;
				behavior.attrs = attrs;
				behavior.when = when;
				behavior.isEnabled = isEnabled;
				behavior.descr = descr;
				if(behavior.isNew){
					((Relation)rel1).setAddedElement();
					behavior.isNew = false;
					behavior.isDropped = false;
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
				case L_DESCR:
					descr = value;
					break;
				case L_TRIGGER:
					switch (value) {
						case "AFTER":
							timing = AFTER;
							break;
						case "BEFORE":
							timing = BEFORE;
							break;
						default:
							timing = INSTEAD;
							break;
					}
					break;
				case L_FOR_EACH:
					isRowType = value.equals("ROW");
					break;
				case L_EXECUTE:
					procname = value;
					break;
				case L_COLUMNS:
					attrs = value;
					break;
				case L_WHEN:
					when = value;
					break;
				case L_BODY:
					def = value;
					break;
			}
		}

		@Override
		public void notifyChange(String elementName, boolean value) {
			if(elementName.equals(L_ENABLED)){
				isEnabled = value;
			}
		}

		@Override
		public void notifyChange(String elementName, boolean[] values) {
			switch (elementName) {
				case L_EVENT:
					int size = 0;
					for (boolean value : values) {
						if (value) {
							size++;
						}
					}
					events = new char[size];
					int evt = 0;
					for(int i=0; i<values.length; i++){
						if(values[i]){
							switch(i){
								case 0: events[evt] = INSERT; break;
								case 1: events[evt] = UPDATE; break;
								case 2: events[evt] = DELETE; break;
								case 3: events[evt] = TRUNCATE; break;
							}
							evt++;
						}
					}
					break;
				case L_OPERATIONS:
					if(values.length > 1) {
						isEnabled = !values[values.length-2];
					}	isDropped = values[values.length-1];
					break;
			}
		}
	}
}
