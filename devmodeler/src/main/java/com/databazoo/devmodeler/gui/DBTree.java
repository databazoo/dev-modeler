
package com.databazoo.devmodeler.gui;

import com.databazoo.components.FontFactory;
import com.databazoo.components.WizardTree;
import com.databazoo.components.elements.ClickableComponent;
import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.components.icons.IconedPath;
import com.databazoo.components.icons.ModelIconRenderer;
import com.databazoo.components.textInput.AutocompleteObserver;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.model.Workspace;
import com.databazoo.devmodeler.model.reference.DraggableComponentReference;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Database tree that is visible to the left of Canvas. Implements object selection and search (filtering).
 *
 * @author bobus
 */
public class DBTree extends WizardTree {
	public static final DBTree instance = new DBTree();
	public static final String L_WORKSPACES = "Workspaces";
	public static int animationDelay;

	public static List<IModelElement> getFlatTree(List<IModelElement> elems, DefaultMutableTreeNode root, boolean onlyLeafs) {
		if(root != null){
			Object userObj = root.getUserObject();
			if(userObj instanceof IModelElement && (!onlyLeafs || root.getChildCount() == 0)){
				elems.add((IModelElement)userObj);
			}
			for(int i=0; i<root.getChildCount(); i++){
				getFlatTree(elems, (DefaultMutableTreeNode)root.getChildAt(i), onlyLeafs);
			}
		}
		return elems;
	}

	int splitWidth = Config.DB_TREE_SPLIT_WIDTH + Settings.getInt(Settings.L_LAYOUT_DB_TREE_WIDTH)*44;

	private final transient List<IModelElement> elements = new ArrayList<>();
	private String[] elementNames;
	private boolean treeInitiatedClick;

