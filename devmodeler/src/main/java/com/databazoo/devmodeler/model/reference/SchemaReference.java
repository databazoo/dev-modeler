
package com.databazoo.devmodeler.model.reference;

import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Workspace;
import com.databazoo.devmodeler.tools.Geometry;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Reference to a schema (implemented as a draggable component)
 *
 * @author bobus
 */
public class SchemaReference extends DraggableComponentReference {
	protected final Schema schema;
	private final List<RelationReference> relations = new ArrayList<>();
	private final List<FunctionReference> functions = new ArrayList<>();
	private final List<ViewReference> views = new ArrayList<>();
	private final List<SequenceReference> sequences = new ArrayList<>();

	public SchemaReference(Workspace workspace, Schema schema) {
		super(workspace);
		this.schema = schema;
		draw();
	}

	public List<RelationReference> getRelations() {
		return relations;
	}

	public List<FunctionReference> getFunctions() {
		return functions;
	}

	public List<ViewReference> getViews() {
		return views;
	}

	public List<SequenceReference> getSequences() {
		return sequences;
	}

	private void draw(){
		setLayout(null);
		overbearingZOrder = 1;
		checkKnownLocation();
		checkSize();
	}
	@Override public void clicked(){
		Canvas.instance.setSelectedElement(null);
	}

	@Override
	public Schema getElement(){
		return schema;
	}
	@Override
	public String toString(){
		return schema.toString();
	}
	@Override
	public Icon getIcon16(){
		return schema.getIcon16();
	}

	void checkSize(DraggableComponentReference entity) {
		if(entity.getLocation().x + entity.getWidth() + Canvas.GRID_SIZE - 5 > getWidth()){
			setSize(new Dimension(entity.getLocation().x + entity.getWidth() + Canvas.GRID_SIZE - 5, getHeight()));
		}
		if(entity.getLocation().y + entity.getHeight() + 10 > getHeight()){
			setSize(new Dimension(getWidth(), entity.getLocation().y + entity.getHeight() + 10));
		}
	}

	@Override
	public void checkConstraints(){
		for(RelationReference rel:relations){
			rel.checkConstraints();
		}
		for(FunctionReference func: functions){
			func.checkConstraints();
		}
	}

	@Override
	public void checkSize(){
		int maxX=0;
		int maxY=0;

		ArrayList<DraggableComponentReference> comps = new ArrayList<>();
		comps.addAll(relations);
		comps.addAll(functions);
		comps.addAll(views);
		comps.addAll(sequences);

		for(DraggableComponentReference comp: comps){
			Dimension s = comp.getSize();
			Point l = comp.getLocation();
			if(s.width+l.x > maxX){
				maxX = s.width+l.x;
			}
			if(s.height+l.y > maxY){
				maxY = s.height+l.y;
			}
		}

		setSize(new Dimension(maxX+Geometry.getZoomed(Canvas.GRID_SIZE-5), maxY+11));
		schema.getDB().getProject().save();
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D graphics = (Graphics2D) g;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int width = getWidth();
		int height = getHeight();
		int gap = 1;

		graphics.setColor(Color.GRAY);
		graphics.setStroke(Canvas.getBasicStroke());
		graphics.drawRoundRect(0, 0, width - gap, height - gap, arcs.width, arcs.height);

		graphics.setFont(new Font(Font.DIALOG, Font.BOLD+Font.ITALIC, 13));
		graphics.drawString(schema.getBehavior().getName(), 16, 16);
	}

	@Override
	protected boolean haveClickableChild(Point p, int clickMask){
		int x = p.x + getLocation().x;
		int y = p.y + getLocation().y;

		ArrayList<LineComponentReference> comps = new ArrayList<>();
		comps.addAll(workspace.getConstraints());
		comps.addAll(workspace.getTriggers());

		for(LineComponentReference comp: comps){
			if(comp.clickedOnLine(x, y))
			{
				if(clickMask == CLICK_TYPE_LEFT) {
					comp.clicked();
				}else if(clickMask == CLICK_TYPE_DOUBLE) {
					comp.doubleClicked();
				}else{
					comp.rightClicked();
				}
				return true;
			}
		}
		return false;
	}

	@Override public void unSelect(){}

	@Override
	public void setLocation(Point loc){
		super.setLocation(loc);
		Canvas.instance.checkWhitespace();
	}

	@Override
	public void setSize(Dimension d){
		super.setSize(d);
		Canvas.instance.checkWhitespace();
	}

	@Override
	public void drop(){
		workspace.getSchemas().remove(this);
		if (workspace.getSchemas().isEmpty()) {
			workspace.drop();
		}
	}

	void checkEmpty() {
		if(relations.isEmpty() && functions.isEmpty() && views.isEmpty() && sequences.isEmpty()) {
			drop();
		}
	}
}
