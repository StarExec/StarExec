<%@tag description="Standard footer for all starexec pages"%>
<%@tag import="org.starexec.data.to.*, org.starexec.constants.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<footer id="pageFooter">
	<c:if test="${not empty user}">
		<ul>
			<li><a target="_blank"
				href="/${starexecRoot}/secure/details/user.jsp?id=${user.id}">${fn:toLowerCase(user.fullName)}</a></li>
			<li>|</li>
			<li><a onclick="javascript:logout();">Logout</a></li>
			<li>|</li>
			<li><a id="about" href="/${starexecRoot}/public/about.jsp">About</a></li>
			<li>|</li>
			<li><a id="help" href="/${starexecRoot}/public/help.jsp">Support</a></li>
			<li>|</li>
			<li><a id="starexeccommand" href="/${starexecRoot}/public/starexeccommand.jsp">StarExec Command</a></li>
			<!--  <li>|</li>
			<li><a id="quickjob" href="/${starexecRoot}/secure/add/quickJob.jsp?sid=-1">Quick Job</a></li>-->
		</ul>
	</c:if>
	<c:if test="${empty user}">
		<ul>
			<li><a id="loginLink" href="/${starexecRoot}/secure/index.jsp">Login</a></li>
			<%--<li>|</li>--%>
			<%--  <li><a href="/${starexecRoot}/public/registration.jsp">Register</a></li>--%>
			<li>|</li>
			<li><a href="/${starexecRoot}/secure/j_security_check?j_username=public&j_password=public">Guest</a></li>
			<li>|</li>
			<li><a id="about" href="/${starexecRoot}/public/about.jsp">About</a></li>
			<li>|</li>
			<li><a id="help" href="/${starexecRoot}/public/help.jsp">Support</a></li>
			<li>|</li>
			<li><a id="starexeccommand" href="/${starexecRoot}/public/starexeccommand.jsp">StarExec Command</a></li>
			<!-- <li>|</li>
			<li><a id="quickJobGuest" href="/${starexecRoot}/secure/j_security_check?j_username=public&j_password=public&quickjob=true">Quick Job</a></li>-->
			
		</ul>
	</c:if>
	<a class="copyright" href="http://www.cs.uiowa.edu" target="_blank">&copy;
		2012-14 The University of Iowa</a>
</footer>

