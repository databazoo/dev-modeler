
package com.databazoo.devmodeler.model;

import java.io.Serializable;

/**
 * Storage of all model elements' properties comply with this interface.
 *
 * @author bobus
 */
public interface IModelBehavior<T extends IModelBehavior> extends Serializable {

    T prepareForEdit();
	T getValuesForEdit();
	void setValuesForEdit(T behavior);
	void notifyChange(String elementName, String value);
	void notifyChange(String elementName, boolean value);
	void notifyChange(String elementName, boolean[] values);

	void saveEdited();
	boolean isDropped();
	void setDropped();
	void setNotDropped();
	boolean isNew();
	void setNew();
	void setNotNew();
}
