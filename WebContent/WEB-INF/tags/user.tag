<%@tag description="A clickable link to another user" import="org.starexec.data.to.*"%>
<%@attribute name="value" required="true" description="The user object to link to" type="org.starexec.data.to.User" %>

<a href="/${starexecRoot}/secure/details/user.jsp?id=${value.id}" target="_blank">${value.fullName}<img class="extLink" src="/${starexecRoot}/images/external.png"/></a>