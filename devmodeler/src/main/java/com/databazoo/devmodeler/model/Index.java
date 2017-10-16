
package com.databazoo.devmodeler.model;

import com.databazoo.components.GCFrame;
import com.databazoo.components.UIConstants;
import com.databazoo.components.elements.DraggableComponentMouseListener;
import com.databazoo.components.elements.EnvironmentComponent;
import com.databazoo.components.text.SelectableText;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.DBCommException;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.SQLOutputConfig;
import com.databazoo.devmodeler.conn.SQLOutputConfigExport;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.gui.Canvas;
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
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.databazoo.devmodeler.gui.UsageElement.COPY_FULL;
import static com.databazoo.devmodeler.gui.UsageElement.INDEX_CONTEXT_COPY;
import static com.databazoo.devmodeler.gui.UsageElement.INDEX_CONTEXT_DROP;
import static com.databazoo.devmodeler.gui.UsageElement.INDEX_CONTEXT_EDIT;
import static com.databazoo.devmodeler.gui.UsageElement.INDEX_CONTEXT_SOURCE;
import static com.databazoo.devmodeler.gui.UsageElement.INDEX_DOUBLE_CLICKED;
import static com.databazoo.devmodeler.gui.UsageElement.SRC_REMOTE;

/**
 * Model representation of indexes, primary keys, unique indexes and unique constraints.
 * Displayed on canvas as an element attached to a table.
 *
 * @author bobus
 */
public class Index extends EnvironmentComponent implements IModelElement {
	public static final String L_CLASS		= "Index";
	public static final Icon ico16			= Theme.getSmallIcon(Theme.ICO_INDEX);
	public static final Icon icoPkey16		= Theme.getSmallIcon(Theme.ICO_PRIMARY_KEY);
	private static int NAME_FONT_SIZE 		= Settings.getInt(Settings.L_FONT_CANVAS_SIZE);
	private static Font NAME_FONT			= new Font(Font.SANS_SERIF, Font.PLAIN, NAME_FONT_SIZE);

	private static final String BTREE 		= "btree";

	static final Color COLOR_UNIQUE			= Color.decode("#FFAA00");
	static final Color COLOR_INDEX			= Color.decode("#FF4A00");
	static final BasicStroke STROKE_SELECT	= new BasicStroke(4.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
	static final BasicStroke STROKE_UNIQUE	= new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
	static final BasicStroke STROKE_INDEX	= new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 4.0f, new float[]{0.1f,4.0f}, 0.0f);

	static final int DISTANCE_FROM_TABLE = 10;

