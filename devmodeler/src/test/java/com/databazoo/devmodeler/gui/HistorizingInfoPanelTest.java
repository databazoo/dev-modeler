package com.databazoo.devmodeler.gui;

import com.databazoo.devmodeler.TestProjectSetup;
import org.junit.Assert;
import org.junit.Test;

public class HistorizingInfoPanelTest extends TestProjectSetup {

    @Test
    public void write() {
        HistorizingInfoPanel panel = new HistorizingInfoPanel();
        for (int i=1; i<=80; i++) {
            panel.write("text " + i);
        }
        Assert.assertEquals(80, panel.lines.size());
        Assert.assertEquals(26*14, panel.getHeight());
        Assert.assertEquals("InfoLine [text 80]", panel.lines.get(panel.lines.size()-1).toString());
    }

    @Test
    public void writeOK() {
        HistorizingInfoPanel panel = new HistorizingInfoPanel();
        for (int i=1; i<=23; i++) {
            panel.write("text " + i);
        }

        int log = panel.write("Loading...");
        panel.writeOK(log);

        for (int i=1; i<=34; i++) {
            panel.write("text " + i);
        }
        Assert.assertEquals(26*14, panel.getHeight());
        Assert.assertEquals("InfoLine [Loading... OK]", panel.lines.get(23).toString());
    }

    @Test
    public void writeFailed() {
        HistorizingInfoPanel panel = new HistorizingInfoPanel();
        for (int i=1; i<=23; i++) {
            panel.write("text " + i);
        }

        int log = panel.write("Loading...");
        panel.writeFailed(log, "Nooooooo!");

        for (int i=1; i<=34; i++) {
            panel.write("text " + i);
        }
        Assert.assertEquals(26*14, panel.getHeight());
        Assert.assertEquals("InfoLine [Loading... FAILED, Nooooooo!]", panel.lines.get(23).toString());
    }
}
