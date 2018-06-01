package com.databazoo.devmodeler.gui;

import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.components.elements.LineComponent;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.gui.view.ViewMode;
import com.databazoo.devmodeler.model.*;
import com.databazoo.devmodeler.model.Package;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.tools.Geometry;
import com.databazoo.tools.Usage;

import javax.swing.*;
import java.awt.*;

import static com.databazoo.devmodeler.gui.UsageElement.LEFT_MENU_BTN_DATA;
import static com.databazoo.devmodeler.gui.UsageElement.LEFT_MENU_BTN_EDIT;

/**
 * Model preview and navigation component. Is placed in the right-bottom corner of visible canvas.
 *
 * @author bobus
 */
public final class HotMenu extends JComponent {
    public static final HotMenu instance = new HotMenu();
    private JButton menuBtnProperties, menuBtnData;

    private HotMenu() {
        setLayout(new GridLayout(1, 0, 1, 0));
        add(drawEditButton());
        add(drawDataButton());
        setSize(new Dimension(90, 28));
    }

    private JButton drawEditButton() {
        menuBtnProperties = new JButton(Theme.getSmallIcon(Theme.ICO_EDIT));
        menuBtnProperties.setToolTipText("Properties of selected entity");
        menuBtnProperties.addActionListener(e -> {
            Usage.log(LEFT_MENU_BTN_EDIT);
            ViewMode viewOld = DesignGUI.getView();
            DesignGUI.get().switchView(ViewMode.DESIGNER, false);
            Canvas.instance.getSelectedElement().doubleClicked();
            DesignGUI.get().switchView(viewOld, false);
        });
        menuBtnProperties.setFocusable(false);
        menuBtnProperties.setEnabled(false);
        return menuBtnProperties;
    }

    private JButton drawDataButton() {
        menuBtnData = new JButton(Theme.getSmallIcon(Theme.ICO_DATA));
        menuBtnData.setToolTipText("View data of selected table");
        menuBtnData.addActionListener(e -> {
            Usage.log(LEFT_MENU_BTN_DATA);
            ViewMode viewOld = DesignGUI.getView();
            DesignGUI.get().switchView(ViewMode.DATA, false);
            Canvas.instance.getSelectedElement().doubleClicked();
            DesignGUI.get().switchView(viewOld, false);
        });
        menuBtnData.setFocusable(false);
        menuBtnData.setEnabled(false);
        return menuBtnData;
    }

    public void checkSize() {
        if (Project.getCurrent() == null || Project.getCurrent().getCurrentWorkspace() != null) {
            setVisible(false);
            return;
        }
        IModelElement element = Canvas.instance.getSelectedElement();
        if (element instanceof Relation ||
                element instanceof View ||
                element instanceof Constraint) {
            menuBtnProperties.setEnabled(true);
            menuBtnData.setEnabled(true);

        } else if (element instanceof Function ||
                element instanceof Package ||
                element instanceof Schema ||
                element instanceof Trigger ||
                element instanceof Sequence) {
            menuBtnProperties.setEnabled(true);
            menuBtnData.setEnabled(false);

        } else {
            menuBtnProperties.setEnabled(false);
            menuBtnData.setEnabled(false);
            setVisible(false);
            return;
        }
        setVisible(true);
        checkLocation(element);
    }

    private void checkLocation(IModelElement element) {
        if (element instanceof LineComponent) {
            LineComponent component = (LineComponent) element;
            Point center = component.getAbsCenter();
            if(center != null) {
                setLocation(center.x - getWidth() / 2 + 1, center.y - getHeight() / 2);
            }
        } else if (element != null) {
            DraggableComponent component = (DraggableComponent) element;
            Point center = Geometry.getZoomed(component.getAbsCenter());
            if(center != null) {
                setLocation(center.x - getWidth()/2 - 2, center.y - component.getHeight()/2 - getHeight());
            }
        }
    }
}
