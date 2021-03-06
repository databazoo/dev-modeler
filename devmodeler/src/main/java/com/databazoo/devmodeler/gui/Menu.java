
package com.databazoo.devmodeler.gui;

import com.databazoo.components.FontFactory;
import com.databazoo.components.UIConstants;
import com.databazoo.components.combo.IconableComboBox;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.gui.view.DifferenceView;
import com.databazoo.devmodeler.gui.view.ViewMode;
import com.databazoo.devmodeler.gui.window.AppInfoWindow;
import com.databazoo.devmodeler.gui.window.ServerActivityWindow;
import com.databazoo.devmodeler.gui.window.datawindow.DataWindow;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;
import com.databazoo.devmodeler.tools.organizer.Organizer;
import com.databazoo.devmodeler.tools.organizer.OrganizerFactory;
import com.databazoo.devmodeler.wizards.DocumentationWizard;
import com.databazoo.devmodeler.wizards.ExportImportWizard;
import com.databazoo.devmodeler.wizards.SettingsWizard;
import com.databazoo.devmodeler.wizards.project.ProjectWizard;
import com.databazoo.devmodeler.wizards.server.ServerAdministrationWizard;
import com.databazoo.tools.Schedule;
import com.databazoo.tools.Usage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import static com.databazoo.components.UIConstants.*;
import static com.databazoo.devmodeler.conn.SupportedElement.SERVER_ADMINISTRATION;
import static com.databazoo.devmodeler.gui.UsageElement.*;
import static com.databazoo.devmodeler.gui.view.DifferenceView.*;
import static com.databazoo.devmodeler.model.Relation.*;
import static com.databazoo.devmodeler.wizards.InfoPanelHistoryWizard.L_NOTIFICATION_LOG;

/**
 * Application menu.
 *
 * @author bobus
 */
public class Menu extends JPanel {

    private static final String L_PROJECTS = "Projects";
    private static final String L_SETTINGS = "Settings";
    private static final String L_ABOUT = "About";
    public static final String L_HELP = "How-to's and UI reference";
    private static final String L_EXIT = "Exit";
    private static final String L_RUNNING_TASKS = "Running tasks";
    private static final String L_USERS_AND_ROLES = "Users";
    private static final String L_DATABASES = "Databases";

    private static final String L_DB_TREE = "Show database tree";
    private static final String L_GRID = "Show grid";
    private static final String L_STRAIGHT_CON = "Draw straight constraints";
    private static final String L_ZOOM_IN = "Zoom in";
    private static final String L_ZOOM_OUT = "Zoom out";

    public static final String L_REARRANGE_ITEMS = "Rearrange items";
    public static final String L_REARRANGE_ALPHABETICAL = "Alphabetical (A-Z, rows and columns)";
    public static final String L_REARRANGE_CIRCULAR = "Circular (A-Z, one large circle)";
    public static final String L_REARRANGE_FORCE_BASED = "Force directed (pull related objects together)";
    public static final String L_REARRANGE_NATURAL = "Natural (force directed, unrelated objects last)";
    public static final String L_REARRANGE_EXPLODE = "Explode (make some space between objects)";
    public static final String L_REARRANGE_IMPLODE = "Shrink (remove some space between objects)";

    private static final String L_EXPORT = "Export";
    private static final String L_EXPORT_XML = "to XML";
    private static final String L_EXPORT_SQL = "to SQL";
    private static final String L_EXPORT_IMAGE = "to image";
    private static final String L_IMPORT = "Import";

    private static final int SIDE_MENU_WIDTH = 390;

    private static final Menu INSTANCE = new Menu();

    public static Menu getInstance() {
        return INSTANCE;
    }

    public static void redrawRightMenu() {
        getInstance().rightMenu.redraw();
    }

    private JButton menuBtnAdmin;

    JToggleButton menuBtnViewDesigner, menuBtnViewOptimizer, menuBtnViewData, menuBtnViewDiff;

    private MyCheckboxMenuItem dbTreeMenuItem;
    private MyMenuItem zoomInMenuItem;
    private MyMenuItem zoomOutMenuItem;
    private JMenu elementsMenuElem;

