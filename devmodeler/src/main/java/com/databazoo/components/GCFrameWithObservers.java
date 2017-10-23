
package com.databazoo.components;

import com.databazoo.components.textInput.AutocompleteObserver;
import com.databazoo.components.textInput.QueryErrorPositionObserver;

/**
 * GCFrame that unregister observers on window close.
 *
 * @author bobus
 */
public class GCFrameWithObservers extends GCFrame {

	public GCFrameWithObservers(String fullName) {
		super(fullName);
	}

	/**
	 * Close subordinated windows, notify observers and invoke garbage collector
	 */
	@Override
	public void dispose(){
		super.dispose();
		AutocompleteObserver.unregister(this);
		QueryErrorPositionObserver.remove(this);
		AutocompletePopupMenu.get().dispose();
	}
}
