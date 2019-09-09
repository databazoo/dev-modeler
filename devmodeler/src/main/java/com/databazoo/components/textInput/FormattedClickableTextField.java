
package com.databazoo.components.textInput;

import com.databazoo.components.AutocompletePopupMenu;
import com.databazoo.components.UIConstants;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.gui.DBTree;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.gui.view.ViewMode;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.tools.formatter.FormatterBase;
import com.databazoo.tools.Dbg;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;


/**
 * Formatted Text Field with ctrl+click handlers.
 *
 * @author bobus
 */
public class FormattedClickableTextField extends FormattedTextField {

	/**
	 * Decide if CTRL was pressed (platform dependent).
	 *
	 * @param ke InputEvent
	 * @return was CTRL pressed?
	 */
	private static boolean isControlPressed(InputEvent ke){
		if(UIConstants.isMac()){
			return (ke.getModifiersEx() & InputEvent.META_DOWN_MASK) != 0;
		}else{
			return (ke.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
		}
	}

	private final Project project;

	/**
	 * Constructor
	 *
	 * @param p Project
	 */
	public FormattedClickableTextField(Project p){
		super();
		addListeners();
		project = p;
	}

	/**
	 * Constructor
	 *
	 * @param p Project
	 * @param text initial text
	 */
	public FormattedClickableTextField(Project p, String text){
		super(text);
		addListeners();
		project = p;
	}

	/**
	 * Constructor
	 *
	 * @param p Project
	 * @param f formatter
	 */
	public FormattedClickableTextField(Project p, FormatterBase f){
		super(f);
		addListeners();
		project = p;
	}


	/**
	 * Constructor
	 *
	 * @param p Project
	 * @param text initial text
	 * @param f formatter
	 */
	public FormattedClickableTextField(Project p, String text, FormatterBase f){
		super(text, f);
		addListeners();
		project = p;
	}

	/**
	 * Add key and mouse listeners
	 */
	private void addListeners(){
		addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent ke) {
				if(formatter != null && isControlPressed(ke)){
					formatter.isControlDown = true;
					format();
				}
			}

			@Override
			public void keyReleased (KeyEvent ke) {
				if(formatter != null && formatter.isControlDown && !isControlPressed(ke)){
					formatter.isControlDown = false;
					format();
				}
			}

		});
		addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked (MouseEvent me) {
				if(isControlPressed(me))
				{
					String text = getText();
					int caretPos = viewToModel2D(me.getPoint());
					int start = caretPos;

					if(text.isEmpty() || start >= text.length()){
						return;
					}

					for(; start >= 0; start--){
						if(!Character.toString(text.charAt(start)).matches("[a-zA-Z0-9_.]+")){
							break;
						}
					}
					int end = caretPos;
					for(; end < text.length(); end++){
						if(!Character.toString(text.charAt(end)).matches("[a-zA-Z0-9_.]+")){
							break;
						}
					}
					if(start != end){
						start++;
						List<IModelElement> elems = findElements(text.substring(start, end));
						if(!elems.isEmpty()){
							if(elems.size() == 1){
								doubleClick(elems.get(0));
							}else{
								drawOptionWindow(elems, start + (end-start)/2, me.getPoint());
							}
						}
					}
				}
			}
		});
	}

	/**
	 * Draw available options if more elements were found.
	 *
	 * @param elems element list
	 * @param center center char to which the context window will be positioned
	 * @param mePoint mouse event point from MouseEvent.getPoint()
	 */
	void drawOptionWindow(List<IModelElement> elems, int center, Point mePoint){
		AutocompletePopupMenu.get().clear();
		for(IModelElement elem : elems){
			String path = elem.getFullPath();
			if(path == null){
				path = "";
			}
			JMenuItem item = new JMenuItem((path.isEmpty() ? "" : path+".")+elem.getName());
			item.setIcon(elem.getIcon16());
			item.addActionListener(new ClickThroughActionListener(elem));
			AutocompletePopupMenu.get().add(item);
		}
		Point loc;
		try {
			Rectangle2D rectangle = modelToView2D(center);
			loc = new Point((int) rectangle.getX(), (int) rectangle.getY());
		} catch (BadLocationException | NullPointerException ex) {
			Dbg.notImportantAtAll("modelToView failed", ex);
			loc = mePoint;
		}
		SwingUtilities.convertPointToScreen(loc, this);
		AutocompletePopupMenu.get().drawAboveCenter(loc.x, loc.y);
	}

	/**
	 * Open properties of a database object (model element).
	 *
	 * @param elem IModelElement
	 */
	void doubleClick(IModelElement elem) {
		ViewMode viewOld = DesignGUI.getView();
		DesignGUI.get().switchView(ViewMode.DESIGNER, false);

		elem.doubleClicked();

		DesignGUI.get().switchView(viewOld, false);

		if(formatter != null) {
			formatter.isControlDown = false;
			format();
		}
	}

	/**
	 * List matching elements.
	 *
	 * @param name element name
	 * @return matching elements list
	 */
	List<IModelElement> findElements(String name){
		String regex;
		if(name.contains(".")){
			regex = "(?is)"+Pattern.quote(name);
		}else{
			regex = "(?is)(.+\\.)?"+Pattern.quote(name);
		}
		List<IModelElement> elements = new ArrayList<>();
		for(DB db : project.getDatabases()){
			elements.addAll(DBTree.getFlatTree(new ArrayList<IModelElement>(), db.getTreeView(regex, false, false), true));
		}
		Collections.sort(elements);

		int limit = Settings.getInt(Settings.L_AUTOCOMPLETE_ELEMS);

		return elements.subList(0, limit >= elements.size() ? elements.size() : limit);
	}

	/**
	 * Action listener to handle the click-through.
	 */
	private class ClickThroughActionListener implements ActionListener {
		private final IModelElement elem;

		private ClickThroughActionListener(IModelElement elem){
			this.elem = elem;
		}

		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			doubleClick(elem);
			AutocompletePopupMenu.get().dispose();
		}
    }

}
