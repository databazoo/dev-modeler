
package com.databazoo.components.icons;

import javax.swing.*;

/**
 * Objects implementing this interface can return a 16x16 icon.
 *
 * @author bobus
 */
@FunctionalInterface
public interface IIconable {

	/**
	 * Get a 16x16 icon.
	 *
	 * @return icon
	 */
	public Icon getIcon16();
}