    private RightMenu rightMenu;
    private String[] dbComboOptions = { "" };
    private String[] connComboOptions = { "" };
    private String[] connComboOptions2 = { "" };
    private IconableComboBox dbCombo = new IconableComboBox(dbComboOptions);
    private IconableComboBox connCombo = new IconableComboBox(connComboOptions);
    private IconableComboBox connCombo2 = new IconableComboBox(connComboOptions2);

    private JButton menuBtnQuery;
    private JToggleButton menuBtnHistory;
    private JCheckBox syncCheckBox;

    private Menu() {
        super(new BorderLayout());
        add(new LeftMenu(), BorderLayout.WEST);
        add(new CenterMenu(), BorderLayout.CENTER);
        add(rightMenu = new RightMenu(), BorderLayout.EAST);
    }

    JCheckBoxMenuItem getDbTreeMenuItem() {
        return dbTreeMenuItem;
    }

    JMenuItem getZoomInMenuItem() {
        return zoomInMenuItem;
    }

    JMenuItem getZoomOutMenuItem() {
        return zoomOutMenuItem;
    }

    public JCheckBox getSyncCheckBox() {
        return syncCheckBox;
    }

    public void checkSyncCheckbox() {
        Schedule.inEDT(() -> {
            if (DesignGUI.getView() != ViewMode.DIFF) {
                int selectedIndex = connCombo.getSelectedIndex();
                syncCheckBox.setEnabled(selectedIndex == 0);
            }
        });
    }

    public IconableComboBox[] getConnCombos() {
        return new IconableComboBox[] { connCombo2, connCombo };
    }

    void switchView(ViewMode view) {
        if (view.equals(ViewMode.DESIGNER)) {
                menuBtnViewDesigner.getModel().setSelected(true);
                menuBtnViewOptimizer.getModel().setSelected(false);
                menuBtnViewData.getModel().setSelected(false);
                menuBtnViewDiff.getModel().setSelected(false);
        } else if (view.equals(ViewMode.OPTIMIZE)) {
                menuBtnViewDesigner.getModel().setSelected(false);
                menuBtnViewOptimizer.getModel().setSelected(true);
                menuBtnViewData.getModel().setSelected(false);
                menuBtnViewDiff.getModel().setSelected(false);
        } else if (view.equals(ViewMode.DATA)) {
                menuBtnViewDesigner.getModel().setSelected(false);
                menuBtnViewOptimizer.getModel().setSelected(false);
                menuBtnViewData.getModel().setSelected(true);
                menuBtnViewDiff.getModel().setSelected(false);
        } else {
                menuBtnViewDesigner.getModel().setSelected(false);
                menuBtnViewOptimizer.getModel().setSelected(false);
                menuBtnViewData.getModel().setSelected(false);
                menuBtnViewDiff.getModel().setSelected(true);
        }
        MenuElementView.getInstance().updateMenuItems();
        rightMenu.redraw();
    }

    public void setEntityButtonsEnabled() {
        menuBtnQuery.setEnabled(Project.getCurrent().getType() != Project.TYPE_ABSTRACT);
    }

    public void setHistoryButtonSelected(boolean selected) {
        menuBtnHistory.setSelected(selected);
    }

    public void setModelingMode(boolean isAbstract) {
        if (isAbstract) {
            menuBtnViewData.setVisible(false);
        } else {
            menuBtnViewData.setVisible(true);
        }
    }

    public void setCompareAvailable(boolean isAvailable) {
        if (!isAvailable && !menuBtnViewDiff.isEnabled()) {
            DesignGUI.get().switchView(ViewMode.DESIGNER, false);
        }
        menuBtnViewDiff.setVisible(isAvailable);
    }

    private class MyMenuItem extends JMenuItem {

        MyMenuItem(String title, Icon icon) {
            super(title, icon);
            addActionListener(new MenuItemListener());
        }

        private MyMenuItem(String title) {
            super(title);
            addActionListener(new MenuItemListener());
        }

        MyMenuItem(String title, Icon icon, boolean enabled) {
            super(title, icon);
            addActionListener(new MenuItemListener());
            setEnabled(enabled);
        }

