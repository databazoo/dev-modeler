package com.databazoo.devmodeler.model;

import com.databazoo.components.UIConstants;
import com.databazoo.components.elements.LineComponent;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.ConnectionUtils;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.SQLOutputConfig;
import com.databazoo.devmodeler.conn.SQLOutputConfigExport;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.RightClickMenu;
import com.databazoo.devmodeler.gui.view.ViewMode;
import com.databazoo.devmodeler.gui.window.datawindow.DataWindow;
import com.databazoo.devmodeler.model.reference.IReferenceElement;
import com.databazoo.devmodeler.model.reference.LineComponentReference;
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
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.databazoo.devmodeler.gui.UsageElement.CONSTRAINT_CONTEXT_COPY;
import static com.databazoo.devmodeler.gui.UsageElement.CONSTRAINT_CONTEXT_DATA;
import static com.databazoo.devmodeler.gui.UsageElement.CONSTRAINT_CONTEXT_DROP;
import static com.databazoo.devmodeler.gui.UsageElement.CONSTRAINT_CONTEXT_EDIT;
import static com.databazoo.devmodeler.gui.UsageElement.CONSTRAINT_CONTEXT_SOURCE;
import static com.databazoo.devmodeler.gui.UsageElement.CONSTRAINT_DOUBLE_CLICKED;
import static com.databazoo.devmodeler.gui.UsageElement.COPY_FULL;

/**
 * Model representation of foreign keys.
 * Visually a line component that connects two tables.
 *
 * @author bobus
 */
public class Constraint extends LineComponent implements IModelElement {
	public static final String L_CLASS = "Constraint";
	public static final Icon ico16 = Theme.getSmallIcon(Theme.ICO_CONSTRAINT);

	private static final String L_CONSTRAINT = "Constraint ";
	private static final String INCOMPLETE = " incomplete.";
	private static final String REL1NAME = " rel1name: ";
	private static final String REL2NAME = " rel2name: ";
	private static final String ATTR1 = " attr1: ";
	private static final String ATTR1NAME = " attr1name: ";
	private static final String ATTR2 = " attr2: ";
	private static final String ATTR2NAME = " attr2name: ";

