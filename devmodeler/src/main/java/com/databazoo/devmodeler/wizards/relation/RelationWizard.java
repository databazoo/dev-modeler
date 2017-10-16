
package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.components.GCFrameWithObservers;
import com.databazoo.components.RotatedTabbedPane;
import com.databazoo.components.WizardTree;
import com.databazoo.components.elements.LineComponent;
import com.databazoo.components.icons.ModelIconRenderer;
import com.databazoo.components.icons.PlainColorIcon;
import com.databazoo.components.table.EditableTable;
import com.databazoo.components.textInput.FormattedClickableTextField;
import com.databazoo.components.textInput.LineNumberRowHeader;
import com.databazoo.components.textInput.NextFieldObserver;
import com.databazoo.components.textInput.QueryErrorPositionObserver;
import com.databazoo.components.textInput.UndoableTextField;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.DBCommException;
import com.databazoo.devmodeler.conn.IColoredConnection;
import com.databazoo.devmodeler.conn.IConnectionQuery;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.OperationCancelException;
import com.databazoo.devmodeler.gui.RightClickMenu;
import com.databazoo.devmodeler.gui.SearchPanel;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Package;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Sequence;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.model.View;
import com.databazoo.devmodeler.model.Workspace;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.RevisionFactory;
import com.databazoo.devmodeler.wizards.HistoryTableModel;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.text.Position;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Wizard for manipulating of all database objects (tables, attributes, indexes, etc.)
 * @author bobus
 */
public class RelationWizard extends RelationWizardPagesSequence {

    public static RelationWizard get(String workspaceName){
		RelationWizard wiz = new RelationWizard();
		wiz.workspaceName = workspaceName;
		return wiz;
	}

	private String workspaceName;
	FormattedClickableTextField queryInput;
	private JButton btnSave2 = new JButton("");
	String forwardSQL;
	String revertSQL;
	protected WizardTree tree;
	private boolean formatOnSQLTabOpen;

	RelationWizard() {}
	RelationWizard(GCFrameWithObservers frame) {
		this.frame = frame;
	}

	public void drawProperties(final Schema schemaIn, final int selectedRowIn) {
		schema = schemaIn;
		setDatabase(schema.getDB());
        Schedule.inEDT(() -> {
            rel = new Relation(schema, schema.getName()+".temporaryTable");
            rel.setTemp();
            drawWindow(schema.getFullName(), createTree(selectedRowIn), Settings.getBool(Settings.L_MAXIMIZE_WIZARDS), true);
            tree.drawProjectName = true;
            frame.setIconImages(Theme.getAllSizes(Theme.ICO_EDIT));
            addKeyListeners();
            setButtonPaneColor();
        });
	}

	public void drawProperties(final Relation relIn) {
		Dbg.toFile();
		setDatabase(relIn.getDB());
        Schedule.inEDT(() -> {
            rel = relIn;
            drawWindow(rel.getFullName(), createTree(2), Settings.getBool(Settings.L_MAXIMIZE_WIZARDS), true);
            tree.drawProjectName = true;
            frame.setIconImages(Theme.getAllSizes(Theme.ICO_EDIT));
            addKeyListeners();
            setButtonPaneColor();
        });
	}

	public void drawProperties(final Relation relIn, final int rowIn) {
		Dbg.toFile();
		database = relIn.getDB();
        Schedule.inEDT(() -> {
            rel = relIn;
            drawWindow(rel.getFullName(), createTree(rowIn+4), Settings.getBool(Settings.L_MAXIMIZE_WIZARDS), true);
            tree.drawProjectName = true;
            frame.setIconImages(Theme.getAllSizes(Theme.ICO_EDIT));
            addKeyListeners();
            setButtonPaneColor();
        });
	}

