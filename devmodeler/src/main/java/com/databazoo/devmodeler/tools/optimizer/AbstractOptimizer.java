
package com.databazoo.devmodeler.tools.optimizer;

import java.util.ArrayList;
import java.util.List;

import com.databazoo.devmodeler.model.DB;

/**
 *
 * @author bobus
 */
abstract class AbstractOptimizer {

	public final List<ModelFlaw> flaws = new ArrayList<>();
	protected final List<DB> databases = new ArrayList<>();
	private String severity = ModelFlaw.L_SEVERITY_ERR_WARN;

	public void setDatabases(List<DB> dbs){
		databases.clear();
		databases.addAll(dbs);
	}

	public void setDatabase(DB db){
		databases.clear();
		databases.add(db);
	}

	public void setSeverity (String severity) {
		this.severity = severity;
	}

	protected void addFlaw(ModelFlaw flaw){
		boolean errorOrWarning = severity.equals(ModelFlaw.L_SEVERITY_ERR_WARN) &&
								!flaw.severity.equals(ModelFlaw.L_SEVERITY_NOTICE);
		if(errorOrWarning || severity.equals(flaw.severity)){
			flaws.add(flaw);
		}
	}
}
