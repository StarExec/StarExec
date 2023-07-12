package org.starexec.util;

import org.starexec.constants.DB;
import org.starexec.constants.R;
import org.starexec.logger.StarLogger;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Contains methods for validating strings from user input to be stored in the database.
 *
 * @author Todd Elvers & Tyler Jensen
 */
public class Validator {
	private static final StarLogger log = StarLogger.getLogger(Validator.class);

	// Compiled patterns used for validation
	private static Pattern patternBoolean;
	private static Pattern patternUserName;
	private static Pattern patternInstitution;
	private static Pattern patternEmail;
	private static Pattern patternUrl;
	private static Pattern patternPrimName;
	private static Pattern patternSpaceName;
	private static Pattern patternPrimDesc;
	private static Pattern patternPassword;
	private static Pattern patternRequestMsg;
	public static final String[] extensions = {".tar", ".tar.gz", ".tgz", ".zip"};
	public static final String[] gitExtensions = {".git"};

	public static void initialize() {
		// Make sure some patterns were loaded first before we compile them
		if (Util.isNullOrEmpty(R.USER_NAME_PATTERN)) {
			log.error("Validator was initialized before patterns were loaded from configuration");
		} else {
			patternBoolean = Pattern.compile(R.BOOLEAN_PATTERN, Pattern.CASE_INSENSITIVE);
			patternUserName = Pattern.compile(R.USER_NAME_PATTERN, Pattern.CASE_INSENSITIVE);
			patternInstitution = Pattern.compile(R.INSTITUTION_PATTERN, Pattern.CASE_INSENSITIVE);
			patternEmail = Pattern.compile(R.EMAIL_PATTERN, Pattern.CASE_INSENSITIVE);
			patternUrl = Pattern.compile(R.URL_PATTERN, Pattern.CASE_INSENSITIVE);
			patternPrimName = Pattern.compile(R.PRIMITIVE_NAME_PATTERN, Pattern.CASE_INSENSITIVE);
			patternSpaceName = Pattern.compile(R.SPACE_NAME_PATTERN, Pattern.CASE_INSENSITIVE);
			patternPrimDesc = Pattern.compile(R.PRIMITIVE_DESC_PATTERN, Pattern.DOTALL);
			patternPassword = Pattern.compile(R.PASSWORD_PATTERN);
			patternRequestMsg = Pattern.compile(R.REQUEST_MESSAGE, Pattern.CASE_INSENSITIVE);
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
		return password != null && patternPassword.matcher(password).matches();
	}

	/**
	 * Validates an institution field
	 *
	 * @param institute the institution to validate
	 * @return true iff institute is less than DB.INSTITUTION_LEN characters
	 * and not null or the empty string
	 */
	public static boolean isValidInstitution(String institute) {
		return institute != null && patternInstitution.matcher(institute).matches();
	}

	/**
	 * Validates an email address and checks if it is in proper email address format
	 *
	 * @param email the email address to validate
	 * @return true iff the email address is less than DB.EMAIL_LEN characters,
	 * not null or the empty string, and is in email address format
	 */
	public static boolean isValidEmail(String email) {
		return email != null && patternEmail.matcher(email).matches();
	}

	/**
	 * Validates a name and checks that it contains only letters and dashes
	 *
	 * @param name the name to check
	 * @return true iff name isn't null, is 32 characters or inter and
	 * contains only letters and dashes
	 */
	public static boolean isValidUserName(String name) {
		return name != null && patternUserName.matcher(name).matches();
	}

	/**
	 * Validates a message by checking that it's not null or the empty string, and
	 * that its between 1 and 512 characters in length
	 *
	 * @param message the message to be checked
	 * @return true iff the message isn't empty and is between
	 * 1 and 512 characters in length
	 */
	public static boolean isValidRequestMessage(String message) {
		return message != null && patternRequestMsg.matcher(message).matches();
	}

	public static boolean isValidSolverName(String name) {
		return isValidPrimName(name, DB.SOLVER_NAME_LEN);
	}

	public static boolean isValidSettingsName(String name) {
		return isValidPrimName(name, DB.SETTINGS_NAME_LEN);
	}

	public static boolean isValidJobName(String name) {
		return isValidPrimName(name, DB.JOB_NAME_LEN);
	}

	public static boolean isValidPipelineName(String name) {
		return isValidPrimName(name, DB.PIPELINE_NAME_LEN);
	}

	public static boolean isValidProcessorName(String name) {
		return isValidPrimName(name, DB.PROCESSOR_NAME_LEN);
	}

	public static boolean isValidQueueName(String name) {
		return isValidPrimName(name, DB.QUEUE_NAME_LEN);
	}

	public static boolean isValidWebsiteName(String name) {
		return isValidPrimName(name, DB.WEBSITE_NAME_LEN);
	}

	public static boolean isValidConfigurationName(String name) {
		return isValidPrimName(name, DB.CONFIGURATION_NAME_LEN);
	}

	/**
	 * Validates a name and checks that it contains only letters, numbers and dashes
	 *
	 * @param name the space's name to check
	 * @return true iff name isn't null, is between 1 and PRIM_NAME_LEN characters and
	 * contains only letters, numbers and dashes
	 */
	private static boolean isValidPrimName(String name, int maxLength) {
		return
			(name != null)
			&& patternPrimName.matcher(name).matches()
			&& (name.length() <= maxLength);
	}

	/**
	 * Validates a name and checks that it contains only letters, numbers and dashes
	 *
	 * @param name the space's name to check
	 * @return true iff name isn't null, is between 1 and Bench_NAME_LEN characters and
	 * contains only letters, numbers and dashes
	 */
	public static boolean isValidBenchName(String name) {
		return isValidPrimName(name, DB.BENCH_NAME_LEN);
	}

	/**
	 * Validates a name and checks that it contains only letters, numbers and dashes
	 *
	 * @param name the space's name to check
	 * @return true iff name isn't null, is between 1 and SOLVER_NAME_LEN characters and
	 * contains only letters, numbers and dashes
	 */
	public static boolean isValidSpaceName(String name) {
		return name != null && patternSpaceName.matcher(name).matches();
	}

	/**
	 * Validates a boolean value by ensuring it is something Boolean.parseBoolean()
	 * can handle
	 *
	 * @param boolString the string to check for a parse-able boolean value
	 * @return true iff boolString isn't null and is either "true" or "false"
	 */
	public static boolean isValidBool(String boolString) {
		return boolString != null && patternBoolean.matcher(boolString).matches();
	}

	/**
	 * Validates a generic description and checks that it contains content and is less than 1024
	 * characters long. ALL characters are allowed in descriptions.
	 *
	 * @param desc the description to check
	 * @return true iff name isn't null or empty and is less than 1024 characters
	 */
	public static boolean isValidPrimDescription(String desc) {
		return desc != null && patternPrimDesc.matcher(desc).matches();
	}

	/**
	 * Validates a website URL and makes sure it begins with http(s) and is less
	 * than 128 characters.
	 *
	 * @param url the URL to check
	 * @return true iff the URL passes the check
	 */
	public static boolean isValidWebsite(String url) {
		return url != null && patternUrl.matcher(url).matches();
	}

	/**
	 * Validates a string to ensure it can be treated as a int
	 *
	 * @param s The string to validate as a int
	 * @return True if the string is numeric, false otherwise
	 */
	public static boolean isValidInteger(String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Determines whether the given string represents a valid id. Valid ids are integers greater than or equal to 0
	 *
	 * @param str The string to check
	 * @return True if valid, false otherwise.
	 * @author Eric Burns
	 */
	public static boolean isValidPosInteger(String str) {
		try {
			int check = Integer.parseInt(str);
			return check >= 0;
		} catch (Exception e) {
			return false;
		}
	}


	/**
	 * Determines whether the given string represents a valid double that is greater than 0
	 *
	 * @param str The string to check
	 * @return True if valid, false otherwise.
	 * @author Eric Burns
	 */
	public static boolean isValidPosDouble(String str) {
		try {
			double check = Double.parseDouble(str);
			return check > 0;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Validates a string to ensure it can be treated as a long
	 *
	 * @param s The string to validate as a long
	 * @return True if the string is numeric, false otherwise
	 */
	public static boolean isValidLong(String s) {
		try {
			Long.parseLong(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Validates a list of strings to ensure every one is a valid int and checks that the list is not null
	 *
	 * @param list The list of strings to validate
	 * @return True if every string in the array can be parsed as a int
	 */
	public static boolean isValidIntegerList(String[] list) {
		return
			(list != null)
			&& Arrays.stream(list).allMatch(Validator::isValidInteger);
	}

	/**
	 * Determines whether the given string is a valid comma-separated list of positive integers
	 *
	 * @param ids The string to check
	 * @return True if the string is a comma-separated list of positive integers, false otherwise
	 */
	public static boolean isValidPosIntegerList(String ids) {
		return
			(ids != null)
			&& Arrays.stream(ids.split(",")).allMatch(Validator::isValidPosInteger);
	}

	public static boolean isValidPictureType(String type) {
		return (type.equals("user") || type.equals("benchmark") || type.equals(R.SOLVER));
	}

	/**
	 * Validates an archive type. Archives must be either .zip, .tar, or .tar.gz/.tgz
	 *
	 * @param format the archive type to check
	 * @return true iff the format is of supported type
	 */
	public static boolean isValidArchiveType(String format) {
		return (format != null)
				&& Arrays.stream(extensions).anyMatch(s->format.endsWith(s));
	}

	/**
	 * Validates an Git URL type. URL must end with .git
	 *
	 * @param format the Git URL type to check
	 * @return true iff the format is of supported type
	 */
	public static boolean isValidGitType(String format) {
		return (format != null)
				&& Arrays.stream(gitExtensions).anyMatch(s->format.endsWith(s));
	}

	/**
	 * Determines whether the given string is a valid comma-separated list of integers
	 *
	 * @param ids The string to check
	 * @return True if the string is a comma-separated list of  integers, false otherwise
	 */
	public static boolean isValidIntegerList(String ids) {
		return
			(ids != null)
			&& Arrays.stream(ids.split(",")).allMatch(Validator::isValidInteger);
	}

	public static List<Integer> convertToIntList(String str) {
		String[] ids = str.split(",");
		return Util.toIntegerList(ids);
	}
}
