package org.starexec.util;


import java.util.regex.Pattern;

/**
 * Validation.java 
 * 
 * @author Todd Elvers
 */
public class Validate {
    
    private static Pattern emailChecker;
    private static Pattern nameChecker;
    private static Pattern alpaNumericChecker;
    private static Pattern passwordChecker;
  
    private static final String ALPHA_NUMERIC_DASH = "[a-zA-Z0-9\\-\\s]+";    
    private static final String PASS_PATTERN_LETTERS = "[a-zA-Z]";
    private static final String PASS_PATTERN_NUMBERS = "[0-9]";
    private static final String PASS_PATTERN_PUNCT   = "[~!@#$%^&*()\\-_=+]";
    private static final String PASS_PATTERN_ILLEGAL = "[^a-zA-Z0-9~!@#$%^&*()\\-_=+]";
    private static final String NAME_PATTERN         = "[a-zA-Z\\-]+";
    private static final String EMAIL_PATTERN        = 
    	"^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";   
        
    
    static {
        emailChecker = Pattern.compile(EMAIL_PATTERN);
        nameChecker = Pattern.compile(NAME_PATTERN);
        alpaNumericChecker = Pattern.compile(ALPHA_NUMERIC_DASH);
    }
        	
	/**
	 * Checks if a password is between 6-16 characters, contains at least
	 * one character, one number, and one punctuation mark 
	 * (acceptable punctuation: ~!@#$%^&*()-=_+)
	 * 
	 * @param password the password to check
	 * @return true iff password contains at least 1 character, 1 number and 1 punctuation
	 * mark, and is between 6-16 characters
	 */
	public static boolean password(String password){
		if(password.length() < 6 || password.length() > 16 || Util.isNullOrEmpty(password)){
			return false;
		} else {
	        passwordChecker = Pattern.compile(PASS_PATTERN_LETTERS);
	        // Check for at least 1 letter in the password
	        if(passwordChecker.matcher(password).find()){
	            passwordChecker = Pattern.compile(PASS_PATTERN_NUMBERS);
		        // Check for at least 1 number in the password
	            if(passwordChecker.matcher(password).find()){
	                passwordChecker = Pattern.compile(PASS_PATTERN_PUNCT);
			        // Check for at least 1 punctuation mark in the password
	                if(passwordChecker.matcher(password).find()){
	                    passwordChecker = Pattern.compile(PASS_PATTERN_ILLEGAL);
	                    // Ensure no illegal characters exist in the password
	                    if(!passwordChecker.matcher(password).find()){
	                        return true;
	                    }
	                }
	            }
	        }
	        return false;
		}
	}

	/**
	 * Validates an institution field
	 * 
	 * @param institute the institution to validate
	 * @return true iff institute is less than 64 characters 
	 * and not null or the empty string
	 */
	public static boolean institute(String institute){
		if(institute.length() > 64 || Util.isNullOrEmpty(institute)){
			return false;
		} else {
			return true;
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
    	if(name.length() > 32 || Util.isNullOrEmpty(name)){
    		return false;
    	} else {
    		return nameChecker.matcher(name).matches();
    	}
    }
    
    /**
     * Validates a name and checks that it contains only letters and dashes
     * 
     * @param name the space's name to check
     * @return true iff name isn't null, is 32 characters or shorter and
     * contains only letters and dashes
     */
    public static boolean spaceName(String name){    	
    	if(name.length() > 32 || Util.isNullOrEmpty(name)){
    		return false;
    	}
    	
    	return alpaNumericChecker.matcher(name).matches();    	
    }
    
    /**
     * Validates a generic description and checks that it contains content and is less than 1024
     * characters long. ALL characters are allowed in descriptions.
     * 
     * @param desc the description to check
     * @return true iff name isn't null or empty and is less than 1024 characters
     */
    public static boolean description(String desc){    	
    	if(desc.length() > 1024 || Util.isNullOrEmpty(desc)){
    		return false;
    	}
    	
    	return true;
    }
}