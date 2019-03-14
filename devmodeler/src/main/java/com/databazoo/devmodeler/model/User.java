package com.databazoo.devmodeler.model;

import com.databazoo.devmodeler.conn.SupportedElement;

public class User {
    private Behavior behavior = new Behavior();

    public User(String name) {
        behavior.name = name;
    }

    public User(String name, String password) {
        behavior.name = name;
        behavior.password = password;
    }

    public User(String name, String password, String extra) {
        behavior.name = name;
        behavior.password = password;
        behavior.extra = extra;
    }

    public String getName() {
        return behavior.name;
    }

    public void setName(String name) {
        behavior.name = name;
    }

    private void drop() {
        // Not supported
    }

    public Behavior getBehavior() {
        return behavior;
    }

    public void setBehavior(IModelBehavior behavior) {
        this.behavior = (Behavior) behavior;
    }

    public class Behavior extends AbstractModelBehavior<Behavior> {

        public static final String L_NAME = "Name";
        public static final String L_PASSWORD = "Password";
        public static final String L_EXTRA = "Info";
        public static final String L_DESCR = "Comment";

        private String name;
        private String password;
        private String extra;
        private String descr = "";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getExtra() {
            return extra;
        }

        public void setExtra(String extra) {
            this.extra = extra;
        }

        public String getDescr() {
            return descr != null ? descr : "";
        }

        public void setDescr(String descr) {
            this.descr = descr;
        }

        @Override
        public Behavior prepareForEdit() {
            valuesForEdit = new Behavior();
            valuesForEdit.name = name;
            valuesForEdit.descr = descr;
            return valuesForEdit;
        }

        @Override
        public void saveEdited() {
            if (isDropped) {
                drop();
            } else {
                behavior.name = name;
                behavior.descr = descr;
                if (behavior.isNew) {
                    behavior.valuesForEdit.isNew = true;
                    behavior.isNew = false;
                    behavior.isDropped = false;
                }
            }
        }

        @Override
        public void notifyChange(String elementName, String value) {
            switch (elementName) {
                case L_NAME:
                    name = value;
                    break;
                case L_PASSWORD:
                    password = value;
                    break;
                case L_EXTRA:
                    extra = value;
                    break;
                case L_DESCR:
                    descr = value;
                    break;
            }
        }

        @Override
        public void notifyChange(String elementName, boolean value) {
        }

        @Override
        public void notifyChange(String elementName, boolean[] values) {
        }
    }
}
