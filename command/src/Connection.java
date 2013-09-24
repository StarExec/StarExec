package org.starexec.command;

/**
 * This class is responsible for communicating with the Starexec server 
 * Its functions generally take HashMap objects mapping String keys 
 * to String values and use the keys and values to create
 * HTTP GET and POST requests to StarExec
 */



import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.Header;




import org.apache.http.conn.ssl.SSLSocketFactory;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.entity.mime.content.StringBody;

import com.google.gson.*;


public class Connection {
	private String baseURL;
	private String sessionID=null;
	HttpClient client=null;
	private String username,password;
	
	private HashMap<Integer,Integer> job_info_indices;
	private HashMap<Integer,Integer> job_out_indices;
	
	@SuppressWarnings("deprecation")
	
	/**
	 * Gets an HttpClient that ignores SSL certificates. The SSL certificate
	 * for StarExec is currently not valid
	 * @return An HttpClient that ignores SSL certificates.
	 */
	//TODO: If StarExec gets a valid certificate, we shouldn't have to do this anymore
	private HttpClient getClient() {
		try{
			HttpClient base=new DefaultHttpClient();
			SSLContext ctx = SSLContext.getInstance("TLS");
			
			//just create a trustmanager that does nothing, as we already know StarExec's certificate
			//is invalid
			X509TrustManager tm = new X509TrustManager() {


			    public X509Certificate[] getAcceptedIssuers() {
			        return null;
			    }

				@Override
				public void checkClientTrusted(X509Certificate[] arg0, String arg1)
						throws CertificateException {
					
					
				}

				@Override
				public void checkServerTrusted(X509Certificate[] arg0, String arg1)
						throws CertificateException {
					
					
				}
			};
			ctx.init(null, new TrustManager[]{tm}, null);
			SSLSocketFactory ssf = new SSLSocketFactory(ctx);
			ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			ClientConnectionManager ccm = base.getConnectionManager();
			SchemeRegistry sr = ccm.getSchemeRegistry();
			sr.register(new Scheme("https", ssf, 443));

			client = new DefaultHttpClient(ccm, base.getParams());
			return client;
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Constructor used for copying the setup of one connection into a new connection. Useful if a connection
	 * gets into a bad state (possibly response streams left open due to errors)
	 * @param con The old connection to copy
	 */
	
	public Connection(Connection con) {
		
		this.baseURL=con.getBaseURL();
		username=con.getUsername();
		password=con.getPassword();
		client=getClient();
		
		job_info_indices=con.getInfoIndices();
		job_out_indices=con.getOutIndices();
	}
	
	/**
	 * Sets the new Connection object's username and password based on user-specified parameters.
	 * Also sets the instance of StarExec that is being connected to
	 * @param commandParams User specified parameters
	 */
	
	public Connection(HashMap<String,String> commandParams) {

		if (commandParams.containsKey(R.PARAM_BASEURL)) {
			this.baseURL=commandParams.get(R.PARAM_BASEURL);
		} else {
			this.baseURL=R.URL_STAREXEC_BASE;
		}
		if (!commandParams.get(R.PARAM_USER).equals(R.PARAM_GUEST)) {
			username=commandParams.get(R.PARAM_USER);
			
			password=commandParams.get(R.PARAM_PASSWORD);
		} else {
			username="public";
			password="public";
		}
		client=getClient();
		
		job_info_indices=new HashMap<Integer,Integer>();
		job_out_indices=new HashMap<Integer,Integer>();
	}
	
	
	/**
	 * Gets the max completion ID for info downloads on the given job.
	 * @param jobID The ID of a job on StarExec
	 * @return The maximum completion ID seen for the job, or 0 if not seen
	 */
	public int getJobInfoCompletion(int jobID) {
		if (!job_info_indices.containsKey(jobID)) {
			job_info_indices.put(jobID, 0);
		} 
		return job_info_indices.get(jobID);
	}
	
	/**
	 * Gets all of the completion indices for job information (not job output)
	 * @return A map of job IDs to the last seen completion indices for those jobs. 
	 */
	
	public HashMap<Integer,Integer> getInfoIndices() {
		return job_info_indices;
	}
	
	/**
	 * Gets the max completion ID yet seen for output downloads on a given job
	 * @param jobID The ID of a job on StarExec
	 * @return The maximum completion ID seen yet, or 0 if not seen.
	 */
	
	public int getJobOutCompletion(int jobID) {
		if (!job_out_indices.containsKey(jobID)) {
			job_out_indices.put(jobID, 0);
		} 
		return job_out_indices.get(jobID);
	}
	
	/**
	 * Gets all of the completion indices for job output (not job info)
	 * @return A map of job IDs to the last seen completion indices for those jobs. 
	 */
	
	public HashMap<Integer,Integer> getOutIndices() {
		return job_out_indices;
	}
	
	/**
	 * Sets the highest seen completion ID for info on a given job
	 * @param jobID An ID of a job on StarExec
	 * @param completion The completion ID
	 */
	public void setJobInfoCompletion(int jobID,int completion) {
		job_info_indices.put(jobID,completion);
	}
	
	/**
	 * Sets the highest seen completion ID for output on a given job
	 * @param jobID An ID of a job on StarExec
	 * @param completion The completion ID
	 */
	public void setJobOutCompletion(int jobID,int completion) {
		job_out_indices.put(jobID,completion);
	}
	
	/**
	 * Gets username being used for this connection
	 * @return The username
	 */
	public String getUsername() {
		return username;
	}
	
	/**
	 * Gets the password being used for this connection
	 * @return The password
	 */
	public String getPassword() {
		return password;
	}
	
	/**
	 * Gets the home URL of the StarExec instance currently connected to
	 * @return The base URL
	 */
	public String getBaseURL() {
		return baseURL;
	}
	
	/**
	 * Log into StarExec with the username and password of this connection
	 * @return An integer indicating status, with 0 being normal and a negative integer
	 * indicating an error
	 * @author Eric Burns
	 */
	public int login() {
		try {
			HttpGet get = new HttpGet(baseURL+R.URL_HOME);
			HttpResponse response=client.execute(get);
			sessionID=HTMLParser.extractCookie(response.getAllHeaders(),R.TYPE_SESSIONID);
			response.getEntity().getContent().close();
			if (!this.isValid()) {
				//if the user specified their own URL, it is probably the problem.
				if (!baseURL.equals(R.URL_STAREXEC_BASE)) {
					return R.ERROR_BAD_URL;
				}
				return R.ERROR_SERVER;
			}
			
			//first sets username and password data into HTTP POST request
			List<NameValuePair> params=new ArrayList<NameValuePair>(3);
			params.add(new BasicNameValuePair("j_username", username));
			params.add(new BasicNameValuePair("j_password",password));
			params.add(new BasicNameValuePair("cookieexists","false"));
			HttpPost post = new HttpPost(baseURL+R.URL_LOGIN);
			post.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
			post=(HttpPost) setHeaders(post);
			
			//Post login credentials to server
			response=client.execute(post);	
			response.getEntity().getContent().close();
			
			
			//On success, starexec will try to redirect, but we don't want that here
			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
			get = new HttpGet(baseURL+R.URL_HOME);
			get=(HttpGet) setHeaders(get);
			response=client.execute(get);
			
			sessionID=HTMLParser.extractCookie(response.getAllHeaders(),R.TYPE_SESSIONID);
			
			response.getEntity().getContent().close();
			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, true);
			
			//this means that the server did not give us a new session for the login
			if (sessionID==null) {
				return R.ERROR_BAD_CREDENTIALS;
			}
			return 0;
			
		} catch (IllegalStateException e) {
			
			return R.ERROR_BAD_URL;
			
		} catch (Exception e) {
			
		}
		
		return R.ERROR_SERVER;
	}
	
	/**
	 * Ends the current Starexec session
	 * @return True on success, false otherwise
	 * @author Eric Burns
	 */
	
	public boolean logout() {
		try {
			HttpPost post=new HttpPost(baseURL+R.URL_LOGOUT);
			post=(HttpPost) setHeaders(post);
			HttpResponse response=client.execute(post);
			response.getEntity().getContent().close();
			return true;
		} catch (Exception e) {
			
			return false;
		}
	}
	
	/**
	 * Creates a POST request to StarExec to create a new job
	 * @param commandParams A HashMap containing key/value pairs gathered from the user input at the command line
	 * @return the new job ID on success, a negative integer otherwise
	 * @author Eric Burns
	 */
	public int createJob(HashMap<String,String> commandParams) {
		try {
			
			int valid=Validator.isValidCreateJobRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			
			HttpGet get=new  HttpGet(baseURL+R.URL_ADDJOB+"?sid="+commandParams.get(R.PARAM_ID));
			get=(HttpGet) setHeaders(get);
			
			//this response contains HTML with a significant amount of data we need to read through 
			//to find job settings
			HttpResponse response=client.execute(get);
			setSessionIDIfExists(response.getAllHeaders());
			
			HttpEntity data=response.getEntity();
			BufferedReader br=new BufferedReader(new InputStreamReader(data.getContent()));
			String wallclock="";
			String cpu="";
			BasicNameValuePair keyValue=null;
			
			String line=br.readLine();
			List<NameValuePair> params=new ArrayList<NameValuePair>();
			while (line!=null) {
				
				line=line.trim();
				keyValue=HTMLParser.extractNameValue(line);
				if (keyValue!=null) {
					
					//we can't put cpuTimeout or wallclockTimeout into the entity immediately
					//because we still want to check if the user set them manually
					if (keyValue.getName().equals("cpuTimeout")) {
						cpu=keyValue.getValue();
					} else if (keyValue.getName().equals("wallclockTimeout")) {
						wallclock=keyValue.getValue();
					} /*else {
						String name=keyValue.getName();
						if (name.equals("configs") || name.equals("bench") || name.equals("solver")) {	
							params.add(keyValue);
						}
					}*/
				}
				
				line=br.readLine();
			}
			if (commandParams.containsKey(R.PARAM_WALLCLOCKTIMEOUT)) {
				wallclock=commandParams.get(R.PARAM_WALLCLOCKTIMEOUT);
			}
			if (commandParams.containsKey(R.PARAM_CPUTIMEOUT)) {
				cpu=commandParams.get(R.PARAM_CPUTIMEOUT);
			}
			
			String traversalMethod="depth";
			if (commandParams.containsKey(R.PARAM_TRAVERSAL)) {
				if (commandParams.get(R.PARAM_TRAVERSAL).equals(R.ARG_ROUNDROBIN)) {
					traversalMethod="robin";
				}
			}
			
			br.close();
			response.getEntity().getContent().close();
			
			HttpPost post=new HttpPost(baseURL+R.URL_POSTJOB);
			
			post=(HttpPost) setHeaders(post);
			
			String name=getDefaultName("");
			String desc="";
			if (commandParams.containsKey(R.PARAM_NAME)) {
				name=commandParams.get(R.PARAM_NAME);
			}
			
			if (commandParams.containsKey(R.PARAM_DESC)) {
				desc=commandParams.get(R.PARAM_DESC);
			}
			
			params.add(new BasicNameValuePair("sid", commandParams.get(R.PARAM_ID)));
			params.add(new BasicNameValuePair("name",name));
			params.add(new BasicNameValuePair("desc",desc));
			params.add(new BasicNameValuePair("wallclockTimeout",wallclock));
			params.add(new BasicNameValuePair("cpuTimeout",cpu));
			params.add(new BasicNameValuePair("queue",commandParams.get(R.PARAM_QUEUEID)));
			params.add(new BasicNameValuePair("postProcess",commandParams.get(R.PARAM_PROCID)));
			params.add(new BasicNameValuePair(R.FORMPARAM_TRAVERSAL,traversalMethod));
			
			params.add(new BasicNameValuePair("runChoice","keepHierarchy"));
			
			post.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
			
			response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			response.getEntity().getContent().close();
			if (response.getStatusLine().getStatusCode()!=302) {
				return R.ERROR_SERVER;
			}
			int id=Integer.valueOf(HTMLParser.extractCookie(response.getAllHeaders(),"New_ID"));
			return id;
		} catch (Exception e) {
			
			return R.ERROR_SERVER;
		}
	}
	
	/**
	 * Sends a copy or link request to the StarExec server and returns a status code
	 * indicating the result of the request
	 * @param commandParams The parameters given by the user at the command line.
	 * @param copy True if a copy should be performed, and false if a link should be performed.
	 * @param type The type of primitive being copied.
	 * @return An integer error code where 0 indicates success and a negative number is an error.
	 */
	public int copyPrimitives(HashMap<String,String> commandParams, boolean copy, String type) {
		try {
			int valid=Validator.isValidCopyRequest(commandParams, type);
			if (valid<0) {
				return valid;
			}
			
			String urlExtension;
			if (type.equals("solver")) {
				urlExtension=R.URL_COPYSOLVER;
			} else if (type.equals("space")) {
				urlExtension=R.URL_COPYSPACE;
			} else if (type.equals("job")) {
				urlExtension=R.URL_COPYJOB;
			}
			else {
				urlExtension=R.URL_COPYBENCH;
			}
			
			urlExtension=urlExtension.replace("{spaceID}", commandParams.get(R.PARAM_TO));
			
			HttpPost post=new HttpPost(baseURL+urlExtension);
			
			List<NameValuePair> params=new ArrayList<NameValuePair>(3);
			
			String[] ids=CommandParser.convertToArray(commandParams.get(R.PARAM_ID));
			//not all of the following are needed for every copy request, but including them does no harm
			//and allows all the copy commands to be handled by this function
			params.add(new BasicNameValuePair("copyToSubspaces", String.valueOf(commandParams.containsKey(R.PARAM_HIERARCHY))));
			params.add(new BasicNameValuePair("fromSpace",commandParams.get(R.PARAM_FROM)));
			for (String id : ids) {
				params.add(new BasicNameValuePair("selectedIds[]",id));
			}
			
			params.add(new BasicNameValuePair("copy",String.valueOf(copy)));
			params.add(new BasicNameValuePair("copyHierarchy", String.valueOf(commandParams.containsKey(R.PARAM_HIERARCHY))));
			post.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
			
			post=(HttpPost) setHeaders(post);
			
			HttpResponse response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			
			JsonElement jsonE=getJsonString(response);
			response.getEntity().getContent().close();
			JsonPrimitive p=jsonE.getAsJsonPrimitive();
			if (p.getAsInt()==0) {
				
				return 0;
				
			} else if (p.getAsInt()>=3 && p.getAsInt()<=6) {
				return R.ERROR_PERMISSION_DENIED;
			} else if (p.getAsInt()==7) {
				return R.ERROR_NAME_NOT_UNIQUE;
			} else if (p.getAsInt()==8) {
				return R.ERROR_INSUFFICIENT_QUOTA;
			} 
			else {
				return R.ERROR_SERVER;
			}
		} catch (Exception e) {
			return R.ERROR_SERVER;
		}
	}
	
	
	/**
	 * Creates a subspace of an existing space on StarExec
	 * @param commandParam A HashMap containing key/value pairs gathered from user input at the command line
	 * @return the new space ID on success and a negative error code otherwise
	 * @author Eric Burns
	 */
	
	public int createSubspace(HashMap<String,String> commandParams) {
		try {
			int valid=Validator.isValidCreateSubspaceRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			
			String name=getDefaultName("");
			
			if (commandParams.containsKey(R.PARAM_NAME)) {
				name=commandParams.get(R.PARAM_NAME);
			}
			String desc="";
			if (commandParams.containsKey(R.PARAM_DESC)) {
				desc=commandParams.get(R.PARAM_DESC);
			}
			
			Boolean locked=false;
			if (commandParams.containsKey(R.PARAM_LOCKED)) {
				locked=true;
			}
			//first sets username and password data into HTTP POST request
			List<NameValuePair> params=new ArrayList<NameValuePair>(3);
			params.add(new BasicNameValuePair("parent", commandParams.get(R.PARAM_ID)));
			params.add(new BasicNameValuePair("name",name));
			params.add(new BasicNameValuePair("desc",desc));
			params.add(new BasicNameValuePair("locked",locked.toString()));
			
			for (String x : R.PARAMS_PERMS) {
				if (commandParams.containsKey(x) || commandParams.containsKey(R.PARAM_ENABLE_ALL_PERMISSIONS)) {
					params.add(new BasicNameValuePair(x,"on"));
				}
			}
			
			HttpPost post = new HttpPost(baseURL+R.URL_ADDSPACE);
			post.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
			post=(HttpPost) setHeaders(post);
			
			HttpResponse response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			response.getEntity().getContent().close();
			
			if (response.getStatusLine().getStatusCode()!=302) {
				return R.ERROR_BAD_PARENT_SPACE;
			}
			int newID=Integer.valueOf(HTMLParser.extractCookie(response.getAllHeaders(),"New_ID"));
			return newID;
		} catch (Exception e) {
			return R.ERROR_SERVER;
		}
	}
	
	/**
	 * Removes the association between a primitive and a space on StarExec
	 * @param commandParams Parameters given by the user
	 * @param type The type of primitive being remove
	 * @return 0 on success, and a negative error code on failure
	 * @author Eric Burns
	 */
	public int removePrimitive(HashMap<String,String> commandParams,String type) {
		try {
			int valid=Validator.isValidRemoveRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			HttpPost post=new HttpPost(baseURL+R.URL_REMOVEPRIMITIVE+"/"+type+"/"+commandParams.get(R.PARAM_FROM));
			String [] ids=CommandParser.convertToArray(commandParams.get(R.PARAM_ID));
			//first sets username and password data into HTTP POST request
			List<NameValuePair> params=new ArrayList<NameValuePair>();
			String key="selectedIds[]";
			for (String id : ids) {
				params.add(new BasicNameValuePair(key, id));
			}
			
			params.add(new BasicNameValuePair("deletePrims",String.valueOf(commandParams.containsKey(R.PARAM_DELETE_PRIMS))));
			post.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
			
			HttpResponse response=client.execute(post);
			
			setSessionIDIfExists(response.getAllHeaders());
			response.getEntity().getContent().close();
			
			return 0;
		} catch (Exception e) {
			return R.ERROR_SERVER;
		}
	}
	
	/**
	 * Resumes a job on starexec that was paused previously
	 * @param commandParams Parameters given by the user at the command line. Should include an ID
	 * @return 0 on success or a negative error code on failure
	 */
	
	public int resumeJob(HashMap<String,String> commandParams) {
		return pauseOrResumeJob(commandParams,false);
	}
	/**
	 * Pauses a job that is currently running on starexec
	 * @param commandParams Parameters given by the user at the command line. Should include an ID
	 * @return 0 on success or a negative error code on failure
	 */
	
	public int pauseJob(HashMap<String,String> commandParams) {
		return pauseOrResumeJob(commandParams,true);
	}
	
	/**
	 * Pauses or resumes a job depending on the value of pause
	 * @param commandParams Parameters given by the user at the command line
	 * @param pause Pauses a job if true and resumes it if false
	 * @return 0 on success or a negative error code on failure
	 */
	
	private int pauseOrResumeJob(HashMap<String,String> commandParams, boolean pause) {
		try {
			int valid=Validator.isValidPauseOrResumeRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			String URL=baseURL+R.URL_PAUSEORRESUME;
			if (pause) {
				URL=URL.replace("{method}", "pause");
			} else {
				URL=URL.replace("{method}","resume");
			}
			URL=URL.replace("{id}", commandParams.get(R.PARAM_ID));
			HttpPost post=new HttpPost(URL);
			post=(HttpPost) setHeaders(post);
			post.setEntity(new UrlEncodedFormEntity(new ArrayList<NameValuePair>(),"UTF-8"));
			HttpResponse response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			response.getEntity().getContent().close();
			return 0;
			
		} catch (Exception e) {
			return R.ERROR_SERVER; 
		}
		
		
	}
	
	/**
	 * Deletes a primitive on StarExec
	 * @param commandParams A HashMap of key/value pairs given by the user at the command line
	 * @param type -- The type of primitive to delete
	 * @return 0 on success and a negative integer otherwise
	 * @author Eric Burns
	 */
	
	public int deletePrimitive(HashMap<String,String> commandParams, String type) {
		try {
			int valid=Validator.isValidDeleteRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			HttpPost post=new HttpPost(baseURL+R.URL_DELETEPRIMITIVE+"/"+type);
			post=(HttpPost) setHeaders(post);
			String[] ids=CommandParser.convertToArray(commandParams.get(R.PARAM_ID));
			List<NameValuePair> params=new ArrayList<NameValuePair>();
			for (String id :ids) {
				params.add(new BasicNameValuePair("selectedIds[]",id));
			}
			post.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
			HttpResponse response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			response.getEntity().getContent().close();
			return 0;
			
		} catch (Exception e) {
			return R.ERROR_SERVER;
		}
	}
	
	/**
	 * Function for downloading archives from StarExec with the given parameters and 
	 * file output location.
	 * @param urlParams A list of name/value pairs that will be encoded into the URL
	 * @param commandParams A list of name/value pairs that the user entered into the command line
	 * @return 0 on success, a negative integer on error
	 * @author Eric Burns
	 */
	
	public int downloadArchive(HashMap<String,String> urlParams,HashMap<String,String> commandParams) {
		HttpResponse response=null;
		try {
			int valid=Validator.isValidDownloadRequest(urlParams, commandParams);
			if (valid<0) {
				return valid;
			}
			//if the use put in the include ids param, pass it on to the server
			if (commandParams.containsKey(R.PARAM_INCLUDE_IDS)) {
				urlParams.put("returnids","true");
			}
			if (commandParams.containsKey(R.PARAM_EXCLUDE_BENCHMARKS)) {
				urlParams.put("includebenchmarks", "false");
			}
			if (commandParams.containsKey(R.PARAM_EXCLUDE_SOLVERS)) {
				urlParams.put("includesolvers","false");
			}
			String location=commandParams.get(R.PARAM_OUTPUT_FILE);
			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
			//First, put in the request for the server to generate the desired archive			
			
			HttpGet get=new HttpGet(HTMLParser.URLEncode(baseURL+R.URL_DOWNLOAD,urlParams));
			
			get=(HttpGet) setHeaders(get);
			response=client.execute(get);
			int lastSeen=-1;
			String done=null;
			setSessionIDIfExists(response.getAllHeaders());
			
			//if we're sending 'since,' it means this is a request for new job data
			if (urlParams.containsKey(R.FORMPARAM_SINCE)) {
				
				//check to see if the job is complete
				done=HTMLParser.extractCookie(response.getAllHeaders(),"Job-Complete");
				lastSeen=Integer.parseInt(HTMLParser.extractCookie(response.getAllHeaders(),"Max-Completion"));
				
				//indicates there was no new informatoin
				if (lastSeen<=Integer.parseInt(urlParams.get(R.FORMPARAM_SINCE))) {
					
					response.getEntity().getContent().close();
					if (done!=null) {
						
						return R.SUCCESS_JOBDONE;
					}
					
					//don't save a empty files
					return R.SUCCESS_NOFILE;
				}
			}
			
			boolean fileFound=false;
			for (Header x : response.getAllHeaders()) {
				if (x.getName().equals("Content-Disposition")) {
					fileFound=true;
					break;
				}
			}
			
			if (!fileFound) {
				return R.ERROR_ARCHIVE_NOT_FOUND;
			}
			
			

			//copy file from the HTTPResponse to an output stream
			File out=new File(location);
			File parent=new File(out.getAbsolutePath().substring(0,out.getAbsolutePath().lastIndexOf(File.separator)));
			parent.mkdirs();
			FileOutputStream outs=new FileOutputStream(out);
			IOUtils.copy(response.getEntity().getContent(), outs);
			outs.close();
			response.getEntity().getContent().close();
			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, true);
			
			//only after we've successfully saved the file should we update the maximum completion index,
			//which keeps us from downloading the same stuff twice
			if (urlParams.containsKey(R.FORMPARAM_SINCE) && lastSeen>=0) {
				if (urlParams.get(R.FORMPARAM_TYPE).equals("job")) {
					this.setJobInfoCompletion(Integer.parseInt(urlParams.get("id")), lastSeen);
					
				} else if (urlParams.get(R.FORMPARAM_TYPE).equals("j_outputs")) {
					this.setJobOutCompletion(Integer.parseInt(urlParams.get("id")), lastSeen);
				}
				
				System.out.println("Maximum completion ID found= "+String.valueOf(lastSeen));
			}
			if (done!=null) {
				return R.SUCCESS_JOBDONE;
			}
			return 0;
		} catch (Exception e) {
			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, true);
			return R.ERROR_SERVER;
		}
		
	}
	
	/**
	 * Given an HttpRespone with a JsonElement in its content, returns
	 * the JsonElement
	 * @param response The HttpResponse that should contain the JsonElement
	 * @return The JsonElement
	 * @throws Exception
	 */
	
	private JsonElement getJsonString(HttpResponse response) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
		StringBuilder builder = new StringBuilder();
		for (String line = null; (line = reader.readLine()) != null;) {
		    builder.append(line).append("\n");
		}
		JsonParser parser=new JsonParser();
		return parser.parse(builder.toString());
		
	}
	
	/**
	 * Gets the ID of the user currently logged in to StarExec
	 * @return The integer user ID
	 */
	
	private int getUserID() {
		try {
			HttpGet get=new HttpGet(baseURL+R.URL_GETID);
			get=(HttpGet) setHeaders(get);
			HttpResponse response=client.execute(get);
			setSessionIDIfExists(get.getAllHeaders());
			JsonElement json=getJsonString(response);
			response.getEntity().getContent().close();
			return json.getAsInt();
			
		} catch (Exception e) {
			
			return R.ERROR_SERVER;
		}
	}
	
	/**
	 * Lists the IDs and names of some kind of primitives in a given space
	 * @param urlParams Parameters to be encoded into the URL to send to the server
	 * @param commandParams Parameters given by the user at the command line
	 * @return An integer error code with 0 indicating success and a negative number indicating an
	 * error
	 * @author Eric Burns
	 */
	public HashMap<Integer,String> getPrimsInSpace(HashMap<String,String> urlParams,HashMap<String,String> commandParams) {
		HashMap<Integer,String> errorMap=new HashMap<Integer,String>();
		HashMap<Integer,String> prims=new HashMap<Integer,String>();
		try {
			int valid=Validator.isValidGetPrimRequest(urlParams,commandParams);
			if (valid<0) {
				errorMap.put(valid, null);
				return errorMap;
			}
			String URL=null;
			if (commandParams.containsKey(R.PARAM_USER)) {
				int id=getUserID();
				if (id<0) {
					errorMap.put(id, null);
					return errorMap;
				}
				urlParams.put(R.PARAM_ID, String.valueOf(id));
				URL=baseURL+R.URL_GETUSERPRIM;
			} else {
				URL=baseURL+R.URL_GETPRIM;
			}
			//in the absence of limit, we want all the primitives
			int maximum=Integer.MAX_VALUE;
			if (commandParams.containsKey(R.PARAM_LIMIT)) {
				maximum=Integer.valueOf(commandParams.get(R.PARAM_LIMIT));
			}
			
			//need to specify the number of columns according to what GetNextPageOfPrimitives in RESTHelpers
			//expects
			String columns="0";
			String type=urlParams.get("type");
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
			
			
			URL=URL.replace("{id}", urlParams.get(R.PARAM_ID));
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
			
			HttpResponse response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			
			JsonElement jsonE=getJsonString(response);
			response.getEntity().getContent().close();
			if (jsonE.isJsonPrimitive()) {
				
				JsonPrimitive j=jsonE.getAsJsonPrimitive();
				int x=j.getAsInt();
				if (x==2) {
					errorMap.put(R.ERROR_PERMISSION_DENIED, null);
					return errorMap;
				} 
				errorMap.put(R.ERROR_SERVER, null);
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
			
			errorMap.put(R.ERROR_SERVER, null);
			return errorMap;
		}
	}
	
	/**
	 * Sets a space or space hierarchy as either public or private
	 * @param commandParams Parameters given by the user at the command line
	 * @param setPublic Set public if true and private if false
	 * @return 0 if successful and a negative error code otherwise
	 * @author Eric Burns
	 */
	public int setSpaceVisibility(HashMap<String,String> commandParams, boolean setPublic) {
		try {
			
			int valid=Validator.isValidSetSpaceVisibilityRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			Boolean hierarchy=false;
			if (commandParams.containsKey(R.PARAM_HIERARCHY)) {
				hierarchy=true;
			}
			String pubOrPriv;
			
			//these strings are specified in StarExec
			if (setPublic) {
				pubOrPriv="makePublic";
			} else {
				pubOrPriv="makePrivate";
			}
			
			HttpPost post=new HttpPost(baseURL+R.URL_EDITSPACEVISIBILITY+"/"+pubOrPriv +"/"+commandParams.get(R.PARAM_ID) +"/" +hierarchy.toString());
			post=(HttpPost) setHeaders(post);
			HttpResponse response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			response.getEntity().getContent().close();
			
			//we should get back an HTTP OK if we're allowed to change the visibility
			if (response.getStatusLine().getStatusCode()!=200) {
				return R.ERROR_BAD_PARENT_SPACE;
			}
			return 0;
		} catch (Exception e) {
			return R.ERROR_SERVER;
		}
	}
	
	/**
	 * This function updates one of the default settings of the current Starexec user
	 * @param setting The field to assign a new value to
	 * @param newVal-- The new value to use for setting
	 * @return A code indicating the success of the operation
	 * @author Eric Burns
	 */
	public int setUserSetting(String setting, HashMap<String,String> commandParams) {
		
		int valid=Validator.isValidSetUserSettingRequest(setting,commandParams);
		if (valid<0) {
			return valid;
		}
		String newVal=commandParams.get(R.PARAM_VAL);		
		try {	
			HttpPost post=new HttpPost(baseURL+R.URL_USERSETTING+setting+"/"+newVal);
			post=(HttpPost) setHeaders(post);
			HttpResponse response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			response.getEntity().getContent().close();
			
			if (response.getStatusLine().getStatusCode()!=200) {
				return R.ERROR_BAD_ARGS;
			}
			
			return 0;
		} catch (Exception e) {
			
			return R.ERROR_SERVER;
		}
	}
	
	
	/**
	 * This method takes in a HashMap mapping String keys to String values
	 * and creates and HTTP POST request that pushes a solver to Starexec
	 * 
	 * @param commandParams The parameters from the command line. "f" or "url", and "id" are required.
	 * @return A status code indicating success or failure
	 * @author Eric Burns
	 */
	
	public int uploadBenchmarks(HashMap<String, String> commandParams) {
		int valid=Validator.isValidUploadBenchmarkRequest(commandParams);
		
		if (valid<0) {
			return valid;
		}
		
		Charset utf8=Charset.forName("UTF-8");
		
		Boolean dependency=false;
		String depRoot="";
		Boolean depLinked=false;
		
		//if the dependency parameter exists, we're using the dependencies it specifies
		if (commandParams.containsKey(R.PARAM_DEPENDENCY)) {
			dependency=true;
			depRoot=commandParams.get(R.PARAM_DEPENDENCY);
			if (commandParams.containsKey(R.PARAM_LINKED)) {
				depLinked=true;
			}
		}
		
		String type=commandParams.get(R.PARAM_BENCHTYPE);
		String space= commandParams.get(R.PARAM_ID);

		
		//don't presever hierarchy by default, but do so if the hierarchy parameter is present
		String benchPlacement="dump";
		if (commandParams.containsKey(R.PARAM_HIERARCHY)) {
			benchPlacement="convert";
		}
		
		File f=null;
		String url="";
		String upMethod="local";
		//if a url is present, the file should be taken from the url
		if (commandParams.containsKey(R.PARAM_URL)) {
			if (commandParams.containsKey(R.PARAM_FILE)) {
				return R.ERROR_FILE_AND_URL;
			}
			upMethod="URL";
			url=commandParams.get(R.PARAM_URL);
		} else {
			f = new File(commandParams.get(R.PARAM_FILE));
		}

		Boolean downloadable=false;
		if (commandParams.containsKey(R.PARAM_DOWNLOADABLE)) {
			downloadable=true;
		}
		
		try {
			HttpPost post = new HttpPost(baseURL+R.URL_UPLOADSOLVER);
			MultipartEntity entity = new MultipartEntity();
			entity.addPart("space", new StringBody(space, utf8));
			entity.addPart("localOrURL",new StringBody(upMethod,utf8));
			
			//it is ok to set URL even if we don't need it
			entity.addPart("url",new StringBody(url,utf8));
			
			entity.addPart("download", new StringBody(downloadable.toString(), utf8));
			entity.addPart("benchType",new StringBody(type,utf8));
			
			entity.addPart("dependency",new StringBody(dependency.toString(),utf8));
			entity.addPart("linked",new StringBody(depRoot,utf8));
			entity.addPart("depRoot",new StringBody(depLinked.toString(),utf8));
			entity.addPart("upMethod", new StringBody(benchPlacement,utf8));
			
			//add all permissions
			for (String perm : R.PARAMS_PERMS) {
				if (commandParams.containsKey(R.PARAM_ENABLE_ALL_PERMISSIONS) || commandParams.containsKey(perm)) {
					entity.addPart(perm,new StringBody("true",utf8));
				} else {
					entity.addPart(perm,new StringBody("false",utf8));
				}
			}
			
			
			//only include the archive file if we need it
			if (upMethod.equals("local")) {
				FileBody fileBody = new FileBody(f);
				entity.addPart("benchFile", fileBody);
			}
			
			post.setEntity(entity);
			post=(HttpPost) setHeaders(post);
			
			HttpResponse response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			response.getEntity().getContent().close();
			return 0;
		} catch (Exception e) {
			
			return R.ERROR_SERVER;
		}
		
	}
	
	/**
	 * This function handles user requests for uploading a space XML archive.
	 * @param commandParams The key/value pairs given by the user at the command line. Should contain
	 * ID and File keys
	 * @return the new configuration ID on success, and a negative error code otherwise
	 * @author Eric Burns
	 */
	
	public int uploadConfiguration(HashMap<String, String> commandParams) {
		try {
			
			int valid=Validator.isValidUploadConfigRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			File f=new File(commandParams.get(R.PARAM_FILE));
			String name=getDefaultName(f.getName()+" ");
			String desc="";
			
			if (commandParams.containsKey(R.PARAM_NAME)) {
				name=commandParams.get(R.PARAM_NAME);
			}
			
			if (commandParams.containsKey(R.PARAM_DESC)) {
				desc=commandParams.get(R.PARAM_DESC);
			}
			
			Charset utf8=Charset.forName("UTF-8");
			HttpPost post=new HttpPost(baseURL+R.URL_UPLOADCONFIG);
			post=(HttpPost) setHeaders(post);
			
			MultipartEntity entity = new MultipartEntity();
			entity.addPart("solverID",new StringBody(commandParams.get(R.PARAM_ID),utf8));
			entity.addPart("uploadConfigDesc",new StringBody(name,utf8));
			entity.addPart("uploadConfigName",new StringBody(desc,utf8));
			
			FileBody fileBody = new FileBody(f);
			entity.addPart("file", fileBody);
			
			
			HttpResponse response=client.execute(post);
			
			setSessionIDIfExists(response.getAllHeaders());
			response.getEntity().getContent().close();
			//we're expecting a redirect to the configuration
			if (response.getStatusLine().getStatusCode()!=302) {
				return R.ERROR_BAD_ARGS;
			}
			int newID=Integer.valueOf(HTMLParser.extractCookie(response.getAllHeaders(),"New_ID"));
			return newID;
		} catch (Exception e) {
			return R.ERROR_SERVER;
		}
	}
	
	
	/**
	 * This method takes in a HashMap mapping String keys to String values
	 * and creates and HTTP POST request that pushes a processor to Starexec
	 * 
	 * @param commandParams The parameters from the command line. A file and an ID are required.
	 * @return The new processor ID on success, or a negative error code on failure
	 * @author Eric Burns
	 */
	
	private int uploadProcessor(HashMap<String, String> commandParams, String type) {
		
		int valid=Validator.isValidUploadProcessorRequest(commandParams);
		if (valid<0) {
			return valid;
		}
		
		Charset utf8=Charset.forName("UTF-8");
		
		String community= commandParams.get(R.PARAM_ID); //id is one of the required parameters		
		File f = new File(commandParams.get(R.PARAM_FILE)); //file is also required

		//if a name is given explicitly, use it instead
		String name=getDefaultName(f.getName());
		if (commandParams.containsKey(R.PARAM_NAME)) {
			name=commandParams.get(R.PARAM_NAME);
		}
		
		//If there is a description, get it
		String desc = "";
		if (commandParams.containsKey(R.PARAM_DESC)) {
			desc=commandParams.get(R.PARAM_DESC);			
		}
		
		try {
			HttpPost post = new HttpPost(baseURL+R.URL_UPLOADPROCESSOR);
			MultipartEntity entity = new MultipartEntity();
			entity.addPart("action",new StringBody("add",utf8));
			entity.addPart("type",new StringBody(type,utf8));
			entity.addPart("name", new StringBody(name, utf8));
			entity.addPart("desc", new StringBody(desc, utf8));
			entity.addPart("com",new StringBody(community,utf8));
			FileBody fileBody = new FileBody(f);
			entity.addPart("file", fileBody);
			
			post.setEntity(entity);
			post=(HttpPost) setHeaders(post);
			
			HttpResponse response=client.execute(post);
			
			setSessionIDIfExists(response.getAllHeaders());
			
			response.getEntity().getContent().close();
			
			if (response.getStatusLine().getStatusCode()!=200) {
				return R.ERROR_BAD_ARGS;
			}
			int id=Integer.valueOf(HTMLParser.extractCookie(response.getAllHeaders(),"New_ID"));
			return id;
		} catch (Exception e) {
			
			return R.ERROR_SERVER;
		}
		
	}
	
	/**
	 * Handles requests for uploading post-processors.
	 * @param commandParams The key/value pairs given by the user at the command line. A file and an ID are required
	 * @return 0 on success and a negative error code otherwise
	 * @author Eric Burns
	 */
	
	public int uploadPostProc(HashMap<String,String> commandParams) {
		return uploadProcessor(commandParams, "post");
	}
	
	/**
	 * Handles requests for uploading benchmark processors.
	 * @param commandParams The key/value pairs given by the user at the command line. A file and an ID are required
	 * @return 0 on success and a negative error code otherwise
	 * @author Eric Burns
	 */
	
	public int uploadBenchProc(HashMap<String,String> commandParams) {
		return uploadProcessor(commandParams, "bench");
	}
	
	/**
	 * This function handles user requests for uploading a space XML archive.
	 * @param commandParams The key/value pairs given by the user at the command line. Should contain
	 * ID and File keys
	 * @return 0 on success, and a negative error code otherwise
	 * @author Eric Burns
	 */
	
	public int uploadSpaceXML(HashMap<String, String> commandParams) {
		try {
			int valid=Validator.isValidUploadSpaceXMLRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			Charset utf8=Charset.forName("UTF-8");
			HttpPost post=new HttpPost(baseURL+R.URL_UPLOADSPACE);
			post=(HttpPost) setHeaders(post);
			
			MultipartEntity entity = new MultipartEntity();
			entity.addPart("space",new StringBody(commandParams.get(R.PARAM_ID),utf8));
			File f=new File(commandParams.get(R.PARAM_FILE));
			FileBody fileBody = new FileBody(f);
			entity.addPart("f", fileBody);
			
			
			HttpResponse response=client.execute(post);
			
			setSessionIDIfExists(response.getAllHeaders());
			
			if (response.getStatusLine().getStatusCode()!=200) {
				return R.ERROR_BAD_ARGS;
			}
			
			return 0;
		} catch (Exception e) {
			return R.ERROR_SERVER;
		}
	}
	
	
	/**
	 * This method takes in a HashMap mapping String keys to String values
	 * and creates and HTTP POST request that pushes a solver to Starexec
	 * 
	 * @param formParams The parameters from the command line. A file or url and and ID are required.
	 * @return The ID of the newly uploaded solver on success, or a negative error code on failure
	 * @author Eric Burns
	 */
	
	public int uploadSolver(HashMap<String, String> formParams) {
		int valid=Validator.isValidSolverUploadRequest(formParams);
		if (valid<0) {
			return valid;
		}
		File f=null;
		String name = "";
		String desc = "";
		String space= formParams.get(R.PARAM_ID); //id is one of the required parameters
		String upMethod="local";
		String url="";
		String descMethod="upload";
		Boolean downloadable=false;
		
		String descFile="";
		Charset utf8=Charset.forName("UTF-8");
		
		//if a url is present, the file should be taken from the url
		if (formParams.containsKey(R.PARAM_URL)) {
			upMethod="URL";
			url=formParams.get(R.PARAM_URL);
			name=getDefaultName("");
		} else {
			f = new File(formParams.get(R.PARAM_FILE));
			//name defaults to the name of the file plus the date if none is given
			name=getDefaultName(f.getName()+" ");							
		}
		
		//if a name is given explicitly, use it instead
		if (formParams.containsKey(R.PARAM_NAME)) {
			name=formParams.get(R.PARAM_NAME);
		}
		
		//d is the key used for directly sending a string description
		if (formParams.containsKey(R.PARAM_DESC)) {
			descMethod="text";
			desc=formParams.get(R.PARAM_DESC);
			
		//df is the "description file" key, which should have a filepath value
		} else if (formParams.containsKey(R.PARAM_DESCRIPTION_FILE)) {
			descMethod="file";
			descFile=formParams.get(R.PARAM_DESCRIPTION_FILE);
		}
		
		if (formParams.containsKey(R.PARAM_DOWNLOADABLE)) {
			downloadable=true;
		}
		
		try {
			HttpPost post = new HttpPost(baseURL+R.URL_UPLOADSOLVER);
			MultipartEntity entity = new MultipartEntity();
			
			entity.addPart("sn", new StringBody(name, utf8));
			entity.addPart("desc", new StringBody(desc, utf8));
			entity.addPart("space", new StringBody(space, utf8));
			entity.addPart("upMethod",new StringBody(upMethod,utf8));
			entity.addPart("url",new StringBody(url,utf8));
			entity.addPart("descMethod", new StringBody(descMethod,utf8));
			entity.addPart("dlable", new StringBody(downloadable.toString(), utf8));
			
			//Only  include the description file if we need it
			if (descMethod.equals("file")) {
				FileBody descFileBody=new FileBody(new File(descFile));
				entity.addPart("d",descFileBody);
			}
			
			//only include the archive file if we need it
			if (upMethod.equals("local")) {
				FileBody fileBody = new FileBody(f);
				entity.addPart("f", fileBody);
			}
			
			post.setEntity(entity);
			post=(HttpPost) setHeaders(post);
			
			HttpResponse response=client.execute(post);
			
			setSessionIDIfExists(response.getAllHeaders());
			
			response.getEntity().getContent().close();
			int newID=Integer.valueOf(HTMLParser.extractCookie(response.getAllHeaders(),"New_ID"));
			return newID;
		} catch (Exception e) {	
			return R.ERROR_SERVER;
		}	
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
	private AbstractHttpMessage setHeaders(AbstractHttpMessage msg) {
		return setHeaders(msg,new String[0]);
	}
	
	
	/**
	 * Updates the JSESSIONID of the current connection if the server has sent a new ID
	 * @param headers an array of HTTP headers
	 * @return 0 if the ID was found and changed, -1 otherwise
	 * @author Eric Burns
	 */
	
	private int setSessionIDIfExists(Header [] headers) {
		String id=HTMLParser.extractCookie(headers, R.TYPE_SESSIONID);
		if (id==null) {
			return -1;
		}
		sessionID=id;
		return 0;
	}
	
	/**
	 * Returns the string name given to a primitive if none is specified. The date is used currently
	 * @param prefix A prefix which should be added to the name
	 * @return A string that will be valid for use as a primitive on Starexec
	 * @author Eric Burns
	 */
	private String getDefaultName(String prefix) {
		String date=Calendar.getInstance().getTime().toString();
		date=date.replace(":", " ");
		
		return prefix+date;
	}
}
