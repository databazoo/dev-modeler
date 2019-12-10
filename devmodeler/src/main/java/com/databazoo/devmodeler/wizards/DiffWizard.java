
package com.databazoo.devmodeler.wizards;

import com.databazoo.components.FileChooser;
import com.databazoo.components.UIConstants;
import com.databazoo.components.WizardTree;
import com.databazoo.components.combo.IconableComboBox;
import com.databazoo.components.containers.HorizontalContainer;
import com.databazoo.components.icons.DiffIconRenderer;
import com.databazoo.components.text.SelectableText;
import com.databazoo.components.textInput.FormattedClickableTextField;
import com.databazoo.components.textInput.TextScrollPane;
import com.databazoo.components.textInput.UndoableTextField;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.ConnectionBase.Query;
import com.databazoo.devmodeler.conn.DBCommException;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.IConnectionQuery;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.gui.OperationCancelException;
import com.databazoo.devmodeler.gui.view.DifferenceView;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;
import com.databazoo.devmodeler.project.Revision;
import com.databazoo.devmodeler.project.Revision.Diff;
import com.databazoo.devmodeler.project.RevisionFactory;
import com.databazoo.devmodeler.tools.comparator.DataDifference;
import com.databazoo.devmodeler.tools.formatter.FormatterDiff;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.FileFilterFactory;
import com.databazoo.tools.Schedule;
import difflib.Patch;
import difflib.myers.MyersDiff;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.*;

/**
 * UI for revisions and change comparison
 *
 * @author bobus
 */
public class DiffWizard extends SQLEnabledWizard {

	private static final int APPLY_REVISION = 6;
	private static final int REVERT_REVISION = 7;
	private static final int ADD_TO_REVISION = 10;

	private static final String L_DIFFERENTIATION_FAILED = "Differentiation failed";
	private static final String L_ACCEPT_APPLY_IN = "Accept (apply in ";
	private static final String FLYWAY_NAMING_REGEX = "[VR][_.\\-]*([0-9_.\\-]+)__.*\\.sql";
	private static final String FLYWAY_EXT = ".sql";
	private static final String COMMENT_CHANGE = "\n\n-- Change ";

	public static DiffWizard get() {
		return new DiffWizard();
	}

	FormattedClickableTextField[] queryInputs;
	private JTabbedPane inputTabs;
	private IModelElement diffElement;
	private String remoteName, localName;
	FormattedClickableTextField queryRevert, queryForward;
	private JCheckBox[] appliedInCheckboxes;
	private IconableComboBox moveRevisionNameCombo;
    IconableComboBox databaseCombo;
	private JButton btnDelete, btnUp, btnDown;

	private Revision revision;
	WizardTree tree;
	private String newChangeForwardSQL;

	private DiffWizard() {
		super(1, 0);
	}

	/**
	 * Draw revision window
	 */
	public void drawRevision(Revision rev) {
		revision = rev;
		tree = new WizardTree(rev.getTreeView(), 0, new DiffIconRenderer(), this);
		drawWindow("Revision " + rev.getName(), tree, Settings.getBool(Settings.L_MAXIMIZE_REVISIONS), true);
	}

	/**
	 * Draw difference window
	 */
	public void drawDifference(IModelElement difference, String localServer, String remoteServer) {
		diffElement = difference;
		localName = localServer;
		remoteName = remoteServer;
		tree = new WizardTree(new DefaultMutableTreeNode(diffElement), 0, new DiffIconRenderer(), this);
		drawWindow("Difference in " + diffElement.getName(), tree, Settings.getBool(Settings.L_MAXIMIZE_REVISIONS), true);
		setTreeVisible(false);
	}

	/**
	 * Draw data difference window
	 */
	public void drawDataDifference(Relation difference, String localServer, String remoteServer) {
		diffElement = difference;
		localName = localServer;
		remoteName = remoteServer;
		tree = new WizardTree(difference.getDataChanged().getTreeView(), 0, new DiffIconRenderer(), this);
		drawWindow("Data changed in " + diffElement.getName(), tree, Settings.getBool(Settings.L_MAXIMIZE_REVISIONS), true);
	}

	/**
	 * Draw "add manual change" window
	 */
	public void drawAddChange(String forwardSQL, DB database, IConnection conn) {
		newChangeForwardSQL = forwardSQL;
		connection = conn;
		tree = new WizardTree(new DefaultMutableTreeNode("Add manual change"), 0, new DiffIconRenderer(), this);
		drawWindow("Add manual change", tree, Settings.getBool(Settings.L_MAXIMIZE_REVISIONS), true);
		setTreeVisible(false);
	}

	@Override
	protected void drawTree(JComponent tree) {
		super.drawTree(tree);

		drawTreeButtons();

		if (diffElement == null || !(diffElement instanceof Relation) || ((Relation) diffElement).getDataChanged() == null) {
			JPanel buttonPanel = new JPanel(new GridLayout(0, 3));
			buttonPanel.add(btnDelete);
			buttonPanel.add(btnUp);
			buttonPanel.add(btnDown);

			drawTreeControls(buttonPanel);
		}
	}

