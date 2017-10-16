package com.databazoo.devmodeler.gui;

import com.databazoo.components.elements.ClickableComponent;
import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Workspace;
import com.databazoo.devmodeler.model.reference.DraggableComponentReference;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;
import com.databazoo.devmodeler.tools.Geometry;
import com.databazoo.tools.Schedule;
import com.databazoo.tools.Usage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import static com.databazoo.devmodeler.gui.UsageElement.NAVIGATOR_DRAGGED;
import static com.databazoo.devmodeler.gui.UsageElement.NAVIGATOR_ZOOMED;

/**
 * Model preview and navigation component. Is placed in the right-bottom corner of visible canvas.
 *
 * @author bobus
 */
public final class Navigator extends JComponent {
    private static final int SLIDER_DEF = 3;
    private static final Double[] SLIDER_MAP = new Double[] { 0.20, 0.40, 0.70, 1.0, 1.25, 1.60 };
    private static final boolean SLIDER_TICKS = Settings.getBool(Settings.L_LAYOUT_NAV_ZOOM_TICKS);
    private static final int ZOOM_BUTTON_SIZE = 24;
    private static final int CANVAS_OFFSET = ZOOM_BUTTON_SIZE + (SLIDER_TICKS ? 8 : 1);
    private static final int SLIDER_OFFSET = SLIDER_TICKS ? 5 : 2;
    public static final Navigator instance = new Navigator();
    private final NavigatorCanvas canvas = new NavigatorCanvas();
    private final JSlider zoomSlider = new JSlider(JSlider.VERTICAL, 0, SLIDER_MAP.length - 1, SLIDER_DEF);
    private final JButton zoomInButton = new JButton(Theme.getSmallIcon(Theme.ICO_ZOOM_IN));
    private final JButton zoomOutButton = new JButton(Theme.getSmallIcon(Theme.ICO_ZOOM_OUT));

    private Navigator() {
        setLayout(null);
        checkSize();
        add(canvas);
        add(drawZoomSlider());
        add(drawZoomInButton());
        add(drawZoomOutButton());
        setComponentZOrder(zoomSlider, 0);
        setComponentZOrder(zoomInButton, 1);
        setComponentZOrder(zoomOutButton, 2);
    }

    private JButton drawZoomInButton() {
        zoomInButton.setSize(ZOOM_BUTTON_SIZE, ZOOM_BUTTON_SIZE);
        zoomInButton.setFocusable(false);
        zoomInButton.addActionListener(e -> zoomIn());
        zoomInButton.setLocation(0, 0);
        return zoomInButton;
    }

    private JButton drawZoomOutButton() {
        zoomOutButton.setSize(ZOOM_BUTTON_SIZE, ZOOM_BUTTON_SIZE);
        zoomOutButton.setFocusable(false);
        zoomOutButton.addActionListener(e -> zoomOut());
        return zoomOutButton;
    }

    private JSlider drawZoomSlider() {
        zoomSlider.setLocation(SLIDER_OFFSET, ZOOM_BUTTON_SIZE);
        zoomSlider.setMajorTickSpacing(1);
        zoomSlider.setPaintTicks(SLIDER_TICKS);
        zoomSlider.setSnapToTicks(true);
        zoomSlider.addChangeListener(evt -> {
            JSlider slider = (JSlider) evt.getSource();
            if (!slider.getValueIsAdjusting()) {
                Canvas.setZoom(SLIDER_MAP[slider.getValue()]);
                Index.checkFontSize();
                Canvas.instance.drawProject(true);
            }
        });
        return zoomSlider;
    }

    public void checkSize() {
        canvas.checkSize();
        Dimension d = canvas.getSize();
        d.width += CANVAS_OFFSET;
        setSize(d);
        zoomSlider.setSize(20, getHeight() - ZOOM_BUTTON_SIZE * 2);
        zoomOutButton.setLocation(0, getHeight() - ZOOM_BUTTON_SIZE);
    }

