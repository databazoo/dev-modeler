package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.components.textInput.FormattedClickableTextField;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.tools.NameGenerator;
import com.databazoo.devmodeler.tools.formatter.FormatterSQL;
import com.databazoo.tools.Dbg;

abstract class RelationWizardPagesIndex extends RelationWizardPagesConstraint {

	void loadNewIndexPage1(){
		Dbg.toFile();
		String name = NameGenerator.createIndexName(rel);
		Index ind = new Index(rel,
				rel.getSchema().getSchemaPrefix() + name,
				rel.getAttributes().get(0).getName(),
				"",
				connection.getAccessMethods()[0],
				new String[0],
				false,
				false,
				false,
				""
		);
		ind.getBehavior().setNew();
		loadIndexPage1(ind);
	}

	void loadIndexPage1(Index index) {
		editableElement = index;
		editableElement.getBehavior().prepareForEdit();
		Dbg.toFile(editableElement.getFullName());
		resetContent();

		focus(addTextInput(Index.Behavior.L_NAME, index.getBehavior().getName()));
		//addTextInput("Tablespace", "");
		addCombo(Index.Behavior.L_METHOD, connection.getAccessMethods(), index.getBehavior().getAccessMethod());

		FormattedClickableTextField input = addTextInput(Index.Behavior.L_COLUMNS, index.getBehavior().getDef());
		addCheckbox(Index.Behavior.L_UNIQUE, index.getBehavior().isUnique());
		//addCheckbox("Clustered", false);
		input.setFormatter(new FormatterSQL());
		input.setAutocomplete(frame, connection);
		if(connection.isSupported(SupportedElement.INDEX_CONDITIONAL)){
			input = addTextInput(Index.Behavior.L_CONDITION, index.getBehavior().getWhere());
			input.setFormatter(new FormatterSQL());
			input.setAutocomplete(frame, connection);
		}
		if(!index.isNew()){
			if(connection.isSupported(SupportedElement.INDEX_COMMENT)){
				addTextInput(Index.Behavior.L_DESCR, index.getBehavior().getDescr());
				if(connection.isSupported(SupportedElement.INDEX_CONDITIONAL)){
					addEmptySlot();
				}
			}
			addCheckboxes(Index.Behavior.L_OPERATIONS, new String[]{L_REINDEX, L_DROP}, new boolean[]{false, dropChecked}, new boolean[]{false, true}, "wrap");
		}else{
			if(connection.isSupported(SupportedElement.INDEX_COMMENT)){
				addTextInput(Index.Behavior.L_DESCR, index.getBehavior().getDescr(), "wrap");
			}
		}
		addSQLInput(index.getQueryCreate(connection));
		setNextButton(BTN_SAVE_TEXT, false, SAVE_IN_MODEL);
	}

}
