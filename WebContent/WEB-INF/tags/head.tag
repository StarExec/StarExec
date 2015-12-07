<%@tag description="Standard html header info for all starexec pages"%>
<%@tag import="java.util.List, org.starexec.util.Validator, org.starexec.util.Util, org.starexec.command.HTMLParser, org.starexec.util.SessionUtil,org.starexec.data.database.Users, org.starexec.data.to.*, org.starexec.constants.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%@attribute name="title" %>
<%@attribute name="css" %>
<%@attribute name="js" %>
<%@attribute name="localJs" %>


<head>
	<title>${title} - StarExec</title>
	<meta charset="utf-8" />
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
	<c:if test="${not empty css}">	
		<c:forEach var="cssFile" items="${fn:split(css, ',')}">
			<link rel="stylesheet" href="${starexecRoot}/css/${fn:trim(cssFile)}.css" />
		</c:forEach>	
	</c:if>		
        <script> var starexecRoot="${starexecRoot}/";
        		 var defaultPageSize=${pagesize}; 
				 var isLocalJobPage = "${isLocalJobPage}" === "true";</script>
        
	<!--[if lt IE 9]> 
		
		<script src="//html5shiv.googlecode.com/svn/trunk/html5.js"></script> 
	<![endif]-->
	<c:choose>
		<c:when test="${isLocalJobPage}">
			<script type="text/javascript" src="${starexecRoot}/js/lib/jquery.min.js"></script>	
		</c:when>
		<c:otherwise>
			<script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>	
		</c:otherwise>
	</c:choose>
	<c:forEach var="globalJsFile" items="${globalJsFiles}">
		<script type="text/javascript" src="${starexecRoot}/js/${globalJsFile}.js"></script>
	</c:forEach>
	<c:if test="${not empty js}">	
		<c:forEach var="jsFile" items="${fn:split(js, ',')}">
			<script type="text/javascript" src="${starexecRoot}/js/${fn:trim(jsFile)}.js"></script>
		</c:forEach>	
	</c:if>
	<c:if test="${isProduction}">	
		<script type="text/javascript">
			var _gaq = _gaq || [];
			_gaq.push(['_setAccount', 'UA-29131619-1']);
			_gaq.push(['_trackPageview']);
			
			(function() {
			  var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
			  ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
			  var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
			})();		
		</script>
	</c:if>						
	<link type="image/ico" rel="icon" href="${starexecRoot}/images/favicon.ico">	
</head>
	
