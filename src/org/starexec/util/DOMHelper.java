package org.starexec.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *Handles general DOM object handling
 *
 **/

public class DOMHelper{

    /**
     * Helper function, gets element by name assuming that there is only one such descendent element
     * if there is more than one descendant element, gets the first in the returned NodeList. If there
     * are no such descendants, returns null
     *@param e the parent element
     *@param name the element name
     *@return The child element with the given name.
     *@author Julio Cervantes
     **/
    public static Element getElementByName(Element e, String name){
    	if (hasElement(e,name)) {
        	return (Element) e.getElementsByTagName(name).item(0);
    	}
    	return null;
    }

    public static boolean hasElement(Element e, String name){
    	return (e.getElementsByTagName(name).getLength() > 0);
    }

}