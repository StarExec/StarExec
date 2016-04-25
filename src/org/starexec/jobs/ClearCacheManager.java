package org.starexec.jobs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.database.Cluster;
import org.starexec.data.to.Queue;

public class ClearCacheManager {
	private static final Logger log = Logger.getLogger(ClearCacheManager.class);

	private static String scriptTemplate=null;
	protected static void initScriptTemplateIf() {
		if (scriptTemplate == null) {
			// Read in the job script template and format it for this global configuration
			File f = new File(R.CONFIG_PATH, "sge/clearCacheScript");
			try {
				scriptTemplate = FileUtils.readFileToString(f);
			} 
			catch (IOException e) {
				log.error("Error reading the jobscript at "+f,e);
			}
			scriptTemplate = scriptTemplate.replace("$$SANDBOX_USER_ONE$$", R.SANDBOX_USER_ONE);
			scriptTemplate = scriptTemplate.replace("$$WORKING_DIR_BASE$$", R.BACKEND_WORKING_DIR);
			scriptTemplate = scriptTemplate.replace("$$MAX_WRITE$$", String.valueOf(R.MAX_PAIR_FILE_WRITE));	

		}
	}
	
	/**
	 * Submits one job per node to clear the solver cache in every node.
	 * @throws IOException
	 */
	public static void clearSolverCacheOnAllNodes() throws IOException {
		log.info("calling clearSolverCacheOnAllNodes");
		initScriptTemplateIf();
		File logBase = new File(R.getSolverCacheClearLogDir());
	
		for (String node : R.BACKEND.getWorkerNodes()) {
			String currentScript = scriptTemplate;
			File logPath = new File(logBase,node);
			if (logPath.exists()) {
				logPath.delete();
			}
			currentScript = currentScript.replace("$$NODE_NAME$$", node);

			String scriptPath = String.format("%s/%s", R.getJobInboxDir(), "cacheclear"+node+".bash");
			File f = new File(scriptPath);

			f.delete();		
			f.getParentFile().mkdirs();
			f.createNewFile();

			if(!f.setExecutable(true, false) || !f.setReadable(true, false)) {
				log.error("Can't change owner permissions on jobscript file. This will prevent the grid engine from being able to open the file. Script path: " + scriptPath);
			}
			//TODO: Delete these temp logging lines
			log.info("submitting following script to SGE");
			log.info(currentScript);
			FileWriter out = new FileWriter(f);
			out.write(currentScript);
			out.close();
			R.BACKEND.submitScript(scriptPath, R.BACKEND_WORKING_DIR, logPath.getAbsolutePath());
		}
	}
}