	private void drawTreeButtons() {
		btnDelete = new JButton("Delete", Theme.getSmallIcon(Theme.ICO_DELETE));
		btnUp = new JButton("Up", Theme.getSmallIcon(Theme.ICO_SORT_UP));
		btnDown = new JButton("Down", Theme.getSmallIcon(Theme.ICO_SORT_DOWN));

		btnDelete.addActionListener(ae -> {
            Object[] options = {"Delete", "Cancel"};
            if (tree.getLeadSelectionRow() == 0) {
                int n = JOptionPane.showOptionDialog(frame, "Delete revision " + revision.getName() + "?", "Delete revision", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                if (n == 0) {
                    revision.drop();
                    project.revisions.remove(revision);
                    project.save();
                    DifferenceView.instance.updateRevisionTable();
                    frame.dispose();
                }
            } else {
                final Diff diff = revision.getChanges().get(tree.getLeadSelectionRow() - 1);
                int n = JOptionPane.showOptionDialog(frame, "Delete change " + diff.toString() + "?", "Delete change", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                if (n == 0) {
                    revision.getChanges().remove(diff);
                    revision.save();
                    tree.assignNewModel(revision.getTreeView());
                    DifferenceView.instance.updateRevisionTable();
                    project.save();
                }
            }
        });
		btnUp.addActionListener(ae -> {
            int selectedRow = tree.getLeadSelectionRow();

            Diff previous = revision.getChanges().get(selectedRow - 2);
            Diff diff = revision.getChanges().get(selectedRow - 1);

            diff.setCreated(previous.getCreated() - 1);
            Collections.sort(revision.getChanges());
            revision.save();
            tree.assignNewModel(revision.getTreeView());
            tree.setSelectionRow(selectedRow - 1);
        });
		btnDown.addActionListener(ae -> {
            int selectedRow = tree.getLeadSelectionRow();

            Diff next = revision.getChanges().get(selectedRow);
            Diff diff = revision.getChanges().get(selectedRow - 1);

            diff.setCreated(next.getCreated() + 1);
            Collections.sort(revision.getChanges());
            revision.save();
            tree.assignNewModel(revision.getTreeView());
            tree.setSelectionRow(selectedRow + 1);
        });
	}

	@Override
	public void valueChanged(final TreeSelectionEvent tse) {
		Schedule.inEDT(() -> {
			if (btnDelete != null && tse.getNewLeadSelectionPath() != null) {
				boolean relationDataChange = diffElement instanceof Relation && ((Relation) diffElement).getDataChanged() != null;
				if (tse.getNewLeadSelectionPath().getPath().length == 1) {
					if (revision != null) {
						loadRevisionPage1();
					} else if (newChangeForwardSQL != null) {
						loadNewChangePage();
					} else if (relationDataChange) {
						tree.setSelectionRow(1);
					} else {
						loadDifferencePage1();
					}
				} else if (relationDataChange) {
					String leadPath = tse.getNewLeadSelectionPath().getLastPathComponent().toString();
					loadDataChangePage(leadPath.equals(DataDifference.L_INSERTED) ? 1 : (leadPath.equals(DataDifference.L_DELETED) ? -1 : 0));
				} else if (tse.getNewLeadSelectionPath().getLastPathComponent().toString().equals(Revision.L_ADD_MANUAL_CHANGE)) {
					loadNewChangePage();
				} else {
					loadChangePage(tree.getSelectionRows()[0] - 1);
				}
				checkTreeButtons();
			} else {
				Schedule.inEDT(Schedule.CLICK_DELAY, () -> valueChanged(tse));
			}
		});
	}

	private void checkTreeButtons() {
		if (tree.getLeadSelectionRow() == 0) {
			btnDelete.setEnabled(true);
			btnUp.setEnabled(false);
			btnDown.setEnabled(false);

		} else if (tree.getLeadSelectionPath().getLastPathComponent().toString().equals(Revision.L_ADD_MANUAL_CHANGE)) {
			btnDelete.setEnabled(false);
			btnUp.setEnabled(false);
			btnDown.setEnabled(false);

		} else if (diffElement != null && diffElement instanceof Relation && ((Relation) diffElement).getDataChanged() != null) {
			// Nothing here...

		} else {
			btnDelete.setEnabled(true);
			btnUp.setEnabled(tree.getLeadSelectionRow() > 1);
			btnDown.setEnabled(tree.getLeadSelectionRow() < revision.getCntChanges());
		}
	}

	private void loadRevisionPage1() {
		resetContent();
		addTitle("Revision " + revision.getName());

		final JLabel approvedByLabel = new JLabel(revision.getApprovedBy());

		final UndoableTextField nameInput = new UndoableTextField(revision.getName());
		nameInput.disableFinder();
		nameInput.setBordered(true);
		nameInput.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent ke) {
				Schedule.reInvokeInWorker(Schedule.Named.MIG_WIZARD_INPUT_NOTIFICATION, UIConstants.TYPE_TIMEOUT, () -> {
                    revision.setName(nameInput.getText());
                    DifferenceView.instance.updateRevisionTable();
                    project.save();
                });
			}
		});
		final UndoableTextField authorInput = new UndoableTextField(revision.getAuthor());
		authorInput.disableFinder();
		authorInput.setBordered(true);
		authorInput.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent ke) {
				Schedule.reInvokeInWorker(Schedule.Named.MIG_WIZARD_INPUT_NOTIFICATION, UIConstants.TYPE_TIMEOUT, () -> {
                    revision.setAuthor(authorInput.getText());
                    DifferenceView.instance.updateRevisionTable();
                    project.save();
                });
			}
		});
		final JCheckBox closedCB = new JCheckBox("Closed", revision.isClosed);
		closedCB.addActionListener(ae -> Schedule.reInvokeInWorker(Schedule.Named.MIG_WIZARD_INPUT_NOTIFICATION, UIConstants.TYPE_TIMEOUT, () -> {
            revision.isClosed = closedCB.isSelected();
            revision.save();
            DifferenceView.instance.updateRevisionTable();
            project.save();
        }));
		final JCheckBox approvedCB = new JCheckBox("Approved", revision.isApproved);
		if (revision.getAuthor().equals(Settings.getStr(Settings.L_REVISION_AUTHOR))) {
			approvedCB.setEnabled(false);
		} else {
			approvedCB.addActionListener(ae -> Schedule.reInvokeInWorker(Schedule.Named.MIG_WIZARD_INPUT_NOTIFICATION, UIConstants.TYPE_TIMEOUT, () -> {
                if (!revision.isClosed && approvedCB.isSelected()) {
                    revision.isClosed = true;
                    closedCB.setSelected(true);
                }
                revision.isApproved = approvedCB.isSelected();
                if (revision.isApproved) {
                    revision.setApprovedBy(Settings.getStr(Settings.L_REVISION_AUTHOR));
                } else {
                    revision.save();
                }
                approvedByLabel.setText(revision.getApprovedBy());
                DifferenceView.instance.updateRevisionTable();
                project.save();
            }));
		}
		final JCheckBox archiveCB = new JCheckBox("Archived", revision.isArchived);
		archiveCB.addActionListener(ae -> Schedule.reInvokeInWorker(Schedule.Named.MIG_WIZARD_INPUT_NOTIFICATION, UIConstants.TYPE_TIMEOUT, () -> {
            if (!revision.isClosed && archiveCB.isSelected()) {
                revision.isClosed = true;
                closedCB.setSelected(true);
            }
            revision.isArchived = archiveCB.isSelected();
            revision.save();
            DifferenceView.instance.updateRevisionTable();
            project.save();
        }));
		JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		statusPanel.add(closedCB);
		statusPanel.add(approvedCB);
		statusPanel.add(archiveCB);

		addPanel("Title", nameInput, "width 50%-100px!");
		addText("Created", "width 100px!");
		addText(revision.getDate());

		addPanel("Author", authorInput, "width 50%-100px!");
		addText("Last change");
		addText(revision.getChangeTime());

		addPanel("Status", statusPanel, "width 50%-100px!");
		addText("Changes");
		addText(Integer.toString(revision.getCntChanges()));

		addComment("UID", "width 80px!");
		addComment(revision.UID);
		addPanel("Approved by", approvedByLabel);

		addEmptyLine();

		JPanel applyPanel = new JPanel(new GridLayout(0, 3, 2, 2));
		applyPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));

		JButton b = new JButton("View revert script", Theme.getSmallIcon(Theme.ICO_VIEW));
		b.addActionListener(ae -> prepareRevert(null));
		applyPanel.add(b);

		b = new JButton("Export to FlyWay", Theme.getSmallIcon(Theme.ICO_FLYWAY));
		b.addActionListener(ae -> saveToFlyway());
		applyPanel.add(b);

		b = new JButton("View forward script", Theme.getSmallIcon(Theme.ICO_VIEW));
		b.addActionListener(ae -> prepareApply(null));
		applyPanel.add(b);

		if (project.getType() != Project.TYPE_ABSTRACT) {
			String appliedIn = "," + revision.getAppliedIn() + ",";
			List<IConnection> conns = project.getConnections();
			for (final IConnection conn : conns) {
				boolean applied = appliedIn.matches(".*[ ,]+" + conn.getName() + "[ ,]+.*");
				if (applied) {
					b = new JButton("Revert", Theme.getSmallIcon(Theme.ICO_REVERT));
					b.addActionListener(ae -> prepareRevert(conn.getName()));
					applyPanel.add(b);
				} else {
					applyPanel.add(new JLabel());
				}
				applyPanel.add(new JLabel(conn.getFullName()));
				if (!applied) {
					b = new JButton("Apply", Theme.getSmallIcon(Theme.ICO_RUN));
					b.addActionListener(ae -> prepareApply(conn.getName()));
					applyPanel.add(b);
				} else {
					applyPanel.add(new JLabel());
				}
			}
		}
		addPanel(applyPanel, "span, width 100%!");

		List<DB> dbs = project.getDatabases();
		queryInputs = new FormattedClickableTextField[dbs.size()];
		inputTabs = new JTabbedPane();
		for (int i = 0; i < dbs.size(); i++) {
			queryInputs[i] = new FormattedClickableTextField(database.getProject());
			queryInputs[i].setAutocomplete(frame, connection);
			inputTabs.addTab(dbs.get(i).getName(), new TextScrollPane(queryInputs[i]));
		}

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		addPanel(inputTabs, "span, grow, height 100%");

		setNextButton("Apply", false, 0);
	}

	private void saveToFlyway() {
		final File flywayPath = project.getFlywayFolder();
		File file = FileChooser.showWithOverwriteNoCache(
				"Save export to",
				"Save",
				getFlywayExportFile(flywayPath, flywayPath.listFiles()),
				FileFilterFactory.getSqlFilter()
		);
		if(file != null) {
			project.setFlywayPath(file.getParent());
			ProjectManager.getInstance().saveProjects();

			writeToFlyway(file);
		}
	}

	private void writeToFlyway(File file) {
		HashMap<String, Integer> dbIndexes = new HashMap<>();
		List<DB> dbs = project.getDatabases();
		for (int i = 0; i < dbs.size(); i++) {
            dbIndexes.put(dbs.get(i).getName(), i);
        }
		String previousDbName = null;
		boolean multipleDBs = false;
		String[] sqlStrings = new String[dbs.size()];
		for (int i = 0; i < revision.getCntChanges(); i++) {
            Diff change = revision.getChanges().get(i);
            if(previousDbName != null && !previousDbName.equals(change.getDBName())){
                multipleDBs = true;
            } else {
                previousDbName = change.getDBName();
            }
            int iDB = dbIndexes.get(change.getDBName());
            if (sqlStrings[iDB] == null) {
                sqlStrings[iDB] = "";
            }
            sqlStrings[iDB] += COMMENT_CHANGE + (i + 1) + "\n" + change.getForwardSQL();
        }

		try {
            try(BufferedWriter out = new BufferedWriter(new FileWriter(file))){
                for (int i = 0; i < sqlStrings.length; i++) {
					writeSqlForDb(dbs.get(i).getName(), multipleDBs, sqlStrings[i], out);
				}
                out.close();
                frame.dispose();
            }
        } catch (IOException ex) {
            Dbg.fixme("Export to SQL failed", ex);
            String message = "File could not be saved.\n\n"
                    + "Error details:\n"
                    + ex.getLocalizedMessage();
            JOptionPane.showMessageDialog(frame, new SelectableText(message, false), ERROR_WHILE_SAVING + file, JOptionPane.ERROR_MESSAGE);
        }
	}

	private void writeSqlForDb(String name, boolean multipleDBs, String sql, BufferedWriter out) throws IOException {
		if(sql != null && !sql.isEmpty()) {
            if (multipleDBs) {
                out.write("/**\n * Changes on ");
                out.write(name);
                out.write("\n**/");
            }
            out.write(sql);
        }
	}

	File getFlywayExportFile(final File path, final File[] files) {
		final String revisionName = ucFirst(revision.getName()
				.replaceAll("[^A-Za-z0-9_.\\-]+", "_")
				.replaceAll("^[0-9_.\\-]*(.*)", "$1")
				.replaceAll("(.*?)[0-9_.\\-]*$", "$1"));
		final String defaultName = "V0001__" + revisionName + FLYWAY_EXT;
		if(files == null) {
			return new File(path, defaultName);
		} else {
			final File lastFile = Arrays.stream(files)
					.filter(file -> file.getName().matches(FLYWAY_NAMING_REGEX))
					.sorted(Comparator.comparing(File::getName))
					.reduce((first, second) -> second)
					.orElse(null);
			if(lastFile != null) {
				String lastRevNumber = lastFile.getName().replaceAll(FLYWAY_NAMING_REGEX, "$1");
				final String versionMajor = lastRevNumber.split("[^0-9]+")[0];
				if(!versionMajor.isEmpty()){
					String leadingZeroes = versionMajor.replaceAll("(0*).*", "$1");
					int newVersionMajor = Integer.parseInt(versionMajor) + 1;
					String newRevNumber = newVersionMajor + lastRevNumber.replaceAll("[0-9]+", "0").substring(1);
					return new File(path, "V" + leadingZeroes + newRevNumber + "__" + revisionName + FLYWAY_EXT);
				}
			}

			return new File(path, defaultName);
		}
	}

	private String ucFirst(String originalName) {
		final int length = originalName.length();
		if(length > 0) {
			return originalName.substring(0, 1).toUpperCase() + originalName.substring(1);
		} else {
			return originalName;
		}
	}

	private void loadChangePage(int row) {
		final Diff diff = revision.getChanges().get(row);
		resetContent();

		moveRevisionNameCombo = new IconableComboBox(getOtherRevisionNames());
		JButton moveButton = new JButton("Move");
		moveButton.addActionListener(e -> {
            Revision selectedRevision;
            if (moveRevisionNameCombo.getSelectedIndex() == 0) {
                selectedRevision = new Revision("#" + RevisionFactory.getNextID(project.revisions), false);
                selectedRevision.setAppliedIn(revision.getAppliedIn());
				try {
					selectedRevision.askForName();
				} catch (OperationCancelException ex) {
					Dbg.notImportantAtAll("Revision cancelled.", ex);
					return;
				}
				project.revisions.add(selectedRevision);
            } else {
                selectedRevision = project.getRevisionByName(
                		moveRevisionNameCombo.getSelectedItem() != null ? moveRevisionNameCombo.getSelectedItem().toString() : ""
				);
            }
            selectedRevision.getChanges().add(diff);
            selectedRevision.setAge();
            selectedRevision.save();
            Collections.sort(selectedRevision.getChanges());

            revision.getChanges().remove(diff);
            revision.save();
            Collections.sort(project.revisions);

            tree.assignNewModel(revision.getTreeView());
            DifferenceView.instance.updateRevisionTable();
            project.save();
        });

		String revertSQL = diff.getRevertSQL().replaceAll("^\\s+", "");
		String forwardSQL = diff.getForwardSQL().replaceAll("^\\s+", "");

		updateQueryTextFields(revertSQL, forwardSQL);

		JScrollPane scroll1, scroll2;

		addText("Revert SQL (OLD)", SPAN2_CENTER);
		addText("Forward SQL (NEW)", SPAN2_CENTER);
		addPanel(scroll2 = new TextScrollPane(queryRevert), SPAN2_H100_W5050);
		addPanel(scroll1 = new TextScrollPane(queryForward), SPAN2_H100_W5050);
		addPanel(" Move to revision", new HorizontalContainer(null, moveRevisionNameCombo, moveButton), "width 50%-120px!, align right");
		addEmptyLine();

		synchronizeScrolls(scroll1, scroll2);

		setButtonsVisible(false);
		validate();
	}

	private void loadNewChangePage() {
		resetContent();

		queryRevert = new FormattedClickableTextField(database.getProject(), "-- no revert needed");
		queryForward = new FormattedClickableTextField(database.getProject(), newChangeForwardSQL);

		queryRevert.setAutocomplete(frame, connection);
		queryForward.setAutocomplete(frame, connection);

		addText("Revert SQL (OLD):", SPAN2_CENTER);
		addText("Forward SQL (NEW):", SPAN2_CENTER);
		addPanel(new TextScrollPane(queryRevert), SPAN2_H100_W5050);
		addPanel(new TextScrollPane(queryForward), SPAN2_H100_W5050);

		if (tree.getRowCount() == 1) {
			JPanel appCheckBoxPanel = new JPanel(new GridLayout(0, 1, 0, 0));
			String[] connNames = project.getConnectionNames();
			appliedInCheckboxes = new JCheckBox[connNames.length];
			for (int i = 0; i < connNames.length; i++) {
				appliedInCheckboxes[i] = new JCheckBox(connNames[i]);
				appCheckBoxPanel.add(appliedInCheckboxes[i]);
			}

			moveRevisionNameCombo = new IconableComboBox(getOtherRevisionNames());
			moveRevisionNameCombo.addActionListener(ae -> checkAppliedCheckboxes());

			addPanel(" Select revision", moveRevisionNameCombo, "width 50%-120px!, align right");
			addPanel(" Already applied in", appCheckBoxPanel);

			checkAppliedCheckboxes();
		}
		addEmptySlot();
		addPanel(" Database ", databaseCombo = new IconableComboBox(project.getDatabaseNames()));
		databaseCombo.setSelectedItem(database.getName());

		setNextButton("Add to revision", true, ADD_TO_REVISION);
	}

	private void loadDataChangePage(int direction) {
		resetContent();

		Relation elem = (Relation) diffElement;

		Map<String, String> changeMap = direction == 0 ? elem.getDataChanged().updated : (direction < 0 ? elem.getDataChanged().deleted : elem.getDataChanged().inserted);

		StringBuilder reverseString = new StringBuilder();
		StringBuilder forwardString = new StringBuilder();

		for (Map.Entry<String, String> reverseQ : changeMap.entrySet()) {
			reverseString.append(reverseQ.getKey()).append("\n");
			forwardString.append(reverseQ.getValue()).append("\n");
		}

		final String revertSQL = reverseString.toString();
		final String forwardSQL = forwardString.toString();

		updateQueryTextFields(revertSQL, forwardSQL);

		JButton buttonAccept1 = new JButton(L_ACCEPT_APPLY_IN + remoteName + ")", Theme.getSmallIcon(Theme.ICO_RUN));
		buttonAccept1.addActionListener(ae -> Schedule.inWorker(() -> accept(diffElement.getDB(), project.getConnectionByName(remoteName), revertSQL)));
		JButton buttonAccept2 = new JButton(L_ACCEPT_APPLY_IN + localName + ")", Theme.getSmallIcon(Theme.ICO_RUN));
		buttonAccept2.addActionListener(ae -> Schedule.inWorker(() -> accept(diffElement.getDB(), project.getConnectionByName(localName), forwardSQL)));
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(0, 2, 0, 0));
		buttonPanel.add(buttonAccept1);
		buttonPanel.add(buttonAccept2);

		JScrollPane scroll1, scroll2;

		addText(localName, SPAN2_CENTER);
		addText(remoteName, SPAN2_CENTER);
		addPanel(scroll2 = new TextScrollPane(queryRevert), SPAN2_H100_W5050);
		addPanel(scroll1 = new TextScrollPane(queryForward), SPAN2_H100_W5050);
		addPanel(buttonAccept1, SPAN2_GROW);
		addPanel(buttonAccept2, SPAN2_GROW);

		synchronizeScrolls(scroll1, scroll2);

		setNextButton("Add to revision", true, ADD_TO_REVISION);
		setButtonsVisible(false);
	}

	private void loadDifferencePage1() {
		resetContent();

		if (diffElement instanceof Constraint) {
			((Constraint) diffElement).checkReferenceNullable();
		}

		final String revertSQL = diffElement.getQueryChangeRevert(connection).replaceAll("^\\s+", "");
		final String forwardSQL = diffElement.getQueryChanged(connection).replaceAll("^\\s+", "");

		updateQueryTextFields(revertSQL, forwardSQL);

		JButton btnAcc1 = new JButton(L_ACCEPT_APPLY_IN + remoteName + ")", Theme.getSmallIcon(Theme.ICO_RUN));
		btnAcc1.addActionListener(ae -> Schedule.inWorker(() -> accept(diffElement.getDB(), project.getConnectionByName(remoteName), revertSQL)));
		JButton btnAcc2 = new JButton(L_ACCEPT_APPLY_IN + localName + ")", Theme.getSmallIcon(Theme.ICO_RUN));
		btnAcc2.addActionListener(ae -> Schedule.inWorker(() -> accept(diffElement.getDB(), project.getConnectionByName(localName), forwardSQL)));
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(0, 2, 0, 0));
		buttonPanel.add(btnAcc1);
		buttonPanel.add(btnAcc2);

		JScrollPane scroll1, scroll2;

		addText(localName, SPAN2_CENTER);
		addText(remoteName, SPAN2_CENTER);
		addPanel(scroll2 = new TextScrollPane(queryRevert), SPAN2_H100_W5050);
		addPanel(scroll1 = new TextScrollPane(queryForward), SPAN2_H100_W5050);
		addPanel(btnAcc1, SPAN2_GROW);
		addPanel(btnAcc2, SPAN2_GROW);

		synchronizeScrolls(scroll1, scroll2);

		setButtonsVisible(false);
	}

	private void updateQueryTextFields(String revertSQL, String forwardSQL) {
		try {
			Patch<String> patch = new MyersDiff<String>().diff(Arrays.asList(revertSQL.split("\\r?\\n")), Arrays.asList(forwardSQL.split("\\r?\\n")));
			queryRevert = new FormattedClickableTextField(database.getProject(), revertSQL, new FormatterDiff(patch.getDeltas(), true));
			queryForward = new FormattedClickableTextField(database.getProject(), forwardSQL, new FormatterDiff(patch.getDeltas(), false));
		} catch (Exception e) {
			Dbg.fixme(L_DIFFERENTIATION_FAILED, e);
			queryRevert = new FormattedClickableTextField(database.getProject(), revertSQL);
			queryForward = new FormattedClickableTextField(database.getProject(), forwardSQL);
		}

		queryRevert.setEditable(false);
		queryForward.setEditable(false);
	}

	private String[] getOtherRevisionNames() {
		String currRevName = revision == null ? "" : revision.getName();
		List<Revision> revs = project.revisions;
		String[] ret = new String[revs.size() + (revision == null ? 1 : 0)];
		ret[0] = "Create new revision";
		int j = ret.length - 1;
		for (Revision rev : revs) {
			if (!rev.getName().equals(currRevName)) {
				ret[j] = rev.getName();
				j--;
			}
		}
		return ret;
	}

	private void accept(DB db, IConnection connection, String sql) {
		Timer t = new Timer("RelationWizardLongQuery");
		try {
			final IConnectionQuery running;
			IConnection dedicatedConn = project.getDedicatedConnection(db.getName(), connection.getName());
			if (dedicatedConn == null) {
				running = connection.prepare(sql, db);
			} else {
				running = dedicatedConn.prepare(sql, db);
			}
			running.useExecuteUpdate(true);

			t.schedule(new TimerTask() {
				@Override
				public void run() {
					drawLongQueryWindow(new IConnectionQuery[]{running});
				}
			}, Settings.getInt(Settings.L_NOTICE_LONG_TIMER) * 1000L);
			running.run().close();

			t.cancel();
			hideLongQueryWindow();

			if (diffElement != null && diffElement instanceof Relation && ((Relation) diffElement).getDataChanged() != null) {
				drawSuccessWindow(frame);
			} else {
				DifferenceView.instance.removeDifference(diffElement);
				frame.dispose();
			}
		} catch (DBCommException ex) {
			t.cancel();
			hideLongQueryWindow();
			Dbg.notImportant("Failed to apply a difference", ex);
			drawErrorWindow(frame, "Error while applying change in " + connection.getName(), MESSAGE_UNI, ex.getLocalizedMessage());
		}
	}

	public void prepareApply(String connName) {
		HashMap<String, Integer> dbIndexes = new HashMap<>();
		List<DB> dbs = project.getDatabases();
		for (int i = 0; i < dbs.size(); i++) {
			dbIndexes.put(dbs.get(i).getName(), i);
		}
		String[] SQLs = new String[queryInputs.length];
		for (int i = 0; i < revision.getCntChanges(); i++) {
			Diff change = revision.getChanges().get(i);
			int iDB = dbIndexes.get(change.getDBName());
			if (SQLs[iDB] == null) {
				SQLs[iDB] = "";
			}
			SQLs[iDB] += COMMENT_CHANGE + (i + 1) + "\n" + change.getForwardSQL();
		}
		for (int i = 0; i < queryInputs.length; i++) {
			queryInputs[i].setText(SQLs[i]);
			queryInputs[i].format();
		}
		if (SQLs[inputTabs.getSelectedIndex()] == null) {
			inputTabs.setSelectedIndex(dbIndexes.get(revision.getChanges().get(0).getDBName()));
		}

		if(connName != null) {
			setNextButton("Apply in " + connName, true, APPLY_REVISION, connName);
		} else {
			setNextButton("Preview", false, APPLY_REVISION);
		}
	}

	public void prepareRevert(String connName) {
		HashMap<String, Integer> dbIndexes = new HashMap<>();
		List<DB> dbs = project.getDatabases();
		for (int i = 0; i < dbs.size(); i++) {
			dbIndexes.put(dbs.get(i).getName(), i);
		}
		String[] SQLs = new String[queryInputs.length];
		for (int i = revision.getCntChanges() - 1; i >= 0; i--) {
			Diff change = revision.getChanges().get(i);
			int iDB = dbIndexes.get(change.getDBName());
			if (SQLs[iDB] == null) {
				SQLs[iDB] = "";
			}
			SQLs[iDB] += COMMENT_CHANGE + (i + 1) + "\n" + change.getRevertSQL();
		}
		for (int i = 0; i < queryInputs.length; i++) {
			queryInputs[i].setText(SQLs[i]);
			queryInputs[i].format();
		}
		if (SQLs[inputTabs.getSelectedIndex()] == null) {
			inputTabs.setSelectedIndex(dbIndexes.get(revision.getChanges().get(0).getDBName()));
		}

		if(connName != null) {
			setNextButton("Revert in " + connName, true, REVERT_REVISION, connName);
		} else {
			setNextButton("Preview", false, REVERT_REVISION);
		}
	}

	@Override
	protected void executeAction(int type, String extra) {
		if (type == APPLY_REVISION) {
			if (connection.isSupported(SupportedElement.TRANSACTION_AUTO_ROLLBACK)) {
				saveToDB(extra, false);
			} else {
				saveToDBWithManualRollback(extra, false);
			}

		} else if (type == REVERT_REVISION) {
			if (connection.isSupported(SupportedElement.TRANSACTION_AUTO_ROLLBACK)) {
				saveToDB(extra, true);
			} else {
				saveToDBWithManualRollback(extra, true);
			}

		} else {
			super.executeAction(type, extra);
		}
	}

	@Override
	protected void executeAction(int type) {
		if (type == ADD_TO_REVISION) {
			addToRevision();
			if (revision == null) {
				frame.dispose();
			} else {
				tree.assignNewModel(revision.getTreeView());
			}
		} else {
			super.executeAction(type);
		}
	}

	public void saveToDB(String serverName, boolean revert) {
		cancelPressed = false;
		IConnection defaultConn = project.getConnectionByName(serverName);
		final IConnectionQuery[] statements = new Query[queryInputs.length];
		Timer t = new Timer("RelationWizardLongQuery");
		try {
			for (int i = 0; i < statements.length; i++) {
				if (queryInputs[i].getText().isEmpty()) {
					continue;
				}
				DB db = project.getDatabases().get(i);
				IConnection dedicatedConn = project.getDedicatedConnection(db.getName(), defaultConn.getName());
				if (dedicatedConn == null) {
					statements[i] = defaultConn.prepareWithBegin(queryInputs[i].getText(), db);
				} else {
					statements[i] = dedicatedConn.prepareWithBegin(queryInputs[i].getText(), db);
				}
				statements[i].useExecuteUpdate(true);
			}

			t.schedule(new TimerTask() {
				@Override
				public void run() {
					drawLongQueryWindow(statements);
				}
			}, Settings.getInt(Settings.L_NOTICE_LONG_TIMER) * 1000L);

			for (IConnectionQuery statement : statements) {
				if (statement != null) {
					statement.run();
				}
				if (cancelPressed) {
					throw new DBCommException("Query cancelled", "");
				}
			}
			for (IConnectionQuery statement : statements) {
				if (statement != null) {
					statement.commit();
					statement.close();
				}
			}
			//Dbg.info("Statements commited OK");

			t.cancel();
			hideLongQueryWindow();

			if (revert) {
				revision.revertIn(defaultConn);
			} else {
				revision.applyIn(defaultConn);
			}
			Schedule.inEDT(this::loadRevisionPage1);
			project.save();
			DifferenceView.instance.updateRevisionTable();
		} catch (DBCommException ex) {
			t.cancel();
			Dbg.notImportant("Failed to write diff to DB", ex);
			for (int i = 0; i < statements.length; i++) {
				try {
					if (statements[i] != null) {
						statements[i].rollback();
						statements[i].close();
					}
				} catch (DBCommException e) {
					Dbg.fixme("ROLLBACK FAILED ON " + project.getDatabases().get(i).getFullName(), e);
				}
			}
			hideLongQueryWindow();
			if (!cancelPressed) {
				drawErrorWindow(frame, "Error while " + (revert ? "reverting" : "applying") + " revision " + revision.getName(), MESSAGE_UNI, ex.getLocalizedMessage());
			}
		}
	}

	public void saveToDBWithManualRollback(String serverName, boolean revert) {
		List<Diff> successfulChanges = new ArrayList<>();
		Diff currentChange = null;

		cancelPressed = false;
		IConnection defaultConn = project.getConnectionByName(serverName);
		Timer t = new Timer("RelationWizardLongQuery");
		try {
			t.schedule(new TimerTask() {
				@Override
				public void run() {
					drawLongQueryWindow(new IConnectionQuery[0]);
				}
			}, Settings.getInt(Settings.L_NOTICE_LONG_TIMER) * 1000L);

			if (revert) {
				for (int i = revision.getCntChanges() - 1; i >= 0; i--) {
					currentChange = revision.getChanges().get(i);
					runStatement(currentChange.getRevertSQL(), currentChange.getDB(), defaultConn);
					successfulChanges.add(currentChange);
				}
			} else {
				for (int i = 0; i < revision.getCntChanges(); i++) {
					currentChange = revision.getChanges().get(i);
					runStatement(currentChange.getForwardSQL(), currentChange.getDB(), defaultConn);
					successfulChanges.add(currentChange);
				}
			}

			t.cancel();
			hideLongQueryWindow();

			if (revert) {
				revision.revertIn(defaultConn);
			} else {
				revision.applyIn(defaultConn);
			}
			Schedule.inEDT(this::loadRevisionPage1);
			project.save();
			DifferenceView.instance.updateRevisionTable();
		} catch (DBCommException ex) {
			t.cancel();
			Dbg.notImportant("Failed to write diff to DB", ex);
			boolean cancelWasPressed = cancelPressed;
			cancelPressed = false;

			// REVERT LAST QUERY LINE-BY-LINE
			IConnection dedicatedConn = project.getDedicatedConnection(currentChange.getDBName(), defaultConn.getName());
			String sql = revert ? currentChange.getForwardSQL() : currentChange.getRevertSQL();
			if (dedicatedConn == null) {
				defaultConn.runManualRollbackAndWait(sql, currentChange.getDB());
			} else {
				dedicatedConn.runManualRollbackAndWait(sql, currentChange.getDB());
			}

			// REVERT SUCCESSFUL QUERIES IN BLOCKS
			for (int i = successfulChanges.size() - 1; i >= 0; i--) {
				currentChange = successfulChanges.get(i);
				sql = revert ? currentChange.getForwardSQL() : currentChange.getRevertSQL();
				try {
					runStatement(sql, currentChange.getDB(), defaultConn);
				} catch (Exception e) {
					Dbg.notImportant("Reverting individual lines may fail depending on which queries were successful in the first place", e);
				}
			}

			hideLongQueryWindow();
			if (!cancelWasPressed) {
				drawErrorWindow(frame, "Error while " + (revert ? "reverting" : "applying") + " revision " + revision.getName(), MESSAGE_UNI, ex.getLocalizedMessage());
			}
		}
	}

	private void runStatement(String sql, DB db, IConnection defaultConn) throws DBCommException {
		IConnectionQuery statement;
		IConnection dedicatedConn = project.getDedicatedConnection(db.getName(), defaultConn.getName());
		if (dedicatedConn == null) {
			statement = defaultConn.prepare(sql, db);
		} else {
			statement = dedicatedConn.prepare(sql, db);
		}
		statement.useExecuteUpdate(true);
		statement.run().close();
		if (cancelPressed) {
			throw new DBCommException("Query cancelled", "");
		}
	}

	private void synchronizeScrolls(final JScrollPane scroll1, final JScrollPane scroll2) {
		scroll1.getViewport().addChangeListener(ce -> Schedule.inEDT(() -> notifyScrolls(scroll1, scroll2)));
		scroll2.getViewport().addChangeListener(ce -> Schedule.inEDT(() -> notifyScrolls(scroll2, scroll1)));
	}

	/**
	 * Synchronize scroll positions for diff outputs.
	 *
	 * @param source scroll pane
	 * @param dest scroll pane
	 */
	private void notifyScrolls(JScrollPane source, JScrollPane dest) {
		int sourceTop = source.getVerticalScrollBar().getValue();
		int sourceLeft = source.getHorizontalScrollBar().getValue();

		int destTop = dest.getVerticalScrollBar().getValue();
		int destLeft = dest.getHorizontalScrollBar().getValue();

		int destMaxTop = dest.getVerticalScrollBar().getMaximum() - dest.getViewport().getHeight();
		int destMaxLeft = dest.getHorizontalScrollBar().getMaximum() - dest.getViewport().getWidth();

		// Scroll is required and have enough scroll space left. Otherwise just scroll to last possible position.
		if ((destTop != sourceTop && (sourceTop <= destMaxTop || destTop < destMaxTop)) ||
				(destLeft != sourceLeft && (sourceLeft <= destMaxLeft || destLeft < destMaxLeft))) {
			Point position = new Point(
					sourceLeft <= destMaxLeft ? sourceLeft : destMaxLeft,
					sourceTop <= destMaxTop ? sourceTop : destMaxTop);
			dest.getViewport().setViewPosition(position);
		}
	}

	private void checkAppliedCheckboxes() {
		if (moveRevisionNameCombo.getSelectedIndex() == 0) {
			for (JCheckBox cb : appliedInCheckboxes) {
				cb.setSelected(connection.getFullName().equals(cb.getText()));
				cb.setEnabled(true);
			}
		} else {
			Revision selectedRevision = project.getRevisionByName((String) moveRevisionNameCombo.getSelectedItem());
			for (JCheckBox cb : appliedInCheckboxes) {
				cb.setSelected(selectedRevision.getAppliedIn().contains(", " + project.getConnectionByName(cb.getText()).getName()));
				cb.setEnabled(false);
			}
		}
	}

	private void addToRevision() {
		Revision selectedRevision;

		if (revision != null) {
			selectedRevision = revision;

		} else if (moveRevisionNameCombo.getSelectedIndex() == 0) {
			selectedRevision = new Revision("#" + RevisionFactory.getNextID(project.revisions), false);
			try {
				selectedRevision.askForName();
			} catch (OperationCancelException ex) {
				Dbg.notImportantAtAll("Revision cancelled.", ex);
				return;
			}
			project.revisions.add(selectedRevision);

			for (JCheckBox cb : appliedInCheckboxes) {
				if (cb.isSelected()) {
					selectedRevision.applyIn(project.getConnectionByName(cb.getText()));
				}
			}

		} else {
			selectedRevision = project.getRevisionByName((String) moveRevisionNameCombo.getSelectedItem());
		}
		selectedRevision.addDifference(project.getDatabaseByName(databaseCombo.getSelectedItem().toString()), new Date(), "Data", "Manual change", queryForward.getText(), queryRevert.getText());
		DifferenceView.instance.updateRevisionTable();
	}

	@Override public void notifyChange(String elementName, String value) {}
	@Override public void notifyChange(String elementName, boolean value) {}
	@Override public void notifyChange(String elementName, boolean[] values) {}
}
