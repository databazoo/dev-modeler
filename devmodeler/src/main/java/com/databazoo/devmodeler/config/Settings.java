
package com.databazoo.devmodeler.config;

import com.databazoo.components.UIConstants;
import com.databazoo.components.icons.IconedPath;
import com.databazoo.devmodeler.project.ProjectManager;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;
import com.databazoo.tools.Usage;
import com.databazoo.tools.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.xml.parsers.SAXParserFactory;
import java.awt.*;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * User settings
 *
 * @author bobus
 */
public class Settings {
	public static final String L_THEME_ICONS			= "GUI.Theme.Icon set";

	public static final String L_FONT_TAB_SIZE			= "GUI.Fonts.Tab size";
	public static final String L_FONT_TEXT_EOL			= "GUI.Fonts.Text EOL";
	public static final String L_FONT_MONO_SIZE			= "GUI.Fonts.Monowidth font size";
	public static final String L_FONT_TREE_SIZE			= "GUI.Fonts.DB Tree font size";
	public static final String L_FONT_CANVAS_SIZE		= "GUI.Fonts.Canvas font size";
	public static final String L_FONT_FORCE_UPPER		= "GUI.Fonts.UPPERCASE keywords";

	public static final String L_LAYOUT_TREE_PROJECT_N	= "GUI.Layout.Tree Project Name";
	public static final String L_LAYOUT_WIZARD_TREE_W	= "GUI.Layout.Wizard Tree width";
	public static final String L_LAYOUT_DB_TREE_WIDTH	= "GUI.Layout.DB Tree width";
	public static final String L_LAYOUT_INFOPANEL		= "GUI.Layout.Show Info panel";
	public static final String L_LAYOUT_DB_TREE			= "GUI.Layout.Show DB Tree";
	public static final String L_LAYOUT_GRID			= "GUI.Layout.Show Grid";
	public static final String L_LAYOUT_SCROLLS			= "GUI.Layout.Hide scrolls";
	public static final String L_LAYOUT_NAV_TRANSPARENT	= "GUI.Layout.Navigator.Transparent navigator";
	public static final String L_LAYOUT_NAV_SIZE		= "GUI.Layout.Navigator.Size";
	public static final String L_LAYOUT_NAV_ZOOM_TICKS	= "GUI.Layout.Navigator.Show zoom ticks";
	public static final String L_LAYOUT_NEIGHBORHOOD	= "GUI.Layout.Show neighborhood";
	public static final String L_LAYOUT_PROJECT_SIMPLE	= "GUI.Layout.Simple project wizard";

	public static final String L_LOCALIZATION_DATE		= "GUI.Localization.Date format";
	public static final String L_LOCALIZATION_TIME		= "GUI.Localization.Time format";
	public static final String L_LOCALIZATION_COUNTRY	= "GUI.Localization.Country and Language";

	public static final String L_MAXIMIZE_PROJECTS		= "GUI.Maximize.Projects";
	public static final String L_MAXIMIZE_WIZARDS		= "GUI.Maximize.Wizards";
	public static final String L_MAXIMIZE_REVISIONS		= "GUI.Maximize.Revisions";
	public static final String L_MAXIMIZE_DATA			= "GUI.Maximize.Data";
	public static final String L_MAXIMIZE_ACTIVITY		= "GUI.Maximize.Server Activity";

	public static final String L_NOTICE_CLOSE			= "GUI.Ask confirmation.Before exit";
	public static final String L_NOTICE_OPEN_PROJECT	= "GUI.Ask confirmation.Reopen last project";
	public static final String L_NOTICE_LONG_TIMER		= "GUI.Ask confirmation.Long query timer";

	public static final String L_AUTOCOMPLETE_CHARS		= "Behavior.Autocompletion.Minimum lenght";
	public static final String L_AUTOCOMPLETE_ELEMS		= "Behavior.Autocompletion.Elements";
	public static final String L_AUTOCOMPLETE_SPACE		= "Behavior.Autocompletion.Add space";
	public static final String L_AUTOCOMPLETE_NATIVE_DT	= "Behavior.Autocompletion.Native data types";

