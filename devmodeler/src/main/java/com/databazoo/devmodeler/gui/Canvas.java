package com.databazoo.devmodeler.gui;

import com.databazoo.components.FontFactory;
import com.databazoo.components.UIConstants;
import com.databazoo.components.elements.ClickableComponent;
import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.components.elements.DraggableComponentMouseListener;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.gui.view.ViewMode;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Inheritance;
import com.databazoo.devmodeler.model.Package;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Sequence;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.model.View;
import com.databazoo.devmodeler.model.Workspace;
import com.databazoo.devmodeler.model.reference.DraggableComponentReference;
import com.databazoo.devmodeler.model.reference.LineComponentReference;
import com.databazoo.devmodeler.model.reference.RelationReference;
import com.databazoo.devmodeler.model.reference.SchemaReference;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.tools.Geometry;
import com.databazoo.devmodeler.tools.MathUtils;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static com.databazoo.devmodeler.gui.view.DifferenceView.L_FUNCTIONS;
import static com.databazoo.devmodeler.gui.view.DifferenceView.L_PACKAGES;
import static com.databazoo.devmodeler.gui.view.DifferenceView.L_SEQUENCES;
import static com.databazoo.devmodeler.gui.view.DifferenceView.L_TABLES;
import static com.databazoo.devmodeler.gui.view.DifferenceView.L_VIEWS;
import static com.databazoo.devmodeler.model.Relation.L_CONSTRAINTS;
import static com.databazoo.devmodeler.model.Relation.L_INDEXES;
import static com.databazoo.devmodeler.model.Relation.L_TRIGGERS;


/**
 * Main canvas.
 *
 * @author bobus
 */
public class Canvas extends ClickableComponent {
	public static final Canvas instance = new Canvas();

	private static final int TYPE_TABLE = 1;
	private static final int TYPE_FUNCTION = 2;
	private static final int TYPE_PACKAGE = 3;
	private static final int TYPE_VIEW = 4;
	private static final int TYPE_SEQUENCE = 5;

	private static final int TYPE_CONSTRAINT = 11;
	private static final int TYPE_TRIGGER = 12;
	private static final int TYPE_INHERITANCE = 13;

	private static final String ACTION_MAIN_SEARCH = "mainSearch";
	private static final String ACTION_TOGGLE_DB_TREE = "toggleDbTree";

	public static final int GRID_SIZE = 30;
	public static final int SNAPPINESS = 8;
	public static final boolean SNAP_TO_GRID = true;
	public static final int DEFAULT_ENTITY_WIDTH = 7 * GRID_SIZE + 6;
	public static final int WHITESPACE = 10 * GRID_SIZE;

	private static final BasicStroke BASIC_STROKE = new BasicStroke();
	private static volatile BasicStroke lineStrokeFull1, lineStrokeFull2, lineStrokeFull4, lineStrokeDashed;

	private static double zoomFactor = 1.0;
	private static Font titleFont = FontFactory.getSans(Font.PLAIN, Settings.getInt(Settings.L_FONT_CANVAS_SIZE));

	public static int ZOOMED_ENTITY_WIDTH = DEFAULT_ENTITY_WIDTH;
	public static int ZOOMED_ATTR_VSIZE_MINUS_4 = Geometry.getZoomed(Attribute.V_SIZE - 4);
	public static int ZOOMED_10 = Geometry.getZoomed(10);

	public static synchronized boolean getZoomNotTooSmall() {
		return zoomFactor > 0.55;
	}

	public static synchronized boolean isDefaultZoom() {
		return MathUtils.equalsPrecision3(zoomFactor, 1.0);
	}

	public static synchronized double getZoom() {
		return zoomFactor;
	}

	static synchronized void setZoom(double val) {
		zoomFactor = val;
		titleFont = FontFactory.getSans(Font.PLAIN, Geometry.getZoomed(Settings.getInt(Settings.L_FONT_CANVAS_SIZE)));
		updateStrokes();
	}

