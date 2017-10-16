package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.components.UIConstants;
import com.databazoo.components.textInput.FormattedClickableTextField;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.model.Package;
import com.databazoo.devmodeler.tools.NameGenerator;
import com.databazoo.devmodeler.tools.formatter.FormatterSQL;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

abstract class RelationWizardPagesPackage extends RelationWizardPagesFunction {

	void loadNewPackagePage1(){
		Dbg.toFile();
		Package newPack = new Package(rel.getSchema(), rel.getSchema().getSchemaPrefix() + NameGenerator.createPackageName(rel.getSchema()),
				connection.getPresetPackageDefinition(),
				connection.getPresetPackageBody(),
				"");
		newPack.getBehavior().setNew();
		loadPackagePage1(newPack);
		if(RelationWizardConfig.isElementLocationSet()){
			newPack.setLocation(RelationWizardConfig.consumeLocation());
		}
	}

	void loadPackagePage1(Package pack) {
		editableElement = pack;
		editableElement.getBehavior().prepareForEdit();
		Dbg.toFile(editableElement.getFullName());
		resetContent();

		focus(addTextInput(Package.Behavior.L_NAME, pack.getBehavior().getName()));

		if(connection.isSupported(SupportedElement.SCHEMA)){
			addCombo(Package.Behavior.L_SCHEMA, pack.getDB().getSchemaNames(), pack.getSchema().getName());
		}

		//addTextInput(Package.Behavior.L_DESCR, pack.getBehavior().descr, SPAN);
		if(!pack.isNew()){
			addEmptySlot();
			addCheckboxes(Package.Behavior.L_OPERATIONS, new String[]{L_DROP}, new boolean[]{dropChecked}, new boolean[]{true});
		}

		bodyInputTitle = Package.Behavior.L_DEFINITION;
		bodyInput = new FormattedClickableTextField(pack.getDB().getProject(), pack.getSrc(), new FormatterSQL());
		bodyInput.addKeyListener(new KeyAdapter(){
			@Override public void keyTyped(KeyEvent ke) {
				Schedule.reInvokeInWorker(Schedule.Named.MIG_WIZARD_INPUT_NOTIFICATION, UIConstants.TYPE_TIMEOUT, () -> {
                    editableElement.getBehavior().getValuesForEdit().notifyChange(Package.Behavior.L_DEFINITION, bodyInput.getText());
                    checkSQLChanges();
                });
			}
		});

		bodyInput.setAutocomplete(frame, connection);
		bodyInputScroll = new JScrollPane(bodyInput);
		bodyInputScroll.getVerticalScrollBar().setUnitIncrement(Settings.getInt(Settings.L_FONT_MONO_SIZE)*2);

		bodyInputTitle2 = Package.Behavior.L_BODY;
		bodyInput2 = new FormattedClickableTextField(pack.getDB().getProject(), pack.getBody(), new FormatterSQL());
		bodyInput2.addKeyListener(new KeyAdapter(){
			@Override public void keyTyped(KeyEvent ke) {
				Schedule.reInvokeInWorker(Schedule.Named.MIG_WIZARD_INPUT_NOTIFICATION, UIConstants.TYPE_TIMEOUT, () -> {
                    editableElement.getBehavior().getValuesForEdit().notifyChange(Package.Behavior.L_BODY, bodyInput2.getText());
                    checkSQLChanges();
                });
			}
		});
		bodyInput2.setAutocomplete(frame, connection);
		bodyInputScroll2 = new JScrollPane(bodyInput2);
		bodyInputScroll2.getVerticalScrollBar().setUnitIncrement(Settings.getInt(Settings.L_FONT_MONO_SIZE)*2);

		//guidedPanel.add(bodyInputScroll, BorderLayout.CENTER);
		//checkComponentsSize();
		addSQLInput(pack.getQueryCreate(connection));
		setNextButton(BTN_SAVE_TEXT, false, SAVE_IN_MODEL);
	}
}
