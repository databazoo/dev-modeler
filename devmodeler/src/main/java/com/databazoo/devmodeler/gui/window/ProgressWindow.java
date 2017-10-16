package com.databazoo.devmodeler.gui.window;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;

import com.databazoo.components.GCFrame;
import com.databazoo.components.containers.VerticalContainer;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.Schedule;

/**
 * A modal window with a progressbar.
 *
 * @author bobus
 */
public class ProgressWindow implements Serializable {
    private static final long serialVersionUID = 1905122041950000001L;

    final JProgressBar progressbar;
    JDialog win;
    int lastPart = 0;

    private ProgressWindow(String title, int parts, Window parentWindow) {
        progressbar = new JProgressBar(0, parts);
        drawWindow(title, parentWindow);
    }

    private void drawWindow(final String title, final Window parentWindow) {
        Schedule.inEDT(() -> {
            win = new JDialog(parentWindow, title, Dialog.ModalityType.APPLICATION_MODAL);
            win.setContentPane(new VerticalContainer(
                    new JLabel("<html><br>" + title + ", please wait...<br><br></html>", JLabel.CENTER),
                    progressbar,
                    null));
            win.setSize(250, 110);
            win.setLocationRelativeTo(parentWindow);
            win.setVisible(GCFrame.SHOW_GUI);
        });
    }

    public void setProgress(int percent) {
        progressbar.setValue(percent);
    }

    public void partLoaded() {
        try {
            progressbar.setValue(++lastPart);
        } catch (Exception ex) {
            Dbg.fixme("Settings progress failed", ex);
        }
    }

    public void done() {
        Schedule.inEDT(() -> win.dispose());
    }

    public static class Builder {
        Integer parts;
        String title;
        Window parentWindow;

        public Builder withParts(Integer parts) {
            this.parts = parts;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withParentWindow(Window parentWindow) {
            this.parentWindow = parentWindow;
            return this;
        }

        public ProgressWindow build(){
            if(parts == null){
                parts = 100;
            }
            if(parentWindow == null){
                parentWindow = DesignGUI.get().frame;
            }
            return new ProgressWindow(title, parts, parentWindow);
        }
    }
}
