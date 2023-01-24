package org.starexec.util;

import org.starexec.constants.R;
import org.starexec.logger.StarLogger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hash.java
 *
 * @author Skylar Stark
 */
public class Hash {
	private static final StarLogger log = StarLogger.getLogger(Hash.class);

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
			return Hash.getHex(hasher.digest());

		} catch (NoSuchAlgorithmException e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	/**
	 * @param raw A raw byte array
	 * @return A string representing the hexidecimal version of the raw input. Every
	 * byte will be represented by exactly two hex characters
	 */
	public static String getHex(byte[] raw) {
		if (raw == null) {
			return null;
		}
		StringBuilder hex = new StringBuilder(2 * raw.length);
		for (byte b : raw) {
			hex.append(String.format("%02x", b));
		}

		return hex.toString();
	}
}
