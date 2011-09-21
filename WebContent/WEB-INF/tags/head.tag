<%@tag description="Standard html header info for all starexec pages"%>
<%@tag import="com.starexec.data.to.*, com.starexec.constants.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@attribute name="title" %>
<%@attribute name="css" %>
<%@attribute name="js" %>

<head>
	<title>${title} - starexec</title>
	<meta charset="utf-8" />
	<link rel="stylesheet" href="/starexec/css/html5.css" />
	<link rel="stylesheet" href="/starexec/css/master.css" />
	<c:if test="${not empty css}">
		<link rel="stylesheet" href="/starexec/css/${css}.css"/>
	</c:if>
	<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.6.4/jquery.min.js"></script>		
	<!--[if lt IE 9]> 
		<script src="//html5shiv.googlecode.com/svn/trunk/html5.js"></script> 
	<![endif]-->
	<script src="/starexec/js/master.js"></script>
	<c:if test="${not empty js}">		
		<script type="text/javascript" src="/starexec/js/${js}.js"></script>
	</c:if>		
	<link type="image/ico" rel="icon" href="/starexec/images/favicon.ico">	
</head>
	