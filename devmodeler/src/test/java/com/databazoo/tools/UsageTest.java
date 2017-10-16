package com.databazoo.tools;

import com.databazoo.devmodeler.gui.UsageElement;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UsageTest {
    private static final Usage.Element NAVIGATOR_DRAGGED = new Usage.Element(Usage.Type.ELEMENT_DRAGGED, -1, true) {};

    @Before
    public void setUp() throws Exception {
        Usage.events.clear();
    }

    @Test
    public void log() throws Exception {
        Usage.log(UsageElement.MAIN_WINDOW_OPEN);
        Usage.log(NAVIGATOR_DRAGGED);
        Usage.log(NAVIGATOR_DRAGGED);
        Usage.log(NAVIGATOR_DRAGGED, "ok");
        Usage.log(UsageElement.MAIN_WINDOW_CLOSE, "Exit code: 0");
        assertEquals(2, Usage.events.size());
        Thread.sleep(600);
        assertEquals(3, Usage.events.size());
    }

    @Test
    public void sendReport() throws Exception {
        log();
        Usage.sendUsageReports = true;
        Usage.sendReport();
    }

}