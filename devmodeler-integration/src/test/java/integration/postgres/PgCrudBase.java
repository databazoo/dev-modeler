
package integration.postgres;

import com.databazoo.devmodeler.conn.ConnectionUtils;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.CheckConstraint;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Sequence;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.model.View;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.Revision;
import com.databazoo.devmodeler.wizards.relation.RelationWizard;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;


public abstract class PgCrudBase extends PgElements {
	private RelationWizard wiz;
	private final LinkedHashMap<String,String> changes = new LinkedHashMap<>();

	PgCrudBase () throws Exception {
		super();
	}

	@Before
	public void setUp() {
		Project.getCurrent().revisions.clear();
	}

	@After
	public void tearDown() throws InterruptedException {
		revertLastRevision();
		Project.getCurrent().syncWithServer();
		for(Revision r: Project.getCurrent().revisions){
			r.drop();
		}
	}

	@Test
	public void testSchema() throws Exception {
		System.out.println("CRUD: Schema");
		assertEquals(0, Project.getCurrent().revisions.size());

		Schema schema = createSchema(null);

		changes.clear();
		changes.put(Schema.Behavior.L_NAME, "crud_test_schema_"+((int)(Math.random()*20))+"_updated");
		changes.put(Schema.Behavior.L_DESCR, "crud_test_schema's description here");

		for(String key : changes.keySet()){
			System.out.println("Testing key "+key);

			wiz = RelationWizard.get(null);
			wiz.drawProperties(schema, 1);
			Thread.sleep(SLEEP_TIMEOUT);

			wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(key, changes.get(key));
			assertEquals(true, wiz.checkSQLChanges());
			wiz.saveInModelAndDB();
			assertEquals(false, wiz.checkSQLChanges());
			assertEquals(1, Project.getCurrent().revisions.size());
			wiz.close();

			assertEquals(false, compareModelAndDB());
		}
		assertEquals(1, Project.getCurrent().revisions.size());

		wiz = RelationWizard.get(null).enableDrop();
		wiz.drawProperties(schema, 1);
		Thread.sleep(SLEEP_TIMEOUT);
		assertEquals(true, wiz.checkSQLChanges());
		wiz.saveInModelAndDB();
		wiz.close();
		assertEquals(1, Project.getCurrent().revisions.size());
		Thread.sleep(SLEEP_TIMEOUT);

		Revision rev = Project.getCurrent().revisions.get(0);
		assertEquals(2+changes.size(), rev.getCntChanges());

		checkLastRevisionApplicable();
		assertEquals(false, compareModelAndDB());
	}

	@Test
	public void testTable() throws Exception {
		System.out.println("CRUD: Table");
		assertEquals(0, Project.getCurrent().revisions.size());

		Schema schema = createSchema(null);
		Relation rel1 = createRelation(schema, "crud_test_rel1");
		createAttribute(rel1, null);
		Relation rel2 = createRelation(schema, "crud_test_rel2");
		createAttribute(rel2, null);

		changes.clear();
		changes.put(Relation.Behavior.L_NAME, "crud_test_rel_"+((int)(Math.random()*20))+"_updated");
		changes.put(Relation.Behavior.L_INHERITS, rel2.getFullName());
		//changes.put(Relation.Behavior.L_HAS_OIDS, true);
		changes.put(Relation.Behavior.L_SCHEMA, "public");
		changes.put(Relation.Behavior.L_DESCR, "crud_test_rel's description here");

		for(String key : changes.keySet()){
			System.out.println("Testing key "+key);

			wiz = RelationWizard.get(null);
			wiz.drawProperties(rel1);
			Thread.sleep(SLEEP_TIMEOUT);

			wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(key, changes.get(key));
			assertEquals(true, wiz.checkSQLChanges());
			wiz.saveInModelAndDB();
			assertEquals(false, wiz.checkSQLChanges());
			assertEquals(1, Project.getCurrent().revisions.size());
			wiz.close();

			assertEquals(false, compareModelAndDB());
		}
		assertEquals(1, Project.getCurrent().revisions.size());

		wiz = RelationWizard.get(null).enableDrop();
		wiz.drawProperties(rel1);
		Thread.sleep(SLEEP_TIMEOUT);
		assertEquals(true, wiz.checkSQLChanges());
		wiz.saveInModelAndDB();
		wiz.close();
		assertEquals(1, Project.getCurrent().revisions.size());
		Thread.sleep(SLEEP_TIMEOUT);

		Revision rev = Project.getCurrent().revisions.get(0);
		assertEquals(6+changes.size(), rev.getCntChanges());

		checkLastRevisionApplicable();
		assertEquals(false, compareModelAndDB());
	}

