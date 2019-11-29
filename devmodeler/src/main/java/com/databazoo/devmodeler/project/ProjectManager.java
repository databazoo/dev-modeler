
package com.databazoo.devmodeler.project;

import com.databazoo.components.GCFrame;
import com.databazoo.components.UIConstants;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.IColoredConnection;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.gui.Canvas;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.Menu;
import com.databazoo.devmodeler.gui.Neighborhood;
import com.databazoo.devmodeler.gui.SearchPanel;
import com.databazoo.devmodeler.gui.view.DifferenceView;
import com.databazoo.devmodeler.gui.view.OptimizerView;
import com.databazoo.devmodeler.gui.window.ProgressWindow;
import com.databazoo.devmodeler.gui.window.Splash;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.project.impl.ProjectAbstract;
import com.databazoo.devmodeler.project.impl.ProjectMaria;
import com.databazoo.devmodeler.project.impl.ProjectMy;
import com.databazoo.devmodeler.project.impl.ProjectPg;
import com.databazoo.devmodeler.tools.organizer.OrganizerFactory;
import com.databazoo.devmodeler.wizards.project.ProjectWizard;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.EncryptedProperties;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

/**
 * Manager of all projects. Can create, store and fetch project configurations from disk.
 * @author bobus
 */
public final class ProjectManager
{
	public static final String L_CREATE_NEW_PROJECT = "Create a new project";
	public static final String L_PROJECTS = "Projects";

	private static final String CONN = "conn";
	private static final String HOST = "host";
	private static final String USER = "user";
	private static final String PASS = "pass";
	private static final String TYPE = "type";

	/**
	 * Instance - no lazy initiation
	 */
	private static final ProjectManager INSTANCE = new ProjectManager();

	/**
	 * Write protection. Some listener may trigger project list write before projects are read fully. This prevents
	 * possible destruction of the project list on disk.
	 */
	public static volatile boolean TOO_EARLY_TO_WRITE_CONFIGS	= true;

	/**
	 * Project should finish loading in 30s. It may happen that revisions will not loaded from disk correctly and
	 * write protection will remain armed. This time limit allows the user to decide if he wants to write the revision
	 * anyway.
	 */
	static final int LOAD_COMPLETE_TIMEOUT = 30;

	/**
	 * Project list properties file
	 */
	public static final String CONFIG_FILE = "config.dat";

	/**
	 * Application home folder
	 */
	static File APP_HOME_FOLDER = new File(new File(System.getProperty("user.home")), ".devmodeler");

	public static ProjectManager getInstance(){
		return INSTANCE;
	}

	public static File getSettingsDirectory(){
		return getSettingsDirectory(null);
	}

	public static File getSettingsDirectory(String filename){
		if (!APP_HOME_FOLDER.exists() && !APP_HOME_FOLDER.mkdirs()) {
			throw new IllegalStateException(APP_HOME_FOLDER.toString());
		}
		if(filename != null) {
			return new File(APP_HOME_FOLDER, filename);
		}
		return APP_HOME_FOLDER;
	}

	private final List<Project> projects = new ArrayList<>();
	private Project currProject;
	private EncryptedProperties props;

	public void init(boolean openLast){
		resetPassword();
		try {
			props.load(new FileInputStream(getSettingsDirectory(CONFIG_FILE)));
			loadProjects();
			Splash.get().partLoaded();
			if(openLast) {
				openLast();
			}else if(!UIConstants.DEBUG){
				Thread.sleep(500);
				ProjectWizard.getInstance();
				TOO_EARLY_TO_WRITE_CONFIGS = false;
			}
		} catch (FileNotFoundException e){
			Dbg.notImportantAtAll(CONFIG_FILE+" not found. Probably new installation.", e);
			TOO_EARLY_TO_WRITE_CONFIGS = false;
		} catch (Exception e){
			Dbg.fixme(CONFIG_FILE+" could not be loaded, no project management", e);
			TOO_EARLY_TO_WRITE_CONFIGS = false;
		}
		new Timer("ProjectManagerAutoSync").schedule(new TimerTask(){
			@Override public void run(){
				if(
					currProject != null &&
					currProject.getType() != Project.TYPE_ABSTRACT &&
					currProject.isPrimary(currProject.getCurrentConn()) &&
					Menu.getInstance().getSyncCheckBox().isSelected()
				){
					currProject.syncWithServer();
				}
			}
		}, Settings.getInt(Settings.L_SYNC_TIMEOUT)*1000L, Settings.getInt(Settings.L_SYNC_INTERVAL)*1000L);
		new Timer("ProjectManagerAutoVersioning").schedule(new TimerTask(){
			@Override public void run(){
				if(currProject != null){
					if(currProject.getVerCommitMode().equals(Project.COMMIT_AFTER_CHANGE)){
						currProject.runPush();
					}
					currProject.runPull();
					currProject.checkNewRevisions();
				}
			}
		}, Settings.getInt(Settings.L_SYNC_TIMEOUT)*1000L, Settings.getInt(Settings.L_SYNC_GIT_SVN_INTERVAL)*1000L);
	}

