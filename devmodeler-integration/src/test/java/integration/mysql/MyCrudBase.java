
package integration.mysql;

import com.databazoo.devmodeler.conn.ConnectionUtils;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;
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
import java.util.List;

import static org.junit.Assert.assertEquals;


public abstract class MyCrudBase extends MyElements {
	private RelationWizard wiz;
	private final LinkedHashMap<String,String> changes = new LinkedHashMap<>();

	MyCrudBase () throws Exception {
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
		Thread.sleep(1500);
	}

	@Test
	public void testTable() throws Exception {
		System.out.println("CRUD: Table");
		assertEquals(0, Project.getCurrent().revisions.size());

		Schema schema = createSchema(null);
		Relation rel1 = createRelation(schema, "crud_test_rel1", false);
		createAttribute(rel1, null);

		changes.clear();
		changes.put(Relation.Behavior.L_NAME, "crud_test_rel_"+((int)(Math.random()*20))+"_updated");
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
		wiz = RelationWizard.get(null);
		wiz.drawProperties(rel1);
		Thread.sleep(SLEEP_TIMEOUT);

		wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Relation.Behavior.L_STORAGE, "InnoDB");
		assertEquals(true, wiz.checkSQLChanges());
		wiz.saveInModelAndDB();
		assertEquals(false, wiz.checkSQLChanges());
		assertEquals(1, Project.getCurrent().revisions.size());
		wiz.close();

		if(getProjectName().contains("5.0")) {
			rel1.getBehavior().setDescr(rel1.getBehavior().getDescr() + "; InnoDB free: 4096 kB");
		}

		assertEquals(false, compareModelAndDB());
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
		assertEquals(4+changes.size(), rev.getCntChanges());

