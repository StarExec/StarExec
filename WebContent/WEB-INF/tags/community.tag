<%@tag description="A clickable link to another user" import="org.starexec.data.to.*"%>
<%@attribute name="value" required="true" description="The community to link to" type="org.starexec.data.to.Space" %>

<a href="/${starexecRoot}/secure/explore/communities.jsp">${value.name}<img class="extLink" src="/${starexecRoot}/images/external.png"/></a>