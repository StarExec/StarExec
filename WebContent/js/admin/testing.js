$(document).ready(function(){
	var $tableTests=$("#tableTests");
	initUI();
	setInterval(function() {
		rows = $tableTests.children("tbody").children("tr.row_selected");
		if (rows.length==0) {
			$tableTests.dataTable().api().ajax.reload(null,false);
		}
	},5000);
});


function initUI(){
	var $tableTests=$("#tableTests");
	var tableConfig = new window.star.DataTableConfig({
		"sAjaxSource"  : starexecRoot + "services/tests/pagination",
		"fnServerData" : fnPaginationHandler // included in this file
	});

	var buttonStyle = {
		icons: {
			primary: "ui-icon-check"
		}
	};

	$("#runAll").button(buttonStyle).click(function() {
		$.post(
			starexecRoot+"services/test/runAllTests",
			{},
			parseReturnCode,
			"json"
		);
		$tableTests.find("tr").removeClass("row_selected");
	});

	$("#runSelected").button(buttonStyle).click(function() {
		nameArray=getSelectedRows($tableTests);
		$.post(
			starexecRoot+"services/test/runTests",
			{testNames : nameArray},
			parseReturnCode,
			"json"
		);
		$tableTests.find("tr").removeClass("row_selected");
	});

	$("#runStress").button(buttonStyle).click(function() {
		window.open(starexecRoot+"secure/admin/stressTest.jsp");
	});

	$tableTests.dataTable(tableConfig).on( "click", "tr", function() {
		$(this).toggleClass("row_selected");
	});

}


/**
 * For a given dataTable, this extracts the id's of the rows that have been
 * selected by the user
 *
 * @param {jQuery} $dataTable the particular dataTable to extract the id's from
 * @returns {Array} list of id values for the selected rows
 * @author Todd Elvers
 */
function getSelectedRows($dataTable){
	var nameArray = [];
	var rows = $dataTable.children("tbody").children("tr.row_selected");
	$.each(rows, function(i, row) {
		nameArray.push($(this).find("a:first").attr("name"));
	});
	return nameArray;
}

function fnPaginationHandler(sSource, aoData, fnCallback) {
	$.get(
		sSource,
		function(nextDataTablePage){
			var s = parseReturnCode(nextDataTablePage);
			if (s) {
				fnCallback(nextDataTablePage);
			}
		},
		"json"
	).error(function(){
		showMessage("error","Internal error populating data table",5000);
	});
}
