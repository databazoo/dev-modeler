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
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A very flexible layout manager.
 * <p>
 * Read the documentation that came with this layout manager for information on usage.
 */
public final class MigLayout implements LayoutManager2, Externalizable {
    // ******** Instance part ********

    /**
     * The component to string constraints mappings.
     */
    private final transient Map<Component, Object> scrConstrMap = new IdentityHashMap<>(8);

    // ******** Transient part ********

    private transient ContainerWrapper cacheParentW = null;

    private transient final Map<ComponentWrapper, CC> ccMap = new HashMap<>(8);

    private transient LC lc = null;
    private transient AC colSpecs = null, rowSpecs = null;
    private transient Grid grid = null;
    private transient int lastModCount = PlatformDefaults.getModCount();
    private transient int lastHash = -1;
    private transient Dimension lastInvalidSize = null;
    private transient boolean lastWasInvalid = false;  // Added in 3.7.1. May have regressions
    private transient Dimension lastParentSize = null;

    private transient boolean dirty = true;
    private long lastSize = 0;

    /**
     * Constructor with no constraints.
     */
    public MigLayout() {
        this("", "", "");
    }

    /**
     * Constructor.
     *
     * @param layoutConstraints The constraints that concern the whole layout. <code>null</code> will be treated as "".
     */
    public MigLayout(String layoutConstraints) {
        this(layoutConstraints, "", "");
    }

    /**
     * Constructor.
     *
     * @param layoutConstraints The constraints that concern the whole layout. <code>null</code> will be treated as "".
     * @param colConstraints    The constraints for the columns in the grid. <code>null</code> will be treated as "".
     */
    public MigLayout(String layoutConstraints, String colConstraints) {
        this(layoutConstraints, colConstraints, "");
    }

    /**
     * Constructor.
     *
     * @param layoutConstraints The constraints that concern the whole layout. <code>null</code> will be treated as "".
     * @param colConstraints    The constraints for the columns in the grid. <code>null</code> will be treated as "".
     * @param rowConstraints    The constraints for the rows in the grid. <code>null</code> will be treated as "".
     */
    public MigLayout(String layoutConstraints, String colConstraints, String rowConstraints) {
        setLayoutConstraints(layoutConstraints);
        setColumnConstraints(colConstraints);
        setRowConstraints(rowConstraints);
    }

    /**
     * Sets the layout constraints for the layout manager instance as a String.
     * <p>
     * See the class JavaDocs for information on how this string is formatted.
     *
     * @param constr The layout constraints as a String representation. <code>null</code> is converted to <code>""</code> for storage.
     * @throws RuntimeException if the constraint was not valid.
     */
    private void setLayoutConstraints(Object constr) {
        if (constr == null || constr instanceof String) {
            constr = ConstraintParser.prepare((String) constr);
            lc = ConstraintParser.parseLayoutConstraint((String) constr);
        } else if (constr instanceof LC) {
            lc = (LC) constr;
        } else {
            throw new IllegalArgumentException("Illegal constraint type: " + constr.getClass().toString());
        }
        dirty = true;
    }

    /**
     * Sets the column layout constraints for the layout manager instance as a String.
     * <p>
     * See the class JavaDocs for information on how this string is formatted.
     *
     * @param constr The column layout constraints as a String representation. <code>null</code> is converted to <code>""</code> for storage.
     * @throws RuntimeException if the constraint was not valid.
     */
    private void setColumnConstraints(Object constr) {
        if (constr == null || constr instanceof String) {
            constr = ConstraintParser.prepare((String) constr);
            colSpecs = ConstraintParser.parseColumnConstraints((String) constr);
        } else if (constr instanceof AC) {
            colSpecs = (AC) constr;
        } else {
            throw new IllegalArgumentException("Illegal constraint type: " + constr.getClass().toString());
        }
        dirty = true;
    }

    /**
     * Sets the row layout constraints for the layout manager instance as a String.
     * <p>
     * See the class JavaDocs for information on how this string is formatted.
     *
     * @param constr The row layout constraints as a String representation. <code>null</code> is converted to <code>""</code> for storage.
     * @throws RuntimeException if the constraint was not valid.
     */
    private void setRowConstraints(Object constr) {
        if (constr == null || constr instanceof String) {
            constr = ConstraintParser.prepare((String) constr);
            rowSpecs = ConstraintParser.parseRowConstraints((String) constr);
        } else if (constr instanceof AC) {
            rowSpecs = (AC) constr;
        } else {
            throw new IllegalArgumentException("Illegal constraint type: " + constr.getClass().toString());
        }
        dirty = true;
    }

