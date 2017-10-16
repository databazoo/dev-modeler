
package com.databazoo.devmodeler.tools.optimizer;

import com.databazoo.devmodeler.model.IModelElement;

/**
 *
 * @author bobus
 */
public class ModelFlaw {

	public static final String L_SEVERITY_ERR_WARN	= "Error, warning";
	public static final String L_SEVERITY_ERROR		= "Error";
	public static final String L_SEVERITY_WARNING	= "Warning";
	public static final String L_SEVERITY_NOTICE	= "Notice";

	public final IModelElement element;
	public final String severity;
	public final String title;
	public final String description;
	public final String descriptionWoColors;
	public final String forwardSQL;
	public final String backwardSQL;

	ModelFlaw (IModelElement element, String severity, String title, String description) {
		this(element, severity, title, description, null, null);
	}


	ModelFlaw (IModelElement element, String severity, String title, String description, String forwardSQL, String backwardSQL) {
		this.element = element;
		this.severity = severity;
		this.title = title;
		this.description = "<html>" + description + "</html>";
		this.descriptionWoColors = this.description.replaceAll("font[^>]*", "u");
		this.forwardSQL = forwardSQL;
		this.backwardSQL = backwardSQL;
	}


}
