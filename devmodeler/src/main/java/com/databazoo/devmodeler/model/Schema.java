package com.databazoo.devmodeler.model;

import com.databazoo.components.FontFactory;
import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.SQLOutputConfig;
import com.databazoo.devmodeler.conn.SQLOutputConfigExport;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.*;
import com.databazoo.devmodeler.gui.Menu;
import com.databazoo.devmodeler.gui.window.datawindow.DataWindow;
import com.databazoo.devmodeler.model.reference.SchemaReference;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.tools.Geometry;
import com.databazoo.devmodeler.tools.comparator.Comparator;
import com.databazoo.devmodeler.tools.organizer.Organizer;
import com.databazoo.devmodeler.tools.organizer.OrganizerFactory;
import com.databazoo.devmodeler.wizards.relation.RelationWizard;
import com.databazoo.tools.Usage;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.databazoo.devmodeler.gui.UsageElement.*;

/**
 * Model representation of schemata.
 * Displayed on canvas as a draggable container.
 *
 * @author bobus
 */
public class Schema extends DraggableComponent implements IModelElement {
	public static final String L_CLASS = "Schema";
	public static final Icon ico16 = Theme.getSmallIcon(Theme.ICO_SCHEMA);

	private static final String CREATE_NEW_WORKSPACE = "Create new workspace";

	static final int RESIZE_INITIAL_VALUE = 1000000;

	private final List<Relation> relations = new ArrayList<>();
	private final List<Sequence> sequences = new ArrayList<>();
	private final List<Function> functions = new ArrayList<>();
	private final List<Package> packages = new ArrayList<>();
	private final List<View> views = new ArrayList<>();
	private DB db;

	private int isDifferent = Comparator.NO_DIFF;
	private Behavior behavior = new Behavior();

	public Schema(DB parent, String name, String descr) {
		super();
		this.db = parent;
		behavior.name = name;
		behavior.dbName = parent.getName();
		behavior.descr = descr;

		setFont(FontFactory.getSans(Font.BOLD + Font.ITALIC, 13));

		setToolTipText("Schema " + name);
		draw();
	}

	public List<Relation> getRelations() {
		return relations;
	}

	public List<Sequence> getSequences() {
		return sequences;
	}

	public List<Function> getFunctions() {
		return functions;
	}

	public List<Package> getPackages() {
		return packages;
	}

	public List<View> getViews() {
		return views;
	}

	private void draw(){
		setLayout(null);
		overbearingZOrder = 1;
	}

	@Override
	public void checkConstraints(){
		new ArrayList<>(relations).forEach(Relation::checkConstraints);
		new ArrayList<>(functions).forEach(Function::checkConstraints);
		new ArrayList<>(packages).forEach(Package::checkConstraints);
	}

	@Override
	public void clicked(){
		if(!isSelected){
			if(Canvas.instance.getSelectedElement() == null &&
					getDB().getConnection().isSupported(SupportedElement.SCHEMA)){
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
				scrollIntoView();
				Canvas.instance.setSelectedElement(this);
				DBTree.instance.selectSchemaByName(getName());
			}else{
				Canvas.instance.setSelectedElement(null);
			}
		}
	}

	@Override
	public void doubleClicked(){
		Usage.log(SCHEMA_DOUBLE_CLICKED, "View: " + DesignGUI.getView().name());
		RelationWizard.get(null).drawProperties(this, 1);
	}

	@Override
	public void rightClicked(){
		rightClicked(null);
	}

