
package com.databazoo.devmodeler.tools.formatter;

import java.util.Arrays;

/**
 * Formatter implementation for Settings.
 *
 * @author bobus
 */
public class FormatterSettings extends FormatterBase {
	public FormatterSettings(){
		ELEMENT_NAMES.addAll(Arrays.asList("serial", "database", "schema", "table", "column", "uc_database", "uc_schema", "uc_table", "uc_column", "lc_database", "lc_schema", "lc_table", "lc_column"));
	}

}
