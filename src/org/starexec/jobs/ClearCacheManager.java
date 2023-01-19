package org.starexec.jobs;

import org.apache.commons.io.FileUtils;
import org.starexec.constants.R;
import org.starexec.logger.StarLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ClearCacheManager {
	private static final StarLogger log = StarLogger.getLogger(ClearCacheManager.class);

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
		File logBase = new File(R.JOB_SOLVER_CACHE_CLEAR_LOG_DIRECTORY);

		for (String node : R.BACKEND.getWorkerNodes()) {
			final String currentScript = scriptTemplate.replace("$$NODE_NAME$$", node);
			File logPath = new File(logBase,node);
			if (logPath.exists()) {
				logPath.delete();
			}

			String scriptPath = String.format("%s/%s", R.getJobInboxDir(), "cacheclear"+node+".bash");
			File f = new File(scriptPath);

			f.delete();
			f.getParentFile().mkdirs();
			f.createNewFile();

			if(!f.setExecutable(true, false) || !f.setReadable(true, false)) {
				log.error("Can't change owner permissions on jobscript file. This will prevent the grid engine from being able to open the file. Script path: " + scriptPath);
			}
			FileWriter out = new FileWriter(f);
			out.write(currentScript);
			out.close();
			R.BACKEND.submitScript(scriptPath, R.BACKEND_WORKING_DIR, logPath.getAbsolutePath());
		}
	}
}
