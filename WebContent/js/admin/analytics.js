"use strict";

jQuery(function($) {
	var resultsTableConfig = new window.star.DataTableConfig({
		"paging": false,
		"columns" : [
			{"title"     : "Event"},
			{"title"     : "Users",
			 "width"     : "120px",
			 "className" : "dt-right"},
			{"title"     : "Count",
			 "width"     : "120px",
			 "className" : "dt-right"},
		],
	});
	var resultsTable = $("#analytics_results").DataTable(resultsTableConfig);

	var $startField = $("#dateselector [name='start']");
	var $endField   = $("#dateselector [name='end']"  );

	var resultsTableRefresh = function(data, textStatus, jqXHR) {
		resultsTable.clear();
		var row = data.length;
		while (--row != -1) {
			resultsTable.row.add([
				data[row]["event"],
				data[row]["users"],
				data[row]["count"],
			]);
		}
		resultsTable.draw();
	};

	$("#dateselector").on("submit", function(event) {
		$.get(
			starexecRoot+"services/analytics",
			{
				"start": $startField.val(),
				"end": $endField.val()
			},
			resultsTableRefresh
		);

		event.preventDefault();
	});

	$("#dateselector").trigger("submit");

});
