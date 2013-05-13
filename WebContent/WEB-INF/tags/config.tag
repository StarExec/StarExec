<%@tag description="A clickable link to a configuration" import="org.starexec.data.to.*"%>
<%@attribute name="value" required="true" description="The configuration object to link to" type="org.starexec.data.to.Configuration"%>

<a title="${value.description}" href="/${starexecRoot}/secure/details/configuration.jsp?id=${value.id}" target="_blank">${value.name}<img class="extLink" src="/${starexecRoot}/images/external.png"/></a>