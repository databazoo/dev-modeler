
package com.databazoo.devmodeler.gui;

import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author bobus
 */
public class RightClickMenu extends JPopupMenu implements java.awt.event.ActionListener {
	public static final Icon ICO_NEW = Theme.getSmallIcon(Theme.ICO_CREATE_NEW);
	public static final Icon ICO_EDIT = Theme.getSmallIcon(Theme.ICO_EDIT);
	public static final Icon ICO_COPY = Theme.getSmallIcon(Theme.ICO_COPY);
	public static final Icon ICO_DATA = Theme.getSmallIcon(Theme.ICO_DATA);
	public static final Icon ICO_RUN = Theme.getSmallIcon(Theme.ICO_RUN);
	public static final Icon ICO_VACUUM = Theme.getSmallIcon(Theme.ICO_VACUUM);
	public static final Icon ICO_DELETE = Theme.getSmallIcon(Theme.ICO_DELETE);

	public static final Icon ICO_FILTER = Theme.getSmallIcon(Theme.ICO_FILTER);
	public static final Icon ICO_ORDER = Theme.getSmallIcon(Theme.ICO_SORT);
	public static final Icon ICO_SQL = Theme.getSmallIcon(Theme.ICO_SQL_WINDOW);

	private static Point location;
	private static Component locationTo;
	public static RightClickMenu get(ActionListener listener){
		return new RightClickMenu(location, listener);
	}

	private final transient ActionListener actionListener;
	private final Map<String,Integer> actionMap = new HashMap<>();

	private RightClickMenu(Point location, ActionListener listener) {
		super();
		setLocation(location);
		this.actionListener = listener;

		final int x = location.x;
		final int y = location.y;

		Schedule.inEDT(() -> show(locationTo, x, y));
	}

	public static synchronized void setLocationTo(Component comp, Point correction){
		locationTo = comp;
		location = correction;
	}

	public RightClickMenu addItem(String name, int actionType){
		if(name.length() > Config.CONTEXT_ITEM_MAX_LENGTH){
			name = name.substring(0, Config.CONTEXT_ITEM_MAX_LENGTH)+"...";
		}

		JMenuItem item = new JMenuItem(name);
		item.addActionListener(this);
		add(item);

		actionMap.put(name, actionType);
		return this;
	}

	public RightClickMenu addItem(String name, Icon ico, int actionType){
		if(name.length() > Config.CONTEXT_ITEM_MAX_LENGTH){
			name = name.substring(0, Config.CONTEXT_ITEM_MAX_LENGTH)+"...";
		}

		JMenuItem item = new JMenuItem(name, ico);
		item.addActionListener(this);
		add(item);

		actionMap.put(name, actionType);
		return this;
	}

	public RightClickMenu addItem(String name, Icon ico, int actionType, String[] submenuValues){
		if(name.length() > Config.CONTEXT_ITEM_MAX_LENGTH){
			name = name.substring(0, Config.CONTEXT_ITEM_MAX_LENGTH)+"...";
		}

		actionMap.put(name, actionType);
		boolean addSeparator = false;
		JMenu submenu = new JMenu(name);
		submenu.setIcon(ico);
		for(String val : submenuValues){
			if(val.startsWith("Create ")){
				addSeparator = true;
			}else if(val.equals("|")){
				addSeparator = true;
				continue;
			}else if(addSeparator){
				submenu.addSeparator();
				addSeparator = false;
			}
			JMenuItem item = new JMenuItem(val, ico);
			item.addActionListener(this);
			submenu.add(item);
			actionMap.put(val, actionType);
		}
		add(submenu);

		return this;
	}

	public RightClickMenu separator(){
		addSeparator();
		return this;
	}

	@Override
	public void actionPerformed(final ActionEvent ae) {
		actionListener.executeMenuAction(actionMap.get(ae.getActionCommand()), ae.getActionCommand());
	}

	@FunctionalInterface
	public interface ActionListener {
		void executeMenuAction(final int type, final String selectedValue);
	}
}
