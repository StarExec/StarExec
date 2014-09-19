<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<star:template title="StarExec Support">
  <div id="support"><br>		
	<ul id="support">
   	<li>
			<a href="/${starexecRoot}/secure/help.jsp">On-page documentation</a>, 
			also available (if logged in) via the Help link at the top of the page
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
			<a href="http://starexec.forumotion.com">StarExec Forum</a>, 
			for questions, bug reports, feature requests
			<br><br>
		</li>
		<li>
			<a href="http://wiki.uiowa.edu/display/stardev/Home">Public Developers Wiki</a>, 
			for those intestered in development aspects
			<br>
		</li>
  </ul>
 </div>
</star:template>
