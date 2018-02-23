"use strict";

jQuery(function($) {
	var $tableTests = $("#tableTests");

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
			showMessage("error", "Internal error populating data table", 5000);
		});
	};

	var colorizeRow = function(row, data, index) {
		if (data[3] !== 0) {
			$(row).addClass("fail");
		} else if (data[4] === "success") {
			$(row).addClass("success");
		}
	};

	var tableConfig = new window.star.DataTableConfig({
		"sAjaxSource": starexecRoot + "services/tests/pagination",
		"fnServerData": fnPaginationHandler,
		"rowCallback": colorizeRow,
		"order": [[4, 'asc'], [0, 'asc']]
	});

	var buttonStyle = {
		icons: {
			primary: "ui-icon-check"
		}
	};

	$("#runAll").button(buttonStyle).click(function() {
		$.post(
			starexecRoot + "services/test/runAllTests",
			{},
			parseReturnCode,
			"json"
		);
		$tableTests.find("tr").removeClass("row_selected");
	});

	$("#runSelected").button(buttonStyle).click(function() {
		var nameArray = getSelectedRows($tableTests);
		$.post(
			starexecRoot + "services/test/runTests",
			{testNames: nameArray},
			parseReturnCode,
			"json"
		);
		$tableTests.find("tr").removeClass("row_selected");
	});

	$("#runStress").button(buttonStyle).click(function() {
		window.open(starexecRoot + "secure/admin/stressTest.jsp");
	});

	$tableTests.dataTable(tableConfig).on("click", "tr", function() {
		$(this).toggleClass("row_selected");
	});

	setInterval(function() {
		var rows = $tableTests.children("tbody").children("tr.row_selected");
		if (rows.length === 0) {
			$tableTests.dataTable().api().ajax.reload(null, false);
		}
	}, 5000);
});

/**
 * For a given dataTable, this extracts the id's of the rows that have been
 * selected by the user
 *
 * @param {jQuery} $dataTable the particular dataTable to extract the id's from
 * @returns {Array} list of id values for the selected rows
 * @author Todd Elvers
 */
function getSelectedRows($dataTable) {
	var nameArray = [];
	var rows = $dataTable.children("tbody").children("tr.row_selected");
	$.each(rows, function(i, row) {
		nameArray.push($(this).find("a:first").attr("name"));
	});
	return nameArray;
}
