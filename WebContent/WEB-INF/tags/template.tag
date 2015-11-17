<%@tag description="Template tag for all starexec pages"%>
<%@tag import="java.util.List, org.starexec.data.to.*, org.starexec.constants.*, org.starexec.util.*"%>
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
			request.setAttribute(starexecRoot, "./");
		} else {
			request.setAttribute(starexecRoot, "/" + R.STAREXEC_APPNAME);
		}

		List<String> globalJsFiles = Util.csvToList(Web.GLOBAL_JS_FILES);
		request.setAttribute("globalJsFiles", globalJsFiles);
		List<String> globalCssFiles = Util.csvToList(Web.GLOBAL_CSS_FILES);
		request.setAttribute("globalCssFiles", globalCssFiles);

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
				<h1 style="width:100%; word-wrap:break-word;" id="mainTemplateHeader">${title}</h1>
				<img alt="loading" src="${starexecRoot}/images/loader.gif" id="loader">			
				<jsp:doBody/>
			</div>		
		<star:footer />
		<div id="buildInfo" title="built by: ${buildUser} (${buildDate})"><a href="${starexecRoot}/public/versionInfo.jsp">StarExec revision ${buildVersion}</a></div>
		</div>
	</body>
</html>
