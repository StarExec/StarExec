package org.starexec.util;


import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * Contains methods for validating strings from user input to be stored in the database. 
 * @author Todd Elvers & Tyler Jensen
 */
public class Validator {
	private static final Logger log = Logger.getLogger(Validator.class);
	
	// Regex patterns loaded from configuration
	public static String BOOLEAN_PATTERN;										
	public static String LONG_PATTERN;
	public static String USER_NAME_PATTERN;
	public static String INSTITUTION_PATTERN;
	public static String EMAIL_PATTERN;
	public static String URL_PATTERN;
	public static String PRIMITIVE_NAME_PATTERN;
	public static String PRIMITIVE_DESC_PATTERN;
	public static String PASSWORD_PATTERN;
	public static String REQUEST_MESSAGE;

	// Compiled patterns used for validation	
	private static Pattern patternBoolean;
	private static Pattern patternInteger;	
	private static Pattern patternUserName;
	private static Pattern patternInstitution;
	private static Pattern patternEmail;
	private static Pattern patternUrl;
	private static Pattern patternPrimName;
	private static Pattern patternPrimDesc;
	private static Pattern patternPassword;
	private static Pattern patternRequestMsg;
	
    public static void initialize() {
    	// Make sure some patterns were loaded first before we compile them
    	if(Util.isNullOrEmpty(USER_NAME_PATTERN)) {
    		log.error("Validator was initialized before patterns were loaded from configuration");    		
    	} else {    		
	    	patternBoolean = Pattern.compile(BOOLEAN_PATTERN, Pattern.CASE_INSENSITIVE);										
	    	patternInteger = Pattern.compile(LONG_PATTERN);
	    	patternUserName = Pattern.compile(USER_NAME_PATTERN, Pattern.CASE_INSENSITIVE);
	    	patternInstitution = Pattern.compile(INSTITUTION_PATTERN, Pattern.CASE_INSENSITIVE);
	    	patternEmail = Pattern.compile(EMAIL_PATTERN, Pattern.CASE_INSENSITIVE);
	    	patternUrl = Pattern.compile(URL_PATTERN, Pattern.CASE_INSENSITIVE);
	    	patternPrimName = Pattern.compile(PRIMITIVE_NAME_PATTERN, Pattern.CASE_INSENSITIVE);
	    	patternPrimDesc = Pattern.compile(PRIMITIVE_DESC_PATTERN, Pattern.CASE_INSENSITIVE);
	    	patternPassword = Pattern.compile(PASSWORD_PATTERN);
	    	patternRequestMsg = Pattern.compile(REQUEST_MESSAGE, Pattern.CASE_INSENSITIVE);
	    	log.debug("Validator patterns successfully compiled");
    	}
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
	public static boolean isValidPassword(String password) {		
		return patternPassword.matcher(password).matches();		
	}
		
	/**
	 * Validates an institution field
	 * 
	 * @param institute the institution to validate
	 * @return true iff institute is less than 64 characters 
	 * and not null or the empty string
	 */
	public static boolean isValidInstitution(String institute){		
		return patternInstitution.matcher(institute).matches();		
	}
	
	/**
	 * Validates an email address and checks if it is in proper email address format
	 * 
	 * @param email the email address to validate
	 * @return true iff the email address is less than 64 characters,
	 * not null or the empty string, and is in email address format
	 */
    public static boolean isValidEmail(String email){
		return patternEmail.matcher(email).matches();
    }
    
    /**
     * Validates a name and checks that it contains only letters and dashes
     * 
     * @param name the name to check
     * @return true iff name isn't null, is 32 characters or inter and
     * contains only letters and dashes
     */
    public static boolean isValidUserName(String name){    	
		return patternUserName.matcher(name).matches();
    }
    
    /**
     * Validates a message by checking that it's not null or the empty string, and
     * that its between 1 and 300 characters in length
     * 
     * @param message the message to be checked
     * @return true iff the message isn't empty and is between 
     * 1 and 300 characters in length 
     */
    public static boolean isValidRequestMessage(String message){
    	return patternRequestMsg.matcher(message).matches();
    }
    
    /**
     * Validates a name and checks that it contains only letters, numbers and dashes
     * 
     * @param name the space's name to check
     * @return true iff name isn't null, is between 1 and 32 characters and
     * contains only letters, numbers and dashes
     */
    public static boolean isValidPrimName(String name){    	
    	return patternPrimName.matcher(name).matches();    	
    }
    
    /**
     * Validates a boolean value by ensuring it is something Boolean.parseBoolean()
     * can handle
     * 
     * @param boolString the string to check for a parse-able boolean value
     * @return true iff boolString isn't null and is either "true" or "false"
     */
    public static boolean isValidBool(String boolString){
    	return patternBoolean.matcher(boolString).matches();
    }
    
    /**
     * Validates a generic description and checks that it contains content and is less than 1024
     * characters int. ALL characters are allowed in descriptions.
     * 
     * @param desc the description to check
     * @return true iff name isn't null or empty and is less than 1024 characters
     */
    public static boolean isValidPrimDescription(String desc){    	    	
    	return patternPrimDesc.matcher(desc).matches();
    }
    
    /** 
     * Validates a website URL and makes sure it begins with http(s) and is less
     * than 128 characters.
     * @param url the URL to check
     * @return true iff the URL passes the check
     */
    public static boolean isValidWebsite(String url) {    	
    	return patternUrl.matcher(url).matches();
    }
    
    /**
     * Validates a string to ensure it can be treated as a int 
     * @param s The string to validate as a int
     * @return True if the string is numeric, false otherwise
     */
    public static boolean isValidInteger(String s) {
    	return patternInteger.matcher(s).matches();
    }
    
    /**
     * Validates a list of strings to ensure every one is a valid int
     * @param list The list of strings to validate
     * @return True if every string in the array can be parsed as a int
     */
    public static boolean isValidIntegerList(String[] list) {
    	for(String s : list) {
    		if(false == patternInteger.matcher(s).matches()) {
    			return false;
    		}
    	}
    	
    	return true;
    }
}