
package com.databazoo.devmodeler.model.explain;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.databazoo.components.FontFactory;
import com.databazoo.components.UIConstants;
import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.gui.window.ExplainContextWindow;
import com.databazoo.tools.Schedule;


/**
 * Icon of given operation in EXPLAIN diagram.
 *
 * @author bobus
 */
public class ExplainOperation extends DraggableComponent {

	private static final String L_DEFAULT = "default";

	public static int DEFAULT_WIDTH = 150;
	public static int DEFAULT_HEIGHT = 124;
	public static int SPACING = 0;
	public static int PADDING = 30;

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private static final IconHashMap ICONS = new IconHashMap();

	public final String opName;
	public final String relName;
	private final String indName;
	public final String[] options;
	public final int level;
	private final transient Icon ico;

	public ExplainLoad parentLink;
	public transient List<ExplainLoad> childrenLinks = new ArrayList<>();
	public transient List<String> extraInfo = new ArrayList<>();
	private int takenY = 0;
	private ExplainOperation explainParent;

	public ExplainOperation(String operationName, String relationName, String indexName,  String[] opts, int lvl) {
		options = opts;
		level = lvl;
		checkSize();

		opName = operationName;
		relName = relationName;
		indName = indexName;

		Icon icon = ICONS.get(opName);
		if(icon != null){
			ico = icon;
		}else{
			ico = ICONS.get(L_DEFAULT);
		}
		draw();
	}

	public ExplainOperation(String name, String[] split, int lvl) {
		options = split;
		level = lvl;
		checkSize();

		if(name.matches(".* on .*")){
			if(name.matches(".* using .*")){
				opName = name.replaceAll("(.*) using (.*) on (.*)", "$1");
				relName = name.replaceAll("(.*) using (.*) on (.*)", "$3");
				indName = name.replaceAll("(.*) using (.*) on (.*)", "$2");
			}else{
				opName = name.replaceAll("(.*) on (.*)", "$1");
				relName = name.replaceAll("(.*) on (.*)", "$2");
				indName = null;
			}
		}else{
			opName = name;
			relName = null;
			indName = null;
		}
		Icon icon = ICONS.get(opName);
		if(icon != null){
			ico = icon;
		}else{
			ico = ICONS.get(L_DEFAULT);
		}
		draw();
	}

	private void draw(){
		setFocusable(true);
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		JLabel lab;
		add(new JLabel(" "));

		lab = new JLabel(opName);
		lab.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(lab);

		lab = new JLabel(ico);
		lab.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(lab);

		if(indName != null){
			lab = new JLabel(indName);
			lab.setToolTipText(indName);
			lab.setAlignmentX(Component.CENTER_ALIGNMENT);
			lab.setFont(FontFactory.getMonospaced(Font.ITALIC, Settings.getInt(Settings.L_FONT_MONO_SIZE)));
			lab.setForeground(UIConstants.COLOR_GREEN);
			add(lab);
		}
		if(relName != null){
			lab = new JLabel(relName);
			lab.setToolTipText(relName);
			lab.setAlignmentX(Component.CENTER_ALIGNMENT);
			lab.setFont(FontFactory.getMonospaced(Font.ITALIC, Settings.getInt(Settings.L_FONT_MONO_SIZE)));
			lab.setForeground(UIConstants.COLOR_GREEN);
			add(lab);
		}
		addFocusListener(new FocusAdapter(){
			@Override public void focusLost(FocusEvent fe) { ExplainContextWindow.get().dispose(); }
		});
	}

	public double getCost(){
		for (String option : options) {
			if (option.contains("cost=")) {
				return Double.parseDouble(option.replaceAll("cost=([0-9.]+\\.\\.)?([0-9.]+)", "$2"));
			} else if (option.contains("rows=")) {
				return Math.pow(Double.parseDouble(option.replaceAll("rows=([0-9.]+)", "$1")), 1.8);
			}
		}
		return 1;
	}

	public void append(ExplainOperation nextOperation) {
		ExplainLoad load = new ExplainLoad();
		load.setRel1(this);
		load.setRel2(nextOperation);
		load.assignToOperations();
	}
/*
	public ExplainOperation getParentNode(){
		if(parentLink == null){
			return null;
		}else{
			return (ExplainOperation) parentLink.getRel1();
		}
	}*/

