package com.databazoo.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.*;
import java.io.File;

import javax.xml.parsers.SAXParserFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLWriterTest {
    private File FILE = new File(new File(System.getProperty("user.dir"), "target"), "test.xml");
    @Test
    public void readWrite() throws Exception {
        Element elemRoot, elem;

        Document doc = XMLWriter.getNewDocument();
        elemRoot = doc.createElement("root");
        doc.appendChild(elemRoot);

        elem = doc.createElement("myTag");
        XMLWriter.setAttribute(elem, "name", "tag1");
        XMLWriter.setAttribute(elem, "testString", "testString");
        XMLWriter.setAttribute(elem, "testChar", 'c');
        XMLWriter.setAttribute(elem, "testInt", 5);
        XMLWriter.setAttribute(elem, "testLong", 10L);
        XMLWriter.setAttribute(elem, "testDouble", 20.05D);
        XMLWriter.setAttribute(elem, "testBoolean", true);
        XMLWriter.setAttribute(elem, "testStringArray", new String[]{"a", "b", "c"});
        XMLWriter.setAttribute(elem, "testCharArray", new char[]{'a', 'b', 'c'});
        XMLWriter.setAttribute(elem, "testIntArray", new int[]{15, 16, 17});
        XMLWriter.setAttribute(elem, "testPoint", new Point(18, 19));
        XMLWriter.setAttribute(elem, "testDimension", new Dimension(18, 19));
        elemRoot.appendChild(elem);

        elem = doc.createElement("myTag");
        XMLWriter.setAttribute(elem, "name", "tag2");
        elemRoot.appendChild(elem);

        XMLWriter.out(doc, FILE);

        TestSAXHandler handler = new TestSAXHandler();
        SAXParserFactory.newInstance().newSAXParser().parse(FILE, handler);

        assertTrue(handler.tag1Loaded);
        assertTrue(handler.tag2Loaded);
    }

    /**
     * Sax handler
     */
    private static class TestSAXHandler extends DefaultHandler {
        boolean tag1Loaded = false;
        boolean tag2Loaded = false;

        @Override
        public void startElement(String uri, String localName, String tagName, Attributes attributes) throws SAXException {
            if(tagName.equalsIgnoreCase("myTag")) {
                if(attributes.getValue("name").equals("tag1")) {
                    tag1Loaded = true;
                    assertEquals("testString", XMLWriter.getString(attributes.getValue("testString")));
                    assertEquals(new Character('c'), XMLWriter.getChar(attributes.getValue("testChar")));
                    assertEquals(5, XMLWriter.getInt(attributes.getValue("testInt")));
                    assertEquals(10L, XMLWriter.getLong(attributes.getValue("testLong")));
                    assertEquals(20.05D, XMLWriter.getDouble(attributes.getValue("testDouble")), 0.000001);
                    assertEquals(true, XMLWriter.getBool(attributes.getValue("testBoolean")));
                    assertEquals(3, XMLWriter.getStringArray(attributes.getValue("testStringArray")).length);
                    assertEquals(3, XMLWriter.getCharArray(attributes.getValue("testCharArray")).length);
                    assertEquals(3, XMLWriter.getIntArray(attributes.getValue("testIntArray")).length);
                    assertEquals(new Point(18, 19), XMLWriter.getPoint(attributes.getValue("testPoint")));
                    assertEquals(new Dimension(18, 19), XMLWriter.getDim(attributes.getValue("testDimension")));
                }else{
                    tag2Loaded = true;
                }
            }
        }
    }
}