	@Test
	public void testAttribute() throws Exception {
		System.out.println("CRUD: Attribute");
		assertEquals(0, Project.getCurrent().revisions.size());

		Schema schema = createSchema(null);
		Relation rel = createRelation(schema, null);
		Attribute attr = createAttribute(rel, null);

		changes.clear();
		changes.put(Attribute.Behavior.L_NAME, "crud_test_attr_"+((int)(Math.random()*20))+"_updated");
		changes.put(Attribute.Behavior.L_PRECISION, "255");
		//changes.put(Attribute.Behavior.L_NULLABLE, true);
		changes.put(Attribute.Behavior.L_DEFAULT, "\"substring\"('text default value'::text, 5)");
		changes.put(Attribute.Behavior.L_DATATYPE, "character");
		changes.put(Attribute.Behavior.L_STORAGE, Attribute.Behavior.L_STORAGE_EXTERNAL);
		changes.put(Attribute.Behavior.L_DESCR, "crud_test_rel's description here");

		for(String key : changes.keySet()){
			System.out.println("Testing "+key+" => "+changes.get(key));

			wiz = RelationWizard.get(null);
			wiz.drawProperties(rel, attr.getName());
			Thread.sleep(SLEEP_TIMEOUT);

			wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(key, changes.get(key));
			assertEquals(true, wiz.checkSQLChanges());
			wiz.saveInModelAndDB();
			assertEquals(false, wiz.checkSQLChanges());
			assertEquals(1, Project.getCurrent().revisions.size());
			wiz.close();

			assertEquals(false, compareModelAndDB());
		}
		assertEquals(1, Project.getCurrent().revisions.size());

		wiz = RelationWizard.get(null).enableDrop();
		wiz.drawProperties(rel, rel.getAttributes().lastIndexOf(attr));
		Thread.sleep(SLEEP_TIMEOUT);
		assertEquals(true, wiz.checkSQLChanges());
		wiz.saveInModelAndDB();
		wiz.close();
		assertEquals(1, Project.getCurrent().revisions.size());
		Thread.sleep(SLEEP_TIMEOUT);

		Revision rev = Project.getCurrent().revisions.get(0);
		assertEquals(4+changes.size(), rev.getCntChanges());

		checkLastRevisionApplicable();
		assertEquals(false, compareModelAndDB());
	}

	@Test
	public void testPK() throws Exception {
		System.out.println("CRUD: PK");
		assertEquals(0, Project.getCurrent().revisions.size());

		Schema schema = createSchema(null);
		Relation rel = createRelation(schema, null);
		Attribute attr1 = createAttribute(rel, "crud_test_col_1");
		Attribute attr2 = createAttribute(rel, "crud_test_col_2");
		Index pKey = createPK(attr1, null);

		changes.clear();
		changes.put(Index.Behavior.L_NAME, "crud_test_pk_"+((int)(Math.random()*20))+"_updated");
		changes.put(Index.Behavior.L_COLUMNS, attr1.getName()+", "+attr2.getName());
		changes.put(Index.Behavior.L_DESCR, "crud_test_rel's description here");

		for(String key : changes.keySet()){
			System.out.println("Testing "+key+" => "+changes.get(key));

			wiz = RelationWizard.get(null);
			wiz.drawProperties(rel, pKey.getName());
			Thread.sleep(SLEEP_TIMEOUT);

			wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(key, changes.get(key));
			assertEquals(true, wiz.checkSQLChanges());
			wiz.saveInModelAndDB();
			assertEquals(false, wiz.checkSQLChanges());
			assertEquals(1, Project.getCurrent().revisions.size());
			wiz.close();

			assertEquals(false, compareModelAndDB());
		}
		assertEquals(1, Project.getCurrent().revisions.size());

		wiz = RelationWizard.get(null).enableDrop();
		wiz.drawProperties(rel, pKey.getName());
		Thread.sleep(SLEEP_TIMEOUT);
		assertEquals(true, wiz.checkSQLChanges());
		wiz.saveInModelAndDB();
		wiz.close();
		assertEquals(1, Project.getCurrent().revisions.size());
		Thread.sleep(SLEEP_TIMEOUT);

		Revision rev = Project.getCurrent().revisions.get(0);
		assertEquals(6+changes.size(), rev.getCntChanges());

		checkLastRevisionApplicable();
		assertEquals(false, compareModelAndDB());
	}

