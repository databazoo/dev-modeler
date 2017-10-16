
package com.databazoo.devmodeler.model.reference;

import com.databazoo.components.icons.IIconable;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.model.Workspace;

/**
 *
 * @author bobus
 */
public interface IReferenceElement extends IIconable {
	IModelElement getElement();
	Workspace getWorkspace();
	void unSelect();
	void drop();
}
