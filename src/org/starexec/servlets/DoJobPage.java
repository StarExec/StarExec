package org.starexec.servlets;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.io.FileUtils;
import org.starexec.constants.R;
import org.starexec.constants.Web;
import org.starexec.data.database.*;
import org.starexec.data.database.AnonymousLinks.PrimitivesToAnonymize;
import org.starexec.data.security.BenchmarkSecurity;
import org.starexec.data.security.JobSecurity;
import org.starexec.data.security.SolverSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.*;
import org.starexec.data.to.enums.ProcessorType;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.exceptions.StarExecException;
import org.starexec.logger.StarLogger;
import org.starexec.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

/*
 * The purpose of this class is to handle all tasks related to getting a job page.
 * @author aguo2 with methods taken from @patHawks and @presdod
 */
public class DoJobPage {
    private static final StarLogger log = StarLogger.getLogger(Download.class);
    private static final String JS_FILE_TYPE = "js";
	private static final String CSS_FILE_TYPE = "css";
	private static final String PNG_FILE_TYPE = "png";
	private static final String GIF_FILE_TYPE = "gif";
	private static final String ICO_FILE_TYPE = "ico";
	private static final String IMAGES_DIRECTORY_NAME = "images";

    /*
     * Creates and puts a README.MD into the zip
     * @author aguo2
     */
    private static void doReadMe(File sandbox) throws StarExecException {
        try {
            File readme = new File(sandbox, "README.md");
            readme.createNewFile();
            FileWriter writer = new FileWriter(readme);
            writer.write("This tool was created by aguo2, presdod, and pathawks. \n\n");
            writer.write("WARNING: if you download the same page multiple times, the OS will mess with the file name \n");
            writer.write("by putting a number at the end. This messes with the CSS, causing some image dependencies to \n");
            writer.write("not load correctly. \n");
            writer.write("\n");
            writer.write("\n");
            writer.write("\n");
            writer.write("\n");
            //mit licence
            writer.write("The MIT License (MIT)\n");
            writer.write("\n");
            writer.write("Permission is hereby granted, free of charge, to any person obtaining a copy\n");
            writer.write("of this software and associated documentation files (the \"Software\"), to deal\n");
            writer.write("in the Software without restriction, including without limitation the rights\n");
            writer.write("to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n");
            writer.write("copies of the Software, and to permit persons to whom the Software is\n");
            writer.write("furnished to do so, subject to the following conditions:\n");
            writer.write("\n");
            writer.write("The above copyright notice and this permission notice shall be included in all\n");
            writer.write("copies or substantial portions of the Software.\n");
            writer.write("\n");
            writer.write("THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR\n");
            writer.write("IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\n");
            writer.write("FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE\n");
            writer.write("AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\n");
            writer.write("LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,\n");
            writer.write("OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE\n");
            writer.write("SOFTWARE.\n");
            writer.close();
        } catch (Exception e) {
            String msg = e.getMessage();
            throw new StarExecException("there was a problem creating the readme for job page: " + msg);
        }

    }
	
	/*
	 * Given the Job ID, find the output files and put it into the format
	 * {pairId}/{stagenumber}.txt
	 * @param output the directory to put the files in
	 * @param pairID the id of the pair
	 */
	private static void getPairOutput(File output, int pairID) throws IOException {
		JobPair pair = JobPairs.getPairDetailed(pairID);
		File pidFile = new File(output, Integer.toString(pairID));
		pidFile.mkdirs();
		for (JoblineStage stage: pair.getStages()) {
			String originalFile = JobPairs.getStdout(pairID, stage.getStageNumber());
			File oPath = new File(originalFile);
			File newFile = new File(pidFile,stage.getStageNumber() + ".txt");
			FileUtils.copyFile(oPath, newFile);
		}
		
	}

	/*
	 * Given the id of the job, put all the jobpair pages, and their output
	 * into the specified directory using root/pair_{pairid}.html and 
	 * output/{pairId}/{stagenumber}.txt
	 * @param directory the directory to put the files in
	 * @param jobID the ID of the job
	 * @param request the request to get the session cookies from
	 * @author aguo2
	 */
	private static void handleJobPairSitesAndOutput(File directory, int jobID, HttpServletRequest request) throws IOException {
		Job j = Jobs.get(jobID);
		//get the JobPairs on the job
		Map<Integer, List<JobPair>> map = JobPairs.buildJobSpaceIdToJobPairMapWithWallCpuTimesRounded(j);
		File outputDir = new File(directory,"output");
		if (!outputDir.mkdirs()) {
			throw new IOException("The directory for output was not created");
		}  
		for (List<JobPair> l : map.values()) {
			for (JobPair jp : l) {
				getPairOutput(outputDir, jp.getId());
				File htmlFile= new File(directory, "pair_" + jp.getId() + ".html");
				//for each jobpair on the job, do the following 
				List<Cookie> requestCookies = Arrays.asList(request.getCookies());
				String url = R.STAREXEC_URL_PREFIX + "://" + R.STAREXEC_SERVERNAME + "/" + R.STAREXEC_APPNAME +
				"/secure/details/pair.jsp?id=" + jp.getId() + "&localJobPage=true";
				Map<String, String> queryParameters = new HashMap<>();
				//if we don't have to cookies, it throws an unauth error
				String htmlText = Util.getWebPage(url, requestCookies);
				FileUtils.writeStringToFile(htmlFile, htmlText, StandardCharsets.UTF_8);
			}
		}
	}

