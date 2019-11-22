package com.databazoo.devmodeler.model;

import com.databazoo.components.FontFactory;
import com.databazoo.components.UIConstants;
import com.databazoo.components.elements.ClickableComponent;
import com.databazoo.components.elements.DraggableComponentMouseListener;
import com.databazoo.components.elements.EnvironmentComponent;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.SQLOutputConfig;
import com.databazoo.devmodeler.conn.SQLOutputConfigExport;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.view.ViewMode;
import com.databazoo.devmodeler.tools.Geometry;
import com.databazoo.devmodeler.tools.comparator.Comparator;
import com.databazoo.devmodeler.wizards.relation.MNRelationWizard;
import com.databazoo.tools.Dbg;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;


/**
 * Model representation of columns.
 *
 * @author bobus
 */
public class Attribute extends EnvironmentComponent implements IModelElement {
	public static final String L_CLASS = "Column";
	public static final Icon ico16 = Theme.getSmallIcon(Theme.ICO_COLUMN);
	public static final int V_SIZE = 14;

	private static Color NULL_COLOR = UIConstants.Colors.BLUE;
	private static Color DEF_COLOR = UIConstants.Colors.GREEN;
	private static Color DRAG_COLOR = UIConstants.Colors.YELLOW;
	private static Color HIGH_COLOR = UIConstants.Colors.getSelectionBackground();
	private static Color LOW_COLOR = UIConstants.Colors.getLabelBackground();

	private Behavior behavior = new Behavior();
	private int attNum;
	private Relation rel;
	private Sequence sequence;
	private int isDifferent = Comparator.NO_DIFF;
	private Color typeColor;
	private int typeOffset;
	private boolean isDragged;
	private boolean isDNDEnabled = false;
	private boolean isForcedSelection = false;

	public Attribute(Relation parent, String name, String attType, boolean attNull, int attNum, String defaultValue, String storage, String descr) {
		super();
		behavior.name = name;
		behavior.attType = attType;
		behavior.attNull = attNull;
		behavior.defaultValue = defaultValue == null ? "" : defaultValue;
		behavior.descr = descr != null ? descr : "";
		if (storage != null) {
			behavior.storage = storage.toCharArray()[0];
		}
		this.attNum = attNum;
		this.rel = parent;

		updateType();

		setBackground(LOW_COLOR);
		setForeground(UIConstants.Colors.getLabelForeground());
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		addMouseListeners();
		parent.getDB().getProject().usedDataTypes.put(behavior.attType, 1);

		if(attNum <= 0){
			Dbg.fixme("Attribute numbering must start with 1");
		}
	}

	private void updateType() {
		if (behavior.attType.contains("(")) {
			behavior.attPrecision = behavior.attType.replaceAll("(.*)\\(([^)]+)\\).*", "$2");
			behavior.attType = behavior.attType.replaceAll("(.*)\\(([^)]+)\\).*", "$1");
		}
		if ((behavior.attType.matches("int(eger|4|8)") || behavior.attType.equals("bigint")) && getSequence() != null) {
			behavior.attType = behavior.attType.matches("int(eger|4)") ? "serial" : "bigserial";
			behavior.defaultValue = "";
		} else if (behavior.attType.equals("bpchar")) {
			behavior.attType = "char";
		}
	}

	Attribute(Relation parent, String name, String attType) {
		super();
		behavior.name = name;
		behavior.attType = attType;
		behavior.attNull = false;
		this.rel = parent;
		setBackground(LOW_COLOR);
		setForeground(UIConstants.Colors.getLabelForeground());
	}

	public void setAttNum(int attNum) {
		this.attNum = attNum;
	}

	public Sequence getSequence() {
		return sequence;
	}

	public void setSequence(Sequence sequence) {
		this.sequence = sequence;
		updateType();
	}

	public Color getTypeColor() {
		return typeColor;
	}

	public int getTypeOffset() {
		return typeOffset;
	}

	/**
	 * Add Mouse Motion Listener with drag function
	 */
	private void addMouseListeners() {
		final Attribute localAttr = this;
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent me) {
				if (DesignGUI.getView() == ViewMode.DESIGNER && isDNDEnabled) {
					isDragged = true;
					HIGH_COLOR = DRAG_COLOR;
				}
			}

