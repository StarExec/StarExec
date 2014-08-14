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
				MessagePrinter.printStatusMessage(status,parser);
				MessagePrinter.printWarningMessages();
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
			int status=shell.parser.runFile(args[0], false,false);
		} else {
			shell.runShell();
		}
		
	}
}
	

