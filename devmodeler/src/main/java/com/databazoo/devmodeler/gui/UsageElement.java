package com.databazoo.devmodeler.gui;

import com.databazoo.tools.Usage;

/**
 * Implementations of {@link Usage.Element}.
 *
 * These constants make it possible to call {@link Usage#log(Usage.Element element)}.
 */
public class UsageElement extends Usage.Element {

    public static final String COPY_FULL = "full";
    public static final String SRC_REMOTE = "remote";
    public static final String WS_REMOVE = "remove";

    public static final UsageElement MAIN_WINDOW_OPEN = new UsageElement(Usage.Type.WINDOW_OPEN, 0);
    public static final UsageElement MAIN_WINDOW_CLOSE = new UsageElement(Usage.Type.WINDOW_CLOSE, 0);

    static final UsageElement NAVIGATOR_ZOOMED = new UsageElement(Usage.Type.ELEMENT_SCROLLED, 1001);
    static final UsageElement NAVIGATOR_DRAGGED = new UsageElement(Usage.Type.ELEMENT_DRAGGED, 1001, true);

    static final UsageElement NEIGHBORHOOD_CLICKED = new UsageElement(Usage.Type.ELEMENT_CLICKED, 1002);
    static final UsageElement NEIGHBORHOOD_CONTEXT = new UsageElement(Usage.Type.CONTEXT_MENU_CLICKED, 1002);
    static final UsageElement NEIGHBORHOOD_BTN_SYNC = new UsageElement(Usage.Type.BUTTON_PUSHED, 1201);
    static final UsageElement NEIGHBORHOOD_BTN_WORKSPACE = new UsageElement(Usage.Type.BUTTON_PUSHED, 1202);

    static final UsageElement RIGHT_MENU_BTN_SYNC = new UsageElement(Usage.Type.BUTTON_PUSHED, 1301);
    static final UsageElement RIGHT_MENU_CHK_SYNC = new UsageElement(Usage.Type.BUTTON_PUSHED, 1302);
    static final UsageElement RIGHT_MENU_CONN_COMBO = new UsageElement(Usage.Type.DROP_DOWN_SELECTED, 1303);
    static final UsageElement RIGHT_MENU_CONN2_COMBO = new UsageElement(Usage.Type.DROP_DOWN_SELECTED, 1304);
    static final UsageElement RIGHT_MENU_DB_COMBO = new UsageElement(Usage.Type.DROP_DOWN_SELECTED, 1305);
    static final UsageElement RIGHT_MENU_ADMIN_SERVER_ACTIVITY = new UsageElement(Usage.Type.DROP_DOWN_SELECTED, 1310);
    static final UsageElement RIGHT_MENU_ADMIN_USERS = new UsageElement(Usage.Type.DROP_DOWN_SELECTED, 1311);
    static final UsageElement RIGHT_MENU_ADMIN_DBS = new UsageElement(Usage.Type.DROP_DOWN_SELECTED, 1312);

    static final UsageElement CENTER_MENU_BTN_DESIGN = new UsageElement(Usage.Type.BUTTON_PUSHED, 1401);
    static final UsageElement CENTER_MENU_BTN_OPTIMIZE = new UsageElement(Usage.Type.BUTTON_PUSHED, 1402);
    static final UsageElement CENTER_MENU_BTN_DATA = new UsageElement(Usage.Type.BUTTON_PUSHED, 1403);
    static final UsageElement CENTER_MENU_BTN_DIFF = new UsageElement(Usage.Type.BUTTON_PUSHED, 1404);

