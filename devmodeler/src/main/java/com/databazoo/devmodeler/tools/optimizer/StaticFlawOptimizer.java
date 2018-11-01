
package com.databazoo.devmodeler.tools.optimizer;

import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;

/**
 * Provides optimization suggestions on the schema.
 *
 * @author bobus
 */
public class StaticFlawOptimizer extends AbstractOptimizer {

	static final String L_TABLE = "Table ";
	static final String L_HAS = " has ";

	private StaticRules rules;

	public StaticFlawOptimizer() {
		rules = new StaticRules(this);
	}

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
					rules.checkPrimaryKeyWrongDatatype(r);
				}
			}
		}
	}

	public void analyzeAttributes () {
		for(DB db : databases){
			for(Schema s : db.getSchemas()){
				for(Relation r : s.getRelations()){
					rules.checkAttributesCount(r);
					rules.checkAttributeDefaultAndNull(r);
					rules.checkRepeatedAttributes(r);
					rules.checkReservedKeywords(r);
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

}