	private static void updateStrokes() {
		lineStrokeDashed = new BasicStroke(
				Math.round(1.0F * Canvas.getZoom()),
				BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_MITER,
				Math.round(20.0F * Canvas.getZoom()),
				new float[]{Math.round(10.0F * Canvas.getZoom())},
				0.0F);
		lineStrokeFull1 = new BasicStroke(Geometry.getZoomed(1), BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
		lineStrokeFull2 = new BasicStroke(Geometry.getZoomed(2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
		lineStrokeFull4 = new BasicStroke(Geometry.getZoomed(4), BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);

		ZOOMED_ENTITY_WIDTH = Geometry.getZoomed(DEFAULT_ENTITY_WIDTH);
		ZOOMED_ATTR_VSIZE_MINUS_4 = Geometry.getZoomed(Attribute.V_SIZE - 4);
		ZOOMED_10 = Geometry.getZoomed(10);
	}

	public static Stroke getBasicStroke(){
		return BASIC_STROKE;
	}

	public static Stroke getLineStrokeFull(int lineWidth){
		if(lineStrokeFull1 == null){
			updateStrokes();
		}
		if (lineWidth == 1) {
			return lineStrokeFull1;
		} else if (lineWidth == 2) {
			return lineStrokeFull2;
		} else if (lineWidth == 4) {
			return lineStrokeFull4;
		} else {
			return new BasicStroke(Geometry.getZoomed(lineWidth), BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
		}
	}

	public static Stroke getLineStrokeDashed(){
		if(lineStrokeFull1 == null){
			updateStrokes();
		}
		return lineStrokeDashed;
	}

	public static Font getTitleFont() {
		return titleFont;
	}

	public volatile boolean quirksMode = false;
	public boolean gridEnabled = Settings.getBool(Settings.L_LAYOUT_CANV_GRID);

	private JScrollPane scrollPane;
	private final Dimension size = new Dimension(3000, 2000);
	private IModelElement selectedElem;
	private Component infoPanel;
	private Navigator overview;
	private DB displayedDB;
	private DragPoint dragPoint;

	private Canvas() {
		setPreferredSize(size);
		setAutoscrolls(true);
		setBackground(UIConstants.isLafWithDarkSkin() ? UIConstants.Colors.getLabelBackground() : Color.white);
		addListeners();
		getActionMap().put(ACTION_MAIN_SEARCH, new AbstractAction(ACTION_MAIN_SEARCH) {
			@Override
			public void actionPerformed(ActionEvent evt) {
				JCheckBoxMenuItem menuItem = Menu.getInstance().getDbTreeMenuItem();
				if (!menuItem.isSelected()) {
					menuItem.setSelected(true);
				}
				SearchPanel.instance.focus();
			}
		});
		getActionMap().put(ACTION_TOGGLE_DB_TREE, new AbstractAction(ACTION_TOGGLE_DB_TREE) {
			@Override
			public void actionPerformed(ActionEvent evt) {
				JCheckBoxMenuItem menuItem = Menu.getInstance().getDbTreeMenuItem();
				menuItem.setSelected(!menuItem.isSelected());
			}
		});
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), ACTION_MAIN_SEARCH);
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), ACTION_TOGGLE_DB_TREE);
	}

	void setScrolls(JScrollPane scr) {
		scrollPane = scr;
		scrollPane.getVerticalScrollBar().setUnitIncrement(DesignGUI.SCROLL_AMOUNT);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(DesignGUI.SCROLL_AMOUNT);
		scrollPane.getViewport().addChangeListener(ce -> checkInfoPanelAndOverviewLocation());
	}

	public void checkInfoPanelAndOverviewLocation() {
		int hVal = scrollPane.getHorizontalScrollBar().getValue();
		int vVal = scrollPane.getVerticalScrollBar().getValue();
		if (infoPanel != null) {
			infoPanel.setLocation(hVal, vVal);
		}
		if (overview != null) {
			overview.checkSize();
			overview.setLocation(hVal + scrollPane.getSize().width - overview.getSize().width - scrollPane.getVerticalScrollBar().getSize().width - 15,
					vVal + scrollPane.getSize().height - overview.getSize().height - scrollPane.getHorizontalScrollBar().getSize().height - 15);
		}
		HotMenu.instance.checkSize();
	}

	private void checkComponentZOrder() {
		if (infoPanel != null) {
			setComponentZOrder(infoPanel, 0);
		}
		if (overview != null) {
			setComponentZOrder(overview, 0);
		}
		setComponentZOrder(HotMenu.instance, 0);
	}

	public void scrollToCenter() {
		Dimension viewportSize = scrollPane.getSize();
		scrollTo(new Point(MathUtils.min0((getWidth() - viewportSize.width) / 2), MathUtils.min0((getHeight() - viewportSize.height) / 2)));
	}

	public void scrollTo(Point newPos) {
		Dimension viewportSize = scrollPane.getSize();

		Geometry.fitPointToLimits(newPos, new Point(0,0), new Point(
				scrollPane.getHorizontalScrollBar().getMaximum() - viewportSize.width,
				scrollPane.getVerticalScrollBar().getMaximum() - viewportSize.height));

		Schedule.inEDT(() -> {
			scrollPane.getViewport().setViewPosition(newPos);
			checkInfoPanelAndOverviewLocation();
		});
	}

