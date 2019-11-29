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
import com.databazoo.tools.Schedule;
import com.databazoo.tools.Usage;

import javax.swing.*;
import java.awt.*;

import static com.databazoo.components.UIConstants.MENU_COMPONENT_HEIGHT;
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
        Schedule.reInvokeInEDT(Schedule.Named.HOT_MENU_RECHECK, Schedule.CLICK_DELAY, () -> {
            setVisible(false);
            if (Project.getCurrent() == null || Project.getCurrent().getCurrentWorkspace() != null) {
                return;
            }

            removeAll();
            IModelElement element = Canvas.instance.getSelectedElement();
            if (element instanceof Relation ||
                    element instanceof View ||
                    element instanceof Constraint) {
                add(Buttons.PROPERTIES);
                if (Project.getCurrent().getType() != Project.TYPE_ABSTRACT) {
                    add(Buttons.DATA);
                }

            } else if (element instanceof Function) {
                add(Buttons.PROPERTIES);

                Buttons.function = (Function) element;
                if (Project.getCurrent().getType() != Project.TYPE_ABSTRACT &&
                        !Buttons.function.getBehavior().getRetType().equals(Function.TRIGGER)) {
                    add(Buttons.RUN);
                }

            } else if (element instanceof Package ||
                    //element instanceof Schema ||
                    element instanceof Trigger ||
                    element instanceof Sequence) {
                add(Buttons.PROPERTIES);

            } else {
                return;
            }
            add(Buttons.COPY);

            setSize(new Dimension(45 * getComponentCount(), MENU_COMPONENT_HEIGHT));
            setVisible(true);

            checkLocation(element);
        });
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
        private static final JButton PROPERTIES = new JButton(Theme.getSmallIcon(Theme.ICO_EDIT));
        private static final JButton DATA = new JButton(Theme.getSmallIcon(Theme.ICO_DATA));
        private static final JButton RUN = new JButton(Theme.getSmallIcon(Theme.ICO_RUN));
        private static final JButton COPY = new JButton(Theme.getSmallIcon(Theme.ICO_COPY));

        private static Function function;

        static {
            drawEditButton();
            drawDataButton();
            drawRunButton();
            drawCopyButton();
        }

        private static void drawEditButton() {
            PROPERTIES.setToolTipText("Properties");
            PROPERTIES.addActionListener(e -> {
                Usage.log(LEFT_MENU_BTN_EDIT);
                ViewMode viewOld = DesignGUI.getView();
                DesignGUI.get().switchView(ViewMode.DESIGNER, false);
                Canvas.instance.getSelectedElement().doubleClicked();
                DesignGUI.get().switchView(viewOld, false);
            });
            PROPERTIES.setFocusable(false);
        }

        private static void drawDataButton() {
            DATA.setToolTipText("View data");
            DATA.addActionListener(e -> {
                Usage.log(LEFT_MENU_BTN_DATA);
                ViewMode viewOld = DesignGUI.getView();
                DesignGUI.get().switchView(ViewMode.DATA, false);
                Canvas.instance.getSelectedElement().doubleClicked();
                DesignGUI.get().switchView(viewOld, false);
            });
            DATA.setFocusable(false);
        }

        private static void drawRunButton() {
            RUN.setToolTipText("Run");
            RUN.addActionListener(e -> DataWindow.get().drawQueryWindow(Project.getCurrent().getCurrentConn().getQueryExecFunction(function), Project.getCurrDB()));
            RUN.setFocusable(false);
        }

        private static void drawCopyButton() {
            COPY.setToolTipText("Copy name to clipboard");
            COPY.addActionListener(e -> {
                DesignGUI.toClipboard(Canvas.instance.getSelectedElement().getName());
            });
            COPY.setFocusable(false);
        }
    }
}
