
package com.databazoo.devmodeler.tools.optimizer;

import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;

/**
 *
 * @author bobus
 */
public class DataBasedOptimizer extends AbstractOptimizer {

	private Rules rules = new Rules();

	public void analyzeIndexes () {
		for(DB db : databases){
			for(Schema s : db.getSchemas()){
				for(Relation r : s.getRelations()){
					rules.checkIndexesUnused(r);
				}
			}
		}
	}

	public void analyzeAttributes () {
		for(DB db : databases){
			for(Schema s : db.getSchemas()){
				for(Relation r : s.getRelations()){
					rules.checkAttributesLesserTypes(r);
				}
			}
		}
	}

	public void analyzeUniqueData () {
		for(DB db : databases){
			for(Schema s : db.getSchemas()){
				for(Relation r : s.getRelations()){
					rules.checkUniqueData(r);
					rules.checkReference(r);
				}
			}
		}
	}

	public void analyzeEmptyValues () {
		for(DB db : databases){
			for(Schema s : db.getSchemas()){
				for(Relation r : s.getRelations()){
					rules.checkReferenceNotNull(r);
					rules.checkEmptyToNull(r);
				}
			}
		}
	}

	public void analyzeNumericValues () {
		for(DB db : databases){
			for(Schema s : db.getSchemas()){
				for(Relation r : s.getRelations()){
					rules.checkToBoolean(r);
					rules.checkColumnTypeOrId(r);
				}
			}
		}
	}

	private class Rules {
		public void checkIndexesUnused(Relation r) {

		}

		public void checkAttributesLesserTypes(Relation r) {

		}

		public void checkUniqueData(Relation r) {

		}

		public void checkReference(Relation r) {

		}

		public void checkReferenceNotNull(Relation r) {

		}

		public void checkEmptyToNull(Relation r) {

		}

		public void checkToBoolean(Relation r) {

		}

		public void checkColumnTypeOrId(Relation r) {

		}
	}
}
