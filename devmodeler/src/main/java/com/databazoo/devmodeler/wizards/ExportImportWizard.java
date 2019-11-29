
package com.databazoo.devmodeler.wizards;

import com.databazoo.components.FileChooser;
import com.databazoo.components.combo.IconableComboBox;
import com.databazoo.components.containers.HorizontalContainer;
import com.databazoo.components.text.SelectableText;
import com.databazoo.components.textInput.FormattedClickableTextField;
import com.databazoo.components.textInput.FormattedTextField;
import com.databazoo.components.textInput.TextScrollPane;
import com.databazoo.components.textInput.UndoableTextField;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.SQLOutputConfigExport;
import com.databazoo.devmodeler.conn.SupportedElement;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.Navigator;
import com.databazoo.devmodeler.gui.SearchPanel;
import com.databazoo.devmodeler.gui.window.ProgressWindow;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.tools.formatter.FormatterSQL;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.FileFilterFactory;
import com.databazoo.tools.Schedule;
import net.miginfocom.swing.MigLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import static com.databazoo.devmodeler.conn.SQLOutputConfigExport.LINE_LENGHT;
import static com.databazoo.devmodeler.conn.SQLOutputConfigExport.padCenter;
import static com.databazoo.tools.Dbg.THIS_SHOULD_NEVER_HAPPEN;

public class ExportImportWizard extends MigWizard {
	private static final int EXPORT_TO_XML = 10;
	private static final int EXPORT_TO_SQL = 11;
	private static final int EXPORT_TO_IMAGE = 12;
	private static final int IMPORT_FROM_XML = 20;

	public static ExportImportWizard get(){
		return new ExportImportWizard();
	}

	private static void copyFile(File sourceFile, File destFile) throws IOException {
		if(!destFile.exists()) {
			destFile.createNewFile();
		}

		FileChannel source = null;
		FileChannel destination = null;
		FileInputStream inputStream = null;
		FileOutputStream outputStream = null;

		try {
			inputStream = new FileInputStream(sourceFile);
			source = inputStream.getChannel();

			outputStream = new FileOutputStream(destFile);
			destination = outputStream.getChannel();

			destination.transferFrom(source, 0, source.size());
		}
		finally {
			if(source != null) {
				source.close();
			}
			if(destination != null) {
				destination.close();
			}
			if(inputStream != null) {
				inputStream.close();
			}
			if(outputStream != null) {
				outputStream.close();
			}
		}
	}
	private DB selectedDB = project.getCurrentDB();
	private String connType, connDefaultType;
	private Preview preview;
	private UndoableTextField textPreview;
	private UndoableTextField importPathField;
	private JCheckBox exportComments, exportSequenceStarts, exportData, exportDrop;
	private JCheckBox exportDatabases;
	private JCheckBox exportSchemata;
	private JCheckBox exportTables;
	private JCheckBox exportIndexes;
	private JCheckBox exportTriggers;
	private JCheckBox exportConstraints;
	private JCheckBox exportFunctions;
	private JCheckBox exportViews;
	private JCheckBox exportSequences;

	private ExportImportWizard(){
		super();
	}

	public void drawImportWindow(){
		JPanel optionPanel = new JPanel(new MigLayout("wrap 2, fillx", "[][fill, grow]"));
		List<DB> dbs = project.getDatabases();
		String[] dbComboOptions = new String[dbs.size()];
		int dbSelected = 0;
		for(int i=0; i<dbs.size(); i++){
			dbComboOptions[i] = dbs.get(i).getFullName();
			if(dbs.get(i).getFullName().equals(project.getCurrentDB().getFullName())){
				dbSelected = i;
			}
		}
		final IconableComboBox dbCombo = new IconableComboBox(dbComboOptions);
		dbCombo.setFocusable(false);
		dbCombo.setSelectedIndex(dbSelected);
		dbCombo.addActionListener(e -> selectedDB = project.getDatabases().get(dbCombo.getSelectedIndex()));
		optionPanel.add(new JLabel(" Database"));
		optionPanel.add(dbCombo);

		drawWindow("Import", optionPanel, Settings.getBool(Settings.L_MAXIMIZE_WIZARDS), true);
		resetContent();

		textPreview = new UndoableTextField("", true);
		textPreview.setEditable(false);
		textPreview.setBordered(true);

		JScrollPane scrl = new TextScrollPane(textPreview);
		scrl.setBorder(null);
		//scrl.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "File preview"));

		importPathField = new UndoableTextField();
		importPathField.disableFinder();
		importPathField.setBordered(true);
		importPathField.setEditable(false);
		importPathField.addMouseListener(new ImportPathListener());

		JButton btnImportPath = new JButton("...");
		btnImportPath.addActionListener(new ImportPathListener());

		addTitle("Import");
		addPanel("File", new HorizontalContainer(null, importPathField, btnImportPath), "span, grow");
		addText("Preview", SPAN);
		addPanel(scrl, "height 100%-30px, grow, span");

		setNextButton("Import", false, IMPORT_FROM_XML);
	}

