<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@page import="java.util.ArrayList, java.util.List"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%	
	try {	
		List<Space> publicCommunities = Communities.getCommsWithPublicSolvers();
		List<Solver> publicSolvers = Solvers.getPublicSolvers();
		request.setAttribute("publicSolvers",publicSolvers);
		request.setAttribute("publicCommunities",publicCommunities);
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "bad request.");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "not found");		
	}
%>
<star:template title="create your own job" css="add/singleJobPair" js="lib/jquery.validate.min, add/singleJobPair,  lib/*, http://code.jquery.com/jquery-latest.js">
	We are currently preparing our public release, and the quick job feature will be enabled again before the release.

</star:template>