<%@tag description="A clickable link to benchmark" import="org.starexec.data.to.*"%>
<%@attribute name="value" required="true" description="The benchmark object to link to" type="org.starexec.data.to.Benchmark" %>

<a title="${value.description}" href="/starexec/pages/details/benchmark.jsp?id=${value.id}">${value.name}</a>