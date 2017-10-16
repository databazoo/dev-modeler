package com.databazoo.devmodeler.wizards;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;

import com.databazoo.components.GCFrame;
import com.databazoo.components.text.SelectableText;
import com.databazoo.devmodeler.conn.DBCommException;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.IConnectionQuery;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.tools.Dbg;

/**
 * Wizard that shows when a call is made to non-existent database - it offers user to create one.
 *
 * @author bobus
 */
public class CreateDbWizard extends MigWizard {
    public static CreateDbWizard get() {
        return new CreateDbWizard();
    }

    private CreateDbWizard() {
        super();
    }

    public void drawCreateDB(IConnection conn, String dbName) {
        connection = conn;
        database = project.getDatabaseByName(dbName);
        if (database == null) {
            database = new DB(project, dbName);
        }
        loadNewDBPage1();
    }

    private void loadNewDBPage1() {
        String queryText = JOptionPane.showInputDialog(
                GCFrame.getActiveWindow(),
                "Database " + database.getName() + " does not yet exist in " + connection.getFullName()
                        + "\n\nCreate it now?\n\n",
                connection.getQueryCreate(database, null));
        if (queryText != null) {
            //Timer t = new Timer("RelationWizardLongQuery");
            String oldAlias = connection.getDbAlias();
            connection.setDbAlias(null);
            try {
                final IConnectionQuery running = connection.prepare(queryText, new DB(project, connection.getDefaultDB()));
                /*t.schedule(new TimerTask(){
                	@Override
                	public void run(){
                		drawLongQueryWindow(running);
                	}
                }, 2000);*/
                running.run().close();
                //t.cancel();
                //hideLongQueryWindow();
                //saveEdited(false);
            } catch (DBCommException ex) {
                //t.cancel();
                //hideLongQueryWindow();
                Dbg.notImportant("No rights to create a DB?", ex);
                String message = "SQL could not be executed correctly. Database was not created.\n\n"
                        + "Alter SQL request manually to fix the reported problem.\n"
                        //+ "Error details:\n"
                        + connection.getCleanError(ex.getLocalizedMessage());
                JOptionPane.showMessageDialog(frame, new SelectableText(message, false), ERROR_WHILE_SAVING, JOptionPane.ERROR_MESSAGE);
            }
            connection.setDbAlias(oldAlias);
        }
    }

    @Override
    public void valueChanged(TreeSelectionEvent tse) {
    }

    @Override
    public void notifyChange(String elementName, String value) {
    }

    @Override
    public void notifyChange(String elementName, boolean value) {
    }

    @Override
    public void notifyChange(String elementName, boolean[] values) {
    }
}
