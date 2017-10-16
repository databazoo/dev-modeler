
package com.databazoo.devmodeler.tools.optimizer;

import com.databazoo.components.UIConstants;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;

/**
 * Provides optimization suggestions on the schema.
 *
 * @author bobus
 */
public class StaticFlawOptimizer extends AbstractOptimizer {

	private static final String L_TABLE = "Table ";
	private static final String L_HAS = " has ";

	private Rules rules = new Rules();

	public void analyzeReferences () {
		for(DB db : databases){
			for(Schema s : db.getSchemas()){
				for(Relation r : s.getRelations()){
					rules.checkNoReferenceOnId(r);
					rules.check2wayReferences(r);
				}
			}
		}
	}

	public void analyzeIndexes () {
		for(DB db : databases){
			for(Schema s : db.getSchemas()){
				for(Relation r : s.getRelations()){
					rules.checkIndexesCount(r);
					rules.checkUniqueOnTimeColumn(r);
					rules.checkUniqueOnNullColumn(r);
					rules.checkIndexesOnReferences(r);
					rules.checkDuplicateIndexes(r);
				}
			}
		}
	}

	public void analyzePrimaryKeys () {
		for(DB db : databases){
			for(Schema s : db.getSchemas()){
				for(Relation r : s.getRelations()){
					rules.checkPrimaryKeyExists(r);
					rules.checkPrimaryKeyOnTimeColumn(r);
					rules.checkPrimaryKeyOnFirstColumn(r);
				}
			}
		}
	}

	public void analyzeAttributes () {
		for(DB db : databases){
			for(Schema s : db.getSchemas()){
				for(Relation r : s.getRelations()){
					rules.checkAttributesCount(r);
				}
			}
		}
	}

	public void analyzeTriggers () {
		for(DB db : databases){
			for(Schema s : db.getSchemas()){
				for(Relation r : s.getRelations()){
					rules.checkTriggersCount(r);
				}
			}
		}
	}

	private class Rules {

		/**
		 * Check whether the relation has a primary key defined.
		 *
		 * @param r relation to check
		 */
		private void checkPrimaryKeyExists (Relation r) {
			for(Index i : r.getIndexes()){
				if(i.getBehavior().isPrimary()){
					return;
				}
			}
			addFlaw(new ModelFlaw(r, ModelFlaw.L_SEVERITY_ERROR, "Missing primary key", "Tables should always use primary keys to uniquely identify rows in the table. "
					+ "Tables without primary keys are carrying great performance risks."));
		}

		/**
		 * Check whether the relation has an acceptable index count.
		 *
		 * @param r relation to check
		 */
		private void checkIndexesCount (Relation r) {
			if(r.getIndexes().size() >= Relation.INDEX_COUNT_LIMIT){
				addFlaw(new ModelFlaw(r, ModelFlaw.L_SEVERITY_WARNING, "Too many indexes", L_TABLE + elementName(r.getName()) + L_HAS +
						value(r.getIndexes().size()) + " indexes. Data manipulation on tables with too many indexes may be slow."));
			}
		}

		/**
		 * Check whether the relation has an acceptable trigger count.
		 *
		 * @param r relation to check
		 */
		private void checkTriggersCount (Relation r) {
			if(r.getTriggers().size() >= Relation.TRIGGER_COUNT_LIMIT){
				addFlaw(new ModelFlaw(r, ModelFlaw.L_SEVERITY_WARNING, "Too many triggers", L_TABLE + elementName(r.getName()) + L_HAS +
						value(r.getTriggers().size()) + " triggers. Data manipulation on tables with too many triggers may be slow."));
			}
		}

		/**
		 * Check whether the relation has an acceptable attribute count.
		 *
		 * @param r relation to check
		 */
		private void checkAttributesCount (Relation r) {
			if(r.getAttributes().size() >= Relation.ATTRIBUTE_COUNT_LIMIT){
				addFlaw(new ModelFlaw(r, ModelFlaw.L_SEVERITY_WARNING, "Too many columns", L_TABLE + elementName(r.getName()) + L_HAS +
						value(r.getAttributes().size()) + " columns. Tables with too many columns have inefficient storage and may be performing slow."));
			}
			if(r.getAttributes().size() < 2){
				addFlaw(new ModelFlaw(r, ModelFlaw.L_SEVERITY_WARNING, "Useless table", L_TABLE + elementName(r.getName()) + L_HAS +
						(r.getAttributes().isEmpty() ? "no columnt" : "only 1 column")+". Tables with so few attributes are generally useless."));
			}
		}

