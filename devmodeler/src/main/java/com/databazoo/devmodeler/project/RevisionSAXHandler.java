package com.databazoo.devmodeler.project;

import java.util.Date;

import com.databazoo.components.UIConstants;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.tools.Dbg;
import com.databazoo.tools.XMLWriter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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
 * SAX handler for revisions
 */
class RevisionSAXHandler extends DefaultHandler {

    private final ProjectIO project;
    private Revision revision;
    private StringBuilder contStr;

    private String diffDbName;
    private DB diffDB;
    private Date diffTime;
    private String diffType;
    private String diffName;
    private String diffForward;
    private String diffRevert;

    RevisionSAXHandler(ProjectIO project) {
        this.project = project;
    }

    @Override
    public void startElement(String uri, String localName, String tagName, Attributes attributes) throws SAXException {
        contStr = new StringBuilder();
        if (tagName.equalsIgnoreCase(REVISION)) {
            revision = new Revision(
                    XMLWriter.getString(attributes.getValue(NAME)),
                    XMLWriter.getString(attributes.getValue(L_UID)),
                    XMLWriter.getDate(attributes.getValue(CREATED)),
                    XMLWriter.getDate(
                            attributes.getValue(LAST_CHANGE) == null ? attributes.getValue(CREATED) : attributes.getValue(LAST_CHANGE)),
                    XMLWriter.getString(attributes.getValue(AUTHOR)),
                    false
            );
            revision.isLoading = true;
            revision.setAppliedIn(XMLWriter.getString(attributes.getValue(APPLIED_IN)));
            revision.setApprovedBy(XMLWriter.getString(attributes.getValue(APPROVED_BY)));
            revision.isClosed = XMLWriter.getBool(attributes.getValue(IS_CLOSED));
            revision.isApproved = XMLWriter.getBool(attributes.getValue(IS_APPROVED));
            revision.isArchived = XMLWriter.getBool(attributes.getValue(IS_ARCHIVED));
            project.revisions.add(revision);

        } else if (tagName.equalsIgnoreCase(CHANGE)) {
            diffDbName = XMLWriter.getString(attributes.getValue(DATABASE));
            diffDB = project.getDatabaseByName(diffDbName);
            diffTime = XMLWriter.getDate(attributes.getValue(CREATED));
            diffType = XMLWriter.getString(attributes.getValue(ELEMENT_TYPE));
            diffName = XMLWriter.getString(attributes.getValue(ELEMENT_NAME));
            diffForward = null;
            diffRevert = null;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        contStr.append(new String(ch, start, length));
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase(FORWARD)) {
            diffForward = contStr.toString();

        } else if (qName.equalsIgnoreCase(REVERT)) {
            diffRevert = contStr.toString();

        } else if (qName.equalsIgnoreCase(REVISION)) {
            revision.isLoading = false;
        }

        if (diffForward != null && diffRevert != null) {
            updateRevision();
            diffForward = null;
            diffRevert = null;
        }
    }

    private void updateRevision() {
        if (diffDB == null) {
            if (UIConstants.DEBUG) {
                diffDB = Project.getCurrDB();
                revision.addDifference(diffDB, diffTime, diffType, diffName, diffForward, diffRevert);
            } else {
                if (project.revisions.contains(revision)) {
                    reportError();
                    project.revisions.remove(revision);
                }
            }
            Dbg.info("Could not load revision " + revision.getName() + ", database " + diffDbName + " does not exist");
        } else {
            revision.addDifference(diffDB, diffTime, diffType, diffName, diffForward, diffRevert);
        }
    }

    private void reportError() {
        if (Settings.getBool(Settings.L_ERRORS_INCOMPAT_REV)) {
            DesignGUI.getInfoPanel().writeFailed(DesignGUI.getInfoPanel()
                    .write("Could not load revision " + revision.getName() + " by " + revision.getAuthor()
                            + ", database lookup "), "Database " + diffDbName + " does not exist");
        }
    }
}
