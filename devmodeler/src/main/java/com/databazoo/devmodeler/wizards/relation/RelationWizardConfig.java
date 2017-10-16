package com.databazoo.devmodeler.wizards.relation;

import java.awt.*;

/**
 * Static configuration container for {@link RelationWizard}.
 */
public class RelationWizardConfig {
    private static Point newElementLocation;

    private RelationWizardConfig() {}

    public static synchronized void setNewElementLocation(Point location){
        newElementLocation = location;
    }

    static synchronized Point consumeLocation(){
        Point loc = newElementLocation;
        setNewElementLocation(null);
        return loc;
    }

    static synchronized boolean isElementLocationSet() {
        return newElementLocation != null;
    }
}
