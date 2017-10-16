package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.components.UIConstants;
import com.databazoo.components.combo.IconableComboBox;
import com.databazoo.components.textInput.FormattedClickableTextField;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.model.CheckConstraint;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.tools.Geometry;
import com.databazoo.devmodeler.tools.NameGenerator;
import com.databazoo.devmodeler.tools.formatter.FormatterSQL;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;
import plugins.api.IModelTable;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

abstract class RelationWizardPagesConstraint extends RelationWizardPagesAttribute {


    void loadNewPrimaryKeyPage1() {
        Dbg.toFile();
        String name = connection.isSupported(SupportedElement.PRIMARY_KEY_NAMED) ? NameGenerator.createPrimaryKeyName(rel) : "PRIMARY";
        Index ind = new Index(rel,
                rel.getSchema().getSchemaPrefix() + name,
                "PRIMARY KEY (" + rel.getAttributes().get(0).getName() + ")",
                "",
                "btree",
                new String[0],
                true,
                true,
                true,
                ""
        );
        ind.getBehavior().setNew();
        loadConstraintPage1(ind);
    }

    void loadNewForeignKeyPage1() {
        Dbg.toFile();
        String name = NameGenerator.createForeignKeyName(rel.getAttributes().get(0));
        Constraint con = new Constraint(rel.getDB(), rel.getSchema().getSchemaPrefix() + name);
        con.setRelsAttrsByName(rel.getDB(),
                rel.getFullName(),
                rel.getSchema().getRelations().get(0).getFullName(),
                rel.getAttributes().get(0).getName(),
                rel.getSchema().getRelations().get(0).getAttributes().get(0).getName(),
                true);
        con.getBehavior().setNew();
        loadConstraintPage1(con);
    }

    void loadNewUniqueConstraintPage1() {
        Dbg.toFile();
        String name = NameGenerator.createUniqueConstraintName(rel);
        Index ind = new Index(rel,
                rel.getSchema().getSchemaPrefix() + name,
                "UNIQUE (" + rel.getAttributes().get(0).getName() + ")",
                "",
                "btree",
                new String[0],
                true,
                false,
                true,
                ""
        );
        ind.getBehavior().setNew();
        loadConstraintPage1(ind);
    }

    void loadNewCheckConstraintPage1() {
        Dbg.toFile();
        String name = NameGenerator.createCheckConstraintName(rel);
        CheckConstraint con = new CheckConstraint(rel.getDB(),
                rel.getSchema().getSchemaPrefix() + name,
                rel.getPKey().isEmpty() ? "" : rel.getPKey() + " > 0",
                ""
        );
        con.setRelByName(rel.getDB(), rel.getFullName(), true);
        con.getBehavior().setNew();
        loadConstraintPage1(con);
    }

    void loadConstraintPage1(IModelElement con) {
        editableElement = con;
        editableElement.getBehavior().prepareForEdit();
        Dbg.toFile(editableElement.getFullName());
        resetContent();

        if (editableElement instanceof Index && ((Index) editableElement).getBehavior().isPrimary() && !connection.isSupported(SupportedElement.PRIMARY_KEY_NAMED)) {
            ((Index) editableElement).getBehavior().setName("PRIMARY");
        } else {
            focus(addTextInput("Name", editableElement.getName()));
        }
        //addTextInput("Tablespace", "");

        boolean checkSQLNow = false;

        if (editableElement instanceof CheckConstraint) {
            addCheckConstraintInputs();

        } else if (editableElement instanceof Constraint) {
            addForeignKeyInputs();

        } else {
            checkSQLNow = addUniqueInputs();
        }
        addSQLInput(editableElement.getQueryCreate(connection));
        setNextButton(BTN_SAVE_TEXT, false, SAVE_IN_MODEL);
        if (checkSQLNow) {
            checkSQLChanges();
        }
    }

