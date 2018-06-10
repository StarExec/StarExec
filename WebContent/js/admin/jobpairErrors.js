"use strict";

jQuery(function($) {
	var resultsTableConfig = new window.star.DataTableConfig({
		"columns": [
			{"title": "Node"},
			{
				"title": "JobPair",
				"width": "120px",
				"className": "dt-right"
			},
			{
				"title": "Time",
				"width": "120px",
				"className": "dt-right"
			},
		],
		"order": [[2, 'desc']]
	});
	var resultsTable = $("#jobpairErrors").DataTable(resultsTableConfig);

	var $startField = $("#dateselector [name='start']");
	var $endField = $("#dateselector [name='end']");

	var resultsTableRefresh = function(data, textStatus, jqXHR) {
		resultsTable.clear();
		var row = data.length;
		while (--row != -1) {
			resultsTable.row.add([
				data[row]["node"]["name"],
				data[row]["jobPair"]["id"],
				star.format.timestamp(data[row]["time"]),
			]);
		}
		resultsTable.draw();
	};

	$("#dateselector").on("submit", function(event) {
		var payload = {};
		if ($startField.val().trim().length != 0) {
			payload["start"] = $startField.val();
		}
		if ($endField.val().trim().length != 0) {
			payload["end"] = $endField.val();
		}

		$.get(
			starexecRoot + "services/jobpairErrors",
			payload,
			resultsTableRefresh
		);

		event.preventDefault();
	});

	$("#dateselector").trigger("submit");
});
