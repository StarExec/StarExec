<%@tag description="A clickable link to another user" import="org.starexec.data.to.*"%>
<%@attribute name="value" required="true" description="The user object to link to" type="org.starexec.data.to.User" %>

<a href="/starexec/secure/details/user.jsp?id=${value.id}">${value.fullName}</a>