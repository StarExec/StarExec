<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%
	try {
		String email = request.getParameter("email");
		request.setAttribute("email", email);
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>
<star:template title="email changed">
	<p>You have successfully changed your email to ${email}</p>
	<p>You must now use this e-mail when logging in to StarExec.</p>
</star:template>