	@Test
	public void testFK() throws Exception {
		System.out.println("CRUD: FK");
		assertEquals(0, Project.getCurrent().revisions.size());

		Schema schema = createSchema(null);
		Relation rel1 = createRelation(schema, "crud_test_rel_1");
		Attribute attr1 = createAttribute(rel1, "crud_test_attr_1");
		Attribute attr3 = createAttribute(rel1, "crud_test_attr_3");
		createPK(attr1, "crud_test_pk_1");

		Relation rel2 = createRelation(schema, "crud_test_rel_2");
		Attribute attr2 = createAttribute(rel2, "crud_test_attr_2");
		Attribute attr4 = createAttribute(rel2, "crud_test_attr_4");
		createPK(attr2, "crud_test_pk_2");
		createUC(attr4, "crud_test_ux_2");

		Relation rel20 = createRelation(schema, "crud_test_rel_20");
		Attribute attr20 = createAttribute(rel20, "crud_test_attr_2");
		Attribute attr21 = createAttribute(rel20, "crud_test_attr_21");
		createPK(attr20, "crud_test_pk_20");
		createUC(attr21, "crud_test_ux_21");

		Constraint con = createConstraint(attr1, attr2, null);

		changes.clear();
		changes.put(Constraint.Behavior.L_NAME, "crud_test_con_"+((int)(Math.random()*20))+"_updated");
		//changes.put(Constraint.Behavior.L_REM_REL, rel20.getFullName());
		changes.put(Constraint.Behavior.L_LOC_ATTR, attr3.getName());
		changes.put(Constraint.Behavior.L_REM_ATTR, attr4.getName());
		changes.put(Constraint.Behavior.L_ON_UPDATE, "RESTRICT");
		changes.put(Constraint.Behavior.L_ON_DELETE, "RESTRICT");
		changes.put(Constraint.Behavior.L_DESCR, "crud_test_con's description here");

		for(String key : changes.keySet()){
			System.out.println("Testing "+key+" => "+changes.get(key));

			wiz = RelationWizard.get(null);
			wiz.drawProperties(rel1, con.getName());
			Thread.sleep(SLEEP_TIMEOUT);

			wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(key, changes.get(key));
			assertEquals(true, wiz.checkSQLChanges());
			wiz.saveInModelAndDB();
			assertEquals(false, wiz.checkSQLChanges());
			assertEquals(1, Project.getCurrent().revisions.size());
			wiz.close();

			assertEquals(false, compareModelAndDB());
		}
		assertEquals(1, Project.getCurrent().revisions.size());

		wiz = RelationWizard.get(null).enableDrop();
		wiz.drawProperties(rel1, con.getName());
		Thread.sleep(SLEEP_TIMEOUT);
		assertEquals(true, wiz.checkSQLChanges());
		wiz.saveInModelAndDB();
		wiz.close();
		assertEquals(1, Project.getCurrent().revisions.size());
		Thread.sleep(SLEEP_TIMEOUT);

		Revision rev = Project.getCurrent().revisions.get(0);
		assertEquals(17+changes.size(), rev.getCntChanges());

		checkLastRevisionApplicable();
		assertEquals(false, compareModelAndDB());
	}