	@Override
	public void rightClicked(final String workspaceName){
		final IConnection conn = getDB().getProject().getCurrentConn();
		RightClickMenu menu = RightClickMenu.get((type, selectedValue) -> {
            Workspace ws;

            int offsetTable = conn.isSupported(SupportedElement.RELATION) ? 1 : 0;
            int offsetFunction = conn.isSupported(SupportedElement.FUNCTION) ? 1 : 0;
            int offsetView = conn.isSupported(SupportedElement.VIEW) ? 1 : 0;
            int offsetSequence = conn.isSupported(SupportedElement.SEQUENCE) ? 1 : 0;

            switch(type){
                case 10:
					Usage.log(SCHEMA_CONTEXT_EDIT);
                	RelationWizard.get(workspaceName).drawProperties(Schema.this, 1);
                	break;
                case 30:
					Usage.log(SCHEMA_CONTEXT_COPY);
                	DesignGUI.toClipboard(Schema.this.getName());
                	break;
                case 32:
					Usage.log(SCHEMA_CONTEXT_SOURCE);
                	DesignGUI.toClipboard(Schema.this.getQueryCreateClear(conn));
                	break;
                case 40:
					Usage.log(SCHEMA_CONTEXT_EDIT, "new table");
                	RelationWizard.get(workspaceName).drawProperties(Schema.this, 2);
                	break;
                case 41:
					Usage.log(SCHEMA_CONTEXT_EDIT, "new function");
                	RelationWizard.get(workspaceName).drawProperties(Schema.this, 2 + offsetTable);
                	break;
                case 42:
					Usage.log(SCHEMA_CONTEXT_EDIT, "new view");
                	RelationWizard.get(workspaceName).drawProperties(Schema.this, 2 + offsetTable + offsetFunction);
                	break;
                case 43:
					Usage.log(SCHEMA_CONTEXT_EDIT, "new sequence");
                	RelationWizard.get(workspaceName).drawProperties(Schema.this, 2 + offsetTable + offsetFunction + offsetView);
                	break;
                case 44:
					Usage.log(SCHEMA_CONTEXT_EDIT, "new package");
                	RelationWizard.get(workspaceName).drawProperties(Schema.this, 2 + offsetTable + offsetFunction + offsetView + offsetSequence);
                	break;
                case 50:
					Usage.log(SCHEMA_CONTEXT_MAINTAIN);
                	DataWindow.get().drawQueryWindow(conn.getQueryVacuum("ANALYZE VERBOSE", relations), getDB());
                	break;
                case 51:
					Usage.log(SCHEMA_CONTEXT_MAINTAIN);
                	DataWindow.get().drawQueryWindow(conn.getQueryVacuum("VACUUM VERBOSE ANALYZE", relations), getDB());
                	break;
                case 52:
					Usage.log(SCHEMA_CONTEXT_MAINTAIN);
                	DataWindow.get().drawQueryWindow(conn.getQueryVacuum("VACUUM FULL VERBOSE ANALYZE", relations), getDB());
                	break;
                case 55:
					Usage.log(SCHEMA_CONTEXT_MAINTAIN);
                	DataWindow.get().drawQueryWindow(conn.getQueryVacuum("ANALYZE TABLE", relations), getDB());
                	break;
                case 56:
					Usage.log(SCHEMA_CONTEXT_MAINTAIN);
                	DataWindow.get().drawQueryWindow(conn.getQueryVacuum("OPTIMIZE TABLE", relations), getDB());
                	break;
                case 57:
					Usage.log(SCHEMA_CONTEXT_MAINTAIN);
                	DataWindow.get().drawQueryWindow(conn.getQueryVacuum("REPAIR TABLE", relations), getDB());
                	break;
                case 61:
					Usage.log(SCHEMA_CONTEXT_DROP);
                	RelationWizard.get(workspaceName).enableDrop().drawProperties(Schema.this, 1);
                	break;
                case 70:
					Usage.log(SCHEMA_CONTEXT_WORKSPACE, WS_REMOVE);
                    String wsName = selectedValue.replace("Remove from ", "");
                    ws = Project.getCurrent().getWorkspaceByName(wsName);
					relations.forEach(ws::remove);
                    SearchPanel.instance.updateDbTree();
                    break;
                case 71:
					Usage.log(SCHEMA_CONTEXT_WORKSPACE);
					final boolean isNewWorkspace = selectedValue.equals(CREATE_NEW_WORKSPACE);
					if(isNewWorkspace){
                        ws = Workspace.create(getDB());
                    }else{
                        ws = Project.getCurrent().getWorkspaceByName(selectedValue);
                    }
                    if(ws != null) {
						relations.forEach(ws::add);
                        if (isNewWorkspace) {
                            ws.organize();
                        }
                        SearchPanel.instance.updateDbTree();
                    }
                    // TODO: select workspace if not selected
                    break;
                case 80:
					Usage.log(SCHEMA_CONTEXT_REARRANGE);
					final Project p = Project.getCurrent();
					final Organizer organizer = OrganizerFactory.get(selectedValue);
                    int cycles = selectedValue.equals(Menu.L_REARRANGE_FORCE_BASED) || selectedValue.equals(Menu.L_REARRANGE_NATURAL) ? 15 : 1;
                    if(p.getCurrentWorkspace() == null) {
						organizer.organize(Schema.this, cycles);
                    }else{
                        String schemaName = Schema.this.getFullName();
                        for(SchemaReference schema : p.getCurrentWorkspace().getSchemas()) {
                            if(schema.getElement().getFullName().equals(schemaName)) {
								organizer.organize(schema, cycles);
                                break;
                            }
                        }
                    }
                    Canvas.instance.drawProject(true);
                    break;
				default: throw new IllegalArgumentException("Menu option " + type + " is not known");
            }
        }).
			addItem("Properties", RightClickMenu.ICO_EDIT, 10).
			separator();
		if(conn.isSupported(SupportedElement.RELATION)) {
			menu.addItem("Add new table", RightClickMenu.ICO_NEW, 40);
		}
		if(conn.isSupported(SupportedElement.FUNCTION)) {
			menu.addItem("Add new function", RightClickMenu.ICO_NEW, 41);
		}
		if(conn.isSupported(SupportedElement.VIEW)) {
			menu.addItem("Add new view", RightClickMenu.ICO_NEW, 42);
		}
		if(conn.isSupported(SupportedElement.SEQUENCE)) {
			menu.addItem("Add new sequence", RightClickMenu.ICO_NEW, 43);
		}
		if(conn.isSupported(SupportedElement.PACKAGE)) {
			menu.addItem("Add new package", RightClickMenu.ICO_NEW, 44);
		}
		menu.separator().
			addItem("Copy name", RightClickMenu.ICO_COPY, 30).
			addItem("Copy source code", RightClickMenu.ICO_COPY, 32);
		if(workspaceName != null){
			menu.separator().
			addItem("Remove from " + workspaceName, RightClickMenu.ICO_DELETE, 70);
		}else{
			menu.separator().
			addItem("Add to workspace", Workspace.ico16, 71, Geometry.concat(new String[]{ CREATE_NEW_WORKSPACE }, Project.getCurrent().getWorkspaceNames(getDB())));
		}
		menu.separator().
			addItem(
					Menu.L_REARRANGE_ITEMS,
					Theme.getSmallIcon(Theme.ICO_ORGANIZE),
					80,
					new String[]{Menu.L_REARRANGE_ALPHABETICAL, Menu.L_REARRANGE_CIRCULAR, /*Menu.L_REARRANGE_FORCE_BASED,*/ Menu.L_REARRANGE_NATURAL, "|", Menu.L_REARRANGE_EXPLODE, Menu.L_REARRANGE_IMPLODE}
					);
		if(getDB().getProject().getType() != Project.TYPE_ABSTRACT) {
			if(conn.isSupported(SupportedElement.VACUUM)){
				menu.separator().
				addItem("Analyze...", RightClickMenu.ICO_VACUUM, 50).
				addItem("Vacuum...", RightClickMenu.ICO_VACUUM, 51).
				addItem("Vacuum full...", RightClickMenu.ICO_VACUUM, 52);
			}else if(conn.isSupported(SupportedElement.OPTIMIZE)){
				menu.separator().
				addItem("Analyze...", RightClickMenu.ICO_VACUUM, 55).
				addItem("Optimize...", RightClickMenu.ICO_VACUUM, 56).
				addItem("Repair...", RightClickMenu.ICO_VACUUM, 57);
			}
		}
		menu.separator().
			//addItem("TRUNCATE...", RightClickMenu.ICO_DELETE, 60).
			addItem("DROP...", RightClickMenu.ICO_DELETE, 61);
	}