	public static void calculateArrowPosition(LineComponent component, Constraint.Behavior behavior) {
		double angle;

		Dimension relSize = new Dimension(component.getRel1().getWidth() - Relation.SHADOW_GAP, component.getRel1().getHeight() - Relation.SHADOW_GAP);
		Dimension conSize = new Dimension(component.getWidth() - 2 - CLICK_TOLERANCE, component.getHeight() - 2 - CLICK_TOLERANCE);

		double relSideRatio = relSize.height * 1.0 / relSize.width;
		double conSideRatio = conSize.height * 1.0 / conSize.width;

		if (component.getDirection() == LEFT_TOP_RIGHT_BOTTOM) {
			if (relSideRatio > conSideRatio) {
				component.arrow1Location = new Point(relSize.width / 2 + CLICK_TOLERANCE / 2, (int) (relSize.width * conSideRatio / 2) + CLICK_TOLERANCE / 2);
			} else {
				component.arrow1Location = new Point((int) (relSize.height / conSideRatio / 2) + CLICK_TOLERANCE / 2, relSize.height / 2 + CLICK_TOLERANCE / 2);
			}
			component.arrow1Location.x -= 14 + 16;
			component.arrow1Location.y -= 14 + 16;
			angle = Math.atan(conSideRatio);

		} else if (component.getDirection() == RIGHT_BOTTOM_LEFT_TOP) {
			if (relSideRatio > conSideRatio) {
				component.arrow1Location = new Point(conSize.width - (relSize.width / 2) + CLICK_TOLERANCE / 2, conSize.height - ((int) (relSize.width * conSideRatio / 2)) + CLICK_TOLERANCE / 2);
			} else {
				component.arrow1Location = new Point(conSize.width - ((int) (relSize.height / conSideRatio / 2)) + CLICK_TOLERANCE / 2, conSize.height - (relSize.height / 2) + CLICK_TOLERANCE / 2);
			}
			component.arrow1Location.x -= 16 + 16;
			component.arrow1Location.y -= 15 + 16;
			angle = Math.atan(conSideRatio) + Math.toRadians(180);

		} else if (component.getDirection() == LEFT_BOTTOM_RIGHT_TOP) {
			if (relSideRatio > conSideRatio) {
				component.arrow1Location = new Point((relSize.width / 2) + CLICK_TOLERANCE / 2, conSize.height - ((int) (relSize.width * conSideRatio / 2)) + CLICK_TOLERANCE / 2);
			} else {
				component.arrow1Location = new Point(((int) (relSize.height / conSideRatio / 2)) + CLICK_TOLERANCE / 2, conSize.height - (relSize.height / 2) + CLICK_TOLERANCE / 2);
			}
			component.arrow1Location.x -= 14 + 16;
			component.arrow1Location.y -= 15 + 16;
			angle = Math.atan(1 / conSideRatio) + Math.toRadians(-90);

		} else {
			if (relSideRatio > conSideRatio) {
				component.arrow1Location = new Point(conSize.width - (relSize.width / 2) + CLICK_TOLERANCE / 2, ((int) (relSize.width * conSideRatio / 2)) + CLICK_TOLERANCE / 2);
			} else {
				component.arrow1Location = new Point(conSize.width - ((int) (relSize.height / conSideRatio / 2)) + CLICK_TOLERANCE / 2, (relSize.height / 2) + CLICK_TOLERANCE / 2);
			}
			component.arrow1Location.x -= 15 + 16;
			component.arrow1Location.y -= 15 + 16;
			angle = Math.atan(1 / conSideRatio) + Math.toRadians(90);
		}
		component.arrow1 = rotateCrowsFoot(angle, true, true);

		if ((behavior.attr1 != null && behavior.attr1.getBehavior().isAttNull()) || (Settings.getBool(Settings.L_PERFORM_PARENT_CARD) && behavior.attr2 != null)) {
			relSize = new Dimension(component.getRel2().getWidth() - Relation.SHADOW_GAP, component.getRel2().getHeight() - Relation.SHADOW_GAP);
			conSize = new Dimension(component.getWidth() - 2 - CLICK_TOLERANCE, component.getHeight() - 2 - CLICK_TOLERANCE);

			relSideRatio = relSize.height * 1.0 / relSize.width;
			conSideRatio = conSize.height * 1.0 / conSize.width;

			if (component.getDirection() == RIGHT_BOTTOM_LEFT_TOP) {
				if (relSideRatio > conSideRatio) {
					component.arrow2Location = new Point(relSize.width / 2 + CLICK_TOLERANCE / 2, (int) (relSize.width * conSideRatio / 2) + CLICK_TOLERANCE / 2);
				} else {
					component.arrow2Location = new Point((int) (relSize.height / conSideRatio / 2) + CLICK_TOLERANCE / 2, relSize.height / 2 + CLICK_TOLERANCE / 2);
				}
				component.arrow2Location.x -= 14 + 16;
				component.arrow2Location.y -= 15 + 16;
				angle = Math.atan(conSideRatio);

			} else if (component.getDirection() == LEFT_TOP_RIGHT_BOTTOM) {
				if (relSideRatio > conSideRatio) {
					component.arrow2Location = new Point(conSize.width - (relSize.width / 2) + CLICK_TOLERANCE / 2, conSize.height - ((int) (relSize.width * conSideRatio / 2)) + CLICK_TOLERANCE / 2);
				} else {
					component.arrow2Location = new Point(conSize.width - ((int) (relSize.height / conSideRatio / 2)) + CLICK_TOLERANCE / 2, conSize.height - (relSize.height / 2) + CLICK_TOLERANCE / 2);
				}
				component.arrow2Location.x -= 16 + 16;
				component.arrow2Location.y -= 15 + 16;
				angle = Math.atan(conSideRatio) + Math.toRadians(180);

			} else if (component.getDirection() == RIGHT_TOP_LEFT_BOTTOM) {
				if (relSideRatio > conSideRatio) {
					component.arrow2Location = new Point((relSize.width / 2) + CLICK_TOLERANCE / 2, conSize.height - ((int) (relSize.width * conSideRatio / 2)) + CLICK_TOLERANCE / 2);
				} else {
					component.arrow2Location = new Point(((int) (relSize.height / conSideRatio / 2)) + CLICK_TOLERANCE / 2, conSize.height - (relSize.height / 2) + CLICK_TOLERANCE / 2);
				}
				component.arrow2Location.x -= 15 + 16;
				component.arrow2Location.y -= 16 + 16;
				angle = Math.atan(1 / conSideRatio) + Math.toRadians(-90);

			} else {
				if (relSideRatio > conSideRatio) {
					component.arrow2Location = new Point(conSize.width - (relSize.width / 2) + CLICK_TOLERANCE / 2, ((int) (relSize.width * conSideRatio / 2)) + CLICK_TOLERANCE / 2);
				} else {
					component.arrow2Location = new Point(conSize.width - ((int) (relSize.height / conSideRatio / 2)) + CLICK_TOLERANCE / 2, (relSize.height / 2) + CLICK_TOLERANCE / 2);
				}
				component.arrow2Location.x -= 15 + 16;
				component.arrow2Location.y -= 15 + 16;
				angle = Math.atan(1 / conSideRatio) + Math.toRadians(90);
			}
			component.arrow2 = rotateCrowsFoot(angle, behavior.attr1.getBehavior().isAttNull(), false);
		} else {
			component.arrow2 = null;
		}
	}

