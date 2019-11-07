package com.databazoo.components;

import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.regex.Pattern;

/**
 * A JTree with modified behavior specially for wizards.
 *
 * @author bobus
 */
public class WizardTree extends JTree {

    private static final Font HOLO_FONT = FontFactory.getSans(Font.BOLD+Font.ITALIC, 64);
	private static final Color HOLO_COLOR = Color.decode("#DCE4ED");
	private static final FontMetrics HOLO_FM = UIConstants.GRAPHICS.getFontMetrics(HOLO_FONT);

	public int defaultSelectedRow = 0;
	public boolean drawProjectName = false;

	/**
	 * Constructor
	 *
	 * @param treeView model
	 * @param row initially selected row
	 * @param renderer cell renderer
	 */
	public WizardTree(DefaultMutableTreeNode treeView, int row, DefaultTreeCellRenderer renderer) {
		super(treeView);
		prepareMouseListener();
		defaultSelectedRow = row;
		setCellRenderer(renderer);
		draw();
	}

	/**
	 * Constructor
	 *
	 * @param treeView model
	 * @param row initially selected row
	 * @param renderer cell renderer
	 * @param listener selection listener
	 */
	public WizardTree(DefaultMutableTreeNode treeView, int row, DefaultTreeCellRenderer renderer, TreeSelectionListener listener) {
		super(treeView);
		prepareMouseListener();
		defaultSelectedRow = row;
		setCellRenderer(renderer);
		addTreeSelectionListener(listener);
		draw();
	}

	/**
	 * Constructor
	 *
	 * @param treeView model
	 * @param renderer cell renderer
	 */
	public WizardTree(DefaultMutableTreeNode treeView, DefaultTreeCellRenderer renderer) {
		super(treeView);
		prepareMouseListener();
		setCellRenderer(renderer);
		draw();
	}

	/**
	 * Constructor
	 *
	 * @param treeView model
	 * @param renderer cell renderer
	 * @param listener selection listener
	 */
	public WizardTree(DefaultMutableTreeNode treeView, DefaultTreeCellRenderer renderer, TreeSelectionListener listener) {
		super(treeView);
		prepareMouseListener();
		setCellRenderer(renderer);
		addTreeSelectionListener(listener);
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		defaultSelectedRow = ProjectManager.getInstance().getCurrentProjectNumber();
		draw();
	}

	/**
	 * Add mouse listener that handles clicks on full width of the tree.
	 */
    private void prepareMouseListener(){
		addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent e) {
				if (e != null && e.getButton()==1 && e.getClickCount()==1) {
					int closestRow = getClosestRowForLocation(e.getX(), e.getY());
					Rectangle closestRowBounds = getRowBounds(closestRow);
					if(closestRowBounds != null && e.getY() >= closestRowBounds.getY() && e.getY() < closestRowBounds.getY() + closestRowBounds.getHeight()) {
						if(e.getX() > closestRowBounds.getX() && closestRow < getRowCount()){
							setSelectionRow(closestRow);
						}
					} else {
						setSelectionRow(-1);
					}
				}
			}

		});
    }

	/**
	 * Prepare the tree.
	 */
	private void draw(){
		setOpaque(false);
		scrollsOnExpand = false;
		setFont(FontFactory.getSans(Font.PLAIN, Settings.getInt(Settings.L_FONT_TREE_SIZE)));
		for (int i = 0; i < getRowCount(); i++) {
			expandRow(i);
		}
		if(defaultSelectedRow >= 0){
			setSelectionRow(defaultSelectedRow);
		}
		scrollsOnExpand = true;
	}

	/**
	 * Swap the model.
	 *
	 * @param updatedTreeModel new model
	 */
	public void assignNewModel(DefaultMutableTreeNode updatedTreeModel) {
		TreeSelectionListener[] listeners = getTreeSelectionListeners();
        for (TreeSelectionListener listener : listeners) {
            removeTreeSelectionListener(listener);
        }
		setModel(new DefaultTreeModel(updatedTreeModel));
        for (TreeSelectionListener listener : listeners) {
            addTreeSelectionListener(listener);
        }
		draw();
		repaint();
	}

	/**
	 * Swap the model and select a row.
	 *
	 * @param updatedTreeModel new model
	 * @param selectRow row number to select
	 */
	public void assignNewModel(DefaultMutableTreeNode updatedTreeModel, int selectRow) {
		defaultSelectedRow = selectRow;
		assignNewModel(updatedTreeModel);
	}

	/**
	 * Paint override.
	 *
	 * @param g Graphics
	 */
	@Override
	protected void paintComponent(Graphics g) {
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
		if(drawProjectName && Project.getCurrent() != null && Settings.getBool(Settings.L_LAYOUT_TREE_PROJECT_N)){
			Graphics2D graphics = (Graphics2D)g;
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setFont(HOLO_FONT);
			graphics.setColor(HOLO_COLOR);

			Rectangle visRect = getVisibleRect();
			AffineTransform oldTransform = graphics.getTransform();
			graphics.translate(visRect.width - 25 + visRect.x, HOLO_FM.stringWidth(Project.getCurrent().getProjectName()) + 15 + visRect.y);
			graphics.rotate(-Math.PI/2);
			graphics.drawString(Project.getCurrent().getProjectName(), 0, 0);
			graphics.setTransform(oldTransform);
		}
		super.paintComponent(g);
	}

	/**
	 * Select a row by name.
	 *
	 * @param name option name
	 */
	public void selectRow(String name){
		selectRow(name, 0);
	}

	/**
	 * Select a row by name.
	 *
	 * @param name option name
	 * @param plusRows n-th row after row with the given name
	 */
	public void selectRow(String name, int plusRows){
		name = Pattern.quote(name);
		for(int i=0; i<getRowCount(); i++){
			if(getPathForRow(i).toString().matches(".*"+name+"]")){
				setSelectionRow(i + plusRows);
				break;
			}
		}
	}
}
