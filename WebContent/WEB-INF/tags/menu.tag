<%@tag description="Template tag for all starexec pages"%>
<%@tag import="org.starexec.data.to.*, org.starexec.constants.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@attribute name="title" %>
<%@attribute name="css" %>
<%@attribute name="js" %>

<!DOCTYPE html>
<html lang="en">
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
	<body>			
		<div id="wrapper">
			<header id="pageHeader">
				<img src="/starexec/images/logo.svg" alt="Starexec Logo">
				<h1>star<span class="font-accent">exec</span></h1>
				<nav>
					<ul>
						<li class="round"><a href="#">dash</a>
						<li class="round"><a href="#">upload</a>
						<li class="round"><a href="#">create</a>
					</ul>
				</nav>
			</header>
			<div id="content" class="round">
				<h1>${title}</h1>				
				<jsp:doBody/>
			</div>		
		<footer id="pageFooter">
			<c:if test="<%= session.getAttribute(P.SESSION_USER) != null %>">
				<ul>
					<li>
						<%
							if(session.getAttribute(P.SESSION_USER) != null) {
								out.write(((User)session.getAttribute(P.SESSION_USER)).getFullName().toLowerCase()); 						
							}
						%>
					</li>
					<li>|</li>
					<li><a href="#">logout</a></li>
				</ul>
			</c:if>
			<c:if test="<%= session.getAttribute(P.SESSION_USER) == null %>">
				<a id="loginLink" href="/starexec/pages/index.jsp">login</a>
			</c:if>				
			<a class="copyright" href="http://www.uiowa.edu">(C) 2011 the university of iowa</a>			
		</footer>
		</div>
	</body>
</html>