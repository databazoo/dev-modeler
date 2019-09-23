
package com.databazoo.devmodeler.wizards;

import com.databazoo.components.text.SelectableText;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.ConnectionUtils;
import com.databazoo.devmodeler.conn.DBCommException;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.conn.IConnectionQuery;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A wizard extension that provides some SQL-related capabilities.
 *
 * @author bobus
 */
public abstract class SQLEnabledWizard extends MigWizard {
    protected final static String MESSAGE_EDITABLE = "SQL could not be executed correctly. Data was not saved.\n\n"
            + "You can also try:\n\n"
            + "1. Alter SQL request manually to fix the reported problem.\n"
            + "2. Only save changes in Model so that you can merge the change into database later.\n\n";
    final static String MESSAGE_UNI = "SQL could not be executed correctly. Data was not saved.\n\n"
            + "Alter SQL request to fix the reported problem.\n\n";

    private JDialog longQueryWindow;
    private transient Timer longQueryTimer;
    boolean cancelPressed;
    private JLabel timeLabel;

    public SQLEnabledWizard() {
    }

    SQLEnabledWizard(int insets, int middleGap) {
        super(insets, middleGap);
    }

    /**
     * Performs in EDT
     */
    void drawSuccessWindow(JFrame frame) {
        Schedule.inEDT(() -> JOptionPane.showMessageDialog(frame, new SelectableText("Query executed successfully", false), "Query successful", JOptionPane.INFORMATION_MESSAGE));
    }

    /**
     * Performs in EDT
     */
    protected void drawErrorWindow(JFrame frame, String title, String message, String exMessage) {
        Schedule.inEDT(() -> JOptionPane.showMessageDialog(frame, new SelectableText(message + connection.getCleanError(exMessage), false), title, JOptionPane.ERROR_MESSAGE));
    }

    protected synchronized void drawLongQueryWindow(final IConnectionQuery[] running) {
        final long startTime = System.currentTimeMillis();

        try {
            Schedule.waitInEDT(() -> {
                timeLabel = new JLabel();
                timeLabel.setPreferredSize(new Dimension(80, 16));
                JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                labelPanel.add(new JLabel("Saving changes takes long. Query already runs for"));
                labelPanel.add(timeLabel);

                JButton cancelButton = new JButton("Cancel query", Theme.getSmallIcon(Theme.ICO_STOP));
                cancelButton.addActionListener(ae -> {
                    cancelPressed = true;
                    for (IConnectionQuery running1 : running) {
                        if (running1 != null) {
                            running1.cancel();
                        }
                    }
                });
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                buttonPanel.add(cancelButton);

                JPanel contPanel = new JPanel(new BorderLayout(10, 10));
                contPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));
                contPanel.add(labelPanel, BorderLayout.CENTER);
                contPanel.add(buttonPanel, BorderLayout.SOUTH);

                longQueryWindow = new JDialog(frame, "Saving takes long", false);
                longQueryWindow.add(contPanel);
                longQueryWindow.pack();
                longQueryWindow.setLocationRelativeTo(frame);
                longQueryWindow.setVisible(true);
            });
        } catch (InvocationTargetException | InterruptedException e) {
            Dbg.fixme("GUI creation failed", e);
        }

        IConnectionQuery tmp = null;
        for (IConnectionQuery running1 : running) {
            if (running1 != null) {
                tmp = running1;
                break;
            }
        }
        final IConnectionQuery lastRunning = tmp;
        longQueryTimer = new Timer("RelationWizardLongQueryRunningTimeCheck");
        longQueryTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (lastRunning == null) {
                    timeLabel.setText(ConnectionUtils.formatTime((System.currentTimeMillis() - startTime) * .001f + Settings.getInt(Settings.L_NOTICE_LONG_TIMER)));
                } else {
                    timeLabel.setText(lastRunning.getRunningTimeString());
                }
            }
        }, 50, 53);
    }

    protected synchronized void hideLongQueryWindow() {
        if (longQueryWindow != null) {
            longQueryWindow.dispose();
        }
        if (longQueryTimer != null) {
            longQueryTimer.cancel();
        }
    }

    protected void runSingleQuery(IConnection connection, String sql, DB db, Runnable onSuccess, Runnable onError, String errorString) {
        java.util.Timer t = new Timer("LongQueryTimer");
        int write = DesignGUI.getInfoPanel().write(sql);
        try {
            IConnectionQuery running = connection.prepare(sql, db);
            running.useExecuteUpdate(true);

            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    drawLongQueryWindow(new IConnectionQuery[]{running});
                }
            }, Settings.getInt(Settings.L_NOTICE_LONG_TIMER) * 1000L);
            running.run().close();

            t.cancel();
            hideLongQueryWindow();
            DesignGUI.getInfoPanel().writeOK(write);
            if (onSuccess != null) {
                onSuccess.run();
            }
        } catch (DBCommException ex) {
            t.cancel();
            hideLongQueryWindow();
            DesignGUI.getInfoPanel().writeFailed(write, ex.getLocalizedMessage());
            if (onError != null) {
                onError.run();
            }
            Dbg.notImportant(errorString, ex);
            drawErrorWindow(frame, errorString, MESSAGE_UNI, ex.getLocalizedMessage());
        }
    }
}