	public static final String L_DND_DRAG_ONLY_SELECTED	= "Behavior.Drag and Drop.Only move selected";
	public static final String L_DND_SAME_TABLE_REL		= "Behavior.Drag and Drop.Self-reference";

	public static final String L_DATA_DEFAULT_DESC		= "Behavior.Data table.Default sort DESC";
	public static final String L_DATA_DEFAULT_LIMIT		= "Behavior.Data table.Default LIMIT value";
	public static final String L_DATA_CONTEXT_LIMIT		= "Behavior.Data table.Context LIMIT value";
	public static final String L_DATA_HISTORY_LIMIT		= "Behavior.Data table.History limit";
	public static final String L_DATA_HISTORY_SIZE		= "Behavior.Data table.History size";
	public static final String L_DATA_RELOAD_AFTER_SAVE	= "Behavior.Data table.Reload after save";
	public static final String L_DATA_BIG_TEXT_EDITOR	= "Behavior.Data table.Open \"big text\" editor";

	public static final String L_SYNC_TIMEOUT			= "Behavior.Synchronization.First cycle in";
	public static final String L_SYNC_INTERVAL			= "Behavior.Synchronization.Interval";
	public static final String L_SYNC_GIT_SVN_INTERVAL	= "Behavior.Synchronization.GIT/SVN interval";
	public static final String L_SYNC_SERVER_ACTIVITY	= "Behavior.Synchronization.Server activity interval";
	public static final String L_SYNC_INITIAL_CHECKED	= "Behavior.Synchronization.Enabled by default";

	public static final String L_REVISION_AUTHOR		= "Behavior.Revisions.Author";
	public static final String L_REVISION_NEW_REV_NAME	= "Behavior.Revisions.New revision name";
	public static final String L_REVISION_ADD_DATA		= "Behavior.Revisions.Add data";
	public static final String L_REVISION_AGGREGATE		= "Behavior.Revisions.Aggregate changes";

	//public static final String L_ACTIVITY_CHECK_INT		= "Behavior.Server Activity.Reload interval";
	//public static final String L_ACTIVITY_FADE_ROWS		= "Behavior.Server Activity.Fade missing rows";

	public static final String L_ERRORS_INCOMPAT_REV	= "Behavior.Error reporting.Revision incompatible";
	public static final String L_ERRORS_CRASH_REPORTS	= "Behavior.Error reporting.Send crash reports";
	public static final String L_ERRORS_USAGE_REPORTS	= "Behavior.Error reporting.Send usage reports";
	public static final String L_ERRORS_SHOW_DBG_INFO	= "Behavior.Error reporting.Show debug info";

	public static final String L_KEYS_CANVAS_ALT_MODES	= "Behavior.Keymap.Canvas.ALT switches views";
	public static final String L_KEYS_CANVAS_META_MODES	= "Behavior.Keymap.Canvas.META/WIN switches views";

	public static final String L_NAMING_SCHEMA			= "Elements names.New elements.Schema";
	public static final String L_NAMING_TABLE			= "Elements names.New elements.Table";
	public static final String L_NAMING_COLUMN			= "Elements names.New elements.Column";
	public static final String L_NAMING_PK				= "Elements names.New elements.Primary Key";
	public static final String L_NAMING_FK				= "Elements names.New elements.Foreign Key";
	public static final String L_NAMING_UNIQUE			= "Elements names.New elements.Unique constraint";
	public static final String L_NAMING_CHECK			= "Elements names.New elements.Check constraint";
	public static final String L_NAMING_INDEX			= "Elements names.New elements.Index";
	public static final String L_NAMING_SEQUENCE		= "Elements names.New elements.Sequence";
	public static final String L_NAMING_TRIGGER			= "Elements names.New elements.Trigger";
	public static final String L_NAMING_VIEW			= "Elements names.New elements.View";
	public static final String L_NAMING_FUNCTION		= "Elements names.New elements.Function";
	public static final String L_NAMING_PACKAGE			= "Elements names.New elements.Package";

	public static final String L_NAMING_ESCAPE_MY		= "Elements names.Quotation.Force for MySQL";