        private MyMenuItem(String title, boolean enabled) {
            super(title);
            addActionListener(new MenuItemListener());
            setEnabled(enabled);
        }
    }

    static class MyCheckboxMenuItem extends JCheckBoxMenuItem {

        MyCheckboxMenuItem(String title, Icon icon, boolean value) {
            super(title, icon, value);
            addItemListener(new MenuItemListener());
        }

		/*MyCheckboxMenuItem(String title, boolean value) {
            super(title, value);
			addItemListener(new MenuItemListener());
		}

		MyCheckboxMenuItem(String title, Icon icon, boolean value, boolean enabled) {
			super(title, icon, value);
			addItemListener(new MenuItemListener());
			setEnabled(enabled);
		}

		MyCheckboxMenuItem(String title, boolean value, boolean enabled) {
			super(title, value);
			addItemListener(new MenuItemListener());
			setEnabled(enabled);
		}*/
    }

    private static class MenuItemListener implements ActionListener, ItemListener {

        @Override
        public void actionPerformed(final ActionEvent ae) {
            switch (ae.getActionCommand()) {
            case L_PROJECTS:
                Usage.log(LEFT_MENU_PROJECTS);
                ProjectWizard.getInstance();
                break;

            case L_SETTINGS:
                Usage.log(LEFT_MENU_SETTINGS);
                SettingsWizard.get();
                break;

            case L_ABOUT:
                Usage.log(LEFT_MENU_ABOUT);
                AppInfoWindow.get().drawInfo();
                break;

            case L_HELP:
                Usage.log(LEFT_MENU_HELP);
                DocumentationWizard.getInstance().draw();
                break;

            case L_EXIT:
                Usage.log(LEFT_MENU_EXIT);
                DesignGUI.get().askIfClose();
                break;

            case L_ZOOM_IN:
                Usage.log(LEFT_MENU_ZOOM_IN);
                Navigator.instance.zoomIn();
                break;

            case L_ZOOM_OUT:
                Usage.log(LEFT_MENU_ZOOM_OUT);
                Navigator.instance.zoomOut();
                break;

            case L_REARRANGE_ALPHABETICAL:
            case L_REARRANGE_CIRCULAR:
            case L_REARRANGE_FORCE_BASED:
            case L_REARRANGE_NATURAL:
            case L_REARRANGE_EXPLODE:
            case L_REARRANGE_IMPLODE:
                Usage.log(LEFT_MENU_REARRANGE);
                Schedule.inEDT(() -> {
                    Object[] options = {"Rearrange", "Cancel"};
                    int n = JOptionPane
                            .showOptionDialog(DesignGUI.get().frame, "Rearrange all elements?", "Rearrange elements", JOptionPane.YES_NO_OPTION,
                                    JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                    if (n == 0) {
                        Schedule.inWorker(() -> {
                            final Project p = Project.getCurrent();
                            final Organizer organizer = OrganizerFactory.get(ae.getActionCommand());
                            if (p.getCurrentWorkspace() == null) {
                                organizer.organize(p.getCurrentDB());
                            } else {
                                organizer.organize(p.getCurrentWorkspace());
                            }
                        });
                    }
                });
                break;

            case L_EXPORT_XML:
                Usage.log(LEFT_MENU_EXPORT_XML);
                ExportImportWizard.get().runExportToXML();
                break;

            case L_EXPORT_SQL:
                Usage.log(LEFT_MENU_EXPORT_SQL);
                ExportImportWizard.get().drawExportToSQL();
                break;

            case L_EXPORT_IMAGE:
                Usage.log(LEFT_MENU_EXPORT_IMG);
                ExportImportWizard.get().drawExportToImage();
                break;

            case L_IMPORT:
                Usage.log(LEFT_MENU_IMPORT);
                ExportImportWizard.get().drawImportWindow();
                break;
            }
        }

        @Override
        public void itemStateChanged(ItemEvent ie) {
            AbstractButton item = (AbstractButton) ie.getItem();
            switch (item.getText()) {
            case L_DB_TREE:
                Usage.log(LEFT_MENU_TGL_TREE);
                DesignGUI.get().toggleDBView();
                Settings.put(Settings.L_LAYOUT_DB_TREE, item.isSelected());
                Settings.save();
                break;
            case L_GRID:
                Usage.log(LEFT_MENU_TGL_GRID);
                Canvas.instance.gridEnabled = item.isSelected();
                Canvas.instance.repaint();
                Settings.put(Settings.L_LAYOUT_CANV_GRID, item.isSelected());
                Settings.save();
                break;
            case L_STRAIGHT_CON:
                //Usage.log(LEFT_MENU_TGL_GRID);
                Constraint.isDrawStraight = item.isSelected();
                DesignGUI.get().drawProject(true);
                Settings.put(Settings.L_LAYOUT_CANV_STRAIGHT, item.isSelected());
                Settings.save();
                break;

            case L_TABLES:
            case L_INDEXES:
            case L_CONSTRAINTS:
            case L_TRIGGERS:
            case L_FUNCTIONS:
            case L_VIEWS:
            case L_SEQUENCES:
            case L_PACKAGES:
                MenuElementView
                        .setChange(item.getText(), item.isSelected())
                        .updateMenuItems();
                Canvas.instance.drawProjectLater(true);
            }
        }
    }

