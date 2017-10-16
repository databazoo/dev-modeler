
package com.databazoo.components.textInput;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;


/**
 * Observer that makes it possible to jump between text fields with TAB.
 *
 * @author bobus
 */
public class NextFieldObserver implements KeyListener, FocusListener {
	private static final Map<Object,NextFieldObserver> instances = new HashMap<>();

	/**
	 * Constructor
	 *
	 * @param parentPage parent container
	 * @return NextFieldObserver
	 */
	public static NextFieldObserver get(Object parentPage) {
		NextFieldObserver ret = instances.get(parentPage);
		if(ret == null){
			ret = new NextFieldObserver();
			instances.put(parentPage, ret);
		}
		return ret;
	}

	private final List<JComponent> fields = new ArrayList<>();
	private int selectedField;

	/**
	 * Register an observer.
	 *
	 * @param input text field, combobox, etc.
	 */
	public void registerObserver(JComponent input){
		registerObserverWoKeyListeners(input);
		input.addKeyListener(this);
		input.addFocusListener(this);
		if(input instanceof UndoableTextField){
			((UndoableTextField)input).moveKeysDisabled = true;
		}
	}

	/**
	 * Register an observer, but do not add key handlers
	 *
	 * @param input text field, combobox, etc.
	 */
	public void registerObserverWoKeyListeners(JComponent input){
		//if(input instanceof AbstractButton || input instanceof JTextComponent || input instanceof JComboBox){
		if(input.isFocusable()){
			fields.add(input);
		}else{
			throw new IllegalArgumentException(input.getClass()+" is not one of supported input field types");
		}
	}

	/**
	 * Forget all inputs on current page
	 */
	public void clear(){
		fields.clear();
	}

	/**
	 * Window is closed - forget all inputs and remove the instance from list
	 */
	public void destroy(){
		fields.clear();
		instances.remove(this);
	}

	/**
	 * Consume events
	 *
	 * @param e KeyEvent
	 */
	@Override
	public void keyTyped (KeyEvent e) {
		if(e != null && !e.isConsumed() && (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB)){
			e.consume();
		}
	}

	/**
	 * Process keys
	 *
	 * @param e KeyEvent
	 */
	@Override public void keyPressed (KeyEvent e) {
		if(e != null && !e.isConsumed() && (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB)){
			if(e.getKeyCode() == KeyEvent.VK_TAB){
				if(e.isShiftDown()){
					moveBack();
				}else{
					moveForth();
				}
			}else{
				moveForth();
			}
			e.consume();
		}
	}
	@Override public void keyReleased (KeyEvent e) {}
	@Override public void focusLost (FocusEvent e) {}
	@Override public void focusGained (FocusEvent e) {
		selectedField = fields.lastIndexOf(e.getSource());
	}

	/**
	 * Jump between fields
	 */
	private void moveBack(){
		if(selectedField==0){
			selectedField = fields.size();
		}
		fields.get(selectedField-1).requestFocusInWindow();
	}

	/**
	 * Jump between fields
	 */
	private void moveForth(){
		if(selectedField==fields.size()-1){
			selectedField = -1;
		}
		fields.get(selectedField+1).requestFocusInWindow();
	}
}
