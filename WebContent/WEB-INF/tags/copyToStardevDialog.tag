<%@tag description="Dialog for copy-to-stardev feature.."%>
<%@tag trimDirectiveWhitespaces="true" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="dialog-copy-to-stardev hiddenDialog">
	<div class="instanceNameDiv copyToStardevInputDiv"><span class="instanceNameText">instance name</span><input class="instanceName" type="text"/></div>
	<div class="stardevUsernameDiv copyToStardevInputDiv"><span class="stardevUsernameText">stardev username</span><input class="stardevUsername" type="text"/></div>
	<div class="stardevPasswordDiv copyToStardevInputDiv"><span class="stardevPasswordText">stardev password</span><input class="stardevPassword" type="password"/></div>
	<div class="stardevSpaceIdDiv copyToStardevInputDiv"><span class="stardevSpaceIdText">stardev space id</span><input class="stardevSpaceId" type="number" min="0"/></div>
</div>
