<%@page contentType="text/html" pageEncoding="UTF-8"  import="java.io.*, java.lang.StringBuilder" %>	
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
/**
	try{
		//Gets current path of the application and tries to open version file
		BufferedReader file=new BufferedReader(new FileReader(application.getRealPath("/")+"public/versionInfo.txt"));
		StringBuilder text=new StringBuilder();
		String nextLine=file.readLine();
		while (nextLine!=null) {
			text.append(nextLine);
			text.append("\n");
			nextLine=file.readLine();
		}
		String finalText=text.toString();
		request.setAttribute("versionInfo",finalText);
	} catch (Exception e) {
		//some problem opening the file, so there's no data to display
		request.setAttribute("versionInfo","Version information could not be found");
	}
	*/


%>
<star:template title="Version Information" css="public/versionInfo">
	<pre id="infoLoc">
In revision 12601:

Bug fixes:

-- Dedicated queues like smteval.q are now getting recognized
   correctly.

-- No more "Session expired" alerts and unusable pages.

-- We will not insert redundant rows into the job_attributes table, which
   was leading to exceptions from the SQL layer.  This should fix
   a bug that was plaguing Geoff.

-- only the user can see their solvers, jobs, and benchmarks on his
   user details page, fixing a bug reported by Harald.

Improvements:

-- The job details pages now have summaries of results by solver.
   
-- A progress dialog is now shown during uploads and downloads.

-- The names of downloaded archives are still long and messy,
   but the directory names in those archives are not (requested by Harald).

-- The unit "s" has been dropped from the job information CSV (requested
   by Harald).

-- The runsolver tool has been updated, and it will now be given the --add-eof
   flag when executed (requested by Geoff).

-- Uploading benchmarks now extracts description for space (starexec_description.txt) and sets it.

-- Implemented back buttons for facilitated navigation.

-- The solver details page warns user if solver is uploaded without configuration.

</pre>
</star:template>