/**
 * The following functions are used to create a dialog informing the user that a process is ongoing
 * A maximum of one dialog should be created on a given page at a time.
 * delaySpinner.css provides the styles for the elements added to the DOM
 * Author: Eric Burns
 */

$(document).ready(function() {
	"use strict";
	var delayToken;
	var delayInterval = null;
	var creatingDelaySpinner = false;
	var $dialog = null;

	var createDialog = function() {
		$dialog = $("<div id='delaySpinner'><p id='delayMessage'>no text</p><p id='imageContainer'><img style='display:block' alt='spinner' id='spinnerImage' src='" + starexecRoot + "images/ajaxloader.gif' /></p></div>")
		.dialog({
			modal: true,
			dialogClass: "delaySpinner",
			title: "Processing Request",
			draggable: false,
			resizable: false,
			show: "fade"
		});
	}
	/** Creates a new delay dialog. If one already exists, does nothing. */
	window.createDialog = function(message) {
		//indicate that we're in the middle of creating the delay
		creatingDelaySpinner = true;
		if ($dialog == null) {
			createDialog();
		}
		$dialog.dialog("open");
		$dialog.text(message);
		//indicate we're done with the delay
		creatingDelaySpinner = false;
	}

	/**
	 * Completely removes dialog if it exists. If we're in the middle of creating a
	 * dialog, waits for the creation to finish before deleting
	 */
	window.destroyDialog = function() {
		//if we're in the middle of creating a spinner, just wait a small amount of time and then call this again
		if (creatingDelaySpinner) {
			setTimeout(window.destroyDialog, 30);
		} else {
			$dialog.dialog("close");
		}
	}

	window.checkCookie = function() {
		if ($.cookie('fileDownloadToken') == delayToken) {
			window.clearInterval(delayInterval);
			delayInterval = null;
			delayToken = null;
			destroyDialog();
		}
	}

	/**
	 * Destroys the dialog only when a cookie has been received from the server with
	 * the name "fileDownloadToken" and the value "curToken." If a previous call to
	 * this function is still working, does nothing.
	 */
	window.destroyOnReturn = function(curToken) {
		if (delayInterval == null) {
			delayToken = curToken;
			delayInterval = setInterval(checkCookie, 50);
		}
	}

	$(window).unload(window.destroyDialog);
});
