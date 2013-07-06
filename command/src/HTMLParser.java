package org.starexec.command;

/**
 * This class reads HTML strings from StarExec and parses out necessary information from them.
 */

import org.apache.http.Header;
import org.apache.http.message.BasicNameValuePair;

public class HTMLParser {
	/**
	 * Extracts the substring between a pair of quotes, where startIndex is the index of the first quote.
	 * If there is no closing quote, the rest of the string from startindex+1 to the end is returned
	 * @param str The string upon which to do the extraction
	 * @param startIndex The index of the first quote
	 * @return The contents of the string between the start quote and the end quote
	 * @author Eric Burns
	 */
	
	private static String extractQuotedString(String str, int startIndex) {
		int endIndex=startIndex+1;
		while (endIndex<str.length()) {
			if (str.charAt(endIndex)=='"') {
				break;
			}
			endIndex+=1;
		}
		return str.substring(startIndex+1,endIndex);
	}
	
	/**
	 * If present, extracts the name and value from the current html line. Returns null if it doesn't exist
	 * @param htmlString The string to be processed
	 * @return A BasicNameValuePair containing the name and the value of an html tag.
	 * @author Eric Burns
	 */
	protected static BasicNameValuePair extractNameValue(String htmlString) {
		
		int startNameIndex=htmlString.indexOf("name=");
		if (startNameIndex<0) {
			return null;
		}
		
		String name=extractQuotedString(htmlString,startNameIndex+5);
		int startValueIndex=htmlString.indexOf("value=");
		if (startValueIndex<0) {
			return null;
		}
		String value=extractQuotedString(htmlString,startValueIndex+6);
		BasicNameValuePair pair=new BasicNameValuePair(name,value);
		
		return pair;
	}
	

	/**
	 * Given a Json string formatted as StarExec does its first line in a table
	 * extract the name of a primitive
	 * @param jsonString The Json string to test for an name
	 * @param type The type of primitive that could be present
	 * @return The name if it exists or null if it does not
	 */
	protected static String extractNameFromJson(String jsonString, String type) {
		
		//spaces are formatted differently from any other primitive
		if (type.equals("spaces")) {
			//space names are flanked on the left by the following
			int startIndex=jsonString.indexOf("onclick=\"openSpace");
			if (startIndex<0) {
				return null;
			}
			while (startIndex<jsonString.length() && jsonString.charAt(startIndex)!='>') {
				startIndex+=1;
			}
			if (startIndex>=jsonString.length()-1) {
				return null;
			}
			startIndex+=1;
			int endIndex=startIndex+1;
			while (endIndex<jsonString.length() && jsonString.charAt(endIndex)!='<') {
				endIndex+=1;
			}
			
			return jsonString.substring(startIndex,endIndex);
			
		}
		//Names are flanked on the left by a link that ends with target="_blank"\>
		int startIndex=jsonString.indexOf("_blank\">");
		
		if (startIndex<0) {
			return null;
		}
		
		//move across "_blank"\>
		startIndex+=8;
		int endIndex=startIndex+1;
		
		//names are flanked on the right by the start of another tag
		while (endIndex<jsonString.length() && jsonString.charAt(endIndex)!='<') {
			endIndex+=1;
		}
		return (jsonString.substring(startIndex,endIndex));
	}

	/**
	 * Given a Json string formatted as StarExec does it's first line in a table
	 * extract the ID of a primitive
	 * @param jsonString The Json string to test for an ID
	 * @return The ID if it exists or null if it does not
	 */
	protected static Integer extractIDFromJson(String jsonString) {
		
		//IDs are stored as the 'value' of a hidden input
		int startIndex=jsonString.indexOf("type=\"hidden\"");
		while (startIndex>=0 && jsonString.charAt(startIndex)!='<') {
			startIndex-=1;
		}
		if (startIndex<0) {
			return null;
		}
		startIndex=jsonString.indexOf("value",startIndex);
		if (startIndex<0) {
			return null;
		}
		startIndex+=7;
		int endIndex=startIndex+1;
		while (endIndex<jsonString.length() && jsonString.charAt(endIndex)!='"') {
			endIndex+=1;
		}
		String id=jsonString.substring(startIndex,endIndex);
		if (Validator.isValidPosInteger(id)) {
			return Integer.valueOf(id);
		}	
		return null;
	}
	

	
	/**
	 * Given the headers of an HttpResponse and the name of a cookie,
	 * check to see if that cookie was set and return its value if so
	 * @param headers-- An array of HTTP headers 
	 * @param cookieName the name of a cookie
	 * @return The value of the given cookie, or null if it was not present
	 * @author Eric Burns
	 */
	
	public static String extractCookie(Header[] headers, String cookieName) {
		
		for (Header x : headers) {
			if (x.getName().equals("Set-Cookie")) {
				String value=x.getValue().trim();
				if (value.contains(cookieName)) {
					int begin=value.indexOf(cookieName);
					
					if (begin<0) {
						return null;
					}
					begin+=cookieName.length()+1;
					
					int end=-1;
					for (int character=begin;character<value.length();character++) {
						if (value.substring(character,character+1).equals(";")) {
							end=character;
							break;
						}
					}
					
					//no semicolon means the cookie is at the end, so use the entire tail of the string
					if (end==-1) {
						end=value.length();
					}
					return value.substring(begin,end);
				}
			}
		}
		return null;
	}
}
