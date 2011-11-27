<%@tag description="A clickable link to another user" import="org.starexec.data.to.*"%>
<%@attribute name="value" required="true" description="The community to link to" type="org.starexec.data.to.Space" %>

<a href="/starexec/secure/community/view.jsp">${value.name}</a>