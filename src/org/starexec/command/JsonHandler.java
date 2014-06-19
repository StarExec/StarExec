package org.starexec.command;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
class JsonHandler {
	/**
	 * Given an HttpRespone with a JsonElement in its content, returns
	 * the JsonElement
	 * @param response The HttpResponse that should contain the JsonElement
	 * @return The JsonElement
	 * @throws Exception
	 */
	
	protected static JsonElement getJsonString(HttpResponse response) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
		StringBuilder builder = new StringBuilder();
		for (String line = null; (line = reader.readLine()) != null;) {
		    builder.append(line).append("\n");
		}
		JsonParser parser=new JsonParser();
		System.out.println(builder.toString());
		return parser.parse(builder.toString());
		
	}
}