		/**
		 * Check whether the relation does not have a primary key on time-based column.
		 *
		 * @param r relation to check
		 */
		private void checkPrimaryKeyOnTimeColumn (Relation r) {
			for(int i=0; i < r.getPkCols().length; i++){
				if(r.getPkCols()[i] > 0){
					for(Attribute attr: r.getAttributes()){
						if(attr.getAttNum() == r.getPkCols()[i]){
							if(attr.getBehavior().getAttType().matches(".*(Time|time|TIME).*")){
								addFlaw(new ModelFlaw(r, ModelFlaw.L_SEVERITY_ERROR, "Primary key on time column", L_TABLE + elementName(r.getName()) +
										" has primary key defined on time column. Inserting data into such table will create duplicities under heavy load."));
								return;
							}
						}
					}
				}
			}
		}

		/**
		 * Check whether the relation does not have a unique key on time-based column.
		 *
		 * @param r relation to check
		 */
		private void checkUniqueOnTimeColumn (Relation r) {
			for(Index index : r.getIndexes()){
				if(index.getBehavior().isUnique() && !index.getBehavior().isPrimary()){
					for(Attribute attr: index.getAttributes()){
						if(attr.getBehavior().getAttType().matches(".*(Time|time|TIME).*")){
							addFlaw(new ModelFlaw(r, ModelFlaw.L_SEVERITY_ERROR, "Unique key on time column", L_TABLE + elementName(r.getName()) +
									L_HAS + elementName(index.getName()) + " unique key defined on time column. Inserting data into such table "
									+ "will create duplicities under heavy load."));
							break;
						}
					}
				}
			}
		}

		/**
		 * Check all foreign keys are covered by an index.
		 *
		 * @param r relation to check
		 */
		private void checkIndexesOnReferences (Relation r) {
			for(Constraint c : r.getConstraints()){
				if(c.getRel1() == r && c.getAttr1() != null){

					// Find an index for constraint
					boolean found = false;
					for(Index i : r.getIndexes()){
						if(i.getAttributes().contains(c.getAttr1())){
							found = true;
							break;
						}
					}
					if(!found){
						addFlaw(new ModelFlaw(r, ModelFlaw.L_SEVERITY_WARNING, "Foreign key not covered by index", L_TABLE + elementName(r.getName()) +
								L_HAS + elementName(c.getName()) + " foreign key that is not covered by any index. Data manipulation on parent table may be slow."));
					}
				}
			}
		}

		/**
		 * Check for columns named `something_id` without a foreign key.
		 *
		 * @param r relation to check
		 */
		private void checkNoReferenceOnId (Relation r) {
			Index pk = null;
			for(Index index : r.getIndexes()){
				if(index.getBehavior().isPrimary()){
					pk = index;
					break;
				}
			}
			for(Attribute a : r.getAttributes()){

				// Skip if is part of primary key
				if(pk != null && !pk.getAttributes().contains(a)){

					// Name contains ID
					String aName = a.getName();
					if (aName.startsWith("ID_") || aName.startsWith("id_") ||
							aName.endsWith("_ID") || aName.endsWith("_id") || aName.endsWith("Id")){

						// Find a FK
						boolean found = false;
						for(Constraint c : r.getConstraints()){
							if(c.getRel1() == r && c.getAttr1() != null && c.getAttr1() == a){
								found = true;
								break;
							}
						}
						if(!found){
							addFlaw(new ModelFlaw(r, ModelFlaw.L_SEVERITY_WARNING, "ID column has no foreign key", L_TABLE + elementName(r.getName()) +
									L_HAS + elementName(a.getName()) + " column with no foreign key. There should be a foreign key constraint defined."));
						}
					}else if (aName.startsWith("ID") || aName.startsWith("id") || aName.startsWith("Id") ||
							aName.endsWith("ID") || aName.endsWith("id") || aName.endsWith("Id")){

						// Find a FK
						boolean found = false;
						for(Constraint c : r.getConstraints()){
							if(c.getRel1() == r && c.getAttr1() != null && c.getAttr1() == a){
								found = true;
								break;
							}
						}
						if(!found){
							addFlaw(new ModelFlaw(r, ModelFlaw.L_SEVERITY_NOTICE, "ID column has no foreign key", L_TABLE + elementName(r.getName()) +
									L_HAS + elementName(a.getName()) + " column with no foreign key. There could be a foreign key constraint defined."));
						}
					}
				}
			}
		}

