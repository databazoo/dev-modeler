
package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.components.combo.IconableComboBox;
import com.databazoo.components.elements.LineComponent;
import com.databazoo.components.textInput.UndoableTextField;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.OperationCancelException;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.tools.NameGenerator;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

/**
 * Create reference with Drag'n'Drop opens this wizard
 * @author bobus
 */
public class MNRelationWizard extends RelationWizard {

	private static final String L_11_RELATION = "1:1 relation";	// (0..1 to 0..1)
	private static final String L_1N_RELATION = "1:N relation";	// (0..1 to 0..N)
	private static final String L_MN_RELATION = "M:N relation";	// (0..N to 0..N)

	public static MNRelationWizard get(){
		return new MNRelationWizard();
	}

	private static String[] getPKeyTypes(){
		return new String[]{"Use both ref. columns as primary key", "Serial primary key + unique index", "Serial primary key only"};
	}

	private Relation rel2;
	private Attribute attr1, attr2;
	private Index uniqueIndex;

	private String rel1Name, rel2Name;
	private UndoableTextField ref1TextField;
	private UndoableTextField ref2TextField;
	private IconableComboBox pkeyTypeCombo;
	//private UndoableTextField pkNameTextField;

	private MNRelationWizard(){
		canLocalColumnChange = true;
	}

	public void drawRelation(Relation rel1, Attribute attr1, Relation rel2, Attribute attr2) {
		this.rel = rel1;
		this.attr1 = attr1;
		this.rel2 = rel2;
		this.attr2 = attr2;

		drawWindow("New relation", createTree(2), Settings.getBool(Settings.L_MAXIMIZE_WIZARDS), true);
		addKeyListeners();
	}

	@Override
	protected DefaultMutableTreeNode getUpdatedTreeModel(){
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("New relation");
		root.add(new DefaultMutableTreeNode(L_11_RELATION));
		root.add(new DefaultMutableTreeNode(L_1N_RELATION));
		root.add(new DefaultMutableTreeNode(L_MN_RELATION));
		return root;
	}

	@Override
	public void valueChanged(final TreeSelectionEvent tse) {
		Schedule.inEDT(() -> {
			if (tse.getNewLeadSelectionPath() != null) {
				if (tse.getNewLeadSelectionPath().getPath().length == 2) {
					switch (tse.getNewLeadSelectionPath().getPathComponent(1).toString()) {
					case L_11_RELATION:
						load11RelationPage1();
						break;
					case L_1N_RELATION:
						load1NRelationPage1();
						break;
					default:
						loadMNRelationPage1();
						break;
					}
				}
			} else {
				Schedule.inEDT(Schedule.CLICK_DELAY, () -> valueChanged(tse));
			}
		});
	}

	private void load11RelationPage1(){
		addUniqueIndex = true;
		load1XRelationPage1();
		checkSQLChanges();
	}

	private void load1NRelationPage1(){
		addUniqueIndex = false;
		load1XRelationPage1();
		checkSQLChanges();
		Schedule.inEDT(Schedule.TYPE_DELAY, () -> uniqueIndex = null);
	}

	private void load1XRelationPage1(){
		rel1Name = null;
		rel2Name = null;
		String name = NameGenerator.createForeignKeyName(attr1); //"fkey_"+rel.getName()+"_"+(rel.getConstraints().size()+1);
		Constraint con = new Constraint(rel.getDB(), rel.getSchema().getSchemaPrefix()+name);
		con.setRelsAttrsByName(rel.getDB(),
			rel.getFullName(),
			rel2.getFullName(),
			attr1.getName(),
			attr2.getName(),
			true);
		con.getBehavior().setNew();
		loadConstraintPage1(con);
	}

