package com.databazoo.devmodeler.gui;

import com.databazoo.devmodeler.gui.view.ViewMode;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Package;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Sequence;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.model.View;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

import static com.databazoo.devmodeler.gui.view.DifferenceView.L_FUNCTIONS;
import static com.databazoo.devmodeler.gui.view.DifferenceView.L_PACKAGES;
import static com.databazoo.devmodeler.gui.view.DifferenceView.L_SEQUENCES;
import static com.databazoo.devmodeler.gui.view.DifferenceView.L_TABLES;
import static com.databazoo.devmodeler.gui.view.DifferenceView.L_VIEWS;
import static com.databazoo.devmodeler.model.Relation.L_CONSTRAINTS;
import static com.databazoo.devmodeler.model.Relation.L_INDEXES;
import static com.databazoo.devmodeler.model.Relation.L_TRIGGERS;

public class MenuElementView {

    private static MenuElementView instance;

    /**
     * Not thread safe, but it's OK here. This is supposed to be called once when GUI (Menu) is created. For other calls
     * {@link #getInstance()} should be used.
     *
     * @param elementsMenuElem menu element
     * @return MenuElementView instance
     */
    public static MenuElementView getInstance(JMenu elementsMenuElem) {
        instance = new MenuElementView(elementsMenuElem);
        return instance;
    }

    /**
     * @param elementsMenuElem menu element
     * @return MenuElementView instance
     */
    public static MenuElementView getInstance() {
        return instance;
    }

    private final JMenu elementsMenuElem;
    private final Map<ViewMode, Map<String, Boolean>> viewElementVisibility = new HashMap<>();

    /**
     * Constructs the visibility map.
     *
     * TODO: This may be saved and loaded from settings.
     *
     * @param elementsMenuElem menu element
     */
    private MenuElementView(JMenu elementsMenuElem) {
        this.elementsMenuElem = elementsMenuElem;
        HashMap<String, Boolean> visibility;

        visibility = new HashMap<>();
        visibility.put(L_TABLES, true);
        visibility.put(L_INDEXES, true);
        visibility.put(L_CONSTRAINTS, true);
        visibility.put(L_TRIGGERS, true);
        visibility.put(L_FUNCTIONS, true);
        visibility.put(L_VIEWS, true);
        visibility.put(L_SEQUENCES, true);
        visibility.put(L_PACKAGES, true);
        viewElementVisibility.put(ViewMode.DESIGNER, visibility);

        visibility = new HashMap<>();
        visibility.put(L_TABLES, true);
        visibility.put(L_INDEXES, true);
        visibility.put(L_CONSTRAINTS, true);
        visibility.put(L_TRIGGERS, true);
        visibility.put(L_FUNCTIONS, true);
        visibility.put(L_VIEWS, true);
        visibility.put(L_SEQUENCES, true);
        visibility.put(L_PACKAGES, true);
        viewElementVisibility.put(ViewMode.OPTIMIZE, visibility);

        visibility = new HashMap<>();
        visibility.put(L_TABLES, true);
        visibility.put(L_INDEXES, false);
        visibility.put(L_CONSTRAINTS, true);
        visibility.put(L_TRIGGERS, false);
        visibility.put(L_FUNCTIONS, false);
        visibility.put(L_VIEWS, true);
        visibility.put(L_SEQUENCES, false);
        visibility.put(L_PACKAGES, false);
        viewElementVisibility.put(ViewMode.DATA, visibility);

        visibility = new HashMap<>();
        visibility.put(L_TABLES, true);
        visibility.put(L_INDEXES, true);
        visibility.put(L_CONSTRAINTS, true);
        visibility.put(L_TRIGGERS, true);
        visibility.put(L_FUNCTIONS, true);
        visibility.put(L_VIEWS, true);
        visibility.put(L_SEQUENCES, true);
        visibility.put(L_PACKAGES, true);
        viewElementVisibility.put(ViewMode.DIFF, visibility);
    }

    void updateMenuItems() {
        ViewMode currentView = DesignGUI.getView();
        if (currentView != null) {
            Map<String, Boolean> map = instance.viewElementVisibility.get(currentView);
            instance.elementsMenuElem.removeAll();
            instance.elementsMenuElem.add(new Menu.MyCheckboxMenuItem(L_TABLES, Relation.ico16, map.get(L_TABLES)));
            instance.elementsMenuElem.add(new Menu.MyCheckboxMenuItem(L_INDEXES, Index.ico16, map.get(L_INDEXES)));
            instance.elementsMenuElem.add(new Menu.MyCheckboxMenuItem(L_CONSTRAINTS, Constraint.ico16, map.get(L_CONSTRAINTS)));
            instance.elementsMenuElem.add(new Menu.MyCheckboxMenuItem(L_TRIGGERS, Trigger.ico16, map.get(L_TRIGGERS)));
            instance.elementsMenuElem.add(new Menu.MyCheckboxMenuItem(L_FUNCTIONS, Function.ico16, map.get(L_FUNCTIONS)));
            instance.elementsMenuElem.add(new Menu.MyCheckboxMenuItem(L_VIEWS, View.ico16, map.get(L_VIEWS)));
            instance.elementsMenuElem.add(new Menu.MyCheckboxMenuItem(L_SEQUENCES, Sequence.ico16, map.get(L_SEQUENCES)));
            instance.elementsMenuElem.add(new Menu.MyCheckboxMenuItem(L_PACKAGES, Package.ico16, map.get(L_PACKAGES)));
        }
    }

    static MenuElementView setChange(String item, boolean value) {
        ViewMode currentView = DesignGUI.getView();
        if (currentView != null) {
            instance.viewElementVisibility.get(currentView).put(item, value);
        }
        return instance;
    }

    static boolean getVisibility(String item) {
        ViewMode currentView = DesignGUI.getView();
        if (currentView != null) {
            Map<String, Boolean> map = instance.viewElementVisibility.get(currentView);
            return map.get(item);
        }
        return true;
    }
}
