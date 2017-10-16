
package com.databazoo.devmodeler;

import com.databazoo.components.UIConstants;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.ConnectionUtils;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.UsageElement;
import com.databazoo.devmodeler.gui.window.Splash;
import com.databazoo.devmodeler.plugincontrol.PluginManager;
import com.databazoo.devmodeler.project.ProjectManager;
import com.databazoo.devmodeler.wizards.SettingsWizard;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;
import com.databazoo.tools.Usage;

/**
 * Main class
 *
 * @author bobus
 */
public class DevModeler {

	/**
	 * main() method
	 *
	 * @param args command-line params
	 */
	public static void main(String[] args) {
		Thread.setDefaultUncaughtExceptionHandler(new Dbg.UncaughtExceptionHandler());
		Usage.log(UsageElement.MAIN_WINDOW_OPEN);
		new DevModeler().start();
	}

	public static void exit(int exitCode){
		DesignGUI.get().frame.dispose();
		Usage.log(UsageElement.MAIN_WINDOW_CLOSE, "Exit Code: " + exitCode);
		Usage.sendReport();
		System.exit(exitCode);
	}

	/**
	 * Run application
	 */
	private void start(){
		Splash splash = Splash.get();

		Settings.init();
		Config.init();
		PluginManager.init();

		Dbg.toFile("Config initialized");
		if(UIConstants.DEBUG){
			splash.dispose();
		}
		splash.partLoaded();

		DesignGUI.get().drawMainWindow();
		ProjectManager.getInstance().init(Settings.getBool(Settings.L_NOTICE_OPEN_PROJECT));
		ConnectionUtils.initConnectionChecker();
		splash.dispose();
	}
}
