
package com.databazoo.components.textInput;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CountDownLatch;

import com.databazoo.components.containers.HorizontalContainer;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.conn.ConnectionPg;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.devmodeler.project.ProjectManager;
import com.databazoo.devmodeler.tools.formatter.FormatterSQL;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class FormattedTextFieldTest {

	@Before
	public void setProjectUp() {
		Settings.init();

		ProjectManager.getInstance().setCurrentProject(ProjectManager.getInstance().createNew("test1", Project.TYPE_PG));
		Project.getCurrent().getConnections().add(new ConnectionPg("", "", "", ""));
	}

	@Test
	public void testComments() throws Exception {
		String text = "-- text 1 --r\n\n------------\n\n-- text 2 --";
		FormattedTextField input = drawInput();

		input.setText(text);
		input.format();
		Thread.sleep(150);

		String formattedText = input.getText();

		assertEquals(text, formattedText);

		text = "/* text 1 */\n\n/*------------\n\n-- text 2 --*/";

		input.setText(text);
		input.format();
		Thread.sleep(150);

		formattedText = input.getText();

		assertEquals(text, formattedText);
	}

	@Test
	public void testParentheses() throws Exception {
		String text = "SELECT * FROM (\n\t(SELECT [1])\n) AS foo";
		FormattedTextField input = drawInput();
		input.setText(text);
		input.setCaretPosition(14);
		input.formatImmediately();

		assertEquals(text, input.getText());

		text = "SELECT * FROM [\n\t(SELECT [1])\n] AS foo";
		//input = drawInput();
		input.setText(text);
		input.setCaretPosition(14);
		input.formatImmediately();

		assertEquals(text, input.getText());

		text = "SELECT * FROM [\n\t(SELECT [1])\n] AS foo";
		//input = drawInput();
		input.setText(text);
		input.setCaretPosition(28);
		input.formatImmediately();

		assertEquals(text, input.getText());
	}

	@Test
	public void testCommonText() throws Exception {
		String text = "SELECT count(*)\nFROM myschema.mytable \nWHERE new.text = 'What the heck?!' --AND text IS NOT NULL\nLIMIT 100";
		FormattedTextField input = drawInput();

		input.setText(text);
		input.format();
		Thread.sleep(150);

		String formattedText = input.getText();

		assertEquals(text, formattedText);
	}

	private FormattedTextField drawInput() throws Exception {
		FormattedTextField input = new FormattedTextField(new FormatterSQL());

		JFrame frame = new JFrame();
		frame.setSize(new Dimension(320, 240));
		frame.setLocationRelativeTo(null);
		//frame.setVisible(true);

		frame.setContentPane(new HorizontalContainer(null, input, null));

		//Thread.sleep(1000);

		return input;
	}

	@Test
	public void testConcurrent() throws Exception {
		final String textBase = "SELECT count(*)\nFROM myschema.mytable \nWHERE new.text = 'What the heck?!' --AND text IS NOT NULL\nLIMIT 100;\n";
		StringBuilder stringBuilder = new StringBuilder();
		for(int i=0; i<100; i++){
			stringBuilder.append(textBase);
		}
		final String text = stringBuilder.toString();
		final FormattedTextField input = drawInput();
		input.setText(text);

		Thread[] threads = new Thread[10];
		final CountDownLatch latch = new CountDownLatch(threads.length);
		for(int i=0; i<threads.length; i++){
			threads[i] = new Thread(() -> {
                assertEquals(text, input.getText());
                input.setText(text);
                assertEquals(text, input.getText());
                input.formatImmediately();
                assertEquals(text, input.getText());
                latch.countDown();
            });
		}
		for (Thread thread : threads) {
			thread.run();
		}
		latch.await();
		assertEquals(text, input.getText());
	}
}
