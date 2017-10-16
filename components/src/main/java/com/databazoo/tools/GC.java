package com.databazoo.tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Garbage collection invocation with optional pre-processors.
 */
public class GC {

    static final List<Runnable> preProcessors = new ArrayList<>();

    /**
     * Add a task to be processed before garbage collection is invoked.
     *
     * This is useful for cleanup tasks that are not to be called periodically.
     *
     * Note: pre-processors are called serially, so putting too heavy tasks here may harm performance.
     *
     * @param preProcessor Runnable instance
     */
    public static void addPreProcessor(Runnable preProcessor){
        preProcessors.add(preProcessor);
    }

    /**
     * Invoke garbage collector.
     */
    public static void invoke(){
        Schedule.reInvokeInWorker(Schedule.Named.GC, 1000, () -> {
            for(Runnable preProcessor : preProcessors) {
                try {
                    preProcessor.run();
                } catch (Exception e){
                    Dbg.fixme("GC preprocessor failed", e);
                }
            }
            System.gc();
        });
    }
}
