<%@tag description="A clickable link to a solver" import="org.starexec.util.Util" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@attribute name="id" required="false" description="id" type="String" %>
<%@attribute name="title" required="true" description="Title" type="String" %>
<%@attribute name="withCount" required="false" type="Boolean"
	description="Should this fieldset contain a count of contained list items" %>
<%@attribute name="test" required="false" type="Boolean"
	description="Will hide panel if this is false" %>
<%@attribute name="expandable" required="false" type="Boolean"
	description="Is this panel expandable" %>

<c:if test="${test!=false}">
	<fieldset
		<c:if test="${expandable == null || expandable.booleanValue()}">class="expd"</c:if>
		<c:if test="${id != null && id.length()!=0}">id="${id}"</c:if>
	>
		<legend>
			<c:if test="${withCount}"><span class="list-count"></span></c:if>
			${title}
		</legend>
		<jsp:doBody/>
	</fieldset>
</c:if>
