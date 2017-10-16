package com.databazoo.devmodeler.project;

import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Workspace;
import com.databazoo.tools.XMLWriter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import static com.databazoo.devmodeler.project.ProjectConstants.DATABASE;
import static com.databazoo.devmodeler.project.ProjectConstants.FUNCTION;
import static com.databazoo.devmodeler.project.ProjectConstants.NAME;
import static com.databazoo.devmodeler.project.ProjectConstants.SEQUENCE;
import static com.databazoo.devmodeler.project.ProjectConstants.TABLE;
import static com.databazoo.devmodeler.project.ProjectConstants.VIEW;

/**
 * SAX handler for workspaces.
 */
class WorkspaceSAXHandler extends DefaultHandler {

    private final ProjectIO project;
    private Workspace ws;

    WorkspaceSAXHandler(ProjectIO project) {
        this.project = project;
    }

    @Override
    public void startElement(String uri, String localName, String tagName, Attributes attributes) throws SAXException {
        if (tagName.equalsIgnoreCase("workspace")) {
            DB db = project.getDatabaseByName(XMLWriter.getString(attributes.getValue(DATABASE)));
            if (db != null) {
                ws = new Workspace(XMLWriter.getString(attributes.getValue(NAME)), db);
                project.workspaces.add(ws);
            } else {
                ws = null;
            }

        } else if (ws != null) {
            if (tagName.equalsIgnoreCase(TABLE)) {
                ws.add(ws.getDB().getRelationByFullName(XMLWriter.getString(attributes.getValue(NAME))));
            } else if (tagName.equalsIgnoreCase(FUNCTION)) {
                ws.add(ws.getDB().getFunctionByFullName(XMLWriter.getString(attributes.getValue(NAME))));
            } else if (tagName.equalsIgnoreCase(VIEW)) {
                ws.add(ws.getDB().getViewByFullName(XMLWriter.getString(attributes.getValue(NAME))));
            } else if (tagName.equalsIgnoreCase(SEQUENCE)) {
                ws.add(ws.getDB().getSequenceByFullName(XMLWriter.getString(attributes.getValue(NAME))));
            }
        }
    }
}
