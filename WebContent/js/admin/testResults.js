$(document).ready(function(){
	var $tableTests=$('#tableTests');
	initUI();
	setInterval(function() {
		$tableTests.dataTable().api().ajax.reload(null,false);
	},5000);
});


function initUI(){
	var $tableTests=$('#tableTests');
	var tableConfig = new window.star.DataTableConfig({
		"sAjaxSource"  : starexecRoot+"services/testResults/pagination/"+$("#sequenceName").attr("value"),
		"fnServerData" : fnPaginationHandler // included in this file
	});

	$tableTests.dataTable(tableConfig).on( "click", "tr", function() {
		$(this).toggleClass("row_selected");
	});
}


function fnPaginationHandler(sSource, aoData, fnCallback) {
	$.get(
		sSource,
		function(nextDataTablePage) {
			var s = parseReturnCode(nextDataTablePage);
			if (s) {
				fnCallback(nextDataTablePage);
			}
		},
		"json"
	).error(function(){
		showMessage('error',"Internal error populating data table",5000);
	});
}
