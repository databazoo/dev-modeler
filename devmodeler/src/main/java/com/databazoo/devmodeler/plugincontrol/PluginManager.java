
package com.databazoo.devmodeler.plugincontrol;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.databazoo.devmodeler.config.Settings;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;
import plugins.IDataWindowPlugin;

/**
 * Manages all plugin initialization and calls
 * @author bobus
 */
public class PluginManager {
	private static final String PLUGIN_DIR = "plugins";
	private static final String IFACE_DATA_WINDOW = "plugins.IDataWindowPlugin";

	static final List<IDataWindowPlugin> dataWindowPlugins = new ArrayList<>();
    private static PluginClassLoader classLoader;

    public static void init(){
        Schedule.inWorker(PluginManager::loadPlugins);
	}

	private static void loadPlugins(){
		File dir = new File(System.getProperty("user.dir"), PLUGIN_DIR);
        classLoader = new PluginClassLoader(dir);
		if (dir.exists() && dir.isDirectory()) {
			String[] files = dir.list();
			if (files != null) {
                Arrays.asList(files).forEach(PluginManager::loadClassByFileName);
			}
		} else {
			Dbg.info("Plugin dir "+dir.toString()+" is not available");
		}
	}

    private static void loadClassByFileName(String file) {
        if (file.endsWith(".class")) {
            try {
                loadDotClass(file);
            } catch (Exception ex) {
                Dbg.notImportant("File " + file + " does not contain a valid plugin class.", ex);
            }

        } else if (file.endsWith(".jar")) {
            try ( ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(file)) ) {
                loadDotJar(zipInputStream);
            } catch (Exception ex) {
                Dbg.notImportant("File " + file + " does not contain a valid plugin class.", ex);
            }
        }
    }

    private static void loadDotJar(ZipInputStream zipInputStream)
            throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        classLoader.setZipInputStream(zipInputStream);
        for (ZipEntry entry; (entry = zipInputStream.getNextEntry()) != null;) {
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                classLoader.setZipEntrySize(entry.getSize());
                loadClass(entry.getName().substring(entry.getName().lastIndexOf('/')));
            }
        }
    }

    private static void loadDotClass(String file) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        classLoader.setZipInputStream(null);
        loadClass(file);
    }

    private static void loadClass(String file) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class c = classLoader.loadClass(file.substring(0, file.indexOf('.')));
        Class[] interfaces = c.getInterfaces();
        for (Class interfaceClass : interfaces) {
            if (interfaceClass.getName().equals(IFACE_DATA_WINDOW)) {
                IDataWindowPlugin plugin = (IDataWindowPlugin) c.newInstance();
                String settingURI = "Global." + Settings.L_GLOBAL_PLUGINS + "." + plugin.getPluginName();
                if (Settings.getBool(settingURI)) {
                    dataWindowPlugins.add(plugin);
                    Settings.put(settingURI, true, plugin.getPluginDescription());
                } else {
                    Settings.put(settingURI, false, plugin.getPluginDescription());
                }
                break;
            }
        }
    }
}
