<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<star:template title="thank you!">	
	<p> You have successfully processed the request to join your community and the user who placed the request will be notified of your decision shortly. </p>
	<c:if test="${not empty param.result and param.result == 'dupLeaderResponse'}">			
		<div class='warn message'>another leader has already handled this request</div>
	</c:if>					
</star:template>