	public static final String L_PERFORM_CARDINALITY	= "Performance.Canvas.Draw cardinality";
	public static final String L_PERFORM_PARENT_CARD	= "Performance.Canvas.Always draw cardinality";
	public static final String L_PERFORM_ANTIALIASING	= "Performance.Canvas.Antialiasing";
	public static final String L_PERFORM_ANIMATION		= "Performance.Animation.Scroll to center";
	public static final String L_PERFORM_ANIMATE_STEPS	= "Performance.Animation.Steps";
	public static final String L_PERFORM_ANIMATE_TIME	= "Performance.Animation.Time";
	public static final String L_PERFORM_REOPEN_CONN	= "Performance.Connection.Reopen after";
	public static final String L_PERFORM_LOAD_PARALLEL	= "Performance.Connection.Load in parallel";

	public static final String L_PERFORM_REL_SIZE_LIMIT	= "Performance.Tables.Estimate rows over";
	public static final String L_PERFORM_COMPARE_LIMIT	= "Performance.Tables.Comparator data limit";

	//public static final String L_GLOBAL_PASSWORD		= "Password";
	public static final String L_GLOBAL_PLUGINS			= "Enabled plug-ins";

	private static final Map<String,String> values = new LinkedHashMap<>();
	private static final Map<String,String> descrs = new HashMap<>();
	private static final Map<String,Map<String,String>> options = new LinkedHashMap<>();
	private static final Map<String,Object> constraints = new HashMap<>();
	private static final Map<String,Boolean> booleans = new HashMap<>();
	static final String EN_UK = "en-UK";

	private static DefaultMutableTreeNode root;

