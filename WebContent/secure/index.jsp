<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<star:template title="starexec preview">
	<c:if test="${user.role == 'unauthorized'}">
		<p><strong>you have not yet been authorized to use the StarExec services.</strong></p><br />
		<p>the leaders of the community you selected during registration will be notified of your request to join shortly.</p>
		<p>once a leader of that community has approved your request, you will receive an email from us. at that point your registration will be complete and you will be free to login and begin using our service.</p><br />
		<p>thank you for your patience!</p>
		<p>you will be automatically logged out after 20 seconds.</p>
		<script language="javascript"> 
			setTimeout(logout,20000); 
		</script>
	</c:if> 
	<c:if test="${user.role != 'unauthorized'}">
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
		<p>this is an alpha release of starexec. please note this is a limited preview, and as with all new software we will be prone to bugs and unfinished features. all user-generated content may be cleared at any time, but your credentials will remain in tact during the preview. you can help us improve starexec's quality by reporting bugs directly to the developers as well as any general feedback on functionality and design by selecting 'give feedback' below. you can return to this page by clicking the starexec logo at the top of any page.</p>
		<br/><br/>		
		<ul>
			<li>
				<a href="mailto:${contactEmail}?subject=[Starexec ${buildVersion}] Feedback">give feedback</a>
			</li>
			<li>
				<a href="mailto:${contactEmail}?subject=[Starexec ${buildVersion}] Bug Report">report bug</a>
			</li>
			<li>
				<a href="http://wiki.uiowa.edu/display/stardev/Home">public dev wiki</a>
			</li>
			<li>
				<a href="http://wiki.uiowa.edu/display/stardev/User+Guide">user guide</a>
			</li>
		</ul>	
		<br/><br/>
		<p>this build was last updated on ${buildDate}</p>
	</c:if>
</star:template>