	synchronized void drawProject(boolean forceCompleteRedraw) {
		try {
			if (Project.getCurrDB() != null) {
				boolean differentDB = forceCompleteRedraw || displayedDB == null || !displayedDB.getName().equals(Project.getCurrDB().getName());
				displayedDB = Project.getCurrDB();

				clear();
				drawSchemata();

				if (quirksMode) {
					if (MenuElementView.getVisibility(L_TABLES)) {
						drawRelations();
					}
					if (MenuElementView.getVisibility(L_SEQUENCES)) {
						drawSequences();
					}
					if (MenuElementView.getVisibility(L_VIEWS)) {
						drawViews();
					}
					if (MenuElementView.getVisibility(L_FUNCTIONS)) {
						drawFunctions();
					}
					if (MenuElementView.getVisibility(L_PACKAGES)) {
						drawPackages();
					}
					return;
				}

				if (MenuElementView.getVisibility(L_TABLES) && differentDB) {
					drawRelations();
					drawInheritances();
				}
				if (MenuElementView.getVisibility(L_CONSTRAINTS)) {
					drawConstraints(differentDB);
				}

				if (DesignGUI.getView() == ViewMode.DATA) {
					drawRelationDataInfo();
				} else {
					if (getZoomNotTooSmall()) {
						drawAttributes();
					}
				}
				if (MenuElementView.getVisibility(L_INDEXES) && getZoomNotTooSmall()) {
						drawIndexes();
					}
				if (MenuElementView.getVisibility(L_SEQUENCES)) {
					drawSequences();
				}
				if (MenuElementView.getVisibility(L_VIEWS)) {
					drawViews();
				}
				if (MenuElementView.getVisibility(L_FUNCTIONS)) {
					drawFunctions();
				}
				if (MenuElementView.getVisibility(L_PACKAGES)) {
					drawPackages();
				}
				if (MenuElementView.getVisibility(L_TRIGGERS)) {
					drawTriggers();
				}

				drawInfoPanel();
				drawOverview();
				drawHotMenu();
				repaint();
			} else {
				drawInfoPanel();
				repaint();
			}
		} catch (Exception e) {
			Dbg.notImportant("Will retry drawing the canvas in a moment.", e);
			drawProjectLater(forceCompleteRedraw);
		}
	}

	synchronized void drawProjectLater(final boolean forceCompleteRedraw) {
		Schedule.reInvokeInWorker(Schedule.Named.CANVAS_DRAW_PROJECT, UIConstants.TYPE_TIMEOUT, () -> drawProject(forceCompleteRedraw));
	}

	private void clear() {
		removeAll();
		Workspace workspace = Project.getCurrent().getCurrentWorkspace();
		if (workspace != null) {
			for(SchemaReference schema : workspace.getSchemas()){
				schema.removeAll();
				schema.getRelations().forEach(Container::removeAll);
			}
		} else {
			for (Schema schema : Project.getCurrDB().getSchemas()) {
				schema.removeAll();
				schema.getRelations().forEach(Container::removeAll);
			}
		}
	}

	private void drawSchemata() {
		Dimension viewportSize = scrollPane.getSize();
		Point viewportPos = scrollPane.getViewport().getViewPosition();
		int horPad = (int) Math.floor(viewportSize.width * 0.2 / 3);
		int verPad = (int) Math.floor(viewportSize.height * 0.2 / 2);

		IConnection conn = Project.getCurrent().getCurrentConn();
		List schemas;
		Workspace workspace = Project.getCurrent().getCurrentWorkspace();
		if (workspace != null) {
			schemas = workspace.getSchemas();
		} else {
			schemas = Project.getCurrDB().getSchemas();
		}
		int schemasInViewport = (int) Math.floor(scrollPane.getSize().width / 400.0);
		for (int i = 0; i < schemas.size(); i++) {
			DraggableComponent schema = (DraggableComponent) schemas.get(i);
			if (workspace == null && !schema.isInEnvironment(conn)) {
				continue;
			}
			if (schema.getRememberedLocation() == null) {
				schema.setSize(new Dimension((int) (viewportSize.width * 0.4), (int) (viewportSize.height * 0.8)));
				schema.setLocation(Geometry.getSnappedPosition(viewportPos.x + horPad + (i % schemasInViewport == 0 ? (int) (viewportSize.width * 0.4 + horPad) : 0), viewportPos.y + verPad));
			} else {
				schema.setSize(Geometry.getZoomed(schema.getRememberedSize() == null ? new Dimension(100, 20) : schema.getRememberedSize()));
				schema.setLocation(Geometry.getZoomed(schema.getRememberedLocation()));
			}
			add(schema);
		}
	}

	private void drawRelations() {
		if (Project.getCurrent().getCurrentWorkspace() != null) {
			drawWorkspace(TYPE_TABLE);
		} else {
			IConnection conn = Project.getCurrent().getCurrentConn();
			for (Schema schema : Project.getCurrDB().getSchemas()) {

				// Skip hidden
				if (!schema.isInEnvironment(conn)) {
					continue;
				}

				int padding = Canvas.GRID_SIZE;
				int top = padding;
				int left = padding;
				int tallest = 0;

				for (Relation relation : schema.getRelations()) {

					// Skip hidden
					if (!relation.isInEnvironment(conn)) {
						continue;
					}

					relation.checkSize();
					schema.add(relation);
					if (relation.getRememberedLocation() == null) {
						relation.setLocation(Geometry.getSnappedPosition(left, top));

						if (tallest < relation.getSize().height) {
							tallest = relation.getSize().height;
						}
						left += padding + relation.getSize().width;
						if (left + relation.getSize().width > schema.getSize().width) {
							left = padding;
							top += tallest + padding;
							tallest = 0;
						}
					} else {
						Dimension s = Geometry.getZoomed(relation.getRememberedSize());
						Point l = Geometry.getZoomed(relation.getRememberedLocation());
						relation.setSize(s);
						relation.setLocation(l);
						if (top < s.height + l.y + padding) {
							left = padding;
							top = s.height + l.y + padding;
						}
					}
				}
				schema.checkSize();
			}
		}
	}