		/**
		 * Check for existence of back and forth foreign keys.
		 *
		 * @param r relation to check
		 */
		private void check2wayReferences(Relation r) {
			for(Constraint c : r.getConstraints()){

				// Skip self-references
				if(c.getRel1() == r && c.getAttr1() != null && c.getRel1() != c.getRel2()){

					// Find a backward FK
					Relation rel2 = (Relation) c.getRel2();
					for(Constraint c2 : rel2.getConstraints()){
						if(c2.getRel2() == r){
							addFlaw(new ModelFlaw(r, ModelFlaw.L_SEVERITY_WARNING, "Bi-directional reference", "There is a 2-way reference between " +
									elementName(r.getName()) + " and " + elementName(rel2.getName()) +
									". 2-way references make models too complicated and are a sign of poor design."));
							break;
						}
					}
				}
			}
		}

		/**
		 * Check for indexes operation on same columns.
		 *
		 * @param r relation to check
		 */
		private void checkDuplicateIndexes(Relation r) {
			for(Index index : r.getIndexes()){
				for(Index index2 : r.getIndexes()){
					if(index != index2 && index.getAttributes().containsAll(index2.getAttributes()) && index2.getAttributes().containsAll(index.getAttributes())){
						addFlaw(new ModelFlaw(r, ModelFlaw.L_SEVERITY_WARNING, "Duplicate indexes", L_TABLE + elementName(r.getName()) +
								" has indexes " + elementName(index.getName()) + " and " + elementName(index2.getName()) +
								" operating on same columns. One of the indexes should be removed."));
						break;
					}
				}
			}
		}

		/**
		 * Check whether the relation does not have a unique key on nullable column.
		 *
		 * @param r relation to check
		 */
		private void checkUniqueOnNullColumn (Relation r) {
			for(Index index : r.getIndexes()){
				if(index.getBehavior().isUnique()){
					for(Attribute attr: index.getAttributes()){
						if(attr.getBehavior().isAttNull()){
							addFlaw(new ModelFlaw(r, ModelFlaw.L_SEVERITY_WARNING, "Unique key on NULL column", L_TABLE +
									elementName(r.getName()) + L_HAS + elementName(index.getName()) + " unique key defined on nullable column " +
									elementName(attr.getName()) + ". NULL values may easily result in duplicities."));
							break;
						}
					}
				}
			}
		}

		/**
		 * Check for PK columns that are not on the beginning of the table.
		 *
		 * @param r relation to check
		 */
		private void checkPrimaryKeyOnFirstColumn(Relation r) {
			for(Index index : r.getIndexes()){
				if(index.getBehavior().isPrimary()){
					boolean found = false;
					for(Attribute attr: index.getAttributes()){
						if(attr.getAttNum() == 1){
							found = true;
							break;
						}
					}
					if(!found) {
						addFlaw(new ModelFlaw(r, ModelFlaw.L_SEVERITY_NOTICE, "Primary key not on first column", L_TABLE + elementName(r.getName()) +
								" has primary key defined in the middle of the table. PK column should be moved to the beginning."));
					}
				}
			}
		}

		private String elementName(String name) {
			return "<font color=\"#" + Integer.toHexString(UIConstants.COLOR_GREEN.getRGB()).substring(2) + "\">" + name + "</font>";
		}

		private String value(int val) {
			return "<font color=\"#" + Integer.toHexString(UIConstants.COLOR_AMBER.getRGB()).substring(2) + "\">" + val + "</font>";
		}
	}

}
