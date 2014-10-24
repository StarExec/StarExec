<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.List, org.starexec.data.security.*, org.starexec.data.to.DefaultSettings.SettingType, org.starexec.constants.*, java.lang.StringBuilder, java.io.File, org.apache.commons.io.FileUtils, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.constants.R" session="true"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%


try {
	
	// grab the ID of the default settings profile that we using
	int id = Integer.parseInt((String)request.getParameter("id"));
	int userId = SessionUtil.getUserId(request);
	String type=request.getParameter("type");
	if ((type.equals("solver")  || type.equals("benchmark")) && SettingSecurity.canModifySettings(id,userId).isSuccess()) {
		// Only allowing editing of the default benchmark if the user
		// is a leader of the community being edited
		DefaultSettings s= Settings.getProfileById(id);
		
		//first, validate that this user can 
		
		request.setAttribute("primId",s.getPrimId());
		request.setAttribute("settingId",s.getId());
		request.setAttribute("type",type);
		if (s.getType()==SettingType.USER) {
			request.setAttribute("settingType","user");
		} else {
			request.setAttribute("settingType","comm");
		}
		
		
	} else {
		response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "The given type parameter was not correct-- it must be either solver or benchmark");
	}
	
} catch (Exception e) {
	response.sendError(HttpServletResponse.SC_BAD_REQUEST);
}
%>

<star:template title="select default ${type}" css="common/delaySpinner, edit/defaultPrimitive, edit/shared, common/table" js="common/delaySpinner, lib/jquery.dataTables.min, lib/jquery.validate.min, edit/defaultPrimitive">
	<span style="display:none;" id="primType" value="${type}"></span>
	<span style="display:none;" id="primId" value="${primId}"></span>
	<span style="display:none;" id="settingId" value="${settingId}"></span>
	<span style="display:none;" id="settingType" value="${settingType}"></span>
	
	<fieldset>
	<form id="selectDefaultPrim">
		<fieldset>
			<legend class="expd" id="primExpd"><span>0</span> ${type}s</legend>
			<table id="prims">
				<thead>
					<tr>
						<th style="width:75%;">name</th>
						<th>type</th>											
					</tr>
				</thead>		
			</table>	
		</fieldset>
		<button type="button" id="cancel">cancel</button>
		<button type="button" id="update">update</button>
		</form>
	</fieldset>
</star:template>