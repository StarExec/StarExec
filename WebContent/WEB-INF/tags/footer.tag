<%@tag description="Standard footer for all starexec pages"%>
<%@tag import="org.starexec.data.to.*, org.starexec.constants.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<footer id="pageFooter">
	<c:if test="${not empty user}">
		<ul>
			<li><a target="_blank" href="/starexec/secure/details/user.jsp?id=${user.id}">${fn:toLowerCase(user.fullName)}</a></li>
			<li>|</li>
			<li><a onclick="javascript:logout();">logout</a></li>
		    <li>|</li>
		  <li><a id="writeJob" href="/starexec/secure/add/singleJobPair.jsp">write job</a></li>
		   <li>|</li>
		  <li><a id="about" href="/starexec/public/about.jsp">about</a></li>
		</ul>
	</c:if>
	<c:if test="${empty user}">
		<ul>
			<li><a id="loginLink" href="/starexec/secure/index.jsp">login</a></li>
			<%--<li>|</li>--%>
		   <%--  <li><a href="/starexec/public/registration.jsp">register</a></li>--%>
		   <li>|</li>
		  <li><a href="/starexec/public/guest.jsp">guest</a></li>
		  		   <li>|</li>
		   <li><a id="about" href="/starexec/public/about.jsp">about</a></li>
		</ul>		
	</c:if>				
	<a class="copyright" href="http://www.cs.uiowa.edu" target="_blank">&copy; 2012 the university of iowa</a>			
</footer>