    private boolean addUniqueInputs() {
        Index index = (Index) editableElement;

        // Primary key
        if (index.getBehavior().isPrimary()) {
            if (connection.isSupported(SupportedElement.PRIMARY_KEY_NAMED)) {
                addTextInput(Index.Behavior.L_DESCR, index.getBehavior().getDescr());
            }

            FormattedClickableTextField input = addTextInput(Index.Behavior.L_COLUMNS, index.getBehavior().getDef());
            input.setFormatter(new FormatterSQL());
            input.setAutocomplete(frame, connection);

            if (!index.isNew()) {
                addCheckboxes(Index.Behavior.L_OPERATIONS, new String[]{L_REINDEX, L_DROP}, new boolean[]{false, dropChecked}, new boolean[]{false, true});
            } else {
                addEmptySlot();
                return true;
            }

            // Unique constraint
        } else {
            addTextInput(Index.Behavior.L_DESCR, index.getBehavior().getDescr());

            FormattedClickableTextField input = addTextInput(Index.Behavior.L_COLUMNS, index.getBehavior().getDef());
            input.setFormatter(new FormatterSQL());
            input.setAutocomplete(frame, connection);

            if (!index.isNew()) {
                addCheckboxes(Constraint.Behavior.L_OPERATIONS, new String[]{L_DROP}, new boolean[]{dropChecked}, new boolean[]{true});
            } else {
                addEmptySlot();
            }
        }
        return false;
    }