	private void drawInheritances() {
		IConnection conn = Project.getCurrent().getCurrentConn();
		if (conn.isSupported(SupportedElement.RELATION_INHERIT)) {
			if (Project.getCurrent().getCurrentWorkspace() != null) {
				drawWorkspace(TYPE_INHERITANCE);
			} else {
				for (Schema schema : Project.getCurrDB().getSchemas()) {

					// Skip hidden
					if (!schema.isInEnvironment(conn)) {
						continue;
					}

					for (Relation relation : schema.getRelations()) {

						// Skip hidden
						if (!relation.isInEnvironment(conn)) {
							continue;
						}

						if (relation.getBehavior().getInheritParentName() != null && !relation.getBehavior().getInheritParentName().isEmpty()) {
							for (Inheritance inh : relation.getInheritances()) {
								if (inh.getRel1() == relation) {
									add(inh);
									inh.checkSize();
									break;
								}
							}
						}
					}
				}
			}
		}
	}

	private void drawAttributes() {
		if (Project.getCurrent().getCurrentWorkspace() != null) {
			drawAttributesWorkspace();
		} else {
			IConnection conn = Project.getCurrent().getCurrentConn();
			for (Schema schema : Project.getCurrDB().getSchemas()) {

				// Skip hidden
				if (!schema.isInEnvironment(conn)) {
					continue;
				}
				for (Relation relation : schema.getRelations()) {

					// Skip hidden
					if (!relation.isInEnvironment(conn)) {
						continue;
					}

					relation.removeAll();
					List<Attribute> relationAttributes = relation.getAttributes();
					int top = Geometry.getZoomedFloored(Attribute.V_SIZE);
					for (int k = 0; k < relationAttributes.size(); k++) {
						Attribute attribute = relationAttributes.get(k);

						// Skip hidden
						if (!attribute.isInEnvironment(conn)) {
							continue;
						}

						attribute.checkSize();
						attribute.setLocation(new Point(1, top + k * Geometry.getZoomedFloored(Attribute.V_SIZE)));
						relation.add(attribute);
					}
				}
			}
		}
	}

	private void drawAttributesWorkspace() {
		for (RelationReference relationReference : Project.getCurrent().getCurrentWorkspace().getRelations()) {
			relationReference.removeAll();
			List<Attribute> relationAttributes = relationReference.getElement().getAttributes();
			int top = Geometry.getZoomed(Attribute.V_SIZE + 1);
			for (int k = 0; k < relationAttributes.size(); k++) {
				Attribute attribute = relationAttributes.get(k);
				attribute.checkSize();
				attribute.setLocation(new Point(1, top + k * Geometry.getZoomedFloored(Attribute.V_SIZE)));
				relationReference.add(attribute);
			}
		}
	}

	private void drawConstraints(boolean differentDB) {
		if (Project.getCurrent().getCurrentWorkspace() != null) {
			drawWorkspace(TYPE_CONSTRAINT);
		} else {
			IConnection conn = Project.getCurrent().getCurrentConn();
			for (Constraint constraint : Project.getCurrDB().getConstraints()) {

				// Skip hidden
				if (!constraint.isInEnvironment(conn)) {
					continue;
				}

				if (differentDB) {
					constraint.checkSize();
				}
				add(constraint);
			}
		}
	}

	private void drawViews() {
		if (Project.getCurrent().getCurrentWorkspace() != null) {
			drawWorkspace(TYPE_VIEW);
		} else {
			IConnection conn = Project.getCurrent().getCurrentConn();
			for (Schema schema : Project.getCurrDB().getSchemas()) {

				// Skip hidden
				if (!schema.isInEnvironment(conn)) {
					continue;
				}

				int padding = Canvas.GRID_SIZE;
				int top = schema.getSize().height;
				int left = padding;
				int tallest = 0;

				for (View view : schema.getViews()) {

					// Skip hidden
					if (!view.isInEnvironment(conn)) {
						continue;
					}

					view.checkSize();
					schema.add(view);
					schema.setComponentZOrder(view, 0);
					if (view.getRememberedLocation() == null) {
						view.setLocation(Geometry.getSnappedPosition(left, top));

						if (tallest < view.getSize().height) {
							tallest = view.getSize().height;
						}
						left += padding + view.getSize().width;
						if (left + view.getSize().width > schema.getSize().width) {
							left = padding;
							top += tallest + padding;
							tallest = 0;
						}
					} else {
						Dimension size = Geometry.getZoomed(view.getRememberedSize());
						Point location = Geometry.getZoomed(view.getRememberedLocation());
						view.setSize(size);
						view.setLocation(location);
						if (top < size.height + location.y + padding) {
							left = padding;
							top = size.height + location.y + padding;
						}
					}
				}
				schema.checkSize();
			}
		}
	}

