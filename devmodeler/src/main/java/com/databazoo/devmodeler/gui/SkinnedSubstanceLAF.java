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
import org.pushingpixels.substance.api.skin.OfficeBlack2007Skin;
import org.pushingpixels.substance.api.skin.OfficeBlue2007Skin;
import org.pushingpixels.substance.api.skin.OfficeSilver2007Skin;
import org.pushingpixels.substance.api.skin.RavenSkin;
import org.pushingpixels.substance.api.skin.TwilightSkin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class SkinnedSubstanceLAF extends SubstanceLookAndFeel {
    private static final Map<SkinDescriptor, SubstanceSkin> SKINS = new LinkedHashMap<>();

    public static final String SKIN_NIMBUS = "Nimbus";
    private static final String SKIN_BRIGHT = "Business Blue Steel";
    private static final String SKIN_DARK = "Graphite Glass";

    static {
        SKINS.put(new SkinDescriptor("Business Black Steel", true, false), new BusinessBlackSteelSkin());  // Good one
        SKINS.put(new SkinDescriptor("Business Blue Steel", true, false), new BusinessBlueSteelSkin());    // Good one
        SKINS.put(new SkinDescriptor("Cerulean", true, false), new CeruleanSkin());
        SKINS.put(new SkinDescriptor("Creme", true, false), new CremeSkin());

        SKINS.put(new SkinDescriptor("Mariner", true, false), new MarinerSkin());
        SKINS.put(new SkinDescriptor("Mist Aqua Skin", true, false), new MistAquaSkin());
        SKINS.put(new SkinDescriptor("Mist Silver", true, false), new MistSilverSkin());

        SKINS.put(new SkinDescriptor("Moderate", true, false), new ModerateSkin());                        // Good one
        SKINS.put(new SkinDescriptor("Nebula Brick Wall", true, false), new NebulaBrickWallSkin());        // Generic - may be a good choice
        SKINS.put(new SkinDescriptor("Nebula", true, false), new NebulaSkin());                            // Good one - no setup required


        SKINS.put(new SkinDescriptor("Graphite", true, true), new GraphiteSkin());
        SKINS.put(new SkinDescriptor("Graphite Glass", true, true), new GraphiteGlassSkin());
        SKINS.put(new SkinDescriptor("Graphite Gold", true, true), new GraphiteGoldSkin());
        SKINS.put(new SkinDescriptor("Raven", true, true), new RavenSkin());
        SKINS.put(new SkinDescriptor("Twilight", true, true), new TwilightSkin());
    }

    private static SubstanceSkin getSkin() {
        String themeCode = Settings.getStr(Settings.L_THEME_COLORS);
        SkinDescriptor skinDescriptor = new SkinDescriptor((themeCode != null ? themeCode : SKIN_BRIGHT), false, false);
        SubstanceSkin skin = SKINS.get(skinDescriptor);
        return skin != null ? skin : SKINS.get(new SkinDescriptor(SKIN_BRIGHT, false, false));
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
        for (Map.Entry<SkinDescriptor, SubstanceSkin> entry : SKINS.entrySet()) {
            if (!entry.getKey().isDark) {
                ret.put("        " + entry.getKey().getName(), entry.getKey().getName());
            }
        }
        ret.put("Dark", SKIN_DARK);
        for (Map.Entry<SkinDescriptor, SubstanceSkin> entry : SKINS.entrySet()) {
            if (entry.getKey().isDark) {
                ret.put("        " + entry.getKey().getName(), entry.getKey().getName());
            }
        }

        return ret;
    }

    private static class SkinDescriptor {
        private final String name;
        private final boolean requiresBorderedTextFields;
        private final boolean isDark;

        SkinDescriptor(String name, boolean requiresBorderedTextFields, boolean isDark) {
            this.name = name;
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
