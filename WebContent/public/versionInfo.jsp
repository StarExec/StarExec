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
-- The job details pages now have summaries of results by solver.

-- Dedicated queues like smteval.q are now getting recognized
   correctly.
   
-- A progress dialog is now shown during uploads and downloads.

-- Uploaded Benchmarks now extracts description for space and sets it.

-- Implemented back buttons for facilitated navigation.

-- The solver details page warns user if solver is uploaded without configurat

</pre>
</star:template>