	private void drawSequences() {
		if (Project.getCurrent().getCurrentWorkspace() != null) {
			drawWorkspace(TYPE_SEQUENCE);
		} else {
			IConnection conn = Project.getCurrent().getCurrentConn();
			for (Schema schema : Project.getCurrDB().getSchemas()) {

				// Skip hidden
				if (!schema.isInEnvironment(conn)) {
					continue;
				}

				int padding = Canvas.GRID_SIZE;
				int top = schema.getSize().height;
				int left = padding;
				int tallest = 0;

				for (Sequence sequence : schema.getSequences()) {

					// Skip hidden
					if (!sequence.isInEnvironment(conn)) {
						continue;
					}

					if (sequence.getAttributes().isEmpty()) {
						sequence.checkSize();
						schema.add(sequence);
						schema.setComponentZOrder(sequence, 0);
						if (sequence.getRememberedLocation() == null) {
							sequence.setLocation(Geometry.getSnappedPosition(left, top));

							if (tallest < sequence.getSize().height) {
								tallest = sequence.getSize().height;
							}
							left += padding + sequence.getSize().width;
							if (left + sequence.getSize().width > schema.getSize().width) {
								left = padding;
								top += tallest + padding;
								tallest = 0;
							}
						} else {
							Dimension size = Geometry.getZoomed(sequence.getRememberedSize());
							Point location = Geometry.getZoomed(sequence.getRememberedLocation());
							sequence.setSize(size);
							sequence.setLocation(location);
							if (top < size.height + location.y + padding) {
								left = padding;
								top = size.height + location.y + padding;
							}
						}
					}
				}
				schema.checkSize();
			}
		}
	}

	private void drawWorkspace(int type) {
		List<LineComponentReference> lineComps = new ArrayList<>();
		switch (type) {
			case TYPE_CONSTRAINT:
				lineComps.addAll(Project.getCurrent().getCurrentWorkspace().getConstraints());
				break;
			case TYPE_TRIGGER:
				lineComps.addAll(Project.getCurrent().getCurrentWorkspace().getTriggers());
				break;
			case TYPE_INHERITANCE:
				lineComps.addAll(Project.getCurrent().getCurrentWorkspace().getInheritances());
				break;
			default:
				for (SchemaReference schemaReference : Project.getCurrent().getCurrentWorkspace().getSchemas()) {
					int padding = Canvas.GRID_SIZE;
					int top = padding;
					int left = padding;
					int tallest = 0;

					List<DraggableComponentReference> componentReferences = new ArrayList<>();
					switch (type) {
						case TYPE_TABLE:
							componentReferences.addAll(schemaReference.getRelations());
							break;
						case TYPE_FUNCTION:
							componentReferences.addAll(schemaReference.getFunctions());
							break;
						case TYPE_VIEW:
							componentReferences.addAll(schemaReference.getViews());
							break;
						case TYPE_SEQUENCE:
							componentReferences.addAll(schemaReference.getSequences());
							break;
						case TYPE_PACKAGE:	/* TODO */
							break;
						default:
							throw new IllegalStateException("Type " + type + " is not known!");
					}

					for (DraggableComponentReference componentReference : componentReferences) {
						schemaReference.add(componentReference);
						if (componentReference.getRememberedLocation() == null) {
							componentReference.setLocation(Geometry.getSnappedPosition(left, top));

							if (tallest < componentReference.getSize().height) {
								tallest = componentReference.getSize().height;
							}
							left += padding + componentReference.getSize().width;
							if (left + componentReference.getSize().width > schemaReference.getSize().width) {
								left = padding;
								top += tallest + padding;
								tallest = 0;
							}
						} else {
							componentReference.checkSize();
							Dimension size = Geometry.getZoomed(componentReference.getRememberedSize());
							Point location = Geometry.getZoomed(componentReference.getRememberedLocation());
							componentReference.setLocation(location);
							if (top < size.height + location.y + padding) {
								left = padding;
								top = size.height + location.y + padding;
							}
						}
					}
					schemaReference.checkSize();
				}
				return;
		}
		for (LineComponentReference comp : lineComps) {
			comp.checkSize();
			add(comp);
		}
	}

