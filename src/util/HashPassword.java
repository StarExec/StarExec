package util;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * HashPassword.java, based on org.starexec.util.Hash
 * 
 * @author Skylar Stark, Aaron Stump
 */
public class HashPassword {
	
	// The hexidecimal alphabet
	static final String HEXES = "0123456789abcdef";

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

    /**
     * Hashes a password using a pre-specified hashing algorithm.
     * 
     * @param unhashedPassword The password to be hashed
     * @return The hashed version of the password
     */
    public static String hashPassword(String unhashedPassword) {
	try {
	    // encoder used to hash password for storage
	    MessageDigest hasher = MessageDigest.getInstance("SHA-512");
	    hasher.update(unhashedPassword.getBytes());
	    // get the hashed version of the password
	    String hashedPass = HashPassword.getHex(hasher.digest());
	    return hashedPass;
			
	} catch (NoSuchAlgorithmException e) {
	    System.err.println(e.getMessage());
	    return null;
	}
    }
	
    public static void main(String args[]) {
	System.out.println(args[0]+" hashes to "+ hashPassword(args[0]));
    }
}