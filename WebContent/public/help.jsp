<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<star:template title="StarExec Support">
<div id="support">
	<ul>
		<li>
			<a href="${starexecRoot}/secure/help.jsp">On-page documentation</a>,
			also available (if logged in) via the Help link at the top of the page
		</li>
		<li>
			<a href="${starexecRoot}/public/quickReference.jsp">Quick Reference</a>
		</li>
		<li>
			<a href="http://wiki.uiowa.edu/display/stardev/User+Guide">User Guide</a>,
			detailed description of StarExec features and functionality
		</li>
		<li>
			<a href="http://starexec.lefora.com">StarExec Forum</a>,
			for questions, bug reports, feature requests
		</li>
		<li>
			<a href="http://wiki.uiowa.edu/display/stardev/Home">Public Developers Wiki</a>,
			for those interested in development aspects
		</li>
		<li>
			<a href="${starexecRoot}/public/WebInterface.pdf">Web Interface Documentation</a> for making direct HTTP calls to Starexec
		</li>
	</ul>
</div>
</star:template>
