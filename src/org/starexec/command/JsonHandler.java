package org.starexec.command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
public class JsonHandler {
	/**
	 * Given an HttpRespone with a JsonElement in its content, returns
	 * the JsonElement
	 * @param response The HttpResponse that should contain the JsonElement
	 * @return The JsonElement
	 * @throws Exception
	 */
	
	public static JsonElement getJsonString(HttpResponse response) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
		StringBuilder builder = new StringBuilder();
		for (String line = null; (line = reader.readLine()) != null;) {
		    builder.append(line).append("\n");
		}
		JsonParser parser=new JsonParser();
		return parser.parse(builder.toString());
		
	}
	
	/**
	 * Gets back a status message from a ValidatorStatusCode sent back from the server
	 * object attached
	 * @param obj The json ValidatorStatusCode object to get the string message from
	 * @return The string message, or null if there is no ValidatorStatusCode
	 */
	
	protected static String getMessageOfResponse(JsonObject obj) {
		try {
			return obj.get("message").getAsString();
		} catch (Exception e) {
			return null;
		}

	}
	
	/**
	 * Gets back whether a request is successful from a response that has a JSON ValidatorStatusCode
	 * object attached
	 * @param obj The json ValidatorStatusCode object to get the boolean success value from
	 * @return Whether the request was successful, or null if there is no ValidatorStatusCode
	 */
	
	protected static Boolean getSuccessOfResponse(JsonObject obj) {
		
		try {
			return obj.get("success").getAsBoolean();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}
	
	/**
	 * Attempts to get a Json object given an HTTP response that has a Json object
	 * in its content
	 * @param response
	 * @return The object, or null if none existed
	 */
	public static JsonObject getJsonObject(HttpResponse response) {
		try {

			JsonElement jsonE=JsonHandler.getJsonString(response);
		
			JsonObject obj=jsonE.getAsJsonObject();
			return obj;
		} catch (Exception e) {
			return null;
		}
	}
	
	
	
	protected static HashMap<String,String> getJsonAttributes(JsonObject obj) {
		Iterator<Entry<String, JsonElement>> iterator=obj.entrySet().iterator();
		HashMap<String,String> attrs=new HashMap<String,String>();
		while (iterator.hasNext()) {
			Entry<String,JsonElement> e=iterator.next();
			String key=e.getKey();
			JsonElement value=e.getValue();
			if (value.isJsonPrimitive()) {
				attrs.put(key, value.getAsJsonPrimitive().getAsString());
			}
		}
		return attrs;
	}
}
