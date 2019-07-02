
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

	// ERROR

	public static ModelFlaw error(IModelElement element, String title, String description) {
		return error(element, title, description, null, null);
	}

	public static ModelFlaw error(IModelElement element, String title, String description, String forwardSQL, String backwardSQL) {
		return new ModelFlaw(element, L_SEVERITY_ERROR, title, description, forwardSQL, backwardSQL);
	}

	public static ModelFlaw error(IModelElement element, String title, String description, Runnable onClick) {
		return new ModelFlaw(element, L_SEVERITY_ERROR, title, description, onClick);
	}

	// WARNING

	public static ModelFlaw warning(IModelElement element, String title, String description) {
		return warning(element, title, description, null, null);
	}

	public static ModelFlaw warning(IModelElement element, String title, String description, String forwardSQL, String backwardSQL) {
		return new ModelFlaw(element, L_SEVERITY_WARNING, title, description, forwardSQL, backwardSQL);
	}

	public static ModelFlaw warning(IModelElement element, String title, String description, Runnable onClick) {
		return new ModelFlaw(element, L_SEVERITY_WARNING, title, description, onClick);
	}

	// NOTICE

	public static ModelFlaw notice(IModelElement element, String title, String description) {
		return notice(element, title, description, null, null);
	}

	public static ModelFlaw notice(IModelElement element, String title, String description, String forwardSQL, String backwardSQL) {
		return new ModelFlaw(element, L_SEVERITY_NOTICE, title, description, forwardSQL, backwardSQL);
	}

	public static ModelFlaw notice(IModelElement element, String title, String description, Runnable onClick) {
		return new ModelFlaw(element, L_SEVERITY_NOTICE, title, description, onClick);
	}

	public final IModelElement element;
	public final String severity;
	public final String title;
	public final String description;
	public final Runnable onClick;
	public final String descriptionWoColors;
	public final String forwardSQL;
	public final String backwardSQL;


	private ModelFlaw (IModelElement element, String severity, String title, String description, Runnable onClick) {
		this(element, severity, title, description, null, null, onClick);
	}

	private ModelFlaw (IModelElement element, String severity, String title, String description, String forwardSQL, String backwardSQL) {
		this(element, severity, title, description, forwardSQL, backwardSQL, null);
	}

	private ModelFlaw (IModelElement element, String severity, String title, String description, String forwardSQL, String backwardSQL, Runnable onClick) {
		this.element = element;
		this.severity = severity;
		this.title = title;
		this.description = "<html>" + description + "</html>";
		this.onClick = onClick;
		this.descriptionWoColors = this.description.replaceAll("font[^>]*", "u");
		this.forwardSQL = forwardSQL;
		this.backwardSQL = backwardSQL;
	}


}
