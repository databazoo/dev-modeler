package com.databazoo.devmodeler.gui;

import com.databazoo.components.FontFactory;
import com.databazoo.components.UIConstants;
import com.databazoo.components.elements.ClickableComponent;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.wizards.InfoPanelHistoryWizard;
import com.databazoo.tools.Schedule;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.databazoo.tools.Schedule.Named.INFO_PANEL_REPAINT;

public class HistorizingInfoPanel extends ClickableComponent implements IInfoPanel {
    private static final int SHOW_LINES = 25;
    private static final int LINE_HEIGHT = 14;
    private static final int CHAR_WIDTH = 7;
    private static final int PADDING = 2 + LINE_HEIGHT / 2;

    List<InfoLine> lines = new LinkedList<>();

    public HistorizingInfoPanel() {
        setFont(FontFactory.getSans(Font.ITALIC, Settings.getInt(Settings.L_FONT_TREE_SIZE)));
        setBackground(UIConstants.Colors.getLabelBackground());
    }

    @Override
    public int write(String message) {
        return write(message, UIConstants.Colors.getLabelForeground());
    }

    @Override
    public int writeGray(String message) {
        return write(message, UIConstants.Colors.GRAY);
    }

    @Override
    public int writeGreen(String message) {
        return write(message, UIConstants.Colors.GREEN);
    }

    @Override
    public int writeRed(String message) {
        return write(message, UIConstants.Colors.RED);
    }

    @Override
    public int writeBlue(String message) {
        return write(message, UIConstants.Colors.BLUE);
    }

    private int write(String message, Color color) {
        int seq = InfoLine.getSequence();
        lines.add(new InfoLine(seq, message.replaceAll("\\s+", " "), color));
        checkSize();
        return seq;
    }

    @Override
    public void writeOK(int uid) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            InfoLine line = lines.get(i);
            if (line.id == uid) {
                line.color = UIConstants.Colors.GREEN;
                line.appendLastLine(" OK");
                line.validTill = LocalDateTime.now().plusSeconds(Config.INFO_PANEL_TIMEOUT_OK);
                break;
            }
        }
        checkSize();
    }

    @Override
    public void writeFailed(int uid, String failedReason) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            InfoLine line = lines.get(i);
            if (line.id == uid) {
                line.color = UIConstants.Colors.RED;
                line.appendLastLine(" FAILED:");
                line.messageLines.add(failedReason.replaceAll("\\s+", " "));
                line.validTill = LocalDateTime.now().plusSeconds(Config.INFO_PANEL_TIMEOUT_FAIL);
                break;
            }
        }
        checkSize();
    }

    private void checkSize() {
        InfoLine.updateTimestamp();

        int height = 0;
        int length = 0;
        for (int i = lines.size() - 1; i >= 0; i--) {
            InfoLine line = lines.get(i);
            if (line.isValid()) {
                for (int j = line.messageLines.size() - 1; j >= 0; j--) {
                    String message = line.messageLines.get(j);
                    if (message.length() > length) {
                        length = message.length();
                    }
                    height++;
                }
                if (height >= SHOW_LINES) {
                    break;
                }
            }
        }
        if (height > 0) {
            setSize(length * CHAR_WIDTH, (height + 1) * LINE_HEIGHT);
            setVisible(true);
            repaint();
        } else {
            setVisible(false);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        graphics.setColor(new Color(getBackground().getRed(), getBackground().getGreen(), getBackground().getBlue(), 230));
        graphics.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setFont(getFont());

        graphics.setColor(UIConstants.Colors.getTableBorders());
        graphics.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);

        InfoLine.updateTimestamp();

        int linesDrawn = 0;
        for (int i = lines.size() - 1; i >= 0; i--) {
            InfoLine line = lines.get(i);
            if (line.isValid()) {
                graphics.setColor(line.color);

                for (int j = line.messageLines.size() - 1; j >= 0; j--) {
                    graphics.drawString(line.messageLines.get(j), PADDING, getHeight() - linesDrawn * LINE_HEIGHT - PADDING);
                    linesDrawn++;
                }
                if (linesDrawn * LINE_HEIGHT >= getHeight() - LINE_HEIGHT) {
                    break;
                }
            }
        }
    }

    @Override
    public void clicked() {
        Schedule.inEDT(Schedule.TYPE_DELAY, () -> setVisible(false));
    }

    @Override
    public void doubleClicked() {
        InfoPanelHistoryWizard.getInstance(lines).drawHistoryPage();
    }

    @Override
    public void rightClicked() {

    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            Schedule.reInvokeInWorker(INFO_PANEL_REPAINT, Schedule.TYPE_DELAY, this::checkSize);
        } else {
            for (int i = lines.size() - 1; i >= 0 && i >= lines.size() - 50; i--) {
                InfoLine line = lines.get(i);
                line.validTill = line.created;
            }
        }
        super.setVisible(visible);
        Menu.getInstance().setHistoryButtonSelected(visible);
    }

    public static class InfoLine {
        private static int SEQUENCE = 0;

        private static synchronized int getSequence() {
            return SEQUENCE++;
        }

        private static LocalDateTime NOW = LocalDateTime.now();

        private static void updateTimestamp() {
            NOW = LocalDateTime.now();
        }

        int id;
        List<String> messageLines;
        Color color;
        LocalDateTime created = LocalDateTime.now();
        LocalDateTime validTill = LocalDateTime.now().plusSeconds(Config.INFO_PANEL_TIMEOUT_WAIT);

        public InfoLine(int id, String message, Color color) {
            this.id = id;
            this.messageLines = new ArrayList<>(Collections.singletonList(message));
            this.color = color;
        }

        public boolean isValid() {
            return validTill.isAfter(NOW);
        }

        public void appendLastLine(String text) {
            int lineNumber = messageLines.size() - 1;
            String currentText = messageLines.get(lineNumber);
            messageLines.remove(lineNumber);
            messageLines.add(currentText + text);
        }

        public List<String> getMessageLines() {
            return messageLines;
        }

        public Color getColor() {
            return color;
        }

        public LocalDateTime getCreated() {
            return created;
        }

        @Override
        public String toString() {
            return "InfoLine " + messageLines;
        }
    }
}
