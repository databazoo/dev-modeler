package com.databazoo.devmodeler.project;

import com.databazoo.tools.XMLWriter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import static com.databazoo.devmodeler.project.ProjectConstants.ROWS;
import static com.databazoo.devmodeler.project.ProjectConstants.TABLE;

/**
 * SAX handler for recent queries.
 */
class QueriesSAXHandler extends DefaultHandler {

    private ProjectIO project;
    private StringBuilder contStr;

    private String queryTable;
    private int queryResult;
    private int queryRows;

    QueriesSAXHandler(ProjectIO project) {
        this.project = project;
    }

    @Override
    public void startElement(String uri, String localName, String tagName, Attributes attributes) throws SAXException {
        contStr = new StringBuilder();
        if (tagName.equalsIgnoreCase("query") || tagName.equalsIgnoreCase("favorite")) {
            queryTable = XMLWriter.getString(attributes.getValue(TABLE));
            queryResult = XMLWriter.getInt(attributes.getValue("result"));
            queryRows = XMLWriter.getInt(attributes.getValue(ROWS));
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        contStr.append(new String(ch, start, length));
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase("query")) {
            project.recentQueries.add(new RecentQuery(
                    queryTable,
                    contStr.toString(),
                    queryResult,
                    queryRows
            ));
        } else if (qName.equalsIgnoreCase("favorite")) {
            project.favorites.add(new RecentQuery(
                    queryTable,
                    contStr.toString(),
                    queryResult,
                    queryRows
            ));
        }
    }
}
