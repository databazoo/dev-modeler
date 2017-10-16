
package com.databazoo.devmodeler.project;

import com.databazoo.components.FileChooser;
import com.databazoo.components.GCFrame;
import com.databazoo.components.text.SelectableText;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.gui.InputDialog;
import com.databazoo.devmodeler.gui.OperationCancelException;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.EncryptedFile;
import com.databazoo.tools.FileFilterFactory;
import com.databazoo.tools.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Project configuration export and import processor.
 *
 * @author bobus
 */
public class ProjectExportImport {

	private static final String FILE_EXTENSION_XML = ".xml";
	private static final String FILE_EXTENSION_ENCRYPTED = ".dproj";
	private static final String PROJECT_FILE_EXTENSION = FILE_EXTENSION_ENCRYPTED;
	private static ProjectExportImport instance;

	public static synchronized ProjectExportImport getInstance() {
		if(instance == null){
			instance = new ProjectExportImport();
		}
		return instance;
	}

	private ProjectExportImport() {}

	public void runImport(){
		File file = FileChooser.show(
			"Select import file",
			"Open",
			new File(System.getProperty("user.home")),
			FileFilterFactory.getImportFilter(true)
		);
		if(file != null) {
			runImport(file);
		}
	}

	void runImport(File file){
		boolean isEncrypted = file.getName().endsWith(FILE_EXTENSION_ENCRYPTED);
		String password = null;
		if(isEncrypted){
			try {
				password = InputDialog.askPassword("Password for the project file is required", "Password for the project file: ");
			} catch (OperationCancelException e) {
				return;
			}
		}
		try {
			ProjectSAX projectSAX = new ProjectSAX();
			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			if (isEncrypted) {
				saxParser.parse(EncryptedFile.asInputStream(file, password), projectSAX);
			} else {
				saxParser.parse(file, projectSAX);
			}
			if(projectSAX.project != null) {
				projectSAX.project.checkInheritances();
				ProjectManager.getInstance().saveProjects();
				projectSAX.project.setLoaded();
				projectSAX.project.save();
			}
		} catch (EncryptedFile.EncryptedFilePasswordException ex) {
			String message = "File could not be loaded.\n\nPassword incorrect.";
			Dbg.info("Import failed: bad password\n" + ex);
			JOptionPane.showMessageDialog(GCFrame.getActiveWindow(), new SelectableText(message, false), "Error while loading "+file, JOptionPane.ERROR_MESSAGE);
			runImport();
		} catch (Exception ex) {
			String message = "File could not be loaded.\n\nError details:\n" + ex.getLocalizedMessage();
			Dbg.fixme("Import failed", ex);
			JOptionPane.showMessageDialog(GCFrame.getActiveWindow(), new SelectableText(message, false), "Error while loading "+file, JOptionPane.ERROR_MESSAGE);
		}
	}

	public void runExportAllProjects(){
	}

	public void runExport(Project selectedProject){
		runExport(selectedProject, null);
	}

	void runExport(Project selectedProject, File file){
		String statusMessage = "before projects read";
		try {
			// Start document
			Document doc = XMLWriter.getNewDocument();
			Element elemRoot = doc.createElement("projects");
			doc.appendChild(elemRoot);

			// Get projects
			List<Project> projects;
			if(selectedProject == null){
				projects = ProjectManager.getInstance().getProjectList();
			}else{
				projects = new ArrayList<>();
				projects.add(selectedProject);
			}

			// Ask user for file name and type
			if(file == null) {
				file = FileChooser.showWithOverwrite(
						"Export " + (selectedProject == null ? "all projects" : selectedProject.projectName) + " to",
						"Save",
						new File(System.getProperty("user.home"), (selectedProject == null ? "all_projects" : selectedProject.projectName) + PROJECT_FILE_EXTENSION),
						FileFilterFactory.getImportFilter(false)
				);
			}
			statusMessage = "after destination file selection";

			if(file != null){
				boolean isEncrypted = file.getName().endsWith(FILE_EXTENSION_ENCRYPTED);
				String password = null;
				if(isEncrypted){
					try {
						password = InputDialog.askPassword("Password for the project file is required", "Password for the project file: ");
					} catch (OperationCancelException e) {
						return;
					}
					statusMessage = "after password protection enabled";
				}else{
					XMLWriter.setAttribute(elemRoot, "author", Settings.getStr(Settings.L_REVISION_AUTHOR));
					XMLWriter.setAttribute(elemRoot, "date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
				}

				// Dump selected projects to XML
				for(Project project : projects){
					final String onProject = "on project " + project.projectName;
					if(!project.isLoaded()){
						statusMessage = onProject + " read";
						project.load();
					}
					statusMessage = onProject + " properties write";
					Element elemProject = doc.createElement("project");
					elemRoot.appendChild(elemProject);

					XMLWriter.setAttribute(elemProject, "name", project.projectName);
					XMLWriter.setAttribute(elemProject, "type", project.getType());

					for(int i=0; i < project.getConnections().size(); i++){
						Element elemServer = doc.createElement("server");
						elemProject.appendChild(elemServer);

						IConnection c = project.getConnections().get(i);
						XMLWriter.setAttribute(elemServer, "name", c.getName()==null ? "conn"+i : c.getName());
						XMLWriter.setAttribute(elemServer, "host", c.getHost());
						XMLWriter.setAttribute(elemServer, "user", c.getUser());
						XMLWriter.setAttribute(elemServer, "type", c.getType());
						if(c.getColor() != null) {
							XMLWriter.setAttribute(elemServer, "color", c.getColor().name());
						}
						if(isEncrypted) {
							XMLWriter.setAttribute(elemServer, "pass", c.getPass());
						}
					}
					for(int i=0; i<project.getDatabases().size(); i++){
						Element elemDB = doc.createElement("database");
						elemProject.appendChild(elemDB);

						DB db = project.getDatabases().get(i);
						XMLWriter.setAttribute(elemDB, "name", db.getName());
						for(IConnection conn : project.getConnections()){
							IConnection c = project.getDedicatedConnection(db.getName(), conn.getName());
							if(c != null){
								Element elemConn = doc.createElement("connection");
								elemDB.appendChild(elemConn);

								XMLWriter.setAttribute(elemConn, "name", c.getName()==null ? "conn"+i : c.getName());
								XMLWriter.setAttribute(elemConn, "host", c.getHost());
								XMLWriter.setAttribute(elemConn, "user", c.getUser());
								XMLWriter.setAttribute(elemConn, "type", c.getType());
								XMLWriter.setAttribute(elemConn, "dbAlias", c.getDbAlias());
								XMLWriter.setAttribute(elemConn, "defaultSchema", c.getDefaultSchema());
								if(isEncrypted) {
									XMLWriter.setAttribute(elemConn, "pass", c.getPass());
								}
							}
						}

						statusMessage = onProject + " structure write";
						project.appendDbStructure(doc, elemDB, db);
					}
				}
				if (isEncrypted) {
					StreamResult res = EncryptedFile.asStreamResult(file, password);
					XMLWriter.out(doc, res);
					res.getOutputStream().close();
				} else {
					XMLWriter.out(doc, file);
				}
			}
		} catch (Exception ex) {
			Dbg.fixme("Project export failed", ex);
			String message = "File could not be saved.\n\n"
					+ "Error occured "+statusMessage+":\n"
					+ ex.getLocalizedMessage();
			JOptionPane.showMessageDialog(GCFrame.getActiveWindow(), new SelectableText(message, false), "Error while saving "+file, JOptionPane.ERROR_MESSAGE);
		}
	}
}
