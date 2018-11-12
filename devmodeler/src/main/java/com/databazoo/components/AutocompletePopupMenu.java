
package com.databazoo.components;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.databazoo.devmodeler.config.Config;
import com.databazoo.tools.Schedule;

/**
 * Popup menu with automatic placement around requested location, automatic close on blur and single instance.
 *
 * @author bobus
 */
public class AutocompletePopupMenu extends JWindow {

	private static AutocompletePopupMenu instance;
	private static final int maxScreenHeight = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight();

	/**
	 * Get the menu
	 *
	 * @return AutocompletePopupMenu
	 */
	public static synchronized AutocompletePopupMenu get(){
		if(instance == null){
			instance = new AutocompletePopupMenu();
		}
		return instance;
	}

	/**
	 * Is menu shown?
	 *
	 * @return is shown?
	 */
	public static synchronized boolean isShown(){
		return instance != null && instance.isVisible();
	}

	/**
	 * Is menu shown and one of options is selected?
	 *
	 * @return is shown and selected?
	 */
	public static synchronized boolean isShownAndSelected(){
		return instance != null && instance.isVisible() && instance.selectedRow >= 0;
	}

	/**
	 * Dispose of any left menus.
	 */
	public static synchronized void disposeNow(){
		if(instance != null){
			instance.dispose();
		}
	}

	private int selectedRow;
	JMenuItem selectedItem;

	/**
	 * Constructor
	 */
	private AutocompletePopupMenu(){
		super();
		setVisible(false);
		setAlwaysOnTop(true);
	}

	/**
	 * Remove instance pointer
	 */
	@Override
	public void dispose(){
		instance = null;
		super.dispose();
	}

	private void prepareVisual() {
		if(!isVisible()){
			getContentPane().setLayout(new GridLayout(0,1));
			getContentPane().setBackground(Color.WHITE);
			((JComponent)getContentPane()).setBorder(new CompoundBorder(new LineBorder(Color.GRAY), new EmptyBorder(10, 2, 10, 2)));
			setVisible(true);
		}
		pack();
	}

	/**
	 * Display menu at given location
	 *
	 * @param x location X
	 * @param y location Y
	 */
	public void draw(final int x, final int y){
		Schedule.inEDT(() ->{
				prepareVisual();
				int newY = y;
				if(y + getHeight() > maxScreenHeight){
					newY -= getHeight() + 20;
				}
				setLocation(x, newY);
			}
		);
	}

	/**
	 * Display menu at given center location
	 *
	 * @param x location X
	 * @param y location Y
	 */
	public void drawAboveCenter(final int x, final int y){
		prepareVisual();
		int newY = y - getHeight();
		if(newY < 0){
			newY += getHeight() + 20;
		}
		setLocation(x-getWidth()/2, newY);
	}

	/**
	 * Add an option to menu
	 *
	 * @param item menu item
	 */
	public void add(JMenuItem item){
		if(item.getText().length() > Config.CONTEXT_ITEM_MAX_LENGTH){
			item.setText(item.getText().substring(0, Config.CONTEXT_ITEM_MAX_LENGTH)+"...");
		}
		getContentPane().add(item);
	}

	/**
	 * Remove all options
	 */
	public void clear(){
		getContentPane().removeAll();
		selectedRow = -1;
	}

	/**
	 * Add separator
	 */
	public void addSeparator(){
		getContentPane().add(new Separator());
	}

	/**
	 * Process keys
	 */
	public void processKeyUp(){
		if(getContentPane().getComponentCount() == 0){
			dispose();
			return;
		} else if(selectedRow <= 0){
			selectedRow = getContentPane().getComponentCount()-1;
		} else {
			selectedRow--;
		}
		Component comp = getContentPane().getComponent(selectedRow);
		if(comp instanceof JMenuItem){
			if(selectedItem != null) {
				selectedItem.menuSelectionChanged(false);
			}
			JMenuItem item = (JMenuItem) comp;
			item.menuSelectionChanged(true);
			selectedItem = item;
		}else{
			processKeyUp();
		}
	}

	/**
	 * Process keys
	 */
	public void processKeyDown(){
		if(getContentPane().getComponentCount() == 0){
			dispose();
			return;
		} else if(selectedRow >= getContentPane().getComponentCount()-1){
			selectedRow = 0;
		} else {
			selectedRow++;
		}
		Component comp = getContentPane().getComponent(selectedRow);
		if(comp instanceof JMenuItem){
			if(selectedItem != null) {
				selectedItem.menuSelectionChanged(false);
			}
			JMenuItem item = (JMenuItem) comp;
			item.menuSelectionChanged(true);
			selectedItem = item;
		}else{
			processKeyDown();
		}
	}

	/**
	 * Process keys
	 */
	public void processKeyEnter(){
		if(selectedRow > -1){
			for(ActionListener listener: selectedItem.getActionListeners()){
				listener.actionPerformed(new ActionEvent(instance, 0, selectedItem.getText()));
			}
		}
	}
}
