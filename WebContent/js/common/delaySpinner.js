/**
 * The following functions are used to create a dialog informing the user that a process is ongoing
 * A maximum of one dialog should be created on a given page at a time.
 * delaySpinner.css provides the styles for the elements added to the DOM
 * Author: Eric Burns
 */


$(document).ready(function() {
	$("body").append("<img style=\"visibility:hidden;\" alt=\"spinner\" id=\"spinnerImage\" src=\"/starexec/images/ajaxloader.gif\"/>")
});

function createDialog(message) {	
	setTimeout(function() {
		$("body").append("<div id=delaySpinner><p id=\"delayMessage\">" +message+ "</p><p id=\"imageContainer\"></p></p>");
		$("#imageContainer").prepend($("#spinnerImage"));
		$("#spinnerImage").css("visibility", "visible");
		$("#delaySpinner").dialog({
			modal:true,
			dialogClass: "delaySpinner",
			title: "Processing Request",
			draggable: false,
			resizable: false,
			show: "fade"
		});
	},20);
	

}

function destroyDialog() {
	$("#delaySpinner").dialog("destroy");
	$("#spinnerImage").css("visibility","hidden");
	$("body").append($("#spinnerImage"));
	$("#delaySpinner").remove();
}