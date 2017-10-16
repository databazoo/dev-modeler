package com.databazoo.devmodeler.gui.window.datawindow;


import javax.swing.*;

import com.databazoo.components.textInput.UndoableTextField;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

abstract class DataWindowOutputMessages extends DataWindowOutputRecentQueries {

	JScrollPane outputScrollMessages;
	private UndoableTextField outputMessages;

	void prepareOutputMessages(){
		outputMessages = new UndoableTextField();
		outputScrollMessages = new JScrollPane(outputMessages);
	}

	synchronized void setMessages(final String string) {
		Schedule.inEDT(() -> {
            try {
                outputMessages.setText(string+"\n");
                outputMessages.setCaretPosition(outputMessages.getText().length());
            } catch (Exception ex){
                Dbg.info(ex);
            }
        });
	}

	String getShortenedWarnings(String warnings){
		int len = warnings.length();
		if(len > Config.WARNING_OVERFLOW_LIMIT) {
			return "-------------------\nNOTE: long output trimmed\n-------------------\n\n" + warnings.substring(len-Config.WARNING_OVERFLOW_LIMIT, len);
		} else {
			return warnings;
		}
	}
}
