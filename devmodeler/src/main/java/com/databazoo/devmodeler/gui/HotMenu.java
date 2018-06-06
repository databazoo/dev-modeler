package com.databazoo.devmodeler.gui;

import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.components.elements.LineComponent;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.gui.view.ViewMode;
import com.databazoo.devmodeler.gui.window.datawindow.DataWindow;
import com.databazoo.devmodeler.model.*;
import com.databazoo.devmodeler.model.Package;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.tools.Geometry;
import com.databazoo.tools.Usage;

import javax.swing.*;
import java.awt.*;

import static com.databazoo.devmodeler.gui.Menu.COMPONENT_HEIGHT;
import static com.databazoo.devmodeler.gui.UsageElement.LEFT_MENU_BTN_DATA;
import static com.databazoo.devmodeler.gui.UsageElement.LEFT_MENU_BTN_EDIT;

/**
 * Model preview and navigation component. Is placed in the right-bottom corner of visible canvas.
 *
 * @author bobus
 */
public final class HotMenu extends JComponent {
    public static final HotMenu instance = new HotMenu();

    private HotMenu() {
        setLayout(new GridLayout(1, 0, 1, 0));
    }

    public void checkSize() {
        if (Project.getCurrent() == null || Project.getCurrent().getCurrentWorkspace() != null) {
            setVisible(false);
            return;
        }

        removeAll();
        IModelElement element = Canvas.instance.getSelectedElement();
        if (element instanceof Relation ||
                element instanceof View ||
                element instanceof Constraint) {
            add(Buttons.properties);
            add(Buttons.data);

        } else if (element instanceof Function) {
            add(Buttons.properties);

            Buttons.function = (Function) element;
            if (Project.getCurrent().getType() != Project.TYPE_ABSTRACT &&
                    !Buttons.function.getBehavior().getRetType().equals(Function.TRIGGER)) {
                add(Buttons.run);
            }

        } else if (element instanceof Package ||
                //element instanceof Schema ||
                element instanceof Trigger ||
                element instanceof Sequence) {
            add(Buttons.properties);

        } else {
            setVisible(false);
            return;
        }
        add(Buttons.copy);

        setSize(new Dimension(45 * getComponentCount(), COMPONENT_HEIGHT));
        setVisible(true);

        checkLocation(element);
    }

    private void checkLocation(IModelElement element) {
        if (element instanceof LineComponent) {
            LineComponent component = (LineComponent) element;
            Point center = component.getAbsCenter();
            if (center != null) {
                setLocation(center.x - getWidth() / 2 + 1, center.y - getHeight() / 2);
            }
        } else if (element != null) {
            DraggableComponent component = (DraggableComponent) element;
            Point center = Geometry.getZoomed(component.getAbsCenter());
            if (center != null) {
                setLocation(center.x - getWidth() / 2 - 2, center.y - component.getHeight() / 2 - getHeight());
            }
        }
    }

    private static class Buttons {
        private static JButton properties;
        private static JButton data;
        private static JButton run;
        private static JButton copy;

        private static Function function;

        static {
            drawEditButton();
            drawDataButton();
            drawRunButton();
            drawCopyButton();
        }

        private static void drawEditButton() {
            properties = new JButton(Theme.getSmallIcon(Theme.ICO_EDIT));
            properties.setToolTipText("Properties");
            properties.addActionListener(e -> {
                Usage.log(LEFT_MENU_BTN_EDIT);
                ViewMode viewOld = DesignGUI.getView();
                DesignGUI.get().switchView(ViewMode.DESIGNER, false);
                Canvas.instance.getSelectedElement().doubleClicked();
                DesignGUI.get().switchView(viewOld, false);
            });
            properties.setFocusable(false);
        }

        private static void drawDataButton() {
            data = new JButton(Theme.getSmallIcon(Theme.ICO_DATA));
            data.setToolTipText("View data");
            data.addActionListener(e -> {
                Usage.log(LEFT_MENU_BTN_DATA);
                ViewMode viewOld = DesignGUI.getView();
                DesignGUI.get().switchView(ViewMode.DATA, false);
                Canvas.instance.getSelectedElement().doubleClicked();
                DesignGUI.get().switchView(viewOld, false);
            });
            data.setFocusable(false);
        }

        private static void drawRunButton() {
            run = new JButton(Theme.getSmallIcon(Theme.ICO_RUN));
            run.setToolTipText("Run");
            run.addActionListener(e -> DataWindow.get().drawQueryWindow(Project.getCurrent().getCurrentConn().getQueryExecFunction(function), Project.getCurrDB()));
            run.setFocusable(false);
        }

        private static void drawCopyButton() {
            copy = new JButton(Theme.getSmallIcon(Theme.ICO_COPY));
            copy.setToolTipText("Copy name to clipboard");
            copy.addActionListener(e -> {
                DesignGUI.toClipboard(Canvas.instance.getSelectedElement().getName());
            });
            copy.setFocusable(false);
        }
    }
}
