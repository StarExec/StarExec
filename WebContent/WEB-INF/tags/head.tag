<%@tag description="Standard html header info for all starexec pages"%>
<%@tag import="java.util.List, org.starexec.util.Validator, org.starexec.util.Util, org.starexec.command.HTMLParser, org.starexec.util.SessionUtil,org.starexec.data.database.Users, org.starexec.data.to.*, org.starexec.constants.*"%>
<%@tag trimDirectiveWhitespaces="true" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%@attribute name="title" %>
<%@attribute name="css" %>
<%@attribute name="js" %>
<%@attribute name="localJs" %>

<head>
	<title>${title} - StarExec</title>
	<meta charset="utf-8" />
	<%-- This viewport meta tag should not be deleted. Allows website to render on phones. --%>
	<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1" />
	<%
		try {
			//try to use a cookie first so we don't always need to ask the database
			String defaultPageSize=String.valueOf(Users.getDefaultPageSize(SessionUtil.getUserId(request)));
			request.setAttribute("pagesize", defaultPageSize);

		} catch (Exception e) {
			//no user could be found
			request.setAttribute("pagesize", 10);
		}
	%>
	<c:forEach var="globalCssFile" items="${globalCssFiles}">
		<link rel="stylesheet" href="${starexecRoot}/css/${globalCssFile}.css" />
	</c:forEach>
	<c:forEach var="cssFile" items="${fn:split(css, ',')}">
		<link rel="stylesheet" href="${starexecRoot}/css/${fn:trim(cssFile)}.css" />
	</c:forEach>
	<script>
		var starexecRoot="${starexecRoot}/";
		var defaultPageSize=${pagesize};
		var isLocalJobPage=${isLocalJobPage};
	</script>
	<c:forEach var="globalJsFile" items="${globalJsFiles}">
		<script type="text/javascript" src="${starexecRoot}/js/${globalJsFile}.js"></script>
	</c:forEach>
	<c:forEach var="jsFile" items="${fn:split(js, ',')}">
		<script type="text/javascript" src="${starexecRoot}/js/${fn:trim(jsFile)}.js"></script>
	</c:forEach>
	<link type="image/ico" rel="icon" href="${starexecRoot}/images/favicon.ico">
</head>