	public void drawProperties(final Relation relIn, final String rowName) {
		Dbg.toFile();
		setDatabase(relIn.getDB());
        Schedule.inEDT(() -> {
            rel = relIn;
            createTree(-1);
            tree.selectRow(rowName);
            drawWindow(rel.getFullName(), tree, Settings.getBool(Settings.L_MAXIMIZE_WIZARDS), true);
            tree.drawProjectName = true;
            frame.setIconImages(Theme.getAllSizes(Theme.ICO_EDIT));
            addKeyListeners();
            setButtonPaneColor();
        });
	}

	public void drawProperties(final Package packIn) {
		Dbg.toFile();
		setDatabase(packIn.getDB());
        Schedule.inEDT(() -> {
            pack = packIn;
            schema = pack.getSchema();
            rel = new Relation(schema, schema.getName()+".temporaryTable");
            rel.setTemp();
            drawWindow(pack.getFullName(), createTree(2), Settings.getBool(Settings.L_MAXIMIZE_WIZARDS), true);
            tree.drawProjectName = true;
            frame.setIconImages(Theme.getAllSizes(Theme.ICO_EDIT));
            addKeyListeners();
            setButtonPaneColor();
        });
	}

	public void drawProperties(final Function funcIn) {
		Dbg.toFile();
		setDatabase(funcIn.getDB());
        Schedule.inEDT(() -> {
            func = funcIn;
            schema = func.getSchema();
            rel = new Relation(schema, schema.getName()+".temporaryTable");
            rel.setTemp();
            drawWindow(func.getFullName(), createTree(2), Settings.getBool(Settings.L_MAXIMIZE_WIZARDS), true);
            tree.drawProjectName = true;
            frame.setIconImages(Theme.getAllSizes(Theme.ICO_EDIT));
            addKeyListeners();
            setButtonPaneColor();
        });
	}

	public void drawProperties(final View viewIn) {
		Dbg.toFile();
		setDatabase(viewIn.getDB());
        Schedule.inEDT(() -> {
            view = viewIn;
            schema = view.getSchema();
            rel = new Relation(schema, schema.getName()+".temporaryTable");
            rel.setTemp();
            drawWindow(view.getFullName(), createTree(2), Settings.getBool(Settings.L_MAXIMIZE_WIZARDS), true);
            tree.drawProjectName = true;
            frame.setIconImages(Theme.getAllSizes(Theme.ICO_EDIT));
            addKeyListeners();
            setButtonPaneColor();
        });
	}

	public void drawProperties(final Sequence seqIn) {
		Dbg.toFile();
		setDatabase(seqIn.getDB());
        Schedule.inEDT(() -> {
            sequence = seqIn;
            schema = seqIn.getSchema();
            rel = new Relation(schema, schema.getName()+".temporaryTable");
            rel.setTemp();
            drawWindow(seqIn.getFullName(), createTree(2), Settings.getBool(Settings.L_MAXIMIZE_WIZARDS), true);
            tree.drawProjectName = true;
            frame.setIconImages(Theme.getAllSizes(Theme.ICO_EDIT));
            addKeyListeners();
            setButtonPaneColor();
        });
	}

    private void setButtonPaneColor() {
        Color color = IColoredConnection.getColor(connection);
        if (color != null) {
            btnSave2.setIcon(new PlainColorIcon(color));
        }
    }

    WizardTree createTree(int row){
		Dbg.toFile();
		tree = new WizardTree(getUpdatedTreeModel(), row, new ModelIconRenderer(), this);
		tree.setRootVisible(false);
		return tree;
	}