    /*
	 * Given the absolute file to the CSS file, replace all instances of the given String with the given String
	 * This needs to happen or the UI experiance for the local page will be ugly and full of errors.
	 * @author aguo2
	 */
	private static void handleCSS(File path, String target, String replacement) throws StarExecException {
		try {
			String css = FileUtils.readFileToString(path,"UTF-8");
			//this is the most maintainable way to do this, yes it's expensive. However, people who use this tool don't 
			//want to code the css themselves. T
			css = css.replace(target, replacement);
			FileUtils.write(path,css,"UTF-8");
		}
		catch (Exception e) {
			String exp = e.getMessage();
			throw new StarExecException("Caught exception while handling css: " + exp);
		}
		
	}

    /* 
	 * Given the path to the sandbox and the name of the css file (excluding the file extention) , 
	 * move the associated css map into the proper place. Note that the file paths passed in should 
	 * be relative, This method handles concating the file name with the root. 
	 * @param sanbox the directory to put the files in
	 * @param cssName the name of the css file, including the parent directory if it has one.
	 */
	private static void handleCSSMaps(File sandbox, String cssName) throws StarExecException{
		try {
			File cssMap = new File(R.STAREXEC_ROOT + CSS_FILE_TYPE + "/" + cssName + ".css.map");
			File cssFolder = new File(sandbox, "css");
			File fullPath = new File(cssFolder, cssName + ".css.map");
			
			FileUtils.copyFile(cssMap,fullPath);
		} 
		catch (Exception e) {
			String exp = e.getMessage();
			throw new StarExecException("Caught exception while handling css: " + exp);
		}
	}

	
    /*
	 * Puts the files of a given file type at a given path into a directory. Note that 
	 * this function handles resolving the full file path, so you only need to provide relative ones
	 * @param containingDirectory where you want the files
	 * @param filetype the type of files, ex. js, css
	 * @param allFilePaths all the RELATIVE file paths
	 * @author presdod
	 * @docs aguo2
	 */
	private static void addFilesInDirectory(File containingDirectory, String filetype, String[] allFilePaths)
			throws IOException {
		// Create a new directory named after the filetype such as /js or /css
		String filetypeDirectoryName = null;
		switch (filetype) {
		case CSS_FILE_TYPE:
		case JS_FILE_TYPE:
			filetypeDirectoryName = filetype;
			break;
		case PNG_FILE_TYPE:
		case GIF_FILE_TYPE:
		case ICO_FILE_TYPE:
			filetypeDirectoryName = IMAGES_DIRECTORY_NAME;
			break;
		default:
			throw new IOException("Attempted to copy unsupported file type: " + filetype);
		}

		File filetypeDirectory = new File(containingDirectory, filetypeDirectoryName);

		for (String filePath : allFilePaths) {
			List<String> filesInHierarchy = new ArrayList<>(Arrays.asList(filePath.split("/")));

			// The last filename is the source file.
			String sourceFile = filesInHierarchy.remove(filesInHierarchy.size() - 1);

			File parentDirectory = filetypeDirectory;
			for (String directory : filesInHierarchy) {
				parentDirectory = new File(parentDirectory, directory);
			}
			parentDirectory.mkdirs();
			File fileOnServer = new File(R.STAREXEC_ROOT + filetypeDirectoryName + "/" + filePath + "." + filetype);
			File fileToBeDownloaded = new File(parentDirectory, sourceFile + "." + filetype);
			FileUtils.copyFile(fileOnServer, fileToBeDownloaded);
		}
	}


    /*
	 * Given the directory to place it in, put the HTML page representing the root job into it. 
	 * @param sandboxDirectory the directory to put it in
	 * @param jobId id of job
	 * @param request the HTTP request to get the session cookies from
	 */
	private static void putRootHtmlFileFromServerInSandbox(File sandboxDirectory, int jobId, HttpServletRequest request)
			throws IOException {
		// Create a new html file in the sandbox.
		File htmlFile = new File(sandboxDirectory, "job.html");
		// Make an HTTP request to our own server to get the HTML for the job page and write it to the new html file.
		String urlToGetJobPageFrom = R.STAREXEC_URL_PREFIX + "://" + R.STAREXEC_SERVERNAME + "/" + R.STAREXEC_APPNAME +
				"/secure/details/job.jsp?id=" + jobId + "&" + Web.LOCAL_JOB_PAGE_PARAMETER + "=true";
		log.debug("Getting job page from " + urlToGetJobPageFrom);
		List<Cookie> requestCookies = Arrays.asList(request.getCookies());
		Map<String, String> queryParameters = new HashMap<>();
		//if we don't have to cookies, it throws an unauth error
		String htmlText = Util.getWebPage(urlToGetJobPageFrom, requestCookies);
		FileUtils.writeStringToFile(htmlFile, htmlText, StandardCharsets.UTF_8);
	}

