
package com.databazoo.components.icons;

import java.awt.*;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;

/**
 * Icon renderer for projects
 *
 * @author bobus
 */
public class ProjectIconRenderer extends DefaultTreeCellRenderer implements ListCellRenderer{

	public static final Icon MyIcon = Theme.getSmallIcon(Theme.ICO_MYSQL);
	public static final Icon PgIcon = Theme.getSmallIcon(Theme.ICO_POSTGRESQL);
	public static final Icon MariaIcon = Theme.getSmallIcon(Theme.ICO_MARIADB);
	public static final Icon DefIcon = Theme.getSmallIcon(Theme.ICO_ABSTRACT);

	public static final Icon MyIconLarge = Theme.getLargeIcon(Theme.ICO_MYSQL);
	public static final Icon PgIconLarge = Theme.getLargeIcon(Theme.ICO_POSTGRESQL);
	public static final Icon MariaIconLarge = Theme.getLargeIcon(Theme.ICO_MARIADB);
	public static final Icon DefIconLarge = Theme.getLargeIcon(Theme.ICO_ABSTRACT);

	/**
	 * Get large icon for a project type.
	 *
	 * @param type project type
	 * @return icon
	 */
	public static Icon getIconForType(int type) {
		if(type == Project.TYPE_MY){
			return MyIconLarge;
		}else if(type == Project.TYPE_PG){
			return PgIconLarge;
		}else if(type == Project.TYPE_MARIA){
			return MariaIconLarge;
		}else {
			return DefIconLarge;
		}
	}

	public ProjectIconRenderer(){
		super();
	}


	/**
	 * TreeCellRenderer override
	 *
	 * @param tree The JTree we're painting.
	 * @param value The value.
	 * @param sel True if the specified node was selected.
	 * @param expanded True if the specified node was expanded.
	 * @param leaf True if the specified node has no child nodes.
	 * @param row Row number.
	 * @param hasFocus True if the specified node is the lead path.
	 * @return A component whose paint() method will render the specified value.
	 */
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		if(!leaf){
			setIcon(Theme.getSmallIcon(Theme.ICO_PROJECTS));
		}else if(value.toString().equals(ProjectManager.L_CREATE_NEW_PROJECT)){
			setIcon(Theme.getSmallIcon(Theme.ICO_CREATE_NEW));
		}else{
			int type = ProjectManager.getInstance().getProjectType(value.toString());
			if(type == Project.TYPE_MY){
				setIcon(MyIcon);
			}else if(type == Project.TYPE_PG){
				setIcon(PgIcon);
			}else if(type == Project.TYPE_MARIA){
				setIcon(MariaIcon);
			}else{
				setIcon(DefIcon);
			}
		}

		return this;
	}


	/**
	 * ListCellRenderer override
	 *
	 * @param list The JList we're painting.
	 * @param value The value returned by list.getModel().getElementAt(index).
	 * @param index The cells index.
	 * @param isSelected True if the specified cell was selected.
	 * @param cellHasFocus True if the specified cell has the focus.
	 * @return A component whose paint() method will render the specified value.
	 */
	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus){
		setText(value.toString());
		/*if(!leaf){
			setIcon(Theme.getSmallIcon(Theme.ICO_FOLDER));
		}else*/ if(value.toString().equals(ProjectManager.L_CREATE_NEW_PROJECT)){
			setIcon(Theme.getSmallIcon(Theme.ICO_CREATE_NEW));
		}else{
			int type = ProjectManager.getInstance().getProjectType(value.toString());
			if(type == Project.TYPE_MY){
				setIcon(MyIcon);
			}else if(type == Project.TYPE_PG){
				setIcon(PgIcon);
			}else if(type == Project.TYPE_MARIA){
				setIcon(MariaIcon);
			}else{
				setIcon(DefIcon);
			}
		}
		return this;
	}
}