    static final UsageElement LEFT_MENU_BTN_PROJECTS = new UsageElement(Usage.Type.BUTTON_PUSHED, 1501);
    static final UsageElement LEFT_MENU_BTN_SQL = new UsageElement(Usage.Type.BUTTON_PUSHED, 1502);
    static final UsageElement LEFT_MENU_BTN_EDIT = new UsageElement(Usage.Type.BUTTON_PUSHED, 1503);
    static final UsageElement LEFT_MENU_BTN_DATA = new UsageElement(Usage.Type.BUTTON_PUSHED, 1504);
    static final UsageElement LEFT_MENU_PROJECTS = new UsageElement(Usage.Type.MENU_SELECTED, 1510);
    static final UsageElement LEFT_MENU_SETTINGS = new UsageElement(Usage.Type.MENU_SELECTED, 1511);
    static final UsageElement LEFT_MENU_ABOUT = new UsageElement(Usage.Type.MENU_SELECTED, 1512);
    static final UsageElement LEFT_MENU_HELP = new UsageElement(Usage.Type.MENU_SELECTED, 1513);
    static final UsageElement LEFT_MENU_EXIT = new UsageElement(Usage.Type.MENU_SELECTED, 1514);
    static final UsageElement LEFT_MENU_ZOOM_IN = new UsageElement(Usage.Type.MENU_SELECTED, 1520);
    static final UsageElement LEFT_MENU_ZOOM_OUT = new UsageElement(Usage.Type.MENU_SELECTED, 1521);
    static final UsageElement LEFT_MENU_TGL_TREE = new UsageElement(Usage.Type.MENU_SELECTED, 1522);
    static final UsageElement LEFT_MENU_TGL_GRID = new UsageElement(Usage.Type.MENU_SELECTED, 1523);
    static final UsageElement LEFT_MENU_REARRANGE = new UsageElement(Usage.Type.MENU_SELECTED, 1530);
    static final UsageElement LEFT_MENU_EXPORT_XML = new UsageElement(Usage.Type.MENU_SELECTED, 1531);
    static final UsageElement LEFT_MENU_EXPORT_SQL = new UsageElement(Usage.Type.MENU_SELECTED, 1532);
    static final UsageElement LEFT_MENU_EXPORT_IMG = new UsageElement(Usage.Type.MENU_SELECTED, 1533);
    static final UsageElement LEFT_MENU_IMPORT = new UsageElement(Usage.Type.MENU_SELECTED, 1534);

    static final UsageElement SEARCH_USED = new UsageElement(Usage.Type.ELEMENT_CLICKED, 1006, true);
    static final UsageElement SEARCH_INVERT = new UsageElement(Usage.Type.BUTTON_PUSHED, 1601);
    static final UsageElement SEARCH_FULLTEXT = new UsageElement(Usage.Type.BUTTON_PUSHED, 1602);
    static final UsageElement SEARCH_HISTORY = new UsageElement(Usage.Type.BUTTON_PUSHED, 1603);
    static final UsageElement SEARCH_CLEAR = new UsageElement(Usage.Type.BUTTON_PUSHED, 1604);

    public static final UsageElement TABLE_DOUBLE_CLICKED = new UsageElement(Usage.Type.ELEMENT_DOUBLE_CLICKED, 2001);
    public static final UsageElement CONSTRAINT_DOUBLE_CLICKED = new UsageElement(Usage.Type.ELEMENT_DOUBLE_CLICKED, 2002);
    public static final UsageElement FUNCTION_DOUBLE_CLICKED = new UsageElement(Usage.Type.ELEMENT_DOUBLE_CLICKED, 2003);
    public static final UsageElement INDEX_DOUBLE_CLICKED = new UsageElement(Usage.Type.ELEMENT_DOUBLE_CLICKED, 2004);
    public static final UsageElement PACKAGE_DOUBLE_CLICKED = new UsageElement(Usage.Type.ELEMENT_DOUBLE_CLICKED, 2005);
    public static final UsageElement SCHEMA_DOUBLE_CLICKED = new UsageElement(Usage.Type.ELEMENT_DOUBLE_CLICKED, 2006);
    public static final UsageElement SEQUENCE_DOUBLE_CLICKED = new UsageElement(Usage.Type.ELEMENT_DOUBLE_CLICKED, 2007);
    public static final UsageElement TRIGGER_DOUBLE_CLICKED = new UsageElement(Usage.Type.ELEMENT_DOUBLE_CLICKED, 2008);
    public static final UsageElement VIEW_DOUBLE_CLICKED = new UsageElement(Usage.Type.ELEMENT_DOUBLE_CLICKED, 2009);
    public static final UsageElement ATTRIBUTE_DOUBLE_CLICKED = new UsageElement(Usage.Type.ELEMENT_DOUBLE_CLICKED, 2010);

