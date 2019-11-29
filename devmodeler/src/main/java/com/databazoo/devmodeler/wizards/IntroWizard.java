
package com.databazoo.devmodeler.wizards;

import com.databazoo.components.WizardTree;
import com.databazoo.components.combo.IconableComboBox;
import com.databazoo.components.containers.HorizontalContainer;
import com.databazoo.components.icons.ModelIconRenderer;
import com.databazoo.components.textInput.TextScrollPane;
import com.databazoo.components.textInput.UndoableTextField;
import com.databazoo.devmodeler.DevModeler;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.project.ProjectManager;
import com.databazoo.devmodeler.wizards.project.ProjectWizard;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import static com.databazoo.tools.Dbg.THIS_SHOULD_NEVER_HAPPEN;

/**
 * Welcome, license and short tutorial wizard
 * @author bobus
 */
public class IntroWizard extends MigWizard {

	private static final int ACCEPT_EULA = 21;
//	protected static final int SAVE_PASSWORD = 22;

	private static final int SKIP_TUTORIAL_1 = 31;
	private static final int SKIP_TUTORIAL_2 = 32;
	private static final int SKIP_TUTORIAL_3 = 33;

	private static final String L_WELCOME		= "End User License Agreement";
//	private static final String L_PASSWORD		= "Set password";
	private static final String L_PROJECT		= "Create first project";
	static final String L_TUTORIAL				= "Quick UI reference";
	static final String L_TUTORIAL_1			= "Main menu panel";
	static final String L_TUTORIAL_2			= "Database objects";
	static final String L_TUTORIAL_3			= "Additional tools";

	public static IntroWizard get(){
		return new IntroWizard();
	}

	protected WizardTree tree;
	boolean eulaAccepted = false;
//	protected JPasswordField passwordField1;
//	protected JPasswordField passwordField2;

	private UndoableTextField eulaText;

	private JComponent createTree(){
		DefaultMutableTreeNode tutNode = new DefaultMutableTreeNode(L_TUTORIAL);
		tutNode.add(new DefaultMutableTreeNode(L_TUTORIAL_1));
		tutNode.add(new DefaultMutableTreeNode(L_TUTORIAL_2));
		tutNode.add(new DefaultMutableTreeNode(L_TUTORIAL_3));

		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		root.add(new DefaultMutableTreeNode(L_WELCOME));
		root.add(tutNode);
		root.add(new DefaultMutableTreeNode(L_PROJECT));

		tree = new WizardTree(root, 1, new ModelIconRenderer(), this);
		tree.setRootVisible(false);
		return tree;
	}

	/**
	 * Draw the Intro wizard.
	 *
	 * Must be started in EDT.
	 */
	public void draw(){
		drawWindow("Welcome to " + Config.APP_NAME_BASE, createTree(), Settings.getBool(Settings.L_MAXIMIZE_WIZARDS), false);
		frame.addWindowListener(new WindowAdapter(){
			@Override public void windowClosed(WindowEvent evt) {
				if(!eulaAccepted){
					DevModeler.exit(0);
				}else{
					DesignGUI.get().drawMainWindow();
					ProjectWizard.getInstance();
				}
			}
		});
	}

	@Override
	public void valueChanged(final TreeSelectionEvent tse) {
		Schedule.inEDT(() -> {
			if (tse.getNewLeadSelectionPath() != null) {
				switch (tse.getNewLeadSelectionPath().getLastPathComponent().toString()) {
				case L_WELCOME:
					loadWelcomePage();
					break;
				/*case L_PASSWORD:
					loadPasswordPage();
					break;*/
				case L_TUTORIAL:
					tree.setSelectionRow(2);
					break;
				case L_TUTORIAL_1:
					loadTutorialPage1();
					break;
				case L_TUTORIAL_2:
					loadTutorialPage2();
					break;
				case L_TUTORIAL_3:
					loadTutorialPage3();
					break;
				case L_PROJECT:
					loadProjectPage();
					break;
				}
			} else {
				Schedule.inEDT(Schedule.CLICK_DELAY, () -> valueChanged(tse));
			}
		});
	}

	private void setPage(String lang) {
		try {
			eulaText.setPage(getClass().getResource("/doc/eula_" +lang+".html"));
		} catch (IOException ex) {
			Dbg.notImportant(THIS_SHOULD_NEVER_HAPPEN, ex);
			try {
				eulaText.setPage(getClass().getResource("/doc/eula_cz.html"));
			} catch (IOException e) {
				Dbg.fixme(THIS_SHOULD_NEVER_HAPPEN, e);
			}
		}
	}