	protected DefaultMutableTreeNode getUpdatedTreeModel(){
		DefaultMutableTreeNode root, schemaNode;

		root = new DefaultMutableTreeNode(this);
		schemaNode = new DefaultMutableTreeNode(rel.getSchema());
		if(!rel.isTemp()) {
            schemaNode.add(rel.getTreeView(true));
        }
		if(sequence != null) {
            schemaNode.add(new DefaultMutableTreeNode(sequence));
        }
		if(view != null) {
            schemaNode.add(new DefaultMutableTreeNode(view));
        }
		if(func != null) {
            schemaNode.add(new DefaultMutableTreeNode(func));
        }
		if(pack != null) {
            schemaNode.add(new DefaultMutableTreeNode(pack));
        }
		if(connection.isSupported(SupportedElement.RELATION)) {
            schemaNode.add(new DefaultMutableTreeNode(Relation.L_NEW_TABLE));
        }
		if(connection.isSupported(SupportedElement.FUNCTION)) {
            schemaNode.add(new DefaultMutableTreeNode(Relation.L_NEW_FUNCTION));
        }
		if(connection.isSupported(SupportedElement.VIEW)) {
            schemaNode.add(new DefaultMutableTreeNode(Relation.L_NEW_VIEW));
        }
		if(connection.isSupported(SupportedElement.SEQUENCE)) {
            schemaNode.add(new DefaultMutableTreeNode(Relation.L_NEW_SEQUENCE));
        }
		if(connection.isSupported(SupportedElement.PACKAGE)) {
            schemaNode.add(new DefaultMutableTreeNode(Relation.L_NEW_PACKAGE));
        }

		root.add(schemaNode);
		if(connection.isSupported(SupportedElement.SCHEMA_CREATE)) {
			root.add(new DefaultMutableTreeNode(Relation.L_NEW_SCHEMA));
		}
		return root;
	}

	@Override
	protected void createButtons(){
		btnCancel.addActionListener(new WizardListener(CLOSE_WINDOW));

		btnSave = new JButton(BTN_SAVE_TEXT);
		btnSave.setEnabled(false);
		btnSave2 = new JButton("Save in Model and "+connection.getName());
		btnSave2.setEnabled(false);
		btnSave2.addActionListener(new WizardListener(SAVE_IN_MODEL_AND_DB));

		if(project.getType() != Project.TYPE_ABSTRACT){
			buttonPane.add(btnSave2);
		}
		buttonPane.add(btnSave);
		buttonPane.add(btnCancel);
	}

