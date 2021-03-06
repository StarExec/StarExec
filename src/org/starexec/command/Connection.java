package org.starexec.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.starexec.constants.R;
import org.starexec.data.to.Permission;
import org.starexec.data.to.enums.CopyPrimitivesOption;
import org.starexec.data.to.tuples.HtmlStatusCodePair;
import org.starexec.util.Util;
import org.starexec.util.Validator;
			// -- tmp --
			import org.apache.http.HeaderElement;
			import org.apache.http.impl.cookie.BasicClientCookie;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletResponse;

/**
 * This class is responsible for communicating with the Starexec server It is
 * designed to be a useful Starexec API, which makes it very important to keep
 * this file well documented.
 *
 * @author Eric
 */
@SuppressWarnings({"deprecation"})
public class Connection {
	final private CommandLogger log = CommandLogger.getLogger(Connection.class);
	DefaultHttpClient client = null;
	private String baseURL;
	private String sessionID = null;
	private String username, password;
	private String lastError;
	private Map<Integer, Integer> job_info_indices; // these two map job ids
	// to the max completion
	// index
	private Map<Integer, PollJobData> job_out_indices;

	/**
	 * Constructor used for copying the setup of one connection into a new
	 * connection. Useful if a connection gets into a bad state (possibly
	 * response streams left open due to errors)
	 *
	 * @param con The old connection to copy
	 */

	protected Connection(Connection con) {

		this.setBaseURL(con.getBaseURL());
		setUsername(con.getUsername());
		setPassword(con.getPassword());
		client = buildClient();

		client.getParams();
		setInfoIndices(con.getInfoIndices());
		setOutputIndices(con.getOutputIndices());
		setLastError(con.getLastError());
	}

	/**
	 * Sets the new Connection object's username and password based on
	 * user-specified parameters. Also sets the instance of StarExec that is
	 * being connected to
	 *
	 * @param user The username for this login
	 * @param pass The password for this login
	 * @param url the URL to the Starexec instance that we want to communicate
	 * with
	 */

	public Connection(String user, String pass, String url) {
		this.setBaseURL(url);
		setUsername(user);
		setPassword(pass);
		initializeComponents();
	}

	/**
	 * Sets the new Connection object's username and password based on
	 * user-specified parameters. The URL instance used is the default
	 * (www.starexec.org)
	 *
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

	private static DefaultHttpClient buildClient() {
		return new DefaultHttpClient();

		// HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		// clientBuilder.disableCookieManagement();

		// return clientBuilder.build();
	}

	private static String convertStreamToString(InputStream is) {
		Scanner s = new Scanner(is).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

	private static Optional<Integer> checkIfValidZipFile(File out) throws IOException {
		ZipFile zipfile = null;
		try {
			// Make sure the file is a valid zipfile.
			zipfile = new ZipFile(out);
			return Optional.empty();
		} catch (IOException e) {
			out.delete();
			throw e; // we got back an invalid archive for some reason
		} finally {
			try {
				if (zipfile != null) {
					zipfile.close();
				}
			} catch (IOException e) {
			}
		}
	}

	private void initializeComponents() {
		client = buildClient();

		setInfoIndices(new HashMap<>());
		setOutputIndices(new HashMap<>());
		lastError = "";
	}

	protected String getBaseURL() {
		return baseURL;
	}

	protected void setBaseURL(String baseURL) {
		this.baseURL = baseURL;
	}

	/**
	 * Gets the username that is being used on this connection
	 *
	 * @return The username as a String
	 */
	protected String getUsername() {
		return username;
	}

	protected void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Gets the password that is being used on this connection
	 *
	 * @return The password as a String
	 */
	protected String getPassword() {
		return password;
	}

	protected void setPassword(String password) {
		this.password = password;
	}

	protected Map<Integer, PollJobData> getOutputIndices() {
		return job_out_indices;
	}

	protected void setOutputIndices(Map<Integer, PollJobData> job_out_indices) {
		this.job_out_indices = job_out_indices;
	}

	protected Map<Integer, Integer> getInfoIndices() {
		return job_info_indices;
	}

	protected void setInfoIndices(Map<Integer, Integer> job_info_indices) {
		this.job_info_indices = job_info_indices;
	}

	/**
	 * @return whether the Connection object represents a valid connection to
	 * the server
	 * @author Eric Burns
	 */

	public boolean isValid() {
		return sessionID != null;
	}

	// TODO: Support dependencies for benchmarks

	/**
	 * Uploads a set of benchmarks to Starexec. The benchmarks will be expanded
	 * in a full space hierarchy.
	 *
	 * @param filePath The path to the archive containing the benchmarks
	 * @param processorID The ID of the processor that should be used on the
	 * benchmarks. If there is no such processor, this can be null
	 * @param spaceID The ID of the space to put the benchmarks in
	 * @param downloadable Whether the benchmarks should be downloadable by
	 * other users.
	 * @return A positive upload id on success, and a negative error code
	 * otherwise.
	 */
	public int uploadBenchmarksToSingleSpace(String filePath, Integer processorID, Integer spaceID, Boolean downloadable) {
		return uploadBenchmarks(filePath, processorID, spaceID, "local", new Permission(), "", downloadable, false, false, false, null);
	}

	protected int uploadBenchmarks(String filePath, Integer type, Integer spaceID, String upMethod, Permission p, String url, Boolean downloadable, Boolean hierarchy, Boolean dependency, Boolean linked, Integer depRoot) {
		HttpResponse response = null;
		try {

			String postUrl = baseURL + C.URL_UPLOADBENCHMARKS;
			HttpPost post = new HttpPost(postUrl);
			MultipartEntityBuilder entity = MultipartEntityBuilder.create();
			logAddTextBody(entity, R.SPACE, spaceID.toString());
			logAddTextBody(entity, "localOrUrlOrGit", upMethod);

			// it is ok to set URL even if we don't need it
			logAddTextBody(entity, "url", url);

			logAddTextBody(entity, "download", downloadable.toString());
			logAddTextBody(entity, "benchType", type.toString());
			logAddTextBody(entity, "dependency", dependency.toString());

			logAddTextBody(entity, "linked", linked.toString());
			if (depRoot == null) {
				logAddTextBody(entity, "depRoot", "-1");
			} else {
				logAddTextBody(entity, "depRoot", depRoot.toString());
			}
			if (hierarchy) {
				logAddTextBody(entity, "upMethod", "convert");
			} else {
				logAddTextBody(entity, "upMethod", "dump");
			}

			for (String x : p.getOnPermissions()) {
				logAddTextBody(entity, x, "true");
			}
			for (String x : p.getOffPermissions()) {
				logAddTextBody(entity, x, "false");
			}

			// only include the archive file if we need it
			if (upMethod.equals("local")) {
				FileBody fileBody = new FileBody(new File(filePath));
				entity.addPart("benchFile", fileBody);
			}

			post.setEntity(entity.build());
			post = (HttpPost) setHeaders(post);

			response = executeGetOrPost(post);

			int returnCode = response.getStatusLine().getStatusCode();

			if (returnCode == HttpServletResponse.SC_FOUND) {
				int id = CommandValidator.getIdOrMinusOne(HTMLParser.extractCookie(response.getAllHeaders(), "New_ID"));
				if (id > 0) {
					return id;
				} else {
					lastError = "We did not get a New_ID header";
					return Status.ERROR_INTERNAL; // we should have gotten an
					// error from the server and
					// no redirect if there was
					// a catchable error
				}

			} else {
				setLastError(HTMLParser.extractCookie(response.getAllHeaders(), C.STATUS_MESSAGE_COOKIE));
				return Status.ERROR_SERVER;
			}
		} catch (Exception e) {
			lastError = Util.getStackTrace(e);
			return Status.ERROR_INTERNAL;
		} finally {
			safeCloseResponse(response);
		}
	}

	public void logAddTextBody(MultipartEntityBuilder entity, String name, String text) {
		log.log("Adding text body for multipart entity: name=" + name + ", text=" + text);
		entity.addTextBody(name, text);
	}

	// Logs and executes a GET/POST request.
	private HttpResponse executeGetOrPost(HttpRequestBase request) throws IOException {
					// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
					//C.debugMode = false; // goes back to false at the end of the function
					if (C.debugMode) {
						System.out.println( "---- now ENTERING executeGetOrPost() --------\n" );
					}
					// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
		log.log("Sending " + request.getMethod() + " request to URI: " + request.getURI());
		List<Header> headers = Arrays.asList(request.getAllHeaders());
		// Bypass the for loop if debug mode is off.
		if (C.debugMode) {
			log.log("Headers for request: ");
			for (Header header : headers) {
				log.log("\tName: " + header.getName());
				log.log("\tValue: " + header.getValue());
				log.log("");
			}
			CookieStore store = client.getCookieStore();
			List<Cookie> cookies = store.getCookies();
			log.log("Cookies before request: ");
			for (Cookie cookie : cookies) {
				log.log("\tName : " + cookie.getName());
				log.log("\tValue: " + cookie.getValue());
				log.log("");
			}
		}

		request.setHeader("User-Agent", C.USER_AGENT);
		HttpResponse response = client.execute(request);

		if (C.debugMode) {
			log.log("Got response from server.");
			log.log("HTTP Status: " + response.getStatusLine().getStatusCode());
			List<Header> responseHeaders = Arrays.asList(response.getAllHeaders());
			log.log("Headers for response: ");
			for (Header header : responseHeaders) {
				log.log("\tName: " + header.getName());
				log.log("\tValue: " + header.getValue());
				log.log("");
			}
			CookieStore store = client.getCookieStore();
			List<Cookie> cookies = store.getCookies();
			log.log("Cookies after request: ");
			for (Cookie cookie : cookies) {
				log.log("\tName : " + cookie.getName());
				log.log("\tValue: " + cookie.getValue());
				log.log("");
			}
		}
					// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
					if (C.debugMode) {
						System.out.println( "\n---- now EXITING executeGetOrPost() --------\n" );
					}					
					//C.debugMode = false;
					// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
		return response;
	}

