<%@tag description="Template tag for all starexec pages"%>
<%@tag import="org.starexec.data.to.*, org.starexec.constants.*"%>
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
				<h1>${title}</h1>				
				<jsp:doBody/>
			</div>		
		<star:footer />
		</div>
	</body>
</html>