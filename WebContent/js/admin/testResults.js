$(document).ready(function(){
	var tableConfig = new window.star.DataTableConfig({
		"sAjaxSource"  : starexecRoot+"services/testResults/pagination/"+$("#sequenceName").attr("value"),
		"fnServerData" : fnPaginationHandler // included in this file
	});
	var $tableTests=$('#tableTests').dataTable(tableConfig);
	setInterval(function() {
		$tableTests.dataTable().api().ajax.reload(null,false);
	},5000);
});


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
