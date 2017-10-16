
package com.databazoo.components.icons;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;

import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.tools.Dbg;

/**
 * Icon renderer for database objects.
 *
 * @author bobus
 */
public class ModelIconRenderer extends DefaultTreeCellRenderer {

	private static final String ATTRIBUTES = "Attributes";
	private static final String CONSTRAINTS = "Constraints";
	private static final String INDEXES = "Indexes";
	private static final String TRIGGERS = "Triggers";

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
		if (value == null) {
			return this;
		}
		try {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			IIconable elem = (IIconable)node.getUserObject();
			setIcon(elem.getIcon16());

		} catch (Exception e) {
			Dbg.notImportantAtAll("Expected class cast issue", e);
			String nodeText = value.toString();
			if(nodeText != null && nodeText.equals("Add new table")){
				setIcon(Theme.getSmallIcon(Theme.ICO_CREATE_NEW));

			}else if(nodeText != null && nodeText.matches("^Add .*")){
				setIcon(Theme.getSmallIcon(Theme.ICO_CREATE_NEW));

			}else{
				TreePath path = tree.getPathForRow(row);
				if(path != null && path.getPathCount()>1){
					String v1 = path.getPathCount()>3 ? path.getPathComponent(3).toString() : "";
					String v2 = path.getPathCount()>4 ? path.getPathComponent(4).toString() : "";
					if(path.getPathComponent(1).toString().equals(ATTRIBUTES) || v1.equals(ATTRIBUTES) || v2.equals(ATTRIBUTES)){
						setIcon(Attribute.ico16);

					}else if(path.getPathComponent(1).toString().equals(CONSTRAINTS) || v1.equals(CONSTRAINTS) || v2.equals(CONSTRAINTS)){
						setIcon(Constraint.ico16);

					}else if(path.getPathComponent(1).toString().equals(INDEXES) || v1.equals(INDEXES) || v2.equals(INDEXES)){
						setIcon(Index.ico16);

					}else if(path.getPathComponent(1).toString().equals(TRIGGERS) || v1.equals(TRIGGERS) || v2.equals(TRIGGERS)){
						if(leaf && !value.toString().equals(TRIGGERS)){
							setIcon(Function.ico16);
						}else{
							setIcon(Trigger.ico16);
						}
					}
				}else{
					setIcon(DB.ico16);
				}
			}
		}
		return this;
	}
}
