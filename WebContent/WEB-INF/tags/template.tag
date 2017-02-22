<%@tag description="Template tag for all starexec pages"%>
<%@tag import="java.util.List, org.starexec.data.to.*, org.starexec.constants.*, org.starexec.util.*"%>
<%@tag trimDirectiveWhitespaces="true" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>

<%@attribute name="title" %>
<%@attribute name="css" %>
<%@attribute name="js" %>

<%

	try {
		String localJobPageParameter = request.getParameter(Web.LOCAL_JOB_PAGE_PARAMETER);
		boolean isLocalJobPage = (localJobPageParameter != null) && localJobPageParameter.equals("true");
		request.setAttribute("isLocalJobPage", isLocalJobPage);

		final String starexecRoot = "starexecRoot";
		if (isLocalJobPage) {
			request.setAttribute(starexecRoot, ".");
		} else {
			request.setAttribute(starexecRoot, "/" + R.STAREXEC_APPNAME);
		}

		request.setAttribute("globalJsFiles", Web.GLOBAL_JS_FILES);
		request.setAttribute("globalCssFiles", Web.GLOBAL_CSS_FILES);

	} catch (Exception e) {
	}
%>

<!DOCTYPE html>
<html lang="en">
	<star:head title="${title}" css="${css}" js="${js}"/>
	<body>
		<div id="wrapper">
			<star:header />
			<div id="content" class="round">
				<div id="mainHeaderWrapper">
					<h1 style="width:100%; word-wrap:break-word;" id="mainTemplateHeader">${title}</h1>
				</div>
				<img alt="loading" src="${starexecRoot}/images/loader.gif" id="loader">
				<jsp:doBody/>
			</div>
			<star:footer />
			<div id="buildInfo" title="built by: ${buildUser} (${buildDate})"><a href="${starexecRoot}/public/versionInfo.jsp">StarExec revision ${buildVersion}</a></div>
		</div>
	</body>
</html>