	private DBTree(){
		super(null, 0, new ModelIconRenderer());
		drawProjectName = true;
		//setDragEnabled(true);
		setFont(FontFactory.getSans(Font.PLAIN, Settings.getInt(Settings.L_FONT_TREE_SIZE)));
		addTreeSelectionListener(new DBViewSelectionListener());
		setExpandsSelectedPaths(true);
		addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent e) {
				int selRow = getClosestRowForLocation(e.getX(), e.getY());
				TreePath selPath = getClosestPathForLocation(e.getX(), e.getY());
				if(selRow != -1) {
					if((e.getModifiers() & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK){

						// Right click - position the menu
						if(selPath.getLastPathComponent() instanceof DefaultMutableTreeNode){
							Object userObject = ((DefaultMutableTreeNode) selPath.getLastPathComponent()).getUserObject();
							if(userObject instanceof ClickableComponent){
								ClickableComponent element = (ClickableComponent)userObject;
								RightClickMenu.setLocationTo(e.getComponent(), new Point(e.getX(), e.getY()));
								element.rightClicked();
							}
						}
						e.consume();

					}else if(e.getClickCount() == 1 && e.getX() > (selPath.getPathCount()-1)*16) {

						// Normal click - expand path
						expandPath(selPath);

					} else if(e.getClickCount() == 2) {

						// Double click
						if(selPath.getLastPathComponent() instanceof DefaultMutableTreeNode){
							Object userObject = ((DefaultMutableTreeNode) selPath.getLastPathComponent()).getUserObject();
							if(userObject instanceof IModelElement){
								IModelElement element = (IModelElement)userObject;
								element.doubleClicked();
							}
						}
						e.consume();
						expandPath(selPath);
					}
				}
			}
		});
	}

	public void selectCurrentDB() {
		DefaultTreeModel model = (DefaultTreeModel) getModel();
		TreeNode root = (TreeNode)model.getRoot();
		String dbName = Project.getCurrent().getCurrentDB().getName();
		for(int g=0; g<root.getChildCount(); g++){
			TreeNode dbNode = root.getChildAt(g);
			if(dbNode.toString().equals(dbName)){
				setSelectionPath(new TreePath(model.getPathToRoot(dbNode)));
				return;
			}
		}
	}

	public void selectWorkspaceByName(String workspaceName) {
		DefaultTreeModel model = (DefaultTreeModel) getModel();
		TreeNode root = ((TreeNode)model.getRoot()).getChildAt(0);
		for(int g=0; g<root.getChildCount(); g++){
			TreeNode wsNode = root.getChildAt(g);
			if(wsNode.toString().equals(workspaceName)){
				setSelectionPath(new TreePath(model.getPathToRoot(wsNode)));
				return;
			}
		}
	}

	public void selectSchemaByName(String schemaName) {
		if(Project.getCurrent().getCurrentConn().isSupported(SupportedElement.SCHEMA)){
			DefaultTreeModel model = (DefaultTreeModel) getModel();
			TreeNode root = (TreeNode)model.getRoot();
			String dbName = Project.getCurrent().getCurrentDB().getName();
			for(int g=0; g<root.getChildCount(); g++){
				TreeNode dbNode = root.getChildAt(g);
				if(dbNode.toString().equals(dbName)){
					for(int i=0; i<dbNode.getChildCount(); i++){
						TreeNode schemaNode = dbNode.getChildAt(i);
						if(schemaNode.toString().equals(schemaName)){
							setSelectionPath(new TreePath(model.getPathToRoot(schemaNode)));
							return;
						}
					}
				}
			}
		}else{
			selectCurrentDB();
		}
	}

	public void selectRelationByName(String dbName, String schemaName, String functionName) {
		if(!treeInitiatedClick){
			DefaultTreeModel model = (DefaultTreeModel) getModel();
			TreeNode root = (TreeNode)model.getRoot();
			if (root != null)
			for(int g=0; g<root.getChildCount(); g++){
				TreeNode dbNode = root.getChildAt(g);
				if(dbNode.toString().equals(dbName)){
					for(int i=0; i<dbNode.getChildCount(); i++){
						Project project = Project.getCurrent();
						if(project.getCurrentConn().isSupported(SupportedElement.SCHEMA)){
							TreeNode schemaNode = dbNode.getChildAt(i);
							if(schemaNode.toString().equals(schemaName)){
								for(int j=0; j<schemaNode.getChildCount(); j++){
									DefaultMutableTreeNode funcNode = (DefaultMutableTreeNode) schemaNode.getChildAt(j);
									if(funcNode.toString().equals(functionName)){
										if(getSelectionPath() != null && getSelectionPath().getPathCount() > 3){
											collapsePath(new TreePath(new Object[]{root, getSelectionPath().getPathComponent(1), getSelectionPath().getPathComponent(2), getSelectionPath().getPathComponent(3)}));
										}
										TreePath newPath = new TreePath(model.getPathToRoot(funcNode));
										setSelectionPath(newPath);
										scrollPathToVisible(newPath);
										expandPath(newPath);
										return;
									}
								}
							}
						}else{
							TreeNode funcNode = dbNode.getChildAt(i);
							if(funcNode.toString().equals(functionName)){
								if(getSelectionPath() != null && getSelectionPath().getPathCount() > 3){
									collapsePath(new TreePath(new Object[]{root, getSelectionPath().getPathComponent(1), getSelectionPath().getPathComponent(2)}));
								}
								TreePath newPath = new TreePath(model.getPathToRoot(funcNode));
								setSelectionPath(newPath);
								scrollPathToVisible(newPath);
								expandPath(newPath);
								return;
							}
						}
					}
				}
			}
		}else{
			treeInitiatedClick = false;
		}
	}

	void checkDB(boolean redrawTree){
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		if(Project.getCurrent() != null){
			List<Workspace> workspaces = Project.getCurrent().getWorkspaces();
			if(!workspaces.isEmpty()){
				DefaultMutableTreeNode ws_node = new DefaultMutableTreeNode(new IconedPath(L_WORKSPACES, Theme.getSmallIcon(Theme.ICO_WORKSPACE)));
				for (Workspace workspace : workspaces) {
					ws_node.add(workspace.getTreeView());
				}
				root.add(ws_node);
			}
			for(DB db: Project.getCurrent().getDatabases()){
				root.add(db.getTreeView(false));
			}
		}
		if(redrawTree) {
			setModel(root);
		}
		updateElementsList(root);
	}

	void checkDB(String search, boolean fulltext, boolean searchNotMatching){
		if(search.isEmpty()){
			checkDB(true);
			return;
		}else{
			search = "(?is).*"+Pattern.quote(search)+".*";
		}
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		List<Workspace> workspaces = Project.getCurrent().getWorkspaces();
		if(!workspaces.isEmpty()){
			DefaultMutableTreeNode ws_node = null;
			for (Workspace workspace : workspaces) {
				DefaultMutableTreeNode node = workspace.getTreeView(search, fulltext, searchNotMatching);
				if(node != null){
					if(ws_node == null){
						ws_node = new DefaultMutableTreeNode(new IconedPath(L_WORKSPACES, Theme.getSmallIcon(Theme.ICO_WORKSPACE)));
						root.add(ws_node);
					}
					ws_node.add(node);
				}
			}
		}
		for(DB db: Project.getCurrent().getDatabases()){
			DefaultMutableTreeNode node = db.getTreeView(search, fulltext, searchNotMatching);
			if(node != null){
				root.add(node);
			}
		}
		setModel(root);
	}

	private void setModel(final DefaultMutableTreeNode root){
		Schedule.inEDT(() -> {
            setModel(new DefaultTreeModel(root));
            setRootVisible(false);
            for (int i = 0; i < getRowCount(); i++) {
                expandRow(i);
            }
			collapseRows();
        });
	}

	private void collapseRows() {
		boolean unfoldMain = getRowCount() < 100;
		if(Project.getCurrent().getCurrentConn().isSupported(SupportedElement.SCHEMA)){
            for (int i = getRowCount()-1; i >= 0; i--) {
                int cnt = getPathForRow(i).getPathCount();
                if((!unfoldMain && cnt == 3) || cnt == 4){
                    collapseRow(i);
                }
            }
        }else{
            for (int i = getRowCount()-1; i >= 0; i--) {
                if(getPathForRow(i).getPathCount() == 3 || (getPathForRow(i).getPathCount() == 4 && getPathForRow(i).getPathComponent(1).toString().equals(L_WORKSPACES))){
                    collapseRow(i);
                }
            }
        }
	}

	void setScrollPaneSize(JScrollPane scrollPane, Container frame) {
		scrollPane.setPreferredSize(new Dimension(splitWidth, frame.getSize().height));
	}

	private synchronized void updateElementsList(DefaultMutableTreeNode root) {
		elements.clear();
		getFlatTree(elements, root, false);
		if(!elements.isEmpty()) {
			elementNames = new String[elements.size() * 2 - 2];
			for (int i = 0; i < elements.size() - 1; i++) {
				elementNames[i * 2] = elements.get(i + 1).getName().toLowerCase();
				elementNames[i * 2 + 1] = elements.get(i + 1).getFullName().toLowerCase();
			}
			elements.clear();
			AutocompleteObserver.updateAutocomplete(elementNames);
		}
	}

	public String[] getElementNames(){
		if(elementNames == null){
			return new String[0];
		}else{
			return elementNames;
		}
	}

	private static class DBViewSelectionListener implements TreeSelectionListener {
		private String oldPath = "";
		@Override
		public void valueChanged(TreeSelectionEvent tse) {
			try {
				Project currProject = Project.getCurrent();
				TreePath path = tse.getNewLeadSelectionPath();
				if(path == null || oldPath.equals(path.toString())){
					return;
				}else{
					oldPath = path.toString();
				}

				animationDelay = 250;

				// HANDLE DATABASE AND WORKSPACE SWITCH
				if(path.getPathCount() > 1){
					String dbName = path.getPathComponent(1).toString();

					// IN WORKSPACE
					if(dbName.equals(L_WORKSPACES) && path.getPathCount() > 2){
						Workspace currWorkspace = currProject.getCurrentWorkspace();

						// WORKSPACE CHANGED
						if(currWorkspace == null || !currWorkspace.toString().equals(path.getPathComponent(2).toString())){
							Canvas.instance.setSelectedElement(null);
							currProject.setOldWorkspace(currWorkspace);
							currProject.setCurrentWorkspace(currProject.getWorkspaceByName(path.getPathComponent(2).toString()));
							currProject.setCurrentDB(currProject.getCurrentWorkspace().getDB());
							Menu.redrawRightMenu();
							Canvas.instance.drawProject(true);
							Navigator.instance.checkSize();
							HotMenu.instance.checkSize();

							animationDelay = 500;
						}
					}else{

						// WAS IN WORKSPACE, BUT NOT ANY MORE
						if(currProject.getCurrentWorkspace() != null){
							Canvas.instance.setSelectedElement(null);
							currProject.setOldWorkspace(currProject.getCurrentWorkspace());
							currProject.setCurrentWorkspace(null);
							currProject.setCurrentDB(dbName);
							Menu.redrawRightMenu();
							Canvas.instance.drawProject(true);

							animationDelay = 500;

						// DATABASE CHANGED
						}else if(!currProject.getCurrentDB().getFullName().equals(dbName)){
							currProject.setCurrentDB(dbName);
							Menu.redrawRightMenu();
							Canvas.instance.drawProject(true);

							animationDelay = 500;
						}
					}
				}

				// GENERATE THE CLICK
				DBTree.instance.treeInitiatedClick = true;
				final Object userObject = ((DefaultMutableTreeNode) DBTree.instance.getLastSelectedPathComponent()).getUserObject();
				if(userObject instanceof IModelElement) {
					IModelElement element = (IModelElement) userObject;
					if (element instanceof DraggableComponent) {
						element.clicked();
					} else if (path.getPathCount() > 4) {
						element.clicked();
					} else {
						Canvas.instance.setSelectedElement(null);
					}
				} else if(userObject instanceof DraggableComponentReference) {
					((DraggableComponentReference) userObject).clicked();
				}
				DBTree.instance.treeInitiatedClick = false;
				currProject.setOldWorkspace(null);
			} catch (ClassCastException e){
				Dbg.fixme("DB tree selection failed.", e);
				DBTree.instance.treeInitiatedClick = false;
			}
		}
	}

}
