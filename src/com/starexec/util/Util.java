package com.starexec.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

public class Util {		
	/**
	 * Be careful not to read in a file that takes up too much memory.
	 * @param f File to insert
	 * @return The string representation of the file
	 * @throws IOException
	 */
	public static String readFile(File f) throws IOException {
		BufferedReader reader = null;
		String ls = getLineSeparator();
		
		try {
			reader = new BufferedReader(new FileReader(f));
			String line = null;
			StringBuilder str = new StringBuilder();
			while( (line = reader.readLine()) != null ) {
				str.append(line + ls);
			}
			reader.close();
			return str.toString();
		} catch(IOException e) {
			if(reader != null) reader.close();
			throw e;
		}
	}
	
	/**
	 * Extracts the file extesion from a file path
	 * @param s The file path
	 * @return The extension of the file
	 */
	public static String getFileExtension(String s){
		return s.substring(s.lastIndexOf('.') + 1);
	}
	
	/**
	 * @return The platform-dependant line separator
	 */
	public static String getLineSeparator(){
		return System.getProperty("line.separator");
	}
	
	/**
	 * Extracts the file name from an absolute path
	 * @param path The path of the file to extract the name from
	 * @return The file's name, not including its extension
	 */
	public static String getFileNameOnly(String path){
		int lastSep = path.lastIndexOf(File.separator);
		int lastDot = path.lastIndexOf('.');
		return path.substring(lastSep + 1, lastDot);
	}
	
	public static boolean paramExists(String name, HttpServletRequest request){
		return !isNullOrEmpty(request.getParameter(name));
	}
	
	public static boolean isNullOrEmpty(String s){
		return (s == null || s.trim().length() <= 0);
	}
}