    /**
     * Sets the component constraint for the component that already must be handled by this layout manager.
     * <p>
     * See the class JavaDocs for information on how this string is formatted.
     *
     * @param constr  The component constraints as a String or {@link CC}. <code>null</code> is ok.
     * @param comp    The component to set the constraints for.
     * @param noCheck Doe not check if the component is handled if true
     * @throws RuntimeException         if the constraint was not valid.
     * @throws IllegalArgumentException If the component is not handling the component.
     */
    private void setComponentConstraintsImpl(Component comp, Object constr, boolean noCheck) {
        Container parent = comp.getParent();
        synchronized (parent != null ? parent.getTreeLock() : new Object()) { // 3.7.2. No sync if not added to a hierarchy. Defeats a NPE.
            if (!noCheck && !scrConstrMap.containsKey(comp)) {
                throw new IllegalArgumentException("Component must already be added to parent!");
            }

            ComponentWrapper cw = new SwingComponentWrapper(comp);

            if (constr == null || constr instanceof String) {
                String cStr = ConstraintParser.prepare((String) constr);

                scrConstrMap.put(comp, constr);
                ccMap.put(cw, ConstraintParser.parseComponentConstraint(cStr));

            } else if (constr instanceof CC) {

                scrConstrMap.put(comp, constr);
                ccMap.put(cw, (CC) constr);

            } else {
                throw new IllegalArgumentException("Constraint must be String or ComponentConstraint: " + constr.getClass().toString());
            }

            dirty = true;
        }
    }

    /**
     * Check if something has changed and if so recreate it to the cached objects.
     *
     * @param parent The parent that is the target for this layout manager.
     */
    private void checkCache(Container parent) {
        if (parent == null) {
            return;
        }

        if (dirty) {
            grid = null;
        }

        // Check if the grid is valid
        int mc = PlatformDefaults.getModCount();
        if (lastModCount != mc) {
            grid = null;
            lastModCount = mc;
        }

        if (!parent.isValid()) {
            if (!lastWasInvalid) {
                lastWasInvalid = true;

                int hash = 0;
                boolean resetLastInvalidOnParent = false; // Added in 3.7.3 to resolve a timing regression introduced in 3.7.1
                for (ComponentWrapper wrapper : ccMap.keySet()) {
                    Object component = wrapper.getComponent();
                    if (component instanceof JTextArea || component instanceof JEditorPane) {
                        resetLastInvalidOnParent = true;
                    }
                    hash ^= wrapper.getLayoutHashCode();
                    hash += 285134905;
                }
                if (resetLastInvalidOnParent) {
                    resetLastInvalidOnParent(parent);
                }

                if (hash != lastHash) {
                    grid = null;
                    lastHash = hash;
                }

                Dimension ps = parent.getSize();
                if (lastInvalidSize == null || !lastInvalidSize.equals(ps)) {
                    if (grid != null) {
                        grid.invalidateContainerSize();
                    }
                    lastInvalidSize = ps;
                }
            }
        } else {
            lastWasInvalid = false;
        }

        ContainerWrapper par = checkParent(parent);

        if (grid == null) {
            grid = new Grid(par, lc, rowSpecs, colSpecs, ccMap);
        }

        dirty = false;
    }

    /**
     * @since 3.7.3
     */
    private void resetLastInvalidOnParent(Container parent) {
        while (parent != null) {
            LayoutManager layoutManager = parent.getLayout();
            if (layoutManager instanceof MigLayout) {
                ((MigLayout) layoutManager).lastWasInvalid = false;
            }
            parent = parent.getParent();
        }
    }

    private ContainerWrapper checkParent(Container parent) {
        if (parent == null) {
            return null;
        }

        if (cacheParentW == null || cacheParentW.getComponent() != parent) {
            cacheParentW = new SwingContainerWrapper(parent);
        }

        return cacheParentW;
    }


    @Override
    public void layoutContainer(final Container parent) {
        synchronized (parent.getTreeLock()) {
            checkCache(parent);

            Insets i = parent.getInsets();
            int[] b = new int[]{
                    i.left,
                    i.top,
                    parent.getWidth() - i.left - i.right,
                    parent.getHeight() - i.top - i.bottom
            };

            if (grid.layout(b, lc.getAlignX(), lc.getAlignY(), true)) {
                grid = null;
                checkCache(parent);
                grid.layout(b, lc.getAlignX(), lc.getAlignY(), false);
            }

            long newSize = grid.getHeight()[1] + (((long) grid.getWidth()[1]) << 32);
            if (lastSize != newSize) {
                lastSize = newSize;
                final ContainerWrapper containerWrapper = checkParent(parent);
                Window win = ((Window) SwingUtilities.getAncestorOfClass(Window.class, (Component) containerWrapper.getComponent()));
                if (win != null) {
                    if (win.isVisible()) {
                        SwingUtilities.invokeLater(() -> adjustWindowSize(containerWrapper));
                    } else {
                        adjustWindowSize(containerWrapper);
                    }
                }
            }
            lastInvalidSize = null;
        }
    }

