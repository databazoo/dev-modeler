package com.databazoo.devmodeler.gui;

import com.databazoo.devmodeler.DevModeler;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.tools.Dbg;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.pushingpixels.substance.api.SubstanceSkin;
import org.pushingpixels.substance.api.skin.BusinessBlackSteelSkin;
import org.pushingpixels.substance.api.skin.BusinessBlueSteelSkin;
import org.pushingpixels.substance.api.skin.CeruleanSkin;
import org.pushingpixels.substance.api.skin.CremeSkin;
import org.pushingpixels.substance.api.skin.GraphiteGlassSkin;
import org.pushingpixels.substance.api.skin.GraphiteGoldSkin;
import org.pushingpixels.substance.api.skin.GraphiteSkin;
import org.pushingpixels.substance.api.skin.MarinerSkin;
import org.pushingpixels.substance.api.skin.MistAquaSkin;
import org.pushingpixels.substance.api.skin.MistSilverSkin;
import org.pushingpixels.substance.api.skin.ModerateSkin;
import org.pushingpixels.substance.api.skin.NebulaSkin;
import org.pushingpixels.substance.api.skin.RavenSkin;
import org.pushingpixels.substance.api.skin.TwilightSkin;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class SkinnedSubstanceLAF extends SubstanceLookAndFeel {
    private static final Map<String, SkinDescriptor> SKINS = new LinkedHashMap<>();
    private static SkinDescriptor selectedSkin;

    public static final String L_NIMBUS_FALLBACK = "Nimbus (fallback)";
    public static final String L_BRIGHT = "Bright";
    public static final String L_DARK = "Dark";

    public static final String SKIN_NIMBUS = "Nimbus";
    private static final String SKIN_BRIGHT = "Business Blue Steel";
    private static final String SKIN_DARK = "Graphite Glass";

    static {
        SKINS.put("Business Black Steel", new SkinDescriptor("Business Black Steel", new BusinessBlackSteelSkin(), true, false, "BusinessBlackSteel.png"));
        SKINS.put("Business Blue Steel", new SkinDescriptor("Business Blue Steel", new BusinessBlueSteelSkin(), true, false, "BusinessBlueSteel.png"));
        SKINS.put("Cerulean", new SkinDescriptor("Cerulean", new CeruleanSkin(), true, false, "Cerulean.png"));
        SKINS.put("Creme", new SkinDescriptor("Creme", new CremeSkin(), true, false, "Creme.png"));

        SKINS.put("Mariner", new SkinDescriptor("Mariner", new MarinerSkin(), true, false, "Mariner.png"));
        SKINS.put("Mist Aqua", new SkinDescriptor("Mist Aqua", new MistAquaSkin(), true, false, "MistAqua.png"));
        SKINS.put("Mist Silver", new SkinDescriptor("Mist Silver", new MistSilverSkin(), true, false, "MistSilver.png"));

        SKINS.put("Moderate", new SkinDescriptor("Moderate", new ModerateSkin(), true, false, "Moderate.png"));
        SKINS.put("Nebula", new SkinDescriptor("Nebula", new NebulaSkin(), true, false, "Nebula.png"));

        SKINS.put("Graphite", new SkinDescriptor("Graphite", new GraphiteSkin(), true, true, "Graphite.png"));
        SKINS.put("Graphite Glass", new SkinDescriptor("Graphite Glass", new GraphiteGlassSkin(), true, true, "GraphiteGlass.png"));
        SKINS.put("Graphite Gold", new SkinDescriptor("Graphite Gold", new GraphiteGoldSkin(), true, true, "GraphiteGold.png"));
        SKINS.put("Raven", new SkinDescriptor("Raven", new RavenSkin(), true, true, "Raven.png"));
        SKINS.put("Twilight", new SkinDescriptor("Twilight", new TwilightSkin(), true, true, "Twilight.png"));
    }

    private static SubstanceSkin getSkin() {
        selectedSkin = SKINS.get(Settings.getStr(Settings.L_THEME_COLORS));
        if (selectedSkin == null) {
            selectedSkin = SKINS.get(SKIN_BRIGHT);
        }
        return selectedSkin.substanceSkin;
    }

    static boolean isCurrentSkinDark() {
        if (selectedSkin != null) {
            return selectedSkin.isDark;
        }
        return false;
    }

    static boolean isCurrentSkinBordered() {
        if (selectedSkin != null) {
            return selectedSkin.requiresBorderedTextFields;
        }
        return false;
    }

    SkinnedSubstanceLAF() {
        super(getSkin());
    }

    /**
     * List themes.
     *
     * @return themes
     */
    public static Map<String, String> getThemes() {
        LinkedHashMap<String, String> ret = new LinkedHashMap<>();
        ret.put(L_NIMBUS_FALLBACK, SKIN_NIMBUS);
        ret.put(L_BRIGHT, SKIN_BRIGHT);
        for (Map.Entry<String, SkinDescriptor> entry : SKINS.entrySet()) {
            if (!entry.getValue().isDark) {
                ret.put("        " + entry.getKey(), entry.getKey());
            }
        }
        ret.put(L_DARK, SKIN_DARK);
        for (Map.Entry<String, SkinDescriptor> entry : SKINS.entrySet()) {
            if (entry.getValue().isDark) {
                ret.put("        " + entry.getKey(), entry.getKey());
            }
        }

        return ret;
    }

    /**
     * List themes as skin descriptors.
     *
     * @return themes
     */
    public static Map<String, SkinDescriptor> getThemeDescriptors() {
        LinkedHashMap<String, SkinDescriptor> ret = new LinkedHashMap<>();
        ret.put(L_NIMBUS_FALLBACK, null);
        ret.put(L_BRIGHT, SKINS.get(SKIN_BRIGHT));
        for (Map.Entry<String, SkinDescriptor> entry : SKINS.entrySet()) {
            if (!entry.getValue().isDark) {
                ret.put("        " + entry.getKey(), entry.getValue());
            }
        }
        ret.put(L_DARK, SKINS.get(SKIN_DARK));
        for (Map.Entry<String, SkinDescriptor> entry : SKINS.entrySet()) {
            if (entry.getValue().isDark) {
                ret.put("        " + entry.getKey(), entry.getValue());
            }
        }

        return ret;
    }

    public static class SkinDescriptor {
        private final String name;
        private final SubstanceSkin substanceSkin;
        private final boolean requiresBorderedTextFields;
        private final boolean isDark;
        private final String imageURI;
        private Icon icon;

        SkinDescriptor(String name, SubstanceSkin businessBlackSteelSkin, boolean requiresBorderedTextFields, boolean isDark, String imageURI) {
            this.name = name;
            this.substanceSkin = businessBlackSteelSkin;
            this.requiresBorderedTextFields = requiresBorderedTextFields;
            this.isDark = isDark;
            this.imageURI = imageURI;
        }

        public String getName() {
            return name;
        }

        public boolean isRequiresBorderedTextFields() {
            return requiresBorderedTextFields;
        }

        public Icon getPreview() {
            if (icon == null) {
                String resourceName = "/gfx/preview/" + imageURI;
                URL resource = getClass().getResource(resourceName);
                icon = new ImageIcon(resource);
            }
            return icon;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SkinDescriptor that = (SkinDescriptor) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
