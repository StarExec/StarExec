package org.starexec.command;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

public class Shell {
	
	private Connection con;
	
	
	public Shell() {
		con=null;
	}
	
	
	
	
	private int pollJob(HashMap<String,String> commandParams) {
		int valid=Validator.isValidPollJobRequest(commandParams);
		if (valid<0) {
			return valid;
		}
		
		try {
			HashMap<String,String> infoURLParams=new HashMap<String,String>();
			HashMap<String,String> outputURLParams=new HashMap<String,String>();
			infoURLParams.put("id", commandParams.get(R.PARAM_ID));
			infoURLParams.put(R.FORMPARAM_TYPE, "job");
			
			outputURLParams.put("id", commandParams.get(R.PARAM_ID));
			outputURLParams.put(R.FORMPARAM_TYPE, "j_outputs");
			String filename=commandParams.get(R.PARAM_OUTPUT_FILE);
			String baseFileName="";
			String extension=null;
			
			for (String x : Validator.VALID_ARCHIVETYPES) {
				if (filename.endsWith(x)) {
					extension="."+x;
					baseFileName=filename.substring(0,filename.length()-x.length()-1);
				}
			}
			if (extension==null) {
				return R.ERROR_BAD_ARCHIVETYPE;
			}
			int infoCounter=1;
			int outputCounter=1;
			double interval=Double.valueOf(commandParams.get(R.PARAM_TIME))*1000;
			String nextName;
			int status;
			boolean infoDone=false;
			boolean outputDone=false;
			while (true) {
				nextName=baseFileName+"-info"+String.valueOf(infoCounter)+extension;
				commandParams.put(R.PARAM_OUTPUT_FILE, nextName);
				infoURLParams.put("since",String.valueOf(con.getJobInfoCompletion(Integer.parseInt(commandParams.get(R.PARAM_ID)))));
				status=con.downloadArchive(infoURLParams, commandParams);
				if (status!=R.SUCCESS_NOFILE) {
					infoCounter+=1;
				}
				if (status==R.SUCCESS_JOBDONE) {
					infoDone=true;
				}
				if (status<0) {
					return status;
				}
				nextName=baseFileName+"-output"+String.valueOf(outputCounter)+extension;
				commandParams.put(R.PARAM_OUTPUT_FILE, nextName);
				outputURLParams.put("since",String.valueOf(con.getJobOutCompletion(Integer.parseInt(commandParams.get(R.PARAM_ID)))));
				status=con.downloadArchive(outputURLParams, commandParams);
				if (status!=R.SUCCESS_NOFILE) {
					outputCounter+=1;
				}
				
				if (status==R.SUCCESS_JOBDONE) {
					outputDone=true;
				}
				
				if (status<0) {
					return status;
				}
				
				if (infoDone && outputDone) {
					return 0;
				}
				
				
				Thread.sleep((long)interval);
			}

		} catch (Exception e) {
			return R.ERROR_SERVER;
		}
	}
	
