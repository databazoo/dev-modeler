package com.databazoo.devmodeler.wizards.relation;

import javax.swing.*;

import com.databazoo.components.combo.IconableComboBox;
import com.databazoo.components.textInput.FormattedClickableTextField;
import com.databazoo.components.textInput.UndoableTextField;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Sequence;
import com.databazoo.devmodeler.model.View;
import com.databazoo.devmodeler.wizards.SQLEnabledWizard;

abstract class RelationWizardPages extends SQLEnabledWizard {

	static final String L_DROP = "DROP";
	static final String L_TRUNCATE = "TRUNCATE";
	//static final String L_ANALYZE = "ANALYZE";
	static final String L_REINDEX = "REINDEX";
	static final String L_DISABLE = "DISABLE";
	static final String L_CYCLE = "Cycle";

	static final int SAVE_IN_MODEL = 21;
	static final int SAVE_IN_MODEL_AND_DB = 22;
	//static final int MORE_INFO_TABLE = 24;
	private static final int MORE_INFO_ATTRIBUTE = 25;
	private static final int MORE_INFO_CONSTRAINT = 26;
	private static final int MORE_INFO_INDEX = 27;
	private static final int MORE_INFO_TRIGGER = 28;
	//static final int MORE_INFO_FUNCTION = 29;
	static final String BTN_SAVE_TEXT = "Save in Model only";

	protected boolean dropChecked = false;
	protected IModelElement editableElement;

	boolean addUniqueIndex = false;
	boolean canLocalColumnChange = false;

	String bodyInputTitle, bodyInputTitle2;
	FormattedClickableTextField newColumnNameInput, bodyInput, bodyInput2;
	JScrollPane bodyInputScroll, bodyInputScroll2;
	IconableComboBox localAttrCombo, remoteAttrCombo;
	IconableComboBox remoteRelCombo, collCombo;
	UndoableTextField uniqueIndexNameInput;

	protected Schema schema;
	protected Relation rel;
	protected Function func;
	protected com.databazoo.devmodeler.model.Package pack;
	protected View view;
	protected Sequence sequence;

	void loadAttributesIntro(){
		resetContent();
		addTitle("Table columns (attributes)");
		addText("Attributes are a crucial part of database design.<br><br>Be sure to choose right data types for attributes to achieve efficient data storage.", SPAN);
		setNextButton("More info", true, MORE_INFO_ATTRIBUTE);
	}

	void loadConstraintsIntro(){
		resetContent();
		addTitle("Foreign keys (constraints)");
		addText("Foreign keys protect your entities. Whenever you change your data you can be sure your data integrity will not be damaged.<br><br>Using <b>cascaded</b> constraints makes it easy to remove your data completely. Removing a parent entity you can also automatically remove all it's children.<br><br><b>Restrict</b>/<b>No action</b> constraints remind you not to remove used rows from enumerated lists (i.e. types, states, currencies, languages, etc.).<br><br><b>Set NULL</b> constraints help you maintain 0:N relations.", SPAN);
		setNextButton("More info", true, MORE_INFO_CONSTRAINT);
	}

	void loadIndexesIntro(){
		resetContent();
		addTitle("Indexes");
		addText("Index is a primary tool for database optimization. Preparing indexes for most frequently used SQL queries is one of the best ways to speed up a database.<br><br><b>Partial indexes</b> are very useful when you often run similar queries (i.e. reading a queue) and can find a distinctive property (i.e. queue row has not been processed yet). Partial indexes are very \"narrow\" so scanning such indexes is usually very efficient.<br><br>Do not forget that indexes speed up SELECTs but <b>slow down</b> INSERTs or UPDATEs. Remove any unused indexes to achieve best results.", SPAN);
		setNextButton("More info", true, MORE_INFO_INDEX);
	}

	void loadTriggersIntro(){
		resetContent();
		addTitle("Triggers");
		addText("Triggers run <b>stored procedures</b> when database changes occur.<br><br>By adding a trigger you can prevent rows from being changed, make additional changes to the row or even execute SQL queries on other tables.<br><br>Note that triggers <b>slow down</b> operations on table, so be careful when using many triggers.", SPAN);
		setNextButton("More info", true, MORE_INFO_TRIGGER);
	}

	protected abstract boolean checkSQLChanges();
	protected abstract void addSQLInput(String text);
	protected abstract void focus(UndoableTextField lastTextField);
}
