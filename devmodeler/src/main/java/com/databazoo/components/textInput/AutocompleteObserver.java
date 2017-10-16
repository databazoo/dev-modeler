package com.databazoo.components.textInput;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.databazoo.components.GCFrameWithObservers;
import com.databazoo.tools.Schedule;

/**
 * Notifies all SQL-enabled text fields of new database objects in DBTree.
 *
 * @author bobus
 */
public class AutocompleteObserver {

	private static final Map<GCFrameWithObservers,ArrayList<UndoableTextField>> observers = new HashMap<>();

	/**
	 * Register observer
	 *
	 * @param window top-most owner of the input
	 * @param input text field
	 */
	public static void registerObserver(GCFrameWithObservers window, UndoableTextField input){
		observers.computeIfAbsent(window, k -> new ArrayList<>()).add(input);
	}

	/**
	 * Stop notifying the window.
	 *
	 * @param window top-most owner of inputs
	 */
	public static void unregister(GCFrameWithObservers window){
		ArrayList<UndoableTextField> inputs = observers.get(window);
		if(inputs != null){
			//Dbg.info("Will destroy "+inputs.size()+" input fields");
			for(UndoableTextField input : inputs){
				input.clearUndo();
			}
		}
		observers.remove(window);
	}

	/**
	 * Process the update.
	 *
	 * @param elementNames the new element list
	 */
	public static void updateAutocomplete(final String[] elementNames){
		Schedule.inWorker(() -> {
            for(ArrayList<UndoableTextField> inputs : new ArrayList<>(observers.values())){
                for(UndoableTextField input : inputs){
                    input.updateAutocomplete(elementNames);
                }
            }
        });
	}
}
