package org.starexec.command;

import org.starexec.constants.R;
import org.starexec.util.Util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * This class is responsible for taking in a String command given by the user through the shell interface
 * (or in a file), generating a Map of the arguments, and passing off the command to the correct function
 * in the ArgumentParser class.
 */
class CommandParser {
	final private CommandLogger log = CommandLogger.getLogger(CommandParser.class);
	final private Map<String, String> variables;
	private ArgumentParser parser = null;

	private boolean returnIDsOnUpload = false;
	private boolean printVerbosePrimDetails = false;

	protected CommandParser() {
		variables = new HashMap<>();
		parser = null;
	}

	public String getLastServerError() {
		return parser.getLastServerError();
	}

	/**
	 * Prints a given (key, value) pair
	 * @param key
	 * @param value
	 */
	private static void formatKeyValuePair(StringBuilder sb, String key, String value) {
		sb.append(key);
		sb.append("= \"");
		sb.append(value);
		sb.append("\"");
	}

	/**
	 * Attributes that should be printed even when `verbose = false`
	 * (aside from "id" which is always printed)
	 */
	final private static String[] defaultAttrs = {"name", "description"};

	/**
	 * Prints out the given attributes
	 *
	 * @param attrs Key value pairs of strings to be printed out
	 * @param verbose If false, only looks for the "id" "name" and "description"
	 *        attributes. Otherwise, prints all attributes
	 */
	private static void printAttributes(Map<String, String> attrs, boolean verbose) {
		// currently prints id, name, description
		StringBuilder sb = new StringBuilder();

		// We always want to print the id *first*
		formatKeyValuePair(sb, "id", attrs.get("id"));

		if (verbose) {                        // if verbose is on
			attrs.forEach( (key, value) -> {  // print all attrs
				if (!key.equals("id")) {      // except the id
					sb.append(" : ");         // delimited by a colon
					formatKeyValuePair(sb, key, value);
				}
			} );
		} else {                              // if verbose is not on
			for (String key : defaultAttrs) { // only print the default Attrs
				if (attrs.containsKey(key)) { // if present
					sb.append(" : ");         // delimited by a colon
					formatKeyValuePair(sb, key, attrs.get(key));
				}
			}
		}

		System.out.println(sb.toString());
	}

	/**
	 * Handles all commands that begin with "view"
	 *
	 * @param c
	 * @param commandParams
	 * @return
	 */