	/**
	 * Fill basic setup
	 */
	public static void init(){
		put(L_THEME_ICONS, null, "Icon set", Theme.getIconThemes());

		put(L_FONT_MONO_SIZE, 11, "Size of monowidth font", new Point(8, 16));
		put(L_FONT_TREE_SIZE, 11, "Size of font for DB Tree and other UI elements", new Point(8, 16));
		put(L_FONT_CANVAS_SIZE, 10, "Size of font for Canvas", new Point(8, 16));
		put(L_FONT_TEXT_EOL, "\n", "Convert all End Of Line symbols to this format", getEOLOptions());
		put(L_FONT_TAB_SIZE, 4, "Size of TAB symbol", new Point(1, 8));
		put(L_FONT_FORCE_UPPER, true, "Automatically convert keywords like SELECT, UPDATE, DELETE, etc. to upper case?");

		put(L_LAYOUT_WIZARD_TREE_W, 3, "How wide should option tree in wizards be?", new Point(1,4));
		put(L_LAYOUT_DB_TREE_WIDTH, 3, "How wide should DB Tree be?", new Point(1,4));
		put(L_LAYOUT_NEIGHBORHOOD, "a", "Visibility of selected object's neighborhood", getNeighborhoodOptions());
		put(L_LAYOUT_INFOPANEL, true, "Display service info?");
		put(L_LAYOUT_DB_TREE, true, "Show DB Tree after start?");
		put(L_LAYOUT_GRID, true, "Show Grid after start?");
		put(L_LAYOUT_SCROLLS, true, "Hide scrollbars of Canvas?");
		put(L_LAYOUT_TREE_PROJECT_N, false, "Display project name hologram on object tree? (helps distinguish windows of different projects)");
		put(L_LAYOUT_PROJECT_SIMPLE, true, "Use a simple new project wizard that does not show advanced project configuration options?");

		put(L_LAYOUT_NAV_SIZE, 200, "Navigator's size in pixels", new Point(50, 400));
		put(L_LAYOUT_NAV_TRANSPARENT, true, "Should Navigator's background be semi-transparent?");
		put(L_LAYOUT_NAV_ZOOM_TICKS, true, "Display ticks by Navigator's zoom slider?");

		put(L_MAXIMIZE_PROJECTS, false, "Maximize Project wizard window?");
		put(L_MAXIMIZE_WIZARDS, false, "Maximize Wizard windows?");
		put(L_MAXIMIZE_REVISIONS, false, "Maximize Revision wizard window?");
		put(L_MAXIMIZE_DATA, false, "Maximize Data window?");
		put(L_MAXIMIZE_ACTIVITY, true, "Maximize Server activity window?");

		put(L_NOTICE_CLOSE, false, "Ask confirmation before closing main window?");
		put(L_NOTICE_OPEN_PROJECT, true, "Reopen last project automatically or ask which project to open?");
		put(L_NOTICE_LONG_TIMER, 2, "This many seconds before \"Saving changes takes long\" window appears", new Point(1, 20));

		put(L_AUTOCOMPLETE_CHARS, 1, "Show autocomplete options after this many characters are typed", new Point(1,5));
		put(L_AUTOCOMPLETE_ELEMS, 10, "Show this many autocomplete options (per group)", new Point(1,20));
		put(L_AUTOCOMPLETE_SPACE, "", "Add space after choosing an autocomplete option?", getAutocompleteSpaceOptions());
		put(L_AUTOCOMPLETE_NATIVE_DT, true, "Suggest native data types? For example: int2 (smallint), int4 (integer), int8 (bigint), float4 (real), float8 (double precision), etc.");

		put(L_DND_DRAG_ONLY_SELECTED, true, "Only allow dragging (moving) object that are selected?");
		put(L_DND_SAME_TABLE_REL, true, "Create self-relations with drag-n-drop?");

		put(L_DATA_DEFAULT_DESC, true, "Sort data newest to oldest by default?");
		put(L_DATA_DEFAULT_LIMIT, 200, "Select this many rows by default (set to 0 for no limit)", new Point(0, 1000));
		put(L_DATA_CONTEXT_LIMIT, 200, "Offer selecting this many rows in context menu for tables and views (set to 0 for no limit)", new Point(0, 1000));
		put(L_DATA_HISTORY_LIMIT, 200, "Remember this many executed queries", new Point(1, 1000));
		put(L_DATA_HISTORY_SIZE, 512, "Total size of executed queries history (in kilobytes)", new Point(1, 100000));
		put(L_DATA_RELOAD_AFTER_SAVE, true, "Reload whole table after you change a cell value?");
		put(L_DATA_BIG_TEXT_EDITOR, "a", "Open a separate window for text editing instead of using in-line editor", getBigTextOptions());

		put(L_SYNC_INITIAL_CHECKED, true, "Check Autosync on program load?");
		put(L_SYNC_TIMEOUT, 60, "Wait this many seconds before first synchronization cycle", new Point(10, 300));
		put(L_SYNC_INTERVAL, 600, "Wait this many seconds between synchronization cycles", new Point(60, 18000));
		put(L_SYNC_GIT_SVN_INTERVAL, 120, "Wait this many seconds between SVN UPDATE or GIT PULL cycles", new Point(5, 1800));

		put(L_REVISION_AUTHOR, System.getProperty("user.name"), "User name (will be used as Author in revisions)");
		put(L_REVISION_NEW_REV_NAME, true, "Always explicitly ask for a name when a new revision is created?");
		put(L_REVISION_ADD_DATA, true, "Add data changes (INSERT / UPDATE / DELETE in data table) to revision automatically");
		put(L_REVISION_AGGREGATE, true, "Aggregate multiple changes on same functions?");

		put(L_LOCALIZATION_COUNTRY, EN_UK, "Locale", getLocales());
		put(L_LOCALIZATION_DATE, "yyyy/MM/dd", "Date format (example: yyyy-MM-dd)");
		put(L_LOCALIZATION_TIME, "HH:mm:ss", "Time format (example: HH:mm:ss)");

		put(L_SYNC_SERVER_ACTIVITY, 1, "Delay between refresh cycles (seconds) in Server Activity window", new Point(1,15));
		//put(L_ACTIVITY_FADE_ROWS, true, "Keep finished process on the list for a few seconds?");

		put(L_ERRORS_INCOMPAT_REV, true, "Revisions may be incompatible between users (databases named differently, etc.) and such revisions are not loaded. Show it as error?");
		put(L_ERRORS_CRASH_REPORTS, true, "Automatically send a report if application crashes.");
		put(L_ERRORS_USAGE_REPORTS, true, "Send anonymous usage statistics like 'which buttons you use the most'. No personal data is tracked.");
		put(L_ERRORS_SHOW_DBG_INFO, false, "Show debugging info like memory consumption, connection and thread count");

		put(L_KEYS_CANVAS_ALT_MODES, true, "You can temporarily switch between Design and Data views by holding ALT key");
		put(L_KEYS_CANVAS_META_MODES, false, "You can temporarily switch between Design and Data views by holding META / WIN key");

		put(L_NAMING_SCHEMA, "new_schema_%serial%", "Pre-set name for a new schema");
		put(L_NAMING_TABLE, "new_table_%serial%", "Pre-set name for a new table");
		put(L_NAMING_COLUMN, "%lc_table%_new_column_%serial%", "Pre-set name for a new column");
		put(L_NAMING_PK, "pk_%lc_table%", "Pre-set name for a new primary key");
		put(L_NAMING_FK, "fk_%lc_table%_%lc_column%", "Pre-set name for a new foreign key");
		put(L_NAMING_UNIQUE, "uc_%lc_table%_%serial%", "Pre-set name for a new unique constraint");
		put(L_NAMING_CHECK, "cc_%lc_table%_%serial%", "Pre-set name for a new check constraint");
		put(L_NAMING_INDEX, "ix_%lc_table%_%serial%", "Pre-set name for a new index");
		put(L_NAMING_SEQUENCE, "new_sequence_%serial%", "Pre-set name for a new sequence");
		put(L_NAMING_TRIGGER, "tr_%lc_table%_%serial%", "Pre-set name for a new trigger");
		put(L_NAMING_VIEW, "new_view_%serial%", "Pre-set name for a new view");
		put(L_NAMING_FUNCTION, "new_function_%serial%", "Pre-set name for a new function");
		put(L_NAMING_PACKAGE, "new_package_%serial%", "Pre-set name for a new package");

		put(L_NAMING_ESCAPE_MY, false, "Force all scripts to generate elements' names as `names` in MySQL?");

		put(L_PERFORM_CARDINALITY, true, "Draw relation cardinality?");
		put(L_PERFORM_PARENT_CARD, false, "Draw cardinality for 1..1 parent?");
		put(L_PERFORM_ANTIALIASING, true, "Use antialiasing when drawing?");
		put(L_PERFORM_ANIMATION, true, "Animate scrolling after you select an element on Canvas?");
		put(L_PERFORM_ANIMATE_STEPS, UIConstants.isPerformant() ? 10 : 5, "Number of scroll steps", new Point(3,15));
		put(L_PERFORM_ANIMATE_TIME, 150, "Total animation time in milliseconds", new Point(100,400));
		put(L_PERFORM_REOPEN_CONN, 30, "Number of seconds to consider connection abandoned. Connection will be closed and reopened once needed again.", new Point(0,300));
		put(L_PERFORM_LOAD_PARALLEL, true, "Load database info in parallel connections? This requires more connections to the server but makes "
				+ "loading faster.");

		put(L_PERFORM_REL_SIZE_LIMIT, 5000000, "When estimated row count is higher, exact row count will not be calculated", new Point(1000000, 25000000));
		put(L_PERFORM_COMPARE_LIMIT, 500, "Do not compare table content when row number is higher", new Point(1, 100000));

		loadFromDisk();
		updateReferences();
	}