	/**
	 * This function handles all the commands that begin with "get"-- in other words,
	 * it handles commands to download some archive from Starexec
	 * @param c The command given by the user
	 * @param commandParams A set of parameter keys mapped to their values
	 * @return An integer status code with negative numbers indicating errors
	 * @author Eric Burns
	 */
	private int handleGetCommand(String c, HashMap<String,String> commandParams) {
		
		try {
			System.out.println("Processing your download request, please wait. This will take some time for large files");
			int serverStatus=0;
			
			HashMap<String,String> urlParams=new HashMap<String,String>();
		
			urlParams.put("id",commandParams.get("id"));
			
			if (c.equals(R.COMMAND_GETJOBOUT)) {
				urlParams.put(R.FORMPARAM_TYPE,"j_outputs");
				
			} else if (c.equals(R.COMMAND_GETJOBINFO)) {
				
				urlParams.put(R.FORMPARAM_TYPE,"job");
				
			} else if (c.equals(R.COMMAND_GETSPACEXML)) {
				urlParams.put(R.FORMPARAM_TYPE,"spaceXML");
				
			} else if (c.equals(R.COMMAND_GETSPACE)) {
				urlParams.put("hierarchy","false");
				urlParams.put(R.FORMPARAM_TYPE,"space");
				
			} else if (c.equals(R.COMMAND_GETSPACEHIERARCHY)) {
				urlParams.put("hierarchy","true");
				urlParams.put(R.FORMPARAM_TYPE,"space");
				
			} else if (c.equals(R.COMMAND_GETPOSTPROC)) {
				urlParams.put(R.FORMPARAM_TYPE,"proc");
				urlParams.put("procClass","post");
				
			} else if (c.equals(R.COMMAND_GETBENCHPROC)) {
				urlParams.put(R.FORMPARAM_TYPE,"proc");
				urlParams.put("procClass","bench");
				
			} else if (c.equals(R.COMMAND_GETBENCH)) {
				urlParams.put(R.FORMPARAM_TYPE, "bench");
				
			} else if (c.equals(R.COMMAND_GETSOLVER)) {
				urlParams.put(R.FORMPARAM_TYPE, "solver");
				
			} else if (c.equals(R.COMMAND_GETJOBPAIR)) {
				urlParams.put(R.FORMPARAM_TYPE, "jp_output");
				
			} else if (c.equals(R.COMMAND_GETNEWJOBINFO)) {
				urlParams.put(R.FORMPARAM_TYPE, "job");
				
				//Note: The reason the parameter "since" is not being taken from R.PARAM_SINCE
				//is that it is actually expected on StarExec-- it is not a command line parameter,
				//even though that parameter also happens to be "since"
				if (commandParams.containsKey(R.FORMPARAM_SINCE)) {
					urlParams.put(R.FORMPARAM_SINCE, commandParams.get(R.PARAM_SINCE));
				} else {
					urlParams.put("since",String.valueOf(con.getJobInfoCompletion(Integer.parseInt(commandParams.get(R.PARAM_ID)))));
				}
				
				
			} else if (c.equals(R.COMMAND_GETNEWJOBOUT)) {
				urlParams.put("type", "j_outputs");
				if (commandParams.containsKey("since")) {
					urlParams.put("since", commandParams.get(R.PARAM_SINCE));
				} else {
					urlParams.put("since",String.valueOf(con.getJobOutCompletion(Integer.parseInt(commandParams.get(R.PARAM_ID)))));
				}
				
			}
			
			else {
				return R.ERROR_BAD_COMMAND;
			}
			serverStatus=con.downloadArchive(urlParams,commandParams);
			if (serverStatus>=0) {
				System.out.println("Download complete");
			}
			return serverStatus;
		} catch (Exception e) {
			return R.ERROR_BAD_ARGS;
		}
		
	}
	/**
	 * Handles all commands that start with "set," indicating a command
	 * to change some setting.
	 * @param c The command given by the user
	 * @param commandParams A set of parameter keys mapped to their values
	 * @return An integer status code with negative numbers indicating errors
	 * @author Eric Burns
	 */
	private int handleSetCommand(String c, HashMap<String,String> commandParams) {
		try {
			int serverStatus=0;
			
			if (c.equals(R.COMMAND_SETARCHIVETYPE)) {
				serverStatus=con.setUserSetting("archivetype",commandParams);
			} else if(c.equals(R.COMMAND_SETFIRSTNAME)) {
				serverStatus=con.setUserSetting("firstname",commandParams);
			} else if (c.equals(R.COMMAND_SETLASTNAME)) {
				serverStatus=con.setUserSetting("lastname",commandParams);
			//} else if (c.equals(R.COMMAND_SETEMAIL)) {
			//	serverStatus=con.setUserSetting("email",parameters.get("val"));
			}  else if (c.equals(R.COMMAND_SETINSTITUTION)) {
				serverStatus=con.setUserSetting("institution",commandParams);
			} else if (c.equals(R.COMMAND_SETSPACEPUBLIC)) {
				serverStatus=con.setSpaceVisibility(commandParams, true);
			} else if (c.equals(R.COMMAND_SETSPACEPRIVATE)) {
				serverStatus=con.setSpaceVisibility(commandParams, false);
			}
			else {
				return R.ERROR_BAD_COMMAND;
			}
			return serverStatus;
		} catch (Exception e ) {
			//likely a null pointer because we are missing an important argument
			return R.ERROR_BAD_ARGS;
		}
		
	}
	
