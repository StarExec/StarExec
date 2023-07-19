<%@tag description="Standard footer for all starexec pages"%>
<%@tag import="org.starexec.data.database.StatusMessage"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<footer id="pageFooter">
	<c:if test="${!isLocalJobPage}">
		<ul>
			<c:if test="${not empty user}">
				<li><a target="_blank"
					href="${starexecRoot}/secure/details/user.jsp?id=${user.id}">${user.fullName}</a></li>
				<li>|</li>
				<li><a onclick="logout();">Logout</a></li>
			</c:if>
			<c:if test="${empty user}">
				<li><a id="loginLink" href="${starexecRoot}/secure/index.jsp">Login</a></li>
			</c:if>
			<li>|</li>
			<li><a id="about" href="${starexecRoot}/public/about.jsp">About</a></li>
			<li>|</li>
			<li><a id="help" href="${starexecRoot}/public/help.jsp">Support</a></li>
			<li>|</li>
			<li><a id="starexeccommand" href="${starexecRoot}/public/starexeccommand.jsp">StarExec Command</a></li>
		</ul>
	</c:if>
	<a class="copyright" href="http://www.cs.uiowa.edu" target="_blank">&copy;
		2012-18 The University of Iowa</a>
</footer>

<%= StatusMessage.getAsHtml() %>