	public static void checkFontSize(){
		NAME_FONT_SIZE = Geometry.getZoomed(Settings.getInt(Settings.L_FONT_CANVAS_SIZE));
		NAME_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, NAME_FONT_SIZE);
	}

	private Relation relation;
	private List<Attribute> attributes = new ArrayList<>();
	private int isDifferent = Comparator.NO_DIFF;
	private boolean isSelected = false;

	private Behavior behavior = new Behavior();

	public Index(Relation relation, String fullName, String def, String where, String accessMethod, String[] options, boolean isunique, boolean isprimary, boolean isconstraint, String descr) {
		this.relation = relation;

		fullName = fullName.replaceAll("\"", "");
		if(fullName.contains(".")){
			behavior.name = fullName.replaceFirst(Pattern.quote(relation.getSchema().getSchemaPrefix()), "");
			behavior.schemaName = relation.getSchema().getName();
		}else{
			behavior.name = fullName;
		}
		behavior.def = def;
		if(where != null && !where.isEmpty()) {
			behavior.where = where.toCharArray()[0]=='{' ? getDB().getProject().getCurrentConn().getWhereFromDef(getInstance()) : where;
		}
		behavior.accessMethod = accessMethod.isEmpty() ? BTREE : accessMethod.toLowerCase();
		behavior.options = options;
		behavior.descr = descr;
		behavior.isUnique = isunique;
		behavior.isPrimary = isprimary;
		behavior.isConstraint = isconstraint;
		if(def.matches("(CREATE|PRIMARY KEY|UNIQUE).*") || def.matches(".*WHERE.*")){
			behavior.def = getDB().getProject().getCurrentConn().getCleanDef(getInstance());
		}
		if(!relation.getDB().getConnection().isSupported(SupportedElement.SCHEMA)){
			behavior.schemaName = "";
		}

		setSize(DISTANCE_FROM_TABLE + UIConstants.GRAPHICS.getFontMetrics(NAME_FONT).stringWidth(behavior.name)+2, Attribute.V_SIZE);
		addMouseMotionListener(new DraggableComponentMouseListener());
	}

	public Relation getRelation() {
		return relation;
	}

	public List<Attribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}

	public boolean isSelected() {
		return isSelected;
	}

	private Index getInstance(){
		return this;
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
		))) {
			return new DefaultMutableTreeNode(this);
		}else{
			return null;
		}
	}

	public void setRelation(Relation relation) {
		this.relation = relation;
		checkColumns();
	}

	public Relation getRel1(){
		return relation;
	}

	/*public String getOptionVector(){
		String ret = "";
		String glue = "";
		for (String option : behavior.options) {
			ret += glue + option;
			glue = " ";
		}
		return ret;
	}*/

	@Override
	public String getName(){
		return (behavior.name);
	}

	@Override
	public String getFullName(){
		return (behavior.schemaName.isEmpty() ? "" : behavior.schemaName+".") + behavior.name;
	}

	@Override
	public String getEditedFullName(){
		Behavior edit = behavior.valuesForEdit;
		return (edit.schemaName.isEmpty() ? "" : edit.schemaName+".") + edit.name;
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
		//conn.isForwardChange = true;
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
		//conn.isForwardChange = false;
		String change = conn.getQueryChanged(this);
		behavior = o;
		return change;
	}
	@Override
	public String getQueryRecursive(SQLOutputConfigExport config) throws SQLOutputConfigExport.LimitReachedException {
		if(config.exportIndexes){
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
	public void setSelected(boolean sel) {
		isSelected = sel;
		repaint();
		relation.getIndexLinePanel().updateCache();
	}

	@Override
	public void clicked(){
		Canvas.instance.setSelectedElement(this);
	}

	@Override
	public void doubleClicked(){
		Usage.log(INDEX_DOUBLE_CLICKED, "View: " + DesignGUI.getView().name());
		RelationWizard.get(null).drawProperties(relation, getName());
	}

	@Override
	public void rightClicked(){
		rightClicked(null);
	}

	@Override
	public void rightClicked(final String workspaceName){
		RightClickMenu menu = RightClickMenu.get((type, selectedValue) -> {
            switch (type) {
                case 10:
					Usage.log(INDEX_CONTEXT_EDIT);
                    RelationWizard.get(workspaceName).drawProperties(relation, getName());
                    break;
                case 30:
					Usage.log(INDEX_CONTEXT_COPY);
                    DesignGUI.toClipboard(Index.this.getName());
                    break;
                case 31:
					Usage.log(INDEX_CONTEXT_COPY, COPY_FULL);
                    DesignGUI.toClipboard(Index.this.getFullName());
                    break;
                case 32:
					Usage.log(INDEX_CONTEXT_SOURCE);
                    DesignGUI.toClipboard(Index.this.getQueryCreateClear(getConnection()));
                    break;
                case 39:
					Usage.log(INDEX_CONTEXT_SOURCE, SRC_REMOTE);
                    if(selectedValue.equals("Model")){
                        DesignGUI.toClipboard(Index.this.getQueryCreateClear(getConnection()));
                    }else{
						Schedule.inWorker(() -> {
                            try {
                                Project p = getDB().getProject();
                                IConnection c = p.getDedicatedConnection(getDB().getName(), p.getConnectionByName(selectedValue).getName());
                                if (c != null) {
                                    DesignGUI.toClipboard(c.loadDDLFromDB(Index.this));
                                }
                            } catch (DBCommException ex) {
								Dbg.fixme("Getting index DDL failed.", ex);
                                JOptionPane.showMessageDialog(GCFrame.getActiveWindow(), new SelectableText("Loading DDL failed\n\n" + ex.getMessage(), false), "Loading DDL failed", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                    break;
                case 61:
					Usage.log(INDEX_CONTEXT_DROP);
                    RelationWizard.get(workspaceName).enableDrop().drawProperties(relation, getName());
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

		menu.separator().
		addItem("DROP...", RightClickMenu.ICO_DELETE, 61);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D graphics = (Graphics2D) g;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if(isSelected){
			graphics.setPaint(Canvas.SELECTION_COLOR_A2);
			graphics.setStroke(Canvas.getLineStrokeFull(NAME_FONT_SIZE));
			graphics.drawLine(NAME_FONT_SIZE/2, getHeight()/2 - NAME_FONT_SIZE/3, getWidth() - DISTANCE_FROM_TABLE/2, getHeight()/2 - NAME_FONT_SIZE/3);
			graphics.setColor(Color.BLACK);
		}else {
			graphics.setColor(behavior.isUnique ? COLOR_UNIQUE : COLOR_INDEX);
		}

		graphics.setFont(NAME_FONT);
		graphics.drawString(behavior.name, DISTANCE_FROM_TABLE/3, getHeight()/2);
	}

	public void checkColumns(){
		attributes.clear();
		attributes.addAll(relation.getAttributes().stream()
				.filter(a -> behavior.def.matches("(?is)(^|.*[^a-z0-9_])" + Matcher.quoteReplacement(a.getName()) + "([^a-z0-9_].*|$)"))
				.collect(Collectors.toList()));
	}

	@Override
	public void checkSize(){
		checkSize(relation);
	}

	public void checkSize(JComponent parent) {
		if(relation != null){
			try {
				int least = 100000;
				int most = 0;
				for (Attribute attribute : attributes) {
					int pos = relation.getAttributes().indexOf(attribute) + 1;
					if(pos < least){
						least = pos;
					}
					if(pos > most){
						most = pos;
					}
				}
				int top = (int) (Geometry.getZoomed(Attribute.V_SIZE)*(least+most)*0.5d)+3;
				setSize(UIConstants.GRAPHICS.getFontMetrics(NAME_FONT).stringWidth(behavior.name)+DISTANCE_FROM_TABLE/2, Geometry.getZoomed(16));
				setLocation(parent.getLocation().x - getWidth() - DISTANCE_FROM_TABLE, parent.getLocation().y + top);
			} catch (ConcurrentModificationException e){
				Dbg.notImportant("This could be handled by defensive copies. Ignoring for now.", e);
			}
		}
		if(!getDescr().isEmpty()){
			setToolTipText(getDescr());
		}
	}

	@Override
	public void drop(){
		relation.getSchema().remove(this);
		relation.getIndexes().remove(this);
		if(behavior.isPrimary){
			relation.setPkCols(new int[0]);
		}
	}

	@Override
	public void unSelect(){
		relation.getIndexLinePanel().repaint();
	}

	@Override
	public IConnection getConnection(){
		return relation.getConnection();
	}

	@Override
	public final DB getDB(){
		return relation.getDB();
	}

	@Override
	public Icon getIcon16(){
		if(behavior.isPrimary){
			return icoPkey16;
		}else if(behavior.isConstraint){
			return Constraint.ico16;
		}else{
			return ico16;
		}
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
		Set<IModelElement> elements = new HashSet<>();
		elements.add(this);
		attributes.forEach(elem -> elements.addAll(elem.getAllSubElements()));
		return elements;
	}

	@Override
	public boolean isNew(){
		return behavior.isNew;
	}

	public void assignToRelation(Relation rel) {
		setRelation(rel);
		rel.getIndexes().add(this);
		if(behavior.isPrimary()){
			rel.setPkCols(new int[attributes.size()]);
			for(int i=0; i<attributes.size(); i++){
				rel.getPkCols()[i] = attributes.get(i).getAttNum();
			}
		}
	}

	@Override
	public int compareTo(IModelElement t) {
		return getName().compareTo(t.getName());
	}

	@Override
	public String getFullPath(){
		return getDB().getName()+"."+ relation.getFullName();
	}

	@Override
	public String getClassName(){
		return L_CLASS;
		//return behavior.isPrimary ? "PKey" : (behavior.isConstraint ? "Constraint" : "Index");
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
		public static final String L_METHOD = "Access method";
		public static final String L_UNIQUE = "Is unique?";
		public static final String L_COLUMNS = "Columns";
		public static final String L_CONDITION = "Condition";
		private static final String L_OPTIONS = "Options";
		public static final String L_OPERATIONS = "Actions";
		public static final String L_DESCR = "Comment";

		private String name;
		private String schemaName = "";
		private String def = "";
		private String where = "";
		private String accessMethod = BTREE;
		private String[] options = new String[0];
		private String descr = "";
		private boolean isUnique = false;
		private boolean isPrimary = false;
		private boolean isConstraint = false;

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

		public String getWhere() {
			return where;
		}

		public void setWhere(String where) {
			this.where = where;
		}

		public String getAccessMethod() {
			return accessMethod;
		}

		/*public void setAccessMethod(String accessMethod) {
			this.accessMethod = accessMethod;
		}*/

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

		public boolean isUnique() {
			return isUnique;
		}

		public void setUnique(boolean unique) {
			isUnique = unique;
		}

		public boolean isPrimary() {
			return isPrimary;
		}

		public void setPrimary(boolean primary) {
			isPrimary = primary;
		}

		public boolean isConstraint() {
			return isConstraint;
		}

		public void setConstraint(boolean constraint) {
			isConstraint = constraint;
		}

		@Override
		public Behavior prepareForEdit(){
			valuesForEdit = new Behavior();
			valuesForEdit.name = name;
			valuesForEdit.schemaName = schemaName;
			valuesForEdit.def = def;
			valuesForEdit.where = where;
			valuesForEdit.accessMethod = accessMethod;
			valuesForEdit.isUnique = isUnique;
			valuesForEdit.isPrimary = isPrimary;
			valuesForEdit.isConstraint = isConstraint;
			valuesForEdit.options = options;
			valuesForEdit.descr = descr;
			return valuesForEdit;
		}

		@Override
		public void saveEdited(){
			if(isDropped){
				drop();
			}else{
				boolean defChange = !behavior.def.equals(def);
				behavior.name = name;
				behavior.schemaName = schemaName;
				behavior.def = def;
				behavior.where = where;
				behavior.accessMethod = accessMethod;
				behavior.isUnique = isUnique;
				behavior.isPrimary = isPrimary;
				behavior.isConstraint = isConstraint;
				behavior.options = options;
				behavior.descr = descr;
				if(behavior.isNew){
					assignToRelation(relation);
					relation.setAddedElement();
					behavior.isNew = false;
					behavior.isDropped = false;
				}else if(defChange){
					checkColumns();
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
				case L_METHOD:
					accessMethod = value.equals("B-TREE") ? BTREE : value;
					break;
				case L_COLUMNS:
					def = value;
					break;
				case L_CONDITION:
					where = value;
					break;
				case L_OPTIONS:
					options = value.split(",\\s*");
					break;
			}
		}

		@Override
		public void notifyChange(String elementName, boolean value) {
			if(elementName.equals(L_UNIQUE)){
				isUnique = value;
			}
		}

		@Override
		public void notifyChange(String elementName, boolean[] values) {
			if(elementName.equals(L_OPERATIONS)){
				isDropped = values[values.length-1];
			}
		}

	}
}