	/**
	 * Handles all commands that start with "push," which are commands
	 * for uploading things.
	 * @param c The command given by the user
	 * @param commandParams A set of parameter keys mapped to their values
	 * @return An integer status code with negative numbers indicating errors
	 * @author Eric Burns
	 */
	private int handlePushCommand(String c, HashMap<String,String> commandParams) {
		try {
			int serverStatus=0;
			
			
			if (c.equals(R.COMMAND_PUSHBENCHMARKS)) {
				
			} else if (c.equals(R.COMMAND_PUSHBENCHPROC)) {
				serverStatus=con.uploadBenchProc(commandParams);
			} else if (c.equals(R.COMMAND_PUSHPOSTPROC)) {
				serverStatus=con.uploadPostProc(commandParams);
			} else if (c.equals(R.COMMAND_PUSHSOLVER)) {
				serverStatus=con.uploadSolver(commandParams);
			}  else if (c.equals(R.COMMAND_PUSHSPACEXML)) {
				serverStatus=con.uploadSpaceXML(commandParams);
			}
			else {
				return R.ERROR_BAD_COMMAND;
			}
			
			return serverStatus;
		} catch (Exception e) {
			return R.ERROR_BAD_ARGS;
		}
	}
	
	/**
	 * Handles all commands that start with "create," which create
	 * some new information.
	 * @param c The command given by the user
	 * @param commandParams A set of parameter keys mapped to their values
	 * @return An integer status code with negative numbers indicating errors
	 * @author Eric Burns
	 */
	
	private int handleCreateCommand(String c, HashMap<String,String> commandParams){
		try {
			int serverStatus=0;
			
			
			if (c.equals(R.COMMAND_CREATEJOB)) {
				serverStatus=con.createJob(commandParams);
			} else if (c.equals(R.COMMAND_CREATESUBSPACE)) {
				serverStatus=con.createSubspace(commandParams);
			} 
			else {
				return R.ERROR_BAD_COMMAND;
			}
			
			return serverStatus;
		} catch (Exception e) {
			return R.ERROR_BAD_ARGS;
		}
	}
	
	/**
	 * Handles all commands that start with "delete," which remove
	 * things from the server.
	 * @param c The command given by the user
	 * @param commandParams A set of parameter keys mapped to their values
	 * @return An integer status code with negative numbers indicating errors
	 * @author Eric Burns
	 */
	private int handleDeleteCommand(String c, HashMap<String,String> commandParams) {
		try {
			int serverStatus=0;
			
			if (c.equals(R.COMMAND_DELETEBENCH)) {
				serverStatus=con.deleteItem(commandParams, "benchmark");
			} else if (c.equals(R.COMMAND_DELETEBENCHPROC) || c.equals(R.COMMAND_DELETEPOSTPROC)) {
				serverStatus=con.deleteItem(commandParams, "processor");
			} else if (c.equals(R.COMMAND_DELETEJOB)) {
				return R.ERROR_COMMAND_NOT_IMPLENETED;
			}  else if (c.equals(R.COMMAND_DELETESOLVER)) {
				serverStatus=con.deleteItem(commandParams,"solver");
			} else if (c.equals(R.COMMAND_DELETESPACE)) {
				return R.ERROR_COMMAND_NOT_IMPLENETED;
			} else if(c.equals(R.COMMAND_DELETECONFIG))  {
				serverStatus=con.deleteItem(commandParams, "configuration");
			}
			else {
				return R.ERROR_BAD_COMMAND;
			}
			
			return serverStatus;
		} catch (Exception e) {
			return R.ERROR_BAD_ARGS;
		}
	}
	
	/**
	 * Handles all the commands that begin with "ls," which list primitives
	 * in a given space
	 * @param c The command given by the user
	 * @param commandParams A set of parameter keys mapped to their values
	 * @return An integer status code with negative numbers indicating errors
	 * @author Eric Burns
	 */
	
