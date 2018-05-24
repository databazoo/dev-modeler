
package com.databazoo.devmodeler.tools.organizer;

import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.model.*;
import com.databazoo.devmodeler.model.Package;
import com.databazoo.devmodeler.model.reference.FunctionReference;
import com.databazoo.devmodeler.model.reference.RelationReference;
import com.databazoo.devmodeler.model.reference.SchemaReference;
import com.databazoo.devmodeler.tools.Geometry;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tool for automatic placement of elements in model
 *
 * TODO: merge organize(Workspace) and organize(DB) back together
 *
 * @author	bobus
 */
public class OrganizerAlphabetical implements Organizer {

	final int H_PAD = Canvas.GRID_SIZE * 2;
	final int V_PAD = Canvas.GRID_SIZE;

	OrganizerAlphabetical(){}

	@Override public void organize(Workspace ws) {
		List<SchemaReference> schemata = ws.getSchemas();
		int perWidth = (int)Math.ceil(Math.sqrt(schemata.size()*0.8));

		for(SchemaReference schema: schemata){
			organize(schema);
		}

		int pad = Canvas.GRID_SIZE;
		int left = Canvas.WHITESPACE;
		int top = Canvas.WHITESPACE;
		int tallest = 0;
		for(int i=0; i<schemata.size(); i++){
			SchemaReference schema = schemata.get(i);
			schema.setLocation(Geometry.getSnappedPosition(left, top));
			left += schema.getWidth() + pad;
			if(tallest < schema.getHeight()){
				tallest = schema.getHeight();
			}
			if(i % perWidth == perWidth-1){
				left = Canvas.WHITESPACE;
				top = Geometry.getSnappedPosition(0, top + tallest + pad).y;
				tallest = 0;
			}
		}
		Canvas.instance.drawProject(true);
	}
	@Override public void organize(SchemaReference schema, int cycles) {
		for(int i=0; i<cycles; i++){
			organize(schema);
		}
	}
	@Override public void organize(SchemaReference schema) {
		List<RelationReference> rels = schema.getRelations();
		List<FunctionReference> funcs = schema.getFunctions();
		int perWidth = (int)Math.ceil(
			Math.sqrt(
				(rels.size()*1.0 + funcs.size()*0.1)
			));
		for (RelationReference rel : rels) {
			rel.isOrganized = false;
			rel.checkSize();
		}
		for (FunctionReference func : funcs) {
			func.isOrganized = false;
		}

		int left = H_PAD;
		int top = V_PAD;
		int tallest = 0;

		if(!rels.isEmpty()){
			for(int i=0; i<rels.size(); i++){
				RelationReference rel = rels.get(i);
				rel.setLocation(Geometry.getSnappedPosition(left, top));
				Dimension functionExtraSpace = organizeFunctions(rel);
				rel.checkConstraints();

				if(tallest < rel.getSize().height + functionExtraSpace.height){
					tallest = rel.getSize().height + functionExtraSpace.height;
				}
				left += H_PAD + rel.getSize().width + functionExtraSpace.width;
				if(i % perWidth == perWidth-1){
					left = H_PAD;
					top = Geometry.getSnappedPosition(0, top + tallest + V_PAD).y;
					tallest = 0;
				}
			}
			for(FunctionReference func: funcs){
				if(!func.isOrganized){
					func.setLocation(new Point(0, 0));
				}
			}
			schema.checkSize();
		}else{
			schema.setSize(H_PAD + perWidth * (Canvas.DEFAULT_ENTITY_WIDTH + H_PAD), 100);
		}
		left = H_PAD;
		top = Geometry.getSnappedPosition(0, top + tallest + V_PAD).y;
		tallest = 0;
		for(FunctionReference func: funcs){
			if(!func.isOrganized){
				func.setLocation(Geometry.getSnappedPosition(left, top));
				func.checkConstraints();

				if(tallest < func.getSize().height){
					tallest = func.getSize().height;
				}
				left += H_PAD + func.getSize().width;
				if(schema.getWidth() < left + func.getSize().width){
					left = H_PAD;
					top = Geometry.getSnappedPosition(0, top + tallest + V_PAD).y;
					tallest = 0;
				}
			}
		}
		schema.checkSize();
	}

