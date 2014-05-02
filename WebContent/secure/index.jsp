<%@page contentType="text/html" pageEncoding="UTF-8"
	import="org.starexec.util.*,org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.Util"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
	int userId = SessionUtil.getUserId(request);
		User user = Users.get(userId);
		System.out.println("role = " + user.getRole());
		System.out.println("unauthorized = " + user.getRole().equals("unauthorized"));
		System.out.println("suspended = " + user.getRole().equals("suspended"));
		if (!user.getRole().equals("unauthorized") && !user.getRole().equals("suspended")){
			String redirectURL = Util.docRoot("secure/explore/spaces.jsp");
    		response.sendRedirect(redirectURL);
		}
%>
<star:template title="starexec preview">
	<c:if test="${user.role == 'unauthorized'}">
		<p>
			<strong>you have not yet been authorized to use the StarExec
				services.</strong>
		</p>
		<br />
		<p>the leaders of the community you selected during registration
			will be notified of your request to join shortly.</p>
		<p>once a leader of that community has approved your request, you
			will receive an email from us. at that point your registration will
			be complete and you will be free to login and begin using our
			service.  you may login as a guest user to explore our public offerings.</p>
		<br />
		<p>thank you for your patience!</p>
		<p>you will be automatically logged out after 20 seconds.</p>
		<script language="javascript">
			setTimeout(logout, 20000);
		</script>
	</c:if>
	<c:if test="${user.role == 'suspended'}">
		<p>
			<strong>You have been suspended and have indefinitely lost access to StarExec services</strong>
		</p>
		<br />
		<p>you will be automatically logged out after 20 seconds.</p>
		<script language="javascript">
			setTimeout(logout, 20000);
		</script>
	</c:if>
	<c:if test="${user.role != 'unauthorized' || user.role != 'suspended'}">
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
		<br />
		<br />
		<li><a
			href="http://starexec.cs.uiowa.edu/starexec/public/quickReference.jsp">quick
				reference</a></li>
		<li><a href="http://wiki.uiowa.edu/display/stardev/User+Guide">user
				guide</a></li>
		<li><a href="http://wiki.uiowa.edu/display/stardev/Home">public
				dev wiki</a></li>

		<li><a
			href="mailto:${contactEmail}?subject=[Starexec ${buildVersion}] Feedback">give
				feedback</a></li>
		<li><a
			href="mailto:${contactEmail}?subject=[Starexec ${buildVersion}] Bug Report">report
				bug</a></li>
		<br />
		<br />
		<p>this build was last updated on ${buildDate}</p>
	</c:if>
</star:template>