<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<star:template title="starexec help">
	
		<style type="text/css">
			#content a:link {
				text-decoration: underline;
			}
			#content ul {
				margin-left: 40px;
			}
			#content ul li {
				padding: 3px;
			}
		</style>		
		<p>Some of the documentation below needs to be updated, but this should still be helpful.  We are also working to create on-page documentation, via the help link at the top of the page. </p>
		<br/><br/>		
		<li>
			<a href="http://starexec.cs.uiowa.edu/starexec/public/quickReference.jsp">quick reference</a>
		</li>
		<li>
			<a href="http://wiki.uiowa.edu/display/stardev/User+Guide">user guide</a>
		</li>		
		<li>
			<a href="http://wiki.uiowa.edu/display/stardev/Home">public dev wiki</a>
		</li>

		<li>
			<a href="http://starexec.wordpress.com">StarExec blog</a>, for upcoming features and current issues
		</li>
		<li>
			<a href="mailto:${contactEmail}?subject=[Starexec ${buildVersion}] Feedback">give feedback or report bug</a>
		</li>
		<br/><br/>
		<p>this build was last updated on ${buildDate}</p>

</star:template>