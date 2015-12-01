package org.starexec.util;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
     *@return The descendant element with the given name.
     *@author Julio Cervantes
     **/
    public static Element getElementByName(Element e, String name){
    	if (hasElement(e,name)) {
        	return (Element) e.getElementsByTagName(name).item(0);
    	}
    	return null;
    }

	/**
	 * Helper function, gets the first child Element of another Element that has a given tagname.
	 * If there are no child elements with the given tag name returns null.
	 * @param e the parent element
	 * @param name the element name
	 * @return The child element with the given name.
	 * @author Albert Giegerich
	 */
	public static Element getChildElementByName(Element e, String name) {
		NodeList childNodes = e.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child instanceof Element) {
				Element childElement = (Element)child;
				if (childElement.getTagName().equals(name)) {
					return childElement;
				}
			}	
		}
		return null;
	}

    public static boolean hasElement(Element e, String name){
    	return (e.getElementsByTagName(name).getLength() > 0);
    }


}