	public void resetPassword(){
		props = new EncryptedProperties(Config.getPwrd());
	}

	public Project createNew(String name, int type){
		if(!TOO_EARLY_TO_WRITE_CONFIGS){
			name = checkName(name);
		}
		Project p;
		switch(type){
			case Project.TYPE_MY: p = new ProjectMy(name); break;
			case Project.TYPE_PG: p = new ProjectPg(name); break;
			case Project.TYPE_MARIA: p = new ProjectMaria(name); break;
			default: p = new ProjectAbstract(name); break;
		}
		projects.add(p);
		return p;
	}

	public String checkName(String name){
		if(getProjectByName(name) != null){
			String newName = JOptionPane.showInputDialog(GCFrame.getActiveWindow(), "Project "+name+" already exists.\n\nPlease provide a different name.", name);
			if(newName != null && !newName.isEmpty()){
				Dbg.info("Project's new name will be "+newName);
				return newName;
			}else{
				return null;
			}
		}

		return name;
	}

	public void saveProjects(){
		if(TOO_EARLY_TO_WRITE_CONFIGS) {
			return;
		}

		props.clear();
		props.setProperty("lastOpen", (int)(new Date()).getTime());
		props.setProperty("lastProjectName", getCurrProjectName());
		for(int k=0; k<projects.size(); k++){
			Project p = projects.get(k);
			final String projectNum = "project" + k;
			props.setProperty(projectNum, p.projectName);
			props.setProperty(projectNum + TYPE, p.getType());
			props.setProperty(projectNum + "lastOpenTimestamp", p.getLastOpen().getTime());
			props.setProperty(projectNum + "projectPath", p.getProjectPath());
			props.setProperty(projectNum + "flywayPath", p.getFlywayPath());

			props.setProperty(projectNum + "revPath", p.getRevisionPath());
			props.setProperty(projectNum + "verType", p.verType);
			props.setProperty(projectNum + "verURL", p.verURL);
			props.setProperty(projectNum + "verPath", p.verPath);
			props.setProperty(projectNum + "verUser", p.verUser);
			props.setProperty(projectNum + "verPass", p.verPass);
			props.setProperty(projectNum + "verCommitMode", p.verCommitMode);
			props.setProperty(projectNum + "verExecutableGIT", p.verExecutableGIT);
			props.setProperty(projectNum + "verExecutableSVN", p.verExecutableSVN);

			props.setProperty(projectNum + "showChangeNote", p.showChangeNote);
			if(p.currentDB != null){
				props.setProperty(projectNum + "lastOpenDB", p.currentDB.getName());
			}
			for(int i=0; i < 100; i++){
				final String connNum = CONN + i;
				props.clear(projectNum + connNum);
				props.clear(projectNum + connNum + HOST);
			}
			//Dbg.info("Connections in "+p.name+": "+p.getConnections().size());
			for(int i=0; i < p.getConnections().size(); i++){
				IConnection c = p.getConnections().get(i);
				final String connNum = CONN + i;
				props.setProperty(projectNum + connNum, c.getName()==null ? connNum : c.getName());
				props.setProperty(projectNum + connNum + HOST, c.getHost());
				props.setProperty(projectNum + connNum + USER, c.getUser());
				props.setProperty(projectNum + connNum + PASS, c.getPass());
				props.setProperty(projectNum + connNum + TYPE, c.getType());
				props.setProperty(projectNum + connNum + "color", c.getColor().name());
			}
			for(int i=0; i<p.getDatabases().size(); i++){
				DB db = p.getDatabases().get(i);
				final String dbNum = "db" + i;
				props.setProperty(projectNum + dbNum, db.getName());
				int s = 0;
				for(int j=0; j < p.getConnections().size(); j++){
					IConnection c = p.getDedicatedConnection(db.getName(), p.getConnections().get(j).getName());
					if(c != null){
						final String connNum = CONN + s;
						props.setProperty(projectNum + dbNum + connNum, c.getName()==null ? connNum : c.getName());
						props.setProperty(projectNum + dbNum + connNum + HOST, c.getHost());
						props.setProperty(projectNum + dbNum + connNum + USER, c.getUser());
						props.setProperty(projectNum + dbNum + connNum + PASS, c.getPass());
						props.setProperty(projectNum + dbNum + connNum + TYPE, c.getType());
						props.setProperty(projectNum + dbNum + connNum + "dbAlias", c.getDbAlias());
						props.setProperty(projectNum + dbNum + connNum + "defaultSchema", c.getDefaultSchema());
						s++;
					}
				}
			}
		}
		try { props.store(new FileOutputStream(getSettingsDirectory(CONFIG_FILE)), Config.APP_NAME+" project list"); } catch (Exception e){
			Dbg.fixme("Could not save projects", e);
		}
		Dbg.info("Project list written to disk");
	}
	private String getCurrProjectName(){
		if(currProject != null){
			return currProject.projectName;
		}else{
			return "";
		}
	}