	public int handleLSCommand(String c, HashMap<String,String> commandParams) {
		try {
			
			
			HashMap<String,String> urlParams=new HashMap<String,String>();
			HashMap<Integer,String> answer=new HashMap<Integer,String>();
			urlParams.put("id", commandParams.get(R.PARAM_ID));
			if (c.equals(R.COMMAND_LISTSOLVERS)) {
				urlParams.put("type", "solvers");
				
			} else if (c.equals(R.COMMAND_LISTBENCHMARKS)) {
				urlParams.put("type", "benchmarks");
				
			} else if (c.equals(R.COMMAND_LISTJOBS)) {
				urlParams.put("type","jobs");
				
			} else if(c.equals(R.COMMAND_LISTUSERS)) {
				urlParams.put("type", "users");
			} else if(c.equals(R.COMMAND_LISTSUBSPACES)) {
				urlParams.put("type","spaces");
			} else if (c.equals(R.COMMAND_LISTPRIMITIVES)) {
				String[] types=new String[] {"solvers","jobs","users","spaces"};
				for (String x : types) {
					urlParams.put("type",x);
					System.out.println(x.toUpperCase()+"\n");
					answer=con.getPrimsInSpace(urlParams,commandParams);
					if (answer.keySet().size()==1) {
						for (int test : answer.keySet()) {
							if (test<0) {
								return test;
							}
						}
					}
					printPrimitives(answer);
					System.out.print("\n");
				}
				
				return 0;
			}
			else {
				return R.ERROR_BAD_COMMAND;
			}
			answer=con.getPrimsInSpace(urlParams, commandParams);
			//if we only have 1 key and it is negative, it represents an error code
			if (answer.keySet().size()==1) {
				for (int x : answer.keySet()) {
					if (x<0) {
						return x;
					}
				}
			}
			
			//print the IDs and names of the primitives returned
			printPrimitives(answer);
			
			return 0;
		} catch (Exception e) {
			return R.ERROR_BAD_ARGS;
		}
	}
	
	private void printPrimitives(HashMap<Integer,String> prims) {
		for (int id : prims.keySet()) {
			System.out.print("id=");
			System.out.print(id);
			System.out.print(" : ");
			System.out.print("name=");
			System.out.println(prims.get(id));
		}
	}
	
	
	/**
	 * This function takes in a command issued by the user, processes it, and
	 * returns a status code indicating the outcome.
	 * @param command The string the user put into the command prompt
	 * @return a Status code where 0 is typical, greater than 0 means some sort of success,
	 * and less than 0 indicates some error
	 * @author Eric Burns
	 */
	public int parseCommand(String command) {
		command=command.trim();
		
		if (command.length()==0) {
			return 0;
		}
		
		String [] splitCommand=command.split(" ");
		
		String c=splitCommand[0].toLowerCase().trim();
		
		HashMap<String,String> commandParams=setParams(command);
		if (command.equalsIgnoreCase(R.COMMAND_EXIT)) {
			if (con!=null) {
				con.logout();
			}
			
			return R.SUCCESS_EXIT;
		} else if (c.equals(R.COMMAND_HELP)) {
			System.out.println(R.HELP_MESSAGE);
			return 0;
		} else if (c.equals(R.COMMAND_SLEEP)) {
			int valid=Validator.isValidSleepCommand(commandParams);
			if (valid<0) {
				return valid;
			}
			try {
				Thread.sleep((long)Double.parseDouble(commandParams.get(R.PARAM_TIME))*1000);
			} catch (Exception e) {
				
			}
			
			return 0;
		} else if(c.equals(R.COMMAND_LOGIN)) {
			
			//don't allow a login if we have a session already-- they should logout first
			if (con!=null) {
				return R.ERROR_CONNECTION_EXISTS;
			}
			int valid=Validator.isValidLoginRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			
			con=new Connection(commandParams);
			valid=con.login();
			if (valid<0) {
				con=null;
				return valid;
			}
			System.out.println("login successful");
			return 0;
		}	
		int status;
		if (con==null) {
			return R.ERROR_NOT_LOGGED_IN;
		}
		
		if (c.equals(R.COMMAND_LOGOUT)) {
			con.logout();
			con=null;
			System.out.println("logout successful");
			return 0;
			
		} else if (c.equals(R.COMMAND_RUNFILE)) {
			int valid=Validator.isValidRunFileRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			this.runFile(commandParams.get(R.PARAM_FILE),commandParams.containsKey(R.PARAM_VERBOSE));
			return 0;
		} else if (c.equals(R.COMMAND_POLLJOB)) {
			status=pollJob(commandParams);
		}
		else if (c.startsWith("get")) {
			status=handleGetCommand(c,commandParams);
		} else if (c.startsWith("set")) {
			status=handleSetCommand(c, commandParams);
		} else if (c.startsWith("push")) {
			status=handlePushCommand(c, commandParams);
		} else if (c.startsWith("delete")) {
			status=handleDeleteCommand(c, commandParams);
		} else if (c.startsWith("create")) {
			status=handleCreateCommand(c, commandParams);
		} else if (c.startsWith("ls")) {
			status=handleLSCommand(c, commandParams);
		}
		else {
			
			return R.ERROR_BAD_COMMAND;
		}
		//If our connection is no longer valid, attempt to get a new one and log back in
		if (con!=null && !con.isValid()) {
			con=new Connection(con.getBaseURL(),con.getUsername(),con.getPassword());
			int valid=con.login();
			if (valid<0) {
				System.out.println("Connection to server was lost");
				return R.ERROR_CONNECTION_LOST;
			}
		}
		
		return status;
	}
	