	private void generateMN(Relation elem, String rel1Name, String rel2Name, String attr1Name, String attr2Name, String ref1Name, String ref2Name, String pkeyType, String pkeyName) {
		attr1 = elem.getDB().getRelationByFullName(rel1Name).getAttributeByName(attr1Name);
		attr2 = elem.getDB().getRelationByFullName(rel2Name).getAttributeByName(attr2Name);

		String attType1 = attr1.getBehavior().getAttType();
		String attType2 = attr2.getBehavior().getAttType();
		String attr1Type = attType1.matches("serial4?") ? "int4" : (attType1.matches("(bigserial|serial8)") ? "int8" : attType1);
		String attr2Type = attType2.matches("serial4?") ? "int4" : (attType2.matches("(bigserial|serial8)") ? "int8" : attType2);

		elem.getAttributes().clear();
		elem.getIndexes().clear();
		elem.getConstraints().clear();

		if(!pkeyType.equals(getPKeyTypes()[0])){
			elem.getAttributes().add(new Attribute(elem, "id", "serial", false, elem.getAttributes().size()+1, "", "p", ""));
		}
		elem.getAttributes().add(new Attribute(elem, ref1Name, attr1Type, false, elem.getAttributes().size()+1, "", "p", ""));
		elem.getAttributes().add(new Attribute(elem, ref2Name, attr2Type, false, elem.getAttributes().size()+1, "", "p", ""));

		Index ind = new Index(elem,
				elem.getSchema().getSchemaPrefix()+pkeyName,
				pkeyType.equals(getPKeyTypes()[0]) ? "PRIMARY KEY ("+ref1Name+","+ref2Name+")" : "PRIMARY KEY (id)",
				"",
				"btree",
				new String[0],
				true,
				true,
				true,
				""
			);
		ind.assignToRelation(elem);
		ind.checkColumns();

		if(pkeyType.equals(getPKeyTypes()[1])){
			ind = new Index(elem,
					elem.getSchema().getSchemaPrefix()+pkeyName.replace("pkey_", "unique_").replace("pk_", "ux_"),
					"UNIQUE ("+ref1Name+", "+ref2Name+")",
					"",
					"btree",
					new String[0],
					true,
					false,
					true,
					""
				);
			ind.assignToRelation(elem);
			ind.checkColumns();
		}

		Attribute conAttr1 = elem.getAttributes().get(pkeyType.equals(getPKeyTypes()[0]) ? 0 : 1);
		Constraint con = new Constraint(elem.getDB(), elem.getSchema().getSchemaPrefix()+NameGenerator.createForeignKeyName(conAttr1)/*"fkey_"+rel.getName()*/, "c", "c", "");
		con.setRel1(elem);
		con.setAttr1(conAttr1);
		con.setRel2(rel);
		con.setAttr2(attr1);
		elem.getConstraints().add(con);

		conAttr1 = elem.getAttributes().get(pkeyType.equals(getPKeyTypes()[0]) ? 1 : 2);
		con = new Constraint(elem.getDB(), elem.getSchema().getSchemaPrefix()+NameGenerator.createForeignKeyName(conAttr1), "c", "c", "");
		con.setRel1(elem);
		con.setAttr1(conAttr1);
		con.setRel2(rel2);
		con.setAttr2(attr2);
		elem.getConstraints().add(con);
	}

