package com.databazoo.devmodeler.config;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Theme processing
 */
public class Theme {
	public static final File THEME_DIR = new File(System.getProperty("user.dir"), "themes");    // TODO: correct path for MACs

	private static final int ICO_SIZE_SMALL		= 16;
	private static final int ICO_SIZE_MEDIUM	= 32;
	private static final int ICO_SIZE_BIGGER	= 48;
	private static final int ICO_SIZE_LARGE		= 64;

	private static final String ICO_EXT			= ".png";

	public static final String ICO_ABOUT_APP	= "info";
	public static final String ICO_APPROVED		= "approved";
	public static final String ICO_ARCHIVED		= "archive";
	public static final String ICO_BUG			= "bug";
	public static final String ICO_CANCEL		= "cancel";
	public static final String ICO_COPY			= "copy";
	public static final String ICO_CREATE_NEW	= "new";
	public static final String ICO_DATA			= "data";
	public static final String ICO_DB_TREE		= "db_tree";
	public static final String ICO_DELETE		= "delete";
	public static final String ICO_DIFFERENCE	= "diff";
	public static final String ICO_EDIT			= "edit";
	public static final String ICO_EXPLAIN		= "explain";
	public static final String ICO_EXIT			= "exit";
	public static final String ICO_EXPORT		= "export";
	public static final String ICO_FAVORITE_ADD = "fav_add";
	public static final String ICO_FAVORITE_REM = "fav_remove";
	public static final String ICO_FILTER		= "filter";
	public static final String ICO_GRID			= "grid";
	public static final String ICO_HISTORY		= "history";
	public static final String ICO_IMPORT		= "import";
	public static final String ICO_KEY			= "key";
	public static final String ICO_LIST			= "list";
	public static final String ICO_LOCKED		= "lock";
	public static final String ICO_LOGO			= "designer";
	public static final String ICO_ORGANIZE		= "organizer";
	public static final String ICO_ORG_ALPHABET	= "organizer_alphabetical";
	public static final String ICO_ORG_CIRCULAR	= "organizer_circular";
	public static final String ICO_ORG_FORCE	= "organizer_force";
	public static final String ICO_ORG_EXPLODE	= "organizer_explode";
	public static final String ICO_ORG_IMPLODE	= "organizer_implode";
	public static final String ICO_PAUSE		= "pause";
	public static final String ICO_PLUGIN		= "plugin";
	public static final String ICO_PROJECTS		= "projects";
	public static final String ICO_REVISION		= "revision";
	public static final String ICO_REVERT		= "revert";
	public static final String ICO_RUN			= "run";
	public static final String ICO_SAVE			= "save";
	public static final String ICO_SEARCH		= "search";
	public static final String ICO_STATUS		= "server_status";
	public static final String ICO_SETTINGS		= "settings";
	public static final String ICO_SORT			= "sort";
	public static final String ICO_SORT_UP		= "sort_up";
	public static final String ICO_SORT_DOWN	= "sort_down";
	public static final String ICO_STOP			= "stop";
	public static final String ICO_SQL_WINDOW	= "sql";
	public static final String ICO_SYNCHRONIZE	= "sync";
	public static final String ICO_VACUUM		= "vacuum";
	public static final String ICO_ZOOM_IN		= "zoom_in";
	public static final String ICO_ZOOM_OUT		= "zoom_out";

	public static final String ICO_DATABASE		= "database";
	public static final String ICO_SCHEMA		= "schema";
	public static final String ICO_SEQUENCE		= "sequence";
	public static final String ICO_TRIGGER_ACT	= "trigger_enabled";
	public static final String ICO_TRIGGER_NA	= "trigger_disabled";
	public static final String ICO_TABLE		= "table";
	public static final String ICO_FUNCTION		= "function";
	public static final String ICO_INDEX		= "index";
	public static final String ICO_PRIMARY_KEY	= "primary_key";
	public static final String ICO_COLUMN		= "column";
	public static final String ICO_VIEW			= "view";
	public static final String ICO_CONSTRAINT	= "constraint";
	public static final String ICO_WORKSPACE	= "workspace";

	public static final String ICO_MYSQL			= "mysql";
	public static final String ICO_POSTGRESQL	= "postgresql";
	public static final String ICO_MARIADB		= "maria_db";
	public static final String ICO_ABSTRACT		= "abstract";
	public static final String ICO_FLYWAY		= "flyway";

