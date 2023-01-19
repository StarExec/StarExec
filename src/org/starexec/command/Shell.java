package org.starexec.command;

import org.starexec.logger.StarLogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * This class is the entry point for StarexecCommand. It allows either the
 * execution of an interactive shell or the execution of a file with
 * line-separated commands.
 * 
 * @author Eric
 */

public class Shell {

	private CommandParser parser = null;

	/**
	 * Initializer simply gets an instance of CommandParser, which is the class
	 * responsible for parsing user commands as Strings
	 */
	public Shell() {

		parser = new CommandParser();
	}

	/**
	 * Runs the interactive shell, which takes commands one at a time and passes
	 * them off to be processed
	 */

	public void runShell() {
		int status;
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				System.out.print("\nStarCom> ");
				String nextLine = br.readLine();
				status = parser.parseCommand(nextLine);
				MessagePrinter.printStatusMessage(status, parser);
				MessagePrinter.printWarningMessages();
				// if the user typed 'exit,' quit the program
				if (status == C.SUCCESS_EXIT) {
					return;
				}
			}
		} catch (Exception e) {
			System.out.println("Internal error, terminating session");
		}
	}

	/**
	 * Either starts a shell or runs a file.
	 * 
	 * @param args Either empty, in which case we should start a shell, or has a
	 *        single argument, which should be the full path to a file to run.
	 *        Any other arguments are not correct.
	 */
	public static void main(String[] args) {
		Shell shell = new Shell();
		System.out.println("Last update = " + C.VERSION); // version is just the
															// date of the last
															// update.
		// if we get a single argument, it's a file we should try to run
		if (args.length == 1) {
			shell.parser.runFile(args[0], false);
		} else {
			shell.runShell();
		}

	}
}