	private static BufferedImage rotateCrowsFoot(double rads, boolean fromZero, boolean toMany) {
		BufferedImage dimg = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = dimg.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		graphics.rotate(rads, 32, 32);

		if (toMany) {
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 31, 45, 3);

			graphics.setColor(Color.BLACK);
			graphics.drawLine(0, 22, 48, 32);
			graphics.drawLine(0, 32, 48, 32);
			graphics.drawLine(0, 42, 48, 32);
		} else {
			graphics.setColor(Color.BLACK);
			graphics.drawLine(41, 28, 41, 37);
		}

		if (fromZero) {
			graphics.setColor(Color.WHITE);
			graphics.fillOval(43, 28, 9, 9);

			graphics.setColor(Color.BLACK);
			graphics.drawOval(43, 28, 9, 9);
		} else {
			graphics.setColor(Color.BLACK);
			graphics.drawLine(46, 28, 46, 37);
		}

		return dimg;
	}

	protected Behavior behavior = new Behavior();

	public int isDifferent = Comparator.NO_DIFF;
	private final DB db;
	private final Set<IModelElement> elements = new HashSet<>();
	private boolean isSelfRelation = false;

	public Constraint(DB parent, String fullName) {
		//name = name.replaceAll("\"", "");
		//fullName = fullName.replaceAll("\"", "");
		if (fullName.contains(".")) {
			Schema schema = parent.getSchemaFromElement(fullName);
			if (schema != null) {
				behavior.name = fullName.replaceFirst(Pattern.quote(schema.getSchemaPrefixWithPublic()), "");
				behavior.schemaName = schema.getName();
			} else {
				behavior.name = fullName;
			}
		} else {
			behavior.name = fullName;
		}
		this.db = parent;
		draw();
	}

	public Constraint(DB parent, String fullName, String onUpdate, String onDelete, String descr) {
		//name = name.replaceAll("\"", "");
		//fullName = fullName.replaceAll("\"", "");
		if (fullName.contains(".")) {
			Schema schema = parent.getSchemaFromElement(fullName);
			if (schema != null) {
				behavior.name = fullName.replaceFirst(Pattern.quote(schema.getName()+"."), "");
				behavior.schemaName = schema.getSchemaPrefix();
			} else {
				behavior.name = fullName;
			}
		} else {
			behavior.name = fullName;
		}
		behavior.descr = descr;
		if (!onUpdate.isEmpty()) {
			behavior.onUpdate = onUpdate.toLowerCase().charAt(0);
		}
		if (!onDelete.isEmpty()) {
			behavior.onDelete = onDelete.toLowerCase().charAt(0);
		}
		this.db = parent;
		draw();
	}

	public boolean isSelfRelation() {
		return isSelfRelation;
	}

	private void draw() {
		lineWidth = 1;
		setLayout(null);

		addDragListeners();
	}

	@Override
	protected void setArrowPosition() {
		calculateArrowPosition(this, behavior);
	}

	@Override
	public void clicked() {
		if (!isSelected) {
			Schedule.inEDT(Schedule.CLICK_DELAY, () -> {
				if (behavior.attr1 != null) {
					behavior.attr1.setForeground(Canvas.SELECTION_COLOR);
				}
				if (behavior.attr2 != null) {
					behavior.attr2.setForeground(Canvas.SELECTION_COLOR);
				}
			});
			Canvas.instance.setSelectedElement(this);
		}
	}

	@Override
	public void doubleClicked() {
		Usage.log(CONSTRAINT_DOUBLE_CLICKED, "View: " + DesignGUI.getView().name());
		if (DesignGUI.getView() == ViewMode.DATA) {
			DataWindow.get().drawConstraintData(this);
		} else {
			RelationWizard.get(null).drawProperties((Relation) rel1, getName());
		}

	}

	@Override
	public void rightClicked() {
		rightClicked(null);
	}

	@Override
	public void rightClicked(final String workspaceName) {
		RightClickMenu.get((type, selectedValue) -> {
            switch (type) {
                case 10:
					Usage.log(CONSTRAINT_CONTEXT_EDIT);
                    RelationWizard.get(workspaceName).drawProperties((Relation) rel1, getName());
                    break;
                case 20:
					Usage.log(CONSTRAINT_CONTEXT_DATA);
                    DataWindow.get().drawConstraintData(Constraint.this);
                    break;
                case 30:
					Usage.log(CONSTRAINT_CONTEXT_COPY);
                    DesignGUI.toClipboard(Constraint.this.getName());
                    break;
                case 31:
					Usage.log(CONSTRAINT_CONTEXT_COPY, COPY_FULL);
                    DesignGUI.toClipboard(Constraint.this.getFullName());
                    break;
                case 32:
					Usage.log(CONSTRAINT_CONTEXT_SOURCE);
                    DesignGUI.toClipboard(Constraint.this.getQueryCreateClear(getConnection()));
                    break;
                case 61:
					Usage.log(CONSTRAINT_CONTEXT_DROP);
                    RelationWizard.get(workspaceName).enableDrop().drawProperties((Relation) rel1, getName());
                    break;
				default: throw new IllegalArgumentException("Menu option " + type + " is not known");
            }
        }).
				addItem("Properties", RightClickMenu.ICO_EDIT, 10).
				separator().
				addItem("SELECT JOIN", RightClickMenu.ICO_DATA, 20).
				separator().
				addItem("Copy name", RightClickMenu.ICO_COPY, 30).
				addItem("Copy name with schema", RightClickMenu.ICO_COPY, 31).
				addItem("Copy source code", RightClickMenu.ICO_COPY, 32).
				separator().
				addItem("DROP...", RightClickMenu.ICO_DELETE, 61);
	}

	@Override
	protected boolean haveClickableChild(Point p, int clickMask) {
		int x = p.x + getLocation().x;
		int y = p.y + getLocation().y;
		for (Constraint con : getDB().getConstraints()) {
			if (con.clickedOnLine(x, y)) {
				if (clickMask == CLICK_TYPE_LEFT) {
					con.clicked();
				} else if (clickMask == CLICK_TYPE_DOUBLE) {
					con.doubleClicked();
				} else {
					con.rightClicked();
				}
				return true;
			}
		}
		for (Trigger trig : getDB().getTriggers()) {
			if (trig.clickedOnLine(x, y)) {
				if (clickMask == CLICK_TYPE_LEFT) {
					trig.clicked();
				} else if (clickMask == CLICK_TYPE_DOUBLE) {
					trig.doubleClicked();
				} else {
					trig.rightClicked();
				}
				return true;
			}
		}
		Canvas.instance.setSelectedElement(null);
		if (clickMask == CLICK_TYPE_LEFT) {
			getDB().clicked();
		} else if (clickMask == CLICK_TYPE_DOUBLE) {
			getDB().doubleClicked();
		} else {
			getDB().rightClicked();
		}
		return true;
	}

	@Override
	public boolean clickedOnLine(int x, int y) {
		if(isSelfRelation){
			int tolerance = 10;
			boolean inOuterSquare = Geometry.isPointInRectangle(getLocation(), getSize(), new Point(x, y));
			boolean inInnerSquare = Geometry.isPointInRectangle(
					new Point(getLocation().x + tolerance, getLocation().y + tolerance),
					new Dimension(getWidth() - tolerance*2, getHeight() - tolerance*2),
					new Point(x, y));
			return inOuterSquare && !inInnerSquare;
		}else{
			return super.clickedOnLine(x, y);
		}
	}

	@Override
	public DefaultMutableTreeNode getTreeView(boolean showCreateIcons) {
		return new DefaultMutableTreeNode(this);
	}

	@Override
	public DefaultMutableTreeNode getTreeView(String search, boolean fulltext, boolean searchNotMatching) {
		boolean nameMatch = getName().matches(search);
		boolean excludedByName = searchNotMatching && nameMatch;
		if (!excludedByName && (searchNotMatching || nameMatch || (
				fulltext && ConnectionUtils.getCurrent(getDB().getName()).getQueryCreate(this, null).matches(search)
		))) {
			return new DefaultMutableTreeNode(this);
		} else {
			return null;
		}
	}

	@Override
	public String getName() {
		return behavior.name;
	}

	@Override
	public String getFullName() {
		return (rel1 != null ? rel1.getFullName() + "." : "") + behavior.name;
	}

	@Override
	public String getEditedFullName() {
		return (rel1 != null ? rel1.getFullName() + "." : "") + behavior.valuesForEdit.name;
	}

	@Override
	public String getQueryCreate(IConnection conn) {
		return conn.getQueryCreate(this, behavior.isNew ? null : SQLOutputConfig.WIZARD);
	}

	@Override
	public String getQueryCreateClear(IConnection conn) {
		return conn.getQueryCreateWithoutComment(this, null);
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
		} else if (behavior.valuesForEdit.isDropped) {
			return conn.getQueryCreateWithoutComment(this, null);
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
		if (config.exportConstraints) {
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
	public int getDifference() {
		return isDifferent;
	}

	@Override
	public void checkSize() {
		if (behavior.attr1 != null) {
			isDashed = behavior.attr1.getBehavior().isAttNull();
		}
		if (rel1 != null && rel2 != null && rel1.getFullName().equals(rel2.getFullName())) {
			isSelfRelation = true;
			checkSizeSelfRelation();
		} else {
			isSelfRelation = false;
			super.checkSize();
		}
	}

	public void setAttr1(Attribute att) {
		behavior.attr1 = att;
		checkReferenceNullable();
	}

	public void setAttr2(Attribute att) {
		behavior.attr2 = att;
	}

	public Attribute getAttr1() {
		return behavior.attr1;
	}

	public Attribute getAttr2() {
		return behavior.attr2;
	}

	public void checkReferenceNullable() {
		if (behavior.attr1 != null) {
			behavior.isReferenceNullable = behavior.attr1.getBehavior().isAttNull();
			if (behavior.valuesForEdit != null) {
				behavior.valuesForEdit.isReferenceNullable = behavior.attr1.getBehavior().isAttNull();
			}
		}
	}

	public void setRelsAttrsByName(DB db, String rel1FullName, String rel2FullName, String attr1FullName, String attr2FullName, boolean isTemp) {
		rel1 = null;
		rel2 = null;
		//rel1FullName = Connection.escapeFullName(rel1FullName);
		//rel2FullName = Connection.escapeFullName(rel2FullName);
		for (Schema schema : db.getSchemas()) {
			for (Relation rel : schema.getRelations()) {
				if (rel.getFullName().equals(rel1FullName)) {
					setRel1(rel);
					//Dbg.info("Assigned table "+rel.getFullName()+" to "+getFullName()+". Table has "+rel.getAttributes().size()+" attributes");
					for (Attribute att : rel.getAttributes()) {
						//Dbg.info("Comparing "+attr1FullName+" to "+att.getName());
						if (att.getName().equals(attr1FullName)) {
							//Dbg.info("Found "+attr1FullName);
							setAttr1(att);
							break;
						}
					}
				}
				if (rel.getFullName().equals(rel2FullName)) {
					setRel2(rel);
					//Dbg.info("Assigned table "+rel.getFullName()+" to "+getFullName()+". Table has "+rel.getAttributes().size()+" attributes");
					for (Attribute att : rel.getAttributes()) {
						//Dbg.info("Comparing "+attr1FullName+" to "+att.getName());
						if (att.getName().equals(attr2FullName)) {
							//Dbg.info("Found "+attr1FullName);
							setAttr2(att);
							break;
						}
					}
				}
				if (isReady()) {
					break;
				}
			}
			if (isReady()) {
				break;
			}
		}
		if (!isReady()) {
			Dbg.fixme(L_CONSTRAINT + getFullName() + INCOMPLETE +
					REL1NAME + rel1FullName +
					REL2NAME + rel2FullName);
		}
		if (getAttr1() == null || getAttr2() == null) {
			Dbg.fixme(L_CONSTRAINT + getFullName() + INCOMPLETE +
					ATTR1 + getAttr1() +
					ATTR1NAME + attr1FullName +
					ATTR2 + getAttr2() +
					ATTR2NAME + attr2FullName);
		}
		if (!isTemp && isReady()) {
			assignToRels();
		}
	}

	public void setRelsAttrsByName(Relation r1, Relation r2, String attr1Name, String attr2Name, boolean isTemp) {
		setRel1(r1);
		for (Attribute att : r1.getAttributes()) {
			if (att.getName().equals(attr1Name)) {
				setAttr1(att);
				break;
			}
		}

		setRel2(r2);
		for (Attribute att : r2.getAttributes()) {
			if (att.getName().equals(attr2Name)) {
				setAttr2(att);
				break;
			}
		}
		if (!isReady()) {
			Dbg.fixme(L_CONSTRAINT + getFullName() + INCOMPLETE);
		}
		if (getAttr1() == null || getAttr2() == null) {
			Dbg.fixme(
					L_CONSTRAINT + getFullName() + INCOMPLETE +
							ATTR1 + getAttr1() +
							ATTR1NAME + attr1Name +
							ATTR2 + getAttr2() +
							ATTR2NAME + attr2Name);
		}
		if (!isTemp && isReady()) {
			assignToRels();
		}
	}

	public void setRelsAttrsByConstraintName(DB db, String rel1FullName, String attr1FullName, String constraint2FullName, boolean isTemp) {
		rel1 = null;
		rel2 = null;
		for (Schema schema : db.getSchemas()) {
			for (Relation rel : schema.getRelations()) {
				if (rel.getFullName().equals(rel1FullName)) {
					setRel1(rel);
					for (Attribute att : rel.getAttributes()) {
						if (att.getName().equals(attr1FullName)) {
							setAttr1(att);
							break;
						}
					}
				}
				for (Index ind : rel.getIndexes()) {
					if (ind.getFullName().equals(constraint2FullName)) {
						setRel2(rel);
						setAttr2(ind.getAttributes().get(0));
						break;
					}
				}
				if (isReady()) {
					break;
				}
			}
			if (isReady()) {
				break;
			}
		}
		if (!isReady()) {
			throw new IllegalArgumentException(
					L_CONSTRAINT + getFullName() + INCOMPLETE + " rel1FullName: " + rel1FullName + " constraint2FullName: " + constraint2FullName);
		}
		if (getAttr1() == null || getAttr2() == null) {
			throw new IllegalArgumentException(
					L_CONSTRAINT + getFullName() + INCOMPLETE + " getAttr1: " + getAttr1() + " attr1FullName: " + attr1FullName + " getAttr2: " + getAttr2() + " constraint2FullName: " + constraint2FullName);
		}
		if (!isTemp) {
			assignToRels();
		}
	}

	public void assignToRels() {
		((Relation) rel1).getConstraints().add(this);
		if (!rel1.getFullName().equals(rel2.getFullName())) {
			((Relation) rel2).getConstraints().add(this);
		}
		((IModelElement) rel1).getDB().getConstraints().add(this);
	}

	@Override
	public void drop() {
		if (rel1 != null) {
			((Relation) rel1).getConstraints().remove(this);
			((IModelElement) rel1).getDB().getConstraints().remove(this);
		}
		if (rel2 != null) {
			((Relation) rel2).getConstraints().remove(this);
		}
		getDB().getProject().getWorkspaces()
				.forEach(workspace -> workspace.getConstraints().stream()
						.filter(reference -> reference.getElement() == this)
						.collect(Collectors.toList())
						.forEach(LineComponentReference::drop)
				);
	}

	@Override
	public void unSelect() {
		if (Project.getCurrent().getCurrentWorkspace() != null) {
			for (IReferenceElement elem : Project.getCurrent().getCurrentWorkspace().getConstraints()) {
				if (elem.getElement().equals(this)) {
					elem.unSelect();
					break;
				}
			}
		} else {
			if (behavior.attr1 != null) {
				behavior.attr1.setForeground(UIConstants.COLOR_FG_ATTRIBUTE);
			}
			if (behavior.attr2 != null) {
				behavior.attr2.setForeground(UIConstants.COLOR_FG_ATTRIBUTE);
			}
		}
	}

	@Override
	public IConnection getConnection() {
		return db.getConnection();
	}

	@Override
	public DB getDB() {
		return db;
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
	public boolean isNew() {
		return behavior.isNew();
	}

	@Override
	public int compareTo(IModelElement t) {
		return getName().compareTo(t.getName());
	}

	private void checkSizeSelfRelation() {
		Relation rel = (Relation) getRel1();
		int level = -1;
		for (Constraint con : rel.getConstraints()) {
			if (con.getFullName().equals(getFullName())) {
				break;
			}
			if (con.isSelfRelation) {
				level++;
			}
		}
		Point loc = rel.getAbsCenter();
		if (loc != null) {
			loc.x -= (level * 10);
			loc.y -= (level * 10);
			setLocation(Geometry.getZoomed(loc));
			setSize(Geometry.getZoomed(new Dimension(rel.getWidth() / 2 + (level * 20) + 30, rel.getHeight() / 2 + (level * 20) + 30)));
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (isSelfRelation) {
			Graphics2D graphics = (Graphics2D) g;
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			if(isSelected){
				graphics.setPaint(Canvas.SELECTION_COLOR_A2);

				graphics.setStroke(Canvas.getLineStrokeFull(4));
				graphics.drawLine(1, 1, getWidth() - 2, 1);
				graphics.drawLine(getWidth() - 2, 1, getWidth() - 2, getHeight() - 2);
				graphics.drawLine(getWidth() - 2, getHeight() - 2, 1, getHeight() - 2);
				graphics.drawLine(1, getHeight() - 2, 1, 1);

				graphics.setStroke(Canvas.getLineStrokeFull(2));
				graphics.drawLine(1, 1, getWidth() - 2, 1);
				graphics.drawLine(getWidth() - 2, 1, getWidth() - 2, getHeight() - 2);
				graphics.drawLine(getWidth() - 2, getHeight() - 2, 1, getHeight() - 2);
				graphics.drawLine(1, getHeight() - 2, 1, 1);
			}

			graphics.setStroke(isDashed ? Canvas.getLineStrokeDashed() : Canvas.getLineStrokeFull(lineWidth));
			graphics.setPaint(lineColor);
			graphics.drawLine(1, 1, getWidth() - 2, 1);
			graphics.drawLine(getWidth() - 2, 1, getWidth() - 2, getHeight() - 2);
			graphics.drawLine(getWidth() - 2, getHeight() - 2, 1, getHeight() - 2);
			graphics.drawLine(1, getHeight() - 2, 1, 1);
		} else {
			super.paintComponent(g);
		}
	}

	@Override
	public String getFullPath() {
		return getDB().getName() + "." + rel1.getFullName();
	}

	@Override
	public String getClassName() {
		return L_CLASS;
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
		public static final String L_LOC_REL = "Local table";
		public static final String L_LOC_ATTR = "Local column";
		public static final String L_REM_REL = "Remote table";
		public static final String L_REM_ATTR = "Remote column";
		public static final String L_ON_UPDATE = "On update";
		public static final String L_ON_DELETE = "On delete";
		public static final String L_OPERATIONS = "Actions";
		public static final String L_DEFINITION = "Definition";
		public static final String L_DESCR = "Comment";
		public static final String L_REM_NULL_TITLE = "Reference";
		public static final String L_REM_NULL = "Can be NULL";
		public static final String L_UX_NAME = "Unique index";
		public static final String L_NEW_COL_NAME = "New col. name";
		public static final String L_NEW_COL_TYPE = "New col. type";

		public static final char CASCADE = 'c';
		public static final char RESTRICT = 'r';
		public static final char SET_NULL = 'n';
		public static final char SET_DEFAULT = 'd';
		public static final char NO_ACTION = 'a';

		protected String name;
		protected String schemaName;
		protected char onDelete = CASCADE;
		protected char onUpdate = CASCADE;
		protected Attribute attr1;
		protected Attribute attr2;
		protected String def = "";
		protected String descr = "";
		protected boolean createNewAttribute = false;
		protected String newColName;
		protected String newColType;
		protected boolean isReferenceNullable = false;

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

		/*public void setOnDelete(char onDelete) {
			this.onDelete = onDelete;
		}

		public void setOnUpdate(char onUpdate) {
			this.onUpdate = onUpdate;
		}*/

		public Attribute getAttr1() {
			return attr1;
		}

		public void setAttr1(Attribute attr1) {
			this.attr1 = attr1;
		}

		public Attribute getAttr2() {
			return attr2;
		}

		public void setAttr2(Attribute attr2) {
			this.attr2 = attr2;
		}

		public String getDef() {
			return def;
		}

		public void setDef(String def) {
			this.def = def;
		}

		public String getDescr() {
			return descr != null ? descr : "";
		}

		public void setDescr(String descr) {
			this.descr = descr;
		}

		public boolean isCreateNewAttribute() {
			return createNewAttribute;
		}

		/*public void setCreateNewAttribute(boolean createNewAttribute) {
			this.createNewAttribute = createNewAttribute;
		}*/

		public String getNewColName() {
			return newColName;
		}

		/*public void setNewColName(String newColName) {
			this.newColName = newColName;
		}*/

		public String getNewColType() {
			return newColType;
		}

		/*public void setNewColType(String newColType) {
			this.newColType = newColType;
		}*/

		public boolean isReferenceNullable() {
			return isReferenceNullable;
		}

		/*public void setReferenceNullable(boolean referenceNullable) {
			isReferenceNullable = referenceNullable;
		}*/

		@Override
		public Behavior prepareForEdit() {
			valuesForEdit = new Behavior();
			valuesForEdit.name = name;
			valuesForEdit.schemaName = schemaName;
			valuesForEdit.onDelete = onDelete;
			valuesForEdit.onUpdate = onUpdate;
			valuesForEdit.createNewAttribute = createNewAttribute;
			valuesForEdit.attr1 = attr1;
			valuesForEdit.attr2 = attr2;
			valuesForEdit.def = def;
			valuesForEdit.descr = descr;
			valuesForEdit.newColName = newColName;
			valuesForEdit.newColType = newColType;
			//valuesForEdit.isReferenceNullable = isReferenceNullable;
			return valuesForEdit;
		}

		@Override
		public void saveEdited() {
			if (isDropped) {
				drop();
			} else {
				behavior.name = name;
				behavior.schemaName = schemaName;
				behavior.onDelete = onDelete;
				behavior.onUpdate = onUpdate;
				behavior.createNewAttribute = false;
				behavior.attr1 = attr1;
				behavior.attr2 = attr2;
				behavior.def = def;
				behavior.descr = descr;
				behavior.newColName = newColName;
				behavior.newColType = newColType;
				if (behavior.isNew) {
					assignToRels();
					((Relation) rel1).setAddedElement();
					behavior.isNew = false;
					behavior.isDropped = false;
				}
				if (createNewAttribute) {
					List<Attribute> attributes = ((Relation) rel1).getAttributes();
					behavior.attr1 = new Attribute((Relation) rel1, newColName, newColType, isReferenceNullable, attributes.get(attributes.size() - 1).getAttNum() + 1, "", "a", "");
					behavior.attr1.assignToRels();
					createNewAttribute = false;
				} else if (attr1 != null && isReferenceNullable != attr1.getBehavior().isAttNull()) {
					attr1.getBehavior().setAttNull(isReferenceNullable);
				}
			}
			getDB().getProject().save();
		}

		@Override
		public void notifyChange(String elementName, String value) {
			Relation rel;
			Attribute attr;
			switch (elementName) {
				case L_NAME:
					name = getConnection().isSupported(SupportedElement.ALL_UPPER) ? value.toUpperCase() : value;
					break;
				case L_DESCR:
					descr = value;
					break;
				case L_LOC_REL:
					rel = ((IModelElement) rel1).getDB().getRelationByFullName(value);
					if (rel != null) {
						rel1 = rel;
					}
					break;
				case L_LOC_ATTR:
					attr = ((Relation) rel1).getAttributeByName(value);
					if (attr != null) {
						attr1 = attr;
						createNewAttribute = false;
					} else {
						attr1 = null;
						createNewAttribute = true;
					}
					break;
				case L_REM_REL:
					rel = ((IModelElement) rel2).getDB().getRelationByFullName(value);
					if (rel != null) {
						rel2 = rel;
					}
					break;
				case L_REM_ATTR:
					attr = ((Relation) rel2).getAttributeByName(value);
					if (attr != null) {
						attr2 = attr;
					}
					break;
				case L_ON_UPDATE:
					onUpdate = fromStr(value);
					break;
				case L_ON_DELETE:
					onDelete = fromStr(value);
					break;
				case L_DEFINITION:
					def = value;
					break;
				case L_NEW_COL_NAME:
					newColName = value;
					break;
				case L_NEW_COL_TYPE:
					newColType = value;
					break;
			}
		}

		@Override
		public void notifyChange(String elementName, boolean value) {
		}

		@Override
		public void notifyChange(String elementName, boolean[] values) {
			switch (elementName) {
				case L_OPERATIONS:
					isDropped = values[values.length - 1];
					break;
				case L_REM_NULL_TITLE:
					isReferenceNullable = values[0];
					break;
			}
		}

		public String getOnUpdate() {
			return toStr(onUpdate);
		}

		public char getOnUpdateChar() {
			return onUpdate;
		}

		public String getOnDelete() {
			return toStr(onDelete);
		}

		public char getOnDeleteChar() {
			return onDelete;
		}

		private String toStr(char act) {
			switch (act) {
				case CASCADE:
					return "CASCADE";
				case RESTRICT:
					return "RESTRICT";
				case SET_NULL:
					return "SET NULL";
				case SET_DEFAULT:
					return "SET DEFAULT";
				default:
					return "NO ACTION";
			}
		}

		private char fromStr(String act) {
			switch (act) {
				case "CASCADE":
					return CASCADE;
				case "RESTRICT":
					return RESTRICT;
				case "SET NULL":
					return SET_NULL;
				case "SET DEFAULT":
					return SET_DEFAULT;
				default:
					return NO_ACTION;
			}
		}
	}
}
