/* Set Status Message
 */

jQuery(function($) {
	var setUrl = starexecRoot + "services/admin/setStatusMessage";
	var getUrl = starexecRoot + "services/admin/getStatusMessage";

	$.get(
		getUrl,
		{},
		function(json) {
			console.log(json);
			$("#enabled").prop("checked", json.enabled);
			$("#message").val(json.message);
			$("#url").val(json.url);
		},
		"json"
	)

	$("form").on("submit", function(event) {
		var json = {
			"enabled": $("#enabled").prop("checked"),
			"message": $("#message").val(),
			"url"    : $("#url").val()
		};
		$.post(setUrl, json, parseReturnCode, "json");
		return false;
	});
});