	public ExplainOperation getParentNode(int lvl){
		if(level == lvl){
			return this;
		}
		if(parentLink == null){
			return null;	// end of line: have no parent
		}else{
			return ((ExplainOperation) parentLink.getRel1()).getParentNode(lvl);
		}
	}

	@Override
	public void checkConstraints(){
		for(ExplainLoad l: childrenLinks){
			l.checkSize();
		}
		if(parentLink != null){
			parentLink.checkSize();
		}
	}
	@Override
	public Point getCenter(){
		Point l = getLocation();
		l.x += (int)Math.round(getSize().width/2.0);
		l.y += (int)Math.round(getSize().height/2.0);
		return l;
	}
	@Override
	public Point getAbsCenter(){
		return getCenter();
	}

	@Override public void clicked(){
		ExplainContextWindow.get().dispose();
		requestFocusInWindow(true);
		Schedule.reInvokeInEDT(Schedule.Named.RELATION_DATA_REPAINT, Schedule.CLICK_DELAY, this::drawMoreInfo);
	}
	@Override public void doubleClicked(){}
	@Override public void rightClicked(){}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D graphics = (Graphics2D) g;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	}

	private void drawMoreInfo(){
		ExplainContextWindow context = ExplainContextWindow.get();
		context.clear();
		context.text("<b>"+opName+"</b>");
		if(relName != null) {
			context.text("Table: <font color='green'>"+relName+"</font>");
		}
		if(indName != null) {
			context.text("Index: <font color='green'>"+indName+"</font>");
		}
		for(String info: extraInfo){
			info = info.replace("<", "&lt;").replace(">", "&gt;");
			if(info.startsWith("        Filter: ")){
				context.text("<font color='red'>"+info+"</font>");
			}else{
				context.text(info);
			}
		}
		if(options.length > 0){
			context.text("");
			for(String option: options){
				context.text(option);
			}
		}
		context.draw(getLocationOnScreen());
	}

	@Override
	public final void checkSize(){
		setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public int getTakenY(){
		if(takenY == 0){
			if(explainParent != null) {
				setTakenY(explainParent.getTakenY());
			}else{
				setTakenY(ExplainOperation.SPACING + ExplainOperation.DEFAULT_HEIGHT);
			}
		}
		return takenY;
	}
	public void setTakenY(int newTakenY){
		takenY = newTakenY;
		if(explainParent != null) {
			explainParent.setTakenY(newTakenY);
		}
	}

	public void setParent (ExplainOperation parent) {
		this.explainParent = parent;
	}

	private static class IconHashMap extends HashMap<String, ImageIcon> {

		IconHashMap(){
			super();
			createIconsBase();
			createIconsPg();
			createIconsMy();
			createIconsOracle();
		}

		private void createIconsBase(){
			put(L_DEFAULT,					Theme.getLargeIcon(Theme.ICO_EXPL_DEFAULT));
			put("Output",					Theme.getLargeIcon(Theme.ICO_EXPL_OUTPUT));
		}

		private void createIconsPg(){
			put("Sort",						Theme.getLargeIcon(Theme.ICO_EXPL_SORT));
			put("Limit",					Theme.getLargeIcon(Theme.ICO_EXPL_LIMIT));
			put("Materialize",				Theme.getLargeIcon(Theme.ICO_EXPL_MATER));

			put("Function Scan",			Theme.getLargeIcon(Theme.ICO_EXPL_PROC));
			put("WindowAgg",				Theme.getLargeIcon(Theme.ICO_EXPL_WIN_AGG));
			put("Aggregate",				Theme.getLargeIcon(Theme.ICO_EXPL_AGG));
			put("GroupAggregate",			Theme.getLargeIcon(Theme.ICO_EXPL_AGG));
			put("HashAggregate",			Theme.getLargeIcon(Theme.ICO_EXPL_AGG));

			put("Seq Scan",					Theme.getLargeIcon(Theme.ICO_EXPL_SEQ));
			put("Index Scan",				Theme.getLargeIcon(Theme.ICO_EXPL_INDEX));
			put("Index Scan Backward",		Theme.getLargeIcon(Theme.ICO_EXPL_INDEX));
			put("Index Only Scan",			Theme.getLargeIcon(Theme.ICO_EXPL_INDEX));
			put("Bitmap Index Scan",		Theme.getLargeIcon(Theme.ICO_EXPL_INDEX));
			put("Bitmap Heap Scan",			Theme.getLargeIcon(Theme.ICO_EXPL_HEAP));
			put("Recheck Cond",				Theme.getLargeIcon(Theme.ICO_EXPL_RECHECK));
			put("Subquery Scan",			Theme.getLargeIcon(Theme.ICO_EXPL_SUB_Q));

			put("Append",					Theme.getLargeIcon(Theme.ICO_EXPL_APPEND));

			put("Hash Left Join",			Theme.getLargeIcon(Theme.ICO_EXPL_JOIN));
			put("Hash Right Join",			Theme.getLargeIcon(Theme.ICO_EXPL_JOIN));
			put("Hash Join",				Theme.getLargeIcon(Theme.ICO_EXPL_JOIN));
			put("Hash",						Theme.getLargeIcon(Theme.ICO_EXPL_HASH));

			put("Merge Left Join",			Theme.getLargeIcon(Theme.ICO_EXPL_JOIN));
			put("Merge Right Join",			Theme.getLargeIcon(Theme.ICO_EXPL_JOIN));
			put("Merge Join",				Theme.getLargeIcon(Theme.ICO_EXPL_JOIN));

			put("Nested Loop Left Join",	Theme.getLargeIcon(Theme.ICO_EXPL_LOOP));
			put("Nested Loop Right Join",	Theme.getLargeIcon(Theme.ICO_EXPL_LOOP));
			put("Nested Loop",				Theme.getLargeIcon(Theme.ICO_EXPL_LOOP));
		}

		private void createIconsMy(){
			put("const",					Theme.getLargeIcon(Theme.ICO_EXPL_SEQ));
			put(" const ",					Theme.getLargeIcon(Theme.ICO_EXPL_INDEX));
			put("ALL",						Theme.getLargeIcon(Theme.ICO_EXPL_SEQ));
			put("eq_ref",					Theme.getLargeIcon(Theme.ICO_EXPL_INDEX));
			put("ref",						Theme.getLargeIcon(Theme.ICO_EXPL_INDEX));
			put("range",					Theme.getLargeIcon(Theme.ICO_EXPL_INDEX));
			put("filesort",					Theme.getLargeIcon(Theme.ICO_EXPL_SORT));
			put("temporary",				Theme.getLargeIcon(Theme.ICO_EXPL_MATER));
			put("join buffer",				Theme.getLargeIcon(Theme.ICO_EXPL_JOIN));
			put("where",					Theme.getLargeIcon(Theme.ICO_EXPL_LIMIT));
			put("index",					Theme.getLargeIcon(Theme.ICO_EXPL_INDEX));
			put("const row not found",		Theme.getLargeIcon(Theme.ICO_EXPL_INVALID));
			put("Impossible WHERE noticed after reading const tables",		Theme.getLargeIcon(Theme.ICO_EXPL_INVALID));
		}

		private void createIconsOracle(){
			put("SELECT STATEMENT",			Theme.getLargeIcon(Theme.ICO_EXPL_SEQ));
			put("COUNT",					Theme.getLargeIcon(Theme.ICO_EXPL_LIMIT));
			put("TABLE ACCESS",				Theme.getLargeIcon(Theme.ICO_EXPL_SEQ));
			put("HASH JOIN",				Theme.getLargeIcon(Theme.ICO_EXPL_HASH));
			put("TABLE ACCESS BY INDEX",	Theme.getLargeIcon(Theme.ICO_EXPL_INDEX));
			put("INDEX",					Theme.getLargeIcon(Theme.ICO_EXPL_INDEX));
			put("NESTED LOOPS",				Theme.getLargeIcon(Theme.ICO_EXPL_LOOP));
		}
	}

}
