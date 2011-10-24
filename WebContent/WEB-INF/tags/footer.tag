<%@tag description="Standard footer for all starexec pages"%>
<%@tag import="org.starexec.data.to.*, org.starexec.constants.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<footer id="pageFooter">
	<c:if test="${not empty user}">
		<ul>
			<li>${fn:toLowerCase(user.fullName)}</li>
			<li>|</li>
			<li><a href="#">logout</a></li>
		</ul>
	</c:if>
	<c:if test="${empty user}">
		<a id="loginLink" href="/starexec/pages/index.jsp">login</a>
	</c:if>				
	<a class="copyright" href="http://www.uiowa.edu">(C) 2011 the university of iowa</a>			
</footer>