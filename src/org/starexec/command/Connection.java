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
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.Header;
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
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;


public class Connection {
	private static Charset utf8=Charset.forName("UTF-8");
	private String baseURL;
	private String sessionID=null;
	//private boolean testMode=false;
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
	
	protected Connection(Connection con) {
		
		this.setBaseURL(con.getBaseURL());
		setUsername(con.getUsername());
		setPassword(con.getPassword());
		client=getClient();
		client.getParams();
		setInfoIndices(con.getInfoIndices());
		setOutputIndices(con.getOutputIndices());
	}
	
	/**
	 * Sets the new Connection object's username and password based on user-specified parameters.
	 * Also sets the instance of StarExec that is being connected to
	 * @param commandParams User specified parameters
	 */
	
	public Connection(String user, String pass, String url) {
		this.setBaseURL(url);
		setUsername(user);
		setPassword(pass);
		initializeComponents();
	}
	
	public Connection(String user, String pass) {
		setBaseURL(R.URL_STAREXEC_BASE);
		setUsername(user);
		setPassword(pass);
		initializeComponents();
	}
	public Connection() {
		setBaseURL(R.URL_STAREXEC_BASE);
		setUsername("public");
		setPassword("public");
		initializeComponents();
	}
	private void initializeComponents() {
		client=getClient();
		setInfoIndices(new HashMap<Integer,Integer>());
		setOutputIndices(new HashMap<Integer,Integer>());
	}

	protected void setBaseURL(String baseURL) {
		this.baseURL = baseURL;
	}

	protected String getBaseURL() {
		return baseURL;
	}

