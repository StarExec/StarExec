<%@page contentType="text/html" pageEncoding="UTF-8"  import="java.io.*, java.lang.StringBuilder" %>	
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
	try{
		BufferedReader file=new BufferedReader(new FileReader(application.getRealPath("/")+"public/versionInfo.txt"));
		StringBuilder text=new StringBuilder();
		String nextLine=file.readLine();
		while (nextLine!=null) {
			System.out.println(nextLine);
			text.append(nextLine);
			text.append("<br>");
			nextLine=file.readLine();
		}
		String finalText=text.toString();
		request.setAttribute("versionInfo",finalText);
	} catch (Exception e) {
		request.setAttribute("versionInfo","Version information could not be found");
	}
	


%>
<star:template title="Version Information" css="explore/quickRef">	
	<pre id="infoLoc"> ${versionInfo}</pre>
	
</star:template>