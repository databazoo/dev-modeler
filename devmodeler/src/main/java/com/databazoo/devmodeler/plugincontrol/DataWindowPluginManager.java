
package com.databazoo.devmodeler.plugincontrol;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.databazoo.tools.Dbg;
import plugins.IDataWindowPlugin;
import plugins.api.IDataWindow;

/**
 *
 * @author bobus
 */
public class DataWindowPluginManager extends PluginManager {

	private static final Map<IDataWindow,ArrayList<IDataWindowPlugin>> cache = new HashMap<>();

	public static void init(IDataWindow instance){
		try {
			ArrayList<IDataWindowPlugin> list = new ArrayList<>();
			cache.put(instance, list);
			for(IDataWindowPlugin plugin: dataWindowPlugins){
				IDataWindowPlugin localPlugin = plugin.clone();
				localPlugin.init(instance);
				list.add(localPlugin);
			}
		} catch (Exception e){
			Dbg.notImportant(Dbg.PLUGIN_MAY_NOT_AFFECT_APP, e);
		}
	}
	public static int getPluginCount(){
		return dataWindowPlugins.size();
	}
	public static Component getWindowComponent(IDataWindow instance, int index){
		try {
			Component comp = cache.get(instance).get(index).getWindowComponent();
			if(comp != null){
				comp.setMinimumSize(new Dimension(20,20));
			}
			return comp;
		} catch (Exception e){
			Dbg.notImportant(Dbg.PLUGIN_MAY_NOT_AFFECT_APP, e);
			return null;
		}
	}
	public static Component getTabComponent(IDataWindow instance, int index){
		try {
			return cache.get(instance).get(index).getTabComponent();
		} catch (Exception e){
			Dbg.notImportant(Dbg.PLUGIN_MAY_NOT_AFFECT_APP, e);
			return null;
		}
	}
	public static String getPluginName(IDataWindow instance, int index){
		try {
			return cache.get(instance).get(index).getPluginName();
		} catch (Exception e){
			Dbg.notImportant(Dbg.PLUGIN_MAY_NOT_AFFECT_APP, e);
			return null;
		}
	}
	public static String getPluginDescription(IDataWindow instance, int index){
		try {
			return cache.get(instance).get(index).getPluginDescription();
		} catch (Exception e){
			Dbg.notImportant(Dbg.PLUGIN_MAY_NOT_AFFECT_APP, e);
			return null;
		}
	}
	public static ImageIcon getPluginIcon(IDataWindow instance, int index){
		try {
			return cache.get(instance).get(index).getPluginIcon();
		} catch (Exception e){
			Dbg.notImportant(Dbg.PLUGIN_MAY_NOT_AFFECT_APP, e);
			return null;
		}
	}

	public static void onQueryChange(IDataWindow instance){
		try {
			for(IDataWindowPlugin plugin: cache.get(instance)){
				plugin.onQueryChange();
			}
		} catch (Exception e){
			Dbg.notImportant(Dbg.PLUGIN_MAY_NOT_AFFECT_APP, e);
		}
	}
	public static void onQueryRun(IDataWindow instance){
		try {
			for(IDataWindowPlugin plugin: cache.get(instance)){
				plugin.onQueryRun();
			}
		} catch (Exception e){
			Dbg.notImportant(Dbg.PLUGIN_MAY_NOT_AFFECT_APP, e);
		}
	}
	public static void onQueryExplain(IDataWindow instance){
		try {
			for(IDataWindowPlugin plugin: cache.get(instance)){
				plugin.onQueryExplain();
			}
		} catch (Exception e){
			Dbg.notImportant(Dbg.PLUGIN_MAY_NOT_AFFECT_APP, e);
		}
	}
	public static void onQueryResult(IDataWindow instance){
		try {
			for(IDataWindowPlugin plugin: cache.get(instance)){
				plugin.onQueryResult();
			}
		} catch (Exception e){
			Dbg.notImportant(Dbg.PLUGIN_MAY_NOT_AFFECT_APP, e);
		}
	}
	public static void onQueryFail(IDataWindow instance){
		try {
			for(IDataWindowPlugin plugin: cache.get(instance)){
				plugin.onQueryFail();
			}
		} catch (Exception e){
			Dbg.notImportant(Dbg.PLUGIN_MAY_NOT_AFFECT_APP, e);
		}
	}
	public static void onWindowClose(IDataWindow instance){
		try {
			for(IDataWindowPlugin plugin: cache.get(instance)){
				plugin.onWindowClose();
			}
		} catch (Exception e){
			Dbg.notImportant(Dbg.PLUGIN_MAY_NOT_AFFECT_APP, e);
		}
		cache.remove(instance);
	}
}