	void addKeyListeners(){
		buttonPane.getActionMap().put("saveInModel",
			new AbstractAction("saveInModel") {
				@Override public void actionPerformed(ActionEvent evt) {
					if(btnSave.isEnabled()) {
						executeAction(SAVE_IN_MODEL);
					}
				}
		   });
		buttonPane.getActionMap().put("saveInModelAndDB",
			new AbstractAction("saveInModelAndDB") {
				@Override public void actionPerformed(ActionEvent evt) {
					if(project.getType() == Project.TYPE_ABSTRACT && btnSave.isEnabled()) {
						executeAction(SAVE_IN_MODEL);

					}else if(project.getType() != Project.TYPE_ABSTRACT && btnSave2.isEnabled()) {
						executeAction(SAVE_IN_MODEL_AND_DB);
					}
				}
		   });
		InputMap map = buttonPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "saveInModelAndDB");
		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "saveInModelAndDB");
		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()+KeyEvent.SHIFT_MASK), "saveInModel");

		frame.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosed(WindowEvent we) {
				NextFieldObserver.get(this).destroy();
			}
		});
		tree.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent e) {
				int selRow = tree.getClosestRowForLocation(e.getX(), e.getY());
				TreePath selPath = tree.getClosestPathForLocation(e.getX(), e.getY());
				if(selRow != -1) {
					if((e.getModifiers() & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK){

						// Right click - position the menu
						if(selPath.getLastPathComponent() instanceof DefaultMutableTreeNode) {
							Object userObject = ((DefaultMutableTreeNode) selPath.getLastPathComponent()).getUserObject();
							if (userObject instanceof IModelElement) {
								IModelElement element = (IModelElement) userObject;
								RightClickMenu.setLocationTo(e.getComponent(), new Point(e.getX(), e.getY()));
								element.rightClicked();
							}
						}
						e.consume();

					}else if(e.getClickCount() == 1 && e.getX() > (selPath.getPathCount()-1)*16) {

						// Normal click - expand path
						tree.expandPath(selPath);

					} else if(e.getClickCount() == 2) {

						// Double click
						e.consume();
						tree.expandPath(selPath);
					}
				}
			}
		});
	}

	@Override
	public void valueChanged(final TreeSelectionEvent tse) {
        Schedule.inEDT(() -> {
            if (tse.getNewLeadSelectionPath() != null) {
                try {
                    String path1 =
                            tse.getNewLeadSelectionPath().getPath().length > 1 ? tse.getNewLeadSelectionPath().getPathComponent(1).toString() : "";
                    String path2 =
                            tse.getNewLeadSelectionPath().getPath().length > 2 ? tse.getNewLeadSelectionPath().getPathComponent(2).toString() : "";
                    String path3 =
                            tse.getNewLeadSelectionPath().getPath().length > 3 ? tse.getNewLeadSelectionPath().getPathComponent(3).toString() : "";
                    String path4 =
                            tse.getNewLeadSelectionPath().getPath().length > 4 ? tse.getNewLeadSelectionPath().getPathComponent(4).toString() : "";
                    if (tse.getNewLeadSelectionPath().getPath().length == 2) {
                        if (path1.equals(Relation.L_NEW_SCHEMA)) {
                            loadNewSchemaPage1();
                        } else {
                            loadSchemaPage1(rel.getSchema());
                        }
                    } else if (tse.getNewLeadSelectionPath().getPath().length == 3) {
                        switch (path2) {
                        case Relation.L_NEW_TABLE:
                            loadNewTablePage1();
                            break;
                        case Relation.L_NEW_FUNCTION:
                            loadNewFunctionPage1();
                            break;
                        case Relation.L_NEW_PACKAGE:
                            loadNewPackagePage1();
                            break;
                        case Relation.L_NEW_VIEW:
                            loadNewViewPage1();
                            break;
                        case Relation.L_NEW_SEQUENCE:
                            loadNewSequencePage1();
                            break;
                        default:
                            if (view != null) {
                                loadViewPage1(view);
                            } else if (func != null) {
                                loadFunctionPage1(func);
                            } else if (pack != null) {
                                loadPackagePage1(pack);
                            } else if (sequence != null) {
                                loadSequencePage1(sequence);
                            } else {
                                loadTablePage1(rel);
                            }
                            break;
                        }
                    } else {
                        switch (path3) {
                        case Relation.L_ATTRIBUTES:
                            if (tse.getNewLeadSelectionPath().getPath().length == 4) {
                                loadAttributesIntro();
                            } else if (path4.equals(Relation.L_NEW_ATTRIBUTE)) {
                                loadNewAttributePage1();
                            } else if (tse.getNewLeadSelectionPath().getPath().length == 6) {
                                loadSequencePage1(rel.getAttributeByName(path4).getSequence());
                            } else {
                                loadAttributePage1(rel.getAttributeByName(path4));
                            }
                            break;
                        case Relation.L_CONSTRAINTS:
                            if (tse.getNewLeadSelectionPath().getPath().length == 4) {
                                loadConstraintsIntro();
                            } else {
                                switch (path4) {
                                case Relation.L_NEW_PK:
                                    if (checkAttributeExists(Relation.L_NEW_PK)) {
                                        loadNewPrimaryKeyPage1();
                                    }
                                    break;
                                case Relation.L_NEW_FK:
                                    if (checkAttributeExists(Relation.L_NEW_FK)) {
                                        loadNewForeignKeyPage1();
                                    }
                                    break;
                                case Relation.L_NEW_UNIQUE_C:
                                    if (checkAttributeExists(Relation.L_NEW_UNIQUE_C)) {
                                        loadNewUniqueConstraintPage1();
                                    }
                                    break;
                                case Relation.L_NEW_CHECK_C:
                                    if (checkAttributeExists(Relation.L_NEW_CHECK_C)) {
                                        loadNewCheckConstraintPage1();
                                    }
                                    break;
                                default:
                                    loadConstraintPage1(rel.getConstraintByName(path4));
                                    break;
                                }
                            }
                            break;
                        case Relation.L_INDEXES:
                            if (tse.getNewLeadSelectionPath().getPath().length == 4) {
                                loadIndexesIntro();
                            } else if (path4.equals(Relation.L_NEW_INDEX)) {
                                if (checkAttributeExists(Relation.L_NEW_INDEX)) {
                                    loadNewIndexPage1();
                                }
                            } else {
                                loadIndexPage1(rel.getIndexByName(path4));
                            }
                            break;
                        case Relation.L_TRIGGERS:
                            if (tse.getNewLeadSelectionPath().getPath().length == 4) {
                                loadTriggersIntro();
                            } else if (path4.equals(Relation.L_NEW_TRIGGER)) {
                                if (connection.isSupported(SupportedElement.TRIGGER_BODY) || rel.getDB().getTriggerFunctionNames().length > 0) {
                                    loadNewTriggerPage1();
                                } else {
                                    JOptionPane.showMessageDialog(frame, "You should create at least one trigger function before creating a trigger",
                                            "Trigger function required", JOptionPane.INFORMATION_MESSAGE);
                                    tree.setSelectionRow(tree.getSelectionRows() != null && tree.getSelectionRows().length > 0 ?
                                            tree.getSelectionRows()[0] + 1 :
                                            1);
                                }
                            } else if (path4.equals(Relation.L_NEW_FUNCTION)) {
                                loadNewTriggerFunctionPage1();
                            } else if (tse.getNewLeadSelectionPath().getPath().length == 5) {
                                loadTriggerPage1(rel.getTriggerByName(path4));
                            } else {
                                loadFunctionPage1((Function) rel.getTriggerByName(path4).getRel2());
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    pageFails++;
                    if (pageFails < Config.WIZARD_FAIL_LIMIT) {
                        Schedule.inEDT(Schedule.CLICK_DELAY, () -> valueChanged(tse));
                    } else {
                        Dbg.fixme("Change failed", e);
                        pageFails = 0;
                    }
                }
            } else {
                pageFails++;
                if (pageFails < Config.WIZARD_FAIL_LIMIT) {
                    Schedule.inEDT(Schedule.CLICK_DELAY, () -> valueChanged(tse));
                } else {
                    pageFails = 0;
                }
            }
        });
	}

	private boolean checkAttributeExists(String elemName){
		if(rel == null || !rel.getAttributes().isEmpty()){
			return true;
		}else{
			JOptionPane.showMessageDialog(frame, "You should create at least one attribute before creating a "+elemName.replace("Add ", ""), "Attribute required", JOptionPane.INFORMATION_MESSAGE);
			tree.selectRow(Relation.L_NEW_ATTRIBUTE);
			return false;
		}
	}

	@Override
	protected void addSQLInput(String text){
		queryInput = new FormattedClickableTextField(editableElement.getDB().getProject(), text);
		queryInput.setAutocomplete(frame, connection);
		JScrollPane queryScrollPane = new JScrollPane(queryInput);
		queryScrollPane.getVerticalScrollBar().setUnitIncrement(Settings.getInt(Settings.L_FONT_MONO_SIZE)*2);
		queryScrollPane.setRowHeaderView(new LineNumberRowHeader(queryInput, queryScrollPane));

		final RotatedTabbedPane outputTabs = new RotatedTabbedPane(JTabbedPane.RIGHT, JTabbedPane.SCROLL_TAB_LAYOUT);

		if(bodyInputScroll != null) {
			bodyInputScroll.setRowHeaderView(new LineNumberRowHeader(bodyInput, bodyInputScroll));
			QueryErrorPositionObserver.getClean(frame).registerObserver(bodyInput);
			NextFieldObserver.get(this).registerObserverWoKeyListeners(bodyInput);

			outputTabs.addTab(bodyInputTitle != null ? bodyInputTitle : editableElement.getClassName()+" body", bodyInputScroll);

			if(bodyInputScroll2 != null) {
				bodyInputScroll2.setRowHeaderView(new LineNumberRowHeader(bodyInput2, bodyInputScroll2));

				outputTabs.addTab(bodyInputTitle2 != null ? bodyInputTitle2 : editableElement.getClassName() + " body", bodyInputScroll2);
			}
		} else {
			QueryErrorPositionObserver.getClean(frame).registerObserver(queryInput);
			NextFieldObserver.get(this).registerObserverWoKeyListeners(queryInput);
			bodyInput = null;
		}
		outputTabs.addTab("SQL", queryScrollPane);
		addPanel(outputTabs, "span, width 100%, height 100%");

		Schedule.inWorker(Schedule.TYPE_DELAY, () -> {
			HistoryTableModel historyTableModel = new HistoryTableModel(editableElement);
			EditableTable historyTable = new EditableTable(historyTableModel) {
				@Override
				protected boolean isColEditable(int colIndex) {
					return false;
				}
			};
			historyTable.addMouseListener(historyTableModel.getMouseListener(historyTable));

			int rowCount = historyTableModel.getRowCount();
			if(rowCount > 0) {
				outputTabs.addTab("History" + (rowCount > 1 ? " (" + rowCount + ")" : ""), new JScrollPane(historyTable));
			}
		});

		outputTabs.addChangeListener(e -> {
            if(outputTabs.getSelectedIndex() == 1 && formatOnSQLTabOpen){
                queryInput.formatImmediately();
            }
        });

		dropChecked = false;
	}

	@Override
	protected void resetContent(){
		super.resetContent();

		bodyInputScroll = null;
		bodyInputScroll2 = null;

		bodyInputTitle = null;
		bodyInputTitle2 = null;

		btnSave.setEnabled(false);
		btnSave2.setEnabled(false);
	}

	@Override
	protected void executeAction(int type)
	{
	    try {
            if (type == CLOSE_WINDOW) {
                frame.dispose();

            } else if (type == SAVE_IN_MODEL) {
                saveEdited(true);

            } else if (type == SAVE_IN_MODEL_AND_DB) {
                saveInModelAndDB();
            }
        } catch (OperationCancelException e) {
            Dbg.info("Revision cancelled. Cancelling the whole operation.");
            checkSQLChanges();
        }
    }

	@Override
	public boolean checkSQLChanges(){
		//Dbg.toFile("Element: "+editableElement.getFullName()+" Object: "+editableElement.hashCode()+" Old behavior: "+editableElement.getBehavior().hashCode()+" New behavior: "+editableElement.getBehavior().getUpdated().hashCode());
		if(editableElement != null && queryInput != null){
			//RelationWizard.changeIsDirectlyReversible = false;
			String changed = editableElement.getQueryChanged(connection);
			if(!changed.isEmpty()){
				updateQueryInput(changed);
				btnSave.setEnabled(true);
				btnSave2.setEnabled(true);
				if(/*RelationWizard.changeIsDirectlyReversible ||*/ Config.SAVE_REVERTS_FOR_INDIRECT){
					revertSQL = editableElement.getQueryChangeRevert(connection);
				}else{
					revertSQL = null;
				}
				//Dbg.info("Forward SQL: "+changed);
				//Dbg.info("Revert SQL: "+revertSQL);
				Dbg.toFile("Forward SQL: "+changed);
				return true;
			}else{
				updateQueryInput(editableElement.getQueryCreate(connection));
				btnSave.setEnabled(false);
				btnSave2.setEnabled(false);
			}
		}
		Dbg.toFile();
		return false;
	}

	private void updateQueryInput(String changed){
		if(editableElement instanceof Function && bodyInputScroll != null && ((JTabbedPane) bodyInputScroll.getParent()).getSelectedIndex() != 1){
			queryInput.setText(changed);
			formatOnSQLTabOpen = true;
		}else{
			queryInput.setQuery(changed);
			formatOnSQLTabOpen = false;
		}
	}

	void saveEdited(boolean inModelOnly) throws OperationCancelException {
		Dbg.toFile();
		if(inModelOnly || forwardSQL == null) {
			forwardSQL = queryInput.getText();
		}

		// Prepare revision
        RevisionFactory.getCurrent(inModelOnly ? null : connection, editableElement.getFullName()).addDifference(
                database,
                new Date(),
                editableElement.getClassName(),
                editableElement.getEditedFullName(),
                forwardSQL, //.replaceAll("(?is)/\\*\\*(.*)\\*\\*/", ""),
                revertSQL
        );

        boolean wasNew = rel.checkAddedElement() || editableElement.getBehavior().isNew() || editableElement.getBehavior().getValuesForEdit().isNew();
        boolean wasDropped = editableElement.getBehavior().getValuesForEdit().isDropped();

        if (wasDropped) {
            Canvas.instance.setSelectedElement(null);
        }

		// Save changed element
		editableElement.getBehavior().getValuesForEdit().saveEdited();

		if (!inModelOnly) {
            if (wasDropped) {
                connection.afterDrop(editableElement);
            } else if (wasNew) {
                connection.afterCreate(editableElement);
            } else {
                connection.afterAlter(editableElement);
            }
        }

        updateProjectAndUI(wasNew, wasDropped);
    }

    private void updateProjectAndUI(boolean wasNew, boolean wasDropped) {
        Dbg.toFile("Element new: " + wasNew + " dropped: " + wasDropped);

        if (wasDropped) {
            if (editableElement instanceof Attribute || editableElement instanceof Constraint || editableElement instanceof Index || editableElement instanceof Trigger) {
                updateTreeToLine1();
            } else {
                clearAndClose();
            }

        } else if (wasNew) {
            Workspace ws = workspaceName != null ? Project.getCurrent().getWorkspaceByName(workspaceName) : null;
            if (editableElement instanceof Schema) {
                schema = (Schema)editableElement;
                updateTreeToLine1();

            } else if (editableElement instanceof Relation) {
                rel = (Relation)editableElement;
                updateTreeToLine1();
                if(ws != null){
                    ws.add(rel);
                    ws.find(rel).setLocation(rel.getLocation());
                }

            } else if (editableElement instanceof Function) {
                func = (Function) editableElement;
                updateTreeToLine1();
                if(ws != null){
                    ws.add(func);
                    ws.find(func).setLocation(func.getLocation());
                }

            } else if (editableElement instanceof Package) {
                pack = (Package) editableElement;
                updateTreeToLine1();
                /*if(ws != null){
                    ws.add(pack);
                    ws.find(pack).setLocation(pack.getLocation());
                }*/

            } else if (editableElement instanceof View) {
                view = (View) editableElement;
                updateTreeToLine1();
                if(ws != null){
                    ws.add(view);
                    ws.find(view).setLocation(view.getLocation());
                }

            } else if (editableElement instanceof Sequence) {
                sequence = (Sequence) editableElement;
                updateTreeToLine1();
                if(ws != null){
                    ws.add(sequence);
                    ws.find(sequence).setLocation(sequence.getLocation());
                }

            } else if (editableElement instanceof Constraint || editableElement instanceof Trigger) {
                LineComponent con = (LineComponent) editableElement;
                updateTreeToLine1();
                database.getProject().getWorkspaces().stream()
                        .filter(w ->
                                w.find((Relation) con.getRel1()) != null ||
                                        (con.getRel2() instanceof Relation && w.find((Relation) con.getRel2()) != null) ||
                                        (con.getRel2() instanceof Function && w.find((Function) con.getRel2()) != null))
                        .forEach(w -> {
                            if(w != null){
                                w.addConstraints((Relation) con.getRel1());
                                if (con.getRel2() instanceof Relation) {
                                    w.add((Relation) con.getRel2());
                                    w.addConstraints((Relation) con.getRel2());

                                } else if (con.getRel2() instanceof Function) {
                                    w.add((Function) con.getRel2());
                                    w.addTriggers((Function) con.getRel2());
                                }
                            }
                        });

            } else {
                updateTreeToLine1();
            }

        } else {
            checkSQLChanges();
            editableElement.repaint();
            rel.repaint();
            DefaultMutableTreeNode node = tree != null ? (DefaultMutableTreeNode)tree.getLastSelectedPathComponent() : null;
            if(node != null){
                ((DefaultTreeModel)tree.getModel()).reload(node);
                if(!node.isLeaf()){
                    tree.expandPath(tree.getSelectionPath());
                }
            }
        }
        Canvas.instance.drawProjectLater(true);
        SearchPanel.instance.updateDbTree();
    }

    private void clearAndClose() {
        schema = null;
        rel = null;
        func = null;
        pack = null;
        view = null;
        sequence = null;
        frame.dispose();
    }

    protected void updateTreeToLine1(){
		if(tree == null) {
			return;
		}
        Schedule.inEDT(() -> {
            Object[] value = tree.getSelectionPath().getPath();
            tree.assignNewModel(getUpdatedTreeModel(), -1);

            TreePath path = null;
            for (Object part : value) {
                int row = (path==null ? 0 : tree.getRowForPath(path));
                path = tree.getNextMatch(part.toString(), row, Position.Bias.Forward);
            }
            if(path != null){
                tree.setSelectionPath(path);
            }else{
                tree.setSelectionRow(1);
            }
        });
	}

	public boolean saveInModelAndDB() throws OperationCancelException {
		Dbg.toFile("preparing query");
		Timer t = new Timer("Relation Wizard Long Query");
		forwardSQL = queryInput.getText();
		if(bodyInputScroll != null) {
			QueryErrorPositionObserver.get(frame).setCorrection(-(forwardSQL.indexOf(bodyInput.getText())));
		}
		try {
			//Dbg.info("Query on "+database.getName()+"\n"+queryInput.getSynchronizedText());
			final IConnectionQuery running = connection.prepare(forwardSQL, database);
			t.schedule(new TimerTask(){
				@Override public void run(){
					drawLongQueryWindow(new IConnectionQuery[]{running});
				}
			}, Settings.getInt(Settings.L_NOTICE_LONG_TIMER)*1000L);
			Dbg.toFile("running query");
			running.run().close();

			t.cancel();
			hideLongQueryWindow();

			saveEdited(false);
			Dbg.toFile("all complete");
			return true;

		} catch (DBCommException ex) {
            Dbg.notImportant("Saving in DB failed", ex);
            if (connection.isSupported(SupportedElement.TRANSACTION_AUTO_ROLLBACK)) {
                Dbg.toFile("Saving in DB failed");
            } else {
                Dbg.toFile("Saving in DB failed, running manual rollback");
                connection.runManualRollback(revertSQL, database);
            }
            t.cancel();
            hideLongQueryWindow();
            Point error = connection.getErrorPosition(forwardSQL, ex.getMessage());
            if (error != null) {
                QueryErrorPositionObserver.get(frame).errorAt(error);
            }
            drawErrorWindow(frame, ERROR_WHILE_SAVING + editableElement.getFullName(), MESSAGE_EDITABLE, ex.getLocalizedMessage());
            return false;

        } catch (OperationCancelException ex) {
            Dbg.toFile("Revision cancelled. Attempting a manual rollback.");
            connection.runManualRollback(revertSQL, database);
		    throw new OperationCancelException(ex);
		}
	}

	@Override
	protected void focus(final UndoableTextField lastTextField) {
		Schedule.inEDT(100, () -> {
            lastTextField.setSelectionStart(0);
            lastTextField.setSelectionEnd(lastTextField.getText().length());
            lastTextField.requestFocusInWindow();
        });
	}

	public RelationWizard enableDrop(){
		dropChecked = true;
		return this;
	}

	public IModelElement getEditedElem(){
		return editableElement;
	}

	@Override
	public void notifyChange (String elementName, String value) {
		editableElement.getBehavior().getValuesForEdit().notifyChange(elementName, value);
		checkSQLChanges();
	}

	@Override
	public void notifyChange (String elementName, boolean value) {
		editableElement.getBehavior().getValuesForEdit().notifyChange(elementName, value);
		checkSQLChanges();
	}

	@Override
	public void notifyChange (String elementName, boolean[] values) {
		editableElement.getBehavior().getValuesForEdit().notifyChange(elementName, values);
		checkSQLChanges();
	}

}
