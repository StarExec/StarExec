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
		<p>this is a beta release of starexec.  you can help us improve starexec's quality by reporting bugs directly to the developers as well as any general feedback on functionality and design by selecting 'give feedback' below.  the links below, particularly the user guide, can be of great help to new users.</p>
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
			<a href="mailto:${contactEmail}?subject=[Starexec ${buildVersion}] Feedback">give feedback</a>
		</li>
		<li>
			<a href="mailto:${contactEmail}?subject=[Starexec ${buildVersion}] Bug Report">report bug</a>
		</li>
		<br/><br/>
		<p>this build was last updated on ${buildDate}</p>

</star:template>