	/**
	 * Uploads a new configuration to an existing solver
	 *
	 * @param name The name of the new configuration
	 * @param desc A description of the configuration
	 * @param filePath The file to upload
	 * @param solverID The ID of the solver to attach the configuration to
	 * @return The ID of the new configuration on success (a positive integer),
	 * or a negative error code on failure
	 */
	public int uploadConfiguration(String name, String desc, String filePath, Integer solverID) {
		HttpResponse response = null;
		try {

			HttpPost post = new HttpPost(baseURL + C.URL_UPLOADCONFIG);
			if (desc == null) {
				desc = "";
			}

			MultipartEntityBuilder entity = MultipartEntityBuilder.create();
			logAddTextBody(entity, "solverId", solverID.toString());
			logAddTextBody(entity, "uploadConfigDesc", desc);
			logAddTextBody(entity, "uploadConfigName", name);

			FileBody fileBody = new FileBody(new File(filePath));
			log.log("Adding file part for multipart entity builder.");
			entity.addPart("file", fileBody);
			post = (HttpPost) setHeaders(post);
			post.setEntity(entity.build());

			response = executeGetOrPost(post);

			int id = CommandValidator.getIdOrMinusOne(HTMLParser.extractCookie(response.getAllHeaders(), "New_ID"));
			if (id <= 0) {
				setLastError(HTMLParser.extractCookie(response.getAllHeaders(), C.STATUS_MESSAGE_COOKIE));
				return Status.ERROR_SERVER;
			}

			return id;
		} catch (Exception e) {
			lastError = Util.getStackTrace(e);
			return Status.ERROR_INTERNAL;
		} finally {
			safeCloseResponse(response);
		}
	}

	/**
	 * Uploads a processor to starexec
	 *
	 * @param name The name to give the processor
	 * @param desc A description for the processor
	 * @param filePath An absolute file path to the file to upload
	 * @param communityID The ID of the community that will be given the
	 * processor
	 * @param type Must be "post" or R.BENCHMARK
	 * @return The positive integer ID assigned the new processor on success, or
	 * a negative error code on failure
	 */

	protected int uploadProcessor(String name, String desc, String filePath, Integer communityID, String type) {
		File f = new File(filePath); // file is also required
		HttpResponse response = null;
		try {
			HttpPost post = new HttpPost(baseURL + C.URL_UPLOADPROCESSOR);
			MultipartEntityBuilder entity = MultipartEntityBuilder.create();
			logAddTextBody(entity, "action", "add");
			logAddTextBody(entity, "type", type);
			logAddTextBody(entity, "name", name);
			logAddTextBody(entity, "desc", desc);
			logAddTextBody(entity, "com", communityID.toString());
			logAddTextBody(entity, "uploadMethod", "local");
			FileBody fileBody = new FileBody(f);
			entity.addPart("file", fileBody);

			post.setEntity(entity.build());
			post = (HttpPost) setHeaders(post);

			response = executeGetOrPost(post);

			// we are expecting to be redirected to the page for the processor
			if (response.getStatusLine().getStatusCode() != HttpServletResponse.SC_FOUND) {
				setLastError(HTMLParser.extractCookie(response.getAllHeaders(), C.STATUS_MESSAGE_COOKIE));
				return Status.ERROR_SERVER;
			}
			return Integer.valueOf(HTMLParser.extractCookie(response.getAllHeaders(), "New_ID"));
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		} finally {
			safeCloseResponse(response);
		}

	}

	/**
	 * Uploads a post processor to starexec
	 *
	 * @param name The name to give the post processor
	 * @param desc A description for the processor
	 * @param filePath An absolute file path to the file to upload
	 * @param communityID The ID of the community that will be given the
	 * processor
	 * @return The positive integer ID assigned the new processor on success, or
	 * a negative error code on failure
	 */
	public int uploadPostProc(String name, String desc, String filePath, Integer communityID) {
		return uploadProcessor(name, desc, filePath, communityID, "post");
	}

	/**
	 * Uploads a benchmark processor to starexec
	 *
	 * @param name The name to give the benchmark processor
	 * @param desc A description for the processor
	 * @param filePath An absolute file path to the file to upload
	 * @param communityID The ID of the community that will be given the
	 * processor
	 * @return The positive integer ID assigned the new processor on success, or
	 * a negative error code on failure
	 */
	public int uploadBenchProc(String name, String desc, String filePath, Integer communityID) {
		return uploadProcessor(name, desc, filePath, communityID, R.BENCHMARK);
	}

	/**
	 * Uploads a pre processor to starexec
	 *
	 * @param name The name to give the pre processor
	 * @param desc A description for the processor
	 * @param filePath An absolute file path to the file to upload
	 * @param communityID The ID of the community that will be given the
	 * processor
	 * @return The positive integer ID assigned the new processor on success, or
	 * a negative error code on failure
	 */
	public int uploadPreProc(String name, String desc, String filePath, Integer communityID) {
		return uploadProcessor(name, desc, filePath, communityID, "pre");
	}

