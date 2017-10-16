package com.databazoo.devmodeler.tools.optimizer;

import com.databazoo.devmodeler.TestProjectSetup;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StaticFlawOptimizerTest extends TestProjectSetup {

    @Test
    public void analyze() throws Exception {
        attribute2.getBehavior().setName("some_id");
        attribute2.getBehavior().setAttNull(true);

        StaticFlawOptimizer optimizer = new StaticFlawOptimizer();
        optimizer.setDatabase(database);
        optimizer.analyzeReferences();
        optimizer.analyzeIndexes();
        optimizer.analyzePrimaryKeys();
        optimizer.analyzeAttributes();
        optimizer.analyzeTriggers();

        assertEquals(3, optimizer.flaws.size());
    }

}