    /*
	 * This function handles all the dependencies that the HTML files need
	 * @param sandboxDirectory the directory you want the dependencies in.
	 * @author aguo2
	 */
	private static void doPageDependencies(File sandboxDirectory) {
		//gets our dependencies
		try {
			addFilesInDirectory(sandboxDirectory, JS_FILE_TYPE, Web.JOB_DETAILS_JS_FILES);
			addFilesInDirectory(sandboxDirectory, JS_FILE_TYPE, Web.JS_FILES_FOR_LOCAL_JOB);
			addFilesInDirectory(sandboxDirectory, CSS_FILE_TYPE, Web.JOB_DETAILS_CSS_FILES);
			addFilesInDirectory(sandboxDirectory, CSS_FILE_TYPE, Web.CSS_FILES_FOR_LOCAL_JOB);
			addFilesInDirectory(sandboxDirectory, PNG_FILE_TYPE, Web.GLOBAL_PNG_FILES);
			addFilesInDirectory(sandboxDirectory, GIF_FILE_TYPE, Web.GLOBAL_GIF_FILES);
			addFilesInDirectory(sandboxDirectory, ICO_FILE_TYPE, Web.GLOBAL_ICO_FILES);
			File csspath = new File(sandboxDirectory, "css/global.css");
			handleCSS(csspath,"/" + R.STAREXEC_APPNAME + "/css", ".");
			csspath = new File(sandboxDirectory, "css/explore/common.css");
			handleCSS(csspath,"qtip-nonPermanentLeader a.tooltipButton,img{cursor:pointer}", "");
			//get the maps as well
			handleCSSMaps(sandboxDirectory, "global");
			handleCSSMaps(sandboxDirectory, "details/job");
			handleCSSMaps(sandboxDirectory, "details/shared");
			handleCSSMaps(sandboxDirectory, "explore/common");
			handleCSSMaps(sandboxDirectory, "common/table");
			handleCSSMaps(sandboxDirectory, "common/delaySpinner");
			handleCSSMaps(sandboxDirectory, "details/pair");
			handleCSSMaps(sandboxDirectory, "prettify/prettify");
			//gets jqurey dependencies, as well as images
			File serverCssJqueryUiImagesDirectory = new File(R.STAREXEC_ROOT + "css/jqueryui/images");
			File sandboxCssJqueryUiDirectory = new File(sandboxDirectory, "css/jqueryui");
			FileUtils.copyDirectoryToDirectory(serverCssJqueryUiImagesDirectory, sandboxCssJqueryUiDirectory);

			File serverCssImagesDirectory = new File(R.STAREXEC_ROOT + "css/images");
			File sandboxCssDirectory = new File(sandboxDirectory, "css/");
			FileUtils.copyDirectoryToDirectory(serverCssImagesDirectory, sandboxCssDirectory);

			File serverCssJstreeDirectory = new File(R.STAREXEC_ROOT + "css/jstree");
			FileUtils.copyDirectoryToDirectory(serverCssJstreeDirectory, sandboxCssDirectory);

			File serverImagesJstreeDirectory = new File(R.STAREXEC_ROOT + "images/jstree");
			File sandboxImagesDirectory = new File(sandboxDirectory, "images/");
			FileUtils.copyDirectoryToDirectory(serverImagesJstreeDirectory, sandboxImagesDirectory);

		} catch (Exception e) {
			String msg = e.getMessage();
			log.error("could not get dependencies for the main page: " + msg);
		}
	}
    
    /*
	 * This function is responsible for packaging the job page archive and sending it to the user. 
	 * @author presdod and aguo2
	 */
	public static void handleJobPage(int jobId, HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		File sandboxDirectory = null;
		try {
			sandboxDirectory = Util.getRandomSandboxDirectory();
			putRootHtmlFileFromServerInSandbox(sandboxDirectory, jobId, request);
			doPageDependencies(sandboxDirectory);
            doReadMe(sandboxDirectory);
			handleJobPairSitesAndOutput(sandboxDirectory, jobId, request);
			List<File> filesToBeDownloaded = Arrays.asList(sandboxDirectory.listFiles());
			ArchiveUtil.createAndOutputZip(filesToBeDownloaded, response.getOutputStream(),
			                               "Job" + String.valueOf(jobId) + "_page"
			);
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		} finally {
			FileUtils.deleteDirectory(sandboxDirectory);
		}
	}


}
