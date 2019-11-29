
package com.databazoo.devmodeler.tools.organizer;

import java.util.ArrayList;
import java.util.stream.Collectors;

import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.reference.IReferenceElement;
import com.databazoo.devmodeler.model.reference.RelationReference;
import com.databazoo.devmodeler.model.reference.SchemaReference;
import com.databazoo.devmodeler.tools.Geometry;

/**
 * Implementation of Circular Layout (http://en.wikipedia.org/wiki/Circular_layout) of elements inside a schema.
 * @author bobus
 */
class OrganizerCircular extends OrganizerAlphabetical {

	@Override
	public void organize(Schema schema) {
		ArrayList<DraggableComponent> elems = new ArrayList<>(schema.getRelations());
		elems.addAll(schema.getFunctions().stream().filter(Function::hasNoTriggers).collect(Collectors.toList()));
		elems.addAll(schema.getViews());
		elems.addAll(schema.getPackages());
		elems.addAll(schema.getSequences().stream().filter(seq -> seq.getAttributes().isEmpty()).collect(Collectors.toList()));
		if(elems.size() <= 2){
			super.organize(schema);

		}else{
			int mostAttrs = 4;
			for (DraggableComponent elem : elems) {
				elem.isOrganized = false;
				if(elem instanceof Relation) {
					Relation r = (Relation) elem;
					r.checkSize();
					if(mostAttrs < r.getCountAttributes()){
						mostAttrs = r.getCountAttributes();
					}
				}
			}

			// Adapt radius slightly to size of biggest tables
			double attrMod = Math.sqrt(mostAttrs*10)/14;

			// Calculate needed radius
			double radius = elems.size() * Canvas.DEFAULT_ENTITY_WIDTH * attrMod / Math.PI;

			// Place elements in circle
			for(int i=0; i<elems.size(); i++){
				DraggableComponent elem = elems.get(i);
				double moved = Math.PI*2f * i / elems.size();

				int x = (int)(radius+Math.sin(moved)*radius) + Canvas.GRID_SIZE;
				int y = (int)(radius-Math.cos(moved)*radius) + Canvas.GRID_SIZE + Canvas.DEFAULT_ENTITY_WIDTH/2 - elem.getHeight()/2;

				elem.setLocation(Geometry.getSnappedPosition(x, y));
				elem.isOrganized = true;
				if(elem instanceof Relation){
					organizeFunctions((Relation) elem);
				}
			}
		}
		schema.checkSize();
	}

	@Override
	public void organize(SchemaReference schema) {
		ArrayList<DraggableComponent> elems = new ArrayList<>(schema.getRelations());
		elems.addAll(schema.getFunctions().stream().filter(func -> func.getElement().hasNoTriggers()).collect(Collectors.toList()));
		elems.addAll(schema.getViews());
		elems.addAll(schema.getSequences().stream().filter(seq -> seq.getElement().getAttributes().isEmpty()).collect(Collectors.toList()));
		if(elems.size() <= 2){
			super.organize(schema);

		}else{
			int mostAttrs = 4;
			for (DraggableComponent elem : elems) {
				elem.isOrganized = false;
				if(elem instanceof RelationReference) {
					Relation r = (Relation) ((IReferenceElement) elem).getElement();
					r.checkSize();
					if(mostAttrs < r.getCountAttributes()){
						mostAttrs = r.getCountAttributes();
					}
				}
			}

			// Adapt radius slightly to size of biggest tables
			double attrMod = Math.sqrt(mostAttrs*10)/14;

			// Calculate needed radius
			double radius = elems.size() * Canvas.DEFAULT_ENTITY_WIDTH * attrMod / Math.PI;

			// Place elements in circle
			for(int i=0; i<elems.size(); i++){
				DraggableComponent elem = elems.get(i);
				double moved = Math.PI*2f * i / elems.size();

				int x = (int)(radius+Math.sin(moved)*radius) + Canvas.GRID_SIZE;
				int y = (int)(radius-Math.cos(moved)*radius) + Canvas.GRID_SIZE + Canvas.DEFAULT_ENTITY_WIDTH/2 - elem.getHeight()/2;

				elem.setLocation(Geometry.getSnappedPosition(x, y));
				elem.isOrganized = true;
				if(elem instanceof RelationReference){
					organizeFunctions((RelationReference) elem);
				}
			}
		}
		schema.checkSize();
		DesignGUI.get().drawProject(true);
	}
}
