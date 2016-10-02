package org.starexec.command;


import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicNameValuePair;
import org.starexec.constants.R;
import org.starexec.data.to.Permission;
import org.starexec.util.ArchiveUtil;
import org.starexec.util.Validator;
import org.starexec.util.Util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;


/**
 * This class is responsible for communicating with the Starexec server 
 * It is designed to be a useful Starexec API, which makes it very important
 * to keep this file well documented.
 * @author Eric
 *
 */
@SuppressWarnings({"deprecation"})
public class Connection {
	final private CommandLogger log = CommandLogger.getLogger(Connection.class);
	private String baseURL;
	private String sessionID=null;
	HttpClient client=null;
	private String username,password;
	private String lastError;
	private HashMap<Integer,Integer> job_info_indices; //these two map job ids to the max completion index
	private HashMap<Integer,PollJobData> job_out_indices;
	
	
	/**
	 * Constructor used for copying the setup of one connection into a new connection. Useful if a connection
	 * gets into a bad state (possibly response streams left open due to errors)
	 * @param con The old connection to copy
	 */
	
	protected Connection(Connection con) {
		
		this.setBaseURL(con.getBaseURL());
		setUsername(con.getUsername());
		setPassword(con.getPassword());
		client=new DefaultHttpClient();

		client.getParams();
		setInfoIndices(con.getInfoIndices());
		setOutputIndices(con.getOutputIndices());
		setLastError(con.getLastError());
	}
	
	/**
	 * Sets the new Connection object's username and password based on user-specified parameters.
	 * Also sets the instance of StarExec that is being connected to
	 * @param user The username for this login
	 * @param pass The password for this login
	 * @param url the URL to the Starexec instance that we want to communicate with
	 */
	
	public Connection(String user, String pass, String url) {
		this.setBaseURL(url);
		setUsername(user);
		setPassword(pass);
		initializeComponents();
	}
	
	/**
	 * Sets the new Connection object's username and password based on user-specified parameters.
	 * The URL instance used is the default (www.starexec.org)
	 * @param user The username for this login
	 * @param pass The password for this login
	 */
	
	public Connection(String user, String pass) {
		setBaseURL(C.URL_STAREXEC_BASE);
		setUsername(user);
		setPassword(pass);
		initializeComponents();
	}
	
	/**
	 * Creates a new connection to the default StarExec instance as a guest user
	 */
	public Connection() {
		setBaseURL(C.URL_STAREXEC_BASE);
		setUsername("public");
		setPassword("public");
		initializeComponents();
	}
	private void initializeComponents() {
		client=new DefaultHttpClient();

		setInfoIndices(new HashMap<Integer,Integer>());
		setOutputIndices(new HashMap<Integer,PollJobData>());
		lastError="";
	}

	protected void setBaseURL(String baseURL1) {
		this.baseURL = baseURL1;
	}

	protected String getBaseURL() {
		return baseURL;
	}

	

	protected void setUsername(String username1) {
		this.username = username1;
	}
	/**
	 * Gets the username that is being used on this connection
	 * @return The username as a String
	 */
	protected String getUsername() {
		return username;
	}

	protected void setPassword(String password1) {
		this.password = password1;
	}
	/**
	 * Gets the password that is being used on this connection
	 * @return The password as a String
	 */
	protected String getPassword() {
		return password;
	}

	protected void setOutputIndices(HashMap<Integer,PollJobData> job_out_indices) {
		this.job_out_indices = job_out_indices;
	}

	protected HashMap<Integer,PollJobData> getOutputIndices() {
		return job_out_indices;
	}

	protected void setInfoIndices(HashMap<Integer,Integer> job_info_indices) {
		this.job_info_indices = job_info_indices;
	}

	protected HashMap<Integer,Integer> getInfoIndices() {
		return job_info_indices;
	}
	
	/**
	 * Updates the JSESSIONID of the current connection if the server has sent a new ID
	 * @param headers an array of HTTP headers
	 * @return 0 if the ID was found and changed, -1 otherwise
	 * @author Eric Burns
	 */
	
	private int setSessionIDIfExists(Header [] headers) {
		String id=HTMLParser.extractCookie(headers, C.TYPE_SESSIONID);
		if (id==null) {
			return -1;
		}
		sessionID=id;
		return 0;
	}
	
	/**
	 * @return whether the Connection object represents a valid connection to the server
	 * @author Eric Burns
	 */
	
	public boolean isValid() {
		if (sessionID==null) {
			return false;
		}
		return true;
	}
	
	/**
	 * Uploads a set of benchmarks to Starexec. The benchmarks will be expanded in a full space hierarchy.
	 * @param filePath The path to the archive containing the benchmarks
	 * @param processorID The ID of the processor that should be used on the benchmarks. If there is no such processor, this 
	 * can be null
	 * @param spaceID The ID of the space to root the new hierarchy at
	 * @param p The permission object representing permissions that should be applied to every space created when
	 * these benchmarks are uploaded
	 * @param downloadable Whether the benchmarks should be downloadable by other users.
	 * @return A positive upload ID on success, and a negative error code otherwise.
	 */
	public int uploadBenchmarksToSpaceHierarchy(String filePath,Integer processorID, Integer spaceID,Permission p, Boolean downloadable) {
		return uploadBenchmarks(filePath,processorID,spaceID,"local",p,"",downloadable,true,false,false,null);
	}
	
	/**
	 * Uploads a set of benchmarks to Starexec. The benchmarks will be expanded in a full space hierarchy.
	 * @param filePath The path to the archive containing the benchmarks
	 * @param processorID The ID of the processor that should be used on the benchmarks. If there is no such processor, this 
	 * can be null
	 * @param spaceID The ID of the space to put the benchmarks in
	 * @param downloadable Whether the benchmarks should be downloadable by other users.
	 * @return A positive upload id on success, and a negative error code otherwise.
	 */
	public int uploadBenchmarksToSingleSpace(String filePath,Integer processorID, Integer spaceID,Boolean downloadable) {
		return uploadBenchmarks(filePath,processorID,spaceID,"local",new Permission(),"",downloadable,false,false,false,null);
	}
	
	//TODO: Support dependencies for benchmarks
	
	protected int uploadBenchmarks(String filePath,Integer type,Integer spaceID, String upMethod, Permission p, String url, Boolean downloadable, Boolean hierarchy,
			Boolean dependency,Boolean linked, Integer depRoot) {
		HttpResponse response = null;
		try {
			
			HttpPost post = new HttpPost(baseURL+C.URL_UPLOADBENCHMARKS);
			MultipartEntityBuilder entity = MultipartEntityBuilder.create();
			entity.addTextBody(R.SPACE,spaceID.toString());
			entity.addTextBody("localOrURL",upMethod);
			
			//it is ok to set URL even if we don't need it
			entity.addTextBody("url",url);
			
			entity.addTextBody("download",downloadable.toString());
			entity.addTextBody("benchType",type.toString());
			entity.addTextBody("dependency",dependency.toString());
			
			entity.addTextBody("linked",linked.toString());
			if (depRoot==null) {
				entity.addTextBody("depRoot","-1");
			} else {
				entity.addTextBody("depRoot",depRoot.toString());
			}
			if (hierarchy) {
				entity.addTextBody("upMethod", "convert");
			} else {
				entity.addTextBody("upMethod","dump");
			}
			
			for (String x : p.getOnPermissions()) {
				entity.addTextBody(x,"true");
			}
			for (String x : p.getOffPermissions()) {
				entity.addTextBody(x,"false");
			}
		
			//only include the archive file if we need it
			if (upMethod.equals("local")) {
				FileBody fileBody = new FileBody(new File(filePath));
				entity.addPart("benchFile", fileBody);
			}
			
			post.setEntity(entity.build());
			post=(HttpPost) setHeaders(post);
			
			response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			int returnCode=response.getStatusLine().getStatusCode();
			
			if (returnCode==302) {
			int id=CommandValidator.getIdOrMinusOne(HTMLParser.extractCookie(response.getAllHeaders(),"New_ID"));
			if (id>0) {
				return id;
			} else {
				return Status.ERROR_INTERNAL; //we should have gotten an error from the server and no redirect if there was a catchable error
			}
			
			} else {
				setLastError(HTMLParser.extractCookie(response.getAllHeaders(), C.STATUS_MESSAGE_COOKIE));
				return Status.ERROR_SERVER;
			}
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		} finally {
			safeCloseResponse(response);
		}
	}
	/**
	 * Uploads a new configuration to an existing solver
	 * @param name The name of the new configuration
	 * @param desc A description of the configuration
	 * @param filePath The file to upload
	 * @param solverID The ID of the solver to attach the configuration to
	 * @return The ID of the new configuration on success (a positive integer), or a negative error code on failure
	 */
	public int uploadConfiguration(String name, String desc, String filePath, Integer solverID) {
		HttpResponse response = null;
		try {
			
			HttpPost post=new HttpPost(baseURL+C.URL_UPLOADCONFIG);
			if (desc==null) {
				desc="";
			}
			
			MultipartEntityBuilder entity = MultipartEntityBuilder.create();
			entity.addTextBody("solverId",solverID.toString());
			entity.addTextBody("uploadConfigDesc",desc);
			entity.addTextBody("uploadConfigName",name);
			
			FileBody fileBody = new FileBody(new File(filePath));
			entity.addPart("file", fileBody);
			post=(HttpPost) setHeaders(post);
			post.setEntity(entity.build());
			
			response=client.execute(post);
			
			setSessionIDIfExists(response.getAllHeaders());
			int id=CommandValidator.getIdOrMinusOne(HTMLParser.extractCookie(response.getAllHeaders(),"New_ID"));
			if (id<=0) {
				setLastError(HTMLParser.extractCookie(response.getAllHeaders(), C.STATUS_MESSAGE_COOKIE));
				return Status.ERROR_SERVER;
			}
			
			return id;
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		} finally {
			safeCloseResponse(response);
		}
	}
	
	/**
	 * Uploads a processor to starexec 
	 * @param name The name to give the processor
	 * @param desc A description for the processor
	 * @param filePath An absolute file path to the file to upload
	 * @param communityID The ID of the community that will be given the processor
	 * @param type Must be "post" or R.BENCHMARK
	 * @return The positive integer ID assigned the new processor on success, or a negative
	 * error code on failure
	 */
	
