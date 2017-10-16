
package com.databazoo.components.icons;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.devmodeler.model.Index;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Sequence;
import com.databazoo.devmodeler.model.Trigger;
import com.databazoo.devmodeler.model.View;
import com.databazoo.devmodeler.project.Revision;
import com.databazoo.devmodeler.project.Revision.Diff;
import com.databazoo.tools.Dbg;

/**
 * Icon renderer for Difference Wizard.
 *
 * @author bobus
 */
public class DiffIconRenderer extends DefaultTreeCellRenderer {
	public static final Icon ico16 = Theme.getSmallIcon(Theme.ICO_DIFFERENCE);

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
		if(value == null){
			return this;
		}
		try {
			Diff elem = (Diff) ((DefaultMutableTreeNode)value).getUserObject();
			switch (elem.getElementClass()) {
				case Relation.L_CLASS:
					setIcon(Relation.ico16);
					break;
				case Attribute.L_CLASS:
					setIcon(Attribute.ico16);
					break;
				case Constraint.L_CLASS:
					setIcon(Constraint.ico16);
					break;
				case DB.L_CLASS:
					setIcon(DB.ico16);
					break;
				case Function.L_CLASS:
					setIcon(Function.ico16);
					break;
				case Index.L_CLASS:
					setIcon(Index.ico16);
					break;
				case Schema.L_CLASS:
					setIcon(Schema.ico16);
					break;
				case Trigger.L_CLASS:
					setIcon(Trigger.ico16);
					break;
				case View.L_CLASS:
					setIcon(View.ico16);
					break;
				case Sequence.L_CLASS:
					setIcon(Sequence.ico16);
					break;
				default:
					setIcon(ico16);
					break;
			}
		} catch (ClassCastException e){
			Dbg.notImportantAtAll("Expected class cast issue", e);
			if(row == 0){
				setIcon(Theme.getSmallIcon(Theme.ICO_REVISION));
			}else if(((DefaultMutableTreeNode)value).getUserObject().equals(Revision.L_ADD_MANUAL_CHANGE)){
				setIcon(Theme.getSmallIcon(Theme.ICO_CREATE_NEW));
			}else{
				setIcon(ico16);
			}
		}
		return this;
	}
}
