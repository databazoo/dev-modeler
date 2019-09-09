
package com.databazoo.devmodeler.wizards.project;

import com.databazoo.devmodeler.project.ProjectManager;

import javax.swing.table.AbstractTableModel;

/**
 * A table model that automatically references currently open Project Wizard
 * @author bobus
 */
public abstract class ProjectTableModel extends AbstractTableModel {

	private ProjectWizard wizard;

	protected final ProjectWizard getWizard(){
		if(wizard==null){
			wizard = ProjectWizard.instance;
		}
		return wizard;
	}

	final void resetProjectGUI(){
		if(getWizard().listedProject != null && getWizard().listedProject.equals(ProjectManager.getInstance().getCurrentProject())){
			ProjectManager.getInstance().resetProjectGUI();
		}
	}

}
