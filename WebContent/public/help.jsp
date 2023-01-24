<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<star:template title="StarExec Support">
	<div id="support">
		<ul>
			<li>
				<a href="${starexecRoot}/secure/help.jsp">On-page
					documentation</a>,
				also available (if logged in) via the Help link at the top of
				the page
			</li>
			<li>
				<a href="${starexecRoot}/public/quickReference.jsp">Quick
					Reference</a>
			</li>
			<li>
				<a href="${starexecRoot}/public/StarExecUserGuide.pdf">User
					Guide</a>,
				detailed description of StarExec features and functionality
			</li>
			<li>
				<a href="https://github.com/StarExec/StarExec">StarExec on GitHub</a>,
				for bug reports and feature requests
			</li>
			<li>
				<a href="${starexecRoot}/public/WebInterface.pdf">Web Interface
					Documentation</a> for making direct HTTP calls to Starexec
			</li>
			<li>
				<a href="https://www.youtube.com/channel/UCoYhHKXD5agIia60z-RiX2A">Video
					Tutorials</a>
			</li>
		</ul>
		<p>Starexec revision ${buildVersion} built ${buildDate}</p>
	</div>
</star:template>
