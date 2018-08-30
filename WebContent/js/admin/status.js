/* Set Status Message
 */

jQuery(function($) {
	var formUrl = starexecRoot + "services/admin/setStatusMessage";
	$("form").on("submit", function(event) {
		var json = {
			"enabled": $("#enabled").prop("checked"),
			"message": $("#message").val(),
			"url"    : $("#url").val()
		};
		$.post(formUrl, json, parseReturnCode, "json");
		return false;
	});
});
