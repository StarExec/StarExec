package org.starexec.command;

/**
 * This class handles the commands given by the user at the command line
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CommandParser {
	
	private ArgumentParser parser=null;
	
	private boolean returnIDsOnUpload=false;
	private boolean printVerbosePrimDetails=false;
	protected CommandParser() {
		parser=null;
	}
	
	public String getLastServerError() {
		return parser.getLastServerError();
	}
	
	/**
	 * Prints out the given attributes
	 * @param attrs Key value pairs of strings to be printed out
	 * @param verbose If false, only looks for the "id" "name" and "description" attributes.
	 * Otherwise, prints all attributes
	 */
	private void printAttributes(Map<String,String> attrs, boolean verbose) {
		//currently prints id, name, description
		StringBuilder sb=new StringBuilder();
		
		sb.append("id= \"");
		sb.append(attrs.get("id"));
		sb.append("\"");
		
		if (!verbose) {
			if (attrs.containsKey("name")) {
				sb.append(" : name= \"");
				sb.append(attrs.get("name"));
				sb.append("\"");
			}
			if (attrs.containsKey("description")) {
				sb.append(" : description= \"");
				sb.append(attrs.get("description"));
				sb.append("\"");
			}

		} else {
			for (String key : attrs.keySet()) {
				if (key.equals("id")) {
					continue;
				}
				sb.append(" : ");
				sb.append(key);
				sb.append("= \"");
				sb.append(attrs.get(key));
				sb.append("\"");
			}
		}
		
		

		
		System.out.println(sb.toString());
		
	}
	
	/**
	 * Handles all commands that begin with "view"
	 * @param c
	 * @param commandParams
	 * @return
	 */
	
	protected int handleViewCommand(String c, HashMap<String,String> commandParams) {
		try {
			Map<String,String> attrs=null;
			if(c.equals(R.COMMAND_VIEWJOB)) {
				attrs=parser.getPrimitiveAttributes(commandParams, "job");
			} else if (c.equals(R.COMMAND_VIEWSOLVER)) {
				attrs=parser.getPrimitiveAttributes(commandParams, "solver");
			}  else if (c.equals(R.COMMAND_VIEWSPACE)) {
				attrs=parser.getPrimitiveAttributes(commandParams, "space");
			} else if (c.equals(R.COMMAND_VIEWBENCH)) {
				attrs=parser.getPrimitiveAttributes(commandParams, "benchmark");
			} else if (c.equals(R.COMMAND_VIEWPROCESSOR)) {
				attrs=parser.getPrimitiveAttributes(commandParams, "processor");
			} else if (c.equals(R.COMMAND_VIEWCONFIGURATION)) {
				attrs=parser.getPrimitiveAttributes(commandParams, "configuration");
			} else if (c.equals(R.COMMAND_VIEWQUEUE)) {
				attrs=parser.getPrimitiveAttributes(commandParams,"queue");
			}
			else {
				return Status.ERROR_BAD_COMMAND;
			}
			//if there was an error
			if (attrs.size()==1 && attrs.containsKey("-1")) {
				return Integer.parseInt(attrs.get("-1"));
			}
			printAttributes(attrs,printVerbosePrimDetails);
			
			//success
			return 0;
		} catch (Exception e ) {
			e.printStackTrace();
			//likely a null pointer because we are missing an important argument
			return Status.ERROR_INTERNAL;
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
	protected int handleSetCommand(String c, HashMap<String,String> commandParams) {
		try {
			int serverStatus=0;
			
			if(c.equals(R.COMMAND_SETFIRSTNAME)) {
				serverStatus=parser.setUserSetting("firstname",commandParams);
			} else if (c.equals(R.COMMAND_SETLASTNAME)) {
				serverStatus=parser.setUserSetting("lastname",commandParams);
			}  else if (c.equals(R.COMMAND_SETINSTITUTION)) {
				serverStatus=parser.setUserSetting("institution",commandParams);
			} else if (c.equals(R.COMMAND_SETSPACEPUBLIC)) {
				serverStatus=parser.setSpaceVisibility(commandParams, true);
			} else if (c.equals(R.COMMAND_SETSPACEPRIVATE)) {
				serverStatus=parser.setSpaceVisibility(commandParams, false);
			}
			else {
				return Status.ERROR_BAD_COMMAND;
			}
			return serverStatus;
		} catch (Exception e ) {
			//likely a null pointer because we are missing an important argument
			return Status.ERROR_INTERNAL;
		}
	}
	
	private void printID(int id) {
		System.out.println("id="+id);

	}
	
	/**
	 * Handles all commands that start with "push," which are commands
	 * for uploading things.
	 * @param c The command given by the user
	 * @param commandParams A set of parameter keys mapped to their values
	 * @return An integer status code with negative numbers indicating errors
	 * @author Eric Burns
	 */
	protected int handlePushCommand(String c, HashMap<String,String> commandParams) {
		try {
		    List<Integer> ids=null;
			int serverStatus;

			if (c.equals(R.COMMAND_PUSHBENCHMARKS)) {
				serverStatus=parser.uploadBenchmarks(commandParams);
			} else if (c.equals(R.COMMAND_PUSHBENCHPROC)) {
				serverStatus=parser.uploadBenchProc(commandParams);
			} else if (c.equals(R.COMMAND_PUSHPOSTPROC)) {
				serverStatus=parser.uploadPostProc(commandParams);
			} else if (c.equals(R.COMMAND_PUSHPREPROC)) {
				serverStatus=parser.uploadPreProc(commandParams);
			}else if (c.equals(R.COMMAND_PUSHSOLVER)) {
				serverStatus=parser.uploadSolver(commandParams);
			}  else if (c.equals(R.COMMAND_PUSHSPACEXML) || c.equals(R.COMMAND_PUSHJOBXML)) {
				boolean isJobXML= c.equals(R.COMMAND_PUSHJOBXML);
			    ids=parser.uploadXML(commandParams,isJobXML);
			    if (ids.size()==0){
			    	serverStatus=Status.ERROR_INTERNAL;
			    } else {
			    	//if the first value is positive, it is an id and we were successful. Otherwise, it is an error code
			    	serverStatus=Math.min(0, ids.get(0));
			    }
			} else if (c.equals(R.COMMAND_PUSHCONFIGRUATION)) {
				serverStatus=parser.uploadConfiguration(commandParams);
			}
			else {
				return Status.ERROR_BAD_COMMAND;
			}
			if (serverStatus>0) {
				if (returnIDsOnUpload) {
					printID(serverStatus);
				}
				return 0;
			}
			if (ids!=null && serverStatus==0 && returnIDsOnUpload) {
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
	 * Handles all commands that start with "create," which create
	 * some new information.
	 * @param c The command given by the user
	 * @param commandParams A set of parameter keys mapped to their values
	 * @return An integer status code with negative numbers indicating errors
	 * @author Eric Burns
	 */
	
	protected int handleCreateCommand(String c, HashMap<String,String> commandParams){
		try {
			int serverStatus=0;
			
			boolean isPollJob=false;
			if (c.equals(R.COMMAND_CREATEJOB)) {
				if (commandParams.containsKey(R.PARAM_TIME) || commandParams.containsKey(R.PARAM_OUTPUT_FILE)) {
					HashMap<String,String> pollParams=new HashMap<String,String>();
					isPollJob=true;
					pollParams.put(R.PARAM_TIME, commandParams.remove(R.PARAM_TIME));
					pollParams.put(R.PARAM_OUTPUT_FILE, commandParams.remove(R.PARAM_OUTPUT_FILE));
					pollParams.put(R.PARAM_ID, "1");
					if (commandParams.containsKey(R.PARAM_OVERWRITE)) {
						pollParams.put(R.PARAM_OVERWRITE, commandParams.remove(R.PARAM_OVERWRITE));
					}
					int valid=Validator.isValidPollJobRequest(pollParams);
					if (valid<0) {
						return valid;
					}
					int id=parser.createJob(commandParams);
					
					if (id<0) {
						return id;
					}
					if (returnIDsOnUpload) {
						System.out.println("id="+id);
					}
					pollParams.put(R.PARAM_ID, String.valueOf(id));
					System.out.println("Job created, polling has begun");
					serverStatus=pollJob(pollParams);
				} else {
					serverStatus=parser.createJob(commandParams);
				}
				
			} else if (c.equals(R.COMMAND_CREATESUBSPACE)) {
				serverStatus=parser.createSubspace(commandParams);
			} 
			else {
				return Status.ERROR_BAD_COMMAND;
			}
			
			if (serverStatus>0) {
				if (returnIDsOnUpload && !isPollJob) {
					System.out.println("id="+serverStatus);
				}
				return 0;
			}
			
			
			return serverStatus;
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}
	
	/**
	 * Handles all commands that start with "copy" or "mirror," which copy
	 * things on the server
	 * @param c The command given by the user
	 * @param commandParams A set of parameter keys mapped to their values
	 * @return An integer status code with negative numbers indicating errors
	 * @author Eric Burns
	 */
	protected int handleCopyCommand(String c, HashMap<String,String> commandParams) {
		try {
			int serverStatus=0;
			List<Integer> ids=null;
			if (c.equals(R.COMMAND_COPYSOLVER)) {
				ids=parser.copyPrimitives(commandParams,"solver");
				serverStatus=Math.min(0, ids.get(0));
			} else if (c.equals(R.COMMAND_LINKSOLVER)) {
				serverStatus=parser.linkPrimitives(commandParams,"solver");
			}  else if (c.equals(R.COMMAND_COPYBENCH)) {
				ids=parser.copyPrimitives(commandParams,"benchmark");
				serverStatus=Math.min(0, ids.get(0));
			} else if(c.equals(R.COMMAND_LINKBENCH))  {
				serverStatus=parser.linkPrimitives(commandParams, "benchmark");;
			} else if (c.equals(R.COMMAND_COPYSPACE)) {
				
				ids=parser.copyPrimitives(commandParams,"space");
				serverStatus=Math.min(0, ids.get(0));
			} else if (c.equals(R.COMMAND_LINKJOB)) {
				serverStatus=parser.linkPrimitives(commandParams,"job");
			} else if (c.equals(R.COMMAND_LINKUSER)) {
				serverStatus=parser.linkPrimitives(commandParams, "user");
			}
			else {
				
				return Status.ERROR_BAD_COMMAND;
			}
			if (serverStatus==0 && ids!=null && returnIDsOnUpload) {
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
	 * Handles all commands that start with "remove," which remove
	 * associations between primitives and spaces on the server
	 * @param c The command given by the user
	 * @param commandParams A set of parameter keys mapped to their values
	 * @return An integer status code with negative numbers indicating errors
	 * @author Eric Burns
	 */
	protected int handleRemoveCommand(String c, HashMap<String,String> commandParams) {
		try {
			int serverStatus=0;
			
			//the types specified below must match the types given in RESTServices.java
			if (c.equals(R.COMMAND_REMOVEBENCHMARK)) {
				serverStatus=parser.removePrimitive(commandParams, "benchmark");
			} else if (c.equals(R.COMMAND_REMOVESOLVER) || c.equals(R.COMMAND_DELETEPOSTPROC)) {
				serverStatus=parser.removePrimitive(commandParams, "solver");
			}  else if (c.equals(R.COMMAND_REMOVEUSER)) {
				serverStatus=parser.removePrimitive(commandParams,"user");
			} else if(c.equals(R.COMMAND_REMOVEJOB))  {
				serverStatus=parser.removePrimitive(commandParams, "job");
			} else if (c.equals(R.COMMAND_REMOVESUBSPACE)) {
				serverStatus=parser.removePrimitive(commandParams,"subspace");
			}
			else {
				return Status.ERROR_BAD_COMMAND;
			}
			
			return serverStatus;
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
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
	protected int handleDeleteCommand(String c, HashMap<String,String> commandParams) {
		try {
			int serverStatus=0;
			
			if (c.equals(R.COMMAND_DELETEBENCH)) {
				serverStatus=parser.deletePrimitive(commandParams, "benchmark");
			} else if (c.equals(R.COMMAND_DELETEBENCHPROC) || c.equals(R.COMMAND_DELETEPOSTPROC)) {
				serverStatus=parser.deletePrimitive(commandParams, "processor");
			} else if (c.equals(R.COMMAND_DELETESOLVER)) {
				serverStatus=parser.deletePrimitive(commandParams,"solver");
			} else if(c.equals(R.COMMAND_DELETECONFIG))  {
				serverStatus=parser.deletePrimitive(commandParams, "configuration");
			} else if (c.equals(R.COMMAND_DELETEJOB)) {
				serverStatus=parser.deletePrimitive(commandParams,"job");
			}
			else {
				return Status.ERROR_BAD_COMMAND;
			}
			
			return serverStatus;
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}
	
	/**
	 * Prints primitives to standard output in a human-readable format
	 * @param prims A HashMap mapping integer IDs to string names
	 */
	private static void printPrimitives(HashMap<Integer,String> prims) {
		for (int id : prims.keySet()) {
			System.out.print("id=");
			System.out.print(id);
			System.out.print(" : ");
			System.out.print("name=");
			System.out.println(prims.get(id));
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
	
	protected int handleLSCommand(String c, HashMap<String,String> commandParams) {
		try {
			HashMap<Integer,String> answer=new HashMap<Integer,String>();
			String type="";
			if (c.equals(R.COMMAND_LISTSOLVERS)) {
				type="solvers";
				
			} else if (c.equals(R.COMMAND_LISTBENCHMARKS)) {
				type="benchmarks";
				
			} else if (c.equals(R.COMMAND_LISTSOLVERCONFIGS)){
			    type="solverconfigs";
			}else if (c.equals(R.COMMAND_LISTJOBS)) {
				type="jobs";
				
			} else if(c.equals(R.COMMAND_LISTUSERS)) {
				type="users";
			} else if(c.equals(R.COMMAND_LISTSUBSPACES)) {
				type="spaces";
			} else if (c.equals(R.COMMAND_LISTPRIMITIVES)) {
				String[] types;
				if (commandParams.containsKey(R.PARAM_USER)) {
					types=new String[] {"solvers","benchmarks","jobs"};
				} else {
					types=new String[] {"solvers","benchmarks","jobs","users","spaces"};
				}
				for (String x : types) {
					System.out.println(x.toUpperCase()+"\n");
					answer=parser.getPrimsInSpace(type,commandParams);
					
					//this block tests to see whether the answer actually indicates an error
					if (answer.keySet().size()==1) {
						for (int test : answer.keySet()) {
							if (test<0) {
								return test;
							}
						}
					}
					CommandParser.printPrimitives(answer);
					System.out.print("\n");
				}
				
				return 0;
			}
			else {
				return Status.ERROR_BAD_COMMAND;
			}
			answer=parser.getPrimsInSpace(type,commandParams);
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
			return Status.ERROR_INTERNAL;
		}
	}
	

	/**
	 * Run commands given in a file in succession
	 * @param filePath The path to a file containing a list of commands
	 * @param verbose Indicates whether to print status
	 * @param test
	 * @return 
	 * @author Eric Burns
	 */
	protected int runFile(String filePath, boolean verbose) {
		
		try {
			BufferedReader br=new BufferedReader(new FileReader(filePath));
			String line=br.readLine();
			int status;
			while (line!=null) {
				if (verbose) {
					System.out.println("Processing Command: "+line);
				}
				status=parseCommand(line);
				if (verbose) {
				    MessagePrinter.printStatusMessage(status,this);	
				    MessagePrinter.printWarningMessages();
				}
				
				//either of the following two statuses indicate that we should stop
				//processing the file
				if (status==R.SUCCESS_EXIT) {
					return status;
				}
				if (status==Status.ERROR_CONNECTION_LOST) {
					return status;
				}
				line=br.readLine();
			}
			return 0;
		} catch (Exception e) {
			
			return Status.ERROR_COMMAND_FILE_TERMINATING;
		} 
	}
	
	protected int exit() {
		
		if (parser!=null) {
			parser.logout();
			parser=null;
		}
		return R.SUCCESS_EXIT;
	}
	
	/**
	 * This function takes in a command issued by the user, processes it, and
	 * returns a status code indicating the outcome.
	 * @param command The string the user put into the command prompt
	 * @return a Status code where 0 is typical, greater than 0 means some sort of success,
	 * and less than 0 indicates some error
	 * @author Eric Burns
	 */
	protected int parseCommand(String command) {
		//means end of input has been reached
		if (command==null) {
			command="exit";
		}
		command=command.trim();
		
		if (command.length()==0) {
			return 0;
		}
		
		String [] splitCommand=command.split(" ");
		String c=splitCommand[0].toLowerCase().trim();
		HashMap<String,String> commandParams=extractParams(command);
		if (commandParams==null) {
			return Status.ERROR_BAD_ARGS;
		}
		if (command.equalsIgnoreCase(R.COMMAND_EXIT)) {
			return exit();
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
				//do nothing-- we shouldn't ever get here
			}
			
			return 0;
		} else if(c.equals(R.COMMAND_LOGIN)) {
			
			//don't allow a login if we have a session already-- they should logout first
			if (parser!=null) {
				return Status.ERROR_CONNECTION_EXISTS;
			}
			int valid=Validator.isValidLoginRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			
			parser=new ArgumentParser(commandParams);
			valid=parser.login();
			
			//if we couldn't log in, scrap this connection and return the error code
			if (valid<0) {
				parser=null;
				return valid;
			}
			
			return R.SUCCESS_LOGIN;
		} else if (c.equals(R.COMMAND_RUNFILE)) {
			int valid=Validator.isValidRunFileRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			return this.runFile(commandParams.get(R.PARAM_FILE),commandParams.containsKey(R.PARAM_VERBOSE));
			
		} else if (c.equals(R.COMMAND_IGNOREIDS)) {
			returnIDsOnUpload=false;
			return 0;
		} else if (c.equals(R.COMMAND_RETURNIDS)) {
			returnIDsOnUpload=true;
			return 0;
		} else if (c.equals(R.COMMAND_VIEWALL)) {
			printVerbosePrimDetails=true;
		} else if (c.equals(R.COMMAND_VIEWLIMITED)) {
			printVerbosePrimDetails=false;
		}
		int status;
		if (parser==null) {
			return Status.ERROR_NOT_LOGGED_IN;
		}
		
		if (c.equals(R.COMMAND_LOGOUT)) {
			parser.logout();
			parser=null;
			
			return R.SUCCESS_LOGOUT;
			
		} else if (c.equals(R.COMMAND_POLLJOB)) {
			status=pollJob(commandParams);
		} else if (c.equals(R.COMMAND_RESUMEJOB)) {
			status=parser.resumeJob(commandParams);
		} else if (c.equals(R.COMMAND_PAUSEJOB)) {
			status=parser.pauseJob(commandParams);
		} else if (c.equals(R.COMMAND_RERUNPAIR)) {
			status=parser.rerunPair(commandParams);
		} else if (c.equals(R.COMMAND_GET_BENCH_UPLOAD_STATUS)) {
			status=parser.printBenchStatus(commandParams);
		}	else if (c.equals(R.COMMAND_RERUNJOB)) {

			status=parser.rerunJob(commandParams);
		} else if (c.startsWith("get")) {
			status=handleGetCommand(c,commandParams);
		} else if (c.startsWith("set")) {
			status=handleSetCommand(c, commandParams);
		} else if (c.startsWith("view")) {
			status=handleViewCommand(c,commandParams);
		} else if (c.startsWith("push")) {

			status=handlePushCommand(c, commandParams);
		} else if (c.startsWith("delete")) {
			status=handleDeleteCommand(c, commandParams);
		} else if (c.startsWith("create")) {
			status=handleCreateCommand(c, commandParams);
		} else if (c.startsWith("ls")) {
			status=handleLSCommand(c, commandParams);
		} else if (c.startsWith("copy") || c.startsWith("link")) {
			status=handleCopyCommand(c,commandParams);
		} else if (c.startsWith("remove")) {
			status=handleRemoveCommand(c,commandParams);
		}
		else {
			return Status.ERROR_BAD_COMMAND;
		}
		//If our connection is no longer valid, attempt to get a new one and log back in without bothering
		//the user
		if (parser!=null && !parser.isConnectionValid()) {
			
			parser.refreshConnection();
			int valid=parser.login();
			if (valid<0) {
				return Status.ERROR_CONNECTION_LOST;
			}
		}
		
		return status;
	}
	
	
	
	/**
	 * Given a comma-separated string, converts it to an array of strings
	 * with all leading and trailing whitespace removed.
	 * @param str The string to convert
	 * @return An array of strings 
	 */
	
	protected static String[] convertToArray(String str) {
		String[] ids=str.split(",");
		for (int x=0;x<ids.length;x++) {
			ids[x]=ids[x].trim();
		}
		
		return ids;
	}
	
	protected static Integer[] convertToIntArray(String str) {
		String[] ids=str.split(",");
		Integer[] answer=new Integer[ids.length];
		for (int x=0;x<ids.length;x++) {
			answer[x]=Integer.parseInt(ids[x]);
		}
		
		return answer;
	}
	
	protected static List<Integer> convertToIntList(String str) {
		String[] ids=str.split(",");
		List<Integer> answer=new ArrayList<Integer>();
		for (String s : ids) {
			answer.add(Integer.parseInt(s));
		}
		return answer;
	}
	

	/**
	 * Polls a job on StarExec, getting incremental job results until the job is completed
	 * @param commandParams Parameters given by the user at the command line
	 * @return an integer code >=0 on success and <0 on failure
	 * @author Eric Burns
	 */
	
	protected int pollJob(HashMap<String,String> commandParams) {
		int valid=Validator.isValidPollJobRequest(commandParams);
		if (valid<0) {
			return valid;
		}
		
		try {
			
			String filename=commandParams.get(R.PARAM_OUTPUT_FILE);
			String baseFileName="";
			String extension=null;
			
			//separate the extension from the name of the file
			for (String x : Validator.VALID_ARCHIVETYPES) {
				if (filename.endsWith(x)) {
					extension="."+x;
					baseFileName=filename.substring(0,filename.length()-x.length()-1);
				}
			}
			if (extension==null) {
				return Status.ERROR_BAD_ARCHIVETYPE;
			}
			int infoCounter=1;
			int outputCounter=1;
			double interval=Double.valueOf(commandParams.get(R.PARAM_TIME))*1000;
			commandParams.remove(R.PARAM_TIME);
			
			String nextName;
			int status;
			
			//only when we're done getting both types of info are we actually done
			boolean infoDone=false;
			boolean outputDone=false;
			Integer since;
			while (true) {
				nextName=baseFileName+"-info"+String.valueOf(infoCounter)+extension;
				commandParams.put(R.PARAM_OUTPUT_FILE, nextName);
				since=parser.getJobInfoCompletion(Integer.parseInt(commandParams.get(R.PARAM_ID)));
				status=parser.downloadArchive("job",since,null,null,commandParams);
				if (status!=R.SUCCESS_NOFILE) {
					infoCounter+=1;
				} else {
					System.out.println(R.successMessages.get(R.SUCCESS_NOFILE));

				}
				if (status==R.SUCCESS_JOBDONE) {
					
					infoDone=true;
				
				} else if (status<0) {
					return status;
				}
				nextName=baseFileName+"-output"+String.valueOf(outputCounter)+extension;
				commandParams.put(R.PARAM_OUTPUT_FILE, nextName);
				since=parser.getJobOutCompletion(Integer.parseInt(commandParams.get(R.PARAM_ID)));
				status=parser.downloadArchive("j_outputs",since,null,null, commandParams);
				if (status!=R.SUCCESS_NOFILE) {
					outputCounter+=1;
				} else {
					System.out.println(R.successMessages.get(R.SUCCESS_NOFILE));
				}
				
				if (status==R.SUCCESS_JOBDONE) {
					
					outputDone=true;
				}
				
				if (status<0) {
					return status;
				}
				
				//we're done with everything
				if (infoDone && outputDone) {
					System.out.println("Job done");
					return 0;
				}
				Thread.sleep((long)interval);
			}

		} catch (Exception e) {
			e.printStackTrace();
			return Status.ERROR_INTERNAL;
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
	protected int handleGetCommand(String c, HashMap<String,String> commandParams) {
		
		try {
			System.out.println("Processing your download request, please wait. This will take some time for large files");
			int serverStatus=0;
			
			String procClass=null;
			String type=null;
			Boolean hierarchy=null;
			Integer since=null;
			if (c.equals(R.COMMAND_GETJOBOUT)) {
				type="j_outputs";
			} else if (c.equals(R.COMMAND_GETJOBINFO)) {
				type="job";
			} else if (c.equals(R.COMMAND_GETSPACEXML)) {
				type="spaceXML";
				
			} else if (c.equals(R.COMMAND_GETJOBXML)){
			        type="jobXML";

			} else if (c.equals(R.COMMAND_GETSPACE)) {
				hierarchy=false;
				type="space";
				
			} else if (c.equals(R.COMMAND_GETSPACEHIERARCHY)) {
				hierarchy=true;
				type="space";
				
			} else if (c.equals(R.COMMAND_GETPOSTPROC)) {
				type="proc";
				procClass="post";
				
			} else if (c.equals(R.COMMAND_GETBENCHPROC)) {
				type="proc";
				procClass="bench";
				
			}else if (c.equals(R.COMMAND_GETPREPROC)) {
				type="proc";
				procClass="pre";
				
			} else if (c.equals(R.COMMAND_GETBENCH)) {
				type="bench";
				
			} else if (c.equals(R.COMMAND_GETSOLVER)) {
				type="solver";
				
			} else if (c.equals(R.COMMAND_GETJOBPAIR)) {
				type="jp_output";
				
			} else if (c.equals(R.COMMAND_GETJOBPAIRS)) {
				type="jp_outputs";
			} else if (c.equals(R.COMMAND_GETNEWJOBINFO)) {
				type="job";
				//Note: The reason the parameter "since" is not being taken from R.PARAM_SINCE
				//is that it is actually expected on StarExec-- it is not a command line parameter,
				//even though that parameter also happens to be "since"
				if (commandParams.containsKey(R.FORMPARAM_SINCE)) {
					since=Integer.parseInt(commandParams.get(R.PARAM_SINCE));
				} else {
					since=parser.getJobInfoCompletion(Integer.parseInt(commandParams.get(R.PARAM_ID)));
				}
			} else if (c.equals(R.COMMAND_GETNEWJOBOUT)) {
				type="j_outputs";
				if (commandParams.containsKey(R.PARAM_SINCE)) {
					since=Integer.parseInt(commandParams.get(R.PARAM_SINCE));
				} else {
					since=parser.getJobOutCompletion(Integer.parseInt(commandParams.get(R.PARAM_ID)));
				}
				
			}
			else {
				return Status.ERROR_BAD_COMMAND;
			}
			serverStatus=parser.downloadArchive(type,since,hierarchy,procClass,commandParams);
			if (serverStatus>=0) {
				System.out.println("Download complete");
			}
			return serverStatus;
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * This function parses a command given by the user and extracts all of the parameters and flags
	 * @param command The string given by the user at the command line
	 * @return A HashMap containing key/value pairs representing parameters input by the user,
	 * or null if there was a parsing error
	 * @author Eric Burns
	 */
	
	protected HashMap<String,String> extractParams(String command) {
		List<String> args=Arrays.asList(command.split(" "));
		
		//the first element is the command, which we don't want
		args=args.subList(1, args.size());
		HashMap<String,String> answer=new HashMap<String,String>();
		int index=0;
		String x;
		StringBuilder value;
		while (index<args.size()) {
			x=args.get(index);
			int equalsIndex=x.indexOf('=');
			
			//no equals sign means a parsing error, so return null
			if (equalsIndex==-1) {
				return null;
			}
			String key=x.substring(0,equalsIndex).toLowerCase();
			
			//we shouldn't have duplicate parameters-- indicates an error
			if (answer.containsKey(key)) {
				return null;
			}
			
			//the value is everything up until the next token with an equals sign
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
}
