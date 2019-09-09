
package com.databazoo.devmodeler.wizards;

import com.databazoo.components.WizardTree;
import com.databazoo.components.icons.ModelIconRenderer;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.tools.formatter.FormatterSettings;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreePath;
import java.util.Map;

/**
 * Wizard for manipulating Settings
 * @author bobus
 */
public class SettingsWizard extends IntroWizard {
	private static final int SAVE_SETTING = 25;

	private String lastURL;

	public static SettingsWizard get(){
		return new SettingsWizard();
	}

	private SettingsWizard(){
		super();
		eulaAccepted = true;
		setContentScrollable(true);
		drawWindow("Settings", getTree(), Settings.getBool(Settings.L_MAXIMIZE_WIZARDS), false);
	}

	private JComponent getTree(){
		tree = new WizardTree(Settings.getTree(), 1, new ModelIconRenderer(), this);
		tree.setRootVisible(false);
		return tree;
	}

	@Override
	public void valueChanged(final TreeSelectionEvent tse) {
		Schedule.inEDT(() -> {
			if (tse.getNewLeadSelectionPath() != null) {
				if (tse.getNewLeadSelectionPath().getPathCount() == 2 || tse.getNewLeadSelectionPath().getLastPathComponent().toString()
						.equals("Keymap")) {
					loadSettingsDefaultPage();
			/*} else if(tse.getNewLeadSelectionPath().getLastPathComponent().toString().equals(Settings.L_GLOBAL_PASSWORD)) {
				loadPasswordPage();
			} else if(tse.getNewLeadSelectionPath().getLastPathComponent().toString().equals(Settings.L_GLOBAL_PLUGINS)) {
				loadPluginPage();*/
				} else {
					loadSettings(tse.getNewLeadSelectionPath().getLastPathComponent().toString(), getUrlForPath(tse.getNewLeadSelectionPath()));
				}
			} else {
				Schedule.inEDT(Schedule.CLICK_DELAY, () -> valueChanged(tse));
			}
		});
	}
	@Override public void notifyChange (String elementName, String value) {
		String key = lastURL+"."+elementName;
		Map<String, String> options = Settings.getStringOptions(key);
		if(options != null){
			value = options.get(value);
		}
		Settings.put(key, value);
		Settings.save();
	}
	@Override public void notifyChange (String elementName, boolean value) {
		Settings.put(lastURL+"."+elementName, value);
		Settings.save();
	}
	@Override public void notifyChange (String elementName, boolean[] values) {}

	private void loadSettings(String title, String url) {
		resetContent();

		lastURL = url;

		addTitle(title);

		for (Object key1 : Settings.getKeys()) {
			String key = (String) key1;
			String keyPart = key.replace(url+".", "");

			// Skip custom stuff
			if(key.startsWith("Global."+Settings.L_GLOBAL_PLUGINS+".") && !Settings.isBoolean(key)){
				Settings.remove(key);
				continue;
			}


			if(key.startsWith(url)){

				// skip keys of subdirectories
				if(key.replace(url+".", "").contains(".")){
					continue;
				}

				//addEmptyLine();
				addPanel(new JLabel(" "), "span, height 12px!");

				if(Settings.isBoolean(key)){
					addCheckbox(keyPart, Settings.getBool(key));

				}else if(Settings.getStringOptions(key) != null){
					String selectedValue = null;
					Map<String, String> options = Settings.getStringOptions(key);
					for(Map.Entry<String, String> data : options.entrySet()){
						String virtualKey = data.getKey();
						String virtualVal = data.getValue();
						if((virtualVal == null && Settings.getStr(key) == null) || (virtualVal != null && options.get(virtualKey).equals(Settings.getStr(key)))){
							selectedValue = virtualKey;
							break;
						}
					}
					addCombo(keyPart, options.keySet().toArray(new String[0]), selectedValue);

				}else if(Settings.getIntConstraints(key) != null){
					addNumberInput(keyPart, Settings.getInt(key), Settings.getIntConstraints(key));
				}else{
					addTextInput(keyPart, Settings.getStr(key), new FormatterSettings(), "");
				}

				addComment("<i>"+Settings.getDescription(key)+"</i>", SPAN);
			}
		}

		setNextButton(null, false, SAVE_SETTING);
		btnSave.setVisible(false);
	}

	private String getUrlForPath(TreePath path){
		StringBuilder ret = new StringBuilder();
		for(int i=1; i<path.getPathCount(); i++){
			ret.append(".").append(path.getPathComponent(i));
		}
		return ret.toString().substring(1);
	}

	private void loadSettingsDefaultPage(){
		resetContent();

		addTitle("Settings");
		addText("Here you can configure "+Config.APP_NAME_BASE+".", SPAN);
		addEmptyLine();
		addText("<i>Note: some changes may require restart of "+Config.APP_NAME_BASE+" to take effect.</i>", SPAN);

		setNextButton(null, false, SAVE_SETTING);
		btnSave.setVisible(false);
	}
/*
	private void loadPluginPage(){
		resetContent();
		addText("<h1>"+Settings.L_GLOBAL_PLUGINS+"</h1>", true);

		JPanel line = new JPanel(new BorderLayout(0,0));
		line.add(titleLabel, BorderLayout.WEST);
		line.add(inputPanel, BorderLayout.CENTER);
		pageContent.add(line);

		setNextButton(null, false, SAVE_SETTING);
		btnSave.setVisible(false);
	}*/

	@Override
	protected void executeAction(int type){
		if(type == CLOSE_WINDOW){
			frame.dispose();

		/*}else if(type == SAVE_PASSWORD){
			//tree.setSelectionRow(tree.getSelectionRows()[0]+1);
			tree.setSelectionRow(0);
			Config.pwrd = new String(passwordField1.getPassword());
			ProjectManager.get().resetPassword();
			ProjectManager.get().saveProjects();*/
		}
	}

}