	protected int uploadProcessor(String name, String desc, String filePath,Integer communityID,String type) {
		File f = new File(filePath); //file is also required
		HttpResponse response = null;
		try {
			HttpPost post = new HttpPost(baseURL+C.URL_UPLOADPROCESSOR);
			MultipartEntityBuilder entity = MultipartEntityBuilder.create();
			entity.addTextBody("action","add");
			entity.addTextBody("type",type);
			entity.addTextBody("name", name);
			entity.addTextBody("desc",desc);
			entity.addTextBody("com",communityID.toString());
			entity.addTextBody("uploadMethod", "local");
			FileBody fileBody = new FileBody(f);
			entity.addPart("file", fileBody);
			
			post.setEntity(entity.build());
			post=(HttpPost) setHeaders(post);
			
			response=client.execute(post);
			
			setSessionIDIfExists(response.getAllHeaders());
						
			//we are expecting to be redirected to the page for the processor
			if (response.getStatusLine().getStatusCode()!=302) {
				setLastError(HTMLParser.extractCookie(response.getAllHeaders(), C.STATUS_MESSAGE_COOKIE));
				return Status.ERROR_SERVER;
			}
			int id=Integer.valueOf(HTMLParser.extractCookie(response.getAllHeaders(),"New_ID"));
			return id;
		} catch (Exception e) {	
			return Status.ERROR_INTERNAL;
		} finally {
			safeCloseResponse(response);
		}
		
	}
	

	/**
	 * Uploads a post processor to starexec 
	 * @param name The name to give the post processor
	 * @param desc A description for the processor
	 * @param filePath An absolute file path to the file to upload
	 * @param communityID The ID of the community that will be given the processor
	 * @return The positive integer ID assigned the new processor on success, or a negative
	 * error code on failure
	 */
	public int uploadPostProc(String name, String desc, String filePath, Integer communityID) {
		return uploadProcessor(name,desc,filePath,communityID, "post");
	}
	
	/**
	 * Uploads a benchmark processor to starexec 
	 * @param name The name to give the benchmark processor
	 * @param desc A description for the processor
	 * @param filePath An absolute file path to the file to upload
	 * @param communityID The ID of the community that will be given the processor
	 * @return The positive integer ID assigned the new processor on success, or a negative
	 * error code on failure
	 */
	public int uploadBenchProc(String name, String desc, String filePath, Integer communityID) {
		return uploadProcessor(name,desc,filePath,communityID, R.BENCHMARK);
	}
	
	/**
	 * Uploads a pre processor to starexec 
	 * @param name The name to give the pre processor
	 * @param desc A description for the processor
	 * @param filePath An absolute file path to the file to upload
	 * @param communityID The ID of the community that will be given the processor
	 * @return The positive integer ID assigned the new processor on success, or a negative
	 * error code on failure
	 */
	public int uploadPreProc(String name, String desc, String filePath, Integer communityID) {
		return uploadProcessor(name,desc,filePath,communityID, "pre");
	}
	
	

	/**
	 * Uploads an xml (job or space) to specified space
	 * @param filePath An absolute file path to the file to upload
	 * @param spaceID The ID of the space where the job is being uploaded to
	 * @param isJobUpload true if job xml upload, false otherwise
	 * @return The ids of the newly created jobs. On failure, a size 1 list with a negative error code
	 * @author Julio Cervantes
	 * @param isJobXML True if this is a job XML and false if it is a space XML
	 */
    public List<Integer> uploadXML(String filePath, Integer spaceID, boolean isJobXML) {
	    List<Integer> ids=new ArrayList<Integer>();
	    HttpResponse response = null;
		try {
		    String ext = C.URL_UPLOADSPACE;
			if(isJobXML){
			    ext = C.URL_UPLOADJOBXML;
			}
			HttpPost post=new HttpPost(baseURL+ext);
			post=(HttpPost) setHeaders(post);
			
			MultipartEntityBuilder entity = MultipartEntityBuilder.create();
			entity.addTextBody(R.SPACE,spaceID.toString());
			File f=new File(filePath);
			FileBody fileBody = new FileBody(f);
			entity.addPart("f", fileBody);
			
			post.setEntity(entity.build());
			
			response=client.execute(post);

			setSessionIDIfExists(response.getAllHeaders());
						
			int code = response.getStatusLine().getStatusCode();
			//if space, gives 200 code.  if job, gives 302
			if (code !=200 && code != 302 ) {
				setLastError(HTMLParser.extractCookie(response.getAllHeaders(), C.STATUS_MESSAGE_COOKIE));
			    ids.add(Status.ERROR_SERVER);  
				return ids;
			}
		        
			
			String[] newIds=HTMLParser.extractMultipartCookie(response.getAllHeaders(),"New_ID");
			for (String s : newIds){
				ids.add(Integer.parseInt(s));
			}
			return ids;
		} catch (Exception e) {
		    ids.add(Status.ERROR_INTERNAL);  
			return ids;
		} finally {
			safeCloseResponse(response);
		}
	    
	}
	
	/**
	 * Uploads a solver to Starexec. The description of the solver will be taken from the archive being uploaded
	 * @param name The name of the solver
	 * @param desc The description of the solver
	 * @param spaceID The ID of the space to put the solver in
	 * @param filePath the path to the solver archive to upload
	 * @param downloadable True if the solver should be downloadable by other users, and false otherwise
	 * @param runTestJob Whether to run a test job after uploading this solver
	 * @param settingId The ID of the settings profile to be used if a test job is created
	 * @param type the type of executable for this upload. See the StarexecCommand docs for a list of codes
	 * @return The ID of the new solver, which must be positive, or a negative error code
	 */
	public int uploadSolver(String name,String desc,Integer spaceID, String filePath, Boolean downloadable, Boolean runTestJob,Integer settingId, Integer type) {
		return uploadSolver(name,desc,"text",spaceID,filePath,downloadable,runTestJob,settingId, type);
	}
	/**
	 * Uploads a solver to Starexec. The description of the solver will be taken from the archive being uploaded
	 * @param name The name of the solver
	 * @param spaceID The ID of the space to put the solver in
	 * @param filePath the path to the solver archive to upload
	 * @param downloadable True if the solver should be downloadable by other users, and false otherwise
	 * @param runTestJob Whether to run a test job after uploading this solver
	 * @param settingId The ID of a settings profile to use if a test job is created
	 * @param type the type of executable for this upload. See the StarexecCommand docs for a list of codes
	 * @return The ID of the new solver, which must be positive, or a negative error code
	 */
	public int uploadSolver(String name,Integer spaceID,String filePath,Boolean downloadable,Boolean runTestJob,Integer settingId, Integer type) {
		return uploadSolver(name,"","upload",spaceID,filePath,downloadable,runTestJob,settingId, type);
	}
	
	
	/**
	 * Uploads a solver to StarexecCommand. Called by one of the overloading methods.
	 * @param entity
	 * @param post
	 * @param name
	 * @param desc
	 * @param descMethod
	 * @param spaceID
	 * @param downloadable
	 * @param runTestJob
	 * @param settingId
	 * @param type
	 * @return
	 */
	private int uploadSolver(MultipartEntityBuilder entity,HttpPost post,String name,String desc,String descMethod,
			Integer spaceID,Boolean downloadable,Boolean runTestJob,Integer settingId, Integer type) {
		HttpResponse response = null;
		try {
			//Only  include the description file if we need it
			if (descMethod.equals("file")) {
				FileBody descFileBody=new FileBody(new File(desc));
				entity.addPart("d",descFileBody);
			}
			
			entity.addTextBody("sn",name);
			if (descMethod.equals("text")) {
				entity.addTextBody("desc",desc);
			} else {
				entity.addTextBody("desc","");
			}
			entity.addTextBody(R.SPACE,spaceID.toString());
			
			entity.addTextBody("descMethod", descMethod);
			entity.addTextBody("dlable", downloadable.toString());
			entity.addTextBody("runTestJob",runTestJob.toString());
			entity.addTextBody("type", type.toString());
			if (settingId!=null) {
				entity.addTextBody("setting", settingId.toString());
	
			}
			post.setEntity(entity.build());
			post=(HttpPost) setHeaders(post);		
			response=client.execute(post);			
			setSessionIDIfExists(response.getAllHeaders());			
			int newID=CommandValidator.getIdOrMinusOne(HTMLParser.extractCookie(response.getAllHeaders(),"New_ID"));
			//if the request was not successful
			if (newID<=0) {
				setLastError(HTMLParser.extractCookie(response.getAllHeaders(), C.STATUS_MESSAGE_COOKIE));
				return Status.ERROR_SERVER;
			}
			return newID;
		} catch (Exception e) {	
			return Status.ERROR_INTERNAL;
		} finally {
			safeCloseResponse(response);
		}
		
	}
	
	/**
	 * 
	 * @param name The name of the solver
	 * @param desc If the upload method is "text", then this should be the description. If it is "file", it should
	 * be a filepath to the needed description file. If it is "upload," it is not needed
	 * @param descMethod Either "text", "upload", or "file"
	 * @param spaceID The ID of the space to put the solver in
	 * @param filePath The path to the solver archive to upload.
	 * @param downloadable True if the solver should be downloadable by other users, and false otherwise
	 * @param runTestJob Whether to run a test job after uploading this solver
	 * @param settingId The ID of the default settings profile that should be used for a test job for this job
	 * @return The ID of the new solver, which must be positive, or a negative error code
	 */
	protected int uploadSolver(String name, String desc,String descMethod,Integer spaceID,String filePath, Boolean downloadable, Boolean runTestJob,Integer settingId,Integer type) {
		try {
			HttpPost post = new HttpPost(baseURL+C.URL_UPLOADSOLVER);
			MultipartEntityBuilder entity = MultipartEntityBuilder.create();
			FileBody fileBody = new FileBody(new File(filePath));
			entity.addPart("f", fileBody);
			entity.addTextBody("url","");
			entity.addTextBody("upMethod","local");

			return uploadSolver(entity,post,name,desc,descMethod,spaceID,downloadable,runTestJob,settingId,type);
		} catch (Exception e) {	
			return Status.ERROR_INTERNAL;
		}
	}
	
	/**
	 * Uploads a solver to Starexec given a URL. 
	 * @param name The name to give the solver
	 * @param desc Either a string description OR a file path to a file containing the description, depending on the value of descMethod
	 * @param descMethod The method by which a description is being provided, which is either 'file' or 'text'
	 * @param spaceID The space to put the solver in
	 * @param url The direct URL to the solver
	 * @param downloadable Whether the solver should be downloadable or not
	 * @param runTestJob Whether to run a test job for this solver after uploading it
	 * @param settingId The ID of the settings profile that will be used if we want to run a test job\
	 * @param type the type of executable for this upload. See the StarexecCommand docs for a list of codes
	 * @return The positive ID for the solver or a negative error code
	 */
	public int uploadSolverFromURL(String name, String desc,String descMethod, Integer spaceID, String url, Boolean downloadable, Boolean runTestJob, Integer settingId, Integer type) {
		try {
			HttpPost post = new HttpPost(baseURL+C.URL_UPLOADSOLVER);
			MultipartEntityBuilder entity = MultipartEntityBuilder.create();
			entity.addTextBody("url",url);
			entity.addTextBody("upMethod","URL");

			return uploadSolver(entity,post,name,desc,descMethod,spaceID,downloadable,runTestJob,settingId, type);
		} catch (Exception e) {	
			return Status.ERROR_INTERNAL;
		}	
	}
	
