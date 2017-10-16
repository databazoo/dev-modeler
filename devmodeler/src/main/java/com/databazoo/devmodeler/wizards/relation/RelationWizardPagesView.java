package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.components.UIConstants;
import com.databazoo.components.textInput.FormattedClickableTextField;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.model.View;
import com.databazoo.devmodeler.tools.NameGenerator;
import com.databazoo.devmodeler.tools.formatter.FormatterSQL;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

abstract class RelationWizardPagesView extends RelationWizardPagesPackage {

	void loadNewViewPage1(){
		Dbg.toFile();
		View viewLoc = new View(rel.getSchema(), rel.getSchema().getSchemaPrefix() + NameGenerator.createViewName(rel.getSchema()), false, "SELECT ;", "");
		viewLoc.getBehavior().setNew();
		loadViewPage1(viewLoc);
		if(RelationWizardConfig.isElementLocationSet()){
			viewLoc.setLocation(RelationWizardConfig.consumeLocation());
		}
	}

	void loadViewPage1(View view) {
		editableElement = view;
		editableElement.getBehavior().prepareForEdit();
		Dbg.toFile(editableElement.getFullName());
		resetContent();

		focus(addTextInput(View.Behavior.L_NAME, view.getBehavior().getName()));

		if(connection.isSupported(SupportedElement.SCHEMA)){
			addCombo(View.Behavior.L_SCHEMA, view.getDB().getSchemaNames(), view.getSchema().getName());
		}

		if(connection.isSupported(SupportedElement.VIEW_MATERIALIZED)){
			addCheckbox(View.Behavior.L_MATERIALIZED, view.getMaterialized());
		}

		addTextInput(View.Behavior.L_DESCR, view.getBehavior().getDescr());

		if(!view.isNew()){
			if(connection.isSupported(SupportedElement.VIEW_MATERIALIZED) == connection.isSupported(SupportedElement.SCHEMA)){
				addEmptySlot();
			}
			addCheckboxes(View.Behavior.L_OPERATIONS, new String[]{L_DROP}, new boolean[]{dropChecked}, new boolean[]{true});
		}

		bodyInputTitle = View.Behavior.L_BODY;
		bodyInput = new FormattedClickableTextField(view.getDB().getProject(), view.getSrc(), new FormatterSQL());
		bodyInput.addKeyListener(new KeyAdapter(){
			@Override public void keyTyped(KeyEvent ke) {
				Schedule.reInvokeInWorker(Schedule.Named.MIG_WIZARD_INPUT_NOTIFICATION, UIConstants.TYPE_TIMEOUT, () -> {
                    editableElement.getBehavior().getValuesForEdit().notifyChange(View.Behavior.L_BODY, bodyInput.getText());
                    checkSQLChanges();
                });
			}
		});
		bodyInput.setAutocomplete(frame, connection);
		bodyInputScroll = new JScrollPane(bodyInput);
		bodyInputScroll.getVerticalScrollBar().setUnitIncrement(Settings.getInt(Settings.L_FONT_MONO_SIZE)*2);

		//guidedPanel.add(bodyInputScroll, BorderLayout.CENTER);
		//checkComponentsSize();
		addSQLInput(view.getQueryCreate(connection));
		setNextButton(BTN_SAVE_TEXT, false, SAVE_IN_MODEL);
	}
}