	/**
	 * Uploads an xml (job or space) to specified space
	 *
	 * @param filePath An absolute file path to the file to upload
	 * @param spaceID The ID of the space where the job is being uploaded to
	 * @param isJobXML True if this is a job XML and false if it is a space XML
	 * @return The ids of the newly created jobs. On failure, a size 1 list with
	 * a negative error code
	 * @author Julio Cervantes
	 */
	public List<Integer> uploadXML(String filePath, Integer spaceID, boolean isJobXML) {
		List<Integer> ids = new ArrayList<>();
		HttpResponse response = null;
		try {
			String ext = C.URL_UPLOADSPACE;
			if (isJobXML) {
				ext = C.URL_UPLOADJOBXML;
			}
			HttpPost post = new HttpPost(baseURL + ext);
			post = (HttpPost) setHeaders(post);

			MultipartEntityBuilder entity = MultipartEntityBuilder.create();
			logAddTextBody(entity, R.SPACE, spaceID.toString());
			File f = new File(filePath);
			FileBody fileBody = new FileBody(f);
			entity.addPart("f", fileBody);

			post.setEntity(entity.build());

			response = executeGetOrPost(post);

			int code = response.getStatusLine().getStatusCode();
			// if space, gives 200 code. if job, gives 302
			if (code != HttpServletResponse.SC_OK && code != HttpServletResponse.SC_FOUND) {
				setLastError(HTMLParser.extractCookie(response.getAllHeaders(), C.STATUS_MESSAGE_COOKIE));
				ids.add(Status.ERROR_SERVER);
				return ids;
			}

			String[] newIds = HTMLParser.extractMultipartCookie(response.getAllHeaders(), "New_ID");
			for (String s : newIds) {
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
	 * Uploads a solver to Starexec. The description of the solver will be taken
	 * from the archive being uploaded
	 *
	 * @param name The name of the solver
	 * @param desc The description of the solver
	 * @param spaceID The ID of the space to put the solver in
	 * @param filePath the path to the solver archive to upload
	 * @param downloadable True if the solver should be downloadable by other
	 * users, and false otherwise
	 * @param runTestJob Whether to run a test job after uploading this solver
	 * @param settingId The ID of the settings profile to be used if a test job
	 * is created
	 * @param type the type of executable for this upload. See the
	 * StarexecCommand docs for a list of codes
	 * @return The ID of the new solver, which must be positive, or a negative
	 * error code
	 */
	public int uploadSolver(String name, String desc, Integer spaceID, String filePath, Boolean downloadable, Boolean runTestJob, Integer settingId, Integer type) {
		return uploadSolver(name, desc, "text", spaceID, filePath, downloadable, runTestJob, settingId, type);
	}

	/**
	 * Uploads a solver to Starexec. The description of the solver will be taken
	 * from the archive being uploaded
	 *
	 * @param name The name of the solver
	 * @param spaceID The ID of the space to put the solver in
	 * @param filePath the path to the solver archive to upload
	 * @param downloadable True if the solver should be downloadable by other
	 * users, and false otherwise
	 * @param runTestJob Whether to run a test job after uploading this solver
	 * @param settingId The ID of a settings profile to use if a test job is
	 * created
	 * @param type the type of executable for this upload. See the
	 * StarexecCommand docs for a list of codes
	 * @return The ID of the new solver, which must be positive, or a negative
	 * error code
	 */
	public int uploadSolver(String name, Integer spaceID, String filePath, Boolean downloadable, Boolean runTestJob, Integer settingId, Integer type) {
		return uploadSolver(name, "", "upload", spaceID, filePath, downloadable, runTestJob, settingId, type);
	}

	/**
	 * Uploads a solver to StarexecCommand. Called by one of the overloading
	 * methods.
	 *
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
	private int uploadSolver(MultipartEntityBuilder entity, HttpPost post, String name, String desc, String descMethod, Integer spaceID, Boolean downloadable, Boolean runTestJob, Integer settingId, Integer type) {
		HttpResponse response = null;
		try {
			// Only include the description file if we need it
			if (descMethod.equals("file")) {
				FileBody descFileBody = new FileBody(new File(desc));
				entity.addPart("d", descFileBody);
			}

			logAddTextBody(entity, "sn", name);
			if (descMethod.equals("text")) {
				logAddTextBody(entity, "desc", desc);
			} else {
				logAddTextBody(entity, "desc", "");
			}
			logAddTextBody(entity, R.SPACE, spaceID.toString());

			logAddTextBody(entity, "descMethod", descMethod);
			logAddTextBody(entity, "dlable", downloadable.toString());
			logAddTextBody(entity, "runTestJob", runTestJob.toString());
			logAddTextBody(entity, "type", type.toString());
			if (settingId != null) {
				logAddTextBody(entity, "setting", settingId.toString());

			}
			post.setEntity(entity.build());
			post = (HttpPost) setHeaders(post);

			response = executeGetOrPost(post);
			int newID = CommandValidator.getIdOrMinusOne(HTMLParser.extractCookie(response.getAllHeaders(), "New_ID"));
			// if the request was not successful
			if (newID <= 0) {
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
	 * @param name The name of the solver
	 * @param desc If the upload method is "text", then this should be the
	 * description. If it is "file", it should be a filepath to the
	 * needed description file. If it is "upload," it is not needed
	 * @param descMethod Either "text", "upload", or "file"
	 * @param spaceID The ID of the space to put the solver in
	 * @param filePath The path to the solver archive to upload.
	 * @param downloadable True if the solver should be downloadable by other
	 * users, and false otherwise
	 * @param runTestJob Whether to run a test job after uploading this solver
	 * @param settingId The ID of the default settings profile that should be
	 * used for a test job for this job
	 * @return The ID of the new solver, which must be positive, or a negative
	 * error code
	 */
	protected int uploadSolver(String name, String desc, String descMethod, Integer spaceID, String filePath, Boolean downloadable, Boolean runTestJob, Integer settingId, Integer type) {
		try {
			HttpPost post = new HttpPost(baseURL + C.URL_UPLOADSOLVER);
			MultipartEntityBuilder entity = MultipartEntityBuilder.create();
			FileBody fileBody = new FileBody(new File(filePath));
			entity.addPart("f", fileBody);
			logAddTextBody(entity, "url", "");
			logAddTextBody(entity, "upMethod", "local");

			return uploadSolver(entity, post, name, desc, descMethod, spaceID, downloadable, runTestJob, settingId, type);
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * Uploads a solver to Starexec given a URL.
	 *
	 * @param name The name to give the solver
	 * @param desc Either a string description OR a file path to a file
	 * containing the description, depending on the value of descMethod
	 * @param descMethod The method by which a description is being provided,
	 * which is either 'file' or 'text'
	 * @param spaceID The space to put the solver in
	 * @param url The direct URL to the solver
	 * @param downloadable Whether the solver should be downloadable or not
	 * @param runTestJob Whether to run a test job for this solver after
	 * uploading it
	 * @param settingId The ID of the settings profile that will be used if we
	 * want to run a test job\
	 * @param type the type of executable for this upload. See the
	 * StarexecCommand docs for a list of codes
	 * @return The positive ID for the solver or a negative error code
	 */
	public int uploadSolverFromURL(String name, String desc, String descMethod, Integer spaceID, String url, Boolean downloadable, Boolean runTestJob, Integer settingId, Integer type) {
		try {
			HttpPost post = new HttpPost(baseURL + C.URL_UPLOADSOLVER);
			MultipartEntityBuilder entity = MultipartEntityBuilder.create();
			logAddTextBody(entity, "url", url);
			logAddTextBody(entity, "upMethod", "URL");

			return uploadSolver(entity, post, name, desc, descMethod, spaceID, downloadable, runTestJob, settingId, type);
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * Uploads a solver to Starexec. The description of the solver will be taken
	 * from the archive being uploaded
	 *
	 * @param name The name of the solver
	 * @param spaceID The ID of the space to put the solver in
	 * @param url The URL of hte archived solver to upload
	 * @param downloadable True if the solver should be downloadable by other
	 * users, and false otherwise
	 * @param runTestJob Whether to run a test job for this solver after
	 * uploading it
	 * @param type The type of the executable being uploaded. See Starexec
	 * Command docs for a list of codes
	 * @param settingId The ID of the settings profile that will be used if we
	 * want to run a test job
	 * @return The ID of the new solver, which must be positive, or a negative
	 * error code
	 */
	public int uploadSolverFromURL(String name, Integer spaceID, String url, Boolean downloadable, Boolean runTestJob, Integer settingId, Integer type) {
		return uploadSolverFromURL(name, "", "upload", spaceID, url, downloadable, runTestJob, settingId, type);
	}

	/**
	 * Sets HTTP headers required to communicate with the StarExec server
	 *
	 * @param msg --The outgoing HTTP request, likely an HttpGet or HttpPost
	 * @return msg with required headers added
	 * @author Eric Burns
	 */

	private AbstractHttpMessage setHeaders(AbstractHttpMessage msg) {
		msg.addHeader("StarExecCommand", "StarExecCommand");
		msg.addHeader("Connection", "keep-alive");
		msg.addHeader("Accept-Language", "en-US,en;q=0.5");

		return msg;
	}

	/**
	 * Changes your first name on StarExec to the given value
	 *
	 * @param name The new name
	 * @return 0 on success or a negative error code on failure
	 */

	public int setFirstName(String name) {
		return this.setUserSetting("firstname", name);
	}

	/**
	 * Changes your last name on StarExec to the given value
	 *
	 * @param name The new name
	 * @return 0 on success or a negative error code on failure
	 */
	public int setLastName(String name) {
		return this.setUserSetting("lastname", name);
	}

	/**
	 * Changes your institution on StarExec to the given value
	 *
	 * @param inst The name of the new institution
	 * @return 0 on success or a negative error code on failure
	 */

	public int setInstitution(String inst) {
		return this.setUserSetting("institution", inst);
	}

	/**
	 * Deletes all of the given solvers permanently
	 *
	 * @param ids The IDs of each solver to delete
	 * @return 0 on success or a negative error code on failure
	 */

	public int deleteSolvers(List<Integer> ids) {
		return deletePrimitives(ids, R.SOLVER);
	}

	/**
	 * Deletes all of the given benchmarks permanently
	 *
	 * @param ids The IDs of each benchmark to delete
	 * @return 0 on success or a negative error code on failure
	 */

	public int deleteBenchmarks(List<Integer> ids) {
		return deletePrimitives(ids, "benchmark");
	}

	/**
	 * Deletes all of the given processors permanently
	 *
	 * @param ids The IDs of each processor to delete
	 * @return 0 on success or a negative error code on failure
	 */

	public int deleteProcessors(List<Integer> ids) {
		return deletePrimitives(ids, "processor");
	}

	/**
	 * Deletes all of the given configurations permanently
	 *
	 * @param ids The IDs of each configuration to delete
	 * @return 0 on success or a negative error code on failure
	 */

	public int deleteConfigurations(List<Integer> ids) {
		return deletePrimitives(ids, "configuration");
	}

	/**
	 * Deletes all of the given jobs permanently
	 *
	 * @param ids The IDs of each job to delete
	 * @return 0 on success or a negative error code on failure
	 */
	public int deleteJobs(List<Integer> ids) {
		return deletePrimitives(ids, R.JOB);
	}

	/**
	 * Deletes all of the given primitives of the given type
	 *
	 * @param ids IDs of some primitive type
	 * @param type The type of primitives being deleted
	 * @return 0 on success or a negative error code on failure
	 */

	protected int deletePrimitives(List<Integer> ids, String type) {
		HttpResponse response = null;
		try {
			HttpPost post = new HttpPost(baseURL + C.URL_DELETEPRIMITIVE + "/" + type);
			post = (HttpPost) setHeaders(post);
			List<NameValuePair> params = new ArrayList<>();
			for (Integer id : ids) {
				params.add(new BasicNameValuePair("selectedIds[]", id.toString()));
			}
			post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

			response = executeGetOrPost(post);
			JsonObject obj = JsonHandler.getJsonObject(response);
			boolean success = JsonHandler.getSuccessOfResponse(obj);
			String message = JsonHandler.getMessageOfResponse(obj);

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
	 * Checks to see whether the given page can be retrieved normally, meaning
	 * we get back HTTP status code 200
	 *
	 * @param relURL The URL following starexecRoot
	 * @return True if the page was retrieved successfully and false otherwise
	 */
	public boolean canGetPage(String relURL) {
		HttpResponse response = null;
		try {
			HttpGet get = new HttpGet(baseURL + relURL);
			get = (HttpGet) setHeaders(get);

			response = executeGetOrPost(get);

			// we should get 200, which is the code for ok
			return response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK;

		} catch (Exception e) {

		} finally {
			safeCloseResponse(response);
		}
		return false;
	}

	/**
	 * Gets the HTML of a page using the open connection.
	 *
	 * @param relUrl The URL following starexecRoot
	 * @return the HTML as a String
	 * @throws IOException if the request fails.
	 */
	public HtmlStatusCodePair getPageHtml(String relUrl) throws IOException {
		HttpResponse response = null;
		try {
			HttpGet get = new HttpGet(baseURL + relUrl);
			get = (HttpGet) setHeaders(get);

			response = executeGetOrPost(get);
			final String html = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
			final int code = response.getStatusLine().getStatusCode();
			return new HtmlStatusCodePair(html, code);
		} finally {
			safeCloseResponse(response);
		}
	}

	/**
	 * Gets the ID of the user currently logged in to StarExec
	 *
	 * @return The integer user ID
	 */
	public int getUserID() {
		HttpResponse response = null;
		try {
			HttpGet get = new HttpGet(baseURL + C.URL_GETID);
			get = (HttpGet) setHeaders(get);

			response = executeGetOrPost(get);
			JsonElement json = JsonHandler.getJsonString(response);
			return json.getAsInt();

		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		} finally {
			safeCloseResponse(response);
		}
	}

	/**
	 * Sets a space or hierarchy to be public or private
	 *
	 * @param spaceID The ID of the individual space or the root of the
	 * hierarchy to work on
	 * @param hierarchy True if working on a hierarchy, false if a single space
	 * @param setPublic True if making the space(s) public, false if private
	 * @return 0 on success or a negative error code otherwise
	 */
	protected int setSpaceVisibility(Integer spaceID, Boolean hierarchy, Boolean setPublic) {
		HttpResponse response = null;
		try {
			HttpPost post = new HttpPost(baseURL + C.URL_EDITSPACEVISIBILITY + "/" + spaceID.toString() + "/" + hierarchy.toString() + "/" + setPublic.toString());
			post = (HttpPost) setHeaders(post);
			response = executeGetOrPost(post);

			// we should get back an HTTP OK if we're allowed to change the visibility
			if (response.getStatusLine().getStatusCode() != HttpServletResponse.SC_OK) {
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
	 * Changes one of the settings for a given user (like name, institution, or
	 * so on)
	 *
	 * @param setting The name of the setting
	 * @param val The new value for the setting
	 * @return
	 */

	protected int setUserSetting(String setting, String val) {
		HttpResponse response = null;
		try {
			int userId = getUserID();
			String url = baseURL + C.URL_USERSETTING + setting + "/" + userId + "/" + val;
			url = url.replace(" ", "%20"); // encodes white space, which can't
			// be used in a URL
			HttpPost post = new HttpPost(url);
			post = (HttpPost) setHeaders(post);
			response = executeGetOrPost(post);
			JsonObject obj = JsonHandler.getJsonObject(response);

			boolean success = JsonHandler.getSuccessOfResponse(obj);
			String message = JsonHandler.getMessageOfResponse(obj);
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
	 * Reruns the job with the given ID
	 *
	 * @param jobID The ID of the job to rerun
	 * @return An integer status code as definied in Status.java
	 */

	public int rerunJob(Integer jobID) {
		return makePostWithId(baseURL + C.URL_RERUNJOB, jobID);
	}

	/**
	 * Reruns the job pair with the given ID
	 *
	 * @param pairID The ID of the pair to rerun
	 * @return A status code as defined in status.java
	 */

	public int rerunPair(Integer pairID) {
		return makePostWithId(baseURL + C.URL_RERUNPAIR, pairID);
	}

	private int makePostWithId(final String url, final Integer id) {
		HttpResponse response = null;
		try {
			String URL = url;
			URL = URL.replace("{id}", id.toString());
			HttpPost post = new HttpPost(URL);
			post = (HttpPost) setHeaders(post);
			post.setEntity(new UrlEncodedFormEntity(new ArrayList<>(), "UTF-8"));
			response = executeGetOrPost(post);
			JsonObject obj = JsonHandler.getJsonObject(response);

			boolean success = JsonHandler.getSuccessOfResponse(obj);
			String message = JsonHandler.getMessageOfResponse(obj);

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
	 *
	 * @param pause Pauses a job if true and resumes it if false
	 * @return 0 on success or a negative error code on failure
	 */

	protected int pauseOrResumeJob(Integer jobID, boolean pause) {
		try {
			String URL = baseURL + C.URL_PAUSEORRESUME;
			if (pause) {
				URL = URL.replace("{method}", "pause");
			} else {
				URL = URL.replace("{method}", "resume");
			}
			return makePostWithId(URL, jobID);
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * Removes the given solvers from the given space. The solvers are NOT
	 * deleted.
	 *
	 * @param solverIds The IDs of the solvers to remove
	 * @param spaceID The ID of the space
	 * @return 0 on success, or a negative integer status code on failure
	 */
	public int removeSolvers(List<Integer> solverIds, Integer spaceID) {
		return removePrimitives(solverIds, spaceID, R.SOLVER, false);
	}

	/**
	 * Removes the given jobs from the given space. The jobs are NOT deleted.
	 *
	 * @param jobIds The IDs of the jobs to remove
	 * @param spaceID The ID of the space
	 * @return 0 on success, or a negative integer status code on failure
	 */

	public int removeJobs(List<Integer> jobIds, Integer spaceID) {
		return removePrimitives(jobIds, spaceID, R.JOB, false);
	}

	/**
	 * Removes the given benchmarks from the given space. The benchmarks are NOT
	 * deleted.
	 *
	 * @param benchmarkIds The IDs of the benchmarks to remove
	 * @param spaceID The ID of the space
	 * @return 0 on success, or a negative integer status code on failure
	 */

	public int removeBenchmarks(List<Integer> benchmarkIds, Integer spaceID) {
		return removePrimitives(benchmarkIds, spaceID, "benchmark", false);
	}

	/**
	 * Removes the given subspaces from the given space.
	 *
	 * @param subspaceIds The IDs of the subspaces to remove
	 * @param recyclePrims If true, all primitives owned by the calling user
	 * that are present in any space being removed will be deleted (or
	 * moved to the recycle bin, if applicable)
	 * @return 0 on success, or a negative integer status code on failure
	 */

	public int removeSubspace(List<Integer> subspaceIds, Boolean recyclePrims) {
		return removePrimitives(subspaceIds, null, "subspace", recyclePrims);
	}

	/**
	 * Removes the association between a primitive and a space on StarExec
	 *
	 * @param type The type of primitive being remove
	 * @return 0 on success, and a negative error code on failure
	 * @author Eric Burns
	 */
	protected int removePrimitives(List<Integer> primIDs, Integer spaceID, String type, Boolean recyclePrims) {
		HttpResponse response = null;
		try {
			HttpPost post;
			if (type.equalsIgnoreCase("subspace")) {
				post = new HttpPost(baseURL + C.URL_REMOVEPRIMITIVE + "/" + type);

			} else {
				post = new HttpPost(baseURL + C.URL_REMOVEPRIMITIVE + "/" + type + "/" + spaceID.toString());

			}
			// first sets username and password data into HTTP POST request
			List<NameValuePair> params = new ArrayList<>();
			String key = "selectedIds[]";
			for (Integer id : primIDs) {
				params.add(new BasicNameValuePair(key, id.toString()));
			}

			params.add(new BasicNameValuePair("recyclePrims", recyclePrims.toString()));
			post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

			response = executeGetOrPost(post);

			JsonObject obj = JsonHandler.getJsonObject(response);

			boolean success = JsonHandler.getSuccessOfResponse(obj);
			String message = JsonHandler.getMessageOfResponse(obj);
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
	 *
	 * @return True on success, false otherwise
	 * @author Eric Burns
	 */

	public boolean logout() {
		HttpResponse response = null;
		try {
			HttpPost post = new HttpPost(baseURL + C.URL_LOGOUT);
			post = (HttpPost) setHeaders(post);
			response = executeGetOrPost(post);
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			safeCloseResponse(response);
		}
	}

	/**
	 * Log into StarExec with the username and password of this connection
	 *
	 * @return An integer indicating status, with 0 being normal and a negative
	 * integer indicating an error
	 * @author Eric Burns
	 */
	public int login() {
		HttpResponse response = null;

					// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
					// System.out.println( "now running login()\n" );
					// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

		try {
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// System.out.println( "at the top of try block\n" );
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			log.log("Logging in...");
			HttpGet get = new HttpGet(baseURL + C.URL_HOME);
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// System.out.println( "creating the first 'get' with " + baseURL + C.URL_HOME + "\n" );
						// for ( Header header : get.getAllHeaders() ) {
						// 	System.out.println( "\t" + header.getName() + " : " + header.getValue() );
						// }
						// System.out.println();
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


			response = executeGetOrPost(get);
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// System.out.println( "response contents after =executeGetOrPost(get):" );
						// for ( Header header : response.getAllHeaders() ) {
						// 	System.out.println( "\t" + header.getName() + " : " + header.getValue() );
						// }
						// System.out.println();
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			sessionID = HTMLParser.extractCookie(response.getAllHeaders(), C.TYPE_SESSIONID);
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// System.out.println( "sessionID after =extractCookie():\n" + sessionID + "\n" );
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			log.log("Set Session ID to: " + sessionID);
			response.getEntity().getContent().close();

						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// System.out.println( "this.isValid(): " + this.isValid() + "\n" );
						// System.out.println( "baseURL: " + baseURL + "; C.URL_STAREXEC_BASE: " + C.URL_STAREXEC_BASE + "\n" );
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			if (!this.isValid()) {
				// if the user specified their own URL, it is probably the
				// problem.
				if (!baseURL.equals(C.URL_STAREXEC_BASE)) {
					return Status.ERROR_BAD_URL;
				}
				return Status.ERROR_INTERNAL;
			}

			// first sets username and password data into HTTP POST request
			List<NameValuePair> params = new ArrayList<>(3);

			params.add(new BasicNameValuePair("j_username", username));
			params.add(new BasicNameValuePair("j_password", password));
			params.add(new BasicNameValuePair("cookieexists", "false"));
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// System.out.println( "params.toString() after the .add()s:\n" + params.toString() + "\n" );
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			HttpPost post = new HttpPost(baseURL + C.URL_LOGIN);
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// System.out.println( "creating the first 'post' with " + baseURL + C.URL_LOGIN + "\n" );
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(params, "UTF-8");
						// post.setEntity( urlEncodedFormEntity );
						// System.out.println( "urlEncodedFormEntity: "+convertStreamToString( urlEncodedFormEntity.getContent() )+"\n" );
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			post = (HttpPost) setHeaders(post);
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// System.out.println( "post contents after =setHeaders(post):" );
						// for ( Header header : post.getAllHeaders() ) {
						// 	System.out.println( "\t" + header.getName() + " : " + header.getValue() );
						// }
						// System.out.println();
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			// Post login credentials to server
			response = executeGetOrPost(post);
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// System.out.println( "response contents after =executeGetOrPost(post):" );
						// for ( Header header : response.getAllHeaders() ) {
						// 	System.out.println( "\t" + header.getName() + " : " + header.getValue() );
						// }
						// System.out.println();
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			// -- fix -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
			// instead of trying to assign to sessionID after the second GET request, do it here, using the response
			// from the POST request
			//sessionID = HTMLParser.extractCookie(response.getAllHeaders(), C.TYPE_SESSIONID);
			// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			response.getEntity().getContent().close();

			// On success, starexec will try to redirect, but we don't want that
			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);

			get = new HttpGet(baseURL + C.URL_HOME);
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// System.out.println( "creating second 'get' with " + baseURL + C.URL_HOME + "\n" );
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			get = (HttpGet) setHeaders(get);

			response = executeGetOrPost(get);
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// System.out.println( "response contents after =executeGetOrPost(get):" );
						// for ( Header header : response.getAllHeaders() ) {
						// 	System.out.println( "\t" + header.getName() + " : " + header.getValue() );
						// }
						// System.out.println();
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			log.log("Set Session ID to: " + sessionID);

			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, true);

			safeCloseResponse(response);

			get = new HttpGet(baseURL + C.URL_LOGGED_IN); // first get to have C.URL_LOGGED_IN
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// System.out.println( "creating third 'get' with " + baseURL + C.URL_LOGGED_IN + "\n" );
						// System.out.println( "get contents after =new HttpGet():" );
						// for ( Header header : get.getAllHeaders() ) {
						// 	System.out.println( header.getName() + " : " + header.getValue() );
						// }
						// System.out.println();
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			get = (HttpGet) setHeaders(get);

			response = executeGetOrPost(get);
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// System.out.println( "response contents after =executeGetOrPost(get):" );
						// for ( Header header : response.getAllHeaders() ) {
						// 	System.out.println( "\t" + header.getName() + " : " + header.getValue() );
						// }
						// System.out.println();
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			// previous location of sessionID assignment (before 5/25/20 ish)
			// sessionID = HTMLParser.extractCookie(response.getAllHeaders(), C.TYPE_SESSIONID);

			boolean loggedIn = convertStreamToString(response.getEntity().getContent()).equals("true");
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
//						String tmpString = convertStreamToString(response.getEntity().getContent());
//						boolean loggedIn = tmpString.equals( "true" );
//						System.out.println( "entity as string: " + tmpString );
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			if (loggedIn) {
				log.log("Service says we're logged in.");
			} else {
				log.log("Service says we're not logged in.");
			}
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// System.out.println( "sessionID: " + sessionID );
						// System.out.println( "loggedIn: " + loggedIn + "\n" );
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^




			// this means that the server did not give us a new session for the
			// login
			if (sessionID == null || !loggedIn) {
							// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
							// System.out.println( "(sessionID == null || !loggedIn): " + true + "\n" );
							// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

				log.log("Returning bad credentials message.");
							// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
							// System.out.println("in catch block; returning Status.ERROR_BAD_CREDENTIALS\n");
							// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

				return Status.ERROR_BAD_CREDENTIALS;
			}
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// System.out.println("returning 0\n");
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			return 0;

		} catch (IllegalStateException e) {
			log.log("Caught IllegalStateException: " + Util.getStackTrace(e));
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// System.out.println("in catch block; returning Status.ERROR_BAD_URL\n");
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			return Status.ERROR_BAD_URL;

		} catch (Exception e) {
			log.log("Caught Exception: " + Util.getStackTrace(e));
			setLastError(e.getMessage() + ": " + Util.getStackTrace(e));
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// System.out.println("in catch block; returning Status.ERROR_INTERNAL_EXCEPTION");
						// System.out.println( e.getMessage() + "\n" );
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			return Status.ERROR_INTERNAL_EXCEPTION;
		} finally {
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// System.out.println("at the top of finally block\n");
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

			safeCloseResponse(response);
						// -- debug -- vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
						// System.out.println( "response.toString() after safeCloseResponse(response):\n" + response.toString() + "\n" );
						// System.out.println("-- END --");
						// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

		}

	}

	/**
	 * Links solvers to a new space
	 *
	 * @param solverIds The solver Ids to be added to a new space
	 * @param oldSpaceId The space they are being linked from, or null if none
	 * exists
	 * @param newSpaceId The ID of the space they are being linked to
	 * @param hierarchy Whether to link to the entire hierarchy
	 * @return 0 on success or a negative status code on error
	 */

	public int linkSolvers(Integer[] solverIds, Integer oldSpaceId, Integer newSpaceId, Boolean hierarchy) {
		return linkPrimitives(solverIds, oldSpaceId, newSpaceId, hierarchy, R.SOLVER);
	}

	/**
	 * Links benchmark to a new space
	 *
	 * @param benchmarkIds The benchmark Ids to be added to a new space
	 * @param oldSpaceId The space they are being linked from, or null if none
	 * exists
	 * @param newSpaceId The ID of the space they are being linked to
	 * @return 0 on success or a negative status code on error
	 */

	public int linkBenchmarks(Integer[] benchmarkIds, Integer oldSpaceId, Integer newSpaceId) {
		return linkPrimitives(benchmarkIds, oldSpaceId, newSpaceId, false, "benchmark");
	}

	/**
	 * Links users to a new space
	 *
	 * @param userIds The user Ids to be added to a new space
	 * @param oldSpaceId The space they are being linked from, or null if none
	 * exists
	 * @param newSpaceId The ID of the space they are being linked to
	 * @return 0 on success or a negative status code on error
	 */
	public int linkUsers(Integer[] userIds, Integer oldSpaceId, Integer newSpaceId) {
		return linkPrimitives(userIds, oldSpaceId, newSpaceId, false, "user");
	}

	/**
	 * Copies solvers to a new space
	 *
	 * @param solverIds The solver Ids to be added to a new space
	 * @param oldSpaceId The space they are being linked from, or null if none
	 * exists
	 * @param newSpaceId The ID of the space they are being linked to
	 * @param hierarchy Whether to link to the entire hierarchy
	 * @return 0 on success or a negative status code on error
	 */

	public List<Integer> copySolvers(Integer[] solverIds, Integer oldSpaceId, Integer newSpaceId, Boolean hierarchy) {
		return copyPrimitives(solverIds, oldSpaceId, newSpaceId, hierarchy, false, R.SOLVER);
	}

	/**
	 * Copies benchmarks to a new space
	 *
	 * @param benchmarkIds The benchmark Ids to be added to a new space
	 * @param oldSpaceId The space they are being linked from, or null if none
	 * exists
	 * @param newSpaceId The ID of the space they are being linked to
	 * @return 0 on success or a negative status code on error
	 */

	public List<Integer> copyBenchmarks(Integer[] benchmarkIds, Integer oldSpaceId, Integer newSpaceId) {
		return copyPrimitives(benchmarkIds, oldSpaceId, newSpaceId, false, false, "benchmark");
	}

	/**
	 * Copies spaces to a new space
	 *
	 * @param spaceIds The space Ids to be added to a new space
	 * @param oldSpaceId The space they are being linked from, or null if none
	 * exists
	 * @param newSpaceId The ID of the space they are being linked to
	 * @param hierarchy Whether to copy every space in the hierarchies rooted at
	 * the given spaces
	 * @return 0 on success or a negative status code on error
	 */

	public List<Integer> copySpaces(Integer[] spaceIds, Integer oldSpaceId, Integer newSpaceId, Boolean hierarchy, Boolean copyPrimitives) {
		return copyPrimitives(spaceIds, oldSpaceId, newSpaceId, hierarchy, copyPrimitives, R.SPACE);
	}

	/**
	 * Copies all the primitives of the given types
	 *
	 * @param ids The IDs of the primitives to copy
	 * @param oldSpaceId A space where the primitives currently reside, or null
	 * if there is no old space
	 * @param newSpaceID The ID of the space to put all the primitives in
	 * @param hierarchy (only for solvers) True if copying the primitives to
	 * each space in a hierarchy (only 1 new prim is created per ID)
	 * @param type The type of the primitives
	 * @return A list of positive IDs on success, or size 1 list with a negative
	 * error code on failure
	 */

	protected List<Integer> copyPrimitives(Integer[] ids, Integer oldSpaceId, Integer newSpaceID, Boolean hierarchy, Boolean copyPrimitives, String type) {
		return copyOrLinkPrimitives(ids, oldSpaceId, newSpaceID, true, hierarchy, copyPrimitives, type);
	}

	/**
	 * Links all the primitives of the given types
	 *
	 * @param ids The IDs of the primitives to link
	 * @param oldSpaceId A space where the primitives currently reside, or null
	 * if there is no old space
	 * @param newSpaceID The ID of the space to put all the primitives in
	 * @param hierarchy (only for solvers and users) True if linking the
	 * primitives to each space in a hierarchy (only 1 new prim is
	 * created per ID)
	 * @param type The type of the primitives
	 * @return 0 on success or a negative error code on failure
	 */

	protected int linkPrimitives(Integer[] ids, Integer oldSpaceId, Integer newSpaceID, Boolean hierarchy, String type) {

		return copyOrLinkPrimitives(ids, oldSpaceId, newSpaceID, false, hierarchy, false, type).get(0);
	}

	/**
	 * Sends a copy or link request to the StarExec server and returns a status
	 * code indicating the result of the request
	 *
	 * @param copy True if a copy should be performed, and false if a link
	 * should be performed.
	 * @param type The type of primitive being copied.
	 * @return An integer error code where 0 indicates success and a negative
	 * number is an error.
	 */
	private List<Integer> copyOrLinkPrimitives(Integer[] ids, Integer oldSpaceId, Integer newSpaceID, Boolean copy, Boolean hierarchy, Boolean copyPrimitives, String type) {
		List<Integer> fail = new ArrayList<>();
		HttpResponse response = null;
		try {
			String urlExtension;
			switch (type) {
			case R.SOLVER:
				urlExtension = C.URL_COPYSOLVER;
				break;
			case R.SPACE:
				urlExtension = C.URL_COPYSPACE;
				break;
			case R.JOB:
				urlExtension = C.URL_COPYJOB;
				break;
			case "user":
				urlExtension = C.URL_COPYUSER;
				break;
			default:
				urlExtension = C.URL_COPYBENCH;
				break;
			}

			urlExtension = urlExtension.replace("{spaceID}", newSpaceID.toString());

			HttpPost post = new HttpPost(baseURL + urlExtension);

			List<NameValuePair> params = new ArrayList<>(3);

			// not all of the following are needed for every copy request, but
			// including them does no harm
			// and allows all the copy commands to be handled by this function
			params.add(new BasicNameValuePair("copyToSubspaces", hierarchy.toString()));
			if (oldSpaceId != null) {
				params.add(new BasicNameValuePair("fromSpace", oldSpaceId.toString()));
			}
			for (Integer id : ids) {
				params.add(new BasicNameValuePair("selectedIds[]", id.toString()));
			}

			params.add(new BasicNameValuePair("copy", copy.toString()));
			params.add(new BasicNameValuePair("copyHierarchy", String.valueOf(hierarchy.toString())));

			CopyPrimitivesOption copyPrimitivesOption = copyPrimitives ? CopyPrimitivesOption.COPY : CopyPrimitivesOption.LINK;

			params.add(new BasicNameValuePair("copyPrimitives", String.valueOf(copyPrimitivesOption.toString())));
			post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

			post = (HttpPost) setHeaders(post);

			response = executeGetOrPost(post);
			JsonObject obj = JsonHandler.getJsonObject(response);

			boolean success = JsonHandler.getSuccessOfResponse(obj);
			String message = JsonHandler.getMessageOfResponse(obj);
			if (success) {
				List<Integer> newPrimIds = new ArrayList<>();
				String[] newIds = HTMLParser.extractMultipartCookie(response.getAllHeaders(), "New_ID");
				if (newIds != null) {
					for (String s : newIds) {
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
	 *
	 * @param name The name to give the new space
	 * @param desc The description to give the new space
	 * @param parentSpaceID The ID of the parent space for the new space
	 * @param p The permission object reflecting all the default permissions for
	 * the new space
	 * @param locked Whether the space should be locked initially
	 * @return the new space ID on success and a negative error code otherwise
	 * @author Eric Burns
	 */

	public int createSubspace(String name, String desc, Integer parentSpaceID, Permission p, Boolean locked) {
		HttpResponse response = null;
		try {
			// first sets username and password data into HTTP POST request
			List<NameValuePair> params = new ArrayList<>(3);
			params.add(new BasicNameValuePair("parent", parentSpaceID.toString()));
			params.add(new BasicNameValuePair("name", name));
			params.add(new BasicNameValuePair("desc", desc));
			params.add(new BasicNameValuePair("locked", locked.toString()));

			for (String x : p.getOnPermissions()) {
				params.add(new BasicNameValuePair(x, "on"));

			}

			HttpPost post = new HttpPost(baseURL + C.URL_ADDSPACE);
			post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
			post = (HttpPost) setHeaders(post);

			response = executeGetOrPost(post);
			if (response.getStatusLine().getStatusCode() != HttpServletResponse.SC_FOUND) {
				return Status.ERROR_BAD_PARENT_SPACE;
			}
			return Integer.valueOf(HTMLParser.extractCookie(response.getAllHeaders(), "New_ID"));
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		} finally {
			safeCloseResponse(response);
		}
	}

	/**
	 * Gets a Map that maps the IDs of solvers to their names for all
	 * solvers in the given space
	 *
	 * @param spaceID The ID of the space
	 * @return A Map mapping IDs to names If there was an error, the Map
	 * will contain only one key, and it will be negative, whereas all
	 * IDs must be positive.
	 */

	public Map<Integer, String> getSolversInSpace(Integer spaceID) {
		return listPrims(spaceID, null, false, "solvers");
	}

	/**
	 * Gets a Map that maps the IDs of solvers to their names for all
	 * benchmarks in the given space
	 *
	 * @param spaceID The ID of the space
	 * @return A Map mapping IDs to names If there was an error, the Map
	 * will contain only one key, and it will be negative, whereas all
	 * IDs must be positive.
	 */

	public Map<Integer, String> getBenchmarksInSpace(Integer spaceID) {
		return listPrims(spaceID, null, false, "benchmarks");
	}

	/**
	 * Gets a Map that maps the IDs of solvers to their names for all jobs
	 * in the given space
	 *
	 * @param spaceID The ID of the space
	 * @return A Map mapping IDs to names If there was an error, the Map
	 * will contain only one key, and it will be negative, whereas all
	 * IDs must be positive.
	 */

	public Map<Integer, String> getJobsInSpace(Integer spaceID) {
		return listPrims(spaceID, null, false, "jobs");
	}

	/**
	 * Gets a Map that maps the IDs of solvers to their names for all users
	 * in the given space
	 *
	 * @param spaceID The ID of the space
	 * @return A Map mapping IDs to names If there was an error, the Map
	 * will contain only one key, and it will be negative, whereas all
	 * IDs must be positive.
	 */

	public Map<Integer, String> getUsersInSpace(Integer spaceID) {
		return listPrims(spaceID, null, false, "users");
	}

	/**
	 * Gets a Map that maps the IDs of solvers to their names for all spaces
	 * in the given space
	 *
	 * @param spaceID The ID of the space
	 * @return A Map mapping IDs to names If there was an error, the Map
	 * will contain only one key, and it will be negative, whereas all
	 * IDs must be positive.
	 */

	public Map<Integer, String> getSpacesInSpace(Integer spaceID) {
		return listPrims(spaceID, null, false, "spaces");
	}

	/**
	 * Gets a Map that maps the IDs of solvers to their names for all
	 * solvers the current user owns
	 *
	 * @return A Map mapping IDs to names If there was an error, the Map
	 * will contain only one key, and it will be negative, whereas all
	 * IDs must be positive.
	 */

	public Map<Integer, String> getSolversByUser() {
		return listPrims(null, null, true, "solvers");
	}

	/**
	 * Gets a Map that maps the IDs of solvers to their names for all
	 * benchmarks the current user owns
	 *
	 * @return A Map mapping IDs to names If there was an error, the Map
	 * will contain only one key, and it will be negative, whereas all
	 * IDs must be positive.
	 */

	public Map<Integer, String> getBenchmarksByUser() {
		return listPrims(null, null, true, "benchmarks");
	}

	/**
	 * Gets a Map that maps the IDs of solvers to their names for all jobs
	 * the current user owns
	 *
	 * @return A Map mapping IDs to names If there was an error, the Map
	 * will contain only one key, and it will be negative, whereas all
	 * IDs must be positive.
	 */
	public Map<Integer, String> getJobsByUser() {
		return listPrims(null, null, true, "jobs");
	}

	/**
	 * @param solverID Integer id of a solver
	 * @param limit Integer limiting number of configurations displayed
	 * @return A Map mapping IDs to names If there was an error, the Map
	 * will contain only one key, and it will be negative, whereas all
	 * IDs must be positive.
	 */
	protected Map<Integer, String> getSolverConfigs(Integer solverID, Integer limit) {
		Map<Integer, String> errorMap = new HashMap<>();
		Map<Integer, String> prims = new HashMap<>();
		HttpResponse response = null;
		try {
			String serverURL = baseURL + C.URL_GETSOLVERCONFIGS;

			HttpPost post = new HttpPost(serverURL);
			post = (HttpPost) setHeaders(post);

			List<NameValuePair> urlParameters = new ArrayList<>();
			urlParameters.add(new BasicNameValuePair("solverid", solverID.toString()));
			if (limit != null) {
				urlParameters.add(new BasicNameValuePair("limit", limit.toString()));
			} else {
				// -1 is a null value
				urlParameters.add(new BasicNameValuePair("limit", "-1"));
			}
			post.setEntity(new UrlEncodedFormEntity(urlParameters));

			response = executeGetOrPost(post);

			final BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

			String line;
			while ((line = br.readLine()) != null) {
				System.out.println(line);

			}

			int code = response.getStatusLine().getStatusCode();
			// if space, gives 200 code. if job, gives 302
			if (code != HttpServletResponse.SC_OK && code != HttpServletResponse.SC_FOUND) {
				System.out.println("Connection.java : " + code + " " + response.getStatusLine().getReasonPhrase());

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
	 * Lists the IDs and names of some kind of primitives in a given space
	 *
	 * @return A Map mapping integer ids to string names
	 * @author Eric Burns
	 */
	protected Map<Integer, String> listPrims(Integer spaceID, Integer limit, boolean forUser, String type) {
		Map<Integer, String> errorMap = new HashMap<>();
		Map<Integer, String> prims = new HashMap<>();
		HttpResponse response = null;
		try {

			Map<String, String> urlParams = new HashMap<>();

			urlParams.put(C.FORMPARAM_TYPE, type);

			String URL;
			if (forUser) {
				int id = getUserID();
				if (id < 0) {
					errorMap.put(id, null);
					return errorMap;
				}

				urlParams.put(C.FORMPARAM_ID, String.valueOf(id));
				URL = baseURL + C.URL_GETUSERPRIM;
			} else {
				urlParams.put(C.FORMPARAM_ID, spaceID.toString());
				URL = baseURL + C.URL_GETPRIM;
			}
			// in the absence of limit, we want all the primitives
			int maximum = Integer.MAX_VALUE;
			if (limit != null) {
				maximum = limit;
			}

			// need to specify the number of columns according to what
			// GetNextPageOfPrimitives in RESTHelpers
			// expects
			String columns = "0";
			switch (type) {
			case "solvers":
				columns = "2";
				break;
			case "users":
				columns = "3";
				break;
			case "benchmarks":
				columns = "2";
				break;
			case "jobs":
				columns = "6";
				break;
			case "spaces":
				columns = "2";
				break;
			}

			URL = URL.replace("{id}", urlParams.get(C.PARAM_ID));
			URL = URL.replace("{type}", urlParams.get("type"));
			HttpPost post = new HttpPost(URL);
			post = (HttpPost) setHeaders(post);

			List<NameValuePair> params = new ArrayList<>();

			params.add(new BasicNameValuePair("sEcho", "1"));
			params.add(new BasicNameValuePair("iColumns", columns));
			params.add(new BasicNameValuePair("sColumns", ""));
			params.add(new BasicNameValuePair("iDisplayStart", "0"));

			params.add(new BasicNameValuePair("iDisplayLength", String.valueOf(maximum)));
			params.add(new BasicNameValuePair("iSortCol_0", "0"));
			params.add(new BasicNameValuePair("sSearch", ""));
			params.add(new BasicNameValuePair("sSortDir_0", "asc"));

			post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

			response = executeGetOrPost(post);

			JsonElement jsonE = JsonHandler.getJsonString(response);
			JsonObject obj = jsonE.getAsJsonObject();
			String message = JsonHandler.getMessageOfResponse(obj);

			// if we got back a ValidatorStatusCode, there was an error
			if (message != null) {
				setLastError(message);
				errorMap.put(Status.ERROR_SERVER, null);
				return errorMap;
			}

			// we should get back a jsonObject which has a jsonArray of
			// primitives labeled 'aaData'
			JsonArray json = jsonE.getAsJsonObject().get("aaData").getAsJsonArray();
			JsonArray curPrim;
			String name;
			Integer id;
			JsonPrimitive element;

			// the array is itself composed of arrays, with each array
			// containing multiple elements
			// describing a single primitive
			for (JsonElement x : json) {
				curPrim = x.getAsJsonArray();
				for (JsonElement y : curPrim) {
					element = y.getAsJsonPrimitive();

					id = HTMLParser.extractIDFromJson(element.getAsString());
					name = HTMLParser.extractNameFromJson(element.getAsString(), urlParams.get("type"));

					// if the element has an ID and a name, save them
					if (id != null && name != null) {
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
	 *
	 * @param solverId The ID of the solver to download
	 * @param filePath The output path where the file will be saved
	 * @return A status code as defined in the Status class
	 */
	public int downloadSolver(Integer solverId, String filePath) throws IOException {
		return downloadArchive(solverId, R.SOLVER, null, null, filePath, false, false, false, false, null, false, false, null, false);
	}

	/**
	 * Downloads a list of job pairs
	 *
	 * @param pairIds The IDs of all the pairs that should be downloaded
	 * @param filePath Absolute file path to the spot that an archive containing
	 * all the given pairs should be placed
	 * @return A status code as defined in status.java
	 */
	public int downloadJobPairs(List<Integer> pairIds, String filePath) {
		HttpResponse response = null;
		try {
			Map<String, String> urlParams = new HashMap<>();
			urlParams.put(C.FORMPARAM_TYPE, R.JOB_OUTPUTS);
			StringBuilder sb = new StringBuilder();
			for (Integer id : pairIds) {
				sb.append(id);
				sb.append(",");
			}
			String ids = sb.substring(0, sb.length() - 1);

			urlParams.put(C.FORMPARAM_ID + "[]", ids);

			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
			// First, put in the request for the server to generate the desired
			// archive

			HttpGet get = new HttpGet(HTMLParser.URLEncode(baseURL + C.URL_DOWNLOAD, urlParams));
			get = (HttpGet) setHeaders(get);
			response = executeGetOrPost(get);

			boolean fileFound = response.getFirstHeader("Content-Disposition") != null;

			if (!fileFound) {
				setLastError(HTMLParser.extractCookie(response.getAllHeaders(), C.STATUS_MESSAGE_COOKIE));

				return Status.ERROR_ARCHIVE_NOT_FOUND;
			}

			// copy file from the HTTPResponse to an output stream
			File out = new File(filePath);
			File parent = new File(out.getAbsolutePath().substring(0, out.getAbsolutePath().lastIndexOf(File.separator)));
			parent.mkdirs();
			FileOutputStream outs = new FileOutputStream(out);
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
	 * Downloads the job output from a job from StarExec in the form of a zip
	 * file
	 *
	 * @param jobId The ID of the job to download the output from
	 * @param filePath The output path where the file will be saved
	 * @return A status code as defined in the Status class
	 */
	public int downloadJobOutput(Integer jobId, String filePath) throws IOException {
		return downloadArchive(jobId, R.JOB_OUTPUT, null, null, filePath, false, false, false, false, null, false, false, null, false);
	}

	/**
	 * Downloads a CSV describing a job from StarExec in the form of a zip file
	 *
	 * @param jobId The ID of the job to download the CSV for
	 * @param filePath The output path where the file will be saved
	 * @param includeIds Whether to include columns in the CSV displaying the
	 * IDs of the primitives involved
	 * @param onlyCompleted If true, only include completed pairs in the csv
	 * @return A status code as defined in the Status class
	 */
	public int downloadJobInfo(Integer jobId, String filePath, boolean includeIds, boolean onlyCompleted) throws IOException {
		return downloadArchive(jobId, R.JOB, null, null, filePath, false, false, includeIds, false, null, onlyCompleted, false, null, false);
	}

	/**
	 * Downloads a space XML file from StarExec in the form of a zip file
	 *
	 * @param spaceId The ID of the space to download the XML for
	 * @param filePath The output path where the file will be saved
	 * @param getAttributes If true, adds benchmark attributes to the XML
	 * @param updateId The ID of the update processor to use in the XML. No
	 * update processor if null
	 * @return A status code as defined in the Status class
	 */
	public int downloadSpaceXML(Integer spaceId, String filePath, boolean getAttributes, Integer updateId) throws IOException {
		return downloadArchive(spaceId, R.SPACE_XML, null, null, filePath, false, false, false, false, null, false, getAttributes, updateId, false);
	}

	/**
	 * Downloads the data contained in a single space
	 *
	 * @param spaceId The ID of the space to download
	 * @param filePath Where to output the ZIP file
	 * @param excludeSolvers If true, excludes solvers from the ZIP file.
	 * @param excludeBenchmarks If true, excludes benchmarks from the ZIP file
	 * @return A status code as defined in the Status class
	 */
	public int downloadSpace(Integer spaceId, String filePath, boolean excludeSolvers, boolean excludeBenchmarks) throws IOException {
		return downloadArchive(spaceId, R.SPACE, null, null, filePath, excludeSolvers, excludeBenchmarks, false, false, null, false, false, null, false);
	}

	/**
	 * Downloads the data contained in a space hierarchy rooted at the given
	 * space
	 *
	 * @param spaceId The ID of the root space of the hierarchy to download
	 * @param filePath Where to output the ZIP file
	 * @param excludeSolvers If true, excludes solvers from the ZIP file.
	 * @param excludeBenchmarks If true, excludes benchmarks from the ZIP file
	 * @return A status code as defined in the Status class
	 */
	public int downloadSpaceHierarchy(Integer spaceId, String filePath, boolean excludeSolvers, boolean excludeBenchmarks) throws IOException {
		return downloadArchive(spaceId, R.SPACE, null, null, filePath, excludeSolvers, excludeBenchmarks, false, true, null, false, false, null, false);
	}

	/**
	 * Downloads a post processor from StarExec in the form of a zip file
	 *
	 * @param procId The ID of the processor to download
	 * @param filePath The output path where the file will be saved
	 * @return A status code as defined in the Status class
	 */
	public int downloadPostProcessor(Integer procId, String filePath) throws IOException {
		return downloadArchive(procId, R.PROCESSOR, null, null, filePath, false, false, false, false, "post", false, false, null, false);
	}

	/**
	 * Downloads a benchmark from StarExec in the form of a zip file
	 *
	 * @param benchId The ID of the benchmark to download
	 * @param filePath The output path where the file will be saved
	 * @return A status code as defined in the Status class
	 */
	public int downloadBenchmark(Integer benchId, String filePath) throws IOException {
		return downloadArchive(benchId, R.BENCHMARK, null, null, filePath, false, false, false, false, null, false, false, null, false);
	}

	/**
	 * Returns the community id that a space is in.
	 *
	 * @param spaceId the space id to get the community of
	 * @return the id of the community the space is in.
	 * @throws IOException
	 */
	public int getCommunityIdOfSpace(int spaceId) throws IOException {
		final String url = baseURL + C.URL_COMMUNITY_FROM_SPACE.replace(C.URL_COMMUNITY_FROM_SPACE_SPACE_ID_PARAM, String.valueOf(spaceId));

		HttpGet get = new HttpGet(url);
		get = (HttpGet) setHeaders(get);
		HttpResponse response = null;

		try {
			response = executeGetOrPost(get);
			return JsonHandler.getJsonString(response).getAsInt();
		} finally {
			safeCloseResponse(response);
		}
	}

	/**
	 * Downloads an archive from Starexec
	 *
	 * @param id The ID of the primitive that is going to be downloaded
	 * @param type The type of the primitive (R.SOLVER, R.BENCHMARK, and so on
	 * @param since If downloading new job info, this represents the last seen
	 * completion index. Otherwise, it should be null
	 * @param filePath The path to where the archive should be output, including
	 * the filename
	 * @param excludeSolvers If downloading a space, whether to exclude solvers
	 * @param excludeBenchmarks If downloading a space, whether to exclude
	 * benchmarks
	 * @param includeIds If downloading a job info CSV, whether to include
	 * columns for IDs
	 * @param hierarchy If downloading a space, whether to get the full
	 * hierarchy
	 * @param procClass If downloading a processor, what type of processor it is
	 * (R.BENCHMARK,"post",or "pre")
	 * @return
	 */
	protected int downloadArchive(Integer id, String type, Integer since, Long lastTimestamp, final String filePath, boolean excludeSolvers, boolean excludeBenchmarks, boolean includeIds, Boolean hierarchy, String procClass, boolean onlyCompleted, boolean includeAttributes, Integer updateId, Boolean longPath) throws IOException {
		HttpResponse response = null;

		try {
			Map<String, String> urlParams = new HashMap<>();
			log.log("Downloading archive of type: " + type);
			urlParams.put(C.FORMPARAM_TYPE, type);
			urlParams.put(C.FORMPARAM_ID, id.toString());
			if (type.equals(R.SPACE)) {
				urlParams.put("hierarchy", hierarchy.toString());
			}
			if (since != null) {
				urlParams.put(C.FORMPARAM_SINCE, since.toString());
			}
			if (lastTimestamp != null) {
				urlParams.put("lastTimestamp", lastTimestamp.toString());
			}
			if (procClass != null) {
				urlParams.put("procClass", procClass);
			}
			// if the use put in the include ids param, pass it on to the server
			if (includeIds) {
				urlParams.put("returnids", "true");
			}
			if (onlyCompleted) {
				urlParams.put("getcompleted", "true");
			}
			if (includeAttributes) {
				urlParams.put("includeattrs", "true");
			}
			if (updateId != null) {
				urlParams.put("updates", "true");
				urlParams.put("upid", updateId.toString());
			}
			if (excludeBenchmarks) {
				urlParams.put("includebenchmarks", "false");
			}
			if (excludeSolvers) {
				urlParams.put("includesolvers", "false");
			}
			if (longPath) {
				urlParams.put("longpath", "true");
			}
			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
			// First, put in the request for the server to generate the desired
			// archive

			String getUrl = HTMLParser.URLEncode(baseURL + C.URL_DOWNLOAD, urlParams);
			HttpGet get = new HttpGet(getUrl);

			log.log("Making request to " + getUrl);

			get = (HttpGet) setHeaders(get);
			response = executeGetOrPost(get);
			final Map<String, String> cookies = getCookies(response);
			final int httpStatus = response.getStatusLine().getStatusCode();
			final boolean fileFound = response.getFirstHeader("Content-Disposition") != null;

			if (!fileFound && httpStatus != HttpServletResponse.SC_NOT_MODIFIED) {
				final String errorMessage = cookies.get(C.STATUS_MESSAGE_COOKIE);
				log.log("Content-Disposition header was missing.");
				log.log("Server status message: " + errorMessage);
				setLastError(errorMessage);
				return Status.ERROR_ARCHIVE_NOT_FOUND;
			}

			final Header modifiedHeader = response.getFirstHeader("Last-Modified");
			final long lastModified;
			if (modifiedHeader != null) {
				lastModified = DateUtils.parseDate(modifiedHeader.getValue()).getTime();
			} else {
				lastModified = 0;
			}

			/* Running-Pairs is not always sent, so we need to default to 0
			 */
			final int runningPairs = Integer.parseInt(cookies.getOrDefault("Running-Pairs", "0"));
			final int foundPairs = Integer.parseInt(cookies.getOrDefault("Pairs-Found", "0"));
			final int totalPairs = Integer.parseInt(cookies.getOrDefault("Total-Pairs", "0"));
			final int oldPairs = Integer.parseInt(cookies.getOrDefault("Older-Pairs", "0"));
			final int lastSeen = Integer.parseInt(cookies.getOrDefault("Max-Completion", "0"));

			// if we're sending 'since,' it means this is a request for new job data
			final boolean isNewJobRequest = urlParams.containsKey(C.FORMPARAM_SINCE);
			final boolean isNewOutputRequest = urlParams.get(C.FORMPARAM_TYPE).equals(R.JOB_OUTPUT);
			final boolean isNewInfoRequest = urlParams.get(C.FORMPARAM_TYPE).equals(R.JOB);
			boolean jobDone = false;
			if (isNewJobRequest) {
				final boolean jobStarted = foundPairs != 0 || runningPairs != 0;
				jobDone = totalPairs == (foundPairs + oldPairs);
				if (isNewOutputRequest && !jobStarted && false) {
					// There are no new pairs so the zip will be empty.
					return C.SUCCESS_NOFILE;
				}

				// check to see if the job is complete
				if (lastSeen <= since) { // indicates there was no new information
					if (jobDone) {
						return C.SUCCESS_JOBDONE;
					} else if (!isNewOutputRequest) {
						return C.SUCCESS_NOFILE;
						// TODO: What to do in this situation?
					}
				}
			}

			if (httpStatus == HttpServletResponse.SC_NOT_MODIFIED) {
				if (jobDone) {
					return C.SUCCESS_JOBDONE;
				} else {
					return C.SUCCESS_NOFILE;
				}
			}

			// copy file from the HTTPResponse to an output stream
			File out = new File(filePath);
			File parent = new File(out.getAbsolutePath().substring(0, out.getAbsolutePath().lastIndexOf(File.separator)));
			parent.mkdirs();
			FileOutputStream outs = new FileOutputStream(out);
			IOUtils.copy(response.getEntity().getContent(), outs);
			outs.close();
			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, true);

			/* If it's not a valid zipfile we need to return SUCCESS_NOFILE
			 * if the request was a new output request, otherwise throw the
			 * exception. Don't return anything if it is a valid zipfile.
			 */
			Optional<Integer> errorCode = checkIfValidZipFile(out);
			if (errorCode.isPresent()) {
				return errorCode.get();
			}

			// only after we've successfully saved the file should we update the
			// maximum completion index,
			// which keeps us from downloading the same stuff twice
			if (isNewJobRequest) {
				if (isNewInfoRequest) {
					this.setJobInfoCompletion(id, lastSeen);
				} else if (isNewOutputRequest) {
					this.setJobOutCompletion(id, new PollJobData(lastSeen, lastModified));
				}

				if (foundPairs != 0) {
					System.out.printf(
							"completed pairs found =%d-%d/%d (highest=%d)\n",
							oldPairs + 1,
							oldPairs + foundPairs,
							totalPairs,
							lastSeen
					);
				}
			}

			return 0;
		} catch (IOException e) {
			log.log("Caught exception in downloadArchive: " + Util.getStackTrace(e));

			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, true);
			throw e;
		} finally {
			safeCloseResponse(response);
		}
	}

	/**
	 * Sets the highest seen completion ID for info on a given job
	 *
	 * @param jobID An ID of a job on StarExec
	 * @param completion The completion ID
	 */
	protected void setJobInfoCompletion(int jobID, int completion) {
		job_info_indices.put(jobID, completion);
	}

	/**
	 * Sets the highest seen completion ID for output on a given job
	 *
	 * @param jobID An ID of a job on StarExec
	 */
	protected void setJobOutCompletion(int jobID, PollJobData data) {
		job_out_indices.put(jobID, data);
	}

	/**
	 * Gets the status of a benchmark archive upload given the ID of the upload
	 * status.
	 *
	 * @param statusId The upload ID to use
	 * @return A human-readable string containing details of the benchmark
	 * upload in the following format benchmarks: {validated} / {failed
	 * validation} / {total} | spaces: {completed} / {total} {error
	 * message if exists} {"upload complete" if finished} Null is
	 * returned if there was an error, and this Connection's error
	 * message will have been set
	 */
	public String getBenchmarkUploadStatus(Integer statusId) {
		HttpResponse response = null;
		try {

			String URL = baseURL + C.URL_GET_BENCH_UPLOAD_STATUS;
			URL = URL.replace("{statusId}", statusId.toString());
			HttpGet get = new HttpGet(URL);
			get = (HttpGet) setHeaders(get);
			response = executeGetOrPost(get);
			JsonObject obj = JsonHandler.getJsonObject(response);

			boolean success = JsonHandler.getSuccessOfResponse(obj);
			String message = JsonHandler.getMessageOfResponse(obj);
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
	 * Creates a job on Starexec according to the given parameters
	 *
	 * @param spaceId The ID of the root space for the job
	 * @param name The name of the job. Must be unique to the space
	 * @param desc A description of the job. Can be empty.
	 * @param postProcId The ID of the post processor to apply to the job output
	 * @param preProcId The ID of the pre procesor that will be run on
	 * benchmarks before they are fed into solvers
	 * @param queueId The ID of the queue to run the job on
	 * @param wallclock The wallclock timeout for job pairs. If null, the
	 * default for the space will be used
	 * @param cpu The cpu timeout for job pairs. If null, the default for the
	 * space will be used.
	 * @param useDepthFirst If true, job pairs will be run in a depth-first
	 * fashion in the space hierarchy.
	 * @param startPaused If true, job will be paused upon creation
	 * @param seed A number that will be passed into the preprocessor for every
	 * job pair in this job If false, they will be run in a round-robin
	 * fashion.
	 * @param maxMemory Specifies the maximum amount of memory, in gigabytes,
	 * that can be used by any one job pair.
	 * @param suppressTimestamps If true, timestamps will not be added to job
	 * output lines. Defaults to false.
	 * @param resultsInterval The interval at which to get incremental results,
	 * in seconds. 0 means no incremental results
	 * @return A status code as defined in status.java
	 */
	public int createJob(Integer spaceId, String name, String desc, Integer postProcId, Integer preProcId, Integer queueId, Integer wallclock, Integer cpu, Boolean useDepthFirst, Double maxMemory, boolean startPaused, Long seed, Boolean suppressTimestamps, Integer resultsInterval) {
		HttpResponse response = null;
		try {
			List<NameValuePair> params = new ArrayList<>();

			String traversalMethod = "depth";
			if (!useDepthFirst) {
				traversalMethod = "robin";
			}

			HttpPost post = new HttpPost(baseURL + C.URL_POSTJOB);

			post = (HttpPost) setHeaders(post);

			params.add(new BasicNameValuePair("sid", spaceId.toString()));
			params.add(new BasicNameValuePair("name", name));
			params.add(new BasicNameValuePair("desc", desc));
			if (wallclock != null) {
				params.add(new BasicNameValuePair("wallclockTimeout", String.valueOf(wallclock)));
			}
			if (cpu != null) {
				params.add(new BasicNameValuePair("cpuTimeout", String.valueOf(cpu)));
			}
			params.add(new BasicNameValuePair("queue", queueId.toString()));
			params.add(new BasicNameValuePair("postProcess", postProcId.toString()));
			params.add(new BasicNameValuePair("preProcess", preProcId.toString()));
			params.add(new BasicNameValuePair("seed", seed.toString()));
			params.add(new BasicNameValuePair("resultsInterval", resultsInterval.toString()));
			params.add(new BasicNameValuePair(C.FORMPARAM_TRAVERSAL, traversalMethod));
			params.add(new BasicNameValuePair(R.BENCHMARKING_FRAMEWORK_OPTION, R.DEFAULT_BENCHMARKING_FRAMEWORK.toString()));

			if (maxMemory != null) {
				params.add(new BasicNameValuePair("maxMem", String.valueOf(maxMemory)));
			}
			params.add(new BasicNameValuePair("runChoice", "keepHierarchy"));
			if (startPaused) {
				params.add(new BasicNameValuePair("pause", "yes"));
			} else {
				params.add(new BasicNameValuePair("pause", "no"));
			}
			if (suppressTimestamps) {
				params.add(new BasicNameValuePair("suppressTimestamp", "yes"));
			} else {
				params.add(new BasicNameValuePair("suppressTimestamp", "no"));
			}

			post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

			response = executeGetOrPost(post);

			String id = HTMLParser.extractCookie(response.getAllHeaders(), "New_ID");

			// make sure the id we got back is positive, indicating we made a
			// job successfully
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
	 *
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
	 *
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
	 * @return The last error that was sent back by the server. This will be
	 * updated whenever ERROR_SERVER is returned as a status code by any
	 * function
	 */
	public String getLastError() {
		return lastError;
	}

	/**
	 * @param lastError the lastError to set. Leading/trailing whitespace and
	 * quotes will be removed
	 */
	private void setLastError(String lastError) {
		if (lastError == null) {
			this.lastError = "";
			return;
		}
		lastError = lastError.trim();
		if (lastError.charAt(0) == '"') {
			lastError = lastError.replaceFirst("\"", "");
		}
		if (lastError.charAt(lastError.length() - 1) == '"') {
			lastError = lastError.substring(0, lastError.length() - 1);
		}
		this.lastError = lastError;
	}

	/**
	 * Gets the attributes for a configuration in a Map
	 *
	 * @param id The primitive ID
	 * @return The Map of attributes, or null on error
	 */
	public Map<String, String> getConfigurationAttributes(int id) {
		return getPrimitiveAttributes(id, "configuration");
	}

	/**
	 * Gets the attributes for a processor in a Map
	 *
	 * @param id The primitive ID
	 * @return The Map of attributes, or null on error
	 */
	public Map<String, String> getProcessorAttributes(int id) {
		return getPrimitiveAttributes(id, "processor");
	}

	/**
	 * Gets the attributes for a solver in a Map
	 *
	 * @param id The primitive ID
	 * @return The Map of attributes, or null on error
	 */
	public Map<String, String> getSolverAttributes(int id) {
		return getPrimitiveAttributes(id, R.SOLVER);
	}

	/**
	 * Gets the attributes for a benchmark in a Map
	 *
	 * @param id The primitive ID
	 * @return The Map of attributes, or null on error
	 */

	public Map<String, String> getBenchmarkAttributes(int id) {
		return getPrimitiveAttributes(id, "benchmark");
	}

	/**
	 * Gets the attributes for a job in a Map
	 *
	 * @param id The primitive ID
	 * @return The Map of attributes, or null on error
	 */

	public Map<String, String> getJobAttributes(int id) {
		return getPrimitiveAttributes(id, R.JOB);
	}

	/**
	 * Gets the attributes for a space in a Map
	 *
	 * @param id The primitive ID
	 * @return The Map of attributes, or null on error
	 */

	public Map<String, String> getSpaceAttributes(int id) {
		return getPrimitiveAttributes(id, R.SPACE);
	}

	/**
	 * Asks the server for a Json object representing a primitive and returns
	 * the attributes of that primitive in a Map of keys to values.
	 *
	 * @param id The ID of the primitive
	 * @param type The type of the primitive
	 * @return The Map, or null on error
	 */
	protected Map<String, String> getPrimitiveAttributes(int id, String type) {
		Map<String, String> failMap = new HashMap<>();
		HttpResponse response = null;
		try {
			HttpGet get = new HttpGet(baseURL + C.URL_GETPRIMJSON.replace("{type}", type).replace("{id}", String.valueOf(id)));
			get = (HttpGet) setHeaders(get);
			response = executeGetOrPost(get);
			JsonElement json = JsonHandler.getJsonString(response);
			String errorMessage = JsonHandler.getMessageOfResponse(json.getAsJsonObject());
			if (errorMessage != null) {
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
	 *
	 * @param response
	 */
	private void safeCloseResponse(HttpResponse response) {
		try {
			response.getEntity().getContent().close();
		} catch (Exception e) {
			// ignore
		}
	}

	/**
	 * Returns a Map of all cookies set by a response.
	 * Cookies are returned as individual Headers of the form:
	 * Set-Cookie: key=value; expires=Sat, 02 May 2009 23:38:25 GMT
	 *
	 * @param response
	 * @return Map of cookies set by response
	 */
	private Map<String, String> getCookies(HttpResponse response) {
		final Map<String, String> cookies = new HashMap<>();
		final Pattern regex = Pattern.compile("^\\s*([^=]+)=([^;]+)");
		for (Header h : response.getHeaders("Set-Cookie")) {
			Matcher matcher = regex.matcher(h.getValue());
			if (matcher.find()) {
				final String k = matcher.group(1);
				final String v = matcher.group(2);
				cookies.put(k, v);
			}
		}
		return cookies;
	}

}
