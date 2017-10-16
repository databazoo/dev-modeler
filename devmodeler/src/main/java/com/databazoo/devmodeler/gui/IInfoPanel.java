package com.databazoo.devmodeler.gui;

import java.io.Serializable;

public interface IInfoPanel extends Serializable {
    int write(String message);

    int writeGray(String message);

    int writeGreen(String message);

    int writeRed(String message);

    int writeBlue(String message);

    void writeOK(int uid);

    void writeFailed(int uid, String failedReason);

    void clicked();

    void doubleClicked();

    void rightClicked();

    boolean isVisible();

    void setVisible(boolean b);
}
