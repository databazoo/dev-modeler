package com.databazoo.tools;

import com.databazoo.components.UIConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.databazoo.tools.Schedule.Named.USAGE_LOG_REPEATED;

/**
 * Tracks and reports application usage.
 */
public class Usage {
    public enum Type {
        WINDOW_OPEN,
        WINDOW_CLOSE,
        BUTTON_PUSHED,
        DROP_DOWN_SELECTED,
        MENU_CLICKED,
        MENU_SELECTED,
        CONTEXT_MENU_CLICKED,
        CONTEXT_MENU_SELECTED,
        ELEMENT_CLICKED,
        ELEMENT_DOUBLE_CLICKED,
        ELEMENT_SCROLLED,
        ELEMENT_DRAGGED,
    }

    public static boolean sendUsageReports = true;

    static List<String[]> events = new ArrayList<>();

    /**
     * Logging method.
     *
     * @param element operation type and element ID
     */
    public static void log(Element element) {
        if(element.isRepeated) {
            Schedule.reInvokeInEDT(USAGE_LOG_REPEATED, Schedule.TYPE_DELAY, () -> log(element.type.name(), element.id));
        } else {
            log(element.type.name(), element.id);
        }
    }

    /**
     * Logging method.
     *
     * @param element operation type and element ID
     * @param description additional information like "Exit code: 0"
     */
    public static void log(Element element, String description) {
        if(element.isRepeated) {
            Schedule.reInvokeInEDT(USAGE_LOG_REPEATED, Schedule.TYPE_DELAY, () -> log(element.type.name(), element.id, description));
        } else {
            log(element.type.name(), element.id, description);
        }
    }

    /**
     * Logging method.
     *
     * @param type operation type
     * @param id element ID
     */
    public static void log(String type, int id) {
        events.add(new String[]{ type, Integer.toString(id) });
    }

    /**
     * Logging method.
     *
     * @param type operation type
     * @param id element ID
     * @param description additional information like "Exit code: 0"
     */
    public static void log(String type, int id, String description) {
        events.add(new String[]{ type, Integer.toString(id), description });
    }

    /**
     * Send the report at application exit.
     *
     * Application should dispose of all application windows, as this operation may take several seconds to execute.
     */
    public static void sendReport(){
        if (!sendUsageReports || UIConstants.DEBUG) {
            return;
        }
        try {
            Map<String, String> map = new LinkedHashMap<>();
            for(int i = 0; i < events.size(); i++) {
                final String[] event = events.get(i);

                map.put("type[" + i + "]", event[0]);
                map.put("id[" + i + "]", event[1]);

                if(event.length > 2) {
                    map.put("descr[" + i + "]", event[2]);
                }
            }
            RestClient.getInstance().postJSON("usage", map);

        } catch (IOException e) {
            Dbg.notImportant("Usage report send failed", e);
        }
    }

    /**
     * Default implementation that makes it possible to call {@link #log(Element element)} with constants.
     *
     * Individual elements are application-dependent, so must be implemented in the application itself.
     *
     * Example implementation:
     * {@code
     *
     *  public class UsageElement extends Usage.Element {
     *
     *      public static final UsageElement MAIN_WINDOW_OPEN = new UsageElement(Usage.Type.WINDOW_OPEN, 0);
     *      public static final UsageElement MAIN_WINDOW_CLOSE = new UsageElement(Usage.Type.WINDOW_CLOSE, 0);
     *
     *      private UsageElement(Usage.Type type, int id) {
     *          super(type, id, false);
     *      }
     *
     *      private UsageElement(Usage.Type type, int id, boolean isRepeated) {
     *          super(type, id, isRepeated);
     *      }
     *  }
     *
     * }
     */
    public static abstract class Element {

        private final Type type;
        private final int id;
        private final boolean isRepeated;

        public Element(Type type, int id, boolean isRepeated) {
            this.type = type;
            this.id = id;
            this.isRepeated = isRepeated;
        }
    }
}