	private void loadMNRelationPage1(){
		// TODO: make configurable through Settings / NameGenerator
		String name = rel.getName()+"_"+rel2.getName();

		final Relation elem = new Relation(rel.getSchema(), name, rel.getSchema().getSchemaPrefix()+name, 2, 0, false, new String[0], "", "");
		elem.getBehavior().setNew();
		editableElement = elem;
		editableElement.getBehavior().prepareForEdit();

		resetContent();

		focus(addTextInput(Relation.Behavior.L_NAME, elem.getName()));
		addCombo(Relation.Behavior.L_SCHEMA, elem.getDB().getSchemaNames(), elem.getSchema().getName());

		addEmptyLine();

		rel1Name = rel.getFullName();
		rel2Name = rel2.getFullName();

		final IconableComboBox localRelCombo = addCombo(Constraint.Behavior.L_LOC_REL, elem.getDB().getRelationNames(), rel.getFullName());
		localRelCombo.addActionListener(ae -> {
            rel1Name = localRelCombo.getSelectedItem().toString();
            rel = elem.getDB().getRelationByFullName(rel1Name);

            localAttrCombo.setModel(new DefaultComboBoxModel<>(rel.getAttributeNames()));

            editableElement.getBehavior().notifyChange(Constraint.Behavior.L_LOC_REL, rel1Name);
            editableElement.getBehavior().notifyChange(Constraint.Behavior.L_LOC_ATTR, localAttrCombo.getSelectedItem().toString());
            checkSQLChanges();
        });
		final IconableComboBox remRelCombo = addCombo(Constraint.Behavior.L_REM_REL, elem.getDB().getRelationNames(), rel2.getFullName());
		remRelCombo.addActionListener(ae -> {
            rel2Name = remRelCombo.getSelectedItem().toString();
            rel2 = elem.getDB().getRelationByFullName(rel2Name);

            remoteAttrCombo.setModel(new DefaultComboBoxModel<>(rel2.getAttributeNames()));

            editableElement.getBehavior().notifyChange(Constraint.Behavior.L_REM_REL, rel2Name);
            editableElement.getBehavior().notifyChange(Constraint.Behavior.L_REM_ATTR, remoteAttrCombo.getSelectedItem().toString());
            checkSQLChanges();
        });
		localAttrCombo = addCombo(Constraint.Behavior.L_LOC_ATTR, rel.getAttributeNames(), attr1.getName());
		remoteAttrCombo = addCombo(Constraint.Behavior.L_REM_ATTR, rel2.getAttributeNames(), attr2.getName());

		ref1TextField = addTextInput(Relation.Behavior.L_MN_REF_COL_1, attr1.getName());
		ref2TextField = addTextInput(Relation.Behavior.L_MN_REF_COL_2, attr2.getName());

		addEmptyLine();

		if(connection.isSupported(SupportedElement.PRIMARY_KEY_SERIAL)){
			pkeyTypeCombo = addCombo(Relation.Behavior.L_MN_PK_TYPE, getPKeyTypes(), getPKeyTypes()[0]);
		}else{
			pkeyTypeCombo = new IconableComboBox(new String[]{getPKeyTypes()[0]});
		}
		addTextInput(Relation.Behavior.L_DESCR, "", SPAN);

		//addTextField(Relation.Behavior.L_MN_PK_NAME, "pkey_"+name);
		//pkNameTextField = lastTextField;

		generateMN(elem, rel1Name, rel2Name, (String)localAttrCombo.getSelectedItem(), (String)remoteAttrCombo.getSelectedItem(), ref1TextField.getText(), ref2TextField.getText(), (String)pkeyTypeCombo.getSelectedItem(), NameGenerator.createPrimaryKeyName(elem));
		addSQLInput(editableElement.getQueryCreate(connection));
		setNextButton(BTN_SAVE_TEXT, false, SAVE_IN_MODEL);
	}

	@Override
	public boolean checkSQLChanges(){
		if(editableElement instanceof Constraint) {
			rel = (Relation) ((LineComponent)editableElement).getRel1();
			rel2 = (Relation) ((LineComponent)editableElement).getRel2();
			if(super.checkSQLChanges()){
				if(addUniqueIndex){
					String name = uniqueIndexNameInput.getText();
					uniqueIndex = new Index(rel,
							rel.getSchema().getSchemaPrefix()+name,
							(String)localAttrCombo.getSelectedItem(),
							"",
							connection.getAccessMethods()[0],
							new String[0],
							true,
							false,
							true,
							""
						);
					uniqueIndex.getBehavior().prepareForEdit();
					uniqueIndex.getBehavior().setNew();
					queryInput.setText(queryInput.getText()+uniqueIndex.getQueryCreate(connection));

					if(revertSQL != null){
						revertSQL += uniqueIndex.getQueryChangeRevert(connection);
					}
				}
				return true;
			}
			return false;
		} else {
			generateMN((Relation)editableElement, rel1Name, rel2Name, (String)localAttrCombo.getSelectedItem(), (String)remoteAttrCombo.getSelectedItem(), ref1TextField.getText(), ref2TextField.getText(), (String)pkeyTypeCombo.getSelectedItem(), "pkey_"+((Relation.Behavior)editableElement.getBehavior()).getName());
			return super.checkSQLChanges();
		}
	}

	@Override
	protected void saveEdited(boolean inModelOnly) throws OperationCancelException {
		super.saveEdited(inModelOnly);
		if(rel1Name != null){
			Relation elem = (Relation) editableElement;
			List<Constraint> cons = elem.getConstraints();
			for(int i=0; i<cons.size(); i++){
				Constraint con = cons.get(0);
				cons.remove(0);
				con.assignToRels();
			}
		}
		if(uniqueIndex != null){
			uniqueIndex.assignToRelation(rel);
			Canvas.instance.drawProject(false);
			uniqueIndex.getBehavior().setNew();
		}
		Canvas.instance.drawProject(true);
		frame.dispose();
	}

	@Override
	protected void updateTreeToLine1(){}

}
