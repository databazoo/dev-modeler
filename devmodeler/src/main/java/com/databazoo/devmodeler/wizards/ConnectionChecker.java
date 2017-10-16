package com.databazoo.devmodeler.wizards;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.databazoo.components.containers.HorizontalContainer;
import com.databazoo.devmodeler.conn.IConnection;

public class ConnectionChecker {

    public static final String L_SERVER = "Server";
    public static final String L_HOST = "Host";
    public static final String L_USER = "Username";
    public static final String L_PASS = "Password";
    public static final String L_NOT_CHECKED = "Not checked";

    private final MigWizard wizard;
    private final IConnection connection;
    private Runnable beforeCheck;
    private JLabel statusLabel;
    private JButton statusCheckButton;

    public ConnectionChecker(MigWizard wizard, IConnection connection) {
        this.wizard = wizard;
        this.connection = connection;
    }

    public ConnectionChecker(MigWizard wizard, IConnection connection, Runnable beforeCheck) {
        this(wizard, connection);
        this.beforeCheck = beforeCheck;
    }

    public void addConnectionCheck() {
        addConnectionCheck(MigWizard.SPAN);
    }

    public void addConnectionCheck(String placement) {
        statusLabel = new JLabel(L_NOT_CHECKED);

        statusCheckButton = new JButton("Check now");
        statusCheckButton.setFocusable(false);
        statusCheckButton.addActionListener(new CheckAction());

        wizard.addPanel("Connection", new HorizontalContainer.Builder()
                .center(statusLabel)
                .right(statusCheckButton)
                .build(), placement);
    }

    private void setButtonEnabled(boolean enabled) {
        statusCheckButton.setEnabled(enabled);
    }

    private void setStatusText(String text) {
        statusLabel.setText(text);
    }

    public void reset() {
        setButtonEnabled(true);
        setStatusText(L_NOT_CHECKED);
    }

    private class CheckAction implements ActionListener {

        @Override public void actionPerformed(ActionEvent e) {
            if (beforeCheck != null) {
                beforeCheck.run();
            }
            setStatusText("<html><font color=blue>Checking...</font></html>");
            setButtonEnabled(false);
            connection.setAutocheckEnabled(true);
            connection.runStatusCheck(() -> {
                setStatusText(connection.getStatusHTML());
                setButtonEnabled(true);
            });
        }
    }
}
