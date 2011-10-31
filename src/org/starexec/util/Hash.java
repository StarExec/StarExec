package org.starexec.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.catalina.util.HexUtils;
import org.apache.log4j.Logger;
import org.starexec.constants.R;


/**
 * Hash.java
 * 
 * @author Skylar Stark
 */
public class Hash {
	private static final Logger log = Logger.getLogger(Hash.class);
	
	
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
			String hashedPass = HexUtils.convert(hasher.digest());
			return hashedPass;
			
		} catch (NoSuchAlgorithmException e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
}
