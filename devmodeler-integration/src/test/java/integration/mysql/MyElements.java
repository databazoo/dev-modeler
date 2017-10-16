
package integration.mysql;

import com.databazoo.devmodeler.conn.ConnectionUtils;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.model.View;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.wizards.relation.MNRelationWizard;
import com.databazoo.devmodeler.wizards.relation.RelationWizard;
import integration.IntegrationBase;

import static org.junit.Assert.assertEquals;

abstract class MyElements extends IntegrationBase {

	MyElements () throws Exception {
		super();
	}

	Schema createSchema(String name) throws Exception {
		return Project.getCurrDB().getSchemas().get(0);
	}
/*
	protected Function createFunction(Schema schema, String name) throws InterruptedException {
		if(name == null) {
			name = "crud_test_function_1";
		}

		RelationWizard w = RelationWizard.get(null);
		w.setDatabase(db);
		w = RelationWizard.get(null);
		w.drawProperties(schema, 3);
		Thread.sleep(SLEEP_TIMEOUT);

		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Function.Behavior.L_NAME, name);
		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Function.Behavior.L_RETURNS, "trigger");
		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Function.Behavior.L_BODY, "\nBEGIN\n\tRETURN NEW;\nEND\n");
		assertEquals(true, w.checkSQLChanges());
		assertEquals(true, w.saveInModelAndDB());
		assertEquals(1, Project.getCurrent().revisions.size());
		w.close();
		Thread.sleep(SLEEP_TIMEOUT);

		Function func = schema.getFunctionByFullName(name);
		System.out.println("Function created: "+func.getFullName());
		Thread.sleep(SLEEP_TIMEOUT);

		return func;
	}
*/
	Relation createRelation(Schema schema, String name, boolean useInnoDB) throws Exception {
		if(name == null) {
			name = "crud_test_table_1";
		}

		RelationWizard w = RelationWizard.get(null);
		w.setDatabase(db);
		w = RelationWizard.get(null);
		w.drawProperties(schema, 2);
		Thread.sleep(SLEEP_TIMEOUT);

		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Relation.Behavior.L_NAME, name);
		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Relation.Behavior.L_STORAGE, useInnoDB ? "InnoDB" : "MyISAM");

		assertEquals(true, w.checkSQLChanges());
		assertEquals(true, w.saveInModelAndDB());
		assertEquals(1, Project.getCurrent().revisions.size());
		w.close();
		Thread.sleep(SLEEP_TIMEOUT);

		Relation rel = schema.getRelationByName(name);
		System.out.println("Relation created: "+rel.getFullName());
		Thread.sleep(SLEEP_TIMEOUT);

		return rel;
	}

	Attribute createAttribute(Relation rel, String name) throws Exception {
		if(name == null) {
			name = "crud_test_attr_1";
		}

		RelationWizard w = RelationWizard.get(null);
		w.drawProperties(rel, rel.getAttributes().size());
		Thread.sleep(SLEEP_TIMEOUT);

		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Attribute.Behavior.L_NAME, name);
		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Attribute.Behavior.L_PRECISION, "50");
		//w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Attribute.Behavior.L_STORAGE, Attribute.Behavior.L_STORAGE_EXTENDED);
		assertEquals(true, w.checkSQLChanges());
		assertEquals(true, w.saveInModelAndDB());
		assertEquals(1, Project.getCurrent().revisions.size());
		w.close();
		Thread.sleep(SLEEP_TIMEOUT);

		Attribute attr = rel.getAttributeByName(name);
		System.out.println("Attribute created: "+attr.getFullName());
		Thread.sleep(SLEEP_TIMEOUT);

		return attr;
	}

	Index createPK(Attribute attr, String name) throws Exception {
		if(name == null) {
			name = "crud_test_pkey_1";
		}

		RelationWizard w = RelationWizard.get(null);
		w.drawProperties(attr.getRel(), Relation.L_NEW_PK);
		Thread.sleep(SLEEP_TIMEOUT);

		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Index.Behavior.L_NAME, name);
		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Index.Behavior.L_COLUMNS, attr.getName());
		assertEquals(true, w.checkSQLChanges());
		assertEquals(true, w.saveInModelAndDB());
		assertEquals(1, Project.getCurrent().revisions.size());
		w.close();
		Thread.sleep(SLEEP_TIMEOUT);

		Index ind = attr.getRel().getIndexes().get(attr.getRel().getIndexes().size()-1);
		System.out.println("PK created: "+ind.getFullName());
		Thread.sleep(SLEEP_TIMEOUT);

		return ind;
	}

	Index createUC(Attribute attr, String name) throws Exception {
		if(name == null) {
			name = "crud_test_unique_index_1";
		}

		RelationWizard w = RelationWizard.get(null);
		w.drawProperties(attr.getRel(), Relation.L_NEW_INDEX);
		Thread.sleep(SLEEP_TIMEOUT);

		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Index.Behavior.L_NAME, name);
		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Index.Behavior.L_COLUMNS, attr.getName());
		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Index.Behavior.L_UNIQUE, true);
		assertEquals(true, w.checkSQLChanges());
		assertEquals(true, w.saveInModelAndDB());
		assertEquals(1, Project.getCurrent().revisions.size());
		w.close();
		Thread.sleep(SLEEP_TIMEOUT);

		Index ind = attr.getRel().getIndexes().get(attr.getRel().getIndexes().size()-1);
		System.out.println("Unique index created: "+ind.getFullName());
		Thread.sleep(SLEEP_TIMEOUT);

		return ind;
	}

	Index createIndex(Attribute attr, String name) throws Exception {
		if(name == null) {
			name = "crud_test_index_1";
		}

		RelationWizard w = RelationWizard.get(null);
		w.drawProperties(attr.getRel(), Relation.L_NEW_INDEX);
		Thread.sleep(SLEEP_TIMEOUT);

		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Index.Behavior.L_NAME, name);
		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Index.Behavior.L_COLUMNS, attr.getName());
		assertEquals(true, w.checkSQLChanges());
		assertEquals(true, w.saveInModelAndDB());
		assertEquals(1, Project.getCurrent().revisions.size());
		w.close();
		Thread.sleep(SLEEP_TIMEOUT);

		Index ind = attr.getRel().getIndexes().get(attr.getRel().getIndexes().size()-1);
		System.out.println("Index created: "+ind.getFullName());
		Thread.sleep(SLEEP_TIMEOUT);

		return ind;
	}

	Trigger createTrigger(Relation rel, String name) throws Exception {
		if(name == null) {
			name = "crud_test_trigger_1";
		}

		RelationWizard w = RelationWizard.get(null);
		w.drawProperties(rel, Relation.L_NEW_TRIGGER);
		Thread.sleep(SLEEP_TIMEOUT);

		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Trigger.Behavior.L_NAME, name);
		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Trigger.Behavior.L_BODY, "BEGIN\n\tUPDATE "+rel.getFullName()+" SET "+rel.getAttributes().get(0)+" = null;\nEND");
		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Trigger.Behavior.L_EVENT, new boolean[]{true, false, false, false});
		assertEquals(true, w.checkSQLChanges());
		assertEquals(true, w.saveInModelAndDB());
		assertEquals(1, Project.getCurrent().revisions.size());
		w.close();
		Thread.sleep(SLEEP_TIMEOUT);

		Trigger trig = rel.getTriggers().get(rel.getTriggers().size()-1);
		System.out.println("Trigger created: "+trig.getFullName());
		Thread.sleep(SLEEP_TIMEOUT);

		return trig;
	}

	Constraint createConstraint(Attribute attr1, Attribute attr2, String name) throws Exception {
		if(name == null) {
			name = "crud_test_constraint_1";
		}

		MNRelationWizard w = MNRelationWizard.get();
		w.drawRelation(attr1.getRel(), attr1, attr2.getRel(), attr2);
		Thread.sleep(SLEEP_TIMEOUT);

		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Constraint.Behavior.L_NAME, name);
		assertEquals(true, w.checkSQLChanges());
		assertEquals(true, w.saveInModelAndDB());
		assertEquals(1, Project.getCurrent().revisions.size());
		w.close();
		Thread.sleep(SLEEP_TIMEOUT);

		Constraint con = attr1.getRel().getConstraints().get(attr1.getRel().getConstraints().size()-1);
		System.out.println("Constraint created: "+con.getFullName());
		Thread.sleep(SLEEP_TIMEOUT);

		return con;
	}

	View createView(Schema schema, Attribute attr, String name) throws Exception {
		if(name == null) {
			name = "crud_test_view_1";
		}

		RelationWizard w = RelationWizard.get(null);
		w.drawProperties(schema, 3);
		Thread.sleep(SLEEP_TIMEOUT);

		// SELECT `virtualdb1`.`new_table1`.`new_attribute1` AS `new_attribute1` FROM `virtualdb1`.`new_table1`
		String dbName = ConnectionUtils.getCurrent(schema.getDB().getName()).getDbAlias();
		String body = "select `"+dbName+"`.`"+attr.getRel().getName()+"`.`"+attr.getName() + "` AS `"+attr.getName() + "` from `"+dbName+"`.`"+attr.getRel().getName()+"`;";

		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(View.Behavior.L_NAME, name);
		w.getEditedElem().getBehavior().getValuesForEdit().notifyChange(View.Behavior.L_BODY, body);
		assertEquals(true, w.checkSQLChanges());
		assertEquals(true, w.saveInModelAndDB());
		assertEquals(1, Project.getCurrent().revisions.size());
		w.close();
		Thread.sleep(SLEEP_TIMEOUT);

		View view = schema.getViews().get(schema.getViews().size()-1);
		System.out.println("View created: "+view.getFullName());
		Thread.sleep(SLEEP_TIMEOUT);

		return view;
	}
}
