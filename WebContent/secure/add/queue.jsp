<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.List, org.starexec.data.database.*, org.starexec.constants.*, org.starexec.util.*, org.starexec.data.to.*;"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%

try {
	int userId = SessionUtil.getUserId(request);
	List<Space> spaces = Spaces.GetAllSpaces();
	request.setAttribute("queueNameLen", R.QUEUE_NAME_LEN);
	request.setAttribute("spaces", spaces);
			
} catch (NumberFormatException nfe) {
	response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given user id was in an invalid format");
} catch (Exception e) {
	response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
}

%>

<star:template title="create queue" css="explore/common, explore/spaces" js="lib/jquery.validate.min, add/queue, CalenderPopup">	
	<form id="addForm" method="POST" action="/${starexecRoot}/secure/reserve/queue" class="reservation">	
		<fieldset id="fieldStep1">
			<legend>Add a Queue</legend>
			<table id="tblConfig" class="shaded contentTbl">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
					<tr class="noHover" title="the space to reserve a queue for">
						<td class="label"><p>space name</p></td>
						<td>
							<select name="spaceName" id="spaces">
       							<c:forEach var="space" items="${spaces}">
           							 <option value="${space.name}">${space.name}</option>
        						</c:forEach>
    						</select>
    					</td>
					</tr>
					<tr class="noHover" title="what would you like to name your reserved queue?">
						<td class="label"><p>queue name</p></td>
						<td><input length="${queueNameLen}" id="txtQueueName" name="name" type="text"/></td>
					</tr>
					<tr>
						<td class="label"><p>number of nodes</p></td>
						<td><input length="3" id="nodeNum" name="node" type="text"/></td>
					</tr>
					<tr class="noHover" title="when would you like to begin your reservation?">
						<td class="label"><p>start date</p></td>
						<td>
							<SCRIPT ID="js1">
								var now = new Date();
								var cal1 = new CalendarPopup();
								cal1.addDisabledDates(null,formatDate(now,"yyyy-MM-dd"));
							</SCRIPT>
							<INPUT TYPE="text" ID="start" NAME="start" VALUE="" SIZE=25>
							<A HREF="#" onClick="cal1.select(document.forms[0].start,'anchor1','MM/dd/yyyy'); return false;" NAME="anchor1" ID="anchor1">select</A>		
						</td>						
					</tr>	
					<tr class="noHover" title="when would you like to end your reservation?">
						<td class="label"><p>end date</p></td>
						<td>
							<SCRIPT ID="js2">
								var cal2 = new CalendarPopup();
							</SCRIPT>
							<INPUT TYPE="text" ID="end" NAME="end" VALUE="" SIZE=25>
							<A HREF="#" onClick="cal1.select(document.forms[0].end,'anchor2','MM/dd/yyyy',(document.forms[0].end.value=='')?document.forms[0].start.value:null); return false;" NAME="anchor2" ID="anchor2">select</A>		 
						</td>						
					</tr>						
				</tbody>
			</table>
	</fieldset>
	<div id="actionBar">
		<button type="submit" class="round" id="btnSubmit">next</button>			
	</div>	
</form>

	<c:if test="${not empty param.result and param.result == 'requestSent'}">			
		<div class='success message'>request sent successfully - you will receive an email when a leader of that community approves/declines your request</div>
	</c:if>
	<c:if test="${not empty param.result and param.result == 'requestNotSent'}">			
		<div class='error message'>you are already a member of that community, or have already requested to be and are awaiting approval</div>
	</c:if>
</star:template>