package net.miginfocom;
/*
 * License (BSD):
 * ==============
 *
 * Copyright (c) 2004, Mikael Grev, MiG InfoCom AB. (miglayout (at) miginfocom (dot) com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * Neither the name of the MiG InfoCom AB nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 * @version 1.0
 * @author Mikael Grev, MiG InfoCom AB
 *         Date: 2006-sep-08
 */

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;

import com.databazoo.tools.Dbg;

import static net.miginfocom.LayoutUtil.MIG_INFO_ISSUE;

/**
 */
class SwingComponentWrapper implements ComponentWrapper {

    /**
     * Debug color for component bounds outline.
     */
    private static final Color DB_COMP_OUTLINE = new Color(0, 0, 200);
    /**
     * Cache.
     */
    private static final IdentityHashMap<FontMetrics, Point.Float> FM_MAP = new IdentityHashMap<>(4);
    private static final Font SUBST_FONT = new Font("sansserif", Font.PLAIN, 11);
    /**
     * Cached method used for getting base line with reflection.
     */
    private static Method BL_METHOD = null;
    private static Method BL_RES_METHOD = null;
    private static Method IMS_METHOD = null;

    static {
        try {
            BL_METHOD = Component.class.getDeclaredMethod("getBaseline", int.class, int.class);
            BL_RES_METHOD = Component.class.getDeclaredMethod("getBaselineResizeBehavior"); // 3.7.2: Removed Class<?> null since that made the method inaccessible.
        } catch (Exception ignored) {
            Dbg.notImportant(MIG_INFO_ISSUE, ignored);
        }
        try {
            IMS_METHOD = Component.class.getDeclaredMethod("isMaximumSizeSet", (Class[]) null);
        } catch (Exception ignored) {
            Dbg.notImportant(MIG_INFO_ISSUE, ignored);
        }
    }

    private static boolean isMaxSizeSetOn1_4() {
        return false;
    }

    private final Component c;
    private Boolean bl = null;
    private boolean prefCalled = false;

    SwingComponentWrapper(Component c) {
        this.c = c;
    }

    @Override
    public final int getBaseline(int width, int height) {
        if (BL_METHOD == null) {
            return -1;
        }

        try {
            Object[] args = new Object[]{
                    width < 0 ? c.getWidth() : width,
                    height < 0 ? c.getHeight() : height
            };

            return (Integer) BL_METHOD.invoke(c, args);
        } catch (Exception ignored) {
            Dbg.notImportant(MIG_INFO_ISSUE, ignored);
            return -1;
        }
    }

    @Override
    public final Object getComponent() {
        return c;
    }


    @Override
    public final float getPixelUnitFactor(boolean isHor) {
        switch (PlatformDefaults.getLogicalPixelBase()) {
            case PlatformDefaults.BASE_FONT_SIZE:
                Font font = c.getFont();
                FontMetrics fm = c.getFontMetrics(font != null ? font : SUBST_FONT);
                Point.Float p = FM_MAP.get(fm);
                if (p == null) {
                    Rectangle2D r = fm.getStringBounds("X", c.getGraphics());
                    p = new Point.Float(((float) r.getWidth()) / 6f, ((float) r.getHeight()) / 13.27734375f);
                    FM_MAP.put(fm, p);
                }
                return isHor ? p.x : p.y;

            case PlatformDefaults.BASE_SCALE_FACTOR:

                Float s = isHor ? PlatformDefaults.getHorizontalScaleFactor() : PlatformDefaults.getVerticalScaleFactor();
                if (s != null) {
                    return s;
                }
                return (isHor ? getHorizontalScreenDPI() : getVerticalScreenDPI()) / (float) PlatformDefaults.getDefaultDPI();

            default:
                return 1f;
        }
    }

//	/** Cache.
//	 */
//	private final static IdentityHashMap<FontMetrics, Point.Float> FM_MAP2 = new IdentityHashMap<FontMetrics, Point.Float>(4);
//	private final static Font SUBST_FONT2 = new Font("sansserif", Font.PLAIN, 11);
//
//	public float getDialogUnit(boolean isHor)
//	{
//		Font font = c.getFont();
//		FontMetrics fm = c.getFontMetrics(font != null ? font : SUBST_FONT2);
//		Point.Float dluP = FM_MAP2.get(fm);
//		if (dluP == null) {
//			float w = fm.charWidth('X') / 4f;
//			int ascent = fm.getAscent();
//			float h = (ascent > 14 ? ascent : ascent + (15 - ascent) / 3) / 8f;
//
//			dluP = new Point.Float(w, h);
//			FM_MAP2.put(fm, dluP);
//		}
//		return isHor ? dluP.x : dluP.y;
//	}

    @Override
    public final int getX() {
        return c.getX();
    }

    @Override
    public final int getY() {
        return c.getY();
    }

    @Override
    public final int getHeight() {
        return c.getHeight();
    }

    @Override
    public final int getWidth() {
        return c.getWidth();
    }