	public static final String ICO_EXPL_DEFAULT	= "default";
	public static final String ICO_EXPL_OUTPUT	= "output";
	public static final String ICO_EXPL_INVALID	= "invalid";
	public static final String ICO_EXPL_INDEX	= "index_scan";
	public static final String ICO_EXPL_LIMIT	= "limit";
	public static final String ICO_EXPL_JOIN	= "join";
	public static final String ICO_EXPL_SORT	= "sort";
	public static final String ICO_EXPL_MATER	= "materialize";
	public static final String ICO_EXPL_SEQ		= "sequence_scan";
	public static final String ICO_EXPL_LOOP	= "nested_loop";
	public static final String ICO_EXPL_HASH	= "hash";
	public static final String ICO_EXPL_APPEND	= "append";
	public static final String ICO_EXPL_SUB_Q	= "subquery";
	public static final String ICO_EXPL_RECHECK	= "index_recheck";
	public static final String ICO_EXPL_HEAP	= "heap";
	public static final String ICO_EXPL_AGG		= "aggregate";
	public static final String ICO_EXPL_WIN_AGG	= "window_aggregate";
	public static final String ICO_EXPL_PROC	= "function";

	private static File theme;
	private static final Map<String, ImageIcon> ICON_CACHE = new HashMap<>();

	/**
	 * Get 16px icon.
	 *
	 * @param icoName icon name
	 * @return icon
	 */
	public static ImageIcon getSmallIcon(String icoName){
		return getImage(icoName, ICO_SIZE_SMALL);
	}

	/**
	 * Get 32px icon.
	 *
	 * @param icoName icon name
	 * @return icon
	 */
	public static ImageIcon getMediumIcon(String icoName){
		return getImage(icoName, ICO_SIZE_MEDIUM);
	}

	/**
	 * Get 48px icon.
	 *
	 * @param icoName icon name
	 * @return icon
	 */
	public static ImageIcon getBiggerIcon(String icoName){
		return getImage(icoName, ICO_SIZE_BIGGER);
	}

	/**
	 * Get 64px icon.
	 *
	 * @param icoName icon name
	 * @return icon
	 */
	public static ImageIcon getLargeIcon(String icoName){
		return getImage(icoName, ICO_SIZE_LARGE);
	}

	/**
	 * Get same icon in multiple sizes.
	 *
	 * @param icoName icon name
	 * @return icon list
	 */
	public static List<Image> getAllSizes(String icoName){
		List<Image> icons = new ArrayList<>();
		for(int icoSize : new Integer[]{ICO_SIZE_SMALL, ICO_SIZE_MEDIUM, ICO_SIZE_BIGGER, ICO_SIZE_LARGE}){
			ImageIcon icon = getImage(icoName, icoSize);
			if(icon != null){
				icons.add(icon.getImage());
			}
		}
		return icons;
	}

	/**
	 * Get icon.
	 *
	 * @param icoName icon name
	 * @param icoSize icon size
	 * @return icon
	 */
	public static synchronized ImageIcon getImage(String icoName, int icoSize) {
		ImageIcon ret;
		String icoID = theme + "/" + icoSize + "/" + icoName + ICO_EXT;
		if (theme != null) {

			ret = ICON_CACHE.get(icoID);
			if(ret != null){
				return ret;
			}

			File dir = new File(theme, String.valueOf(icoSize));
			File ico = new File(dir, icoName + ICO_EXT);
			if (ico.exists() && ico.canRead()) {
				ret = new ImageIcon(ico.getAbsolutePath());
				ICON_CACHE.put(icoID, ret);
				return ret;
			}
		}

		icoID = "/gfx/Default/" + icoSize + "/" + icoName + ICO_EXT;
		ret = ICON_CACHE.get(icoID);
		if(ret != null){
			return ret;
		}

		URL res = Theme.class.getResource(icoID);
		if (res != null) {
			ret = new ImageIcon(res);
			ICON_CACHE.put(icoID, ret);
			return ret;
		}

		return null;
	}

	/**
	 * List themes.
	 *
	 * @return themes
	 */
	public static HashMap<String,String> getIconThemes(){
		LinkedHashMap<String, String> ret = new LinkedHashMap<>();
		ret.put("Default", null);

		String[] themes = THEME_DIR.list();
		if(themes != null) {
			for(String themeName : themes){
				if (new File(THEME_DIR, themeName).isDirectory()){
					ret.put(themeName, themeName);
				}
			}
		}
		return ret;
	}

	/**
	 * Select a theme (also on load)
	 *
	 * @param themeName theme name
	 */
	public static void setTheme(String themeName){
		if(themeName != null && !themeName.isEmpty()) {
			File dir = new File(THEME_DIR, themeName);

			System.out.println(dir.toString());

			if (dir.exists()) {
				theme = dir;
			}
		}
	}
}
