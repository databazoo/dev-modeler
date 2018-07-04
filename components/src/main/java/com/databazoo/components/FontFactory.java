package com.databazoo.components;

import com.databazoo.tools.Dbg;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class FontFactory {

    private static final FontProvider FONT_PROVIDER = new FontProvider();

    public static Font getMonospaced(int style, int size) {
        return new Font(FONT_PROVIDER.monoName, style, size);
    }

    public static Font getSans(int style, int size) {
        return new Font(FONT_PROVIDER.sansName, style, size);
    }

    public static String getMonospacedName() {
        return FONT_PROVIDER.monoName;
    }

    private static class FontProvider {
        private final String[] monoNames = new String[]{
                "Liberation Mono",
                "Noto Mono",
                "Noto Sans Mono",
                "Courier New",
                Font.MONOSPACED
        };
        private final String[] sansNames = new String[]{
                "DejaVu Sans",
                "Lucida Sans",
                "Noto Sans",
                "Verdana",
                Font.SANS_SERIF
        };

        private String monoName;
        private String sansName;

        FontProvider() {
            List<String> availableFontFamilyNames = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());

            for (String name : monoNames) {
                if (availableFontFamilyNames.contains(name)) {
                    monoName = name;
                    Dbg.info("mono font is " + name);
                    break;
                }
            }

            for (String name : sansNames) {
                if (availableFontFamilyNames.contains(name)) {
                    sansName = name;
                    System.out.println("sans font is " + name);
                    break;
                }
            }
        }
    }
}
