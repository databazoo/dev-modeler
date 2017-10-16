
package com.databazoo.tools;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.databazoo.components.UIConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Input-output wrapper for XML documents.
 * @author bobus
 */
public interface XMLWriter {

	int XML_WRITE_RETRY = 3;

	static Document getNewDocument() throws ParserConfigurationException {
		return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
	}

	static void setAttribute(Element elem, String name, String value) {
		if(value != null && !value.isEmpty() && !value.equals("null")){
			elem.setAttribute(name, value);
		}
	}
	static void setAttribute(Element elem, String name, char value) {
		setAttribute(elem, name, Character.toString(value));
	}
	static void setAttribute(Element elem, String name, int value) {
		setAttribute(elem, name, Integer.toString(value));
	}
	static void setAttribute(Element elem, String name, long value) {
		setAttribute(elem, name, Long.toString(value));
	}
	static void setAttribute(Element elem, String name, double value) {
		setAttribute(elem, name, Double.toString(value));
	}
	static void setAttribute(Element elem, String name, boolean value) {
		setAttribute(elem, name, value ? "1":"");
	}

	static void setAttribute(Element elem, String name, String[] value) {
		setAttribute(elem, name, getString(value));
	}
	static void setAttribute(Element elem, String name, char[] value) {
		setAttribute(elem, name, getString(value));
	}
	static void setAttribute(Element elem, String name, int[] value) {
		setAttribute(elem, name, getString(value));
	}

	static void setAttribute(Element elem, String name, Point value) {
		setAttribute(elem, name, getString(value));
	}
	static void setAttribute(Element elem, String name, Dimension value) {
		setAttribute(elem, name, getString(value));
	}

	static String getString(String[] input){
		StringBuilder ret = new StringBuilder();
		for (String str : input) {
			ret.append(",").append(str);
		}
		return ret.toString().length() > 0 ? ret.toString().substring(1) : "";
	}
	static String getString(char[] input){
		return String.valueOf(input);
	}
	static String getString(int[] input){
		StringBuilder ret = new StringBuilder();
		for (int anInput : input) {
			ret.append(",").append(anInput);
		}
		return ret.toString().length() > 0 ? ret.toString().substring(1) : "";
	}
	static String getString(Point input){
		return input.x+","+input.y;
	}
	static String getString(Dimension input){
		return input.width+","+input.height;
	}

	static void out(Document doc, File file) throws TransformerException, IOException, InterruptedException {
		boolean success = false;
		File fileTmp = new File(file.getParentFile(), file.getName()+".tmp");
		for(int i=0; i<XML_WRITE_RETRY; i++){
			out(doc, new StreamResult(file));
			out(doc, new StreamResult(fileTmp));

			if(file.length() == fileTmp.length()){
				success = true;
				fileTmp.delete();
				break;
			}else{
				Dbg.info(file.getName()+" filesize mismatch, retrying");
				Thread.sleep((long) (UIConstants.TYPE_TIMEOUT*Math.random()));
			}
		}
		if(!success){
			throw new IOException("Failed to write file "+file.getPath()+" after "+XML_WRITE_RETRY+" attempts");
		}
	}

	static void out(Document doc, StreamResult streamResult) throws TransformerException, IOException, InterruptedException {
		Transformer t = TransformerFactory.newInstance().newTransformer();
		t.setOutputProperty(OutputKeys.INDENT, "yes");
		t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		t.transform(new DOMSource(doc), streamResult);
	}

	static String getString(Element elem, String attrName){
		return getString(elem.getAttribute(attrName));
	}
	static Character getChar(Element elem, String attrName){
		return getChar(elem.getAttribute(attrName));
	}
	static int getInt(Element elem, String attrName){
		return getInt(elem.getAttribute(attrName));
	}
	static long getLong(Element elem, String attrName){
		return getLong(elem.getAttribute(attrName));
	}
	static boolean getBool(Element elem, String attrName){
		return getBool(elem.getAttribute(attrName));
	}
	static Dimension getDim(Element elem, String attrName){
		return getDim(elem.getAttribute(attrName));
	}
	static Point getPoint(Element elem, String attrName){
		return getPoint(elem.getAttribute(attrName));
	}

	static String[] getStringArray(Element elem, String attrName) {
		return getStringArray(elem.getAttribute(attrName));
	}
	static char[] getCharArray(Element elem, String attrName) {
		return getCharArray(elem.getAttribute(attrName));
	}
	static int[] getIntArray(Element elem, String attrName) {
		return getIntArray(elem.getAttribute(attrName));
	}

	static String getString(String value){
		if(value != null){
			return value;
		}
		return "";
	}
	static Date getDate(String value) {
		return new Date(getLong(value));
	}
	static Character getChar(String value){
		if(value != null && value.length() > 0){
			return value.charAt(0);
		}
		return null;
	}
	static int getInt(String value){
		if(value != null){
			return Integer.parseInt(value);
		}
		return 0;
	}
	static long getLong(String value){
		if(value != null){
			return Long.parseLong(value);
		}
		return 0;
	}
	static double getDouble(String value){
		if(value != null){
			return Double.parseDouble(value);
		}
		return 0;
	}
	static boolean getBool(String value){
		return value != null && value.equals("1");
	}
	static Dimension getDim(String value){
		if(value != null){
			String[] parts = value.split(",");
			return new Dimension(Integer.parseInt(parts[0]),Integer.parseInt(parts[1]));
		}
		return null;
	}
	static Point getPoint(String value){
		if(value != null){
			String[] parts = value.split(",");
			return new Point(Integer.parseInt(parts[0]),Integer.parseInt(parts[1]));
		}
		return null;
	}

	static String[] getStringArray(String value) {
		if(value != null){
			return value.split(",");
		}
		return new String[0];
	}
	static char[] getCharArray(String value) {
		if(value != null){
			return value.toCharArray();
		}
		return new char[0];
	}
	static int[] getIntArray(String value) {
		if(value != null && !value.isEmpty()){
			String[] parts = value.split(",");
			int[] ret = new int[parts.length];
			for(int i=0; i<parts.length; i++){
				ret[i] = Integer.parseInt(parts[i]);
			}
			return ret;
		}
		return new int[0];
	}
}
