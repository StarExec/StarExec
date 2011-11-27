package org.starexec.util;


import java.util.regex.Pattern;

/**
 * Validation.java 
 * 
 * @author Todd Elvers
 */
public class Validate {
    
	private static Pattern nameChecker;
    private static Pattern emailChecker;
    private static Pattern institutionChecker;
    private static Pattern alphaNumericChecker;
    private static Pattern safeTextChecker;
    private static Pattern passwordChecker;
  
    private static final String PASS_PATTERN = 
        "^"											  	// Beginning of string
        + "(?=.*[A-Za-z0-9\\Q~`!@#$%^&*()_-+=\\E]+$)" 	// Permit only numbers, letters and ~`!@#$%^&*()_-+=
        + "(?=.*[0-9]{1,})"							  	// Require at least 1 digit
        + "(?=.*[A-Za-z]{1,})"						  	// Require at least one character (ignore case)
        + "(?=.*[\\Q~`!@#$%^&*()_-+=\\E]+{1,})"			// Require at least one special character
        + ".{6,20}"										// Permit only 6-20 characters
        + "$";											// End of string
    
    private static final String NAME_PATTERN = 
    	"^[a-zA-Z\\-'\\s]{2,32}$"; 
    
    private static final String INSTITUTION_PATTERN = 
    	"^[a-zA-Z\\-\\s]{2,64}$";
    
    
    private static final String EMAIL_PATTERN = 
    	"^[_A-Za-z0-9-]+" +
    	"(\\.[_A-Za-z0-9-]+)" +
    	"*@[A-Za-z0-9]+" +
    	"(\\.[A-Za-z0-9]+)" +
    	"*(\\.[A-Za-z]{2,})$";   
        
    private static final String ALPHA_NUMERIC_DASH =
    	"^[a-zA-Z0-9\\-_]{1,32}$";
    
    private static final String SAFE_TEXT =
        	"^[a-zA-Z0-9\\-\\s_.!?/,\\\\+=\"'#$%&*()\\[{}\\]]+$";
    
    static {
    	nameChecker = Pattern.compile(NAME_PATTERN);
        emailChecker = Pattern.compile(EMAIL_PATTERN);
        institutionChecker = Pattern.compile(INSTITUTION_PATTERN);
        passwordChecker = Pattern.compile(PASS_PATTERN);
        alphaNumericChecker = Pattern.compile(ALPHA_NUMERIC_DASH);
        safeTextChecker = Pattern.compile(SAFE_TEXT);
    }
        	
	/**
	 * Checks if a password is between 6-20 characters, contains at least
	 * one character, one number, and one punctuation mark 
	 * (acceptable punctuation: `~!@#$%^&*()-=_+)
	 * 
	 * @param password the password to check
	 * @return true iff password contains at least 1 character, 1 number and 1 punctuation
	 * mark, and is between 6-20 characters
	 */
	public static boolean password(String password) {
		if (Util.isNullOrEmpty(password)) {
			return false;
		} else {
			return passwordChecker.matcher(password).matches();
		}

	}
	

	/**
	 * Validates an institution field
	 * 
	 * @param institute the institution to validate
	 * @return true iff institute is less than 64 characters 
	 * and not null or the empty string
	 */
	public static boolean institution(String institute){
		if(Util.isNullOrEmpty(institute)){
			return false;
		} else {
			return institutionChecker.matcher(institute).matches();
		}
	}
	
	/**
	 * Validates an email address and checks if it is in proper email address format
	 * 
	 * @param email the email address to validate
	 * @return true iff the email address is less than 64 characters,
	 * not null or the empty string, and is in email address format
	 */
    public static boolean email(String email){
    	if(email.length() > 64 || Util.isNullOrEmpty(email)){
    		return false;
    	} else {
    		return emailChecker.matcher(email).matches();
    	}
    }
    
    /**
     * Validates a name and checks that it contains only letters and dashes
     * 
     * @param name the name to check
     * @return true iff name isn't null, is 32 characters or longer and
     * contains only letters and dashes
     */
    public static boolean name(String name){    	
    	if(Util.isNullOrEmpty(name)){
    		return false;
    	} else {
    		return nameChecker.matcher(name).matches();
    	}
    }
    
    /**
     * Validates a message by checking that it's not null or the empty string, and
     * that its between 1 and 300 characters in length
     * 
     * @param message the message to be checked
     * @return true iff the message isn't empty and is between 
     * 1 and 300 characters in length 
     */
    public static boolean message(String message){
    	if(message.length() < 1 || message.length() > 300 || Util.isNullOrEmpty(message)){
    		return false;
    	} 

    	return safeTextChecker.matcher(message).matches();
    }
    
    /**
     * Validates a name and checks that it contains only letters, numbers and dashes
     * 
     * @param name the space's name to check
     * @return true iff name isn't null, is between 1 and 32 characters and
     * contains only letters, numbers and dashes
     */
    public static boolean spaceName(String name){    	
    	if(Util.isNullOrEmpty(name)){
    		return false;
    	}
    	
    	return alphaNumericChecker.matcher(name).matches();    	
    }
    
    /**
     * Validates a name and checks that it contains only letters, numbers and dashes
     * 
     * @param name the bench type name to check
     * @return true iff name isn't null, is between 1 and 32 characters and
     * contains only letters, numbers and dashes
     */
    public static boolean benchTypeName(String name){    	
    	if(Util.isNullOrEmpty(name)){
    		return false;
    	}
    	
    	return alphaNumericChecker.matcher(name).matches();    	
    }
    
    /**
     * Validates a generic description and checks that it contains content and is less than 1024
     * characters long. ALL characters are allowed in descriptions.
     * 
     * @param desc the description to check
     * @return true iff name isn't null or empty and is less than 1024 characters
     */
    public static boolean description(String desc){    	
    	if(desc.length() > 512 || Util.isNullOrEmpty(desc)){
    		return false;
    	}
    	
    	return safeTextChecker.matcher(desc).matches();
    }
}