    /**
     * Checks the parent window if its size is within parameters as set by the LC.
     *
     * @param parent The parent who's window to possibly adjust the size for.
     */
    private void adjustWindowSize(ContainerWrapper parent) {
        BoundSize wBounds = lc.getPackWidth();
        BoundSize hBounds = lc.getPackHeight();

        if (wBounds == null && hBounds == null) {
            return;
        }

        Window win = ((Window) SwingUtilities.getAncestorOfClass(Window.class, (Component) parent.getComponent()));
        if (win == null) {
            return;
        }

        Dimension prefSize = win.getPreferredSize();
        int targW = constrain(checkParent(win), win.getWidth(), prefSize.width, wBounds);
        int targH = constrain(checkParent(win), win.getHeight(), prefSize.height, hBounds);

        int x = Math.round(win.getX() - ((targW - win.getWidth()) * (1 - lc.getPackWidthAlign())));
        int y = Math.round(win.getY() - ((targH - win.getHeight()) * (1 - lc.getPackHeightAlign())));

        win.setBounds(x, y, targW, targH);
    }

    private int constrain(ContainerWrapper parent, int winSize, int prefSize, BoundSize constrain) {
        if (constrain == null) {
            return winSize;
        }

        int retSize = winSize;
        UnitValue wUV = constrain.getPreferred();
        if (wUV != null) {
            retSize = wUV.getPixels(prefSize, parent, parent);
        }

        retSize = constrain.constrain(retSize, prefSize, parent);

        return constrain.getGapPush() ? Math.max(winSize, retSize) : retSize;
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            return getSizeImpl(parent, LayoutUtil.MIN);
        }
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            if (lastParentSize == null || !parent.getSize().equals(lastParentSize)) {
                for (ComponentWrapper wrapper : ccMap.keySet()) {
                    Component c = (Component) wrapper.getComponent();
                    if (c instanceof JTextArea || c instanceof JEditorPane || (c instanceof JComponent && Boolean.TRUE.equals(((JComponent) c).getClientProperty("migLayout.dynamicAspectRatio")))) {
                        layoutContainer(parent);
                        break;
                    }
                }
            }

            lastParentSize = parent.getSize();
            return getSizeImpl(parent, LayoutUtil.PREF);
        }
    }

    @Override
    public Dimension maximumLayoutSize(Container parent) {
        return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    // Implementation method that does the job.
    private Dimension getSizeImpl(Container parent, int sizeType) {
        checkCache(parent);

        Insets i = parent != null ? parent.getInsets() : new Insets(0, 0, 0, 0);

        int w = LayoutUtil.getSizeSafe(grid != null ? grid.getWidth() : null, sizeType) + i.left + i.right;
        int h = LayoutUtil.getSizeSafe(grid != null ? grid.getHeight() : null, sizeType) + i.top + i.bottom;

        return new Dimension(w, h);
    }

    @Override
    public float getLayoutAlignmentX(Container parent) {
        return lc != null && lc.getAlignX() != null ? lc.getAlignX().getPixels(1, checkParent(parent), null) : 0;
    }

    @Override
    public float getLayoutAlignmentY(Container parent) {
        return lc != null && lc.getAlignY() != null ? lc.getAlignY().getPixels(1, checkParent(parent), null) : 0;
    }

    @Override
    public void addLayoutComponent(String s, Component comp) {
        addLayoutComponent(comp, s);
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        synchronized (comp.getParent().getTreeLock()) {
            setComponentConstraintsImpl(comp, constraints, true);
        }
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        synchronized (comp.getParent().getTreeLock()) {
            scrConstrMap.remove(comp);
            ccMap.remove(new SwingComponentWrapper(comp));
        }
    }

    @Override
    public void invalidateLayout(Container target) {
//		if (lc.isNoCache())  // Commented for 3.5 since there was too often that the "nocache" was needed and the user did not know.
        dirty = true;

        // the validity of components is maintained automatically.
    }

    // ************************************************
    // Persistence Delegate and Serializable combined.
    // ************************************************

    private Object readResolve() throws ObjectStreamException {
        return LayoutUtil.getSerializedObject(this);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        LayoutUtil.setSerializedObject(this, LayoutUtil.readAsXML(in));
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        if (getClass() == MigLayout.class) {
            LayoutUtil.writeAsXML(out, this);
        }
    }
}