			@Override
			public void mouseReleased(MouseEvent me) {
				if (DesignGUI.getView() == ViewMode.DESIGNER && isDNDEnabled) {
					Attribute remoteAttr = null;
					Relation remoteRel = null;

					Point myLocation = localAttr.getLocation();
					Point relLocation = localAttr.getRel().getLocation();
					Point schemaLocation = localAttr.getRel().getSchema().getLocation();

					Point absLocation = new Point(
							myLocation.x + relLocation.x + schemaLocation.x + me.getX(),
							myLocation.y + relLocation.y + schemaLocation.y + me.getY());

					Component componentAtCanvas = Canvas.instance.getComponentAt(absLocation);
					if(componentAtCanvas instanceof Schema) {
						Schema schema = (Schema) componentAtCanvas;
						schemaLocation = schema.getLocation();
						absLocation.x -= schemaLocation.x;
						absLocation.y -= schemaLocation.y;
						Component componentAtSchema = schema.getComponentAt(absLocation);
						if(componentAtSchema instanceof Relation) {
							remoteRel = (Relation) componentAtSchema;
							relLocation = remoteRel.getLocation();
							absLocation.x -= relLocation.x;
							absLocation.y -= relLocation.y;
							remoteAttr = (Attribute) remoteRel.getComponentAt(absLocation);
						}
					}

					HIGH_COLOR = UIConstants.Colors.getSelectionBackground();
					localAttr.repaint();
					isDragged = false;

					if (remoteRel != null && remoteAttr != null) {
						if ((!remoteRel.getFullName().equals(localAttr.getRel().getFullName()) || Settings.getBool(Settings.L_DND_SAME_TABLE_REL)) && !localAttr.equals(remoteAttr)) {
							MNRelationWizard.get().drawRelation(localAttr.getRel(), localAttr, remoteRel, remoteAttr);
						}
						remoteAttr.setBackground(HIGH_COLOR);
						remoteAttr.repaint();
					}
					Canvas.DragPoint.hideDP();
				}
			}

			@Override
			public void mouseEntered(MouseEvent me) {
				drawSelected();
			}

