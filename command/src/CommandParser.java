package org.starexec.command;

/**
 * This class parses strings given by the user at the command line into more usable data structures
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class CommandParser {
	/**
	 * Given a comma-separated string, converts it to an array of strings
	 * with all leading and trailing whitespace removed.
	 * @param str The string to convert
	 * @return An array of strings 
	 */
	
	public static String[] convertToArray(String str) {
		String[] ids=str.split(",");
		for (int x=0;x<ids.length;x++) {
			ids[x]=ids[x].trim();
		}
		
		return ids;
	}

	/**
	 * This function parses a command given by the user and extracts all of the parameters and flags
	 * @param command The string given by the user at the command line
	 * @return A HashMap containing key/value pairs representing parameters input by the user,
	 * or null if there was a parsing error
	 * @author Eric Burns
	 */
	
	public static HashMap<String,String> extractParams(String command) {
		List<String> args=Arrays.asList(command.split(" "));
		
		//the first element is the command, which we don't want
		args=args.subList(1, args.size());
		HashMap<String,String> answer=new HashMap<String,String>();
		int index=0;
		String x;
		StringBuilder value;
		while (index<args.size()) {
			x=args.get(index);
			int equalsIndex=x.indexOf('=');
			
			//no equals sign means a parsing error, so return null
			if (equalsIndex==-1) {
				return null;
			}
			String key=x.substring(0,equalsIndex).toLowerCase();
			
			//we shouldn't have duplicate parameters-- indicates an error
			if (answer.containsKey(key)) {
				return null;
			}
			
			//the value is everything up until the next token with an equals sign
			value=new StringBuilder();
			value.append(x.substring(equalsIndex+1));
			index+=1;
			String nextString;
			while (true) {
				if (index==args.size()) {
					break;
				}
				nextString=args.get(index);
				//the next string contains the next key
				if (nextString.contains("=")) {
					break;
				} else {
					//otherwise, this string is part of the current value
					value.append(" ");
					value.append(nextString);
					index+=1;
				}
			}
			
			answer.put(key, value.toString());
		}
		return answer;
	}
}
