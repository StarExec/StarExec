<%@page isErrorPage="true" contentType="text/html" pageEncoding="UTF-8"  import="org.starexec.constants.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	String desc = "";
	
	switch(pageContext.getErrorData().getStatusCode()) {
		case 400:
			desc = "bad request";
			break;
		case 403:
			desc = "forbidden";
			break;
		case 404:
			desc = "not found";
			break;
		case 405:
			desc = "method not allowed";
			break;
		case 500:
			desc = "internal server error";
			request.setAttribute("didLog", true);
			break;
		default:
			break;
	}
	
	request.setAttribute("errorDesc", desc);
%>

<star:template title="http ${pageContext.errorData.statusCode} - ${errorDesc}" css="error">		
	<p><c:out value="${requestScope['javax.servlet.error.message']}"/></p>
	<div id="actions" class="starexecErrorPage">
		<a href="#" onclick="history.go(-1);return false;">try again</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href="mailto:${contactEmail}?subject=[Starexec] Error Report">report error</a>
	</div>		
</star:template>