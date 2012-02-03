<%@tag description="A clickable link to a configuration" import="org.starexec.data.to.*"%>
<%@attribute name="value" required="true" description="The configuration object to link to" type="org.starexec.data.to.Configuration"%>

<a title="${value.description}" href="/starexec/secure/details/solver.jsp?id=${value.solverId}">${value.name}<img class="extLink" src="/starexec/images/external.png"/></a>