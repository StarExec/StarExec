<%@tag description="Template tag for all starexec pages"%>
<%@tag import="org.starexec.data.to.*, org.starexec.constants.*, org.starexec.util.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>

<%@attribute name="title" %>
<%@attribute name="css" %>
<%@attribute name="js" %>

<!DOCTYPE html>
<html lang="en">
	<star:head title="${title}" css="${css}" js="${js}"/>	
	<body>			
		<div id="wrapper">
			<star:header />
			<div id="content" class="round">
				<h1 style="width:100%; word-wrap:break-word;">${title}</h1>
				<img alt="loading" src="/${starexecRoot}/images/loader.gif" id="loader">			
				<jsp:doBody/>
			</div>		
		<star:footer />
		<div id="buildInfo" title="built by: ${buildUser} (${buildDate})"><a href="/${starexecRoot}/public/versionInfo.jsp">StarExec revision ${buildVersion}</a></div>
		</div>
	</body>
</html>