	private void loadProjects(){
		//Dbg.info("Loading project list");
		for(int i=0; i<1000; i++){
			final String projectNum = "project" + i;
			String projectName = props.getStr(projectNum);
			if(projectName != null){
				//Dbg.info("Load project "+projectName);
				createNew(projectName, props.getInt(projectNum + TYPE));
				Project p = projects.get(projects.size()-1);

				p.setLastOpen(props.getLong(projectNum + "lastOpenTimestamp"));
				p.projectPath		= props.getStr(projectNum + "projectPath");
				p.flywayPath 		= props.getStr(projectNum + "flywayPath");
				p.revPath			= props.getStr(projectNum + "revPath");
				p.showChangeNote	= props.getStr(projectNum + "showChangeNote") == null || props.getBool(projectNum + "showChangeNote");

				p.setVerType(props.getStr(projectNum + "verType"));
				p.setVerURL(props.getStr(projectNum + "verURL"));
				p.setVerPath(props.getStr(projectNum + "verPath"));
				p.setVerUser(props.getStr(projectNum + "verUser"));
				p.setVerPass(props.getStr(projectNum + "verPass"));
				p.setVerCommitMode(props.getStr(projectNum + "verCommitMode"));
				p.setVerExecutableGIT(props.getStr(projectNum + "verExecutableGIT"));
				p.setVerExecutableSVN(props.getStr(projectNum + "verExecutableSVN"));

				if(p.projectPath == null || p.projectPath.isEmpty()){
					Dbg.info("Project "+projectName+" has no project path");
					p.projectPath = getSettingsDirectory(p.projectName).toString();
					p.revPath = new File(getSettingsDirectory(p.projectName), "revisions").toString();
					Schedule.reInvokeInWorker(Schedule.Named.PROJECT_MANAGER_SAVE_PROJECTS, 5000, this::saveProjects);
				}else{
					//Dbg.info("Project "+projectName+" has a project path, loading databases");
					for(int j=0; j<1000; j++){
						final String dbNum = "db" + j;
						if(props.getStr(projectNum + dbNum) != null){
							if(!props.getStr(projectNum + dbNum).isEmpty()) {
								p.databases.add(new DB(p, props.getStr(projectNum + dbNum)));
							}
						}else{
							break;
						}
					}
					//Dbg.info("Loaded "+p.databases.size()+" DBs for "+p.name);

					if(props.getStr(projectNum + "lastOpenDB") != null){
						p.setCurrentDB(props.getStr(projectNum + "lastOpenDB"));
					}else if(!p.databases.isEmpty()){
						p.currentDB = p.databases.get(0);
					}
					for(int j=0; j<1000; j++){
						final String connNum = CONN + j;
						if(props.getProperty(projectNum + connNum + HOST) != null && props.getStr(projectNum + connNum) != null && !props.getStr(projectNum + connNum).isEmpty()){
							p.addConnection(
								props.getStr(projectNum + connNum),
								props.getStr(projectNum + connNum + HOST),
								props.getStr(projectNum + connNum + USER),
								props.getStr(projectNum + connNum + PASS),
								props.getInt(projectNum + connNum + TYPE),
								IColoredConnection.ConnectionColor.fromString(props.getStr(projectNum + connNum + "color"))
							);
						}else{
							break;
						}
					}
					for(int k=0; k<p.databases.size(); k++){
						final String dbNum = "db" + k;
						for(int j=0; j<1000; j++){
							final String connNum = CONN + j;
							if(props.getStr(projectNum + dbNum + connNum) != null){
								p.addDedicatedConnection(p.databases.get(k).getName(),
									props.getStr(projectNum + dbNum + connNum),
									props.getStr(projectNum + dbNum + connNum + HOST),
									props.getStr(projectNum + dbNum + connNum + USER),
									props.getStr(projectNum + dbNum + connNum + PASS),
									props.getInt(projectNum + dbNum + connNum + TYPE),
									props.getStr(projectNum + dbNum + connNum + "dbAlias"),
									props.getStr(projectNum + dbNum + connNum + "defaultSchema")
								);
							}else{
								break;
							}
						}
					}
					//Dbg.info("Project basic info loaded from XML "+p.name);
				}
			}else{
				break;
			}
		}
	}

