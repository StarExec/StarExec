<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<star:template title="StarExec Support">
	
<!--		<style type="text/css">
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
-->
    <br>		
		<li>
			<a href="/${starexecRoot}/secure/help.jsp">On-page documentation</a>, 
			also available via the Help link at the top of the page
			<br><br>
		</li>
		<li>
			<a href="/${starexecRoot}/public/quickReference.jsp">Quick Reference</a>
			<br><br>
		</li>
		<li>
			<a href="http://wiki.uiowa.edu/display/stardev/User+Guide">User Guide</a>, 
			detailed description of StarExec features and functionality
			<br><br>
		</li>		
		<li>
			<a href="http://starexec.wordpress.com">StarExec Blog</a>, 
			for upcoming features and current issues
			<br><br>
		</li>
		<li>
			<a href="http://starexec.forumotion.com">StarExec Forum</a>, 
			for questions, bug reports, feature requests
			<br><br>
		</li>
		<li>
			<a href="http://wiki.uiowa.edu/display/stardev/Home">Public Developers Wiki</a>, 
			for those intestered in development aspects
			<br>
		</li>
		<br/><br/>
		<!-- <p>this build was last updated on ${buildDate}</p> -->

</star:template>
