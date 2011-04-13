package com.starexec.util;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Simple class to deal with hashing for storing database passwords.
 * @author Tyler Jensen
 */
public class SHA256 {
	
	/**
	 * Calculates the SHA-256 hash of s and returns its 64-character hexadecimal string representation.
	 * @param s The string to hash
	 * @return 64 character hex-string representation of the hash
	 * @throws NoSuchAlgorithmException 
	 */
	public static synchronized String getHash(String s) throws NoSuchAlgorithmException{	
		return bytesToHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(Charset.forName("UTF-8"))));	// Hash the String s and return its hex string format
	}
	/**
	 * Utility method to convert the digest into a hex-string
	 * @param b The byte array to convert to a hex string
	 * @return The hex-string version of the byte array
	 */
	private static String bytesToHex(byte[] b) {
	      char hexDigit[] = {'0', '1', '2', '3', '4', '5', '6', '7',
	                         '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	      StringBuffer buf = new StringBuffer();
	      for (int j=0; j<b.length; j++) {
	         buf.append(hexDigit[(b[j] >> 4) & 0x0f]);
	         buf.append(hexDigit[b[j] & 0x0f]);
	      }
	      return buf.toString();
	   }
}