	protected int handleViewCommand(String c, Map<String, String> commandParams) {
		try {
			Map<String, String> attrs = null;
			switch (c) {
			case C.COMMAND_VIEWJOB:
				attrs = parser.getPrimitiveAttributes(commandParams, R.JOB);
				break;
			case C.COMMAND_VIEWSOLVER:
				attrs = parser.getPrimitiveAttributes(commandParams, R.SOLVER);
				break;
			case C.COMMAND_VIEWSPACE:
				attrs = parser.getPrimitiveAttributes(commandParams, R.SPACE);
				break;
			case C.COMMAND_VIEWBENCH:
				attrs = parser.getPrimitiveAttributes(commandParams, "benchmark");
				break;
			case C.COMMAND_VIEWPROCESSOR:
				attrs = parser.getPrimitiveAttributes(commandParams, "processor");
				break;
			case C.COMMAND_VIEWCONFIGURATION:
				attrs = parser.getPrimitiveAttributes(commandParams, "configuration");
				break;
			case C.COMMAND_VIEWQUEUE:
				attrs = parser.getPrimitiveAttributes(commandParams, "queue");
				break;
			default:
				return Status.ERROR_BAD_COMMAND;
			}
			// if there was an error
			if (attrs.size() == 1 && attrs.containsKey("-1")) {
				return Integer.parseInt(attrs.get("-1"));
			}
			printAttributes(attrs, printVerbosePrimDetails);

			// success
			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			// likely a null pointer because we are missing an important
			// argument
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * Handles all commands that start with "set," indicating a command to
	 * change some setting.
	 *
	 * @param c The command given by the user
	 * @param commandParams A set of parameter keys mapped to their values
	 * @return An integer status code with negative numbers indicating errors
	 * @author Eric Burns
	 */
	protected int handleSetCommand(String c, Map<String, String> commandParams) {
		try {
			switch (c) {
			case C.COMMAND_SETFIRSTNAME:
					return parser.setUserSetting("firstname", commandParams);
				case C.COMMAND_SETLASTNAME:
					return parser.setUserSetting("lastname", commandParams);
				case C.COMMAND_SETINSTITUTION:
					return parser.setUserSetting("institution", commandParams);
				case C.COMMAND_SETSPACEPUBLIC:
					return parser.setSpaceVisibility(commandParams, true);
				case C.COMMAND_SETSPACEPRIVATE:
					return parser.setSpaceVisibility(commandParams, false);
				default:
					return Status.ERROR_BAD_COMMAND;
			}
		} catch (Exception e) {
			// likely a null pointer because we are missing an important
			// argument
			return Status.ERROR_INTERNAL;
		}
	}

	private static void printID(int id) {
		System.out.println("id=" + id);
	}

	/**
	 * Handles all commands that start with "push," which are commands for
	 * uploading things.
	 *
	 * @param c The command given by the user
	 * @param commandParams A set of parameter keys mapped to their values
	 * @return An integer status code with negative numbers indicating errors
	 * @author Eric Burns
	 */
	protected int handlePushCommand(String c, Map<String, String> commandParams) {
		try {
			List<Integer> ids = null;
			int serverStatus;

			switch (c) {
			case C.COMMAND_PUSHBENCHMARKS:
				serverStatus = parser.uploadBenchmarks(commandParams);
				break;
			case C.COMMAND_PUSHBENCHPROC:
				serverStatus = parser.uploadBenchProc(commandParams);
				break;
			case C.COMMAND_PUSHPOSTPROC:
				serverStatus = parser.uploadPostProc(commandParams);
				break;
			case C.COMMAND_PUSHPREPROC:
				serverStatus = parser.uploadPreProc(commandParams);
				break;
			case C.COMMAND_PUSHSOLVER:
				serverStatus = parser.uploadSolver(commandParams);
				break;
			case C.COMMAND_PUSHSPACEXML:
			case C.COMMAND_PUSHJOBXML:
				boolean isJobXML = c.equals(C.COMMAND_PUSHJOBXML);
				ids = parser.uploadXML(commandParams, isJobXML);
				if (ids.isEmpty()) {
					serverStatus = Status.ERROR_INTERNAL;
				} else {
					// if the first value is positive, it is an id and we were
					// successful. Otherwise, it is an error code
					serverStatus = Math.min(0, ids.get(0));
				}
				break;
			case C.COMMAND_PUSHCONFIGRUATION:
				serverStatus = parser.uploadConfiguration(commandParams);
				break;
			default:
				return Status.ERROR_BAD_COMMAND;
			}
			if (serverStatus > 0) {
				if (returnIDsOnUpload) {
					printID(serverStatus);
				}
				return serverStatus;
			}
			if (ids != null && serverStatus == 0 && returnIDsOnUpload) {
				for (Integer id : ids) {
					printID(id);
				}
			}
			return serverStatus;
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * Handles all commands that start with "create," which create some new
	 * information.
	 *
	 * @param c The command given by the user
	 * @param commandParams A set of parameter keys mapped to their values
	 * @return An integer status code with negative numbers indicating errors
	 * @author Eric Burns
	 */

	protected int handleCreateCommand(String c, Map<String, String> commandParams) {
		try {
			int serverStatus = 0;

			boolean isPollJob = false;
			switch (c) {
			case C.COMMAND_CREATEJOB:
				if (commandParams.containsKey(C.PARAM_TIME) || commandParams.containsKey(C.PARAM_OUTPUT_FILE)) {
					Map<String, String> pollParams = new HashMap<>();
					isPollJob = true;
					pollParams.put(C.PARAM_TIME, commandParams.remove(C.PARAM_TIME));
					pollParams.put(C.PARAM_OUTPUT_FILE, commandParams.remove(C.PARAM_OUTPUT_FILE));
					pollParams.put(C.PARAM_ID, "1");
					if (commandParams.containsKey(C.PARAM_OVERWRITE)) {
						pollParams.put(C.PARAM_OVERWRITE, commandParams.remove(C.PARAM_OVERWRITE));
					}
					int valid = CommandValidator.isValidPollJobRequest(pollParams);
					if (valid < 0) {
						return valid;
					}
					int id = parser.createJob(commandParams);

					if (id < 0) {
						return id;
					}
					if (returnIDsOnUpload) {
						printID(id);
					}
					pollParams.put(C.PARAM_ID, String.valueOf(id));
					System.out.println("Job created, polling has begun");
					serverStatus = pollJob(pollParams);
				} else {
					serverStatus = parser.createJob(commandParams);
				}

				break;
			case C.COMMAND_CREATESUBSPACE:
				serverStatus = parser.createSubspace(commandParams);
				break;
			default:
				return Status.ERROR_BAD_COMMAND;
			}

			if (serverStatus > 0) {
				if (returnIDsOnUpload && !isPollJob) {
					printID(serverStatus);
				}
			}

			return serverStatus;
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * Handles all commands that start with "copy" or "mirror," which copy
	 * things on the server
	 *
	 * @param c The command given by the user
	 * @param commandParams A set of parameter keys mapped to their values
	 * @return An integer status code with negative numbers indicating errors
	 * @author Eric Burns
	 */
	protected int handleCopyCommand(String c, Map<String, String> commandParams) {
		try {
			int serverStatus = 0;
			List<Integer> ids = null;
			switch (c) {
			case C.COMMAND_COPYSOLVER:
				ids = parser.copyPrimitives(commandParams, R.SOLVER);
				serverStatus = Math.min(0, ids.get(0));
				break;
			case C.COMMAND_LINKSOLVER:
				serverStatus = parser.linkPrimitives(commandParams, R.SOLVER);
				break;
			case C.COMMAND_COPYBENCH:
				ids = parser.copyPrimitives(commandParams, "benchmark");
				serverStatus = Math.min(0, ids.get(0));
				break;
			case C.COMMAND_LINKBENCH:
				serverStatus = parser.linkPrimitives(commandParams, "benchmark");
				break;
			case C.COMMAND_COPYSPACE:
				ids = parser.copyPrimitives(commandParams, R.SPACE);
				serverStatus = Math.min(0, ids.get(0));
				break;
			case C.COMMAND_LINKJOB:
				serverStatus = parser.linkPrimitives(commandParams, R.JOB);
				break;
			case C.COMMAND_LINKUSER:
				serverStatus = parser.linkPrimitives(commandParams, "user");
				break;
			default:
				return Status.ERROR_BAD_COMMAND;
			}
			if (serverStatus == 0 && ids != null && returnIDsOnUpload) {
				for (Integer id : ids) {
					printID(id);
				}
			}

			return serverStatus;
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * Handles all commands that start with "remove," which remove associations
	 * between primitives and spaces on the server
	 *
	 * @param c The command given by the user
	 * @param commandParams A set of parameter keys mapped to their values
	 * @return An integer status code with negative numbers indicating errors
	 * @author Eric Burns
	 */
	protected int handleRemoveCommand(String c, Map<String, String> commandParams) {
		try {
			// the types specified below must match the types given in
			// RESTServices.java
			switch (c) {
			case C.COMMAND_REMOVEBENCHMARK:
				return parser.removePrimitive(commandParams, "benchmark");
			case C.COMMAND_REMOVESOLVER:
			case C.COMMAND_DELETEPOSTPROC:
				return parser.removePrimitive(commandParams, R.SOLVER);
			case C.COMMAND_REMOVEUSER:
				return parser.removePrimitive(commandParams, "user");
			case C.COMMAND_REMOVEJOB:
				return parser.removePrimitive(commandParams, R.JOB);
			case C.COMMAND_REMOVESUBSPACE:
				return parser.removePrimitive(commandParams, "subspace");
			default:
					return Status.ERROR_BAD_COMMAND;
			}
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * Handles all commands that start with "delete," which remove things from
	 * the server.
	 *
	 * @param c The command given by the user
	 * @param commandParams A set of parameter keys mapped to their values
	 * @return An integer status code with negative numbers indicating errors
	 * @author Eric Burns
	 */
	protected int handleDeleteCommand(String c, Map<String, String> commandParams) {
		try {
			switch (c) {
			case C.COMMAND_DELETEBENCH:
				return parser.deletePrimitive(commandParams, "benchmark");
			case C.COMMAND_DELETEBENCHPROC:
			case C.COMMAND_DELETEPOSTPROC:
				return parser.deletePrimitive(commandParams, "processor");
			case C.COMMAND_DELETESOLVER:
				return parser.deletePrimitive(commandParams, R.SOLVER);
			case C.COMMAND_DELETECONFIG:
				return parser.deletePrimitive(commandParams, "configuration");
			case C.COMMAND_DELETEJOB:
				return parser.deletePrimitive(commandParams, R.JOB);
			default:
				return Status.ERROR_BAD_COMMAND;
			}
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * Prints primitives to standard output in a human-readable format
	 *
	 * @param prims A Map mapping integer IDs to string names
	 */
	private static void printPrimitives(Map<Integer, String> prims) {
		for (int id : prims.keySet()) {
			System.out.print("id=");
			System.out.print(id);
			System.out.print(" : ");
			System.out.print("name=");
			System.out.println(prims.get(id));
		}
	}

	/**
	 * Handles all the commands that begin with "ls," which list primitives in a
	 * given space
	 *
	 * @param c The command given by the user
	 * @param commandParams A set of parameter keys mapped to their values
	 * @return An integer status code with negative numbers indicating errors
	 * @author Eric Burns
	 */

	protected int handleLSCommand(String c, Map<String, String> commandParams) {
		try {
			Map<Integer, String> answer;
			String type = "";
			switch (c) {
			case C.COMMAND_LISTSOLVERS:
				type = "solvers";
				break;
			case C.COMMAND_LISTBENCHMARKS:
				type = "benchmarks";
				break;
			case C.COMMAND_LISTSOLVERCONFIGS:
				type = "solverconfigs";
				break;
			case C.COMMAND_LISTJOBS:
				type = "jobs";
				break;
			case C.COMMAND_LISTUSERS:
				type = "users";
				break;
			case C.COMMAND_LISTSUBSPACES:
				type = "spaces";
				break;
			case C.COMMAND_LISTPRIMITIVES:
				String[] types;
				if (commandParams.containsKey(C.PARAM_USER)) {
					types = new String[]{"solvers", "benchmarks", "jobs"};
				} else {
					types = new String[]{"solvers", "benchmarks", "jobs", "users", "spaces"};
				}
				for (String x : types) {
					System.out.println(x.toUpperCase() + "\n");
					answer = parser.listPrimsBySpaceOrUser(type, commandParams);

					// this block tests to see whether the answer actually
					// indicates an error
					if (answer.keySet().size() == 1) {
						for (int test : answer.keySet()) {
							if (test < 0) {
								return test;
							}
						}
					}
					CommandParser.printPrimitives(answer);
					System.out.print("\n");
				}

				return 0;
			default:
				return Status.ERROR_BAD_COMMAND;
			}
			answer = parser.listPrimsBySpaceOrUser(type, commandParams);
			// if we only have 1 key and it is negative, it represents an error
			// code
			if (answer.keySet().size() == 1) {
				for (int x : answer.keySet()) {
					if (x < 0) {
						return x;
					}
				}
			}

			// print the IDs and names of the primitives returned
			printPrimitives(answer);

			return 0;
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * Run commands given in a file in succession
	 *
	 * @param filePath The path to a file containing a list of commands
	 * @param verbose Indicates whether to print status
	 * @return
	 * @author Eric Burns
	 */
	protected int runFile(String filePath, boolean verbose) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filePath));
			String line = br.readLine();
			int status;
			while (line != null) {
				if (verbose) {
					System.out.println("Processing Command: " + line);
				}
				status = parseCommand(line);
				MessagePrinter.printStatusMessage(status, this);
				MessagePrinter.printWarningMessages();

				// either of the following two statuses indicate that we should
				// stop
				// processing the file
				if (status == C.SUCCESS_EXIT) {
					return status;
				}
				if (status == Status.ERROR_CONNECTION_LOST) {
					return status;
				}
				line = br.readLine();
			}
			return 0;
		} catch (Exception e) {

			return Status.ERROR_COMMAND_FILE_TERMINATING;
		} finally {
			try {
				br.close();
			} catch (IOException e) {
			}
		}
	}

	protected int exit() {

		if (parser != null) {
			parser.logout();
			parser = null;
		}
		return C.SUCCESS_EXIT;
	}

	/**
	 * This function takes in a command issued by the user, processes it, and
	 * returns a status code indicating the outcome.
	 *
	 * @param command The string the user put into the command prompt
	 * @return a Status code where 0 is typical, greater than 0 means some sort
	 *         of success, and less than 0 indicates some error
	 * @author Eric Burns
	 */
	protected int parseCommand(String command) {
		// means end of input has been reached
		if (command == null) {
			command = C.COMMAND_EXIT;
		}
		command = command.trim();

		// Empty lines and comments are equivalent.
		if (command.isEmpty() || command.startsWith(C.COMMENT_SYMBOL)) {
			return 0;
		}

		// If the line starts with a '$', this is trying to set a variable
		if (command.charAt(0) == '$') {
			int split = command.indexOf('=');
			// If there is no '=', print the variable contents. Used for debugging
			if (split == -1) {
				String key = command.substring(1);
				String value = variables.get(key);
				System.out.println("$" + key + " = " + value);
				return Status.STATUS_SUCCESS;
			}

			// The variable name will be everything between '$' and '=' (exclusive)
			String key = command.substring(1,split - 1).trim();

			// The variable value will be the result of parsing everything
			// following the '=' as a command
			String rest = command.substring(split + 1);
			Integer value = parseCommand(rest);
			variables.put(key, value.toString());
			return value;
		}

		String[] splitCommand = command.split(" ");
		String c = splitCommand[0].toLowerCase().trim();

		// If the command itself contains an equal sign chances are the user was
		// trying to set a variable and forgot to include the '$'.
		// Warn the user about this.
		if (c.contains("=")) {
			System.out.println("Variable names must start with '$'");
		}

		Map<String, String> commandParams = extractParams(command);
		if (commandParams == null) {
			return Status.ERROR_BAD_ARGS;
		}
		if (command.equalsIgnoreCase(C.COMMAND_EXIT)) {
			return exit();
		} else if (c.equals(C.COMMAND_DEBUG)) {
			C.debugMode = !C.debugMode;
			if (C.debugMode) {
				System.out.println("Debug mode has been enabled.");
			} else {
				System.out.println("Debug mode has been disabled.");
			}
			return 0;
		} else if (c.equals(C.COMMAND_PRINT)) {
			int valid = CommandValidator.isValidPrintCommand(commandParams);
			if (valid < 0) {
				return valid;
			}
			System.out.println(commandParams.get(C.PARAM_MESSAGE));
			return 0;
		} else if (c.equals(C.COMMAND_HELP)) {
			System.out.println(C.HELP_MESSAGE);
			return 0;
		} else if (c.equals(C.COMMAND_SLEEP)) {
			int valid = CommandValidator.isValidSleepCommand(commandParams);
			if (valid < 0) {
				return valid;
			}
			try {
				Thread.sleep((long) Double.parseDouble(commandParams.get(C.PARAM_TIME)) * 1000);
			} catch (InterruptedException e) {
				// do nothing-- we shouldn't ever get here
			}

			return 0;
		} else if (c.equals(C.COMMAND_LOGIN)) {

			// don't allow a login if we have a session already-- they should
			// logout first
			if (parser != null) {
				return Status.ERROR_CONNECTION_EXISTS;
			}
			int valid = CommandValidator.isValidLoginRequest(commandParams);
			if (valid < 0) {
				return valid;
			}

			parser = new ArgumentParser(commandParams);
			valid = parser.login();

			// if we couldn't log in, scrap this connection and return the error
			// code
			if (valid < 0) {
				parser = null;
				return valid;
			}

			return C.SUCCESS_LOGIN;
		} else if (c.equals(C.COMMAND_RUNFILE)) {
			int valid = CommandValidator.isValidRunFileRequest(commandParams);
			if (valid < 0) {
				return valid;
			}
			return this.runFile(commandParams.get(C.PARAM_FILE), commandParams.containsKey(C.PARAM_VERBOSE));

		} else if (c.equals(C.COMMAND_IGNOREIDS)) {
			returnIDsOnUpload = false;
			return 0;
		} else if (c.equals(C.COMMAND_RETURNIDS)) {
			returnIDsOnUpload = true;
			return 0;
		} else if (c.equals(C.COMMAND_VIEWALL)) {
			printVerbosePrimDetails = true;
		} else if (c.equals(C.COMMAND_VIEWLIMITED)) {
			printVerbosePrimDetails = false;
		}
		int status;
		if (parser == null) {
			return Status.ERROR_NOT_LOGGED_IN;
		}

		if (c.equals(C.COMMAND_LOGOUT)) {
			parser.logout();
			parser = null;

			return C.SUCCESS_LOGOUT;

		} else if (c.equals(C.COMMAND_POLLJOB)) {
			status = pollJob(commandParams);
		} else if (c.equals(C.COMMAND_RESUMEJOB)) {
			status = parser.resumeJob(commandParams);
		} else if (c.equals(C.COMMAND_PAUSEJOB)) {
			status = parser.pauseJob(commandParams);
		} else if (c.equals(C.COMMAND_RERUNPAIR)) {
			status = parser.rerunPair(commandParams);
		} else if (c.equals(C.COMMAND_GET_BENCH_UPLOAD_STATUS)) {
			status = parser.printBenchStatus(commandParams);
		} else if (c.equals(C.COMMAND_RERUNJOB)) {

			status = parser.rerunJob(commandParams);
		} else if (c.startsWith("get")) {
			status = handleGetCommand(c, commandParams);
		} else if (c.startsWith("set")) {
			status = handleSetCommand(c, commandParams);
		} else if (c.startsWith("view")) {
			status = handleViewCommand(c, commandParams);
		} else if (c.startsWith("push")) {

			status = handlePushCommand(c, commandParams);
		} else if (c.startsWith("delete")) {
			status = handleDeleteCommand(c, commandParams);
		} else if (c.startsWith("create")) {
			status = handleCreateCommand(c, commandParams);
		} else if (c.startsWith("ls")) {
			status = handleLSCommand(c, commandParams);
		} else if (c.startsWith("copy") || c.startsWith("link")) {
			status = handleCopyCommand(c, commandParams);
		} else if (c.startsWith("remove")) {
			status = handleRemoveCommand(c, commandParams);
		} else {
			return Status.ERROR_BAD_COMMAND;
		}
		// If our connection is no longer valid, attempt to get a new one and
		// log back in without bothering
		// the user
		if (parser != null && !parser.isConnectionValid()) {

			parser.refreshConnection();
			int valid = parser.login();
			if (valid < 0) {
				return Status.ERROR_CONNECTION_LOST;
			}
		}

		return status;
	}

	protected static Integer[] convertToIntArray(String str) {
		String[] ids = str.split(",");
		Integer[] answer = new Integer[ids.length];
		for (int x = 0; x < ids.length; x++) {
			answer[x] = Integer.parseInt(ids[x]);
		}

		return answer;
	}

	protected static List<Integer> convertToIntList(String str) {
		String[] ids = str.split(",");
		List<Integer> answer = new ArrayList<>();
		for (String s : ids) {
			answer.add(Integer.parseInt(s));
		}
		return answer;
	}

	/**
	 * Polls a job on StarExec, getting incremental job results until the job is
	 * completed
	 *
	 * @param commandParams Parameters given by the user at the command line
	 * @return an integer code >=0 on success and <0 on failure
	 * @author Eric Burns
	 */

	protected int pollJob(Map<String, String> commandParams) {
		int valid = CommandValidator.isValidPollJobRequest(commandParams);
		log.log("Is valid: " + valid);
		if (valid < 0) {
			return valid;
		}

		try {

			String filename = commandParams.get(C.PARAM_OUTPUT_FILE);
			String baseFileName = "";
			String extension = null;

			// separate the extension from the name of the file
			for (String x : CommandValidator.VALID_ARCHIVETYPES) {
				if (filename.endsWith(x)) {
					extension = "." + x;
					baseFileName = filename.substring(0, filename.length() - x.length() - 1);
				}
			}
			if (extension == null) {
				return Status.ERROR_BAD_ARCHIVETYPE;
			}
			int infoCounter = 1;
			int outputCounter = 1;
			double interval = Double.valueOf(commandParams.get(C.PARAM_TIME)) * 1000;
			commandParams.remove(C.PARAM_TIME);

			String nextName;
			int status;

			// only when we're done getting both types of info are we actually
			// done
			boolean infoDone = false;
			boolean outputDone = false;
			Integer since;
			while (true) {
				nextName = baseFileName + "-info" + String.valueOf(infoCounter) + extension;
				commandParams.put(C.PARAM_OUTPUT_FILE, nextName);
				since = parser.getJobInfoCompletion(Integer.parseInt(commandParams.get(C.PARAM_ID)));
				status = parser.downloadArchive(R.JOB, since, null, null, null, commandParams);
				if (status == C.SUCCESS_NOFILE) {
					System.out.println(C.successMessages.get(C.SUCCESS_NOFILE));
				} else {
					infoCounter += 1;
				}
				if (status == C.SUCCESS_JOBDONE) {

					infoDone = true;

				} else if (status < 0) {
					log.log("Failed to downloadArchive");
					return status;
				}
				nextName = baseFileName + "-output" + String.valueOf(outputCounter) + extension;
				commandParams.put(C.PARAM_OUTPUT_FILE, nextName);
				PollJobData data = parser.getJobOutCompletion(Integer.parseInt(commandParams.get(C.PARAM_ID)));
				since = data.since;
				long lastModified = data.lastModified;
				status = parser.downloadArchive(R.JOB_OUTPUT, since, lastModified, null, null, commandParams);
				if (status == C.SUCCESS_NOFILE) {
					System.out.println(C.successMessages.get(C.SUCCESS_NOFILE));
				} else {
					outputCounter += 1;
				}

				if (status == C.SUCCESS_JOBDONE) {
					outputDone = true;
				}

				if (status < 0) {
					return status;
				}

				// we're done with everything
				if (infoDone && outputDone) {
					System.out.println("Job done");
					return 0;
				}
				try {
					Thread.sleep((long) interval);
				} catch(InterruptedException e) {
				}
			}

		} catch (Exception e) {
			log.log(Util.getStackTrace(e));
			e.printStackTrace();
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * This function handles all the commands that begin with "get"-- in other
	 * words, it handles commands to download some archive from Starexec
	 *
	 * @param c The command given by the user
	 * @param commandParams A set of parameter keys mapped to their values
	 * @return An integer status code with negative numbers indicating errors
	 * @author Eric Burns
	 */
	protected int handleGetCommand(String c, Map<String, String> commandParams) {

		try {
			int serverStatus = 0;

			String procClass = null;
			String type = null;
			Boolean hierarchy = null;
			Integer since = null;
			Long lastModified = 0L;
			switch (c) {
			case C.COMMAND_GETJOBOUT:
				type = R.JOB_OUTPUT;
				break;
			case C.COMMAND_GETJOBINFO:
				type = R.JOB;
				break;
			case C.COMMAND_GETSPACEXML:
				type = R.SPACE_XML;
				break;
			case C.COMMAND_GETJOBXML:
				type = R.JOB_XML;
				break;
			case C.COMMAND_GETSPACE:
				hierarchy = false;
				type = R.SPACE;
				break;
			case C.COMMAND_GETSPACEHIERARCHY:
				hierarchy = true;
				type = R.SPACE;
				break;
			case C.COMMAND_GETPOSTPROC:
				type = R.PROCESSOR;
				procClass = "post";
				break;
			case C.COMMAND_GETBENCHPROC:
				type = R.PROCESSOR;
				procClass = R.BENCHMARK;
				break;
			case C.COMMAND_GETPREPROC:
				type = R.PROCESSOR;
				procClass = "pre";
				break;
			case C.COMMAND_GETBENCH:
				type = R.BENCHMARK;
				break;
			case C.COMMAND_GETSOLVER:
				type = R.SOLVER;
				break;
			case C.COMMAND_GETJOBPAIR:
				type = R.PAIR_OUTPUT;
				break;
			case C.COMMAND_GETJOBPAIRS:
				type = R.JOB_OUTPUTS;
				break;
			case C.COMMAND_GETNEWJOBINFO:
				type = R.JOB;
				// Note: The reason the parameter "since" is not being taken
				// from R.PARAM_SINCE
				// is that it is actually expected on StarExec-- it is not a
				// command line parameter,
				// even though that parameter also happens to be "since"
				if (commandParams.containsKey(C.FORMPARAM_SINCE)) {
					since = Integer.parseInt(commandParams.get(C.PARAM_SINCE));
				} else {
					since = parser.getJobInfoCompletion(Integer.parseInt(commandParams.get(C.PARAM_ID)));
				}
				break;
			case C.COMMAND_GETNEWJOBOUT:
				type = R.JOB_OUTPUT;
				if (commandParams.containsKey(C.PARAM_SINCE)) {
					since = Integer.parseInt(commandParams.get(C.PARAM_SINCE));
				} else {
					PollJobData data = parser.getJobOutCompletion(Integer.parseInt(commandParams.get(C.PARAM_ID)));
					since = data.since;
					lastModified = data.lastModified;
				}

				break;
			default:
				return Status.ERROR_BAD_COMMAND;
			}
			System.out
					.println("Processing your download request, please wait. This will take some time for large files");
			serverStatus = parser.downloadArchive(type, since, lastModified, hierarchy, procClass, commandParams);
			if (serverStatus >= 0 && serverStatus != C.SUCCESS_NOFILE) {
				System.out.println("Download complete");
			}
			return serverStatus;
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * This function parses a command given by the user and extracts all of the
	 * parameters and flags
	 *
	 * @param command The string given by the user at the command line
	 * @return A Map containing key/value pairs representing parameters
	 *         input by the user, or null if there was a parsing error
	 * @author Eric Burns
	 */

	protected Map<String, String> extractParams(String command) {
		List<String> args = Arrays.asList(command.split(" "));

		// the first element is the command, which we don't want
		args = args.subList(1, args.size());
		Map<String, String> answer = new HashMap<>();
		int index = 0;
		String x;
		StringBuilder value;
		while (index < args.size()) {
			x = args.get(index);
			int equalsIndex = x.indexOf('=');

			// no equals sign means a parsing error, so return null
			if (equalsIndex == -1) {
				return null;
			}
			String key = x.substring(0, equalsIndex).toLowerCase();

			// we shouldn't have duplicate parameters-- indicates an error
			if (answer.containsKey(key)) {
				return null;
			}

			// the value is everything up until the next token with an equals
			// sign
			value = new StringBuilder();
			value.append(x.substring(equalsIndex + 1));
			index += 1;
			String nextString;
			while (true) {
				if (index == args.size()) {
					break;
				}
				nextString = args.get(index);
				// the next string contains the next key
				if (nextString.contains("=")) {
					break;
				} else {
					// otherwise, this string is part of the current value
					value.append(" ");
					value.append(nextString);
					index += 1;
				}
			}

			// If the parameter value starts with a '$', it might be a variable
			String v = value.toString();
			try {
				if (v.charAt(0) == '$') {
					String variableName = v.substring(1);
					// If our variables table contains an entry for this name
					// use that instead
					if (variables.containsKey(variableName)) {
						v = variables.get(variableName);
					}
					// If our variables table DOES NOT contain an entry for this
					// name, do nothing and just let the parameter pass through
				}
			} catch (StringIndexOutOfBoundsException e) {
				// Some parameters take no value, so `v.charAt(0)` will throw
				// (`verbose`)
			}

			answer.put(key, v);
		}
		return answer;
	}
}