	@Test
	public void testUC() throws Exception {
		System.out.println("CRUD: UC");
		assertEquals(0, Project.getCurrent().revisions.size());

		Schema schema = createSchema(null);
		Relation rel = createRelation(schema, null);
		Attribute attr1 = createAttribute(rel, "crud_test_attr_1");
		Attribute attr2 = createAttribute(rel, "crud_test_attr_2");
		Index ind = createUC(attr1, null);

		changes.clear();
		changes.put(Index.Behavior.L_NAME, "crud_test_unique_con_"+((int)(Math.random()*20))+"_updated");
		changes.put(Index.Behavior.L_COLUMNS, attr1.getName()+", "+attr2.getName());
		changes.put(Index.Behavior.L_DESCR, "crud_test_con's description here");

		for(String key : changes.keySet()){
			System.out.println("Testing "+key+" => "+changes.get(key));

			wiz = RelationWizard.get(null);
			wiz.drawProperties(rel, ind.getName());
			Thread.sleep(SLEEP_TIMEOUT);

			wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(key, changes.get(key));
			assertEquals(true, wiz.checkSQLChanges());
			wiz.saveInModelAndDB();
			assertEquals(false, wiz.checkSQLChanges());
			assertEquals(1, Project.getCurrent().revisions.size());
			wiz.close();

			assertEquals(false, compareModelAndDB());
		}
		assertEquals(1, Project.getCurrent().revisions.size());

		wiz = RelationWizard.get(null).enableDrop();
		wiz.drawProperties(rel, rel.getAttributes().size()+2);
		Thread.sleep(SLEEP_TIMEOUT);
		assertEquals(true, wiz.checkSQLChanges());
		wiz.saveInModelAndDB();
		wiz.close();
		assertEquals(1, Project.getCurrent().revisions.size());
		Thread.sleep(SLEEP_TIMEOUT);

		Revision rev = Project.getCurrent().revisions.get(0);
		assertEquals(6+changes.size(), rev.getCntChanges());

		checkLastRevisionApplicable();
		assertEquals(false, compareModelAndDB());
	}

	@Test
	public void testCC() throws Exception {
		System.out.println("CRUD: CC");
		assertEquals(0, Project.getCurrent().revisions.size());

		Schema schema = createSchema(null);
		Relation rel = createRelation(schema, null);
		Attribute attr = createAttribute(rel, null);
		CheckConstraint con = createCC(attr, null);

		changes.clear();
		changes.put(CheckConstraint.Behavior.L_NAME, "crud_test_unique_con_"+((int)(Math.random()*20))+"_updated");
		changes.put(CheckConstraint.Behavior.L_DEFINITION, attr.getName()+" IS NULL");
		changes.put(CheckConstraint.Behavior.L_DESCR, "crud_test_con's description here");

		for(String key : changes.keySet()){
			System.out.println("Testing "+key+" => "+changes.get(key));

			wiz = RelationWizard.get(null);
			wiz.drawProperties(rel, con.getName());
			Thread.sleep(SLEEP_TIMEOUT);

			wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(key, changes.get(key));
			assertEquals(true, wiz.checkSQLChanges());
			wiz.saveInModelAndDB();
			assertEquals(false, wiz.checkSQLChanges());
			assertEquals(1, Project.getCurrent().revisions.size());
			wiz.close();

			assertEquals(false, compareModelAndDB());
		}
		assertEquals(1, Project.getCurrent().revisions.size());

		wiz = RelationWizard.get(null).enableDrop();
		wiz.drawProperties(rel, con.getName());
		Thread.sleep(SLEEP_TIMEOUT);
		assertEquals(true, wiz.checkSQLChanges());
		wiz.saveInModelAndDB();
		wiz.close();
		assertEquals(1, Project.getCurrent().revisions.size());
		Thread.sleep(SLEEP_TIMEOUT);

		Revision rev = Project.getCurrent().revisions.get(0);
		assertEquals(5+changes.size(), rev.getCntChanges());

		checkLastRevisionApplicable();
		assertEquals(false, compareModelAndDB());
	}

	@Test
	public void testIndex() throws Exception {
		System.out.println("CRUD: Index");
		assertEquals(0, Project.getCurrent().revisions.size());

		Schema schema = createSchema(null);
		Relation rel = createRelation(schema, null);
		Attribute attr1 = createAttribute(rel, "crud_test_col_1");
		Attribute attr2 = createAttribute(rel, "crud_test_col_2");
		Attribute attr3 = createAttribute(rel, "crud_test_col_3");
		Index pKey = createIndex(attr1, null);

		wiz = RelationWizard.get(null);
		wiz.drawProperties(rel, pKey.getName());
		Thread.sleep(SLEEP_TIMEOUT);

		wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Index.Behavior.L_METHOD, "hash");
		assertEquals(true, wiz.checkSQLChanges());
		wiz.saveInModelAndDB();
		assertEquals(false, wiz.checkSQLChanges());
		assertEquals(1, Project.getCurrent().revisions.size());
		wiz.close();

