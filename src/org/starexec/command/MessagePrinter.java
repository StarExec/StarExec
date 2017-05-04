package org.starexec.command;

import java.util.List;

/**
 * This class handles printing messages to stdout. This is mostly done to
 * communicate with users at the command prompt.
 */
public class MessagePrinter {

	/**
	 * Given an status code defined in R, prints the message that corresponds to
	 * that status code.
	 * 
	 * @param statusCode An integer status code.
	 * @author Eric Burns
	 * @param parser The Parser object that will contain any error messages from
	 *        recent commands
	 */
	protected static void printStatusMessage(int statusCode, CommandParser parser) {

		if (statusCode < 0) {
			System.out.print("ERROR: ");
			String message = null;
			if (statusCode == Status.ERROR_SERVER) {
				message = parser.getLastServerError();
			} else {
				message = Status.getStatusMessage(statusCode);

			}
			if (message != null) {
				System.out.println(message);
			} else {
				System.out.println("Unknown error");
			}
			if (statusCode == Status.ERROR_MISSING_PARAM) {
				System.out.println("Missing param = \"" + CommandValidator.getMissingParam() + "\"");
			}
		} else if (statusCode > 0) {
			String message = C.successMessages.get(statusCode);
			if (message != null) {
				System.out.println(message);
			}
		}

	}

	/**
	 * Prints a warning if the user gave and unnecessary parameters that were
	 * ignored
	 */
	protected static void printWarningMessages() {
		List<String> up = CommandValidator.getUnnecessaryParams();
		if (up.size() > 0) {
			System.out.print("WARNING: The following unnecessary parameters were ignored: ");
			for (String x : up) {
				System.out.print(x + " ");
			}
			System.out.print("\n");
		}
	}
}