    private void addForeignKeyInputs() {
        Constraint c = (Constraint) editableElement;
        if (addUniqueIndex) {
            uniqueIndexNameInput = addTextInput(Constraint.Behavior.L_UX_NAME, c.getName().replaceAll("fkey_(.*)", "unique_$1").replaceAll("fk_(.*)", "ux_$1"));
        } else {
            addEmptySlot();
        }

        addEmptyLine();

        if (editableElement.isNew() && canLocalColumnChange) {
            final IconableComboBox localRelCombo = addCombo(Constraint.Behavior.L_LOC_REL, ((IModelElement) c.getRel1()).getDB().getRelationNames(), c.getRel1().getFullName());
            localRelCombo.addActionListener(ae -> {
                String relName = localRelCombo.getSelectedItem().toString();
                String[] attNames = rel.getDB().getRelationByFullName(relName).getAttributeNames();
                localAttrCombo.setModel(new DefaultComboBoxModel<>(RelationWizardPagesConstraint.this instanceof MNRelationWizard ? Geometry.concat(new String[]{"Create new column"}, attNames) : attNames));

                editableElement.getBehavior().getValuesForEdit().notifyChange(Constraint.Behavior.L_LOC_REL, relName);
                editableElement.getBehavior().getValuesForEdit().notifyChange(Constraint.Behavior.L_LOC_ATTR, localAttrCombo.getSelectedItem().toString());
                checkSQLChanges();
                checkNewColumnNameInputVisibility();
            });
        } else {
            addPanel(Constraint.Behavior.L_LOC_REL, new JLabel(" " + c.getRel1().getFullName()));
        }
        remoteRelCombo = addCombo(Constraint.Behavior.L_REM_REL, ((IModelElement) c.getRel2()).getDB().getRelationNames(), c.getRel2().getFullName());
        remoteRelCombo.addActionListener(ae -> {
            String relName = (String) remoteRelCombo.getSelectedItem();

            remoteAttrCombo.setModel(new DefaultComboBoxModel<>(rel.getDB().getRelationByFullName(relName).getAttributeNames()));

            editableElement.getBehavior().getValuesForEdit().notifyChange(Constraint.Behavior.L_REM_REL, relName);
            editableElement.getBehavior().getValuesForEdit().notifyChange(Constraint.Behavior.L_REM_ATTR, remoteAttrCombo.getSelectedItem().toString());
            checkSQLChanges();
        });
        String[] attNames = ((IModelTable) c.getRel1()).getAttributeNames();
        if (RelationWizardPagesConstraint.this instanceof MNRelationWizard) {
            localAttrCombo = addCombo(Constraint.Behavior.L_LOC_ATTR, Geometry.concat(new String[]{"Create new column"}, attNames), c.getAttr1().getName());
            localAttrCombo.addActionListener(ae -> {
                checkNewColumnNameInputVisibility();
                checkNewColumnDataType();
            });
        } else {
            localAttrCombo = addCombo(Constraint.Behavior.L_LOC_ATTR, attNames, c.getAttr1().getName());
        }
        remoteAttrCombo = addCombo(Constraint.Behavior.L_REM_ATTR, ((IModelTable) c.getRel2()).getAttributeNames(), c.getAttr2().getName());
        remoteAttrCombo.addActionListener(ae -> checkNewColumnDataType());

        newColumnNameInput = addTextInput("", remoteAttrCombo.getSelectedItem().toString());
        newColumnNameInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                Schedule.reInvokeInWorker(Schedule.Named.RELATION_WIZARD_INPUT_LISTENER, UIConstants.TYPE_TIMEOUT,
                        () -> editableElement.getBehavior().getValuesForEdit().notifyChange(Constraint.Behavior.L_NEW_COL_NAME, newColumnNameInput.getText()));
            }
        });
        checkNewColumnNameInputVisibility();
        addEmptySlot();

        final IconableComboBox updateCombo;
        if (connection.isSupported(SupportedElement.FOREIGN_KEY_ON_UPDATE)) {
            updateCombo = addCombo(Constraint.Behavior.L_ON_UPDATE, connection.getConstraintUpdateActions(), c.getBehavior().getOnUpdate());

        } else {
            updateCombo = null;
            if (connection.isSupported(SupportedElement.FOREIGN_KEY_COMMENT)) {
                addEmptySlot();
            }
        }

        if (connection.isSupported(SupportedElement.FOREIGN_KEY_COMMENT)) {
            addTextInput(Constraint.Behavior.L_DESCR, c.getBehavior().getDescr());
        } else if (connection.isSupported(SupportedElement.FOREIGN_KEY_ON_UPDATE)) {
            addEmptySlot();
        }
        final IconableComboBox deleteCombo = addCombo(Constraint.Behavior.L_ON_DELETE, connection.getConstraintDeleteActions(), c.getBehavior().getOnDelete());
        Schedule.inWorker(Schedule.CLICK_DELAY, new Runnable() {
            @Override
            public void run() {
                if (updateCombo != null) {
                    notifyChange(Constraint.Behavior.L_ON_UPDATE, updateCombo.getSelectedItem().toString());
                }
                notifyChange(Constraint.Behavior.L_ON_DELETE, deleteCombo.getSelectedItem().toString());
            }
        });
        if (!c.isNew()) {
            addCheckboxes(Constraint.Behavior.L_OPERATIONS, new String[]{L_DROP}, new boolean[]{dropChecked}, new boolean[]{true});
        } else {
            addCheckboxes(Constraint.Behavior.L_REM_NULL_TITLE, new String[]{Constraint.Behavior.L_REM_NULL}, new boolean[]{c.getAttr1().getBehavior().isAttNull()}, new boolean[]{true});
        }
    }

    private void addCheckConstraintInputs() {
        CheckConstraint c = (CheckConstraint) editableElement;

        addTextInput(Constraint.Behavior.L_DESCR, c.getBehavior().getDescr());
        addTextInput(CheckConstraint.Behavior.L_DEFINITION, c.getBehavior().getDef()).
                setAutocomplete(frame, connection);
        if (!c.isNew()) {
            addCheckboxes(Constraint.Behavior.L_OPERATIONS, new String[]{L_DROP}, new boolean[]{dropChecked}, new boolean[]{true});
        } else {
            addEmptySlot();
        }
    }

    private void checkNewColumnNameInputVisibility() {
        boolean newValue = localAttrCombo.getSelectedItem().toString().equals("Create new column");
        if (newValue != newColumnNameInput.isVisible()) {
            newColumnNameInput.setVisible(newValue);
            newColumnNameInput.setText(remoteAttrCombo.getSelectedItem().toString());
            editableElement.getBehavior().getValuesForEdit().notifyChange(Constraint.Behavior.L_NEW_COL_NAME, newColumnNameInput.getText());
        }
    }

    private void checkNewColumnDataType() {
        String value = rel.getDB().
                getRelationByFullName(remoteRelCombo.getSelectedItem().toString()).
                getAttributeByName(remoteAttrCombo.getSelectedItem().toString()).
                getFullType();
        switch (value) {
            case "serial":
            case "serial4":
                value = "integer";
                break;
            case "bigserial":
            case "serial8":
                value = "bigint";
                break;
        }
        Dbg.info("Setting datatype to " + value);
        editableElement.getBehavior().getValuesForEdit().notifyChange(Constraint.Behavior.L_NEW_COL_TYPE, value);
    }
}