    @Override
    public final int getScreenLocationX() {
        Point p = new Point();
        SwingUtilities.convertPointToScreen(p, c);
        return p.x;
    }

    @Override
    public final int getScreenLocationY() {
        Point p = new Point();
        SwingUtilities.convertPointToScreen(p, c);
        return p.y;
    }

    @Override
    public final int getMinimumHeight(int sz) {
        if (!prefCalled) {
            c.getPreferredSize(); // To defeat a bug where the minimum size is different before and after the first call to getPreferredSize();
            prefCalled = true;
        }
        return c.getMinimumSize().height;
    }

    @Override
    public final int getMinimumWidth(int sz) {
        if (!prefCalled) {
            c.getPreferredSize(); // To defeat a bug where the minimum size is different before and after the first call to getPreferredSize();
            prefCalled = true;
        }
        return c.getMinimumSize().width;
    }

    @Override
    public final int getPreferredHeight(int sz) {
        // If the component has not gotten size yet and there is a size hint, trick Swing to return a better height.
        if (c.getWidth() == 0 && c.getHeight() == 0 && sz != -1) {
            c.setBounds(c.getX(), c.getY(), sz, 1);
        }

        return c.getPreferredSize().height;
    }

    @Override
    public final int getPreferredWidth(int sz) {
        // If the component has not gotten size yet and there is a size hint, trick Swing to return a better height.
        if (c.getWidth() == 0 && c.getHeight() == 0 && sz != -1) {
            c.setBounds(c.getX(), c.getY(), 1, sz);
        }

        return c.getPreferredSize().width;
    }

    @Override
    public final int getMaximumHeight(int sz) {
        if (!isMaxSet(c)) {
            return Short.MAX_VALUE;
        }

        return c.getMaximumSize().height;
    }

    @Override
    public final int getMaximumWidth(int sz) {
        if (!isMaxSet(c)) {
            return Short.MAX_VALUE;
        }

        return c.getMaximumSize().width;
    }


    private boolean isMaxSet(Component c) {
        if (IMS_METHOD != null) {
            try {
                return (Boolean) IMS_METHOD.invoke(c, (Object[]) null);
            } catch (Exception ex) {
                Dbg.notImportant(MIG_INFO_ISSUE, ex);
                IMS_METHOD = null;  // So we do not try every time.
            }
        }
        return isMaxSizeSetOn1_4();
    }

    @Override
    public final ContainerWrapper getParent() {
        Container p = c.getParent();
        return p != null ? new SwingContainerWrapper(p) : null;
    }

    @Override
    public final int getHorizontalScreenDPI() {
        return PlatformDefaults.getDefaultDPI();
    }

    @Override
    public final int getVerticalScreenDPI() {
        return PlatformDefaults.getDefaultDPI();
    }

    @Override
    public final int getScreenWidth() {
        try {
            return c.getToolkit().getScreenSize().width;
        } catch (HeadlessException ex) {
            Dbg.notImportant(MIG_INFO_ISSUE, ex);
            return 1024;
        }
    }

    @Override
    public final int getScreenHeight() {
        try {
            return c.getToolkit().getScreenSize().height;
        } catch (HeadlessException ex) {
            Dbg.notImportant(MIG_INFO_ISSUE, ex);
            return 768;
        }
    }

    @Override
    public final boolean hasBaseline() {
        if (bl == null) {
            try {
                if (BL_RES_METHOD == null || BL_RES_METHOD.invoke(c).toString().equals("OTHER")) {
                    bl = Boolean.FALSE;
                } else {
                    Dimension d = c.getMinimumSize();
                    bl = getBaseline(d.width, d.height) > -1;
                }
            } catch (Exception ignored) {
                Dbg.notImportant(MIG_INFO_ISSUE, ignored);
                bl = Boolean.FALSE;
            }
        }
        return bl;
    }

    @Override
    public final String getLinkId() {
        return c.getName();
    }

    @Override
    public final void setBounds(int x, int y, int width, int height) {
        c.setBounds(x, y, width, height);
    }

    @Override
    public boolean isVisible() {
        return c.isVisible();
    }

    @Override
    public final int[] getVisualPadding() {
        if (c instanceof JTabbedPane) {
            if (UIManager.getLookAndFeel().getClass().getName().endsWith("WindowsLookAndFeel")) {
                return new int[]{-1, 0, 2, 2};
            }
        }

        return null;
    }

    @Override
    public int getLayoutHashCode() {
        Dimension d = c.getMaximumSize();
        int hash = d.width + (d.height << 5);

        d = c.getPreferredSize();
        hash += (d.width << 10) + (d.height << 15);

        d = c.getMinimumSize();
        hash += (d.width << 20) + (d.height << 25);

        if (c.isVisible()) {
            hash += 1324511;
        }

        String id = getLinkId();
        if (id != null) {
            hash += id.hashCode();
        }

        return hash;
    }

    @Override
    public final int hashCode() {
        return getComponent().hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        return o instanceof ComponentWrapper && getComponent().equals(((ComponentWrapper) o).getComponent());

    }

}