	/**
	 * Uploads a solver to Starexec. The description of the solver will be taken from the archive being uploaded
	 * @param name The name of the solver
	 * @param desc The description of the solver
	 * @param spaceID The ID of the space to put the solver in
	 * @param url The URL of the archived solver to upload
	 * @param downloadable True if the solver should be downloadable by other users, and false otherwise
	 * @param runTestJob Whether to run a test job for this solver after uploading it
	 * @param settingId The ID of the settings profile that will be used if we want to run a test job
	 * @param type The type of the executable being uploaded. See Starexec Command docs for a list of codes

	 * @return The ID of the new solver, which must be positive, or a negative error code
	 */
	public int uploadSolverFromURL(String name,String desc,Integer spaceID, String url, Boolean downloadable, Boolean runTestJob,Integer settingId, Integer type) {
		return uploadSolverFromURL(name,desc,"text",spaceID,url,downloadable, runTestJob,settingId, type);
	}
	/**
	 * Uploads a solver to Starexec. The description of the solver will be taken from the archive being uploaded
	 * @param name The name of the solver
	 * @param spaceID The ID of the space to put the solver in
	 * @param url The URL of hte archived solver to upload
	 * @param downloadable True if the solver should be downloadable by other users, and false otherwise
	 * @param runTestJob Whether to run a test job for this solver after uploading it
	 * @param type The type of the executable being uploaded. See Starexec Command docs for a list of codes
	 * @param settingId The ID of the settings profile that will be used if we want to run a test job
	 * @return The ID of the new solver, which must be positive, or a negative error code
	 */
	public int uploadSolverFromURL(String name,Integer spaceID,String url,Boolean downloadable, Boolean runTestJob, Integer settingId, Integer type) {
		return uploadSolverFromURL(name,"","upload",spaceID,url,downloadable, runTestJob,settingId, type);
	}
	
	
	
	/**
	 * Sets HTTP headers required to communicate with the StarExec server
	 * @param msg --The outgoing HTTP request, likely an HttpGet or HttpPost
	 * @param cookies A list of additional cookies that may need to be added to messages
	 * @return msg with required headers added
	 * @author Eric Burns
	 */
	
	private AbstractHttpMessage setHeaders(AbstractHttpMessage msg, String[] cookies) {
		StringBuilder cookieString=new StringBuilder();
		cookieString.append("killmenothing; JSESSIONID=");
		cookieString.append(sessionID);
		cookieString.append(";");
		for (String x : cookies) {
			cookieString.append(x);
			cookieString.append(";");
		}
		msg.addHeader("Cookie",cookieString.toString());
		msg.addHeader("Connection", "keep-alive");
		msg.addHeader("Accept-Language","en-US,en;q=0.5");

		return msg;
	}
	/**
	 * Sets all the default headers StarExec needs to an HttpMessage without any cookies
	 * @param msg
	 * @return
	 */
	private AbstractHttpMessage setHeaders(AbstractHttpMessage msg) {
		return setHeaders(msg,new String[0]);
	}
	
	/**
	 * Changes your first name on StarExec to the given value
	 * @param name The new name
	 * @return 0 on success or a negative error code on failure
	 */
	
	public int setFirstName(String name) {
		return this.setUserSetting("firstname", name);
	}
	
	/**
	 * Changes your last name on StarExec to the given value
	 * @param name The new name
	 * @return 0 on success or a negative error code on failure
	 */
	public int setLastName(String name) {
		return this.setUserSetting("lastname", name);
	}
	
	/**
	 * Changes your institution on StarExec to the given value
	 * @param inst The name of the new institution
	 * @return 0 on success or a negative error code on failure
	 */
	
	public int setInstitution(String inst) {
		return this.setUserSetting("institution",inst);
	}
	
	/**
	 * Deletes all of the given solvers permanently
	 * @param ids The IDs of each solver to delete
	 * @return 0 on success or a negative error code on failure
	 */
	
	public int deleteSolvers(List<Integer> ids) {
		return deletePrimitives(ids,R.SOLVER);
	}
	
	/**
	 * Deletes all of the given benchmarks permanently
	 * @param ids The IDs of each benchmark to delete
	 * @return 0 on success or a negative error code on failure
	 */
	
	public int deleteBenchmarks(List<Integer> ids) {
		return deletePrimitives(ids,"benchmark");
	}
	
	/**
	 * Deletes all of the given processors permanently
	 * @param ids The IDs of each processor to delete
	 * @return 0 on success or a negative error code on failure
	 */
	
	public int deleteProcessors(List<Integer> ids) {
		return deletePrimitives(ids,"processor");
	}
	
	/**
	 * Deletes all of the given configurations permanently
	 * @param ids The IDs of each configuration to delete
	 * @return 0 on success or a negative error code on failure
	 */
	
	public int deleteConfigurations(List<Integer> ids) {
		return deletePrimitives(ids,"configuration");
	}
	
	/**
	 * Deletes all of the given jobs permanently
	 * @param ids The IDs of each job to delete
	 * @return 0 on success or a negative error code on failure
	 */
	public int deleteJobs(List<Integer> ids) {
		return deletePrimitives(ids,R.JOB);
	}
	
	/**
	 * Deletes all of the given primitives of the given type
	 * @param ids IDs of some primitive type
	 * @param type The type of primitives being deleted
	 * @return 0 on success or a negative error code on failure
	 */
	
	protected int deletePrimitives(List<Integer> ids, String type) {
		HttpResponse response = null;
		try {
			HttpPost post=new HttpPost(baseURL+C.URL_DELETEPRIMITIVE+"/"+type);
			post=(HttpPost) setHeaders(post);
			List<NameValuePair> params=new ArrayList<NameValuePair>();
			for (Integer id :ids) {
				params.add(new BasicNameValuePair("selectedIds[]",id.toString()));
			}
			post.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
			response=client.execute(post);
			JsonObject obj=JsonHandler.getJsonObject(response);
			boolean success=JsonHandler.getSuccessOfResponse(obj);
			String message=JsonHandler.getMessageOfResponse(obj);
			setSessionIDIfExists(response.getAllHeaders());
			
			if (success) {
				return 0;
			} else {
				setLastError(message);
				return Status.ERROR_SERVER;
			}
			
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		} finally {
			safeCloseResponse(response);
		}
		
	}
	
		/**
		 * Checks to see whether the given page can be retrieved normally, meaning we get back HTTP status code 200
		 * @param relURL The URL following starexecRoot
		 * @return True if the page was retrieved successfully and false otherwise
		 */
	public boolean canGetPage(String relURL) {
		HttpResponse response=null;
		try {
			HttpGet get=new HttpGet(baseURL+relURL);
			get=(HttpGet) setHeaders(get);
			response=client.execute(get);
			setSessionIDIfExists(get.getAllHeaders());
			
			//we should get 200, which is the code for ok
			return response.getStatusLine().getStatusCode()==200;
			
		} catch (Exception e) {
			
		} finally {
			safeCloseResponse(response);
		}
		return false;
	}
	
	/**
	 * Gets the ID of the user currently logged in to StarExec
	 * @return The integer user ID
	 */	
	public int getUserID() {
		HttpResponse response = null;
		try {
			HttpGet get=new HttpGet(baseURL+C.URL_GETID);
			get=(HttpGet) setHeaders(get);
			response=client.execute(get);
			setSessionIDIfExists(get.getAllHeaders());
			JsonElement json=JsonHandler.getJsonString(response);
			return json.getAsInt();
			
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		} finally {
			safeCloseResponse(response);
		}
	}
	/**
	 * Sets a space or hierarchy to be public or private
	 * @param spaceID The ID of the individual space or the root of the hierarchy to work on
	 * @param hierarchy True if working on a hierarchy, false if a single space 
	 * @param setPublic True if making the space(s) public, false if private
	 * @return 0 on success or a negative error code otherwise
	 */
	protected int setSpaceVisibility(Integer spaceID,Boolean hierarchy, Boolean setPublic) {
		HttpResponse response = null;
		try {
			HttpPost post=new HttpPost(baseURL+C.URL_EDITSPACEVISIBILITY+"/"+spaceID.toString() +"/" +hierarchy.toString()+"/"+setPublic.toString());
			post=(HttpPost) setHeaders(post);
			response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			
			//we should get back an HTTP OK if we're allowed to change the visibility
			if (response.getStatusLine().getStatusCode()!=200) {
				return Status.ERROR_BAD_PARENT_SPACE;
			}
			return 0;
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		} finally {
			safeCloseResponse(response);
		}
	}
	
	/**
	 * Changes one of the settings for a given user (like name, institution, or so on)
	 * @param setting The name of the setting
	 * @param val The new value for the setting
	 * @return
	 */
	
	protected int setUserSetting(String setting,String val) {
		HttpResponse response = null;
		try {	
			int userId=getUserID();
			String url=baseURL+C.URL_USERSETTING+setting+"/"+userId+"/"+val;
			url=url.replace(" ", "%20"); //encodes white space, which can't be used in a URL
			HttpPost post=new HttpPost(url);
			post=(HttpPost) setHeaders(post);
			response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			JsonObject obj=JsonHandler.getJsonObject(response);

			boolean success=JsonHandler.getSuccessOfResponse(obj);
			String message=JsonHandler.getMessageOfResponse(obj);
			if (success) {
				return 0;
			} else {
				setLastError(message);
				return Status.ERROR_SERVER;
			}
			
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		} finally {
			safeCloseResponse(response);
		}
	}
	
	/**
	 * Resumes a job on starexec that was paused previously
	 * @param jobID the ID of the job to resume running
	 * @return 0 on success or a negative error code on failure
	 */
	
	public int resumeJob(Integer jobID) {
		return pauseOrResumeJob(jobID,false);
	}
	/**
	 * Pauses a job that is currently running on starexec
	 * @param jobID The ID of the job to rerun
	 * @return 0 on success or a negative error code on failure
	 */
	
	public int pauseJob(Integer jobID) {
		return pauseOrResumeJob(jobID,true);
	}
	
	/**
	 * Reruns the job with the given ID
	 * @param jobID The ID of the job to rerun
	 * @return An integer status code as definied in Status.java
	 */
	
	public int rerunJob(Integer jobID) {
		HttpResponse response = null;
		try {
			String URL=baseURL+C.URL_RERUNJOB;
			URL=URL.replace("{id}", jobID.toString());
			HttpPost post=new HttpPost(URL);
			post=(HttpPost) setHeaders(post);
			post.setEntity(new UrlEncodedFormEntity(new ArrayList<NameValuePair>(),"UTF-8"));
			response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			JsonObject obj=JsonHandler.getJsonObject(response);

			boolean success=JsonHandler.getSuccessOfResponse(obj);
			String message=JsonHandler.getMessageOfResponse(obj);
			
			if (success) {
				return 0;
			} else {
				setLastError(message);
				return Status.ERROR_SERVER;
			}
			
		} catch (Exception e) {
			return Status.ERROR_INTERNAL; 
		} finally {
			safeCloseResponse(response);
		}
	}
	
