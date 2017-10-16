
package com.databazoo.devmodeler.tools.organizer;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.databazoo.components.UIConstants;
import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.window.ProgressWindow;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.Inheritance;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Sequence;
import com.databazoo.devmodeler.model.Workspace;
import com.databazoo.devmodeler.model.reference.FunctionReference;
import com.databazoo.devmodeler.model.reference.IReferenceElement;
import com.databazoo.devmodeler.model.reference.LineComponentReference;
import com.databazoo.devmodeler.model.reference.RelationReference;
import com.databazoo.devmodeler.model.reference.SchemaReference;
import com.databazoo.devmodeler.tools.Geometry;
import com.databazoo.tools.Dbg;

/**
 * Implementation of Force-directed Layout (http://en.wikipedia.org/wiki/Force_directed_layout) of elements inside a schema.
 * @author bobus
 */
class OrganizerForceDirected extends OrganizerAlphabetical {
	private final static int RESISTANCE = 1500000;
	private final static int GRAVITY = 15;
	private final static int FORCE_LIMIT = 800;
	private final static int CYCLES = UIConstants.isPerformant() ? 100 : 60;
	private final static int[] EXPLOSIVE_CYCLES = new int[]{10};
	private final static int SNAPPINESS = 1;

	private final Map<DraggableComponent, Point> forces = new HashMap<>();
	private Point lLoc;
	private Point forceVector;

	@Override
	public void organize(Workspace ws) {
		organize(op -> {
			OrganizerFactory.getCircular().organize(ws);
			for(int cycle=0; cycle<CYCLES; cycle++){
				super.organize(ws);
				op.setProgress((cycle+1)*100/CYCLES);
			}
		});
	}

	@Override
	public void organize(DB db) {
		organize(op -> {
			OrganizerFactory.getCircular().organize(db);
			int nextExplosion = 0;
			for (int cycle = 0; cycle < CYCLES; cycle++) {
				super.organize(db);
				if (cycle == EXPLOSIVE_CYCLES[nextExplosion] - 1) {
					explode(db);
					if (nextExplosion + 1 < EXPLOSIVE_CYCLES.length) {
						nextExplosion++;
					}
				}
				op.setProgress((cycle + 1) * 100 / CYCLES);
			}
			for (Schema schema : db.getSchemas()) {
				snapLocationsToGrid(schema);
			}
		});
	}

	private void organize(Consumer<ProgressWindow> function){
		Canvas.instance.quirksMode = true;

		ProgressWindow op = new ProgressWindow.Builder().withTitle("Rearranging elements").build();
		function.accept(op);
		op.done();
		Canvas.instance.quirksMode = false;
		Canvas.instance.drawProject(true);
	}

	private void explode(DB db) {
		for(Schema schema : db.getSchemas()){
			List<DraggableComponent> elems = new ArrayList<>(schema.getRelations());
			for(Function func : schema.getFunctions()){
				if(func.hasNoTriggers()){
					elems.add(func);
				}else {
					func.isOrganized = false;
				}
			}
			elems.addAll(schema.getViews());
			elems.addAll(schema.getPackages());
			for(Sequence seq: schema.getSequences()){
				if(seq.getAttributes().isEmpty()) {
					elems.add(seq);
				}
			}
			for(DraggableComponent rel : elems){
				Point loc = rel.getRememberedLocation();
				rel.setLocation(new Point((int)loc.getX()*2, (int)loc.getY()*2));
			}
			Point loc = schema.getRememberedLocation();
			schema.setLocation(new Point((int)loc.getX()*2, (int)loc.getY()*2));
		}
	}

	@Override
	public void organize(Schema schema, int cycles) {
		super.organize(schema,cycles);
		snapLocationsToGrid(schema);
	}

	private void snapLocationsToGrid(Schema schema) {
		List<DraggableComponent> elems = new ArrayList<>(schema.getRelations());
		elems.addAll(schema.getFunctions().stream().filter(Function::hasNoTriggers).collect(Collectors.toList()));
		elems.addAll(schema.getViews());
		elems.addAll(schema.getPackages());
		elems.addAll(schema.getSequences().stream().filter(seq -> seq.getAttributes().isEmpty()).collect(Collectors.toList()));
		for (DraggableComponent elem : elems) {
			Point pos = Geometry.getSnappedPosition(Math.round(((float)elem.getX())/SNAPPINESS), Math.round(((float)elem.getY())/SNAPPINESS));
			elem.setLocation(new Point((int)pos.getX()*SNAPPINESS, (int)pos.getY()*SNAPPINESS));
		}
	}

	@Override
	public void organize(Schema schema) {
		try {
			organizeSchema(schema);
		} catch (Exception e) {
			Dbg.info(e);
		}
	}

	@Override
	public void organize(SchemaReference schema) {
		try {
			organizeSchema(schema);
		} catch (Exception e) {
			Dbg.info(e);
		}
	}

