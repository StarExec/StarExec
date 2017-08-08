<%@tag description="Show text files and hide binary files"%>
<%@tag trimDirectiveWhitespaces="true" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@attribute name="isBinary" %>
<%@attribute name="text" %>
<%@attribute name="lang" %>

<c:set var="preClass" value='prettyprint${(empty lang) ? "" : " lang-".concat(lang)}' />
<c:choose>
	<c:when test="${(not empty isBinary) && isBinary==true}">
		<p>Cannot display binary file</p>
	</c:when>
	<c:otherwise>
		<pre class="${preClass}">${text}</pre>
	</c:otherwise>
</c:choose>