	private void drawFunctions() {
		if (Project.getCurrent().getCurrentWorkspace() != null) {
			drawWorkspace(TYPE_FUNCTION);
		} else {
			IConnection conn = Project.getCurrent().getCurrentConn();
			for (Schema schema : Project.getCurrDB().getSchemas()) {

				// Skip hidden
				if (!schema.isInEnvironment(conn)) {
					continue;
				}

				int padding = /*getZoomed*/(Canvas.GRID_SIZE);
				int top = schema.getSize().height;
				int left = padding;
				int tallest = 0;

				for (Function function : schema.getFunctions()) {

					// Skip hidden
					if (!function.isInEnvironment(conn)) {
						continue;
					}

					function.checkSize();
					schema.add(function);
					schema.setComponentZOrder(function, 0);
					if (function.getRememberedLocation() == null) {
						function.setLocation(Geometry.getSnappedPosition(left, top));

						if (tallest < function.getSize().height) {
							tallest = function.getSize().height;
						}
						left += padding + function.getSize().width;
						if (left + function.getSize().width > schema.getSize().width) {
							left = padding;
							top += tallest + padding;
							tallest = 0;
						}
					} else {
						Dimension size = Geometry.getZoomed(function.getRememberedSize());
						Point location = Geometry.getZoomed(function.getRememberedLocation());
						function.setSize(size);
						function.setLocation(location);
						if (top < size.height + location.y + padding) {
							left = padding;
							top = size.height + location.y + padding;
						}
					}
				}
				schema.checkSize();
			}
		}
	}

	private void drawPackages() {
		if (Project.getCurrent().getCurrentWorkspace() != null) {
			drawWorkspace(TYPE_PACKAGE);
		} else {
			IConnection conn = Project.getCurrent().getCurrentConn();
			for (Schema schema : Project.getCurrDB().getSchemas()) {

				// Skip hidden
				if (!schema.isInEnvironment(conn)) {
					continue;
				}

				int padding = /*getZoomed*/(Canvas.GRID_SIZE);
				int top = schema.getSize().height;
				int left = padding;
				int tallest = 0;

				for (Package pack : schema.getPackages()) {

					// Skip hidden
					if (!pack.isInEnvironment(conn)) {
						continue;
					}

					pack.checkSize();
					schema.add(pack);
					schema.setComponentZOrder(pack, 0);
					if (pack.getRememberedLocation() == null) {
						pack.setLocation(Geometry.getSnappedPosition(left, top));

						if (tallest < pack.getSize().height) {
							tallest = pack.getSize().height;
						}
						left += padding + pack.getSize().width;
						if (left + pack.getSize().width > schema.getSize().width) {
							left = padding;
							top += tallest + padding;
							tallest = 0;
						}
					} else {
						Dimension size = Geometry.getZoomed(pack.getRememberedSize());
						Point location = Geometry.getZoomed(pack.getRememberedLocation());
						pack.setSize(size);
						pack.setLocation(location);
						if (top < size.height + location.y + padding) {
							left = padding;
							top = size.height + location.y + padding;
						}
					}
				}
				schema.checkSize();
			}
		}
	}

	private void drawTriggers() {
		if (!Project.getCurrDB().getConnection().isSupported(SupportedElement.TRIGGER_BODY)) {
			if (Project.getCurrent().getCurrentWorkspace() != null) {
				drawWorkspace(TYPE_TRIGGER);
			} else {
				IConnection conn = Project.getCurrent().getCurrentConn();
				for (Trigger trigger : Project.getCurrDB().getTriggers()) {

					// Skip hidden
					if (!trigger.isInEnvironment(conn)) {
						continue;
					}

					trigger.checkSize();
					add(trigger);
				}
			}
		}
	}

	private void drawIndexes() {
		if (Project.getCurrent().getCurrentWorkspace() != null) {
			drawIndexesWorkspace();
		} else {
			IConnection conn = Project.getCurrent().getCurrentConn();
			for (Schema schema : Project.getCurrDB().getSchemas()) {

				// Skip hidden
				if (!schema.isInEnvironment(conn)) {
					continue;
				}
				for (Relation relation : schema.getRelations()) {

					// Skip hidden
					if (!relation.isInEnvironment(conn)) {
						continue;
					}
					relation.checkIndexLinesSize();
					schema.add(relation.getIndexLinePanel());
					for (Index ind : relation.getIndexes()) {

						// Skip hidden
						if (!ind.isInEnvironment(conn)) {
							continue;
						}

						ind.checkSize();
						schema.add(ind);
					}
				}
			}
		}
	}

	private void drawIndexesWorkspace() {
		for (SchemaReference schema : Project.getCurrent().getCurrentWorkspace().getSchemas()) {
			for (RelationReference relationReference : schema.getRelations()) {
				relationReference.checkIndexLinesSize();
				schema.add(relationReference.getIndexLinePanel());
				for (Index index : relationReference.getElement().getIndexes()) {
					index.checkSize(relationReference);
					schema.add(index);
				}
			}
		}
	}

	private void drawRelationDataInfo() {
		if (Project.getCurrent().getCurrentWorkspace() != null) {
			drawRelationDataInfoWorkspace();
		} else {
			for (Schema schema : Project.getCurrDB().getSchemas()) {
				schema.getRelations().forEach(this::drawRelationDataInfo);
			}
		}
	}

