
package com.databazoo.components.combo;

import java.awt.*;

import javax.swing.*;

/**
 * Combobox cell renderer that allows a separator to be added automatically after certain value.
 */
public abstract class ComboSeparatorsRenderer implements ListCellRenderer<String>{
    private final ListCellRenderer<? super String> delegate;
    private final JPanel separatorPanel = new JPanel(new BorderLayout());
    private final JSeparator separator = new JSeparator();

	/**
	 * Constructor
	 *
	 * @param delegate renderer delegate
	 */
    protected ComboSeparatorsRenderer(ListCellRenderer<? super String> delegate){
        this.delegate = delegate;
		separatorPanel.setBackground(Color.WHITE);
    }

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
    public Component getListCellRendererComponent(JList list, String value, int index, boolean isSelected, boolean cellHasFocus){
        Component comp = delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if(index!=-1 && addSeparatorAfter(list, value, index)){
			if(!isSelected){
				comp.setBackground(Color.WHITE);
			}
            separatorPanel.removeAll();
            separatorPanel.add(comp, BorderLayout.CENTER);
            separatorPanel.add(separator, BorderLayout.SOUTH);
            return separatorPanel;
        }else{
			return comp;
		}
    }

	/**
	 * Add separator now?
	 *
	 * @param list The JList we're painting.
	 * @param value The value returned by list.getModel().getElementAt(index).
	 * @param index The cells index.
	 * @return Add separator now?
	 */
    protected abstract boolean addSeparatorAfter(JList list, Object value, int index);
}
