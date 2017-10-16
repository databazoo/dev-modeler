package com.databazoo.devmodeler.wizards.project;

/**
 * Servers table model for simple new project page.
 *
 * @author bobus
 */
public class SimpleConnectionsTableModel extends ConnectionsTableModel {

	private final ProjectWizard wiz;

	public SimpleConnectionsTableModel(final ProjectWizard wiz) {
		setRows(1);
		this.wiz = wiz;
	}

	void checkStatus () {
		String res = getValueAt(0, 5).toString();
		if(!res.isEmpty()) {
			wiz.simpleConnectionStatusLabel.setText(res.replaceFirst("<font", "test: <font").replace('F', 'f'));
			wiz.simpleDbChooseButton.setEnabled(res.contains("OK"));
			if(res.contains("OK") && wiz.simpleDbInput.getText().isEmpty()){
				wiz.showDatabasesFromServer();
			}
		}
	}

	public void resetURL() {
		setValueAt(ConnectionsTableModel.DEFAULT_URL, 0, 2);
	}

	public String getHost() {
		return getValueAt(0, 2).toString();
	}

	public String getUser() {
		return getValueAt(0, 3).toString();
	}

	public String getPassword() {
		return getValueAt(0, 4).toString();
	}


	public void setHost(String value) {
		setValueAt(value, 0, 2);
	}
	public void setUser(String value) {
		setValueAt(value, 0, 3);
	}
	public void setPassword(String value) {
		setValueAt(value, 0, 4);
	}
}
