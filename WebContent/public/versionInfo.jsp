<%@page contentType="text/html" pageEncoding="UTF-8"  import="java.io.*, java.lang.StringBuilder" %>	
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
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
	


%>
<star:template title="Version Information" css="public/versionInfo">
	<pre id="infoLoc"> ${versionInfo}</pre>
</star:template>