	@Override
	public void mouseUp(){
		getDB().getProject().save();
	}

	@Override
	public Point getAbsCenter(){
		Point l = getRememberedLocation();
		if(l != null) {
			return new Point(l.x + (int) Math.round(getRememberedSize().width / 2.0),
					l.y + (int) Math.round(getRememberedSize().height / 2.0));
		}else{
			return null;
		}
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
		return false;
	}

	void checkSize(DraggableComponent entity) {

		// Expand left or top
		if (Canvas.isDefaultZoom() && (entity.getLocation().x < Canvas.GRID_SIZE || entity.getLocation().y < Canvas.GRID_SIZE)) {
			Point sizeDiff = Geometry.getSnappedPosition(
					entity.getLocation().x < Canvas.GRID_SIZE ? Canvas.DEFAULT_ENTITY_WIDTH : 0,
					entity.getLocation().y < Canvas.GRID_SIZE ? Canvas.DEFAULT_ENTITY_WIDTH : 0);
			for(Component elem : getComponents()){
				elem.setLocation(new Point(elem.getLocation().x+sizeDiff.x, elem.getLocation().y+sizeDiff.y));
			}
			setLocation(Geometry.getSnappedPosition(getLocation().x-sizeDiff.x, getLocation().y-sizeDiff.y));
			setSize(new Dimension(getWidth()+sizeDiff.x, getHeight()+sizeDiff.y));

		} else {
			// Expand right or bottom - just check total used size with currently moved entity
			int usedWidth = entity.getLocation().x + entity.getWidth() + Canvas.GRID_SIZE - 5;
			int usedHeight = entity.getLocation().y + entity.getHeight() + 10;

			if (usedWidth > getWidth()) {
				setSize(new Dimension(usedWidth, getHeight()));
			}
			if (usedHeight > getHeight()) {
				setSize(new Dimension(getWidth(), usedHeight));
			}
		}
	}

