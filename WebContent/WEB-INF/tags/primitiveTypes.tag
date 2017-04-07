<%@tag import="org.starexec.data.to.enums.Primitive, java.util.EnumSet"%>
<%@tag description="Dialog for copy-to-stardev feature.."%>
<%@tag trimDirectiveWhitespaces="true" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	request.setAttribute("allPrimitiveTypes", EnumSet.allOf(Primitive.class));
%>
<c:forEach var="primitiveType" items="${allPrimitiveTypes}">
	<span class="hidden ${primitiveType.cssClass}" value="${primitiveType.toString()}"></span>
</c:forEach>