	private static void updateReferences() {
		Dbg.sendCrashReports = getBool(L_ERRORS_CRASH_REPORTS);
		Usage.sendUsageReports = getBool(L_ERRORS_USAGE_REPORTS);
	}

	/**
	 * Get options
	 *
	 * @return option map
	 */
	private static HashMap<String,String> getLocales(){
		LinkedHashMap<String, String> ret = new LinkedHashMap<>();
		ret.put(EN_UK, EN_UK);
		return ret;
	}

	/**
	 * Get options
	 *
	 * @return option map
	 */
	private static HashMap<String,String> getBigTextOptions(){
		LinkedHashMap<String, String> ret = new LinkedHashMap<>();
		ret.put("For all text values", "a");
		ret.put("For long text only", "l");
		ret.put("Do not use \"big text\" editor", "n");
		return ret;
	}

	/**
	 * Get options
	 *
	 * @return option map
	 */
	private static HashMap<String,String> getNeighborhoodOptions(){
		LinkedHashMap<String, String> ret = new LinkedHashMap<>();
		ret.put("Always visible", "v");
		ret.put("Auto hide", "a");
		ret.put("Always hidden", "h");
		return ret;
	}

	/**
	 * Get options
	 *
	 * @return option map
	 */
	private static HashMap<String,String> getEOLOptions(){
		LinkedHashMap<String, String> ret = new LinkedHashMap<>();
		ret.put("Do not change", null);
		ret.put("UNIX                 \\n", "\n");
		ret.put("Windows          \\r\\n", "\r\n");
		return ret;
	}