		assertEquals(false, compareModelAndDB());

		changes.clear();
		changes.put(Index.Behavior.L_NAME, "crud_test_index_"+((int)(Math.random()*20))+"_updated");
		changes.put(Index.Behavior.L_METHOD, "btree");
		changes.put(Index.Behavior.L_COLUMNS, attr2.getName()+", "+attr3.getName());
		changes.put(Index.Behavior.L_CONDITION, "("+attr1.getName()+" IS NOT NULL)");
		//changes.put(Index.Behavior.L_UNIQUE, true);
		changes.put(Index.Behavior.L_DESCR, "crud_test_rel's description here");

		for(String key : changes.keySet()){
			System.out.println("Testing "+key+" => "+changes.get(key));

			wiz = RelationWizard.get(null);
			wiz.drawProperties(rel, pKey.getName());
			Thread.sleep(SLEEP_TIMEOUT);

			wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(key, changes.get(key));
			assertEquals(true, wiz.checkSQLChanges());
			wiz.saveInModelAndDB();
			assertEquals(false, wiz.checkSQLChanges());
			assertEquals(1, Project.getCurrent().revisions.size());
			wiz.close();

			assertEquals(false, compareModelAndDB());
		}
		assertEquals(1, Project.getCurrent().revisions.size());

		wiz = RelationWizard.get(null).enableDrop();
		wiz.drawProperties(rel, pKey.getName());
		Thread.sleep(SLEEP_TIMEOUT);
		assertEquals(true, wiz.checkSQLChanges());
		wiz.saveInModelAndDB();
		wiz.close();
		assertEquals(1, Project.getCurrent().revisions.size());
		Thread.sleep(SLEEP_TIMEOUT);

		Revision rev = Project.getCurrent().revisions.get(0);
		assertEquals(8+changes.size(), rev.getCntChanges());

