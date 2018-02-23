"use strict";

jQuery(function($) {
	var fnPaginationHandler = function(sSource, aoData, fnCallback) {
		$.get(
			sSource,
			function(nextDataTablePage) {
				var s = parseReturnCode(nextDataTablePage);
				if (s) {
					fnCallback(nextDataTablePage);
				}
			},
			"json"
		).error(function() {
			showMessage('error', "Internal error populating data table", 5000);
		});
	};

	var tableConfig = new window.star.DataTableConfig({
		"sAjaxSource": starexecRoot + "services/testResults/pagination/" + $(
			"#sequenceName").attr("value"),
		"fnServerData": fnPaginationHandler,
		"order": [[1, 'asc'], [0, 'asc']]
	});

	var $tableTests = $('#tableTests').dataTable(tableConfig);

	setInterval(function() {
		$tableTests.dataTable().api().ajax.reload(null, false);
	}, 5000);
});