    private class LeftMenu extends JPanel {

        private static final int MENU_WIDTH = 244;

        private LeftMenu() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setPreferredSize(new Dimension(SIDE_MENU_WIDTH - 10, MENU_HEIGHT));
            draw();
        }

        private void draw() {
            JMenuBar menuBar = new JMenuBar();

            menuBar.setBorder(new EmptyBorder(4, 8, 0, 0));
            menuBar.setPreferredSize(new Dimension(MENU_WIDTH, MENU_HEIGHT));
            menuBar.setLayout(new GridLayout(1, 0, 0, 0));

            menuBar.add(drawFileMenu());
            menuBar.add(drawViewMenu());
            menuBar.add(drawModelMenu());
            //menuBar.add(drawToolMenu());

            add(menuBar);

            add(drawProjectButton());
            add(drawQueryButton());
            add(drawHistoryButton());
        }

        private JMenu drawFileMenu() {
            JMenu fileMenuElem = new JMenu(" File ");
            fileMenuElem.add(new MyMenuItem(L_PROJECTS, Theme.getSmallIcon(Theme.ICO_PROJECTS)));
            fileMenuElem.add(new MyMenuItem(L_SETTINGS, Theme.getSmallIcon(Theme.ICO_SETTINGS)));
            fileMenuElem.add(new MyMenuItem(L_HELP));
            fileMenuElem.add(new MyMenuItem(L_ABOUT));
            fileMenuElem.add(new MyMenuItem(L_EXIT, Theme.getSmallIcon(Theme.ICO_EXIT)));
            return fileMenuElem;
        }

        private JMenu drawViewMenu() {
            JMenu viewMenuElem = new JMenu(" View ");
            viewMenuElem.add(dbTreeMenuItem = new MyCheckboxMenuItem(L_DB_TREE, Theme.getSmallIcon(Theme.ICO_DB_TREE),
                    Settings.getBool(Settings.L_LAYOUT_DB_TREE)));
            viewMenuElem.add(new MyCheckboxMenuItem(L_GRID, Theme.getSmallIcon(Theme.ICO_GRID), Settings.getBool(Settings.L_LAYOUT_CANV_GRID)));
            viewMenuElem.add(new MyCheckboxMenuItem(L_STRAIGHT_CON, null, Settings.getBool(Settings.L_LAYOUT_CANV_STRAIGHT)));
            viewMenuElem.addSeparator();
            viewMenuElem.add(drawElementsMenu());
            viewMenuElem.addSeparator();
            viewMenuElem.add(zoomInMenuItem = new MyMenuItem(L_ZOOM_IN, Theme.getSmallIcon(Theme.ICO_ZOOM_IN), true));
            viewMenuElem.add(zoomOutMenuItem = new MyMenuItem(L_ZOOM_OUT, Theme.getSmallIcon(Theme.ICO_ZOOM_OUT), true));
            return viewMenuElem;
        }

        private JMenu drawElementsMenu() {
            elementsMenuElem = new JMenu("Elements");
            MenuElementView.getInstance(elementsMenuElem).updateMenuItems();
            return elementsMenuElem;
        }

