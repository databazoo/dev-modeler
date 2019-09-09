package com.databazoo.components.textInput;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.ConnectionPg;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TextFieldFinderTest {

	@BeforeClass
	public static void hideGUI(){
		GCFrame.SHOW_GUI = false;
	}

	@Before
	public void setProjectUp() {
		Settings.init();

		ProjectManager.getInstance().setCurrentProject(ProjectManager.getInstance().createNew("test1", Project.TYPE_PG));
		Project.getCurrent().getConnections().add(new ConnectionPg("", "", "", ""));
	}

	@Test
	public void search() {
		UndoableTextField textField = new UndoableTextField("text trololo text trololo text trololo text text");
		//textField.setPreferredSize(new Dimension(800, 600));
		textField.setSize(new Dimension(800, 600));

		JScrollPane scrollPane = new TextScrollPane(textField);
		//scrollPane.setPreferredSize(new Dimension(600, 400));
		scrollPane.setSize(new Dimension(600, 400));

		textField.setSelectionStart(5);
		textField.setSelectionEnd(12);
		assertEquals("trololo", textField.getSelectedText());

		TextFieldFinder finder = TextFieldFinder.getFinder(textField);
		finder.search(true);

		assertEquals(18, textField.getSelectionStart());
		assertEquals("trololo", textField.getSelectedText());

		finder.search(true);

		assertEquals(31, textField.getSelectionStart());
		assertEquals("trololo", textField.getSelectedText());

		finder.search(true);

		assertEquals(5, textField.getSelectionStart());
		assertEquals("trololo", textField.getSelectedText());

		finder.search(false);

		assertEquals(31, textField.getSelectionStart());
		assertEquals("trololo", textField.getSelectedText());

		finder.replace();

		assertEquals("text trololo text trololo text  text text", textField.getText());

		finder.replaceAll();

		assertEquals("text  text  text  text text", textField.getText());

		finder.dispose();
	}

	@Test
	public void searchStandalone() throws Exception {
		GCFrame.SHOW_GUI = true;	// Must be true

		UndoableTextField textField = new UndoableTextField("text trololo text trololo text trololo text text");
		//textField.setPreferredSize(new Dimension(800, 600));
		textField.setSize(new Dimension(800, 600));

		GCFrame frame = new GCFrame("search standalone test");
		frame.getContentPane().add(new TextScrollPane(textField));
		frame.setSize(new Dimension(100, 80));
		frame.setLocation(0, 0);
		frame.setVisible(GCFrame.SHOW_GUI);

		textField.setSelectionStart(5);
		textField.setSelectionEnd(12);
		assertEquals("trololo", textField.getSelectedText());

		TextFieldFinder finder = TextFieldFinder.getFinder(textField);
		finder.search(true);

		assertEquals(18, textField.getSelectionStart());
		assertEquals("trololo", textField.getSelectedText());

		finder.search(true);

		assertEquals(31, textField.getSelectionStart());
		assertEquals("trololo", textField.getSelectedText());

		finder.search(true);

		assertEquals(5, textField.getSelectionStart());
		assertEquals("trololo", textField.getSelectedText());

		finder.search(false);

		assertEquals(31, textField.getSelectionStart());
		assertEquals("trololo", textField.getSelectedText());

		finder.replace();

		assertEquals("text trololo text trololo text  text text", textField.getText());

		frame.dispose();

		assertFalse(finder.isVisible());
	}

}