		checkLastRevisionApplicable();
		assertEquals(false, compareModelAndDB());
	}

	@Test
	public void testAttribute() throws Exception {
		System.out.println("CRUD: Attribute");
		assertEquals(0, Project.getCurrent().revisions.size());

		Schema schema = createSchema(null);
		Relation rel = createRelation(schema, null, false);
		Attribute attr = createAttribute(rel, null);

		changes.clear();
		changes.put(Attribute.Behavior.L_NAME, "crud_test_attr_"+((int)(Math.random()*20))+"_updated");
		changes.put(Attribute.Behavior.L_PRECISION, "200");
		//changes.put(Attribute.Behavior.L_NULLABLE, true);
		changes.put(Attribute.Behavior.L_DEFAULT, "text's default value");
		changes.put(Attribute.Behavior.L_DATATYPE, "char");
		changes.put(Attribute.Behavior.L_DESCR, "crud_test_attr's description here");

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
		assertEquals(3+changes.size(), rev.getCntChanges());

		checkLastRevisionApplicable();
		assertEquals(false, compareModelAndDB());
	}

	@Test
	public void testPK() throws Exception {
		System.out.println("CRUD: PK");
		assertEquals(0, Project.getCurrent().revisions.size());

		Schema schema = createSchema(null);
		Relation rel = createRelation(schema, null, false);
		Attribute attr1 = createAttribute(rel, "crud_test_col_1");
		Attribute attr2 = createAttribute(rel, "crud_test_col_2");

		// REMOVE DEFAULT PRIMARY KEY
		wiz = RelationWizard.get(null).enableDrop();
		wiz.drawProperties(rel, "id");
		Thread.sleep(SLEEP_TIMEOUT);

		assertEquals(true, wiz.checkSQLChanges());
		wiz.saveInModelAndDB();
		Thread.sleep(SLEEP_TIMEOUT);
		assertEquals(false, wiz.checkSQLChanges());
		assertEquals(1, Project.getCurrent().revisions.size());
		wiz.close();

		// CREATE CUSTOM PRIMARY KEY ON NON-AUTOINCREMENT COLUMN
		wiz = RelationWizard.get(null);
		wiz.drawProperties(rel, Relation.L_NEW_PK);
		Thread.sleep(SLEEP_TIMEOUT);

		assertEquals(true, wiz.checkSQLChanges());
		wiz.saveInModelAndDB();
		Thread.sleep(SLEEP_TIMEOUT);
		assertEquals(false, wiz.checkSQLChanges());
		assertEquals(1, Project.getCurrent().revisions.size());
		wiz.close();

		assertEquals(false, compareModelAndDB());

		// NOW TRY CHANGES ON PKEY
		changes.clear();
		changes.put(Index.Behavior.L_COLUMNS, attr1.getName()+","+attr2.getName());

		for(String key : changes.keySet()){
			System.out.println("Testing "+key+" => "+changes.get(key));

			wiz = RelationWizard.get(null);
			wiz.drawProperties(rel, "PRIMARY");
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

		// DROP PRIMARY KEY
		wiz = RelationWizard.get(null).enableDrop();
		wiz.drawProperties(rel, "PRIMARY");
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
		Relation rel1 = createRelation(schema, "crud_test_rel_1", true);
		Attribute attr1 = createAttribute(rel1, "crud_test_attr_1");
		Attribute attr3 = createAttribute(rel1, "crud_test_attr_3");
		createIndex(attr1, "crud_test_index_1");
		createIndex(attr3, "crud_test_index_3");

		Relation rel2 = createRelation(schema, "crud_test_rel_2", true);
		Attribute attr2 = createAttribute(rel2, "crud_test_attr_2");
		Attribute attr4 = createAttribute(rel2, "crud_test_attr_4");
		createUC(attr2, "crud_test_ux_1");
		createUC(attr4, "crud_test_ux_2");

		Relation rel20 = createRelation(schema, "crud_test_rel_20", true);
		Attribute attr20 = createAttribute(rel20, "crud_test_attr_2");
		Attribute attr21 = createAttribute(rel20, "crud_test_attr_21");
		createUC(attr20, "crud_test_ux_20");
		createUC(attr21, "crud_test_ux_21");

		Constraint con = createConstraint(attr1, attr2, null);

		Project.getCurrent().syncWithServer();

		List<Revision> revs = Project.getCurrent().revisions;
		for(int i=0; i<revs.size(); i++){
			Revision rev = revs.get(i);
			if(rev.isIncoming){
				revs.remove(rev);
				i--;
			}
		}

		changes.clear();
		changes.put(Constraint.Behavior.L_NAME, "crud_test_con_"+((int)(Math.random()*20))+"_updated");
		//changes.put(Constraint.Behavior.L_REM_REL, rel20.getFullName());
		changes.put(Constraint.Behavior.L_LOC_ATTR, attr3.getName());
		changes.put(Constraint.Behavior.L_REM_ATTR, attr4.getName());
		changes.put(Constraint.Behavior.L_ON_UPDATE, "RESTRICT");
		changes.put(Constraint.Behavior.L_ON_DELETE, "RESTRICT");

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
		Relation rel = createRelation(schema, null, false);
		Attribute attr1 = createAttribute(rel, "crud_test_attr_1");
		Attribute attr2 = createAttribute(rel, "crud_test_attr_2");
		Index ind = createUC(attr1, null);

		changes.clear();
		changes.put(Index.Behavior.L_NAME, "crud_test_unique_con_"+((int)(Math.random()*20))+"_updated");
		changes.put(Index.Behavior.L_COLUMNS, attr1.getName()+","+attr2.getName());
		if(!getProjectName().contains("5.0") && !getProjectName().contains("5.1")) {
			changes.put(Index.Behavior.L_DESCR, "crud_test_con's description here");
		}

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
		wiz.drawProperties(rel, ind.getName());
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
		Relation rel = createRelation(schema, null, false);
		Attribute attr1 = createAttribute(rel, "crud_test_col_1");
		Attribute attr2 = createAttribute(rel, "crud_test_col_2");
		Attribute attr3 = createAttribute(rel, "crud_test_col_3");
		Index ind = createIndex(attr1, null);

		wiz = RelationWizard.get(null);
		wiz.drawProperties(rel, ind.getName());
		Thread.sleep(SLEEP_TIMEOUT);

		wiz.getEditedElem().getBehavior().getValuesForEdit().notifyChange(Index.Behavior.L_METHOD, "fulltext");
		assertEquals(true, wiz.checkSQLChanges());
		wiz.saveInModelAndDB();
		assertEquals(false, wiz.checkSQLChanges());
		assertEquals(1, Project.getCurrent().revisions.size());
		wiz.close();

		assertEquals(false, compareModelAndDB());

		changes.clear();
		changes.put(Index.Behavior.L_NAME, "crud_test_index_"+((int)(Math.random()*20))+"_updated");
		changes.put(Index.Behavior.L_METHOD, "btree");
		changes.put(Index.Behavior.L_COLUMNS, attr2.getName()+","+attr3.getName());
		if(!getProjectName().contains("5.0") && !getProjectName().contains("5.1")) {
			changes.put(Index.Behavior.L_DESCR, "crud_test_con's description here");
		}

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
		wiz.drawProperties(rel, ind.getName());
		Thread.sleep(SLEEP_TIMEOUT);
		assertEquals(true, wiz.checkSQLChanges());
		wiz.saveInModelAndDB();
		wiz.close();
		assertEquals(1, Project.getCurrent().revisions.size());
		Thread.sleep(SLEEP_TIMEOUT);

		Revision rev = Project.getCurrent().revisions.get(0);
		assertEquals(7+changes.size(), rev.getCntChanges());

		checkLastRevisionApplicable();
		assertEquals(false, compareModelAndDB());
	}
/*
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
	}*/

	@Test
	public void testTrigger() throws Exception {
		System.out.println("CRUD: Trigger");
		assertEquals(0, Project.getCurrent().revisions.size());

		Schema schema = createSchema(null);
		Relation rel = createRelation(schema, null, false);
		Attribute attr1 = createAttribute(rel, "crud_test_col_1");
		Attribute attr2 = createAttribute(rel, "crud_test_col_2");
		Trigger trig = createTrigger(rel, null);

		changes.clear();
		changes.put(Trigger.Behavior.L_NAME, "crud_test_trig_"+((int)(Math.random()*20))+"_updated");
		changes.put(Trigger.Behavior.L_TRIGGER, "BEFORE");
		//changes.put(Trigger.Behavior.L_EVENT, new boolean[]{true, false, false, false});
		changes.put(Trigger.Behavior.L_BODY, "BEGIN\n\tSET NEW."+attr2.getName()+" = null;\nEND");

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
		assertEquals(5+changes.size(), rev.getCntChanges());

		checkLastRevisionApplicable();
		assertEquals(false, compareModelAndDB());
	}

	@Test
	public void testView() throws Exception {
		System.out.println("CRUD: View");
		assertEquals(0, Project.getCurrent().revisions.size());

		Schema schema = createSchema(null);
		Relation rel = createRelation(schema, null, false);
		Attribute attr1 = createAttribute(rel, "crud_test_col_1");
		Attribute attr2 = createAttribute(rel, "crud_test_col_2");
		View view = createView(schema, attr1, null);

		String dbName = ConnectionUtils.getCurrent(schema.getDB().getName()).getDbAlias();
		String body = "select `"+dbName+"`.`"+rel.getName()+"`.`"+attr1.getName() + "` AS `"+attr1.getName() + "`,`"+dbName+"`.`"+rel.getName()+"`.`"+attr2.getName() + "` AS `"+attr2.getName() + "` from `"+dbName+"`.`"+rel.getName()+"`;";

		changes.clear();
		changes.put(View.Behavior.L_NAME, "crud_test_view_"+((int)(Math.random()*20))+"_updated");
		changes.put(View.Behavior.L_BODY, body);

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
		assertEquals(5+changes.size(), rev.getCntChanges());

		checkLastRevisionApplicable();
		assertEquals(false, compareModelAndDB());
	}

	@Test
	public void testServerStatus() throws Exception {
		System.out.println("CRUD: Server status");
		assertEquals(0, Project.getCurrent().revisions.size());

		IConnection conn = ConnectionUtils.getCurrent("db1");
		conn.runStatusCheck();
		Thread.sleep(SLEEP_TIMEOUT);
		Assert.assertNotNull(conn.getServerStatus());
	}
}
