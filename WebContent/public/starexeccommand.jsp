<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<star:template title="StarExecCommand">
	
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
		<p>StarExecCommand is a Java program that allows users to make requests of the StarExec server from a command prompt.
		Documentation can be found in the archive, and is also available <a href="${starexecRoot}/public/manual.txt">here</a><br/><br/>
		<a href="${starexecRoot}/public/starexeccommand.zip">Download StarExecCommand</a></p>
		

</star:template>