package com.databazoo.tools;

public class FrameCheck {
    private long currentSecond = 0;
    private long currentCount = 0;
    private long currentComponent = 0;

    public static FrameCheck instance = new FrameCheck();

    public void reportFrame() {
        long seconds = System.currentTimeMillis() / 1000;
        if (seconds == currentSecond) {
            currentCount++;
        } else {
            currentSecond = seconds;
            if (currentCount > 0) {
                Dbg.info("FPS: " + currentCount + " Component repaints: " + currentComponent);
                currentCount = 0;
                currentComponent = 0;
            }
        }
    }
    public void reportComponent() {
        currentComponent++;
    }
}
