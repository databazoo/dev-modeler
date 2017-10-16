
package com.databazoo.devmodeler.project;

import com.databazoo.components.GCFrame;
import com.databazoo.devmodeler.config.Config;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.conn.IConnection;
import com.databazoo.devmodeler.gui.InputDialog;
import com.databazoo.devmodeler.gui.OperationCancelException;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Function;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.databazoo.devmodeler.project.ProjectConstants.APPLIED_IN;
import static com.databazoo.devmodeler.project.ProjectConstants.APPROVED_BY;
import static com.databazoo.devmodeler.project.ProjectConstants.AUTHOR;
import static com.databazoo.devmodeler.project.ProjectConstants.CHANGE;
import static com.databazoo.devmodeler.project.ProjectConstants.CREATED;
import static com.databazoo.devmodeler.project.ProjectConstants.DATABASE;
import static com.databazoo.devmodeler.project.ProjectConstants.ELEMENT_NAME;
import static com.databazoo.devmodeler.project.ProjectConstants.ELEMENT_TYPE;
import static com.databazoo.devmodeler.project.ProjectConstants.FORWARD;
import static com.databazoo.devmodeler.project.ProjectConstants.IS_APPROVED;
import static com.databazoo.devmodeler.project.ProjectConstants.IS_ARCHIVED;
import static com.databazoo.devmodeler.project.ProjectConstants.IS_CLOSED;
import static com.databazoo.devmodeler.project.ProjectConstants.LAST_CHANGE;
import static com.databazoo.devmodeler.project.ProjectConstants.L_UID;
import static com.databazoo.devmodeler.project.ProjectConstants.NAME;
import static com.databazoo.devmodeler.project.ProjectConstants.REVERT;
import static com.databazoo.devmodeler.project.ProjectConstants.REVISION;

/**
 * A set of changes that were made to the database. Contains individual differences and provides some magic that finds current revision in the project.
 *
 * @author bobus
 */
public class Revision implements Comparable<Revision>, Serializable {
    private static final long serialVersionUID = 1905122041950000001L;

    public static final String L_NAME = "Name";
    public static final String L_ADD_MANUAL_CHANGE = "Add manual change";

    public static final Icon ICO_LOCKED = Theme.getSmallIcon(Theme.ICO_LOCKED);
    public static final Icon ICO_ARCHIVED = Theme.getSmallIcon(Theme.ICO_ARCHIVED);
    public static final Icon ICO_APPROVED = Theme.getSmallIcon(Theme.ICO_APPROVED);

    private transient Timer saveDelay;

    public final String UID;
    private String name;
    private Date created, changed;
    private Date fileLastModified = new Date();
    String author;
    public boolean isIncoming;
    public boolean isLoading = false;
    public boolean isClosed = false;
    public boolean isApproved = false;
    public boolean isArchived = false;
    final transient List<IConnection> appliedIn = new ArrayList<>();
    private final transient List<Diff> changes = new ArrayList<>();
    private String approvedBy;

    public Revision(String name, boolean isIncoming) {
        this.name = name;
        this.created = new Date();
        this.changed = new Date();
        this.author = isIncoming ? "?" : Settings.getStr(Settings.L_REVISION_AUTHOR);
        this.isIncoming = isIncoming;
        this.UID = getRandomUID();
    }

    public Revision(String name, String uid, Date date, Date lastChange, String author, boolean isIncoming) {
        this.name = name;
        this.created = date;
        this.changed = lastChange;
        if (author == null || author.isEmpty()) {
            this.author = isIncoming ? "?" : Settings.getStr(Settings.L_REVISION_AUTHOR);
        } else {
            this.author = author;
        }
        this.isIncoming = isIncoming;
        this.UID = uid != null ? uid : getRandomUID();
    }

    public void askForName() throws OperationCancelException {
        String newName = InputDialog.ask("Create new revision", "New revision name: ", getName(), 500, "Create revision", "Cancel operation");
        Dbg.toFile("Revision " + getName() + " is renamed to " + newName);
        name = newName;
    }

    Date getLastModified() {
        return fileLastModified;
    }

