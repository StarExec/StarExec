<%@tag description="Dialog for copy-to-stardev feature.."%>
<%@tag trimDirectiveWhitespaces="true" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="dialog-copy-to-stardev hiddenDialog" title="copy to stardev">
	<div class="instanceNameDiv copyToStardevInputDiv"><span class="instanceNameText stardevInstanceNameText">instance name</span><input class="instanceName" type="text"/></div>
	<div class="stardevUsernameDiv copyToStardevInputDiv"><span class="usernameText stardevUsernameText">stardev username</span><input class="stardevUsername" type="text"/></div>
	<div class="stardevPasswordDiv copyToStardevInputDiv"><span class="passwordText stardevPasswordText">stardev password</span><input class="stardevPassword" type="password"/></div>
	<div class="stardevSpaceIdDiv copyToStardevInputDiv"><span class="spaceIdText stardevSpaceIdText"></span><input class="stardevSpaceId" type="number" min="0"/></div>
	<div class="stardevProcIdDiv copyToStardevInputDiv hidden"><span class="stardevProcIdText">stardev processor id</span><input class="stardevProcId" type="number" min="0"/></div>
	<div class="uploadProcessorWithBenchmarkDiv hidden">upload with processor<input class="uploadProcessorWithBenchmarkCheckbox" type="checkbox" checked></div>
</div>
