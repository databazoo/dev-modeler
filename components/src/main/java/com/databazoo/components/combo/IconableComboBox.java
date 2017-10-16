
package com.databazoo.components.combo;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JComboBox with support for icons and colors.
 *
 * @author bobus
 */
public class IconableComboBox extends JComboBox<String> {
	private final transient List<Icon> icons = new ArrayList<>();
	private final List<Color> bgs = new ArrayList<>();
	private final List<Color> fgs = new ArrayList<>();

	/**
	 * Constructor
	 */
	public IconableComboBox(){
		super();
		setRenderer(new IconListRenderer());
	}

	/**
	 * Constructor
	 */
	public IconableComboBox(String[] items){
		super(items);
		setRenderer(new IconListRenderer());
	}

	/**
	 * Add an option to the combo
	 *
	 * @param itemName name for the option
	 */
	@Override
	public void addItem(String itemName){
		addItem(itemName, null, null, null);
	}

	/**
	 * Add an option to the combo
	 *
	 * @param itemName name for the option
	 * @param icon icon for the option
	 */
	public void addItem(String itemName, Icon icon){
		addItem(itemName, icon, null, null);
	}

	/**
	 * Add an option to the combo
	 *
	 * @param itemName name for the option
	 * @param icon icon for the option
	 * @param foreground color for the option
	 * @param background color for the option
	 */
	public void addItem(String itemName, Icon icon, Color foreground, Color background){
		super.addItem(itemName);
		icons.add(icon);
		fgs.add(foreground);
		bgs.add(background);
	}

	/**
	 * Select an element by partial name. Search can be done with a regular expression.
	 */
	public void setSelectedByNamePart(String regex){
		for(int i=0; i<getItemCount(); i++){
			Object item = getItemAt(i);
			if(item.toString().matches(regex)){
				setSelectedIndex(i);
				break;
			}
		}
	}

	/**
	 * Also remove all references
	 */
	@Override
	public void removeAllItems() {
		super.removeAllItems();
		icons.clear();
		fgs.clear();
		bgs.clear();
	}

	/**
	 * Icon renderer for the combobox
	 */
	private class IconListRenderer extends DefaultListCellRenderer {

		/**
		 * ListCellRenderer override
		 *
		 * @param list The JList we're painting.
		 * @param value The value returned by list.getModel().getElementAt(index).
		 * @param index The cells index.
		 * @param isSelected True if the specified cell was selected.
		 * @param cellHasFocus True if the specified cell has the focus.
		 * @return A component whose paint() method will render the specified value.
		 */
		@Override
		public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if(index < icons.size() && index >= 0){
				if(icons.get(index) != null){
					label.setIcon(icons.get(index));
				}
				if(bgs.get(index) != null){
					label.setBackground(bgs.get(index));
				}
				if(fgs.get(index) != null){
					label.setForeground(fgs.get(index));
				}
			}

			return label;
		}
	}
}
