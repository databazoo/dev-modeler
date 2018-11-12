package com.databazoo.devmodeler.gui;

import com.databazoo.devmodeler.config.Settings;
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
import org.pushingpixels.substance.api.skin.NebulaBrickWallSkin;
import org.pushingpixels.substance.api.skin.NebulaSkin;
import org.pushingpixels.substance.api.skin.RavenSkin;
import org.pushingpixels.substance.api.skin.TwilightSkin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class SkinnedSubstanceLAF extends SubstanceLookAndFeel {
    private static final Map<String, SkinDescriptor> SKINS = new LinkedHashMap<>();
    private static SkinDescriptor selectedSkin;

    static final String SKIN_NIMBUS = "Nimbus";
    private static final String SKIN_BRIGHT = "Business Blue Steel";
    private static final String SKIN_DARK = "Graphite Glass";

    static {
        SKINS.put("Business Black Steel", new SkinDescriptor("Business Black Steel", new BusinessBlackSteelSkin(), true, false));   // Good one
        SKINS.put("Business Blue Steel", new SkinDescriptor("Business Blue Steel", new BusinessBlueSteelSkin(), true, false));      // Good one
        SKINS.put("Cerulean", new SkinDescriptor("Cerulean", new CeruleanSkin(), true, false));
        SKINS.put("Creme", new SkinDescriptor("Creme", new CremeSkin(), true, false));

        SKINS.put("Mariner", new SkinDescriptor("Mariner", new MarinerSkin(), true, false));
        SKINS.put("Mist Aqua", new SkinDescriptor("Mist Aqua", new MistAquaSkin(), true, false));
        SKINS.put("Mist Silver", new SkinDescriptor("Mist Silver", new MistSilverSkin(), true, false));

        SKINS.put("Moderate", new SkinDescriptor("Moderate", new ModerateSkin(), true, false));                                     // Good one
        SKINS.put("Nebula Brick Wall", new SkinDescriptor("Nebula Brick Wall", new NebulaBrickWallSkin(), true, false));            // Generic - may be a good choice
        SKINS.put("Nebula", new SkinDescriptor("Nebula", new NebulaSkin(), true, false));                                           // Good one - no setup required

        SKINS.put("Graphite", new SkinDescriptor("Graphite", new GraphiteSkin(), true, true));
        SKINS.put("Graphite Glass", new SkinDescriptor("Graphite Glass", new GraphiteGlassSkin(), true, true));
        SKINS.put("Graphite Gold", new SkinDescriptor("Graphite Gold", new GraphiteGoldSkin(), true, true));
        SKINS.put("Raven", new SkinDescriptor("Raven", new RavenSkin(), true, true));
        SKINS.put("Twilight", new SkinDescriptor("Twilight", new TwilightSkin(), true, true));
    }

    private static SubstanceSkin getSkin() {
        selectedSkin = SKINS.get(Settings.getStr(Settings.L_THEME_COLORS));
        if (selectedSkin == null) {
            selectedSkin = SKINS.get(SKIN_BRIGHT);
        }
        return selectedSkin.substanceSkin;
    }

    public static boolean isCurrentSkinDark() {
        if (selectedSkin != null) {
            return selectedSkin.isDark;
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
        ret.put("Nimbus (fallback)", SKIN_NIMBUS);
        ret.put("Bright", SKIN_BRIGHT);
        for (Map.Entry<String, SkinDescriptor> entry : SKINS.entrySet()) {
            if (!entry.getValue().isDark) {
                ret.put("        " + entry.getKey(), entry.getKey());
            }
        }
        ret.put("Dark", SKIN_DARK);
        for (Map.Entry<String, SkinDescriptor> entry : SKINS.entrySet()) {
            if (entry.getValue().isDark) {
                ret.put("        " + entry.getKey(), entry.getKey());
            }
        }

        return ret;
    }

    private static class SkinDescriptor {
        private final String name;
        private final SubstanceSkin substanceSkin;
        private final boolean requiresBorderedTextFields;
        private final boolean isDark;

        SkinDescriptor(String name, SubstanceSkin businessBlackSteelSkin, boolean requiresBorderedTextFields, boolean isDark) {
            this.name = name;
            this.substanceSkin = businessBlackSteelSkin;
            this.requiresBorderedTextFields = requiresBorderedTextFields;
            this.isDark = isDark;
        }

        public String getName() {
            return name;
        }

        public boolean isRequiresBorderedTextFields() {
            return requiresBorderedTextFields;
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