	private void openLast(){
		Dbg.toFile();
		String lastProjectName = props.getStr("lastProjectName");
		if((lastProjectName == null || getProjectByName(lastProjectName)==null) && projects.size() > 0){
			lastProjectName = projects.get(0).projectName;
		}
		TOO_EARLY_TO_WRITE_CONFIGS = false;
		openProject(lastProjectName);
		Dbg.toFile("last project reopened");
	}

	public List<Project> getProjectList(){
		return projects;
	}

	public void setCurrentProject(Project project){
		currProject = project;
		DesignGUI.get().setTitle(project.projectName);
	}
	public Project getCurrentProject(){
		return currProject;
	}

	public void remove(Project p){
		deleteIfExists(getSettingsDirectory(p.getProjectName()+".tmp"));
		deleteIfExists(getSettingsDirectory(p.getProjectName()+".dat"));
		if(!p.getRevPath().isEmpty()) {
			deleteIfExists(new File(p.getRevPath()));
		}
		projects.remove(p);
		saveProjects();
	}

	private void deleteIfExists(File file) {
		try{
			if(file.exists()) {
				if(!file.delete()){
					throw new IllegalStateException();
				}
			}
		} catch (Exception e){
			Dbg.fixme(file.getAbsolutePath()+" could not be deleted", e);
		}
	}

	public DefaultMutableTreeNode getTreeView(){
		DefaultMutableTreeNode list = new DefaultMutableTreeNode(L_PROJECTS);
		for(Project p: projects){
			DefaultMutableTreeNode pr = new DefaultMutableTreeNode(p.projectName);
			list.add(pr);
		}
		list.add(new DefaultMutableTreeNode(L_CREATE_NEW_PROJECT));
		return list;
	}

	public int getProjectType(String projectName) {
		for(Project p: projects){
			if(p.projectName.equals(projectName)){
				return p.getType();
			}
		}
		return Project.TYPE_ABSTRACT;
	}

	public Project getProjectByName(String projectName) {
		for(Project p: projects){
			if(p.projectName.equals(projectName)){
				return p;
			}
		}
		return null;
	}

	public void openProject(String projectName){
		waitForWriteClearance();
		checkZoomBeforeProjectClose();
		closeCurrentProject();

		TOO_EARLY_TO_WRITE_CONFIGS = true;
		boolean readFromDisk = false;
		for(Project p: projects){
			if(p.projectName.equals(projectName)){
				readFromDisk = p.load();
				break;
			}
		}

		final long startTime = System.currentTimeMillis();
		Splash.get().partLoaded();
		resetProjectGUI();

		if(!readFromDisk){
			removeProtectionAfterLoad();
		}

		Dbg.info("Project displayed in "+((System.currentTimeMillis() - startTime)/1000.0)+"s");

		if(GCFrame.SHOW_GUI) {
			OptimizerView.instance.runStaticFlawsAnalysis();
		}
	}

	void removeProtectionAfterLoad(){
		TOO_EARLY_TO_WRITE_CONFIGS = false;
		saveProjects();
		Menu.getInstance().setCompareAvailable(true);
	}

	private void waitForWriteClearance(){
		for(int i=0; i<150; i++){
			if(!TOO_EARLY_TO_WRITE_CONFIGS){
				break;
			}else if(!UIConstants.DEBUG){
				try {
					Thread.sleep(100);
				} catch (Exception e) {
					Dbg.notImportant("Nothing we can do", e);
				}
			}
		}
	}

	public void checkZoomBeforeProjectClose(){
		if(currProject != null){
			if(Canvas.instance.checkZoomBeforeProjectClose()){
				saveProjects();
				if(currProject.isLoaded()){
					currProject.saveToXML();
				}
			}
		}
	}

	public int getCurrentProjectNumber(){
		if(currProject != null){
			for(int i=0; i < projects.size(); i++){
				if(projects.get(i).projectName.equals(currProject.projectName)){
					return i+1;
				}
			}
		}
		return 0;
	}

