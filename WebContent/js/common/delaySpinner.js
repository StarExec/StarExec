/**
 * The following functions are used to create a dialog informing the user that a process is ongoing
 * A maximum of one dialog should be created on a given page at a time.
 * delaySpinner.css provides the styles for the elements added to the DOM
 * Author: Eric Burns
 */
var delayToken;
var delayInterval=null;
var creatingDelaySpinner=false;

//Requests img resource needed for the dialog
$(document).ready(function() {
	$("body").append("<img style=\"display:none;\" alt=\"spinner\" id=\"spinnerImage\" src=\""+starexecRoot+"images/ajaxloader.gif\"/>");
});


//Creates a new delay dialog. If one already exists, does nothing.

function createDialog(message) {
	//indicate that we're in the middle of creating the delay
	creatingDelaySpinner=true;
	setTimeout(function() {
		if ($("#delaySpinner").length==0) {
			$("body").append("<div id=delaySpinner><p id=\"delayMessage\">" +message+ "</p><p id=\"imageContainer\"></p></p>");
			$("#imageContainer").prepend($("#spinnerImage"));
			$("#spinnerImage").css("display", "block");
			$("#delaySpinner").dialog({
				modal:true,
				dialogClass: "delaySpinner",
				title: "Processing Request",
				draggable: false,
				resizable: false,
				show: "fade"
			});
		}
		//indicate we're done with the delay
		creatingDelaySpinner=false;
	},0);
}

//Completely removes dialog if it exists. If we're in the middle of creating a dialog, waits for the creation to finish before deleting
function destroyDialog() {
	
	//if we're in the middle of creating a spinner, just wait a small amount of time and then call this again
	if (creatingDelaySpinner) {
		setTimeout(function() {
			destroyDialog();
		},30);
		return;
	}
	if ($("#delaySpinner").length>=1) {
		$("#delaySpinner").dialog("destroy");
		$("#spinnerImage").css("display","none");
		$("body").append($("#spinnerImage"));
		$("#delaySpinner").remove();
	}
	
}

function checkCookie() {
	if ($.cookie('fileDownloadToken')==delayToken) {
		window.clearInterval(delayInterval);	
		delayInterval=null;
		delayToken=null;
		destroyDialog();
	}
}

//Destroys the dialog only when a cookie has been received from the server with the 
//name "fileDownloadToken" and the value "curToken." If a previous call to this function
//is still working, does nothing.

function destroyOnReturn(curToken) {
	if (delayInterval==null) {
		delayToken=curToken;
		
		delayInterval=setInterval(checkCookie,50);
	} 
	
	
}