    public void checkSchemata(){
        Schedule.reInvokeInEDT(Schedule.Named.NAVIGATOR_CHECK_SCHEMATA, Schedule.CLICK_DELAY, () -> {
            canvas.drawSchemata();
            repaint();
        });
    }

    private boolean canZoomIn() {
        return zoomSlider.getValue() < zoomSlider.getMaximum();
    }

    void zoomIn() {
        Usage.log(NAVIGATOR_ZOOMED);
        if (canZoomIn()) {
            zoomSlider.setValue(zoomSlider.getValue() + 1);
            updateMenu();
        }
    }

    private boolean canZoomOut() {
        return zoomSlider.getValue() > zoomSlider.getMinimum();
    }

    void zoomOut() {
        Usage.log(NAVIGATOR_ZOOMED);
        if (canZoomOut()) {
            zoomSlider.setValue(zoomSlider.getValue() - 1);
            updateMenu();
        }
    }

    void resetZoom() {
        if (canZoomOut()) {
            zoomSlider.setValue(SLIDER_DEF);
            updateMenu();
        }
    }

    private void updateMenu() {
        Menu.getInstance().getZoomInMenuItem().setEnabled(canZoomIn());
        Menu.getInstance().getZoomOutMenuItem().setEnabled(canZoomOut());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    /**
     * Navigation canvas, where shemata (and possibly other items) can be previewed.
     */
    private static class NavigatorCanvas extends ClickableComponent {
        private static final Color PANEL_BG = Color.LIGHT_GRAY;
        private static final Color PANEL_FG = Color.WHITE;
        private static final Color PANEL_BG_NT = Color.decode("#e1e1e1");
        private Rectangle[] schemata;
        private double sizeCoeff = 0.0;

        NavigatorCanvas() {
            if (Settings.getBool(Settings.L_LAYOUT_NAV_TRANSPARENT)) {
                setBackground(new Color(PANEL_BG.getRed(), PANEL_BG.getGreen(), PANEL_BG.getBlue(), 120));
            } else {
                setBackground(PANEL_BG_NT);
            }
            setForeground(new Color(PANEL_FG.getRed(), PANEL_FG.getGreen(), PANEL_FG.getBlue(), 120));
            setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            setLocation(CANVAS_OFFSET, 0);
            checkSize();
            addDragListeners();
        }

        private void addDragListeners() {
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    setScrollsPosition(e.getPoint());
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    setScrollsPosition(e.getPoint());
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private void setScrollsPosition(Point mousePoint) {
            Usage.log(NAVIGATOR_DRAGGED);
            Point newPos = new Point(
                    (int) Math.round(mousePoint.x / sizeCoeff - Canvas.instance.getScrollSize().width / 2),
                    (int) Math.round(mousePoint.y / sizeCoeff - Canvas.instance.getScrollSize().height / 2));
            Geometry.fitPointToLimits(newPos, new Point(0, 0), Canvas.instance.getScrollMaxSize());
            Canvas.instance.scrollTo(newPos);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int x = Math.round((float) (Canvas.instance.getScrollPosition().x * sizeCoeff));
            int y = Math.round((float) (Canvas.instance.getScrollPosition().y * sizeCoeff));
            int w = Math.round((float) (Canvas.instance.getScrollSize().width * sizeCoeff));
            int h = Math.round((float) (Canvas.instance.getScrollSize().height * sizeCoeff));
            Graphics2D graphics = (Graphics2D) g;
            graphics.setColor(getBackground());
            graphics.fillRect(0, 0, getWidth(), getHeight());
            graphics.setColor(getForeground());
            graphics.fillRect(x, y, w, h);
            if (schemata != null) {
                graphics.setColor(Color.GRAY);
                for (Rectangle schema : schemata) {
                    graphics.drawRect(schema.x, schema.y, schema.width, schema.height);
                }
            }
            IModelElement elem = Canvas.instance.getSelectedElement();
            if (elem != null && elem instanceof DraggableComponent && ((DraggableComponent) elem).getAbsCenter() != null
                    && elem.getDB().equals(Project.getCurrDB())) {
                if (Project.getCurrent().getCurrentWorkspace() == null) {
                    Point center = ((DraggableComponent) elem).getAbsCenter();
                    graphics.setColor(Color.RED);
                    graphics.drawRect(
                            (int) Math.round((Geometry.getZoomed(center.x) - elem.getSize().width / 2) * sizeCoeff),
                            (int) Math.round((Geometry.getZoomed(center.y) - elem.getSize().height / 2) * sizeCoeff),
                            (int) Math.round((elem.getSize().width) * sizeCoeff),
                            (int) Math.round((elem.getSize().height) * sizeCoeff));
                } else {
                    DraggableComponentReference refElem = null;
                    for (DraggableComponentReference ref : Project.getCurrent().getCurrentWorkspace().getRelations()) {
                        if (ref.getElement().equals(elem)) {
                            refElem = ref;
                            break;
                        }
                    }
                    if (refElem == null) {
                        for (DraggableComponentReference ref : Project.getCurrent().getCurrentWorkspace().getRelations()) {
                            if (ref.getElement().equals(elem)) {
                                refElem = ref;
                                break;
                            }
                        }
                    }
                    if (refElem != null) {
                        Point center = refElem.getAbsCenter();
                        graphics.setColor(Color.RED);
                        graphics.drawRect(
                                (int) Math.round((Geometry.getZoomed(center.x) - refElem.getSize().width / 2) * sizeCoeff),
                                (int) Math.round((Geometry.getZoomed(center.y) - refElem.getSize().height / 2) * sizeCoeff),
                                (int) Math.round((refElem.getSize().width) * sizeCoeff),
                                (int) Math.round((refElem.getSize().height) * sizeCoeff));
                    }
                }
            }
            graphics.setColor(getForeground());
            graphics.fillRect(x, y, w, h);
            graphics.setColor(PANEL_FG);
            graphics.drawRect(x - 1, y - 1, w + 1, h + 1);
        }

        @Override
        public void clicked() {
            checkSize();
        }

        @Override
        public void doubleClicked() {
        }

        @Override
        public void rightClicked() {
        }

        public final void checkSize() {
            Dimension newSize;
            if (Canvas.instance != null) {
                int maxSize = Settings.getInt(Settings.L_LAYOUT_NAV_SIZE);
                Dimension canvasSize = Canvas.instance.getSize();
                if (canvasSize.width > canvasSize.height) {
                    newSize = new Dimension(maxSize, (int) Math.round(canvasSize.height * 1.0 / canvasSize.width * maxSize));
                } else {
                    newSize = new Dimension((int) Math.round(canvasSize.width * 1.0 / canvasSize.height * maxSize), maxSize);
                }
                sizeCoeff = newSize.width * 1.0 / canvasSize.width;
                if(!newSize.equals(getSize())) {
                    setSize(newSize);
                    drawSchemata();
                }
            }
        }

        private void drawSchemata() {
            if (ProjectManager.getInstance() != null) {
                Project proj = Project.getCurrent();
                if (proj != null && proj.isLoaded() && proj.getCurrentDB() != null) {
                    Workspace currentWorkspace = proj.getCurrentWorkspace();
                    List schemasInDB = currentWorkspace == null ? proj.getCurrentDB().getSchemas() : currentWorkspace.getSchemas();
                    schemata = new Rectangle[schemasInDB.size()];
                    for (int i = 0; i < schemasInDB.size(); i++) {
                        DraggableComponent schema = (DraggableComponent) schemasInDB.get(i);
                        schemata[i] = new Rectangle((int) Math.round(schema.getLocation().x * sizeCoeff),
                                (int) Math.round(schema.getLocation().y * sizeCoeff),
                                (int) Math.round(schema.getSize().width * sizeCoeff), (int) Math.round(schema.getSize().height * sizeCoeff));
                    }
                }
            }
        }
    }
}
