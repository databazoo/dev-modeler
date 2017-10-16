
package integration;

import org.junit.Before;

import com.databazoo.components.UIConstants;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.window.Splash;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;
import com.databazoo.devmodeler.tools.comparator.Comparator;
import com.databazoo.devmodeler.wizards.DiffWizard;

/**
 *
 * @author bobus
 */
public abstract class IntegrationBase {
	protected final static int SLEEP_TIMEOUT = 750;
	protected DB db;

	public IntegrationBase() throws Exception {
		Splash splash = Splash.get();
		if(!splash.alreadyLoaded()){
			initGUI(splash);
		}
	}

	private void initGUI(Splash splash) throws Exception {
		Settings.init();
		Config.init();
		splash.partLoaded();
		if(!UIConstants.DEBUG) {
			throw new Exception("Application has to be in DEBUG mode for integration tests");
		}

		DesignGUI.get().drawMainWindow();
		ProjectManager.getInstance().init(false);
		splash.dispose();

		setProjectUp();
		/*for(Revision r: Project.getCurrent().revisions){
			r.drop();
		}*/
		Project.getCurrent().revisions.clear();
		Settings.put(Settings.L_REVISION_NEW_REV_NAME, false);
	}

	@Before
	public void setProjectUp() {
		System.out.println("\nProject is "+getProjectName()+"\n");
		ProjectManager.getInstance().openProject(getProjectName());
		try {
			Thread.sleep(500);
		} catch (InterruptedException ex) {}
		db = Project.getCurrDB();
	}

	protected abstract String getProjectName();

	protected void checkLastRevisionApplicable() throws InterruptedException {
		DiffWizard w = DiffWizard.get();
		w.drawRevision(Project.getCurrent().revisions.get(0));
		Thread.sleep(SLEEP_TIMEOUT);

		w.prepareRevert(db.getConnection().getName());
		Thread.sleep(SLEEP_TIMEOUT);
		w.saveToDB(db.getConnection().getName(), true);
		Thread.sleep(SLEEP_TIMEOUT);

		w.prepareApply(db.getConnection().getName());
		Thread.sleep(SLEEP_TIMEOUT);
		w.saveToDB(db.getConnection().getName(), false);

		w.close();
		Thread.sleep(SLEEP_TIMEOUT);
	}

	protected void revertLastRevision() throws InterruptedException {
		if(!Project.getCurrent().revisions.isEmpty()){
			DiffWizard w = DiffWizard.get();
			w.drawRevision(Project.getCurrent().revisions.get(0));
			Thread.sleep(SLEEP_TIMEOUT);

			w.prepareRevert(db.getConnection().getName());
			Thread.sleep(SLEEP_TIMEOUT);
			w.saveToDB(db.getConnection().getName(), true);
			Thread.sleep(SLEEP_TIMEOUT);

			w.close();
		}
		for (int i=0; i < db.getSchemas().size(); i++) {
			Schema lastSchema = db.getSchemas().get(i);
			if(lastSchema.getName().contains("crud_test_schema")){
				lastSchema.drop();
				i--;
			}
		}
		Thread.sleep(SLEEP_TIMEOUT);
	}

	protected boolean compareModelAndDB() throws InterruptedException {
		DB remoteDB = new DB(Project.getCurrent(), Project.getCurrDB().getConnection(), db.getName());
		remoteDB.load();

		Comparator comparator = Comparator.withReportOnly();
		comparator.compareDBs(db, remoteDB);
		return comparator.hadDifference() || comparator.pendingDifference();
	}
}