	/**
	 * Reruns the job pair with the given ID
	 * @param pairID The ID of the pair to rerun
	 * @return A status code as defined in status.java
	 */
	
	public int rerunPair(Integer pairID) {
		HttpResponse response = null;
		try {
			String URL=baseURL+C.URL_RERUNPAIR;
			URL=URL.replace("{id}", pairID.toString());
			HttpPost post=new HttpPost(URL);
			post=(HttpPost) setHeaders(post);
			post.setEntity(new UrlEncodedFormEntity(new ArrayList<NameValuePair>(),"UTF-8"));
			response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			JsonObject obj=JsonHandler.getJsonObject(response);

			boolean success=JsonHandler.getSuccessOfResponse(obj);
			String message=JsonHandler.getMessageOfResponse(obj);

			if (success) {
				return 0;
			} else {
				setLastError(message);
				return Status.ERROR_SERVER;
			}
			
		} catch (Exception e) {
			return Status.ERROR_INTERNAL; 
		} finally {
			safeCloseResponse(response);
		}
	}
	
	/**
	 * Pauses or resumes a job depending on the value of pause
	 * @param commandParams Parameters given by the user at the command line
	 * @param pause Pauses a job if true and resumes it if false
	 * @return 0 on success or a negative error code on failure
	 */
	
	protected int pauseOrResumeJob(Integer jobID, boolean pause) {
		HttpResponse response = null;
		try {
			String URL=baseURL+C.URL_PAUSEORRESUME;
			if (pause) {
				URL=URL.replace("{method}", "pause");
			} else {
				URL=URL.replace("{method}","resume");
			}
			URL=URL.replace("{id}", jobID.toString());
			HttpPost post=new HttpPost(URL);
			post=(HttpPost) setHeaders(post);
			post.setEntity(new UrlEncodedFormEntity(new ArrayList<NameValuePair>(),"UTF-8"));
			response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			JsonObject obj=JsonHandler.getJsonObject(response);

			boolean success=JsonHandler.getSuccessOfResponse(obj);
			String message=JsonHandler.getMessageOfResponse(obj);
			
			
			if (success) {
				return 0;
			}else {
				setLastError(message);
				return Status.ERROR_SERVER;
			}
			
		} catch (Exception e) {
			return Status.ERROR_INTERNAL; 
		} finally {
			safeCloseResponse(response);
		}
	}
	
	/**
	 * Removes the given solvers from the given space. The solvers are NOT deleted.
	 * @param solverIds The IDs of the solvers to remove
	 * @param spaceID The ID of the space
	 * @return 0 on success, or a negative integer status code on failure
	 */
	public int removeSolvers(List<Integer> solverIds, Integer spaceID) {
		return removePrimitives(solverIds,spaceID,R.SOLVER,false);
	}
	
	/**
	 * Removes the given jobs from the given space. The jobs are NOT deleted.
	 * @param jobIds The IDs of the jobs to remove
	 * @param spaceID The ID of the space
	 * @return 0 on success, or a negative integer status code on failure
	 */
	
	public int removeJobs(List<Integer> jobIds, Integer spaceID) {
		return removePrimitives(jobIds,spaceID, R.JOB,false);
	}
	
	/**
	 * Removes the given users from the given space. The users are NOT deleted.
	 * @param userIds The IDs of the users to remove
	 * @param spaceID The ID of the space
	 * @return 0 on success, or a negative integer status code on failure
	 */
	
	public int removeUsers(List<Integer> userIds, Integer spaceID) {
		return removePrimitives(userIds,spaceID, "user",false);
	}
	
	/**
	 * Removes the given benchmarks from the given space. The benchmarks are NOT deleted.
	 * @param benchmarkIds The IDs of the benchmarks to remove
	 * @param spaceID The ID of the space
	 * @return 0 on success, or a negative integer status code on failure
	 */
	
	public int removeBenchmarks(List<Integer> benchmarkIds, Integer spaceID) {
		return removePrimitives(benchmarkIds, spaceID, "benchmark",false);
	}
	
	/**
	 * Removes the given subspaces from the given space.
	 * @param subspaceIds The IDs of the subspaces to remove
	 * @param spaceID The ID of the space
	 * @param recyclePrims If true, all primitives owned by the calling user that are present in any
	 * space being removed will be deleted (or moved to the recycle bin, if applicable)
	 * @return 0 on success, or a negative integer status code on failure
	 */
	
	public int removeSubspace(List<Integer> subspaceIds, Boolean recyclePrims) {
		return removePrimitives(subspaceIds, null, "subspace", recyclePrims);
	}
	
	/**
	 * Removes the association between a primitive and a space on StarExec
	 * @param commandParams Parameters given by the user
	 * @param type The type of primitive being remove
	 * @return 0 on success, and a negative error code on failure
	 * @author Eric Burns
	 */
	protected int removePrimitives(List<Integer> primIDs,Integer spaceID,String type, Boolean recyclePrims) {
		HttpResponse response = null;
		try {
			HttpPost post = null;
			if (type.equalsIgnoreCase("subspace")) {
				post=new HttpPost(baseURL+C.URL_REMOVEPRIMITIVE+"/"+type);

			} else {
				post=new HttpPost(baseURL+C.URL_REMOVEPRIMITIVE+"/"+type+"/"+spaceID.toString());

			}
			//first sets username and password data into HTTP POST request
			List<NameValuePair> params=new ArrayList<NameValuePair>();
			String key="selectedIds[]";
			for (Integer id : primIDs) {
				params.add(new BasicNameValuePair(key, id.toString()));
			}
			
			params.add(new BasicNameValuePair("recyclePrims",recyclePrims.toString()));
			post.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
			
			response=client.execute(post);
			
			setSessionIDIfExists(response.getAllHeaders());
			JsonObject obj=JsonHandler.getJsonObject(response);

			boolean success=JsonHandler.getSuccessOfResponse(obj);
			String message=JsonHandler.getMessageOfResponse(obj);
			if (success) {
				return 0;
			} else {
				setLastError(message);
				return Status.ERROR_SERVER;
			}
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		} finally {
			safeCloseResponse(response);
		}
	}
	/**
	 * Ends the current Starexec session
	 * @return True on success, false otherwise
	 * @author Eric Burns
	 */
	
	public boolean logout() {
		HttpResponse response = null;
		try {
			HttpPost post=new HttpPost(baseURL+C.URL_LOGOUT);
			post=(HttpPost) setHeaders(post);
			response=client.execute(post);
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			safeCloseResponse(response);
		}
	}
	
