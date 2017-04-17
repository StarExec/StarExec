<%@tag import="org.starexec.constants.Web"%>
<%@tag description="Dialog for copy-to-stardev feature.."%>
<%@tag trimDirectiveWhitespaces="true" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	request.setAttribute("copyToStardevButtonText", Web.COPY_TO_STARDEV_BUTTON_TEXT);
%>
<button class="copyToStarDev" type="button">${copyToStardevButtonText}</button>