	public void drawExportToImage(){
		// TODO: whole model / selected element
		// TODO: split into A4s
		// TODO: Print

		JPanel optionPanel = new JPanel(new MigLayout("wrap 2, fillx", "[][fill, grow]", "[]20px[]"));
		List<DB> dbs = project.getDatabases();
		String[] dbComboOptions = new String[dbs.size()];
		int dbSelected = 0;
		for(int i=0; i<dbs.size(); i++){
			dbComboOptions[i] = dbs.get(i).getFullName();
			if(dbs.get(i).getFullName().equals(project.getCurrentDB().getFullName())){
				dbSelected = i;
			}
		}
		final IconableComboBox dbCombo = new IconableComboBox(dbComboOptions);
		dbCombo.setFocusable(false);
		dbCombo.setSelectedIndex(dbSelected);
		dbCombo.addActionListener(e -> {
            selectedDB = project.getDatabases().get(dbCombo.getSelectedIndex());
            project.setCurrentDB(selectedDB);
            DesignGUI.get().drawProject(true);
            Schedule.reInvokeInEDT(Schedule.Named.EXPORT_IMPORT_WIZARD_DB_COMBO_LISTENER, Schedule.TYPE_DELAY, preview::repaint);
        });

		optionPanel.add(new JLabel(" Database"));
		optionPanel.add(dbCombo);

		optionPanel.add(new JLabel(" Elements"));
		optionPanel.add(getElementsPanel(true));

		drawWindow("Export to image", optionPanel, Settings.getBool(Settings.L_MAXIMIZE_WIZARDS), true);
		resetContent();

		addTitle("Export");
		//addText("Preview", SPAN);
		addPanel(preview = new Preview(), "height 100%, grow, span");

		setNextButton("Export", true, EXPORT_TO_IMAGE);
	}

