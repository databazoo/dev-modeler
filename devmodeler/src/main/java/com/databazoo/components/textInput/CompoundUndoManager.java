package com.databazoo.components.textInput;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


class CompoundUndoManager implements UndoableEditListener, Serializable {
	private static final long serialVersionUID = 1905122041950000001L;

	private UndoableTextField component;
	private transient List<Edit> changes = new ArrayList<>();
	private int changeIndex = 0;

	final AbstractAction undoAction = new AbstractAction("Undo") { @Override public void actionPerformed (ActionEvent e) { undo(); } };
	final AbstractAction redoAction = new AbstractAction("Redo") { @Override public void actionPerformed (ActionEvent e) { redo(); } };

	CompoundUndoManager(UndoableTextField textComponent){
		this.component = textComponent;
	}

	@Override
	public void undoableEditHappened(UndoableEditEvent e){

		if(changes.isEmpty()){
			return;
		}

		// Skip formatting changes and undo/redo operations
		if(component.undoEnabled){

			// Remove dead redo branches
			for(int i=changeIndex+1; i<changes.size(); ){
				changes.remove(i);
			}

			// Get last edit
			Edit lastEdit = changes.get(changeIndex);

			// Get current edit
			String currPresentationName = e.getEdit().getPresentationName();
			int posDiff = component.getCaretPosition() - lastEdit.caretPos;

			// Continue change
			if(currPresentationName.equals(lastEdit.presentationName) && posDiff >= -1 && posDiff <= 1){
				lastEdit.update();

			// Save change as new edit
			}else{
				changes.add(new Edit(currPresentationName));
				changeIndex++;
			}
		}
	}

	void updateOriginalText(){
		changes.add(new Edit());
	}

	private void undo(){
		if(changeIndex > 0){
			changeIndex--;
			updateText();
		}
	}

	private void redo(){
		if(changeIndex < changes.size()-1){
			changeIndex++;
			updateText();
		}
	}

	private void updateText(){
		Edit lastEdit = changes.get(changeIndex);

		component.undoEnabled = false;
		component.setText(lastEdit.text);
		component.setCaretPosition(lastEdit.caretPos);
		component.undoEnabled = true;
	}

	public final void clear(){
		changes.clear();
	}

	public final void destroy(){
		changes.clear();
		component = null;
	}

	private class Edit {
		private final String presentationName;

		private String text;
		private int caretPos;

		private Edit(){
			this("");
		}

		private Edit (String currPresentationName) {
			update();
			presentationName = currPresentationName;
		}

		private void update(){
			text = component.getText();
			caretPos = component.getCaretPosition();
		}
	}
}
