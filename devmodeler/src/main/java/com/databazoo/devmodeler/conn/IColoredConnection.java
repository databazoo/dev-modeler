package com.databazoo.devmodeler.conn;

import com.databazoo.devmodeler.project.Project;

import java.awt.*;

public interface IColoredConnection {

    enum ConnectionColor {
        DEFAULT("Default", null),
        RED("Red", Color.decode("#EE8888")),
        ORANGE("Orange", Color.decode("#FFCC88")),
        YELLOW("Yellow", Color.decode("#FFFF88")),
        GREEN("Green", Color.decode("#88EE88")),
        BLUE("Blue", Color.decode("#88CCFF")),
        VIOLET("Violet", Color.decode("#8888EE")),;

        public static ConnectionColor fromString(String colorName) {
            for (ConnectionColor color : values()) {
                if (color.getColorName().equals(colorName) || color.name().equals(colorName)) {
                    return color;
                }
            }
            return DEFAULT;
        }

        private final String colorName;
        private final Color color;

        ConnectionColor(String colorName, Color color) {
            this.colorName = colorName;
            this.color = color;
        }

        public String getColorName() {
            return colorName;
        }

        public Color getColor() {
            return color;
        }
    }

    /**
     * Returns a color for a connection.
     * Should be called when opening a new window (when the current project is valid).
     * It is the only reliable way, as dedicated connections may not have the updated color set.
     *
     * @param connection currently available connection
     * @return color or null
     */
    static Color getColor(IConnection connection) {
       return Project.getCurrent().getConnections().stream()
               .filter(conn -> conn.getName().equals(connection.getName()))
               .findAny()
               .map(iConnection -> iConnection.getColor() != null ? iConnection.getColor().getColor() : ConnectionColor.DEFAULT.getColor())
               .orElseGet(ConnectionColor.DEFAULT::getColor);
    }

    ConnectionColor getColor();

    void setColor(ConnectionColor color);
}