	private void loadWelcomePage(){
		resetContent();
		final String[] langValues = new String[]{"en", "cz"};
		final IconableComboBox langCombo = new IconableComboBox(new String[]{"English", "Čeština"});
		langCombo.addActionListener(ae -> Schedule.inEDT(() -> setPage(langValues[langCombo.getSelectedIndex()])));

		eulaText = new UndoableTextField();
		eulaText.setEditable(false);
		setPage(langValues[0]);

		addPanel(new HorizontalContainer(new JLabel("                    ", JLabel.CENTER), new JLabel("<html><h2>"+L_WELCOME+"</h2><html>", JLabel.CENTER), langCombo), "width 100%-6px!, span");
		addPanel(new TextScrollPane(eulaText), "height 100%, width 100%-6px!, span");

		setNextButton("I ACCEPT", true, ACCEPT_EULA);
	}

	/*protected void loadPasswordPage(){
		resetContent();

		addTitle(L_PASSWORD);
		addText("Here you can set up a password to prevent unauthorized access to "+Config.APP_NAME_BASE+".<br><br>"
				+ "This password will also be used to securely store login information to your databases.<br><br>"
				+ "You can leave the password empty so that you are not asked to enter it every time you start "+Config.APP_NAME_BASE+". (NOTE: insecure!)<br><br>"
				+ "&nbsp;", "width 100%-6px!, span");

		passwordField1 = new JPasswordField(20);
		passwordField1.addKeyListener(new KeyAdapter(){
			@Override public void keyTyped(KeyEvent ke) { checkPasswordsMatch(); }
		});
		addText("Password", "width 130px!");
		addPanel(passwordField1, "wrap");

		passwordField2 = new JPasswordField(20);
		passwordField2.addKeyListener(new KeyAdapter(){
			@Override public void keyTyped(KeyEvent ke) { checkPasswordsMatch(); }
		});
		addText("Repeat password", "width 130px!");
		addPanel(passwordField2, "wrap");

		Config.pwrd = "ABC";
		ProjectManager.get().resetPassword();

		setNextButton("Save password", true, SAVE_PASSWORD);
		checkPasswordsMatch();
	}*/

	private void loadTutorialPage1(){
		resetContent();
		addPanel(new JLabel(new ImageIcon(IntroWizard.class.getResource("/gfx/tutorial1.png"))), "align center, span");

		setNextButton("Got it!", true, SKIP_TUTORIAL_1);
	}

	private void loadTutorialPage2(){
		resetContent();
		addPanel(new JLabel(new ImageIcon(IntroWizard.class.getResource("/gfx/tutorial2.png"))), "align center, span");

		setNextButton("Got it!", true, SKIP_TUTORIAL_2);
	}

	private void loadTutorialPage3(){
		resetContent();
		addPanel(new JLabel(new ImageIcon(IntroWizard.class.getResource("/gfx/tutorial3.png"))), "align center, span");

		setNextButton("Got it!", true, SKIP_TUTORIAL_3);
	}

	private void loadProjectPage(){
		frame.dispose();
	}

	/*private synchronized void checkPasswordsMatch(){
		Schedule.reInvokeInEDT(Schedule.Named.INTRO_WIZARD_PASSWORDS_MATCH, UIConstants.TYPE_TIMEOUT, new TimerTask(){
			@Override public void run(){
				btnSave.setEnabled(new String(passwordField1.getPassword()).equals(new String(passwordField2.getPassword())));
				btnSave.setText(new String(passwordField1.getPassword()).isEmpty() ? "No passwords, please" : "Save password");
			}
		});
	}*/

	@Override
	protected void executeAction(int type){
		Schedule.inEDT(() -> {
			if (type == CLOSE_WINDOW) {
				frame.dispose();

			} else if (type == ACCEPT_EULA) {
				tree.setSelectionRow(1);
				eulaAccepted = true;
				Config.setPwrdToDefault();
				ProjectManager.getInstance().resetPassword();

		/*}else if(type == SAVE_PASSWORD){
			tree.setSelectionRow(2);
			Config.pwrd = new String(passwordField1.getPassword());
			ProjectManager.get().resetPassword();*/

			} else if (type == SKIP_TUTORIAL_1) {
				tree.setSelectionRow(4);

			} else if (type == SKIP_TUTORIAL_2) {
				tree.setSelectionRow(5);

			} else if (type == SKIP_TUTORIAL_3) {
				tree.setSelectionRow(6);
			}
		});
	}
	@Override public void notifyChange (String elementName, String value) {}
	@Override public void notifyChange (String elementName, boolean value) {}
	@Override public void notifyChange (String elementName, boolean[] values) {}

}
