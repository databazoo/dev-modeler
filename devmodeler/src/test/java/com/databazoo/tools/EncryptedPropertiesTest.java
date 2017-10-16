package com.databazoo.tools;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.junit.Test;

public class EncryptedPropertiesTest {
    private File FILE = new File(new File(System.getProperty("user.dir"), "target"), "test.properties");
    @Test
    public void readWrite() throws Exception {
        EncryptedProperties props;
        props = new EncryptedProperties("TEST");
        //props.load(new FileInputStream(path + "/props.test"));
        props.setProperty("testVector", "90 60 90");
        props.setProperty("testString", "my string");
        props.setProperty("testBool", true);
        props.setProperty("testInt", 10);
        props.setProperty("testLong", 10L);
        props.store(new FileOutputStream(FILE), "TEST");

        props = new EncryptedProperties("TEST");
        props.load(new FileInputStream(FILE));

        assertEquals("my string", props.getStr("testString"));
        assertEquals(true, props.getBool("testBool"));
        assertEquals(10, props.getInt("testInt"));
        assertEquals(10L, props.getLong("testLong"));
        assertEquals(3, props.getStrVector("testVector").length);
        assertEquals(3, props.getIntVector("testVector").length);
    }
}
