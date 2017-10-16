package com.databazoo.devmodeler.wizards.relation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Objects;

import com.databazoo.components.UIConstants;
import com.databazoo.components.combo.ComboSeparatorsRenderer;
import com.databazoo.components.combo.IconableComboBox;
import com.databazoo.components.containers.HorizontalContainer;
import com.databazoo.components.textInput.NextFieldObserver;
import com.databazoo.components.textInput.UndoableTextField;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.DataTypes;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.tools.Geometry;
import com.databazoo.devmodeler.tools.NameGenerator;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

abstract class RelationWizardPagesAttribute extends RelationWizardPagesTable {

	void loadNewAttributePage1(){
		Dbg.toFile();
		Attribute attr = new Attribute(rel, NameGenerator.createAttributeName(rel), connection.getDefaultDataType(), false, rel.getAttributes().isEmpty() ? 1 : rel.getAttributes().get(rel.getAttributes().size()-1).getAttNum()+1, "", null, "");
		if(connection.isSupported(SupportedElement.ATTRIBUTE_COLLATION)){
			if(connection.isSupported(SupportedElement.RELATION_COLLATION)){
				attr.getBehavior().setCollation(rel.getBehavior().getCollation());
			}else{
				attr.getBehavior().setCollation(connection.getDefaultCollation());
			}
		}
		attr.getBehavior().setNew();
		loadAttributePage1(attr);
	}

	void loadAttributePage1(final Attribute attr) {
		editableElement = attr;
		editableElement.getBehavior().prepareForEdit();
		Dbg.toFile(editableElement.getFullName());
		resetContent();

		focus(addTextInput(Attribute.Behavior.L_NAME, attr.getBehavior().getName()));

		final UndoableTextField typePrecision = new UndoableTextField(attr.getBehavior().getAttPrecision());
		typePrecision.disableFinder();
		typePrecision.setPreferredSize(new Dimension(80,20));
		typePrecision.setBordered(true);
		typePrecision.addKeyListener(new KeyAdapter(){
			@Override
			public void keyTyped(KeyEvent ke) {
				Schedule.reInvokeInWorker(Schedule.Named.MIG_WIZARD_INPUT_NOTIFICATION, UIConstants.TYPE_TIMEOUT, () -> {
                    editableElement.getBehavior().getValuesForEdit().notifyChange(Attribute.Behavior.L_PRECISION, typePrecision.getText());
                    checkSQLChanges();
                });
			}
		});

		final String[] usedTypes = project.usedDataTypes.keySet().toArray(new String[0]);
		for(int i=0; i<usedTypes.length; i++){
			usedTypes[i] = connection.getDataTypes().toFullType(usedTypes[i]);
		}
		Arrays.sort(usedTypes);
		final IconableComboBox typeCombo = new IconableComboBox(Geometry.concat(usedTypes, connection.getDataTypes().getVals()));
		typeCombo.setSelectedItem(connection.getDataTypes().toFullType(attr.getBehavior().getAttType()));
		typeCombo.setRenderer(new ComboSeparatorsRenderer(typeCombo.getRenderer()) {
			@Override protected boolean addSeparatorAfter(JList list, Object value, int index) {
				return index>=0 && index == usedTypes.length-1;
			}
		});
		typeCombo.addActionListener(e -> {
            String type = (String)typeCombo.getSelectedItem();
            editableElement.getBehavior().getValuesForEdit().notifyChange(Attribute.Behavior.L_DATATYPE, connection.getDataTypes().toShortType(type));

            String precisionForType = connection.getPrecisionForType(connection.getDataTypes().toShortType(type));
            if(precisionForType != null){
                typePrecision.setText(precisionForType);
                editableElement.getBehavior().getValuesForEdit().notifyChange(Attribute.Behavior.L_PRECISION, precisionForType);
            }
            checkCollationCombo(type);
            checkSQLChanges();
        });

		NextFieldObserver.get(this).registerObserver(typeCombo);
		NextFieldObserver.get(this).registerObserver(typePrecision);

		addPanel(Attribute.Behavior.L_DATATYPE, new HorizontalContainer(null, typeCombo, typePrecision));
		addTextInput(Attribute.Behavior.L_DEFAULT, attr.getBehavior().getDefaultValue()).
				setAutocomplete(frame, null, UndoableTextField.KEYWORDS_SQL, DataTypes.getTypeNames(connection));

		if(connection.isSupported(SupportedElement.RELATION_COLLATION)){
			collCombo = addCombo(Relation.Behavior.L_COLLATION, Geometry.concat(new String[]{""}, connection.getCollations()), attr.getBehavior().getCollation());
			checkCollationCombo((String)typeCombo.getSelectedItem());
		}
		if(connection.isSupported(SupportedElement.ATTRIBUTE_STORAGE)){
			addCombo(Attribute.Behavior.L_STORAGE, getStorageTypes(Objects.equals(attr.getBehavior().getStorage(), Attribute.Behavior.L_STORAGE_AUTO)), attr.getBehavior().getStorage());
		}

		if(connection.isSupported(SupportedElement.ATTRIBUTE_COLLATION) || connection.isSupported(SupportedElement.ATTRIBUTE_STORAGE)) {
			addCheckbox(Attribute.Behavior.L_NULLABLE, attr.getBehavior().isAttNull());
			addTextInput(Attribute.Behavior.L_DESCR, attr.getBehavior().getDescr());
			if(!editableElement.isNew()) {
				addEmptySlot();
				addCheckboxes(Attribute.Behavior.L_OPERATIONS, new String[]{L_DROP}, new boolean[]{dropChecked}, new boolean[]{true});
			}
		}else{
			addTextInput(Attribute.Behavior.L_DESCR, attr.getBehavior().getDescr());
			addCheckbox(Attribute.Behavior.L_NULLABLE, attr.getBehavior().isAttNull());
			if(!editableElement.isNew()) {
				addCheckboxes(Attribute.Behavior.L_OPERATIONS, new String[]{L_DROP}, new boolean[]{dropChecked}, new boolean[]{true});
			}else{
				addEmptySlot();
			}
		}
		addSQLInput(attr.getQueryCreate(connection));
		setNextButton(BTN_SAVE_TEXT, false, SAVE_IN_MODEL);
	}

	private void checkCollationCombo(String type){
		if(connection.isSupported(SupportedElement.RELATION_COLLATION)){
			if(connection.isCollatable(type)){
				if(!collCombo.isEnabled()){
					String relColl = ((Attribute)editableElement).getRel().getBehavior().getCollation();
					collCombo.setEnabled(true);
					collCombo.setSelectedItem(relColl != null ? relColl : connection.getDefaultCollation());
				}
			}else{
				collCombo.setSelectedIndex(0);
				collCombo.setEnabled(false);
			}
		}
	}

	private String[] getStorageTypes(boolean canUseAuto){
		if(canUseAuto) {
			return new String[]{
					Attribute.Behavior.L_STORAGE_AUTO,
					Attribute.Behavior.L_STORAGE_PLAIN,
					Attribute.Behavior.L_STORAGE_MAIN,
					Attribute.Behavior.L_STORAGE_EXTERNAL,
					Attribute.Behavior.L_STORAGE_EXTENDED
			};
		} else {
			return new String[]{
					Attribute.Behavior.L_STORAGE_PLAIN,
					Attribute.Behavior.L_STORAGE_MAIN,
					Attribute.Behavior.L_STORAGE_EXTERNAL,
					Attribute.Behavior.L_STORAGE_EXTENDED
			};
		}
	}
}