			@Override
			public void mouseExited(MouseEvent me) {
				if (!isForcedSelection) {
					drawNotSelected();
				}
			}
		});

		addMouseMotionListener(new DraggableComponentMouseListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (isDNDEnabled) {
					Point loc = new Point(
							e.getX() + getX() + rel.getX() + rel.getParent().getX() - 5,
							e.getY() + getY() + rel.getY() + rel.getParent().getY() - 5
					);
					Canvas.DragPoint.showDP(Attribute.this).setLocation(loc);
				} else {
					super.mouseDragged(e);
				}
			}
		});
	}

	void setDNDEnabled(boolean isEnabled) {
		isDNDEnabled = isEnabled && DesignGUI.getView() != ViewMode.DATA;
		setCursor(Cursor.getPredefinedCursor(isDNDEnabled ? Cursor.CROSSHAIR_CURSOR : Cursor.DEFAULT_CURSOR));
	}

	void setForcedSelection(boolean isForcedSelection) {
		this.isForcedSelection = isForcedSelection;
		if (isForcedSelection) {
			drawSelected();
		} else {
			drawNotSelected();
		}
	}

	private void drawSelected() {
		setBackground(HIGH_COLOR);
		setForeground(UIConstants.Colors.getSelectionForeground());
		repaint();
	}

	private void drawNotSelected() {
		setBackground(LOW_COLOR);
		setForeground(UIConstants.Colors.getLabelForeground());
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D graphics = (Graphics2D) g;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

		if (isDragged) {
			graphics.setColor(DRAG_COLOR);
			graphics.fillRect(0, 0, getWidth(), getHeight());
		} else if (getBackground() != null) {
			graphics.setColor(getBackground());
			if(!rel.isSelected()) {
				graphics.fillRect(0, 0, getWidth(), getHeight());
			}else{
				graphics.fillRect(3, 0, getWidth() - 6, getHeight());
			}
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setFont(Canvas.getTitleFont());
		graphics.setColor(getForeground());
		graphics.drawString(behavior.name, 1, Canvas.ZOOMED_ATTR_VSIZE_MINUS_4);
		if (typeColor != null) {
			graphics.setColor(typeColor);
		}
		graphics.drawString(getFullType(), typeOffset, Canvas.ZOOMED_ATTR_VSIZE_MINUS_4);

		int nullMargin = 0;
		if (behavior.attNull) {
			graphics.setColor(NULL_COLOR);
			graphics.drawString("ø", typeOffset - Canvas.ZOOMED_10, Canvas.ZOOMED_ATTR_VSIZE_MINUS_4);
			nullMargin = Canvas.ZOOMED_10;
		}
		if (behavior.defaultValue != null && !behavior.defaultValue.isEmpty()) {
			graphics.setColor(DEF_COLOR);
			graphics.drawString("d", typeOffset - Canvas.ZOOMED_10 - nullMargin, Canvas.ZOOMED_ATTR_VSIZE_MINUS_4);
		}
	}

	@Override
	public void clicked() {
		if (getParent() != null) {
			((ClickableComponent) getParent()).clicked();
		} else if (rel != null) {
			rel.clicked();
		}
	}

	@Override
	public void doubleClicked() {
		rel.doubleClicked(this);
	}

	@Override
	public void rightClicked() {
		rightClicked(null);
	}

	@Override
	public void rightClicked(String workspaceName) {
		if (getParent() != null) {
			((ClickableComponent) getParent()).rightClicked();
		} else if (rel != null) {
			rel.rightClicked();
		}
	}

	@Override
	public DefaultMutableTreeNode getTreeView(boolean showCreateIcons) {
		DefaultMutableTreeNode attribute = new DefaultMutableTreeNode(this);
		if (sequence != null) {
			attribute.add(sequence.getTreeView(showCreateIcons));
		}
		return attribute;
	}

	@Override
	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching) {
		DefaultMutableTreeNode attrNode = new DefaultMutableTreeNode(this);
		if (sequence != null) {
			DefaultMutableTreeNode child = sequence.getTreeView(search, fulltext, searchNotMatching);
			if (child != null) {
				attrNode.add(child);
			}
		}
		boolean nameMatch = getFullName().matches(search);
		boolean excludedByName = searchNotMatching && nameMatch;
		if (!excludedByName && (!attrNode.isLeaf() || searchNotMatching || nameMatch || (
				fulltext && getDB().getProject().getCurrentConn().getQueryCreate(this, null).matches(search)
		))) {
			return attrNode;
		} else {
			return null;
		}
	}

	public void setRel(Relation rel) {
		this.rel = rel;
	}

	public void assignToRels() {
		rel.getAttributes().add(this);
	}

	@Override
	public String getName() {
		return (behavior.name);
	}

	@Override
	public String getFullName() {
		return rel.getName() + '.' + behavior.name;
	}

	@Override
	public String getEditedFullName() {
		return rel.getName() + '.' + behavior.valuesForEdit.name;
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
		if (behavior.isNew) {
			Behavior o = behavior;
			behavior = behavior.valuesForEdit;
			String ret = conn.getQueryCreate(this, null);
			behavior = o;
			return ret;
		} else {
			return conn.getQueryChanged(this);
		}
	}

	@Override
	public String getQueryChangeRevert(IConnection conn) {
		if (behavior.isNew) {
			behavior.isDropped = true;
		} else if (behavior.valuesForEdit.isDropped()) {
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
	public String getQueryRecursive(SQLOutputConfigExport config) {
		throw new UnsupportedOperationException("Not supported.");
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
	public int getDifference() {
		return isDifferent;
	}

	@Override
	public void setSelected(boolean sel) {
		rel.setSelected(sel);
	}

	public Relation getRel() {
		return rel;
	}

	@Override
	public void drop() {
		rel.getAttributes().remove(this);
		rel.remove(this);
		if (behavior.defaultValue.equals("auto_increment")) {
			for (Index ind : rel.getIndexes()) {
				if (ind.getBehavior().isPrimary()) {
					ind.drop();
					break;
				}
			}
		}
		// TODO: remove referring constraints
		// TODO: remove sequence ?
	}

	@Override
	public void checkSize() {
		setSize(new Dimension(Canvas.ZOOMED_ENTITY_WIDTH - 7, Geometry.getZoomedFloored(Attribute.V_SIZE)));
		typeOffset = getSize().width - UIConstants.GRAPHICS.getFontMetrics(Canvas.getTitleFont()).stringWidth(getFullType()) - 2;
		if (!getDescr().isEmpty()) {
			setToolTipText(getDescr());
		}

		if (rel != null) {
			setDNDEnabled(rel.isSelected());
		}
	}

	public void checkSizeNoZoom() {
		setSize(new Dimension(Canvas.DEFAULT_ENTITY_WIDTH - 7, Attribute.V_SIZE));
		typeOffset = getSize().width - UIConstants.GRAPHICS.getFontMetrics(FontFactory.getSans(Font.PLAIN, Settings.getInt(Settings.L_FONT_CANVAS_SIZE))).stringWidth(getFullType()) - 2;
	}

	@Override
	public void unSelect() {

	}

	public void setTypeColor(Color c) {
		typeColor = c;
	}

	@Override
	public IConnection getConnection() {
		return rel.getConnection();
	}

	@Override
	public DB getDB() {
		return rel.getDB();
	}

	@Override
	public Icon getIcon16() {
		return ico16;
	}

	@Override
	public String toString() {
		return behavior.name;
	}

	@Override
	public String getDescr() {
		if (behavior.descr != null && !behavior.descr.isEmpty()) {
			return behavior.descr;
		} else {
			return "";
		}
	}

	@Override
	public Set<IModelElement> getAllSubElements() {
		Set<IModelElement> elements = new HashSet<>();
		elements.add(this);
		if(sequence != null) {
			elements.addAll(sequence.getAllSubElements());
		}
		return elements;
	}

	public String getFullType() {
		return behavior.attType + (behavior.attPrecision != null && !behavior.attPrecision.isEmpty() ? "(" + behavior.attPrecision + ")" : "");
	}

	public String getFullType(Behavior localBehavior) {
		return localBehavior.attType + (localBehavior.attPrecision != null && !localBehavior.attPrecision.isEmpty() ? "(" + localBehavior.attPrecision + ")" : "");
	}

	@Override
	public boolean isNew() {
		return behavior.isNew;
	}

	@Override
	public int compareTo(IModelElement t) {
		if (t instanceof Attribute) {
			return getAttNum().compareTo(((Attribute) t).getAttNum());
		} else {
			return getName().compareTo(t.getName());
		}
	}

	public Integer getAttNum() {
		return attNum;
	}

	@Override
	public String getFullPath() {
		return getDB().getName() + "." + rel.getFullName();
	}

	@Override
	public String getClassName() {
		return L_CLASS;
	}

	public Attribute getInstance() {
		return this;
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
		public static final String L_DATATYPE = "Datatype";
		public static final String L_PRECISION = "Precision";
		public static final String L_DEFAULT = "Default value";
		public static final String L_NULLABLE = "Can be NULL?";
		public static final String L_STORAGE = "Storage";
		public static final String L_OPERATIONS = "Actions";
		public static final String L_DESCR = "Comment";

		public static final String L_STORAGE_AUTO = "Automatic";
		public static final String L_STORAGE_MAIN = "MAIN";
		public static final String L_STORAGE_PLAIN = "PLAIN";
		public static final String L_STORAGE_EXTENDED = "EXTENDED";
		public static final String L_STORAGE_EXTERNAL = "EXTERNAL";

		private String name;
		private String attType;
		private String attPrecision;
		private boolean attNull;
		private String defaultValue;
		private char storage = 'a';
		private String collation;
		private String descr = "";

		public Behavior() {}
		public Behavior(String name, String attType, String attPrecision, boolean attNull, String defaultValue, char storage, String collation, String descr) {
			this.name = name;
			this.attType = attType;
			this.attPrecision = attPrecision;
			this.attNull = attNull;
			this.defaultValue = defaultValue;
			this.storage = storage;
			this.collation = collation;
			this.descr = descr;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getAttType() {
			return attType;
		}

		public void setAttType(String attType) {
			this.attType = attType;
		}

		public String getAttPrecision() {
			return attPrecision;
		}

		/*public void setAttPrecision(String attPrecision) {
			this.attPrecision = attPrecision;
		}*/

		public boolean isAttNull() {
			return attNull;
		}

		public void setAttNull(boolean attNull) {
			this.attNull = attNull;
		}

		public String getDefaultValue() {
			return defaultValue;
		}

		public void setDefaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
		}

		public void setStorage(char storage) {
			this.storage = storage;
		}

		public String getCollation() {
			return collation;
		}

		public void setCollation(String collation) {
			this.collation = collation;
		}

		public String getDescr() {
			return descr != null ? descr : "";
		}

		public void setDescr(String descr) {
			this.descr = descr;
		}

		@Override
		public Behavior prepareForEdit() {
			return valuesForEdit = new Behavior(name, attType, attPrecision, attNull, defaultValue, storage, collation, descr);
		}

		@Override
		public void saveEdited() {
			if (isDropped) {
				drop();
			} else {
				boolean nameChanged = !behavior.name.equals(name);
				String oldName = behavior.name;
				behavior.name = name;
				behavior.attType = attType;
				behavior.attPrecision = attPrecision;
				behavior.attNull = attNull;
				behavior.defaultValue = defaultValue;
				behavior.storage = storage;
				behavior.collation = collation;
				behavior.descr = descr;
				if (behavior.isNew) {
					assignToRels();
					rel.setAddedElement();
					behavior.isNew = false;
					behavior.isDropped = false;
				} else if (nameChanged) {
					rel.getIndexes().stream().filter(index -> index.getAttributes().contains(getInstance()))
							.forEach(index -> index.getBehavior().setDef(index.getBehavior().getDef().replaceAll("(^|[^a-z0-9_]+)" + oldName + "([^a-z0-9_]+|$)", "$1" + behavior.name + "$2")));
				}
			}
			getDB().getProject().save();
		}

		public String getStorage() {
			switch (storage) {
				case 'a':
					return L_STORAGE_AUTO;
				case 'm':
					return L_STORAGE_MAIN;
				case 'p':
					return L_STORAGE_PLAIN;
				case 'x':
					return L_STORAGE_EXTENDED;
				default:
					return L_STORAGE_EXTERNAL;
			}
		}

		public char getStorageChar() {
			return storage;
		}

		@Override
		public void notifyChange(String elementName, String value) {
			switch (elementName) {
				case L_NAME:
					name = getConnection().isSupported(SupportedElement.ALL_UPPER) ? value.toUpperCase() : value;
					break;
				case L_DATATYPE:
					/*if(value.equals("serial") || value.equals("bigserial")){
					attType = value.equals("serial") ? "int4" : "int8";
					def = "nextval('"+rel.getName()+"_"+name+"_seq'::regclass)";
					}else{*/
					attType = value;
					//}
					break;
				case L_PRECISION:
					attPrecision = value;
					break;
				case L_DEFAULT:
					defaultValue = value;
					break;
				case Relation.Behavior.L_COLLATION:
					collation = value.isEmpty() ? null : value;
					break;
				case L_DESCR:
					descr = value;
					break;
				case L_STORAGE:
					switch (value) {
						case L_STORAGE_AUTO:
							storage = 'a';
							break;
						case L_STORAGE_MAIN:
							storage = 'm';
							break;
						case L_STORAGE_PLAIN:
							storage = 'p';
							break;
						case L_STORAGE_EXTENDED:
							storage = 'x';
							break;
						default:
							storage = 'y';
							break;
					}
					break;
			}
		}

		@Override
		public void notifyChange(String elementName, boolean value) {
			if (elementName.equals(L_NULLABLE)) {
				attNull = value;
			}
		}

		@Override
		public void notifyChange(String elementName, boolean[] values) {
			if (elementName.equals(L_OPERATIONS)) {
				isDropped = values[values.length - 1];
			}
		}
	}
}
