<%@tag description="Template tag for all starexec pages"%>
<%@tag import="org.starexec.data.to.*, org.starexec.constants.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>

<%@attribute name="title" %>
<%@attribute name="css" %>
<%@attribute name="js" %>

<!DOCTYPE html>
<html lang="en">
	<head>
		<title>${title} - starexec</title>
		<meta charset="utf-8" />
		<link rel="stylesheet" href="/starexec/css/html5.css" />
		<link rel="stylesheet" href="/starexec/css/detail/master.css" />	
		<c:if test="${not empty css}">	
			<c:forEach var="cssFile" items="${fn:split(css, ',')}">
				<link rel="stylesheet" href="/starexec/css/${fn:trim(cssFile)}.css"/>
			</c:forEach>	
		</c:if>				
		<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.6.4/jquery.min.js"></script>		
		<!--[if lt IE 9]> 
			<script src="//html5shiv.googlecode.com/svn/trunk/html5.js"></script> 
		<![endif]-->			
		<c:if test="${not empty js}">	
		<c:forEach var="jsFile" items="${fn:split(js, ',')}">
				<script type="text/javascript" src="/starexec/js/${fn:trim(jsFile)}.js"></script>
			</c:forEach>	
		</c:if>		
		<link type="image/ico" rel="icon" href="/starexec/images/favicon.ico">	
	</head>	
	<body>			
		<div id="wrapper">			
			<div id="content">
				<jsp:doBody/>								
			</div>				
		</div>
	</body>
</html>