	/**
	 * Get options
	 *
	 * @return option map
	 */
	private static HashMap<String,String> getAutocompleteSpaceOptions(){
		LinkedHashMap<String, String> ret = new LinkedHashMap<>();
		ret.put("Always", "y");
		ret.put("Auto", "");
		ret.put("Never", "n");
		return ret;
	}

	/**
	 * Get description for given setting.
	 *
	 * @param key option
	 * @return description
	 */
	public static String getDescription(String key){
		return descrs.get(key);
	}

	/**
	 * Generate tree model
	 *
	 * @return tree model
	 */
	public static DefaultMutableTreeNode getTree(){
		if(root == null) {
			root = new DefaultMutableTreeNode();
			//Object[] keys = values.keySet().toArray();
			//Arrays.sort(keys);
            for (String key : values.keySet())
			{
                String[] parts = key.split("\\.");

				// Skip custom section
                if(parts[0].equals("Global")) {
                    continue;
                }

                DefaultMutableTreeNode lastNode = root;
                for(int j=0; j<parts.length-1; j++){
                    boolean found = false;
                    for(int k=0; k<lastNode.getChildCount(); k++){
                        TreeNode node = lastNode.getChildAt(k);
                        if(node.toString().equals(parts[j])){
                            lastNode = (DefaultMutableTreeNode) node;
                            found = true;
                            break;
                        }
                    }
                    if(!found){
                        DefaultMutableTreeNode newChildNode = new DefaultMutableTreeNode(new IconedPath(parts[j], (j < parts.length - 2 ? null : IconedPath.ICO_LIST)));
                        lastNode.add(newChildNode);
                        lastNode = newChildNode;
                    }
                }
            }
			DefaultMutableTreeNode global = new DefaultMutableTreeNode(new IconedPath("Global", null));
			//global.add(new DefaultMutableTreeNode(new IconedPath(L_GLOBAL_PASSWORD, IconedPath.ICO_KEY)));
			global.add(new DefaultMutableTreeNode(new IconedPath(L_GLOBAL_PLUGINS, IconedPath.ICO_PLUGIN)));
			root.add(global);
		}
		return root;
	}

	/**
	 * Get setting keys.
	 *
	 * @return key array
	 */
	public static Object[] getKeys(){
		Object[] keys = values.keySet().toArray();
		//Arrays.sort(keys);
		return keys;
	}

	/**
	 * Store values.
	 *
	 * @param key key
	 * @param value value
	 */
	public static void put(String key, boolean value){
		values.put(key, value ? "1" : "");
		booleans.put(key, true);
		updateReferences();
	}

	/**
	 * Store values.
	 *
	 * @param key key
	 * @param value value
	 * @param descr description
	 */
	public static void put(String key, boolean value, String descr){
		put(key, value);
		descrs.put(key, descr);
		updateReferences();
	}

	/**
	 * Store values.
	 *
	 * @param key key
	 * @param value value
	 */
	public static void put(String key, int value){
		values.put(key, Integer.toString(value));
		updateReferences();
	}

	/**
	 * Store values.
	 *
	 * @param key key
	 * @param value value
	 * @param descr description
	 * @param cons numeric constraints
	 */
	private static void put(String key, int value, String descr, Point cons){
		put(key, value);
		descrs.put(key, descr);
		constraints.put(key, cons);
		updateReferences();
	}