	@Override public void organize(DB db) {
		List<Schema> schemata = db.getSchemas();
		int perWidth = (int)Math.ceil(Math.sqrt(schemata.size()*0.8));

		int widest = 0;
		for(Schema schema: schemata){
			organize(schema);
			if(widest < schema.getWidth()){
				widest = schema.getWidth();
			}
		}

		int pad = Canvas.GRID_SIZE;
		int left = Canvas.WHITESPACE;
		int top = Canvas.WHITESPACE;
		int tallest = 0;
		for(int i=0; i<schemata.size(); i++){
			Schema schema = schemata.get(i);
			schema.setLocation(Geometry.getSnappedPosition(left, top));
			left += schema.getWidth() + pad;
			if(tallest < schema.getHeight()){
				tallest = schema.getHeight();
			}
			if(i % perWidth == perWidth-1){
				left = Canvas.WHITESPACE;
				top = Geometry.getSnappedPosition(0, top + tallest + pad).y;
				tallest = 0;
			}
		}
		Canvas.instance.drawProject(true);
	}
	@Override public void organize(Schema schema, int cycles) {
		for(int i=0; i<cycles; i++){
			organize(schema);
		}
	}
	@Override public void organize(Schema schema) {
		List<Relation> rels = schema.getRelations();
		List<Function> funcs = schema.getFunctions();
		List<Package> packs = schema.getPackages();
		int perWidth = (int)Math.ceil(
			Math.sqrt(
				(rels.size()*1.0 + funcs.size()*0.1 + packs.size()*0.2)	// lower values for tall schema, higher values for wide schema
			));
		for (Relation rel : rels) {
			rel.isOrganized = false;
			rel.checkSize();
		}
		for (Function func : funcs) {
			func.isOrganized = false;
		}
		for (Package pack : packs) {
			pack.isOrganized = false;
		}

		int left = H_PAD;
		int top = V_PAD;
		int tallest = 0;

		if(!rels.isEmpty()){
			for(int i=0; i<rels.size(); i++){
				Relation rel = rels.get(i);
				rel.setLocation(Geometry.getSnappedPosition(left, top));
				Dimension functionExtraSpace = organizeFunctions(rel);
				rel.checkConstraints();
				rel.isOrganized = true;

				if(tallest < rel.getSize().height + functionExtraSpace.height){
					tallest = rel.getSize().height + functionExtraSpace.height;
				}
				left += H_PAD + rel.getSize().width + functionExtraSpace.width;
				if(i % perWidth == perWidth-1){
					left = H_PAD;
					top = Geometry.getSnappedPosition(0, top + tallest + V_PAD).y;
					tallest = 0;
				}
			}
			for(Function func: funcs){
				if(!func.isOrganized){
					func.setLocation(new Point(0, 0));
				}
			}
			for(Package pack : packs){
				if(!pack.isOrganized){
					pack.setLocation(new Point(0, 0));
				}
			}
			schema.checkSize();
		}else{
			schema.setSize(H_PAD + perWidth * (Canvas.DEFAULT_ENTITY_WIDTH + H_PAD), 100);
		}
		left = H_PAD;
		top = Geometry.getSnappedPosition(0, top + tallest + V_PAD).y;
		tallest = 0;
		ArrayList<DraggableComponent> elems = new ArrayList<>();
		elems.addAll(schema.getViews());
		elems.addAll(schema.getSequences().stream().filter(seq -> seq.getAttributes().isEmpty()).collect(Collectors.toList()));
		elems.addAll(funcs.stream().filter(func -> !func.isOrganized).collect(Collectors.toList()));
		elems.addAll(packs.stream().filter(func -> !func.isOrganized).collect(Collectors.toList()));
		for(DraggableComponent func: elems){
			func.setLocation(Geometry.getSnappedPosition(left, top));
			func.checkConstraints();
			func.isOrganized = true;

			if(tallest < func.getSize().height){
				tallest = func.getSize().height;
			}
			left += H_PAD + func.getSize().width;
			if(H_PAD + perWidth * (Canvas.DEFAULT_ENTITY_WIDTH + H_PAD) < left + func.getSize().width){
				left = H_PAD;
				top = Geometry.getSnappedPosition(0, top + tallest + V_PAD).y;
				tallest = 0;
			}
		}
		schema.checkSize();
	}

	public Dimension organizeFunctions(RelationReference rel) {
		boolean hasFuncs = false;
		int funcOffsetY = rel.getLocation().y + rel.getSize().height;
		for(Trigger trig: rel.getElement().getTriggers()){
			FunctionReference func = rel.getWorkspace().find((Function)trig.getRel2());
			if(func != null && !func.isOrganized && func.getElement().getBehavior().getSchemaName().equals(rel.getElement().getBehavior().getSchemaName())){
				Point loc = Geometry.getSnappedPosition(rel.getLocation().x + H_PAD*2, funcOffsetY + V_PAD);
				func.setLocation(loc);
				funcOffsetY = loc.y;
				func.isOrganized = true;
				hasFuncs = true;
			}
		}
		return new Dimension(hasFuncs ? H_PAD*2 : 0, funcOffsetY - rel.getLocation().y - rel.getSize().height);
	}

	public Dimension organizeFunctions(Relation rel) {
		boolean hasFuncs = false;
		int funcOffsetY = rel.getLocation().y + rel.getSize().height;
		for(Trigger trig: rel.getTriggers()){
			Function func = (Function)trig.getRel2();
			if(func != null && !func.isOrganized && func.getBehavior().getSchemaName().equals(rel.getBehavior().getSchemaName())){
				Point loc = Geometry.getSnappedPosition(rel.getLocation().x + H_PAD*2, funcOffsetY + V_PAD);
				func.setLocation(loc);
				funcOffsetY = loc.y;
				func.isOrganized = true;
				hasFuncs = true;
			}
		}
		return new Dimension(hasFuncs ? H_PAD*2 : 0, funcOffsetY - rel.getLocation().y - rel.getSize().height);
	}
}