        private JMenu drawModelMenu() {
            JMenu modelMenuElem = new JMenu(" Model ");
            modelMenuElem.add(drawRearrangeMenu());
            modelMenuElem.addSeparator();
            modelMenuElem.add(drawExportMenu());
            modelMenuElem.addSeparator();
            modelMenuElem.add(new MyMenuItem(L_IMPORT, Theme.getSmallIcon(Theme.ICO_IMPORT)));
            return modelMenuElem;
        }

        private JMenu drawToolMenu() {
            JMenu modelMenuElem = new JMenu(" Tools ");
            modelMenuElem.add(new MyMenuItem("Compare databases"));
            modelMenuElem.add(new MyMenuItem("Server status"));
            modelMenuElem.add(new MyMenuItem("SQL query window"));
            return modelMenuElem;
        }

        private JMenu drawRearrangeMenu() {
            JMenu rearrangeMenuElem = new JMenu(L_REARRANGE_ITEMS);
            rearrangeMenuElem.setIcon(Theme.getSmallIcon(Theme.ICO_ORGANIZE));
            rearrangeMenuElem.add(new MyMenuItem(L_REARRANGE_ALPHABETICAL, Theme.getSmallIcon(Theme.ICO_ORG_ALPHABET)));
            rearrangeMenuElem.add(new MyMenuItem(L_REARRANGE_CIRCULAR, Theme.getSmallIcon(Theme.ICO_ORG_CIRCULAR)));
            //rearrangeMenuElem.add(new MyMenuItem(L_REARRANGE_FORCE_BASED, Theme.getSmallIcon(Theme.ICO_ORG_FORCE)));
            rearrangeMenuElem.add(new MyMenuItem(L_REARRANGE_NATURAL, Theme.getSmallIcon(Theme.ICO_ORG_FORCE)));
            rearrangeMenuElem.addSeparator();
            rearrangeMenuElem.add(new MyMenuItem(L_REARRANGE_EXPLODE, Theme.getSmallIcon(Theme.ICO_ORG_EXPLODE)));
            rearrangeMenuElem.add(new MyMenuItem(L_REARRANGE_IMPLODE, Theme.getSmallIcon(Theme.ICO_ORG_IMPLODE)));
            return rearrangeMenuElem;
        }

        private JMenu drawExportMenu() {
            JMenu exportMenuElem = new JMenu(L_EXPORT);
            exportMenuElem.setIcon(Theme.getSmallIcon(Theme.ICO_EXPORT));
            exportMenuElem.add(new MyMenuItem(L_EXPORT_SQL, Theme.getSmallIcon(Theme.ICO_EXPORT)));
            exportMenuElem.add(new MyMenuItem(L_EXPORT_XML, Theme.getSmallIcon(Theme.ICO_EXPORT)));
            exportMenuElem.add(new MyMenuItem(L_EXPORT_IMAGE, Theme.getSmallIcon(Theme.ICO_EXPORT)));
            return exportMenuElem;
        }

        private JButton drawProjectButton() {
            JButton menuBtnProject = new JButton(Theme.getSmallIcon(Theme.ICO_PROJECTS));
            menuBtnProject.setToolTipText("Projects");
            menuBtnProject.addActionListener(e -> {
                Usage.log(LEFT_MENU_BTN_PROJECTS);
                ProjectWizard.getInstance();
            });
            menuBtnProject.setFocusable(false);
            menuBtnProject.setPreferredSize(MENU_BUTTON_SIZE);
            return menuBtnProject;
        }

        private JButton drawQueryButton() {
            menuBtnQuery = new JButton(Theme.getSmallIcon(Theme.ICO_SQL_WINDOW));
            menuBtnQuery.setToolTipText("Run SQL queries");
            menuBtnQuery.addActionListener(e -> {
                Usage.log(LEFT_MENU_BTN_SQL);
                DataWindow.get().drawQueryWindow();
            });
            menuBtnQuery.setFocusable(false);
            menuBtnQuery.setPreferredSize(MENU_BUTTON_SIZE);
            return menuBtnQuery;
        }

