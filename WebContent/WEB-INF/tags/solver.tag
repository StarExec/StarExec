<%@tag description="A clickable link to a solver" import="org.starexec.data.to.*"%>
<%@attribute name="value" required="true" description="The solver object to link to" type="org.starexec.data.to.Solver" %>

<a title="${value.description}" href="${starexecRoot}/secure/details/solver.jsp?id=${value.id}" target="_blank">${value.name}<img class="extLink" src="${starexecRoot}/images/external.png"/></a>