	private void drawRelationDataInfoWorkspace() {
		for (SchemaReference schema : Project.getCurrent().getCurrentWorkspace().getSchemas()) {
			schema.getRelations().forEach(this::drawRelationDataInfo);
		}
	}

	public void drawRelationDataInfo(Relation relation) {
		relation.removeAll();
		int attSize = Geometry.getZoomed(Attribute.V_SIZE);
		List<Attribute> attributeList = new ArrayList<>();
		relation.getDataInfo(attributeList);
		for (int k = 0; k < attributeList.size(); k++) {
			Attribute val = attributeList.get(k);
			val.setLocation(1, (k + 1) * attSize);
			val.setRel(relation);
			relation.add(val);
			if (k + 1 >= relation.getAttributes().size() && !relation.isSelected()) {
				break;
			}
		}
		relation.repaint();
	}

	public void drawRelationDataInfo(RelationReference relationReference) {
		relationReference.removeAll();
		int attSize = Geometry.getZoomed(Attribute.V_SIZE);
		List<Attribute> attributeList = new ArrayList<>();
		relationReference.getElement().getDataInfo(attributeList);
		for (int k = 0; k < attributeList.size(); k++) {
			Attribute val = attributeList.get(k);
			val.setLocation(1, (k + 1) * attSize);
			relationReference.add(val);
			if ((k + 1) * attSize > relationReference.getSize().height - 2 * attSize - 2) {
				break;
			}
		}
		repaint();
	}

	public Point getScrollPosition() {
		return new Point(scrollPane.getHorizontalScrollBar().getValue(), scrollPane.getVerticalScrollBar().getValue());
	}

	public Dimension getScrollSize() {
		return scrollPane.getSize();
	}

	public Dimension getScrollMaxSize() {
		return new Dimension(scrollPane.getHorizontalScrollBar().getMaximum() - scrollPane.getSize().width, scrollPane.getVerticalScrollBar().getMaximum() - scrollPane.getSize().height);
	}

	public void setSelectedElement(IModelElement element) {
		if (selectedElem != null) {
			if (element == null || !(
					selectedElem instanceof Function ?
							selectedElem.toString().equals(element.toString()) :
							(selectedElem.getFullPath() + selectedElem.getFullName()).equals(element.getFullPath() + element.getFullName())
			)) {
				selectedElem.unSelect();
				selectedElem.setSelected(false);
			}
		}
		selectedElem = element;
		if (selectedElem != null) {
			selectedElem.setSelected(true);
			Menu.getInstance().setEntityButtonsEnabled();
			Schedule.inWorker(Schedule.TYPE_DELAY, () -> Neighborhood.instance.draw(selectedElem));
		} else {
			Menu.getInstance().setEntityButtonsEnabled();
			if (Project.getCurrent().getCurrentWorkspace() != null) {
				DBTree.instance.selectWorkspaceByName(Project.getCurrent().getCurrentWorkspace().toString());
			} else {
				DBTree.instance.selectCurrentDB();
			}
			Neighborhood.instance.draw(null);
		}
		if (overview != null) {
			overview.checkSize();
			overview.repaint();
		}
        HotMenu.instance.checkSize();
	}

	public IModelElement getSelectedElement() {
		return selectedElem;
	}

	public synchronized void checkWhitespace() {
		if(scrollPane == null) {
			return;
		}
		Schedule.reInvokeInEDT(Schedule.Named.CANVAS_CHECK_WHITESPACE, UIConstants.TYPE_TIMEOUT, () -> {
            Project project = Project.getCurrent();
            List schemasInDB = project.getCurrentWorkspace() != null ? project.getCurrentWorkspace().getSchemas() : (project.getCurrentDB() != null ? project.getCurrentDB().getSchemas() : new ArrayList());
            Rectangle usedSpace = new Rectangle(size.width, size.height, 0, 0);
            for (Object schemasInDB1 : schemasInDB) {
                DraggableComponent schema = (DraggableComponent) schemasInDB1;
                if (schema.getLocation().x < usedSpace.x) {
                    usedSpace.x = schema.getLocation().x;
                }
                if (schema.getLocation().y < usedSpace.y) {
                    usedSpace.y = schema.getLocation().y;
                }
                if (schema.getLocation().x + schema.getWidth() > usedSpace.width) {
                    usedSpace.width = schema.getLocation().x + schema.getWidth();
                }
                if (schema.getLocation().y + schema.getHeight() > usedSpace.height) {
                    usedSpace.height = schema.getLocation().y + schema.getHeight();
                }
            }
            boolean sizeChanged = false;

            // MARGIN CHANGE: LEFT AND TOP
            Point locationDiff = new Point(Geometry.getZoomed(WHITESPACE) - usedSpace.x, Geometry.getZoomed(WHITESPACE) - usedSpace.y);
            if (locationDiff.x != 0 || locationDiff.y != 0) {
                scrollRectToVisible(new Rectangle(scrollPane.getHorizontalScrollBar().getValue() + locationDiff.x,
                        scrollPane.getVerticalScrollBar().getValue() + locationDiff.y,
                        scrollPane.getSize().width,
                        scrollPane.getSize().height));
                for (Object schemasInDB1 : schemasInDB) {
                    if (schemasInDB1 instanceof Schema) {
                        Schema schema = (Schema) schemasInDB1;
                        schema.setLocationWoChecks(new Point(schema.getLocation().x + locationDiff.x, schema.getLocation().y + locationDiff.y));
                        schema.checkConstraints();
                    } else {
                        SchemaReference schema = (SchemaReference) schemasInDB1;
                        schema.setLocation(new Point(schema.getLocation().x + locationDiff.x, schema.getLocation().y + locationDiff.y));
                        schema.checkConstraints();
                    }
                }
                size.width += locationDiff.x;
                size.height += locationDiff.y;
                sizeChanged = true;
            }

            // MARGIN CHANGE: RIGHT AND BOTTOM
            if (usedSpace.width + Geometry.getZoomed(WHITESPACE) != size.width) {
                size.width = usedSpace.width + Geometry.getZoomed(WHITESPACE);
                sizeChanged = true;
            }
            if (usedSpace.height + Geometry.getZoomed(WHITESPACE) != size.height) {
                size.height = usedSpace.height + Geometry.getZoomed(WHITESPACE);
                sizeChanged = true;
            }
            if (sizeChanged) {
                setSize(size);
                checkInfoPanelAndOverviewLocation();
            }
            checkComponentZOrder();
        });
	}

