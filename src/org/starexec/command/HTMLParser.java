package org.starexec.command;

import org.apache.http.Header;
import org.starexec.util.Validator;

import java.util.Map;
import java.util.regex.*;

/**
 * This class reads HTML strings from StarExec and parses out necessary
 * information from them. HTML parsing is really to be avoided as much as
 * possible due to how fragile it can be.
 */

public class HTMLParser {

	/**
	 * This method encodes a given set of parameters into the given URL to be
	 * used in HTTP Get requests
	 *
	 * @param u The initial URL to be built upon
	 * @param params a list of name/value pairs to be encoded into the URL
	 * @return A new URL with the base u and the parameters in params encoded
	 * @author Eric Burns
	 */

	public static String URLEncode(String u, Map<String, String> params) {
		StringBuilder answer = new StringBuilder(u);
		answer.append("?");
		params.forEach ( (key, value) -> {
			answer.append(key);
			answer.append("=");
			answer.append(value);
			answer.append("&");
		} );

		return answer.substring(0, answer.length() - 1);
	}

	/**
	 * Given a Json string formatted as StarExec does its first line in a table
	 * extract the name of a primitive
	 *
	 * @param jsonString The Json string to test for an name
	 * @param type The type of primitive that could be present
	 * @return The name if it exists or null if it does not
	 */
	public static String extractNameFromJson(String jsonString, String type) {

		// spaces are formatted differently from any other primitive
		if (type.equals("spaces")) {
			// space names are flanked on the left by the following
			int startIndex = jsonString.indexOf("onclick=\"openSpace");
			if (startIndex < 0) {
				return null;
			}
			while (startIndex < jsonString.length() && jsonString.charAt(startIndex) != '>') {
				startIndex += 1;
			}
			if (startIndex >= jsonString.length() - 1) {
				return null;
			}
			startIndex += 1;
			int endIndex = startIndex + 1;
			while (endIndex < jsonString.length() && jsonString.charAt(endIndex) != '<') {
				endIndex += 1;
			}

			return jsonString.substring(startIndex, endIndex);

		}
		// Names are flanked on the left by a link that ends with
		// target="_blank"\>
		int startIndex = jsonString.indexOf("_blank\">");

		if (startIndex < 0) {
			return null;
		}

		// move across "_blank"\>
		startIndex += 8;
		int endIndex = startIndex + 1;

		// names are flanked on the right by the start of another tag
		while (endIndex < jsonString.length() && jsonString.charAt(endIndex) != '<') {
			endIndex += 1;
		}
		return (jsonString.substring(startIndex, endIndex));
	}

	private static final Pattern extractHiddenValue = Pattern.compile("\"(\\d+)\"[^>]*type=\"hidden\"");

	/**
	 * Given a Json string formatted as StarExec does its first line in a table
	 * extract the ID of a primitive
	 *
	 * @param jsonString The Json string to test for an ID
	 * @return The ID if it exists or null if it does not
	 */
	public static Integer extractIDFromJson(String jsonString) {
		if (jsonString == null) {
			return null;
		}

		Matcher m = extractHiddenValue.matcher(jsonString);
		if ( m.find() ) {
			String id = m.group(1);
			if (Validator.isValidPosInteger(id)) {
				return Integer.valueOf(id);
			}
		}
		return null;
	}

	/**
	 * Extracts all the values of a comma-separated cookie as a list of strings.
	 *
	 * @param headers Array of headers to check for cookies in
	 * @param cookieName The name of the cookie to look for
	 * @return A string array, where each value in the array is one value of the
	 *         comma-separated cookie.
	 */
	public static String[] extractMultipartCookie(Header[] headers, String cookieName) {
		String value = extractCookie(headers, cookieName);
		if (value == null) {
			return null;
		}

		return value.replace("\"", "").split(",");
	}

	/**
	 * Given the headers of an HttpResponse and the name of a cookie, check to
	 * see if that cookie was set and return its value if so
	 *
	 * @param headers An array of HTTP headers
	 * @param cookieName the name of a cookie
	 * @return The value of the given cookie, or null if it was not present
	 * @author Eric Burns
	 */

	public static String extractCookie(Header[] headers, String cookieName) {
		if (headers == null || cookieName == null) {
			return null;
		}
		for (Header x : headers) {
			// cookies are parsed in the code below. Note that cookies are in a
			// form like
			// Set-Cookie: name=Nicholas; expires=Sat, 02 May 2009 23:38:25 GMT
			// where all cookies are on a single semicolon-delimited line.
			if (x.getName().equals("Set-Cookie")) {

				String value = x.getValue().trim();
				if (value.contains(cookieName)) {
					int begin = value.indexOf(cookieName);

					if (begin < 0) {
						return null;
					}
					begin += cookieName.length() + 1;

					int end = -1;
					for (int character = begin; character < value.length(); character++) {
						if (value.substring(character, character + 1).equals(";")) {
							end = character;
							break;
						}
					}

					// no semicolon means the cookie is at the end, so use the
					// entire tail of the string
					if (end == -1) {
						end = value.length();
					}
					return value.substring(begin, end);
				}
			}
		}
		return null;
	}
}