	/**
	 * Given an error code returned from a Connection, prints the error message defined
	 * in R that corresponds to that error code.
	 * @param errorCode An integer error code.
	 * @author Eric Burns
	 */
	private static void printErrorMessage(int errorCode) {
		
		System.out.print("ERROR: ");
		String message=R.errorMessages.get(errorCode);
		if (message!=null) {
			System.out.println(message);
		} else {
			System.out.println("Unknown error");
		}
		if(errorCode==R.ERROR_MISSING_PARAM) {
			System.out.println("Missing param = \"" + Validator.getMissingParam()+  "\"");
		}
	}
	
	/**
	 * Prints a warning if the user gave and unnecessary parameters that were ignored
	 */
	private static void printWarningMessages() {
		List<String> up=Validator.getUnnecessaryParams();
		if (up.size()>0) {
			System.out.print("WARNING: The following unnecessary parameters were ignored: ");
			for (String x : up) {
				System.out.print(x+" ");
			}
			System.out.print("\n");
		}
	}
	
	public void runShell() {
		int status;
		try {
			
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	       
			while (true) {
				
				System.out.print("\nStarCom> ");
				String nextLine = br.readLine();
				status=parseCommand(nextLine);
				
				if (status==R.SUCCESS_EXIT) {
					System.out.println("\nGoodbye");
					return;
				} else if (status<0) {
					if (status==R.ERROR_CONNECTION_LOST) {
						return;
					}
					printErrorMessage(status);
					
				}
				printWarningMessages();
				
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Internal error, terminating session");
			return;
		}
	}
	
	
	/**
	 * Run commands given in a file in succession
	 * @param filePath The path to a file containing a list of commands
	 * @param verbose Indicates whether to print status
	 * @author Eric Burns
	 */
	public void runFile(String filePath, boolean verbose) {
		try {
			BufferedReader br=new BufferedReader(new FileReader(filePath));
			String line=br.readLine();
			int status;
			while (line!=null) {
				if (verbose) {
					System.out.println("Processing Command: "+line);
				}
				status=parseCommand(line);
				if (status<0) {
					if (status==R.ERROR_CONNECTION_LOST) {
						return;
					}
					if (verbose) {
						printErrorMessage(status);
						
					}
				}
				if (verbose) {
					printWarningMessages();
				}
				line=br.readLine();
			}
		} catch (Exception e) {
			System.out.println("Internal error, teriminating session");
			return;
		}
	}
	
	
	
	/**
	 * This function parses a command given by the user and extracts all of the parameters and flags
	 * @param command The string given by the user at the command line
	 * @return A HashMap containing key/value pairs representing parameters input by the user
	 * @author Eric Burns
	 */
	
	private static HashMap<String,String> setParams(String command) {
		List<String> args=Arrays.asList(command.split(" "));
		args=args.subList(1, args.size());
		HashMap<String,String> answer=new HashMap<String,String>();
		int index=0;
		String x;
		StringBuilder value;
		while (index<args.size()) {
			x=args.get(index);
			int equalsIndex=x.indexOf('=');
			
			
			if (equalsIndex==-1) {
				answer.put(x.toLowerCase().trim(), null);
				index+=1;
				continue;
			}
			String key=x.substring(0,equalsIndex).toLowerCase();
			
			//we shouldn't have duplicate entries-- indicates an error
			if (answer.containsKey(key)) {
				return null;
			}
			value=new StringBuilder();
			value.append(x.substring(equalsIndex+1));
			index+=1;
			String nextString;
			while (true) {
				if (index==args.size()) {
					break;
				}
				nextString=args.get(index);
				//the next string contains the next key
				if (nextString.contains("=")) {
					break;
				} else {
					//otherwise, this string is part of the current value
					value.append(" ");
					value.append(nextString);
					index+=1;
				}
			}
			
			answer.put(key, value.toString());
		}
		return answer;
	}
	
	
	public static void main(String[] args) {
		Shell shell=new Shell();
		shell.runShell();
	}
}
	

