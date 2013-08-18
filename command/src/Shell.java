package org.starexec.command;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class Shell {
	
	
	private CommandParser parser=null;
	
	public Shell() {
		
		parser=new CommandParser();
	}
	
	
	/**
	 * Given an status code defined in R, prints the message that corresponds to that status code.
	 * @param statusCode An integer status code.
	 * @author Eric Burns
	 */
	protected static void printStatusMessage(int statusCode) {
		
		if (statusCode<0) {
			System.out.print("ERROR: ");
			String message=R.errorMessages.get(statusCode);
			if (message!=null) {
				System.out.println(message);
			} else {
				System.out.println("Unknown error");
			}
			if(statusCode==R.ERROR_MISSING_PARAM) {
				System.out.println("Missing param = \"" + Validator.getMissingParam()+  "\"");
			}
		} else if (statusCode>0) {
			String message=R.successMessages.get(statusCode);
			if (message!=null) {
				System.out.println(message);
			}
		}
		
	}
	
	/**
	 * Prints a warning if the user gave and unnecessary parameters that were ignored
	 */
	protected static void printWarningMessages() {
		List<String> up=Validator.getUnnecessaryParams();
		if (up.size()>0) {
			System.out.print("WARNING: The following unnecessary parameters were ignored: ");
			for (String x : up) {
				System.out.print(x+" ");
			}
			System.out.print("\n");
		}
	}
	
	/**
	 * Runs the interactive shell, which takes commands one at a time and passes them 
	 * off to be processed
	 */
	
	public void runShell() {
		int status;
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				
				System.out.print("\nStarCom> ");
				String nextLine = br.readLine();
				status=parser.parseCommand(nextLine);
				printStatusMessage(status);
				printWarningMessages();
				//if the user typed 'exit,' quit the program
				if (status==R.SUCCESS_EXIT) {
					return;
				} 
			}
		} catch (Exception e) {	
			System.out.println("Internal error, terminating session");
			return;
		}
	}
	
	

	public static void main(String[] args) {
		
		Shell shell=new Shell();
		System.out.println("Last update = "+R.VERSION);
		//if we get a single argument, it's a file we should try to run
		if (args.length==1) {
			shell.parser.runFile(args[0], false);
		}
		shell.runShell();
	}
}
	

