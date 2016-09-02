<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.HashMap, java.util.Map, java.util.ArrayList, java.util.List, org.apache.commons.lang3.StringUtils, org.starexec.app.RESTHelpers, org.starexec.constants.*, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.data.to.JobStatus.JobStatusCode, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType, org.starexec.util.dataStructures.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int jobId = Integer.parseInt(request.getParameter("jobId"));
        request.setAttribute("jobId", jobId);

        int configId = Integer.parseInt(request.getParameter("configId"));
        request.setAttribute("configId", configId);

        int stageNumber = Integer.parseInt(request.getParameter("stageNumber"));
        request.setAttribute("stageNumber", stageNumber);
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>
<star:template title="" js="util/sortButtons, util/jobDetailsUtilityFunctions, common/delaySpinner, lib/jquery.jstree, lib/jquery.dataTables.min, details/shared, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/table, common/delaySpinner, explore/common, details/shared">		
	<p>jobId ${jobId}</p>
    <p>configId ${configId}</p>
    <p>stageNumber ${stageNumber}</p>
</star:template>
