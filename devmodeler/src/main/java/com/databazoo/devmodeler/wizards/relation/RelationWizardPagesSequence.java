package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.model.Sequence;
import com.databazoo.devmodeler.tools.NameGenerator;
import com.databazoo.tools.Dbg;

abstract class RelationWizardPagesSequence extends RelationWizardPagesView {

	void loadNewSequencePage1(){
		Dbg.toFile();
		Sequence seqLoc = new Sequence(rel.getSchema(), rel.getSchema().getSchemaPrefix() + NameGenerator.createSequenceName(rel.getSchema()), new String[0], "1", "1", "9223372036854775807", "1", true, "");
		seqLoc.getBehavior().setNew();
		loadSequencePage1(seqLoc);
		if(RelationWizardConfig.isElementLocationSet()){
			seqLoc.setLocation(RelationWizardConfig.consumeLocation());
		}
	}

	void loadSequencePage1(Sequence seq) {
		editableElement = seq;
		editableElement.getBehavior().prepareForEdit();
		Dbg.toFile(editableElement.getFullName());
		resetContent();

		focus(addTextInput(Sequence.Behavior.L_NAME, seq.getBehavior().getName()));

		if(connection.isSupported(SupportedElement.SCHEMA)){
			addCombo(Sequence.Behavior.L_SCHEMA, seq.getDB().getSchemaNames(), seq.getSchema().getName());
		}
		//addTextInput("Owner", "");

		addTextInput(Sequence.Behavior.L_START, seq.getBehavior().getCurrent());
		addTextInput(Sequence.Behavior.L_INCREMENT, seq.getBehavior().getIncrement());
		addTextInput(Sequence.Behavior.L_MIN, seq.getBehavior().getMin());
		addTextInput(Sequence.Behavior.L_DESCR, seq.getBehavior().getDescr());
		addTextInput(Sequence.Behavior.L_MAX, seq.getBehavior().getMax());

		if(!seq.isNew()){
			addCheckboxes(Sequence.Behavior.L_OPERATIONS, new String[]{L_CYCLE, L_DROP}, new boolean[]{seq.getBehavior().isCycle(), dropChecked}, new boolean[]{true, true});
		}else{
			addEmptySlot();
		}

		addSQLInput(seq.getQueryCreate(connection));
		setNextButton(BTN_SAVE_TEXT, false, SAVE_IN_MODEL);
	}
}
