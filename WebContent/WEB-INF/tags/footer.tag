<%@tag description="Standard footer for all starexec pages"%>
<%@tag import="org.starexec.data.to.*, org.starexec.constants.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

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