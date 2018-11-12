package com.databazoo.components;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Properties;

import com.databazoo.tools.Dbg;

/**
 * UI Constants
 */
public class UIConstants {
    public static final Graphics2D GRAPHICS = (Graphics2D) new BufferedImage(5,5,BufferedImage.TYPE_INT_ARGB).getGraphics();

    public static final String OS = System.getProperty("os.name").toLowerCase();

    public static final int TYPE_TIMEOUT = 500;

    public static final Color COLOR_RED = Color.decode("#D20000");
    public static final Color COLOR_RED_SELECTED = Color.decode("#FF7777");
    public static final Color COLOR_GREEN = Color.decode("#009900");
    public static final Color COLOR_GREEN_DARK = Color.decode("#003300");
    public static final Color COLOR_BLUE = Color.BLUE;
    public static final Color COLOR_BLUE_DARK = Color.decode("#000055");
    public static final Color COLOR_YELLOW = Color.YELLOW;
    public static final Color COLOR_GRAY = Color.decode("#999999");
    public static final Color COLOR_LIGHT_GRAY = Color.decode("#CCCCCC");
    public static final Color COLOR_BROWN = Color.decode("#CE7B00");
    public static final Color COLOR_AMBER = Color.decode("#770000");
    // TODO: select from environment
    public static final Color COLOR_BG_DARK = Color.decode("#9297A1");
    public static final Color COLOR_BG_LIGHT = Color.decode("#F2F2F2");
    public static final Color COLOR_FG_ATTRIBUTE = Color.decode("#000000");
    public static final Color COLOR_BG_ATTRIBUTE = Color.decode("#F5F9FF");
    public static final Color COLOR_TABLE_BORDERS = Color.decode("#F0F0F0");
    public static final Color COLOR_HILIGHT_INSERT = Color.decode("#AAFFAA");
    public static final Color COLOR_HILIGHT_DELETE = Color.decode("#FFCCCC");
    public static final Color COLOR_HILIGHT_CHANGE = Color.decode("#CCCCFF");

    public static final boolean DEBUG = getProperty("app.debug") != null && getProperty("app.debug").equalsIgnoreCase("true");
    public static final String APP_VERSION = "app.version";

    public static volatile Properties PROPERTIES;

    private static volatile String versionWithEnvironment;

    private static Boolean performant;
    private static AffineTransform transform;
    private static boolean lafWithRotatedTabs;

    /**
     * Check platform.
     *
     * @return is Windows platform?
     */
    public static boolean isWindows() {
        return OS.contains("win");
    }

    /**
     * Check platform.
     *
     * @return is MacOS platform?
     */
    public static boolean isMac() {
        return OS.contains("mac");
    }

    public static synchronized boolean isRetina() {
        if (transform == null) {
            transform = GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration()
                    .getDefaultTransform();
            Dbg.info("Graphics configuration is " +
                    transform.getScaleX() + "x" + transform.getScaleY() + ", " +
                    (transform.getScaleX() > 1D ? "" : "not ") + "HiDPI");
        }
        return transform.getScaleX() > 1D;
    }

    public static String getProperty(String property) {
        if (PROPERTIES == null) {
            try {
                PROPERTIES = new Properties();
                PROPERTIES.load(UIConstants.class.getResource("/app.properties").openStream());
            } catch (IOException e) {
                Dbg.fixme("Could not load properties", e);
            }
        }
        return PROPERTIES.getProperty(property);
    }

    public static String getAppVersion() {
        return getProperty(APP_VERSION);
    }

    /**
     * Get app version with environment.
     *
     * @return string
     */
    public static String getVersionWithEnvironment() {
        return versionWithEnvironment == null ? "uninitialized application on " + getJREVersion() : versionWithEnvironment;
    }

    public static void setVersionWithEnvironment(String versionWithEnvironment) {
        UIConstants.versionWithEnvironment = versionWithEnvironment;
    }

    /**
     * Get username and OS.
     *
     * @return string
     */
    public static String getUsernameWithOS() {
        return System.getProperty("user.name") + "." + OS;
    }

    /**
     * Get JRE version with bitness.
     *
     * @return string
     */
    public static String getJREVersion() {
        return System.getProperty("java.version") + " " + (System.getProperty("os.arch").contains("64") ? "64bit" : "32bit");
    }

    /**
     * What is the performance of the host computer?
     */
    public static synchronized boolean isPerformant(){
        if(performant == null){
            long start = System.currentTimeMillis();
            double sum = .0d;
            for(int i=0; i<300000; i++){
                sum += Math.pow(i, 1.600000001) - Math.pow(i, 1.6);
            }
            long total = System.currentTimeMillis() - start;
            performant = total < 100;
            Dbg.info("Performance calibration complete in " + total + "ms with " +
                    (performant ? "positive" : "negative") + " result of " + sum + ".");
        }
        return performant;
    }

    public static boolean isLafWithRotatedTabs() {
        return lafWithRotatedTabs;
    }

    public static void setLafWithRotatedTabs(boolean lafWithRotatedTabs) {
        UIConstants.lafWithRotatedTabs = lafWithRotatedTabs;
    }
}