	@Override
	public void checkSize(){
		int minX = RESIZE_INITIAL_VALUE;
		int minY = RESIZE_INITIAL_VALUE;
		int maxX = 0;
		int maxY = 0;
		for(Relation rel: relations){
			Dimension s = rel.getSize();
			Point l = rel.getLocation();
			if(s.width+l.x > maxX){ maxX = s.width+l.x; }
			if(s.height+l.y > maxY){ maxY = s.height+l.y; }
			if(l.x < minX){ minX = l.x; }
			if(l.y < minY){ minY = l.y; }
		}
		for(Function func: functions){
			Dimension s = func.getSize();
			Point l = func.getLocation();
			if(s.width+l.x > maxX){ maxX = s.width+l.x; }
			if(s.height+l.y > maxY){ maxY = s.height+l.y; }
			if(l.x < minX){ minX = l.x; }
			if(l.y < minY){ minY = l.y; }
		}
		for(Package pack : packages){
			Dimension s = pack.getSize();
			Point l = pack.getLocation();
			if(s.width+l.x > maxX){ maxX = s.width+l.x; }
			if(s.height+l.y > maxY){ maxY = s.height+l.y; }
			if(l.x < minX){ minX = l.x; }
			if(l.y < minY){ minY = l.y; }
		}
		for(View view: views){
			Dimension s = view.getSize();
			Point l = view.getLocation();
			if(s.width+l.x > maxX){ maxX = s.width+l.x; }
			if(s.height+l.y > maxY){ maxY = s.height+l.y; }
			if(l.x < minX){ minX = l.x; }
			if(l.y < minY){ minY = l.y; }
		}
		for(Sequence seq: sequences){
			if(seq.getAttributes().isEmpty()) {
				Dimension s = seq.getSize();
				Point l = seq.getLocation();
				if(s.width+l.x > maxX){ maxX = s.width+l.x; }
				if(s.height+l.y > maxY){ maxY = s.height+l.y; }
				if(l.x < minX){ minX = l.x; }
				if(l.y < minY){ minY = l.y; }
			}
		}

		// Contract left and top margins
		if (Canvas.isDefaultZoom() && ((minX != RESIZE_INITIAL_VALUE && minX > Canvas.DEFAULT_ENTITY_WIDTH) || (minY != RESIZE_INITIAL_VALUE && minY > Canvas.DEFAULT_ENTITY_WIDTH))) {
			minX -= Canvas.GRID_SIZE;
			minY -= Canvas.GRID_SIZE;
			for(Component elem : getComponents()){
				elem.setLocation(Geometry.getSnappedPosition(elem.getLocation().x-minX, elem.getLocation().y-minY));
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
		Navigator.instance.checkSchemata();
		HotMenu.instance.checkSize();
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
	public String getEditedFullName(){
		return behavior.valuesForEdit.name;
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
		if(config.exportSchemata){
			config.updateLimit();
			config.append((config.conn != null ? config.conn : getConnection()).getQueryCreate(this, config));
		}

		for(Relation rel : relations){
			rel.getQueryRecursive(config);
		}
		for(Function func : functions){
			func.getQueryRecursive(config);
		}
		for(Package pack : packages){
			pack.getQueryRecursive(config);
		}
		for(View view: views){
			view.getQueryRecursive(config);
		}
		for(Sequence seq: sequences){
			if(seq.getAttributes().isEmpty()) {
				seq.getQueryRecursive(config);
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

	@Override
	public DefaultMutableTreeNode getTreeView(boolean showCreateIcons){
		DefaultMutableTreeNode schemaNode = new DefaultMutableTreeNode(this);

		for(Relation rel: new ArrayList<>(relations)){
			schemaNode.add(rel.getTreeView(showCreateIcons));
		}
		for(Function func: new ArrayList<>(functions)){
			schemaNode.add(func.getTreeView(showCreateIcons));
		}
		for(Package pack : new ArrayList<>(packages)){
			schemaNode.add(pack.getTreeView(showCreateIcons));
		}
		for(View view: new ArrayList<>(views)){
			schemaNode.add(view.getTreeView(showCreateIcons));
		}
		new ArrayList<>(sequences).stream()
				.filter(seq -> seq.getAttributes().isEmpty())
				.forEachOrdered(seq -> schemaNode.add(seq.getTreeView(showCreateIcons)));
		return schemaNode;
	}

	@Override
	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching) {

		// Fail fast for name exclusion
		boolean nameMatch = getFullName().matches(search);
		if(searchNotMatching && nameMatch){
			return null;
		}
		DefaultMutableTreeNode schemaNode = new DefaultMutableTreeNode(this);

		List<IModelElement> elements = new ArrayList<>();
		elements.addAll(relations);
		elements.addAll(functions);
		elements.addAll(packages);
		elements.addAll(views);

		for(IModelElement element: elements){
			DefaultMutableTreeNode child = element.getTreeView(search, fulltext, searchNotMatching);
			if(child != null) {
				schemaNode.add(child);
			}
		}
		new ArrayList<>(sequences).stream()
				.filter(seq -> seq.getAttributes().isEmpty())
				.forEachOrdered(seq -> {
					DefaultMutableTreeNode child = seq.getTreeView(search, fulltext, searchNotMatching);
					if (child != null) {
						schemaNode.add(child);
					}
				});

		if (!schemaNode.isLeaf() || searchNotMatching || nameMatch || (
				fulltext && getDB().getProject().getCurrentConn().getQueryCreate(this, null).matches(search)
		)) {
			return schemaNode;
		} else {
			return null;
		}
	}

	@Override
	public void drop(){
		new ArrayList<>(relations).forEach(Relation::drop);
		new ArrayList<>(sequences).forEach(Sequence::drop);
		new ArrayList<>(functions).forEach(Function::drop);
		new ArrayList<>(packages).forEach(Package::drop);
		new ArrayList<>(views).forEach(View::drop);
		db.getSchemas().remove(this);
		getDB().getProject().getWorkspaces()
				.forEach(workspace -> workspace.getSchemas().stream()
						.filter(reference -> reference.getElement() == this)
						.collect(Collectors.toList())
						.forEach(SchemaReference::drop)
				);
	}

	@Override
	public void setLocation(Point loc){
		super.setLocation(loc);
		Canvas.instance.checkWhitespace();
	}

	public void setLocationWoChecks(Point loc){
		super.setLocation(loc);
	}

	@Override
	public void setSize(Dimension d){
		setSizeWoChecks(d);
		Canvas.instance.checkWhitespace();
	}

	public void setSizeWoChecks(Dimension d){
		if(d == null){
			d = new Dimension(Canvas.ZOOMED_ENTITY_WIDTH, 30);
		}else{
			if(d.height < 30) {
				d.height = 30;
			}
			if(d.width < Canvas.ZOOMED_ENTITY_WIDTH) {
				d.width = Canvas.ZOOMED_ENTITY_WIDTH;
			}
		}
		super.setSize(d);
	}

	@Override
	public void unSelect(){
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	@Override
	public IConnection getConnection(){
		return db.getConnection();
	}

	@Override
	public DB getDB(){
		return db;
	}

	@Override
	public Icon getIcon16(){
		return ico16;
	}
	@Override
	public String toString(){
		return behavior.name;
	}

	String[] getRelationNames(){
		String[] names = new String[relations.size()];
		for(int i=0; i<relations.size(); i++){
			names[i] = relations.get(i).getFullName();
		}
		return names;
	}

	Map<String,Relation> getRelationMap(){
		Map<String,Relation> map = new HashMap<>(relations.size());
		for (Relation relation : relations) {
			map.put(relation.getFullName(), relation);
		}
		return map;
	}

	public String[] getTriggerFunctionNames(){
		int size = 0;
		for(Function func: functions){
			if(func.getRetType().equalsIgnoreCase("trigger")){
				size++;
			}
		}
		int k = 0;
		String[] names = new String[size];
		for(Function func: functions){
			if(func.getRetType().equalsIgnoreCase("trigger")){
				names[k] = func.getFullName();
				k++;
			}
		}
		return names;
	}
	public Function getFunctionByName(String name) {
		for(Function func: functions){
			if(func.getName().equals(name)){
				return func;
			}
		}
		return null;
	}
    public Relation getRelationByName(String name) {
        for(Relation rel: relations){
            if(rel.getName().equals(name)){
                return rel;
            }
        }
        return null;
    }
    public Sequence getSequenceByName(String name) {
        for(Sequence sequence : sequences){
            if(sequence.getName().equals(name)){
                return sequence;
            }
        }
        return null;
    }
    public View getViewByName(String name) {
        for(View view : views){
            if(view.getName().equals(name)){
                return view;
            }
        }
        return null;
    }
    public Package getPackageByName(String name) {
        for(Package pack : packages){
            if(pack.getName().equals(name)){
                return pack;
            }
        }
        return null;
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
		relations.forEach(elem -> elements.addAll(elem.getAllSubElements()));
		functions.forEach(elem -> elements.addAll(elem.getAllSubElements()));
		sequences.forEach(elem -> elements.addAll(elem.getAllSubElements()));
		views.forEach(elem -> elements.addAll(elem.getAllSubElements()));
		packages.forEach(elem -> elements.addAll(elem.getAllSubElements()));
		return elements;
	}

	@Override
	public boolean isNew(){
		return behavior.isNew;
	}

	public void assignToDB(DB db){
		this.db = db;
		db.getSchemas().add(this);
	}

	@Override
	public int compareTo(IModelElement t) {
		return getName().compareTo(t.getName());
	}

	@Override
	public String getFullPath(){
		return getDB().getName();
	}

	@Override
	public String getClassName(){
		return L_CLASS;
	}
	@Override
	protected void paintComponent(Graphics g) {
		Shape clip = g.getClip();
		boolean attributeRedraw = clip.getBounds().width == Canvas.ZOOMED_ENTITY_WIDTH - 7 && clip.getBounds().height == Attribute.V_SIZE;
		if(!attributeRedraw && (
				clip.intersects(0, 0, getWidth(), 30) ||
				clip.intersects(0, getHeight()-10D, getWidth(), 10) ||
				clip.intersects(0, 0, 10, getHeight()) ||
				clip.intersects(getWidth()-10D, 0, 10, getHeight())
		)) {
			Graphics2D graphics = (Graphics2D) g;
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int widthNoGap = getWidth() - 1;
			int heightNoGap = getHeight() - 1;

			graphics.setStroke(Canvas.getBasicStroke());
			if (isSelected) {
				graphics.setColor(Canvas.SELECTION_COLOR_A1);
				graphics.drawRoundRect(1, 1, widthNoGap - 2, heightNoGap - 2, arcs.width - 1, arcs.height - 1);
				graphics.setColor(Canvas.SELECTION_COLOR_A2);
				graphics.drawRoundRect(2, 2, widthNoGap - 4, heightNoGap - 4, arcs.width - 2, arcs.height - 2);
				graphics.setColor(Canvas.SELECTION_COLOR_A3);
				graphics.drawRoundRect(3, 3, widthNoGap - 6, heightNoGap - 6, arcs.width - 3, arcs.height - 3);
			}
			graphics.setColor(Color.GRAY);
			graphics.drawRoundRect(0, 0, widthNoGap, heightNoGap, arcs.width, arcs.height);

			graphics.drawString(behavior.name, 16, 16);
		}
	}

	public String getSchemaPrefix(){
		return getName().equals("public") ? "" : getName() + ".";
	}

	public String getSchemaPrefixWithPublic(){
		return getName() + ".";
	}

	private void setSchemaNameForChildren(){
		for(Relation rel: relations){
			rel.getBehavior().setSchemaName(getName());
			for(Index ind: rel.getIndexes()){
				ind.getBehavior().setSchemaName(getName());
			}
			rel.getConstraints().stream()
					.filter(con -> con.getRel1().getFullName().equals(rel.getFullName()))
					.forEach(con -> con.getBehavior().setSchemaName(getName()));
			for(Trigger trig: rel.getTriggers()){
				trig.getBehavior().setSchemaName(getName());
			}
		}
		for(Function func: functions){
			func.getBehavior().setSchemaName(getName());
		}
		for(Package pack : packages){
			pack.getBehavior().setSchemaName(getName());
		}
		for(View view : views){
			view.getBehavior().setSchemaName(getName());
		}
		for(Sequence seq : sequences){
			seq.getBehavior().setSchemaName(getName());
		}
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
		public static final String L_OPERATIONS = "Actions";
		public static final String L_DESCR = "Comment";

		private String name;
		private String dbName;
		private String[] options = new String[0];
		private String descr = "";

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDbName() {
			return dbName;
		}

		public void setDbName(String dbName) {
			this.dbName = dbName;
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
			valuesForEdit.dbName = dbName;
			valuesForEdit.options = options;
			valuesForEdit.descr = descr;
			return valuesForEdit;
		}

		@Override
		public void saveEdited(){
			if(isDropped){
				drop();
			}else{
				boolean nameChanged = !behavior.name.equals(name);
				behavior.name = name;
				behavior.dbName = dbName;
				behavior.options = options;
				behavior.descr = descr;
				if(behavior.isNew){
					assignToDB(getDB().getProject().getDatabaseByName(dbName));
					behavior.valuesForEdit.isNew = true;
					behavior.isNew = false;
					behavior.isDropped = false;
				}else{
					if(nameChanged){
						setSchemaNameForChildren();
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
					name = getConnection().isSupported(SupportedElement.ALL_UPPER) ? value.toUpperCase() : value;
					break;
				case L_DESCR:
					descr = value;
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
	}
}
