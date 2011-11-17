package org.starexec.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;
import org.starexec.constants.R;


/**
 * Hash.java
 * 
 * @author Skylar Stark
 */
public class Hash {
	private static final Logger log = Logger.getLogger(Hash.class);
	
	// The hexidecimal alphabet
	static final String HEXES = "0123456789ABCDEF";
	
	/**
	 * Hashes a password using a pre-specified hashing algorithm.
	 * 
	 * @param unhashedPassword The password to be hashed
	 * @return The hashed version of the password
	 */
	public static String hashPassword(String unhashedPassword) {
		try {
			// encoder used to hash password for storage
			MessageDigest hasher = MessageDigest.getInstance(R.PWD_HASH_ALGORITHM);
			hasher.update(unhashedPassword.getBytes());
		    // get the hashed version of the password
			String hashedPass = Hash.getHex(hasher.digest());
			return hashedPass;
			
		} catch (NoSuchAlgorithmException e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	/**
	 * @param raw A raw byte array
	 * @return A string representing the hexidecimal version of the raw input
	 */
	public static String getHex( byte [] raw ) {
	    if ( raw == null ) {
	      return null;
	    }
	    
	    final StringBuilder hex = new StringBuilder( 2 * raw.length );
	    
	    for ( final byte b : raw ) {
	      hex.append(HEXES.charAt((b & 0xF0) >> 4))
	         .append(HEXES.charAt((b & 0x0F)));
	    }
	    
	    return hex.toString();
	}
}