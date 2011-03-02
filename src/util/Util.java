package util;

public class Util {
	public static String getFileExtension(String s){
		return s.substring(s.lastIndexOf('.') + 1);
	}
}