    public static final UsageElement TABLE_CONTEXT_EDIT = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2101);
    public static final UsageElement TABLE_CONTEXT_DATA = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2102);
    public static final UsageElement TABLE_CONTEXT_COPY = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2103);
    public static final UsageElement TABLE_CONTEXT_SOURCE = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2104);
    public static final UsageElement TABLE_CONTEXT_WORKSPACE = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2105);
    public static final UsageElement TABLE_CONTEXT_MAINTAIN = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2106);
    public static final UsageElement TABLE_CONTEXT_TRUNCATE = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2107);
    public static final UsageElement TABLE_CONTEXT_DROP = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2108);

    public static final UsageElement CONSTRAINT_CONTEXT_EDIT = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2201);
    public static final UsageElement CONSTRAINT_CONTEXT_DATA = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2202);
    public static final UsageElement CONSTRAINT_CONTEXT_COPY = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2203);
    public static final UsageElement CONSTRAINT_CONTEXT_SOURCE = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2204);
    public static final UsageElement CONSTRAINT_CONTEXT_DROP = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2208);

    public static final UsageElement FUNCTION_CONTEXT_EDIT = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2301);
    public static final UsageElement FUNCTION_CONTEXT_EXEC = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2302);
    public static final UsageElement FUNCTION_CONTEXT_COPY = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2303);
    public static final UsageElement FUNCTION_CONTEXT_SOURCE = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2304);
    public static final UsageElement FUNCTION_CONTEXT_WORKSPACE = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2305);
    public static final UsageElement FUNCTION_CONTEXT_DROP = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2308);

    public static final UsageElement INDEX_CONTEXT_EDIT = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2401);
    public static final UsageElement INDEX_CONTEXT_COPY = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2403);
    public static final UsageElement INDEX_CONTEXT_SOURCE = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2404);
    public static final UsageElement INDEX_CONTEXT_DROP = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2408);

    public static final UsageElement PACKAGE_CONTEXT_EDIT = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2501);
    public static final UsageElement PACKAGE_CONTEXT_COPY = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2503);
    public static final UsageElement PACKAGE_CONTEXT_SOURCE = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2504);
    public static final UsageElement PACKAGE_CONTEXT_DROP = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2508);

    public static final UsageElement SCHEMA_CONTEXT_EDIT = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2601);
    public static final UsageElement SCHEMA_CONTEXT_COPY = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2603);
    public static final UsageElement SCHEMA_CONTEXT_SOURCE = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2604);
    public static final UsageElement SCHEMA_CONTEXT_WORKSPACE = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2605);
    public static final UsageElement SCHEMA_CONTEXT_MAINTAIN = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2606);
    public static final UsageElement SCHEMA_CONTEXT_REARRANGE = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2607);
    public static final UsageElement SCHEMA_CONTEXT_DROP = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2608);

    public static final UsageElement SEQUENCE_CONTEXT_EDIT = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2701);
    public static final UsageElement SEQUENCE_CONTEXT_COPY = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2703);
    public static final UsageElement SEQUENCE_CONTEXT_SOURCE = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2704);
    public static final UsageElement SEQUENCE_CONTEXT_WORKSPACE = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2705);
    public static final UsageElement SEQUENCE_CONTEXT_DROP = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2708);

    public static final UsageElement TRIGGER_CONTEXT_EDIT = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2801);
    public static final UsageElement TRIGGER_CONTEXT_COPY = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2803);
    public static final UsageElement TRIGGER_CONTEXT_SOURCE = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2804);
    public static final UsageElement TRIGGER_CONTEXT_DROP = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2808);

    public static final UsageElement VIEW_CONTEXT_EDIT = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2901);
    public static final UsageElement VIEW_CONTEXT_DATA = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2902);
    public static final UsageElement VIEW_CONTEXT_COPY = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2903);
    public static final UsageElement VIEW_CONTEXT_SOURCE = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2904);
    public static final UsageElement VIEW_CONTEXT_WORKSPACE = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2905);
    public static final UsageElement VIEW_CONTEXT_DROP = new UsageElement(Usage.Type.CONTEXT_MENU_SELECTED, 2908);

    private UsageElement(Usage.Type type, int id) {
        super(type, id, false);
    }

    private UsageElement(Usage.Type type, int id, boolean isRepeated) {
        super(type, id, isRepeated);
    }
}
