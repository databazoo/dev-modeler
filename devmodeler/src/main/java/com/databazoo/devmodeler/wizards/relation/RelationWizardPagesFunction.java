package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.components.UIConstants;
import com.databazoo.components.combo.IconableComboBox;
import com.databazoo.components.textInput.FormattedClickableTextField;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.gui.DBTree;
import com.databazoo.devmodeler.model.DataTypes;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.tools.NameGenerator;
import com.databazoo.devmodeler.tools.formatter.FormatterSQL;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

abstract class RelationWizardPagesFunction extends RelationWizardPagesTrigger {

	void loadNewTriggerFunctionPage1(){
		Dbg.toFile();
		Function function = new Function(
				rel.getSchema(),
				rel.getSchema().getSchemaPrefix() + NameGenerator.createFunctionName(rel.getSchema()),
				connection.getPresetFucntionReturn(true),
				connection.getPresetFucntionArgs(true),
				connection.getPresetFucntionSource(true),
				"plpgsql", "VOLATILE", false, 100, 0, "");
		function.getBehavior().setNew();
		loadFunctionPage1(function);
	}

	void loadNewFunctionPage1(){
		Dbg.toFile();
		Function function = new Function(
				rel.getSchema(),
				rel.getSchema().getSchemaPrefix() + NameGenerator.createFunctionName(rel.getSchema()),
				connection.getPresetFucntionReturn(false),
				connection.getPresetFucntionArgs(false),
				connection.getPresetFucntionSource(false),
				"plpgsql", "VOLATILE", false, 100, 0, "");
		function.getBehavior().setNew();
		loadFunctionPage1(function);
		if(RelationWizardConfig.isElementLocationSet()){
			function.setLocation(RelationWizardConfig.consumeLocation());
		}
	}

	void loadFunctionPage1(Function func) {
		editableElement = func;
		editableElement.getBehavior().prepareForEdit();
		Dbg.toFile(editableElement.getFullName());
		resetContent();

		focus(addTextInput(Function.Behavior.L_NAME, func.getBehavior().getName()));

		if(connection.isSupported(SupportedElement.SCHEMA)){
			addCombo(Function.Behavior.L_SCHEMA, func.getDB().getSchemaNames(), func.getSchema().getName());
		}

		addTextInput(Function.Behavior.L_ARGUMENTS, func.getArgs(), SPAN).
				setAutocomplete(frame, DBTree.instance.getElementNames(), null, DataTypes.getTypeNames(connection));

		addTextInput(Function.Behavior.L_RETURNS, func.getRetType(), SPAN).
				setAutocomplete(frame, DBTree.instance.getElementNames(), null, DataTypes.getTypeNames(connection));

		if(!connection.isSupported(SupportedElement.FUNCTION_SQL_ONLY)) {
			final IconableComboBox volatilityCombo = new IconableComboBox(Function.getVolatilityOptions());
			volatilityCombo.setSelectedItem(func.getVolatility());
			volatilityCombo.addActionListener(e -> {
                editableElement.getBehavior().getValuesForEdit().notifyChange(Function.Behavior.L_VOLATILITY, (String) volatilityCombo.getSelectedItem());
                checkSQLChanges();
            });

			final JCheckBox strictCB = new JCheckBox(Function.Behavior.L_STRICT, false);
			strictCB.setEnabled(false);
			strictCB.addActionListener(ae -> {
                editableElement.getBehavior().getValuesForEdit().notifyChange(Function.Behavior.L_STRICT, strictCB.isSelected());
                checkSQLChanges();
            });

			final JCheckBox secDefCB = new JCheckBox(Function.Behavior.L_SEC_DEFINER, func.isSecurityDefiner());
			secDefCB.addActionListener(ae -> {
                editableElement.getBehavior().getValuesForEdit().notifyChange(Function.Behavior.L_SEC_DEFINER, secDefCB.isSelected());
                checkSQLChanges();
            });

			final JTextField costTXT = new JTextField(Integer.toString(func.getCost()), 4);
			costTXT.addKeyListener(new KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent ke) {
					Schedule.reInvokeInWorker(Schedule.Named.MIG_WIZARD_INPUT_NOTIFICATION, UIConstants.TYPE_TIMEOUT, () -> {
                        editableElement.getBehavior().getValuesForEdit().notifyChange(Function.Behavior.L_COST, costTXT.getText());
                        checkSQLChanges();
                    });
				}
			});

			final JTextField rowsTXT = new JTextField(Integer.toString(func.getRows()), 4);
			rowsTXT.addKeyListener(new KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent ke) {
					Schedule.reInvokeInWorker(Schedule.Named.MIG_WIZARD_INPUT_NOTIFICATION, UIConstants.TYPE_TIMEOUT, () -> {
                        editableElement.getBehavior().getValuesForEdit().notifyChange(Function.Behavior.L_ROWS, rowsTXT.getText());
                        checkSQLChanges();
                    });
				}
			});

			JPanel behaviorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			behaviorPanel.add(volatilityCombo);
			behaviorPanel.add(new JLabel("  "));
			behaviorPanel.add(strictCB);
			behaviorPanel.add(new JLabel("  "));
			behaviorPanel.add(secDefCB);

			JPanel costRowsPanel = new JPanel(new GridLayout(1, 0, 0, 0));
			costRowsPanel.add(costTXT);
			costRowsPanel.add(rowsTXT);

			addPanel(Function.Behavior.L_BEHAVIOR, behaviorPanel);
			addPanel(Function.Behavior.L_COST + ", " + Function.Behavior.L_ROWS, costRowsPanel);

			addCombo(Function.Behavior.L_LANGUAGE, connection.getLanguages(), func.getLang());
		}

		if(!func.isNew()){
			addCheckboxes(Function.Behavior.L_OPERATIONS, new String[]{L_DROP}, new boolean[]{dropChecked}, new boolean[]{true});
		}
		addTextInput(Function.Behavior.L_DESCR, func.getBehavior().getDescr(), SPAN);

		bodyInputTitle = Function.Behavior.L_BODY;
		bodyInput = new FormattedClickableTextField(func.getDB().getProject(), func.getSrc(), new FormatterSQL());
		bodyInput.addKeyListener(new KeyAdapter(){
			@Override public void keyTyped(KeyEvent ke) {
				Schedule.reInvokeInWorker(Schedule.Named.MIG_WIZARD_INPUT_NOTIFICATION, UIConstants.TYPE_TIMEOUT, () -> {
                    editableElement.getBehavior().getValuesForEdit().notifyChange(Function.Behavior.L_BODY, bodyInput.getText());
                    checkSQLChanges();
                });
			}
		});
		bodyInput.setAutocomplete(frame, connection);
		bodyInputScroll = new JScrollPane(bodyInput);
		bodyInputScroll.getVerticalScrollBar().setUnitIncrement(Settings.getInt(Settings.L_FONT_MONO_SIZE)*2);

		//guidedPanel.add(bodyInputScroll, BorderLayout.CENTER);
		//checkComponentsSize();
		addSQLInput(func.getQueryCreate(connection));
		setNextButton(BTN_SAVE_TEXT, false, SAVE_IN_MODEL);
	}
}