		checkLastRevisionApplicable();
		assertEquals(false, compareModelAndDB());
	}

	@Test
	public void testFunction() throws Exception {
		System.out.println("CRUD: Function");
		assertEquals(0, Project.getCurrent().revisions.size());

		Schema schema = createSchema(null);
		Relation rel = createRelation(schema, null);
		Attribute attr = createAttribute(rel, null);
		Function func = createFunction(schema, null);

		wiz = RelationWizard.get(null);
		wiz.drawProperties(func);
		Thread.sleep(SLEEP_TIMEOUT);

		wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Function.Behavior.L_BODY, "SELECT "+attr.getName()+" FROM "+rel.getFullName()+";");
		wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Function.Behavior.L_RETURNS, "TABLE("+attr.getName()+" character varying)");
		wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Function.Behavior.L_ARGUMENTS, "id integer");
		wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Function.Behavior.L_LANGUAGE, "sql");
		wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Function.Behavior.L_ROWS, "1000");
		assertEquals(true, wiz.checkSQLChanges());
		wiz.saveInModelAndDB();
		assertEquals(false, wiz.checkSQLChanges());
		assertEquals(1, Project.getCurrent().revisions.size());
		wiz.close();

		assertEquals(false, compareModelAndDB());

		assertEquals(1, Project.getCurrent().revisions.size());

		changes.clear();
		changes.put(Function.Behavior.L_NAME, "crud_test_func_"+((int)(Math.random()*20))+"_updated");
		changes.put(Function.Behavior.L_SCHEMA, "public");
		changes.put(Function.Behavior.L_DESCR, "crud_test_func's description here");
		changes.put(Function.Behavior.L_VOLATILITY, "STABLE");
		changes.put(Function.Behavior.L_COST, "55");
		changes.put(Function.Behavior.L_ROWS, "25");

		for(String key : changes.keySet()){
			System.out.println("Testing "+key+" => "+changes.get(key));

			wiz = RelationWizard.get(null);
			wiz.drawProperties(func);
			Thread.sleep(SLEEP_TIMEOUT);

			wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(key, changes.get(key));
			assertEquals(true, wiz.checkSQLChanges());
			wiz.saveInModelAndDB();
			assertEquals(false, wiz.checkSQLChanges());
			assertEquals(1, Project.getCurrent().revisions.size());
			wiz.close();

			assertEquals(false, compareModelAndDB());
		}
		assertEquals(1, Project.getCurrent().revisions.size());

		wiz = RelationWizard.get(null).enableDrop();
		wiz.drawProperties(func);
		Thread.sleep(SLEEP_TIMEOUT);
		assertEquals(true, wiz.checkSQLChanges());
		wiz.saveInModelAndDB();
		wiz.close();
		assertEquals(1, Project.getCurrent().revisions.size());
		Thread.sleep(SLEEP_TIMEOUT);

		Revision rev = Project.getCurrent().revisions.get(0);
		assertEquals(4+changes.size(), rev.getCntChanges());	// INCORRECT NUMBER IS USED BECAUSE SOME OPERATIONS ON FUNCTIONS ARE MERGED INTO ONE.

		checkLastRevisionApplicable();
		assertEquals(false, compareModelAndDB());
	}

	@Test
	public void testTrigger() throws Exception {
		System.out.println("CRUD: Trigger");
		assertEquals(0, Project.getCurrent().revisions.size());

		Schema schema = createSchema(null);
		Relation rel = createRelation(schema, null);
		Function func1 = createFunction(schema, "crud_test_trig_func_1");
		Function func2 = createFunction(schema, "crud_test_trig_func_2");
		Trigger trig = createTrigger(rel, func1, null);

		changes.clear();
		changes.put(Trigger.Behavior.L_NAME, "crud_test_trig_"+((int)(Math.random()*20))+"_updated");
		changes.put(Trigger.Behavior.L_TRIGGER, "BEFORE");
		//changes.put(Trigger.Behavior.L_EVENT, new boolean[]{true, false, false, false});
		changes.put(Trigger.Behavior.L_FOR_EACH, "STATEMENT");
		changes.put(Trigger.Behavior.L_EXECUTE, func2.getFullName());
		changes.put(Trigger.Behavior.L_DESCR, "crud_test_func's description here");

		for(String key : changes.keySet()){
			System.out.println("Testing "+key+" => "+changes.get(key));

			wiz = RelationWizard.get(null);
			wiz.drawProperties(rel, trig.getName());
			Thread.sleep(SLEEP_TIMEOUT);

			wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(key, changes.get(key));
			assertEquals(true, wiz.checkSQLChanges());
			wiz.saveInModelAndDB();
			assertEquals(false, wiz.checkSQLChanges());
			assertEquals(1, Project.getCurrent().revisions.size());
			wiz.close();

			assertEquals(false, compareModelAndDB());
		}
		assertEquals(1, Project.getCurrent().revisions.size());

		wiz = RelationWizard.get(null).enableDrop();
		wiz.drawProperties(rel, trig.getName());
		Thread.sleep(SLEEP_TIMEOUT);
		assertEquals(true, wiz.checkSQLChanges());
		wiz.saveInModelAndDB();
		wiz.close();
		assertEquals(1, Project.getCurrent().revisions.size());
		Thread.sleep(SLEEP_TIMEOUT);

		Revision rev = Project.getCurrent().revisions.get(0);
		assertEquals(6+changes.size(), rev.getCntChanges());

		checkLastRevisionApplicable();
		assertEquals(false, compareModelAndDB());
	}

	@Test
	public void testView() throws Exception {
		System.out.println("CRUD: View");
		assertEquals(0, Project.getCurrent().revisions.size());

		Schema schema = createSchema(null);
		Relation rel = createRelation(schema, null);
		Attribute attr1 = createAttribute(rel, "crud_test_col_1");
		Attribute attr2 = createAttribute(rel, "crud_test_col_2");
		View view = createView(schema, attr1, null);

		String body;
		if(getProjectName().contains("9.3")){
			body = "SELECT "+attr1.getFullName() + ",\n    " + attr2.getFullName() + "\n   " + "FROM "+rel.getFullName()+";";
		}else{
			body = "SELECT "+attr1.getFullName() + ", " + attr2.getFullName() + " FROM "+rel.getFullName()+";";
		}

		changes.clear();
		changes.put(View.Behavior.L_NAME, "crud_test_view_"+((int)(Math.random()*20))+"_updated");
		changes.put(View.Behavior.L_SCHEMA, "public");
		changes.put(View.Behavior.L_BODY, body);
		changes.put(View.Behavior.L_DESCR, "crud_test_func's description here");

		for(String key : changes.keySet()){
			System.out.println("Testing "+key+" => "+changes.get(key));

			wiz = RelationWizard.get(null);
			wiz.drawProperties(view);
			Thread.sleep(SLEEP_TIMEOUT);

			wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(key, changes.get(key));
			assertEquals(true, wiz.checkSQLChanges());
			wiz.saveInModelAndDB();
			assertEquals(false, wiz.checkSQLChanges());
			assertEquals(1, Project.getCurrent().revisions.size());
			wiz.close();

			assertEquals(false, compareModelAndDB());
		}
		assertEquals(1, Project.getCurrent().revisions.size());

		wiz = RelationWizard.get(null).enableDrop();
		wiz.drawProperties(view);
		Thread.sleep(SLEEP_TIMEOUT);
		assertEquals(true, wiz.checkSQLChanges());
		wiz.saveInModelAndDB();
		wiz.close();
		assertEquals(1, Project.getCurrent().revisions.size());
		Thread.sleep(SLEEP_TIMEOUT);

		Revision rev = Project.getCurrent().revisions.get(0);
		assertEquals(6+changes.size(), rev.getCntChanges());

		checkLastRevisionApplicable();
		assertEquals(false, compareModelAndDB());
	}

	@Test
	public void testSequence() throws Exception {
		System.out.println("CRUD: Sequence");
		assertEquals(0, Project.getCurrent().revisions.size());

		Schema schema = createSchema(null);
		Sequence seq = createSequence(schema, null);

		changes.clear();
		changes.put(Sequence.Behavior.L_NAME, "crud_test_seq_"+((int)(Math.random()*20))+"_updated");
		changes.put(Sequence.Behavior.L_SCHEMA, "public");
		changes.put(Sequence.Behavior.L_START, "44");
		changes.put(Sequence.Behavior.L_INCREMENT, "4");
		changes.put(Sequence.Behavior.L_MIN, "20");
		changes.put(Sequence.Behavior.L_MAX, "200");
		//changes.put(Sequence.Behavior.L_CYCLE, true);
		changes.put(Sequence.Behavior.L_DESCR, "crud_test_seq's description here");

		for(String key : changes.keySet()){
			System.out.println("Testing "+key+" => "+changes.get(key));

			wiz = RelationWizard.get(null);
			wiz.drawProperties(seq);
			Thread.sleep(SLEEP_TIMEOUT);

			wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(key, changes.get(key));
			assertEquals(true, wiz.checkSQLChanges());
			wiz.saveInModelAndDB();
			assertEquals(false, wiz.checkSQLChanges());
			assertEquals(1, Project.getCurrent().revisions.size());
			wiz.close();

			assertEquals(false, compareModelAndDB());
		}
		assertEquals(1, Project.getCurrent().revisions.size());

		wiz = RelationWizard.get(null).enableDrop();
		wiz.drawProperties(seq);
		Thread.sleep(SLEEP_TIMEOUT);
		assertEquals(true, wiz.checkSQLChanges());
		wiz.saveInModelAndDB();
		wiz.close();
		assertEquals(1, Project.getCurrent().revisions.size());
		Thread.sleep(SLEEP_TIMEOUT);

		Revision rev = Project.getCurrent().revisions.get(0);
		assertEquals(3+changes.size(), rev.getCntChanges());

		checkLastRevisionApplicable();
		assertEquals(false, compareModelAndDB());
	}

	@Test
	public void testServerStatus() throws Exception {
		System.out.println("CRUD: Sequence");
		assertEquals(0, Project.getCurrent().revisions.size());

		IConnection conn = ConnectionUtils.getCurrent("db1");
		conn.runStatusCheck();
		Thread.sleep(SLEEP_TIMEOUT);
		Assert.assertNotNull(conn.getServerStatus());
	}
}