    private String getRandomUID() {
        return UUID.randomUUID().toString();
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return Config.DATE_FORMAT.format(created);
    }

    public String getChangeDate() {
        return Config.DATE_FORMAT.format(changed);
    }

    public String getChangeTime() {
        return Config.DATE_TIME_FORMAT.format(changed);
    }

    public Date getCreated() {
        return created;
    }

    public Date getChanged() {
        return changed;
    }

    public void setAge() {
        changed = new Date();
    }

    public String getAuthor() {
        return author;
    }

    public String getApprovedBy() {
        return approvedBy == null || !isApproved ? "" : approvedBy;
    }

    public void setName(String newName) {
        name = newName;
        save();
    }

    public void setAuthor(String newAuthor) {
        author = newAuthor;
        save();
    }

    public void setApprovedBy(String newAuthor) {
        approvedBy = newAuthor;
        save();
    }

    public List<Diff> getChanges() {
        return changes;
    }

    public int getCntChanges() {
        return changes.size();
    }

    public String getAppliedIn() {
        StringBuilder ret = new StringBuilder("Model");
        for (IConnection appliedConn : appliedIn) {
            ret.append(", ").append(appliedConn.getName());
        }
        return ret.toString();
    }

    public void applyIn(IConnection c) {
        if (c.getName().equals("Model")) {
            return;
        }
        if (!ProjectManager.TOO_EARLY_TO_WRITE_CONFIGS) {
            Dbg.toFile("Revision " + getName() + " was applied in " + c.getName());
        }
        for (IConnection appliedConn : appliedIn) {
            if (appliedConn.getName().equals(c.getName())) {
                return;
            }
        }
        appliedIn.add(c);
        save();
    }

    public void revertIn(IConnection c) {
        if (!ProjectManager.TOO_EARLY_TO_WRITE_CONFIGS) {
            Dbg.toFile("Revision " + getName() + " was reverted in " + c.getName());
        }
        for (IConnection appliedConn : appliedIn) {
            if (appliedConn.getName().equals(c.getName())) {
                appliedIn.remove(appliedConn);
                break;
            }
        }
        save();
    }

    public void setAppliedIn(String str) {
        if (!ProjectManager.TOO_EARLY_TO_WRITE_CONFIGS) {
            Dbg.toFile("Revision " + getName() + " was applied in " + str);
        }
        for (String appliedName : str.split(",\\s*")) {
            for (IConnection conn : Project.getCurrent().getConnections()) {
                if (appliedName.equals(conn.getName())) {
                    applyIn(conn);
                    break;
                }
            }
        }
        save();
    }

    public void addDifference(DB db, Date created, String elementClass, String elementFullName, String forwardSQL, String revertSQL) {
        Diff lastChange = changes.isEmpty() ? null : changes.get(changes.size() - 1);

        // Aggregate forward SQL for functions
        if (
                Settings.getBool(Settings.L_REVISION_AGGREGATE) &&

                        forwardSQL != null &&
                        !forwardSQL.isEmpty() &&

                        lastChange != null &&
                        lastChange.getDB().equals(db) &&
                        lastChange.elementClass.equals(Function.L_CLASS) &&
                        lastChange.elementClass.equals(elementClass) &&
                        lastChange.elementFullName.equals(elementFullName) &&
                        !lastChange.forwardSQL.contains("DROP FUNCTION") &&
                        !lastChange.revertSQL.contains("DROP FUNCTION") &&
                        !forwardSQL.contains("DROP FUNCTION") &&
                        !revertSQL.contains("DROP FUNCTION") &&
                        !lastChange.forwardSQL.startsWith("COMMENT ON FUNCTION") &&
                        !lastChange.revertSQL.startsWith("COMMENT ON FUNCTION") &&
                        !forwardSQL.startsWith("COMMENT ON FUNCTION") &&
                        !revertSQL.startsWith("COMMENT ON FUNCTION")
                ) {
            lastChange.forwardSQL = forwardSQL;
            lastChange.created = new Date();
        } else {
            changes.add(new Diff(db, created, elementClass, elementFullName, forwardSQL, revertSQL));
        }
        checkLastDiffAge();
        save();
    }