	protected void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}

	protected String getSessionID() {
		return sessionID;
	}

	protected void setUsername(String username) {
		this.username = username;
	}
	/**
	 * Gets the username that is being used on this connection
	 * @return The username as a String
	 */
	protected String getUsername() {
		return username;
	}

	protected void setPassword(String password) {
		this.password = password;
	}
	/**
	 * Gets the password that is being used on this connection
	 * @return The password as a String
	 */
	protected String getPassword() {
		return password;
	}

	protected void setOutputIndices(HashMap<Integer,Integer> job_out_indices) {
		this.job_out_indices = job_out_indices;
	}

	protected HashMap<Integer,Integer> getOutputIndices() {
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
		String id=HTMLParser.extractCookie(headers, R.TYPE_SESSIONID);
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
	 * @return 0 on success, and a negative error code otherwise.
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
	 * @return 0 on success, and a negative error code otherwise.
	 */
	public int uploadBenchmarksToSingleSpace(String filePath,Integer processorID, Integer spaceID,Boolean downloadable) {
		return uploadBenchmarks(filePath,processorID,spaceID,"local",new Permission(),"",downloadable,false,false,false,null);
	}
	
	//TODO: Support dependencies for benchmarks
	
	protected int uploadBenchmarks(String filePath,Integer type,Integer spaceID, String upMethod, Permission p, String url, Boolean downloadable, Boolean hierarchy,
			Boolean dependency,Boolean linked, Integer depRoot) {		
		try {
			
			HttpPost post = new HttpPost(baseURL+R.URL_UPLOADBENCHMARKS);
			MultipartEntity entity = new MultipartEntity();
			entity.addPart("space", new StringBody(spaceID.toString(), utf8));
			entity.addPart("localOrURL",new StringBody(upMethod,utf8));
			
			//it is ok to set URL even if we don't need it
			entity.addPart("url",new StringBody(url,utf8));
			
			entity.addPart("download", new StringBody(downloadable.toString(), utf8));
			entity.addPart("benchType",new StringBody(type.toString(),utf8));
			
				entity.addPart("dependency",new StringBody(dependency.toString(),utf8));
				entity.addPart("linked",new StringBody(linked.toString(),utf8));
			if (depRoot==null) {
				entity.addPart("depRoot",new StringBody("-1",utf8));
			} else {
				entity.addPart("depRoot",new StringBody(depRoot.toString(),utf8));
			}
			if (hierarchy) {
				entity.addPart("upMethod", new StringBody("convert",utf8));
			} else {
				entity.addPart("upMethod", new StringBody("dump",utf8));
			}
			
			for (String x : p.getOnPermissions()) {
				entity.addPart(x,new StringBody("true",utf8));
			}
			for (String x : p.getOffPermissions()) {
				entity.addPart(x,new StringBody("false",utf8));
			}
		
			//only include the archive file if we need it
			if (upMethod.equals("local")) {
				FileBody fileBody = new FileBody(new File(filePath));
				entity.addPart("benchFile", fileBody);
			}
			
			post.setEntity(entity);
			post=(HttpPost) setHeaders(post);
			
			HttpResponse response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			response.getEntity().getContent().close();
			
			//TODO: improve the error handling here
			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			return Status.ERROR_SERVER;
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
		try {
			
			
			HttpPost post=new HttpPost(baseURL+R.URL_UPLOADCONFIG);
			post=(HttpPost) setHeaders(post);
			
			MultipartEntity entity = new MultipartEntity();
			entity.addPart("solverID",new StringBody(solverID.toString(),utf8));
			entity.addPart("uploadConfigDesc",new StringBody(name,utf8));
			entity.addPart("uploadConfigName",new StringBody(desc,utf8));
			
			FileBody fileBody = new FileBody(new File(filePath));
			entity.addPart("file", fileBody);
			
			
			HttpResponse response=client.execute(post);
			
			setSessionIDIfExists(response.getAllHeaders());
			response.getEntity().getContent().close();
			//we're expecting a redirect to the configuration
			if (response.getStatusLine().getStatusCode()!=302) {
				return Status.ERROR_BAD_ARGS;
			}
			int newID=Integer.valueOf(HTMLParser.extractCookie(response.getAllHeaders(),"New_ID"));
			return newID;
		} catch (Exception e) {
			return Status.ERROR_SERVER;
		}
	}
	
	/**
	 * Uploads a processor to starexec 
	 * @param name The name to give the processor
	 * @param desc A description for the processor
	 * @param filePath An absolute file path to the file to upload
	 * @param communityID The ID of the community that will be given the processor
	 * @param type Must be "post" "pre" or "bench"
	 * @return The positive integer ID assigned the new processor on success, or a negative
	 * error code on failure
	 */
	
	protected int uploadProcessor(String name, String desc, String filePath,Integer communityID,String type) {

		File f = new File(filePath); //file is also required

		try {
			HttpPost post = new HttpPost(baseURL+R.URL_UPLOADPROCESSOR);
			MultipartEntity entity = new MultipartEntity();
			entity.addPart("action",new StringBody("add",utf8));
			entity.addPart("type",new StringBody(type,utf8));
			entity.addPart("name", new StringBody(name, utf8));
			entity.addPart("desc", new StringBody(desc, utf8));
			entity.addPart("com",new StringBody(communityID.toString(),utf8));
			FileBody fileBody = new FileBody(f);
			entity.addPart("file", fileBody);
			
			post.setEntity(entity);
			post=(HttpPost) setHeaders(post);
			
			HttpResponse response=client.execute(post);
			
			setSessionIDIfExists(response.getAllHeaders());
			
			response.getEntity().getContent().close();
			
			if (response.getStatusLine().getStatusCode()!=200) {
				return Status.ERROR_BAD_ARGS;
			}
			int id=Integer.valueOf(HTMLParser.extractCookie(response.getAllHeaders(),"New_ID"));
			return id;
		} catch (Exception e) {
			
			return Status.ERROR_SERVER;
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
		return uploadProcessor(name,desc,filePath,communityID, "bench");
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
	
	
	
	public int uploadSpaceXML(String filePath, Integer spaceID) {
		try {
			
			HttpPost post=new HttpPost(baseURL+R.URL_UPLOADSPACE);
			post=(HttpPost) setHeaders(post);
			
			MultipartEntity entity = new MultipartEntity();
			entity.addPart("space",new StringBody(spaceID.toString(),utf8));
			File f=new File(filePath);
			FileBody fileBody = new FileBody(f);
			entity.addPart("f", fileBody);
			
			
			HttpResponse response=client.execute(post);
			
			setSessionIDIfExists(response.getAllHeaders());
			
			if (response.getStatusLine().getStatusCode()!=200) {
				return Status.ERROR_BAD_ARGS;
			}
			
			return 0;
		} catch (Exception e) {
			return Status.ERROR_SERVER;
		}
	}
	
	/**
	 * Uploads a solver to Starexec. The description of the solver will be taken from the archive being uploaded
	 * @param name The name of the solver
	 * @param desc The description of the solver
	 * @param spaceID The ID of the space to put the solver in
	 * @param filePath the path to the solver archive to upload
	 * @param downloadable True if the solver should be downloadable by other users, and false otherwise
	 * @return The ID of the new solver, which must be positive, or a negative error code
	 */
	public int uploadSolver(String name,String desc,Integer spaceID, String filePath, Boolean downloadable) {
		return uploadSolver(name,desc,"text",spaceID,filePath,downloadable);
	}
	/**
	 * Uploads a solver to Starexec. The description of the solver will be taken from the archive being uploaded
	 * @param name The name of the solver
	 * @param spaceID The ID of the space to put the solver in
	 * @param filePath the path to the solver archive to upload
	 * @param downloadable True if the solver should be downloadable by other users, and false otherwise
	 * @return The ID of the new solver, which must be positive, or a negative error code
	 */
	public int uploadSolver(String name,Integer spaceID,String filePath,Boolean downloadable) {
		return uploadSolver(name,null,"upload",spaceID,filePath,downloadable);
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
	 * @return The ID of the new solver, which must be positive, or a negative error code
	 */
	protected int uploadSolver(String name, String desc,String descMethod,Integer spaceID,String filePath, Boolean downloadable) {
		try {
			
			HttpPost post = new HttpPost(baseURL+R.URL_UPLOADSOLVER);
			MultipartEntity entity = new MultipartEntity();
			//Only  include the description file if we need it
			if (descMethod.equals("file")) {
				FileBody descFileBody=new FileBody(new File(desc));
				entity.addPart("d",descFileBody);
			}
			entity.addPart("sn", new StringBody(name, utf8));
			if (descMethod.equals("text")) {
				entity.addPart("desc", new StringBody(desc, utf8));
			} else {
				entity.addPart("desc", new StringBody("", utf8));
			}
			
			entity.addPart("space", new StringBody(spaceID.toString(), utf8));
			entity.addPart("upMethod",new StringBody("local",utf8));
			entity.addPart("url",new StringBody("",utf8));
			entity.addPart("descMethod", new StringBody(descMethod,utf8));
			entity.addPart("dlable", new StringBody(downloadable.toString(), utf8));

			FileBody fileBody = new FileBody(new File(filePath));
			entity.addPart("f", fileBody);

			post.setEntity(entity);
			post=(HttpPost) setHeaders(post);
			
			HttpResponse response=client.execute(post);
			
			setSessionIDIfExists(response.getAllHeaders());
			
			response.getEntity().getContent().close();
			int newID=Integer.valueOf(HTMLParser.extractCookie(response.getAllHeaders(),"New_ID"));
			return newID;
		} catch (Exception e) {	
			return Status.ERROR_SERVER;
		}	
	}
	
	/**
	 * Uploads a solver to Starexec. The description of the solver will be taken from the archive being uploaded
	 * @param name The name of the solver
	 * @param desc The description of the solver
	 * @param spaceID The ID of the space to put the solver in
	 * @param url The URL of the archived solver to upload
	 * @param downloadable True if the solver should be downloadable by other users, and false otherwise
	 * @return The ID of the new solver, which must be positive, or a negative error code
	 */
	public int uploadSolverFromURL(String name,String desc,Integer spaceID, String url, Boolean downloadable) {
		return uploadSolverFromURL(name,desc,"text",spaceID,url,downloadable);
	}
	/**
	 * Uploads a solver to Starexec. The description of the solver will be taken from the archive being uploaded
	 * @param name The name of the solver
	 * @param spaceID The ID of the space to put the solver in
	 * @param url The URL of hte archived solver to upload
	 * @param downloadable True if the solver should be downloadable by other users, and false otherwise
	 * @return The ID of the new solver, which must be positive, or a negative error code
	 */
	public int uploadSolverFromURL(String name,Integer spaceID,String url,Boolean downloadable) {
		return uploadSolverFromURL(name,null,"upload",spaceID,url,downloadable);
	}
	
	public int uploadSolverFromURL(String name, String desc,String descMethod, Integer spaceID, String url, Boolean downloadable) {
		try {
			HttpPost post = new HttpPost(baseURL+R.URL_UPLOADSOLVER);
			MultipartEntity entity = new MultipartEntity();
			if (descMethod.equals("file")) {
				FileBody descFileBody=new FileBody(new File(desc));
				desc="";
				entity.addPart("d",descFileBody);
			}
			entity.addPart("sn", new StringBody(name, utf8));
			if (descMethod.equals("text")) {
				entity.addPart("desc", new StringBody(desc, utf8));
			} else {
				entity.addPart("desc", new StringBody("", utf8));
			}
			entity.addPart("space", new StringBody(spaceID.toString(), utf8));
			entity.addPart("upMethod",new StringBody("URL",utf8));
			entity.addPart("url",new StringBody(url,utf8));
			entity.addPart("descMethod", new StringBody(descMethod,utf8));
			entity.addPart("dlable", new StringBody(downloadable.toString(), utf8));
			
			post.setEntity(entity);
			post=(HttpPost) setHeaders(post);
			
			HttpResponse response=client.execute(post);
			
			setSessionIDIfExists(response.getAllHeaders());
			
			response.getEntity().getContent().close();
			int newID=Integer.valueOf(HTMLParser.extractCookie(response.getAllHeaders(),"New_ID"));
			return newID;
		} catch (Exception e) {	
			return Status.ERROR_SERVER;
		}	
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
	
	
	public int setFirstName(String name) {
		return this.setUserSetting("firstname", name);
	}
	
	public int setLastName(String name) {
		return this.setUserSetting("lastname", name);
	}
	
	public int setInstitution(String inst) {
		return this.setUserSetting("institution",inst);
	}
	
	public int deleteSolvers(List<Integer> ids) {
		return deletePrimitives(ids,"solver");
	}
	
	public int deleteBenchmarks(List<Integer> ids) {
		return deletePrimitives(ids,"benchmark");
	}
	
	public int deleteProcessors(List<Integer> ids) {
		return deletePrimitives(ids,"processor");
	}
	
	public int deleteConfigurations(List<Integer> ids) {
		return deletePrimitives(ids,"configuration");
	}
	public int deleteJobs(List<Integer> ids) {
		return deletePrimitives(ids,"job");
	}
	
	
	protected int deletePrimitives(List<Integer> ids, String type) {
		try {
			
			HttpPost post=new HttpPost(baseURL+R.URL_DELETEPRIMITIVE+"/"+type);
			post=(HttpPost) setHeaders(post);
			List<NameValuePair> params=new ArrayList<NameValuePair>();
			for (Integer id :ids) {
				params.add(new BasicNameValuePair("selectedIds[]",id.toString()));
			}
			post.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
			HttpResponse response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			response.getEntity().getContent().close();
			return 0;
			
		} catch (Exception e) {
			return Status.ERROR_SERVER;
		}
		
	}
	
	/**
	 * Gets the ID of the user currently logged in to StarExec
	 * @return The integer user ID
	 */
	
	public int getUserID() {
		try {
			HttpGet get=new HttpGet(baseURL+R.URL_GETID);
			get=(HttpGet) setHeaders(get);
			HttpResponse response=client.execute(get);
			setSessionIDIfExists(get.getAllHeaders());
			JsonElement json=JsonHandler.getJsonString(response);
			response.getEntity().getContent().close();
			return json.getAsInt();
			
		} catch (Exception e) {
			return Status.ERROR_SERVER;
		}
	}
	
	protected int setSpaceVisibility(Integer spaceID,Boolean hierarchy, Boolean setPublic) {
		try {
			String pubOrPriv="";
			//these strings are specified in StarExec
			if (setPublic) {
				pubOrPriv="makePublic";
			} else {
				pubOrPriv="makePrivate";
			}
			HttpPost post=new HttpPost(baseURL+R.URL_EDITSPACEVISIBILITY+"/"+pubOrPriv +"/"+spaceID.toString() +"/" +hierarchy.toString());
			post=(HttpPost) setHeaders(post);
			HttpResponse response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			response.getEntity().getContent().close();
			
			//we should get back an HTTP OK if we're allowed to change the visibility
			if (response.getStatusLine().getStatusCode()!=200) {
				return Status.ERROR_BAD_PARENT_SPACE;
			}
			return 0;
		} catch (Exception e) {
			return Status.ERROR_SERVER;
		}
	}
	
	protected int setUserSetting(String setting,String val) {
		try {	
			int userId=getUserID();
			String url=baseURL+R.URL_USERSETTING+setting+"/"+userId+"/"+val;
			url=url.replace(" ", "%20"); //encodes white space, which can't be used in a URL
			HttpPost post=new HttpPost(url);
			post=(HttpPost) setHeaders(post);
			HttpResponse response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			response.getEntity().getContent().close();
			
			if (response.getStatusLine().getStatusCode()!=200) {
				return Status.ERROR_BAD_ARGS;
			}
			
			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			return Status.ERROR_SERVER;
		}
	}
	
	/**
	 * Resumes a job on starexec that was paused previously
	 * @param commandParams Parameters given by the user at the command line. Should include an ID
	 * @return 0 on success or a negative error code on failure
	 */
	
	public int resumeJob(Integer jobID) {
		return pauseOrResumeJob(jobID,false);
	}
	/**
	 * Pauses a job that is currently running on starexec
	 * @param commandParams Parameters given by the user at the command line. Should include an ID
	 * @return 0 on success or a negative error code on failure
	 */
	
	public int pauseJob(Integer jobID) {
		return pauseOrResumeJob(jobID,true);
	}
	
	/**
	 * Pauses or resumes a job depending on the value of pause
	 * @param commandParams Parameters given by the user at the command line
	 * @param pause Pauses a job if true and resumes it if false
	 * @return 0 on success or a negative error code on failure
	 */
	
	protected int pauseOrResumeJob(Integer jobID, boolean pause) {
		try {
			String URL=baseURL+R.URL_PAUSEORRESUME;
			if (pause) {
				URL=URL.replace("{method}", "pause");
			} else {
				URL=URL.replace("{method}","resume");
			}
			URL=URL.replace("{id}", jobID.toString());
			HttpPost post=new HttpPost(URL);
			post=(HttpPost) setHeaders(post);
			post.setEntity(new UrlEncodedFormEntity(new ArrayList<NameValuePair>(),"UTF-8"));
			HttpResponse response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			response.getEntity().getContent().close();
			return 0;
			
		} catch (Exception e) {
			return Status.ERROR_SERVER; 
		}
	}
	
	/**
	 * Removes the given solvers from the given space. The solvers are NOT deleted.
	 * @param solverIds The IDs of the solvers to remove
	 * @param spaceID The ID of the space
	 * @return 0 on success, or a negative integer status code on failure
	 */
	public int removeSolvers(List<Integer> solverIds, Integer spaceID) {
		return removePrimitives(solverIds,spaceID,"solver",false);
	}
	
	/**
	 * Removes the given jobs from the given space. The jobs are NOT deleted.
	 * @param jobIds The IDs of the jobs to remove
	 * @param spaceID The ID of the space
	 * @return 0 on success, or a negative integer status code on failure
	 */
	
	public int removeJobs(List<Integer> jobIds, Integer spaceID) {
		return removePrimitives(jobIds,spaceID, "job",false);
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
	 * @return 0 on success, or a negative integer status code on failure
	 */
	
	public int removeSubspace(List<Integer> subspaceIds, Integer spaceID, Boolean deletePrims) {
		return removePrimitives(subspaceIds, spaceID,"subspace", deletePrims);
	}
	
	/**
	 * Removes the association between a primitive and a space on StarExec
	 * @param commandParams Parameters given by the user
	 * @param type The type of primitive being remove
	 * @return 0 on success, and a negative error code on failure
	 * @author Eric Burns
	 */
	protected int removePrimitives(List<Integer> primIDs,Integer spaceID,String type, Boolean deletePrims) {
		try {
			
			HttpPost post=new HttpPost(baseURL+R.URL_REMOVEPRIMITIVE+"/"+type+"/"+spaceID.toString());
			//first sets username and password data into HTTP POST request
			List<NameValuePair> params=new ArrayList<NameValuePair>();
			String key="selectedIds[]";
			for (Integer id : primIDs) {
				params.add(new BasicNameValuePair(key, id.toString()));
			}
			
			params.add(new BasicNameValuePair("deletePrims",deletePrims.toString()));
			post.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
			
			HttpResponse response=client.execute(post);
			
			setSessionIDIfExists(response.getAllHeaders());
			response.getEntity().getContent().close();
			
			return 0;
		} catch (Exception e) {
			return Status.ERROR_SERVER;
		}
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
					return Status.ERROR_BAD_URL;
				}
				return Status.ERROR_SERVER;
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
				return Status.ERROR_BAD_CREDENTIALS;
			}
			return 0;
			
		} catch (IllegalStateException e) {
			
			return Status.ERROR_BAD_URL;
			
		} catch (Exception e) {
			
		}
		
		return Status.ERROR_SERVER;
	}
	
	public int linkSolvers(Integer[] solverIds, Integer oldSpaceId, Integer newSpaceId, Boolean hierarchy) {
		return copyOrLinkPrimitives(solverIds,oldSpaceId,newSpaceId,false,hierarchy,"solver");
	}
	
	public int linkBenchmarks(Integer[] benchmarkIds, Integer oldSpaceId, Integer newSpaceId) {
		return copyOrLinkPrimitives(benchmarkIds,oldSpaceId,newSpaceId,false,false,"benchmark");
	}
	
	public int linkJobs(Integer[] jobIds, Integer oldSpaceId, Integer newSpaceId) {
		return copyOrLinkPrimitives(jobIds,oldSpaceId,newSpaceId,false,false,"job");
	}
	public int linkUsers(Integer[] userIds, Integer oldSpaceId, Integer newSpaceId) {
		return copyOrLinkPrimitives(userIds,oldSpaceId,newSpaceId,false,false,"user");
	}
	
	
	public int copySolvers(Integer[] solverIds, Integer oldSpaceId, Integer newSpaceId, Boolean hierarchy) {
		return copyOrLinkPrimitives(solverIds,oldSpaceId,newSpaceId,true,hierarchy,"solver");
	}
	
	public int copyBenchmarks(Integer[] benchmarkIds, Integer oldSpaceId, Integer newSpaceId) {
		return copyOrLinkPrimitives(benchmarkIds,oldSpaceId,newSpaceId,true,false,"benchmark");
	}
	
	public int copySpaces(Integer[] spaceIds, Integer oldSpaceId, Integer newSpaceId, Boolean hierarchy) {
		return copyOrLinkPrimitives(spaceIds,oldSpaceId,newSpaceId,true,hierarchy,"space");
	}
	
	
	
	
	
	/**
	 * Sends a copy or link request to the StarExec server and returns a status code
	 * indicating the result of the request
	 * @param commandParams The parameters given by the user at the command line.
	 * @param copy True if a copy should be performed, and false if a link should be performed.
	 * @param type The type of primitive being copied.
	 * @return An integer error code where 0 indicates success and a negative number is an error.
	 */
	protected int copyOrLinkPrimitives(Integer[] ids, Integer oldSpaceId, Integer newSpaceID, Boolean copy, Boolean hierarchy, String type) {
		try {
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
			
			urlExtension=urlExtension.replace("{spaceID}", newSpaceID.toString());
			
			HttpPost post=new HttpPost(baseURL+urlExtension);
			
			List<NameValuePair> params=new ArrayList<NameValuePair>(3);
			
			//not all of the following are needed for every copy request, but including them does no harm
			//and allows all the copy commands to be handled by this function
			params.add(new BasicNameValuePair("copyToSubspaces", hierarchy.toString()));
			params.add(new BasicNameValuePair("fromSpace",oldSpaceId.toString()));
			for (Integer id : ids) {
				params.add(new BasicNameValuePair("selectedIds[]",id.toString()));
			}
			
			params.add(new BasicNameValuePair("copy",copy.toString()));
			params.add(new BasicNameValuePair("copyHierarchy", String.valueOf(hierarchy.toString())));
			post.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
			
			post=(HttpPost) setHeaders(post);
			
			HttpResponse response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			
			JsonElement jsonE=JsonHandler.getJsonString(response);
			response.getEntity().getContent().close();
			JsonPrimitive p=jsonE.getAsJsonPrimitive();
			if (p.getAsInt()==0) {
				
				return 0;
				
			} else if (p.getAsInt()>=3 && p.getAsInt()<=6) {
				return Status.ERROR_PERMISSION_DENIED;
			} else if (p.getAsInt()==7) {
				return Status.ERROR_NAME_NOT_UNIQUE;
			} else if (p.getAsInt()==8) {
				return Status.ERROR_INSUFFICIENT_QUOTA;
			} 
			else {
				return Status.ERROR_SERVER;
			}
		} catch (Exception e) {
			return Status.ERROR_SERVER;
		}
	}
	
	/**
	 * Creates a subspace of an existing space on StarExec
	 * @param commandParam A HashMap containing key/value pairs gathered from user input at the command line
	 * @return the new space ID on success and a negative error code otherwise
	 * @author Eric Burns
	 */
	
	public int createSubspace(String name, String desc,Integer parentSpaceID, Permission p, Boolean locked) {
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
			
			HttpPost post = new HttpPost(baseURL+R.URL_ADDSPACE);
			post.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
			post=(HttpPost) setHeaders(post);
			
			HttpResponse response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			response.getEntity().getContent().close();
			
			if (response.getStatusLine().getStatusCode()!=302) {
				return Status.ERROR_BAD_PARENT_SPACE;
			}
			int newID=Integer.valueOf(HTMLParser.extractCookie(response.getAllHeaders(),"New_ID"));
			return newID;
		} catch (Exception e) {
			return Status.ERROR_SERVER;
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
		return getPrims(spaceID, null, false, "solvers");
	}
	/**
	 * Gets a HashMap that maps the IDs of solvers to their names for all benchmarks in the given
	 * space
	 * @param spaceID The ID of the space
	 * @return A HashMap mapping IDs to names If there was an error, the HashMap will contain only one key, and it will
	 * be negative, whereas all IDs must be positive.
	 */
	
	public HashMap<Integer,String> getBenchmarksInSpace(Integer spaceID) {
		return getPrims(spaceID, null, false, "benchmarks");
	}
	/**
	 * Gets a HashMap that maps the IDs of solvers to their names for all jobs in the given
	 * space
	 * @param spaceID The ID of the space
	 * @return A HashMap mapping IDs to names If there was an error, the HashMap will contain only one key, and it will
	 * be negative, whereas all IDs must be positive.
	 */
	
	public HashMap<Integer,String> getJobsInSpace(Integer spaceID) {
		return getPrims(spaceID, null, false, "jobs");
	}
	/**
	 * Gets a HashMap that maps the IDs of solvers to their names for all users in the given
	 * space
	 * @param spaceID The ID of the space
	 * @return A HashMap mapping IDs to names If there was an error, the HashMap will contain only one key, and it will
	 * be negative, whereas all IDs must be positive.
	 */
	
	public HashMap<Integer,String> getUsersInSpace(Integer spaceID) {
		return getPrims(spaceID, null, false, "users");
	}
	/**
	 * Gets a HashMap that maps the IDs of solvers to their names for all spaces in the given
	 * space
	 * @param spaceID The ID of the space
	 * @return A HashMap mapping IDs to names If there was an error, the HashMap will contain only one key, and it will
	 * be negative, whereas all IDs must be positive.
	 */
	
	public HashMap<Integer,String> getSpacesInSpace(Integer spaceID) {
		return getPrims(spaceID, null, false, "spaces");
	}
	/**
	 * Gets a HashMap that maps the IDs of solvers to their names for all solvers the current user owns
	 * @return A HashMap mapping IDs to names If there was an error, the HashMap will contain only one key, and it will
	 * be negative, whereas all IDs must be positive.
	 */
	
	public HashMap<Integer,String> getSolversByUser() {
		return getPrims(null, null, true, "solvers");
	}
	/**
	 * Gets a HashMap that maps the IDs of solvers to their names for all benchmarks the current user owns
	 * @return A HashMap mapping IDs to names If there was an error, the HashMap will contain only one key, and it will
	 * be negative, whereas all IDs must be positive.
	 */
	
	public HashMap<Integer,String> getBenchmarksByUser() {
		return getPrims(null, null, true, "benchmarks");
	}
	/**
	 * Gets a HashMap that maps the IDs of solvers to their names for all jobs the current user owns
	 * @return A HashMap mapping IDs to names If there was an error, the HashMap will contain only one key, and it will
	 * be negative, whereas all IDs must be positive.
	 */
	public HashMap<Integer,String> getJobsByUser() {
		return getPrims(null, null, true, "jobs");
	}

	
	/**
	 * Lists the IDs and names of some kind of primitives in a given space
	 * @param urlParams Parameters to be encoded into the URL to send to the server
	 * @param commandParams Parameters given by the user at the command line
	 * @return An integer error code with 0 indicating success and a negative number indicating an
	 * error
	 * @author Eric Burns
	 */
	protected HashMap<Integer,String> getPrims(Integer spaceID, Integer limit, boolean forUser, String type) {
		HashMap<Integer,String> errorMap=new HashMap<Integer,String>();
		HashMap<Integer,String> prims=new HashMap<Integer,String>();
		
		try {
			HashMap<String,String> urlParams=new HashMap<String,String>();
			
			urlParams.put(R.FORMPARAM_TYPE, type);
		
			String URL=null;
			if (forUser) {
				int id=getUserID();
				if (id<0) {
					errorMap.put(id, null);
					return errorMap;
				}
				
				urlParams.put(R.FORMPARAM_ID, String.valueOf(id));
				URL=baseURL+R.URL_GETUSERPRIM;
			} else {
				urlParams.put(R.FORMPARAM_ID, spaceID.toString());
				URL=baseURL+R.URL_GETPRIM;
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
			
			JsonElement jsonE=JsonHandler.getJsonString(response);
			response.getEntity().getContent().close();
			if (jsonE.isJsonPrimitive()) {
				
				JsonPrimitive j=jsonE.getAsJsonPrimitive();
				int x=j.getAsInt();
				if (x==2) {
					errorMap.put(Status.ERROR_PERMISSION_DENIED, null);
					return errorMap;
				} 
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
			errorMap.put(Status.ERROR_SERVER, null);
			
			return errorMap;
		}
	}
	/**
	 * Downloads a solver from StarExec in the form of a zip file
	 * @param solverId The ID of the solver to download
 	 * @param filePath The output path where the file will be saved
	 * @return A status code as defined in the Status class
	 */
	public int downloadSolver(Integer solverId, String filePath) {
		return downloadArchive(solverId, "solver",null,filePath,false,false,false,false,null);
	}
	/**
	 * Downloads job pair output for one pair from StarExec in the form of a zip file
	 * @param pairId The ID of the pair to download
 	 * @param filePath The output path where the file will be saved
	 * @return A status code as defined in the Status class
	 */
	public int downloadJobPair(Integer pairId, String filePath) {
		return downloadArchive(pairId,"jp_output",null,filePath,false,false,false,false,null);
	}
	/**
	 * Downloads the job output from a job from StarExec in the form of a zip file
	 * @param jobId The ID of the job to download the output from
 	 * @param filePath The output path where the file will be saved
	 * @return A status code as defined in the Status class
	 */
	public int downloadJobOutput(Integer jobId, String filePath) {
		return downloadArchive(jobId,"j_outputs",null,filePath,false,false,false,false,null);
	}
	/**
	 * Downloads a CSV describing a job from StarExec in the form of a zip file
	 * @param jobId The ID of the job to download the CSV for
 	 * @param filePath The output path where the file will be saved
 	 * @param includeIds Whether to include columns in the CSV displaying the IDs of the primitives involved
	 * @return A status code as defined in the Status class
	 */
	public int downloadJobInfo(Integer jobId, String filePath, boolean includeIds) {
		return downloadArchive(jobId,"job",null,filePath,false,false,includeIds,false,null);
	}
	/**
	 * Downloads a space XML file from StarExec in the form of a zip file
	 * @param spaceId The ID of the space to download the XML for
 	 * @param filePath The output path where the file will be saved
	 * @return A status code as defined in the Status class
	 */
	public int downloadSpaceXML(Integer spaceId, String filePath) {
		return downloadArchive(spaceId, "spaceXML",null,filePath,false,false,false,false,null);
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
		return downloadArchive(spaceId, "space",null,filePath,excludeSolvers,excludeBenchmarks,false,false,null);
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
		return downloadArchive(spaceId,"space",null,filePath,excludeSolvers,excludeBenchmarks,false,true,null);
	}
	/**
	 * Downloads a pre processor from StarExec in the form of a zip file
	 * @param procId The ID of the processor to download
 	 * @param filePath The output path where the file will be saved
	 * @return A status code as defined in the Status class
	 */
	public int downloadPreProcessor(Integer procId, String filePath) {
		return downloadArchive(procId,"proc",null,filePath,false,false,false,false,"pre");
	}
	/**
	 * Downloads a benchmark processor from StarExec in the form of a zip file
	 * @param procId The ID of the processor to download
 	 * @param filePath The output path where the file will be saved
	 * @return A status code as defined in the Status class
	 */
	public int downloadBenchProcessor(Integer procId, String filePath) {
		return downloadArchive(procId,"proc",null,filePath,false,false,false,false,"bench");
	}
	/**
	 * Downloads a post processor from StarExec in the form of a zip file
	 * @param procId The ID of the processor to download
 	 * @param filePath The output path where the file will be saved
	 * @return A status code as defined in the Status class
	 */
	public int downloadPostProcessor(Integer procId, String filePath) {
		return downloadArchive(procId,"proc",null,filePath,false,false,false,false,"post");
	}
	/**
	 * Downloads a benchmark from StarExec in the form of a zip file
	 * @param benchId The ID of the benchmark to download
 	 * @param filePath The output path where the file will be saved
	 * @return A status code as defined in the Status class
	 */
	public int downloadBenchmark(Integer benchId,String filePath) {
		return downloadArchive(benchId,"bench",null,filePath,false,false,false,false,null);
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
		return downloadArchive(jobId,"job",since,filePath,false,false,includeIds,false,null);
	}
	/**
	 * Downloads output from a job from StarExec in the form of a zip file. Only job pairs
	 * that have a completion ID greater than "since" are included
	 * @param jobId The ID of the job to download the output
 	 * @param filePath The output path where the file will be saved
 	 * @param since A completion ID, indicating that only pairs with completion IDs greater should be included
	 * @return A status code as defined in the Status class
	 */
	public int downloadNewJobOutput(Integer jobId, String filePath, int since) {
		return downloadArchive(jobId,"j_outputs",since,filePath,false,false,false,false,null);
	}
	
	/**
	 * Downloads an archive from Starexec
	 * @param id The ID of the primitive that is going to be downloaded
	 * @param type The type of the primitive ("solver", "bench", and so on
	 * @param since If downloading new job info, this represents the last seen completion index. Otherwise,
	 * it should be null
	 * @param filePath The path to where the archive should be output, including the filename
	 * @param excludeSolvers If downloading a space, whether to exclude solvers
	 * @param excludeBenchmarks If downloading a space, whether to exclude benchmarks 
	 * @param includeIds If downloading a job info CSV, whether to include columns for IDs
	 * @param hierarchy If downloading a space, whether to get the full hierarchy
	 * @param procClass If downloading a processor, what type of processor it is ("bench","post",or "pre")
	 * @return
	 */
	protected int downloadArchive(Integer id, String type, Integer since, String filePath, boolean excludeSolvers, boolean excludeBenchmarks, boolean includeIds, Boolean hierarchy,String procClass) {
		HttpResponse response=null;
		try {
			HashMap<String,String> urlParams=new HashMap<String,String>();
			urlParams.put(R.FORMPARAM_TYPE, type);
			urlParams.put(R.FORMPARAM_ID, id.toString());
			if (type.equals("space")) {
				urlParams.put("hierarchy",hierarchy.toString());
			}
			if (since!=null) {
				urlParams.put(R.FORMPARAM_SINCE,since.toString());
			}
			if (procClass!=null) {
				urlParams.put("procClass", procClass);
			}
			//if the use put in the include ids param, pass it on to the server
			if (includeIds) {
				urlParams.put("returnids","true");
			}
			if (excludeBenchmarks) {
				urlParams.put("includebenchmarks", "false");
			}
			if (excludeSolvers) {
				urlParams.put("includesolvers","false");
			}
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
				
				//indicates there was no new information
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
				response.getEntity().getContent().close();
				return Status.ERROR_ARCHIVE_NOT_FOUND;
			}
			
			//copy file from the HTTPResponse to an output stream
			File out=new File(filePath);
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
				
			}
			if (done!=null) {
				return R.SUCCESS_JOBDONE;
			}
			return 0;
		} catch (Exception e) {
			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, true);
			return Status.ERROR_SERVER;
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
	protected void setJobOutCompletion(int jobID,int completion) {
		job_out_indices.put(jobID,completion);
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
	 * If false, they will be run in a round-robin fashion.
	 * @return
	 */
	public int createJob(Integer spaceId, String name,String desc, Integer postProcId,Integer preProcId,Integer queueId, Integer wallclock, Integer cpu, Boolean useDepthFirst) {
		try {
			
			
			HttpGet get=new  HttpGet(baseURL+R.URL_ADDJOB+"?sid="+spaceId.toString());
			get=(HttpGet) setHeaders(get);
			
			//this response contains HTML with a significant amount of data we need to read through 
			//to find job settings
			HttpResponse response=client.execute(get);
			setSessionIDIfExists(response.getAllHeaders());
			
			HttpEntity data=response.getEntity();
			BufferedReader br=new BufferedReader(new InputStreamReader(data.getContent()));
			BasicNameValuePair keyValue=null;
			
			String line=br.readLine();
			List<NameValuePair> params=new ArrayList<NameValuePair>();
			String cpuTime=null,wallclockTime=null;
			
			while (line!=null) {
				
				line=line.trim();
				keyValue=HTMLParser.extractNameValue(line);
				if (keyValue!=null) {
					
					//we can't put cpuTimeout or wallclockTimeout into the entity immediately
					//because we still want to check if the user set them manually
					if (keyValue.getName().equals("cpuTimeout")) {
						cpuTime=keyValue.getValue();
					} else if (keyValue.getName().equals("wallclockTimeout")) {
						wallclockTime=keyValue.getValue();
					} 
				}
				
				line=br.readLine();
			}
			if (cpu!=null) {
				cpuTime=String.valueOf(cpu);
			}
			if (wallclock!=null) {
				wallclockTime=String.valueOf(wallclock);
			}
			
			
			String traversalMethod="depth";
			if (!useDepthFirst) {
				traversalMethod="robin";
			}
			br.close();
			response.getEntity().getContent().close();
			
			HttpPost post=new HttpPost(baseURL+R.URL_POSTJOB);
			
			post=(HttpPost) setHeaders(post);
			
			params.add(new BasicNameValuePair("sid", spaceId.toString()));
			params.add(new BasicNameValuePair("name",name));
			params.add(new BasicNameValuePair("desc",desc));
			params.add(new BasicNameValuePair("wallclockTimeout",wallclockTime));
			params.add(new BasicNameValuePair("cpuTimeout",cpuTime));
			params.add(new BasicNameValuePair("queue",queueId.toString()));
			params.add(new BasicNameValuePair("postProcess",postProcId.toString()));
			params.add(new BasicNameValuePair("preProcess",preProcId.toString()));
			params.add(new BasicNameValuePair(R.FORMPARAM_TRAVERSAL,traversalMethod));
			
			params.add(new BasicNameValuePair("runChoice","keepHierarchy"));
			
			post.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
			
			response=client.execute(post);
			setSessionIDIfExists(response.getAllHeaders());
			response.getEntity().getContent().close();
			if (response.getStatusLine().getStatusCode()!=302) {
				return Status.ERROR_SERVER;
			}
			int id=Integer.valueOf(HTMLParser.extractCookie(response.getAllHeaders(),"New_ID"));
			return id;
		} catch (Exception e) {
			return Status.ERROR_SERVER;
		}
	}
	
	
	/**
	 * Gets the max completion ID yet seen for output downloads on a given job
	 * @param jobID The ID of a job on StarExec
	 * @return The maximum completion ID seen yet, or 0 if not seen.
	 */
	
	protected int getJobOutCompletion(int jobID) {
		if (!job_out_indices.containsKey(jobID)) {
			job_out_indices.put(jobID, 0);
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
	
}
