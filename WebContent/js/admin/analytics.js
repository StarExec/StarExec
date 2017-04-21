"use strict";

jQuery(function($) {
	var resultsTableConfig = new window.star.DataTableConfig();
	var resultsTable = $("#analytics_results").DataTable(resultsTableConfig);

	var $startField = $("#dateselector [name='start']");
	var $endField   = $("#dateselector [name='end']"  );

	var resultsTableRefresh = function(data, textStatus, jqXHR) {
		resultsTable.clear();
		var row = data.length;
		while (--row != -1) {
			resultsTable.row.add([
				data[row]["event"],
				data[row]["count"],
			]);
		}
		resultsTable.page.len(data.length).draw();
	};

	$("#dateselector").on("submit", function(event) {
		$.get(
			starexecRoot+"services/analytics",
			{
				"start": $startField.val(),
				"end": $startField.val()
			},
			resultsTableRefresh
		);

		event.preventDefault();
	});

	$("#dateselector").trigger("submit");
});