	public void runExportToXML(){
		File file = FileChooser.showWithOverwrite(
			"Save export to",
			"Save",
			new File(System.getProperty("user.home"), database.getName()+".xml"),
			FileFilterFactory.getXmlFilter()
		);
		if(file != null){
			try {
				File sourceFile = null;
				String[] dbNames = project.getDatabaseNames();
				for(int i=0; i<dbNames.length; i++){
					if(dbNames[i].equals(database.getName())){
						sourceFile = new File(project.getProjectPath(), "database"+(i+1)+".xml");
						break;
					}
				}
				if(sourceFile == null){
					throw new IllegalStateException("Could not find generated XML file");
				}else{
					copyFile(sourceFile, file);
				}
			} catch (Exception ex) {
				Dbg.fixme("Export to XML failed", ex);
				String message = "File could not be saved.\n\n"
								+ "Error details:\n"
								+ ex.getLocalizedMessage();
				JOptionPane.showMessageDialog(frame, new SelectableText(message, false), ERROR_WHILE_SAVING+file.getName(), JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void runExportToImage(){
		File file = FileChooser.showWithOverwrite(
			"Save export to",
			"Save",
			new File(System.getProperty("user.home"), database.getName()+".png"),
			FileFilterFactory.getImagesFilter(false)
		);
		if(file != null){
			try {
				ImageIO.write(preview.img, file.getName().contains("png") ? "png" : "jpeg", file);
				frame.dispose();
			} catch (IOException ex) {
				Dbg.fixme("Export to image failed", ex);
				String message = "File could not be saved.\n\n"
								+ "Error details:\n"
								+ ex.getLocalizedMessage();
				JOptionPane.showMessageDialog(frame, new SelectableText(message, false), ERROR_WHILE_SAVING+file.getName(), JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void runImportFromXML(){
		project.loadDbFromXML(selectedDB, new File(importPathField.getText()), true);
		DesignGUI.get().drawProjectLater(true);
		SearchPanel.instance.updateDbTree();
		frame.dispose();
	}

	@Override
	protected void executeAction(int type) {
		if(type == CLOSE_WINDOW){
			frame.dispose();

		}else if(type == EXPORT_TO_IMAGE){
			runExportToImage();

		}else if(type == EXPORT_TO_SQL){
			runExportToSQL();

		}else if(type == IMPORT_FROM_XML){
			runImportFromXML();
		}
	}

	@Override public void valueChanged(TreeSelectionEvent tse){}
	@Override public void notifyChange (String elementName, String value) {}
	@Override public void notifyChange (String elementName, boolean value) {}
	@Override public void notifyChange (String elementName, boolean[] values) {}

	public void drawExportToSQL(){
		drawWindow("Export", new SQLOptionPanel(), Settings.getBool(Settings.L_MAXIMIZE_WIZARDS), true);
		resetContent();

		textPreview = new FormattedClickableTextField(project, new FormatterSQL());
		//textPreview.setEditable(false);
		textPreview.setBordered(true);
		((FormattedTextField)textPreview).format();

		runExportToSQLPreview();

		JScrollPane scrl = new TextScrollPane(textPreview);
		scrl.setBorder(null);
		//scrl.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "File preview"));

		addTitle("Export");
		addText("Preview", SPAN);
		addPanel(scrl, "height 100%-30px, grow, span");

		setNextButton("Export", true, EXPORT_TO_SQL);
	}

	private void runExportToSQLPreview(){
		textPreview.setText(getStructureDump(20));
		((FormattedTextField)textPreview).format();
		textPreview.setCaretPosition(0);
	}

	private void runExportToSQL(){
		File file = FileChooser.showWithOverwrite(
			"Save export to",
			"Save",
			new File(System.getProperty("user.home"), database.getName()+".sql"),
			FileFilterFactory.getSqlFilter()
		);
		if(file != null){
			try {
				try(BufferedWriter out = new BufferedWriter(new FileWriter(file))){
					out.write(getStructureDump(null));
					out.close();
				}
				frame.dispose();
			} catch (IOException ex) {
				Dbg.fixme("Export to SQL failed", ex);
				String message = "File could not be saved.\n\n"
								+ "Error details:\n"
								+ ex.getLocalizedMessage();
				JOptionPane.showMessageDialog(frame, new SelectableText(message, false), ERROR_WHILE_SAVING+file, JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private String getStructureDump(Integer limit){
		ProgressWindow progressWindow = exportData.isSelected() ?
				new ProgressWindow.Builder()
						.withTitle("Generating structure dump")
						.withParts(selectedDB.getRelationNames().length+1)
						.withParentWindow(this.frame).build() :
				null;
		try {
			StringBuilder ret = new StringBuilder();
			if (exportComments.isSelected()) {
				ret
						.append("/*").append(padCenter("", LINE_LENGHT - 4, '*')).append("*/\n")
						.append("/*").append(padCenter(" Structure dump by " + Config.APP_NAME + " ", LINE_LENGHT - 4, ' ')).append("*/\n")
						.append("/*").append(padCenter(" " + Config.DATE_TIME_FORMAT.format(new Date()) + " ", LINE_LENGHT - 4, ' ')).append("*/\n")
						.append("/*").append(padCenter("", LINE_LENGHT - 4, '*')).append("*/\n\n");
			}
			SQLOutputConfigExport.Builder builder = new SQLOutputConfigExport.Builder();
			builder
					.withComments(exportComments.isSelected())
					.withSequenceStarts(exportSequenceStarts.isSelected())
					.withData(exportData.isSelected() && Project.getCurrent().getType() != Project.TYPE_ABSTRACT)
					.withDrop(exportDrop.isSelected())
					.withOriginal(false)
					.withEmptyLines(true)
					.withSkipTriggersConstraints(true);
			if(connDefaultType != null && connType != null && !connType.equals(connDefaultType)){
				builder.withConn(Project.getConnectionByType(connType));
			}
			SQLOutputConfigExport config = builder
					.withProgressWindow(progressWindow)
					.withPreviewLimit(limit)
					.withDatabases(exportDatabases.isSelected())
					.withSchemata(exportSchemata.isSelected())
					.withTables(exportTables.isSelected())
					.withIndexes(exportTables.isSelected())
					.withTriggers(exportTriggers.isSelected())
					.withConstraints(exportConstraints.isSelected())
					.withFunctions(exportFunctions.isSelected())
					.withViews(exportViews.isSelected())
					.withSequences(exportSequences.isSelected())
					.build();
			if(progressWindow != null) {
				progressWindow.partLoaded();
			}

			// TODO: multiple DBs?
			ret.append(selectedDB.getQueryRecursive(config));

			if(progressWindow != null) {
				progressWindow.done();
			}
			return ret.toString();
		} finally {
			if(progressWindow != null) {
				progressWindow.done();
			}
		}
	}

	private JPanel getElementsPanel(boolean isForImage){
		ActionListener optionsListener = e -> Schedule.inWorker(this::runExportToSQLPreview);
		JPanel checkBoxPanel = new JPanel(new GridLayout(0,1));

		exportDatabases = new JCheckBox(" Database", false);
		exportDatabases.addActionListener(optionsListener);
		checkBoxPanel.add(exportDatabases);

		exportSchemata = new JCheckBox(" Schemata", true);
		exportSchemata.addActionListener(optionsListener);
		if(connection.isSupported(SupportedElement.SCHEMA)){
			checkBoxPanel.add(exportSchemata);
		}else{
			exportSchemata.setSelected(false);
		}

		exportTables = new JCheckBox(" Tables", true);
		exportTables.addActionListener(optionsListener);
		if(connection.isSupported(SupportedElement.RELATION)){
			checkBoxPanel.add(exportTables);
		}else{
			exportTables.setSelected(false);
		}

		exportIndexes = new JCheckBox(" Indexes", true);
		exportIndexes.addActionListener(optionsListener);
		if(connection.isSupported(SupportedElement.INDEX)){
			checkBoxPanel.add(exportIndexes);
		}else{
			exportIndexes.setSelected(false);
		}

		exportFunctions = new JCheckBox(" Functions", true);
		exportFunctions.addActionListener(optionsListener);
		if(connection.isSupported(SupportedElement.FUNCTION)){
			checkBoxPanel.add(exportFunctions);
		}else{
			exportFunctions.setSelected(false);
		}

		exportViews = new JCheckBox(" Views", true);
		exportViews.addActionListener(optionsListener);
		if(connection.isSupported(SupportedElement.VIEW)){
			checkBoxPanel.add(exportViews);
		}else{
			exportViews.setSelected(false);
		}

		exportSequences = new JCheckBox(" Sequences", true);
		exportSequences.addActionListener(optionsListener);
		if(connection.isSupported(SupportedElement.SEQUENCE)){
			checkBoxPanel.add(exportSequences);
		}else{
			exportSequences.setSelected(false);
		}

		exportTriggers = new JCheckBox(" Triggers", true);
		exportTriggers.addActionListener(optionsListener);
		if(connection.isSupported(SupportedElement.TRIGGER)){
			checkBoxPanel.add(exportTriggers);
		}else{
			exportTriggers.setSelected(false);
		}

		exportConstraints = new JCheckBox(" Foreign keys", true);
		exportConstraints.addActionListener(optionsListener);
		if(connection.isSupported(SupportedElement.FOREIGN_KEY)){
			checkBoxPanel.add(exportConstraints);
		}else{
			exportConstraints.setSelected(false);
		}


		if(isForImage){
			exportDatabases.setEnabled(false);
			exportSchemata.setEnabled(false);
			exportTables.setEnabled(false);
			exportIndexes.setEnabled(false);
			exportFunctions.setEnabled(false);
			exportViews.setEnabled(false);
			exportSequences.setEnabled(false);
			exportTriggers.setEnabled(false);
			exportConstraints.setEnabled(false);
		}

		return checkBoxPanel;
	}

	private static class Preview extends JComponent {
		private transient BufferedImage img;

		Preview(){
			setBorder(null);
		}

		@Override
		protected void paintComponent(Graphics g) {
			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

			Rectangle innerSpace = new Rectangle(4, 20, getWidth()-8, getHeight()-24);

			img = new BufferedImage(Canvas.instance.getWidth(), Canvas.instance.getHeight(), BufferedImage.TYPE_INT_RGB);
			if(DesignGUI.getInfoPanel().isVisible()){
				DesignGUI.getInfoPanel().setVisible(false);
			}
			boolean oldGridEnabled = Canvas.instance.gridEnabled;

			Navigator.instance.setVisible(false);
			Canvas.instance.gridEnabled = false;

			Canvas.instance.printAll(img.getGraphics());

			Navigator.instance.setVisible(true);
			Canvas.instance.gridEnabled = oldGridEnabled;

			int maxW = innerSpace.width;
			int maxH = innerSpace.height;
			int newW = maxW;
			int newH = (int)(maxW / (double)Canvas.instance.getWidth() * Canvas.instance.getHeight());

			if(newH > maxH){
				newH = maxH;
				newW = (int)(maxH / (double)Canvas.instance.getHeight() * Canvas.instance.getWidth());
			}

			Image scaledImage = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
			g.drawImage(scaledImage, 1+innerSpace.x + (maxW-newW)/2, 1+innerSpace.y + (maxH-newH)/2, null);
		}
	}

	private class ImportPathListener implements ActionListener, MouseListener{

		@Override
		public void actionPerformed(ActionEvent ae) {
			runAction();
		}

		@Override
		public void mouseClicked(MouseEvent me) {
			runAction();
		}

		@Override public void mousePressed(MouseEvent me) {}
		@Override public void mouseReleased(MouseEvent me) {}
		@Override public void mouseEntered(MouseEvent me) {}
		@Override public void mouseExited(MouseEvent me) {}

		private void runAction(){
			File file = FileChooser.show(
				"Select import file",
				"Open",
				new File(System.getProperty("user.home")),
				FileFilterFactory.getImportFilter(true)
			);
			if(file != null){
				importPathField.setText(file.toString());
				btnSave.setEnabled(true);
				readLinesFromFile(file);
			}
		}

		private void readLinesFromFile(File selectedFile) {
			try {
				textPreview.setText(getFirstLines(selectedFile));
				textPreview.setCaretPosition(0);
			} catch (FileNotFoundException e) {
				Dbg.notImportant(THIS_SHOULD_NEVER_HAPPEN, e);
			}
		}

		private String getFirstLines(File selectedFile) throws FileNotFoundException {
			StringBuilder out = new StringBuilder();
			int i=0;
			try (Scanner scanner = new Scanner(new FileInputStream(selectedFile))) {
                while (scanner.hasNextLine()) {
                    out.append(scanner.nextLine()).append("\n");
                    if (i++ > 50) {
                        break;
                    }
                }
            }
			return out.toString();
		}
	}

	private class SQLOptionPanel extends JPanel {
		private IconableComboBox dbCombo;

		SQLOptionPanel() {
			setLayout(new MigLayout("wrap 2, fillx", "[][fill, grow]", "[]20px[]"));

			ActionListener optionsListener = e -> Schedule.inWorker(() -> {
                selectedDB = project.getDatabases().get(dbCombo.getSelectedIndex());
                runExportToSQLPreview();
            });

			add(new JLabel(" Database"));
			add(getDbCombobox(optionsListener));

			add(new JLabel(" Dialect"));
			add(getDialectComboBox());

			add(new JLabel(" Options"));
			add(getOptionsPanel());

			add(new JLabel(" Elements"));
			add(getElementsPanel(false));

			exportComments.addActionListener(optionsListener);
			exportSequenceStarts.addActionListener(optionsListener);
			exportData.addActionListener(optionsListener);
			exportDrop.addActionListener(optionsListener);

			exportSequenceStarts.setEnabled(false);
		}

		private JPanel getOptionsPanel() {
			JPanel checkBoxPanel = new JPanel(new GridLayout(0,1));
			checkBoxPanel.add(exportComments = new JCheckBox(" Comments", true));
			checkBoxPanel.add(exportSequenceStarts = new JCheckBox(" Sequence values"));
			checkBoxPanel.add(exportData = new JCheckBox(" Data"));
			checkBoxPanel.add(exportDrop = new JCheckBox(" DROP commands"));
			return checkBoxPanel;
		}

		private IconableComboBox getDbCombobox(ActionListener optionsListener) {
			List<DB> dbs = project.getDatabases();
			String[] dbComboOptions = new String[dbs.size()];
			int dbSelected = 0;
			for(int i=0; i<dbs.size(); i++){
				dbComboOptions[i] = dbs.get(i).getFullName();
				if(dbs.get(i).getFullName().equals(project.getCurrentDB().getFullName())){
					dbSelected = i;
					break;
				}
			}

			dbCombo = new IconableComboBox(dbComboOptions);
			dbCombo.setFocusable(false);
			dbCombo.setSelectedIndex(dbSelected);
			dbCombo.addActionListener(optionsListener);

			return dbCombo;
		}

		private IconableComboBox getDialectComboBox() {
			final IconableComboBox dialectCombo = new IconableComboBox(Project.getConnectionTypeList());
			dialectCombo.setFocusable(false);
			dialectCombo.setSelectedItem(connDefaultType = Project.getCurrent().getTypeString());

			dialectCombo.addActionListener(e -> {
                connType = dialectCombo.getSelectedItem().toString();
                runExportToSQLPreview();
            });
			return dialectCombo;
		}
	}
}