	/**
	 * Store values.
	 *
	 * @param key key
	 * @param value value
	 */
	public static void put(String key, String value){
		if(key.equals(L_THEME_ICONS)){
			Theme.setTheme(value);
		}
		values.put(key, value);
		updateReferences();
	}

	/**
	 * Store values.
	 *
	 * @param key key
	 * @param value value
	 * @param descr description
	 */
	private static void put(String key, String value, String descr){
		put(key, value);
		descrs.put(key, descr);
		updateReferences();
	}

	/**
	 * Store values.
	 *
	 * @param key key
	 * @param value value
	 * @param descr description
	 * @param opts options list
	 */
	private static void put(String key, String value, String descr, HashMap<String,String> opts){
		put(key, value);
		descrs.put(key, descr);
		options.put(key, opts);
		updateReferences();
	}

	/**
	 * Read values.
	 *
	 * @param key key
	 * @return value
	 */
	public static boolean getBool(String key){
		return values.get(key) != null && values.get(key).equals("1");
	}

	/**
	 * Read values.
	 *
	 * @param key key
	 * @return value
	 */
	public static int getInt(String key){
		return Integer.parseInt(values.get(key));
	}

	/**
	 * Read values.
	 *
	 * @param key key
	 * @return value
	 */
	public static String getStr(String key){
		return values.get(key);
	}

	/**
	 * Load data from disk
	 */
	private static void loadFromDisk(){
		try {
			SAXParserFactory.newInstance().newSAXParser().parse(ProjectManager.getSettingsDirectory("config.xml"), new SettingsSAXHandler());
		} catch (FileNotFoundException e){
			Dbg.notImportantAtAll("File not found means this is the first run of the application", e);
		} catch (Exception e){
			Dbg.fixme("config.xml could not be loaded, file broken", e);
		}
	}

	/**
	 * Write data to disk
	 */
	private static void saveToDisk(){
		try {
			Document doc = XMLWriter.getNewDocument();
			Element elemRoot = doc.createElement("settings");
			doc.appendChild(elemRoot);

			for (Object key : getKeys()) {
				Element elem = doc.createElement("setting");
				elemRoot.appendChild(elem);

				XMLWriter.setAttribute(elem, "path", (String) key);
				XMLWriter.setAttribute(elem, "value", getStr((String) key));
			}
			XMLWriter.out(doc, ProjectManager.getSettingsDirectory("config.xml"));
		} catch (Exception ex) {
			Dbg.fixme("Application settings could not be saved", ex);
		}
	}

	/**
	 * Get string options.
	 *
	 * @param key key
	 * @return options map
	 */
	public static Map<String,String> getStringOptions(String key) {
		return options.get(key);
	}

	/**
	 * Get numerig constraints.
	 *
	 * @param key key
	 * @return constraints
	 */
	public static Point getIntConstraints(String key) {
		try {
			return (Point)constraints.get(key);
		} catch (ClassCastException e){
			Dbg.fixme("Loading int constraints for non-int column", e);
			return null;
		}
	}

	/**
	 * Is value boolean?
	 *
	 * @param key key
	 * @return Is value boolean?
	 */
	public static boolean isBoolean(String key) {
		return booleans.containsKey(key);
	}

	/**
	 * Delayed save to disk
	 */
	public static synchronized void save(){
		Schedule.reInvokeInWorker(Schedule.Named.SETTINGS_SAVE, UIConstants.TYPE_TIMEOUT, Settings::saveToDisk);
	}

	/**
	 * Remove key.
	 *
	 * @param key key
	 */
	public static void remove(String key) {
		values.remove(key);
	}

	/**
	 * Sax handler
	 */
	private static class SettingsSAXHandler extends DefaultHandler {
		@Override
		public void startElement(String uri, String localName, String tagName, Attributes attributes) throws SAXException {
			if(tagName.equalsIgnoreCase("setting")) {
				String key = XMLWriter.getString(attributes.getValue("path"));
				if(values.containsKey(key) || key.startsWith("Global."+L_GLOBAL_PLUGINS+".")) {
					put(key, XMLWriter.getString(attributes.getValue("value")));
				}
			}
		}
	}
}