	public void openCreatedProject(){
		final Project p = getProjectList().get(getProjectList().size()-1);
		setCurrentProject(p);
		saveProjects();

		if(p.getType() != Project.TYPE_ABSTRACT){
			DesignGUI.get().drawProject(true);
			DesignGUI.getInfoPanel().write("Project created. Start loading databases from server");
			Schedule.inWorker(() -> {
                final CountDownLatch latch = new CountDownLatch(p.getDatabases().size());
                final ProgressWindow progress = new ProgressWindow.Builder().withTitle("Loading database info").withParts(p.getDatabases().size()*9).build();
                for(final DB db: p.getDatabases()){
					Schedule.inWorker(() -> {
                        db.getConnection().setProgressChecker(progress);
                        db.load();
                        db.getConnection().setProgressChecker(null);

						OrganizerFactory.getAlphabetical().organize(db);
                        if(db.getFullName().equals(p.getCurrentDB().getFullName())){
                            DesignGUI.get().drawProject(true);
                            Canvas.instance.scrollToCenter();
                        }
                        latch.countDown();
                    });
                }
                try{
                    latch.await();
                } catch(Exception e){
                    Dbg.notImportant("Nothing we can do", e);
                }

                resetProjectGUI();
                DesignGUI.getInfoPanel().write("Loading databases from server complete");
                progress.done();
                Menu.getInstance().setCompareAvailable(true);
            });
		}else{
			DesignGUI.getInfoPanel().write("Project created");
			ProjectAbstract ap = (ProjectAbstract) p;
			ap.setAbstractConnection();
			ap.addPublicSchemaToAllDBs();
			resetProjectGUI();
		}
	}

	public void resetProjectGUI(){
		if(GCFrame.SHOW_GUI){
			DesignGUI.get().drawProject(true);
			SearchPanel.instance.clearSearch();
			SearchPanel.instance.updateDbTree();
			Menu.redrawRightMenu();
			Menu.getInstance().setModelingMode(Project.getCurrent().getType() == Project.TYPE_ABSTRACT);
			Menu.getInstance().setEntityButtonsEnabled();
			Menu.getInstance().setCompareAvailable(false);
			Menu.getInstance().checkSyncCheckbox();
			Canvas.instance.scrollToCenter();
			DifferenceView.instance.updateFilters();
			DifferenceView.instance.updateRevisionTable();
			OptimizerView.instance.updateFilters();
			Neighborhood.instance.draw(null);
		}
	}

	public void closeCurrentProject(){
		if(currProject != null && currProject.changeHappened){
			if(currProject.getVerType().equals(Project.VERSIONING_MANUAL)) {
				if(currProject.showChangeNote && !UIConstants.DEBUG){
					JOptionPane.showMessageDialog(DesignGUI.get().frame, "You made some changes in project "+currProject.projectName +".\n\nDo not forget to commit them.", "Do not forget to commit", JOptionPane.WARNING_MESSAGE);
				}
			}else{
				if((Project.COMMIT_ON_CLOSE_ASK+Project.COMMIT_ON_CLOSE_SILENT).contains(currProject.getVerCommitMode())){
					final JDialog messageWin = new JDialog(DesignGUI.get().frame, "Committing, please wait", false);
					//messageWin.setDefaultCloseOperation(GCFrame.DISPOSE_ON_CLOSE);
					messageWin.setIconImages(Theme.getAllSizes(Theme.ICO_LOGO));

					JPanel contentPane = new JPanel();
					contentPane.add(new JLabel("<html>Committing changes in project "+currProject.projectName +".<br><br>Please wait...</html>"));
					contentPane.setBorder(new EmptyBorder(30, 30, 30, 30));

					messageWin.setContentPane(contentPane);
					messageWin.pack();
					messageWin.setLocationRelativeTo(GCFrame.getActiveWindow());
					messageWin.setVisible(true);
					currProject.runPush();
					messageWin.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
					Schedule.inWorker(1000, messageWin::dispose);
				}else{
					currProject.runPush();
				}
			}
			currProject.changeHappened = false;
		}
	}

	public void moveUp(Project listedProject){
		assignProjectNumbers();
		listedProject.ordNumber -= 3;
		Collections.sort(projects);
	}
	public void moveDown(Project listedProject){
		assignProjectNumbers();
		listedProject.ordNumber += 3;
		Collections.sort(projects);
	}

	private void assignProjectNumbers(){
		for(int i=0; i<projects.size(); i++){
			Project p = projects.get(i);
			p.ordNumber = i*2;
		}
	}

}
