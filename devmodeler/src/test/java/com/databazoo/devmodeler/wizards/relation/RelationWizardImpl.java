package com.databazoo.devmodeler.wizards.relation;

import com.databazoo.components.GCFrameWithObservers;
import com.databazoo.devmodeler.conn.ConnectionPg;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.tools.Schedule;

import java.util.HashMap;
import java.util.Map;

class RelationWizardImpl extends RelationWizard {
    RelationWizardImpl() {
        connection = new RWTestConnection();
    }

    RelationWizardImpl(GCFrameWithObservers frame) {
        super(frame);
        Schedule.inEDT(Schedule.CLICK_DELAY, () -> connection = new RWTestConnection());
    }

    RWTestConnection getConnection() {
        return (RWTestConnection) connection;
    }

    public static class RWTestConnection extends ConnectionPg {

        public static final String AFTER_CREATE = "afterCreate";
        public static final String AFTER_ALTER = "afterAlter";
        public static final String AFTER_DROP = "afterDrop";

        private Map<String, Integer> calls = new HashMap<>();

        RWTestConnection() {
            super("", "", "", "", false);
        }

        @Override
        public void afterCreate(IModelElement elem) {
            calls.put(AFTER_CREATE, 1);
        }

        @Override
        public void afterAlter(IModelElement elem) {
            calls.put(AFTER_ALTER, 1);
        }

        @Override
        public void afterDrop(IModelElement elem) {
            calls.put(AFTER_DROP, 1);
        }

        public boolean isCalled(String methodName) {
            return calls.containsKey(methodName);
        }
    }
}
