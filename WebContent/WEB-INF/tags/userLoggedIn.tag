<%@tag description="Only outputs body when user is logged in" %>
<%@tag import="org.starexec.util.SessionUtil, org.starexec.constants.R" %>
<%@tag trimDirectiveWhitespaces="true" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
	int uid = SessionUtil.getUserId(request);
	request.setAttribute("userLoggedIn", uid != R.PUBLIC_USER_ID);
%>

<c:if test="${userLoggedIn}">
<jsp:doBody />
</c:if>
