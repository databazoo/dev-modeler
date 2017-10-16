package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.tools.NameGenerator;
import com.databazoo.tools.Dbg;

abstract class RelationWizardPagesSchema extends RelationWizardPages {

	void loadNewSchemaPage1(){
		Dbg.toFile();
		Schema newSchema = new Schema(rel.getDB(), NameGenerator.createSchemaName(rel.getDB()), "");
		newSchema.getBehavior().setNew();
		loadSchemaPage1(newSchema);
		if(RelationWizardConfig.isElementLocationSet()){
			newSchema.setLocation(RelationWizardConfig.consumeLocation());
		}
	}

	void loadSchemaPage1(Schema schema) {
		editableElement = schema;
		editableElement.getBehavior().prepareForEdit();
		Dbg.toFile(editableElement.getFullName());
		resetContent();

		if(connection.isSupported(SupportedElement.SCHEMA_CREATE)){
			focus(addTextInput(Schema.Behavior.L_NAME, schema.getBehavior().getName()));
			/*if(schema.getBehavior().isNew){
				addCombo(Schema.Behavior.L_DATABASE, project.getDatabaseNames(), schema.getDB().getName());
			}*/
			//addTextInput("Tablespace", "");
			//addTextInput("Owner", "");
			addTextInput(Schema.Behavior.L_DESCR, schema.getDescr());
		}
		if(!schema.getBehavior().isNew()){
			addEmptySlot();
			addCheckboxes(Schema.Behavior.L_OPERATIONS, new String[]{L_DROP}, new boolean[]{dropChecked}, new boolean[]{true});
		}
		addSQLInput(schema.getQueryCreate(connection));
		setNextButton(BTN_SAVE_TEXT, false, SAVE_IN_MODEL);
	}
}
