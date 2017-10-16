package com.databazoo.tools;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamResult;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class EncryptedFileTest {

    @Test
    public void cbcWithKeyTest() throws Exception {
        File file = new File("/tmp/cbcFile.test");
        String text = "test 1 2 3\n\nsome text";
        byte[] keyBytes = "SECRET_1SECRET_2SECRET_3SECRET_4".getBytes();

        BouncyCastleAESwithCBC cbcHandler = new BouncyCastleAESwithCBC(keyBytes);
        cbcHandler.initCiphers();
        cbcHandler.cbcEncrypt(new ByteArrayInputStream(text.getBytes()), new FileOutputStream(file));

        BouncyCastleAESwithCBC cbcHandler2 = new BouncyCastleAESwithCBC(keyBytes);
        ByteArrayOutputStream baos;
        cbcHandler2.cbcDecrypt(new FileInputStream(file), baos = new ByteArrayOutputStream());

        assertEquals(text, baos.toString());
    }

    @Test
    public void cbcWithPasswordTest() throws Exception {
        File file = new File("/tmp/cbcFile.test");
        String text = "test 1 2 3\n\nsome text";
        String password = "ABC";

        BouncyCastleAESwithCBC cbcHandler = new BouncyCastleAESwithCBC(password);
        cbcHandler.initCiphers();
        cbcHandler.cbcEncrypt(new ByteArrayInputStream(text.getBytes()), new FileOutputStream(file));

        BouncyCastleAESwithCBC cbcHandler2 = new BouncyCastleAESwithCBC(password);
        ByteArrayOutputStream baos;
        cbcHandler2.cbcDecrypt(new FileInputStream(file), baos = new ByteArrayOutputStream());

        assertEquals(text, baos.toString());
    }

    @Test
    public void projectReadWrite() throws Exception {
        File file = new File("/tmp/encryptedFile.test");
        String password = "ABC";

        Document doc = XMLWriter.getNewDocument();
        Element elemRoot = doc.createElement("projects");
        doc.appendChild(elemRoot);
        elemRoot.appendChild(doc.createElement("project1"));
        elemRoot.appendChild(doc.createElement("project2"));

        StreamResult res = EncryptedFile.asStreamResult(file, password);
        XMLWriter.out(doc, res);
        res.getOutputStream().close();

        TestSAX testSAX = new TestSAX();
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        saxParser.parse(EncryptedFile.asInputStream(file, password), testSAX);

        assertEquals(3, testSAX.elements.size());
    }

    private class TestSAX extends DefaultHandler {
        List<String> elements = new ArrayList<>();

        @Override
        public void startElement(String uri, String localName, String tagName, Attributes attributes) throws SAXException {
            elements.add(tagName);
        }
    }
}