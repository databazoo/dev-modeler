package com.databazoo.devmodeler.gui;

import com.databazoo.components.GCFrame;
import com.databazoo.tools.Schedule;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

public interface InputDialog {

    /**
     * Show a simple text input dialog.
     *
     * @param windowName window title
     * @param fieldName field description like "New value: "
     * @param defaultValue pre-filled value
     * @return user-provided value
     * @throws OperationCancelException if user pressed cancel
     */
    static String ask(String windowName, String fieldName, String defaultValue) throws OperationCancelException {
        return ask(windowName, fieldName, defaultValue, 200, "OK", "Cancel");
    }

    /**
     * Show a simple text input dialog.
     *
     * @param windowName window title
     * @param fieldName field description like "New value: "
     * @param defaultValue pre-filled value
     * @param acceptOption accept button text
     * @param cancelOption cancel button text
     * @return user-provided value
     * @throws OperationCancelException if user pressed cancel
     */
    static String ask(String windowName, String fieldName, String defaultValue, int fieldSize, String acceptOption, String cancelOption) throws OperationCancelException {
        StringBuilder sb = new StringBuilder();
        try {
            Schedule.waitInEDT(() -> {
                JTextField input = new JTextField(defaultValue);
                input.setPreferredSize(new Dimension(fieldSize, input.getPreferredSize().height));
                input.selectAll();
                if (getOption(windowName, fieldName, acceptOption, cancelOption, input) == 0) {
                    sb.append(input.getText());
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            throw new OperationCancelException("Operation interrupted", e);
        }
        if (!sb.toString().isEmpty()) {
            return sb.toString();
        } else {
            throw new OperationCancelException();
        }
    }

    /**
     * Show a password input dialog.
     *
     * @param windowName window title
     * @param fieldName field description like "New value: "
     * @return user-provided value
     * @throws OperationCancelException if user pressed cancel
     */
    static String askPassword(String windowName, String fieldName) throws OperationCancelException {
        return askPassword(windowName, fieldName, 20, "OK", "Cancel");
    }

    /**
     * Show a password input dialog.
     *
     * @param windowName window title
     * @param fieldName field description like "New value: "
     * @param fieldSize input field size
     * @param acceptOption accept button text
     * @param cancelOption cancel button text
     * @return user-provided value
     * @throws OperationCancelException if user pressed cancel
     */
    static String askPassword(String windowName, String fieldName, int fieldSize, String acceptOption, String cancelOption) throws OperationCancelException {
        JPasswordField pass = new JPasswordField(fieldSize);
        if (getOption(windowName, fieldName, acceptOption, cancelOption, pass) == 0) {
            return new String(pass.getPassword());
        } else {
            throw new OperationCancelException();
        }
    }

    static int getOption(String windowName, String fieldName, String acceptOption, String cancelOption, JTextComponent input) {
        JPanel panel = new JPanel();
        panel.add(new JLabel(fieldName));
        panel.add(input);

        if (GCFrame.SHOW_GUI) {
            Schedule.inEDT(Schedule.CLICK_DELAY, input::requestFocusInWindow);
            return JOptionPane.showOptionDialog(GCFrame.getActiveWindow(), panel, windowName,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[]{acceptOption, cancelOption}, acceptOption);
        } else {
            return 0;
        }
    }
}