	/**
	 * Log into StarExec with the username and password of this connection
	 * @return An integer indicating status, with 0 being normal and a negative integer
	 * indicating an error
	 * @author Eric Burns
	 */
	public int login() {
		HttpResponse response = null;
		try {
			HttpGet get = new HttpGet(baseURL+C.URL_HOME);
			response=client.execute(get);
			sessionID=HTMLParser.extractCookie(response.getAllHeaders(),C.TYPE_SESSIONID);
			response.getEntity().getContent().close();
			if (!this.isValid()) {
				//if the user specified their own URL, it is probably the problem.
				if (!baseURL.equals(C.URL_STAREXEC_BASE)) {
					return Status.ERROR_BAD_URL;
				}
				return Status.ERROR_INTERNAL;
			}
			
			//first sets username and password data into HTTP POST request
			List<NameValuePair> params=new ArrayList<NameValuePair>(3);
			params.add(new BasicNameValuePair("j_username", username));
			params.add(new BasicNameValuePair("j_password",password));
			params.add(new BasicNameValuePair("cookieexists","false"));
			HttpPost post = new HttpPost(baseURL+C.URL_LOGIN);
			post.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
			post=(HttpPost) setHeaders(post);
			
			//Post login credentials to server
			response=client.execute(post);	
			response.getEntity().getContent().close();
			
			
			//On success, starexec will try to redirect, but we don't want that here
			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
			get = new HttpGet(baseURL+C.URL_HOME);
			get=(HttpGet) setHeaders(get);
			response=client.execute(get);
			
			sessionID=HTMLParser.extractCookie(response.getAllHeaders(),C.TYPE_SESSIONID);
			
			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, true);
			
			//this means that the server did not give us a new session for the login
			if (sessionID==null) {
				return Status.ERROR_BAD_CREDENTIALS;
			}
			return 0;
			
		} catch (IllegalStateException e) {
			
			return Status.ERROR_BAD_URL;
			
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		} finally {
			safeCloseResponse(response);
		}
		
		
	}
	/**
	 * Links solvers to a new space
	 * @param solverIds The solver Ids to be added to a new space
	 * @param oldSpaceId The space they are being linked from, or null if none exists
	 * @param newSpaceId The ID of the space they are being linked to
	 * @param hierarchy Whether to link to the entire hierarchy
	 * @return 0 on success or a negative status code on error
	 */
	
	public int linkSolvers(Integer[] solverIds, Integer oldSpaceId, Integer newSpaceId, Boolean hierarchy) {
		return linkPrimitives(solverIds,oldSpaceId,newSpaceId,hierarchy,R.SOLVER);
	}
	
	/**
	 * Links benchmark to a new space
	 * @param benchmarkIds The benchmark Ids to be added to a new space
	 * @param oldSpaceId The space they are being linked from, or null if none exists
	 * @param newSpaceId The ID of the space they are being linked to
	 * @return 0 on success or a negative status code on error
	 */
	
	public int linkBenchmarks(Integer[] benchmarkIds, Integer oldSpaceId, Integer newSpaceId) {
		return linkPrimitives(benchmarkIds,oldSpaceId,newSpaceId,false,"benchmark");
	}
	
	/**
	 * Links jobs to a new space
	 * @param jobIds The job Ids to be added to a new space
	 * @param oldSpaceId The space they are being linked from, or null if none exists
	 * @param newSpaceId The ID of the space they are being linked to
	 * @return 0 on success or a negative status code on error
	 */
	
	public int linkJobs(Integer[] jobIds, Integer oldSpaceId, Integer newSpaceId) {
		return linkPrimitives(jobIds,oldSpaceId,newSpaceId,false,R.JOB);
	}
	
	/**
	 * Links users to a new space
	 * @param userIds The user Ids to be added to a new space
	 * @param oldSpaceId The space they are being linked from, or null if none exists
	 * @param newSpaceId The ID of the space they are being linked to
	 * @return 0 on success or a negative status code on error
	 */
	public int linkUsers(Integer[] userIds, Integer oldSpaceId, Integer newSpaceId) {
		return linkPrimitives(userIds,oldSpaceId,newSpaceId,false,"user");
	}
	
	
	/**
	 * Copies solvers to a new space
	 * @param solverIds The solver Ids to be added to a new space
	 * @param oldSpaceId The space they are being linked from, or null if none exists
	 * @param newSpaceId The ID of the space they are being linked to
	 * @param hierarchy Whether to link to the entire hierarchy
	 * @return 0 on success or a negative status code on error
	 */
	
	public List<Integer> copySolvers(Integer[] solverIds, Integer oldSpaceId, Integer newSpaceId, Boolean hierarchy) {
		return copyPrimitives(solverIds,oldSpaceId,newSpaceId,hierarchy,false,R.SOLVER);
	}
	
	
	/**
	 * Copies benchmarks to a new space
	 * @param benchmarkIds The benchmark Ids to be added to a new space
	 * @param oldSpaceId The space they are being linked from, or null if none exists
	 * @param newSpaceId The ID of the space they are being linked to
	 * @return 0 on success or a negative status code on error
	 */
	
	public List<Integer> copyBenchmarks(Integer[] benchmarkIds, Integer oldSpaceId, Integer newSpaceId) {
		return copyPrimitives(benchmarkIds,oldSpaceId,newSpaceId,false,false,"benchmark");
	}
	
	/**
	 * Copies spaces to a new space
	 * @param spaceIds The space Ids to be added to a new space
	 * @param oldSpaceId The space they are being linked from, or null if none exists
	 * @param newSpaceId The ID of the space they are being linked to
	 * @param hierarchy Whether to copy every space in the hierarchies rooted at the given spaces
	 * @return 0 on success or a negative status code on error
	 */
	
	
	public List<Integer> copySpaces(Integer[] spaceIds, Integer oldSpaceId, Integer newSpaceId, Boolean hierarchy, Boolean copyPrimitives) {
		return copyPrimitives(spaceIds,oldSpaceId,newSpaceId,hierarchy,copyPrimitives,R.SPACE);
	}
	
	/**
	 * Copies all the primitives of the given types
	 * @param ids The IDs of the primitives to copy
	 * @param oldSpaceId A space where the primitives currently reside, or null if there is no old space
	 * @param newSpaceID The ID of the space to put all the primitives in
	 * @param hierarchy (only for solvers) True if copying the primitives to each space in a hierarchy (only 1 new prim is created per ID)
	 * @param type The type of the primitives
	 * @return A list of positive IDs on success, or size 1 list with a negative error code on failure
	 */
	
	protected List<Integer> copyPrimitives(Integer[] ids, Integer oldSpaceId, Integer newSpaceID, Boolean hierarchy, Boolean copyPrimitives, String type) {
		return copyOrLinkPrimitives( ids, oldSpaceId, newSpaceID, true, hierarchy, copyPrimitives, type);
	}
	
	/**
	 * Links all the primitives of the given types
	 * @param ids The IDs of the primitives to link
	 * @param oldSpaceId A space where the primitives currently reside, or null if there is no old space
	 * @param newSpaceID The ID of the space to put all the primitives in
	 * @param hierarchy (only for solvers and users) True if linking the primitives to each space in a hierarchy (only 1 new prim is created per ID)
	 * @param type The type of the primitives
	 * @return 0 on success or a negative error code on failure
	 */
	
	protected int linkPrimitives(Integer[] ids, Integer oldSpaceId, Integer newSpaceID, Boolean hierarchy, String type) {

		return copyOrLinkPrimitives( ids, oldSpaceId, newSpaceID, false, hierarchy, false, type).get(0);
	}
	
	/**
	 * Sends a copy or link request to the StarExec server and returns a status code
	 * indicating the result of the request
	 * @param commandParams The parameters given by the user at the command line.
	 * @param copy True if a copy should be performed, and false if a link should be performed.
	 * @param type The type of primitive being copied.
	 * @return An integer error code where 0 indicates success and a negative number is an error.
	 */
	private List<Integer> copyOrLinkPrimitives(Integer[] ids, Integer oldSpaceId, Integer newSpaceID, Boolean copy, Boolean hierarchy, Boolean copyPrimitives, String type) {
		List<Integer> fail=new ArrayList<Integer>();
		HttpResponse response = null;
		try {
			String urlExtension;
			if (type.equals(R.SOLVER)) {
				urlExtension=C.URL_COPYSOLVER;
			} else if (type.equals(R.SPACE)) {
				urlExtension=C.URL_COPYSPACE;
			} else if (type.equals(R.JOB)) {
				urlExtension=C.URL_COPYJOB;
			} else if (type.equals("user")) {
				urlExtension=C.URL_COPYUSER;
			}
			else {
				urlExtension=C.URL_COPYBENCH;
			}
			
			urlExtension=urlExtension.replace("{spaceID}", newSpaceID.toString());
			
			HttpPost post=new HttpPost(baseURL+urlExtension);
			
			List<NameValuePair> params=new ArrayList<NameValuePair>(3);
			
			//not all of the following are needed for every copy request, but including them does no harm
			//and allows all the copy commands to be handled by this function
			params.add(new BasicNameValuePair("copyToSubspaces", hierarchy.toString()));
			if (oldSpaceId!=null) {
				params.add(new BasicNameValuePair("fromSpace",oldSpaceId.toString()));
			}
			for (Integer id : ids) {
				params.add(new BasicNameValuePair("selectedIds[]",id.toString()));
			}
			
			params.add(new BasicNameValuePair("copy",copy.toString()));
			params.add(new BasicNameValuePair("copyHierarchy", String.valueOf(hierarchy.toString())));
            params.add(new BasicNameValuePair("copyPrimitives", String.valueOf(copyPrimitives.toString())));
			post.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
			
			post=(HttpPost) setHeaders(post);
			
			response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			JsonObject obj=JsonHandler.getJsonObject(response);

			boolean success=JsonHandler.getSuccessOfResponse(obj);
			String message=JsonHandler.getMessageOfResponse(obj);
			if (success) {
				List<Integer> newPrimIds=new ArrayList<Integer>();
				String[] newIds=HTMLParser.extractMultipartCookie(response.getAllHeaders(),"New_ID");
				if (newIds!=null) {
					for (String s : newIds){
						newPrimIds.add(Integer.parseInt(s));
					}
					
				} else {
					newPrimIds.add(0);
				}
				return newPrimIds;
			} else {
				setLastError(message);
				fail.add(Status.ERROR_SERVER);
				return fail;
			}
		} catch (Exception e) {
			fail.add(Status.ERROR_INTERNAL);
			return fail;
		} finally {
			safeCloseResponse(response);
		}
	}
	
	/**
	 * Creates a subspace of an existing space on StarExec
	 * @param name The name to give the new space
	 * @param desc The description to give the new space
	 * @param parentSpaceID The ID of the parent space for the new space
	 * @param p The permission object reflecting all the default permissions for the new space
	 * @param locked Whether the space should be locked initially
	 * @return the new space ID on success and a negative error code otherwise
	 * @author Eric Burns
	 */
	
	public int createSubspace(String name, String desc,Integer parentSpaceID, Permission p, Boolean locked) {
		HttpResponse response = null;
		try {
			//first sets username and password data into HTTP POST request
			List<NameValuePair> params=new ArrayList<NameValuePair>(3);
			params.add(new BasicNameValuePair("parent", parentSpaceID.toString()));
			params.add(new BasicNameValuePair("name",name));
			params.add(new BasicNameValuePair("desc",desc));
			params.add(new BasicNameValuePair("locked",locked.toString()));
			
			for (String x : p.getOnPermissions()) {
				params.add(new BasicNameValuePair(x,"on"));
				
			}
			
			HttpPost post = new HttpPost(baseURL+C.URL_ADDSPACE);
			post.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
			post=(HttpPost) setHeaders(post);
			
			response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());			
			if (response.getStatusLine().getStatusCode()!=302) {
				return Status.ERROR_BAD_PARENT_SPACE;
			}
			int newID=Integer.valueOf(HTMLParser.extractCookie(response.getAllHeaders(),"New_ID"));
			return newID;
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		} finally {
			safeCloseResponse(response);
		}
	}
	
	/**
	 * Gets a HashMap that maps the IDs of solvers to their names for all solvers in the given
	 * space
	 * @param spaceID The ID of the space
	 * @return A HashMap mapping IDs to names If there was an error, the HashMap will contain only one key, and it will
	 * be negative, whereas all IDs must be positive.
	 */
	
	public HashMap<Integer,String> getSolversInSpace(Integer spaceID) {
		return listPrims(spaceID, null, false, "solvers");
	}
	/**
	 * Gets a HashMap that maps the IDs of solvers to their names for all benchmarks in the given
	 * space
	 * @param spaceID The ID of the space
	 * @return A HashMap mapping IDs to names If there was an error, the HashMap will contain only one key, and it will
	 * be negative, whereas all IDs must be positive.
	 */
	
	public HashMap<Integer,String> getBenchmarksInSpace(Integer spaceID) {
		return listPrims(spaceID, null, false, "benchmarks");
	}
	/**
	 * Gets a HashMap that maps the IDs of solvers to their names for all jobs in the given
	 * space
	 * @param spaceID The ID of the space
	 * @return A HashMap mapping IDs to names If there was an error, the HashMap will contain only one key, and it will
	 * be negative, whereas all IDs must be positive.
	 */
	
	public HashMap<Integer,String> getJobsInSpace(Integer spaceID) {
		return listPrims(spaceID, null, false, "jobs");
	}
	/**
	 * Gets a HashMap that maps the IDs of solvers to their names for all users in the given
	 * space
	 * @param spaceID The ID of the space
	 * @return A HashMap mapping IDs to names If there was an error, the HashMap will contain only one key, and it will
	 * be negative, whereas all IDs must be positive.
	 */
	
	public HashMap<Integer,String> getUsersInSpace(Integer spaceID) {
		return listPrims(spaceID, null, false, "users");
	}
	/**
	 * Gets a HashMap that maps the IDs of solvers to their names for all spaces in the given
	 * space
	 * @param spaceID The ID of the space
	 * @return A HashMap mapping IDs to names If there was an error, the HashMap will contain only one key, and it will
	 * be negative, whereas all IDs must be positive.
	 */
	
	public HashMap<Integer,String> getSpacesInSpace(Integer spaceID) {
		return listPrims(spaceID, null, false, "spaces");
	}
	/**
	 * Gets a HashMap that maps the IDs of solvers to their names for all solvers the current user owns
	 * @return A HashMap mapping IDs to names If there was an error, the HashMap will contain only one key, and it will
	 * be negative, whereas all IDs must be positive.
	 */
	
	public HashMap<Integer,String> getSolversByUser() {
		return listPrims(null, null, true, "solvers");
	}
	/**
	 * Gets a HashMap that maps the IDs of solvers to their names for all benchmarks the current user owns
	 * @return A HashMap mapping IDs to names If there was an error, the HashMap will contain only one key, and it will
	 * be negative, whereas all IDs must be positive.
	 */
	
	public HashMap<Integer,String> getBenchmarksByUser() {
		return listPrims(null, null, true, "benchmarks");
	}
	/**
	 * Gets a HashMap that maps the IDs of solvers to their names for all jobs the current user owns
	 * @return A HashMap mapping IDs to names If there was an error, the HashMap will contain only one key, and it will
	 * be negative, whereas all IDs must be positive.
	 */
	public HashMap<Integer,String> getJobsByUser() {
		return listPrims(null, null, true, "jobs");
	}

	/**
	 * @param solverID Integer id of a solver
	 * @param limit Integer limiting number of configurations displayed
	 * @return A HashMap mapping IDs to names If there was an error, the HashMap will contain only one key, and it will
	 * be negative, whereas all IDs must be positive.
	 */
    protected HashMap<Integer,String> getSolverConfigs(Integer solverID, Integer limit){
	HashMap<Integer,String> errorMap=new HashMap<Integer,String>();
	HashMap<Integer,String> prims=new HashMap<Integer,String>();
	try{
	    String serverURL = baseURL+C.URL_GETSOLVERCONFIGS;
	    
	    HttpPost post=new HttpPost(serverURL);
	    post=(HttpPost) setHeaders(post);

		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair("solverid",solverID.toString()));
		if(limit != null){
		    urlParameters.add(new BasicNameValuePair("limit",limit.toString()));
		}
		else{
		    // -1 is a null value
		    urlParameters.add(new BasicNameValuePair("limit","-1"));
		}
		post.setEntity(new UrlEncodedFormEntity(urlParameters));
	   
	    HttpResponse response=client.execute(post);
			
	    setSessionIDIfExists(response.getAllHeaders());
			
	    
	    final BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
	    
	    String line;
	    while((line = br.readLine()) != null){
	    	System.out.println(line);
		
	    }

	    int code = response.getStatusLine().getStatusCode();
	    //if space, gives 200 code.  if job, gives 302
	    if (code !=200 && code != 302 ) {
		System.out.println("Connection.java : "+code + " " +response.getStatusLine().getReasonPhrase());
				
	    }
		        
			
			
	    return prims;
		}catch (Exception e) {
		    errorMap.put(Status.ERROR_INTERNAL, e.getMessage());
			
			return errorMap;
		}
    }
	/**
	 * Lists the IDs and names of some kind of primitives in a given space
	 * @param urlParams Parameters to be encoded into the URL to send to the server
	 * @param commandParams Parameters given by the user at the command line
	 * @return A HashMap mapping integer ids to string names
	 * @author Eric Burns
	 */
	protected HashMap<Integer,String> listPrims(Integer spaceID, Integer limit, boolean forUser, String type) {
		HashMap<Integer,String> errorMap=new HashMap<Integer,String>();
		HashMap<Integer,String> prims=new HashMap<Integer,String>();
		HttpResponse response = null;
		try {
		    
			
			HashMap<String,String> urlParams=new HashMap<String,String>();
			
			urlParams.put(C.FORMPARAM_TYPE, type);
		
			String URL=null;
			if (forUser) {
				int id=getUserID();
				if (id<0) {
					errorMap.put(id, null);
					return errorMap;
				}
				
				urlParams.put(C.FORMPARAM_ID, String.valueOf(id));
				URL=baseURL+C.URL_GETUSERPRIM;
			} else {
				urlParams.put(C.FORMPARAM_ID, spaceID.toString());
				URL=baseURL+C.URL_GETPRIM;
			}
			//in the absence of limit, we want all the primitives
			int maximum=Integer.MAX_VALUE;
			if (limit!=null) {
				maximum=limit;
			}
			
			//need to specify the number of columns according to what GetNextPageOfPrimitives in RESTHelpers
			//expects
			String columns="0";
			if (type.equals("solvers")) {
				columns="2"; 
			} else if (type.equals("users")) {
				columns="3";
			} else if (type.equals("benchmarks")) {
				columns="2";
			} else if (type.equals("jobs")) {
				columns="6";
			} else if (type.equals("spaces")) {
				columns="2";
			}
			
			
			URL=URL.replace("{id}", urlParams.get(C.PARAM_ID));
			URL=URL.replace("{type}", urlParams.get("type"));
			HttpPost post=new HttpPost(URL);
			post=(HttpPost) setHeaders(post);
			
			List<NameValuePair> params=new ArrayList<NameValuePair>();
			
			params.add(new BasicNameValuePair("sEcho", "1"));
			params.add(new BasicNameValuePair("iColumns",columns));
			params.add(new BasicNameValuePair("sColumns",""));
			params.add(new BasicNameValuePair("iDisplayStart","0"));
			
			params.add(new BasicNameValuePair("iDisplayLength",String.valueOf(maximum)));
			params.add(new BasicNameValuePair("iSortCol_0","0"));
			params.add(new BasicNameValuePair("sSearch",""));
			params.add(new BasicNameValuePair("sSortDir_0","asc"));
			
			post.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
			
			response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			
			JsonElement jsonE=JsonHandler.getJsonString(response);
			JsonObject obj=jsonE.getAsJsonObject();
			String message=JsonHandler.getMessageOfResponse(obj);
			
			//if we got back a ValidatorStatusCode, there was an error
			if (message!=null) {
				setLastError(message);
				errorMap.put(Status.ERROR_SERVER, null);
				return errorMap;
			}
			
			//we should get back a jsonObject which has a jsonArray of primitives labeled 'aaData'
			JsonArray json=jsonE.getAsJsonObject().get("aaData").getAsJsonArray();
			JsonArray curPrim;
			String name;
			Integer id;
			JsonPrimitive element;
			
			//the array is itself composed of arrays, with each array containing multiple elements
			//describing a single primitive
			for (JsonElement x : json) {
				curPrim=x.getAsJsonArray();
				for (JsonElement y : curPrim) {
					element=y.getAsJsonPrimitive();
					
					id=HTMLParser.extractIDFromJson(element.getAsString());
					name=HTMLParser.extractNameFromJson(element.getAsString(),urlParams.get("type"));
					
					//if the element has an ID and a name, save them
					if (id!=null && name!=null) {
						prims.put(id, name);
					}
					
				}
				
			}
			
			return prims;
		
		} catch (Exception e) {
		    errorMap.put(Status.ERROR_INTERNAL, e.getMessage());
			
			return errorMap;
		} finally {
			safeCloseResponse(response);
		}
	}
	/**
	 * Downloads a solver from StarExec in the form of a zip file
	 * @param solverId The ID of the solver to download
 	 * @param filePath The output path where the file will be saved
	 * @return A status code as defined in the Status class
	 */
	public int downloadSolver(Integer solverId, String filePath) {
		return downloadArchive(solverId, R.SOLVER,null,null,filePath,false,false,false,false,null,false,false,null,false);
	}
	/**
	 * Downloads job pair output for one pair from StarExec in the form of a zip file
	 * @param pairId The ID of the pair to download
 	 * @param filePath The output path where the file will be saved
	 * @return A status code as defined in the Status class
	 */
	public int downloadJobPair(Integer pairId, String filePath, Boolean longPath) {
		return downloadArchive(pairId,R.PAIR_OUTPUT,null,null,filePath,false,false,false,false,null,false,false,null,longPath);
	}
	
	/**
	 * Downloads a list of job pairs
	 * @param pairIds The IDs of all the pairs that should be downloaded
	 * @param filePath Absolute file path to the spot that an archive containing all the given pairs should be placed
	 * @return A status code as defined in status.java
	 */
	public int downloadJobPairs(List<Integer> pairIds, String filePath) {
		HttpResponse response=null;
		try {
			HashMap<String,String> urlParams=new HashMap<String,String>();
			urlParams.put(C.FORMPARAM_TYPE, R.JOB_OUTPUTS);
			StringBuilder sb=new StringBuilder();
			for (Integer id : pairIds) {
				sb.append(id);
				sb.append(",");
			}
			String ids=sb.substring(0,sb.length()-1);
			
			urlParams.put(C.FORMPARAM_ID+"[]",ids);
			
			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
			//First, put in the request for the server to generate the desired archive			
			
			HttpGet get=new HttpGet(HTMLParser.URLEncode(baseURL+C.URL_DOWNLOAD,urlParams));
			
			get=(HttpGet) setHeaders(get);
			response=client.execute(get);
			
			setSessionIDIfExists(response.getAllHeaders());
			
			
			
			boolean fileFound=false;
			for (Header x : response.getAllHeaders()) {
				if (x.getName().equals("Content-Disposition")) {
					fileFound=true;
					break;
				}
			}
			
			if (!fileFound) {
				setLastError(HTMLParser.extractCookie(response.getAllHeaders(), C.STATUS_MESSAGE_COOKIE));

				return Status.ERROR_ARCHIVE_NOT_FOUND;
			}
			
			
			
			//copy file from the HTTPResponse to an output stream
			File out=new File(filePath);
			File parent=new File(out.getAbsolutePath().substring(0,out.getAbsolutePath().lastIndexOf(File.separator)));
			parent.mkdirs();
			FileOutputStream outs=new FileOutputStream(out);
			IOUtils.copy(response.getEntity().getContent(), outs);
			outs.close();
			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, true);

			return 0;
		} catch (Exception e) {
			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, true);
			return Status.ERROR_INTERNAL;
		} finally {
			safeCloseResponse(response);
		}
		
	}
	/**
	 * Downloads the job output from a job from StarExec in the form of a zip file
	 * @param jobId The ID of the job to download the output from
 	 * @param filePath The output path where the file will be saved
	 * @return A status code as defined in the Status class
	 */
	public int downloadJobOutput(Integer jobId, String filePath) {
		return downloadArchive(jobId,R.JOB_OUTPUT,null,null,filePath,false,false,false,false,null,false,false,null,false);
	}
	/**
	 * Downloads a CSV describing a job from StarExec in the form of a zip file
	 * @param jobId The ID of the job to download the CSV for
 	 * @param filePath The output path where the file will be saved
 	 * @param includeIds Whether to include columns in the CSV displaying the IDs of the primitives involved
 	 * @param onlyCompleted If true, only include completed pairs in the csv
	 * @return A status code as defined in the Status class
	 */
	public int downloadJobInfo(Integer jobId, String filePath, boolean includeIds, boolean onlyCompleted) {
		return downloadArchive(jobId,R.JOB,null,null,filePath,false,false,includeIds,false,null,onlyCompleted,false,null,false);
	}
	/**
	 * Downloads a space XML file from StarExec in the form of a zip file
	 * @param spaceId The ID of the space to download the XML for
 	 * @param filePath The output path where the file will be saved
	 * @param getAttributes If true, adds benchmark attributes to the XML
	 * @param updateId The ID of the update processor to use in the XML. No update processor if null
	 * @return A status code as defined in the Status class
	 */
	public int downloadSpaceXML(Integer spaceId, String filePath,boolean getAttributes, Integer updateId) {
		return downloadArchive(spaceId, R.SPACE_XML,null,null,filePath,false,false,false,false,null,false,getAttributes,updateId,false);
	}
	/**
	 * Downloads the data contained in a single space 
	 * @param spaceId The ID of the space to download
	 * @param filePath Where to output the ZIP file
	 * @param excludeSolvers If true, excludes solvers from the ZIP file.
	 * @param excludeBenchmarks If true, excludes benchmarks from the ZIP file
	 * @return A status code as defined in the Status class
	 */
	public int downloadSpace(Integer spaceId, String filePath, boolean excludeSolvers, boolean excludeBenchmarks) {
		return downloadArchive(spaceId, R.SPACE,null,null,filePath,excludeSolvers,excludeBenchmarks,false,false,null,false,false,null,false);
	}
	/**
	 * Downloads the data contained in a space hierarchy rooted at the given space 
	 * @param spaceId The ID of the root space of the hierarchy to download
	 * @param filePath Where to output the ZIP file
	 * @param excludeSolvers If true, excludes solvers from the ZIP file.
	 * @param excludeBenchmarks If true, excludes benchmarks from the ZIP file
	 * @return A status code as defined in the Status class
	 */
	public int downloadSpaceHierarchy(Integer spaceId, String filePath,boolean excludeSolvers,boolean excludeBenchmarks) {
		return downloadArchive(spaceId,R.SPACE,null,null,filePath,excludeSolvers,excludeBenchmarks,false,true,null,false,false,null,false);
	}
	/**
	 * Downloads a pre processor from StarExec in the form of a zip file
	 * @param procId The ID of the processor to download
 	 * @param filePath The output path where the file will be saved
	 * @return A status code as defined in the Status class
	 */
	public int downloadPreProcessor(Integer procId, String filePath) {
		return downloadArchive(procId,R.PROCESSOR,null,null,filePath,false,false,false,false,"pre",false,false,null,false);
	}
	/**
	 * Downloads a benchmark processor from StarExec in the form of a zip file
	 * @param procId The ID of the processor to download
 	 * @param filePath The output path where the file will be saved
	 * @return A status code as defined in the Status class
	 */
	public int downloadBenchProcessor(Integer procId, String filePath) {
		return downloadArchive(procId,R.PROCESSOR,null,null,filePath,false,false,false,false,R.BENCHMARK,false,false,null,false);
	}
	/**
	 * Downloads a post processor from StarExec in the form of a zip file
	 * @param procId The ID of the processor to download
 	 * @param filePath The output path where the file will be saved
	 * @return A status code as defined in the Status class
	 */
	public int downloadPostProcessor(Integer procId, String filePath) {
		return downloadArchive(procId,R.PROCESSOR,null,null,filePath,false,false,false,false,"post",false,false,null,false);
	}
	/**
	 * Downloads a benchmark from StarExec in the form of a zip file
	 * @param benchId The ID of the benchmark to download
 	 * @param filePath The output path where the file will be saved
	 * @return A status code as defined in the Status class
	 */
	public int downloadBenchmark(Integer benchId,String filePath) {
		return downloadArchive(benchId,R.BENCHMARK,null,null,filePath,false,false,false,false,null,false,false,null,false);
	}
	/**
	 * Downloads a CSV describing a job from StarExec in the form of a zip file. Only job pairs
	 * that have a completion ID greater than "since" are included
	 * @param jobId The ID of the job to download the CSV for
 	 * @param filePath The output path where the file will be saved
 	 * @param includeIds Whether to include columns in the CSV displaying the IDs of the primitives involved
 	 * @param since A completion ID, indicating that only pairs with completion IDs greater should be included
	 * @return A status code as defined in the Status class
	 */
	public int downloadNewJobInfo(Integer jobId, String filePath, boolean includeIds, int since) {
		return downloadArchive(jobId,R.JOB,since,null,filePath,false,false,includeIds,false,null,false,false,null,false);
	}
	/**
	 * Downloads output from a job from StarExec in the form of a zip file. Only job pairs
	 * that have a completion ID greater than "since" are included
	 * @param jobId The ID of the job to download the output
 	 * @param filePath The output path where the file will be saved
 	 * @param since A completion ID, indicating that only pairs with completion IDs greater should be included
	 * @return A status code as defined in the Status class
	 */
	public int downloadNewJobOutput(Integer jobId, String filePath, int since, long lastModified) {
		return downloadArchive(jobId,R.JOB_OUTPUT,since,lastModified,filePath,false,false,false,false,null,false,false,null,false);
	}
	
	/**
	 * Downloads an archive from Starexec
	 * @param id The ID of the primitive that is going to be downloaded
	 * @param type The type of the primitive (R.SOLVER, R.BENCHMARK, and so on
	 * @param since If downloading new job info, this represents the last seen completion index. Otherwise,
	 * it should be null
	 * @param filePath The path to where the archive should be output, including the filename
	 * @param excludeSolvers If downloading a space, whether to exclude solvers
	 * @param excludeBenchmarks If downloading a space, whether to exclude benchmarks 
	 * @param includeIds If downloading a job info CSV, whether to include columns for IDs
	 * @param hierarchy If downloading a space, whether to get the full hierarchy
	 * @param procClass If downloading a processor, what type of processor it is (R.BENCHMARK,"post",or "pre")
	 * @return
	 */
	protected int downloadArchive(Integer id, String type, Integer since, Long lastTimestamp, String filePath,
			boolean excludeSolvers, boolean excludeBenchmarks, boolean includeIds, Boolean hierarchy,
			String procClass, boolean onlyCompleted,boolean includeAttributes,Integer updateId, Boolean longPath) {
		HttpResponse response=null;
		
		try {
			HashMap<String,String> urlParams=new HashMap<String,String>();
			log.log("Downloading archive of type: "+type);
			urlParams.put(C.FORMPARAM_TYPE, type);
			urlParams.put(C.FORMPARAM_ID, id.toString());
			if (type.equals(R.SPACE)) {
				urlParams.put("hierarchy",hierarchy.toString());
			}
			if (since!=null) {
				urlParams.put(C.FORMPARAM_SINCE,since.toString());
			}
			if (lastTimestamp!=null) {
				urlParams.put("lastTimestamp",lastTimestamp.toString());
			}
			if (procClass!=null) {
				urlParams.put("procClass", procClass);
			}
			//if the use put in the include ids param, pass it on to the server
			if (includeIds) {
				urlParams.put("returnids","true");
			}
			if (onlyCompleted) {
				urlParams.put("getcompleted","true");
			}
			if (includeAttributes) {
				urlParams.put("includeattrs", "true");
			}
			if (updateId!=null) {
				urlParams.put("updates","true");
				urlParams.put("upid", updateId.toString());
			}
			if (excludeBenchmarks) {
				urlParams.put("includebenchmarks", "false");
			}
			if (excludeSolvers) {
				urlParams.put("includesolvers","false");
			}
            if (longPath) {
                urlParams.put("longpath","true");
            }
			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
			//First, put in the request for the server to generate the desired archive			
			
			HttpGet get=new HttpGet(HTMLParser.URLEncode(baseURL+C.URL_DOWNLOAD,urlParams));
			
			get=(HttpGet) setHeaders(get);
			response=client.execute(get);
			Boolean done=false;
			setSessionIDIfExists(response.getAllHeaders());
			
			
			
			boolean fileFound=false;
			
			for (Header x : response.getAllHeaders()) {
				if (x.getName().equals("Content-Disposition")) {
					fileFound=true;
					break;
				}
			}
			
			if (!fileFound) {
				setLastError(HTMLParser.extractCookie(response.getAllHeaders(), C.STATUS_MESSAGE_COOKIE));
				return Status.ERROR_ARCHIVE_NOT_FOUND;
			}
			
			//TODO: handle these prints better
			Integer totalPairs=null;
			Integer pairsFound=null;
			Integer oldPairs=null;
            //Integer runningPairsFound=null;
			int lastSeen=-1;
			//if we're sending 'since,' it means this is a request for new job data
			boolean isNewJobRequest=urlParams.containsKey(C.FORMPARAM_SINCE);
			boolean isNewOutputRequest = isNewJobRequest && urlParams.get(C.FORMPARAM_TYPE).equals(R.JOB_OUTPUT);
			if (isNewJobRequest) {
				
				totalPairs=Integer.parseInt(HTMLParser.extractCookie(response.getAllHeaders(),"Total-Pairs"));
				pairsFound=Integer.parseInt(HTMLParser.extractCookie(response.getAllHeaders(),"Pairs-Found"));
				oldPairs=Integer.parseInt(HTMLParser.extractCookie(response.getAllHeaders(),"Older-Pairs"));
				//runningPairsFound=Integer.parseInt(HTMLParser.extractCookie(response.getAllHeaders(),"Running-Pairs"));
				
				//check to see if the job is complete
				done=totalPairs==(pairsFound+oldPairs);
				lastSeen=Integer.parseInt(HTMLParser.extractCookie(response.getAllHeaders(),"Max-Completion"));
				//indicates there was no new information
				if (lastSeen<=since) {
					if (done) {
						return C.SUCCESS_JOBDONE;
					}
					if (!isNewOutputRequest) {
						return C.SUCCESS_NOFILE;
						//TODO: What to do in this situation?
					}
				}
			}
			
			//copy file from the HTTPResponse to an output stream
			File out=new File(filePath);
			File parent=new File(out.getAbsolutePath().substring(0,out.getAbsolutePath().lastIndexOf(File.separator)));
			parent.mkdirs();
			FileOutputStream outs=new FileOutputStream(out);
			IOUtils.copy(response.getEntity().getContent(), outs);
			outs.close();
			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, true);
			
			if (!CommandValidator.isValidZip(out)) {
				out.delete();
				if (isNewOutputRequest) {
					return C.SUCCESS_NOFILE;
				}
				return Status.ERROR_INTERNAL; //we got back an invalid archive for some reason
			}
			long lastModified = ArchiveUtil.getMostRecentlyModifiedFileInZip(out);
			
			//only after we've successfully saved the file should we update the maximum completion index,
			//which keeps us from downloading the same stuff twice
			if (urlParams.containsKey(C.FORMPARAM_SINCE) && lastSeen>=0) {
				
				if (urlParams.get(C.FORMPARAM_TYPE).equals(R.JOB)) {
					this.setJobInfoCompletion(id, lastSeen);
				} else if (urlParams.get(C.FORMPARAM_TYPE).equals(R.JOB_OUTPUT)) {
					this.setJobOutCompletion(id, new PollJobData(lastSeen,lastModified));
				}
                if(pairsFound != 0) {
				    System.out.println("completed pairs found ="+(oldPairs+1)+"-"+(oldPairs+pairsFound)+"/"+totalPairs +" (highest="+lastSeen+")");
                }
				/*
                if(runningPairsFound != 0) {
                    System.out.println("output from running pairs found="+runningPairsFound); 
                }*/

			}
			if (done) {
				return C.SUCCESS_JOBDONE;
			}
			return 0;
		} catch (IOException e) {
			log.log("Caught exception in downloadArchive: " + Util.getStackTrace(e));

			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, true);
			return Status.ERROR_INTERNAL;
		} finally {
			safeCloseResponse(response);
		}
		
	}
	
	/**
	 * Sets the highest seen completion ID for info on a given job
	 * @param jobID An ID of a job on StarExec
	 * @param completion The completion ID
	 */
	protected void setJobInfoCompletion(int jobID,int completion) {
		job_info_indices.put(jobID,completion);
	}
	
	/**
	 * Sets the highest seen completion ID for output on a given job
	 * @param jobID An ID of a job on StarExec
	 * @param completion The completion ID
	 */
	protected void setJobOutCompletion(int jobID,PollJobData data) {
		job_out_indices.put(jobID,data);
	}	
	
	/**
	 * Gets the status of a benchmark archive upload given the ID of the upload status.
	 * @param statusId The upload ID to use
	 * @return A human-readable string containing details of the benchmark upload in the following format
	 * benchmarks: {validated} / {failed validation} / {total} | spaces: {completed} / {total} 
	 * {error message if exists}
	 * {"upload complete" if finished}
	 * Null is returned if there was an error, and this Connection's error message will have been set
	 */
	public String getBenchmarkUploadStatus(Integer statusId) {
		HttpResponse response = null;
		try {
			
			String URL=baseURL+C.URL_GET_BENCH_UPLOAD_STATUS;
			URL=URL.replace("{statusId}",statusId.toString());
			HttpGet get=new HttpGet(URL);
			get=(HttpGet) setHeaders(get);
			response=client.execute(get);
			setSessionIDIfExists(get.getAllHeaders());
			JsonObject obj=JsonHandler.getJsonObject(response);

			boolean success=JsonHandler.getSuccessOfResponse(obj);
			String message=JsonHandler.getMessageOfResponse(obj);
			if (success) {
				
				return message;
			} else {
				setLastError(message);
			}
			
		} catch (Exception e) {
			setLastError("Internal error getting benchmark upload details");
			return null;
		} finally {
			safeCloseResponse(response);
		}
		return null;
	}
	/**
	 * Creates a job on Starexec according to the given paramteters
	 * @param spaceId The ID of the root space for the job
	 * @param name The name of the job. Must be unique to the space
	 * @param desc A description of the job. Can be empty.
	 * @param postProcId The ID of the post processor to apply to the job output
	 * @param preProcId The ID of the pre procesor that will be run on benchmarks before they are fed into solvers
	 * @param queueId The ID of the queue to run the job on
	 * @param wallclock The wallclock timeout for job pairs. If null, the default for the space will be used
	 * @param cpu The cpu timeout for job pairs. If null, the default for the space will be used.
	 * @param useDepthFirst If true, job pairs will be run in a depth-first fashion in the space hierarchy.
	 * @param startPaused If true, job will be paused upon creation
	 * @param seed A number that will be passed into the preprocessor for every job pair in this job
	 * If false, they will be run in a round-robin fashion.
	 * @param maxMemory Specifies the maximum amount of memory, in gigabytes, that can be used by any one job pair.
	 * @param suppressTimestamps If true, timestamps will not be added to job output lines. Defaults to false.
	 * @param resultsInterval The interval at which to get incremental results, in seconds. 0 means no incremental results
	 * @return A status code as defined in status.java
	 */
	public int createJob(Integer spaceId, String name,String desc, Integer postProcId,Integer preProcId,Integer queueId, Integer wallclock, Integer cpu,
			Boolean useDepthFirst, Double maxMemory, boolean startPaused,Long seed, Boolean suppressTimestamps, Integer resultsInterval) {
		HttpResponse response = null;
		try {
			List<NameValuePair> params=new ArrayList<NameValuePair>();

			String traversalMethod="depth";
			if (!useDepthFirst) {
				traversalMethod="robin";
			}
			
			HttpPost post=new HttpPost(baseURL+C.URL_POSTJOB);
			
			post=(HttpPost) setHeaders(post);
			
			params.add(new BasicNameValuePair("sid", spaceId.toString()));
			params.add(new BasicNameValuePair("name",name));
			params.add(new BasicNameValuePair("desc",desc));
			if (wallclock!=null) {
				params.add(new BasicNameValuePair("wallclockTimeout",String.valueOf(wallclock)));
			}
			if (cpu!=null) {
				params.add(new BasicNameValuePair("cpuTimeout",String.valueOf(cpu)));
			}
			params.add(new BasicNameValuePair("queue",queueId.toString()));
			params.add(new BasicNameValuePair("postProcess",postProcId.toString()));
			params.add(new BasicNameValuePair("preProcess",preProcId.toString()));
			params.add(new BasicNameValuePair("seed",seed.toString()));
			params.add(new BasicNameValuePair("resultsInterval", resultsInterval.toString()));
			params.add(new BasicNameValuePair(C.FORMPARAM_TRAVERSAL,traversalMethod));
			if (maxMemory!=null) {
				params.add(new BasicNameValuePair("maxMem",String.valueOf(maxMemory)));
			}
			params.add(new BasicNameValuePair("runChoice","keepHierarchy"));
			if (startPaused) {
				params.add(new BasicNameValuePair("pause","yes"));
			} else {
				params.add(new BasicNameValuePair("pause","no"));
			}
			if (suppressTimestamps) {
				params.add(new BasicNameValuePair("suppressTimestamp", "yes"));
			} else {
				params.add(new BasicNameValuePair("suppressTimestamp", "no"));
			}
			
			post.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
			
			response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());

			String id=HTMLParser.extractCookie(response.getAllHeaders(),"New_ID");
			
			//make sure the id we got back is positive, indicating we made a job successfully
			if (Validator.isValidPosInteger(id)) {
				return Integer.parseInt(id);
			}
			
			setLastError(HTMLParser.extractCookie(response.getAllHeaders(), C.STATUS_MESSAGE_COOKIE));
			return Status.ERROR_SERVER;
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		} finally {
			safeCloseResponse(response);
		}
	}
	
	
	/**
	 * Gets the max completion ID yet seen for output downloads on a given job
	 * @param jobID The ID of a job on StarExec
	 * @return The maximum completion ID seen yet, or 0 if not seen.
	 */
	
	protected PollJobData getJobOutCompletion(int jobID) {
		if (!job_out_indices.containsKey(jobID)) {
			job_out_indices.put(jobID, new PollJobData());
		} 
		return job_out_indices.get(jobID);
		
	}
	/**
	 * Gets the max completion ID for info downloads on the given job.
	 * @param jobID The ID of a job on StarExec
	 * @return The maximum completion ID seen for the job, or 0 if not seen
	 */
	protected int getJobInfoCompletion(int jobID) {
		if (!job_info_indices.containsKey(jobID)) {
			job_info_indices.put(jobID, 0);
		} 
		return job_info_indices.get(jobID);
	}

	/**
	 * @param lastError the lastError to set. Leading/trailing whitespace and quotes will be removed
	 */
	private void setLastError(String lastError) {
		if (lastError==null) {
			this.lastError="";
			return;
		}
		lastError=lastError.trim();
		if (lastError.charAt(0)=='"') {
			lastError=lastError.replaceFirst("\"", "");
		}
		if (lastError.charAt(lastError.length()-1)=='"') {
			lastError=lastError.substring(0,lastError.length()-1);
		}
		this.lastError = lastError;
	}

	/**
	 * @return The last error that was sent back by the server. This will be updated whenever ERROR_SERVER is returned
	 * as a status code by any function
	 */
	public String getLastError() {
		return lastError;
	}
	/**
	 * Gets the attributes for a queue in a Map
	 * @param id The primitive ID
	 * @return The Map of attributes, or null on error
	 */
	public Map<String,String> getQueueAttributes(int id) {
		return getPrimitiveAttributes(id, "queue");
	}
	/**
	 * Gets the attributes for a configuration in a Map
	 * @param id The primitive ID
	 * @return The Map of attributes, or null on error
	 */
	public Map<String,String> getConfigurationAttributes(int id) {
		return getPrimitiveAttributes(id, "configuration");
	}
	/**
	 * Gets the attributes for a processor in a Map
	 * @param id The primitive ID
	 * @return The Map of attributes, or null on error
	 */
	public Map<String,String> getProcessorAttributes(int id) {
		return getPrimitiveAttributes(id, "processor");
	}
	/**
	 * Gets the attributes for a solver in a Map
	 * @param id The primitive ID
	 * @return The Map of attributes, or null on error
	 */
	public Map<String,String> getSolverAttributes(int id) {
		return getPrimitiveAttributes(id, R.SOLVER);
	}
	
	/**
	 * Gets the attributes for a benchmark in a Map
	 * @param id The primitive ID
	 * @return The Map of attributes, or null on error
	 */
	
	public Map<String,String> getBenchmarkAttributes(int id) {
		return getPrimitiveAttributes(id, "benchmark");
	}
	
	/**
	 * Gets the attributes for a job in a Map
	 * @param id The primitive ID
	 * @return The Map of attributes, or null on error
	 */
	
	public Map<String,String> getJobAttributes(int id) {
		return getPrimitiveAttributes(id, R.JOB);
	}
	/**
	 * Gets the attributes for a space in a Map
	 * @param id The primitive ID
	 * @return The Map of attributes, or null on error
	 */
	
	public Map<String,String> getSpaceAttributes(int id) {
		return getPrimitiveAttributes(id, R.SPACE);
	}
	
	/**
	 * Asks the server for a Json object representing a primitive and returns the attributes of that primitive in a
	 * Map of keys to values.
	 * @param id The ID of the primitive
	 * @param type The type of the primitive
	 * @return The Map, or null on error
	 */
	protected Map<String,String> getPrimitiveAttributes(int id, String type) {
		HashMap<String, String> failMap=new HashMap<String,String>();
		HttpResponse response=null;
		try {
			HttpGet get=new HttpGet(baseURL+C.URL_GETPRIMJSON.replace("{type}", type).replace("{id}",String.valueOf(id)));
			get=(HttpGet) setHeaders(get);
			response=client.execute(get);
			setSessionIDIfExists(get.getAllHeaders());
			JsonElement json=JsonHandler.getJsonString(response);
			String errorMessage=JsonHandler.getMessageOfResponse(json.getAsJsonObject());
			if (errorMessage!=null) {
				this.setLastError(errorMessage);
				failMap.put("-1", String.valueOf(Status.ERROR_SERVER));
				return failMap;
			}
			return JsonHandler.getJsonAttributes(json.getAsJsonObject());
		} catch (Exception e) {
			failMap.put("-1", String.valueOf(Status.ERROR_INTERNAL));
			return failMap;
		} finally {
			safeCloseResponse(response);
		}
	}
	
	/**
	 * Closes an HttpResponse, suppressing any errors.
	 * @param response
	 */
	private void safeCloseResponse(HttpResponse response) {
		try {
			response.getEntity().getContent().close();
		} catch (Exception e) {
			//ignore
		}
	}

	
}

