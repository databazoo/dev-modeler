package com.databazoo.devmodeler.wizards.relation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import com.databazoo.components.UIConstants;
import com.databazoo.components.combo.IconableComboBox;
import com.databazoo.components.textInput.FormattedClickableTextField;
import com.databazoo.components.textInput.NextFieldObserver;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.tools.NameGenerator;
import com.databazoo.devmodeler.tools.formatter.FormatterSQL;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

abstract class RelationWizardPagesTrigger extends RelationWizardPagesIndex {

	void loadNewTriggerPage1(){
		Dbg.toFile();
		String funcName;
		if(!connection.isSupported(SupportedElement.TRIGGER_BODY)){
			if(Function.lastCreatedTriggerFunction == null){
				String[] funcs = rel.getSchema().getTriggerFunctionNames();
				if(funcs.length == 0) {
					String[] funcNames = rel.getDB().getTriggerFunctionNames();
					funcName = funcNames.length > 0 ? funcNames[0] : "";
				}else{
					funcName = funcs[0];
				}
			}else{
				funcName = Function.lastCreatedTriggerFunction.getFullName();
			}
		}else{
			funcName = connection.getPresetTriggerBody();
		}

		Trigger trig = new Trigger(rel.getDB(), rel.getSchema().getSchemaPrefix() + NameGenerator.createTriggerName(rel), funcName, "", "", true, "");
		trig.setRelFuncByNameDef(rel.getDB(), rel.getFullName(), null, true);
		trig.getBehavior().setNew();
		loadTriggerPage1(trig);
	}

	void loadTriggerPage1(Trigger trigger) {
		editableElement = trigger;
		editableElement.getBehavior().prepareForEdit();
		Dbg.toFile(editableElement.getFullName());
		resetContent();

		focus(addTextInput(Trigger.Behavior.L_NAME, trigger.getBehavior().getName(), SPAN));
		//addEmptySlot();

		final IconableComboBox eventsCombo = new IconableComboBox(Trigger.TRIGGER_OPTIONS);
		eventsCombo.setSelectedItem(trigger.getBehavior().getTimingPart());
		eventsCombo.addActionListener(e -> {
            editableElement.getBehavior().getValuesForEdit().notifyChange(Trigger.Behavior.L_TRIGGER, (String)eventsCombo.getSelectedItem());
            checkSQLChanges();
        });
		NextFieldObserver.get(this).registerObserver(eventsCombo);

		boolean[] vals = trigger.getBehavior().getEventsPart();
		final JPanel cont = new JPanel();
		for(int i=0; i<Trigger.EVENT_OPTIONS.length; i++){
			JCheckBox cbComponent = new JCheckBox(Trigger.EVENT_OPTIONS[i], vals[i]);
			cbComponent.addActionListener(ae -> {
                Component[] comps = cont.getComponents();
                boolean[] cbVals = new boolean[comps.length];
                for(int i1 = 0; i1 <comps.length; i1++){
                    if(comps[i1] instanceof JCheckBox){
                        cbVals[i1] = ((JCheckBox) comps[i1]).isSelected();
                    }
                }
                editableElement.getBehavior().getValuesForEdit().notifyChange(Trigger.Behavior.L_EVENT, cbVals);
                checkSQLChanges();
            });
			cont.add(cbComponent);
			NextFieldObserver.get(this).registerObserver(cbComponent);
		}

		/*JSplitPane panel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, eventsCombo, cont);
		panel.setDividerLocation(150);
		panel.setDividerSize(0);*/

		addPanel(Trigger.Behavior.L_TRIGGER, eventsCombo);
		addPanel(cont, SPAN2_CENTER);

		if(connection.isSupported(SupportedElement.TRIGGER_ATTRIBUTES)){
			addTextInput(Trigger.Behavior.L_COLUMNS, trigger.getBehavior().getAttrs());
			addTextInput(Trigger.Behavior.L_WHEN, trigger.getBehavior().getWhen());
		}

		if(connection.isSupported(SupportedElement.TRIGGER_STATEMENT)){
			addCombo(Trigger.Behavior.L_FOR_EACH, Trigger.STATEMENT_OPTIONS, trigger.getBehavior().getRowType());
		}
		if(!connection.isSupported(SupportedElement.TRIGGER_BODY)){
			addTextInput(Trigger.Behavior.L_DESCR, trigger.getBehavior().getDescr());
			addCombo(Trigger.Behavior.L_EXECUTE, ((IModelElement)trigger.getRel2()).getDB().getTriggerFunctionNames(), trigger.getRel2().getFullName());
		}
		if(!editableElement.isNew()) {
			if(connection.isSupported(SupportedElement.TRIGGER_DISABLE)){
				addCheckboxes(Trigger.Behavior.L_OPERATIONS, new String[]{L_DISABLE, L_DROP}, new boolean[]{!trigger.getBehavior().isEnabled(), dropChecked}, new boolean[]{true, true});
			}else{
				addCheckboxes(Trigger.Behavior.L_OPERATIONS, new String[]{L_DROP}, new boolean[]{dropChecked}, new boolean[]{true});
			}
			if(!connection.isSupported(SupportedElement.TRIGGER_STATEMENT)) {
				addEmptySlot();
			}
		}else if(connection.isSupported(SupportedElement.TRIGGER_STATEMENT)) {
			addEmptySlot();
		}

		if(connection.isSupported(SupportedElement.TRIGGER_BODY)){
			bodyInputTitle = Trigger.Behavior.L_BODY;
			bodyInput = new FormattedClickableTextField(trigger.getDB().getProject(), trigger.getBehavior().getDef(), new FormatterSQL());
			bodyInput.addKeyListener(new KeyAdapter(){
				@Override public void keyTyped(KeyEvent ke) {
					Schedule.reInvokeInWorker(Schedule.Named.MIG_WIZARD_INPUT_NOTIFICATION, UIConstants.TYPE_TIMEOUT, () -> {
                        editableElement.getBehavior().getValuesForEdit().notifyChange(Trigger.Behavior.L_BODY, bodyInput.getText());
                        checkSQLChanges();
                    });
				}
			});
			bodyInput.setAutocomplete(frame, connection);
			bodyInputScroll = new JScrollPane(bodyInput);
			bodyInputScroll.getVerticalScrollBar().setUnitIncrement(Settings.getInt(Settings.L_FONT_MONO_SIZE)*2);
		}

		addSQLInput(trigger.getQueryCreate(connection));
		setNextButton(BTN_SAVE_TEXT, false, SAVE_IN_MODEL);
	}
}