        private JToggleButton drawHistoryButton() {
            menuBtnHistory = new JToggleButton(Theme.getSmallIcon(Theme.ICO_HISTORY));
            menuBtnHistory.setToolTipText(L_NOTIFICATION_LOG);
            menuBtnHistory.addActionListener(e -> {
                //Usage.log(LEFT_MENU_BTN_SQL);
                DesignGUI.getInfoPanel().clicked();
                DesignGUI.getInfoPanel().doubleClicked();

            });
            menuBtnHistory.setFocusable(false);
            menuBtnHistory.setPreferredSize(MENU_BUTTON_SIZE);
            return menuBtnHistory;
        }
    }

    private class CenterMenu extends JPanel {

        private CenterMenu() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 0, 4));
            draw();
        }

        private void draw() {
            JPanel viewMenu = new JPanel(new GridLayout(1, 0, 3, 0));

            viewMenu.add(drawDesignerButton());
            viewMenu.add(drawOptimizerButton());
            viewMenu.add(drawDataButton());
            viewMenu.add(drawDiffButton());

            add(viewMenu, BorderLayout.CENTER);
        }

        private JToggleButton drawDiffButton() {
            menuBtnViewDiff = new JToggleButton("Changes", Theme.getSmallIcon(Theme.ICO_DIFFERENCE));
            menuBtnViewDiff.setToolTipText("History of changes, database comparator");
            menuBtnViewDiff.addActionListener(e -> {
                Usage.log(CENTER_MENU_BTN_DIFF);
                DesignGUI.get().switchView(ViewMode.DIFF, true);
            });
            menuBtnViewDiff.setFocusable(false);
            return menuBtnViewDiff;
        }

        private JToggleButton drawDataButton() {
            menuBtnViewData = new JToggleButton("Data", Theme.getSmallIcon(Theme.ICO_DATA));
            menuBtnViewData.setToolTipText("Data view [ALT]");
            menuBtnViewData.addActionListener(e -> {
                Usage.log(CENTER_MENU_BTN_DATA);
                DesignGUI.get().switchView(ViewMode.DATA, true);
            });
            menuBtnViewData.setFocusable(false);
            return menuBtnViewData;
        }

        private JToggleButton drawOptimizerButton() {
            menuBtnViewOptimizer = new JToggleButton("Optimize", Theme.getSmallIcon(Theme.ICO_APPROVED));
            menuBtnViewOptimizer.setToolTipText("Model optimizer");
            menuBtnViewOptimizer.addActionListener(e -> {
                Usage.log(CENTER_MENU_BTN_OPTIMIZE);
                DesignGUI.get().switchView(ViewMode.OPTIMIZE, true);
            });
            menuBtnViewOptimizer.setFocusable(false);
            return menuBtnViewOptimizer;
        }

        private JToggleButton drawDesignerButton() {
            menuBtnViewDesigner = new JToggleButton("Design", Theme.getSmallIcon(Theme.ICO_ABSTRACT));
            menuBtnViewDesigner.setToolTipText("Design view [ALT]");
            menuBtnViewDesigner.addActionListener(e -> {
                Usage.log(CENTER_MENU_BTN_DESIGN);
                DesignGUI.get().switchView(ViewMode.DESIGNER, true);
            });
            menuBtnViewDesigner.setFocusable(false);
            return menuBtnViewDesigner;
        }
    }

    private class RightMenu extends JComponent {

        private static final int ABSTRACT_DB_COMBO_WIDTH = 280;
        private static final int NORMAL_DB_COMBO_WIDTH = 130;
        private static final int NORMAL_CONN_COMBO_WIDTH = 140;
        private static final int COMPARE_CONN_COMBO_WIDTH = 193;
        private static final int SYNC_WIDTH = 75;
        private static final int SYNC_BUTTON_HEIGHT = 16;
        private static final int SYNC_CHECKBOX_HEIGHT = 14;
        private JPanel syncPanel;
        private String dbSelected;
        private String connSelected;

        private RightMenu() {
            setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            setPreferredSize(new Dimension(SIDE_MENU_WIDTH, MENU_HEIGHT));
            draw();
            redraw();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (DesignGUI.getView() == ViewMode.DIFF && Project.getCurrent().getType() != Project.TYPE_ABSTRACT && DifferenceView.instance.isCompareSelected()) {
                Graphics2D graphics = (Graphics2D) g;
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                graphics.setPaint(UIConstants.Colors.getPanelBackground());
                graphics.fillRect(getWidth() - SIDE_MENU_WIDTH, 0, SIDE_MENU_WIDTH - 3, getHeight());
                graphics.setPaint(UIConstants.Colors.getTableBorders());
                graphics.drawRect(getWidth() - SIDE_MENU_WIDTH, 0, SIDE_MENU_WIDTH - 3, getHeight());
            }
        }

        private void draw() {
            syncPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            syncPanel.setPreferredSize(new Dimension(80, MENU_COMPONENT_HEIGHT));
            syncPanel.add(drawSyncCheckbox());
            syncPanel.add(getSyncNowButton());

            menuBtnAdmin = new JButton(Theme.getSmallIcon(Theme.ICO_SETTINGS));
            menuBtnAdmin.setToolTipText("Administration tools");
            menuBtnAdmin.addActionListener(e -> {
                RightClickMenu.setLocationTo(menuBtnAdmin, new Point(2, MENU_COMPONENT_HEIGHT - 2));
                RightClickMenu.get((type, selectedValue) -> {
                    switch (type) {
                    case 10:
                        Usage.log(RIGHT_MENU_ADMIN_SERVER_ACTIVITY);
                        ServerActivityWindow.get(Project.getCurrent().getCurrentConn(), Project.getCurrDB().getName());
                        break;
                    case 11:
                        Usage.log(RIGHT_MENU_ADMIN_DBS);
                        ServerAdministrationWizard.getInstance().drawDatabases();
                        break;
                    case 12:
                        Usage.log(RIGHT_MENU_ADMIN_USERS);
                        ServerAdministrationWizard.getInstance().drawUsersRoles();
                        break;
                    default:
                        throw new IllegalArgumentException("Menu option " + type + " is not known");
                    }

                })
                        .addItem(L_RUNNING_TASKS, 10)
                        .addItem(L_DATABASES, 11)
                        .addItem(L_USERS_AND_ROLES, 12);
            });
            menuBtnAdmin.setFocusable(false);
            menuBtnAdmin.setPreferredSize(MENU_BUTTON_SIZE);
        }

        private JButton getSyncNowButton() {
            JButton syncNowBtn = new JButton("sync now");
            syncNowBtn.setPreferredSize(new Dimension(SYNC_WIDTH, SYNC_BUTTON_HEIGHT));
            syncNowBtn.setFocusable(false);
            syncNowBtn.setFont(FontFactory.getSans(Font.PLAIN, 10));
            syncNowBtn.setMargin(new Insets(0, 0, 0, 0));
            syncNowBtn.addActionListener(e -> {
                Usage.log(RIGHT_MENU_BTN_SYNC);
                if (Project.getCurrent().isSyncing()) {
                    DesignGUI.getInfoPanel().write("Synchronization with server is already running");
                }
                Schedule.inWorker(() -> Project.getCurrent().syncWithServer());
            });
            return syncNowBtn;
        }

        private JCheckBox drawSyncCheckbox() {
            syncCheckBox = new JCheckBox("auto sync");
            syncCheckBox.setSelected(Settings.getBool(Settings.L_SYNC_INITIAL_CHECKED) && !UIConstants.DEBUG);
            syncCheckBox.setPreferredSize(new Dimension(SYNC_WIDTH, SYNC_CHECKBOX_HEIGHT));
            syncCheckBox.setFocusable(false);
            syncCheckBox.setFont(FontFactory.getSans(Font.PLAIN, 10));
            syncCheckBox.addActionListener(e -> Usage.log(RIGHT_MENU_CHK_SYNC));
            return syncCheckBox;
        }

        private void redraw() {
            Schedule.inEDT(() -> {
                removeAll();
                redrawComboOptions();

                if (DesignGUI.getView() == ViewMode.DIFF && Project.getCurrent().getType() != Project.TYPE_ABSTRACT && DifferenceView.instance
                        .isCompareSelected()) {
                    draw2ConnectionCombos();
                } else {
                    drawDbAndConnectionCombo();
                }

                connCombo.addActionListener(e -> {
                    Usage.log(RIGHT_MENU_CONN_COMBO);
                    IConnection currConn = Project.getCurrent().getConnections().get(connCombo.getSelectedIndex());
                    Project.getCurrent().setCurrentConn(currConn.getName());
                    checkSyncCheckbox();
                });
                repaint();
            });
        }

        private void drawDbAndConnectionCombo() {
            dbCombo = new IconableComboBox(dbComboOptions);
            dbCombo.setFocusable(false);
            if (dbComboOptions.length > 0) {
                dbCombo.setSelectedItem(dbSelected);
            }

            dbCombo.setPreferredSize(new Dimension(NORMAL_DB_COMBO_WIDTH, MENU_COMPONENT_HEIGHT));
            connCombo.setPreferredSize(new Dimension(NORMAL_CONN_COMBO_WIDTH, MENU_COMPONENT_HEIGHT));

            if (ProjectManager.getInstance() != null && Project.getCurrent() != null && Project.getCurrent().getType() == Project.TYPE_ABSTRACT) {
                add(dbCombo);
                dbCombo.setPreferredSize(new Dimension(ABSTRACT_DB_COMBO_WIDTH, MENU_COMPONENT_HEIGHT));
            } else {
                add(syncPanel);
                add(dbCombo);
                add(connCombo);
                if (Project.getCurrent() != null && Project.getCurrent().getCurrentConn().isSupported(SERVER_ADMINISTRATION)) {
                    add(menuBtnAdmin);
                }
            }
            setBorder(new EmptyBorder(0, 0, 0, 0));
            dbCombo.addActionListener(e -> {
                Usage.log(RIGHT_MENU_DB_COMBO);
                Project.getCurrent().setCurrentDB(Project.getCurrent().getDatabases().get(dbCombo.getSelectedIndex()));
                DesignGUI.get().drawProject(true);
            });
            Menu.this.validate();
        }

        private void draw2ConnectionCombos() {
            if (DifferenceView.instance.getConnSelected() > connComboOptions2.length - 1) {
                DifferenceView.instance.setConnSelected(connComboOptions2.length - 1);
            }
            connCombo2 = new IconableComboBox(connComboOptions2);
            connCombo2.setFocusable(false);
            if (connComboOptions2.length > 0) {
                connCombo2.setSelectedIndex(DifferenceView.instance.getConnSelected());
            }

            connCombo2.setPreferredSize(new Dimension(COMPARE_CONN_COMBO_WIDTH, MENU_COMPONENT_HEIGHT));
            connCombo.setPreferredSize(new Dimension(COMPARE_CONN_COMBO_WIDTH, MENU_COMPONENT_HEIGHT));

            add(connCombo2);
            add(connCombo);
            setBorder(new EmptyBorder(0, 0, 0, 2));
            connCombo2.addActionListener(e -> {
                Usage.log(RIGHT_MENU_CONN2_COMBO);
                DifferenceView.instance.setConnSelected(connCombo2.getSelectedIndex());
            });
        }

        private void redrawComboOptions() {
            final Project p = Project.getCurrent();
            if (ProjectManager.getInstance() != null && p != null) {

                dbSelected = p.getCurrentDB() != null ? p.getCurrentDB().getFullName() : "";
                dbComboOptions = p.getDatabases().stream()
                        .map(DB::getFullName)
                        .toArray(String[]::new);

                connSelected = p.getCurrentConn() != null ? p.getCurrentConn().getFullName() : "";
                connComboOptions = p.getConnections().stream()
                        .map(IConnection::getFullName)
                        .toArray(String[]::new);

                connComboOptions2 = p.getConnections().stream()
                        .map(IConnection::getFullName)
                        .toArray(String[]::new);
            }

            redrawConnCombo();
        }

        private void redrawConnCombo() {
            connCombo = new IconableComboBox(connComboOptions);
            connCombo.setFocusable(false);
            if (connComboOptions.length > 0) {
                connCombo.setSelectedItem(connSelected);
            }
        }
    }
}