	private void addListeners() {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && !e.isConsumed()) {
					e.consume();
				} else {
					setSelectedElement(null);
				}
			}
		});
		addMouseMotionListener(new DraggableComponentMouseListener());
	}

	@Override
	public void paintComponent(Graphics g) {
		Rectangle bounds = g.getClipBounds();
		boolean attributeRedraw = bounds.width == Canvas.ZOOMED_ENTITY_WIDTH - 7 && bounds.height == Attribute.V_SIZE;
		if(!attributeRedraw) {
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());

			if (gridEnabled && isDefaultZoom()) {
				g.setColor(UIConstants.Colors.getLabelForeground());
				drawDots(g, bounds);
			}
		}
	}

	private void drawDots(Graphics g, Rectangle bounds) {
		for (int y = bounds.y - bounds.y % GRID_SIZE; y < bounds.y + bounds.height; y += GRID_SIZE) {
            for (int x = bounds.x - bounds.x % GRID_SIZE; x < bounds.x + bounds.width; x += GRID_SIZE) {
                g.drawLine(x, y, x, y);
            }
        }
	}

	void setInfoPanel(Component infoPanel) {
		this.infoPanel = infoPanel;
	}

	void drawInfoPanel() {
		if(infoPanel != null) {
			add(infoPanel);
			setComponentZOrder(infoPanel, 0);
		}
	}

	void setOverview(Navigator overview) {
		this.overview = overview;
	}

	private void drawOverview() {
		if(overview != null) {
			add(overview);
			setComponentZOrder(overview, 0);
		}
	}

	private void drawHotMenu() {
        add(HotMenu.instance);
        setComponentZOrder(HotMenu.instance, 0);
    }

	public boolean checkZoomBeforeProjectClose() {
		if (!isDefaultZoom()) {
			Navigator.instance.resetZoom();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void clicked() {}

	@Override
	public void doubleClicked() {}

	@Override
	public void rightClicked() {
		Project.getCurrDB().rightClicked();
	}

	public static final class DragPoint extends DraggableComponent {

		public static DraggableComponent showDP(Attribute attr) {
			if (instance.dragPoint == null) {
				instance.add(instance.dragPoint = new DragPoint(attr));
				instance.add(instance.dragPoint.con);
			}
			return Canvas.instance.dragPoint;
		}

		public static void hideDP() {
			if (instance.dragPoint != null) {
				instance.remove(instance.dragPoint);
				instance.remove(instance.dragPoint.con);
				instance.dragPoint = null;
				instance.repaint();
			}
		}

		private Constraint con;

		private DragPoint(Attribute attr) {
			super();
			setPreferredSize(new Dimension(16, 16));
			setSize(new Dimension(16, 16));
			setBackground(UIConstants.Colors.YELLOW);

			createLine(attr);
		}

		@Override
		public void checkConstraints() {
		}

		@Override
		public void checkSize() {
		}

		@Override
		public void clicked() {
		}

		@Override
		public void doubleClicked() {
		}

		@Override
		public void rightClicked() {
		}

		@Override
		public Point getAbsCenter() {
			return Geometry.getUnZoomed(new Point(getX() + 8, getY() + 8));
		}

		@Override
		public void setLocation(Point location) {
			setLocation(location.x, location.y);
			con.checkSize();
		}

		private void createLine(Attribute attr) {
			con = new Constraint(attr.getDB(), "tmpLine");
			con.setRel1(attr.getRel());
			con.setAttr1(attr);
			con.setRel2(this);
			con.setAttr2(null);
		}


	}
}
