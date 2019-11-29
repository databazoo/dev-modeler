
package com.databazoo.devmodeler.config;

import com.databazoo.components.UIConstants;
import com.databazoo.devmodeler.DevModeler;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.InputDialog;
import com.databazoo.devmodeler.gui.OperationCancelException;
import com.databazoo.devmodeler.gui.window.Splash;
import com.databazoo.devmodeler.project.ProjectManager;
import com.databazoo.devmodeler.wizards.IntroWizard;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.EncryptedProperties;
import com.databazoo.tools.RestClient;
import com.databazoo.tools.Schedule;
import us.monoid.web.JSONResource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Global application config.
 *
 * @author bobus
 */
public class Config {
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(Settings.getStr(Settings.L_LOCALIZATION_DATE));
	public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat(Settings.getStr(Settings.L_LOCALIZATION_DATE) + " " + Settings.getStr(Settings.L_LOCALIZATION_TIME));
	//public static SimpleDateFormat timeFormat = new SimpleDateFormat (Settings.getStr(Settings.L_LOCALIZATION_TIME));

	public static final String EDITION_BETA = "RC";
	public static final String EDITION_COMMUNITY = "Community Edition";

	private static final String LAST_OPEN = "lastOpen";
	private static final String PROJECT0 = "project0";

	public static final int DB_TREE_SPLIT_WIDTH = 136;
	public static final int WARNING_OVERFLOW_LIMIT = 256 * 1024;
	public static final int FORMATTER_OVERFLOW_LIMIT = 40 * 1024;
	public static final int FORMATTER_OVERFLOW_LIMIT2 = 20 * 1024;

	public static final int STATUS_ROW_TTL = 7;
	public static final int STATUS_ROW_TTL2 = 0;            // 0=show gray 4=split gray/light gray
	public static final int STATUS_SLOW_QUERY = 5;
	public static final boolean SAVE_REVERTS_FOR_INDIRECT = true;
	public static final boolean SHOW_LARGE_ICONS_IN_PROJECT = false;

	public static final int INFO_PANEL_MAX_LABELS = 40;
	public static final int INFO_PANEL_TIMEOUT_OK = 4;
	public static final int INFO_PANEL_TIMEOUT_FAIL = 10;
	public static final int INFO_PANEL_TIMEOUT_WAIT = 20;
	public static final int WIZARD_FAIL_LIMIT = 10;
	public static final int SCROLL_THRESHOLD = 200;
	public static final int MIN_QUERY_INPUT_SIZE = 92;
	public static final int MAX_QUERY_INPUT_SIZE = 450;
	public static final int CONTEXT_ITEM_MAX_LENGTH = 100;
	public static final int DELETE_WINDOW_MAX_ROWS = 25;
	public static final int DELETE_WINDOW_MAX_WIDTH = 1000;
	public static final int DATA_COL_WIDTH_CHAR_LIMIT = 600;
	public static final int VIEW_DIVIDER_LOCATION = 285;

	private static String pwrd;
	public static String APP_VERSION = UIConstants.getAppVersion().replace("-DEBUG", "");
	public static final String APP_NAME_BASE = UIConstants.getProperty("app.name");
	public static String APP_NAME = "";
	public static final String APP_DEFAULT_URL = UIConstants.getProperty("app.url");
	public static final String APP_COPYRIGHT = UIConstants.getProperty("app.copyright");

	/**
	 * Check password, licence, app name and new version.
	 */
	public static void init() {
		setVersionWithEnvironment();
		getPassword();
		resetAppName();
		checkJavaVersion();
		if (!UIConstants.DEBUG) {
			getNewVersion();
		}
		UIConstants.PROPERTIES = null;
	}

	private static void setVersionWithEnvironment() {
		UIConstants.setVersionWithEnvironment("v" + APP_VERSION + " on " + UIConstants.getJREVersion());
	}

	private static void checkJavaVersion() {
		Schedule.inWorker(2000, () -> {
            if(System.getProperty("java.version").contains("1.7")){
                DesignGUI.getInfoPanel().writeRed("Java 7 is outdated. Please upgrade to newer Java Runtime Environment.");
            }
        });
	}

	/**
	 * Check password
	 *
	 * @return password
	 */
	public static String getPassword() {
		if (pwrd == null) {
			if (isApplicationStartedPreviously()) {
				pwrd = "";
				EncryptedProperties p = new EncryptedProperties(pwrd);
				try {
					p.load(new FileInputStream(ProjectManager.getSettingsDirectory(ProjectManager.CONFIG_FILE)));
					if (p.getInt(LAST_OPEN) <= 0 && p.getStr(PROJECT0) == null) {
						pwrd = "ABC";
						p = new EncryptedProperties(pwrd);
						p.load(new FileInputStream(ProjectManager.getSettingsDirectory(ProjectManager.CONFIG_FILE)));
						if (p.getInt(LAST_OPEN) <= 0 && p.getStr(PROJECT0) == null) {
							throw new IllegalAccessException("Empty password won't work");
						}
					}
				} catch (Exception e) {
					Dbg.notImportant("Default password was not used.", e);
					Splash.get().setVisible(false);
					while (true) {
						try {
							pwrd = InputDialog.askPassword("Password for " + APP_NAME_BASE + " is required", "Password for " + APP_NAME_BASE + ": ");
							p = new EncryptedProperties(pwrd);
							p.load(new FileInputStream(ProjectManager.getSettingsDirectory(ProjectManager.CONFIG_FILE)));
							if (p.getInt(LAST_OPEN) > 0 || p.getStr(PROJECT0) != null) {
								break;
							} else {
								Thread.sleep(500);
							}
						} catch (IOException | InterruptedException ex) {
							Dbg.notImportant("Not much we can do here.", ex);
						} catch (OperationCancelException ex) {
							DevModeler.exit(0);
						}
					}
					Splash.get().setVisible(true);
				}
			} else {
				pwrd = "";
			}
		}
		return pwrd;
	}

	public static boolean isApplicationStartedPreviously() {
		return ProjectManager.getSettingsDirectory(ProjectManager.CONFIG_FILE).isFile();
	}

	/**
	 * Update application name on license check.
	 */
	public static void resetAppName() {
		APP_NAME = APP_NAME_BASE + " " + APP_VERSION + " " + EDITION_COMMUNITY;
		setVersionWithEnvironment();
	}

	/**
	 * Check for new version.
	 */
	private static void getNewVersion() {
		Schedule.reInvokeInWorker(Schedule.Named.NEW_VERSION_CHECK, 3000, () -> {
            try {
                JSONResource json = RestClient.getInstance().getJSON("version", "generation", UIConstants.getAppVersion().substring(0, 3));
                String availableVersion = json.get("available").toString();
                if (!availableVersion.equals(APP_VERSION)) {
                    if (DesignGUI.getInfoPanel() != null) {
                        DesignGUI.getInfoPanel().writeBlue("New version " + availableVersion + " is available");
                    }
                }
            } catch (Exception e) {
                Dbg.notImportant("This is a background check. Terminating silently.", e);
            }
        });
	}

	public static String getPwrd() {
		return pwrd;
	}

	public static void setPwrdToDefault() {
		Config.pwrd = "ABC";
	}
}
