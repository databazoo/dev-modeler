package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.tools.Geometry;
import com.databazoo.devmodeler.tools.NameGenerator;
import com.databazoo.tools.Dbg;

abstract class RelationWizardPagesTable extends RelationWizardPagesSchema {

	void loadNewTablePage1(){
		Dbg.toFile();
		Relation relation = new Relation(rel.getSchema(), rel.getSchema().getSchemaPrefix() + NameGenerator.createRelationName(rel.getSchema()));
		if(connection.isSupported(SupportedElement.RELATION_STORAGE)){
			relation.getBehavior().setStorage(connection.getDefaultStorageEngine());
		}
		if(connection.isSupported(SupportedElement.RELATION_COLLATION)){
			relation.getBehavior().setCollation(connection.getDefaultCollation());
		}
		relation.getBehavior().setNew();
		loadTablePage1(relation);
		if(RelationWizardConfig.isElementLocationSet()){
			relation.setLocation(RelationWizardConfig.consumeLocation());
		}
	}

	void loadTablePage1(Relation rel) {
		editableElement = rel;
		editableElement.getBehavior().prepareForEdit();
		Dbg.toFile(editableElement.getFullName());
		resetContent();

		focus(addTextInput(Relation.Behavior.L_NAME, rel.getBehavior().getName()));
		if(/*rel.getBehavior().isNew && */connection.isSupported(SupportedElement.SCHEMA)){
			addCombo(Relation.Behavior.L_SCHEMA, rel.getDB().getSchemaNames(), rel.getSchema().getName());
		}
		if(connection.isSupported(SupportedElement.RELATION_INHERIT)){
			addCombo(Relation.Behavior.L_INHERITS, Geometry.concat(new String[]{""}, rel.getDB().getRelationNames()), rel.getBehavior().getInheritParentName());
		}
		//addTextInput("Tablespace", "");
		//addTextInput("Owner", "");
		if(connection.isSupported(SupportedElement.RELATION_STORAGE)){
			addCombo(Relation.Behavior.L_STORAGE, connection.getStorageEngines(), rel.getBehavior().getStorage());
		}
		if(connection.isSupported(SupportedElement.RELATION_COLLATION)){
			addCombo(Relation.Behavior.L_COLLATION, connection.getCollations(), rel.getBehavior().getCollation()==null ? connection.getDefaultCollation() : rel.getBehavior().getCollation());
		}
		addTextInput(Relation.Behavior.L_DESCR, rel.getDescr());
		if(connection.isSupported(SupportedElement.RELATION_OID)){
			addCheckbox(Relation.Behavior.L_HAS_OIDS, rel.getBehavior().hasOIDs());
		}else if(!rel.getBehavior().isNew() && connection.isSupported(SupportedElement.RELATION_COLLATION)){
			addEmptySlot();
		}
		if(!rel.getBehavior().isNew()){
			String[] ops = new String[]{L_TRUNCATE, L_DROP};
			boolean[] checked = new boolean[]{false, dropChecked};
			boolean[] enabled = new boolean[]{false, true};
			/*if(connection.isSupported(Connection.RELATION_VACUUM)) {
				ops = new String[]{"VACUUM", "TRUNCATE", L_DROP};
				checked = new boolean[]{false, false, dropChecked};
				enabled = new boolean[]{false, false, true};
			} else if(connection.isSupported(Connection.RELATION_REPAIR)) {
				ops = new String[]{"CHECK", "REPAIR", "ANALYZE", "TRUNCATE", L_DROP};
				checked = new boolean[]{false, false, false, false, dropChecked};
				enabled = new boolean[]{false, false, false, false, true};
			}*/
			addCheckboxes(Relation.Behavior.L_OPERATIONS, ops, checked, enabled);
		}else if(!connection.isSupported(SupportedElement.RELATION_COLLATION)){
			addEmptySlot();
		}
		addSQLInput(rel.getQueryCreate(connection));
		setNextButton(BTN_SAVE_TEXT, false, SAVE_IN_MODEL);
	}
}