    /*public void addDifference(DB db, Date created, String elementClass, String elementFullName, String[] changedProperties, String[] changedValues, String forwardSQL, String revertSQL) {
        changes.add(new Diff(db, created, elementClass, elementFullName, changedProperties, changedValues, forwardSQL, revertSQL));
        save();
    }*/

    private void checkLastDiffAge() {
        Diff lastDiff = changes.get(changes.size() - 1);
        if (lastDiff.created.after(changed)) {
            changed = lastDiff.created;
        }
    }

    public DefaultMutableTreeNode getTreeView() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(this);
        for (Diff change : changes) {
            if (change.getElementClass() != null && !change.getElementClass().isEmpty()) {
                root.add(new DefaultMutableTreeNode(change));
            } else {
                root.add(new DefaultMutableTreeNode("change"));
            }
        }
        root.add(new DefaultMutableTreeNode(L_ADD_MANUAL_CHANGE));
        return root;
    }

    public final synchronized void save() {
        if (saveDelay != null) {
            saveDelay.cancel();
        }
        if (!isLoading) {
            saveDelay = new Timer("RevisionSaveDelay");
            saveDelay.schedule(new TimerTask() {
                @Override
                public void run() {
                    saveDelay = null;
                    if (!isIncoming && (!ProjectManager.TOO_EARLY_TO_WRITE_CONFIGS || allowByAsking())) {

                        Project.getCurrent().changeHappened = true;
                        Project.getCurrent().changedRevision = Revision.this;

                        Dbg.toFile("Revision " + getName() + " is to be saved");
                        try {
                            Document doc = XMLWriter.getNewDocument();
                            Element root = doc.createElement(REVISION);
                            doc.appendChild(root);

                            XMLWriter.setAttribute(root, NAME, name);
                            XMLWriter.setAttribute(root, L_UID, UID);
                            XMLWriter.setAttribute(root, CREATED, created.getTime());
                            XMLWriter.setAttribute(root, LAST_CHANGE, changed.getTime());
                            XMLWriter.setAttribute(root, AUTHOR, author);
                            XMLWriter.setAttribute(root, APPROVED_BY, approvedBy);
                            XMLWriter.setAttribute(root, APPLIED_IN, getAppliedIn());
                            XMLWriter.setAttribute(root, IS_CLOSED, isClosed);
                            XMLWriter.setAttribute(root, IS_APPROVED, isApproved);
                            XMLWriter.setAttribute(root, IS_ARCHIVED, isArchived);

                            for (Diff change : getChanges()) {
                                Element elemChange = doc.createElement(CHANGE);
                                root.appendChild(elemChange);

                                XMLWriter.setAttribute(elemChange, DATABASE, change.getDBName());
                                XMLWriter.setAttribute(elemChange, CREATED, change.getCreated());
                                XMLWriter.setAttribute(elemChange, ELEMENT_TYPE, change.getElementClass());
                                XMLWriter.setAttribute(elemChange, ELEMENT_NAME, change.getElementFullName());

                                Element elemForward = doc.createElement(FORWARD);
                                elemChange.appendChild(elemForward);
                                elemForward.appendChild(doc.createTextNode(change.getForwardSQL()));

                                Element elemRevert = doc.createElement(REVERT);
                                elemChange.appendChild(elemRevert);
                                elemRevert.appendChild(doc.createTextNode(change.getRevertSQL()));
                            }
                            File file = new File(Project.getCurrent().revPath, UID + ".xml");
                            XMLWriter.out(doc, file);
                            fileLastModified = new Date(file.lastModified());
                            Dbg.toFile("Revision " + getName() + " written OK, removing verification copy");
                        } catch (Exception ex) {
                            Dbg.fixme("Revision " + getName() + " could not be written correctly", ex);
                        }
                    } else {
                        Dbg.toFile("Revision " + getName() + " not saved!\n\tIs incoming: " + isIncoming + " Too early to write: " + ProjectManager.TOO_EARLY_TO_WRITE_CONFIGS);
                    }
                }
            }, 1000);
        }
    }

    private boolean allowByAsking() {
        long difference = new Date().getTime() - Project.getCurrent().getLastOpen().getTime();

        if (difference > ProjectManager.LOAD_COMPLETE_TIMEOUT * 1000) {
            Dbg.fixme("Revision is not incoming but is not allowed to be saved. Time after project load: " + difference);

            String message = "It is possible that revisions were not loaded from disk correctly.\n\nAre you sure you wish to write revision " + getName() + " \"as is\"?";
            Object[] options = {"Yes, write this revision now", "No, I will backup and restart"/*+Config.APP_NAME_BASE*/};
            return JOptionPane.showOptionDialog(GCFrame.getActiveWindow(), message, "Write revision now?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]) == 0;
        }

        return false;
    }

    public void drop() {
        Dbg.toFile("Revision " + getName() + " is to be removed");
        new File(Project.getCurrent().revPath, UID + ".xml").delete();

        Project.getCurrent().changeHappened = true;
        Project.getCurrent().changedRevision = Revision.this;
    }

    @Override
    public String toString() {
        return getName() + (isIncoming ? " IN" : "");
    }

    @Override
    public int compareTo(Revision t) {
        return changed.compareTo(t.changed);
    }

    public static class Diff implements Comparable<Diff> {
        private final DB database;
        private String elementClass;
        private String elementFullName;
        private String[] changedProperties;
        private String[] changedValues;
        private String forwardSQL;
        private String revertSQL;
        private Date created;
        private Revision revision;

		/* Diff(DB db, String forwardSQL, String revertSQL) {
            this.database = db;
			this.forwardSQL = forwardSQL;
			this.revertSQL = revertSQL;
			this.created = new Date();
			clearSQLs();
		}*/

        Diff(DB db, Date created, String elementClass, String elementFullName, String forwardSQL, String revertSQL) {
            this.database = db;
            this.created = created;
            this.elementClass = elementClass;
            this.elementFullName = elementFullName;
            this.forwardSQL = forwardSQL;
            this.revertSQL = revertSQL;
        }

        Diff(DB db, Date created, String elementClass, String elementFullName, String[] changedProperties, String[] changedValues, String forwardSQL, String revertSQL) {
            this.database = db;
            this.created = created;
            this.elementClass = elementClass;
            this.elementFullName = elementFullName;
            this.changedProperties = changedProperties;
            this.changedValues = changedValues;
            this.forwardSQL = forwardSQL;
            this.revertSQL = revertSQL;
        }

        public void setCreated(Long newTime) {
            created = new Date(newTime);
        }

        public Long getCreated() {
            return created.getTime();
        }

        public String getElementClass() {
            return elementClass;
        }

        public String getElementFullName() {
            return elementFullName;
        }

        public String[] getChangedProperties() {
            return changedProperties;
        }

        public String[] getChangedValues() {
            return changedValues;
        }

        public DB getDB() {
            return database;
        }

        public String getDBName() {
            if (database != null) {
                return database.getName();
            } else {
                return "";
            }
        }

        public Revision getRevision() {
            return revision;
        }

        public void setRevision(Revision revision) {
            this.revision = revision;
        }

        public String getForwardSQL() {
            return forwardSQL;
        }

        public String getRevertSQL() {
            if (revertSQL == null) {
                return "/** TODO: LOAD SQL FOR MERGE **/";
            }
            return revertSQL;
        }

        private void clearSQLs() {
            if (forwardSQL != null) {
                forwardSQL = forwardSQL.replaceAll("(?is)(^\\s+|\\s+$)", "");
            }
            if (revertSQL != null) {
                revertSQL = revertSQL.replaceAll("(?is)(^\\s+|\\s+$)", "");
            }
        }

        @Override
        public String toString() {
            if (elementFullName != null && !elementFullName.isEmpty()) {
                String dbName = "";
                if (Project.getCurrent().getDatabases().size() > 1) {
                    dbName = " (" + database.getName() + ")";
                }
                if (elementFullName.contains(".")) {
                    return elementFullName.replaceAll(".+\\.(.+)", "$1") + dbName;
                } else {
                    return elementFullName + dbName;
                }
            } else {
                return "";
            }
        }

        @Override
        public int compareTo(Diff t) {
            return created.compareTo(t.created);
        }
    }
}