	private void organizeSchema(Object schema) {
		List<DraggableComponent> elems = new ArrayList<>();
		if(schema instanceof Schema) {
			Schema scLocal = (Schema) schema;
			elems.addAll(scLocal.getRelations());
			for (Function func : scLocal.getFunctions()) {
				if (func.hasNoTriggers()) {
					elems.add(func);
				} else {
					func.isOrganized = false;
				}
			}
			elems.addAll(scLocal.getViews());
			elems.addAll(scLocal.getPackages());
			elems.addAll(scLocal.getSequences().stream().filter(seq -> seq.getAttributes().isEmpty()).collect(Collectors.toList()));
		}else if(schema instanceof SchemaReference) {
			SchemaReference scLocal = (SchemaReference) schema;
			elems.addAll(scLocal.getRelations());
			for (FunctionReference func : scLocal.getFunctions()) {
				if (func.getElement().hasNoTriggers()) {
					elems.add(func);
				} else {
					func.isOrganized = false;
				}
			}
			elems.addAll(scLocal.getViews());
			elems.addAll(scLocal.getSequences().stream().filter(seq -> seq.getElement().getAttributes().isEmpty()).collect(Collectors.toList()));
		}else{
			throw new IllegalArgumentException("Invalid object type - Schema or SchemaReference expected");
		}
		if(elems.size() <= 1 && schema instanceof Schema){
			super.organize((Schema)schema);

		}else{
			forces.clear();
			for (DraggableComponent elem : elems) {
				elem.isOrganized = false;
				forces.put(elem, new Point(0,0));
				if(elem instanceof Relation) {
					elem.checkSize();
				}else if(elem instanceof RelationReference) {
					((IReferenceElement) elem).getElement().checkSize();
				}
			}
			for (DraggableComponent elem : elems) {
				lLoc = elem.getAbsCenter();
				forceVector = forces.get(elem);

				// Push away all elements
				for (DraggableComponent remote : elems) {
					if(remote==elem) {
						continue;
					}
					push(elem, remote);
				}

				// Relations
				if(elem instanceof Relation) {
					Relation r = (Relation) elem;

					// Pull with constraints
					for (Constraint con : r.getConstraints()) {
						if(con.getRel2()==null || (con.getRel1()==elem && con.getRel2()==elem)) {
							continue;
						}
						pull(elem, con.getRel2()==elem ? con.getRel1() : con.getRel2(), null);
					}

					// Pull with inheritances
					if(!r.getInheritances().isEmpty()){
						for(Inheritance in: r.getInheritances()){
							if(in.getRel1()==elem){
								pull(elem, in.getRel2(), null);
							}
						}
					}
				}

				// RelationReferences
				if(elem instanceof RelationReference) {
					RelationReference r = (RelationReference) elem;

					// Pull with constraints
					for (LineComponentReference con : r.getLinks()) {
						if(con.getRel2()==null || (con.getRel1()==elem && con.getRel2()==elem)) {
							continue;
						}
						pull(elem, con.getRel2()==elem ? con.getRel1() : con.getRel2(), null);
					}
				}

				// Pull to center
				pull(elem, null, ((DraggableComponent)elem.getParent()).getAbsCenter());

				applyElementVector(elem);

				if(elem instanceof Relation) {
					organizeFunctions((Relation) elem);
				}
			}
		}
		((DraggableComponent)schema).checkSize();
	}

	private void applyElementVector(DraggableComponent elem){
		if(forceVector.x != 0 || forceVector.y != 0){
			Point loc = new Point(elem.getLocation().x + forceVector.x, elem.getLocation().y + forceVector.y);
			if(loc.x < 0){ loc.x = 0; }
			if(loc.y < 0){ loc.y = 0; }
			elem.setLocation(loc);
		}
	}

	private void pull(DraggableComponent local, DraggableComponent remote, Point rLoc){
		if(remote != null){
			rLoc = remote.getAbsCenter();
		}

		// Location differences
		int distX = lLoc.x - rLoc.x;
		int distY = lLoc.y - rLoc.y;

		// Absolute distance
		double dist = Math.sqrt(Math.pow(distX, 2) + Math.pow(distY, 2));

		// Force grows linearly
		double force = GRAVITY * Math.pow(dist, 0.6d) / 100f;

		if(
			(
			local != null && local instanceof Relation &&
			remote != null && remote instanceof Relation &&
			((Relation)remote).getBehavior().getSchemaName().equals(((Relation)local).getBehavior().getSchemaName())) ||
			remote == null
		){
			force *= 5;
		}
		if(force > FORCE_LIMIT){
			Dbg.info("Restricting pull force from "+force);
			force = FORCE_LIMIT;
		}

		// Split force between X and Y vectors
		double vectorX = -Math.signum(distX) * Math.sqrt( Math.pow(force, 2) * Math.pow(distX, 2) / (Math.pow(distX, 2) + Math.pow(distY, 2)));
		double vectorY = -Math.signum(distY) * Math.sqrt( Math.pow(force, 2) * Math.pow(distY, 2) / (Math.pow(distX, 2) + Math.pow(distY, 2)));

		forceVector.x += Math.round(vectorX);
		forceVector.y += Math.round(vectorY);
	}

	private void push(DraggableComponent local, DraggableComponent remote){
		Point rLoc = remote.getAbsCenter();

		// Location differences
		int distX = lLoc.x - rLoc.x;
		int distY = lLoc.y - rLoc.y;

		// Ignore all resistance on large distances
		if(distX > FORCE_LIMIT || distY > FORCE_LIMIT || distX < -FORCE_LIMIT || distY < -FORCE_LIMIT) {
			return;
		}

		// Absolute distance
		double dist = Math.sqrt(Math.pow(distX, 2) + Math.pow(distY, 2));

		// Force reduced by distance squared
		double force = RESISTANCE / Math.pow(dist, 1.8);
		if(force > FORCE_LIMIT){
			Dbg.info("Restricting push force from "+force);
			force = FORCE_LIMIT;
		}

		// Split force between X and Y vectors
		double vectorX = Math.signum(distX) * Math.sqrt( Math.pow(force, 2) * Math.pow(distX, 2) / (Math.pow(distX, 2) + Math.pow(distY, 2)));
		double vectorY = Math.signum(distY) * Math.sqrt( Math.pow(force, 2) * Math.pow(distY, 2) / (Math.pow(distX, 2) + Math.pow(distY, 2)));

		forceVector.x += Math.round(vectorX);
		forceVector.y += Math.round(vectorY);
	}

}
