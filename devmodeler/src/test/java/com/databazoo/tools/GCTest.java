package com.databazoo.tools;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GCTest {
    private int calls = 0;

    @Test
    public void gcPreProcessorTest() throws Exception {
        GC.preProcessors.clear();
        GC.addPreProcessor(() -> calls++);
        GC.invoke();
        GC.invoke();
        GC.invoke();
        Thread.sleep(1500);

        assertEquals(1, calls);
    }
}