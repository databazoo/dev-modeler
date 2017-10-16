
package com.databazoo.devmodeler.tools.formatter;

import java.util.Arrays;

import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.tools.Geometry;

/**
 * Formatter implementation working with datatypes.
 *
 * @author bobus
 */
public class FormatterDataType extends FormatterBase {
	static final String[] EXTRA = new String[]{
			"varying",
			"double",
			"precision",
			"rowtype",
			"btree",
			"hash",
			"auto_increment",
			"identity",
			"true",
			"false",
			"now",
			"current_date",
			"current_timestamp",
			"zone"
	};
	public FormatterDataType(){
		BEGIN_CLAUSES.add( "default" );
		BEGIN_CLAUSES.add( "null" );
		if(Project.getCurrent() != null){
			DATATYPE_NAMES.addAll(Arrays.asList(
				Geometry.concat(
					EXTRA, Geometry.concat(
					Project.getCurrent().getCurrentConn().getDataTypes().getKeys(),
					Project.getCurrent().getCurrentConn().getDataTypes().getVals()
				))));
		}else{
			DATATYPE_NAMES.addAll(Arrays.asList(EXTRA));
		}
	}

}
