
package com.databazoo.components.icons;

import javax.swing.*;

import com.databazoo.devmodeler.config.Theme;

/**
 * Text node with an icon.
 *
 * @author bobus
 */
public class IconedPath implements IIconable {
	public static final Icon ICO_LIST = Theme.getSmallIcon(Theme.ICO_LIST);
	public static final Icon ICO_KEY = Theme.getSmallIcon(Theme.ICO_KEY);
	public static final Icon ICO_PLUGIN = Theme.getSmallIcon(Theme.ICO_PLUGIN);

	private final String name;
	private final Icon ico;

	/**
	 * Constructor
	 *
	 * @param name node name
	 * @param ico node icon
	 */
	public IconedPath(String name, Icon ico) {
		this.name = name;
		this.ico = ico;
	}

	/**
	 * Get node name.
	 *
	 * @return node name
	 */
	@Override
	public String toString(){
		return name;
	}

	/**
	 * Get a 16x16 icon.
	 *
	 * @return icon
	 */
	@Override
	public Icon getIcon16(){
		return ico;
	}
}
